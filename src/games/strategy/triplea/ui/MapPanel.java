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

/*
 * MapPanel.java
 * 
 * Created on November 5, 2001, 1:54 PM
 */

package games.strategy.triplea.ui;

import games.strategy.engine.data.*;
import games.strategy.engine.data.events.*;
import games.strategy.triplea.Constants;
import games.strategy.triplea.image.MapImage;
import games.strategy.triplea.ui.screen.*;
import games.strategy.ui.*;
import games.strategy.util.ListenerList;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.*;

/**
 * Responsible for drawing the large map and keeping it updated.
 * 
 * @author Sean Bridges
 */
public class MapPanel extends ImageScrollerLargeView
{

    private ListenerList m_mapSelectionListeners = new ListenerList();

    private GameData m_data;
    private Territory m_currentTerritory; //the territory that the mouse is
    // currently over
    //could be null
    private MapPanelSmallView m_smallView;

    private SmallMapImageManager m_smallMapImageManager;
    
    //keep a reference to the images from the last paint to
    //prevent them from being gcd
    private Set m_images = new HashSet();;
    
    private RouteDescription m_routeDescription;

    private TileManager m_tileManager = new TileManager();
    
    /** Creates new MapPanel */
    public MapPanel(Dimension imageDimensions, GameData data, MapPanelSmallView smallView) throws IOException
    {
        super(imageDimensions);
        
        setDoubleBuffered(false);
       

        m_smallView = smallView;
        m_smallMapImageManager = new SmallMapImageManager(smallView, MapImage.getInstance().getSmallMapImage(),  m_tileManager);
        
        setGameData(data);

        this.addMouseListener(MOUSE_LISTENER);
        this.addMouseMotionListener(MOUSE_MOTION_LISTENER);
        
        this.addScrollListener(new ScrollListener()
        {

            public void scrolled(int x, int y)
            {
                repaint();
            }
            
        });
        m_tileManager.createTiles(new Rectangle(imageDimensions), data, MapData.getInstance());
        m_tileManager.resetTiles(data, MapData.getInstance());
    } 
     
    

    // Beagle Code used to chnage map skin
    public void changeImage(Dimension newDimensions)
    {
 
       
        super.setDimensions(newDimensions);
        m_tileManager.createTiles(new Rectangle(newDimensions), m_data, MapData.getInstance());
        m_tileManager.resetTiles(m_data, MapData.getInstance());

        
    }

    public boolean isShowing(Territory territory)
    {

        Point territoryCenter = MapData.getInstance().getCenter(territory);

        Rectangle screenBounds = new Rectangle(super.getXOffset(), super.getYOffset(), super.getWidth(), super.getHeight());
        return screenBounds.contains(territoryCenter);

    }

    public void centerOn(Territory territory)
    {

        if (territory == null)
            return;

        Point p = MapData.getInstance().getCenter(territory);

        //when centering dont want the map to wrap around,
        //eg if centering on hawaii
        super.setTopLeft(p.x - (getWidth() / 2), p.y - (getHeight() / 2));
    }

    public void setRoute(Route route)
    {
        setRoute(route, null, null);
    }

    /**
     * Set the route, could be null.
     */
    public void setRoute(Route route, Point start, Point end)
    {
        if (route == null)
        {
            m_routeDescription = null;
            repaint();
            return;
        }
        RouteDescription newVal = new RouteDescription(route, start, end);
        if (m_routeDescription != null && m_routeDescription.equals(newVal))
        {
            return;
        }

        m_routeDescription = newVal;
        repaint();

    }

    public void addMapSelectionListener(MapSelectionListener listener)
    {

        m_mapSelectionListeners.add(listener);
    }

    public void removeMapSelectionListener(MapSelectionListener listener)
    {

        m_mapSelectionListeners.remove(listener);
    }

    private void notifyTerritorySelected(Territory t, MouseEvent me)
    {

        Iterator iter = m_mapSelectionListeners.iterator();

        while (iter.hasNext())
        {
            MapSelectionListener msl = (MapSelectionListener) iter.next();
            msl.territorySelected(t, me);
        }
    }

    private void notifyMouseMoved(Territory t, MouseEvent me)
    {

        Iterator iter = m_mapSelectionListeners.iterator();

        while (iter.hasNext())
        {
            MapSelectionListener msl = (MapSelectionListener) iter.next();
            msl.mouseMoved(t, me);
        }
    }

