/*
    Copyright (C) 2011-2020 Andy Cheung

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package ac.adproj.mchat.ui;

import static ac.adproj.mchat.ui.CommonDialogs.*;

import java.io.IOException;

import org.eclipse.swt.widgets.Display;

import ac.adproj.mchat.protocol.ClientListener;
import ac.adproj.mchat.protocol.Protocol;

public class ClientUI extends BaseChattingUI {
    private ClientListener listener;
    
    public void initListener(byte[] ipAddress, int port, String userName) throws IOException {
        if (!ClientListener.checkNameDuplicates(ipAddress, userName)) {
            listener = new ClientListener(this, (message) -> appendMessageDisplay(message), ipAddress, port, userName);
        } else {
            errorDialog("用户名重复了！");
            initListener(ipAddress, port, getUserName());
        }
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
        String portString = inputDialog(Integer.toString(Protocol.CLIENT_DEFAULT_PORT), "请输入客户端端口：", "必须输入端口！", (s) -> {
            int port = Integer.parseInt(s);
            return s.matches("\\d+") && port > 0 && port <= 65535;
        });
        
        return Integer.parseInt(portString);
    }
    
    private static String getUserName() {
        return inputDialog("请输入用户名", "必须输入用户名！");
    }

    public static void main(String[] args) throws IOException {
        ClientUI ui = new ClientUI();
        
        ui.setText(ui.getText() + " - C");
        
        ui.open();
        
        ui.initListener(getServerIPAddress(), getPort(), getUserName());

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
