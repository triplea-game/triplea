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
 * @version 1.0
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
	public static boolean hasEnoughMovement(Collection units, IntegerMap alreadyMoved, int length)
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
	 * AA and factory dont count as enemy.
	 */
	public static boolean onlyAlliedUnitsOnPath(Route route, PlayerID player, GameData data)
	{
		Match alliedOrNonCombat = new CompositeMatchOr(Matches.UnitIsAAOrFactory, Matches.alliedUnit(player, data));
		for(int i = 0; i < route.getLength() - 1; i++)
		{
			Territory current = route.at(i);
			if(!current.getUnits().allMatch( alliedOrNonCombat))
				return false;
		}
		return true;
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
	
	
	/**
	 * A blitz means no nuetral territories before end,
	 * at least one enemy territory with no units before end.
	 * no enemy units before end (excepting aa and factories).
	 * No water before end.
	 */
	public static boolean isBlitz(Route route, PlayerID player, GameData data)
	{
		if(route.getLength() < 2)
			return false;
		
		Match nonCombatOrAllied = new CompositeMatchOr(Matches.UnitIsAAOrFactory,
													   Matches.alliedUnit(player, data));
		
		boolean blitzableFound = false;
		
		for(int i = 0; i < route.getLength() - 1; i++)
		{
			Territory current = route.at(i);
			
			//if was previously blitzed then still a blitz route
			if(DelegateFinder.battleDelegate(data).getBattleTracker().wasBlitzed(current))
				blitzableFound = true;
			
			//if conquered but not blitzed then cant blitz through it
			if(DelegateFinder.battleDelegate(data).getBattleTracker().wasConquered(current) && 
			   !DelegateFinder.battleDelegate(data).getBattleTracker().wasBlitzed(current))
				return false;
			
			//cant blitz through water
			if(current.isWater())				
				return false;
			
			
			
			if(ownedByNonNeutralEnemy(current, player, data))
			{
				//cant blitz through units that arent factories or aa
				//CompositeMatch match = new 
				if(!current.getUnits().allMatch(nonCombatOrAllied))
					return false;
				blitzableFound = true;		
			}
		}
		return blitzableFound;
	}
	
	public static boolean isUnload(Route route)
	{
		return route.getStart().isWater() && !route.getEnd().isWater();
	}
	
	public static boolean isLoad(Route route)
	{
		return !route.getStart().isWater() && route.getEnd().isWater();
	}
	
	private static boolean ownedByNonNeutralEnemy(Territory territory, PlayerID player, GameData data)
	{		
		PlayerID owner = territory.getOwner();
		if( isFriendly(owner, player, data))
			return false;
		//neutral is null
		if(owner.equals(PlayerID.NULL_PLAYERID))
			return false;
		return true;        
	}
	
	public static boolean canBlitz(Collection units)
	{
		Iterator iter = units.iterator();
		while(iter.hasNext() )
		{
			Unit unit = (Unit) iter.next();
			UnitAttatchment ua = UnitAttatchment.get(unit.getUnitType());
			if(!ua.getCanBlitz())
				return false;
		}
		return true;
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
	
	public static boolean hasSea(Collection units)
	{
		Iterator iter = units.iterator();
		while(iter.hasNext())
		{
			Unit unit = (Unit) iter.next();
			UnitAttatchment ua = UnitAttatchment.get(unit.getType());
			if( ua.isSea())
				return true;
		}
		return false;
	}

	public static boolean hasAir(Collection units)
	{
		Iterator iter = units.iterator();
		while(iter.hasNext())
		{
			Unit unit = (Unit) iter.next();
			UnitAttatchment ua = UnitAttatchment.get(unit.getType());
			if( ua.isAir())
				return true;
		}
		return false;
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

	
	public static boolean isAir(Collection units)
	{
		Iterator iter = units.iterator();
		while(iter.hasNext())
		{
			Unit unit = (Unit) iter.next();
			UnitAttatchment ua = UnitAttatchment.get(unit.getType());
			if(! ua.isAir())
				return false;
		}
		return true;
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
	public static boolean canLand(Collection airUnits, Territory territory, PlayerID player, GameData data)
	{
		if( !Match.allMatch(airUnits, Matches.UnitIsAir))
			throw new IllegalArgumentException("can only test if air will land");
		
		
		if(DelegateFinder.battleDelegate(data).getBattleTracker().wasConquered(territory))
			return false;
		
		if(territory.isWater())
		{
			//if they cant all land on carriers
			if(! Match.allMatch(airUnits, Matches.UnitCanLandOnCarrier))
				return false;
			
			//when doing the calculation, make sure to include the units 
			//in the territory
			Set friendly = new HashSet();
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
		
	public static Collection getNonLand(Collection units)
	{
		Collection nonLand = new ArrayList(units.size());
		Iterator iter = units.iterator();
		while(iter.hasNext())
		{
			Unit unit = (Unit) iter.next();
			UnitAttatchment ua = UnitAttatchment.get(unit.getUnitType());
			if(ua.isAir() || ua.isSea())
			{
				nonLand.add(unit);
			}
		}
		return nonLand;
	}
	
	public static Collection getFriendly(Territory territory, PlayerID player, GameData data)
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
	

	public static int getLeastMovement(Collection units, IntegerMap alreadyMoved)
	{
		if(units.size() == 0)
			throw new IllegalArgumentException("no units");
		int least = Integer.MAX_VALUE;
		Iterator iter = units.iterator();
		while(iter.hasNext())
		{
			Unit unit = (Unit) iter.next();
			int left = movementLeft(unit, alreadyMoved);
			if(left < least)
				least = left;
		}
		return least;
	}
	
	public static int movementLeft(Unit unit, IntegerMap alreadyMoved)
	{
	
		int already = alreadyMoved.getInt(unit);
		int canMove = UnitAttatchment.get(unit.getType()).getMovement(unit.getOwner());
		return canMove - already;
	
	}
	
	public static int getTransportCapacityFree(Territory territory, PlayerID id, GameData data, TransportTracker tracker)
	{
		Match friendlyTransports = new CompositeMatchAnd(Matches.UnitIsTransport, 
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
	
	public static boolean hasSomeLand(Collection units)
	{
		Match notAirOrSea = new CompositeMatchAnd(Matches.UnitIsNotAir, Matches.UnitIsNotSea);
		return Match.someMatch(units, notAirOrSea);
	}
	
	/** Creates new MoveValidator */
    private MoveValidator() 
	{
    }
}
