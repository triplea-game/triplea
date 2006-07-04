package games.strategy.engine.framework.ui.background;

import java.awt.*;

import javax.swing.*;

public class WaitDialog extends JDialog
{

    public WaitDialog(Component parent, String waitMessage)
    {
        super(JOptionPane.getFrameForComponent(parent), "Please Wait" , true);
        WaitPanel panel = new WaitPanel(waitMessage);
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
        
    }
    
    
    
}
