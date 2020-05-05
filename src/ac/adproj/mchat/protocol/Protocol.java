package ac.adproj.mchat.protocol;

/**
 * Strings that will be used in the Protocol and Listeners.
 *  
 * @author Andy Cheung
 */
public interface Protocol {
    /**
     * Port of chatting service of Server.
     */
    int SERVER_PORT = 10240;
    
    /**
     * Port of UDP Users' Name Query Service.
     */
    int SERVER_CHECK_DUPLICATE_PORT = 10241;
    
    /**
     * Default port of client.
     */
    int CLIENT_DEFAULT_PORT = 10242;
    
    /**
     * Size of buffers in NIO.
     */
    int BUFFER_SIZE = 1024;
    
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
    
    // <<< DUP ? >>> (Name)
    String CHECK_DUPLICATE_REQUEST_HEADER = "<<< DUP ? >>> ";
    String USER_NAME_DUPLICATED = ">>> DUPLICATED <<< ";
    String USER_NAME_NOT_EXIST = "<<< Clear >>>";
}
