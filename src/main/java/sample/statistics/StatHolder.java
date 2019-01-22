package sample.statistics;

public class StatHolder {
    private long clientQueryStart;
    private long sortingStart;
    private long serverQueryStart;

    private long clientTime;
    private long sortingTime;
    private long serverTime;


    public void startClientTimer() {
        clientQueryStart = System.currentTimeMillis();
    }

    public void endClientTimer() {
        clientTime += System.currentTimeMillis() - clientQueryStart;
    }

    public long getClientTime() {
        return clientTime;
    }

    public void startSortingTimer() {
        sortingStart = System.currentTimeMillis();
    }

    public void endSortingTimer() {
        sortingTime += System.currentTimeMillis() - sortingStart;
    }

    public long getSortingTime() {
        return sortingTime;
    }

    public void startServerTimer() {
        serverQueryStart = System.currentTimeMillis();
    }

    public void endServerTimer() {
        serverTime += System.currentTimeMillis() - serverQueryStart;
    }

    public long getServerTime() {
        return serverTime;
    }


}
