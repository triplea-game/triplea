package games.strategy.engine.framework.ui.background;

import java.awt.BorderLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class WaitPanel extends JPanel
{
    public WaitPanel(String waitMessage)
    {
        setLayout(new BorderLayout());

        JLabel label = new JLabel(waitMessage);
        label.setBorder(new EmptyBorder(10, 10, 10, 10));

        add(BorderLayout.NORTH, label);

        int min = 0;
        int max = 100;
        JProgressBar progress = new JProgressBar(min, max);
        progress.setBorder(new EmptyBorder(10, 10, 10, 10));
        add(progress, BorderLayout.CENTER);
        progress.setIndeterminate(true);
    }
    
    
}
