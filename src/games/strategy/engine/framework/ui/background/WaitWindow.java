package games.strategy.engine.framework.ui.background;

import java.awt.*;
import java.util.*;
import java.util.Timer;

import javax.swing.*;
import javax.swing.border.*;

public class WaitWindow extends JWindow
{

    private final Object m_mutex = new Object();
    private Timer m_timer = new Timer();


    public WaitWindow(String waitMessage)
    {
        // super("Game Loading, Please wait");
        // setIconImage(GameRunner.getGameIcon(this));
        setSize(200, 80);

        WaitPanel mainPanel = new WaitPanel(waitMessage);

        setLocationRelativeTo(null);
        // setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        mainPanel.setBorder(new LineBorder(Color.BLACK));

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    public void showWait()
    {
        TimerTask task = new TimerTask()
        {

            @Override
            public void run()
            {
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        toFront();
                    }
                }

                );

            }

        };

        synchronized(m_mutex)
        {
            if(m_timer != null)
                m_timer.schedule(task, 15, 15);
        }

    }

    public void doneWait()
    {
        synchronized(m_mutex)
        {
            if(m_timer != null)
            {
                m_timer.cancel();
                m_timer = null;
            }
        }

        SwingUtilities.invokeLater(new Runnable()
        {

            public void run()
            {

                setVisible(false);
                removeAll();
                dispose();

            }
        });

    }

    public static void main(String[] args)
    {

        SwingUtilities.invokeLater(new Runnable()
        {

            public void run()
            {
                final WaitWindow window = new WaitWindow("Loading game, please wait.");
                window.setVisible(true);

                window.showWait();

            }

        });

    }

}
