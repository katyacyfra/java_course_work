package sample.statistics;

import sample.ClientMessageProtos;
import sample.server.ServerThreadPerClient;
import sample.server.Sorter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class StatServer {
    private static boolean listening = true;

    public static void run() {
        try (ServerSocket serverSocket = new ServerSocket(8085)) {
            while (listening) {
                Socket socket = serverSocket.accept();
                Thread t = new Thread(new StatServer.Task(socket));
                t.start();
            }
        } catch (IOException e) {
            //port taken
            //System.err.println("Could not listen on port " + 8085);
            //System.exit(-1);
        }
    }

    public static void quit() {
        System.out.println("Quit!");
        listening = false;
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

                StatAggregator.newServerCount(clientAndQuery);
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


