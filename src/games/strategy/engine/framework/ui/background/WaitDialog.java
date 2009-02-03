package games.strategy.engine.framework.ui.background;

import java.awt.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class WaitDialog extends JDialog
{

    public WaitDialog(Component parent, String waitMessage)
    {
        super(JOptionPane.getFrameForComponent(parent), "Please Wait" , true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        WaitPanel panel = new WaitPanel(waitMessage);
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
        
    }
    
    public WaitDialog(Component parent, String waitMessage, final
                      Runnable cancel) {
        this(parent, waitMessage);
        Button cancelButton = new Button("Cancel");
        add(cancelButton);
        cancelButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    cancel.run();
                }
            });
    }
    
    
}
