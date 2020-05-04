package ac.jcourse.mchat.protocol;

/**
 * Protocol Strings that will be used in Listeners.
 *  
 * @author Andy Cheung
 */
public interface Protocol {
    int SERVER_PORT = 10240;
    int SERVER_MULTICAST_PORT = 10241;
    
    int CLIENT_PORT = 10242;
    int CLIENT_MULTICAST_PORT = 10243;
    
    byte[] MULTICAST_IP = {(byte) 230, 0, 119, 1};
    
    // << CONNECT >>(UUID)>>>>(Name)<< CONNECT >>
    String CONNECTING_GREET_LEFT_HALF = "<< CONNECT >>";
    String CONNECTING_GREET_MIDDLE_HALF = ">>>>>";
    String CONNECTING_GREET_RIGHT_HALF = "<< CONNECT >>";
    
    String DISCONNECT = "<< DISCONNECT >>";
    
    // << MESSAGE >>> <<<< (UUID) >>>> << MESSAGE >> (messageContent)
    String MESSAGE_HEADER_LEFT_HALF = "<< MESSAGE >>> <<<<";
    String MESSAGE_HEADER_MIDDLE_HALF = ">>>>>";
    String MESSAGE_HEADER_RIGHT_HALF = " << MESSAGE >>";
    
    String DEBUG_MODE_STRING = "/// DEBUG ///";
    
    String BROADCAST_MESSAGE_UUID = "SERVER";
    
    String DENIED = ">>> DENIED <<<";
}
