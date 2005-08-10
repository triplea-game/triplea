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
 * MoveValidator.java
 *
 * Created on November 9, 2001, 4:05 PM
 */

package games.strategy.triplea.delegate;

import java.util.*;

import games.strategy.util.*;
import games.strategy.engine.data.*;

import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.*;

/**
 *
 * @author  Sean Bridges
 *
 * Provides some static methods for validating movement.
 */
public class MoveValidator
{

	/**
	 * Tests the given collection of units to see if they have the movement neccessary
	 * to move.
	 * @arg alreadyMoved maps Unit -> movement
	 */
	public static boolean hasEnoughMovement(Collection<Unit> units, IntegerMap<Unit> alreadyMoved, int length)
	{

		Iterator iter = units.iterator();
		while(iter.hasNext() )
		{
			Unit unit = (Unit) iter.next();
			int left = movementLeft(unit, alreadyMoved);
			if(left == -1)
				return false;
			if(left < length)
				return false;
		}
		return true;
	}


	/**
	 * Checks that there are no enemy units on the route except possibly at the end.
	 * Submerged enemy units are not considered as they don't affect
	 * movement.
	 * AA and factory dont count as enemy.
	 */
	public static boolean onlyAlliedUnitsOnPath(Route route, PlayerID player, GameData data)
	{
		CompositeMatch<Unit> alliedOrNonCombat = new CompositeMatchOr<Unit>(Matches.UnitIsAAOrFactory, Matches.alliedUnit(player, data));

		// Submerged units do not interfere with movement
		// only relevant for 4th edition
		alliedOrNonCombat.add(Matches.unitIsSubmerged(data));
		
		for(int i = 0; i < route.getLength() - 1; i++)
		{
			Territory current = route.at(i);
			if(!current.getUnits().allMatch( alliedOrNonCombat))
				return false;
		}
		return true;
	}

	public static boolean enemyDestroyerOnPath(Route route, PlayerID player, GameData data)
	{
		Match<Unit> enemyDestroyer = new CompositeMatchAnd<Unit>(Matches.UnitIsDestroyer, Matches.enemyUnit(player, data));
		for(int i = 0; i < route.getLength() - 1; i++)
		{
			Territory current = route.at(i);
			if(current.getUnits().someMatch( enemyDestroyer))
				return true;
		}
		return false;
	}

	
	

	public static boolean hasConqueredNonBlitzedOnRoute(Route route, GameData data)
	{
		for(int i = 0; i < route.getLength() - 1; i++)
		{
			Territory current = route.at(i);
			if(DelegateFinder.battleDelegate(data).getBattleTracker().wasConquered(current) &&
			   !DelegateFinder.battleDelegate(data).getBattleTracker().wasBlitzed(current))
				return true;
		}
		return false;

	}


	public static boolean isBlitzable(Territory current, GameData data, PlayerID player)
	{
	    if(current.isWater())
			return false;

	    //cant blitz on neutrals
	    if(current.getOwner().isNull())
	        return false;

	    if(DelegateFinder.battleDelegate(data).getBattleTracker().wasConquered(current) &&
		   !DelegateFinder.battleDelegate(data).getBattleTracker().wasBlitzed(current))
			return false;
	    
	    CompositeMatch<Unit> blitzableUnits = new CompositeMatchOr<Unit>();
	    blitzableUnits.add(Matches.alliedUnit(player, data));
	    boolean fourthEdition = data.getProperties().get(Constants.FOURTH_EDITION, false);
	    //4th edition, cant blitz through factories and aa guns
	    //2nd edition you can 
	    if(!fourthEdition)
	    {
	        blitzableUnits.add(Matches.UnitIsAAOrFactory);
	    }
	    
	    if(!current.getUnits().allMatch(blitzableUnits))
	        return false;
	    
	    return true;
	}
	
	


	public static boolean isUnload(Route route)
	{
	    if(route.getLength() == 0)
	        return false;
		return route.getStart().isWater() && !route.getEnd().isWater();
	}

	public static boolean isLoad(Route route)
	{
		return !route.getStart().isWater() && route.getEnd().isWater();
	}




	public static boolean hasNuetralBeforeEnd(Route route)
	{
		for(int i = 0; i < route.getLength() - 1; i++)
		{
			Territory current = route.at(i);
			//neutral is owned by null and is not sea
			if(!current.isWater() && current.getOwner().equals(PlayerID.NULL_PLAYERID))
				return true;
		}
		return false;
	}

