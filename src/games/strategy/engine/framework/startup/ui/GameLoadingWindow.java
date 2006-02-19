package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.framework.GameRunner;

import java.awt.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class GameLoadingWindow extends JWindow 
{
    
    private InfiniteProgressPanel m_waitPanel = new InfiniteProgressPanel(true);
    
    
    public GameLoadingWindow()
    {
//        super("Game Loading, Please wait");
//        setIconImage(GameRunner.getGameIcon(this));
        setSize(400,400);
        
        setLayout(new BorderLayout());
        
//        JPanel p = new JPanel();
//        add(p, BorderLayout.CENTER);
//        
        //center ourselves
        setLocationRelativeTo(null);
        
       
      //  setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        
    }

    
    public void showWait()
    {
        SwingUtilities.invokeLater(new Runnable()
        {
        
            public void run()
            {
                
                
                SwingUtilities.invokeLater(new Runnable()
                {
                
                    public void run()
                    {
                        setGlassPane(m_waitPanel);
                        m_waitPanel.setVisible(true);  
                        
                        
                    }
                });
            }
        
        });
        
    }

    
    public void doneWait()
    {
        SwingUtilities.invokeLater(new Runnable()
                {
                
                    public void run()
                    {
                        setVisible(false);
                        m_waitPanel.setVisible(false);
                        dispose();

                    }
                }
        );
        
    }
    
    public static void main(String[] args)
    {
        
        
        SwingUtilities.invokeLater(new Runnable()
        {
        
            public void run()
            {
                final GameLoadingWindow window = new GameLoadingWindow();
                window.setVisible(true);
                
                window.showWait();
        
            }
        
        });
        
    }
    
}
