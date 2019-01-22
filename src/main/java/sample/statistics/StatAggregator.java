package sample.statistics;

import java.util.ArrayList;
import java.util.List;

public class StatAggregator {
    static private List<Long> clientTimesPerVal = new ArrayList<>();
    static private List<Long> sortingTimesPerVal = new ArrayList<>();
    static private List<Long> serverTimesPerVal = new ArrayList<>();

    static private List<Long> clientTimes = new ArrayList<>();
    static public List<Long> clientServerTimes = new ArrayList<>();
    static public List<Long> clientSortingTimes = new ArrayList<>();

    static private long serverTimes;
    static private long sortingTimes;

    public static void addTimePerClient(long time, int queries) {
        clientTimesPerVal.add(time);
    }

    public static void addServerTimePerClient(long time, int queries) {
        serverTimesPerVal.add(time);
    }

    public static void addSortingTimePerClient(long time, int queries) {
        sortingTimesPerVal.add(time/(long) queries);
    }

    public static List<Long> getClientTimes() {
        return clientTimes;
    }

    public static long getServerTimes() {
        return serverTimes;
    }

    public static long getSortingTimes() {
        return sortingTimes;
    }

    public static void newServerCount(int del) {
        long sumSort = 0;
        long sumServer = 0;
        for (long v : serverTimesPerVal) {
            sumServer += v;
        }
        for (long v : sortingTimesPerVal) {
            sumSort += v;
        }
        serverTimes = sumServer/del;
        sortingTimes = sumSort/del;
        sortingTimesPerVal.clear();
        serverTimesPerVal.clear();
    }


    public static void newCount() {
        long sum = 0;
        for (long v : clientTimesPerVal) {
            sum += v;
        }
        clientTimes.add(sum/clientTimesPerVal.size());
        clientTimesPerVal.clear();
    }

    public static void reset() {
        clientTimes.clear();
        clientSortingTimes.clear();
        clientServerTimes.clear();
    }



}
