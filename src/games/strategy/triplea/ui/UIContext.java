package games.strategy.triplea.ui;

import java.awt.Window;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import games.strategy.triplea.image.*;

/**
 * A place to find images and map data for a ui.
 * 
 * @author sgb
 */
public class UIContext
{
    private MapData m_mapData;
    private TileImageFactory m_tileImageFactory = new TileImageFactory();
    private String m_mapDir;
    private UnitImageFactory m_unitImageFactory;
    private MapImage m_mapImage ;
    private FlagIconImageFactory m_flagIconImageFactory = new FlagIconImageFactory();
    private DiceImageFactory m_diceImageFactory = new DiceImageFactory();

    private boolean m_isShutDown;
    
    private List<CountDownLatch> m_latchesToCloseOnShutdown = new ArrayList<CountDownLatch>();
    private List<Window> m_windowsToCloseOnShutdown = new ArrayList<Window>();
    
    public UIContext()
    {
        m_mapImage = new MapImage();
    }

    public void setMapDir(String dir)
    {
        m_mapData = new MapData(dir);
        m_tileImageFactory.setMapDir(dir);

        m_mapImage.loadMaps(dir); // load map data
        
        //only create once
        if(m_mapDir == null)
        {
            m_unitImageFactory = new UnitImageFactory(m_mapData.getDefaultUnitScale());
        }

        m_mapDir = dir;
    }
    
    public MapData getMapData()
    {
        return m_mapData;
    }

    public String getMapDir()
    {
        return m_mapDir;
    }
    
    public TileImageFactory getTileImageFactory()
    {
        return m_tileImageFactory;
    }
    
    public UnitImageFactory getUnitImageFactory()
    {
        return m_unitImageFactory;
    }
    
    public MapImage getMapImage()
    {
        return m_mapImage;
    }
    
    public FlagIconImageFactory getFlagImageFactory()
    {
        return m_flagIconImageFactory;
    }
    
    public DiceImageFactory getDiceImageFactory()
    {
        return m_diceImageFactory;
    }
    
    /**
     * Add a latch that will be released when the game shuts down.
     */
    public void addShutdownLatch(CountDownLatch latch)
    {
        synchronized(this)
        {
            if(m_isShutDown)
            {
                releaseLatch(latch);
                return;
            }
            m_latchesToCloseOnShutdown.add(latch);
        }
    }
        
    public void removeShutdownLatch(CountDownLatch latch)
    {
        synchronized(this)
        {
            m_latchesToCloseOnShutdown.remove(latch);
        }
    }
    
    /**
     * Add a latch that will be released when the game shuts down.
     */
    public void addShutdownWindow(Window window)
    {
        synchronized(this)
        {
            if(m_isShutDown)
            {
                closeWindow(window);
                return;
            }
            m_windowsToCloseOnShutdown.add(window);
        }
    }
        
    private void closeWindow(Window window)
    {
       window.setVisible(false);
    }

    public void removeShutdownWindow(Window window)
    {
        synchronized(this)
        {
            m_latchesToCloseOnShutdown.remove(window);
        }
    }    
    
    
    private void releaseLatch(CountDownLatch latch)
    {
        while(latch.getCount() > 0)
        {
            latch.countDown();
        }
    
    }

    
    public void shutDown()
    {
        synchronized(this)
        {
            if(m_isShutDown)
                return;
            m_isShutDown = true;
            
            for(CountDownLatch latch : m_latchesToCloseOnShutdown)
            { 
                releaseLatch(latch);
            }
            
            for(Window window : m_windowsToCloseOnShutdown)
            { 
                closeWindow(window);
            }
            
        }
    }
}
