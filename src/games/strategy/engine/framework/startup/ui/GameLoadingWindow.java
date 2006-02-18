package games.strategy.engine.framework.startup.ui;

import java.awt.Font;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class GameLoadingWindow extends JWindow 
{
    
    InfiniteProgressPanel m_waitPanel = new InfiniteProgressPanel(true);

    public GameLoadingWindow()
    {
//        super("Game Loading, Please wait");
//        setIconImage(GameRunner.getGameIcon(this));
        setSize(450,200);
        
        //center ourselves
        setLocationRelativeTo(null);
        
        JLabel label = new JLabel();
        label.setFont(new Font("Serif", Font.PLAIN, 22)); 
        label.setText("Loading, please wait...");
        
        label.setBorder(new EmptyBorder(0,150,0,0));
        getContentPane().add(label);
        
        
        
//        etDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        
    }

    
    public void showWait()
    {
        SwingUtilities.invokeLater(new Runnable()
        {
        
            public void run()
            {
                
                setVisible(true);
                
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

                    }
                }
        );
        
    }
    
    public static void main(String[] args)
    {
        new GameLoadingWindow().showWait();
    }
    
}
