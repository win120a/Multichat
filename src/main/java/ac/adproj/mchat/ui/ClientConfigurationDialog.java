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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;

/**
 * Client configuration dialog.
 *
 * @author SWT desinger
 * @author Andy Cheung
 */
public class ClientConfigurationDialog extends Shell {
    private Text serverIP;
    private Text port;
    private Text nicknameField;
    private StatusWrapper status;

    class StatusWrapper {
        public final byte[] ip;
        public final int port;
        public final String nickname;
        
        public StatusWrapper(String ip, String port, String nickname) {
            super();
            
            String[] address = ip.split("[.]");
            
            byte[] addressByteArray = new byte[4];
            
            for (int i = 0; i < address.length; i++) {
                addressByteArray[i] = (byte) Integer.parseInt(address[i]);
            }
            
            this.ip = addressByteArray;
            this.port = Integer.parseInt(port);
            this.nickname = nickname;
        }
    }
    
    public static StatusWrapper showDialog() {
        try {
            Display display = Display.getDefault();
            ClientConfigurationDialog shell = new ClientConfigurationDialog(display);
            shell.open();
            shell.layout();
            while (!shell.isDisposed()) {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            }
            
            return shell.status;
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }

    /**
     * Create the shell.
     * @param display
     */
    public ClientConfigurationDialog(Display display) {
        super(display, SWT.CLOSE | SWT.TITLE | SWT.APPLICATION_MODAL);
        setLayout(new FormLayout());
        
        Label lblNewLabel = new Label(this, SWT.NONE);
        lblNewLabel.setAlignment(SWT.CENTER);
        FormData fd_lblNewLabel = new FormData();
        fd_lblNewLabel.left = new FormAttachment(0, 10);
        fd_lblNewLabel.top = new FormAttachment(0, 37);
        lblNewLabel.setLayoutData(fd_lblNewLabel);
        lblNewLabel.setText("\u670D\u52A1\u5668 IP");
        
        serverIP = new Text(this, SWT.BORDER);
        serverIP.setText("127.0.0.1");
        fd_lblNewLabel.right = new FormAttachment(serverIP, -14);
        FormData fd_serverIP = new FormData();
        fd_serverIP.top = new FormAttachment(0, 34);
        fd_serverIP.right = new FormAttachment(100, -10);
        fd_serverIP.left = new FormAttachment(0, 114);
        serverIP.setLayoutData(fd_serverIP);
        
        Button ok = new Button(this, SWT.NONE);
        ok.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int prt = Integer.parseInt(port.getText());
                
                boolean legalPort = port.getText().matches("\\d+") && prt > 0 && prt <= 65535;
                boolean legalIP = serverIP.getText().matches("\\d.+\\d.+\\d.+\\d+");
                
                if (legalPort & legalIP & nicknameField.getText().replaceAll("\\s+", "").isEmpty()) {
                    CommonDialogs.errorDialog("三者均不可空白，且前两者要按格式输入！");
                    return;
                }
                
                status = new StatusWrapper(serverIP.getText(), port.getText(), nicknameField.getText());
                ClientConfigurationDialog.this.setVisible(false);
                dispose();
            }
        });
        FormData fd_ok = new FormData();
        fd_ok.bottom = new FormAttachment(100, -10);
        fd_ok.left = new FormAttachment(0, 161);
        ok.setLayoutData(fd_ok);
        ok.setText("\u786E\u5B9A");
        
        Button cancel = new Button(this, SWT.NONE);
        cancel.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                dispose();
            }
        });
        fd_ok.right = new FormAttachment(100, -354);
        FormData fd_cancel = new FormData();
        fd_cancel.top = new FormAttachment(ok, 0, SWT.TOP);
        fd_cancel.left = new FormAttachment(ok, 85);
        fd_cancel.right = new FormAttachment(100, -148);
        cancel.setLayoutData(fd_cancel);
        cancel.setText("\u53D6\u6D88");
        
        Label label = new Label(this, SWT.NONE);
        label.setAlignment(SWT.CENTER);
        label.setText("\u5BA2\u6237\u673A\u7AEF\u53E3");
        FormData fd_label = new FormData();
        fd_label.top = new FormAttachment(lblNewLabel, 34);
        fd_label.right = new FormAttachment(lblNewLabel, 0, SWT.RIGHT);
        fd_label.left = new FormAttachment(0, 10);
        label.setLayoutData(fd_label);
        
        port = new Text(this, SWT.BORDER);
        port.setText("0");
        FormData fd_port = new FormData();
        fd_port.top = new FormAttachment(label, -3, SWT.TOP);
        fd_port.left = new FormAttachment(serverIP, 0, SWT.LEFT);
        fd_port.right = new FormAttachment(serverIP, 0, SWT.RIGHT);
        port.setLayoutData(fd_port);
        
        Label label_1 = new Label(this, SWT.NONE);
        label_1.setAlignment(SWT.CENTER);
        label_1.setText("\u6635\u79F0");
        FormData fd_label_1 = new FormData();
        fd_label_1.top = new FormAttachment(label, 34);
        fd_label_1.right = new FormAttachment(lblNewLabel, 0, SWT.RIGHT);
        fd_label_1.left = new FormAttachment(0, 10);
        label_1.setLayoutData(fd_label_1);
        
        nicknameField = new Text(this, SWT.BORDER);
        FormData fd_nicknameField = new FormData();
        fd_nicknameField.top = new FormAttachment(label_1, -3, SWT.TOP);
        fd_nicknameField.left = new FormAttachment(serverIP, 0, SWT.LEFT);
        fd_nicknameField.right = new FormAttachment(serverIP, 0, SWT.RIGHT);
        nicknameField.setLayoutData(fd_nicknameField);
        createContents();
    }

    /**
     * Create contents of the shell.
     */
    protected void createContents() {
        setText("Client Configuration");
        setSize(642, 301);
    }

    @Override
    protected void checkSubclass() {
        // Disable the check that prevents subclassing of SWT components
    }

}
