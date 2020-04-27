package ac.jcourse.mchat.protocol.handler;

import java.nio.channels.AsynchronousSocketChannel;

public interface Handler {
    default String handleMessage(String message, AsynchronousSocketChannel channel) {
        return message;
    }
}