    private void notifyMouseEntered(Territory t)
    {

        Iterator iter = m_mapSelectionListeners.iterator();

        while (iter.hasNext())
        {
            MapSelectionListener msl = (MapSelectionListener) iter.next();
            msl.mouseEntered(t);
        }
    }

    private Territory getTerritory(int x, int y)
    {

        int imageWidth = (int) getImageDimensions().getWidth();
        if (x < 0)
            x += imageWidth;
        else if (x > imageWidth)
            x -= imageWidth;

        String name = MapData.getInstance().getTerritoryAt(x, y);
        if (name == null)
            return null;
        return m_data.getMap().getTerritory(name);
    }

    public void resetMap()
    {
       m_tileManager.resetTiles(m_data, MapData.getInstance());
       repaint();
    }
    
    
    

    private MouseListener MOUSE_LISTENER = new MouseAdapter()
    {

        public void mouseReleased(MouseEvent e)
        {

            Territory terr = getTerritory(e.getX() + getXOffset(), e.getY() + getYOffset());
            if (terr != null)
                notifyTerritorySelected(terr, e);
        }

        public void mouseExit(MouseEvent e)
        {

            if (m_currentTerritory != null)
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
            if (terr != m_currentTerritory)
            {
                m_currentTerritory = terr;
                notifyMouseEntered(terr);
            }

            notifyMouseMoved(terr, e);
        }
    };

    public void updateCounties(Collection countries)
    {
        m_tileManager.updateTerritories(countries, m_data, MapData.getInstance());
        m_smallMapImageManager.update(m_data, MapData.getInstance());
        m_smallView.repaint();
        repaint();
    }

    public void setGameData(GameData data)
    {

        //clean up any old listeners
        if (m_data != null)
        {
            m_data.removeTerritoryListener(TERRITORY_LISTENER);
            m_data.removeDataChangeListener(TECH_UPDATE_LISTENER);
        }

        m_data = data;
        m_data.addTerritoryListener(TERRITORY_LISTENER);
        
        m_data.addDataChangeListener(TECH_UPDATE_LISTENER);
        m_data.addDataChangeListener(UNITS_HIT_LISTENER);
        
        m_tileManager.resetTiles(m_data, MapData.getInstance());

    }

    private final TerritoryListener TERRITORY_LISTENER = new TerritoryListener()
    {

        public void unitsChanged(Territory territory)
        {
            updateCounties(Collections.singleton(territory));
            repaint();
        }

        public void ownerChanged(Territory territory)
        {
            m_smallMapImageManager.updateTerritoryOwner(territory, m_data, MapData.getInstance());
            updateCounties(Collections.singleton(territory));
            repaint();
        }
    };

    /**
     * We want to redraw the map where units were hit
     */
    private final GameDataChangeListener UNITS_HIT_LISTENER = new GameDataChangeListener()
    {

        public void gameDataChanged(Change aChange)
        {
            if (!(aChange instanceof UnitHitsChange))
                return;

            m_tileManager.resetTiles(m_data, MapData.getInstance());
        }
    };


