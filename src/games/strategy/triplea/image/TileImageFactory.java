package games.strategy.triplea.image;

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

import games.strategy.engine.data.PlayerID;
import games.strategy.triplea.Constants;
import games.strategy.triplea.ui.MapData;
import games.strategy.ui.ImageIoCompletionWatcher;

import java.awt.*;
import java.io.File;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.HashMap;
import java.util.prefs.*;

public final class TileImageFactory
{

    private static boolean s_showReliefImages = true;
    private static final boolean cacheImages = true;
    private final static String SHOW_RELIEF_IMAGES_PREFERENCE = "ShowRelief";

    static
    {
        Preferences prefs = Preferences.userNodeForPackage(TileImageFactory.class);
        s_showReliefImages = prefs.getBoolean(SHOW_RELIEF_IMAGES_PREFERENCE, false);
        s_mapDir="classic";//default to using the classic map files
    }

    public static boolean getShowReliefImages()
    {
        return s_showReliefImages;
    }

    public static void setShowReliefImages(boolean aBool)
    {
        s_showReliefImages = aBool;
        Preferences prefs = Preferences.userNodeForPackage(TileImageFactory.class);
        prefs.putBoolean(SHOW_RELIEF_IMAGES_PREFERENCE, s_showReliefImages);
        try
        {
          prefs.flush();
        }
        catch (BackingStoreException ex)
        {
          ex.printStackTrace();
        }
    }

    private static String s_mapDir;

    public static String getMapDir()
    {
      return s_mapDir;
    }
    
    public static void setMapDir(String dir)
    {
      s_mapDir=dir;
    }

    private HashMap m_cache = new HashMap();

    // one instance in the application
    private static TileImageFactory s_singletonInstance = new TileImageFactory();


    // return the singleton
    public static TileImageFactory getInstance()
    {
        return s_singletonInstance;
    }


    // constructor
    private TileImageFactory()
    {
    }

 
    public Color getPlayerColour(PlayerID owner)
    {
        return MapData.getInstance().getPlayerColor(owner.getName());
    }

    
    public Image getBaseTile(int x, int y)
    {
        String key = x + "_" + y + "base";
        SoftReference ref = (SoftReference) m_cache.get(key);
        
        if(ref != null)
        {
            Image cached = (Image) ref.get();
            if(cached != null)
                return cached;
        }
        
        String fileName = Constants.MAP_DIR+s_mapDir+File.separator +"baseTiles"+java.io.File.separator + x + "_" + y + ".png";
        URL file = this.getClass().getResource(fileName);
        if(file == null)
            return null;
        Image img = loadImageCompletely(file);
        if(cacheImages)
            m_cache.put(key, new SoftReference(img));
        return img;
    }

    
    public Image getReliefTile(int x, int y)
    {
        String key = x + "_" + y + "relief";
        SoftReference ref = (SoftReference) m_cache.get(key);
        
        if(ref != null)
        {
            Image cached = (Image) ref.get();
            if(cached != null)
                return cached;
        }
        
        String fileName = Constants.MAP_DIR+s_mapDir+File.separator +"reliefTiles"+java.io.File.separator + x + "_" + y + ".png";
        URL file = this.getClass().getResource(fileName);
        if(file == null)
            return null;
        Image img = loadImageCompletely(file);
        if(cacheImages)
            m_cache.put(key, new SoftReference(img));
        return img;
        
        
    }





    private Image loadImageCompletely(URL imageLocation)
    {

//       try
//    {
//        return ImageIO.read(imageLocation);
//    } catch (IOException e)
//    {
//        e.printStackTrace();
//        throw new IllegalStateException(e.getMessage());
//    }
        
      // use the local toolkit to load the image
      Toolkit tk = Toolkit.getDefaultToolkit();
      Image img = tk.createImage(imageLocation);

      // force it to be loaded *now*
      ImageIoCompletionWatcher watcher = new ImageIoCompletionWatcher();
      boolean isLoaded = tk.prepareImage(img, -1, -1, watcher);

      // use the watcher to block while loading
      if (!isLoaded)
      {
          watcher.waitForCompletion();
      }

      // done!
      return img;
    }

}//end class TerritoryImageFactory