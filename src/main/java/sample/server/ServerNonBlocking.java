package sample.server;
//https://stackoverflow.com/questions/1057224/java-thread-blocks-while-registering-channel-with-selector-while-select-is-cal

import com.google.protobuf.InvalidProtocolBufferException;
import sample.ClientMessageProtos;
import sample.statistics.StatAggregator;

import sample.statistics.StatServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;

import java.nio.ByteBuffer;

import java.nio.channels.*;
import java.util.Iterator;
import java.util.List;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



/*
Клиент устанавливает постоянное соединение.
Сервер производит неблокирующую обработку.
Каждый запрос обрабатывается пуле потоков фиксированного размера.
Сервер работает с сокетами в однопоточном режиме
(один поток и селектор на прием всех сообщений и один поток и селектор на отправку всех сообщений).
 */

public class ServerNonBlocking {
    private static boolean listening = true;
    private static Selector selector;
    private static Selector sendSelector;
    private static ExecutorService es = Executors.newFixedThreadPool(5);


    public static void main(String[] arg) throws IOException {
        try {
            Thread statThread = new Thread(() -> StatServer.run());
            statThread.start();

            selector = Selector.open();
            sendSelector = Selector.open();

            ServerSocketChannel serverSocket = ServerSocketChannel.open();
            InetSocketAddress addr = new InetSocketAddress("localhost", 8083);

            serverSocket.bind(addr);
            serverSocket.configureBlocking(false);
            int options = serverSocket.validOps();
            serverSocket.register(selector, SelectionKey.OP_ACCEPT);
            //SelectionKey key = serverSocket.register(selector, options, null);

            Thread senderThread = new Thread(() -> {
                try {
                    processSender();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            senderThread.start();

            while (true) {
                selector.select();
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = keys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey current = iterator.next();
                    if (current.isAcceptable()) {
                        processAccept(serverSocket);
                    } else if (current.isReadable()) {
                        processRead(current);
                    }
                    iterator.remove();
                }

            }
        } finally {
            es.shutdown();
        }

    }

    private static void processAccept(ServerSocketChannel serverSocketChannel) throws IOException {
        SocketChannel client = serverSocketChannel.accept();
        client.configureBlocking(false);
        Attachment attach = new Attachment();
        client.register(selector, SelectionKey.OP_READ, attach);


    }

    private static void processRead(SelectionKey key) {
        SocketChannel client = (SocketChannel) key.channel();
        Attachment message = (Attachment) key.attachment();

        try {
            //reading size
            if (message.size == -1) {
                int bytesToRead = client.read(message.intBuf);
                if (bytesToRead <= 0) {
                    key.cancel();
                    //return;
                } else {
                    //start working
                    message.sh.startServerTimer();
                }

                //allocate size
                if (message.intBuf.position() == 4) {
                    message.intBuf.flip();
                    message.size = message.intBuf.getInt();
                    message.arrayBuf = ByteBuffer.allocate(message.size);
                }
            }
            //reading message
            if (message.size > 0) {
                client.read(message.arrayBuf);
                if (message.arrayBuf.position() == message.size) {
                    message.size = -1;
                    message.arrayBuf.flip();
                    //sort
                    es.submit(new Task(message, key));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            key.cancel();
        }
    }

    private static void processSender() throws IOException {

        while (true) {
            //https://stackoverflow.com/questions/1057224/java-thread-blocks-while-registering-channel-with-selector-while-select-is-cal
            sendSelector.select(100);
            Set<SelectionKey> keys = sendSelector.selectedKeys();
            Iterator<SelectionKey> iterator = keys.iterator();
            while (iterator.hasNext()) {
                SelectionKey current = iterator.next();
                Attachment message = (Attachment) current.attachment();
                SocketChannel socket = (SocketChannel) current.channel();


                if (message != null) {
                    ClientMessageProtos.Sorting.Builder serializer = ClientMessageProtos.Sorting.newBuilder();
                    List<Integer> result = message.result;
                    serializer.setSize(result.size());
                    serializer.addAllNumber(result);
                    ClientMessageProtos.Sorting serializedMessage = serializer.build();
                    ByteBuffer buff = ByteBuffer.allocate(4 + serializedMessage.getSerializedSize());

                    //os.writeInt32NoTag(message.getSerializedSize());

                    try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                        serializedMessage.writeDelimitedTo(bos);
                        buff.put(bos.toByteArray());
                        buff.flip();
                    }
                    socket.write(buff);
                    //socket.write(message.intBuf);
                    //socket.write(ByteBuffer.wrap(bytesToSend));

                    if (buff.position() == buff.limit())
                    {
                        message.sh.endServerTimer();
                        message.intBuf.rewind();
                        message.size = -1;
                        current.cancel();
                    }

                    StatAggregator.addServerTimePerClient(message.sh.getServerTime(), 1);
                    StatAggregator.addSortingTimePerClient(message.sh.getSortingTime(), 1);
                }
                iterator.remove();
            }
        }
    }

    static class Task implements Runnable {
        List<Integer> array;
        Attachment attachment;
        SocketChannel channel;

        Task(Attachment atch, SelectionKey key) throws InvalidProtocolBufferException {
            attachment = atch;
            channel = (SocketChannel) key.channel();

            ByteBuffer buf = atch.arrayBuf;
            byte[] byteArray = buf.array();
            ClientMessageProtos.Sorting message = ClientMessageProtos.Sorting.parseFrom(byteArray);
            if (message == null) { //stop reading
                key.cancel();
                return;
            }
            array = message.getNumberList();
        }

        @Override
        public void run() {
            attachment.sh.startSortingTimer();
            List<Integer> result = Sorter.sort(array);
            attachment.sh.endSortingTimer();
            attachment.result = result;
            try {
                channel.register(sendSelector, SelectionKey.OP_WRITE, attachment);
                sendSelector.wakeup();
            } catch (ClosedChannelException e) {
                e.printStackTrace();
            }


        }
    }


    }

