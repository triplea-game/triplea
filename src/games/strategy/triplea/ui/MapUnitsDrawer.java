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

import games.strategy.engine.data.*;
import games.strategy.thread.ThreadPool;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.image.*;
import games.strategy.triplea.util.*;
import games.strategy.util.Match;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;

import javax.swing.SwingUtilities;

/**
 * Used to draw units on a map.
 * 
 *  
 */

public class MapUnitsDrawer
{
    private static ThreadPool s_threadPool = new ThreadPool(1, "Map Units Thread Pool");

    private final Object m_updateLock = new Object();
    private GameData m_data;
    private final MapPanelSmallView m_smallView;
    private final MapPanel m_mapPanel;

    /**
     * A set of Territories
     */
    private final Set m_waitingToBeUpdated = new HashSet();

    /**
     * Maps Territory name as String -> Set of Territory marks which territories
     * have units that spill onto other territories.
     */
    private final Map m_updateDependencies = new HashMap();

    public MapUnitsDrawer(GameData data, MapPanelSmallView smallView, MapPanel mapPanel)
    {
        m_data = data;
        m_smallView = smallView;
        m_mapPanel = mapPanel;
    }

    public void setData(GameData data)
    {
        synchronized(m_updateLock)
        {
            m_data = data;
        }
    }

    /**
     * Doesnt return until all updates are finished.
     */
    public void waitForUpdates()
    {
        s_threadPool.waitForAll();
    }

    Object getLock()
    {
        return m_updateLock;
    }

    public void queueUpdate(Collection territories)
    {
        if (territories.isEmpty())
            return;

        synchronized (m_updateLock)
        {
            m_waitingToBeUpdated.addAll(territories);
        }
        queueUpdate();
    }

    /**
     * Public since we want anyone to be able to tell us to redraw
     */
    public void queueUpdate()
    {
        s_threadPool.runTask(new QueueUpdate());
    }

    public void queueUpdate(Territory t)
    {
        queueUpdate(Collections.singletonList(t));
    }

