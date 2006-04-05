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

package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.GameRunner;
import games.strategy.triplea.*;
import games.strategy.triplea.image.*;
import games.strategy.triplea.util.Stopwatch;

import java.awt.Window;
import java.io.File;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.logging.*;
import java.util.prefs.*;

import javax.swing.*;

/**
 * A place to find images and map data for a ui.
 * 
 * @author sgb
 */
public class UIContext
{
    
    private static final String UNIT_SCALE_PREF = "UnitScale";
    private static final String MAP_SKIN_PREF = "MapSkin";
    
    private static final Logger s_logger = Logger.getLogger(UIContext.class.getName());
    
    private MapData m_mapData;
    private TileImageFactory m_tileImageFactory = new TileImageFactory();
    private String m_mapDir;
    private UnitImageFactory m_unitImageFactory = new UnitImageFactory();
    private MapImage m_mapImage ;
    private FlagIconImageFactory m_flagIconImageFactory = new FlagIconImageFactory();
    private DiceImageFactory m_diceImageFactory = new DiceImageFactory();
    private final IPCImageFactory m_ipcImageFactory = new IPCImageFactory();
    private boolean m_isShutDown;
    
    private List<CountDownLatch> m_latchesToCloseOnShutdown = new ArrayList<CountDownLatch>();
    private List<Window> m_windowsToCloseOnShutdown = new ArrayList<Window>();
    private List<Active> m_activeToDeactivate = new ArrayList<Active>();
    
    
    public UIContext()
    {
        m_mapImage = new MapImage();
    }

    /**
     * Get the preferences for the map. 
     */
    private static Preferences getPreferencesForMap(String mapName)
    {
        return Preferences.userNodeForPackage(UIContext.class).node(mapName);
    }

    /**
     * Get the preferences for the map or map skin
     */
    private static Preferences getPreferencesMapOrSkin(String mapDir)
    {
        return Preferences.userNodeForPackage(UIContext.class).node(mapDir);
    }
    
    private static String getDefaultMapDir(GameData data)
    {
        String mapName = (String) data.getProperties().get(Constants.MAP_NAME);
        Preferences prefs = getPreferencesForMap(mapName);
        String mapDir =  prefs.get(MAP_SKIN_PREF, mapName);
     
        //check for existence
        try
        {
            ResourceLoader.getMapresourceLoader(mapDir);
        }
        catch(RuntimeException re)
        {
            //an error
            //clear the skin
            prefs.remove(MAP_SKIN_PREF);
            //return the default
            return mapName;
        }
        return mapDir;
        
    }
    
    public void setDefaltMapDir(GameData data)
    {
        internalSetMapDir(getDefaultMapDir(data));
    }
    
    
    public void setMapDir(GameData data, String mapDir)
    {
        internalSetMapDir(mapDir);
        
        //set the default after internal suceeds, if an error is thrown
        //we dont want to persist it
        String mapName = (String) data.getProperties().get(Constants.MAP_NAME);
        Preferences prefs = getPreferencesForMap(mapName);
        prefs.put(MAP_SKIN_PREF, mapDir);
        
        try
        {
            prefs.flush();
        } catch (BackingStoreException e)
        {
            e.printStackTrace();
        }
    }
    
