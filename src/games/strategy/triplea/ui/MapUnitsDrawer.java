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

package games.strategy.triplea.ui;

import java.util.*;

import java.awt.*;
import java.awt.geom.Ellipse2D;

import games.strategy.engine.data.*;
import games.strategy.triplea.image.MapImage;
import games.strategy.triplea.image.TerritoryImageFactory;
import games.strategy.triplea.image.UnitIconImageFactory;
import games.strategy.thread.*;
import javax.swing.*;
import java.lang.reflect.*;
import games.strategy.triplea.delegate.*;
import games.strategy.util.*;
import java.awt.geom.*;


/**
 * Used to draw units on a map.
*
 */

public class MapUnitsDrawer
{
    private static ThreadPool s_threadPool = new ThreadPool(1);

    private Object m_lock = new Object();
    private final GameData m_data;
    private MapPanelSmallView m_smallView;
    private MapPanel m_mapPanel;

    /**
     * A set of Territories
     */
    private Set m_waitingToBeUpdated = new HashSet();

    /**
     * Maps Territory -> Set of Territory names as Strings
     * marks which territories have units that spill onto other territories.
     */
    private Map m_updateDependencies = new HashMap();



    public MapUnitsDrawer(GameData data, MapPanelSmallView smallView, MapPanel mapPanel)
    {
        m_data = data;
        m_smallView = smallView;
        m_mapPanel = mapPanel;
    }

    /**
     * Doesnt return until all updates are finished.
     */
    public void waitForUpdates()
    {
        s_threadPool.waitForAll();
    }

    public  void queueUpdate(Collection  territories)
    {
        synchronized(m_lock)
        {
            m_waitingToBeUpdated.addAll(territories);
            queueUpdate();
        }
    }

    /**
     * Public since we want anyone to be able to tell us to redraw
     */
    public void queueUpdate()
    {
        s_threadPool.runTask(new QueueUpdate());
    }

    public  void queueUpdate(Territory t)
    {
        queueUpdate(Collections.singletonList(t));
    }
    /**
     * Add any dependent territories to the waiting to be updated queue,
     * and any territories that are dependent on that dependent territory
     */
    private void addDependentsToQueue(Territory territory)
    {
        //add territories what we are dependent on
        Set dependents = (Set) m_updateDependencies.get(territory);
        if(dependents != null)
        {
            Iterator iter = dependents.iterator();
            while (iter.hasNext())
            {
                String territoryName = (String) iter.next();
                Territory newTerritory = m_data.getMap().getTerritory(territoryName);
                if (!m_waitingToBeUpdated.contains(newTerritory))
                {
                    m_waitingToBeUpdated.add(newTerritory);
                    addDependentsToQueue(newTerritory);
                }
            }
        }
        //add everything that is dependent on us.
        Iterator entryIter = m_updateDependencies.entrySet().iterator();
        while (entryIter.hasNext())
        {
            Map.Entry entry = (Map.Entry) entryIter.next();
            Territory newTerritory = (Territory) entry.getKey();

            //already accounted for
            if(m_waitingToBeUpdated.contains(newTerritory ))
                continue;

            if( ((Set) entry.getValue()).contains(territory.getName()) )
            {
                m_waitingToBeUpdated.add(newTerritory);
                addDependentsToQueue(newTerritory);
            }
        }
    }

    private void performUpdate()
    {

            if (m_waitingToBeUpdated.isEmpty())
                return;

            //add any dependencies
           Iterator dependencyIter = new HashSet( m_waitingToBeUpdated).iterator();
           while (dependencyIter.hasNext())
           {
               Territory territory = (Territory)dependencyIter.next();
               addDependentsToQueue(territory);
           }

            //clear the sea zones first
            //they can overlap with land zones
            Iterator iter = Match.getMatches(m_waitingToBeUpdated, Matches.TerritoryIsWater).iterator();
            while (iter.hasNext())
            {
                Territory territory = (Territory)iter.next();
                MapImage.getInstance().resetTerritory(territory);
            }

            //now clear the land zones
            //they can overlap with land zones
            iter = Match.getMatches(m_waitingToBeUpdated, Matches.TerritoryIsLand).iterator();
            while (iter.hasNext())
            {
                Territory territory = (Territory) iter.next();
                MapImage.getInstance().resetTerritory(territory);
            }

            //now draw the units
            Iterator terrTter =   m_waitingToBeUpdated.iterator();
            while (terrTter.hasNext())
            {
                Territory territory = (Territory)terrTter.next();
                drawUnits(territory);
            }

            m_waitingToBeUpdated.clear();


    }

    private void drawUnits(Territory territory)
    {
      Set dependencies = new HashSet();

      //add the territories that are contained inside of this territory.
      //this includes islands surrounded by sea zones.
      if(TerritoryData.getInstance().hasContainedTerritory(territory.getName()))
      {
          dependencies.addAll(TerritoryData.getInstance().getContainedTerritory(territory.getName()));
      }

      Graphics graphics = m_mapPanel.getOffscreenGraphics();

      graphics.setColor(Color.white);
      graphics.setFont(MapImage.MAP_FONT);

      Iterator placementPoints = TerritoryData.getInstance().getPlacementPoints(territory).iterator();
      if(placementPoints == null || !placementPoints.hasNext())
        throw new IllegalStateException("No where to palce units");

      Point lastPlace = null;

      UnitCollection units = territory.getUnits();
      Iterator players = units.getPlayersWithUnits().iterator();

      Rectangle2D tempRectanlge = new Rectangle2D.Double();

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
            graphics.drawImage(img, place.x, place.y, m_mapPanel);
            if(count != 1)
            {
              graphics.drawString(String.valueOf(count), place.x + (UnitIconImageFactory.UNIT_ICON_WIDTH / 4), place.y + UnitIconImageFactory.UNIT_ICON_HEIGHT);
            }

             drawUnitOnSmallScreen(player, place);

             //check to see if we are drawing on another territory
             tempRectanlge.setFrame(place.x, place.y, UnitIconImageFactory.UNIT_ICON_HEIGHT, UnitIconImageFactory.UNIT_ICON_WIDTH);
             Collection intersects = TerritoryData.getInstance().intersectsOrIsContainedIn(tempRectanlge);
             if(!intersects.isEmpty())
                 dependencies.addAll(intersects);
          }
        }//end for each unit type
      }//end for each player

      //remove yourself
      dependencies.remove(territory.getName());
      if(dependencies.isEmpty())
          m_updateDependencies.remove(territory);
      else
          m_updateDependencies.put(territory, dependencies);
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

    class QueueUpdate implements Runnable
    {
        public void run()
        {
            synchronized (m_lock)
            {
                try
                {
                    //hopefull we can merge changes together
                    //wait and give other changes a chace to happen
                    m_lock.wait(50);
                }
                catch (InterruptedException ex)
                { //not a big deal
                }

                //dont check to see if we have anything to do and return if we dont
                //we may be called to perform an update just to repaint the screen
                performUpdate();
            }

            try
            {
                SwingUtilities.invokeAndWait(
                    new Runnable()
                {
                    public void run()
                    {
                        //finally, redraw the screen
                        m_mapPanel.update();
                        m_smallView.repaint();
                    }
                }
                );
            }
            catch (InvocationTargetException ex1)
            {
                ex1.printStackTrace();
            }
            catch (InterruptedException ex1)
            {
                ex1.printStackTrace();
            }

        }
    };


}
