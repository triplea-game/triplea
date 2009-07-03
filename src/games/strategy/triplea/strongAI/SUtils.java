package games.strategy.triplea.strongAI;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
//import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.CanalAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveValidator;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.InverseMatch;
import games.strategy.util.Match;
import games.strategy.util.IntegerMap;

//import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
//import java.util.Formatter;
import java.util.List;
import java.util.Set;
import java.util.Iterator;
//import java.util.HashMap;


public class SUtils
{
	/**
	* determine the threat to the capital of the player's allies
	* returns boolean true or false whether threat exists
	* returns capitals threatened in threats
	*/
	public static boolean threatToAlliedCapitals(GameData data, PlayerID player, List<Territory> threats, boolean tFirst)
	{
		List<Territory> alliedCapitols = new ArrayList<Territory>();
	    for(PlayerID otherPlayer : data.getPlayerList().getPlayers())
        {
	    	if (otherPlayer == player)
	    		continue;
            Territory capitol =  TerritoryAttachment.getCapital(otherPlayer, data);
	        if(capitol != null && data.getAllianceTracker().isAllied(player, capitol.getOwner()))
	        	alliedCapitols.add(capitol);
        }
	    for (Territory cap : alliedCapitols)
	    {
	    	float landThreat = getStrengthOfPotentialAttackers(cap, data, player, tFirst, true);
	    	float capStrength = strength(cap.getUnits().getMatches(Matches.alliedUnit(player, data)), false, false, tFirst) + 5.0F;
	    	if (capStrength*1.10F < landThreat) //trouble
	    	{
	    		threats.add(cap);
	    	}
	    }
	    return threats.size()>0;
	}
	
    public static final Match<Territory> TerritoryIsImpassableToLandUnits = new Match<Territory>()
    {
        public boolean match(Territory t)
        {
            if (t.isWater())
                return true;
            else if (t.getOwner().equals(PlayerID.NULL_PLAYERID))
                return true;
            else if (TerritoryAttachment.get(t).isImpassible())
                return true;
            else
                return false;
        }
    };
    public final static Match<Territory> TerritoryIsNotImpassableToLandUnits = new InverseMatch<Territory>(TerritoryIsImpassableToLandUnits);

    /**
     * Interleave infantry and artillery/armor for loading on transports
     */
    public static List<Unit> sortTransportUnits(List<Unit> transUnits)
    {
		List<Unit> sorted = new ArrayList<Unit>();
		List<Unit> infantry = new ArrayList<Unit>();
		List<Unit> artillery = new ArrayList<Unit>();
		List<Unit> armor = new ArrayList<Unit>();

		for (Unit x : transUnits)
		{
			if (Matches.UnitIsArtillerySupportable.match(x))
				infantry.add(x);
			else if (Matches.UnitIsArtillery.match(x))
				artillery.add(x);
			else if (Matches.UnitCanBlitz.match(x))
				armor.add(x);
		}
		int artilleryCount = artillery.size();
		int armorCount = armor.size();
		int infCount = infantry.size();
		for (int j=0; j < infCount; j++) //interleave the artillery and armor with inf
		{
			sorted.add(infantry.get(j));
			if (armorCount > 0)
			{
				sorted.add(armor.get(armorCount-1));
				armorCount--;
			}
			else if (artilleryCount > 0)
			{
				sorted.add(artillery.get(artilleryCount-1));
				artilleryCount--;
			}
		}
		if (artilleryCount > 0)
		{
			for (int j2=0; j2 < artilleryCount; j2++)
				sorted.add(artillery.get(j2));
		}
		if (armorCount > 0)
		{
			for (int j3=0; j3 < armorCount; j3++)
				sorted.add(armor.get(j3));
		}
		return sorted;

	}

    /**
     * All the territories that border one of our territories
     */

    public static List<Territory> getNeighboringEnemyLandTerritories(GameData data, PlayerID player)
    {
        ArrayList<Territory> rVal = new ArrayList<Territory>();
        for(Territory t : data.getMap())
        {
            if(Matches.isTerritoryEnemy(player, data).match(t) && !t.getOwner().isNull() )
            {
                if(!data.getMap().getNeighbors(t, Matches.isTerritoryOwnedBy(player)).isEmpty())
                    rVal.add(t);
            }
        }
        return rVal;
    }
    /**
     * 
     * All territories that border our terr or allied terr
     * 
     * boolean allied - search for enemies of allied territories, not just owned
     * 
     */
    public static List<Territory> getNeighboringEnemyLandTerritories(GameData data, PlayerID player, boolean allied)
    {
        ArrayList<Territory> rVal = new ArrayList<Territory>();
        for(Territory t : data.getMap())
        {
            if(Matches.isTerritoryEnemy(player, data).match(t) && !t.getOwner().isNull() )
            {
				if (allied && !data.getMap().getNeighbors(t, Matches.isTerritoryAllied(player, data)).isEmpty())
                   	    rVal.add(t);
                else if(!data.getMap().getNeighbors(t, Matches.isTerritoryOwnedBy(player)).isEmpty())
                    rVal.add(t);
            }
        }
        return rVal;
    }
    /**
     * find the water terr next to target which is closest to the source
     * target: land destination
     * source: present location
     * minDist: actual distance from source to resulting water terr
     * Returns null if target has no water territories
     */
    public static Territory getClosestWaterTerr(Territory target, Territory source, int minDist, GameData data, PlayerID player)
    { 
		Set<Territory> waterTerr = data.getMap().getNeighbors(target, Matches.TerritoryIsWater);
		Territory result = null;
		if (waterTerr.size() == 0)
		{
			minDist=0;
			return result;
		}
		minDist = 100;
		int thisDist = 100;
		for (Territory checkTerr : waterTerr)
		{
			thisDist = data.getMap().getWaterDistance(source, checkTerr);
			if (thisDist < minDist)
			{
				minDist = thisDist;
				result = checkTerr;
			}
		}
		return result;
	}

    /**
     * All the territories that border a certain territory
     */
    public static List<Territory> getNeighboringEnemyLandTerritories(GameData data, PlayerID player, Territory check)
    {
        List<Territory> rVal = new ArrayList<Territory>();
        List<Territory> checkList = getExactNeighbors(check, 1, data, false);
        for(Territory t : checkList)
        {
//            if(Matches.isTerritoryEnemyAndNotNuetralWater(player, data).match(t))
            if (Matches.isTerritoryEnemy(player, data).match(t) && !t.getOwner().isNull() && Matches.TerritoryIsNotImpassable.match(t))
               rVal.add(t);
        }
        return rVal;
    }

    /**
     * All Allied Territories which neighbor a territory
     * This duplicates getNeighbors(check, Matches.isTerritoryAllied(player, data))
     */
    public static List<Territory> getNeighboringLandTerritories(GameData data, PlayerID player, Territory check)
    { 
    	ArrayList<Territory> rVal = new ArrayList<Territory>();
    	List<Territory> checkList = getExactNeighbors(check, 1, data, false);
    	for (Territory t : checkList)
    	{
			if (Matches.isTerritoryAllied(player, data).match(t) && Matches.TerritoryIsNotImpassable.match(t))
				rVal.add(t);
		}
		return rVal;
	}

    /**
     * Does this territory have any land? i.e. it isn't an island
     * @return boolean (true if a land territory is a neighbor to t
     */
	public static boolean doesLandExistAt(Territory t, GameData data)
	{ //simply: is this territory surrounded by water
		boolean isLand = false;
		Set<Territory> checkList = data.getMap().getNeighbors(t, Matches.TerritoryIsLand);
		for (Territory checkNeutral : checkList)
		{
			if (Matches.TerritoryIsNotImpassable.match(checkNeutral) && !t.getOwner().isNull())
				isLand=true;
		}
		return isLand;
	}

	/**
	 * Recursive routine for finding the distance to an enemy
	 * @param t
	 * @param beenThere - list of territories already checked
	 * @param data
	 * @param player
	 * @return int of distance to enemy
	 */
	public static int distanceToEnemy(Territory t, List<Territory> beenThere, GameData data, PlayerID player)
	{ //find the distance to the closest land territory by recursion
	  //xDist must be passed as 0
	  //if no enemy territory can be found...it returns 0
		List<Territory> thisTerr = getNeighboringEnemyLandTerritories(data, player, t);
		beenThere.add(t);
		int newDist = 1;

		if (thisTerr.size() == 0) //searches land territories
		{
			List<Territory> newTerrList = getNeighboringLandTerritories(data, player, t);
			newTerrList.removeAll(beenThere);
			if (newTerrList.size() == 0)
				newDist = 0;
			else
			{
				int minDist = 100;
				for (Territory t2 : newTerrList)
				{
					int aDist = distanceToEnemy(t2, beenThere, data, player);
					newDist += aDist;
					if (newDist < minDist && aDist > 0)
						minDist = newDist;
				}
				if (minDist < 100)
					newDist = minDist;
			}
			// xDist = 0;
		}
		return newDist;
	}

	/**
	 * List containing the enemy Capitals
	 */
	public static List<Territory> getEnemyCapitals(GameData data, PlayerID player)
	{ //generate a list of all enemy capitals
		List<Territory> enemyCapitals = new ArrayList<Territory>();
	    for(PlayerID otherPlayer : data.getPlayerList().getPlayers())
        {
            Territory capitol =  TerritoryAttachment.getCapital(otherPlayer, data);
	        if(capitol != null && !data.getAllianceTracker().isAllied(player, capitol.getOwner()))
	        	enemyCapitals.add(capitol);

	    }
	    return enemyCapitals;

	}

