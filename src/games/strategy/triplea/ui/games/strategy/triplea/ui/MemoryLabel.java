package games.strategy.triplea.ui;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.ref.WeakReference;
import java.text.DecimalFormat;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

public class MemoryLabel extends JLabel
{

    public MemoryLabel()
    {
        update();
        
        addMouseListener( new MouseAdapter()
        {
        
            public void mouseReleased(MouseEvent e)
            {
                if(e.isPopupTrigger())
                    gc(e);

            }
        
            public void mousePressed(MouseEvent e)
            {
                if(e.isPopupTrigger())
                    gc(e);

            }
        
        });
        
        
        Thread t = new Thread(new Updater(this), "Memory Label Updater");
        t.start();
    }
    
    protected void gc(MouseEvent e)
    {
        JPopupMenu menu = new JPopupMenu();
        menu.add(new AbstractAction("Garbage Collect")
        {
        
            public void actionPerformed(ActionEvent arg0)
            {
                System.gc();
                System.runFinalization();
                System.gc();
                System.runFinalization();
                System.gc();
            }
        });
        
        menu.show(this, e.getX(), e.getY());
    }

    public void update()
    {
        long free = Runtime.getRuntime().freeMemory();
        long total = Runtime.getRuntime().totalMemory();
        long used = total - free;
        
        DecimalFormat format = new DecimalFormat("###.##");
        
        setText(  format.format(used / 1000000.0) + "/" + format.format(total / 1000000.0) + " MB");
    }
    
    
    
    
    public static void main(String[] args)
    {
        JFrame f = new JFrame();
        f.add(new MemoryLabel());
        f.pack();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setVisible(true);
        
    }
    
    
    
}

/**
 * 
 * This thread will stop when the label is garbage collected
 * 
 * @author sgb
 */
class Updater implements Runnable
{
    private final WeakReference<MemoryLabel> m_label;
    
    Updater(MemoryLabel label)
    {
        m_label = new WeakReference<MemoryLabel>(label);
    }
    
    public void run()
    {
        while(m_label.get() != null)
        {
            sleep();            
            update();
        }
        
    }

    private void update()
    {
        SwingUtilities.invokeLater( new Runnable()
        {
        
            public void run()
            {
                MemoryLabel label = m_label.get();
                if(!label.isVisible())
                    return;
                
                if(label != null)
                    label.update();
            }
        
        });
    }

    private void sleep()
    {
        try
        {
            Thread.sleep(2000);
        } catch (InterruptedException e)
        {}
    }
    
}