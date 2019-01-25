package sample.statistics;

import sample.Client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class StatRunner {
    public StatRunner(int port, int changedParameter, int step, int from, int to, int N, int M, int D, int X) throws BrokenBarrierException, InterruptedException {
        if (to < from) {
            throw new IllegalArgumentException("from should be less then to");
        }
        List<Integer> values = scale(step, from, to);

        switch (changedParameter) {
            case 1:
                runN(port, values, M, D, X);
                break;
            case 2:
                runM(port, values, N, D, X);
                break;
            case 3:
                runD(port, values, M, N, X);
                break;
        }
    }

    public List<Integer> scale(int step, int from, int to) {
        List<Integer> result = new ArrayList<>();
        int current = from;
        while (current <= to) {
            result.add(current);
            current += step;
        }
        return result;
    }

    private void runConf(int port, int X, int N, int M, int D) throws InterruptedException {
        ExecutorService es = Executors.newFixedThreadPool(M);
        for (int i = 0; i < M; i++) {
            es.execute(new Client(port, X, D, N));
        }
        es.shutdown();
        es.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }

    private void askForServerStat(int X, int M) {
        try (Socket query = new Socket("localhost", 8085);
             DataInputStream is = new DataInputStream(query.getInputStream());
             DataOutputStream os = new DataOutputStream(query.getOutputStream())) {
            os.writeInt(X * M);
            os.flush();

            StatAggregator.clientServerTimes.add(is.readLong());
            StatAggregator.clientSortingTimes.add(is.readLong());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void runN(int port, List<Integer> values, int M, int D, int X) throws BrokenBarrierException, InterruptedException {
        for (Integer n : values) {
            runConf(port, X, n, M, D);
            StatAggregator.newCount();
            askForServerStat(X, M);
            System.out.println("done " + n);
        }
    }

    private void runM(int port, List<Integer> values, int N, int D, int X) throws InterruptedException {
        for (Integer m : values) {
            runConf(port, X, N, m, D);
            StatAggregator.newCount();
            askForServerStat(X, m);
            System.out.println("done " + m);
        }
    }

    private void runD(int port, List<Integer> values, int M, int N, int X) throws InterruptedException {
        for (Integer d : values) {
            runConf(port, X, N, M, d);
            StatAggregator.newCount();
            askForServerStat(X, M);
            System.out.println("done " + d);
        }
    }
}
