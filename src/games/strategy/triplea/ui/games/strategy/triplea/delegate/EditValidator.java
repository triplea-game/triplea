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
 * EditValidator.java
 *
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.Match;

import java.util.Collection;
import java.util.List;

/**
 *
 * @author  Tony Clayton
 *
 * Provides some static methods for validating game edits.
 */
public class EditValidator
{
    private static String validateTerritoryBasic(GameData data, Territory territory, PlayerID player)
    {
        String result = null;

        // territory cannot contain enemy units
        if (!Matches.territoryIsEmptyOfCombatUnits(data, player).match(territory))
            return "Territory contains enemy units";

        // territory cannot be in a pending battle
        BattleTracker battleTracker = DelegateFinder.battleDelegate(data).getBattleTracker();
        if (battleTracker.getPendingBattle(territory, true) != null)
            return "Territory contains a pending SBR battle";
        if (battleTracker.getPendingBattle(territory, false) != null)
            return "Territory contains a pending battle";

        // territory cannot be in an UndoableMove route
        List<UndoableMove> moves = DelegateFinder.moveDelegate(data).getMovesMade();
        for (UndoableMove move : moves)
        {
            if (move.getRoute().getStart() == territory 
                    || move.getRoute().getEnd() == territory)
                return "Territory is start or end of a pending move";
        }

        return result;
    }

    public static String validateChangeTerritoryOwner(GameData data, Territory territory, PlayerID player)
    {
        String result = null;

        if (Matches.TerritoryIsWater.match(territory))
            return "Territory is water";

        if ((result = validateTerritoryBasic(data, territory, player)) != null)
            return result;

        return result;
    }

    public static String validateAddUnits(GameData data, Territory territory, Collection<Unit> units)
    {
        String result = null;

        if (units.isEmpty())
            return "No units selected";

        PlayerID player = units.iterator().next().getOwner();

        // check land/water sanity
        if (territory.isWater())
        {
            if (!Match.allMatch(units, Matches.UnitIsSea))
            {
            	if(Match.allMatch(units, Matches.UnitIsLand))
            	{           		
	            	//Set up matches
	            	TransportTracker transportTracker = new TransportTracker();
	            	Match<Unit> friendlyTransports = new CompositeMatchAnd<Unit>(Matches.UnitIsTransport,Matches.alliedUnit(player, data));
	            	Match<Unit> friendlyLandUnits = new CompositeMatchAnd<Unit>(Matches.UnitIsLand,Matches.alliedUnit(player, data));
	            	//Determine transport capacity
	                int transportCapacityTotal = MoveValidator.getTransportCapacityFree(territory, player, data, transportTracker);
	                int transportCost = MoveValidator.getTransportCost(territory.getUnits().getMatches(friendlyLandUnits)) + MoveValidator.getTransportCost(units);
	                //Get any transports in the sea zone
	            	Collection<Unit> transports = territory.getUnits().getMatches(friendlyTransports);
	            	
	            	if(transports.size() == 0 || transportCapacityTotal - transportCost < 0)
	            		return "Can't add land units to water";
            	}
            	if (Match.allMatch(units, Matches.UnitIsAir))
            	{
            		//Set up matches
	            	Match<Unit> friendlyCarriers = new CompositeMatchAnd<Unit>(Matches.UnitIsCarrier,Matches.alliedUnit(player, data));
	            	Match<Unit> friendlyAirUnits = new CompositeMatchAnd<Unit>(Matches.UnitIsAir,Matches.alliedUnit(player, data));
	            	//Determine transport capacity
	                int carrierCapacityTotal = MoveValidator.carrierCapacity(territory.getUnits().getMatches(friendlyCarriers));
	                int carrierCost = MoveValidator.carrierCost(territory.getUnits().getMatches(friendlyAirUnits)) + MoveValidator.carrierCost(units);
	                //Get any transports in the sea zone
	            	Collection<Unit> carriers = territory.getUnits().getMatches(friendlyCarriers);
	            	
	            	if(carriers.size() == 0 || carrierCapacityTotal - carrierCost < 0)
	            		return "Can't add land units to water";
            	}
            }
        }
        else
        {
            // Can't add to neutral or enemy territory
            if (!Matches.isTerritoryFriendly(player, data).match(territory))
                return "Can't add units to neutral or enemy territory";

            if (Match.someMatch(units, Matches.UnitIsSea))
                return "Can't add sea units to land";
        }

        if ((result = validateTerritoryBasic(data, territory, player)) != null)
            return result;

        return result;
    }

    public static String validateRemoveUnits(GameData data, Territory territory, Collection<Unit> units)
    {
        String result = null;

        if (units.isEmpty())
            return "No units selected";

        PlayerID player = units.iterator().next().getOwner();

        // all units should be same owner
        if (!Match.allMatch(units, Matches.unitIsOwnedBy(player)))
            return "Not all units have the same owner";

        if ((result = validateTerritoryBasic(data, territory, player)) != null)
            return result;

        TransportTracker transportTracker = new TransportTracker();

        // if transport selected, all transported units must be deleted too
        for (Unit unit : Match.getMatches(units, Matches.UnitCanTransport))
        {
            if (!units.containsAll(transportTracker.transporting(unit)))
                return "Can't remove transport without removing transported units";
        }

        // if transported units selected, transport must be deleted too
        for (Unit unit : Match.getMatches(units, Matches.UnitCanBeTransported))
        {
            Unit transport = transportTracker.transportedBy(unit);
            if (transport != null && !units.contains(transport))
                return "Can't remove transported units without removing transport";
        }

        // TODO: if carrier selected, all carried planes must be deleted too

        // TODO: if carried planes selected, carrier must be deleted too

        return result;
    }
}