	/**
	 * Looking for the closest land territory next to an enemy capital which is owned by an ally
	 * Use this for dumping units from transports
	 * @capTerr - actual Capital being targeted (returned parameter)
	 * @fromTerr - source of units
	 * @return - Land Territory with allied units
	 */
	public static Territory getAlliedLandTerrNextToEnemyCapital(int minDist, Territory capTerr, Territory fromTerr, GameData data, PlayerID player)
	{
        minDist = 100;
        Territory capWaterTerr = null;
        capTerr = null;
        List<Territory> enemyCapitals = getEnemyCapitals(data, player);
        if (enemyCapitals.size() > 0)
        {
			for (Territory badCapital : enemyCapitals)
			{
				List<Territory> areaTerritories = getNeighboringLandTerritories(data, player, badCapital);
				if (areaTerritories.size() == 0)
					continue;
				for (Territory nextToCapital : areaTerritories)
				{
					int capDist = 100;
					Territory tcapTerr = null;
					for (Territory tmpCapTerr : data.getMap().getNeighbors(nextToCapital, Matches.TerritoryIsWater))
					{
						int tDist = data.getMap().getWaterDistance(fromTerr, tmpCapTerr);
						if (tDist < capDist)
						{
							capDist = tDist;
							tcapTerr = tmpCapTerr;
						}
					}
					if (tcapTerr == null)
						continue;
					if (capDist < minDist)
					{
						capWaterTerr = nextToCapital;
						capTerr = tcapTerr;
						minDist = capDist;
					}
				}
			}
		}
		return capWaterTerr;
	}

	/**
	 *determines the Land Route to a capital...returns if it exists and puts it in goRoute
	 *returns null for the Route if it does not exist
	 * 
	 * @param thisTerr - Territory to be checked
	 * @param goRoute - contains the actual route
	 * @return - true if the route exists, false if it doesn't exist
	 */
	public static boolean landRouteToEnemyCapital(Territory thisTerr, Route goRoute, GameData data, PlayerID player)
	{//is there a land route between territory and enemy
     //   Territory myCapital = TerritoryAttachment.getCapital(player, data);

        boolean routeExists = false;
        Route route = null;

	    for(PlayerID otherPlayer : data.getPlayerList().getPlayers())
        {
            Territory capitol =  TerritoryAttachment.getCapital(otherPlayer, data);
	        if(capitol != null && !data.getAllianceTracker().isAllied(player, capitol.getOwner()))
	        {
	            route = data.getMap().getRoute(thisTerr, capitol, TerritoryIsNotImpassableToLandUnits);
	            if(route != null)
	           	   routeExists = true;
	        }

	    }
	    if (routeExists)
	    	goRoute = route;
	    else
	    	goRoute = null;

	    return routeExists;
	}

	/**
     * Fill a List with units from the passed list up to maxStrength
     */
    public static List<Unit> getUnitsUpToStrength(double maxStrength, Collection<Unit> units, boolean attacking, boolean sea)
    {
        if(strength(units, attacking, sea, false) < maxStrength)
            return new ArrayList<Unit>(units);

        ArrayList<Unit> rVal = new ArrayList<Unit>();

        for(Unit u : units)
        {
            rVal.add(u);
            if(strength(rVal, attacking, sea, false) > maxStrength)
                return rVal;
        }

        return rVal;

    }
    
    /**
     * Finds a list of territories which contain airUnits, but don't have land Units 
     * 
     * @return List of Territories
     */
    public static List<Territory> TerritoryOnlyPlanes(GameData data, PlayerID player)
    {
		List <Unit> airUnits = new ArrayList<Unit>();
		List <Unit> landUnits = new ArrayList<Unit>();
		List <Territory> returnTerr = new ArrayList<Territory>();
		int aUnit = 0, lUnit = 0;
		//find all territories for this player which only contain planes
		CompositeMatch<Unit> airUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsAir);
		CompositeMatch<Unit> landUnit = new CompositeMatchAnd<Unit>(Matches.isUnitAllied(player, data), Matches.UnitIsLand, Matches.UnitIsNotAA);
		List<Territory> owned = allOurTerritories(data, player);

