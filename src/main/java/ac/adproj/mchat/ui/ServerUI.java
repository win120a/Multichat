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

import ac.adproj.mchat.model.Protocol;
import ac.adproj.mchat.protocol.ServerListener;
import ac.adproj.mchat.service.CommonThreadPool;
import ac.adproj.mchat.service.MessageDistributor;
import ac.adproj.mchat.web.WebServerStarter;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import java.io.IOException;

public class ServerUI extends BaseChattingUI {
    private ServerListener listener;

    private void initListener() throws IOException {
        listener = ServerListener.getInstance();
        
        MessageDistributor.getInstance().registerSubscriber(message -> this.getDisplay().asyncExec(() -> {
            appendMessageDisplay(message);
        }));
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

        listener.disconnectAll();
    }

    public static void main(String[] args) throws Exception {
        ServerUI ui = new ServerUI();

        ui.setText(ui.getText() + " - S");
        ui.disconnect.setText("断开全部客户端");

        ui.initListener();
        
        Display d = ui.getDisplay();
        
        WebServerStarter s = new WebServerStarter();
        s.start(8090);

        ui.open();
        ui.layout();

        while (!ui.isDisposed()) {
            if (!d.readAndDispatch()) {
                d.sleep();
            }
        }
        
        s.close();
        ui.listener.close();
        CommonThreadPool.shutdown();
    }

}
