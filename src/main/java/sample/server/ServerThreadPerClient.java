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
import java.util.List;

/*
Клиент устанавливает постоянное соединение.
Сервер создает отдельный поток на общение (прием запроса, выполнение запроса и отправку ответа) с конкретным клиентом.
 */
public class ServerThreadPerClient {
    private static boolean listening = true;

    public static void main(String[] arg) {
        Thread statThread = new Thread(() -> StatServer.run());
        statThread.start();
        try (ServerSocket serverSocket = new ServerSocket(8081)) {
            while (listening) {
                Socket socket = serverSocket.accept();
                Thread t = new Thread(new Task(socket));
                t.start();
            }
            //close StatServer
        } catch (IOException e) {
            System.err.println("Could not listen on port " + 8081);
            System.exit(-1);
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
            int queryCounter  = 0;
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
            }
            finally {
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
