package games.strategy.engine.framework.startup.ui;

import java.awt.*;

import javax.swing.*;
import javax.swing.border.*;

public class GameLoadingWindow extends JWindow 
{
    
    
    public GameLoadingWindow()
    {
//        super("Game Loading, Please wait");
//        setIconImage(GameRunner.getGameIcon(this));
        setSize(200, 80);
        
        
        JPanel mainPanel = new JPanel();
        
        
        mainPanel.setLayout(new BorderLayout());
        
        JLabel label = new JLabel("Loading game, please wait.");
        label.setBorder(new EmptyBorder(10,10,10,10 ));
        
        mainPanel.add(BorderLayout.NORTH, label);
        
            
          int min = 0;
          int max = 100;
          JProgressBar progress = new JProgressBar(min, max);
          progress.setBorder(new EmptyBorder(10,10,10,10));
          mainPanel.add(progress, BorderLayout.CENTER);
          progress.setIndeterminate(true);
        
          setLocationRelativeTo(null);
         // setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
          
          
          mainPanel.setBorder(new LineBorder(Color.BLACK));
          
          
          setLayout(new BorderLayout());
          add(mainPanel, BorderLayout.CENTER);
    }

    
    public void showWait()
    {
       
       
    }

    
    public void doneWait()
    {
        SwingUtilities.invokeLater(new Runnable()
                {
                
                    public void run()
                    {
                        
                        setVisible(false);
                        removeAll();
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
