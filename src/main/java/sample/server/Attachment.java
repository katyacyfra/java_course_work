package sample.server;

import sample.statistics.StatHolder;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

class Attachment {
    int size = -1;
    ByteBuffer intBuf = ByteBuffer.allocate(4);
    ByteBuffer arrayBuf;
    StatHolder sh = new StatHolder();

    List<Integer> result = new ArrayList<>();

}