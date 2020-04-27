package ac.jcourse.mchat.protocol;

public interface Listener extends Protocol, AutoCloseable {
    void sendCommunicationData(String text);
    
    void sendMessage(String message);
}
