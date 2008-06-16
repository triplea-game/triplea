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

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeAttachmentChange;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.events.GameDataChangeListener;
import games.strategy.engine.data.events.TerritoryListener;
import games.strategy.thread.LockUtil;
import games.strategy.triplea.Constants;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.triplea.ui.screen.SmallMapImageManager;
import games.strategy.triplea.ui.screen.Tile;
import games.strategy.triplea.ui.screen.TileManager;
import games.strategy.triplea.ui.screen.UnitsDrawer;
import games.strategy.triplea.util.Stopwatch;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.ui.ImageScrollModel;
import games.strategy.ui.ImageScrollerLargeView;
import games.strategy.ui.ScrollListener;
import games.strategy.ui.Util;
import games.strategy.util.ListenerList;
import games.strategy.util.Tuple;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

/**
 * Responsible for drawing the large map and keeping it updated.
 * 
 * @author Sean Bridges
 */
public class MapPanel extends ImageScrollerLargeView
{
    private static Logger s_logger = Logger.getLogger(MapPanel.class.getName());
    
    private ListenerList<MapSelectionListener> m_mapSelectionListeners = new ListenerList<MapSelectionListener>();
    private ListenerList<UnitSelectionListener> m_unitSelectionListeners = new ListenerList<UnitSelectionListener>();
    private ListenerList<MouseOverUnitListener> m_mouseOverUnitsListeners = new ListenerList<MouseOverUnitListener>();    

    private GameData m_data;
    private Territory m_currentTerritory; //the territory that the mouse is
    // currently over
    //could be null
    private MapPanelSmallView m_smallView;
    
    //units the mouse is currently over
    private Tuple<Territory, List<Unit>> m_currentUnits;

    private SmallMapImageManager m_smallMapImageManager;
    
    //keep a reference to the images from the last paint to
    //prevent them from being gcd
    private List<Object> m_images = new ArrayList<Object>();
    
    private RouteDescription m_routeDescription;

    private final TileManager m_tileManager;
    
    private final BackgroundDrawer m_backgroundDrawer;
    
    private BufferedImage m_mouseShadowImage = null;
    private final UIContext m_uiContext;
    

    private final LinkedBlockingQueue<Tile> m_undrawnTiles = new LinkedBlockingQueue<Tile>();
    
    private List<Unit> m_highlightUnits;

    private Cursor m_hiddenCursor = null;
    
    /** Creates new MapPanel */
    public MapPanel(GameData data, MapPanelSmallView smallView, UIContext uiContext, ImageScrollModel model) throws IOException
    {
        super(uiContext.getMapData().getMapDimensions(), model);

        m_uiContext = uiContext;
        m_scale = uiContext.getScale();
        m_backgroundDrawer = new BackgroundDrawer(this);
        
        m_tileManager = new TileManager(m_uiContext);
        
        
        Thread t = new Thread(m_backgroundDrawer, "Map panel background drawer");
        t.setDaemon(true);
        t.start();
        
        setDoubleBuffered(false);
       

        m_smallView = smallView;
        m_smallMapImageManager = new SmallMapImageManager(smallView, m_uiContext.getMapImage().getSmallMapImage(),  m_tileManager);
        
        setGameData(data);

        this.addMouseListener(MOUSE_LISTENER);
        this.addMouseMotionListener(MOUSE_MOTION_LISTENER);
        
        this.addScrollListener(new ScrollListener()
        {

            public void scrolled(int x, int y)
            {
                SwingUtilities.invokeLater(new Runnable(){
                
                    public void run()
                    {
                        repaint();
                    }
                
                });
                
            }
            
        });
        recreateTiles(data, uiContext);
        
        m_uiContext.addActive(new Active()
        {
        
            public void deactivate()
            {
                //super.deactivate
                MapPanel.this.deactivate();
                clearUndrawn();
                m_backgroundDrawer.stop();
            }
        
        });
        
        
       
    }
    
    LinkedBlockingQueue<Tile> getUndrawnTiles()
    {
        return m_undrawnTiles;
    }

    private void recreateTiles(GameData data, UIContext uiContext)
    {
        m_tileManager.createTiles(new Rectangle(m_uiContext.getMapData().getMapDimensions()), data, m_uiContext.getMapData());
        m_tileManager.resetTiles(data, uiContext.getMapData());
    } 
     
