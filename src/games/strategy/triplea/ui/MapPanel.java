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

import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.util.*;
import java.net.URL;
import javax.swing.*;

import games.strategy.util.PointFileReaderWriter;
import games.strategy.engine.data.*;
import games.strategy.ui.*;
import games.strategy.engine.data.events.TerritoryListener;

import games.strategy.triplea.image.*;

import java.io.*;

/**
 *
 * Responsible for drawing the large map and keeping it updated.
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class MapPanel extends ImageScrollerLargeView
{

  private java.util.List m_mapSelectionListeners = new ArrayList();

  private final GameData m_data;
  private Territory m_currentTerritory; //the territory that the mouse is currently over
                                        //could be null
  private MapPanelSmallView m_smallView;

  //current route we are displaying, could be null
  private Route m_route;


  /** Creates new MapPanel */
  public MapPanel(Image image, GameData data, MapPanelSmallView smallImage) throws
      IOException
  {
      super(image);
      setDoubleBuffered(false);
      m_smallView = smallImage;
      m_data = data;
      data.addTerritoryListener(TERRITORY_LISTENER);


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
      initTerritories();

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

  private void initTerritories()
  {
    clearOffScreen();

    Iterator iter = m_data.getMap().getTerritories().iterator();
    while(iter.hasNext())
    {
      Territory terr = (Territory) iter.next();
      updateTerritory(terr);
    }
    drawRoute();

    update();
    m_smallView.repaint();
  }

  /**
   * Draw m_route to the screen, do nothing if null.
   */
  private void drawRoute()
  {
    if(m_route == null)
      return;

    Graphics2D graphics = (Graphics2D) getOffscreenGraphics();
    graphics.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    graphics.setPaint(Color.red);

    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    Territory current = m_route.getStart();

    Point currentStart =  TerritoryData.getInstance().getCenter(current);

    Point previousPoint = null;
    Point currentFinish = null;

    if(m_route.getLength() > 0)
        currentFinish = TerritoryData.getInstance().getCenter( m_route.at(0));

    Point nextPoint = null;
    if(m_route.getLength() > 1)
        nextPoint = TerritoryData.getInstance().getCenter( m_route.at(1));

    for(int i = 0; i < m_route.getLength(); i++)
    {
        if(i < m_route.getLength())
        {
            Ellipse2D oval = new Ellipse2D.Double(currentStart.x - 3,
                currentStart.y - 3, 6, 6);
            graphics.draw(oval);
        }

        if (nextPoint != null)
            drawCurvedLineWithNextPoint(graphics, currentStart.x,
                                        currentStart.y, currentFinish.x,
                                        currentFinish.y, nextPoint.x,
                                        nextPoint.y);
//        else if (previousPoint != null)
//            drawCurvedLineWithNextPoint(graphics, currentFinish.x,
//                                        currentFinish.y, currentStart.x,
//                                        currentStart.y, previousPoint.x,
//                                        previousPoint.y);
        else
            drawLineSegment(graphics, currentStart.x, currentStart.y,
                            currentFinish.x, currentFinish.y);

      previousPoint = currentStart;
      currentStart = currentFinish;
      currentFinish = nextPoint;
      if(m_route.getLength() > i + 2)
          nextPoint = TerritoryData.getInstance().getCenter( m_route.at(i + 2 ));
      else
          nextPoint = null;

    }
  }


  /**
   * (x,y) - the first point to draw from
   * (xx, yy) - the point to draw too
   * (xxx, yyy) - the next point that the line segment will be drawn to
   */
  private void drawCurvedLineWithNextPoint(Graphics2D graphics, int x, int y, int xx, int yy, int xxx, int yyy)
  {
      final int maxControlLength = 150;
      int controlDiffx = xx - xxx;
      int controlDiffy = yy - yyy;



      if( Math.abs(controlDiffx) > maxControlLength || Math.abs(controlDiffy) > maxControlLength)
      {
          double ratio = 0.0;
          try
          {
              ratio = Math.abs(controlDiffx / controlDiffy);
          }
          catch (ArithmeticException ex)
          {
              ratio = 1000;
          }

          if( Math.abs(controlDiffx) >  Math.abs(controlDiffy))
          {
              controlDiffx = controlDiffx < 0 ?  -maxControlLength : maxControlLength;
              controlDiffy = controlDiffy < 0 ?  (int) (-maxControlLength / ratio) : (int) (maxControlLength / ratio);
          }
          else
          {
              controlDiffy = controlDiffy < 0 ?  -maxControlLength : maxControlLength;
              controlDiffx = controlDiffx < 0 ?  (int) (-maxControlLength * ratio) : (int) (maxControlLength * ratio);

          }
//          controlDiffx = controlDiffx < 0 ?  -maxControlLength : maxControlLength;
//          controlDiffx *= ration;
//          controlDiffy = controlDiffy < 0 ? -maxControlLength ; max
//          controlDiffy *= ratio;
      }



      int controlx = xx + controlDiffx;
      int controly = yy + controlDiffy;

      QuadCurve2D.Double curve = new QuadCurve2D.Double(x,y,controlx, controly, xx,yy);
      graphics.draw(curve);
  }

  //http://www.experts-exchange.com/Programming/Programming_Languages/Java/Q_20627343.html
  private void drawLineSegment( Graphics2D graphics, int x, int y, int xx, int yy)
  {
    float arrowWidth = 12.0f ;
    float theta = 0.7f ;
    int[] xPoints = new int[ 3 ] ;
    int[] yPoints = new int[ 3 ] ;
    float[] vecLine = new float[ 2 ] ;
    float[] vecLeft = new float[ 2 ] ;
    float fLength;
    float th;
    float ta;
    float baseX, baseY ;

    xPoints[ 0 ] = xx ;
    yPoints[ 0 ] = yy ;

    // build the line vector
    vecLine[ 0 ] = (float)xPoints[ 0 ] - x ;
    vecLine[ 1 ] = (float)yPoints[ 0 ] - y ;

    // build the arrow base vector - normal to the line
    vecLeft[ 0 ] = -vecLine[ 1 ] ;
    vecLeft[ 1 ] = vecLine[ 0 ] ;

    // setup length parameters
    fLength = (float)Math.sqrt( vecLine[0] * vecLine[0] + vecLine[1] * vecLine[1] ) ;
    th = arrowWidth / ( 2.0f * fLength ) ;
    ta = arrowWidth / ( 2.0f * ( (float)Math.tan( theta ) / 2.0f ) * fLength ) ;

    // find the base of the arrow
    baseX = ( (float)xPoints[ 0 ] - ta * vecLine[0]);
    baseY = ( (float)yPoints[ 0 ] - ta * vecLine[1]);

    // build the points on the sides of the arrow
    xPoints[ 1 ] = (int)( baseX + th * vecLeft[0] );
    yPoints[ 1 ] = (int)( baseY + th * vecLeft[1] );
    xPoints[ 2 ] = (int)( baseX - th * vecLeft[0] );
    yPoints[ 2 ] = (int)( baseY - th * vecLeft[1] );

    //draw an arrow
      Shape line = new Line2D.Double( x, y, (int)baseX, (int)baseY);
      graphics.draw(line);

      graphics.fillPolygon(xPoints, yPoints, 3);


  }


  private void updateTerritory(Territory terr)
  {
    Graphics graphics = getOffscreenGraphics();
    graphics.setColor(Color.white);
    graphics.setFont(MapImage.MAP_FONT);

    Iterator placementPoints = TerritoryData.getInstance().getPlacementPoints(terr).iterator();
    if(placementPoints == null || !placementPoints.hasNext())
      throw new IllegalStateException("No where to palce units");

    Point lastPlace = null;

    UnitCollection units = terr.getUnits();
    Iterator players = units.getPlayersWithUnits().iterator();

    while(players.hasNext())
    {
      PlayerID player = (PlayerID) players.next();

      Iterator types = m_data.getUnitTypeList().iterator();
      while(types.hasNext())
      {
        UnitType current = (UnitType) types.next();
        int count = units.getUnitCount(current, player);
        if(count != 0)
        {
          Point place;
          if(placementPoints.hasNext())
          {
            place = (Point) placementPoints.next();
            lastPlace = new Point(place.x, place.y);
          }
          else
          {
            place = lastPlace;
            lastPlace.x += UnitIconImageFactory.UNIT_ICON_WIDTH;
          }

          Image img = UnitIconImageFactory.instance().getImage(current, player, m_data);
          graphics.drawImage(img, place.x, place.y, this);
          if(count != 1)
          {
            graphics.drawString(String.valueOf(count), place.x + (UnitIconImageFactory.UNIT_ICON_WIDTH / 4), place.y + UnitIconImageFactory.UNIT_ICON_HEIGHT);
          }

           drawUnitOnSmallScreen(player, place);


        }
      }//end for each unit type
    }//end for each player
  }

  private void drawUnitOnSmallScreen(PlayerID player, Point place)
  {
      double smallLargeRatio = 1 / 15.0; // ((float) m_smallView.getHeight()) / ((float) getHeight());

      Graphics2D smallOffscreen = (Graphics2D) m_smallView.getOffScreenImage().
          getGraphics();
      smallOffscreen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      smallOffscreen.setColor(TerritoryImageFactory.getInstance().
                              getPlayerColour(player).darker());

      Ellipse2D oval = new Ellipse2D.Double(place.x * smallLargeRatio - 3,
                                       place.y * smallLargeRatio - 3,
                                       4,
                                       4);

      smallOffscreen.fill(oval);
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

  public void refreshMap()
  {
    initTerritories();
  }

  private final TerritoryListener TERRITORY_LISTENER = new TerritoryListener()
  {
    public void unitsChanged(Territory territory)
    {
      initTerritories();
    }

    public void ownerChanged(Territory territory)
    {
      MapImage.getInstance().setOwner(territory, territory.getOwner());

      initTerritories();
    }
  };
}
