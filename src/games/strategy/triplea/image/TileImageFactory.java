package games.strategy.triplea.image;

/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;
import games.strategy.triplea.Constants;
import games.strategy.triplea.util.Stopwatch;
import games.strategy.ui.Util;

import java.awt.*;
import java.io.*;
import java.lang.ref.*;
import java.net.URL;
import java.util.*;
import java.util.logging.*;
import java.util.logging.Logger;
import java.util.prefs.*;

import javax.imageio.ImageIO;

public final class TileImageFactory
{
    //if this system property is set to true, we dont load any graphics
    //this allows for faster start times
    private static final boolean s_dontLoadImages = Boolean.valueOf(System.getProperties().getProperty("triplea.fastGraphics", "false")).booleanValue();
    private final Object m_mutex = new Object();

    // one instance in the application
    private static TileImageFactory s_singletonInstance = new TileImageFactory();
    private final static String SHOW_RELIEF_IMAGES_PREFERENCE = "ShowRelief";
    private static boolean s_showReliefImages = true;

    private static final Logger s_logger = Logger.getLogger(TileImageFactory.class.getName());
    
    //maps image name to ImageRef
    private HashMap m_imageCache = new HashMap();

    static
    {
        Preferences prefs = Preferences.userNodeForPackage(TileImageFactory.class);
        s_showReliefImages = prefs.getBoolean(SHOW_RELIEF_IMAGES_PREFERENCE, false);
        s_mapDir = "classic";//default to using the classic map files
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
        } catch (BackingStoreException ex)
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
        s_mapDir = dir;
        synchronized (getInstance().m_mutex)
        {
            //we manually want to clear each ref to allow the soft reference to
            // be removed
            Iterator values = getInstance().m_imageCache.values().iterator();
            while (values.hasNext())
            {
                ImageRef imageRef = (ImageRef) values.next();
                imageRef.clear();
            }
            getInstance().m_imageCache.clear();
        }
    }

    // return the singleton
    public static TileImageFactory getInstance()
    {
        return s_singletonInstance;
    }

    // constructor
    private TileImageFactory()
    {
    }

    /**
     * Take advantage of awt loading of images in another thread this starts the
     * loading of an image in a background thread calls to getImage will ensure
     * the image has finished loading
     */
    public void prepareReliefTile(int x, int y)
    {
        String fileName = getReliefTileImageName(x, y);
        //image is already loaded
        prepareImage(fileName, true);
    }

    private void prepareImage(String fileName, boolean transparent)
    {
        if(s_dontLoadImages)
            return;
        
        synchronized (m_mutex)
        {
            if (isImageLoaded(fileName) != null)
                return;
            URL url = this.getClass().getResource(fileName);
            if (url == null)
                return;
            startLoadingImage(url, fileName, transparent);
        }
    }

    /**
     * @param fileName
     * @return
     */
    private Image isImageLoaded(String fileName)
    {
        if (m_imageCache.get(fileName) == null)
            return null;
        return ((ImageRef) m_imageCache.get(fileName)).getImage();
    }

    /**
     * Take advantage of awt loading of images in another thread this starts the
     * loading of an image in a background thread calls to getImage will ensure
     * the image has finished loading
     */
    public void prepareBaseTile(int x, int y)
    {
        String fileName = getBaseTileImageName(x, y);
        //image is already loaded
        prepareImage(fileName, false);
    }

    public Image getBaseTile(int x, int y)
    {
        String fileName = getBaseTileImageName(x, y);
        return getImage(fileName, false);
    }

    /**
     * @param x
     * @param y
     * @return
     */
    private String getBaseTileImageName(int x, int y)
    {
        String fileName = Constants.MAP_DIR + s_mapDir + File.separator + "baseTiles" + java.io.File.separator + x + "_" + y + ".png";
        return fileName;
    }

    /**
     * @param fileName
     * @return
     */
    private Image getImage(String fileName, boolean transparent)
    {
        if(s_dontLoadImages)
            return null;
        
        synchronized (m_mutex)
        {
            Image rVal = isImageLoaded(fileName);
            if (rVal != null)
                return rVal;

            URL url = this.getClass().getResource(fileName);
            if (url == null)
                return null;

            startLoadingImage(url, fileName, transparent);
        }
        return getImage(fileName, transparent);
    }

    public Image getReliefTile(int x, int y)
    {
        String fileName = getReliefTileImageName(x, y);
        return getImage(fileName, true);
    }

    /**
     * @param x
     * @param y
     * @return
     */
    private String getReliefTileImageName(int x, int y)
    {
        String fileName = Constants.MAP_DIR + s_mapDir + File.separator + "reliefTiles" + java.io.File.separator + x + "_" + y + ".png";
        return fileName;
    }

    /**
     * @param imageLocation
     * @return
     */
    private void startLoadingImage(URL imageLocation, String fileName, boolean transparent)
    {
        synchronized (m_mutex)
        {
		    Image image;
		    try
		    {
		        Stopwatch loadingImages = new Stopwatch(s_logger, Level.FINEST, "Loading image:" + imageLocation);
		        Image fromFile = ImageIO.read(imageLocation);
		    
		        //using a copy reduces memory, a simpler memory format?
		        image = Util.createImage(fromFile.getWidth(null), fromFile.getHeight(null), transparent);
		        image.getGraphics().drawImage(fromFile, 0,0, null);
		        loadingImages.done();
		        
		    } catch (IOException e)
		    {
		        throw new IllegalStateException(e.getMessage());
		    }
		    
            ImageRef ref = new ImageRef(image);
            m_imageCache.put(fileName, ref);
        }
    }

}//end class TerritoryImageFactory

/**
 * We keep a soft reference to the image to allow it to be garbage collected.
 * 
 * Also, the image may not have finished watching when we are created, but the
 * getImage method ensures that the image will be loaded before returning.
 * 
 * @author Sean Bridges
 */

class ImageRef
{
    public static final ReferenceQueue s_referenceQueue = new ReferenceQueue();
    public static final Logger s_logger = Logger.getLogger(ImageRef.class.getName());
    
    private static final AtomicInteger s_imageCount = new AtomicInteger();
    
    static
    {
        Runnable r = new Runnable()
        {
            public void run()
            {
                while(true)
                {
                    try
                    {
                        s_referenceQueue.remove();
                        s_logger.finer("Removed soft reference image. Image count:" + s_imageCount.decrementAndGet() );
                    } catch (InterruptedException e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        };
        Thread t = new Thread(r, "Tile Image Factory Soft Reference Reclaimer");
        t.setDaemon(true);
        t.start();
        
    }
    
    
    private final Reference m_image;
    //private final Object m_hardRef;

    public ImageRef(final Image image)
    {
        m_image = new SoftReference(image, s_referenceQueue);
        //m_hardRef = image;
        s_logger.finer("Added soft reference image. Image count:" + s_imageCount.incrementAndGet() );
    }

    public Image getImage()
    {
        return (Image) m_image.get();
    }

    public void clear()
    {
        m_image.enqueue();
        m_image.clear();
    }
}


