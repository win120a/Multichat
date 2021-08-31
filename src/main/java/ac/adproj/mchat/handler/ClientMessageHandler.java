package ac.adproj.mchat.handler;

import ac.adproj.mchat.model.Protocol;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.function.Consumer;

import static ac.adproj.mchat.handler.MessageType.getMessageType;

/**
 * Message Handler of client.
 * 
 * @author Andy Cheung
 */
public class ClientMessageHandler implements Handler {
    private Consumer<Void> forceLogoffCallback;

    /**
     * Get instance.
     * 
     * @param forceLogoffCallback Callback method wrapper when server urges the client to
     *                            be disconnected.
     */
    public ClientMessageHandler(Consumer<Void> forceLogoffCallback) {
        super();
        this.forceLogoffCallback = forceLogoffCallback;
    }

    @Override
    public String handleMessage(String message, AsynchronousSocketChannel channel) {

        switch (getMessageType(message)) {
            case INCOMING_MESSAGE:
                message = message.replace(Protocol.MESSAGE_HEADER_LEFT_HALF, "")
                        .replace(Protocol.MESSAGE_HEADER_RIGHT_HALF, "")
                        .replace(Protocol.MESSAGE_HEADER_MIDDLE_HALF, ": ");
                break;

            case LOGOFF:
                forceLogoffCallback.accept(null);
                message = "Server closed the connection.";
                break;

            case UNKNOWN:
            default:
                message = "";
                break;
        }

        return message;
    }
}