    private final GameDataChangeListener TECH_UPDATE_LISTENER = new GameDataChangeListener()
    {

        public void gameDataChanged(Change aChange)
        {

            //find the players with tech changes
            Set playersWithTechChange = new HashSet();
            getPlayersWithTechChanges(aChange, playersWithTechChange);

            if (playersWithTechChange.isEmpty())
                return;

            m_tileManager.resetTiles(m_data, MapData.getInstance());
            repaint();
        }

       
        private void getPlayersWithTechChanges(Change aChange, Set players)
        {

            if (aChange instanceof CompositeChange)
            {
                CompositeChange composite = (CompositeChange) aChange;
                Iterator iter = composite.getChanges().iterator();
                while (iter.hasNext())
                {
                    Change item = (Change) iter.next();
                    getPlayersWithTechChanges(item, players);

                }
            } else
            {
                if (aChange instanceof ChangeAttatchmentChange)
                {
                    ChangeAttatchmentChange changeAttatchment = (ChangeAttatchmentChange) aChange;
                    if (changeAttatchment.getAttatchmentName().equals(Constants.TECH_ATTATCHMENT_NAME))
                    {
                        players.add((PlayerID) changeAttatchment.getAttatchedTo());
                    }
                }

            }
        }

    };

        
    public void paint(Graphics g)
    {
        super.paint(g);
        Set images = new HashSet();

        //handle wrapping off the screen to the left
        if(m_x < 0)
        {
            Rectangle leftBounds = new Rectangle(m_dimensions.width + m_x, m_y, -m_x, getHeight());
            Iterator tiles = m_tileManager.getTiles(leftBounds).iterator();
            
            while (tiles.hasNext())
            {
                Tile tile = (Tile) tiles.next();
                Image img = tile.getImage(m_data, MapData.getInstance());
                g.drawImage(img, tile.getBounds().x -leftBounds.x, tile.getBounds().y - m_y, this);
                images.add(tile);
            }
        }
        
        Rectangle mainBounds = new Rectangle(m_x, m_y, getWidth(), getHeight());
        Iterator tiles = m_tileManager.getTiles(mainBounds).iterator();
        
        while (tiles.hasNext())
        {
            Tile tile = (Tile) tiles.next();
            Image img = tile.getImage(m_data, MapData.getInstance());
            g.drawImage(img, tile.getBounds().x - m_x, tile.getBounds().y - m_y, this);
            images.add(tile);
        }

        double leftOverlap = m_x + getWidth() - m_dimensions.getWidth();
        //handle wrapping off the screen to the left
        if(leftOverlap > 0)
        {
            Rectangle rightBounds = new Rectangle(0 , m_y, (int) leftOverlap, getHeight());
            tiles = m_tileManager.getTiles(rightBounds).iterator();
            rightBounds.translate((int) (leftOverlap - getWidth()), 0);
            
            while (tiles.hasNext())
            {
                Tile tile = (Tile) tiles.next();
                Image img = tile.getImage(m_data, MapData.getInstance());
                g.drawImage(img, tile.getBounds().x -rightBounds.x, tile.getBounds().y - m_y, this);
                images.add(tile);
            }
        }

        
        MapRouteDrawer.drawRoute((Graphics2D) g, m_routeDescription, this);
        
        m_images.clear();
        m_images.addAll(images);
    }

    
    public Image getTerritoryImage(Territory territory)
    {
        return m_tileManager.createTerritoryImage(territory, m_data,MapData.getInstance());
    }



    /**
     * 
     */
    public void initSmallMap()
    {
        Iterator territories = m_data.getMap().getTerritories().iterator();
        
        while (territories.hasNext())
        {
            Territory territory = (Territory) territories.next();
            m_smallMapImageManager.updateTerritoryOwner(territory, m_data, MapData.getInstance());
            
        }
        
        m_smallMapImageManager.update(m_data, MapData.getInstance());
        
    }  
}

class RouteDescription
{

    private final Route m_route;
    private final Point m_start;
    private final Point m_end;

    public RouteDescription(Route route, Point start, Point end)
    {
        m_route = route;
        m_start = start;
        m_end = end;
    }

    public boolean equals(Object o)
    {
        if (o == null)
            return false;
        if (o == this)
            return true;
        RouteDescription other = (RouteDescription) o;

        if (m_start == null && other.m_start != null || other.m_start == null && m_start != null
                || (m_start != other.m_start && !m_start.equals(other.m_start)))
            return false;

        if (m_route == null && other.m_route != null || other.m_route == null && m_route != null
                || (m_route != other.m_route && !m_route.equals(other.m_route)))
            return false;

        if (m_end == null && other.m_end != null || other.m_end == null && m_end != null)
            return false;

        //we dont want to be updating for every small change,
        //if the end points are close enough, they are close enough
        if (other.m_end == null && this.m_end != null)
            return false;
        if (other.m_end != null && this.m_end == null)
            return false;

        int xDiff = m_end.x - other.m_end.x;
        xDiff *= xDiff;
        int yDiff = m_end.y - other.m_end.y;
        yDiff *= yDiff;
        int endDiff = (int) Math.sqrt(xDiff + yDiff);

        return endDiff < 6;

    }

    public Route getRoute()
    {
        return m_route;
    }

    public Point getStart()
    {
        return m_start;
    }

    public Point getEnd()
    {
        return m_end;
    }

    
    

}