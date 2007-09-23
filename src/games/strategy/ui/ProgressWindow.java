package games.strategy.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

public class ProgressWindow extends JWindow
{

    public ProgressWindow(Frame owner, String title)
    {
        super(owner);
        JLabel label = new JLabel(title);
        label.setBorder(new EmptyBorder(10, 10, 10, 10));
        JProgressBar progressBar = new JProgressBar();
        progressBar.setBorder(new EmptyBorder(10, 10, 10, 10));
        progressBar.setIndeterminate(true);
        JPanel panel = new JPanel();
        panel.setBorder(new LineBorder(Color.BLACK));
        panel.setLayout(new BorderLayout());
        panel.add(BorderLayout.NORTH, label);
        panel.add(progressBar, BorderLayout.CENTER);
        setLayout(new BorderLayout());
        setSize(200, 80);
        add(panel, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);
    }
}
