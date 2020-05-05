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

public abstract class BaseChattingUI extends Shell {
    protected Text messageToSend;
    protected Text messageDisplay;
    protected Button disconnect;
    protected Button send;
    
    private boolean hasReceivedMessage;
    
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
    public BaseChattingUI(Display display) {
        super(display, SWT.SHELL_TRIM);
        setLayout(new GridLayout(4, false));
        createContents();
    }
    
    public BaseChattingUI() {
        this(new Display());
    }

    /**
     * Create contents of the shell.
     */
    protected void createContents() {
        
        messageDisplay = new Text(this, SWT.BORDER | SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL | SWT.CANCEL | SWT.MULTI);
        GridData gd_messageDisplay = new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1);
        gd_messageDisplay.heightHint = 377;
        messageDisplay.setLayoutData(gd_messageDisplay);
        
        messageToSend = new Text(this, SWT.BORDER);
        messageToSend.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
        
        disconnect = new Button(this, SWT.NONE);
        disconnect.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleDisconnect();
            }
        });
        disconnect.setText("  \u65AD\u5F00  ");
        
        send = new Button(this, SWT.NONE);
        send.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false, 1, 1));
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
        
        setText("\u53CC\u7AEF\u5520\u55D1\u7A0B\u5E8F");
        setSize(794, 577);
    }
    
    protected abstract void handleSendMessage(String text);
    protected abstract void handleDisconnect();

    @Override
    protected void checkSubclass() {
        // Disable the check that prevents subclassing of SWT components
    }
}
