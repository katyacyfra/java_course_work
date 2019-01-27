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
import java.net.SocketException;
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
    private static ExecutorService es = Executors.newFixedThreadPool(5);
    private static ServerSocket serverSocket;

    public static void main(String[] arg) {
        Thread statThread = new Thread(() -> StatServer.run());
        statThread.start();
        Socket socket = null;
        try {
            serverSocket = new ServerSocket(8082);
            while (true) {
                socket = serverSocket.accept();
                Thread t = new Thread(new ServerThreadPerClient.Task(socket));
                t.start();
            }
        } catch (SocketException e) {
            //close StatServer
            System.out.println("ThreadPool Server closed");
        } catch (IOException e) {
            System.err.println("Could not listen on port " + 8082);
        } finally {
            es.shutdown();
            try {
                es.awaitTermination(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
                es.shutdownNow();
            }
            try {
                if (!serverSocket.isClosed()) {
                    serverSocket.close();
                }
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }

                if (!serverSocket.isClosed()) {
                    serverSocket.close();
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
            ExecutorService answer = Executors.newSingleThreadExecutor();
            StatHolder sh = new StatHolder();
            try (DataInputStream is = new DataInputStream(clientSocket.getInputStream());
                 DataOutputStream os = new DataOutputStream(clientSocket.getOutputStream())) {
                while (true) {
                    sh.startServerTimer();
                    ClientMessageProtos.Sorting array = ClientMessageProtos.Sorting.parseDelimitedFrom(is);
                    if (array == null) { //stop reading
                        break;
                    }
                    es.submit(() -> {
                        sh.startSortingTimer();
                        List<Integer> result = Sorter.sort(array.getNumberList());
                        sh.endSortingTimer();

                        //send
                        answer.submit(() -> {
                            ClientMessageProtos.Sorting.Builder serializer = ClientMessageProtos.Sorting.newBuilder();
                            serializer.setSize(array.getSize());
                            serializer.addAllNumber(result);
                            ClientMessageProtos.Sorting message = serializer.build();
                            try {
                                message.writeDelimitedTo(os);
                                os.flush();
                                StatAggregator.addServerTimePerClient(sh.getServerTime(), 1);
                                StatAggregator.addSortingTimePerClient(sh.getSortingTime(), 1);
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
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


        }
    }
}