    GameData getData()
    {
        return m_data;
    }

    // Beagle Code used to chnage map skin
    public void changeImage(Dimension newDimensions)
    {
        m_model.setMaxBounds((int) newDimensions.getWidth(), (int) newDimensions.getHeight());
        m_tileManager.createTiles(new Rectangle(newDimensions), m_data, m_uiContext.getMapData());
        m_tileManager.resetTiles(m_data, m_uiContext.getMapData());
    }
    
    

    @Override
    public Dimension getPreferredSize()
    {
       return getImageDimensions();
    }


    @Override
    public Dimension getMinimumSize()
    {
       return new Dimension(200,200);
    }

    public boolean isShowing(Territory territory)
    {

        Point territoryCenter = m_uiContext.getMapData().getCenter(territory);

        Rectangle2D screenBounds = new Rectangle2D.Double(super.getXOffset(), super.getYOffset(), super.getScaledWidth(), super.getScaledHeight());
        return screenBounds.contains(territoryCenter);

    }

    /**
     * the units must all be in the same stack on the map, and exist in the given territory.
     * call with an null args
     */
    public void setUnitHighlight(List<Unit> units, Territory territory)
    {
        m_highlightUnits =units;
        SwingUtilities.invokeLater(new Runnable()
        {
        
            public void run()
            {
                repaint();
            }
        
        });
        
    }
    
    public void centerOn(Territory territory)
    {
        if (territory == null)
            return;

        Point p = m_uiContext.getMapData().getCenter(territory);

        //when centering dont want the map to wrap around,
        //eg if centering on hawaii
        super.setTopLeft((int) ( p.x - (getScaledWidth() / 2)), (int) ( p.y - (getScaledHeight() / 2)));
    }

    public void setRoute(Route route)
    {
        setRoute(route, null, null, null);
    }

    /**
     * Set the route, could be null.
     */
    public void setRoute(Route route, Point start, Point end, Image cursorImage)
    {
        if (route == null)
        {
            m_routeDescription = null;
            SwingUtilities.invokeLater(new Runnable()
            {
            
                public void run()
                {
                    repaint();
                }
            
            });
            
            return;
        }
        RouteDescription newVal = new RouteDescription(route, start, end, cursorImage);
        if (m_routeDescription != null && m_routeDescription.equals(newVal))
        {
            return;
        }

        m_routeDescription = newVal;
        SwingUtilities.invokeLater(new Runnable()
        {
        
            public void run()
            {
                repaint();
            }
        
        });

    }

    public void addMapSelectionListener(MapSelectionListener listener)
    {
        m_mapSelectionListeners.add(listener);
    }

    public void removeMapSelectionListener(MapSelectionListener listener)
    {
        m_mapSelectionListeners.remove(listener);
    }

    public void addMouseOverUnitListener(MouseOverUnitListener listener)
    {
        m_mouseOverUnitsListeners.add(listener);
    }

    public void removeMouseOverUnitListener(MouseOverUnitListener listener)
    {
        m_mouseOverUnitsListeners.remove(listener);
    }
    
    
    private void notifyTerritorySelected(Territory t, MouseDetails me)
    {

        Iterator<MapSelectionListener> iter = m_mapSelectionListeners.iterator();

        while (iter.hasNext())
        {
            MapSelectionListener msl = iter.next();
            msl.territorySelected(t, me);
        }
    }

    private void notifyMouseMoved(Territory t, MouseDetails me)
    {

        Iterator<MapSelectionListener> iter = m_mapSelectionListeners.iterator();

        while (iter.hasNext())
        {
            MapSelectionListener msl = iter.next();
            msl.mouseMoved(t, me);
        }
    }

    private void notifyMouseEntered(Territory t)
    {

        Iterator<MapSelectionListener> iter = m_mapSelectionListeners.iterator();

        while (iter.hasNext())
        {
            MapSelectionListener msl = iter.next();
            msl.mouseEntered(t);
        }
    }

    
    public void addUnitSelectionListener(UnitSelectionListener listener)
    {
        m_unitSelectionListeners.add(listener);
    }
    
    public void removeUnitSelectionListener(UnitSelectionListener listener)
    {
        m_unitSelectionListeners.remove(listener);
    }

