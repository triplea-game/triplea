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
import games.strategy.triplea.TripleAUnit;
//import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.CanalAttachment;
import games.strategy.triplea.attatchments.PlayerAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.baseAI.AbstractAI;
import games.strategy.triplea.delegate.BattleCalculator;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveValidator;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.triplea.delegate.BattleDelegate;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.InverseMatch;
import games.strategy.util.Match;
import games.strategy.util.IntegerMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Iterator;
import java.util.HashMap;
import java.util.logging.Logger;

public class SUtils
{
	
	private final static int PURCHASE_LOOP_MAX_TIME_MILLIS = 150 * 1000;
    private final static Logger s_logger = Logger.getLogger(StrongAI.class.getName());
    public final static List<Territory> EMPTY_LIST = Collections.unmodifiableList(new ArrayList<Territory>());
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
	    	float landThreat = getStrengthOfPotentialAttackers(cap, data, player, tFirst, true, null);
	    	float capStrength = strength(cap.getUnits().getMatches(Matches.alliedUnit(player, data)), false, false, tFirst) + 5.0F;
	    	if (capStrength*1.05F < landThreat) //trouble
	    	{
	    		threats.add(cap);
	    	}
	    }
	    return threats.size()>0;
	}
	
	/**
	 * Returns a List of all territories with a water neighbor
	 * 
	 * @param data
	 * @param allTerr - List of Territories
	 * @return
	 */
	public static List<Territory> stripLandLockedTerr(GameData data, List<Territory> allTerr)
	{
		List<Territory> waterTerrs = new ArrayList<Territory>(allTerr);
    	Iterator<Territory> wFIter = waterTerrs.iterator();
    	while (wFIter.hasNext())
    	{ 
    		Territory waterFact = wFIter.next();
    		if (Matches.territoryHasWaterNeighbor(data).invert().match(waterFact))
    			wFIter.remove();
    	}
    	return waterTerrs;
	}
	
	// returns all territories that are water territories.  used to remove convoy zones from places the ai will put a factory (veqryn)
	public static List<Territory> onlyWaterTerr(GameData data, List<Territory> allTerr)
	{
		List<Territory> water = new ArrayList<Territory>(allTerr);
    	Iterator<Territory> wFIter = water.iterator();
    	while (wFIter.hasNext())
    	{ 
    		Territory waterFact = wFIter.next();
    		if (!Matches.TerritoryIsWater.match(waterFact))
    			wFIter.remove();
    	}
    	return water;
	}
	
	/**
	 * Determine if a List has any bombers in it
	 * 
	 * @param units
	 * @return
	 */
	public static Collection<Territory> bomberTerrInList(List<Collection<Unit>> units, List<Route> routes)
	{
		Collection<Territory> bTerrs = new ArrayList<Territory>();
		for (int i=0; i < units.size(); i++)
		{
			Collection<Unit> checkUnits = units.get(i);
			Route checkRoute = routes.get(i);
			if (checkRoute == null || checkRoute.getEnd() == null)
				continue;
			Territory endTerr = checkRoute.getEnd();
			if (Match.someMatch(checkUnits, Matches.UnitIsStrategicBomber))
				bTerrs.add(endTerr);
		}
		return bTerrs;
			
	}
	/**
	 * Prepares a map of the strength of every enemy land Territory
	 * Includes neutral if they are attackable
	 * @param data
	 * @param player
	 * @param enemyMap
	 * @return
	 */
	public static Territory landAttackMap(GameData data, PlayerID player, HashMap<Territory, Float> enemyMap)
	{
		Territory largestTerr = null;
		List<Territory> enemyTerrs = SUtils.allEnemyTerritories(data, player);
		if (enemyTerrs.isEmpty())
			return null;
		Iterator<Territory> eTIter = enemyTerrs.iterator();
		while (eTIter.hasNext())
		{
			Territory eTerr = eTIter.next();
			if (Matches.TerritoryIsWater.match(eTerr) || Matches.TerritoryIsImpassable.match(eTerr))
				eTIter.remove();
			else
			{
				float eStrength = SUtils.strength(eTerr.getUnits().getMatches(Matches.enemyUnit(player, data)), true, false, true);
				enemyMap.put(eTerr, eStrength);
			}
		}
		SUtils.reorder(enemyTerrs, enemyMap, true);
		if(enemyTerrs.isEmpty()) {
			return null;
		}
		largestTerr = enemyTerrs.get(0);
		return largestTerr;
	}
	/**
	 * Assumes that water is passable to air units always
	 * @param data
	 * @return
	 */
	public static Match<Territory> TerritoryIsImpassableToAirUnits (final GameData data)
	{
		return new Match<Territory>()
		{
			public boolean match(Territory t)
			{
				if (Matches.TerritoryIsLand.match(t) && Matches.TerritoryIsImpassable.match(t))
					return true;
				return false;
			}
		};
	}
	public final static Match<Territory> TerritoryIsNotImpassableToAirUnits (final GameData data)
	{
		return new InverseMatch<Territory>(TerritoryIsImpassableToAirUnits(data));
	}
	
        
    /**
     * Return a list of Territories on a Continent which are allied
     * @param data
     * @param player
     * @param startTerr
     * @param contiguousTerr - actual list to be created
     * @param ignoreTerr - cannot be null (should be an empty List<Territory>) - Add Territories here to ignore them
     */
    public static void continentAlliedUnitTerr(GameData data, PlayerID player, Territory startTerr, List<Territory> contiguousTerr, List<Territory> ignoreTerr)
    {
    	Set<Territory> neighbor1 = data.getMap().getNeighbors(startTerr, Matches.TerritoryIsNotImpassableToLandUnits(player));
    	neighbor1.removeAll(contiguousTerr);
    	neighbor1.removeAll(ignoreTerr);
    	for (Territory n1 : neighbor1)
    	{
    		if (Matches.isTerritoryAllied(player, data).match(n1))
    			contiguousTerr.add(n1);
    		else
    			ignoreTerr.add(n1);
    		SUtils.continentAlliedUnitTerr(data, player, n1, contiguousTerr, ignoreTerr);
    	}
    	if (!contiguousTerr.contains(startTerr) && !ignoreTerr.contains(startTerr))
    		contiguousTerr.add(startTerr);
    }
    
    /**
     * Return a players allied strength on the entire continent
     * @param data
     * @param player
     * @param continentTerr
     * @return
     */
    public static float strengthOnContinent(GameData data, PlayerID player, Territory continentTerr)
    {
    	float continentStrength = 0.0F;
    	boolean island = !SUtils.doesLandExistAt(continentTerr, data, false); //just make sure this is really a "continent"
    	if (island)
    	{
    		if (Matches.isTerritoryAllied(player, data).match(continentTerr))
    		{
    			continentStrength += SUtils.strength(continentTerr.getUnits().getMatches(Matches.alliedUnit(player, data)), true, false, false);
    			continentStrength += 5.0F; //make sure that an empty terr doesn't get a 0.0F value
    		}
    		return continentStrength;
    			
    	}
    	
    	List<Territory> allContinentTerr = new ArrayList<Territory>();
    	List<Territory> ignoreTerr = new ArrayList<Territory>();
    	SUtils.continentAlliedUnitTerr(data, player, continentTerr, allContinentTerr, ignoreTerr);
    	for (Territory cTerr : allContinentTerr)
    		continentStrength += SUtils.strength(cTerr.getUnits().getMatches(Matches.alliedUnit(player, data)), true, false, false);
    	
    	return continentStrength;
    }
    /**
     * Remove enemy territories, territories which have no threat and no enemy land neighbor
     * @param data
     * @param player
     * @param terrList
     */
    public static void removeUnthreatenedTerritories(GameData data, PlayerID player, List<Territory> terrList)
    {
    	Iterator<Territory> tIter = terrList.iterator();
    	while (tIter.hasNext())
    	{
    		Territory checkTerr = tIter.next();
    		if (Matches.isTerritoryEnemy(player, data).match(checkTerr))
    			tIter.remove();
    		else
    		{
    			float eStrength = SUtils.getStrengthOfPotentialAttackers(checkTerr, data, player, false, false, null);
    			if (eStrength == 0.0F && Matches.territoryHasEnemyLandNeighbor(data, player).invert().match(checkTerr))
    				tIter.remove();
    		}
    	}
    }
    /**
     * Create a List which contains a series of collections each which has a different movement
     * 
     * @param returnUnits
     * @param data
     * @param player
     * @param units
     */
    public static void breakUnitsBySpeed(List<Collection<Unit>> returnUnits, GameData data, PlayerID player, List<Unit> units)
    {
    	if (units.isEmpty())
    		return;
    	int maxSpeed = MoveValidator.getMaxMovement(units);
    	List<Unit> copyOfUnits = new ArrayList<Unit>(units);
    	for (int i=maxSpeed; i>=0; i--)
    	{
    		Collection<Unit> newUnits = new ArrayList<Unit>();
    		Iterator<Unit> unitIter = copyOfUnits.iterator();
    		while (unitIter.hasNext())
    		{
    			Unit unit1 = unitIter.next();
    			if (MoveValidator.hasEnoughMovement(unit1, i))
    			{
    				newUnits.add(unit1);
    				unitIter.remove();
    			}
    		}
    		if (!newUnits.isEmpty())
    			returnUnits.add(newUnits);
    	}
    }

    /**
     * Look for a set of target Territories based from factories
     * Return Map of these with their production values
     * Islands are considered up to a distance of 5
     * @param data
     * @param player
     * @return
     */
    public static IntegerMap<Territory> targetTerritories(GameData data, PlayerID player, int tDistance)
    {
    	CompositeMatch<Unit> enemyFactory = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data), Matches.UnitIsFactory);
    	List<Territory> playerFactories = SUtils.findUnitTerr(data, player, enemyFactory);
    	IntegerMap<Territory> targetMap = new IntegerMap<Territory>();
    	int checkDist = tDistance - 1;
    	for (Territory mT : playerFactories)
    	{
    		Collection<Territory> initialGroup = data.getMap().getNeighbors(mT, tDistance);
    		for (Territory checkTerr : initialGroup)
    		{
    			if (Matches.isTerritoryEnemy(player, data).match(checkTerr) && Matches.TerritoryIsLand.match(checkTerr) && Matches.TerritoryIsNotImpassable.match(checkTerr))
    			{
    				int cDist = data.getMap().getDistance(mT, checkTerr); 
    				if (cDist < checkDist || (cDist >= checkDist && !SUtils.doesLandExistAt(checkTerr, data, false)))
    				{
    					int terrProduction = TerritoryAttachment.get(checkTerr).getProduction();
    					targetMap.put(checkTerr, terrProduction);
    				}
    			}
    		}
    	}
    	return targetMap;
    }
    /**
     * Interleave infantry and artillery/armor for loading on transports
     */
    public static List<Unit> sortTransportUnits(List<Unit> transUnits)
    {
		List<Unit> sorted = new ArrayList<Unit>();
		List<Unit> infantry = new ArrayList<Unit>();
		List<Unit> artillery = new ArrayList<Unit>();
		List<Unit> armor = new ArrayList<Unit>();
		List<Unit> others = new ArrayList<Unit>();

		for (Unit x : transUnits)
		{
			if (Matches.UnitIsArtillerySupportable.match(x))
				infantry.add(x);
			else if (Matches.UnitIsArtillery.match(x))
				artillery.add(x);
			else if (Matches.UnitCanBlitz.match(x))
				armor.add(x);
			else
				others.add(x);
		}
		int artilleryCount = artillery.size();
		int armorCount = armor.size();
		int infCount = infantry.size();
		int othersCount = others.size();
		for (int j=0; j < infCount; j++) //interleave the artillery and armor with inf
		{
			sorted.add(infantry.get(j));
			// this should be based on combined attack and defense powers, not on attachments like blitz
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
			else if (othersCount > 0)
			{
				sorted.add(others.get(othersCount-1));
				othersCount--;
			}
			
		}
		if (artilleryCount > 0)
		{
			for (int j2=0; j2 < artilleryCount; j2++)
				sorted.add(artillery.get(j2));
		}
		if (othersCount > 0)
		{
			for (int j4=0; j4 < othersCount; j4++)
				sorted.add(others.get(j4));
		}
		if (armorCount > 0)
		{
			for (int j3=0; j3 < armorCount; j3++)
				sorted.add(armor.get(j3));
		}
		return sorted;

	}
    /**
     * Generate HashMap of the costs of all players
     */
    public static HashMap<PlayerID, IntegerMap<UnitType>> getPlayerCostMap(GameData data)
    {
    	HashMap<PlayerID, IntegerMap<UnitType>> costMap = new HashMap<PlayerID, IntegerMap<UnitType>>();
    	Collection<PlayerID> playerList = data.getPlayerList().getPlayers();
    	for (PlayerID cPlayer : playerList)
    	{
    		IntegerMap<UnitType> playerCostMap = BattleCalculator.getCosts(cPlayer, data);
    		costMap.put(cPlayer, playerCostMap);
    	}
    	return costMap;
    }
  
    /**
     * Trim a list of territories to a maximum number
     * If the list has fewer entries than max number, nothing is done
     * @param xTerrList
     * @param maxNum - maxNum of entries
     */
    public static void trimTerritoryList(List<Territory> xTerrList, int maxNum)
    {
    	int totNum = xTerrList.size();
    	if (maxNum >= totNum)
    		return;
		Iterator<Territory> xIter = xTerrList.iterator();
		int maxCount = 0;
		while (xIter.hasNext())
		{
			Territory test = xIter.next();
			if (maxCount > maxNum)
				xIter.remove();
			maxCount++;
		}
    }
    
    public static int evaluateNonCombat(int numMoves, int evalDistance, HashMap<Integer, HashMap<Territory, Float>> reinforcedTerrList, 
    		HashMap<Integer, IntegerMap<Territory>> unitCountList, PlayerID player, GameData data, boolean tFirst)
    {
    	/**
    	 * All Enemy Territories - Count units within 4 spaces as: 40%, 60%, 90%, 100% x productionvalue
    	 * Enemy Factories - Add 1 to Production Value
    	 * Enemy Capital - Add another 1 to Production Value
    	 */
    	if (evalDistance > 10) //limit to 10..really is 9 because 10 will be a factor of 0
    		return -1;
    	CompositeMatch<Unit> enemyFactory = new CompositeMatchAnd<Unit>(Matches.UnitIsFactory, Matches.enemyUnit(player, data));
    	CompositeMatch<Unit> alliedFactory = new CompositeMatchAnd<Unit>(Matches.UnitIsFactory, Matches.alliedUnit(player, data));
    	int bestFit = -1;
    	float maxScore = 0.0F;
    	List<Territory> enemyCaps = SUtils.getEnemyCapitals(data, player);
    	List<Territory> enemyFactories = SUtils.findUnitTerr(data, player, enemyFactory);

/*    	List<Territory> ourFriendlyTerr = new ArrayList<Territory>();
    	List<Territory> ourEnemyTerr = new ArrayList<Territory>();
    	HashMap<Territory, Float> rankMap = SUtils.rankTerritories(data, ourFriendlyTerr, ourEnemyTerr, null, player, tFirst, false);
*/
    	List<Territory> allEnemyTerr = SUtils.allEnemyTerritories(data, player);
    	IntegerMap<Territory> productionMap = new IntegerMap<Territory>();
    	for (Territory enemy : allEnemyTerr)
    	{
    		int prodValue = TerritoryAttachment.get(enemy).getProduction();
    		if (enemyCaps.contains(enemy))
    			prodValue++;
    		if (enemyFactories.contains(enemy))
    			prodValue++;
    		prodValue++;
    		productionMap.put(enemy, prodValue);
    	}
    	Set<Integer> keySet = reinforcedTerrList.keySet();
    	s_logger.fine("Moves Available for: "+keySet);
    	for (Integer i=0; i <= numMoves -1; i++)
    	{
    		HashMap<Territory, Float> reinforcedTerr = reinforcedTerrList.get(i);
    		IntegerMap<Territory> unitCount = unitCountList.get(i);
    		Set<Territory> goTerr = reinforcedTerr.keySet();
    		float score = 0.0F;
    		for (Territory eTerr : allEnemyTerr)
    		{
    			List<Territory> eNeighbors = new ArrayList<Territory>();
    			for (int j=evalDistance; j > 0; j--)
    			{
    				eNeighbors.addAll(SUtils.getExactNeighbors(eTerr, j, player, false));
    				for (Territory eN : eNeighbors)
    				{
    					if (goTerr.contains(eN))
    						score += reinforcedTerr.get(eN)*(1.0F - (j-1)*0.20)*productionMap.getInt(eTerr);
    				}
    				eNeighbors.clear();
    			}
    		}
    		if (score > maxScore)
    		{
    			maxScore = score;
    			bestFit = i;
    		}
    	}
    	return bestFit;
    }
    
    /**
     * Determine the current TUV strength of all players
     * @return IntegerMap<PlayerID> - contains TUV of each player
     */
    public static IntegerMap<PlayerID> getPlayerTUV(GameData data)
    {
    	Collection<PlayerID> playerList = data.getPlayerList().getPlayers();
    	IntegerMap<PlayerID> TUVMap = new IntegerMap<PlayerID>();
    	for (PlayerID qSet : playerList)
    		TUVMap.put(qSet, 0); //initialize map
    	
    	HashMap<PlayerID, IntegerMap<UnitType>> costMap = getPlayerCostMap(data);
    	for (Territory allTerr : data.getMap().getTerritories())
    	{
    		for (PlayerID onePlayer : playerList)
    		{
    			CompositeMatch<Unit> nonSeaUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(onePlayer), Matches.UnitIsNotSea);
    			Collection<Unit> playerUnits = allTerr.getUnits().getMatches(nonSeaUnit);
    			IntegerMap<UnitType> uMap = SUtils.convertListToMap(playerUnits);
    			int tuv = TUVMap.getInt(onePlayer);
    			tuv += SUtils.determineTUV(uMap, costMap.get(onePlayer));
    			TUVMap.put(onePlayer, tuv);
    		}
		}
    	return TUVMap;
	}
    /**
     * Return all land Territories which are within range between startTerr & targetTerr
     * @param startTerr
     * @param targetTerr
     * @param data
     * @return
     */
    public static Collection<Territory> islandCapitalTerritories(Territory startTerr, Territory targetTerr, GameData data)
    {
    	Collection<Territory> goTerrs = new ArrayList<Territory>();
    	int maxDistance = data.getMap().getDistance(startTerr, targetTerr) + 1;
    	Set<Territory> allTerrs = data.getMap().getNeighbors(targetTerr, 5);
    	Iterator<Territory> aTIter = allTerrs.iterator();
    	while (aTIter.hasNext())
    	{
    		Territory xTerr = aTIter.next();
    		int xDistance = data.getMap().getDistance(startTerr, xTerr);
    		if (Matches.TerritoryIsLand.match(xTerr) && Matches.TerritoryIsNotImpassable.match(xTerr) && xDistance <= maxDistance)
    			goTerrs.add(xTerr);
    	}
    	return goTerrs;
    	
    }
    /**
     * Return the closest allied Territory to targetTerr
     * Creates a list of the top 3 closest points and then returns the one with the maximum # of units
     * Always returns a neighbor of startTerr if the neighbor is in the top 3
     * 
     * @param startTerr
     * @param targetTerr
     * @param data
     * @param contiguousTerritories - Set of 3 potential landing points
     * @param player
     * @param tFirst
     * @return
     */
    public static Territory closestAmphibAlliedTerritory(Territory startTerr, Territory targetTerr, List<Territory> contiguousTerritories, GameData data, PlayerID player, boolean tFirst)
    {
    	int distance = data.getMap().getDistance(startTerr, targetTerr); //raw distance
    	List<Territory> ignoreTerr = new ArrayList<Territory>();
    	SUtils.continentAlliedUnitTerr(data, player, targetTerr, contiguousTerritories, ignoreTerr);
    	if (contiguousTerritories.isEmpty())
    	{
    		s_logger.fine("Player: " + player.getName() + "; Territory must be an island: "+targetTerr.getName());
    		return null;
    	}
    	IntegerMap<Territory> unitMap = new IntegerMap<Territory>();
    	IntegerMap<Territory> distanceMap = new IntegerMap<Territory>();
    	Iterator<Territory> cIter = contiguousTerritories.iterator();
    	while (cIter.hasNext())
    	{
    		Territory checkTerr = cIter.next();
    		if (Matches.territoryHasWaterNeighbor(data).match(checkTerr))
    		{
    			int checkDist = data.getMap().getDistance(startTerr, checkTerr) + data.getMap().getLandDistance(checkTerr, targetTerr);
    			int unitCount = checkTerr.getUnits().countMatches(Matches.alliedUnit(player, data));
    			unitMap.put(checkTerr, unitCount);
    			distanceMap.put(checkTerr, checkDist);
    		}
    		else
    			cIter.remove();
    	}
    	SUtils.reorder(contiguousTerritories, distanceMap, false);
    	SUtils.trimTerritoryList(contiguousTerritories, 3); //look at the top 3
    	boolean isWaterNeighbor = false;
    	Territory goTerr = null;
    	Iterator<Territory> checkIter = contiguousTerritories.iterator();
    	while (checkIter.hasNext() && !isWaterNeighbor)
    	{
    		Territory checkTerr2 = checkIter.next();
    		Set<Territory> waterNeighbors = data.getMap().getNeighbors(checkTerr2, Matches.TerritoryIsWater);
    		isWaterNeighbor = waterNeighbors.contains(startTerr);
    		if (isWaterNeighbor)
    			goTerr = checkTerr2;
    	}
    	if (goTerr == null)
    	{
    		SUtils.reorder(contiguousTerritories, unitMap, true);
    		if (!contiguousTerritories.isEmpty())
    			goTerr = contiguousTerritories.get(0);
    	}
    	return goTerr;
    }
    /**
     * Find enemyCapital closest to startTerr
     * @param startTerr
     * @param data
     * @param player
     * @return
     */
    public static Territory closestEnemyCapital(Territory startTerr, GameData data, PlayerID player)
    {
		List<Territory> eCapitols = SUtils.getEnemyCapitals(data, player);
		int maxDistance = 100;
		Territory goCap = null;
		if (eCapitols.isEmpty())
			return goCap;
		for (Territory eCap : eCapitols)
		{
			int newDist = data.getMap().getDistance(startTerr, eCap);
			if (newDist <= maxDistance)
			{
				goCap = eCap;
				maxDistance = newDist;
			}
		}
		return goCap;
    }

    public static boolean calculateTUVDifference(Territory eTerr, Collection<Unit> invasionUnits, Collection<Unit> defenderUnits, HashMap<PlayerID, IntegerMap<UnitType>> costMap, PlayerID player, GameData data, boolean aggressive, boolean subRestricted)
    {
    	int evaluationFactor = -5;
    	if (aggressive)
    		evaluationFactor = -2;
    	IntegerMap<UnitType> myCostMap = costMap.get(player);
    	
		PlayerID ePlayer = eTerr.getOwner();
		IntegerMap<UnitType> playerCostMap = costMap.get(ePlayer);
		int eTUV = (Matches.TerritoryIsNeutral.match(eTerr)) ? 0 : 
			BattleCalculator.getTUV(defenderUnits, ePlayer, playerCostMap, data);
		
		int myTUV = BattleCalculator.getTUV(invasionUnits, myCostMap);
		IntegerMap<UnitType> myAttackUnits = convertListToMap(invasionUnits);
		IntegerMap<UnitType> defenseUnits = convertListToMap(defenderUnits);
		s_logger.fine("Territory: "+eTerr.getName()+"; myTUV: "+myTUV+"; EnemyTUV: "+ eTUV);
		boolean weWin = quickBattleEstimator(myAttackUnits, defenseUnits, player, ePlayer, false, subRestricted);
		int myNewTUV = determineTUV(myAttackUnits, myCostMap);
		IntegerMap<UnitType> eCostMap = costMap.get(ePlayer);
		int eNewTUV = (Matches.TerritoryIsNeutral.match(eTerr)) ? 0 : determineTUV(defenseUnits, eCostMap);
		
		int production = TerritoryAttachment.get(eTerr).getProduction();
		if(Matches.TerritoryIsNeutral.match(eTerr))
			production *= 3;
		
		int myTUVLost = (myTUV - myNewTUV) - (weWin ? production : 0);
		int eTUVLost = eTUV - eNewTUV;
		s_logger.fine("Territory: "+eTerr.getName()+"; myTUV: "+myNewTUV+"; EnemyTUV: "+ eNewTUV+"; My TUV Lost: "+ myTUVLost+"; Enemy TUV Lost: "+ eTUVLost);
		s_logger.fine("Aggressive: "+ aggressive+"; Evaluation Factor: "+evaluationFactor);
		if (weWin && (myTUVLost <= (eTUVLost + (7 + evaluationFactor))))
			return true;
		else if (myTUVLost < (eTUVLost + evaluationFactor)) 
			return true;
		return false;

    }
    public static int determineTUV(IntegerMap<UnitType> unitList, IntegerMap<UnitType> unitCost)
    {
    	int totalValue = 0;
    	Set<UnitType> uTypes = unitList.keySet();
    	for (UnitType uType : uTypes)
    		totalValue += unitList.getInt(uType)*unitCost.getInt(uType);
    	return totalValue;
    }

    /**
     * All the territories that border one of our territories
     */

    public static List<Territory> getNeighboringEnemyLandTerritories(GameData data, PlayerID player)
    {
        ArrayList<Territory> rVal = new ArrayList<Territory>();
        for(Territory t : data.getMap())
        {
            if(Matches.isTerritoryEnemy(player, data).match(t) && Matches.TerritoryIsLand.match(t) && Matches.TerritoryIsNotImpassable.match(t))
            {
                if(!data.getMap().getNeighbors(t, Matches.isTerritoryOwnedBy(player)).isEmpty())
                    rVal.add(t);
            }
        }
        return rVal;
    }
    
    /**
     * All allied Territories which have a Land Enemy Neighbor
     * @neutral - include neutral territories
     * @allied - include allied territories
     * return - List of territories
     */
    public static List<Territory> getTerritoriesWithEnemyNeighbor(GameData data, PlayerID player, boolean allied, boolean neutral)
    {
    	List<Territory> ourTerr = new ArrayList<Territory>();
    	List<Territory> enemyLandTerr = SUtils.allEnemyTerritories(data, player);
    	if (!neutral)
    	{
    		Iterator<Territory> eIter = enemyLandTerr.iterator();
    		while (eIter.hasNext())
    		{
    			Territory checkTerr = eIter.next();
    			if (Matches.TerritoryIsNeutral.match(checkTerr))
    				eIter.remove();
    		}
    	}
    	Iterator<Territory> eIter = enemyLandTerr.iterator();
    	while (eIter.hasNext())
    	{
    		Territory enemy = eIter.next();
    		if (SUtils.doesLandExistAt(enemy, data, false))
    		{
    			List<Territory> newTerrs = new ArrayList<Territory>();
    			if (allied)
    				newTerrs.addAll(SUtils.getNeighboringLandTerritories(data, player, enemy));
    			else
    				newTerrs.addAll(data.getMap().getNeighbors(enemy, Matches.isTerritoryOwnedBy(player)));
    			for (Territory nT : newTerrs)
    			{
    				if (!ourTerr.contains(nT))
    					ourTerr.add(nT);
    			}
    		}
    	}
    	return ourTerr;
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
        CompositeMatch<Territory> enemyLand =new CompositeMatchAnd<Territory>(Matches.isTerritoryEnemy(player, data), Matches.TerritoryIsNotImpassable, Matches.TerritoryIsLand); 
        for(Territory t : data.getMap().getTerritories())
        {
            if(enemyLand.match(t))
            {
				if (allied)
				{
					if (!data.getMap().getNeighbors(t, Matches.isTerritoryAllied(player, data)).isEmpty())
                   	    rVal.add(t);
                }
                else if(!data.getMap().getNeighbors(t, Matches.isTerritoryOwnedBy(player)).isEmpty())
                    rVal.add(t);
            }
        }
        return rVal;
    }
    
    /**
     * 
     * All neutral territories that border our terr or allied terr
     * 
     * boolean allied - search for neutral near allied territories, not just owned
     * 
     */
    public static List<Territory> getNeighboringNeutralLandTerritories(GameData data, PlayerID player, boolean allied)
    {        
        ArrayList<Territory> rVal = new ArrayList<Territory>();
        for(Territory t : data.getMap())
        {
            if(Matches.isTerritoryFreeNeutral(data).match(t) && !t.isWater())
            {
            	TerritoryAttachment ta = TerritoryAttachment.get(t);
            	if(ta == null)
            		continue;
            	
            	// There is no immediate gain from attacking a neutral country without production,
            	// so they are not included.  On the other hand, sometimes conquering those neutral countries
            	// might, on rare occasions, give you a strategic advantage on the next turn.
            	if(ta.getProduction() == 0 || ta.isImpassible())
                    continue;
            	
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
    public static Territory getClosestWaterTerr(Territory target, Territory source, GameData data, PlayerID player, boolean allowEnemy)
    {
    	CompositeMatch<Territory> waterCond = null;
    	if (allowEnemy)
    		waterCond = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater);
    	else
    		waterCond = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasNoEnemyUnits(player, data));
		Set<Territory> waterTerr = data.getMap().getNeighbors(target, waterCond);
		Territory result = null;
		int minDist = 0;
		if (waterTerr.size() == 0)
		{
			minDist=0;
			return result;
		}
		else if (waterTerr.contains(source))
		{
			minDist = 0;
			return source;
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

    /*
     * Find the safest Water Territory next to a Territory
     * Ignore our units for now TODO: Look at the greatest potential of units moving
     * Returns closest Terr when more than one is 0.0F
     */
    public static Territory getSafestWaterTerr(Territory target, Territory source, List<Territory> ignoreTerr, GameData data, PlayerID player, boolean allowEnemy, boolean tFirst)
    {
    	CompositeMatch<Territory> waterCond = null;
    	CompositeMatch<Unit> alliedSeaUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsNotLand, Matches.alliedUnit(player, data));
    	if (allowEnemy)
    		waterCond = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater);
    	else
    		waterCond = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasNoEnemyUnits(player, data));
		Set<Territory> waterTerr = data.getMap().getNeighbors(target, waterCond);
		Territory result = null;
		if (waterTerr.size() == 0)
			return result;

		HashMap<Territory, Float> waterStrength = new HashMap<Territory, Float>();
		float eStrength = 0.0F, ourStrength = 0.0F;
		for (Territory xWaterTerr : waterTerr)
		{
			eStrength = getStrengthOfPotentialAttackers(xWaterTerr, data, player, tFirst, false, ignoreTerr);
			if (ignoreTerr == null || !ignoreTerr.contains(xWaterTerr))
				eStrength += strength(xWaterTerr.getUnits().getMatches(Matches.enemyUnit(player, data)), true, true, tFirst);
			eStrength -= SUtils.strength(xWaterTerr.getUnits().getMatches(alliedSeaUnit), false, true, tFirst);
			if (xWaterTerr == source && eStrength <= 0.0F)
				return xWaterTerr;
			waterStrength.put(xWaterTerr, -eStrength);
		}
		List<Territory> waterTerrList = new ArrayList<Territory>(waterTerr);
		reorder(waterTerrList, waterStrength, true);
		float maxStrength = -10000.0F;
		List<Territory> safeTerrList = new ArrayList<Territory>();
		for (Territory checkTerr : waterTerrList)
		{
			eStrength = waterStrength.get(checkTerr);
			if (eStrength >= 0.0F)
				safeTerrList.add(checkTerr);
			if (eStrength > maxStrength)
			{
				maxStrength = eStrength;
				result = checkTerr;
			}
		}
		if (safeTerrList.size() > 1 && source != null)
		{
			int minDist = 0;
			for (Territory safeTerr : safeTerrList)
			{
				int thisDist = data.getMap().getWaterDistance(source, safeTerr);
				if (thisDist < minDist)
				{
					minDist = thisDist;
					result = safeTerr;
				}
			}
		}
		if (source == null && result != null && waterStrength.get(result) < 0.0F)
			return null;
		return result;
	}
    
    /**
     * Build a list of territories that *could be* used by the enemy for blitzing
     * Doesn't worry about whether blocking units are there or not
     * @param checkTerr - Territory which enemy would blitz
     * @param data
     * @param player
     * @return
     */
    public static List<Territory> possibleBlitzTerritories(Territory checkTerr, GameData data, PlayerID player)
    {
    	CompositeMatch<Unit> blitzUnit = new CompositeMatchAnd<Unit>(Matches.UnitCanBlitz, Matches.enemyUnit(player, data));
    	List<Territory> blitzTerr = new ArrayList<Territory>();
    	List<Territory> twoTerr = SUtils.getExactNeighbors(checkTerr, 2, player, false);
    	List<Territory> oneTerr = SUtils.getExactNeighbors(checkTerr, 1, player, false);
    	for (Territory blitzFrom : twoTerr)
    	{
    		List<Unit> blitzUnits = blitzFrom.getUnits().getMatches(blitzUnit);
    		if (blitzUnits.isEmpty())
    			continue;
    		List<Territory> blitzNeighbors = SUtils.getExactNeighbors(blitzFrom, 1, player, false);
    		for (Territory blitzCheck : blitzNeighbors)
    		{
    			if (oneTerr.contains(blitzCheck))
    				blitzTerr.add(blitzCheck);
    		}
    	}
    	return blitzTerr;
    }

    /**
     * All the territories that border a certain territory
     */
    public static List<Territory> getNeighboringEnemyLandTerritories(GameData data, PlayerID player, Territory check)
    {
        List<Territory> rVal = new ArrayList<Territory>();
        List<Territory> checkList = getExactNeighbors(check, 1, player, false);
        for(Territory t : checkList)
        {
            if (Matches.isTerritoryEnemy(player, data).match(t) && Matches.TerritoryIsNotImpassable.match(t) && Matches.TerritoryIsLand.match(t))
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
    	List<Territory> checkList = getExactNeighbors(check, 1, player, false);
    	for (Territory t : checkList)
    	{
			if (Matches.isTerritoryAllied(player, data).match(t) && Matches.TerritoryIsNotImpassableToLandUnits(player).match(t))
				rVal.add(t);
		}
		return rVal;
	}

    /**
     * Does this territory have any land? i.e. it isn't an island
     * @neutral - count an attackable neutral as a land neighbor
     * @return boolean (true if a land territory is a neighbor to t
     */
	public static boolean doesLandExistAt(Territory t, GameData data, boolean neutral)
	{ //simply: is this territory surrounded by water
		boolean isLand = false;
		Set<Territory> checkList = data.getMap().getNeighbors(t, Matches.TerritoryIsLand);
		if (!neutral)
		{
			Iterator<Territory> nIter = checkList.iterator();
			while (nIter.hasNext())
			{
				Territory nTerr = nIter.next();
				if (Matches.TerritoryIsNeutral.match(nTerr))
					nIter.remove();
			}
		}
		for (Territory checkNeutral : checkList)
		{
			if (Matches.TerritoryIsNotImpassable.match(checkNeutral))
				isLand=true;
		}
		return isLand;
	}

	/*
	 * distance to the closest enemy
	 * just utilises findNearest
	 */
	public static int distanceToEnemy(Territory t, GameData data, PlayerID player, boolean sea) {
		
		// note: neutrals are enemies
		// also note: if sea, you are finding distance to enemy sea units, not to enemy land over sea
		if (Matches.TerritoryIsImpassable.match(t))
			return 0;
		Match<Territory> endCondition;
		Match<Territory> routeCondition;
		if(sea)
		{
			endCondition = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasEnemyUnits(player, data));
			routeCondition = Matches.TerritoryIsWater;
		}
		else 
		{
			endCondition = new CompositeMatchAnd<Territory>(Matches.isTerritoryEnemy(player, data),Matches.TerritoryIsNotImpassable);
			routeCondition = new CompositeMatchAnd<Territory>(Matches.isTerritoryAllied(player, data),Matches.TerritoryIsNotImpassable, Matches.TerritoryIsLand);
		}
		return findNearest(t, endCondition, routeCondition, data).getLength();
	}
	
	
	/**
	 * Recursive routine for finding the distance to an enemy
	 * @param t
	 * @param beenThere - list of territories already checked
	 * @param data
	 * @param player
	 * @return int of distance to enemy
	 */
	/*
	public static int distanceToEnemyOld(Territory t, List<Territory> beenThere, GameData data, PlayerID player, boolean sea)
	{ //find the distance to the closest enemy land territory by recursion
	  //if no enemy territory can be found...it returns 0
		if (Matches.TerritoryIsImpassable.match(t))
			return 0;
		CompositeMatch<Territory> enemyWater = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasEnemyUnits(player, data));
		CompositeMatch<Territory> noEnemyAndWater = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasNoEnemyUnits(player, data));
		List<Territory> thisTerr = new ArrayList<Territory>();
		if (sea)
			thisTerr.addAll(data.getMap().getNeighbors(t, enemyWater));
		else
			thisTerr.addAll(getNeighboringEnemyLandTerritories(data, player, t));
		beenThere.add(t);
		int newDist = 1;

		if (thisTerr.size() == 0) //searches more territories
		{
			List<Territory> newTerrList = new ArrayList<Territory>();
			if (sea)
				newTerrList.addAll(data.getMap().getNeighbors(t, noEnemyAndWater));
			else
				newTerrList.addAll(getNeighboringLandTerritories(data, player, t));
			
			newTerrList.removeAll(beenThere);
			beenThere.addAll(newTerrList);
			if (newTerrList.size() == 0)
				newDist = 0;
			else
			{
				int minDist = 100;
				for (Territory t2 : newTerrList)
				{
					int aDist = distanceToEnemy(t2, new ArrayList<Territory>(beenThere), data, player, sea);
					if (aDist < minDist && aDist > 0)
						minDist = aDist;
				}
				if (minDist < 100)
					newDist += minDist;
				else
					newDist = 0;
			}
		}
		return newDist;
	}
*/
	/**
	 * List containing the enemy Capitals
	 */
	public static List<Territory> getEnemyCapitals(GameData data, PlayerID player)
	{ //generate a list of all enemy capitals
		List<Territory> enemyCapitals = new ArrayList<Territory>();
		List<PlayerID> ePlayers = SUtils.getEnemyPlayers(data, player);
	    for(PlayerID otherPlayer : ePlayers)
        {
            Territory capitol =  TerritoryAttachment.getCapital(otherPlayer, data);
	        if(capitol != null && Matches.TerritoryIsNotImpassable.match(capitol)) //Mongolia is listed as capitol of China in AA50 games
	        	enemyCapitals.add(capitol);

	    }
	    return enemyCapitals;

	}
	
	/**
	 * Returns a list of all enemy players
	 * @param data
	 * @param player
	 * @return List<PlayerID> enemyPlayers
	 */
	public static List<PlayerID> getEnemyPlayers(GameData data, PlayerID player)
	{
		List<PlayerID> enemyPlayers = new ArrayList<PlayerID>();
		for (PlayerID players : data.getPlayerList().getPlayers())
		{
			if (!data.getAllianceTracker().isAllied(player, players))
				enemyPlayers.add(players);
		}
		return enemyPlayers;
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
	            route = data.getMap().getRoute(thisTerr, capitol, Matches.TerritoryIsNotImpassableToLandUnits(player));
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
		CompositeMatch<Unit> landUnit = new CompositeMatchAnd<Unit>(Matches.isUnitAllied(player, data), Matches.UnitIsLand, Matches.UnitIsNotAA, Matches.UnitIsNotFactory);
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
		CompositeMatch<Unit> landUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsLand, Matches.UnitIsNotAA, Matches.UnitIsNotFactory);
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
    public static float getStrengthOfPotentialAttackers(Territory location, GameData data, PlayerID player, boolean tFirst, boolean ignoreOnlyPlanes, List<Territory> ignoreTerr)
    {
        TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();
        PlayerID ePlayer = null;
		List<PlayerID> qID = getEnemyPlayers(data, player);
		HashMap<PlayerID, Float> ePAttackMap = new HashMap<PlayerID, Float>();
		boolean doIgnoreTerritories = ignoreTerr != null;
		Iterator<PlayerID> playerIter = qID.iterator();
		if (location == null)
			return -1000.0F;
    	boolean nonTransportsInAttack = false;
    	boolean onWater = location.isWater();
    	if (!onWater)
    		nonTransportsInAttack = true;

		Set<Territory> waterTerr = data.getMap().getNeighbors(location, Matches.TerritoryIsWater);
		while (playerIter.hasNext())
		{
	    	float seaStrength = 0.0F, firstStrength = 0.0F, secondStrength = 0.0F, blitzStrength = 0.0F, strength=0.0F, airStrength=0.0F;
			ePlayer = playerIter.next();
			CompositeMatch<Unit> enemyPlane = new CompositeMatchAnd<Unit>(Matches.UnitIsAir, Matches.unitIsOwnedBy(ePlayer), Matches.UnitIsNotStatic(ePlayer));
			CompositeMatch<Unit> enemyBomber = new CompositeMatchAnd<Unit>(Matches.UnitIsStrategicBomber, Matches.unitIsOwnedBy(ePlayer), Matches.UnitIsNotStatic(ePlayer));
			CompositeMatch<Unit> enemyTransport = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(ePlayer), Matches.UnitIsSea, Matches.UnitIsTransport, Matches.UnitIsNotStatic(ePlayer));
			CompositeMatch<Unit> enemyShip = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(ePlayer), Matches.UnitIsSea, Matches.UnitIsNotStatic(ePlayer));
			CompositeMatch<Unit> enemyTransportable = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(ePlayer), Matches.UnitCanBeTransported, Matches.UnitIsNotAA, Matches.UnitIsNotStatic(ePlayer));
			CompositeMatch<Unit> aTransport = new CompositeMatchAnd<Unit>(Matches.UnitIsSea, Matches.UnitIsTransport, Matches.UnitIsNotStatic(ePlayer));
			//Find Maximum plane movement for this player
			List<Territory> eFTerrs = SUtils.findUnitTerr(data, ePlayer, Matches.UnitCanLandOnCarrier);
			List<Territory> eBTerrs = SUtils.findUnitTerr(data, ePlayer, Matches.UnitIsStrategicBomber);
			int maxFighterDistance = 0, maxBomberDistance = 0;
			for (Territory eFTerr : eFTerrs)
			{
				List<Unit> eFUnits = eFTerr.getUnits().getMatches(Matches.UnitCanLandOnCarrier);
				maxFighterDistance = Math.max(maxFighterDistance, MoveValidator.getMaxMovement(eFUnits));
			}
			for (Territory eBTerr : eBTerrs)
			{
				List<Unit> eBUnits = eBTerr.getUnits().getMatches(Matches.UnitIsStrategicBomber);
				maxBomberDistance = Math.max(maxBomberDistance, MoveValidator.getMaxMovement(eBUnits));
			}
			maxFighterDistance--; //must be able to land...we will miss fighters who have a Carrier that can reach same sea zone...C'est la vie
			if (maxFighterDistance < 0)
				maxFighterDistance = 0;
			maxBomberDistance--; //must be able to land...won't miss anything here...unless special bombers that can land on carrier per above
			if (maxBomberDistance < 0)
				maxBomberDistance = 0;
			List<Territory> eTTerrs = SUtils.findUnitTerr(data, ePlayer, aTransport);
			int maxTransportDistance = 0;
			for (Territory eTTerr : eTTerrs)
			{
				List<Unit> eTUnits = eTTerr.getUnits().getMatches(aTransport);
				maxTransportDistance = Math.max(maxTransportDistance, MoveValidator.getMaxMovement(eTUnits));
			}
			List<Unit> alreadyLoaded = new ArrayList<Unit>();
			List<Route> blitzTerrRoutes = new ArrayList<Route>();
			List<Territory> checked = new ArrayList<Territory>();
			List<Unit> enemyWaterUnits = new ArrayList<Unit>();
			for(Territory t : data.getMap().getNeighbors(location,  onWater ? Matches.TerritoryIsWater :  Matches.TerritoryIsLand))
			{
				if (doIgnoreTerritories && ignoreTerr.contains(t))
					continue;
				List<Unit> enemies = t.getUnits().getMatches(Matches.unitIsOwnedBy(ePlayer));
				enemyWaterUnits.addAll(enemies);
				firstStrength+= strength(enemies, true, onWater, tFirst);
				checked.add(t);
			}
			if (Matches.TerritoryIsLand.match(location))
			{
				blitzStrength = determineEnemyBlitzStrength(location, blitzTerrRoutes, null, data, ePlayer);
			}
			else //get ships attack strength
			{
				for (int i=2; i<=3; i++)
				{
					List<Territory> moreTerr = getExactNeighbors(location, i, player, false);
					if (doIgnoreTerritories)
						moreTerr.removeAll(ignoreTerr);
					for (Territory shipTerr : moreTerr)
					{
						if (!shipTerr.isWater())
							continue;
						else if (ePlayer == null)
							continue;
						List <Unit> moreEnemies = shipTerr.getUnits().getMatches(enemyShip);
						if (moreEnemies.isEmpty())
							continue;
						int maxShipDistance = MoveValidator.getLeastMovement(moreEnemies);
						Route seaRoute = getMaxSeaRoute(data, shipTerr, location, ePlayer, true, maxShipDistance);
        			
						if (seaRoute == null || seaRoute.getEnd() == null || seaRoute.getEnd() != location) //no valid route...ignore ships
							continue;
						enemyWaterUnits.addAll(moreEnemies);
						secondStrength += strength(moreEnemies, true, true, tFirst);
					}
				}
			}
			List<Unit> allEnemyPlanes = new ArrayList<Unit>();
			for (Territory t2 : data.getMap().getNeighbors(location, maxFighterDistance)) //get air strength
			{
				if (doIgnoreTerritories && ignoreTerr.contains(t2))
					continue;
				if (!checked.contains(t2) && t2.getUnits().someMatch(Matches.unitIsOwnedBy(ePlayer)))
				{
					int airDist = data.getMap().getDistance(t2, location, SUtils.TerritoryIsNotImpassableToAirUnits(data)); 
					if (airDist <= maxFighterDistance)
					{
						List<Unit> attackPlanes = t2.getUnits().getMatches(enemyPlane);
						Iterator<Unit> planeIter = attackPlanes.iterator();
						while (planeIter.hasNext())
						{
							Unit ePlane = planeIter.next();
							if (!SUtils.airUnitIsLandable(ePlane, t2, location, ePlayer, data) && !SUtils.airUnitIsLandableOnCarrier(ePlane, t2, location, ePlayer, data))
								planeIter.remove();
						}
						allEnemyPlanes.addAll(attackPlanes);
						airStrength += allairstrength(attackPlanes, true);
					}
				}
			}
			for (Territory t3 : data.getMap().getNeighbors(location, maxBomberDistance))
			{
				if (doIgnoreTerritories && ignoreTerr.contains(t3))
					continue;
				if (!checked.contains(t3) && t3.getUnits().someMatch(Matches.unitIsOwnedBy(ePlayer)))
				{
					int airDist = data.getMap().getDistance(t3, location, SUtils.TerritoryIsNotImpassableToAirUnits(data));
					if (airDist <= maxBomberDistance)
					{
						List<Unit> bomberPlanes = t3.getUnits().getMatches(enemyBomber);
						bomberPlanes.removeAll(allEnemyPlanes);
						Iterator<Unit> bombIter = bomberPlanes.iterator();
						while (bombIter.hasNext())
						{
							Unit bPlane = bombIter.next();
							if (!SUtils.airUnitIsLandable(bPlane, t3, location, ePlayer, data))
								bombIter.remove();
						}
						allEnemyPlanes.addAll(bomberPlanes);
						airStrength += allairstrength(bomberPlanes, true);
					}
				}
			}
			if (Matches.territoryHasWaterNeighbor(data).match(location) && Matches.TerritoryIsLand.match(location))
			{
				for (Territory t4 : data.getMap().getNeighbors(location, maxTransportDistance))
				{
					if (!t4.isWater())
						continue;
					boolean transportsCounted = false;
					Iterator<Territory> iterTerr = waterTerr.iterator();
					while (!transportsCounted && iterTerr.hasNext())
					{
						Territory waterCheck = iterTerr.next();
						if (ePlayer == null)
							continue;
						List<Unit> transports = t4.getUnits().getMatches(enemyTransport);
						if (transports.isEmpty())
							continue;
						if(!t4.equals(waterCheck))
						{
							Route seaRoute = getMaxSeaRoute(data, t4, waterCheck, ePlayer, true, maxTransportDistance);
							if (seaRoute == null || seaRoute.getEnd()== null || seaRoute.getEnd() != waterCheck) //no valid route...ignore ships
								continue;
						}
						List<Unit> loadedUnits = new ArrayList<Unit>();
						int availInf = 0, availOther = 0;
						for (Unit xTrans : transports)
						{
							Collection<Unit> thisTransUnits = tracker.transporting(xTrans);
							if (thisTransUnits == null)
							{
								availInf += 2;
								availOther += 1;
								continue;
							}
							else
							{
								int Inf = 2, Other = 1;
								for (Unit checkUnit : thisTransUnits)
								{
									if (Matches.UnitIsInfantry.match(checkUnit))
										Inf--;
									if (Matches.UnitIsNotInfantry.match(checkUnit))
									{
										Inf--;
										Other--;
									}
									loadedUnits.add(checkUnit);
								}
								availInf += Inf;
								availOther += Other;
							}
						}
						Set<Territory> transNeighbors = data.getMap().getNeighbors(t4, Matches.isTerritoryAllied(ePlayer, data));
						for (Territory xN : transNeighbors)
						{
							List<Unit> aTransUnits = xN.getUnits().getMatches(enemyTransportable);
							aTransUnits.removeAll(alreadyLoaded);
							List<Unit> availTransUnits = SUtils.sortTransportUnits(aTransUnits);
							for (Unit aTUnit : availTransUnits)
							{
								if (availInf > 0 && Matches.UnitIsInfantry.match(aTUnit))
								{
									availInf--;
									loadedUnits.add(aTUnit);
									alreadyLoaded.add(aTUnit);
								}
								if (availInf > 0 && availOther > 0 && Matches.UnitIsNotInfantry.match(aTUnit))
								{
									availInf--;
									availOther--;
									loadedUnits.add(aTUnit);
									alreadyLoaded.add(aTUnit);
								}
							}
						}
						seaStrength += strength(loadedUnits, true, false, tFirst);
						transportsCounted = true;
					}
				}
			}
			strength = seaStrength + blitzStrength + firstStrength + secondStrength;
			if (!ignoreOnlyPlanes || strength > 0.0F)
				strength += airStrength;
			if (onWater)
			{
				Iterator<Unit> eWaterIter = enemyWaterUnits.iterator();
				while (eWaterIter.hasNext() && !nonTransportsInAttack)
				{
					if (Matches.UnitIsNotTransport.match(eWaterIter.next()))
						nonTransportsInAttack=true;
				}
			}
			if (!nonTransportsInAttack)
				strength = 0.0F;
			ePAttackMap.put(ePlayer, strength);
		}
		float maxStrength = 0.0F;
		for (PlayerID xP : qID)
		{
			if (ePAttackMap.get(xP) > maxStrength)
			{
				ePlayer = xP;
				maxStrength = ePAttackMap.get(xP);
			}
		}
		for (PlayerID xP : qID)
		{
			if (ePlayer != xP)
				maxStrength += ePAttackMap.get(xP)*0.40F; //give 40% of other players...this is will affect a lot of decisions by AI
		}
        return maxStrength;
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
		for (Territory t : data.getMap().getTerritories())
			if (Matches.isTerritoryAllied(player, data).match(t))
				ours.add(t);
		return ours;
	}
	/**
	 * All Enemy Territories in a modifiable List
	 */
	public static List<Territory> allEnemyTerritories(GameData data, PlayerID player)
	{
		List<Territory> badGuys = new ArrayList<Territory>();
		for (Territory t : data.getMap().getTerritories())
			if (Matches.isTerritoryEnemyAndNotNuetralWater(player, data).match(t))
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
		Match<Territory> canGo = new CompositeMatchOr<Territory>(endCondition,routeCondition);
		Map<Territory,Territory> visited = new HashMap<Territory,Territory>();
		Queue<Territory> q = new LinkedList<Territory>();
		List<Territory> route = new ArrayList<Territory>();
		q.add(start);
		Territory current = null;
		visited.put(start,null);
		while(!q.isEmpty()){
	        current = q.remove();
	        if(endCondition.match(current))
	            break;
	        else
	        {
	        	for(Territory neighbor : data.getMap().getNeighbors(current, canGo)) {
	                if(!visited.containsKey(neighbor)){
	                    q.add(neighbor);
	                    visited.put(neighbor, current);
	                }
	            }
	        }
	    }
		
		if (current == null || !endCondition.match(current)){
	        return null;
	    }
	    for(Territory t = current; t != null; t = visited.get(t))
	    {
	        route.add(t);
	    }
	    Collections.reverse(route);

	    return new Route(route);
    }
	
	/**
	 * Find the Route to the nearest Territory
	 * @param start - starting territory
	 * @param endCondition - condition for the ending Territory
	 * @param routeCondition - condition for each Territory in Route
	 * @param data
	 * @return
	 */
	/*
    public static Route findNearestOld(Territory start, Match<Territory> endCondition, Match<Territory> routeCondition, GameData data)
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
    */
    /**
     * Find Route from start to a Territory having endCondition which has a maximum of a certain set of Units (unitCondition)
     * @param start - initial territory
     * @param endCondition - final territory must match this
     * @param routeCondition - all territories on route must match this
     * @param unitCondition - units must match this
     * @param maxUnits - how many units were found there
     * @return - Route to the endCondition
     */
    public static Route findNearestMaxContaining(Territory start, Match<Territory> endCondition, Match<Territory> routeCondition, final Match<Unit> unitCondition, final int maxUnits, GameData data)
    {
    	Match<Territory>condition = new Match<Territory>()
        {
            public boolean match(Territory t)
            {
                return t.getUnits().getMatches( unitCondition).size()>maxUnits;
            }
        };
    	return findNearest(start, new CompositeMatchAnd<Territory>(endCondition,condition), routeCondition, data);
    	/*
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
        */
    }

    public static Route findNearestNotEmpty(Territory start, Match<Territory> endCondition, Match<Territory> routeCondition, GameData data)
    {
    	
    	Route r = findNearest(start, new CompositeMatchAnd<Territory>(endCondition,Matches.TerritoryIsEmpty.invert()), routeCondition, data);
    	if( r == null)
    		r = findNearest(start, endCondition, routeCondition, data);
    	return r;
    	/*
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
        // error here should be != null
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
        */
    }
    /**
     * true or false...does a land route exist from territory to any enemy owned capitol?
     */
    public  static boolean hasLandRouteToEnemyOwnedCapitol(Territory t, PlayerID player, GameData data)
    {
        for(PlayerID ePlayer : data.getPlayerList().getPlayers())
        {
            Territory capitol = TerritoryAttachment.getCapital(ePlayer, data);

            if(capitol == null || data.getAllianceTracker().isAllied(player, capitol.getOwner()))
                continue;

            if(data.getMap().getDistance(t, capitol, Matches.TerritoryIsNotImpassableToLandUnits(player)) != -1)
            {
                return true;
            }

        }
        return false;

    }
    public static boolean airUnitIsLandableOnCarrier(Unit u, Territory source, Territory target, PlayerID player, GameData data)
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
		int rDist = data.getMap().getDistance(source, target, SUtils.TerritoryIsNotImpassableToAirUnits(data));
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
		CompositeMatch<Unit> ourCarrier = new CompositeMatchAnd<Unit>(Matches.UnitIsCarrier, Matches.unitIsOwnedBy(player));
		for (Territory t : data.getMap().getTerritories())
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
			airstrength += 1.00F;
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
				airstrength += 1.00F;
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
            strength +=  1.00F;
            //the number of pips on the dice
            if(attacking)
                strength += unitAttatchment.getAttack(u.getOwner())*(unitAttatchment.isTwoHit() ? 2 : 1)*unitAttatchment.getAttackRolls(u.getOwner());
            else
                strength += unitAttatchment.getDefense(u.getOwner())*(unitAttatchment.isTwoHit() ? 2 : 1);
            if(attacking)
            {
                if(unitAttatchment.getAttack(u.getOwner()) == 0)
                    strength -= 0.50F; //adjusted KDM
            }
            if (unitAttatchment.getTransportCapacity()>0 && !transportsFirst)
                strength -=0.50F; //only allow transport to have 0.35 on defense; none on attack
        }
        else if (unitAttatchment.isAir() & sea) //we can count airplanes in sea attack
        {
        	strength += 1.00F;
        	if (attacking)
        		strength += unitAttatchment.getAttack(u.getOwner())*unitAttatchment.getAttackRolls(u.getOwner());
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
            	int unitAttack = unitAttatchment.getAttack(u.getOwner());
				//BB = 6.0; AC=2.0/4.0; SUB=3.0; DS=4.0; TR=0.50/2.0; F=4.0/5.0; B=5.0/2.0;
                strength += 1.00F; //played with this value a good bit

                if(attacking)
                    strength += unitAttack*(unitAttatchment.isTwoHit() ? 2 : 1);
                else
                    strength += unitAttatchment.getDefense(u.getOwner())*(unitAttatchment.isTwoHit() ? 2 : 1);

                if(attacking)
                {
                    if(unitAttack == 0)
                        strength -= 0.50F;
                }
                if (unitAttack == 0 && unitAttatchment.getTransportCapacity() > 0 && !transportsFirst)
                    strength -=0.50F; //only allow transport to have 0.35 on defense; none on attack
            }
            else if (unitAttatchment.isAir() == sea)
            {
				strength += 1.00F;
                if(attacking)
                    strength += unitAttatchment.getAttack(u.getOwner())*unitAttatchment.getAttackRolls(u.getOwner());
                else
                    strength += unitAttatchment.getDefense(u.getOwner());
			}

        }

        if(attacking && !sea)
        {
            int art = Match.countMatches(units, Matches.UnitIsArtillery);
            int artSupport = Match.countMatches(units, Matches.UnitIsArtillerySupportable);
            strength += Math.min(art, artSupport);
        }

        return strength;
    }

	/**
	 * determines a suitable Territory for a factory
	 * suitable: At Least 2 PU
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
    public static Territory findFactoryTerritory(GameData data, PlayerID player, float risk, boolean buyfactory, boolean onWater)
    {
    	final BattleDelegate delegate = DelegateFinder.battleDelegate(data);
    	CompositeMatch<Territory> enemyNoWater = new CompositeMatchAnd<Territory>(Matches.TerritoryIsNotImpassableToLandUnits(player), Matches.isTerritoryEnemyAndNotNuetralWater(player, data));
    	CompositeMatch<Territory> alliedNoWater = new CompositeMatchAnd<Territory>(Matches.TerritoryIsNotImpassableToLandUnits(player), Matches.isTerritoryAllied(player, data));
		List<Territory> owned = allOurTerritories(data, player);
		List<Territory> existingFactories = SUtils.findCertainShips(data, player, Matches.UnitIsFactory);
		owned.removeAll(existingFactories);
		List<Territory> isWaterConvoy = SUtils.onlyWaterTerr(data, owned);
		owned.removeAll(isWaterConvoy);
		List<Territory> cloneFactTerritories = new ArrayList<Territory>(owned);
        for (Territory deleteBad : cloneFactTerritories) // removed just conquered territories (for combat before purchase games) (veqryn)
        {
			if (delegate.getBattleTracker().wasConquered(deleteBad))
				owned.remove(deleteBad);
		}
		Collections.shuffle(owned);
		if (onWater)
		{
			List<Territory> waterOwned = SUtils.stripLandLockedTerr(data, owned);
			owned.retainAll(waterOwned);
			if (owned.isEmpty())
				return null;
			IntegerMap<Territory> terrProd = new IntegerMap<Territory>();
			for (Territory prodTerr : owned)
			{
				// sorting territories to have ones with greatest production and closeness to enemy first (by land, then by sea) (veqryn)
				int territoryValue =0;
				if (hasLandRouteToEnemyOwnedCapitol(prodTerr, player, data))
					territoryValue += 3;
				if (findNearest(prodTerr, Matches.territoryHasEnemyFactory(data, player), Matches.TerritoryIsNotImpassableToLandUnits(player), data) != null)
					territoryValue += 1;
				int dist = distanceToEnemy(prodTerr, data, player, false);
				if (dist != 0)
					territoryValue += 10 - dist;
				else
				{
					dist = distanceToEnemy(prodTerr, data, player, true);
					territoryValue += 8 - dist;
				}
				territoryValue += 4 * TerritoryAttachment.get(prodTerr).getProduction();
				List<Territory> weOwnAll = getNeighboringEnemyLandTerritories(data, player, prodTerr);
				List<Territory> isWater = SUtils.onlyWaterTerr(data, weOwnAll);
				weOwnAll.removeAll(isWater);
		    	Iterator<Territory> weOwnAllIter = weOwnAll.iterator();
		    	while (weOwnAllIter.hasNext())
		    	{ 
		    		Territory tempFact = weOwnAllIter.next();
		    		if (Matches.TerritoryIsNeutral.match(tempFact) || Matches.TerritoryIsImpassable.match(tempFact))
		    			weOwnAllIter.remove();
		    	}
				territoryValue -= 15 * weOwnAll.size();
				if (TerritoryAttachment.get(prodTerr).getProduction() < 2)
					territoryValue -= 100;
				if (TerritoryAttachment.get(prodTerr).getProduction() < 1)
					territoryValue -= 100;
				terrProd.put(prodTerr, territoryValue);
			}
			SUtils.reorder(owned, terrProd, true);
			return owned.get(0); //TODO: cleanup this to buy the best possible location
		}
		
		// TODO: we need to put the territories in an order that is a mix between high production and closeness to the enemy
		// because currently this entire factory location picker just picks the first good territory it finds. (veqryn)
		IntegerMap<Territory> terrProd = new IntegerMap<Territory>();
		for (Territory prodTerr : owned)
		{
			// sorting territories to have ones with greatest production and closeness to enemy first (by land, then by sea) (veqryn)
			int territoryValue =0;
			if (hasLandRouteToEnemyOwnedCapitol(prodTerr, player, data))
				territoryValue += 3;
			if (findNearest(prodTerr, Matches.territoryHasEnemyFactory(data, player), Matches.TerritoryIsNotImpassableToLandUnits(player), data) != null)
				territoryValue += 1;
			int dist = distanceToEnemy(prodTerr, data, player, false);
			if (dist != 0)
				territoryValue += 10 - dist;
			else
			{
				dist = distanceToEnemy(prodTerr, data, player, true);
				territoryValue += 5 - dist;
			}
			territoryValue += 4 * TerritoryAttachment.get(prodTerr).getProduction();
			terrProd.put(prodTerr, territoryValue);
		}
		SUtils.reorder(owned, terrProd, true);
		Territory minTerr = null;
		float minRisk = 1.0F;
		risk = 1.0F;
		IntegerMap<Territory> prodMap = new IntegerMap<Territory>();
		for (Territory factTerr : existingFactories)
			prodMap.put(factTerr, TerritoryAttachment.get(factTerr).getProduction());
		for (Territory t: owned)
		{
			int puValue = TerritoryAttachment.get(t).getProduction();
			if (puValue < 2 || Matches.territoryHasOwnedFactory(data, player).match(t))
				continue;
			List<Territory> weOwnAll = getNeighboringEnemyLandTerritories(data, player, t);
			List<Territory> isWater = SUtils.onlyWaterTerr(data, weOwnAll);
			weOwnAll.removeAll(isWater);
	    	Iterator<Territory> weOwnAllIter = weOwnAll.iterator();
	    	while (weOwnAllIter.hasNext())
	    	{ 
	    		Territory tempFact = weOwnAllIter.next();
	    		if (Matches.TerritoryIsNeutral.match(tempFact) || Matches.TerritoryIsImpassable.match(tempFact))
	    			weOwnAllIter.remove();
	    	}
			if (weOwnAll.size() > 0)
				continue;
			int numOnContinent = 0;
			for (Territory factTerr : existingFactories)
			{
				Route fRoute = data.getMap().getRoute(t, factTerr, Matches.TerritoryIsNotImpassableToLandUnits(player));
				if (fRoute != null && fRoute.getEnd() != null)
					numOnContinent = prodMap.getInt(factTerr);
			}
//	        This prevents purchasing a factory in a map like NWO
//			if (numOnContinent >= 6)
//				continue;
			List<Territory> twoAway = getExactNeighbors(t, 2, player, false);
			List<Territory> threeAway = getExactNeighbors(t, 3, player, false);
			List<Territory> closeAllies = SUtils.getNeighboringLandTerritories(data, player, t);
			float tStrength = strength(t.getUnits().getMatches(Matches.unitIsOwnedBy(player)), false, false, false);
			for (Territory cA : closeAllies)
			{
				tStrength += SUtils.strength(cA.getUnits().getMatches(Matches.alliedUnit(player, data)), false, false, false);
			}
			boolean badIdea = false;
			float twoCheckStrength = 0.0F, threeCheckStrength = 0.0F;
			for (Territory twoCheck : twoAway)
			{
				if (Matches.territoryHasEnemyUnits(player, data).match(twoCheck))
					twoCheckStrength += strength(twoCheck.getUnits().getMatches(Matches.enemyUnit(player, data)), true, false, false);
			}
			if (twoCheckStrength > (puValue*3.0F + tStrength))
					badIdea = true;
			/* TODO: (veqryn) this portion of the code counts naval vessels and any other enemy units 
					 within 3 spaces of the territory that will have a factory.  It only compares it to the 
					 ai's units on the territory, and does not take into account any of the ai's units in other 
					 territories nearby.  This needs to only care about LAND and AIR units and not care about SEA units.
					 And it needs to take into account friendly land and air within 2 spaces of the territory.
			*/
					 
			for (Territory threeCheck : threeAway)
			{
				if (Matches.territoryHasEnemyUnits(player, data).match(threeCheck))
				{ //only count it if it has a path
					Route d1 = data.getMap().getLandRoute(threeCheck, t);
					threeCheckStrength += strength(threeCheck.getUnits().getMatches(Matches.enemyUnit(player, data)), true, false, false);
				}
			}
			
			if ((twoCheckStrength + threeCheckStrength) > (puValue*8.0F + tStrength)*4) //take at least 2 moves to invade (veqryn multiplying friendly for now)
			{
				badIdea = true;
			}
			if (badIdea)
				continue;
			Route nearEnemyRoute = findNearest(t, enemyNoWater, alliedNoWater, data);
			Route factoryRoute = findNearest(t, Matches.territoryHasEnemyFactory(data, player), Matches.TerritoryIsNotImpassableToLandUnits(player), data);
			if (nearEnemyRoute == null || factoryRoute == null)
				continue;
			int routeLength = nearEnemyRoute.getLength();
			int factoryLength = factoryRoute.getLength();
			if (buyfactory && hasLandRouteToEnemyOwnedCapitol(t, player, data) && factoryLength <= 8) //try to keep Britain from building in SA
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

	/**
	 * Gets the neighbors which are exactly a certain # of territories away (distance)
	 * Removes the inner circle neighbors
	 * neutral - whether to include neutral countries
	 */
    public static List<Territory> getExactNeighbors(Territory territory, int distance, PlayerID player, boolean neutral)
	{
    	// This will return territories that are not impassable, but have an impassable territory in the way. (or water in the way)
    	GameData data = player.getData();
		if(distance < 0)
			throw new IllegalArgumentException("Distance must be positive not:" + distance);

		List<Territory> startClone = new ArrayList<Territory>();
		if(distance == 0)
			return startClone;

		startClone.addAll(data.getMap().getNeighbors(territory,distance));
		if(distance == 1)
			return startClone;
		
		startClone.removeAll(data.getMap().getNeighbors(territory,distance-1));
		startClone.remove(territory);
		if (!neutral || Properties.getNeutralsImpassable(data))
		{
			Iterator<Territory> t = startClone.iterator();
			while (t.hasNext())
			{
			    if (Matches.TerritoryIsNeutral.match(t.next()))
			       t.remove();
		    }
		}
		Iterator<Territory> t2 = startClone.iterator();
		while (t2.hasNext())
		{
		    if (Matches.TerritoryIsImpassable.match(t2.next()))
		       t2.remove();
	    }
		/* Code for a loop version. TODO: Find things only on a path that does not include impassables, water/land, restricted territories.
		List<Territory> startX = new ArrayList<Territory>();
		List<Territory> innerCircle = new ArrayList<Territory>();
		Set<Territory> start = data.getMap().getNeighbors(territory);
		startClone.addAll(start);
		if(distance > 1)
		{
			innerCircle.addAll(startClone);
			for (int i=2; i<=distance; i++)
			{
				innerCircle.addAll(startX);
				startX.clear();
				Set<Territory> start2 = data.getMap().getNeighbors(territory, i);
				startX.addAll(start2);
				startClone.addAll(startX);
				startClone.removeAll(innerCircle);
			}
		}
		startClone.remove(territory);
		*/
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
	public static Territory findASeaTerritoryToPlaceOn(Territory landTerr, GameData data, PlayerID player, boolean tFirst)
	{
		CompositeMatch<Territory> ourSeaTerr = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasUnitsOwnedBy(player));
		CompositeMatch<Unit> seaUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsSea);
		CompositeMatch<Unit> airUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsAir);
		CompositeMatch<Unit> seaAirUnit = new CompositeMatchOr<Unit>(seaUnit, airUnit);
		Territory seaPlaceAt = null, bestSeaPlaceAt = null;
		Territory xPlace = null;
        if (landTerr == null)
        	return seaPlaceAt;
		Set<Territory> seaNeighbors = data.getMap().getNeighbors(landTerr, ourSeaTerr);
		float eStrength = 0.0F, minStrength = 1000.0F, maxStrength = -1000.0F;
		for (Territory t: seaNeighbors) //give preference to territory with units
		{
			float enemyStrength = getStrengthOfPotentialAttackers(t, data, player, tFirst, true, null);
			float extraEnemy = SUtils.strength(t.getUnits().getMatches(Matches.enemyUnit(player, data)), true, true, tFirst);
			enemyStrength += extraEnemy;
			float ourStrength = strength(t.getUnits().getMatches(seaAirUnit), false, true, tFirst);
			float existingStrength = strength(t.getUnits().getMatches(Matches.alliedUnit(player, data)), false, true, tFirst);
			ourStrength += existingStrength;
			float strengthDiff = enemyStrength - ourStrength;
			if (strengthDiff < minStrength && ourStrength > 0.0F)
			{
				seaPlaceAt = t;
				minStrength = strengthDiff;
			}
			if (strengthDiff > maxStrength && strengthDiff < 3.0F && (ourStrength > 0.0F || existingStrength > 0.0F) )
			{
				bestSeaPlaceAt = t;
				maxStrength = strengthDiff;
			}
		}
		if (seaPlaceAt == null && bestSeaPlaceAt == null)
		{
			Set<Territory> seaNeighbors2 = data.getMap().getNeighbors(landTerr, Matches.TerritoryIsWater);
			for (Territory t: seaNeighbors2) //find Terr away from enemy units
			{
				float enemyStrength = getStrengthOfPotentialAttackers(t, data, player, tFirst, true, null);
				float ourStrength = strength(t.getUnits().getMatches(seaAirUnit), false, true, tFirst);
				if (t.getUnits().someMatch(Matches.enemyUnit(player, data)))
				{
					xPlace = t; //try to avoid Territories with enemy Units
					continue;
				}
				if ((enemyStrength - ourStrength) < minStrength)
				{
					seaPlaceAt = t;
					minStrength = enemyStrength - ourStrength;
				}
			}
		}
		if (seaPlaceAt == null && bestSeaPlaceAt == null && xPlace != null)
			seaPlaceAt = xPlace; //this will be null if there are no water territories
		if (bestSeaPlaceAt == null)
			return seaPlaceAt;
		else
			return bestSeaPlaceAt;
	}

	/*
	 * Invite escorts ships purely for bombarding territory
	 * Does not use strength method because it will add in 1.0F for each unit and the escorts cannot take a loss
	 */
	public static float inviteBBEscort(Territory goTerr, float remainingStrengthNeeded, Collection<Unit> unitsAlreadyMoved, List<Collection<Unit>> moveUnits,
			List<Route> moveRoutes, GameData data, PlayerID player)
	{
		CompositeMatch<Unit> BBUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.unitCanBombard(player), Matches.UnitIsNotStatic(player));
		float BBStrength = 0.0F;
		List<Territory> BBTerr = findOurShips(goTerr, data, player, BBUnit);
		for (Territory BBT : BBTerr)
		{
			List<Unit> BBUnits = BBT.getUnits().getMatches(BBUnit);
			BBUnits.removeAll(unitsAlreadyMoved);
			if (BBUnits.isEmpty())
				continue;
			List<Unit> BBAddUnits = new ArrayList<Unit>();
			if (BBT == goTerr)
			{
				unitsAlreadyMoved.addAll(BBUnits);
				Iterator<Unit> BBIter = BBUnits.iterator();
				while (BBIter.hasNext() && BBStrength < remainingStrengthNeeded) 
				{
					Unit BB = BBIter.next();
					UnitAttachment ua = UnitAttachment.get(BB.getType());
					BBStrength += ua.getAttack(player)*ua.getAttackRolls(player);
				}
				continue;
			}
			int BBDistance = MoveValidator.getLeastMovement(BBUnits);
			Route BBRoute = getMaxSeaRoute(data, BBT, goTerr, player, true, BBDistance);
			if (BBRoute == null || BBRoute.getEnd() == null || BBRoute.getEnd() != goTerr)
				continue;
			Iterator<Unit> BBIter = BBUnits.iterator();
			while (BBIter.hasNext() && BBStrength < remainingStrengthNeeded) 
			{
				Unit BB = BBIter.next();
				UnitAttachment ua = UnitAttachment.get(BB.getType());
				BBStrength += ua.getAttack(player)*ua.getAttackRolls(player);
				BBAddUnits.add(BB);
			}
			if (BBAddUnits.size() > 0)
			{
				moveUnits.add(BBAddUnits);
				moveRoutes.add(BBRoute);
				unitsAlreadyMoved.addAll(BBAddUnits);
			}
			
		}
		return BBStrength;
	}
	/**
	 * Invite Ship Attack to a certain sea territory (enemy)
	 * Air on a carrier will be included
	 * Transports will only be included if tFirst is true
	 * Units to be moved will be placed in moveUnits and routes in moveRoutes
	 * carrier & fighters will be moved as a single unit
	 */
	
	public static float inviteShipAttack(Territory eTerr, float remainingStrengthNeeded, Collection<Unit> unitsAlreadyMoved, 
			List<Collection<Unit>> moveUnits, List<Route> moveRoutes, GameData data, PlayerID player, boolean attacking, boolean tFirst, boolean includeTransports)
	{
		final BattleDelegate battleD = DelegateFinder.battleDelegate(data);
		CompositeMatch<Unit> ownedSeaUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsSea, Matches.UnitIsNotStatic(player));
		CompositeMatch<Unit> ownedAirUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsAir, Matches.UnitCanLandOnCarrier, Matches.UnitIsNotStatic(player));
		CompositeMatch<Unit> carrierUnit = new CompositeMatchAnd<Unit>(ownedSeaUnit, Matches.UnitIsCarrier, Matches.UnitIsNotStatic(player));
		CompositeMatch<Unit> carrierAndFighters = new CompositeMatchOr<Unit>(carrierUnit, ownedAirUnit);
		CompositeMatch<Unit> ownedSeaUnitSansTransports = new CompositeMatchAnd<Unit>(ownedSeaUnit, Matches.UnitIsNotTransport, Matches.UnitIsNotStatic(player));
		
		HashMap<Territory, Float> attackShipMap = new HashMap<Territory, Float>();
		HashMap<Territory, List<Unit>> attackUnitMap = new HashMap<Territory, List<Unit>>();
		HashMap<Territory, List<Unit>> carrierUnitMap = new HashMap<Territory, List<Unit>>();
		List<Territory> possibleShipTerr = SUtils.findCertainShips(data, player, Matches.UnitIsSea);
		Iterator<Territory> pSIter = possibleShipTerr.iterator();
		while (pSIter.hasNext())
		{//Remove if: 1) Land; 2) No Sea Units; 3) Battle Already Fought in Sea Zone
			Territory countX = pSIter.next();
			if (Matches.TerritoryIsLand.match(countX) || battleD.getBattleTracker().wasBattleFought(countX))
				pSIter.remove();
		}
		Iterator<Territory> pSIter2 = possibleShipTerr.iterator();
		while (pSIter2.hasNext())
		{
			Territory shipTerr = pSIter2.next();
			float terrStrength = 0.0F;
			List<Unit> attackShips = new ArrayList<Unit>();
			List<Unit> carrierShips = new ArrayList<Unit>();
			if (includeTransports)
				attackShips.addAll(shipTerr.getUnits().getMatches(ownedSeaUnit));
			else
				attackShips.addAll(shipTerr.getUnits().getMatches(ownedSeaUnitSansTransports));
			int maxShipDistance = 0;
			attackShips.removeAll(unitsAlreadyMoved);
			carrierShips.addAll(shipTerr.getUnits().getMatches(carrierAndFighters));
			carrierShips.removeAll(unitsAlreadyMoved);
			attackShips.removeAll(carrierShips);
			if (attackShips.isEmpty() && carrierShips.isEmpty())
			{
				pSIter2.remove();
				continue;
			}
			if (!attackShips.isEmpty())
				maxShipDistance = MoveValidator.getMaxMovement(attackShips);
			else
				maxShipDistance = MoveValidator.getMaxMovement(carrierShips);
			Route thisRoute = getMaxSeaRoute(data, shipTerr, eTerr, player, attacking, maxShipDistance);
			if (thisRoute == null || thisRoute.getEnd() != eTerr)
			{
				pSIter2.remove();
				continue;
			}
			if (carrierShips.size() > 0 && MoveValidator.hasEnoughMovement(carrierShips, thisRoute))
			{
				terrStrength += strength(carrierShips, attacking, true, tFirst);
			}
			Iterator<Unit> aSIter = attackShips.iterator();
			
			while (aSIter.hasNext())
			{
				Unit attackShip = aSIter.next();
				if (MoveValidator.hasEnoughMovement(attackShip, thisRoute))
				{
					terrStrength += uStrength(attackShip, attacking, true, tFirst);
				}
				else
					aSIter.remove();
			}
			carrierUnitMap.put(shipTerr, carrierShips);
			attackUnitMap.put(shipTerr, attackShips);
			attackShipMap.put(shipTerr, terrStrength);
		}
		reorder(possibleShipTerr, attackShipMap, false);
		//now that they are ordered, add them in whole groups
		float unitStrength = 0.0F;
		for (Territory addShipTerr : possibleShipTerr)
		{
			List<Unit> theseUnits = attackUnitMap.get(addShipTerr);
			List<Unit> otherUnits = carrierUnitMap.get(addShipTerr);
			if (theseUnits.isEmpty() && otherUnits.isEmpty())
				continue;
			int maxUnitDistance = 0;
			if (!theseUnits.isEmpty())
				maxUnitDistance = MoveValidator.getMaxMovement(theseUnits);
			else
				maxUnitDistance = MoveValidator.getMaxMovement(otherUnits);
			Route newRoute = getMaxSeaRoute(data, addShipTerr, eTerr, player, attacking, maxUnitDistance);
			if (newRoute == null || newRoute.getEnd() == null || newRoute.getEnd() != eTerr)
				continue;
			if (remainingStrengthNeeded > unitStrength)
			{
				if (!theseUnits.isEmpty())
				{
					moveUnits.add(theseUnits);
					moveRoutes.add(newRoute);
					unitsAlreadyMoved.addAll(theseUnits);
					unitStrength += strength(theseUnits, attacking, true, tFirst);
				}
				if (remainingStrengthNeeded > unitStrength && (!otherUnits.isEmpty()))
				{
					moveUnits.add(otherUnits);
					moveRoutes.add(newRoute);
					unitsAlreadyMoved.addAll(otherUnits);
					unitStrength += strength(otherUnits, attacking, true, tFirst);
				}
			}
		}
		return unitStrength;
	}

	/**
	 * Invite transports to bring units to this location
	 * 
	 * @param noncombat - is this in noncombat
	 * @param target - Land Territory needing units
	 * @param remainingStrengthNeeded - how many units we needed moved to this location
	 * @param unitsAlreadyMoved - List of Units which is not available for further movement
	 */
	public static float inviteTransports(boolean noncombat, Territory target, float remainingStrengthNeeded, Collection<Unit> unitsAlreadyMoved, 
							List<Collection<Unit>> moveUnits, List<Route> moveRoutes, GameData data, PlayerID player, boolean tFirst, boolean allowEnemy, 
							List<Territory> seaTerrAttacked)
	{ //needs a check for remainingStrength
        TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();
        CompositeMatch<Unit> airUnits = new CompositeMatchAnd<Unit>(Matches.UnitCanLandOnCarrier, Matches.unitIsOwnedBy(player), Matches.UnitIsNotStatic(player));
		CompositeMatch<Unit> escortShip = new CompositeMatchAnd<Unit>(Matches.UnitIsSea, Matches.UnitIsNotTransport, Matches.unitIsOwnedBy(player), Matches.UnitIsNotStatic(player));
		CompositeMatch<Unit> escortUnit = new CompositeMatchOr<Unit>(airUnits, escortShip);
		CompositeMatch<Unit> transportingUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsTransport, Matches.unitIsOwnedBy(player), Matches.UnitIsNotStatic(player));
		Set<Territory> tCopy = data.getMap().getNeighbors(target, 3);
		List<Territory> testCapNeighbors = new ArrayList<Territory>(tCopy);
		List<Territory> waterNeighbors = new ArrayList<Territory>();
		List<Territory> alreadyMovedFrom = new ArrayList<Territory>();
		List<Territory> myFactories = findCertainShips(data, player, Matches.UnitIsFactory);
		List<Territory> waterFactoryNeighbors = new ArrayList<Territory>();
		for (Territory myFactory : myFactories)
		{
			Set<Territory> wFN = data.getMap().getNeighbors(myFactory, Matches.TerritoryIsWater);
			waterFactoryNeighbors.addAll(wFN);
		}
		for (Territory firstWaterCheck : tCopy)
		{
			if (!firstWaterCheck.isWater())
				testCapNeighbors.remove(firstWaterCheck);
			else if (data.getMap().getDistance(target, firstWaterCheck)==1)
			{
				waterNeighbors.add(firstWaterCheck);
				testCapNeighbors.remove(firstWaterCheck);
			}
		}
		int waterSize = waterNeighbors.size();
		for (int i=0; i < waterSize-1; i++)
		{
			for (int j=i+1; j < waterSize; j++)
			{
				Territory iTerr = waterNeighbors.get(i);
				Territory jTerr = waterNeighbors.get(j);
				if (!waterFactoryNeighbors.contains(iTerr) && waterFactoryNeighbors.contains(jTerr))
				{
					waterNeighbors.remove(jTerr);
					waterNeighbors.remove(iTerr);
					waterNeighbors.add(i, jTerr);
					waterNeighbors.add(j, iTerr);
				}
			}
		}
		boolean transportsForAttack = false;
		Territory firstLocation = null;
		float unitStrength = 0.0F;
		for (Territory waterCheck : waterNeighbors)
		{
			float strengthAtTarget = getStrengthOfPotentialAttackers(waterCheck, data, player, tFirst, false, seaTerrAttacked);
			float shipStrength = 0.0F;
			if (Matches.territoryHasOwnedTransportingUnits(player).match(waterCheck))
			{
				int xminDist = 0;
				List<Unit> tUnits = new ArrayList<Unit>();
				List<Unit> tranUnits = waterCheck.getUnits().getMatches(transportingUnit);
				tranUnits.removeAll(unitsAlreadyMoved);
				List<Unit> escorts = waterCheck.getUnits().getMatches(escortUnit);
				escorts.removeAll(unitsAlreadyMoved);
				for (Unit xTran : tranUnits)
				{
					if (remainingStrengthNeeded > unitStrength)
					{
						Collection<Unit> loadOne = tracker.transporting(xTran);
						unitStrength += strength(loadOne, true, false, false);
						tUnits.add(xTran);
					}
				}
				if (tFirst)
				{
					shipStrength += strength(tUnits, false, true, tFirst);
				}
				if (shipStrength < strengthAtTarget)
				{
					for (Unit testEscort: escorts)
					{
						if (shipStrength < strengthAtTarget)
						{
							shipStrength += uStrength(testEscort, false, true, tFirst);
							tUnits.add(testEscort);
						}
					}
				}
				if (tUnits.size() > 0)
				{
					unitsAlreadyMoved.addAll(tUnits); //no actual move needed...just stay here
					transportsForAttack = true;
				}
			}
			for (Territory otherSource : testCapNeighbors)
			{
				if (alreadyMovedFrom.contains(otherSource))
					continue;
				alreadyMovedFrom.add(otherSource);
				List<Unit> tranUnits = otherSource.getUnits().getMatches(transportingUnit);
				tranUnits.removeAll(unitsAlreadyMoved);
				if (tranUnits.isEmpty())
					continue;
				int maxDistance = MoveValidator.getMaxMovement(tranUnits);
				Route sRoute = getMaxSeaRoute(data, otherSource, waterCheck, player, allowEnemy, maxDistance);
				if (sRoute == null || sRoute.getEnd() != waterCheck)
					continue;
				int newDist = sRoute.getLength();
				Iterator<Unit> tranIter = tranUnits.iterator();
				while (tranIter.hasNext())
				{
					Unit thisTran = tranIter.next();
					TripleAUnit ta = TripleAUnit.get(thisTran);
					if (ta.getMovementLeft() < newDist)
						tranIter.remove();
					else if (!tracker.isTransporting(thisTran))
						tranIter.remove();
				}
				List<Unit> escorts = otherSource.getUnits().getMatches(escortUnit);
				escorts.removeAll(unitsAlreadyMoved);
				Iterator<Unit> escortIter = escorts.iterator();
				while(escortIter.hasNext())
				{
					Unit thisEscort = escortIter.next();
					if (Matches.unitHasMoved.match(thisEscort))
						escortIter.remove();
				}
						
				List<Unit> allUnits = new ArrayList<Unit>();
				for (Unit xTran : tranUnits)
				{
					if (remainingStrengthNeeded > unitStrength)
					{
						Collection<Unit> loadOne = tracker.transporting(xTran);
						unitStrength += strength(loadOne, true, false, false);
						allUnits.add(xTran);
						allUnits.addAll(loadOne);
						if (tFirst)
							shipStrength += uStrength(xTran, false, true, tFirst);
					}
				}
				if (shipStrength < strengthAtTarget)
				{
					for (Unit eUnit : escorts)
					{
						if (shipStrength < strengthAtTarget)
						{
							shipStrength += uStrength(eUnit, false, true, tFirst);
							allUnits.add(eUnit);
						}
					}
				}
				if (allUnits.size() > 0)
				{
					transportsForAttack = true;
					moveUnits.add(allUnits);
					moveRoutes.add(sRoute);
					unitsAlreadyMoved.addAll(allUnits);
				}
			}
		}
		return unitStrength;
	}
	
	/**
	 * Territory to which we want airplanes (maybe for an attack)
	 * if the target is on water, give preference to water based planes
	 * 
	 * @param noncombat
	 * @param fightersOnly - ignore anything that cannot land on AC
	 * @param withCarrier - fighters on Carriers are accompanied by carrier
	 * @param target  - target territory
	 * @param remainingStrengthNeeded - use to determine how many to bring
	 * @param unitsAlreadyMoved - Units not available for further movement
	 */
	public static float invitePlaneAttack(boolean noncombat, boolean fightersOnly, Territory target, float remainingStrengthNeeded, Collection<Unit> unitsAlreadyMoved,
							List<Collection<Unit>> moveUnits, List<Route> moveRoutes, GameData data, PlayerID player)
	{
        CompositeMatch<Unit> airUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsAir, Matches.UnitIsNotStatic(player));
        CompositeMatch<Unit> fighterUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanLandOnCarrier, Matches.UnitIsNotStatic(player));
        CompositeMatch<Unit> carrierUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsCarrier, Matches.UnitIsNotStatic(player));
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
		float committedStrength = 0.0F;
		if (isWater)
		{
			if (noncombat)
				availSpace = ACUnits.size()*2 - fightersOnAC.size();
			for (Territory owned : planeOnWater) //make sure that these planes are not already involved in an attack
			{
				if (noncombat && availSpace <= 0)
					continue;
				List<Unit> tmpUnits2 = new ArrayList<Unit>();
				if (remainingStrengthNeeded > committedStrength && (!Matches.territoryHasEnemyUnits(player, data).match(owned) || noncombat))
				{
					Route thisRoute = data.getMap().getRoute(owned, target, Matches.TerritoryIsNotImpassable);
					if (thisRoute == null)
						continue;
					int rDist = thisRoute.getLength();
					List<Unit> allAirUnits = owned.getUnits().getMatches(fighterUnit);
					for (Unit u2 : allAirUnits)
					{
						if (u2 != null && noncombat == availSpace > 0)
						{
							if (MoveValidator.hasEnoughMovement(u2, rDist) && !unitsAlreadyMoved.contains(u2) && remainingStrengthNeeded > committedStrength)
							{
								boolean canLand = airUnitIsLandable(u2, owned, target, player, data);
								if (noncombat && fightersOnly &&  availSpace > 0)
									canLand = true;
								if (canLand)
								{
									committedStrength += airstrength(u2, true);
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
			if (remainingStrengthNeeded > committedStrength && !Matches.territoryHasEnemyUnits(player, data).match(owned))
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
					if (u2 != null && MoveValidator.hasEnoughMovement(u2, rDist) && !unitsAlreadyMoved.contains(u2))
					{
						boolean canLand = airUnitIsLandable(u2, owned, target, player, data);
						if (noncombat && !isWater)
							canLand = true;
						else if (noncombat && fightersOnly &&  availSpace > 0)
							canLand = true;
						if (canLand && remainingStrengthNeeded > committedStrength)
						{
							committedStrength += airstrength(u2, true);
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
		remainingStrengthNeeded -= committedStrength;
		return committedStrength;
	}

	/**
	 * Look for possible land based Attack units
	 * Ignores blitzing
	 * @param enemy - territory to be invaded
	 * @param remainingStrengthNeeded - total strength of units needed - stop adding when this reaches 0.0
	 */
	public static float inviteLandAttack(boolean nonCombat, Territory enemy, float remainingStrengthNeeded, Collection<Unit> unitsAlreadyMoved,
								List<Collection<Unit>> moveUnits, List<Route> moveRoutes, GameData data, PlayerID player, boolean attacking,
								boolean forced, List<Territory> alreadyAttacked)
	{
		CompositeMatch<Unit> landUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsLand, Matches.UnitIsNotAA, Matches.UnitIsNotStatic(player), Matches.UnitIsNotFactory);
		
		List<Territory> ourLandNeighbors = getNeighboringLandTerritories(data, player, enemy);
		float totStrength = 0.0F;
		int totList = ourLandNeighbors.size();
		//reorder with the terr with neighbors having fewest enemy units first
		for (int i = 0; i < totList-1; i++)
		{
			for (int j = i + 1; j < totList; j++ )
			{
				Territory iTerr = ourLandNeighbors.get(i);
				Territory jTerr = ourLandNeighbors.get(j);
				int jUnits = 0, iUnits = 0;
				Set<Territory> jTerrNeighbors = data.getMap().getNeighbors(jTerr, Matches.territoryHasEnemyUnits(player, data));
				jTerrNeighbors.removeAll(alreadyAttacked);
				Set<Territory> iTerrNeighbors = data.getMap().getNeighbors(iTerr, Matches.territoryHasEnemyUnits(player, data));
				iTerrNeighbors.removeAll(alreadyAttacked);
				for (Territory jT : jTerrNeighbors)
					jUnits += jT.getUnits().countMatches(Matches.enemyUnit(player, data));
				for (Territory iT : iTerrNeighbors)
					iUnits += iT.getUnits().countMatches(Matches.enemyUnit(player, data));
			
				if (jUnits < iUnits)
				{
					ourLandNeighbors.remove(j);
					ourLandNeighbors.remove(i);
					ourLandNeighbors.add(i, jTerr);
					ourLandNeighbors.add(j, iTerr);
				}
			}
		}
		//interleave units using transport sorter so that infantry and artillery are next to each other
		for (Territory invadeFrom : ourLandNeighbors)
		{
			List<Unit> ourAttackUnits = invadeFrom.getUnits().getMatches(landUnit);
			ourAttackUnits.removeAll(unitsAlreadyMoved);
			List<Unit> ourSortedUnits = sortTransportUnits(ourAttackUnits);
			List<Unit> attackUnits = new ArrayList<Unit>();
			Route attackRoute = data.getMap().getLandRoute(invadeFrom, enemy);
			if (attackRoute == null)
				continue;
			Iterator<Unit> sortIter = ourSortedUnits.iterator();
			while (sortIter.hasNext() && remainingStrengthNeeded > totStrength)
			{
				float aStrength = 0.0F;
				Unit attackUnit = sortIter.next();
				if (Matches.UnitTypeIsInfantry.match(attackUnit.getType())) //look at the next unit
				{
					if (sortIter.hasNext())
					{
						Unit attackUnit2 = sortIter.next();
						List<Unit> twoUnits = new ArrayList<Unit>();
						twoUnits.add(attackUnit);
						twoUnits.add(attackUnit2);
						aStrength = strength(twoUnits, attacking, false, false);
						totStrength += aStrength;
						attackUnits.addAll(twoUnits);
						continue;
					}
				}
				aStrength = uStrength(attackUnit, attacking, false, false);
				totStrength += aStrength;
				attackUnits.add(attackUnit);
			}
			if( attackUnits.isEmpty())
				continue;
			moveUnits.add(attackUnits);
			moveRoutes.add(attackRoute);
			unitsAlreadyMoved.addAll(attackUnits);
		}
		return totStrength;
	}

	/**
	 * Look for possible blitzing units
	 * Currently restricts the territory to one to which there are no existing enemy units as neighbors unless forced
	 * Use forced to get units to blitz no matter what
	 * Should be modified to compare strength of source territory and its neighbors
	 */
	public static float inviteBlitzAttack(boolean nonCombat, Territory enemy, float remainingStrengthNeeded, Collection<Unit> unitsAlreadyMoved,
								List<Collection<Unit>> moveUnits, List<Route> moveRoutes, GameData data, PlayerID player, boolean attacking, boolean forced)
	{//Blitz through owned into enemy
		CompositeMatch<Unit> blitzUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanBlitz, Matches.UnitIsNotStatic(player));
		CompositeMatch<Territory> alliedAndNotWater = new CompositeMatchAnd<Territory>(Matches.isTerritoryAllied(player, data), Matches.TerritoryIsNotImpassableToLandUnits(player));
		CompositeMatch<Territory> noEnemyUnitsAndNotWater = new CompositeMatchAnd<Territory>(Matches.territoryHasNoEnemyUnits(player, data), Matches.TerritoryIsNotImpassableToLandUnits(player));
		
 	    //Are there blitzable units available?
		List<Territory> blitzFrom = getExactNeighbors(enemy, 2, player, false);
		List<Territory> blitzCopy = new ArrayList<Territory>(blitzFrom);
		Route tRoute = null;
		float totStrength = 0.0F;
		for (Territory t : blitzCopy)
		{	
			if (nonCombat)
				tRoute = getTwoRoute(t, enemy, alliedAndNotWater, null, data);
			else
				tRoute = getTwoRoute(t, enemy, noEnemyUnitsAndNotWater, null, data);
			if (tRoute == null || tRoute.getLength()>2)
				blitzFrom.remove(t);
		}

		List<Unit> blitzUnits = new ArrayList<Unit>();
		float bStrength = 0.0F;
		if (forced) //if a route is available, bring in the units no matter what
		{
			for (Territory blitzTerr : blitzFrom)
			{
				blitzUnits.clear();
				List<Unit> tmpBlitz = new ArrayList<Unit>();
				blitzUnits.addAll(blitzTerr.getUnits().getMatches(blitzUnit));
				if(blitzUnits.isEmpty())
					continue;
				Route blitzRoute = getTwoRoute(blitzTerr, enemy, noEnemyUnitsAndNotWater, null, data);
				if (blitzRoute != null)
				{
					for (Unit blitzer : blitzUnits)
					{
						if (remainingStrengthNeeded > totStrength)
						{
							bStrength = uStrength(blitzer, attacking, false, false);
							totStrength += bStrength;
							tmpBlitz.add(blitzer);
						}
					}
					if(tmpBlitz.isEmpty())
						continue;
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
				blitzUnits.clear();
				Set <Territory> badTerr = data.getMap().getNeighbors(blitzTerr, Matches.territoryHasEnemyLandUnits(player, data));
				if (badTerr.isEmpty())
				{
					blitzUnits.addAll(blitzTerr.getUnits().getMatches(blitzUnit));
					if(blitzUnits.isEmpty())
						continue;
					List<Unit> tmpBlitz = new ArrayList<Unit>();
					Route blitzRoute = getTwoRoute(blitzTerr, enemy, noEnemyUnitsAndNotWater, null, data);
					if (blitzRoute != null)
					{
						for (Unit blitzer : blitzUnits)
						{
							if (remainingStrengthNeeded > totStrength && !unitsAlreadyMoved.contains(blitzer))
							{
								tmpBlitz.add(blitzer);
								bStrength = uStrength(blitzer, attacking, false, false);
								totStrength += bStrength;
							}
						}
						if(tmpBlitz.isEmpty())
							continue;
						moveRoutes.add(blitzRoute);
						moveUnits.add(tmpBlitz);
						unitsAlreadyMoved.addAll(tmpBlitz);
						blitzUnits.clear();
					}
				}
			}
		}
		return totStrength;
	}

	/**
	 * Takes a List of territories and finds the one closest to an enemy capitol by Land
	 * @param ourTerr
	 * @param byLand - force the route to be traced by Land
	 * @return Territory closest or null if none has a land route
	 */
	public static Territory closestToEnemyCapital(List<Territory> ourTerr, GameData data, PlayerID player, boolean byLand)
	{
		List<Territory> enemyCap = getEnemyCapitals(data, player);
		int thisDist = 0, capDist = 100;
		Territory returnTerr = null;
		for (Territory checkTerr : ourTerr)
		{
			for (Territory eCap : enemyCap)
			{
				if (byLand)
					thisDist = data.getMap().getDistance(checkTerr, eCap, Matches.TerritoryIsNotImpassableToLandUnits(player));
				else
					thisDist = data.getMap().getDistance(checkTerr, eCap);
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
	 * Make a two step route in which the condition only applies to the middle territory in the route and not the target
	 * @param blockedTerr - List of Territories the route cannot pass through
	 */
	public static Route getTwoRoute(Territory t1, Territory t2, Match<Territory> condition, List<Territory>blockedTerr, GameData data)
	{
		if (t1.equals(t2))
			return null;
		Route r = new Route();
		r.setStart(t1);
		Set <Territory> circleMatch = data.getMap().getNeighbors(t1, condition);
		if (circleMatch.contains(t2)) //neighbors
			return null;
		Set <Territory> checkTerr = data.getMap().getNeighbors(t2, condition);
		circleMatch.retainAll(checkTerr);
		boolean routeCompleted = false;
		Iterator<Territory> circleIter = circleMatch.iterator();
		while (circleIter.hasNext() && !routeCompleted)
		{
			Territory t3 = circleIter.next();
			if (blockedTerr != null && blockedTerr.contains(t3))
				continue;
			r.add(t3);
			r.add(t2);
			routeCompleted = true;
		}
		if (r.getLength() != 2)
			r = null;
		return r;
	}
	/**
	 * Determine the enemy potential for blitzing a territory - all enemies are combined
	 * @param blitzHere - Territory expecting to be blitzed
	 * @param blitzTerr - Territory which is being blitzed through (not guaranteed to be all possible route territories!)
	 * @param data
	 * @param ePlayer - the enemy Player
	 * @return actual strength of enemy units (armor)
	 */
	public static float determineEnemyBlitzStrength(Territory blitzHere, List<Route> blitzTerrRoutes, List<Territory> blockTerr, GameData data, PlayerID ePlayer)
	{
		CompositeMatch<Unit> blitzUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(ePlayer), Matches.UnitCanBlitz, Matches.UnitIsNotStatic(ePlayer));
		CompositeMatch<Territory> validBlitzRoute = new CompositeMatchAnd<Territory>(Matches.territoryHasNoEnemyUnits(ePlayer, data), Matches.TerritoryIsNotImpassableToLandUnits(ePlayer));
		
		float eStrength = 0.0F;
		List<Territory> pBlitzTerr = getExactNeighbors(blitzHere, 2, ePlayer, false);
		Set<Territory> blitzStepTerr = data.getMap().getNeighbors(blitzHere, validBlitzRoute);
		Iterator<Territory> bIter = pBlitzTerr.iterator();
		while (bIter.hasNext())
		{
			Territory checkTerr = bIter.next();
			if (checkTerr.isWater() || Matches.TerritoryIsImpassableToLandUnits(ePlayer).match(checkTerr))
				bIter.remove();
		}
		if (blitzStepTerr.isEmpty())
			return 0.0F;
		for (Territory checkBlitzTerr : pBlitzTerr)
		{
			Route blitzRoute = getTwoRoute(checkBlitzTerr, blitzHere, validBlitzRoute, blockTerr, data);
			if (blitzRoute == null)
				continue;
			List <Unit> blitzUnits = checkBlitzTerr.getUnits().getMatches(blitzUnit);
			if (blitzUnits.isEmpty())
				continue;
			blitzTerrRoutes.add(blitzRoute);
			eStrength += strength(blitzUnits, true, false, true);
		}
		return eStrength;
	}
	/**
	 * Determine if a list has something other than a transport
	 * Use for verifying sea attack
	 * @param unitList - List of units
	 * @return true - has a non-transport unit
	 */
	public static boolean ListContainsOtherThanTransports(List<Unit> unitList)
	{
		Iterator<Unit> unitIter = unitList.iterator();
		boolean hasNonTransport = false;
		while (unitIter.hasNext() && !hasNonTransport)
		{
			Unit unit = unitIter.next();
			hasNonTransport = Matches.UnitIsNotTransport.match(unit);
		}
		return hasNonTransport;
	}
	/**
	 * Verifies an entire set of moves
	 * 1st: Repair routes that are invalid
	 * 2nd: Remove transport only attacks
	 * @param moveUnits
	 * @param moveRoutes
	 * @param data
	 * @param player
	 */
	public static void verifyMoves(List<Collection<Unit>> moveUnits, List<Route> moveRoutes, GameData data, PlayerID player)
	{
		List<Unit> alreadyMoved = new ArrayList<Unit>();
		Iterator<Collection<Unit>> moveIter = moveUnits.iterator();
		Iterator<Route> routeIter = moveRoutes.iterator();
		HashMap<Territory, Collection<Unit>> attackMap = new HashMap<Territory, Collection<Unit>>();
		HashMap<Integer, Territory> routeMap = new HashMap<Integer, Territory>(); //used to track the routes of a set of units
		HashMap<Integer, Route> insertRoutes = new HashMap<Integer, Route>();
		int listCount = 0;
		while(moveIter.hasNext() && routeIter.hasNext())
		{
			listCount++;
			Collection<Unit> attackUnit = moveIter.next();
			Route attackRoute = routeIter.next();
			if (attackRoute == null || attackRoute.getEnd() == null)
			{
				moveIter.remove();
				routeIter.remove();
				continue;
			}
			Route newRoute = repairRoute(attackUnit, attackRoute, data, player);
			if (newRoute != null)
			{
				routeIter.remove();
				insertRoutes.put(listCount, newRoute);
			}
		}
		if (insertRoutes.size() > 0)
		{
			Set<Integer> placeValues = insertRoutes.keySet();
			for (Integer thisone : placeValues)
			{
				Route thisRoute = insertRoutes.get(thisone);
				moveRoutes.add(thisone, thisRoute);
			}
		}
		//generate attackMap
		Iterator<Collection<Unit>> moveIter2 = moveUnits.iterator();
		Iterator<Route> routeIter2 = moveRoutes.iterator();
		Integer routeCounter = 0;
		while (moveIter2.hasNext() && routeIter2.hasNext())
		{
			routeCounter++;
			Collection<Unit> currentUnits = new ArrayList<Unit>();
			Collection<Unit> theseUnits = moveIter2.next();
			Route thisRoute = routeIter2.next();
			Territory target = thisRoute.getEnd();
			if (attackMap.containsKey(target))
			{
				Collection<Unit> addUnits = attackMap.get(target);
				if (!addUnits.isEmpty())
					currentUnits.addAll(addUnits);
			}
			currentUnits.addAll(theseUnits);
			attackMap.put(target, currentUnits);
			routeMap.put(routeCounter, target);
		}
		//has a collection of all units moving to a given target
		//is this a good move??
		//Check a transport only attack
		Set<Territory> targetTerrs = attackMap.keySet();
		for (Territory targetTerr : targetTerrs)
		{
			List<Unit> enemyUnits = targetTerr.getUnits().getMatches(Matches.enemyUnit(player, data));
			Collection<Unit> ourUnits = attackMap.get(targetTerr);
			boolean enemyUnitsExist = enemyUnits.size() > 0;
			if (enemyUnitsExist && targetTerr.isWater())
			{
				Iterator<Unit> unitIter = ourUnits.iterator();
				boolean nonTransport = false;
				while (unitIter.hasNext() && !nonTransport)
				{
					Unit thisUnit = unitIter.next();
					if (alreadyMoved.contains(thisUnit))
					{
						continue;
					}
					if (Matches.UnitIsNotTransport.match(thisUnit))
						nonTransport = true;
				}
				if (!nonTransport || ourUnits.isEmpty()) //move of transports into an attack
				{
					int routeCounter2 = 0;
					List<Integer> deleteValues = new ArrayList<Integer>();
					for (Route xRoute : moveRoutes)
					{
						if (xRoute.getEnd() == targetTerr)
							deleteValues.add(routeCounter2);
						routeCounter2++;
					}
					for (int delOne : deleteValues)
					{
						moveUnits.remove(delOne);
						moveRoutes.remove(delOne);
					}
				}
				else
					alreadyMoved.addAll(ourUnits);
			}
		}
		
	}
	/**
	 * Verify an individual move - Route exists (not null) and there is an endpoint
	 * @param moveUnits
	 * @param moveRoute
	 * @return - new Route which allows for movement or null if Move is valid
	 */
	public static Route repairRoute(Collection<Unit> moveUnits, Route moveRoute, GameData data, PlayerID player)
	{
		boolean canMove = MoveValidator.hasEnoughMovement(moveUnits, moveRoute); 
		if (!canMove)
		{
			Route newRoute = new Route();
			Iterator<Territory> routeIter = moveRoute.iterator();
			newRoute.setStart(routeIter.next());
			boolean routeDone = false;
			while (routeIter.hasNext() && !routeDone)
			{
				Territory nextTerr = routeIter.next();
				Route oldRoute = newRoute;
				newRoute.add(nextTerr);
				if (MoveValidator.hasEnoughMovement(moveUnits, newRoute))
				{
					if (!MoveValidator.onlyAlliedUnitsOnPath(newRoute, player, data))
						return newRoute;
				}
				else
					return oldRoute;
			}
			
		}
		
		return null;
	}
	/**
	 * Does water exist around this territory
	 * @param checkTerr
	 * @return true if water exists, false if it doesn't
	 */
	public static boolean isWaterAt(Territory checkTerr, GameData data)
	{
		boolean Water = Matches.territoryHasWaterNeighbor(data).match(checkTerr);
		return Water;
	}
	
	/**
	 * Map a list of units
	 * @param units
	 * @return
	 */
	public static IntegerMap<UnitType> convertListToMap(Collection<Unit> units)
	{
		IntegerMap<UnitType> ourList = new IntegerMap<UnitType>();
		for (Unit u : units)
		{
			UnitType uT = u.getType();
			ourList.put(uT, 0);
		}
		Set<UnitType> ourTypeList = ourList.keySet();
		for (UnitType u2 : ourTypeList)
		{
			int count = 0;
			for (Unit u3 : units)
			{
				if (u3.getType() == u2)
					count++;
			}
			ourList.put(u2, count);
		}
		return ourList;
	}
	/**
	 * Very basic estimation of battle through recursion
	 * Determine pips on dice and divide by 6 to get hits
	 * Remove weakest attackers/defenders
	 * TODO: fix battleEstimator for ships
	 * 
	 * @param attacker - IntegerMap of number of attacking units (returns adjusted #s)
	 * @param defender - IntegerMap of number of defending units
	 * @return - true if Attacker probably wins
	 */
	public static boolean quickBattleEstimator(IntegerMap<UnitType> attacker, IntegerMap<UnitType> defender, PlayerID aPlayer, PlayerID dPlayer, boolean sea, boolean subRestricted)
	{
		try
		{
			return quickBattleEstimatorInternal(attacker, defender, aPlayer, dPlayer, sea, subRestricted);
		} catch(StackOverflowError e) {
			//bug 2968146 NWO 1.7.7 on Hard AI
			e.printStackTrace(System.out);
			return false;
		}
	}
	
	private static boolean quickBattleEstimatorInternal(IntegerMap<UnitType> attacker, IntegerMap<UnitType> defender, PlayerID aPlayer, PlayerID dPlayer, boolean sea, boolean subRestricted)
	{
		int totAttack = 0, totDefend = 0, deadA = 0, deadD = 0, deadModA = 0, deadModD = 0, countInf = 0, countArt = 0, planeAttack = 0, subDefend = 0;
		boolean planesOnly = true;
		boolean destroyerPresent = false;
		boolean subsOnly = true;
		Set <UnitType> attackingUnits = attacker.keySet();
		Set <UnitType> defendingUnits = defender.keySet();
		for (UnitType aUnit : attackingUnits)
		{
			UnitAttachment ua = UnitAttachment.get(aUnit);
			totAttack += ua.getAttackRolls(aPlayer)*ua.getAttack(aPlayer)*attacker.getInt(aUnit);
			countInf += Matches.UnitTypeIsInfantry.match(aUnit) ? 1 : 0;
			countArt += Matches.UnitTypeIsArtillery.match(aUnit) ? 1 : 0;
			if (Matches.UnitTypeIsNotAir.match(aUnit))
				planesOnly = false;
			else
				planeAttack = ua.getAttackRolls(aPlayer)*ua.getAttack(aPlayer)*attacker.getInt(aUnit);
			if (Matches.UnitTypeIsDestroyer.match(aUnit))
				destroyerPresent = true;
		}
		totAttack += Math.min(countInf, countArt);
		deadD = totAttack/6;
		deadModA = totAttack % 6;
		for (UnitType dUnit : defendingUnits)
		{
			UnitAttachment ua = UnitAttachment.get(dUnit);
			totDefend += ua.getDefenseRolls(dPlayer)*ua.getDefense(dPlayer)*defender.getInt(dUnit);
			if (Matches.UnitTypeIsSub.match(dUnit) && planesOnly)
				totDefend -= ua.getDefenseRolls(dPlayer)*ua.getDefense(dPlayer)*defender.getInt(dUnit);
			if (Matches.UnitTypeIsSub.invert().match(dUnit))
				subsOnly = false;
			else
				subDefend += ua.getDefenseRolls(dPlayer)*ua.getDefense(dPlayer)*defender.getInt(dUnit);
		}
		if (subRestricted && subsOnly && !destroyerPresent)
			totAttack -= planeAttack;
		if (planesOnly)
			totDefend -= subDefend;
		deadA = totDefend/6;
		deadModD = totDefend % 6;
		if (deadD == 0 && deadA == 0 && deadModA <= 2 && deadModD <= 2 && deadModA == deadModD)
		{ //declare it a tie when equal attack/defend and 2 or less
			deadA = 1;
			deadD = 1;
		}
		else
		{
			/**
			 * Run a set of 6 and see if we get at least 4 hits
			 * Convert 4 hits into 1 true hit...3 or less is ignored
			 * give advantage to defender by not counting first pass for attacker
			 */
			int testD = 0, testA = 0; //give an estimate at prob
			while (testD == 0 && testA == 0 && (deadModD > 0 || deadModA > 0))
			{
				for (int i=1; i <= 6; i++)
				{
					testD += (deadModD*100 >= Math.random()*600) ? 1 : 0;
					testA += (i > 1 && deadModA*100 >= Math.random()*600) ? 1 : 0;
				}
			}
			deadA += testD >= 4 ? 1 : 0;
			deadD += testA >= 4 ? 1 : 0;
		}
		IntegerMap<UnitType> newAttacker = removeUnits(attacker, true, deadA, aPlayer, sea);
		IntegerMap<UnitType> newDefender = removeUnits(defender, false, deadD, dPlayer, sea);
		
		if (newAttacker.totalValues() > 0 && newDefender.totalValues() > 0)
			quickBattleEstimatorInternal(newAttacker, newDefender, aPlayer, dPlayer, sea, subRestricted);
		
		
		for (UnitType nA : attackingUnits)
			attacker.put(nA, newAttacker.getInt(nA));
		for (UnitType nD : defendingUnits)
			defender.put(nD, newDefender.getInt(nD));
		boolean weWin = false;
		for (UnitType AA : attackingUnits)
		{
			if (Matches.UnitTypeIsNotAir.match(AA) && newAttacker.getInt(AA) > 0)
				weWin = true;
		}
		return (sea ? newAttacker.totalValues() > 0 : weWin);
		
	}
	/**
	 * Determine the strength of a territory
	 * 
	 * @param data
	 * @param thisTerr
	 * @param player
	 * @param attacking - attacking strength or defending
	 * @param sea
	 * @param tFirst
	 * @param allied - allied = true - all allied units --> false - owned units only
	 * @return
	 */
	public static float strengthOfTerritory(GameData data, Territory thisTerr, PlayerID player, boolean attacking, boolean sea, boolean tFirst, boolean allied)
	{
		List<Unit> theUnits = new ArrayList<Unit>();
		if (allied)
			theUnits.addAll(thisTerr.getUnits().getMatches(Matches.alliedUnit(player, data)));
		else
			theUnits.addAll(thisTerr.getUnits().getMatches(Matches.unitIsOwnedBy(player)));
		float theStrength = SUtils.strength(theUnits, attacking, sea, tFirst);
		return theStrength;
	}
	/**
	 * Look through a list and find planes do not have a good landing point available
	 * 
	 * TODO: Analyze and determine when our units have evacuated the landing points to make an attack
	 * 
	 * @param data
	 * @param xMoveUnits
	 * @param xMoveRoutes
	 * @param player
	 * @param alreadyAttacked
	 * @return
	 */
	
	public static float verifyPlaneAttack(GameData data, List<Collection<Unit>> xMoveUnits, List<Route> xMoveRoutes, PlayerID player, List<Territory> alreadyAttacked)
	{
		Iterator<Collection<Unit>> xMoveIter = xMoveUnits.iterator();
		int routeNo = 0;
		float removeStrength = 0.0F;
		HashMap<Territory, List<Integer>> badRouteMap = new HashMap<Territory, List<Integer>>(); 
		HashMap<Territory, Float> strengthDiffMap = new HashMap<Territory, Float>();
		List<Integer> emptyList = new ArrayList<Integer>();
		for (Territory alliedTerr : SUtils.allAlliedTerritories(data, player))
		{
			float eStrength = SUtils.getStrengthOfPotentialAttackers(alliedTerr, data, player, false, false, alreadyAttacked);
			float ourStrength = SUtils.strengthOfTerritory(data, alliedTerr, player, false, false, false, true);
			if (Matches.territoryHasAlliedFactory(data, player).match(alliedTerr))
				ourStrength += ourStrength*0.25F;
			if (ourStrength > 3.0F)
				strengthDiffMap.put(alliedTerr, eStrength*0.85F - ourStrength);
			else if (eStrength > 3.0F)
				strengthDiffMap.put(alliedTerr, (eStrength*1.25F + 3.0F) - ourStrength); //avoid empty territories
			else if (eStrength < 3.0F)
				strengthDiffMap.put(alliedTerr, -ourStrength - 3.0F);
			else
				strengthDiffMap.put(alliedTerr, eStrength - ourStrength);
			badRouteMap.put(alliedTerr, emptyList);
		}
		while (xMoveIter.hasNext())
		{
			Collection<Unit> xMoves = xMoveIter.next();
			Route goRoute = xMoveRoutes.get(routeNo);
			int routeLength = goRoute.getLength();
			Territory endTerr = goRoute.getEnd();
			Iterator<Unit> xMIter = xMoves.iterator();
			while(xMIter.hasNext())
			{
				Unit plane = xMIter.next();
				boolean safePlane = false;
				if (Matches.UnitIsAir.match(plane))
				{
					int moveAvailable = TripleAUnit.get(plane).getMovementLeft();
					moveAvailable -= routeLength;
					List<Territory> endNeighbors = new ArrayList<Territory>(data.getMap().getNeighbors(endTerr, moveAvailable));
					Iterator<Territory> eIter = endNeighbors.iterator();
					while (eIter.hasNext())
					{
						Territory newTerr = eIter.next();
						if (Matches.TerritoryIsWater.match(newTerr) || Matches.isTerritoryAllied(player, data).invert().match(newTerr))
							eIter.remove();
					}
					SUtils.reorder(endNeighbors, strengthDiffMap, false);
					Iterator<Territory> eIter2 = endNeighbors.iterator();
					while (eIter.hasNext() && !safePlane)
					{
						Territory newTerr = eIter2.next();
						if (strengthDiffMap.containsKey(newTerr))
						{
							float strengthDiff = strengthDiffMap.get(newTerr) - SUtils.uStrength(plane, false, false, false);
							strengthDiffMap.put(newTerr, strengthDiff);
							if (strengthDiff <= 0.0F)
								safePlane = true;
							else
							{
								List<Integer> RouteNos = badRouteMap.get(newTerr);
								RouteNos.add(routeNo);
								badRouteMap.put(newTerr, RouteNos);
							}
						}
					}
				}
			}
		}
		List<Territory> badMoveTerrs = new ArrayList<Territory>(badRouteMap.keySet());
		float strengthEliminated = 0.0F;
		for (Territory checkTerr : badMoveTerrs)
		{
			float strengthDiff = strengthDiffMap.get(checkTerr);
			if (strengthDiff > 0.0F)
				continue;
			List<Integer> routeNumber = badRouteMap.get(checkTerr);
			for (Integer killRoute : routeNumber)
			{
				Collection<Unit> killUnits = xMoveUnits.get(killRoute);
				strengthEliminated += SUtils.strength(killUnits, true, false, false);
				xMoveUnits.remove(killRoute);
				xMoveRoutes.remove(killRoute);
			}
		}
		return strengthEliminated;
	}
	/**
	 * 
	 * Method for removing a set of units from an IntegerMap of units
	 * Major Assumption: Cost Increases: infantry < artillery < armour
	 * If Ship is passed through in a land attack it is removed
	 * 
	 * @param units - Units and #
	 * @param attacking
	 * @param killNum - # of units to kill
	 * @return
	 */
	
	public static IntegerMap<UnitType> removeUnits(IntegerMap<UnitType> units, boolean attacking, int killNum, PlayerID player, boolean sea)
	{
		IntegerMap<UnitType> finalList = new IntegerMap<UnitType>();
		Set<UnitType> unitList = units.keySet();
		List<UnitType> orderedUnitList = new ArrayList<UnitType>(unitList);
		for (int i = 0; i < unitList.size(); i++)
		{
			UnitType unit1 = orderedUnitList.get(i);
			boolean isInf1 = Matches.UnitTypeIsInfantry.match(unit1);
			boolean isArt1 = Matches.UnitTypeIsArtillery.match(unit1);
			boolean isTank1 = UnitAttachment.get(unit1).getCanBlitz();
			if (!sea && Matches.unitTypeCanBombard(player).match(unit1))
			{
				orderedUnitList.remove(i);
				continue;
			}
			int ipip = 0;
			UnitAttachment ua = UnitAttachment.get(unit1);
			if (attacking)
				ipip = ua.getAttack(player);
			else
				ipip = ua.getDefense(player);
			// TODO: we should interleave artillery and infantry when they both have same base attack value
			for (int j = i+1; j < unitList.size(); j++)
			{
				UnitType unit2 = orderedUnitList.get(j);
				boolean isInf2 = Matches.UnitTypeIsInfantry.match(unit2);
				boolean isArt2 = Matches.UnitTypeIsArtillery.match(unit2);
				boolean isTank2 = UnitAttachment.get(unit2).getCanBlitz();

				UnitAttachment ua2 = UnitAttachment.get(unit2);
				int ipip2 = 0;
				if (attacking)
					ipip2 = ua2.getAttack(player);
				else
					ipip2 = ua2.getDefense(player);
				if (ipip > ipip2 || (ipip == ipip2 && (((isInf1 || isArt1) && (!isInf2 || !isArt2)) || (isTank1 && !isInf2 && !isArt2 && !isTank2)))) 
				{
					UnitType itemp = orderedUnitList.get(i);
					UnitType itemp2 = orderedUnitList.get(j);
					//we know that i < j always
					orderedUnitList.remove(i);
					orderedUnitList.remove(j-1);
					orderedUnitList.add(i, itemp2);
					orderedUnitList.add(j, itemp);
					
				}
			}
		}
		
		
		for (UnitType unitKill : orderedUnitList)
		{
			int minusNum = Math.min(units.getInt(unitKill), killNum );
			finalList.put(unitKill, units.getInt(unitKill) - minusNum);
			killNum -= minusNum;
		}
		
		return finalList;
	}
	/**
	 * Determine how many more ships the enemy has than the player
	 * fighters on AC are counted also
	 * 
	 * Each ship is given the same value except for BB (two)
	 * Transports are treated as 0.5 if tFirst and 0 if not
	 * TODO: Add land based fighters and bombers to this list
	 * 
	 * @param checkTerr - territory to be checked
	 * @param data
	 * @param player
	 * @param attackAdv - total advantage the enemy has 
	 * @param tFirst - can transports be killed before other units
	 * @return
	 */
	public static int shipThreatToTerr(Territory checkTerr, GameData data, PlayerID player, boolean tFirst)
	{
		CompositeMatchAnd<Unit> enemyUnit = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data));
		CompositeMatchAnd<Unit> enemySeaUnit = new CompositeMatchAnd<Unit>(enemyUnit, Matches.UnitIsSea, Matches.UnitIsNotTransport);
		CompositeMatchAnd<Unit> enemyAirUnit = new CompositeMatchAnd<Unit>(enemyUnit, Matches.UnitIsAir);
		CompositeMatchAnd<Unit> enemyBBUnit = new CompositeMatchAnd<Unit>(enemyUnit, Matches.UnitIsBB);
		CompositeMatchAnd<Unit> enemyTransportUnit = new CompositeMatchAnd<Unit>(enemyUnit, Matches.UnitIsTransport);
		CompositeMatchAnd<Unit> alliedSeaUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsSea, Matches.UnitIsNotTransport);
		CompositeMatchAnd<Unit> alliedTransport = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsTransport);
		CompositeMatchAnd<Unit> alliedAirUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitCanLandOnCarrier);
		CompositeMatchAnd<Unit> alliedBBUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsBB);
		boolean isWater = isWaterAt(checkTerr, data);
		if (!isWater) //no way to get here
			return -1;
		
		Set<Territory> waterNeighbors = data.getMap().getNeighbors(checkTerr, Matches.TerritoryIsWater);
		Set<Territory> shipNeighbors = data.getMap().getNeighbors(checkTerr, 4);
		int totAttackCount = 0; int totTransCount = 0;
		List<Territory> checkThese = new ArrayList<Territory>();
		List<Territory> checkThese2 = new ArrayList<Territory>();
		PlayerID ePlayer = null;
		List<PlayerID> ePlayers = getEnemyPlayers(data, player);
		if (!ePlayers.isEmpty())
			ePlayer = ePlayers.get(0); //doesn't matter which one
		for (Territory shipTerr : shipNeighbors)
		{
			List<Unit> allShips = shipTerr.getUnits().getMatches(Matches.UnitIsSea);
			if (allShips.isEmpty())
				continue;
			int shipDistance = MoveValidator.getLeastMovement(allShips);
			Iterator<Territory> waterIter = waterNeighbors.iterator();
			while (waterIter.hasNext()) //verify it is in range
			{
				Territory waterTerr = waterIter.next();
				Route testERoute = getMaxSeaRoute(data, shipTerr, waterTerr, ePlayer, true, shipDistance);
				Route testARoute = getMaxSeaRoute(data, shipTerr, waterTerr, player, true, shipDistance);
				if (testERoute != null)
				{
					int testLength = testERoute.getLength();
					if (shipTerr.isWater() && testLength <= (shipDistance + 1))
					{	
						checkThese.add(shipTerr);
					}
				}
				if (testARoute != null)
				{
					int testLength = testARoute.getLength();
					if (shipTerr.isWater() && testLength <= (shipDistance + 1))
					{	
						checkThese2.add(shipTerr);
					}
				}
			}
		}
		if (checkTerr.isWater())
		{
			checkThese.add(checkTerr);
			checkThese2.add(checkTerr);
		}
		for (Territory sT : checkThese)
		{
			totAttackCount += sT.getUnits().countMatches(enemySeaUnit) + sT.getUnits().countMatches(enemyAirUnit);
			totAttackCount += sT.getUnits().countMatches(enemyBBUnit);
		
			totTransCount += sT.getUnits().countMatches(enemyTransportUnit);
			
		}
		for (Territory sT : checkThese2)
		{
			totAttackCount -= sT.getUnits().countMatches(alliedSeaUnit) - sT.getUnits().countMatches(alliedAirUnit);
			totAttackCount -= sT.getUnits().countMatches(alliedBBUnit);
		
			totTransCount -= sT.getUnits().countMatches(alliedTransport);
			
		}
		if (tFirst)
			totAttackCount += totTransCount/2; //treat transport as half an attack ship
		
		return totAttackCount;
	}
	
	/**
	 * Remove any territories which cannot be amphibiously invaded
	 * @param territories
	 * @param data
	 */
	public static void removeNonAmphibTerritories(List<Territory> territories, GameData data)
	{
		if (territories.isEmpty())
			return;
		Iterator<Territory> tIter = territories.iterator();
		while (tIter.hasNext())
		{
			Territory checkTerr = tIter.next();
			if (Matches.territoryHasWaterNeighbor(data).invert().match(checkTerr))
				tIter.remove();
		}
		return;
	}
	
	@SuppressWarnings("unchecked")
	public static void reorder(List<?> reorder, final IntegerMap map, final boolean greaterThan)
	{
		if(!map.keySet().containsAll(reorder)) {
			throw new IllegalArgumentException("Not all of:" + reorder + " in:" + map.keySet());
		}
		
		Collections.sort(reorder, new Comparator<Object>() {

			public int compare(Object o1, Object o2) {
				//get int returns 0 if no value
				int v1 = map.getInt(o1);
				int v2 = map.getInt(o2);
				
				if(greaterThan) {
					int t = v1;
					v1 = v2;
					v2 = t;
				}
				
				if(v1 > v2) {
					return 1;
				} else if(v1 == v2) {
					return 0;
				} else {
					return -1;
				}
			}			
		});
	}
	
	
	public static void reorder(List<?> reorder, final Map<?, ? extends Number> map, final boolean greaterThan)
	{
	
		Collections.sort(reorder, new Comparator<Object>() {

			public int compare(Object o1, Object o2) {
				double v1 = safeGet(map, o1);
				double v2 = safeGet(map, o2);
				
				if(greaterThan) {
					double t = v1;
					v1 = v2;
					v2 = t;
				}
				
				if(v1 > v2) {
					return 1;
				} else if(v1 == v2) {
					return 0;
				} else {
					return -1;
				}
			}

			private double safeGet(final Map<?, ? extends Number> map, Object o1) {
				if(!map.containsKey(o1)) {
					return 0;
				}
				return map.get(o1).doubleValue();
			}			
		});
	}
	
	
	/**
	 * Take the mix of Production Rules and determine the best purchase set for attack, defense or transport
	 * 
	 * So much more that can be done with this...track units and try to minimize or maximize the # purchased
	 */

	public static boolean findPurchaseMix(IntegerMap<ProductionRule> bestAttack, IntegerMap<ProductionRule> bestDefense, IntegerMap<ProductionRule> bestTransport, 
											IntegerMap<ProductionRule> bestMaxUnits, IntegerMap<ProductionRule> bestMobileAttack, List<ProductionRule> rules, int totPU, int maxUnits, GameData data, PlayerID player, int fighters)
	{
        Resource key = data.getResourceList().getResource(Constants.PUS);
		IntegerMap<String> parameters = new IntegerMap<String>();
		parameters.put("attack", 0);
		parameters.put("defense", 0);
		parameters.put("maxAttack", 0);
		parameters.put("maxDefense", 0);
		parameters.put("maxUnitAttack", 0);
		parameters.put("maxTransAttack", 0);
		parameters.put("maxMobileAttack", 0);
		parameters.put("maxTransCost", 100000);
		parameters.put("maxAttackCost", 100000);
		parameters.put("maxUnitCount", 0);
		parameters.put("maxDefenseCost", 100000);
		parameters.put("maxUnitCost", 100000);
		parameters.put("totcost", 0);
		parameters.put("totUnit", 0);
		parameters.put("totMovement", 0);
		parameters.put("maxMovement", 0);
		parameters.put("maxUnits", maxUnits); //never changed
		parameters.put("maxCost", totPU); //never changed
		parameters.put("infantry", 0);
		parameters.put("nonInfantry", 0);
		HashMap<ProductionRule, Boolean> infMap = new HashMap<ProductionRule, Boolean>();
		HashMap<ProductionRule, Boolean> nonInfMap = new HashMap<ProductionRule, Boolean>();
		HashMap<ProductionRule, Boolean> supportableInfMap = new HashMap<ProductionRule, Boolean>();
		Iterator<ProductionRule> prodIter = rules.iterator();
		HashMap<ProductionRule, Boolean> transportMap= new HashMap<ProductionRule, Boolean>();
		int minCost = 10000;
		ProductionRule minCostRule = null;
		while (prodIter.hasNext())
		{
			ProductionRule rule = prodIter.next();
			bestAttack.put(rule, 0); //initialize with 0
			bestDefense.put(rule, 0);
			bestMaxUnits.put(rule, 0);
			bestTransport.put(rule, 0);
			UnitType x = (UnitType) rule.getResults().keySet().iterator().next();
			supportableInfMap.put(rule, UnitAttachment.get(x).isArtillerySupportable());
			transportMap.put(rule, Matches.UnitTypeCanBeTransported.match(x));
			infMap.put(rule, Matches.UnitTypeIsInfantry.match(x));
			nonInfMap.put(rule, Matches.UnitTypeCanBeTransported.match(x) && Matches.UnitTypeIsInfantry.invert().match(x) && Matches.UnitTypeIsAA.invert().match(x));
		}
		int countNum = 1;
		int goodLoop = purchaseLoop (parameters, countNum, bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack, transportMap, infMap, nonInfMap, supportableInfMap, data, player, fighters);
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
	 * @param bestTransport - list of the rules and the number to be purchased (optimized for transporting)
	 * @param bestMaxUnits - list of the rules and the number to be purchased (optimized for attack and max units)
	 * @param bestTransport - list of the rules and the number to be purchased (optimized for transport)
	 * @return - integer which is 1 if bestAttack has changed, 2 if bestDefense has changed, 3 if both have changed
	 */
	
	public static int purchaseLoop(IntegerMap<String> parameters, int ruleNum, IntegerMap<ProductionRule> bestAttack, IntegerMap<ProductionRule> bestDefense, IntegerMap<ProductionRule> bestTransport,
								   IntegerMap<ProductionRule> bestMaxUnits, IntegerMap<ProductionRule> bestMobileAttack, HashMap<ProductionRule, Boolean> transportMap, 
								   HashMap<ProductionRule, Boolean> infMap, HashMap<ProductionRule, Boolean> nonInfMap, HashMap<ProductionRule, Boolean> supportableInfMap, 
								   GameData data, PlayerID player, int fighters)
	{
		long start = System.currentTimeMillis();
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
		 * Assumptions: 1) artillery purchased with infantry has a bonus
		 *              2) fighters have attack: 3 and defense: 4 TODO: Recode this to use fighter attack/defense and to handle tech bonus
		 */
        Resource key = data.getResourceList().getResource(Constants.PUS);
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
		Integer totCost =  parameters.getInt("totcost");
		Integer totMovement = parameters.getInt("totMovement");
		Integer maxCost = parameters.getInt("maxCost");
		Integer maxUnits = parameters.getInt("maxUnits");
		Integer totUnits = parameters.getInt("totUnits");
		Integer maxAttack = parameters.getInt("maxAttack");
		Integer maxDefense = parameters.getInt("maxDefense");
		Integer maxTransAttack = parameters.getInt("maxTransAttack");
		Integer maxTransCost = parameters.getInt("maxTransCost");
		Integer maxAttackCost = parameters.getInt("maxAttackCost");
		Integer maxDefenseCost = parameters.getInt("maxDefenseCost");
		Integer maxUnitAttack = parameters.getInt("maxUnitAttack");
		Integer maxUnitCost = parameters.getInt("maxUnitCost");
		Integer maxUnitCount = parameters.getInt("maxUnitCount");
		Integer maxMobileAttack = parameters.getInt("maxMobileAttack");
		Integer maxMovement = parameters.getInt("maxMovement");
		Integer supportableInfCount = parameters.getInt("supportableInfCount");
		Integer infCount = parameters.getInt("infantry");
		Integer nonInfCount = parameters.getInt("nonInfantry");
		int parametersChanged = 0, thisParametersChanged = 0;
		UnitType x = (UnitType) rule.getResults().keySet().iterator().next();
		UnitAttachment u = UnitAttachment.get(x);
		boolean thisIsSupportableInf = supportableInfMap.get(rule);
		boolean thisIsInf = infMap.get(rule);
		boolean thisIsNonInf = nonInfMap.get(rule);
		boolean thisIsArt = u.isArtillery();
		int uMovement = u.getMovement(player);
		int uAttack = u.getAttack(player);
		int uDefense = u.getDefense(player);
		int aRolls = u.getAttackRolls(player);
		int cost = rule.getCosts().getInt(key);
		// Discourage buying submarines, since the AI has no clue how to use them (veqryn)
		boolean thisIsSub = u.isSub();
		if (thisIsSub && uAttack >= 1)
			uAttack--;
		else if (thisIsSub && uDefense >= 1)
			uDefense--;
		// Encourage buying balanced units. Added by veqryn, to decrease the rate at which the AI buys walls, fortresses, and mortars, among other specialy units that should not be bought often if at all.
		if (u.getMovement(player) == 0)
			uAttack = 0;
		if ((u.getAttack(player) == 0 || u.getDefense(player) - u.getAttack(player) >= 4) && u.getDefense(player) >= 1)
		{
			uDefense--;
			if (u.getDefense(player) - u.getAttack(player) >= 4)
				uDefense--;
		}
		if ((u.getDefense(player) == 0 || u.getAttack(player) - u.getDefense(player) >= 4) && u.getAttack(player) >= 1)
		{
			uAttack--;
			if (u.getAttack(player) - u.getDefense(player) >= 4)
				uAttack--;
		}
		// TODO: stop it from buying zero movement units under all circumstances.  Also, lessen the number of artillery type units bought slightly. And lessen sub purchases, or eliminate entirely. (veqryn)
		// TODO: some transport ships have large capacity, others have a small capacity and are made for fighting.  Make sure if the AI is buying transports, it chooses high capacity transports even if more expensive and less att/def than normal ships
		int fightersremaining = fighters;
		int usableMaxUnits = maxUnits;
		if (usableMaxUnits*ruleCheck.size() > 1000 && Math.random() <= 0.50)
			usableMaxUnits = usableMaxUnits/2;
		for (int i=0; i <= (usableMaxUnits - totUnits); i++)
		{
			if (i > 0) //allow 0 so that this unit might be skipped...due to low value...consider special capabilities later
			{
				totCost += cost;
				if (totCost > maxCost)
					continue;
				if (thisIsInf)
					infCount++;
				else if (thisIsNonInf)
					nonInfCount++;
				if (thisIsSupportableInf)
					supportableInfCount++;
				//give bonus of 1 hit per 2 units and if fighters are on the capital, a bonus for carrier equal to fighter attack or defense
				int carrierLoad = Math.min(u.getCarrierCapacity(), fightersremaining);
				if (carrierLoad < 0)
					carrierLoad = 0;
				int bonusAttack = (u.isTwoHit() ? uAttack : 0) + (uAttack > 0 && (i % 2)==0 ? 1 : 0) + carrierLoad*3;
				if (thisIsArt && i <= supportableInfCount)
					bonusAttack++; //add one bonus for each artillery purchased with supportable infantry
				int bonusDefense = (u.isTwoHit() ? uDefense : 0) + (uDefense > 0 && (i % 2)==0 ? 1 : 0) + (carrierLoad*4);
				fightersremaining -= carrierLoad;
				totUnits++;
				totAttack += uAttack*aRolls + bonusAttack; 
				totDefense += uDefense*aRolls + bonusDefense;
				totMovement += uMovement;
			}
			if (totUnits <= maxUnits && ruleIter.hasNext())
			{
				parameters.put("attack", totAttack);
				parameters.put("defense", totDefense);
				parameters.put("totcost", totCost);
				parameters.put("totUnits", totUnits);
				parameters.put("totMovement", totMovement);
				parameters.put("infantry", infCount);
				parameters.put("nonInfantry", nonInfCount);
				parameters.put("supportableInfCount", supportableInfCount);
				parametersChanged = purchaseLoop(parameters, counter, bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack, transportMap,
						infMap, nonInfMap, supportableInfMap, data, player, fighters);
				maxAttack = parameters.getInt("maxAttack");
				maxTransAttack = parameters.getInt("maxTransAttack");
				maxTransCost = parameters.getInt("maxTransCost");
				maxDefense = parameters.getInt("maxDefense");
				maxAttackCost = parameters.getInt("maxAttackCost");
				maxDefenseCost = parameters.getInt("maxDefenseCost");
				maxUnitCost = parameters.getInt("maxUnitCost");
				maxUnitAttack = parameters.getInt("maxUnitAttack");
				maxMobileAttack = parameters.getInt("maxMobileAttack");
				maxMovement = parameters.getInt("maxMovement");
				if(System.currentTimeMillis() - start > PURCHASE_LOOP_MAX_TIME_MILLIS) {
					break;
				}
			}
			if (totCost == 0)
				continue;
			//parameters changed: 001: attack, 010: defense, 100: maxUnits, 1000: transport, 10000: mobileAttack
			if (parametersChanged > 0) //change forced by another rule
			{
				if ((parametersChanged -3) % 4 == 0)
				{
					bestAttack.put(rule, i);
					bestDefense.put(rule, i);
					thisParametersChanged = 3;
					parametersChanged -=3;
				}
				else if ((parametersChanged - 1 ) % 4 == 0 )
				{
					bestAttack.put(rule, i);
					if (thisParametersChanged % 2 == 0)
						thisParametersChanged += 1;
					parametersChanged -=1;
				}
				else if ((parametersChanged - 2) % 4 == 0 )
				{
					bestDefense.put(rule, i);
					if ((thisParametersChanged + 2) % 4  != 0 && (thisParametersChanged + 1) % 4 != 0)
						thisParametersChanged +=2;
					parametersChanged -=2;
				}
				if ((parametersChanged > 0) && (parametersChanged - 4) % 8 == 0)
				{
					bestMaxUnits.put(rule, i);
					if (thisParametersChanged == 0 || (thisParametersChanged - 4) % 8 != 0)
						thisParametersChanged +=4;
					parametersChanged -=4;
				}
				if ((parametersChanged - 8) % 16 == 0)
				{
					bestTransport.put(rule, i);
					if (thisParametersChanged == 0 || (thisParametersChanged - 8) % 16 != 0)
						thisParametersChanged += 8;
				}
				if (parametersChanged >= 16)
				{
					bestMobileAttack.put(rule, i);
					if (thisParametersChanged < 16)
						thisParametersChanged += 16;
				}
				parametersChanged = 0;
				continue;
		    }
			if ((totAttack > maxAttack) || (totAttack == maxAttack && (Math.random() < 0.50)))
			{
				maxAttack = totAttack;
				maxAttackCost = totCost;
				parameters.put("maxAttack", maxAttack);
				parameters.put("maxAttackCost", maxAttackCost);
				bestAttack.put(rule, i);
				if (thisParametersChanged % 2 == 0)
					thisParametersChanged += 1;
				Iterator <ProductionRule> changeIter = ruleCheck.iterator();
				ProductionRule changeThis = null;
				int countThis = 1;
				while (changeIter.hasNext()) //have to clear the rules below this rule
				{
					changeThis = changeIter.next();
					if (countThis >= counter)
						bestAttack.put(changeThis, 0);
					countThis++;
				}
			}
			if ((totDefense > maxDefense) || (totDefense == maxDefense && (Math.random() < 0.50)))
			{
				maxDefense = totDefense;
				maxDefenseCost = totCost;
				parameters.put("maxDefense", maxDefense);
				parameters.put("maxDefenseCost", maxDefenseCost);
				bestDefense.put(rule, i);
				if ((thisParametersChanged + 2) % 4 != 0 && (thisParametersChanged + 1) % 4 != 0)
					thisParametersChanged += 2;
				Iterator <ProductionRule> changeIter = ruleCheck.iterator();
				ProductionRule changeThis = null;
				int countThis = 1;
				while (changeIter.hasNext()) //have to clear the rules below this rule
				{
					changeThis = changeIter.next();
					if (countThis >= counter)
						bestDefense.put(changeThis, 0);
					countThis++;
				}
			}
			if (totAttack > maxUnitAttack && totUnits >= maxUnitCount)
			{
				maxUnitAttack = totAttack;
				maxUnitCount = totUnits;
				maxUnitCost = totCost;
				parameters.put("maxUnitAttack", maxUnitAttack);
				parameters.put("maxUnitCount", maxUnitCount);
				parameters.put("maxUnitCost", maxUnitCost);
				bestMaxUnits.put(rule, i);
				if ((thisParametersChanged + 4) % 8 != 0)
					thisParametersChanged += 4;
				Iterator <ProductionRule> changeIter = ruleCheck.iterator();
				ProductionRule changeThis = null;
				int countThis = 1;
				while (changeIter.hasNext()) //have to clear the rules below this rule
				{
					changeThis = changeIter.next();
					if (countThis >= counter)
						bestMaxUnits.put(changeThis, 0);
					countThis++;
				}
			}
			if (totAttack > maxTransAttack && (infCount <= nonInfCount + 1 && infCount >= nonInfCount -1))
			{
				maxTransAttack = totAttack;
				maxTransCost = totCost;
				parameters.put("maxTransAttack", totAttack);
				parameters.put("maxTransCost", maxTransCost);
				bestTransport.put(rule, i);
				if ((thisParametersChanged + 8) % 16 != 0)
					thisParametersChanged += 8;
				Iterator <ProductionRule> changeIter = ruleCheck.iterator();
				ProductionRule changeThis = null;
				int countThis = 1;
				while (changeIter.hasNext())
				{
					changeThis = changeIter.next();
					if (countThis >= counter)
						bestTransport.put(changeThis, 0);
					countThis++;
				}
			}
			if ((totAttack >= maxMobileAttack && (totMovement > maxMovement )) || (totAttack > maxMobileAttack && (totMovement >= maxMovement)))
			{
				maxMobileAttack = totAttack;
				maxMovement = totMovement;
				parameters.put("maxMobileAttack", maxMobileAttack);
				parameters.put("maxMovement", maxMovement);
				bestMobileAttack.put(rule, i);
				if (thisParametersChanged < 16)
					thisParametersChanged += 16;
				Iterator<ProductionRule> changeIter = ruleCheck.iterator();
				ProductionRule changeThis = null;
				int countThis = 1;
				while (changeIter.hasNext())
				{
					changeThis = changeIter.next();
					if (countThis >= counter)
						bestMobileAttack.put(changeThis, 0);
					countThis++;
				}
			}
/*			Iterator <ProductionRule> intCheckIter = ruleCheck.iterator();
			ProductionRule intCheck = null;
			infCount = 0;
			nonInfCount = 0;
			supportableInfCount = 0;
			//this biases the purchase of artillery to the transport progression
			//need a better way to track this, but for now, we'll go with it
			//should reduce the artillery purchase in WW2V3, which seems high anyways
			while (intCheckIter.hasNext()) //have to clear the rules below this rule
			{
				intCheck = intCheckIter.next();
				infCount += infMap.get(intCheck) ? bestTransport.getInt(intCheck) : 0;
				nonInfCount += nonInfMap.get(intCheck) ? bestTransport.getInt(rule) : 0;
				supportableInfCount += supportableInfMap.get(intCheck) ? bestTransport.getInt(rule) : 0;
			}
*/		}
		return thisParametersChanged;
	}
	
	public static List<PlayerID> getAlliedPlayers (GameData data, PlayerID player)
	{
    	Collection<PlayerID> playerList = data.getPlayerList().getPlayers();
    	List<PlayerID> aPlayers = new ArrayList<PlayerID>(playerList);
    	List<PlayerID> ePlayers = getEnemyPlayers(data, player);
    	aPlayers.removeAll(ePlayers);
    	return aPlayers;
	}
	
	/**
	 * use TUV for a set of allies and their enemies to determine if TUV superiority indicates that a player can be more
	 * aggressive in an attack. 
	 * @param data
	 * @param player
	 * @param aggressiveFactor - float which will set how much more TUV is needed to allow aggressive
	 * @return
	 */
	
	public static boolean determineAggressiveAttack(GameData data, PlayerID player, float aggressiveFactor)
	{
		int alliedTUV = getAlliedEnemyTUV(data, player, true);
		int enemyTUV = getAlliedEnemyTUV(data, player, false);
    	return (alliedTUV*100) > (enemyTUV*100*aggressiveFactor);
	}
	
	/**
	 * Determine TUV for allies/enemies
	 * @param data
	 * @param player
	 * @param allied - boolean indicating for which set to gather TUV
	 * @return
	 */
	public static int getAlliedEnemyTUV(GameData data, PlayerID player, boolean allied)
	{
    	IntegerMap<PlayerID> unitMap = getPlayerTUV(data);
    	int TUV = 0;
    	
    	if (allied)
    	{
    		List<PlayerID> aPlayers = getAlliedPlayers(data, player);
    		for (PlayerID aP : aPlayers)
    			TUV += unitMap.getInt(aP);
    	}
    	else
    	{
    		List<PlayerID> ePlayers = getEnemyPlayers(data, player);
    		for (PlayerID eP : ePlayers)
    			TUV += unitMap.getInt(eP);
    	}
    	return TUV;
		
	}

    public static Route getMaxSeaRoute(final GameData data, Territory start, Territory destination, final PlayerID player, boolean attacking, int maxDistance)
    {
    	if (start == null || destination == null || !start.isWater() || !destination.isWater())
    	{
			return null;
		}
   		Match<Territory> routeCond = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasNoEnemyUnits(player, data));
   		Route r = data.getMap().getWaterRoute(start, destination);
   		if (r == null || r.getEnd() == null)
   			return null;
   		int rDist = r.getLength();
   		if (MoveValidator.validateCanal(r, player, data) != null)
   			return null;
   		if ((rDist <= 1) && r.getEnd() != null)
			return r;
   		boolean noEnemyUnits = MoveValidator.onlyAlliedUnitsOnPath(r, player, data);
   		if (!noEnemyUnits)
   			noEnemyUnits = MoveValidator.onlyIgnoredUnitsOnPath(r, player, data, attacking);
   		if (noEnemyUnits)
   		{
   			if (rDist <= maxDistance)
   				return r;
   			else
   			{
   				Route route2 = new Route();
   				route2.setStart(start);
   				for (int i = 1; i <= maxDistance; i++)
   					route2.add(r.getTerritories().get(i));
   				return route2;
   			}
   		}
   			//check for alternate path
   		for (int i=1; i < rDist; i++)
   		{
   			Territory currentTerr = r.getTerritories().get(i);
   			if (Matches.territoryHasEnemyUnits(player, data).match(currentTerr))
   			{
   				Territory previousTerr = r.getTerritories().get(i-1);
   				Set<Territory> currNeighbors = data.getMap().getNeighbors(currentTerr, routeCond);
   				Territory nextTerr = r.getTerritories().get(i+1);
   				currNeighbors.remove(previousTerr);
   				currNeighbors.remove(nextTerr);
   				boolean switchTerr = false;
   				for (Territory xTerr : currNeighbors)
   				{
   					if (switchTerr)
   						continue;
   					Set<Territory> xNeighbors = data.getMap().getNeighbors(xTerr);
   					if (xNeighbors.contains(previousTerr) && xNeighbors.contains(nextTerr))
   					{
   						Route newRoute = new Route();
   						List<Territory> oldRTerrs = r.getTerritories();
   						newRoute.setStart(r.getStart());
   						for (int j=1; j < i; j++)
   							newRoute.add(oldRTerrs.get(j));
   						
   						newRoute.add(xTerr);
   						for (int k=i+1; k < rDist; k++)
   							newRoute.add(oldRTerrs.get(k));
   						
   						newRoute.add(destination);
   						r = newRoute;
   						switchTerr = true;
   					}
   				}
   				if(!switchTerr)
   					return null;
   			}
   		}
   		if(r == null)
            return null;
        Route newRoute = new Route();
        if(r.getLength() > maxDistance)
        {
           newRoute.setStart(start);
           for (int i = 1; i <= maxDistance; i++)
        	   newRoute.add( r.getTerritories().get(i) );
        }
        else
        	newRoute = r;
        return newRoute;
    }
    /**
     * Returns the players current pus available
     * @param data
     * @param player
     * @return
     */
	public static int getLeftToSpend(GameData data, PlayerID player)
	{
        Resource pus = data.getResourceList().getResource(Constants.PUS);
        return player.getResources().getQuantity(pus);
	}
	
	/**
	 * Is the territory a neighbor of an allied factory and is that factory threatened?
	 * Add in 50% of the factory's allied territory's strength + 100% of the strength of the factory territory
	 * 
	 * @param data
	 * @param eTerr
	 * @param player
	 * @return
	 */
	public static boolean territoryHasThreatenedAlliedFactoryNeighbor(GameData data, Territory eTerr, PlayerID player)
	{
		if (Matches.territoryHasAlliedFactoryNeighbor(data, player).invert().match(eTerr))
			return false;
		Set<Territory> aNeighbors = data.getMap().getNeighbors(eTerr);
		List<Territory> factTerr = new ArrayList<Territory>();
		for (Territory checkTerr : aNeighbors)
		{
			if (Matches.territoryHasAlliedFactory(data, player).match(checkTerr))
				factTerr.add(checkTerr);
		}
		boolean isThreatened = false;
		for (Territory factory : factTerr)
		{
			float eStrength = SUtils.getStrengthOfPotentialAttackers(factory, data, player, false, true, null);
			eStrength += eStrength* 1.15F + (eStrength > 2.0F ? 3.0F : 0.0F);
			float myStrength = SUtils.strength(factory.getUnits().getUnits(), false, false, false);
			if (eStrength > myStrength)
			{
				Set<Territory> factNeighbors = data.getMap().getNeighbors(factory, Matches.isTerritoryAllied(player, data));
				float addStrength = 0.0F;
				for (Territory fNTerr : factNeighbors)
				{
					addStrength += SUtils.strengthOfTerritory(data, fNTerr, player, false, false, false, true);
				}
				myStrength += addStrength*0.50F;
			}
			if (eStrength > myStrength)
				isThreatened = true;
		}
		return isThreatened;
	}
	
    /**
     * Territory ranking system
     * 
     * @param data
     * @param ourFriendlyTerr
     * @param ourEnemyTerr
     * @param player
     * @param tFirst
     * @param waterBased - attack is Water Based - Remove all terr with no avail water
     * @param nonCombat - if nonCombat, emphasize threatened factories over their neighbors
     * @return HashMap ranking of Territories
     */
    public static HashMap<Territory, Float> rankTerritories(GameData data, List<Territory> ourFriendlyTerr, List<Territory> ourEnemyTerr, List<Territory> ignoreTerr, PlayerID player, boolean tFirst, boolean waterBased, boolean nonCombat)
    {
		HashMap<Territory, Float> landRankMap = new HashMap<Territory, Float>();
		HashMap<Territory, Float> landStrengthMap = new HashMap<Territory, Float>();
		CompositeMatch<Territory> noEnemyOrWater = new CompositeMatchAnd<Territory>(Matches.TerritoryIsNotImpassableToLandUnits(player), Matches.isTerritoryAllied(player, data));
		CompositeMatch<Territory> enemyAndNoWater = new CompositeMatchAnd<Territory>(Matches.TerritoryIsNotImpassableToLandUnits(player), Matches.isTerritoryEnemyAndNotNuetralWater(player, data));
		TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();
        List<PlayerID> ePlayers = getEnemyPlayers(data, player);
    	PlayerID ePlayer = ePlayers.get(0);
    	List<Territory> enemyCapitals = SUtils.getEnemyCapitals(data, player);
    	Territory myCapital = TerritoryAttachment.getCapital(player, data);
    	int minDist = 1000;
    	int playerPUs = getLeftToSpend(data, player);
    	for (Territory eCapTerr : enemyCapitals)
    	{
    		int dist = data.getMap().getDistance(myCapital, eCapTerr);
    		minDist = Math.min(minDist, dist);
    	}
    	
		/**
		 * Send units because:
		 * 1) Production Value
		 * 2) Victory City
		 * 3) Has a Land Route to Enemy Capitol
		 * 4) Has enemy factory
		 * 5) Is close to enemy
		 * 6) Is closer than half the distance from cap to Enemy cap
		 */
    	List<Territory> alliedFactories = SUtils.getEnemyCapitals(data, ePlayer);
    	Iterator<Territory> aFIter = alliedFactories.iterator();
    	while (aFIter.hasNext())
    	{
    		Territory aFTerr = aFIter.next();
    		float aFPotential = SUtils.getStrengthOfPotentialAttackers(aFTerr, data, player, tFirst, true, null);
    		float alliedStrength = SUtils.strengthOfTerritory(data, aFTerr, player, false, false, tFirst, true);
    		if (aFPotential < alliedStrength*0.75F || aFPotential < 1.0F)
    			aFIter.remove();
    	}
    	List<Territory> aFNeighbors = new ArrayList<Territory>();
    	for (Territory aF : alliedFactories)
    	{
    		aFNeighbors.addAll(data.getMap().getNeighbors(aF, Matches.isTerritoryAllied(player, data)));
    	}
		for (Territory eTerr : data.getMap().getTerritories())
		{
			if (eTerr.isWater() || Matches.TerritoryIsImpassable.match(eTerr))
				continue;
			float alliedPotential = getStrengthOfPotentialAttackers(eTerr, data, ePlayer, tFirst, true, null);
			float rankStrength = getStrengthOfPotentialAttackers(eTerr, data, player, tFirst, true, ignoreTerr);
			float productionValue = (float)TerritoryAttachment.get(eTerr).getProduction();
			float eTerrValue = 0.0F;
			boolean island = !SUtils.doesLandExistAt(eTerr, data, false);
			eTerrValue += Matches.TerritoryIsVictoryCity.match(eTerr) ? 2.0F : 0.0F;
			boolean lRCap = hasLandRouteToEnemyOwnedCapitol(eTerr, player, data);
			eTerrValue += lRCap ? 2.0F : 0.0F;
			if (lRCap && (!Matches.territoryHasEnemyFactory(data, player).match(eTerr) && !Matches.territoryHasAlliedFactory(data, player).match(eTerr)))
			{
				Route eCapRoute = findNearest(eTerr, Matches.territoryHasEnemyFactory(data, player), Matches.TerritoryIsNotImpassableToLandUnits(player), data);
				if (eCapRoute != null)
					eTerrValue -= eCapRoute.getLength() - 1;
			}
			eTerrValue += Matches.territoryHasEnemyFactoryNeighbor(data, player).match(eTerr) ? 2.0F : 0.0F;
			int eMinDist = 1000;
			for (Territory eTerrCap : enemyCapitals)
			{
				int eDist = data.getMap().getDistance(eTerr, eTerrCap, Matches.TerritoryIsNotImpassable);
				eMinDist = Math.min(eMinDist, eDist);
			}
			eTerrValue -= eMinDist - 1;
//			eTerrValue += (eMinDist < minDist - 1) ? 4.0F : 0.0F; //bonus for general closeness to enemy Capital
			
			if (Matches.TerritoryIsLand.match(eTerr) && Matches.isTerritoryEnemyAndNotNeutral(player, data).match(eTerr))
			{
				ourEnemyTerr.add(eTerr);
				eTerrValue += productionValue;
				float eTerrStrength = strength(eTerr.getUnits().getMatches(Matches.enemyUnit(player, data)), false, false, tFirst);
				eTerrValue += alliedPotential > (rankStrength + eTerrStrength) ? productionValue : 0.0F;
				if (island)
					eTerrValue += 5.0F;
				eTerrValue += eTerr.getUnits().countMatches(Matches.UnitIsAir)*2; //bonus for killing air units
				eTerrValue += Matches.territoryHasEnemyFactory(data, player).match(eTerr) ? 4.0F : 0.0F;
				eTerrValue += Matches.territoryHasAlliedFactoryNeighbor(data, player).match(eTerr) ? 8.0F : 0.0F;
//				eTerrValue += (data.getMap().getNeighbors(eTerr, Matches.territoryHasAlliedFactory(data, player)).size() > 0 ? 3.0F : 0.0F);
				eTerrValue += Matches.territoryHasEnemyLandNeighbor(data, player).invert().match(eTerr) ? productionValue + 1 : 0.0F;
/*				if (data.getSequence().getRound() == 1)
				{
					eTerrValue += SUtils.doesLandExistAt(eTerr, data) ? 0.0F : 50.0F;
				}
*/				float netStrength = eTerrStrength - alliedPotential + 0.5F*rankStrength;
				landStrengthMap.put(eTerr, netStrength);
				landRankMap.put(eTerr, eTerrValue + netStrength*0.25F);
			}
			else if (Matches.isTerritoryAllied(player, data).match(eTerr) && Matches.TerritoryIsNotNeutral.match(eTerr))
			{
				boolean hasENeighbors = Matches.territoryHasEnemyLandNeighbor(data, player).match(eTerr);				
				Route testERoute = findNearest(eTerr, enemyAndNoWater, noEnemyOrWater, data);

				eTerrValue += (hasENeighbors ? 1.0F : -1.0F);
				eTerrValue += (aFNeighbors.contains(eTerr)) ? 8.0F : 0.0F;
				eTerrValue += (testERoute == null ? -1.0F : -(testERoute.getLength()-2));
				eTerrValue += (testERoute != null ? productionValue : 0.0F);
				float aTerrStrength = strength(eTerr.getUnits().getMatches(Matches.alliedUnit(player, data)), false, false, tFirst);
				//bonus for allied factory and allied factory with enemy neighbor
				boolean hasAlliedFactory = Matches.territoryHasAlliedFactory(data, player).match(eTerr);
				if (hasAlliedFactory)
				{
					eTerrValue +=  4.0F + (hasENeighbors && rankStrength > 5.0F ? 3.0F : 0.0F);
					alliedFactories.add(eTerr);
				}
				float netStrength = rankStrength - aTerrStrength - 0.5F*alliedPotential;
				landStrengthMap.put(eTerr, netStrength);
				landRankMap.put(eTerr, eTerrValue + netStrength*0.50F);
				if ((netStrength > -15.0F && rankStrength > 2.0F) || hasENeighbors || testERoute != null)
					ourFriendlyTerr.add(eTerr);
			}
			else if (Matches.TerritoryIsNeutral.match(eTerr))
			{
				if (Matches.TerritoryIsNotImpassable.match(eTerr) && (Matches.isTerritoryFreeNeutral(data).match(eTerr) || Properties.getNeutralCharge(data) <= playerPUs))
				{
					eTerrValue += -100.0F; // Make sure most neutral territories have lower priorities than enemy territories.
					boolean hasENeighbors = Matches.territoryHasEnemyLandNeighbor(data, player).match(eTerr);
					Route testERoute = findNearest(eTerr, enemyAndNoWater, noEnemyOrWater, data);
					eTerrValue += (hasENeighbors ? 1.0F : -1.0F);
					eTerrValue += (testERoute == null ? -1.0F : -(testERoute.getLength()-1));
					eTerrValue += productionValue > 0 ? productionValue : -5.0F;
					float netStrength = rankStrength - 0.5F*alliedPotential;
					landStrengthMap.put(eTerr, netStrength);
					landRankMap.put(eTerr, eTerrValue + netStrength*0.50F);
				}
			}
		}
		if (nonCombat)
		{ 
			CompositeMatch alliedLandTerr = new CompositeMatchAnd<Territory>(Matches.isTerritoryAllied(player, data), Matches.TerritoryIsLand, Matches.TerritoryIsNotImpassable);
			Set<Territory> terrList = landRankMap.keySet();
			for (Territory terr1 : alliedFactories)
			{
				float landRank = landRankMap.get(terr1);
				if (Matches.territoryHasEnemyLandNeighbor(data, player).match(terr1))
				{
					for (Territory neighbor : data.getMap().getNeighbors(terr1, alliedLandTerr))
					{
						float thisRank = landRankMap.get(neighbor);
						landRank = Math.max(landRank, thisRank);
					}
					landRank += 1.0F;
					landRankMap.put(terr1, landRank);
				}
			}
		}
		return landRankMap;

    }
    
    public static float twoAwayStrengthNotCounted(GameData data, PlayerID player, Territory eTerr)
    {
		List<Territory> blitzers = SUtils.possibleBlitzTerritories(eTerr, data, player);
		float nonBlitzStrength = 0.0F; //blitzStrength has already been included in the rankStrength...add in 2 away
		List<Territory> checkTerrs = new ArrayList<Territory>();
		for (Territory bTerr : blitzers)
		{
			List<Territory> bTNeighbors = new ArrayList<Territory>(data.getMap().getNeighbors(bTerr));
			bTNeighbors.removeAll(blitzers);
			bTNeighbors.remove(eTerr);
			for (Territory bT : bTNeighbors)
			{
				if (!checkTerrs.contains(bT))
					checkTerrs.add(bT);
			}
		}
		CompositeMatch<Territory> landPassable = new CompositeMatchAnd<Territory>(Matches.TerritoryIsLand, Matches.TerritoryIsNotImpassable);
		Iterator<Territory> bIter = checkTerrs.iterator();
		while (bIter.hasNext())
		{
			Territory newBTerr = bIter.next();
			Set<Territory> newBNeighbors = data.getMap().getNeighbors(newBTerr, landPassable);
			boolean blitzCounted = false;
			Iterator<Territory> newBNIter = newBNeighbors.iterator();
			while (!blitzCounted && newBNIter.hasNext())
			{
				Territory bCheck = newBNIter.next();
				if (blitzers.contains(bCheck) && bCheck.getUnits().getMatches(Matches.alliedUnit(player, data)).isEmpty())
					blitzCounted = true;
			}
			if (!blitzCounted && Matches.isTerritoryEnemyAndNotNuetralWater(player, data).match(newBTerr))
				nonBlitzStrength += SUtils.strength(newBTerr.getUnits().getMatches(Matches.enemyUnit(player, data)), true, false, false);
		}
		return nonBlitzStrength;
    }

    /**
     * Target Territories for amphibious non-combat movement
     * No Islands will result
     * 
     * @param data
     * @param ignoreTerr
     * @param player
     * @param tFirst
     * @param nonCombat
     * @return
     */
    public static HashMap<Territory, Float> rankAmphibReinforcementTerritories(GameData data, List<Territory> ignoreTerr, PlayerID player, boolean tFirst)
    {
		HashMap<Territory, Float> landRankMap = new HashMap<Territory, Float>();
		HashMap<Territory, Float> landStrengthMap = new HashMap<Territory, Float>();
		CompositeMatch<Territory> noEnemyOrWater = new CompositeMatchAnd<Territory>(Matches.TerritoryIsNotImpassableToLandUnits(player), Matches.isTerritoryAllied(player, data));
		CompositeMatch<Territory> enemyAndNoWater = new CompositeMatchAnd<Territory>(Matches.TerritoryIsNotImpassableToLandUnits(player), Matches.isTerritoryEnemyAndNotNuetralWater(player, data));
        List<PlayerID> ePlayers = getEnemyPlayers(data, player);
    	PlayerID ePlayer = ePlayers.get(0);
    	List<Territory> enemyCapitals = SUtils.getEnemyCapitals(data, player);
    	Territory myCapital = TerritoryAttachment.getCapital(player, data);
    	int minDist = 1000;
    	int playerIPCs = getLeftToSpend(data, player);
    	Territory targetCap = null;
    	for (Territory eCapTerr : enemyCapitals)
    	{
    		int dist = data.getMap().getDistance(myCapital, eCapTerr);
    		if (minDist > dist)
    		{
    			minDist = dist;
    			targetCap = eCapTerr;
    		}
    	}
    	CompositeMatch<Territory> continentTerr = new CompositeMatchAnd<Territory>(Matches.isTerritoryAllied(player, data), Matches.territoryHasValidLandRouteTo(data, targetCap));
		/**
		 * Send units because:
		 * 1) Production Value
		 * 2) Victory City
		 * 3) Has a Land Route to Enemy Capitol
		 * 4) Has enemy factory
		 * 5) Is close to enemy
		 * 6) Is close to a threatened allied capital
		 */
    	List<Territory> alliedFactories = new ArrayList<Territory>();
		for (Territory aTerr : data.getMap().getTerritories())
		{
			if (!continentTerr.match(aTerr) || Matches.isTerritoryEnemy(player, data).match(aTerr) || Matches.TerritoryIsImpassable.match(aTerr) || Matches.territoryHasWaterNeighbor(data).invert().match(aTerr))
				continue;
			float alliedPotential = getStrengthOfPotentialAttackers(aTerr, data, ePlayer, tFirst, true, null);
			float localStrength = SUtils.strength(aTerr.getUnits().getUnits(), false, false, tFirst);
			float rankStrength = getStrengthOfPotentialAttackers(aTerr, data, player, tFirst, true, ignoreTerr);
			float productionValue = (float)TerritoryAttachment.get(aTerr).getProduction();
			float aTerrValue = 0.0F;
			aTerrValue += Matches.TerritoryIsVictoryCity.match(aTerr) ? 2.0F : 0.0F;
			aTerrValue += Matches.territoryHasEnemyFactoryNeighbor(data, player).match(aTerr) ? 2.0F : 0.0F;
			aTerrValue -= data.getMap().getDistance(aTerr, targetCap, Matches.TerritoryIsNotImpassable)-1;
			Territory capTerr = aTerr;
			if (Matches.territoryHasAlliedFactoryNeighbor(data, player).equals(aTerr))
			{
				Set<Territory> neighbors = data.getMap().getNeighbors(aTerr, Matches.territoryHasAlliedFactory(data, player));
				if (!neighbors.isEmpty())
					capTerr = neighbors.iterator().next();
			}
			if ( Matches.territoryHasAlliedFactory(data, player).match(capTerr) ) //does this need reinforcing?
			{
				float addCapValue = aTerr.equals(capTerr) ? 5.0F : 0.0F;
				if (rankStrength > alliedPotential + localStrength)
					aTerrValue += 10.0F + addCapValue;
				else
				{
					float xValue = SUtils.twoAwayStrengthNotCounted(data, player, aTerr);
					if (rankStrength + xValue > (alliedPotential + localStrength)*1.05F)
						aTerrValue += 10.0F + addCapValue;
				}
			}
			
			boolean hasENeighbors = Matches.territoryHasEnemyLandNeighbor(data, player).match(aTerr);
			Route testERoute = findNearest(aTerr, enemyAndNoWater, noEnemyOrWater, data);
			
			aTerrValue += (hasENeighbors ? 1.0F : -1.0F);
			aTerrValue += (hasENeighbors && Matches.territoryHasAlliedFactoryNeighbor(data, player).match(aTerr)) ? 5.0F : 0.0F;
			aTerrValue += (testERoute == null ? -1.0F : -(testERoute.getLength()-1));
			aTerrValue += (testERoute != null ? productionValue : 0.0F);
			float aTerrStrength = strength(aTerr.getUnits().getMatches(Matches.alliedUnit(player, data)), false, false, tFirst);
			//bonus for allied factory and allied factory with enemy neighbor
			boolean hasAlliedFactory = Matches.territoryHasAlliedFactory(data, player).match(aTerr);
			if (hasAlliedFactory)
			{
				aTerrValue +=  4.0F + (hasENeighbors && rankStrength > 5.0F ? 3.0F : 0.0F);
				alliedFactories.add(aTerr);
			}
			boolean worthTroopDrop = (aTerrStrength + alliedPotential) > (rankStrength - 3.0F)*0.80F;
			worthTroopDrop = worthTroopDrop && (aTerrStrength + 0.80F*alliedPotential) < 1.25F*(rankStrength + 3.0F);
			aTerrValue += worthTroopDrop ? 5.0F : -2.0F;
			float netStrength = rankStrength - aTerrStrength - 0.8F*alliedPotential;
			landStrengthMap.put(aTerr, netStrength);
			landRankMap.put(aTerr, aTerrValue); 
		}
		return landRankMap;

    }

}
