/*
    Copyright (C) 2011-2024 Andy Cheung

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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Base Chatting UI.
 * 
 * @author Andy Cheung
 */
public abstract class BaseChattingUI extends Shell {
    protected Text messageToSend;
    protected Text messageDisplay;
    protected Button disconnect;
    protected Button send;
    
    private boolean hasReceivedMessage;
    
    /**
     * Append text to the chatting content field.
     * 
     * @param message The chatting message.
     */
    public void appendMessageDisplay(String message) {
        if (hasReceivedMessage) {
            messageDisplay.setText(messageDisplay.getText() + "\r\n" + message);
        } else {
            messageDisplay.setText(message);
            hasReceivedMessage = true;
        }
    }

    /**
     * Create the shell.
     * @param display
     */
    protected BaseChattingUI(Display display) {
        super(display, SWT.SHELL_TRIM);
        setLayout(new GridLayout(4, false));
        createContents();
    }
    
    protected BaseChattingUI() {
        this(new Display());
    }

    /**
     * Create contents of the shell.
     */
    protected void createContents() {
        
        messageDisplay = new Text(this, SWT.BORDER | SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL | SWT.CANCEL | SWT.MULTI);
        GridData gd_messageDisplay = new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1);
        gd_messageDisplay.heightHint = 364;
        messageDisplay.setLayoutData(gd_messageDisplay);
        
        messageToSend = new Text(this, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.CANCEL | SWT.MULTI);
        GridData gd_messageToSend = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 2);
        gd_messageToSend.heightHint = 103;
        messageToSend.setLayoutData(gd_messageToSend);
        
        disconnect = new Button(this, SWT.NONE);
        disconnect.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 2, 1));
        disconnect.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleDisconnect();
            }
        });
        disconnect.setText("  \u65AD\u5F00  ");
        
        setText("\u591A\u7AEF\u804A\u5929\u7A0B\u5E8F (TCP)");
        setSize(870, 647);
        
        send = new Button(this, SWT.NONE);
        send.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 2, 1));
        send.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (messageToSend.getText().isBlank()) {
                    return;
                }
                
                handleSendMessage(messageToSend.getText());
                messageToSend.setText("");
            }
        });
        send.setText("  \u53D1\u9001  ");
    }
    
    /**
     * Handler of clicking event of "Send" button.
     * 
     * @param text The text of the message.
     */
    protected abstract void handleSendMessage(String text);
    
    /**
     * Handler of the "Disconnect" button.
     */
    protected abstract void handleDisconnect();

    @Override
    protected void checkSubclass() {
        // Disable the check that prevents subclassing of SWT components
    }
}
