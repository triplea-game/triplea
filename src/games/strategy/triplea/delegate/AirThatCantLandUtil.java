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

package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;


/**
 * Utility for detecting and removing units that can't land at the end of a phase. 
 */
public class AirThatCantLandUtil
{
    private final GameData m_data;
    private final IDelegateBridge m_bridge;
    
    
    public AirThatCantLandUtil(final GameData data, final IDelegateBridge bridge)
    {
        m_data = data;
        m_bridge = bridge;
    }

    public static boolean isLHTRCarrierProduction(GameData data)
    {
        return data.getProperties().get(Constants.LHTR_CARRIER_PRODUCTION_RULES, false);
    }

    public Collection<Territory> getTerritoriesWhereAirCantLand(PlayerID player)
    {
        Collection<Territory> cantLand = new ArrayList<Territory>();
        Iterator territories = m_data.getMap().getTerritories().iterator();
        while (territories.hasNext())
        {
            Territory current = (Territory) territories.next();
            CompositeMatch<Unit> ownedAir = new CompositeMatchAnd<Unit>();
            ownedAir.add(Matches.UnitIsAir);
            ownedAir.add(Matches.alliedUnit(player, m_data));
            Collection<Unit> air = current.getUnits().getMatches(ownedAir);
            if (air.size() != 0 && !MoveValidator.canLand(air, current, player, m_data))
            {
                cantLand.add(current);
            }
        }
        return cantLand;
    }

    public void removeAirThatCantLand(PlayerID player, boolean spareAirInSeaZonesBesideFactories)
    {
        Iterator<Territory> territories = getTerritoriesWhereAirCantLand(player).iterator();
        while (territories.hasNext())
        {
            Territory current = territories.next();
            CompositeMatch<Unit> ownedAir = new CompositeMatchAnd<Unit>();
            ownedAir.add(Matches.UnitIsAir);
            ownedAir.add(Matches.alliedUnit(player, m_data));
            Collection<Unit> air = current.getUnits().getMatches(ownedAir);

            boolean hasNeighboringFriendlyFactory =  m_data.getMap().getNeighbors(current, Matches.territoryHasOwnedFactory(m_data, player)).size() > 0;
            boolean skip = spareAirInSeaZonesBesideFactories && current.isWater() && hasNeighboringFriendlyFactory;

            if(!skip)
                removeAirThatCantLand(player, current, air);
        }
    }

    private void removeAirThatCantLand(PlayerID player, Territory territory, Collection<Unit> airUnits)
    {

        Collection<Unit> toRemove = new ArrayList<Unit>(airUnits.size());
        //if we cant land on land then none can
        if (!territory.isWater())
        {
            toRemove.addAll(airUnits);
        } else
        //on water we may just no have enough carriers
        {
            //find the carrier capacity
            Collection carriers = territory.getUnits().getMatches(Matches.alliedUnit(player, m_data));
            int capacity = MoveValidator.carrierCapacity(carriers);

            Iterator iter = airUnits.iterator();
            while (iter.hasNext())
            {
                Unit unit = (Unit) iter.next();
                UnitAttachment ua = UnitAttachment.get(unit.getType());
                int cost = ua.getCarrierCost();
                if (cost == -1 || cost > capacity)
                    toRemove.add(unit);
                else
                    capacity -= cost;
            }
        }

        Change remove = ChangeFactory.removeUnits(territory, toRemove);

        String transcriptText = MyFormatter.unitsToTextNoOwner(toRemove) + " could not land in " + territory.getName() + " and "
                + (toRemove.size() > 1 ? "were" : "was") + " removed";
        m_bridge.getHistoryWriter().startEvent(transcriptText);

        m_bridge.addChange(remove);

    }
    
}