	public static int getTransportCost(Collection units)
  {
    if(units == null)
      return 0;

    int cost = 0;
    Iterator iter = units.iterator();
    while (iter.hasNext())
    {
      Unit item = (Unit) iter.next();
      cost += UnitAttatchment.get(item.getType()).getTransportCost();
    }
    return cost;
  }


	public static boolean hasUnitsThatCantGoOnWater(Collection units)
	{
		Iterator iter = units.iterator();
		while(iter.hasNext())
		{
			Unit unit = (Unit) iter.next();
			UnitAttatchment ua = UnitAttatchment.get(unit.getType());
			if(!ua.isSea())
			{

				if(ua.isAir() )
				{
					return false;
				}
				//not air and not sea so land
				else if(ua.getTransportCost() == -1)
					return true;
			}
		}
		return false;
	}


	public static int carrierCapacity(Collection units)
	{
		int sum = 0;
		Iterator iter = units.iterator();
		while(iter.hasNext())
		{
			Unit unit = (Unit) iter.next();
			UnitAttatchment ua = UnitAttatchment.get(unit.getType());
			if(ua.getCarrierCapacity() != -1)
			{
				sum+=ua.getCarrierCapacity();
			}
		}
		return sum;
	}

	public static int carrierCost(Collection units)
	{
		int sum = 0;
		Iterator iter = units.iterator();
		while(iter.hasNext())
		{
			Unit unit = (Unit) iter.next();
			UnitAttatchment ua = UnitAttatchment.get(unit.getType());
			if(ua.getCarrierCost() != -1)
				sum+=ua.getCarrierCost();
		}
		return sum;
	}

	public static boolean hasWater(Route route)
	{
		if(route.getStart().isWater())
			return true;

		return route.someMatch(Matches.TerritoryIsWater);
	}


	public static boolean hasLand(Route route)
	{
		if(!route.getStart().isWater())
			return true;

		for(int i = 0; i < route.getLength(); i++)
		{
			Territory t = route.at(i);
			if(! t.isWater())
				return true;
		}
		return false;
	}

	/**
	 * Returns true if the given air units can land in the
	 * given territory.
	 * Does not take into account whether a battle has been
	 * fought in the territory already.
	 *
	 * Note units must only be air units
	 */
	public static boolean canLand(Collection<Unit> airUnits, Territory territory, PlayerID player, GameData data)
	{
		if( !Match.allMatch(airUnits, Matches.UnitIsAir))
			throw new IllegalArgumentException("can only test if air will land");


		if(!territory.isWater())
			if(DelegateFinder.battleDelegate(data).getBattleTracker().wasConquered(territory))
				return false;

		if(territory.isWater())
		{
			//if they cant all land on carriers
			if(! Match.allMatch(airUnits, Matches.UnitCanLandOnCarrier))
				return false;

			//when doing the calculation, make sure to include the units
			//in the territory
			Set<Unit> friendly = new HashSet<Unit>();
			friendly.addAll(getFriendly(territory, player, data));
			friendly.addAll(airUnits);

			//make sure we have the carrier capacity
			int capacity = carrierCapacity(friendly);
			int cost = carrierCost(friendly);
			return  capacity >=  cost;
		}
		else
		{
			return isFriendly(player, territory.getOwner(), data);
		}
	}

	public static Collection<Unit> getNonLand(Collection<Unit> units)
	{
        CompositeMatch<Unit> match = new CompositeMatchOr<Unit>();
        match.add(Matches.UnitIsAir);
        match.add(Matches.UnitIsSea);
        return Match.getMatches(units, match);
	}

	public static Collection<Unit> getFriendly(Territory territory, PlayerID player, GameData data)
	{
		return territory.getUnits().getMatches(Matches.alliedUnit(player,data));
	}

	public static boolean isFriendly(PlayerID p1, PlayerID p2, GameData data)
	{
		if(p1.equals(p2) )
			return true;
		else return data.getAllianceTracker().isAllied(p1,p2);
	}

	public static boolean ownedByFriendly(Unit unit, PlayerID player, GameData data)
	{
		PlayerID owner = unit.getOwner();
		return(isFriendly(owner, player, data));
	}


	public static int getMaxMovement(Collection<Unit> units, IntegerMap<Unit> alreadyMoved)
	{
		if(units.size() == 0)
			throw new IllegalArgumentException("no units");
		int max = 0;
		Iterator iter = units.iterator();
		while(iter.hasNext())
		{
			Unit unit = (Unit) iter.next();
			int left = movementLeft(unit, alreadyMoved);
			max = Math.max(left, max);
		}
		return max;
	}

	
	public static int getLeastMovement(Collection<Unit> units, IntegerMap<Unit> alreadyMoved)
	{
		if(units.size() == 0)
			throw new IllegalArgumentException("no units");
		int least = Integer.MAX_VALUE;
		Iterator iter = units.iterator();
		while(iter.hasNext())
		{
			Unit unit = (Unit) iter.next();
			int left = movementLeft(unit, alreadyMoved);
			least = Math.min(left, least);
		}
		return least;
	}

