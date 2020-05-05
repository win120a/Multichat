package ac.jcourse.mchat.ui;

import static ac.jcourse.mchat.ui.CommonDialogs.inputDialog;

import java.io.IOException;

import org.eclipse.swt.widgets.Display;

import ac.jcourse.mchat.protocol.ClientListener;
import ac.jcourse.mchat.protocol.Protocol;

public class ClientUI extends BaseChattingUI {
    private ClientListener listener;
    
    public void initListener(byte[] ipAddress, int port, String userName) throws IOException {
        listener = new ClientListener(this, (message) -> appendMessageDisplay(message), ipAddress, port, userName);
    }

    @Override
    protected void handleSendMessage(String text) {
        listener.sendMessage(text);
        appendMessageDisplay(listener.getUserName() + ": " + text);
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
    
    private static byte[] getServerIPAddress() {

        String ipAddress = inputDialog("127.0.0.1", "请输入服务器IPv4地址：", "必须输入IP地址！", (s) -> s.matches("\\d.+\\d.+\\d.+\\d+"));

        String[] address = ipAddress.split("[.]");
        
        byte[] addressByteArray = new byte[4];
        
        for (int i = 0; i < address.length; i++) {
            addressByteArray[i] = (byte) Integer.parseInt(address[i]);
        }
        
        return addressByteArray;
    }
    
    private static int getPort() {
        String portString = inputDialog(Integer.toString(Protocol.CLIENT_PORT), "请输入客户端端口：", "必须输入端口！", (s) -> {
            int port = Integer.parseInt(s);
            return s.matches("\\d+") && port > 0 && port <= 65535;
        });
        
        return Integer.parseInt(portString);
    }

    public static void main(String[] args) throws IOException {
        ClientUI ui = new ClientUI();
        
        ui.setText(ui.getText() + " - C");
        
        ui.open();
        
        ui.initListener(getServerIPAddress(), getPort(), inputDialog("请输入用户名", "必须输入用户名！"));

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