    /**
     * Add any dependent territories to the waiting to be updated queue, and any
     * territories that are dependent on that dependent territory
     */
    private void addDependentsToQueue(Territory territory)
    {

        //add territories what we are dependent on
        Set dependents = (Set) m_updateDependencies.get(territory);
        if (territory.isWater())
        {
            if (dependents == null)
                dependents = new HashSet();

            Collection containedTerritorys = MapData.getInstance().getContainedTerritory(territory.getName());
            if (containedTerritorys != null)
                addAll(dependents, containedTerritorys);
        }

        if (dependents != null)
        {
            Iterator iter = dependents.iterator();
            while (iter.hasNext())
            {

                Territory newTerritory = (Territory) iter.next();
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
            if (m_waitingToBeUpdated.contains(newTerritory))
                continue;

            if (((Set) entry.getValue()).contains(territory))
            {
                m_waitingToBeUpdated.add(newTerritory);
                addDependentsToQueue(newTerritory);
            }
        }
    }

    private void performUpdate()
    {
        synchronized (m_updateLock)
        {
            try
            {
                m_data.acquireChangeLock();

                if (m_waitingToBeUpdated.isEmpty())
                    return;

                //add any dependencies
                Iterator dependencyIter = new HashSet(m_waitingToBeUpdated).iterator();
                while (dependencyIter.hasNext())
                {
                    Territory territory = (Territory) dependencyIter.next();
                    addDependentsToQueue(territory);
                }

                //clear the sea zones first
                //they can overlap with land zones
                Iterator iter = Match.getMatches(m_waitingToBeUpdated, Matches.TerritoryIsWater).iterator();
                while (iter.hasNext())
                {
                    Territory territory = (Territory) iter.next();
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
                Iterator terrTter = m_waitingToBeUpdated.iterator();
                while (terrTter.hasNext())
                {
                    Territory territory = (Territory) terrTter.next();
                    drawUnits(territory);
                }

                m_waitingToBeUpdated.clear();
            } finally
            {
                m_data.releaseChangeLock();
            }
        }

    }

    /**
     * Add the given Territory names to the set of Territories, using the
     * GameData's map to convert
     */
    private void addAll(Collection addTo, Collection territoryNames)
    {

        Iterator containedIter = territoryNames.iterator();
        while (containedIter.hasNext())
        {
            String containedTerrName = (String) containedIter.next();
            Territory containedTerritory = m_data.getMap().getTerritory(containedTerrName);
            addTo.add(containedTerritory);

        }

    }

    private void drawUnits(Territory territory)
    {

        Set dependencies = new HashSet();

        //if we are contained in another territory, then add a dependency.
        if (m_data.getMap().getNeighbors(territory).size() == 1)
        {
            dependencies.addAll(m_data.getMap().getNeighbors(territory));
        }

        Graphics graphics = m_mapPanel.getOffscreenGraphics();

        graphics.setColor(Color.white);
        graphics.setFont(MapImage.MAP_FONT);

        Iterator placementPoints = MapData.getInstance().getPlacementPoints(territory).iterator();
        if (placementPoints == null || !placementPoints.hasNext())
        {
            throw new IllegalStateException("No where to place units:" + territory.getName());
        }

        Point lastPlace = null;

        Iterator unitCategoryIter = UnitSeperator.categorize(territory.getUnits().getUnits()).iterator();

        Rectangle2D tempRectanlge = new Rectangle2D.Double();

        while (unitCategoryIter.hasNext())
        {
            UnitCategory category = (UnitCategory) unitCategoryIter.next();

            Point place;
            if (placementPoints.hasNext())
            {
                place = (Point) placementPoints.next();
                lastPlace = new Point(place.x, place.y);
            } else
            {
                place = lastPlace;
                lastPlace.x += UnitIconImageFactory.instance().getUnitImageWidth();
            }

            Image img = UnitIconImageFactory.instance().getImage(category.getType(), category.getOwner(), m_data, category.getDamaged());
            graphics.drawImage(img, place.x, place.y, m_mapPanel);
            int count = category.getUnits().size();
            if (count != 1)
            {
                graphics.drawString(String.valueOf(count), place.x + (UnitIconImageFactory.instance().getUnitImageWidth() / 4), place.y
                        + UnitIconImageFactory.instance().getUnitImageHeight());
            }

            drawUnitOnSmallScreen(category.getOwner(), place);

            //check to see if we are drawing on another territory
            tempRectanlge.setFrame(place.x, place.y, UnitIconImageFactory.UNIT_ICON_HEIGHT, UnitIconImageFactory.UNIT_ICON_WIDTH);
            Collection intersects = MapData.getInstance().territoriesThatOverlap(tempRectanlge);
            if (!intersects.isEmpty())
                addAll(dependencies, intersects);
        }

        //remove yourself
        dependencies.remove(territory);

        if (dependencies.isEmpty())
            m_updateDependencies.remove(territory);
        else
            m_updateDependencies.put(territory, dependencies);

    }

    private void drawUnitOnSmallScreen(PlayerID player, Point place)
    {
        double smallLargeRatio = 1 / 15.0; // ((float) m_smallView.getHeight())
                                           // / ((float) getHeight());

        Graphics2D smallOffscreen = (Graphics2D) m_smallView.getOffScreenImage().getGraphics();
        smallOffscreen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        smallOffscreen.setColor(TerritoryImageFactory.getInstance().getPlayerColour(player).darker());

        Ellipse2D oval = new Ellipse2D.Double(place.x * smallLargeRatio, place.y * smallLargeRatio, 4, 4);

        smallOffscreen.fill(oval);
    }

    private static long latestQueudUpdate = 0;
    private final Object m_scheduleLock = new Object();

    class QueueUpdate implements Runnable
    {

        private long scheduledTime = System.currentTimeMillis();

        QueueUpdate()
        {

        }

        public void run()
        {
            synchronized (m_scheduleLock)
            {
                try
                {
                    //hopefull we can merge changes together
                    //wait and give other changes a chace to happen
                    //this works because as we wait, more territories
                    //can be scheduled for update
                    m_scheduleLock.wait(5);
                } catch (InterruptedException ex)
                { //not a big deal
                }

                //if an update occured after we were scheduled, we dont need to
                // do anything
                if (scheduledTime < latestQueudUpdate)
                    return;
                latestQueudUpdate = System.currentTimeMillis();

                //dont check to see if we have anything to do and return if we
                // dont
                //we may be called to perform an update just to repaint the
                // screen
                performUpdate();

            }

            //dont do invoke and wait since we may be blocking the event queue
            //in waitForUpdate()
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    //finally, redraw the screen
                    m_mapPanel.repaint();
                    m_smallView.repaint();
                }
            });
        }
    };
}