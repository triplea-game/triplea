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


import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.image.*;
import java.net.URL;

import games.strategy.util.*;
import games.strategy.ui.Util;
//import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.*;

public final class TerritoryImageFactory
{

  // one instance in the application
  private static TerritoryImageFactory s_singletonInstance = new
      TerritoryImageFactory();

  // data
  private HashMap m_playerColors = new HashMap();
  private HashMap m_baseTerritoryImages = new HashMap();
  private HashMap m_ownedTerritoryImages = new HashMap();
  private GraphicsConfiguration m_localGraphicSystem = null;
  private BufferedImage m_waterImage = null;

  // return the singleton
  public static TerritoryImageFactory getInstance()
  {
    return s_singletonInstance;
  }

  // returns the water image
  public BufferedImage getWaterImage()
  {
    return m_waterImage;
  }

  // returns an image of the desired territory with the desired owner
  public BufferedImage getTerritoryImage(Territory place, PlayerID owner)
  {

    BufferedImage terrImage;

    // lookup via a composite key
    String key = place.getName() + " owned by " + owner.getName();

    // look in the cache first
    if (m_ownedTerritoryImages.containsKey(key))
      terrImage = (BufferedImage) m_ownedTerritoryImages.get(key);
    else
    { // load it if not found
      terrImage = createTerritoryImage(place, owner);
      m_ownedTerritoryImages.put(key, terrImage);
    }

    return terrImage;

  }

  // constructor
  private TerritoryImageFactory()
  {

    // local graphic system is used to create compatible bitmaps
    m_localGraphicSystem = GraphicsEnvironment.getLocalGraphicsEnvironment()
        .getDefaultScreenDevice()
        .getDefaultConfiguration();

    // cache the player colors
    m_playerColors.put("British", new Color(153, 102, 0));
    m_playerColors.put("Americans", new Color(102, 102, 0));
    m_playerColors.put("Russians", new Color(153, 51, 0));
    m_playerColors.put("Germans", new Color(119, 119, 119));
    m_playerColors.put("Japanese", new Color(255, 153, 0));
    m_playerColors.put("no one", new Color(204, 153, 51));

    // the water image can be pre-loaded
    m_waterImage = createWaterImage();
  }

  // dynamically create a new territory image
  private BufferedImage createTerritoryImage(Territory place, PlayerID owner)
  {
    // get the base image and the color to apply
    Image baseImage = getBaseImage(place);
    Color newColor = (Color) m_playerColors.get(owner.getName());

    // Get the bounds. Note that the  source image should be completely loaded
    // before calling this method so its OK to use null observers.
    int width = baseImage.getWidth(null);
    int height = baseImage.getHeight(null);

    // Create a buffered image in the most optimal format, which allows a
    //    fast blit to the screen.
    BufferedImage workImage = m_localGraphicSystem.createCompatibleImage(width,
        height,
        Transparency.BITMASK);

    // fill in the workImage with the desired color
    Graphics2D gc = (Graphics2D) workImage.getGraphics();
    gc.setColor(newColor);
    gc.fillRect(0, 0, width, height);

    // setup our composite
    Composite prevComposite = gc.getComposite();
    gc.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_IN));

    // draw the image, and check for the possibility it doesn't complete now
    ImageIoCompletionWatcher watcher = new ImageIoCompletionWatcher();
    boolean drawComplete = gc.drawImage(baseImage, 0, 0, watcher);

    // use the watcher to for the draw to finish
    if (!drawComplete)
    {
      try
      {
        watcher.runUntilOpComplete();
        watcher.join();
      }
      catch (InterruptedException ie)
      {
        return null;
      }
    }

    // cleanup
    gc.setComposite(prevComposite);

    // done
    return workImage;

  }

  // dynamically a buffered version of the water image
  private BufferedImage createWaterImage()
  {

    // get the base image
    Image baseImage = loadImageCompletely(this.getClass().getResource(
        "countries/water.gif"));

    // Get the bounds. Note that the  source image should be completely loaded
    // before calling this method so its OK to use null observers.
    int width = baseImage.getWidth(null);
    int height = baseImage.getHeight(null);

    // Create a buffered image in the most optimal format, which allows a
    //    fast blit to the screen.
    BufferedImage workImage = m_localGraphicSystem.createCompatibleImage(width,
        height,
        Transparency.BITMASK);

    // draw the image, and check for the possibility it doesn't complete now
    ImageIoCompletionWatcher watcher = new ImageIoCompletionWatcher();
    boolean drawComplete = workImage.getGraphics().drawImage(baseImage, 0, 0,
        watcher);

    // use the watcher to for the draw to finish
    if (!drawComplete)
    {
      try
      {
        watcher.runUntilOpComplete();
        watcher.join();
      }
      catch (InterruptedException ie)
      {
        return null;
      }
    }

    // done
    return workImage;

  }

  // returns the base territory image that the others are derived from
  private Image getBaseImage(Territory place)
  {

    String key = place.getName();

    // look in the cache first
    if (m_baseTerritoryImages.containsKey(key))
      return (Image) m_baseTerritoryImages.get(key);

    // load it on the fly

    URL file = this.getClass().getResource("countries/"
                                           + key.replace(' ', '_')
                                           + ".gif");
    Image baseImage = loadImageCompletely(file);

    // cache for future use
    m_baseTerritoryImages.put(key, baseImage);

    // done!
    return baseImage;

  }

  // loads an image and blocks until complete
  private Image loadImageCompletely(URL imageLocation)
  {

    // name?
    int pathLen = imageLocation.getFile().lastIndexOf("/");
    String name = imageLocation.getFile().substring(pathLen + 1);

    // use the local toolkit to load the image
    Toolkit tk = Toolkit.getDefaultToolkit();
    Image img = tk.createImage(imageLocation);

    // force it to be loaded *now*
    ImageIoCompletionWatcher watcher = new ImageIoCompletionWatcher();
    boolean isLoaded = tk.prepareImage(img, -1, -1, watcher);

    // use the watcher to block while loading
    if (!isLoaded)
    {
      try
      {
        watcher.runUntilOpComplete();
        watcher.join();
      }
      catch (InterruptedException ie)
      {
        return null;
      }
    }

    // done!
    return img;

  }

}

class ImageIoCompletionWatcher extends Thread implements ImageObserver
{

  private boolean m_started = false;
  private boolean m_complete = false;

  public void runUntilOpComplete()
  {
    if (!m_started)
    {
      this.start();
      m_started = true;
    }
  }

  public boolean isComplete()
  {
    return m_complete;
  }

  public void run()
  {

    // just hang out until the draw is complete
    waitForDrawComplete();

  }

  public boolean imageUpdate(Image image, int flags, int x, int y, int width,
                             int height)
  {

    // wait for complete or error/abort
    if ( ( (flags & ALLBITS) != 0) || ( (flags & ABORT) != 0))
    {
      notifyDrawComplete();
      return false;
    }

    return true;

  }

  private synchronized void waitForDrawComplete()
  {

    if (!m_complete)
    {
      try
      {
        wait();
      }
      catch (InterruptedException ie)
      {}
    }
  }

  private synchronized void notifyDrawComplete()
  {
    m_complete = true;
    notifyAll();
  }

}