	public static int movementLeft(Unit unit, IntegerMap<Unit> alreadyMoved)
	{

		int already = alreadyMoved.getInt(unit);
		int canMove = UnitAttatchment.get(unit.getType()).getMovement(unit.getOwner());
		return canMove - already;

	}

	public static int getTransportCapacityFree(Territory territory, PlayerID id, GameData data, TransportTracker tracker)
	{
		Match<Unit> friendlyTransports = new CompositeMatchAnd<Unit>(Matches.UnitIsTransport,
		                                                 Matches.alliedUnit(id, data));
		Collection transports = territory.getUnits().getMatches(friendlyTransports);
		int sum = 0;
		Iterator iter = transports.iterator();
		while(iter.hasNext())
		{
			Unit transport = (Unit) iter.next();
			sum += tracker.getAvailableCapacity(transport);
		}
		return sum;
	}

	public static boolean hasSomeLand(Collection<Unit> units)
	{
		Match<Unit> notAirOrSea = new CompositeMatchAnd<Unit>(Matches.UnitIsNotAir, Matches.UnitIsNotSea);
		return Match.someMatch(units, notAirOrSea);
	}

  public static String validateCanal(Route route, PlayerID player, GameData data)
  {
    Collection territories = route.getTerritories();

    //check suez canal
    Territory eastMed = data.getMap().getTerritory("East Mediteranean Sea Zone");
    Territory redSea = data.getMap().getTerritory("Red Sea Zone");
    if (territories.contains(eastMed) && territories.contains(redSea))
    {
      Territory egypt = data.getMap().getTerritory("Anglo Sudan Egypt");
      Territory iraq = data.getMap().getTerritory("Syria Jordan");

      if (!data.getAllianceTracker().isAllied(player, egypt.getOwner()) ||
          !data.getAllianceTracker().isAllied(player, iraq.getOwner()))
        return "Must own Egypt and Syria/Jordan  to go through Suez Canal";

      BattleTracker tracker = DelegateFinder.battleDelegate(data).getBattleTracker();
      if (tracker.wasConquered(egypt) || tracker.wasConquered(iraq))
        return "Cannot move through canal without owning Egypt and Syria/Jordan for an entire turn.";
    }

    //suez 4th edition
    Territory sz15 = data.getMap().getTerritory("15 Sea Zone");
    Territory sz34 = data.getMap().getTerritory("34 Sea Zone");
    if (territories.contains(sz15) && territories.contains(sz34))
    {
      Territory egypt = data.getMap().getTerritory("Anglo Egypt");
      Territory iraq = data.getMap().getTerritory("Trans-Jordan");

      if (!data.getAllianceTracker().isAllied(player, egypt.getOwner()) ||
          !data.getAllianceTracker().isAllied(player, iraq.getOwner()))
        return "Must own Egypt and Jordan  to go through Suez Canal";

      BattleTracker tracker = DelegateFinder.battleDelegate(data).getBattleTracker();
      if (tracker.wasConquered(egypt) || tracker.wasConquered(iraq))
        return "Cannot move through canal without owning Egypt and Jordan for an entire turn.";

    }
    
    //check panama canal
    Territory carib = data.getMap().getTerritory("Carribean Sea Zone");
    Territory westPan = data.getMap().getTerritory("West Panama Sea Zone");
    
    Territory sz19 = data.getMap().getTerritory("19 Sea Zone");
    Territory sz20 = data.getMap().getTerritory("20 Sea Zone");

    
    if ( (territories.contains(carib) && territories.contains(westPan)) ||
          (territories.contains(sz19) && territories.contains(sz20)) )
    {
      Territory panama = data.getMap().getTerritory("Panama");

      if (!data.getAllianceTracker().isAllied(player, panama.getOwner()))

        return "Must own panama to go through Panama Canal";

      BattleTracker tracker = DelegateFinder.battleDelegate(data).getBattleTracker();
      if (tracker.wasConquered(panama))
        return "Cannot move through canal without owning panama an entire turn.";
    }

    return null;

  }


	/** Creates new MoveValidator */
    private MoveValidator()
	{
    }
}