    private void notifyUnitSelected(List<Unit> units, Territory t, MouseDetails me)
    {
        for(UnitSelectionListener listener : m_unitSelectionListeners)
        {
            listener.unitsSelected(units,t, me);
        }
    }
    
    private void notifyMouseEnterUnit(List<Unit> units, Territory t, MouseDetails me)
    {
        for(MouseOverUnitListener listener : m_mouseOverUnitsListeners)
        {
            listener.mouseEnter(units,t, me);
        }
    }
    
    
    private Territory getTerritory(double x, double y)
    {
        String name = m_uiContext.getMapData().getTerritoryAt(normalizeX(x), y);
        if (name == null)
            return null;
        return m_data.getMap().getTerritory(name);
    }

    private double normalizeX(double x)
    {
        if(!m_uiContext.getMapData().scrollWrapX())
            return x;
        
        int imageWidth = (int) getImageDimensions().getWidth();
        if (x < 0)
            x += imageWidth;
        else if (x > imageWidth)
            x -= imageWidth;
        return x;
    }

    public void resetMap()
    {
        
       m_tileManager.resetTiles(m_data, m_uiContext.getMapData());
       
       SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                repaint();
            }
        });
       
       m_smallMapImageManager.update(m_data, m_uiContext.getMapData());
       
    }

    private MouseListener MOUSE_LISTENER = new MouseAdapter()
    {
        
        
        /**
         * Invoked when the mouse exits a component.
         */
        public void mouseExited(MouseEvent e)
        {
            if(unitsChanged(null))
            {
                MouseDetails md = convert(e);
                m_currentUnits = null;
                notifyMouseEnterUnit(Collections.<Unit>emptyList(), getTerritory(e.getX(),e.getY()), md );
            }
        }

        @Override
        //this can't be mouseClicked, since 
        //a lot of people complain that clicking doesn't work 
        //well
        public void mouseReleased(MouseEvent e)
        {
            
            MouseDetails md = convert(e);
            double scaledMouseX = e.getX() / m_scale;
            double scaledMouseY = e.getY() / m_scale;
            
            double x = normalizeX(scaledMouseX + getXOffset());
            double y = scaledMouseY + getYOffset();            
            
            Territory terr = getTerritory(x,y);
            if (terr != null)
                notifyTerritorySelected(terr, md);
            
            if(!m_unitSelectionListeners.isEmpty())
            {
                Tuple<Territory, List<Unit>> tuple = m_tileManager.getUnitsAtPoint(x,y, m_data);
                
                if(tuple == null)
                    tuple = new Tuple<Territory, List<Unit>>(getTerritory(x,y), new ArrayList<Unit>(0) );
                
                notifyUnitSelected(tuple.getSecond(), tuple.getFirst(), md );
            }

            
        }

    };
    
    private MouseDetails convert(MouseEvent me)
    {
        double scaledMouseX = me.getX() / m_scale;
        double scaledMouseY = me.getY() / m_scale;
        
        double x = normalizeX(scaledMouseX + getXOffset());
        double y = scaledMouseY + getYOffset();    
        
        return new MouseDetails(me, x,y);
    }
    

    private final MouseMotionListener MOUSE_MOTION_LISTENER = new MouseMotionAdapter()
    {

        public void mouseMoved(MouseEvent e)
        {
            MouseDetails md = convert(e);

            double scaledMouseX = e.getX() / m_scale;
            double scaledMouseY = e.getY() / m_scale;
            
            double x = normalizeX(scaledMouseX + getXOffset());
            double y = scaledMouseY + getYOffset();            

            
            Territory terr = getTerritory(x,y);
            //we can use == here since they will be the same object.
            //dont use .equals since we have nulls
            if (terr != m_currentTerritory)
            {
                m_currentTerritory = terr;
                notifyMouseEntered(terr);
            }
            notifyMouseMoved(terr, md);

                        
            Tuple<Territory, List<Unit>> tuple = m_tileManager.getUnitsAtPoint(x,y, m_data);
            
            if(unitsChanged(tuple))
            {
                m_currentUnits = tuple;
                if(tuple == null)
                    notifyMouseEnterUnit(Collections.<Unit>emptyList(), getTerritory(x,y), md );
                else
                    notifyMouseEnterUnit(tuple.getSecond(), tuple.getFirst(), md );
            }
                
            
            
        }
    };
    
    private boolean unitsChanged( Tuple<Territory, List<Unit>> newUnits)
    {
        //both are null
        if(newUnits == m_currentUnits)
            return false;
        
        //one is null
        if(newUnits == null || m_currentUnits == null)
            return true;

        
        if(!newUnits.getFirst().equals(m_currentUnits.getFirst()))
            return true;
        
        return !games.strategy.util.Util.equals(newUnits.getSecond(), m_currentUnits.getSecond());
        
    }
    

    public void updateCountries(Collection<Territory> countries)
    {
        m_tileManager.updateTerritories(countries, m_data, m_uiContext.getMapData());
        m_smallMapImageManager.update(m_data, m_uiContext.getMapData());
        SwingUtilities.invokeLater(new Runnable()
        {
        
            public void run()
            {
                m_smallView.repaint();
                repaint();        
            }
        
        });
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
        
        clearUndrawn();
        
        m_tileManager.resetTiles(m_data, m_uiContext.getMapData());

    }

    private final TerritoryListener TERRITORY_LISTENER = new TerritoryListener()
    {

        public void unitsChanged(Territory territory)
        {
            updateCountries(Collections.singleton(territory));
            SwingUtilities.invokeLater(new Runnable()
            {
            
                public void run()
                {
                    repaint();
                }
            
            });
            
        }

        public void ownerChanged(Territory territory)
        {
            m_smallMapImageManager.updateTerritoryOwner(territory, m_data, m_uiContext.getMapData());
            updateCountries(Collections.singleton(territory));
            SwingUtilities.invokeLater(new Runnable()
            {
            
                public void run()
                {
                    repaint();
                }
            
            });
        }
    };


    private final GameDataChangeListener TECH_UPDATE_LISTENER = new GameDataChangeListener()
    {

        public void gameDataChanged(Change aChange)
        {

            //find the players with tech changes
            Set<PlayerID> playersWithTechChange = new HashSet<PlayerID>();
            getPlayersWithTechChanges(aChange, playersWithTechChange);

            if (playersWithTechChange.isEmpty())
                return;

            m_tileManager.resetTiles(m_data, m_uiContext.getMapData());
            SwingUtilities.invokeLater(new Runnable()
            {
            
                public void run()
                {
                    repaint();
                }
            
            });
         
        }

       
        private void getPlayersWithTechChanges(Change aChange, Set<PlayerID> players)
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
                if (aChange instanceof ChangeAttachmentChange)
                {
                    ChangeAttachmentChange changeAttatchment = (ChangeAttachmentChange) aChange;
                    if (changeAttatchment.getAttatchmentName().equals(Constants.TECH_ATTATCHMENT_NAME))
                    {
                        players.add((PlayerID) changeAttatchment.getAttatchedTo());
                    }
                }

            }
        }

    };

    // this one is useful for screenshots
    public void print(Graphics g)
    {
        Graphics2D g2d = (Graphics2D) g;
        super.print(g2d);
        //make sure we use the same data for the entire print
        final GameData gameData = m_data;

        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, getImageWidth(), getImageHeight());

        Collection<Tile> tileList = m_tileManager.getTiles(bounds);
        Iterator<Tile> tilesIter = tileList.iterator();
        while (tilesIter.hasNext())
        {
            Tile tile = tilesIter.next();
            LockUtil.acquireLock(tile.getLock());
            try
            {
                Image img = tile.getImage(gameData, m_uiContext.getMapData());
                if(img != null)
                {
                    AffineTransform t = new AffineTransform();
                    t.translate((tile.getBounds().x -bounds.getX()) * m_scale, (tile.getBounds().y - m_model.getY()) * m_scale);
                    g2d.drawImage(img, t, this);
                }
            }
            finally 
            {
                LockUtil.releaseLock(tile.getLock());
            }
        }
    }
        
    public void paint(Graphics g)
    {
        Graphics2D g2d = (Graphics2D) g;
        super.paint(g2d);
        g2d.clip(new Rectangle2D.Double(0,0, (getImageWidth() * m_scale), ( getImageHeight() * m_scale)));
        
        int x = m_model.getX();
        int y = m_model.getY();
        
        List<Tile> images = new ArrayList<Tile>();
        List<Tile> undrawnTiles = new ArrayList<Tile>();
        
        Stopwatch stopWatch = new Stopwatch(s_logger, Level.FINER, "Paint");

        //make sure we use the same data for the entire paint
        final GameData data = m_data;
        
        //if the map fits on screen, dont draw any overlap
        boolean mapFitsOnScreen = mapFitsOnScreen();
        
        //handle wrapping off the screen to the left
        if(!mapFitsOnScreen && x < 0  && m_uiContext.getMapData().scrollWrapX())
        {
            Rectangle2D.Double leftBounds = new Rectangle2D.Double(m_model.getMaxWidth() + x, y, -x, getScaledHeight());
            drawTiles(g2d, images, data, leftBounds,0, undrawnTiles);
        }
        
        //handle the non overlap
        Rectangle2D.Double mainBounds = new Rectangle2D.Double(x, y, getScaledWidth(), getScaledHeight());
        drawTiles(g2d, images, data, mainBounds,0, undrawnTiles);
        
        double leftOverlap = x + getScaledWidth() - m_model.getMaxWidth();
        //handle wrapping off the screen to the right
        if(!mapFitsOnScreen && leftOverlap > 0 && m_uiContext.getMapData().scrollWrapX())
        {
            Rectangle2D.Double rightBounds = new Rectangle2D.Double(0 , y, leftOverlap, getScaledHeight());
            drawTiles(g2d, images, data, rightBounds, leftOverlap, undrawnTiles);
        }

        
        if(m_routeDescription != null && m_mouseShadowImage != null && m_routeDescription.getEnd() != null)
        {
            AffineTransform t = new AffineTransform();
            t.translate(m_scale * normalizeX(m_routeDescription.getEnd().getX() - getXOffset()) , m_scale * (m_routeDescription.getEnd().getY() - getYOffset()));
            t.scale(m_scale, m_scale);
            g2d.drawImage(m_mouseShadowImage, t, this);
        }

        MapRouteDrawer.drawRoute((Graphics2D) g2d, m_routeDescription, this, m_uiContext.getMapData());
        
        //used to keep strong references to what is on the screen so it wont be garbage collected
        //other references to the images are weak references
        m_images.clear();
        m_images.addAll(images);

        
        if(m_highlightUnits != null)
        {
            Rectangle r = m_tileManager.getUnitRect(m_highlightUnits, m_data );
            Unit first = m_highlightUnits.get(0);
            
            BufferedImage highlight = (BufferedImage) m_uiContext.getUnitImageFactory().getHighlightImage(first.getType(), first.getOwner(), m_data, first.getHits() != 0);
            
            AffineTransform t = new AffineTransform();
            t.translate( normalizeX(r.getX() - getXOffset()) * m_scale, (r.getY() - getYOffset()) * m_scale);
            t.scale(m_scale, m_scale);
            
            g2d.drawImage(highlight, t, this );
            
        }
            
                
        //draw the tiles nearest us first
        //then draw farther away
        updateUndrawnTiles(undrawnTiles, 30, true);
        updateUndrawnTiles(undrawnTiles, 257, true);
        //when we are this far away, dont force the tiles to stay in memroy
        updateUndrawnTiles(undrawnTiles, 513, false);
        updateUndrawnTiles(undrawnTiles, 767, false);
        
        
        clearUndrawn();
        m_undrawnTiles.addAll(undrawnTiles);
        
        stopWatch.done();
        
    }

    private void clearUndrawn()
    {
        for(int i =0; i < 3; i++) 
        {
            try
            {
                //several bug reports indicate that 
                //clear can throw an exception
                //http://sourceforge.net/tracker/index.php?func=detail&aid=1832130&group_id=44492&atid=439737
                //ignore
                m_undrawnTiles.clear();
                return;
            } catch(Exception e)
            {
                e.printStackTrace(System.out);            
            }    
        }
    }

    boolean mapFitsOnScreen()
    {
        return m_model.getMaxWidth() < getScaledWidth();
    }

   
    /**
     * If we have nothing left undrawn, draw the tiles within preDrawMargin of us, optionally
     * forcing the tiles to remain in memory. 
     */
    private void updateUndrawnTiles(List<Tile> undrawnTiles, int preDrawMargin, boolean forceInMemory)
    {
        //draw tiles near us if we have nothing left to draw
        //that way when we scroll slowly we wont notice a glitch
        if(undrawnTiles.isEmpty())
        {
            
            Rectangle2D extendedBounds = new Rectangle2D.Double( Math.max(m_model.getX() -preDrawMargin, 0),Math.max(m_model.getY() -preDrawMargin, 0), getScaledWidth() + (2 * preDrawMargin),  getScaledHeight() + (2 * preDrawMargin));
            Iterator tiles = m_tileManager.getTiles(extendedBounds).iterator();

            while (tiles.hasNext())
            {
                Tile tile = (Tile) tiles.next();
                if(tile.isDirty())
                {
                    undrawnTiles.add(tile);
                }
                else if(forceInMemory)
                {
                    m_images.add(tile.getRawImage());
                }
            }
            
            
        }
    }

    private void drawTiles(Graphics2D g, List<Tile> images, final GameData data, Rectangle2D.Double bounds, double overlap, List<Tile> undrawn)
    {
        List tileList = m_tileManager.getTiles(bounds);
        Iterator tiles = tileList.iterator();

        if(overlap != 0)
        {
            bounds = new Rectangle2D.Double(bounds.getX() + (overlap - getScaledWidth()), bounds.getY(), bounds.getHeight(), bounds.getWidth());
        }
        
        while (tiles.hasNext())
        {
            Image img = null;
            Tile tile = (Tile) tiles.next();

            LockUtil.acquireLock(tile.getLock());
            try
            {
                if(tile.isDirty())
                {
                    //take what we can get to avoid screen flicker
                    undrawn.add(tile);
                    img = tile.getRawImage();
                    
                }
                else
                {
                    img = tile.getImage(data, m_uiContext.getMapData());
                    images.add(tile);
                }
                if(img != null)
                {
                    AffineTransform t = new AffineTransform();
                    t.translate(m_scale * (tile.getBounds().x -bounds.getX()), m_scale * (tile.getBounds().y - m_model.getY()));
                    g.drawImage(img, t, this);
                }
            }
            finally 
            {
                LockUtil.releaseLock(tile.getLock());
            }
            
            
          
        }
    }

    public Image getTerritoryImage(Territory territory)
    {
        getData().acquireReadLock();
        try
        {
            return m_tileManager.createTerritoryImage(territory, m_data,m_uiContext.getMapData());
        }
        finally 
        {
            getData().releaseReadLock();
        }
    }

    public Image getTerritoryImage(Territory territory, Territory focusOn)
    {
        getData().acquireReadLock();
        try
        {
            return m_tileManager.createTerritoryImage(territory, focusOn, m_data,m_uiContext.getMapData());
        }
        finally 
        {
            getData().releaseReadLock();
        }
    }
    
    public double getScale()
    {
        return m_scale;
    }

    
    @Override
    public void setScale(double newScale)
    {
        super.setScale(newScale);
        //setScale will check bounds, and normalize the scale correctly
        double normalizedScale = m_scale;
        
        m_uiContext.setScale(normalizedScale);        
        recreateTiles(getData(), m_uiContext);        
        repaint();
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
            m_smallMapImageManager.updateTerritoryOwner(territory, m_data, m_uiContext.getMapData());
            
        }
        
        m_smallMapImageManager.update(m_data, m_uiContext.getMapData());
        
    }  
    
    public void setMouseShadowUnits(Collection<Unit> units)
    {
        if(units == null || units.isEmpty())
        {
            m_mouseShadowImage = null;
            SwingUtilities.invokeLater(new Runnable()
                    {
                        public void run()
                        {
                            repaint();
                        }
                    
                    });
            return;
        }
        
        Set<UnitCategory> categories =  UnitSeperator.categorize(units);
        
        final int icon_width =  m_uiContext.getUnitImageFactory().getUnitImageWidth();
        
        final int xSpace = 5;
         
        BufferedImage img = Util.createImage( categories.size() * (xSpace + icon_width), UnitImageFactory.UNIT_ICON_HEIGHT, true); 
        Graphics2D g = (Graphics2D) img.getGraphics();
        
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f) );
        
        Rectangle bounds = new Rectangle(0,0,0,0);
        
        
        getData().acquireReadLock();
        try
        {
            int i = 0;
            for(UnitCategory category : categories)
            {
                Point place = new Point( i * (icon_width + xSpace), 0);
                UnitsDrawer drawer = new UnitsDrawer(category.getUnits().size(), category.getType().getName(), 
                        category.getOwner().getName(), place,category.getDamaged(), false, "", m_uiContext );
                drawer.draw(bounds, m_data, g, m_uiContext.getMapData(), null, null);
               
                i++;
            }
        }
        finally
        {
            getData().releaseReadLock();
        }
        m_mouseShadowImage = img;
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                repaint();
            }
        
        });
        g.dispose();
    }
    
    public void setTerritoryOverlay(Territory territory, Color color, int alpha)
    {
        m_tileManager.setTerritoryOverlay(territory, color, alpha, m_data, m_uiContext.getMapData() );
    }

    public void setTerritoryOverlayForBorder(Territory territory, Color color)
    {
        m_tileManager.setTerritoryOverlayForBorder(territory, color, m_data, m_uiContext.getMapData());
    }
    
    public void clearTerritoryOverlay(Territory territory)
    {
        m_tileManager.clearTerritoryOverlay(territory, m_data, m_uiContext.getMapData());
    }

    public UIContext getUIContext()
    {
        return m_uiContext;
    }

    public void hideMouseCursor()
    {
        if(m_hiddenCursor == null)
            m_hiddenCursor = getToolkit().createCustomCursor(new BufferedImage(1,1,BufferedImage.TYPE_4BYTE_ABGR), new Point(0,0), "Hidden");
        setCursor(m_hiddenCursor);
    }

    public void showMouseCursor()
    {
        setCursor(Cursor.getDefaultCursor());
    }

    public Image getErrorImage()
    {
        return m_uiContext.getMapData().getErrorImage();
    }

    public Image getWarningImage()
    {
        return m_uiContext.getMapData().getWarningImage();
    }

    public Image getInfoImage()
    {
        return m_uiContext.getMapData().getInfoImage();
    }

    public Image getHelpImage()
    {
        return m_uiContext.getMapData().getHelpImage();
    }

}

