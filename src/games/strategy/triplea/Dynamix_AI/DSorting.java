/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package games.strategy.triplea.Dynamix_AI;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Dynamix_AI.CommandCenter.CachedCalculationCenter;
import games.strategy.util.Match;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author Stephen
 */
public class DSorting
{
    //Think of the compare method like this: the integer returned tells java the position of the first object in relation to the second...
    //If -1 is return, java puts the first object before the second, 0 means they're equal(not sure which would come first), 1 tells java to put the second object before the first

    ///////////////////////////////////////////////Territory Sorting///////////////////////////////////////////////
    public static List<Territory> SortTerritoriesByX(List<Territory> ters, Comparator<Territory> comparator)
    {
        List<Territory> result = new ArrayList<Territory>(ters);
        Collections.sort(result, comparator);
        return result;
    }
    public static List<Territory> SortTerritoriesByScores_A(List<Territory> ters, final HashMap<Territory, Integer> scores)
    {
        List<Territory> result = new ArrayList<Territory>(ters);
        Collections.sort(result, new Comparator<Territory>()
        {
            public int compare(Territory t1, Territory t2)
            {
                if (!scores.containsKey(t1) && !scores.containsKey(t2))
                    return 0; //We can't compare these, so say they're equal
                if (!scores.containsKey(t1))
                    return 1;
                if (!scores.containsKey(t2))
                    return -1;

                int score1 = scores.get(t1);
                int score2 = scores.get(t2);

                return score1 - score2;
            }
        });
        return result;
    }
    public static List<Territory> SortTerritoriesByScores_D(List<Territory> ters, final HashMap<Territory, Integer> scores)
    {
        return DUtils.InvertList(SortTerritoriesByScores_A(ters, scores));
    }
    public static List<Territory> SortTerritoriesByDistance_A(final List<Territory> ters, final GameData data, final Territory target, final Match<Territory> routeMatch)
    {
        return SortTerritoriesByX(ters, new Comparator<Territory>()
        {
            public int compare(Territory t1, Territory t2)
            {
                Route route1 = data.getMap().getRoute(t1, t2, routeMatch);
                Route route2 = data.getMap().getRoute(t1, t2, routeMatch);

                int distance1 = 0;
                int distance2 = 0;
                if(route1 == null)
                    distance1 = DConstants.Integer_HalfMax;
                else
                    distance1 = route1.getLength();
                if(route2 == null)
                    distance2 = DConstants.Integer_HalfMax;
                else
                    distance2 = route1.getLength();

                return distance1 - distance2;
            }
        });
    }
    public static List<Territory> SortTerritoriesByDistance_D(final List<Territory> ters, final GameData data, final Territory target, final Match<Territory> routeMatch)
    {
        return DUtils.InvertList(SortTerritoriesByDistance_A(ters, data, target, routeMatch));
    }
    public static List<Territory> SortTerritoriesByLandDistance_A(final List<Territory> ters, final GameData data, final Territory target)
    {
        return SortTerritoriesByX(ters, new Comparator<Territory>()
        {
            public int compare(Territory t1, Territory t2)
            {
                Route route1 = CachedCalculationCenter.GetLandRoute(data, t1, t2);
                Route route2 = CachedCalculationCenter.GetLandRoute(data, t1, t2);

                int distance1 = 0;
                int distance2 = 0;
                if(route1 == null)
                    distance1 = DConstants.Integer_HalfMax;
                else
                    distance1 = route1.getLength();
                if(route2 == null)
                    distance2 = DConstants.Integer_HalfMax;
                else
                    distance2 = route1.getLength();

                return distance1 - distance2;
            }
        });
    }
    public static List<Territory> SortTerritoriesByLandDistance_D(final List<Territory> ters, final GameData data, final Territory target)
    {
        return DUtils.InvertList(SortTerritoriesByLandDistance_A(ters, data, target));
    }
    public static List<Territory> SortTerritoriesBySeaDistance_A(final List<Territory> ters, final GameData data, final Territory target)
    {
        return SortTerritoriesByX(ters, new Comparator<Territory>()
        {
            public int compare(Territory t1, Territory t2)
            {
                Route route1 = CachedCalculationCenter.GetSeaRoute(data, t1, t2);
                Route route2 = CachedCalculationCenter.GetSeaRoute(data, t1, t2);

                int distance1 = 0;
                int distance2 = 0;
                if(route1 == null)
                    distance1 = DConstants.Integer_HalfMax;
                else
                    distance1 = route1.getLength();
                if(route2 == null)
                    distance2 = DConstants.Integer_HalfMax;
                else
                    distance2 = route1.getLength();

                return distance1 - distance2;
            }
        });
    }
    public static List<Territory> SortTerritoriesBySeaDistance_D(final List<Territory> ters, final GameData data, final Territory target)
    {
        return DUtils.InvertList(SortTerritoriesBySeaDistance_A(ters, data, target));
    }
    public static List<Territory> SortTerritoriesByNoCondDistance_A(final List<Territory> ters, final GameData data, final Territory target)
    {
        return SortTerritoriesByX(ters, new Comparator<Territory>()
        {
            public int compare(Territory t1, Territory t2)
            {
                Route route1 = CachedCalculationCenter.GetRoute(data, t1, t2);
                Route route2 = CachedCalculationCenter.GetRoute(data, t1, t2);

                int distance1 = 0;
                int distance2 = 0;
                if(route1 == null)
                    distance1 = DConstants.Integer_HalfMax;
                else
                    distance1 = route1.getLength();
                if(route2 == null)
                    distance2 = DConstants.Integer_HalfMax;
                else
                    distance2 = route1.getLength();

                return distance1 - distance2;
            }
        });
    }
    public static List<Territory> SortTerritoriesByNoCondDistance_D(final List<Territory> ters, final GameData data, final Territory target)
    {
        return DUtils.InvertList(SortTerritoriesByNoCondDistance_A(ters, data, target));
    }
    public static List<Territory> SortTerritoriesByLandThenNoCondDistance_A(final List<Territory> ters, final GameData data, final Territory target)
    {
        return SortTerritoriesByX(ters, new Comparator<Territory>()
        {
            public int compare(Territory t1, Territory t2)
            {
                Route route1 = CachedCalculationCenter.GetLandRoute(data, t1, t2);
                Route route2 = CachedCalculationCenter.GetLandRoute(data, t1, t2);
                Route route1_nc = CachedCalculationCenter.GetRoute(data, t1, t2);
                Route route2_nc = CachedCalculationCenter.GetRoute(data, t1, t2);

                if(route1_nc == null && route2_nc == null)
                    return 0; //We can't compare these, so say they're equal
                if(route1_nc == null)
                    return 1;
                if(route2_nc == null)
                    return -1;

                int distance1 = route1_nc.getLength() * 100;
                int distance2 = route2_nc.getLength() * 100;
                if(route1 == null)
                    distance1 = DConstants.Integer_HalfMax;
                else
                    distance1 = route1.getLength();
                if(route2 == null)
                    distance2 = DConstants.Integer_HalfMax;
                else
                    distance2 = route1.getLength();

                return distance1 - distance2;
            }
        });
    }
    public static List<Territory> SortTerritoriesByLandThenNoCondDistance_D(final List<Territory> ters, final GameData data, final Territory target)
    {
        return DUtils.InvertList(SortTerritoriesByLandThenNoCondDistance_A(ters, data, target));
    }
    ///////////////////////////////////////////////End Territory Sorting///////////////////////////////////////////////

    ///////////////////////////////////////////////Unit Sorting///////////////////////////////////////////////
    public static List<Unit> SortUnitsByX(List<Unit> units, Comparator<Unit> comparator)
    {
        List<Unit> result = new ArrayList<Unit>(units);
        Collections.sort(result, comparator);
        return result;
    }
    public static List<Unit> SortUnitsByCost_A(List<Unit> units, final Resource resource)
    {
        return SortUnitsByX(units, new Comparator<Unit>()
        {
            public int compare(Unit o1, Unit o2)
            {
                int cost1 = DUtils.GetTUVOfUnits(Collections.singletonList(o1), o1.getOwner(), resource);
                int cost2 = DUtils.GetTUVOfUnits(Collections.singletonList(o2), o2.getOwner(), resource);

                return cost1 - cost2;
            }
        });
    }
    ///////////////////////////////////////////////End Unit Sorting///////////////////////////////////////////////
}
