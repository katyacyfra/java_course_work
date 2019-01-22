package sample.server;

import sample.ClientMessageProtos;
import sample.statistics.StatAggregator;
import sample.statistics.StatHolder;
import sample.statistics.StatServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/*
Клиент устанавливает постоянное соединение.
Сервер создает по отдельному  потоку на каждого клиента для приема от него данных + по одному SingleThreadExecutor для отсылки данных клиенту. Полученные от клиента запросы попадают в общий пул потоков фиксированного размера.
После обработки ответ клиенту отправляется через соответствующий SingleThreadExecutor.
 */
public class ServerThreadPool {
    private static boolean listening = true;
    private static ExecutorService es = Executors.newFixedThreadPool(5);

    public static void main(String[] arg) {
        Thread statThread = new Thread(() -> StatServer.run());
        statThread.start();
        try (ServerSocket serverSocket = new ServerSocket(8082)) {
            while (listening) {
                Socket socket = serverSocket.accept();
                Thread t = new Thread(new ServerThreadPerClient.Task(socket));
                t.start();
            }
            //close StatServer
        } catch (IOException e) {
            System.err.println("Could not listen on port " + 8082);
            System.exit(-1);
        }
        finally {
            es.shutdown();
        }
    }

    static class Task implements Runnable {
        Socket clientSocket;

        Task(Socket client) {
            clientSocket = client;
        }

        @Override
        public void run() {
            ExecutorService answer = Executors.newSingleThreadExecutor();
            StatHolder sh = new StatHolder();
            int queryCounter  = 0;
            try (DataInputStream is = new DataInputStream(clientSocket.getInputStream());
                 DataOutputStream os = new DataOutputStream(clientSocket.getOutputStream())) {
                sh.startServerTimer();
                while (true) {
                    ClientMessageProtos.Sorting array = ClientMessageProtos.Sorting.parseDelimitedFrom(is);
                    if (array == null) { //stop reading
                        break;
                    }

                    queryCounter++;
                    es.submit(() -> {
                        sh.startSortingTimer();
                        List<Integer> result = Sorter.sort(array.getNumberList());
                        sh.endSortingTimer();

                        //send
                        answer.submit(()-> {
                            ClientMessageProtos.Sorting.Builder serializer = ClientMessageProtos.Sorting.newBuilder();
                            serializer.setSize(array.getSize());
                            serializer.addAllNumber(result);
                            ClientMessageProtos.Sorting message = serializer.build();
                            try {
                                message.writeDelimitedTo(os);
                                os.flush();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                    });


                    //System.out.println(result);
                    sh.endServerTimer();
                }
                answer.shutdown();
                answer.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (IOException | InterruptedException e) {
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
