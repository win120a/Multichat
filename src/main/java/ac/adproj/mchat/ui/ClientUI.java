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

import ac.adproj.mchat.protocol.ClientListener;
import org.eclipse.swt.widgets.Display;

import java.io.IOException;

import static ac.adproj.mchat.ui.CommonDialogs.errorDialog;
import static ac.adproj.mchat.ui.CommonDialogs.inputDialog;

public class ClientUI extends BaseChattingUI {
    private ClientListener listener;
    
    public void initListener(byte[] ipAddress, int port, String userName) throws IOException {
        if (!ClientListener.checkNameDuplicates(ipAddress, userName)) {
            listener = new ClientListener(this, this::appendMessageDisplay, ipAddress, port, userName);
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
    
    private static String getUserName() {
        return inputDialog("请输入用户名", "必须输入用户名！");
    }

    public static void main(String[] args) throws IOException {
        ClientUI ui = new ClientUI();
        
        ui.setText(ui.getText() + " - C");
        
        ui.open();
        
        ClientConfigurationDialog.StatusWrapper cfd = ClientConfigurationDialog.showDialog();
        
        if (cfd == null) {
            System.exit(-1);
        }
        
        ui.initListener(cfd.ip, cfd.port, cfd.nickname);

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
