package ac.jcourse.mchat.protocol;


public interface Protocol {
    int SERVER_PORT = 10240;
    int CLIENT_PORT = 10241;
    
    String DISCONNECT = "<< DISCONNECT >>";
    String CONNECTING_GREET = "<< CONNECT >>";
    String MESSAGE_HEADER_LEFT_HALF = "<< MESSAGE >>> <<<<";
    String MESSAGE_HEADER_RIGHT_HALF = ">>>>";
    
    String DEBUG_MODE_STRING = "/// DEBUG ///";
}