class RouteDescription
{

    private final Route m_route;
    //this point is in map co-ordinates, un scaled
    private final Point m_start;
    //this point is in map co-ordinates, un scaled    
    private final Point m_end;

    private final Image m_cursorImage;

    public RouteDescription(Route route, Point start, Point end)
    {
        m_route = route;
        m_start = start;
        m_end = end;
        m_cursorImage = null;
    }

    public RouteDescription(Route route, Point start, Point end, Image cursorImage)
    {
        m_route = route;
        m_start = start;
        m_end = end;
        m_cursorImage = cursorImage;
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

        if (m_cursorImage != other.m_cursorImage)
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

    public Image getCursorImage()
    {
        return m_cursorImage;
    }
    
  
}

class BackgroundDrawer implements Runnable
{
    
    
    //use a weak reference, if we see the panel is gc'd, then we can stop this thread
    private final WeakReference<MapPanel> m_mapPanelRef;
    
    BackgroundDrawer(MapPanel panel)
    {
        m_mapPanelRef = new WeakReference<MapPanel>(panel);
    }
    
    public void stop()
    {
        //the thread will eventually wake up and notice we are done
        m_mapPanelRef.clear();
    }
    
    public void run()
    {
        
        while(m_mapPanelRef.get() != null)
        {
            BlockingQueue<Tile> undrawnTiles;
            MapPanel panel = m_mapPanelRef.get();
            if(panel == null)
                continue;
            undrawnTiles = panel.getUndrawnTiles();
            panel = null;
            
            Tile tile;
            try
            {
                tile = undrawnTiles.poll(2000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e)
            {
               continue;
            }

            if(tile == null)
                continue;
            
            final MapPanel mapPanel = m_mapPanelRef.get();
            if(mapPanel == null)
            {
                continue;
            }
            
            GameData data = mapPanel.getData();
            data.acquireReadLock();
            try
            {
                tile.getImage(data, mapPanel.getUIContext().getMapData());
            }
            finally 
            {
                data.releaseReadLock();
            }
            
            SwingUtilities.invokeLater(new Runnable()
            {
               public void run()
               { 
                   mapPanel.repaint();
               }
            });
            
            
        }
    }
    
    
    
}
