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
 * MapPanel.java
 *
 * Created on November 5, 2001, 1:54 PM
 */

package games.strategy.triplea.ui;

import java.io.IOException;
import java.util.ArrayList;

import java.awt.*;
import java.awt.event.*;

import games.strategy.engine.data.*;
import games.strategy.engine.data.events.TerritoryListener;
import games.strategy.ui.ImageScrollerLargeView;
import java.util.*;

/**
 * Responsible for drawing the large map and keeping it updated.
 *
 * @author  Sean Bridges
 */
public class MapPanel extends ImageScrollerLargeView
{

  private java.util.List m_mapSelectionListeners = new ArrayList();

  private GameData m_data;
  private Territory m_currentTerritory; //the territory that the mouse is currently over
                                        //could be null
  private MapPanelSmallView m_smallView;

  //current route we are displaying, could be null
  private Route m_route;

  private final MapUnitsDrawer m_mapsUnitDrawer;


  /** Creates new MapPanel */
  public MapPanel(Image image, GameData data, MapPanelSmallView smallView) throws
      IOException
  {
      super(image);
      m_smallView = smallView;
      m_mapsUnitDrawer = new MapUnitsDrawer(m_data, m_smallView, this);
      setGameData(data);
      initTerritories();

      this.addMouseListener(MOUSE_LISTENER);
      this.addMouseMotionListener(MOUSE_MOTION_LISTENER);

  }


  public void centerOn(Territory territory)
  {
    if(territory == null)
      return;

    Point p =TerritoryData.getInstance().getCenter(territory);

    //when centering dont want the map to wrap around,
    //eg if centering on hawaii
    super.setTopLeft(p.x - (getWidth()/2), p.y- (getHeight()/2));
  }

  /**
   * Set the route, could be null.
   */
  public void setRoute(Route route)
  {
    if(m_route != route || (m_route != null && !m_route.equals(route)))
    {
      m_route = route;
      //get the territory to update
      m_mapsUnitDrawer.queueUpdate();

    }
  }


  public void addMapSelectionListener(MapSelectionListener listener)
  {
    m_mapSelectionListeners.add(listener);
  }

  public void removeMapSelectionListener(MapSelectionListener listener)
  {
    m_mapSelectionListeners.remove(listener);
  }

  private void notifyTerritorySelected(Territory t,MouseEvent me)
  {
    java.util.List listeners = new ArrayList(m_mapSelectionListeners.size());
    listeners.addAll(m_mapSelectionListeners);

    for(int i = 0; i < listeners.size(); i++)
    {
      MapSelectionListener msl = (MapSelectionListener) listeners.get(i);
      msl.territorySelected(t, me);
    }
  }

  private void notifyMouseEntered(Territory t)
  {
    java.util.List listeners = new ArrayList(m_mapSelectionListeners.size());
    listeners.addAll(m_mapSelectionListeners);

    for(int i = 0; i < listeners.size(); i++)
    {
      MapSelectionListener msl = (MapSelectionListener) listeners.get(i);
      msl.mouseEntered(t);
    }
  }



  private Territory getTerritory(int x, int y)
  {
    int imageWidth = (int) getImageDimensions().getWidth();
    if(x < 0)
      x+= imageWidth;
    else if(x > imageWidth)
      x-= imageWidth;

      String name = TerritoryData.getInstance().getTerritoryAt(x,y);
      if(name == null)
          return null;
      return m_data.getMap().getTerritory(name);
  }

  public void paint(Graphics g)
  {
    synchronized(m_mapsUnitDrawer.getLock())
    {
      super.paint(g);
      MapRouteDrawer.drawRoute( (Graphics2D) g, m_route, this);
    }
  }

  private void initTerritories()
  {
    m_mapsUnitDrawer.queueUpdate(m_data.getMap().getTerritories());
    m_mapsUnitDrawer.waitForUpdates();
  }



  private MouseListener MOUSE_LISTENER = new MouseAdapter()
  {
    public void mouseReleased(MouseEvent e)
    {
      Territory terr = getTerritory(e.getX() + getXOffset(), e.getY() + getYOffset());
      if(terr != null)
        notifyTerritorySelected(terr, e);
    }

    public void mouseExit(MouseEvent e)
    {
      if(m_currentTerritory != null)
      {
        m_currentTerritory = null;
        notifyMouseEntered(null);
      }
    }


  };

  private final MouseMotionListener MOUSE_MOTION_LISTENER = new MouseMotionAdapter()
  {
    public void mouseMoved(MouseEvent e)
    {
      Territory terr = getTerritory(e.getX() + getXOffset(), e.getY() + getYOffset());
      //we can use == here since they will be the same object.
      //dont use .equals since we have nulls
      if(terr != m_currentTerritory)
      {
        m_currentTerritory = terr;
        notifyMouseEntered(terr);
      }
    }
  };

  public void updateCounties(Collection countries)
  {
      m_mapsUnitDrawer.queueUpdate(countries);
  }

  public void setGameData(GameData data)
  {
      //clean up any old listeners
      if(m_data != null)
      {
          m_data.removeTerritoryListener(TERRITORY_LISTENER);
      }

      m_data = data;
      m_data.addTerritoryListener(TERRITORY_LISTENER);
      m_mapsUnitDrawer.setData(m_data);


  }

  private final TerritoryListener TERRITORY_LISTENER = new TerritoryListener()
  {
    public void unitsChanged(Territory territory)
    {
        m_mapsUnitDrawer.queueUpdate(territory);
    }

    public void ownerChanged(Territory territory)
    {
        m_mapsUnitDrawer.queueUpdate(territory);
    }
  };



}
