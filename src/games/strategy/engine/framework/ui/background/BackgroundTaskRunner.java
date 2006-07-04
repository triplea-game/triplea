package games.strategy.engine.framework.ui.background;

import java.awt.Component;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

public class BackgroundTaskRunner
{
    
    public static void runInBackground(Component parent, String waitMessage, final Runnable r)
    {
        if(!SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("Wrong thread");
        
        final WaitDialog window = new WaitDialog(parent, waitMessage);

        final AtomicBoolean doneWait = new AtomicBoolean(false);
        
        
        Thread t = new Thread(new Runnable()
        {
        
            public void run()
            {
                try
                {
                    r.run();
                }
                finally
                {
                    SwingUtilities.invokeLater(new Runnable()
                    {
                    
                        public void run()
                        {
                            doneWait.set(true);
                            window.setVisible(false);
                            window.dispose();
                        }
                    
                    });
                    
                }
            }
        
        });
        t.start();
        
        if(!doneWait.get())
        {
            window.pack();
            window.setLocationRelativeTo(parent);
            window.setVisible(true);
        }
    }
    
}