		for (Territory t: owned)
		{
			airUnits = t.getUnits().getMatches(airUnit);
			landUnits = t.getUnits().getMatches(landUnit);
			aUnit = airUnits.size();
			lUnit = landUnits.size();
			if (aUnit > 0 & lUnit == 0) //we want places that have air units only
				returnTerr.add(t);
		}
		return returnTerr;
	}

    /**
     * Finds the Territory within 3 Territories which has a maximum # of units
     * Uses friendly to determine if we are looking for our guys or enemy
     * Returns Territory with a maximum # of units (enemy or friendly)
     */
	public static Territory findNearestMaxUnits(PlayerID player, GameData data, Territory t, boolean friendly)
	{
		CompositeMatch<Unit> landUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsLand, Matches.UnitIsNotAA);
		CompositeMatch<Unit> enemyLandUnit = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data), landUnit);
		CompositeMatch<Unit> ourLandUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), landUnit);
		int totUnit = 0;
		int maxUnit = 0;
		Territory maxTerr = null ;

		Collection <Territory> nearNeighbors = data.getMap().getNeighbors(t, 3);
		nearNeighbors.remove(t);
		for (Territory t2 : nearNeighbors)
		{
			if (t2.isWater() || TerritoryAttachment.get(t2).isImpassible())
				continue;
			if (friendly)
			{
				List <Unit> ourGuys = t2.getUnits().getMatches(ourLandUnit);
				totUnit = ourGuys.size();
				if (totUnit > maxUnit )
				{
					maxUnit = totUnit;
					maxTerr = t2;
				}
			}
			else
			{
				List <Unit> theirGuys = t2.getUnits().getMatches(enemyLandUnit);
				totUnit = theirGuys.size();
				if (totUnit > maxUnit )
				{
					maxUnit = totUnit;
					maxTerr = t2;
				}
			}

		}
		if (maxTerr == null)
			maxTerr = t;
		return maxTerr;
	}

	public static Collection<Unit> whatPlanesNeedToLand(boolean doBombers, Territory thisTerritory, PlayerID player)
	{ /*
		this considers carriers in our current territory, but no where else
	    can't check for other carriers here because the route has been established
	    could look at passing the route through and modify it if a sea based territory
	    the premise of this is a little messed up because we are requiring our bombers and fighters
	    to use the same route...probably needs a complete rewrite
	  */

		Collection <Unit> ourPlanes = new ArrayList<Unit>();
		Collection <Unit> fighters = new ArrayList<Unit>();
		CompositeMatch<Unit> fighterUnit = new CompositeMatchAnd<Unit>(Matches.UnitCanLandOnCarrier, Matches.unitIsOwnedBy(player));
		CompositeMatch<Unit> bomberUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsStrategicBomber, Matches.unitIsOwnedBy(player));

		CompositeMatch<Unit> alliedFighter = new CompositeMatchAnd<Unit>(Matches.UnitCanLandOnCarrier);
		//assume the only other fighters would be allied

		//separate fighters and bombers so that we can coordinate fighters with carriers
		fighters.addAll(thisTerritory.getUnits().getMatches(fighterUnit));
		Collection <Unit> bombers = thisTerritory.getUnits().getMatches(bomberUnit);
		if (thisTerritory.isWater())
		{
			Collection <Unit> carriers = thisTerritory.getUnits().getMatches(Matches.UnitIsCarrier);
			Collection <Unit> otherfighters = thisTerritory.getUnits().getMatches(alliedFighter);
			otherfighters.removeAll(fighters);
			/* we can land on any allied carrier
			   but we must allow their own fighters to land there
			*/
			int numAlliedFighters = otherfighters.size();
			int carrierCapacity = carriers.size()*2 - (numAlliedFighters + 1);
			for (int i=0; i<=carrierCapacity; i++)
			{
				fighters.remove(i);
			}
		}
		if (!doBombers)
			ourPlanes.addAll(fighters);
		else
			ourPlanes.addAll(bombers);
		return ourPlanes;
	}

	/**
	 * Returns the strength of all attackers to a territory
	 * differentiates between sea and land attack
	 * determines all transports within range of territory
	 * determines all air units within range of territory (using 2 for fighters and 3 for bombers)
	 * does not check for extended range fighters or bombers
	 * @param tFirst - can transports be killed before other sea units
	 * @param ignoreOnlyPlanes - if true, returns 0.0F if only planes can attack the territory
	 */
    public static float getStrengthOfPotentialAttackers(Territory location, GameData data, PlayerID player, boolean tFirst, boolean ignoreOnlyPlanes)
    {
        float seaStrength = 0.0F, firstStrength = 0.0F, secondStrength = 0.0F, blitzStrength = 0.0F, strength=0.0F, airStrength=0.0F;
		CompositeMatch<Unit> enemyPlane = new CompositeMatchAnd<Unit>(Matches.UnitIsAir, Matches.enemyUnit(player, data));
		CompositeMatch<Unit> enemyBomber = new CompositeMatchAnd<Unit>(Matches.UnitIsStrategicBomber, Matches.enemyUnit(player, data));
		CompositeMatch<Unit> enemyTransport = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data), Matches.UnitIsTransport);
		CompositeMatch<Unit> enemyShip = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data), Matches.UnitIsSea);
		CompositeMatch<Territory> validSeaRoute = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasNoAlliedUnits(player, data));
		Set<Territory> waterTerr = data.getMap().getNeighbors(location, Matches.TerritoryIsWater);
		List<Territory> checked = new ArrayList<Territory>();
		
        for(Territory t : data.getMap().getNeighbors(location,  location.isWater() ? Matches.TerritoryIsWater :  Matches.TerritoryIsLand))
        {
           	List<Unit> enemies = t.getUnits().getMatches(Matches.enemyUnit(player, data));
            firstStrength+= strength(enemies, true, location.isWater(), tFirst);
            checked.add(t);
        }
        if (Matches.TerritoryIsLand.match(location))
        {
        	blitzStrength = determineEnemyBlitzStrength(location, data, player);
        }
        else //get ships attack strength
        {
        	for (int i=2; i<=3; i++)
        	{
        		List<Territory> moreTerr = getExactNeighbors(location, i, data, false);
        		for (Territory shipTerr : moreTerr)
        		{
        			if (!shipTerr.isWater())
        				continue;
            		Route seaRoute = data.getMap().getRoute(shipTerr, location, validSeaRoute);
            		if (seaRoute == null) //no valid route...ignore ships
            			continue;
        			List <Unit> moreEnemies = shipTerr.getUnits().getMatches(enemyShip);
        			secondStrength += strength(moreEnemies, true, true, tFirst);
        		}
        	}
        }
 
        for (Territory t2 : data.getMap().getNeighbors(location, 4)) //get air strength
        {
			if (!checked.contains(t2) && t2.getUnits().someMatch(Matches.enemyUnit(player, data)))
			{
				int airDist = data.getMap().getDistance(t2, location, Matches.TerritoryIsNotImpassable); 
				if (airDist < 3) //limit fighter reach to 2 sectors
				{
					List<Unit> attackPlanes = t2.getUnits().getMatches(enemyPlane);
					airStrength += allairstrength(attackPlanes, true);
				}
				else if (airDist == 3)
				{
					List<Unit> bomberPlanes = t2.getUnits().getMatches(enemyBomber);
					airStrength += allairstrength(bomberPlanes, true);
				}
			}
			if (!t2.isWater() || waterTerr.isEmpty() || location.isWater())
				continue;
			boolean transportsCounted = false;
			Iterator<Territory> iterTerr = waterTerr.iterator();
			while (!transportsCounted && iterTerr.hasNext())
            {
				Territory waterCheck = iterTerr.next();
				if (data.getMap().getWaterDistance(t2, waterCheck) <=2)
				{
			        List<Unit> transports = t2.getUnits().getMatches(enemyTransport);
			        int transNum= transports.size();
			        seaStrength += transNum*4.7F; //Big Assumption of transport potential unit strength...2 inf = 3.4, 1 inf + other = 5.4
			        transportsCounted = true;
			    }
			}
		}
        strength = seaStrength + blitzStrength + firstStrength + secondStrength;
        if (!ignoreOnlyPlanes || strength > 0.0F)
		    strength += airStrength;

        return strength;
    }

    /**
     * 
     * Returns all Territories which contain Allied Units within a radius of 4 Territories of t
     * Works for land units as well as ships
     * @return
     */
    public static List<Territory> findOurShips(Territory t, GameData data, PlayerID player)
    {
		//Return territories of Allied Ships within a sea radius of 4
		List<Territory> shipTerr = new ArrayList<Territory>();
		Collection<Territory> tNeighbors = data.getMap().getNeighbors(t, 4);
		for (Territory t2 : tNeighbors)
		{
			if (t2.getUnits().someMatch(Matches.isUnitAllied(player, data)))
				shipTerr.add(t2);
		}
		return shipTerr;
	}

    /**
     * Returns a list of Territories containing Allied Ships within a radius of 3 which meet the condition
     * @param unitCondition - Match condition
     * @return - List of Territories - works ships or land units
     */
	public static List<Territory> findOurShips(Territory t, GameData data, PlayerID player, Match<Unit> unitCondition)
	{
		//Return territories of Allied Ships within a sea radius of 3 (AC Limit)
		CompositeMatch<Unit> limitShips = new CompositeMatchAnd<Unit>(unitCondition, Matches.isUnitAllied(player, data));
		List<Territory> shipTerr = new ArrayList<Territory>();
		Collection<Territory> tNeighbors = data.getMap().getNeighbors(t, 3);
		for (Territory t2 : tNeighbors)
		{
			if (t2.getUnits().someMatch(limitShips))
				shipTerr.add(t2);
		}
		return shipTerr;
	}

	/**
	 * Identical to findOurShips...just owned rather than allied
	 * Could be merged with findOurShips with a boolean for allied
	 * Finds all units within 3 Territories of t which match unitCondition
	 * returns the List
	 */
	public static List<Territory> findOnlyMyShips(Territory t, GameData data, PlayerID player, Match<Unit> unitCondition)
	{
		//Return territories of Owned Ships within a sea radius of 3 (AC Limit)
		CompositeMatch<Unit> limitShips = new CompositeMatchAnd<Unit>(unitCondition, Matches.unitIsOwnedBy(player));
		List<Territory> shipTerr = new ArrayList<Territory>();
		Collection<Territory> tNeighbors = data.getMap().getNeighbors(t, 3);
		for (Territory t2 : tNeighbors)
		{
			if (t2.getUnits().someMatch(limitShips))
				shipTerr.add(t2);
		}
		return shipTerr;
	}

	/**
	 *
	 * Return All Territories containing a Certain Owned Unit (Land, Sea or Air) specified by unitCondition
	 * @return List of Territories
	 */
	public static List<Territory> findCertainShips(GameData data, PlayerID player, Match<Unit> unitCondition)
	{
		CompositeMatch<Unit> limitShips = new CompositeMatchAnd<Unit>(unitCondition, Matches.unitIsOwnedBy(player));
		List<Territory> shipTerr = new ArrayList<Territory>();
		Collection<Territory> allTerr = data.getMap().getTerritories();
		for (Territory t2 : allTerr)
		{
			if (t2.getUnits().someMatch(limitShips))
				shipTerr.add(t2);
		}
		return shipTerr;
	}

	/**
	 * Return Territories containing any unit depending on unitCondition
	 * Differs from findCertainShips because it doesn't require the units be owned 
	 */
	public static List<Territory> findUnitTerr(GameData data, PlayerID player, Match<Unit> unitCondition)
	{
		//Return territories containing a certain unit or set of Units
		CompositeMatch<Unit> limitShips = new CompositeMatchAnd<Unit>(unitCondition);
		List<Territory> shipTerr = new ArrayList<Territory>();
		Collection<Territory> tNeighbors = data.getMap().getTerritories();
		for (Territory t2 : tNeighbors)
		{
			if (t2.getUnits().someMatch(limitShips))
				shipTerr.add(t2);
		}
		return shipTerr;
	}

	/**
	 * Returns list of territories within 4 territories of t and contain owned planes
	 */
	public static List<Territory> findOurPlanes(Territory t, GameData data, PlayerID player)
	{
		//Return territories of our planes within 4 of this Territory
		CompositeMatch<Unit> ownedAndAir = new CompositeMatchAnd<Unit>(Matches.UnitIsAir, Matches.unitIsOwnedBy(player));
		List<Territory> airTerr = new ArrayList<Territory>();
		Collection<Territory> tNeighbors = data.getMap().getNeighbors(t, 4);
		for (Territory t2 : tNeighbors)
		{
			if (t2.getUnits().someMatch(ownedAndAir))
				airTerr.add(t2);
		}
		return airTerr;
	}

	/**
	 * Returns a list of territories within maxDist which meat the requirement of unitCondition
	 * No requirements of ownership or allied units
	 * @return
	 */
	public static List<Territory> findUnits(Territory t, GameData data, Match<Unit> unitCondition, int maxDist)
	{
		//Return territories of our Units within maxDist of this Territory
		List<Territory> anyTerr = new ArrayList<Territory>();
		Collection<Territory> tNeighbors = data.getMap().getNeighbors(t, maxDist);
		for (Territory t2 : tNeighbors)
		{
			if (t2.getUnits().someMatch(unitCondition))
				anyTerr.add(t2);
		}
		return anyTerr;
	}

	/**
	 * Territories we actually own in a modifiable List
	 */
	public static List<Territory> allOurTerritories(GameData data, PlayerID player)
	{
		Collection<Territory> ours = data.getMap().getTerritoriesOwnedBy(player);
		List<Territory> ours2 = new ArrayList<Territory>();
		ours2.addAll(ours);
		return ours2;
	}
	/**
	 * All Allied TErritories in a modifiable List
	 */
	public static List<Territory> allAlliedTerritories(GameData data, PlayerID player)
	{ 
		List<Territory> ours = new ArrayList<Territory>();
		for (Territory t : data.getMap())
			if (Matches.isTerritoryAllied(player, data).match(t))
				ours.add(t);
		ours.addAll(allOurTerritories(data, player));
		return ours;
	}
	/**
	 * All Enemy Territories in a modifiable List
	 */
	public static List<Territory> allEnemyTerritories(GameData data, PlayerID player)
	{
		List<Territory> badGuys = new ArrayList<Territory>();
		for (Territory t : data.getMap())
			if (Matches.isTerritoryEnemy(player, data).match(t))
				badGuys.add(t);
		return badGuys;
	}

	/**
	 * Find the Route to the nearest Territory
	 * @param start - starting territory
	 * @param endCondition - condition for the ending Territory
	 * @param routeCondition - condition for each Territory in Route
	 * @param data
	 * @return
	 */
    public static Route findNearest(Territory start, Match<Territory> endCondition, Match<Territory> routeCondition, GameData data)
    {
        Route shortestRoute = null;
        for(Territory t : data.getMap().getTerritories())
        {
            if(endCondition.match(t))
            {
                CompositeMatchOr<Territory> routeOrEnd = new CompositeMatchOr<Territory>(routeCondition, Matches.territoryIs(t));
                Route r = data.getMap().getRoute(start, t, routeOrEnd);
                if(r != null)
                {
                    if(shortestRoute == null || r.getLength() < shortestRoute.getLength())
                       shortestRoute = r;
                }
            }
        }
        return shortestRoute;
    }
    /**
     * Find Route from start to a Territory having endCondition which has a maximum of a certain set of Units (unitCondition)
     * @param start - initial territory
     * @param endCondition - final territory must match this
     * @param routeCondition - all territories on route must match this
     * @param unitCondition - units must match this
     * @param maxUnits - how many units were found there
     * @return - Route to the endCondition
     */
    public static Route findNearestMaxContaining(Territory start, Match<Territory> endCondition, Match<Territory> routeCondition, Match<Unit> unitCondition, int maxUnits, GameData data)
    {
        Route shortestRoute = null;
        for(Territory t : data.getMap().getTerritories())
        {
            if(endCondition.match(t))
            {
				List<Unit> countUnits = t.getUnits().getMatches(unitCondition);
				if (countUnits.size() > maxUnits)
					continue;
                CompositeMatchOr<Territory> routeOrEnd = new CompositeMatchOr<Territory>(routeCondition, Matches.territoryIs(t));
                Route r = data.getMap().getRoute(start, t, routeOrEnd);
                if(r != null)
                {
                    if(shortestRoute == null || r.getLength() < shortestRoute.getLength())
                       shortestRoute = r;
                }
            }
        }
        return shortestRoute;
    }

    public static Route findNearestNotEmpty(Territory start, Match<Territory> endCondition, Match<Territory> routeCondition, GameData data)
    {
        Route shortestRoute = null;
        for(Territory t : data.getMap().getTerritories())
        {
			if (t.getUnits().size() > 0)
			{
            	if(endCondition.match(t))
            	{
            	    CompositeMatchOr<Territory> routeOrEnd = new CompositeMatchOr<Territory>(routeCondition, Matches.territoryIs(t));
            	    Route r = data.getMap().getRoute(start, t, routeOrEnd);
            	    if(r != null)
            	    {
            	        if(shortestRoute == null || r.getLength() < shortestRoute.getLength())
            	           shortestRoute = r;
            	    }
				}
            }
        }
        if (shortestRoute == null)
            return shortestRoute;
        else
        {
        	for(Territory t2 : data.getMap().getTerritories())
        	{
        	    if(endCondition.match(t2))
        	    {
        	        CompositeMatchOr<Territory> routeOrEnd = new CompositeMatchOr<Territory>(routeCondition, Matches.territoryIs(t2));

        	        Route r = data.getMap().getRoute(start, t2, routeOrEnd);
        	        if(r != null)
        	        {
        	            if(shortestRoute == null || r.getLength() < shortestRoute.getLength())
        	               shortestRoute = r;
        	        }
        	    }
			}
        }
        return shortestRoute;
    }
    /**
     * true or false...does a land route exist from territory to any enemy capitol?
     */
    public  static boolean hasLandRouteToEnemyOwnedCapitol(Territory t, PlayerID us, GameData data)
    {
        for(PlayerID player : data.getPlayerList())
        {
            Territory capitol = TerritoryAttachment.getCapital(player, data);

            if(data.getAllianceTracker().isAllied(us, capitol.getOwner()))
                continue;

            if(data.getMap().getDistance(t, capitol, TerritoryIsNotImpassableToLandUnits) != -1)
            {
                return true;
            }

        }
        return false;

    }
    public static boolean airUnitIsLandableOnCarrier(Unit u, Territory source, Territory target, Territory acTarget, PlayerID player, GameData data)
    {
		// Warning: THIS DOES NOT VERIFY THE # OF PLANES PLANNING TO LAND on the AC
		//          Calling program must verify that there is room on the AC
		List<Territory> acTerr = ACTerritory(player, data);
		int rDist = data.getMap().getDistance(source, target);
		boolean landable = false;
		if (MoveValidator.hasEnoughMovement(u, rDist));
		{
			for (Territory t : acTerr)
			{
				int rDist2 = data.getMap().getDistance(target, t);
				if (MoveValidator.hasEnoughMovement(u, rDist+rDist2))
					landable = true;
			}
		}
		return landable;
	}

    public static boolean airUnitIsLandable(Unit u, Territory source, Territory target, PlayerID owner, GameData data)
    {
		int rDist = data.getMap().getDistance(source, target);
		boolean landable = false;
		if (MoveValidator.hasEnoughMovement(u, rDist))
		{
			Collection <Territory> tNeighbors = data.getMap().getNeighbors(target,4);
			for (Territory landOwned : tNeighbors)
			{
				int rDist2 = data.getMap().getDistance(target, landOwned);
				if (MoveValidator.hasEnoughMovement(u, rDist+rDist2) & !landOwned.isWater() & landOwned.getOwner()==owner)
					landable=true; //we can land
										//we need to check for an available A/C
			}
		}
		return landable;
	}

    /**
     * Returns a list of all territories containing owned AirCraft Carriers
     */
	public static List<Territory> ACTerritory(PlayerID player, GameData data)
	{ //Return Territories containing AirCraft Carriers
		List <Territory> carriers = new ArrayList<Territory>();
		List <Territory> owned = allOurTerritories(data, player);
		CompositeMatch<Unit> ourCarrier = new CompositeMatchAnd<Unit>(Matches.UnitIsCarrier, Matches.unitIsOwnedBy(player));
		for (Territory t : owned)
		{
			if (t.getUnits().someMatch(ourCarrier))
				carriers.add(t);
		}
		return carriers;
	}


	public static void addUnitCollection(List<Collection<Unit>> aUnits, Collection <Unit> bUnits)
	{
		Collection <Unit> cUnits= new ArrayList<Unit>();

		cUnits.addAll(bUnits);
		aUnits.add(cUnits);
	}

    /**
     * Determine the available strength of a single air unit
     * Primarily useful for a sea battle needing support
     * Moved from AIUtils to improve portability with changes by others
     */

    public static float airstrength(Unit airunit, boolean attacking)
    {
		float airstrength = 0.0F;
		Unit u = airunit;
		UnitAttachment unitAttatchment = UnitAttachment.get(u.getType());
		if (unitAttatchment.isAir())
		{
			airstrength += 0.70F;
			if (attacking)
				airstrength += unitAttatchment.getAttack(u.getOwner());
			else
				airstrength += unitAttatchment.getDefense(u.getOwner());
		}
		return airstrength;
    }

    /**
     * Determine the strength of a collection of airUnits
     */
    public static float allairstrength(Collection<Unit> units, boolean attacking)
    {
		float airstrength = 0.0F;
		for (Unit u : units)
		{
//		Unit u= units;
			UnitAttachment unitAttatchment = UnitAttachment.get(u.getType());
			if (unitAttatchment.isAir())
			{
				airstrength += 0.70F;
				if (attacking)
					airstrength += unitAttatchment.getAttack(u.getOwner());
				else
					airstrength += unitAttatchment.getDefense(u.getOwner());
			}
		}
		return airstrength;
    }

    /**
     * Determine the strength of a single unit
     */
    public static float uStrength(Unit units, boolean attacking, boolean sea, boolean transportsFirst)
    {
        float strength = 0.0F;
        Unit u = units;

        UnitAttachment unitAttatchment = UnitAttachment.get(u.getType());
        if(unitAttatchment.isAA() || unitAttatchment.isFactory())
        {
            //nothing
        }
        else if(unitAttatchment.isSea() == sea)
        {
            strength +=  0.70F;
            //two hit
            if(unitAttatchment.isTwoHit())
                strength +=0.70F;
            //the number of pips on the dice
            if(attacking)
                strength += unitAttatchment.getAttack(u.getOwner());
            else
                strength += unitAttatchment.getDefense(u.getOwner());
            if(attacking)
            {
                if(unitAttatchment.getAttack(u.getOwner()) == 0)
                    strength -= 0.35F; //adjusted KDM
            }
            if (unitAttatchment.getTransportCapacity()>0 && !transportsFirst)
                strength -=0.35F; //only allow transport to have 0.35 on defense; none on attack
        }
        else if (unitAttatchment.isAir() & sea) //we can count airplanes in sea attack
        {
        	strength += 0.70F;
        	if (attacking)
        		strength += unitAttatchment.getAttack(u.getOwner());
        	else
        		strength += unitAttatchment.getDefense(u.getOwner());
		}

        return strength;
    }

    /**
     * Get a quick and dirty estimate of the strength of some units in a battle.<p>
     *
     * @param units - the units to measure
     * @param attacking - are the units on attack or defense
     * @param sea - calculate the strength of the units in a sea or land battle?
     * @return
     */
    public static float strength(Collection<Unit> units, boolean attacking, boolean sea, boolean transportsFirst)
    {
        float strength = 0.0F;

        for(Unit u : units)
        {
            UnitAttachment unitAttatchment = UnitAttachment.get(u.getType());
            if(unitAttatchment.isAA() || unitAttatchment.isFactory())
                continue;
            else if(unitAttatchment.isSea() == sea)
            {
				//BB = 5.4; AC=1.7/3.7; SUB=2.7; DS=3.7; TR=0.35/1.7; F=3.7/4.7; B=4.7/1.7;
                strength += 0.70F; //played with this value a good bit

                if(unitAttatchment.isTwoHit())
                    strength += 0.70F;

                if(attacking)
                    strength += unitAttatchment.getAttack(u.getOwner());
                else
                    strength += unitAttatchment.getDefense(u.getOwner());

                if(attacking)
                {
                    if(unitAttatchment.getAttack(u.getOwner()) == 0)
                        strength -= 0.35F;
                }
                if (unitAttatchment.getTransportCapacity()>0 && !transportsFirst)
                    strength -=0.35F; //only allow transport to have 0.35 on defense; none on attack
            }
            else if (unitAttatchment.isAir() == sea)
            {
				strength += 0.70;
                if(attacking)
                    strength += unitAttatchment.getAttack(u.getOwner());
                else
                    strength += unitAttatchment.getDefense(u.getOwner());
			}

        }

        if(attacking)
        {
            int art = Match.countMatches(units, Matches.UnitIsArtillery);
            int artSupport = Match.countMatches(units, Matches.UnitIsArtillerySupportable);
            strength += Math.min(art, artSupport);
        }

        return strength;
    }

	/**
	 * determines a suitable Territory for a factory
	 * suitable: At Least 2 IPC
	 * All Territories around it are owned
	 * Strength of Units in the Territory and 1 Territory away
	 * Is greater than the sum of all enemy Territory 2 away
	 * Territory should be closest to an enemy Capital
	 * @param data
	 * @param player
	 * @param risk - not really used...should pass a relative risk back
	 * @param buyfactory
	 * @return
	 */
    public static Territory findFactoryTerritory(GameData data, PlayerID player, float risk, boolean buyfactory)
    {
		List<Territory> owned = allOurTerritories(data, player);
		Territory minTerr = null;
		float minRisk = 1.0F;

		risk = 1.0F;

		for (Territory t: owned)
		{
			int ipcValue = TerritoryAttachment.get(t).getProduction();
			if (ipcValue < 2 || Matches.territoryHasOwnedFactory(data, player).match(t))
				continue;
			List<Territory> weOwnAll = getNeighboringEnemyLandTerritories(data, player, t);
			if (weOwnAll.size() > 0)
				continue;
			Set <Territory> factCheck = data.getMap().getNeighbors(t, Matches.territoryHasEnemyFactory(data, player));
			if (factCheck.size()>0)
				continue;
			List<Territory> twoAway = getExactNeighbors(t, 2, data, false);
			List<Territory> threeAway = getExactNeighbors(t, 3, data, false);
			boolean badIdea = false;
			float twoCheckStrength = 0.0F, threeCheckStrength = 0.0F;
			for (Territory twoCheck : twoAway)
			{
				if (Matches.territoryHasEnemyUnits(player, data).match(twoCheck))
					twoCheckStrength += strength(twoCheck.getUnits().getMatches(Matches.enemyUnit(player, data)), true, false, false);
			}
			float tStrength = strength(t.getUnits().getMatches(Matches.unitIsOwnedBy(player)), false, false, false);
			if (twoCheckStrength > 10.0F)
					badIdea = true;
			for (Territory threeCheck : threeAway)
			{
				if (Matches.territoryHasEnemyUnits(player, data).match(threeCheck))
				{ //only count it if it has a path
					Route d1 = data.getMap().getLandRoute(threeCheck, t);
					threeCheckStrength += strength(threeCheck.getUnits().getMatches(Matches.enemyUnit(player, data)), true, false, false);
				}
			}
			if (threeCheckStrength > 15.0F)
			{
				badIdea = true;
			}
			if (badIdea)
				continue;
			if (buyfactory && hasLandRouteToEnemyOwnedCapitol(t, player, data))
			{
				minTerr=t;
				risk = 0.00F;
				return minTerr;
			}
		}
		risk = minRisk;
		buyfactory= false;
		return minTerr;

	}

    public static void getStrengthAt(float Strength1, float Strength2, GameData data, PlayerID player, Territory ourTerr,
    		boolean sea, boolean contiguous, boolean tFirst)
    {
		//gives our sea Strength within one and two territories of ourTerr
		//defensive strength
		Collection<Unit> seaUnits = new ArrayList<Unit>();
		Collection<Unit> airUnits = new ArrayList<Unit>();
		Collection<Unit> landUnits = new ArrayList<Unit>();
		int rDist=0, r=2;
		float thisStrength = 0.0F;

		if (!sea)
		    r=1;
		if (!ourTerr.isWater() && sea)
		    r=3; //if we have a land terr and looking at sea...look 3 out rather than 2
		List<Territory> nearNeighbors = new ArrayList<Territory>();
		Set <Territory> nN = data.getMap().getNeighbors(ourTerr, r);
		nearNeighbors.addAll(nN);
		if (ourTerr.isWater() == sea)
		    nearNeighbors.add(ourTerr);
		CompositeMatch<Unit> owned = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player));

		CompositeMatch<Unit> seaUnit = new CompositeMatchAnd<Unit>(owned, Matches.UnitIsSea);
		CompositeMatch<Unit> airUnit = new CompositeMatchAnd<Unit>(owned, Matches.UnitIsAir);
		CompositeMatch<Unit> landUnit = new CompositeMatchAnd<Unit>(owned, Matches.UnitIsLand);

		Strength1 = 0.0F;
		Strength2 = 0.0F;

		for (Territory t: nearNeighbors)
		{
			if (contiguous)
			{
				if (t.isWater() && sea)
				{ //don't count anything in a transport
					rDist = data.getMap().getWaterDistance(ourTerr, t);
					seaUnits = t.getUnits().getMatches(seaUnit);
					airUnits = t.getUnits().getMatches(airUnit);
					thisStrength = strength(seaUnits, false, true, tFirst) + allairstrength(airUnits, false);
				}
				else if (!t.isWater() && !sea)
				{
					rDist = data.getMap().getLandDistance(ourTerr, t);
					landUnits = t.getUnits().getMatches(landUnit);
					airUnits = t.getUnits().getMatches(airUnit);
					thisStrength = strength(landUnits, false, false, tFirst) + allairstrength(airUnits, false);
				}
				else
					continue;
			}
			else
			{
				rDist = data.getMap().getDistance(ourTerr, t);
				if (t.isWater() && sea)
				{ //don't count anything in a transport
					seaUnits = t.getUnits().getMatches(seaUnit);
					airUnits = t.getUnits().getMatches(airUnit);
					thisStrength = strength(seaUnits, false, true, tFirst) + allairstrength(airUnits, false);
				}
				else if (!t.isWater())
				{
					landUnits = t.getUnits().getMatches(landUnit);
					airUnits = t.getUnits().getMatches(airUnit);
					thisStrength = strength(landUnits, false, false, tFirst) + allairstrength(airUnits, false);
				}
			}
			if (rDist == 0 || rDist == 1)
				Strength1 += thisStrength;
			if (rDist >= 0 && rDist <=3)
				Strength2 += thisStrength;
			thisStrength = 0.0F;
			rDist = 0;
		}
	}

    //gives enemy Strength within one and three zones of territory if sea, one and two if land
	//attack strength
	//sea is true if this is a sea check
	public static void getEnemyStrengthAt(float Strength1, float Strength2, GameData data, PlayerID player, Territory ourTerr,
				boolean sea, boolean contiguous, boolean tFirst)
	{
		Collection<Unit> seaUnits = new ArrayList<Unit>();
		int rDist=0;
		Collection<Unit> airUnits = new ArrayList<Unit>();
		Collection<Unit> landUnits = new ArrayList<Unit>();
		float thisStrength = 0.0F;
		int sDist = 3;
		if (!sea)
			sDist = 2;

		Collection <Territory> nearNeighbors = data.getMap().getNeighbors(ourTerr, sDist);

		CompositeMatch<Unit> seaUnit = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data), Matches.UnitIsSea);
		CompositeMatch<Unit> airUnit = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data),	Matches.UnitIsAir);
		CompositeMatch<Unit> landUnit = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data), Matches.UnitIsLand);
		Strength1 = 0.0F;
		Strength2 = 0.0F;

		for (Territory t: nearNeighbors)
		{
			if (!Matches.isTerritoryEnemy(player, data).match(t) && !t.isWater())
				continue;
			if (contiguous)
			{
				if (t.isWater() && sea)
				{
					rDist = data.getMap().getWaterDistance(ourTerr, t);
					seaUnits = t.getUnits().getMatches(seaUnit);
					airUnits = t.getUnits().getMatches(airUnit);
					thisStrength = strength(seaUnits, true, true, tFirst) + allairstrength(airUnits, true);
				}
				else if (!t.isWater() && !sea)
				{
					rDist = data.getMap().getLandDistance(ourTerr, t);
					landUnits = t.getUnits().getMatches(landUnit);
					airUnits = t.getUnits().getMatches(airUnit);
					thisStrength = strength(landUnits, true, false, tFirst) + allairstrength(airUnits, true);
				}
				else
					continue;
			}
			else
			{
				rDist = data.getMap().getDistance(ourTerr, t);
				if (t.isWater() && sea)
				{
					seaUnits = t.getUnits().getMatches(seaUnit);
					airUnits = t.getUnits().getMatches(airUnit);
					thisStrength = strength(seaUnits, true, true, tFirst) + allairstrength(airUnits, true);
				}
				else if (!t.isWater())
				{
					landUnits = t.getUnits().getMatches(landUnit);
					airUnits = t.getUnits().getMatches(airUnit);
					thisStrength = strength(landUnits, true, false, tFirst) + allairstrength(airUnits, true);
				}
			}

			if (rDist == 0 || rDist == 1)
				Strength1 += thisStrength;
			if (rDist >= 0 && rDist <=2)
				Strength2 += thisStrength;
			thisStrength = 0.0F;
			rDist = 0;
		}
	}

	/**
	 * Gets the neighbors which are exactly a certain # of territories away (distance)
	 * Removes the inner circle neighbors
	 * neutral - whether to include neutral countries
	 */
    public static List<Territory> getExactNeighbors(Territory territory, int distance, GameData data, boolean neutral)
	{
		if(distance < 0)
			throw new IllegalArgumentException("Distance must be positive not:" + distance);

		List<Territory> startClone = new ArrayList<Territory>();
		if(distance == 0)
			return startClone;

		List<Territory> startX = new ArrayList<Territory>();
		Set<Territory> start = data.getMap().getNeighbors(territory);
		startClone.addAll(start);
		if(distance == 1)
			return startClone;
		for (int i=2; i<=distance; i++)
		{
			Set<Territory> start2 = data.getMap().getNeighbors(territory, i);
			startX.addAll(start2);
			startX.remove(startClone);
			startClone.clear();
			startClone.addAll(startX);
			startX.clear();
		}
		startClone.remove(territory);
		if (!neutral && Properties.getNeutralsImpassable(data))
		{
			for (Territory t : startX)
			{
			    if (TerritoryIsImpassableToLandUnits.match(t))
			       startX.remove(t);
		    }
		}
		return startClone;

	}

    /**
     * Does this Route contain water anywhere
     * Differs from route.crossesWater in that it checks the beginning and end
     * @param testRoute
     * @return - true if water does not exist...false if water does exist
     */
	public static boolean RouteHasNoWater(Route testRoute)
	{//simply...does the route contain a water territory
		int routeLength = testRoute.getLength();
		boolean nowater = true;
		for (int i=0; i<routeLength; i++)
		{
			Territory t = testRoute.getTerritories().get(i);
			if (t.isWater())
				nowater = false;
		}
		return nowater;
	}

	/**
	 * Look for an available sea Territory to place sea Units
	 * if other owned sea units exist, place them with these units
	 * Otherwise, look for the location which is least likely to get them killed
	 * 
	 * @param landTerr - factory territory
	 * @param tFirst - can transports be killed during battle
	 * 
	 * Should be modified to include the list of units which will be dropped (for strength measurement)
	 */
	public static Territory findASeaTerritoryToPlaceOn(Territory landTerr, float eStrength, GameData data, PlayerID player, boolean tFirst)
	{
		CompositeMatch<Territory> ourSeaTerr = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasUnitsOwnedBy(player));
		CompositeMatch<Unit> seaUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsSea);
		CompositeMatch<Unit> airUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsAir);
		CompositeMatch<Unit> seaAirUnit = new CompositeMatchOr<Unit>(seaUnit, airUnit);
		Territory seaPlaceAt = null;
		Territory xPlace = null;
        if (landTerr == null)
        	return seaPlaceAt;
		Set<Territory> seaNeighbors = data.getMap().getNeighbors(landTerr, ourSeaTerr);
		float xMinStrength = 0.0F, minStrength = 1000.0F, fStrength = 0.0F, ourStrength = 0.0F;
		for (Territory t: seaNeighbors) //give preference to territory with units
		{
			fStrength = getStrengthOfPotentialAttackers(t, data, player, tFirst, true);
			ourStrength = strength(t.getUnits().getMatches(seaAirUnit), false, true, tFirst);
			if ((fStrength - ourStrength) < minStrength)
			{
				seaPlaceAt = t;
				minStrength = fStrength - ourStrength;
			}
		}
		if (seaPlaceAt == null)
		{
			Set<Territory> seaNeighbors2 = data.getMap().getNeighbors(landTerr, Matches.TerritoryIsWater);
			for (Territory t: seaNeighbors2) //find Terr away from enemy units
			{
				fStrength = getStrengthOfPotentialAttackers(t, data, player, tFirst, true);
				ourStrength = strength(t.getUnits().getMatches(seaAirUnit), false, true, tFirst);
				if (t.getUnits().someMatch(Matches.enemyUnit(player, data)))
				{
					xPlace = t; //try to avoid Territories with enemy Units
					xMinStrength = fStrength - ourStrength;
					continue;
				}
				if ((fStrength - ourStrength) < minStrength)
				{
					seaPlaceAt = t;
					minStrength = fStrength - ourStrength;
				}
			}
		}
		if (seaPlaceAt == null)
		{
			eStrength = xMinStrength;
			seaPlaceAt = xPlace; //this will be null if there are no water territories
		}
		else
			eStrength = minStrength;
		if (minStrength == 1000.0F)
			eStrength = 0.0F;
		return seaPlaceAt;
	}

	/**
	 * Invite transports to bring units to this location
	 * 
	 * @param noncombat - is this in noncombat
	 * @param target - Land Territory needing units
	 * @param remainingStrengthNeeded - how many units we needed moved to this location
	 * @param unitsAlreadyMoved - List of Units which is not available for further movement
	 */
	public static void inviteTransports(boolean noncombat, Territory target, float remainingStrengthNeeded, Collection<Unit> unitsAlreadyMoved, 
							List<Collection<Unit>> moveUnits, List<Route> moveRoutes, GameData data, PlayerID player)
	{ //needs a check for remainingStrength
        TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();
		CompositeMatch<Unit> escortUnit1 = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsSub);
		CompositeMatch<Unit> escortUnit2 = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsDestroyer);
		CompositeMatch<Unit> escortUnit3 = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsTwoHit);
		CompositeMatch<Unit> escortUnits = new CompositeMatchOr<Unit>(escortUnit1, escortUnit2, escortUnit3);
		CompositeMatch<Unit> transportingUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsTransport, Matches.unitIsOwnedBy(player), StrongAI.Transporting, StrongAI.HasntMoved);
		Set<Territory> tCopy = data.getMap().getNeighbors(target, 3);
		List<Territory> testCapNeighbors = new ArrayList<Territory>(tCopy);
		List<Territory> waterNeighbors = new ArrayList<Territory>();
		for (Territory waterCheck : tCopy)
		{
			if (!waterCheck.isWater())
				testCapNeighbors.remove(waterCheck);
			else
			{
				if (data.getMap().getDistance(target, waterCheck) == 1)
					waterNeighbors.add(waterCheck);
				if (Matches.territoryHasOwnedTransportingUnits(player).match(waterCheck))
				{
					int xminDist = 0;
					Territory waterDest = SUtils.getClosestWaterTerr(target, waterCheck, xminDist, data, player);
					Route sRoute = getMaxSeaRoute(data, waterCheck, waterDest, player);
					if (sRoute != null && sRoute.getLength() <= 2)
					{
						List<Unit> tranUnits = waterCheck.getUnits().getMatches(transportingUnit);
						List<Unit> escorts = waterCheck.getUnits().getMatches(escortUnits);
						
						List<Unit> allUnits = new ArrayList<Unit>();
						List<Unit> xloadedUnits = new ArrayList<Unit>();
						for (Unit xTran : tranUnits)
						{
							Collection<Unit> loadOne = tracker.transporting(xTran);
							xloadedUnits.addAll(loadOne);
						}
						allUnits.addAll(tranUnits);
						allUnits.addAll(escorts);
						allUnits.addAll(xloadedUnits);
						moveUnits.add(allUnits);
						moveRoutes.add(sRoute);
						unitsAlreadyMoved.addAll(allUnits);
					}
				}
			}
		}

	}
	
	/**
	 * Territory to which we want airplanes (maybe for an attack)
	 * if the target is on water, give preference to water based planes
	 * 
	 * @param noncombat
	 * @param fightersOnly - ignore anything that cannot land on AC
	 * @param target  - target territory
	 * @param remainingStrengthNeeded - use to determine how many to bring
	 * @param unitsAlreadyMoved - Units not available for further movement
	 */
	public static void invitePlaneAttack(boolean noncombat, boolean fightersOnly, Territory target, float remainingStrengthNeeded, Collection<Unit> unitsAlreadyMoved,
							List<Collection<Unit>> moveUnits, List<Route> moveRoutes, GameData data, PlayerID player)
	{
        CompositeMatch<Unit> airUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsAir);
        CompositeMatch<Unit> fighterUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanLandOnCarrier);
        CompositeMatch<Unit> carrierUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsCarrier);
		List<Territory> planeTerr = findOurPlanes(target, data, player);
		List<Territory> planeOnWater = new ArrayList<Territory>(planeTerr);
		for (Territory qP : planeTerr)
		{
			if (Matches.TerritoryIsLand.match(qP))
				planeOnWater.remove(qP);
		}
		int availSpace = 0;
		List<Unit> ACUnits = target.getUnits().getMatches(carrierUnit);
		List<Unit> fightersOnAC = target.getUnits().getMatches(fighterUnit);
		boolean isWater = target.isWater();
		if (isWater)
		{
			if (noncombat)
				availSpace = ACUnits.size()*2 - fightersOnAC.size();
			for (Territory owned : planeOnWater) //make sure that these planes are not already involved in an attack
			{
				if (noncombat && availSpace <= 0)
					continue;
				List<Unit> tmpUnits2 = new ArrayList<Unit>();
				if (remainingStrengthNeeded > 0.0 && (!Matches.territoryHasEnemyUnits(player, data).match(owned) || noncombat))
				{
					Route thisRoute = data.getMap().getRoute(owned, target, Matches.TerritoryIsNotImpassable);
					if (thisRoute == null)
						continue;
					int rDist = thisRoute.getLength();
					List<Unit> allAirUnits = owned.getUnits().getMatches(fighterUnit);
					for (Unit u2 : allAirUnits)
					{
						if (noncombat == availSpace > 0)
						{
							if (MoveValidator.hasEnoughMovement(u2, rDist) && !unitsAlreadyMoved.contains(u2) && remainingStrengthNeeded > 0.0)
							{
								boolean canLand = airUnitIsLandable(u2, owned, target, player, data);
								if (noncombat && fightersOnly &&  availSpace > 0)
									canLand = true;
								if (canLand)
								{
									remainingStrengthNeeded -= airstrength(u2, true);
									tmpUnits2.add(u2);
									if (noncombat)
										availSpace--;
								}
							}
						}
					}
					if (tmpUnits2.size() > 0)
					{
						moveRoutes.add(thisRoute);
						moveUnits.add(tmpUnits2);
						unitsAlreadyMoved.addAll(tmpUnits2);
					}
				}
			}
			planeTerr.removeAll(planeOnWater);
		}
		for (Territory owned : planeTerr) //make sure that these planes are not already involved in an attack
		{
			if (noncombat && isWater && availSpace <= 0)
				continue;
			List<Unit> tmpUnits2 = new ArrayList<Unit>();
			if (remainingStrengthNeeded > 0.0 && !Matches.territoryHasEnemyUnits(player, data).match(owned))
			{
				Route thisRoute = data.getMap().getRoute(owned, target, Matches.TerritoryIsNotImpassable);
				if (thisRoute == null)
					continue;
				int rDist = thisRoute.getLength();
				List<Unit> allAirUnits = new ArrayList<Unit>();
				if (fightersOnly)
					allAirUnits.addAll(owned.getUnits().getMatches(fighterUnit));
				else
					allAirUnits=owned.getUnits().getMatches(airUnit);
				for (Unit u2 : allAirUnits)
				{
					if (MoveValidator.hasEnoughMovement(u2, rDist) && !unitsAlreadyMoved.contains(u2))
					{
						boolean canLand = airUnitIsLandable(u2, owned, target, player, data);
						if (noncombat && !isWater)
							canLand = true;
						else if (noncombat && fightersOnly &&  availSpace > 0)
							canLand = true;
						if (canLand && remainingStrengthNeeded > 0.0)
						{
							remainingStrengthNeeded -= airstrength(u2, true);
							tmpUnits2.add(u2);
							if (noncombat && fightersOnly && isWater)
							{
								availSpace--;
							}
						}
					}
				}
				if (tmpUnits2.size() > 0)
				{
					moveRoutes.add(thisRoute);
					moveUnits.add(tmpUnits2);
					unitsAlreadyMoved.addAll(tmpUnits2);
				}
			}
		}
	}

	/**
	 * Look for possible blitzing units
	 * Currently restricts the territory to one to which there are no existing enemy units as neighbors unless forced
	 * Use forced to get units to blitz no matter what
	 * Should be modified to compare strength of source territory and its neighbors
	 */
	public static void inviteBlitzAttack(boolean nonCombat, Territory enemy, float remainingStrengthNeeded, Collection<Unit> unitsAlreadyMoved,
								List<Collection<Unit>> moveUnits, List<Route> moveRoutes, GameData data, PlayerID player, boolean attacking, boolean forced)
	{//Blitz through owned into enemy
		CompositeMatch<Unit> blitzUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanBlitz);
		CompositeMatch<Territory> alliedAndNotWater = new CompositeMatchAnd<Territory>(Matches.isTerritoryAllied(player, data), Matches.TerritoryIsLand);
		CompositeMatch<Territory> noEnemyUnitsAndNotWater = new CompositeMatchAnd<Territory>(Matches.territoryHasNoEnemyUnits(player, data), Matches.TerritoryIsLand);
		
 	    //Are there blitzable units available?
		List<Territory> blitzFrom = getExactNeighbors(enemy, 2, data, false);
		List<Territory> blitzCopy = new ArrayList<Territory>(blitzFrom);
		Route tRoute = null;
		for (Territory t : blitzCopy)
		{	
			if (nonCombat)
				tRoute = data.getMap().getRoute(t, enemy, alliedAndNotWater);
			else
				tRoute = data.getMap().getRoute(t, enemy, noEnemyUnitsAndNotWater);
			if (tRoute == null || tRoute.getLength()>2)
				blitzFrom.remove(t);
		}

		List<Unit> blitzUnits = new ArrayList<Unit>();
		if (forced) //if a route is available, bring in the units no matter what
		{
			for (Territory blitzTerr : blitzFrom)
			{
				List<Unit> tmpBlitz = new ArrayList<Unit>();
				blitzUnits.addAll(blitzTerr.getUnits().getMatches(blitzUnit));
				Route blitzRoute = data.getMap().getRoute(blitzTerr, enemy, noEnemyUnitsAndNotWater);
				if (blitzRoute != null)
				{
					for (Unit blitzer : blitzUnits)
					{
						if (remainingStrengthNeeded > 0.0F)
						{
							remainingStrengthNeeded -= uStrength(blitzer, attacking, false, false);
							tmpBlitz.add(blitzer);
						}
					}
					moveUnits.add(tmpBlitz);
					moveRoutes.add(blitzRoute);
					unitsAlreadyMoved.addAll(tmpBlitz);
				}
				blitzUnits.clear();
			}
		}
		else //the source territory must not have enemy Units around it
		{
			for (Territory blitzTerr : blitzFrom)
			{
				Set <Territory> badTerr = data.getMap().getNeighbors(blitzTerr, Matches.territoryHasEnemyLandUnits(player, data));
				if (badTerr.isEmpty())
				{
					blitzUnits.addAll(blitzTerr.getUnits().getMatches(blitzUnit));
					List<Unit> tmpBlitz = new ArrayList<Unit>();
					Route blitzRoute = data.getMap().getRoute(blitzTerr, enemy, noEnemyUnitsAndNotWater);
					if (blitzRoute != null)
					{
						for (Unit blitzer : blitzUnits)
						{
							if (remainingStrengthNeeded > 0.0F && !unitsAlreadyMoved.contains(blitzer))
							{
								tmpBlitz.add(blitzer);
								remainingStrengthNeeded -= uStrength(blitzer, attacking, false, false);
							}
						}
						moveRoutes.add(blitzRoute);
						moveUnits.add(tmpBlitz);
						unitsAlreadyMoved.addAll(tmpBlitz);
						blitzUnits.clear();
					}
				}
			}
		}
	}

	/**
	 * Takes a List of territories and finds the one closest to an enemy capitol by Land
	 * @param ourTerr
	 * @return Territory closest or null if none has a land route
	 */
	public static Territory closestToEnemyCapital(List<Territory> ourTerr, GameData data, PlayerID player)
	{
		List<Territory> enemyCap = getEnemyCapitals(data, player);
		int thisDist = 0, capDist = 100;
		Territory returnTerr = null;
		for (Territory checkTerr : ourTerr)
		{
			for (Territory eCap : enemyCap)
			{
				thisDist = data.getMap().getLandDistance(checkTerr, eCap);
				if (thisDist == -1)
					continue;
				if (thisDist < capDist)
				{
					capDist = thisDist;
					returnTerr = checkTerr;
				}
			}
		}
		return returnTerr;
	}
	/**
	 * Determine the enemy potential for blitzing a territory - all enemies are combined
	 * @param blitzHere - Territory expecting to be blitzed
	 * @param data
	 * @param player
	 * @return actual strength of enemy units (armor)
	 */
	public static float determineEnemyBlitzStrength(Territory blitzHere, GameData data, PlayerID player)
	{
		CompositeMatch<Unit> blitzUnit = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data), Matches.UnitCanBlitz);
		CompositeMatch<Territory> validBlitzRoute = new CompositeMatchAnd<Territory>(Matches.territoryHasNoAlliedUnits(player, data), TerritoryIsNotImpassableToLandUnits);
		
		float eStrength = 0.0F;
		List<Territory> blitzTerr = getExactNeighbors(blitzHere, 2, data, false);
		for (Territory checkBlitzTerr : blitzTerr)
		{
			Route blitzRoute = data.getMap().getRoute(checkBlitzTerr, blitzHere, validBlitzRoute);
			if (blitzRoute == null)
				continue;
			List <Unit> blitzUnits = checkBlitzTerr.getUnits().getMatches(blitzUnit);
			eStrength += strength(blitzUnits, true, false, true);
		}
		return eStrength;
	}

	/**
	 * Does water exist around this territory
	 * @param checkTerr
	 * @return true if water exists, false if it doesn't
	 */
	public static boolean isWaterAt(Territory checkTerr, GameData data)
	{
		boolean Water = false;
		Set<Territory> nearNeighbors = data.getMap().getNeighbors(checkTerr, Matches.TerritoryIsWater);
		if (nearNeighbors!=null && nearNeighbors.size() > 0)
			Water = true;
		return Water;
	}

	/**
	 * Take the mix of Production Rules and determine the best purchase set for attack, defense or transport
	 * 
	 * So much more that can be done with this...track units and try to minimize or maximize the # purchased
	 */

	public static boolean findPurchaseMix(IntegerMap<ProductionRule> bestAttack, IntegerMap<ProductionRule> bestDefense, IntegerMap<ProductionRule> bestTransport, 
											List<ProductionRule> rules, int totIPC, int maxUnits, GameData data, PlayerID player, boolean fighterPresent)
	{
		IntegerMap<String> parameters = new IntegerMap<String>();
		parameters.put("attack", 0);
		parameters.put("defense", 0);
		parameters.put("maxAttack", 0);
		parameters.put("maxDefense", 0);
		parameters.put("maxAttackCost", 100000);
		parameters.put("maxDefenseCost", 100000);
		parameters.put("totcost", 0);
		parameters.put("totUnit", 0);
		parameters.put("maxUnits", maxUnits); //never changed
		parameters.put("maxCost", totIPC); //never changed
		Iterator<ProductionRule> prodIter = rules.iterator();
		while (prodIter.hasNext())
		{
			ProductionRule rule = prodIter.next();
			bestAttack.put(rule, 0); //initialize with 0
			bestDefense.put(rule, 0);
		}
		int countNum = 1;
		int goodLoop = purchaseLoop (parameters, countNum, bestAttack, bestDefense, bestTransport, data, player, fighterPresent);
		if (goodLoop > 0 && bestAttack.size() > 0 && bestDefense.size() > 0)
			return true;
		else
			return false;
	}
	
	/**
	 * Recursive routine to determine the bestAttack and bestDefense set of purchase
	 * Expects bestAttack to already be filled with the rules
	 * @param parameters - set of parameters to be used (8 of them)
	 * @param ruleNum - which rule should the routine use
	 * @param bestAttack - list of the rules and the number to be purchased (optimized for attack)
	 * @param bestDefense - list of the rules and the number to be purchased (optimized for defense)
	 * @parma bestTransport - list of the rules and the number to be purchased (optimized for transport)
	 * @return - integer which is 1 if bestAttack has changed, 2 if bestDefense has changed, 3 if both have changed
	 */
	public static int purchaseLoop(IntegerMap<String> parameters, int ruleNum, IntegerMap<ProductionRule> bestAttack, IntegerMap<ProductionRule> bestTransport, IntegerMap<ProductionRule> bestDefense, GameData data, PlayerID player, boolean fighterPresent)
	{
		/*
		 * It is expected that this is called with a subset of possible units (i.e. just land Units or just Air Units)
		 * Routine has the potential to be very costly if the number of rules is high
		 * Computation cost is exponential with the number of rules:   maxUnits^(number of rules(i.e. different Units))
		 *              Germany on revised map has maxunits of 14 and ships size is 5 --> 14^5 potential iterations (537824)
		 *              Becomes 1.4 billion if there are 8 units
		 * intended to be self-nesting for each rule in bestAttack
		 * countMax tells us which rule we are on...it should increase each time it is passed
		 * parametersChanged tells us if the next call changed the parameters (forcing a change at this level)
		 * thisParametersChanged tells us if this routine changed parameters either way (by calculation or by return from a nested call)
		 */
        Resource key = data.getResourceList().getResource(Constants.IPCS);
		Set<ProductionRule> ruleCheck = bestAttack.keySet();
		Iterator<ProductionRule> ruleIter = ruleCheck.iterator();
		int counter = 1;
		ProductionRule rule = null;
		while (counter<= ruleNum && ruleIter.hasNext())
		{
			rule = ruleIter.next();
			counter++;
		}
		if (rule == null)
			return 0;
		Integer totAttack = parameters.getInt("attack");
		Integer totDefense = parameters.getInt("defense");
		Integer totCost =  parameters.getInt("cost");
		Integer maxCost = parameters.getInt("maxCost");
		Integer maxUnits = parameters.getInt("maxUnits");
		Integer totUnits = parameters.getInt("totUnits");
		Integer maxAttack = parameters.getInt("maxAttack");
		Integer maxDefense = parameters.getInt("maxDefense");
		Integer maxAttackCost = parameters.getInt("maxAttackCost");
		Integer maxDefenseCost = parameters.getInt("maxDefenseCost");
		int parametersChanged = 0, thisParametersChanged = 0;
		for (int i=0; i <= (maxUnits - totUnits); i++)
		{
			if (i > 0) //allow 0 so that this unit might be skipped...due to low value...consider special capabilities later
			{
				int cost = rule.getCosts().getInt(key);
				totCost += cost;
				if (totCost > maxCost)
					continue;
				UnitType x = (UnitType) rule.getResults().keySet().iterator().next();
				UnitAttachment u = UnitAttachment.get(x);
				int uAttack = u.getAttack(player);
				int uDefense = u.getDefense(player);
				//give bonus of 1 hit per 2 units and if fighters are on the capital, a bonus for carrier
				int bonusAttack = (u.isTwoHit() ? 1 : 0) + (uAttack > 0 && (i % 2)==0 ? 1 : 0) + (fighterPresent && u.getCarrierCapacity() > 0 ? 1 : 0);
				int bonusDefense = (u.isTwoHit() ? 1 : 0) + (uDefense > 0 && (i % 2)==0 ? 1 : 0) + (fighterPresent && u.getCarrierCapacity() > 0 ? 2 : 0); 
				totUnits++;
				totAttack += uAttack + bonusAttack; 
				totDefense += uDefense + bonusDefense;
			}
			if (totUnits < maxUnits && ruleIter.hasNext())
			{
				parameters.put("attack", totAttack);
				parameters.put("defense", totDefense);
				parameters.put("cost", totCost);
				parameters.put("totUnits", totUnits);
				parametersChanged = purchaseLoop(parameters, counter, bestAttack, bestDefense, bestTransport, data, player, fighterPresent);
				maxAttack = parameters.getInt("maxAttack");
				maxDefense = parameters.getInt("maxDefense");
				maxAttackCost = parameters.getInt("maxAttackCost");
				maxDefenseCost = parameters.getInt("maxDefenseCost");
			}
			if (totCost == 0)
				continue;
			if (parametersChanged > 0) //change forced by another rule
			{
				if (parametersChanged == 3)
				{
					bestAttack.put(rule, i);
					bestDefense.put(rule, i);
					thisParametersChanged = 3;
				}
				if (parametersChanged == 1 )
				{
					bestAttack.put(rule, i);
					if (thisParametersChanged == 2 || thisParametersChanged == 0)
						thisParametersChanged += 1;
				}
				if (parametersChanged == 2 )
				{
					bestDefense.put(rule, i);
					if (thisParametersChanged <=1)
						thisParametersChanged +=2;
				}
				parametersChanged = 0;
				continue;
		    }
			if ((totAttack > maxAttack) || (totAttack == maxAttack && (totCost < maxAttackCost)))
			{
				maxAttack = totAttack;
				maxAttackCost = totCost;
				parameters.put("maxAttack", maxAttack);
				parameters.put("maxAttackCost", maxAttackCost);
				bestAttack.put(rule, i);
				if (thisParametersChanged == 0 || thisParametersChanged == 2)
					thisParametersChanged += 1;
				Iterator <ProductionRule> changeIter = ruleCheck.iterator();
				ProductionRule changeThis = null;
				int countThis = 1;
				while (changeIter.hasNext()) //have to clear the rules below this rule
				{
					changeThis = changeIter.next();
					if (countThis >= counter)
						bestAttack.put(changeThis, 0);
				}
			}
			if ((totDefense > maxDefense) || (totDefense == maxDefense && (totCost < maxDefenseCost)))
			{
				maxDefense = totDefense;
				maxDefenseCost = totCost;
				parameters.put("maxDefense", maxDefense);
				parameters.put("maxDefenseCost", maxDefenseCost);
				bestDefense.put(rule, i);
				if (thisParametersChanged <=1)
					thisParametersChanged +=2;
				Iterator <ProductionRule> changeIter = ruleCheck.iterator();
				ProductionRule changeThis = null;
				int countThis = 1;
				while (changeIter.hasNext()) //have to clear the rules below this rule
				{
					changeThis = changeIter.next();
					if (countThis >= counter)
						bestDefense.put(changeThis, 0);
				}
			}
		}
		return thisParametersChanged;
	}

    public static Route getMaxSeaRoute(final GameData data, Territory start, Territory destination, final PlayerID player)
    {
    	Match<Territory> routeCond = null;
    	if (start == null || destination == null)
    	{
			Route badRoute = null;
			return badRoute;
		}
    	Set<CanalAttachment> canalAttachments = CanalAttachment.get(destination);
    	if(! canalAttachments.isEmpty()) {
    		routeCond = new CompositeMatchAnd<Territory>(
                    Matches.TerritoryIsWater,
                    Matches.territoryHasEnemyUnits(player, data).invert());
    	} else {
    		routeCond = new CompositeMatchAnd<Territory>(
                Matches.TerritoryIsWater,
                Matches.territoryHasEnemyUnits(player, data).invert(),
                passableChannel(data, player));
    	}
        Route r = data.getMap().getRoute(start, destination, routeCond);
        if(r == null)
            return null;
        if(r.getLength() > 2)
        {
           Route newRoute = new Route();
           newRoute.setStart(start);
           newRoute.add( r.getTerritories().get(1) );
           newRoute.add( r.getTerritories().get(2) );
           r = newRoute;
        }
        return r;
    }

    public static final Match<Territory> passableChannel(final GameData data, final PlayerID player)
    {
    	return new Match<Territory>()
        {
    		public boolean match(Territory o)
    		{
    			Set<CanalAttachment> canalAttachments = CanalAttachment.get(o);
    			if(canalAttachments.isEmpty())
    				return true;

    			Iterator<CanalAttachment> iter = canalAttachments.iterator();
    			while(iter.hasNext() )
    			{
    				CanalAttachment canalAttachment = iter.next();
    				if(!Match.allMatch( canalAttachment.getLandTerritories(), Matches.isTerritoryAllied(player, data)))
    					return false;
    			}
    			return true;
    		}
        };
    }

}
