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
package games.strategy.engine.data;

import games.strategy.triplea.delegate.Matches;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RouteFinder
{
    private final GameMap m_map;
    private final Match<Territory> m_condition;
    private final Match<Territory> m_contCondition;
    private final Map<Territory, Territory> m_previous;
    private final Map<Territory, Territory> m_contPrevious;

    public RouteFinder(GameMap map, Match<Territory> condition)
    {
        m_map = map;
        m_condition = condition;
        m_contCondition = null;
        m_previous = new HashMap<Territory, Territory>();
        m_contPrevious = new HashMap<Territory, Territory>();
    }
    public RouteFinder(GameMap map, Match<Territory> condition, Match<Territory> continuationCondition)
    {
        m_map = map;
        m_condition = condition;
        m_contCondition = continuationCondition;
        m_previous = new HashMap<Territory, Territory>();
        m_contPrevious = new HashMap<Territory, Territory>();
    }

    public Route findRoute(Territory start, Territory end)
    {
        Set<Territory> startSet = m_map.getNeighbors(start, m_condition);
        for (Territory t : startSet)
        {
            m_previous.put(t, start);
        }
        if (m_contCondition != null)
        {
            Set<Territory> startSet_cont = m_map.getNeighbors(start, m_contCondition);
            for (Territory t : startSet_cont)
            {
                m_contPrevious.put(t, start);
            }
        }
        if (calculate(startSet, end))
        {
            return getRoute(start, end);
        }
        return null;
    }

    public Route findRouteIgnoringEndCond(Territory start, Territory end, boolean useShuffling)
    {
        List<Territory> startSet = new ArrayList<Territory>(m_map.getNeighbors(start, m_condition));
        if(useShuffling)
            Collections.shuffle(startSet);
        for (Territory t : startSet)
        {
            m_previous.put(t, start);
        }
        if (m_contCondition != null)
        {
            List<Territory> startSetCont = new ArrayList<Territory>(m_map.getNeighbors(start, m_contCondition));
            if(useShuffling)
                Collections.shuffle(startSetCont);
            for (Territory t : startSetCont)
            {
                m_contPrevious.put(t, start);
            }
            if (calculateIgnoringEndCond(startSet, startSetCont, end, useShuffling))
            {
                return getRoute(start, end);
            }
        }
        else
        {
            if (calculateIgnoringEndCond(startSet, end, useShuffling))
            {
                return getRoute(start, end);
            }
        }
        return null;
    }

    private boolean calculate(Set<Territory> startSet, Territory end)
    {

        Set<Territory> nextSet = new HashSet<Territory>();

        for (Territory t : startSet)
        {
            Set<Territory> neighbors = m_map.getNeighbors(t, m_condition);
            for (Territory neighbor : neighbors)
            {
                if (!m_previous.containsKey(neighbor))
                {
                    m_previous.put(neighbor, t);
                    if (neighbor.equals(end))
                    {
                        return true;
                    }
                    nextSet.add(neighbor);
                }
            }
            if (m_contCondition != null)
            {
                Set<Territory> neighborsCont = m_map.getNeighbors(t, m_contCondition);
                for (Territory neighbor : neighborsCont)
                {
                    if (!m_contPrevious.containsKey(neighbor))
                    {
                        m_contPrevious.put(neighbor, t);
                        if (neighbor.equals(end))
                        {
                            return true;
                        }
                        nextSet.add(neighbor);
                    }
                }
            }
        }
        if (nextSet.isEmpty())
        {
            return false;
        }
        return calculate(nextSet, end);
    }
    private boolean calculateIgnoringEndCond(List<Territory> startSet, Territory end, boolean useShuffling)
    {
        return calculateIgnoringEndCond(startSet, new ArrayList<Territory>(), end, useShuffling);
    }
    private boolean calculateIgnoringEndCond(List<Territory> startSet, List<Territory> startContSet, Territory end, boolean useShuffling)
    {
        List<Territory> nextSet = new ArrayList<Territory>();
        List<Territory> nextContSet = new ArrayList<Territory>();

        for (Territory t : startSet)
        {
            List<Territory> neighbors = null;
            if (m_map.getNeighbors(t).contains(end))
            {
                neighbors = new ArrayList<Territory>(m_map.getNeighbors(t));
            }
            else
            {
                neighbors = new ArrayList<Territory>(m_map.getNeighbors(t, m_condition));
            }
            if(useShuffling)
                Collections.shuffle(neighbors);
            for (Territory neighbor : neighbors)
            {
                if (!m_previous.containsKey(neighbor))
                {
                    m_previous.put(neighbor, t);
                    if (neighbor.equals(end))
                    {
                        return true;
                    }
                    nextSet.add(neighbor);
                }
            }
        }
        if (m_contCondition != null)
        {
            for (Territory t : startContSet)
            {
                List<Territory> neighborsCont = null;
                if (m_map.getNeighbors(t).contains(end))
                {
                    neighborsCont = new ArrayList<Territory>(m_map.getNeighbors(t));
                }
                else
                {
                    neighborsCont = new ArrayList<Territory>(m_map.getNeighbors(t, m_contCondition));
                }
                if(useShuffling)
                    Collections.shuffle(neighborsCont);
                for (Territory neighbor : neighborsCont)
                {
                    if (!m_contPrevious.containsKey(neighbor))
                    {
                        m_contPrevious.put(neighbor, t);
                        if (neighbor.equals(end))
                        {
                            return true;
                        }
                        nextContSet.add(neighbor);
                    }
                }
            }
        }
        if ((m_contCondition == null && nextSet.isEmpty()) || (m_contCondition != null && nextContSet.isEmpty()))
        {
            return false;
        }
        return calculateIgnoringEndCond(nextSet, nextContSet, end, useShuffling);
    }

    private Route getRoute(Territory start, Territory destination)
    {
        List<Territory> route = new ArrayList<Territory>();

        Territory current = destination;
        while (current != start)
        {
            if (current == null)
            {
                return null;
            }
            route.add(current);
            if (m_previous.containsKey(current))
                current = m_previous.get(current);
            else
                current = m_contPrevious.get(current);
        }
        route.add(start);
        Collections.reverse(route);
        return new Route(route);
    }
}
