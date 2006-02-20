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
 * CountryImage.java
 *
 * Created on January 8, 2002, 9:15 PM
 */

package games.strategy.triplea.image;

import games.strategy.triplea.*;
import games.strategy.ui.Util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;

/**
 * Responsible for drawing countries on the map.
 * Is not responsible for drawing things on top of the map, such as units, routes etc.
 */
public class MapImage
{

  private static Image loadImage(ResourceLoader loader, String name)
  {
    URL mapFileUrl= loader.getResource(name);
    
    if(mapFileUrl == null)
        throw new IllegalStateException("resource not found:" + name);
    Image img =  Toolkit.getDefaultToolkit().createImage(mapFileUrl);
    
    MediaTracker tracker = new MediaTracker( new Panel());
    tracker.addImage(img, 1 );
    try
    {
      tracker.waitForAll();
      if(tracker.isErrorAny())
      	throw new IllegalStateException("Error loading");
      return img;
    }
    catch(InterruptedException ie)
    {
      ie.printStackTrace();
      return loadImage(loader, name);
    }
  }

  
  private BufferedImage m_smallMapImage;
  public static final Font MAP_FONT = new Font("Ariel", Font.BOLD, 12);

  /** Creates a new instance of CountryImage */
  public MapImage()
  {

  }

  public BufferedImage getSmallMapImage()
  {
    return m_smallMapImage;
  }

  public void loadMaps(ResourceLoader loader)
  {
     Image smallFromFile =  loadImage(loader, Constants.SMALL_MAP_FILENAME);
      
     m_smallMapImage = Util.createImage(smallFromFile.getWidth(null), smallFromFile.getHeight(null), false);
     Graphics g = m_smallMapImage.getGraphics();
     g.drawImage(smallFromFile, 0,0, null);
     g.dispose();
     
     smallFromFile.flush();
    
  }





}
