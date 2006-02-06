/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * ActionPanel.java
 *
 * Created on December 4, 2001, 7:41 PM
 */

package games.strategy.triplea.ui;

import java.util.concurrent.CountDownLatch;

import games.strategy.engine.data.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * 
 * Abstract superclass for all action panels. <br>
 * 
 * 
 * @author Sean Bridges
 * @version 1.0
 *  
 */
public abstract class ActionPanel extends JPanel
{
    private GameData m_data;
    private PlayerID m_currentPlayer;
    private MapPanel m_map;    
    private boolean m_active;
    private CountDownLatch m_latch;
    private final Object m_latchLock = new Object();
    
    
    /** Creates new ActionPanel */
    public ActionPanel(GameData data, MapPanel map)
    {
        m_data = data;
        m_map = map;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(5, 5, 0, 0));
    }

    
    /**
     * Waitfor another thread to call release.
     * If the thread is interupted, we will return silently.<p>
     * 
     * A memory barrier will be crossed both on entering and before exiting this method.<p>
     * 
     * This method will return in the event of the game shutting down.<p>
     */
    protected void waitForRelease()
    {
        synchronized(m_latchLock)
        {
            if(m_latch != null)
                throw new IllegalStateException("Latch not null");
            m_latch = new CountDownLatch(1);
            
            m_map.getUIContext().addShutdownLatch(m_latch);
        }
        
        try
        {
            m_latch.await();
        } catch (InterruptedException e)
        {
        }
        
        //cross a memory barrier
        synchronized(m_latchLock)
        {}
        
    }
    
    /**
     * Release the latch acquired by waitOnNewLatch()<p>
     * 
     * This method will crossed on entering this method.<p>
     */
    protected void release()
    {
        synchronized(m_latchLock)
        {
            //not set up yet
            //this is ok as we set up in one thread
            //and wait in another
            //if the release happens too early
            //the user will be able to press done again
            if(m_latch == null)
                return;
            
            m_map.getUIContext().removeShutdownLatch(m_latch);
            
            m_latch.countDown();
            m_latch = null;
        }
        
    }
    
    
    
    protected GameData getData()
    {
        return m_data;
    }

    public void display(PlayerID player)
    {
        m_currentPlayer = player;
        setActive(true);
    }

    protected PlayerID getCurrentPlayer()
    {
        return m_currentPlayer;
    }

    protected MapPanel getMap()
    {
        return m_map;
    }

    /**
     * Called when the history panel shows used to disable the panel
     * temporarily.
     */
    public void setActive(boolean aBool)
    {
        m_active = aBool;
    }

    public boolean getActive()
    {
        return m_active;
    }

    /**
     * Refreshes the action panel. Should be run within the swing event queue.
     */
    protected final Runnable REFRESH = new Runnable()
    {
        public void run()
        {
            revalidate();
            repaint();
        }
    };
}