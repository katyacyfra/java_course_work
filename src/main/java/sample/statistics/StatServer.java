package sample.statistics;

import sample.ClientMessageProtos;
import sample.server.ServerThreadPerClient;
import sample.server.Sorter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;

public class StatServer {
    private static ServerSocket serverSocket;

    public static void run() {
        Socket socket = null;
        try {
            serverSocket = new ServerSocket(8085);
            while (true) {
                socket = serverSocket.accept();
                Thread t = new Thread(new StatServer.Task(socket));
                t.start();
            }
        } catch (SocketException e) {
            System.out.println("Stat server closed");
        } catch (IOException e) {
            System.out.println("Could not listen on port " + 8085);
        } finally {
            try {
                if (!serverSocket.isClosed()) {
                    serverSocket.close();
                }
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    public static void quit() {
        ;
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
            try (DataOutputStream os = new DataOutputStream(clientSocket.getOutputStream());
                 DataInputStream is = new DataInputStream(clientSocket.getInputStream())) {

                int clientAndQuery = is.readInt();

                StatAggregator.newServerCount();
                long serverTime = StatAggregator.getServerTimes();
                long sortingTime = StatAggregator.getSortingTimes();

                os.writeLong(serverTime);
                os.writeLong(sortingTime);
                os.flush();

            } catch (IOException e) {
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


