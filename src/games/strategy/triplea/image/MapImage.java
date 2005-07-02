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

import games.strategy.engine.data.GameData;
import games.strategy.triplea.Constants;
import games.strategy.ui.Util;

import java.awt.*;
import java.net.URL;

/**
 * Responsible for drawing countries on the map.
 * Is not responsible for drawing things on top of the map, such as units, routes etc.
 */
public class MapImage
{

  private static MapImage s_instance = new MapImage();
  
  public static synchronized MapImage getInstance()
  {
    return s_instance;
  }

  private static Image loadImage(String name)
  {
    URL mapFileUrl=MapImage.class.getResource(name);
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
      return loadImage(name);
    }
  }

  
  private Image m_smallMapImage;
  public static final Font MAP_FONT = new Font("Ariel", Font.BOLD, 12);

  /** Creates a new instance of CountryImage */
  public MapImage()
  {

  }

  public Image getSmallMapImage()
  {
    return m_smallMapImage;
  }

  public void loadMaps(GameData data)
  {
    loadMaps();
    
  }

  

  private void loadMaps()
  {
     Image smallFromFile =  loadImage(Constants.MAP_DIR+TileImageFactory.getMapDir()+java.io.File.separator+Constants.SMALL_MAP_FILENAME);
      
     m_smallMapImage = Util.createImage(smallFromFile.getWidth(null), smallFromFile.getHeight(null), false);
     m_smallMapImage.getGraphics().drawImage(smallFromFile, 0,0, null);
    
  }





}
