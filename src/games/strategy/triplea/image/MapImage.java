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

import java.util.Iterator;
import java.util.List;

import java.awt.*;
import java.awt.image.ImageObserver;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.attatchments.TerritoryAttatchment;
import games.strategy.triplea.ui.TerritoryData;
import games.strategy.util.NullImageObserver;

/**
 * Responsible for drawing countries on the map.
* Is not responsible for drawing things on top of the map, such as units, routes etc.
 */
public class MapImage
{

  private final static String LARGE_IMAGE_FILENAME = "images/maps/largeMap.gif";

  private static MapImage s_instance;
  private static final ImageObserver s_observer = new NullImageObserver();

  public static synchronized MapImage getInstance()
  {
    if(s_instance == null)
    {
      s_instance = new MapImage();
    }
    return s_instance;
  }

  private static Image loadImage(String name)
  {
    Image img =  Toolkit.getDefaultToolkit().createImage(MapImage.class.getResource(name));
    MediaTracker tracker = new MediaTracker( new Panel());
    tracker.addImage(img,1 );
    try
    {
      tracker.waitForAll();
      return img;
    } catch(InterruptedException ie)
    {
      ie.printStackTrace();
      return loadImage(name);
    }
  }

  private Image m_largeMapImage;
  private Image m_smallMapImage;
  private double m_smallLargeRatio = 15.0;
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
    initMaps(data);
  }

  public Image getLargeMapImage()
  {
    return m_largeMapImage;
  }

  private void loadMaps()
  {
    Image largeFromFile = loadImage(LARGE_IMAGE_FILENAME);

    //create from a component to make screen drawing faster
    //if you create an image from a component then no operations
    //have to be done when drawing the image to the screen, just a simple
    //byte copy
    Frame frame = new Frame();
    frame.addNotify();
    m_largeMapImage = frame.createImage(largeFromFile.getWidth(s_observer), largeFromFile.getHeight(s_observer));
    m_smallMapImage = frame.createImage( (int) (largeFromFile.getWidth(s_observer) / m_smallLargeRatio),
                                         (int) ( largeFromFile.getHeight(s_observer) / m_smallLargeRatio));

    frame.dispose();
    frame = null;

    m_largeMapImage.getGraphics().drawImage(largeFromFile, 0,0,s_observer);
    m_smallMapImage.getGraphics().drawImage(largeFromFile, 0,0, m_smallMapImage.getWidth(null), m_smallMapImage.getHeight(null), s_observer);

    largeFromFile = null;
    System.gc();

  }

  private void initMaps(GameData data)
  {
    m_smallLargeRatio = ((float) m_largeMapImage.getHeight(s_observer)) / ((float) m_smallMapImage.getHeight(s_observer));

    Iterator territories = data.getMap().iterator();
    while(territories.hasNext())
    {
      Territory current = (Territory) territories.next();
      PlayerID id = current.getOwner();

      if(!current.isWater())
        setOwnerInternal(current, id);
    }


    //draw the names
    territories = data.getMap().iterator();
    while(territories.hasNext())
    {
      Territory current = (Territory) territories.next();
      drawTerritoryName(current);
    }


  }

  public void setOwner(Territory territory, PlayerID id)
  {
      setOwnerInternal(territory, id);

      drawTerritoryName(territory);



  }

  private void setOwnerInternal(Territory territory, PlayerID id)
  {
    if(territory.isWater())
      return;


    Graphics largeGraphics = m_largeMapImage.getGraphics();
    largeGraphics.setColor(TerritoryImageFactory.getInstance().getPlayerColour(id));

    Graphics smallGraphics = m_smallMapImage.getGraphics();
    smallGraphics.setColor(TerritoryImageFactory.getInstance().getPlayerColour(id));

    List polys = TerritoryData.getInstance().getPolygons(territory);
    Iterator polyIter = polys.iterator();
    while (polyIter.hasNext())
    {
        Polygon item = (Polygon)polyIter.next();
        largeGraphics.fillPolygon(item);
        smallGraphics.fillPolygon(scale(item, m_smallLargeRatio ));
    }

    Rectangle dirty = TerritoryData.getInstance().getBoundingRect(territory);



    Image reliefImage = TerritoryImageFactory.getInstance().getReliefImage(territory);
    m_largeMapImage.getGraphics().drawImage(reliefImage,dirty.x, dirty.y, s_observer);

//    m_smallMapImage.getGraphics().drawImage(reliefImage,
//                                            (int)  (dirty.x / m_smallLargeRatio ),
//                                            (int)  (dirty.y / m_smallLargeRatio ),
//                                            (int)  (dirty.width / m_smallLargeRatio ),
//                                            (int)  (dirty.height / m_smallLargeRatio ),
//                                            s_observer
//                                            );


  }

  /**
   * Scale the polygon by scale.
   * returns a new polygon.
   */
  private static Polygon scale(Polygon p, double scale)
  {
      int[] xpoints = new int[p.npoints];
      int[] ypoints = new int[p.npoints];

      for(int i = 0; i < p.npoints; i++)
      {
          xpoints[i] = (int) (p.xpoints[i] / scale);
          ypoints[i] = (int) (p.ypoints[i] / scale);
      }

      return new Polygon(xpoints, ypoints, p.npoints);

  }


  private void drawTerritoryName(Territory territory)
  {
      if (territory.isWater())
          return;

      Graphics g = m_largeMapImage.getGraphics();
      Rectangle bounds = TerritoryData.getInstance().getBoundingRect(territory);
      g.setFont(MAP_FONT);

      TerritoryAttatchment ta = TerritoryAttatchment.get(territory);
      g.setColor(Color.black);
      FontMetrics fm = g.getFontMetrics();
      int x = bounds.x;
      int y = bounds.y;

      x += (int) bounds.getWidth() >> 1;
      y += (int) bounds.getHeight() >> 1;

      x -= fm.stringWidth(territory.getName()) >> 1;
      y += fm.getAscent() >> 1;

      g.drawString(territory.getName(), x, y);

      if (ta.getProduction() > 0)
      {
          String prod = new Integer(ta.getProduction()).toString();
          x = bounds.x + ( ( ( (int) bounds.getWidth()) - fm.stringWidth(prod)) >> 1);
          y += fm.getLeading() + fm.getAscent();
          g.drawString(prod, x, y);
      }
  }

}
