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
import java.util.concurrent.TimeUnit;

/*
Клиент устанавливает постоянное соединение.
Сервер производит неблокирующую обработку.
Каждый запрос обрабатывается пуле потоков фиксированного размера.
Сервер работает с сокетами в однопоточном режиме
(один поток и селектор на прием всех сообщений и один поток и селектор на отправку всех сообщений).
 */

public class ServerNonBlocking {
    private static Selector selector;
    private static Selector sendSelector;
    private static ExecutorService es = Executors.newFixedThreadPool(10);
    private static ServerSocketChannel serverSocket;


    public static void main(String[] arg) {
        try {
            selector = Selector.open();
            sendSelector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }


        //serverThread
        Thread serverThread = new Thread(new ServerAcceptTask());
        serverThread.start();

        System.out.println("Start reader");
        //reader Thread
        Thread readerThread = new Thread(new ReadTask());
        readerThread.start();

        //sender thread
        System.out.println("Start sender");
        Thread senderThread = new Thread(new SendTask());
        senderThread.start();

    }

    public static void quit() {
        try {
            selector.close();
            sendSelector.close();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ReadTask implements Runnable {
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
                        message.readBuf = ByteBuffer.allocate(message.size);
                        message.intBuf.clear();
                    }
                }
                //reading message
                if (message.size > 0) {
                    client.read(message.readBuf);
                    if (message.readBuf.position() == message.readBuf.capacity()) {
                        message.readBuf.flip();
                        //sort
                        es.submit(new Task(key));

                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
                key.cancel();
            }
        }

        @Override
        public void run() {
            try {
                while (true) {
                    selector.select(30);
                    Set<SelectionKey> keys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = keys.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey current = iterator.next();
                        if (current.isReadable()) {
                            processRead(current);
                        }
                        iterator.remove();
                    }

                }
            } catch (ClosedSelectorException e) {
                System.out.println("Close selector");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static class SendTask implements Runnable {
        @Override
        public void run() {
            while (true) {
                //https://stackoverflow.com/questions/1057224/java-thread-blocks-while-registering-channel-with-selector-while-select-is-cal
                try {
                    sendSelector.select(100);
                } catch (ClosedSelectorException e) {
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Iterator<SelectionKey> iterator;
                try {
                    Set<SelectionKey> keys = sendSelector.selectedKeys();
                    iterator = keys.iterator();
                } catch (ClosedSelectorException e) {
                    break;
                }
                while (iterator.hasNext()) {
                    SelectionKey current = iterator.next();
                    Attachment message = (Attachment) current.attachment();
                    SocketChannel socket = (SocketChannel) current.channel();

                    if (message.sizeToWrite != -1) {
                        try {
                            while (message.writeBuf.hasRemaining()) {
                               socket.write(message.writeBuf);
                            }
                            message.sh.endServerTimer();
                            message.sizeToWrite = -1;
                            StatAggregator.addServerTimePerClient(message.sh.getServerTime(), 1);
                            StatAggregator.addSortingTimePerClient(message.sh.getSortingTime(), 1);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    iterator.remove();
                }
            }

        }
    }

    private static class ServerAcceptTask implements Runnable {
        @Override
        public void run() {

            //statServer thread
            Thread statThread = new Thread(() -> StatServer.run());
            statThread.start();

            try {
                serverSocket = ServerSocketChannel.open();
                InetSocketAddress addr = new InetSocketAddress("localhost", 8083);
                serverSocket.bind(addr);

                while (true) {
                    SocketChannel client = serverSocket.accept();
                    client.configureBlocking(false);
                    Attachment attach = new Attachment();
                    client.register(selector, SelectionKey.OP_READ, attach);
                    client.register(sendSelector, SelectionKey.OP_WRITE, attach);
                }
            }
                catch (ClosedChannelException e) {
            } catch (IOException e) {
                System.out.println("Could not listen on port " + 8083);
            } finally {
                System.out.println("Shutdown");
                es.shutdown();
                try {
                    es.awaitTermination(60, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    es.shutdownNow();

                }
                try {
                    StatServer.quit();
                    statThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    }


    private static class Task implements Runnable {
        List<Integer> array;
        Attachment attachment;
        SocketChannel channel;

        Task(SelectionKey key) throws InvalidProtocolBufferException {
            attachment = (Attachment) key.attachment();
            channel = (SocketChannel) key.channel();

            ByteBuffer buf = attachment.readBuf;
            byte[] byteArray = buf.array();
            ClientMessageProtos.Sorting message = ClientMessageProtos.Sorting.parseFrom(byteArray);
            buf.clear();
            attachment.size = -1;
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
            ClientMessageProtos.Sorting.Builder serializer = ClientMessageProtos.Sorting.newBuilder();
            serializer.setSize(result.size());
            serializer.addAllNumber(result);
            ClientMessageProtos.Sorting serializedMessage = serializer.build();

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                serializedMessage.writeDelimitedTo(bos);
                attachment.writeBuf = ByteBuffer.wrap(bos.toByteArray());
                attachment.sizeToWrite = serializedMessage.getSerializedSize() + 4;

            } catch (IOException e) {
                e.printStackTrace();
            }
            sendSelector.wakeup();
        }
    }


}

