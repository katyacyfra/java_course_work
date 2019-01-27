package sample;

import com.google.common.primitives.Ints;
import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;
import sample.statistics.StatAggregator;
import sample.statistics.StatHolder;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.net.Socket;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
//https://groups.google.com/forum/#!topic/protobuf/LDeQyhxbhJY

public class Client implements Runnable {
    private int numberofQueries;
    private int delta;
    private int numberOfElements;
    private int architecturePort;


    public Client(int architecture, int queries, int delt, int elementsNumber) {
        architecturePort = architecture;
        numberofQueries = queries;
        numberOfElements = elementsNumber;
        delta = delt;
    }

    @Override
    public void run() {
        //time
        StatHolder sh = new StatHolder();

        int counter = 0;
        try (Socket query = new Socket("localhost", architecturePort);
             DataOutputStream os = new DataOutputStream(query.getOutputStream());
             DataInputStream is = new DataInputStream(query.getInputStream())) {
            for (int i = 0; i < numberofQueries; ++i) {
                int[] randomIntsArray = IntStream.generate(() -> new Random().nextInt(1000000)).limit(numberOfElements).toArray();
                ClientMessageProtos.Sorting.Builder array = sample.ClientMessageProtos.Sorting.newBuilder();

                array.setSize(numberOfElements);
                array.addAllNumber(Ints.asList(randomIntsArray));

                //query
                sh.startClientTimer();
                ClientMessageProtos.Sorting message = array.build();
                //out.writeInt32NoTag(message.getSerializedSize());
                if (architecturePort == 8083) { // nonBlocking
                    int size = message.getSerializedSize();
                    byte[] bytesSize = ByteBuffer.allocate(4).putInt(size).array();
                    os.write(bytesSize);
                    message.writeTo(os);
                } else {
                    message.writeDelimitedTo(os);
                }
                os.flush();

                //answer
                counter++;
                sample.ClientMessageProtos.Sorting sorted = sample.ClientMessageProtos.Sorting.parseDelimitedFrom(is);
                sh.endClientTimer();
                TimeUnit.MILLISECONDS.sleep(delta);
            }
            StatAggregator.addTimePerClient(sh.getClientTime(), numberofQueries);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

}
