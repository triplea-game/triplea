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

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import java.awt.*;
import java.awt.image.BufferedImage;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.ui.ImageIoCompletionWatcher;
import games.strategy.triplea.ui.*;
import games.strategy.triplea.*;
import java.util.List;
import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.imageio.*;
import java.io.*;

public final class TerritoryImageFactory
{

    private static boolean s_showReliefImages = true;
    private final static String SHOW_RELIEF_IMAGES_PREFERENCE = "ShowRelief";


    static
    {
        Preferences prefs = Preferences.userNodeForPackage(TerritoryImageFactory.class);
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
        Preferences prefs = Preferences.userNodeForPackage(TerritoryImageFactory.class);
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


    private final int CACHE_SIZE = 5;

    private static String s_mapDir;

    public static String getMapDir()
    {
      return s_mapDir;
    }
    
    public static void setMapDir(String dir)
    {
      s_mapDir=dir;
    }

    private LinkedList m_cachedTerritories = new LinkedList();

    // one instance in the application
    private static TerritoryImageFactory s_singletonInstance = new TerritoryImageFactory();

    // data
    private Map m_playerColors = new HashMap();

    private GraphicsConfiguration m_localGraphicSystem = null;

    // return the singleton
    public static TerritoryImageFactory getInstance()
    {
        return s_singletonInstance;
    }

    // returns an image of the desired territory with the desired owner
    public BufferedImage getTerritoryImage(Territory place, PlayerID owner)
    {
        return createTerritoryImage(place, owner, true);
    }

    // constructor
    private TerritoryImageFactory()
    {

        // local graphic system is used to create compatible bitmaps
        m_localGraphicSystem = GraphicsEnvironment.getLocalGraphicsEnvironment()
            .getDefaultScreenDevice()
            .getDefaultConfiguration();

        // cache the player colors
        m_playerColors.put("British",   new Color(153, 102, 0));
        m_playerColors.put("Americans", new Color(102, 102, 0));
        m_playerColors.put("Russians",  new Color(153, 51, 0));
        m_playerColors.put("Germans",   new Color(119, 119, 119));
        m_playerColors.put("Japanese",  new Color(255, 153, 0));
        m_playerColors.put("Italians",  new Color(90, 90, 90));
        m_playerColors.put(PlayerID.NULL_PLAYERID.getName(), new Color(204, 153, 51));
    }

    // dynamically create a new territory image
    private BufferedImage createTerritoryImage(Territory place, PlayerID owner, boolean addReliefHighlights)
    {
       Rectangle bounding = TerritoryData.getInstance().getBoundingRect(place);

       BufferedImage workImage = m_localGraphicSystem.createCompatibleImage(bounding.width,
           bounding.height,
           Transparency.BITMASK);


       List polys = TerritoryData.getInstance().getPolygons(place);
       Iterator iter = polys.iterator();
       Graphics graphics = workImage.getGraphics();
       if(place.isWater())
       {
           graphics.setColor(Color.blue);
       }
       else
       {
           graphics.setColor(getPlayerColour(place.getOwner()));
       }

       Image img = getReliefImage(place);

       while (iter.hasNext())
       {
           Polygon polygon = (Polygon)iter.next();
           //use a copy
           polygon = new Polygon(polygon.xpoints, polygon.ypoints, polygon.npoints);

           polygon.translate(-bounding.x, -bounding.y);
           graphics.fillPolygon(polygon);
       }
       if(!place.isWater())
       {
           if(img != null)
               graphics.drawImage(img, 0,0, new ImageIoCompletionWatcher());
       }


       return workImage;
    }

    public Color getPlayerColour(PlayerID owner)
    {
        Color newColor = (Color) m_playerColors.get(owner.getName());
        return newColor;
    }


    public Image getReliefImage(Territory place)
    {
        if(place.isWater())
            return null;
        if(!s_showReliefImages)
            return null;

        //is it in the cache?
        for(int i = 0; i < m_cachedTerritories.size(); i++)
        {
            ImageName current = (ImageName) m_cachedTerritories.get(i);
            if(current.name.equals(place.getName()))
            {
                //move it to the front of the cache
                m_cachedTerritories.remove(i);
                m_cachedTerritories.add(0, current);

                return current.image;
            }
        }

      String key = place.getName() + "_relief";
      // load it on the fly

      String fileName = Constants.MAP_DIR+s_mapDir+File.separator +"countries"+java.io.File.separator + key.replace(' ', '_')  + ".png";
      URL file = this.getClass().getResource(fileName);
      if(file == null)
          return null;
      Image baseImage = loadImageCompletely(file);

      //put it in the cache
      m_cachedTerritories.add(0, new ImageName(place.getName(), baseImage));
      while(m_cachedTerritories.size() > CACHE_SIZE)
      {
          m_cachedTerritories.remove(CACHE_SIZE);
      }


      // done!
      return baseImage;

    }



    public Image getSeaImage(Territory place)
    {
      if(!place.isWater())
          throw new IllegalArgumentException(place + " is not a sea zone");

      //is it in the cache?
      for(int i = 0; i < m_cachedTerritories.size(); i++)
      {
          ImageName current = (ImageName) m_cachedTerritories.get(i);
          if(current.name.equals(place.getName()))
          {
            //move it to the front of the cache
            m_cachedTerritories.remove(i);
            m_cachedTerritories.add(0, current);

            return current.image;
          }
      }
      String key = place.getName();
      // load it on the fly

      String fileName = Constants.MAP_DIR+s_mapDir+java.io.File.separator+
        "seazones"+java.io.File.separator+ key.replace(' ', '_')  + ".png";
      URL file = this.getClass().getResource(fileName);
      if(file == null)
          throw new IllegalArgumentException("not found:" + fileName);
      Image baseImage = loadImageCompletely(file);

      //put it in the cache
      m_cachedTerritories.add(0, new ImageName(place.getName(), baseImage));
      while(m_cachedTerritories.size() > CACHE_SIZE)
      {
          m_cachedTerritories.remove(CACHE_SIZE);
      }


      // done!
      return baseImage;

    }


    private Image loadImageCompletely(URL imageLocation)
    {

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


/**
  Inner Class ImageName
*/
class ImageName
{
    public ImageName(String name, Image image)
    {
        this.name  = name;
        this.image = image;
    }

    public final String name;
    public final Image image;

}//end inner class ImageName
