package games.strategy.engine.framework.ui.background;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

public class WaitDialog extends JDialog
{

    public WaitDialog(Component parent, String waitMessage)
    {
        this(parent, waitMessage,null);            
    }    
    
    public WaitDialog(Component parent, String waitMessage, Action cancelAction) {
        super(JOptionPane.getFrameForComponent(parent), "Please Wait" , true);
        
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        WaitPanel panel = new WaitPanel(waitMessage);
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
        
        if(cancelAction != null) {
            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(cancelAction);
            add(cancelButton,BorderLayout.SOUTH);
        }
    }
    
    
}
