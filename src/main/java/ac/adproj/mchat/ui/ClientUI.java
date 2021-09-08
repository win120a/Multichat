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
import lombok.extern.slf4j.Slf4j;
import org.eclipse.swt.widgets.Display;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static ac.adproj.mchat.ui.CommonDialogs.errorDialog;
import static ac.adproj.mchat.ui.CommonDialogs.inputDialog;

@Slf4j
public class ClientUI extends BaseChattingUI {

    private final AtomicReference<ClientListener> listenerAtomicReference = new AtomicReference<>();

    private ClientListener getListener() {
        return listenerAtomicReference.get();
    }

    private static String getUserName() {
        return inputDialog("请输入用户名", "必须输入用户名！");
    }

    public static void main(String[] args) {
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
            ui.getListener().close();
        } catch (Exception e) {
            // ignore
        }
    }

    public void initListener(byte[] ipAddress, int port, String userName) {
        setText("连接服务器中……");

        ClientListener.checkNameDuplicatesAsync(ipAddress, userName, hasDuplicate -> {
            if (!hasDuplicate) {
                try {
                    getDisplay().syncExec(() -> setText("\u591A\u7AEF\u804A\u5929\u7A0B\u5E8F (TCP)"));

                    listenerAtomicReference.compareAndExchange(null,
                            new ClientListener(this, this::appendMessageDisplay, ipAddress, port, userName));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                getDisplay().syncExec(() -> errorDialog("用户名重复了！"));
                initListener(ipAddress, port, getUserName());
            }
        }, () -> getDisplay().syncExec(() -> {
            errorDialog("查询用户名的占用情况时，服务器没响应！");
            System.exit(-1);
        }));
    }

    @Override
    protected void handleSendMessage(String text) {
        getListener().sendMessage(text);
        appendMessageDisplay(getListener().getUserName() + ": " + text);
    }

    @Override
    protected void handleDisconnect() {
        try {
            getListener().disconnect();
        } catch (IOException e) {
            log.error("Error when disconnecting (initiated by user).", e);
        }

        send.setEnabled(false);
        disconnect.setEnabled(false);
    }

}
