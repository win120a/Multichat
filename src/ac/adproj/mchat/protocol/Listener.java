package ac.adproj.mchat.protocol;

/**
 * Interface that represents a connection listener.
 * 
 * @author Andy Cheung
 * @date 2020/4/26
 * @see Protocol
 * @see AutoCloseable
 */
public interface Listener extends Protocol, AutoCloseable {
    void sendMessage(String message, String uuid);

    void sendCommunicationData(String text, String uuid);
    
    boolean isConnected();
}
