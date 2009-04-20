package games.strategy.triplea.strongAI;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveValidator;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class SUtils
{
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

    public static final Match<UnitType> UnitTypeIsSub  = new Match<UnitType>()
    {
        public boolean match(UnitType obj)
        {
            UnitType type = (UnitType) obj;
            UnitAttachment ua = UnitAttachment.get(type);
            return ua.isSub();
        }
    };

    public static final Match<UnitType> UnitTypeIsBB  = new Match<UnitType>()
    {
        public boolean match(UnitType obj)
        {
            UnitType type = (UnitType) obj;
            UnitAttachment ua = UnitAttachment.get(type);
            return ua.isTwoHit();
        }
    };
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
    public static List<Territory> getNeighboringEnemyLandTerritories(GameData data, PlayerID player, boolean allied)
    {
        ArrayList<Territory> rVal = new ArrayList<Territory>();
        for(Territory t : data.getMap())
        {
            if(Matches.isTerritoryEnemy(player, data).match(t) && !t.getOwner().isNull() )
            {
				if (allied && !data.getMap().getNeighbors(t, Matches.territoryHasUnitsOwnedBy(player)).isEmpty())
                   	    rVal.add(t);
                else if(!data.getMap().getNeighbors(t, Matches.isTerritoryOwnedBy(player)).isEmpty())
                    rVal.add(t);
            }
        }
        return rVal;
    }

    public static Territory getClosestWaterTerr(Territory target, Territory source, int minDist, GameData data, PlayerID player)
    { //find the water terr surrounding target which is closest to source
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
        List<Territory> checkList = getExactNeighbors(check, 1, data);
        for(Territory t : checkList)
        {
            if(Matches.isTerritoryEnemyAndNotNuetralWater(player, data).match(t))
               rVal.add(t);
        }
        return rVal;
    }

    public static List<Territory> getNeighboringLandTerritories(GameData data, PlayerID player, Territory check)
    { //find land territories owned by allies of player which neighbor a territory
    	ArrayList<Territory> rVal = new ArrayList<Territory>();
    	List<Territory> checkList = getExactNeighbors(check, 1, data);
    	for (Territory t : checkList)
    	{
			if (Matches.isTerritoryAllied(player, data).match(t))
				rVal.add(t);
		}
		return rVal;
	}

	public static boolean doesLandExistAt(Territory t, GameData data)
	{ //simply: is this territory surrounded by water
		boolean isLand = false;
		Set<Territory> checkList = data.getMap().getNeighbors(t, Matches.TerritoryIsWater);
		if (checkList != null && checkList.size() > 0)
			isLand = true;
		return isLand;
	}

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

//	public static float calcRank(GameData data, PlayerID player, Territory eTerr, Territory myTerr)
//	{//finish later
//		// Ranking: Strength + Distance Away + Distance to Capital - 2*Value
//		List<Territory> eTerrs = new ArrayList<Territory>();
//		List<Territory> eCap = getEnemyCapitals(data, player);
//		for (Territory capTerr : eCap)
//		{
///*			PlayerID ePlaya = capTerr.getOwner();
//			List<Territory> eTerritories = data.getMap().getTerritoriesOwnedBy( ePlaya);
//			for (Territory badGuyTerr : eTerritories)
//			{
//				float rank1 = data.getMap().getLandDistance(badGuyTerr, capTerr);
//				if (rank1 == -1)
//					rank1 = data.getMap().getDistance(badGuyTerr, capTerr); //island or distant continent
////				rank1 += badGuyTerr.get
//			}
//*/		}
//		float tRank = 0.0F;
//		return tRank;
//
//	}

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

	public static boolean landRouteToEnemyCapital(Territory thisTerr, Route goRoute, GameData data, PlayerID player)
	{//is there a land route between territory and enemy
        Territory myCapital = TerritoryAttachment.getCapital(player, data);
        
        boolean routeExists = false;
        Route route = null;

	    for(PlayerID otherPlayer : data.getPlayerList().getPlayers())
        {
            Territory capitol =  TerritoryAttachment.getCapital(otherPlayer, data);
	        if(capitol != null && !data.getAllianceTracker().isAllied(player, capitol.getOwner()))
	        {
	            route = data.getMap().getLandRoute(myCapital, capitol);
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

    /* Fill a Unit List with units from the passed list
     * Continues to fill until it passes maxstrength
     */
    public static List<Unit> getUnitsUpToStrength(double maxStrength, Collection<Unit> units, boolean attacking, boolean sea)
    {
        if(strength(units, attacking, sea) < maxStrength)
            return new ArrayList<Unit>(units);

        ArrayList<Unit> rVal = new ArrayList<Unit>();

        for(Unit u : units)
        {
            rVal.add(u);
            if(strength(rVal, attacking, sea) > maxStrength)
                return rVal;
        }

        return rVal;

    }
    public static List<Territory> TerritoryOnlyPlanes(GameData data, PlayerID player)
    {
		List <Unit> airUnits = new ArrayList<Unit>();
		List <Unit> landUnits = new ArrayList<Unit>();
		List <Territory> returnTerr = new ArrayList<Territory>();
		int aUnit = 0;
		int lUnit = 0;
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

	public static Territory findNearestNonEmpty(PlayerID player, GameData data, Territory t, boolean friendly)
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
			if (t2.isWater())
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
	  	
	  	int r = 0;
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
			int ourFighters = fighters.size();
			int numFighters = ourFighters + numAlliedFighters;
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


    public static float getStrengthOfPotentialAttackers(Territory location, GameData data, PlayerID player)
    {
        float seaStrength = 0.0F, firstStrength = 0.0F, strength=0.0F, airStrength=0.0F;
		CompositeMatch<Unit> enemyPlane = new CompositeMatchAnd<Unit>(Matches.UnitIsAir, Matches.enemyUnit(player, data));
		CompositeMatch<Unit> enemyBomber = new CompositeMatchAnd<Unit>(Matches.UnitIsStrategicBomber, Matches.enemyUnit(player, data));
		
		List<Territory> checked = new ArrayList<Territory>();

        for(Territory t : data.getMap().getNeighbors(location,  location.isWater() ? Matches.TerritoryIsWater :  Matches.TerritoryIsLand))
        {
           	List<Unit> enemies = t.getUnits().getMatches(Matches.enemyUnit(player, data));
            firstStrength+= strength(enemies, true, location.isWater());
            checked.add(t);
        }
        for (Territory t2 : data.getMap().getNeighbors(location, 3))
        {
			if (!checked.contains(t2))
			{
				if (data.getMap().getDistance(t2, location) < 3) //limit fighter reach to 2 sectors
				{
					List<Unit> attackPlanes = t2.getUnits().getMatches(enemyPlane);
					airStrength += allairstrength(attackPlanes, true);
				}
				else
				{
					List<Unit> bomberPlanes = t2.getUnits().getMatches(enemyBomber);
					airStrength += allairstrength(bomberPlanes, true);
				}
			}

			if (!t2.isWater())
				continue;
			List<Unit> transports = t2.getUnits().getMatches(Matches.UnitIsTransport);
			int transNum= transports.size();
			seaStrength += transNum*3.5F;
		}

		strength = seaStrength + firstStrength + airStrength;

        return strength;
    }

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

	public static List<Territory> findCertainShips(GameData data, PlayerID player, Match<Unit> unitCondition)
	{
		//Return territories containing a certain ship..will work for land units also
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

	public static List<Territory> allOurTerritories(GameData data, PlayerID player)
	{
		//this is already written...the territories we actually own (land)...but in a List
		Collection<Territory> ours = data.getMap().getTerritoriesOwnedBy(player);
		List<Territory> ours2 = new ArrayList<Territory>();
		ours2.addAll(ours);
		return ours2;
	}

	public static List<Territory> allAlliedTerritories(GameData data, PlayerID player)
	{ //is this working???
		List<Territory> ours = new ArrayList<Territory>();
		for (Territory t : data.getMap())
			if (Matches.isTerritoryAllied(player, data).match(t))
				ours.add(t);
		ours.addAll(allOurTerritories(data, player));
		return ours;
	}


	public static List<Territory> allEnemyTerritories(GameData data, PlayerID player)
	{
		List<Territory> badGuys = new ArrayList<Territory>();
		for (Territory t : data.getMap())
			if (Matches.isTerritoryEnemy(player, data).match(t))
				badGuys.add(t);
		return badGuys;
	}

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

    public static Route findNearestMaxContaining(Territory start, Match<Territory> endCondition, Match<Territory> routeCondition, Match<Unit> unitCondition, int maxUnits, GameData data)
    {
		//CompositeMatch<Unit> someUnit = new CompositeMatchOr<Unit>(Matches.UnitIsLand, Matches.UnitIsAir, Matches.enemyUnit(player, data));
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

    public  static boolean hasLandRouteToEnemyOwnedCapitol(Territory t, PlayerID us, GameData data)
    {
        for(PlayerID player : data.getPlayerList())
        {
            Territory capitol = TerritoryAttachment.getCapital(player, data);

            if(data.getAllianceTracker().isAllied(us, capitol.getOwner()))
                continue;

            if(data.getMap().getDistance(t, capitol, Matches.TerritoryIsLand) != -1)
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

    /*
     * Determine the available strength of air units
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


// A copy of the unit strength calculator in AIUtils
// Changed to do one unit at a time
    public static float uStrength(Unit units, boolean attacking, boolean sea)
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
            //1.2 points since we can absorb a hit
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
    public static float strength(Collection<Unit> units, boolean attacking, boolean sea)
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

    public static Territory findFactoryTerritory(GameData data, PlayerID player, float risk, boolean buyfactory)
    {
		//determines a suitable Territory for a factory
		//suitable: At Least 3 IPC
		//          All Territories around it are owned
		//          Strength of Units in the Territory and 1 Territory away
		//             Is greater than the sum of all enemy Territory 2 away
		//          Territory should be closest to an enemy Capital
		
		List<Territory> owned = allOurTerritories(data, player);
		
		
		Territory minTerr = null;
		float minRisk = 1.0F;
		
		risk = 1.0F;
		
		for (Territory t: owned)
		{
			int ipcValue = TerritoryAttachment.get(t).getProduction();
			if (ipcValue <= 2 || Matches.territoryHasOwnedFactory(data, player).match(t))
				continue;
			List<Territory> weOwnAll = getNeighboringEnemyLandTerritories(data, player, t);
			if (weOwnAll.size() > 0)
				continue;
			Set <Territory> factCheck = data.getMap().getNeighbors(t, Matches.territoryHasOwnedFactory(data, player));
			if (factCheck.size()>0)
				continue;
			List<Territory> twoAway = getExactNeighbors(t, 2, data);
			boolean badIdea = false;
			for (Territory twoCheck : twoAway)
			{
				if (Matches.territoryHasEnemyUnits(player, data).match(twoCheck))
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
    		boolean sea, boolean contiguous)
    {
		//gives our sea Strength within one and two territories of ourTerr
		//defensive strength
		Collection<Unit> seaUnits = new ArrayList<Unit>();
		Collection<Unit> airUnits = new ArrayList<Unit>();
		Collection<Unit> landUnits = new ArrayList<Unit>();
		int rDist=0;
		float thisStrength = 0.0F;

		Collection <Territory> nearNeighbors = data.getMap().getNeighbors(ourTerr, 2);
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
					thisStrength = strength(seaUnits, false, true) + allairstrength(airUnits, false);
				}
				else if (!t.isWater() && !sea)
				{
					rDist = data.getMap().getLandDistance(ourTerr, t);
					landUnits = t.getUnits().getMatches(landUnit);
					airUnits = t.getUnits().getMatches(airUnit);
					thisStrength = strength(landUnits, false, false) + allairstrength(airUnits, false);
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
					thisStrength = strength(seaUnits, false, true) + allairstrength(airUnits, false);
				}
				else if (!t.isWater())
				{
					landUnits = t.getUnits().getMatches(landUnit);
					airUnits = t.getUnits().getMatches(airUnit);
					thisStrength = strength(landUnits, false, false) + allairstrength(airUnits, false);
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

	public static void getEnemyStrengthAt(float Strength1, float Strength2, GameData data, PlayerID player, Territory ourTerr,
				boolean sea, boolean contiguous)
	{
		//gives enemy Strength within one and three zones of territory if sea, one and two if land
		//attack strength
		//sea is true if this is a sea check
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
					thisStrength = strength(seaUnits, true, true) + allairstrength(airUnits, true);
				}
				else if (!t.isWater() && !sea)
				{
					rDist = data.getMap().getLandDistance(ourTerr, t);
					landUnits = t.getUnits().getMatches(landUnit);
					airUnits = t.getUnits().getMatches(airUnit);
					thisStrength = strength(landUnits, true, false) + allairstrength(airUnits, true);
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
					thisStrength = strength(seaUnits, true, true) + allairstrength(airUnits, true);
				}
				else if (!t.isWater())
				{
					landUnits = t.getUnits().getMatches(landUnit);
					airUnits = t.getUnits().getMatches(airUnit);
					thisStrength = strength(landUnits, true, false) + allairstrength(airUnits, true);
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

	@SuppressWarnings("unchecked")
    public static List<Territory> getExactNeighbors(Territory territory, int distance, GameData data)
	{
		/* This routine will get the neighbors which are exactly a certain # away
		   It removes all of the inner circle neighbors which are closer than #
		   Uses identical format the getNeighbors routines in gameMap
		   It returns a modifiable List
		*/
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
		return startClone;

	}

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

	public static Territory findASeaTerritoryToPlaceOn(Territory landTerr, GameData data, PlayerID player)
	{

		List<Territory> seaNeighbors = getExactNeighbors(landTerr, 1, data);
		Territory seaPlaceAt = null;
		Territory xPlace = null;
		CompositeMatch<Unit> seaUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsSea, Matches.UnitIsAir, Matches.unitIsOwnedBy(player));
		float mTot = 500.0F, e1 = 0.0F, e2 = 0.0F, s1 = 0.0F;
		int i=0;
		for (Territory t1 : seaNeighbors)//find a terr with other units
		{
			if (Matches.territoryHasUnitsOwnedBy(player).match(t1) && t1.isWater())
			{
				seaPlaceAt = t1;
				return seaPlaceAt;
			}
		}
		for (Territory t: seaNeighbors) //find Terr away from enemy units
		{
			if (!t.isWater() || t.getUnits().someMatch(Matches.enemyUnit(player, data)))
				continue;
			if (xPlace == null)
				xPlace = t;
			getEnemyStrengthAt(e1, e2, data, player, t, true, true);
			s1 = strength(t.getUnits().getMatches(seaUnit), false, true);
			float eTot = e1 - s1;
			if (eTot < mTot)
			{
				seaPlaceAt = t;
				mTot = eTot;
			}
			i++;
		}
		if (seaPlaceAt == null)
			seaPlaceAt = xPlace; //this will be null if there are no water territories

		return seaPlaceAt;
	}

	public static void invitePlaneAttack(Territory enemy, float remainingStrengthNeeded, Collection<Unit> unitsAlreadyMoved,
							List<Collection<Unit>> moveUnits, List<Route> moveRoutes, GameData data, PlayerID player)
	{
        CompositeMatch<Unit> airUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsAir);
		List<Territory> planeTerr = findOurPlanes(enemy, data, player);
		for (Territory owned : planeTerr) //make sure that these planes are not already involved in an attack
		{
			List<Unit> tmpUnits2 = new ArrayList<Unit>();
			if (remainingStrengthNeeded > 0.0 && !Matches.territoryHasEnemyUnits(player, data).match(owned))
			{
				Route thisRoute = data.getMap().getRoute(owned, enemy);
				int rDist = data.getMap().getDistance(owned, enemy);
				List<Unit> allAirUnits=owned.getUnits().getMatches(airUnit);
				for (Unit u2 : allAirUnits)
				{
					if (MoveValidator.hasEnoughMovement(u2, rDist) && !unitsAlreadyMoved.contains(u2))
					{
						boolean canLand = airUnitIsLandable(u2, owned, enemy, player, data);
						if (canLand && remainingStrengthNeeded > 0.0)
						{
							remainingStrengthNeeded -= airstrength(u2, true);
							tmpUnits2.add(u2);
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

	public static void inviteBlitzAttack(Territory enemy, Territory owned, float remainingStrengthNeeded, Collection<Unit> unitsAlreadyMoved,
								List<Collection<Unit>> moveUnits, List<Route> moveRoutes, GameData data, PlayerID player)
	{//Blitz through owned into enemy
		CompositeMatch<Unit> blitzUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanBlitz);
 	    //Are there blitzable units available?
 	    Set<Territory> blitzFrom = data.getMap().getNeighbors(owned, Matches.territoryHasLandUnitsOwnedBy(player));
 	    for (Territory blitzTerr : blitzFrom)
 	    {
		   List<Unit> tmpBlitz = new ArrayList<Unit>();
		   Set<Territory> badTerr = data.getMap().getNeighbors(blitzTerr, Matches.territoryHasEnemyLandUnits(player, data));
		   if (blitzTerr == enemy)
		   	   continue;
		   if (badTerr.size() == 0)
		   {
			   List<Unit> blitzUnits = blitzTerr.getUnits().getMatches(blitzUnit);
			   for (Unit blitzer : blitzUnits)
			   {
				    if (unitsAlreadyMoved.contains(blitzer))
				    	continue;
					if (remainingStrengthNeeded > 0.0F)
					{
						tmpBlitz.add(blitzer);
						remainingStrengthNeeded -= uStrength(blitzer, true, false);
					}
			   }
			   Route blitzRoute = data.getMap().getLandRoute(blitzTerr, enemy);
			   if (tmpBlitz.size() > 0)
			   {
					moveRoutes.add(blitzRoute);
					moveUnits.add(tmpBlitz);
					unitsAlreadyMoved.addAll(tmpBlitz);
			   }
		   }
		}
	}

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

	public static boolean isWaterAt(Territory checkTerr, GameData data)
	{
		boolean Water = false;
		Set<Territory> nearNeighbors = data.getMap().getNeighbors(checkTerr, Matches.TerritoryIsWater);
		if (nearNeighbors!=null && nearNeighbors.size() > 0)
			Water = true;
		return Water;
	}

}
