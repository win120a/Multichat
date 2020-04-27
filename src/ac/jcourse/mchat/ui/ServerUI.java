package ac.jcourse.mchat.ui;

import java.io.IOException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import ac.jcourse.mchat.protocol.Protocol;
import ac.jcourse.mchat.protocol.ServerListener;

public class ServerUI extends BaseChattingUI {
    private ServerListener listener;

    public void initListener() throws IOException {
        listener = new ServerListener(this, (message) ->  {
            appendMessageDisplay(message);
        });
    }

    @Override
    protected void handleSendMessage(String text) {
        if (!listener.isConnected()) {
            MessageDialog.openError(ServerUI.this, "出错", "没有连接到客户端。");
            return;
        }
        
        appendMessageDisplay("SERVER" + ": " + text);
        
        listener.sendMessage(text, Protocol.BROADCAST_MESSAGE_UUID);
    }

    @Override
    protected void handleDisconnect() {
        if (!listener.isConnected()) {
            MessageDialog.openError(ServerUI.this, "出错", "没有连接到客户端。");
            return;
        }
        
        try {
            listener.disconnectAll();
        } catch (IOException e) {
            e.printStackTrace();
            MessageDialog.openError(ServerUI.this, "出错", "断开失败：" + e.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {
        ServerUI ui = new ServerUI();

        ui.setText(ui.getText() + " - S");
        ui.disconnect.setText("断开全部客户端");

        ui.initListener();
        
        Display d = ui.getDisplay();

        ui.open();
        ui.layout();

        while (!ui.isDisposed()) {
            if (!d.readAndDispatch()) {
                d.sleep();
            }
        }
        
        ui.listener.close();
    }

}
