package sample.server;

import sample.ClientMessageProtos;
import sample.statistics.StatAggregator;
import sample.statistics.StatHolder;
import sample.statistics.StatRunner;
import sample.statistics.StatServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;

/*
Клиент устанавливает постоянное соединение.
Сервер создает отдельный поток на общение (прием запроса, выполнение запроса и отправку ответа) с конкретным клиентом.
 */
public class ServerThreadPerClient {
    public static ServerSocket serverSocket;

    public static void main(String[] arg) {
        Thread statThread = new Thread(() -> StatServer.run());
        statThread.start();
        Socket socket = null;
        try {
            serverSocket = new ServerSocket(8081);
            while (true) {
                socket = serverSocket.accept();
                Thread t = new Thread(new Task(socket));
                t.start();
            }
        } catch (SocketException e) {
            System.out.println("Thread per Client Server closed");
        } catch (IOException e) {
            System.err.println("Could not listen on port " + 8081);
        } finally {
            try {
                if (!serverSocket.isClosed()) {
                    serverSocket.close();
                }
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
                StatServer.quit();
                statThread.join();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void quit() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class Task implements Runnable {
        Socket clientSocket;

        Task(Socket client) {
            clientSocket = client;
        }

        @Override
        public void run() {
            StatHolder sh = new StatHolder();
            int queryCounter = 0;
            try (DataInputStream is = new DataInputStream(clientSocket.getInputStream());
                 DataOutputStream os = new DataOutputStream(clientSocket.getOutputStream())) {
                //int receivedSize = in.readInt32();
                //System.out.println("received: " + receivedSize);
                sh.startServerTimer();
                while (true) {
                    ClientMessageProtos.Sorting array = ClientMessageProtos.Sorting.parseDelimitedFrom(is);
                    if (array == null) { //stop reading
                        break;
                    }
                    queryCounter++;
                    sh.startSortingTimer();
                    List<Integer> result = Sorter.sort(array.getNumberList());
                    sh.endSortingTimer();
                    //System.out.println(result);

                    ClientMessageProtos.Sorting.Builder serializer = ClientMessageProtos.Sorting.newBuilder();
                    serializer.setSize(array.getSize());
                    serializer.addAllNumber(result);
                    ClientMessageProtos.Sorting message = serializer.build();

                    //os.writeInt32NoTag(message.getSerializedSize());
                    message.writeDelimitedTo(os);
                    os.flush();
                    sh.endServerTimer();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    StatAggregator.addServerTimePerClient(sh.getServerTime(), queryCounter);
                    StatAggregator.addSortingTimePerClient(sh.getSortingTime(), queryCounter);

                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


        }
    }

}
