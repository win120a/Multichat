package ac.adproj.mchat.protocol.handler;

import java.nio.channels.AsynchronousSocketChannel;

/**
 * Interface that represents a message handler.
 * 
 * @author Andy Cheung
 * @date 2020/4/26
 */
public interface Handler {
    default String handleMessage(String message, AsynchronousSocketChannel channel) {
        return message;
    }
}