    private void internalSetMapDir(String dir)
    {
        Stopwatch stopWatch = new Stopwatch(s_logger, Level.FINE, "Loading UI Context");
        
        ResourceLoader loader = ResourceLoader.getMapresourceLoader(dir);

        m_mapData = new MapData(loader);
        
        double scale = getPreferencesMapOrSkin(dir).getDouble(UNIT_SCALE_PREF, m_mapData.getDefaultUnitScale());
        m_unitImageFactory.setResourceLoader(loader, scale);
        
        m_flagIconImageFactory.setResourceLoader(loader);
        m_ipcImageFactory.setResourceLoader(loader);
        
        m_tileImageFactory.setMapDir(loader);
        m_mapImage.loadMaps(loader); // load map data
        
        m_mapDir = dir;
        
        stopWatch.done();
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
    
    public IPCImageFactory getIPCImageFactory()
    {
        return m_ipcImageFactory;
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
        
    
    
    public void removeACtive(Active actor)
    {
        synchronized(this)
        {
            m_activeToDeactivate.remove(actor);
        }
    }
    
    /**
     * Add a latch that will be released when the game shuts down.
     */
    public void addActive(Active actor)
    {
        synchronized(this)
        {
            if(m_isShutDown)
            {
                closeActor(actor);
                return;
            }
            m_activeToDeactivate.add(actor);
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
        
    private static void closeWindow(Window window)
    {
       window.setVisible(false);
       window.dispose();
       
       
       //there is a bug in java (1.50._06  for linux at least)
       //where frames are not garbage collected.
       //
       //http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6364875
       //http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6368950
       //
       //so remove all references to everything
       //to minimize the damage


       if(window instanceof JFrame)
       {
           JFrame frame = ((JFrame) window);
           
           JMenuBar menu = (JMenuBar) frame.getJMenuBar();
           if(menu != null)
           {
               while(menu.getMenuCount() > 0)
                   menu.remove(0);
           }
           
           frame.setMenuBar(null);
           frame.setJMenuBar(null);
           frame.getRootPane().removeAll();           
           frame.getRootPane().setJMenuBar(null);
           frame.getContentPane().removeAll();
           frame.setIconImage(null);
           
           
           clearInputMap(frame.getRootPane());
           
       }
 
       
    }
    
    private static void clearInputMap(JComponent c)
    {
       c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).clear();
       c.getInputMap(JComponent.WHEN_FOCUSED).clear();
       c.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).clear();
       
        c.getActionMap().clear();
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

    
    public boolean isShutDown()
    {
        return m_isShutDown;
    }
    
    public void shutDown()
    {
        synchronized(this)
        {
            if(m_isShutDown)
                return;
            m_isShutDown = true;
        }
        
        for(CountDownLatch latch : m_latchesToCloseOnShutdown)
        { 
            releaseLatch(latch);
        }
        
        for(Window window : m_windowsToCloseOnShutdown)
        { 
            closeWindow(window);
        }
        
        for(Active actor : m_activeToDeactivate)
        {
            closeActor(actor);
        }     
    }
    
    /**
     * returns the map skins for the game data.
     * 
     * returns is a map of display-name -> map directory
     */
    public static Map<String,String> getSkins(GameData data)
    {
        String mapName = data.getProperties().get(Constants.MAP_NAME).toString();
        Map <String,String> rVal = new LinkedHashMap<String,String>();
        rVal.put("Original", mapName);
        
        
        File root = new File( GameRunner.getRootFolder(), "maps" );
        for(File f : root.listFiles())
        {
            if(!f.isDirectory())
            {
                //jar files
                if(f.getName().endsWith(".zip") && f.getName().startsWith(mapName + "-"))
                {
                    String nameWithExtension = f.getName().substring(f.getName().indexOf('-') +1);
                    rVal.put(nameWithExtension.substring(0, nameWithExtension.length() - 4),  f.getName());
                    
                }
            }
            //directories
            else if(f.getName().startsWith(mapName + "-") )
            {
                rVal.put(f.getName().substring(f.getName().indexOf('-') +1),  f.getName());
            }
        }
        return rVal;
    }

    private void closeActor(Active actor) 
    {
        try
        {
            actor.deactivate();
        }
        catch(RuntimeException re)
        {
            re.printStackTrace();
        }
        
    }

    public void setUnitScaleFactor(double scaleFactor)
    {
        m_unitImageFactory.setScaleFactor(scaleFactor);
        Preferences prefs = getPreferencesMapOrSkin(getMapDir());
        prefs.putDouble(UNIT_SCALE_PREF, scaleFactor);
        try
        {
            prefs.flush();
        } catch (BackingStoreException e)
        {
            e.printStackTrace();
        }
        
    }
}
