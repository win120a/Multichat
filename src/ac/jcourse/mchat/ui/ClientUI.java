package ac.jcourse.mchat.ui;

import java.awt.Component;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

import javax.swing.Icon;
import javax.swing.JOptionPane;

import org.eclipse.swt.widgets.Display;

import ac.jcourse.mchat.protocol.ClientListener;
import ac.jcourse.mchat.protocol.Protocol;

import static ac.jcourse.mchat.protocol.Protocol.*;

public class ClientUI extends BaseChattingUI {
    
    private AsynchronousSocketChannel asc;
    private String uuid;
    private ClientListener listener;
    
    public void initListener(byte[] ipAddress, int port) throws IOException {
        listener = new ClientListener(this, (message) -> appendMessageDisplay(message), ipAddress, port);
        uuid = listener.getUuid();
    }

    @Override
    protected void handleSendMessage(String text) {
        listener.sendMessage(text);
        appendMessageDisplay(uuid + ": " + text);
    }

    @Override
    protected void handleDisconnect() {
        try {
            listener.disconnect();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        send.setEnabled(false);
        disconnect.setEnabled(false);
    }
    
    private static String inputDialog(String askMessage, String errorMessage) {
        return inputDialog(null, askMessage, errorMessage);
    }
    
    private static String inputDialog(String defaultString, String askMessage, String errorMessage) {
        String response;
        int counter = 0;
        
        do {
            response = (String) JOptionPane.showInputDialog(null, askMessage, "Input", JOptionPane.QUESTION_MESSAGE, null, null, defaultString);
            counter++;
        } while ((response == null || response.isBlank()) && counter <= 5);
        
        if (counter > 5) {
            JOptionPane.showMessageDialog(null, errorMessage, "ERROR", JOptionPane.ERROR_MESSAGE);
            System.exit(-1);
        }
        
        return response;
    }
    
    private static byte[] getServerIPAddress() {

        String ipAddress = inputDialog("127.0.0.1", "请输入服务器IPv4地址：", "必须输入IP地址！");

        String[] address = ipAddress.split("[.]");
        
        byte[] addressByteArray = new byte[4];
        
        for (int i = 0; i < address.length; i++) {
            addressByteArray[i] = Byte.parseByte(address[i]);
        }
        
        return addressByteArray;
    }
    
    private static int getPort() {
        String port = inputDialog(Integer.toString(Protocol.CLIENT_PORT), "请输入客户端端口：", "必须输入端口！");
        
        return Integer.parseInt(port);
    }

    public static void main(String[] args) throws IOException {
        ClientUI ui = new ClientUI();
        
        ui.setText(ui.getText() + " - C");
        
        ui.open();
        
        ui.initListener(getServerIPAddress(), getPort());

        Display d = ui.getDisplay();
        
        ui.setActive();

        while (!ui.isDisposed()) {
            if (!d.readAndDispatch()) {
                d.sleep();
            }
        }
        
        try {
            ui.listener.close();
        } catch (Exception e) {
            // ignore
        }
    }

}
