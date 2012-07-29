package games.strategy.triplea.ai.strongAI;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.BattleCalculator;
import games.strategy.triplea.delegate.BattleDelegate;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveValidator;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.IntegerMap;
import games.strategy.util.InverseMatch;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
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
	public static boolean threatToAlliedCapitals(final GameData data, final PlayerID player, final List<Territory> threats, final boolean tFirst)
	{
		final List<Territory> alliedCapitols = new ArrayList<Territory>();
		for (final PlayerID otherPlayer : data.getPlayerList().getPlayers())
		{
			if (otherPlayer == player)
				continue;
			final Territory capitol = TerritoryAttachment.getCapital(otherPlayer, data);
			if (capitol != null && data.getRelationshipTracker().isAllied(player, capitol.getOwner()))
				alliedCapitols.add(capitol);
		}
		for (final Territory cap : alliedCapitols)
		{
			final float landThreat = getStrengthOfPotentialAttackers(cap, data, player, tFirst, true, null);
			final float capStrength = strength(cap.getUnits().getMatches(Matches.alliedUnit(player, data)), false, false, tFirst) + 5.0F;
			if (capStrength * 1.05F < landThreat) // trouble
			{
				threats.add(cap);
			}
		}
		return threats.size() > 0;
	}
	
	/**
	 * Returns a List of all territories with a water neighbor
	 * 
	 * @param data
	 * @param allTerr
	 *            - List of Territories
	 * @return
	 */
	public static List<Territory> stripLandLockedTerr(final GameData data, final List<Territory> allTerr)
	{
		final List<Territory> waterTerrs = new ArrayList<Territory>(allTerr);
		final Iterator<Territory> wFIter = waterTerrs.iterator();
		while (wFIter.hasNext())
		{
			final Territory waterFact = wFIter.next();
			if (Matches.territoryHasWaterNeighbor(data).invert().match(waterFact))
				wFIter.remove();
		}
		return waterTerrs;
	}
	
	// returns all territories that are water territories. used to remove convoy zones from places the ai will put a factory (veqryn)
	public static List<Territory> onlyWaterTerr(final GameData data, final List<Territory> allTerr)
	{
		final List<Territory> water = new ArrayList<Territory>(allTerr);
		final Iterator<Territory> wFIter = water.iterator();
		while (wFIter.hasNext())
		{
			final Territory waterFact = wFIter.next();
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
	public static Collection<Territory> bomberTerrInList(final List<Collection<Unit>> units, final List<Route> routes)
	{
		final Collection<Territory> bTerrs = new ArrayList<Territory>();
		for (int i = 0; i < units.size(); i++)
		{
			final Collection<Unit> checkUnits = units.get(i);
			final Route checkRoute = routes.get(i);
			if (checkRoute == null || checkRoute.getEnd() == null)
				continue;
			final Territory endTerr = checkRoute.getEnd();
			if (Match.someMatch(checkUnits, Matches.UnitIsStrategicBomber))
				bTerrs.add(endTerr);
		}
		return bTerrs;
	}
	
	/**
	 * Prepares a map of the strength of every enemy land Territory
	 * Includes neutral if they are attackable
	 * 
	 * @param data
	 * @param player
	 * @param enemyMap
	 * @return
	 */
	public static Territory landAttackMap(final GameData data, final PlayerID player, final HashMap<Territory, Float> enemyMap)
	{
		Territory largestTerr = null;
		final List<Territory> enemyTerrs = SUtils.allEnemyTerritories(data, player);
		if (enemyTerrs.isEmpty())
			return null;
		final Iterator<Territory> eTIter = enemyTerrs.iterator();
		while (eTIter.hasNext())
		{
			final Territory eTerr = eTIter.next();
			if (Matches.TerritoryIsWater.match(eTerr) || Matches.TerritoryIsImpassable.match(eTerr))
				eTIter.remove();
			else
			{
				final float eStrength = SUtils.strength(eTerr.getUnits().getMatches(Matches.enemyUnit(player, data)), true, false, true);
				enemyMap.put(eTerr, eStrength);
			}
		}
		SUtils.reorder(enemyTerrs, enemyMap, true);
		if (enemyTerrs.isEmpty())
		{
			return null;
		}
		largestTerr = enemyTerrs.get(0);
		return largestTerr;
	}
	
	/**
	 * Assumes that water is passable to air units always
	 * 
	 * @param data
	 * @return
	 */
	public static Match<Territory> TerritoryIsImpassableToAirUnits(final GameData data)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				if (Matches.TerritoryIsLand.match(t) && Matches.TerritoryIsImpassable.match(t))
					return true;
				return false;
			}
		};
	}
	
	public final static Match<Territory> TerritoryIsNotImpassableToAirUnits(final GameData data)
	{
		return new InverseMatch<Territory>(TerritoryIsImpassableToAirUnits(data));
	}
	
	/**
	 * Return a list of Territories on a Continent which are allied
	 * 
	 * @param data
	 * @param player
	 * @param startTerr
	 * @param contiguousTerr
	 *            - actual list to be created
	 * @param ignoreTerr
	 *            - cannot be null (should be an empty List<Territory>) - Add Territories here to ignore them
	 */
	public static void continentAlliedUnitTerr(final GameData data, final PlayerID player, final Territory startTerr, final List<Territory> contiguousTerr, final List<Territory> ignoreTerr)
	{
		final Set<Territory> neighbor1 = data.getMap().getNeighbors(startTerr, Matches.TerritoryIsNotImpassableToLandUnits(player, data));
		neighbor1.removeAll(contiguousTerr);
		neighbor1.removeAll(ignoreTerr);
		for (final Territory n1 : neighbor1)
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
	 * 
	 * @param data
	 * @param player
	 * @param continentTerr
	 * @return
	 */
	public static float strengthOnContinent(final GameData data, final PlayerID player, final Territory continentTerr)
	{
		float continentStrength = 0.0F;
		final boolean island = !SUtils.doesLandExistAt(continentTerr, data, false); // just make sure this is really a "continent"
		if (island)
		{
			if (Matches.isTerritoryAllied(player, data).match(continentTerr))
			{
				continentStrength += SUtils.strength(continentTerr.getUnits().getMatches(Matches.alliedUnit(player, data)), true, false, false);
				continentStrength += 5.0F; // make sure that an empty terr doesn't get a 0.0F value
			}
			return continentStrength;
		}
		final List<Territory> allContinentTerr = new ArrayList<Territory>();
		final List<Territory> ignoreTerr = new ArrayList<Territory>();
		SUtils.continentAlliedUnitTerr(data, player, continentTerr, allContinentTerr, ignoreTerr);
		for (final Territory cTerr : allContinentTerr)
			continentStrength += SUtils.strength(cTerr.getUnits().getMatches(Matches.alliedUnit(player, data)), true, false, false);
		return continentStrength;
	}
	
	/**
	 * Remove enemy territories, territories which have no threat and no enemy land neighbor
	 * 
	 * @param data
	 * @param player
	 * @param terrList
	 */
	public static void removeUnthreatenedTerritories(final GameData data, final PlayerID player, final List<Territory> terrList)
	{
		final Iterator<Territory> tIter = terrList.iterator();
		while (tIter.hasNext())
		{
			final Territory checkTerr = tIter.next();
			if (Matches.isTerritoryEnemy(player, data).match(checkTerr))
				tIter.remove();
			else
			{
				final float eStrength = SUtils.getStrengthOfPotentialAttackers(checkTerr, data, player, false, false, null);
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
	public static void breakUnitsBySpeed(final List<Collection<Unit>> returnUnits, final GameData data, final PlayerID player, final List<Unit> units)
	{
		if (units.isEmpty())
			return;
		final int maxSpeed = MoveValidator.getMaxMovement(units);
		final List<Unit> copyOfUnits = new ArrayList<Unit>(units);
		for (int i = maxSpeed; i >= 0; i--)
		{
			final Collection<Unit> newUnits = new ArrayList<Unit>();
			final Iterator<Unit> unitIter = copyOfUnits.iterator();
			while (unitIter.hasNext())
			{
				final Unit unit1 = unitIter.next();
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
	 * 
	 * @param data
	 * @param player
	 * @return
	 */
	public static IntegerMap<Territory> targetTerritories(final GameData data, final PlayerID player, final int tDistance)
	{
		final CompositeMatch<Unit> enemyFactory = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data), Matches.UnitCanProduceUnits);
		final List<Territory> playerFactories = SUtils.findUnitTerr(data, player, enemyFactory);
		final IntegerMap<Territory> targetMap = new IntegerMap<Territory>();
		final int checkDist = tDistance - 1;
		for (final Territory mT : playerFactories)
		{
			final Collection<Territory> initialGroup = data.getMap().getNeighbors(mT, tDistance);
			for (final Territory checkTerr : initialGroup)
			{
				if (Matches.isTerritoryEnemy(player, data).match(checkTerr) && Matches.TerritoryIsLand.match(checkTerr) && Matches.TerritoryIsNotImpassable.match(checkTerr))
				{
					final int cDist = data.getMap().getDistance(mT, checkTerr);
					if (cDist < checkDist || (cDist >= checkDist && !SUtils.doesLandExistAt(checkTerr, data, false)))
					{
						final int terrProduction = TerritoryAttachment.get(checkTerr).getProduction();
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
	public static List<Unit> sortTransportUnits(final List<Unit> transUnits)
	{
		final List<Unit> sorted = new ArrayList<Unit>();
		final List<Unit> infantry = new ArrayList<Unit>();
		final List<Unit> artillery = new ArrayList<Unit>();
		final List<Unit> armor = new ArrayList<Unit>();
		final List<Unit> others = new ArrayList<Unit>();
		for (final Unit x : transUnits)
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
		final int infCount = infantry.size();
		int othersCount = others.size();
		for (int j = 0; j < infCount; j++) // interleave the artillery and armor with inf
		{
			sorted.add(infantry.get(j));
			// this should be based on combined attack and defense powers, not on attachments like blitz
			if (armorCount > 0)
			{
				sorted.add(armor.get(armorCount - 1));
				armorCount--;
			}
			else if (artilleryCount > 0)
			{
				sorted.add(artillery.get(artilleryCount - 1));
				artilleryCount--;
			}
			else if (othersCount > 0)
			{
				sorted.add(others.get(othersCount - 1));
				othersCount--;
			}
		}
		if (artilleryCount > 0)
		{
			for (int j2 = 0; j2 < artilleryCount; j2++)
				sorted.add(artillery.get(j2));
		}
		if (othersCount > 0)
		{
			for (int j4 = 0; j4 < othersCount; j4++)
				sorted.add(others.get(j4));
		}
		if (armorCount > 0)
		{
			for (int j3 = 0; j3 < armorCount; j3++)
				sorted.add(armor.get(j3));
		}
		return sorted;
	}
	
	/**
	 * Generate HashMap of the costs of all players
	 */
	public static HashMap<PlayerID, IntegerMap<UnitType>> getPlayerCostMap(final GameData data)
	{
		final HashMap<PlayerID, IntegerMap<UnitType>> costMap = new HashMap<PlayerID, IntegerMap<UnitType>>();
		final Collection<PlayerID> playerList = data.getPlayerList().getPlayers();
		for (final PlayerID cPlayer : playerList)
		{
			final IntegerMap<UnitType> playerCostMap = BattleCalculator.getCostsForTUV(cPlayer, data);
			costMap.put(cPlayer, playerCostMap);
		}
		return costMap;
	}
	
	/**
	 * Trim a list of territories to a maximum number
	 * If the list has fewer entries than max number, nothing is done
	 * 
	 * @param xTerrList
	 * @param maxNum
	 *            - maxNum of entries
	 */
	public static void trimTerritoryList(final List<Territory> xTerrList, final int maxNum)
	{
		final int totNum = xTerrList.size();
		if (maxNum >= totNum)
			return;
		final Iterator<Territory> xIter = xTerrList.iterator();
		int maxCount = 0;
		while (xIter.hasNext())
		{
			// Territory test = xIter.next();
			if (maxCount > maxNum)
				xIter.remove();
			maxCount++;
		}
	}
	
	public static Route TrimRoute_BeforeFirstTerWithEnemyUnits(final Route route, final int newRouteJumpCount, final PlayerID player, final GameData data)
	{
		final List<Territory> newTers = new ArrayList<Territory>();
		int i = 0;
		for (final Territory ter : route.getTerritories())
		{
			if (ter.getUnits().getMatches(new CompositeMatchAnd<Unit>(Matches.unitHasDefenseThatIsMoreThanOrEqualTo(1), Matches.unitIsEnemyOf(data, player))).size() > 0)
				break;
			newTers.add(ter);
			i++;
			if (i > newRouteJumpCount)
				break;
		}
		return new Route(newTers);
	}
	
	public static int evaluateNonCombat(final int numMoves, final int evalDistance, final HashMap<Integer, HashMap<Territory, Float>> reinforcedTerrList,
				final HashMap<Integer, IntegerMap<Territory>> unitCountList, final PlayerID player, final GameData data, final boolean tFirst)
	{
		/**
		 * All Enemy Territories - Count units within 4 spaces as: 40%, 60%, 90%, 100% x productionvalue
		 * Enemy Factories - Add 1 to Production Value
		 * Enemy Capital - Add another 1 to Production Value
		 */
		if (evalDistance > 10) // limit to 10..really is 9 because 10 will be a factor of 0
			return -1;
		final CompositeMatch<Unit> enemyFactory = new CompositeMatchAnd<Unit>(Matches.UnitCanProduceUnits, Matches.enemyUnit(player, data));
		// CompositeMatch<Unit> alliedFactory = new CompositeMatchAnd<Unit>(Matches.UnitIsFactory, Matches.alliedUnit(player, data));
		int bestFit = -1;
		float maxScore = 0.0F;
		final List<Territory> enemyCaps = SUtils.getEnemyCapitals(data, player);
		final List<Territory> enemyFactories = SUtils.findUnitTerr(data, player, enemyFactory);
		/*    	List<Territory> ourFriendlyTerr = new ArrayList<Territory>();
		    	List<Territory> ourEnemyTerr = new ArrayList<Territory>();
		    	HashMap<Territory, Float> rankMap = SUtils.rankTerritories(data, ourFriendlyTerr, ourEnemyTerr, null, player, tFirst, false);
		*/
		final List<Territory> allEnemyTerr = SUtils.allEnemyTerritories(data, player);
		final IntegerMap<Territory> productionMap = new IntegerMap<Territory>();
		for (final Territory enemy : allEnemyTerr)
		{
			int prodValue = TerritoryAttachment.get(enemy).getProduction();
			if (enemyCaps.contains(enemy))
				prodValue++;
			if (enemyFactories.contains(enemy))
				prodValue++;
			prodValue++;
			productionMap.put(enemy, prodValue);
		}
		final Set<Integer> keySet = reinforcedTerrList.keySet();
		s_logger.fine("Moves Available for: " + keySet);
		for (Integer i = 0; i <= numMoves - 1; i++)
		{
			final HashMap<Territory, Float> reinforcedTerr = reinforcedTerrList.get(i);
			// IntegerMap<Territory> unitCount = unitCountList.get(i);
			final Set<Territory> goTerr = reinforcedTerr.keySet();
			float score = 0.0F;
			for (final Territory eTerr : allEnemyTerr)
			{
				final List<Territory> eNeighbors = new ArrayList<Territory>();
				for (int j = evalDistance; j > 0; j--)
				{
					eNeighbors.addAll(SUtils.getExactNeighbors(eTerr, j, player, data, false));
					for (final Territory eN : eNeighbors)
					{
						if (goTerr.contains(eN))
							score += reinforcedTerr.get(eN) * (1.0F - (j - 1) * 0.20) * productionMap.getInt(eTerr);
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
	 * 
	 * @return IntegerMap<PlayerID> - contains TUV of each player
	 */
	public static IntegerMap<PlayerID> getPlayerTUV(final GameData data)
	{
		final Collection<PlayerID> playerList = data.getPlayerList().getPlayers();
		final IntegerMap<PlayerID> TUVMap = new IntegerMap<PlayerID>();
		for (final PlayerID qSet : playerList)
			TUVMap.put(qSet, 0); // initialize map
		final HashMap<PlayerID, IntegerMap<UnitType>> costMap = getPlayerCostMap(data);
		for (final Territory allTerr : data.getMap().getTerritories())
		{
			for (final PlayerID onePlayer : playerList)
			{
				final CompositeMatch<Unit> nonSeaUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(onePlayer), Matches.UnitIsNotSea);
				final Collection<Unit> playerUnits = allTerr.getUnits().getMatches(nonSeaUnit);
				final IntegerMap<UnitType> uMap = SUtils.convertListToMap(playerUnits);
				int tuv = TUVMap.getInt(onePlayer);
				tuv += SUtils.determineTUV(uMap, costMap.get(onePlayer));
				TUVMap.put(onePlayer, tuv);
			}
		}
		return TUVMap;
	}
	
	/**
	 * Return all land Territories which are within range between startTerr & targetTerr
	 * 
	 * @param startTerr
	 * @param targetTerr
	 * @param data
	 * @return
	 */
	public static Collection<Territory> islandCapitalTerritories(final Territory startTerr, final Territory targetTerr, final GameData data)
	{
		final Collection<Territory> goTerrs = new ArrayList<Territory>();
		final int maxDistance = data.getMap().getDistance(startTerr, targetTerr) + 1;
		final Set<Territory> allTerrs = data.getMap().getNeighbors(targetTerr, 5);
		final Iterator<Territory> aTIter = allTerrs.iterator();
		while (aTIter.hasNext())
		{
			final Territory xTerr = aTIter.next();
			final int xDistance = data.getMap().getDistance(startTerr, xTerr);
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
	 * @param contiguousTerritories
	 *            - Set of 3 potential landing points
	 * @param player
	 * @param tFirst
	 * @return
	 */
	public static Territory closestAmphibAlliedTerritory(final Territory startTerr, final Territory targetTerr, final List<Territory> contiguousTerritories, final GameData data,
				final PlayerID player, final boolean tFirst)
	{
		// int distance = data.getMap().getDistance(startTerr, targetTerr); // raw distance
		final List<Territory> ignoreTerr = new ArrayList<Territory>();
		SUtils.continentAlliedUnitTerr(data, player, targetTerr, contiguousTerritories, ignoreTerr);
		if (contiguousTerritories.isEmpty())
		{
			s_logger.fine("Player: " + player.getName() + "; Territory must be an island: " + targetTerr.getName());
			return null;
		}
		final IntegerMap<Territory> unitMap = new IntegerMap<Territory>();
		final IntegerMap<Territory> distanceMap = new IntegerMap<Territory>();
		final Iterator<Territory> cIter = contiguousTerritories.iterator();
		while (cIter.hasNext())
		{
			final Territory checkTerr = cIter.next();
			if (Matches.territoryHasWaterNeighbor(data).match(checkTerr))
			{
				final int checkDist = data.getMap().getDistance(startTerr, checkTerr) + data.getMap().getLandDistance(checkTerr, targetTerr);
				final int unitCount = checkTerr.getUnits().countMatches(Matches.alliedUnit(player, data));
				unitMap.put(checkTerr, unitCount);
				distanceMap.put(checkTerr, checkDist);
			}
			else
				cIter.remove();
		}
		SUtils.reorder(contiguousTerritories, distanceMap, false);
		SUtils.trimTerritoryList(contiguousTerritories, 3); // look at the top 3
		boolean isWaterNeighbor = false;
		Territory goTerr = null;
		final Iterator<Territory> checkIter = contiguousTerritories.iterator();
		while (checkIter.hasNext() && !isWaterNeighbor)
		{
			final Territory checkTerr2 = checkIter.next();
			final Set<Territory> waterNeighbors = data.getMap().getNeighbors(checkTerr2, Matches.TerritoryIsWater);
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
	 * 
	 * @param startTerr
	 * @param data
	 * @param player
	 * @return
	 */
	public static Territory closestEnemyCapital(final Territory startTerr, final GameData data, final PlayerID player)
	{
		final List<Territory> eCapitols = SUtils.getEnemyCapitals(data, player);
		int maxDistance = 100;
		Territory goCap = null;
		if (eCapitols.isEmpty())
			return goCap;
		for (final Territory eCap : eCapitols)
		{
			final int newDist = data.getMap().getDistance(startTerr, eCap);
			if (newDist <= maxDistance)
			{
				goCap = eCap;
				maxDistance = newDist;
			}
		}
		return goCap;
	}
	
	public static boolean calculateTUVDifference(final Territory eTerr, final Collection<Unit> invasionUnits, final Collection<Unit> defenderUnits,
				final HashMap<PlayerID, IntegerMap<UnitType>> costMap, final PlayerID player, final GameData data, final boolean aggressive, final boolean subRestricted, final boolean tFirst)
	{
		int evaluationFactor = -5;
		if (aggressive)
			evaluationFactor = -2;
		final IntegerMap<UnitType> myCostMap = costMap.get(player);
		final PlayerID ePlayer = eTerr.getOwner();
		final IntegerMap<UnitType> playerCostMap = costMap.get(ePlayer);
		final int eTUV = (Matches.TerritoryIsNeutralButNotWater.match(eTerr)) ? 0 : BattleCalculator.getTUV(defenderUnits, ePlayer, playerCostMap, data);
		final int myTUV = BattleCalculator.getTUV(invasionUnits, myCostMap);
		final IntegerMap<UnitType> myAttackUnits = convertListToMap(invasionUnits);
		final IntegerMap<UnitType> defenseUnits = convertListToMap(defenderUnits);
		s_logger.fine("Territory: " + eTerr.getName() + "; myTUV: " + myTUV + "; EnemyTUV: " + eTUV);
		final boolean weWin = quickBattleEstimator(myAttackUnits, defenseUnits, player, ePlayer, false, subRestricted);
		final int myNewTUV = determineTUV(myAttackUnits, myCostMap);
		final IntegerMap<UnitType> eCostMap = costMap.get(ePlayer);
		final int eNewTUV = (Matches.TerritoryIsNeutralButNotWater.match(eTerr)) ? 0 : determineTUV(defenseUnits, eCostMap);
		int production = TerritoryAttachment.get(eTerr).getProduction();
		if (Matches.TerritoryIsNeutralButNotWater.match(eTerr))
			production *= 3;
		final int myTUVLost = (myTUV - myNewTUV) - (weWin ? production : 0);
		final int eTUVLost = eTUV - eNewTUV;
		s_logger.fine("Territory: " + eTerr.getName() + "; myTUV: " + myNewTUV + "; EnemyTUV: " + eNewTUV + "; My TUV Lost: " + myTUVLost + "; Enemy TUV Lost: " + eTUVLost);
		s_logger.fine("Aggressive: " + aggressive + "; Evaluation Factor: " + evaluationFactor);
		// failsafe, just freaking attack already damnit!
		if (weWin && (eTUV == 0 || (((SUtils.strength(defenderUnits, false, eTerr.isWater(), tFirst) * 5F) + 10F) < SUtils.strength(invasionUnits, true, eTerr.isWater(), tFirst))))
			return true; // essentially, when a player can't build a unit, it has ZERO TUV, so this means the AIs will never attack it if they consider TUV value. this is because the TUV is not listed in the cost map
		if (weWin && (myTUVLost <= (eTUVLost + (7 + evaluationFactor))))
			return true;
		else if (myTUVLost < (eTUVLost + evaluationFactor))
			return true;
		return false;
	}
	
	public static int determineTUV(final IntegerMap<UnitType> unitList, final IntegerMap<UnitType> unitCost)
	{
		int totalValue = 0;
		final Set<UnitType> uTypes = unitList.keySet();
		for (final UnitType uType : uTypes)
			totalValue += unitList.getInt(uType) * unitCost.getInt(uType);
		return totalValue;
	}
	
	/**
	 * All the territories that border one of our territories
	 */
	public static List<Territory> getNeighboringEnemyLandTerritories(final GameData data, final PlayerID player)
	{
		final ArrayList<Territory> rVal = new ArrayList<Territory>();
		for (final Territory t : data.getMap())
		{
			if (Matches.isTerritoryEnemy(player, data).match(t) && Matches.TerritoryIsLand.match(t) && Matches.TerritoryIsNotImpassable.match(t))
			{
				if (!data.getMap().getNeighbors(t, Matches.isTerritoryOwnedBy(player)).isEmpty())
					rVal.add(t);
			}
		}
		return rVal;
	}
	
	/**
	 * All allied Territories which have a Land Enemy Neighbor
	 * 
	 * @neutral - include neutral territories
	 * @allied - include allied territories
	 *         return - List of territories
	 */
	public static List<Territory> getTerritoriesWithEnemyNeighbor(final GameData data, final PlayerID player, final boolean allied, final boolean neutral)
	{
		final List<Territory> ourTerr = new ArrayList<Territory>();
		final List<Territory> enemyLandTerr = SUtils.allEnemyTerritories(data, player);
		if (!neutral)
		{
			final Iterator<Territory> eIter = enemyLandTerr.iterator();
			while (eIter.hasNext())
			{
				final Territory checkTerr = eIter.next();
				if (Matches.TerritoryIsNeutralButNotWater.match(checkTerr))
					eIter.remove();
			}
		}
		final Iterator<Territory> eIter = enemyLandTerr.iterator();
		while (eIter.hasNext())
		{
			final Territory enemy = eIter.next();
			if (SUtils.doesLandExistAt(enemy, data, false))
			{
				final List<Territory> newTerrs = new ArrayList<Territory>();
				if (allied)
					newTerrs.addAll(SUtils.getNeighboringLandTerritories(data, player, enemy));
				else
					newTerrs.addAll(data.getMap().getNeighbors(enemy, Matches.isTerritoryOwnedBy(player)));
				for (final Territory nT : newTerrs)
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
	public static List<Territory> getNeighboringEnemyLandTerritories(final GameData data, final PlayerID player, final boolean allied)
	{
		final ArrayList<Territory> rVal = new ArrayList<Territory>();
		final CompositeMatch<Territory> enemyLand = new CompositeMatchAnd<Territory>(Matches.isTerritoryEnemy(player, data), Matches.TerritoryIsNotImpassable, Matches.TerritoryIsLand);
		for (final Territory t : data.getMap().getTerritories())
		{
			if (enemyLand.match(t))
			{
				if (allied)
				{
					if (!data.getMap().getNeighbors(t, Matches.isTerritoryAllied(player, data)).isEmpty())
						rVal.add(t);
				}
				else if (!data.getMap().getNeighbors(t, Matches.isTerritoryOwnedBy(player)).isEmpty())
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
	public static List<Territory> getNeighboringNeutralLandTerritories(final GameData data, final PlayerID player, final boolean allied)
	{
		final ArrayList<Territory> rVal = new ArrayList<Territory>();
		for (final Territory t : data.getMap())
		{
			if (Matches.isTerritoryFreeNeutral(data).match(t) && !t.isWater())
			{
				final TerritoryAttachment ta = TerritoryAttachment.get(t);
				if (ta == null)
					continue;
				// There is no immediate gain from attacking a neutral country without production,
				// so they are not included. On the other hand, sometimes conquering those neutral countries
				// might, on rare occasions, give you a strategic advantage on the next turn.
				if (ta.getProduction() == 0 || ta.getIsImpassible())
					continue;
				if (allied && !data.getMap().getNeighbors(t, Matches.isTerritoryAllied(player, data)).isEmpty())
					rVal.add(t);
				else if (!data.getMap().getNeighbors(t, Matches.isTerritoryOwnedBy(player)).isEmpty())
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
	public static Territory getClosestWaterTerr(final Territory target, final Territory source, final GameData data, final PlayerID player, final boolean allowEnemy)
	{
		CompositeMatch<Territory> waterCond = null;
		if (allowEnemy)
			waterCond = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater);
		else
			waterCond = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasNoEnemyUnits(player, data));
		final Set<Territory> waterTerr = data.getMap().getNeighbors(target, waterCond);
		Territory result = null;
		int minDist = 0;
		if (waterTerr.size() == 0)
		{
			minDist = 0;
			return result;
		}
		else if (waterTerr.contains(source))
		{
			minDist = 0;
			return source;
		}
		minDist = 100;
		int thisDist = 100;
		for (final Territory checkTerr : waterTerr)
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
	public static Territory getSafestWaterTerr(final Territory target, final Territory source, final List<Territory> ignoreTerr, final GameData data, final PlayerID player, final boolean allowEnemy,
				final boolean tFirst)
	{
		CompositeMatch<Territory> waterCond = null;
		final CompositeMatch<Unit> alliedSeaUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsNotLand, Matches.alliedUnit(player, data));
		if (allowEnemy)
			waterCond = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater);
		else
			waterCond = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasNoEnemyUnits(player, data));
		final Set<Territory> waterTerr = data.getMap().getNeighbors(target, waterCond);
		Territory result = null;
		if (waterTerr.size() == 0)
			return result;
		final HashMap<Territory, Float> waterStrength = new HashMap<Territory, Float>();
		float eStrength = 0.0F;
		// float ourStrength = 0.0F;
		for (final Territory xWaterTerr : waterTerr)
		{
			eStrength = getStrengthOfPotentialAttackers(xWaterTerr, data, player, tFirst, false, ignoreTerr);
			if (ignoreTerr == null || !ignoreTerr.contains(xWaterTerr))
				eStrength += strength(xWaterTerr.getUnits().getMatches(Matches.enemyUnit(player, data)), true, true, tFirst);
			eStrength -= SUtils.strength(xWaterTerr.getUnits().getMatches(alliedSeaUnit), false, true, tFirst);
			if (xWaterTerr == source && eStrength <= 0.0F)
				return xWaterTerr;
			waterStrength.put(xWaterTerr, -eStrength);
		}
		final List<Territory> waterTerrList = new ArrayList<Territory>(waterTerr);
		reorder(waterTerrList, waterStrength, true);
		float maxStrength = -10000.0F;
		final List<Territory> safeTerrList = new ArrayList<Territory>();
		for (final Territory checkTerr : waterTerrList)
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
			for (final Territory safeTerr : safeTerrList)
			{
				final int thisDist = data.getMap().getWaterDistance(source, safeTerr);
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
	 * 
	 * @param checkTerr
	 *            - Territory which enemy would blitz
	 * @param data
	 * @param player
	 * @return
	 */
	public static List<Territory> possibleBlitzTerritories(final Territory checkTerr, final GameData data, final PlayerID player)
	{
		final CompositeMatch<Unit> blitzUnit = new CompositeMatchAnd<Unit>(Matches.UnitCanBlitz, Matches.enemyUnit(player, data));
		final List<Territory> blitzTerr = new ArrayList<Territory>();
		final List<Territory> twoTerr = SUtils.getExactNeighbors(checkTerr, 2, player, data, false);
		final List<Territory> oneTerr = SUtils.getExactNeighbors(checkTerr, 1, player, data, false);
		for (final Territory blitzFrom : twoTerr)
		{
			final List<Unit> blitzUnits = blitzFrom.getUnits().getMatches(blitzUnit);
			if (blitzUnits.isEmpty())
				continue;
			final List<Territory> blitzNeighbors = SUtils.getExactNeighbors(blitzFrom, 1, player, data, false);
			for (final Territory blitzCheck : blitzNeighbors)
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
	public static List<Territory> getNeighboringEnemyLandTerritories(final GameData data, final PlayerID player, final Territory check)
	{
		final List<Territory> rVal = new ArrayList<Territory>();
		final List<Territory> checkList = getExactNeighbors(check, 1, player, data, false);
		for (final Territory t : checkList)
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
	public static List<Territory> getNeighboringLandTerritories(final GameData data, final PlayerID player, final Territory check)
	{
		final ArrayList<Territory> rVal = new ArrayList<Territory>();
		final List<Territory> checkList = getExactNeighbors(check, 1, player, data, false);
		for (final Territory t : checkList)
		{
			if (Matches.isTerritoryAllied(player, data).match(t) && Matches.TerritoryIsNotImpassableToLandUnits(player, data).match(t))
				rVal.add(t);
		}
		return rVal;
	}
	
	/**
	 * Does this territory have any land? i.e. it isn't an island
	 * 
	 * @neutral - count an attackable neutral as a land neighbor
	 * @return boolean (true if a land territory is a neighbor to t
	 */
	public static boolean doesLandExistAt(final Territory t, final GameData data, final boolean neutral)
	{ // simply: is this territory surrounded by water
		boolean isLand = false;
		final Set<Territory> checkList = data.getMap().getNeighbors(t, Matches.TerritoryIsLand);
		if (!neutral)
		{
			final Iterator<Territory> nIter = checkList.iterator();
			while (nIter.hasNext())
			{
				final Territory nTerr = nIter.next();
				if (Matches.TerritoryIsNeutralButNotWater.match(nTerr))
					nIter.remove();
			}
		}
		for (final Territory checkNeutral : checkList)
		{
			if (Matches.TerritoryIsNotImpassable.match(checkNeutral))
				isLand = true;
		}
		return isLand;
	}
	
	/*
	 * distance to the closest enemy
	 * just utilises findNearest
	 */
	public static int distanceToEnemy(final Territory t, final GameData data, final PlayerID player, final boolean sea)
	{
		// note: neutrals are enemies
		// also note: if sea, you are finding distance to enemy sea units, not to enemy land over sea
		if (Matches.TerritoryIsImpassable.match(t))
			return 0;
		Match<Territory> endCondition;
		Match<Territory> routeCondition;
		if (sea)
		{
			endCondition = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasEnemyUnits(player, data));
			routeCondition = Matches.TerritoryIsWater;
		}
		else
		{
			endCondition = new CompositeMatchAnd<Territory>(Matches.isTerritoryEnemy(player, data), Matches.TerritoryIsNotImpassable, Matches.TerritoryIsLand);
			routeCondition = new CompositeMatchAnd<Territory>(Matches.isTerritoryAllied(player, data), Matches.TerritoryIsNotImpassable, Matches.TerritoryIsLand);
		}
		final Route r = findNearest(t, endCondition, routeCondition, data);
		if (r == null)
			return 0;
		else
			return r.getLength();
	}
	
	/**
	 * Recursive routine for finding the distance to an enemy
	 * 
	 * @param t
	 * @param beenThere
	 *            - list of territories already checked
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
	public static List<Territory> getEnemyCapitals(final GameData data, final PlayerID player)
	{ // generate a list of all enemy capitals
		final List<Territory> enemyCapitals = new ArrayList<Territory>();
		final List<PlayerID> ePlayers = SUtils.getEnemyPlayers(data, player);
		for (final PlayerID otherPlayer : ePlayers)
		{
			final Territory capitol = TerritoryAttachment.getCapital(otherPlayer, data);
			if (capitol != null && Matches.TerritoryIsNotImpassable.match(capitol)) // Mongolia is listed as capitol of China in AA50 games
				enemyCapitals.add(capitol);
		}
		return enemyCapitals;
	}
	
	/**
	 * List containing all 'live' enemy capitals (ones owned by original owner)
	 */
	public static List<Territory> getLiveEnemyCapitals(final GameData data, final PlayerID player)
	{ // generate a list of all enemy capitals
		final List<Territory> enemyCapitals = new ArrayList<Territory>();
		final List<PlayerID> ePlayers = SUtils.getEnemyPlayers(data, player);
		for (final PlayerID otherPlayer : ePlayers)
		{
			enemyCapitals.addAll(TerritoryAttachment.getAllCurrentlyOwnedCapitals(otherPlayer, data));
		}
		enemyCapitals.retainAll(Match.getMatches(enemyCapitals, Matches.TerritoryIsNotImpassableToLandUnits(player, data)));
		return enemyCapitals;
	}
	
	/**
	 * Returns a list of all enemy players
	 * 
	 * @param data
	 * @param player
	 * @return List<PlayerID> enemyPlayers
	 */
	public static List<PlayerID> getEnemyPlayers(final GameData data, final PlayerID player)
	{
		final List<PlayerID> enemyPlayers = new ArrayList<PlayerID>();
		for (final PlayerID players : data.getPlayerList().getPlayers())
		{
			if (!data.getRelationshipTracker().isAllied(player, players))
				enemyPlayers.add(players);
		}
		return enemyPlayers;
	}
	
	/**
	 * Looking for the closest land territory next to an enemy capital which is owned by an ally
	 * Use this for dumping units from transports
	 * 
	 * @capTerr - actual Capital being targeted (returned parameter)
	 * @fromTerr - source of units
	 * @return - Land Territory with allied units
	 */
	public static Territory getAlliedLandTerrNextToEnemyCapital(int minDist, final Territory capTerr, final Territory fromTerr, final GameData data, final PlayerID player)
	{
		minDist = 100;
		Territory capWaterTerr = null;
		// capTerr = null;
		final List<Territory> enemyCapitals = getEnemyCapitals(data, player);
		if (enemyCapitals.size() > 0)
		{
			for (final Territory badCapital : enemyCapitals)
			{
				final List<Territory> areaTerritories = getNeighboringLandTerritories(data, player, badCapital);
				if (areaTerritories.size() == 0)
					continue;
				for (final Territory nextToCapital : areaTerritories)
				{
					int capDist = 100;
					Territory tcapTerr = null;
					for (final Territory tmpCapTerr : data.getMap().getNeighbors(nextToCapital, Matches.TerritoryIsWater))
					{
						final int tDist = data.getMap().getWaterDistance(fromTerr, tmpCapTerr);
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
						// capTerr = tcapTerr;
						minDist = capDist;
					}
				}
			}
		}
		return capWaterTerr;
	}
	
	/**
	 * determines the Land Route to a capital...returns if it exists and puts it in goRoute
	 * returns null for the Route if it does not exist
	 * 
	 * @param thisTerr
	 *            - Territory to be checked
	 * @param goRoute
	 *            - contains the actual route
	 * @return - true if the route exists, false if it doesn't exist
	 */
	public static boolean landRouteToEnemyCapital(final Territory thisTerr, final Route goRoute, final GameData data, final PlayerID player)
	{// is there a land route between territory and enemy
		// Territory myCapital = TerritoryAttachment.getCapital(player, data);
		// boolean routeExists = false;
		Route route = null;
		for (final PlayerID otherPlayer : data.getPlayerList().getPlayers())
		{
			if (!data.getRelationshipTracker().isAllied(player, otherPlayer))
			{
				final Territory capitol = TerritoryAttachment.getCapital(otherPlayer, data);
				if (capitol != null)
				{
					route = data.getMap().getRoute(thisTerr, capitol, Matches.TerritoryIsNotImpassableToLandUnits(player, data));
					if (route != null)
					{
						return true;
						// routeExists = true;
					}
				}
			}
		}
		return false;
		/*if (routeExists)
			goRoute = route;
		else
			goRoute = null;
		
		return routeExists;*/
	}
	
	/**
	 * Fill a List with units from the passed list up to maxStrength
	 */
	public static List<Unit> getUnitsUpToStrength(final double maxStrength, final Collection<Unit> units, final boolean attacking, final boolean sea)
	{
		if (strength(units, attacking, sea, false) < maxStrength)
			return new ArrayList<Unit>(units);
		final ArrayList<Unit> rVal = new ArrayList<Unit>();
		for (final Unit u : units)
		{
			rVal.add(u);
			if (strength(rVal, attacking, sea, false) > maxStrength)
				return rVal;
		}
		return rVal;
	}
	
	/**
	 * Finds a list of territories which contain airUnits, but don't have land Units
	 * 
	 * @return List of Territories
	 */
	public static List<Territory> TerritoryOnlyPlanes(final GameData data, final PlayerID player)
	{
		List<Unit> airUnits = new ArrayList<Unit>();
		List<Unit> landUnits = new ArrayList<Unit>();
		final List<Territory> returnTerr = new ArrayList<Territory>();
		int aUnit = 0, lUnit = 0;
		// find all territories for this player which only contain planes
		final CompositeMatch<Unit> airUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsAir);
		final CompositeMatch<Unit> landUnit = new CompositeMatchAnd<Unit>(Matches.isUnitAllied(player, data), Matches.UnitIsLand, Matches.UnitIsNotInfrastructure);
		final List<Territory> owned = allOurTerritories(data, player);
		for (final Territory t : owned)
		{
			airUnits = t.getUnits().getMatches(airUnit);
			landUnits = t.getUnits().getMatches(landUnit);
			aUnit = airUnits.size();
			lUnit = landUnits.size();
			if (aUnit > 0 & lUnit == 0) // we want places that have air units only
				returnTerr.add(t);
		}
		return returnTerr;
	}
	
	/**
	 * Finds the Territory within 3 Territories which has a maximum # of units
	 * Uses friendly to determine if we are looking for our guys or enemy
	 * Returns Territory with a maximum # of units (enemy or friendly)
	 */
	public static Territory findNearestMaxUnits(final PlayerID player, final GameData data, final Territory t, final boolean friendly)
	{
		final CompositeMatch<Unit> landUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsLand, Matches.UnitIsNotInfrastructure);
		final CompositeMatch<Unit> enemyLandUnit = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data), landUnit);
		final CompositeMatch<Unit> ourLandUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), landUnit);
		int totUnit = 0;
		int maxUnit = 0;
		Territory maxTerr = null;
		final Collection<Territory> nearNeighbors = data.getMap().getNeighbors(t, 3);
		nearNeighbors.remove(t);
		for (final Territory t2 : nearNeighbors)
		{
			if (t2.isWater() || TerritoryAttachment.get(t2).getIsImpassible())
				continue;
			if (friendly)
			{
				final List<Unit> ourGuys = t2.getUnits().getMatches(ourLandUnit);
				totUnit = ourGuys.size();
				if (totUnit > maxUnit)
				{
					maxUnit = totUnit;
					maxTerr = t2;
				}
			}
			else
			{
				final List<Unit> theirGuys = t2.getUnits().getMatches(enemyLandUnit);
				totUnit = theirGuys.size();
				if (totUnit > maxUnit)
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
	
	public static Collection<Unit> whatPlanesNeedToLand(final boolean doBombers, final Territory thisTerritory, final PlayerID player)
	{ /*
		this considers carriers in our current territory, but no where else
		  can't check for other carriers here because the route has been established
		  could look at passing the route through and modify it if a sea based territory
		  the premise of this is a little messed up because we are requiring our bombers and fighters
		  to use the same route...probably needs a complete rewrite
		*/
		final Collection<Unit> ourPlanes = new ArrayList<Unit>();
		final Collection<Unit> fighters = new ArrayList<Unit>();
		final CompositeMatch<Unit> fighterUnit = new CompositeMatchAnd<Unit>(Matches.UnitCanLandOnCarrier, Matches.unitIsOwnedBy(player));
		final CompositeMatch<Unit> bomberUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsStrategicBomber, Matches.unitIsOwnedBy(player));
		final CompositeMatch<Unit> alliedFighter = new CompositeMatchAnd<Unit>(Matches.UnitCanLandOnCarrier);
		// assume the only other fighters would be allied
		// separate fighters and bombers so that we can coordinate fighters with carriers
		fighters.addAll(thisTerritory.getUnits().getMatches(fighterUnit));
		final Collection<Unit> bombers = thisTerritory.getUnits().getMatches(bomberUnit);
		if (thisTerritory.isWater())
		{
			final Collection<Unit> carriers = thisTerritory.getUnits().getMatches(Matches.UnitIsCarrier);
			final Collection<Unit> otherfighters = thisTerritory.getUnits().getMatches(alliedFighter);
			otherfighters.removeAll(fighters);
			/* we can land on any allied carrier
			   but we must allow their own fighters to land there
			*/
			final int numAlliedFighters = otherfighters.size();
			final int carrierCapacity = carriers.size() * 2 - (numAlliedFighters + 1);
			for (int i = 0; i <= carrierCapacity; i++)
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
	 * 
	 * @param tFirst
	 *            - can transports be killed before other sea units
	 * @param ignoreOnlyPlanes
	 *            - if true, returns 0.0F if only planes can attack the territory
	 */
	public static float getStrengthOfPotentialAttackers(final Territory location, final GameData data, final PlayerID player, final boolean tFirst, final boolean ignoreOnlyPlanes,
				final List<Territory> ignoreTerr)
	{
		final TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();
		PlayerID ePlayer = null;
		final List<PlayerID> qID = getEnemyPlayers(data, player);
		final HashMap<PlayerID, Float> ePAttackMap = new HashMap<PlayerID, Float>();
		final boolean doIgnoreTerritories = ignoreTerr != null;
		final Iterator<PlayerID> playerIter = qID.iterator();
		if (location == null)
			return -1000.0F;
		boolean nonTransportsInAttack = false;
		final boolean onWater = location.isWater();
		if (!onWater)
			nonTransportsInAttack = true;
		final Set<Territory> waterTerr = data.getMap().getNeighbors(location, Matches.TerritoryIsWater);
		while (playerIter.hasNext())
		{
			float seaStrength = 0.0F, firstStrength = 0.0F, secondStrength = 0.0F, blitzStrength = 0.0F, strength = 0.0F, airStrength = 0.0F;
			ePlayer = playerIter.next();
			final CompositeMatch<Unit> enemyPlane = new CompositeMatchAnd<Unit>(Matches.UnitIsAir, Matches.unitIsOwnedBy(ePlayer), Matches.UnitCanMove);
			final CompositeMatch<Unit> enemyTransport = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(ePlayer), Matches.UnitIsSea, Matches.UnitIsTransport, Matches.UnitCanMove);
			final CompositeMatch<Unit> enemyShip = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(ePlayer), Matches.UnitIsSea, Matches.UnitCanMove);
			final CompositeMatch<Unit> enemyTransportable = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(ePlayer), Matches.UnitCanBeTransported, Matches.UnitIsNotAA,
						Matches.UnitCanMove);
			final CompositeMatch<Unit> aTransport = new CompositeMatchAnd<Unit>(Matches.UnitIsSea, Matches.UnitIsTransport, Matches.UnitCanMove);
			final List<Territory> eFTerrs = SUtils.findUnitTerr(data, ePlayer, enemyPlane);
			int maxFighterDistance = 0, maxBomberDistance = 0;
			// should change this to read production frontier and tech
			// reality is 99% of time units considered will have full move.
			// and likely player will have at least 1 max move plane.
			for (final Territory eFTerr : eFTerrs)
			{
				final List<Unit> eFUnits = eFTerr.getUnits().getMatches(enemyPlane);
				maxFighterDistance = Math.max(maxFighterDistance, MoveValidator.getMaxMovement(eFUnits));
			}
			maxFighterDistance--; // must be able to land...we will miss fighters who have a Carrier that can reach same sea zone...C'est la vie
			if (maxFighterDistance < 0)
				maxFighterDistance = 0;
			maxBomberDistance--; // must be able to land...won't miss anything here...unless special bombers that can land on carrier per above
			if (maxBomberDistance < 0)
				maxBomberDistance = 0;
			final List<Territory> eTTerrs = SUtils.findUnitTerr(data, ePlayer, aTransport);
			int maxTransportDistance = 0;
			for (final Territory eTTerr : eTTerrs)
			{
				final List<Unit> eTUnits = eTTerr.getUnits().getMatches(aTransport);
				maxTransportDistance = Math.max(maxTransportDistance, MoveValidator.getMaxMovement(eTUnits));
			}
			final List<Unit> alreadyLoaded = new ArrayList<Unit>();
			final List<Route> blitzTerrRoutes = new ArrayList<Route>();
			final List<Territory> checked = new ArrayList<Territory>();
			final List<Unit> enemyWaterUnits = new ArrayList<Unit>();
			for (final Territory t : data.getMap().getNeighbors(location, onWater ? Matches.TerritoryIsWater : Matches.TerritoryIsLand))
			{
				if (doIgnoreTerritories && ignoreTerr.contains(t))
					continue;
				final List<Unit> enemies = t.getUnits().getMatches(Matches.unitIsOwnedBy(ePlayer));
				enemyWaterUnits.addAll(enemies);
				firstStrength += strength(enemies, true, onWater, tFirst);
				checked.add(t);
			}
			if (Matches.TerritoryIsLand.match(location))
			{
				blitzStrength = determineEnemyBlitzStrength(location, blitzTerrRoutes, null, data, ePlayer);
			}
			else
			// get ships attack strength
			{ // old assumed fleets won't split up, new lets them. no biggie.
				// assumes max ship movement is 3.
				// note, both old and new implementations
				// allow units to be calculated that are in
				// territories we have already assaulted
				// this can be easily changed
				final HashSet<Integer> ignore = new HashSet<Integer>();
				ignore.add(Integer.valueOf(1));
				final List<Route> r = new ArrayList<Route>();
				final List<Unit> ships = findAttackers(location, 3, ignore, ePlayer, data, enemyShip, Matches.territoryIsBlockedSea(ePlayer, data), ignoreTerr, r, true);
				secondStrength = strength(ships, true, true, tFirst);
				enemyWaterUnits.addAll(ships);
				/*
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
				*/
			}
			final List<Unit> attackPlanes = findPlaneAttackersThatCanLand(location, maxFighterDistance, ePlayer, data, ignoreTerr, checked);
			airStrength += allairstrength(attackPlanes, true);
			if (Matches.territoryHasWaterNeighbor(data).match(location) && Matches.TerritoryIsLand.match(location))
			{
				for (final Territory t4 : data.getMap().getNeighbors(location, maxTransportDistance))
				{
					if (!t4.isWater())
						continue;
					boolean transportsCounted = false;
					final Iterator<Territory> iterTerr = waterTerr.iterator();
					while (!transportsCounted && iterTerr.hasNext())
					{
						final Territory waterCheck = iterTerr.next();
						if (ePlayer == null)
							continue;
						final List<Unit> transports = t4.getUnits().getMatches(enemyTransport);
						if (transports.isEmpty())
							continue;
						if (!t4.equals(waterCheck))
						{
							final Route seaRoute = getMaxSeaRoute(data, t4, waterCheck, ePlayer, true, maxTransportDistance);
							if (seaRoute == null || seaRoute.getEnd() == null || seaRoute.getEnd() != waterCheck) // no valid route...ignore ships
								continue;
						}
						final List<Unit> loadedUnits = new ArrayList<Unit>();
						int availInf = 0, availOther = 0;
						for (final Unit xTrans : transports)
						{
							final Collection<Unit> thisTransUnits = tracker.transporting(xTrans);
							if (thisTransUnits == null)
							{
								availInf += 2;
								availOther += 1;
								continue;
							}
							else
							{
								int Inf = 2, Other = 1;
								for (final Unit checkUnit : thisTransUnits)
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
						final Set<Territory> transNeighbors = data.getMap().getNeighbors(t4, Matches.isTerritoryAllied(ePlayer, data));
						for (final Territory xN : transNeighbors)
						{
							final List<Unit> aTransUnits = xN.getUnits().getMatches(enemyTransportable);
							aTransUnits.removeAll(alreadyLoaded);
							final List<Unit> availTransUnits = SUtils.sortTransportUnits(aTransUnits);
							for (final Unit aTUnit : availTransUnits)
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
				final Iterator<Unit> eWaterIter = enemyWaterUnits.iterator();
				while (eWaterIter.hasNext() && !nonTransportsInAttack)
				{
					if (Matches.UnitIsNotTransport.match(eWaterIter.next()))
						nonTransportsInAttack = true;
				}
			}
			if (!nonTransportsInAttack)
				strength = 0.0F;
			ePAttackMap.put(ePlayer, strength);
		}
		float maxStrength = 0.0F;
		for (final PlayerID xP : qID)
		{
			if (ePAttackMap.get(xP) > maxStrength)
			{
				ePlayer = xP;
				maxStrength = ePAttackMap.get(xP);
			}
		}
		for (final PlayerID xP : qID)
		{
			if (ePlayer != xP)
				maxStrength += ePAttackMap.get(xP) * 0.40F; // give 40% of other players...this is will affect a lot of decisions by AI
		}
		return maxStrength;
	}
	
	/**
	 * 
	 * Returns all Territories which contain Allied Units within a radius of 4 Territories of t
	 * Works for land units as well as ships
	 * 
	 * @return
	 */
	public static List<Territory> findOurShips(final Territory t, final GameData data, final PlayerID player)
	{
		// Return territories of Allied Ships within a sea radius of 4
		final List<Territory> shipTerr = new ArrayList<Territory>();
		final Collection<Territory> tNeighbors = data.getMap().getNeighbors(t, 4);
		for (final Territory t2 : tNeighbors)
		{
			if (t2.getUnits().someMatch(Matches.isUnitAllied(player, data)))
				shipTerr.add(t2);
		}
		return shipTerr;
	}
	
	/**
	 * Returns a list of Territories containing Allied Ships within a radius of 3 which meet the condition
	 * 
	 * @param unitCondition
	 *            - Match condition
	 * @return - List of Territories - works ships or land units
	 */
	public static List<Territory> findOurShips(final Territory t, final GameData data, final PlayerID player, final Match<Unit> unitCondition)
	{
		// Return territories of Allied Ships within a sea radius of 3 (AC Limit)
		final CompositeMatch<Unit> limitShips = new CompositeMatchAnd<Unit>(unitCondition, Matches.isUnitAllied(player, data));
		final List<Territory> shipTerr = new ArrayList<Territory>();
		final Collection<Territory> tNeighbors = data.getMap().getNeighbors(t, 3);
		for (final Territory t2 : tNeighbors)
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
	public static List<Territory> findOnlyMyShips(final Territory t, final GameData data, final PlayerID player, final Match<Unit> unitCondition)
	{
		// Return territories of Owned Ships within a sea radius of 3 (AC Limit)
		final CompositeMatch<Unit> limitShips = new CompositeMatchAnd<Unit>(unitCondition, Matches.unitIsOwnedBy(player));
		final List<Territory> shipTerr = new ArrayList<Territory>();
		final Collection<Territory> tNeighbors = data.getMap().getNeighbors(t, 3);
		for (final Territory t2 : tNeighbors)
		{
			if (t2.getUnits().someMatch(limitShips))
				shipTerr.add(t2);
		}
		return shipTerr;
	}
	
	/**
	 * 
	 * Return all territories that have units matching unitCondition and owned by us.
	 * 
	 * @return List of territories
	 */
	public static List<Territory> findTersWithUnitsMatching(final GameData data, final PlayerID player, final Match<Unit> unitCondition)
	{
		final CompositeMatch<Unit> unitMatch = new CompositeMatchAnd<Unit>(unitCondition, Matches.unitIsOwnedBy(player));
		final List<Territory> result = new ArrayList<Territory>();
		final Collection<Territory> allTers = data.getMap().getTerritories();
		for (final Territory ter : allTers)
		{
			if (ter.getUnits().someMatch(unitMatch))
				result.add(ter);
		}
		return result;
	}
	
	public static int findNumberOfUnitsMatching(final GameData data, final PlayerID player, final Match<Unit> unitCondition)
	{
		int total = 0;
		final Collection<Territory> allTers = data.getMap().getTerritories();
		for (final Territory ter : allTers)
		{
			total += Match.countMatches(ter.getUnits().getUnits(), unitCondition);
		}
		return total;
	}
	
	/**
	 * Return Territories containing any unit depending on unitCondition
	 * Differs from findCertainShips because it doesn't require the units be owned
	 */
	public static List<Territory> findUnitTerr(final GameData data, final PlayerID player, final Match<Unit> unitCondition)
	{
		// Return territories containing a certain unit or set of Units
		final CompositeMatch<Unit> limitShips = new CompositeMatchAnd<Unit>(unitCondition);
		final List<Territory> shipTerr = new ArrayList<Territory>();
		final Collection<Territory> tNeighbors = data.getMap().getTerritories();
		for (final Territory t2 : tNeighbors)
		{
			if (t2.getUnits().someMatch(limitShips))
				shipTerr.add(t2);
		}
		return shipTerr;
	}
	
	/**
	 * Returns list of territories within 4 territories of t and contain owned planes
	 */
	public static List<Territory> findOurPlanes(final Territory t, final GameData data, final PlayerID player)
	{
		// Return territories of our planes within 4 of this Territory
		final CompositeMatch<Unit> ownedAndAir = new CompositeMatchAnd<Unit>(Matches.UnitIsAir, Matches.unitIsOwnedBy(player));
		final List<Territory> airTerr = new ArrayList<Territory>();
		final Collection<Territory> tNeighbors = data.getMap().getNeighbors(t, 4);
		for (final Territory t2 : tNeighbors)
		{
			if (t2.getUnits().someMatch(ownedAndAir))
				airTerr.add(t2);
		}
		return airTerr;
	}
	
	/**
	 * Returns a list of territories within maxDist which meat the requirement of unitCondition
	 * No requirements of ownership or allied units
	 * 
	 * @return
	 */
	public static List<Territory> findUnits(final Territory t, final GameData data, final Match<Unit> unitCondition, final int maxDist)
	{
		// Return territories of our Units within maxDist of this Territory
		final List<Territory> anyTerr = new ArrayList<Territory>();
		final Collection<Territory> tNeighbors = data.getMap().getNeighbors(t, maxDist);
		for (final Territory t2 : tNeighbors)
		{
			if (t2.getUnits().someMatch(unitCondition))
				anyTerr.add(t2);
		}
		return anyTerr;
	}
	
	/**
	 * Territories we actually own in a modifiable List
	 */
	public static List<Territory> allOurTerritories(final GameData data, final PlayerID player)
	{
		final Collection<Territory> ours = data.getMap().getTerritoriesOwnedBy(player);
		final List<Territory> ours2 = new ArrayList<Territory>();
		ours2.addAll(ours);
		return ours2;
	}
	
	/**
	 * All Allied TErritories in a modifiable List
	 */
	public static List<Territory> allAlliedTerritories(final GameData data, final PlayerID player)
	{
		final List<Territory> ours = new ArrayList<Territory>();
		for (final Territory t : data.getMap().getTerritories())
			if (Matches.isTerritoryAllied(player, data).match(t))
				ours.add(t);
		return ours;
	}
	
	/**
	 * All Enemy Territories in a modifiable List
	 */
	public static List<Territory> allEnemyTerritories(final GameData data, final PlayerID player)
	{
		final List<Territory> badGuys = new ArrayList<Territory>();
		for (final Territory t : data.getMap().getTerritories())
			if (Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data).match(t))
				badGuys.add(t);
		return badGuys;
	}
	
	public static List<Unit> findAttackers(final Territory start, final int maxDistance, final HashSet<Integer> ignoreDistance, final PlayerID player, final GameData data,
				final Match<Unit> unitCondition, final Match<Territory> routeCondition, final List<Territory> blocked, final List<Route> routes, final boolean sea)
	{
		final IntegerMap<Territory> distance = new IntegerMap<Territory>();
		final Map<Territory, Territory> visited = new HashMap<Territory, Territory>();
		final List<Unit> units = new ArrayList<Unit>();
		final Queue<Territory> q = new LinkedList<Territory>();
		q.add(start);
		Territory current = null;
		distance.put(start, 0);
		visited.put(start, null);
		while (!q.isEmpty())
		{
			current = q.remove();
			if (distance.getInt(current) == maxDistance)
				break;
			for (final Territory neighbor : data.getMap().getNeighbors(current))
			{
				if (!distance.keySet().contains(neighbor))
				{
					if (!neighbor.getUnits().someMatch(unitCondition))
						if (!routeCondition.match(neighbor))
							continue;
					if (sea)
					{
						final Route r = new Route();
						r.setStart(neighbor);
						r.add(current);
						if (MoveValidator.validateCanal(r, null, player, data) != null)
							continue;
					}
					distance.put(neighbor, distance.getInt(current) + 1);
					visited.put(neighbor, current);
					if (blocked != null && blocked.contains(neighbor))
						continue;
					q.add(neighbor);
					final Integer dist = Integer.valueOf(distance.getInt(neighbor));
					if (ignoreDistance.contains(dist))
						continue;
					for (final Unit u : neighbor.getUnits())
					{
						if (unitCondition.match(u) && MoveValidator.hasEnoughMovement(u, dist))
						{
							units.add(u);
						}
					}
				}
			}
		}
		// pain in the ass, should just redesign stop blitz attack
		for (final Territory t : visited.keySet())
		{
			final Route r = new Route();
			Territory t2 = t;
			r.setStart(t);
			while (t2 != null)
			{
				t2 = visited.get(t2);
				if (t2 != null)
					r.add(t2);
			}
			routes.add(r);
		}
		return units;
	}
	
	/**
	 * does not count planes already in the starting territory
	 * 
	 * @param start
	 * @param maxDistance
	 * @param player
	 * @param data
	 * @param ignore
	 * @param checked
	 * @return
	 */
	public static List<Unit> findPlaneAttackersThatCanLand(final Territory start, final int maxDistance, final PlayerID player, final GameData data, final List<Territory> ignore,
				final List<Territory> checked)
	{
		final IntegerMap<Territory> distance = new IntegerMap<Territory>();
		final IntegerMap<Unit> unitDistance = new IntegerMap<Unit>();
		final List<Unit> units = new ArrayList<Unit>();
		final Queue<Territory> q = new LinkedList<Territory>();
		Territory lz = null, ac = null;
		final CompositeMatch<Unit> enemyPlane = new CompositeMatchAnd<Unit>(Matches.UnitIsAir, Matches.unitIsOwnedBy(player), Matches.UnitCanMove);
		final CompositeMatch<Unit> enemyCarrier = new CompositeMatchAnd<Unit>(Matches.UnitIsCarrier, Matches.unitIsOwnedBy(player), Matches.UnitCanMove);
		q.add(start);
		Territory current = null;
		distance.put(start, 0);
		while (!q.isEmpty())
		{
			current = q.remove();
			if (distance.getInt(current) == maxDistance)
				break;
			for (final Territory neighbor : data.getMap().getNeighbors(current, SUtils.TerritoryIsNotImpassableToAirUnits(data)))
			{
				if (!distance.keySet().contains(neighbor))
				{
					q.add(neighbor);
					distance.put(neighbor, distance.getInt(current) + 1);
					if (lz == null && Matches.isTerritoryAllied(player, data).match(neighbor) && !neighbor.isWater())
						lz = neighbor;
					if ((ignore != null && ignore.contains(neighbor)) || (checked != null && checked.contains(neighbor)))
					{
						for (final Unit u : neighbor.getUnits())
						{
							if (ac == null && enemyCarrier.match(u))
								ac = neighbor;
						}
					}
					else
					{
						for (final Unit u : neighbor.getUnits())
						{
							if (ac == null && enemyCarrier.match(u))
								ac = neighbor;
							if (enemyPlane.match(u))
								unitDistance.put(u, distance.getInt(neighbor));
						}
					}
				}
			}
		}
		for (final Unit u : unitDistance.keySet())
		{
			if (lz != null && MoveValidator.hasEnoughMovement(u, unitDistance.getInt(u) + distance.getInt(lz)))
				units.add(u);
			else if (ac != null && Matches.UnitCanLandOnCarrier.match(u) && MoveValidator.hasEnoughMovement(u, unitDistance.getInt(u) + distance.getInt(ac)))
				units.add(u);
		}
		// s_logger.fine("LZ " + lz + " "+ distance.getInt(lz));
		return units;
	}
	
	/**
	 * finds all units matching some condition over route define by condtion
	 * with maximum length
	 * returns map of units to their distance.
	 * 
	 */
	public static IntegerMap<Unit> findAllUnits(final Territory start, final int maxDistance, final Match<Territory> routeCondition, final Match<Unit> unitCondition, final GameData data)
	{
		final IntegerMap<Territory> distance = new IntegerMap<Territory>();
		final IntegerMap<Unit> unitDistance = new IntegerMap<Unit>();
		final Queue<Territory> q = new LinkedList<Territory>();
		q.add(start);
		Territory current = null;
		distance.put(start, 0);
		while (!q.isEmpty())
		{
			current = q.remove();
			if (distance.getInt(current) == maxDistance)
				break;
			for (final Territory neighbor : data.getMap().getNeighbors(current, routeCondition))
			{
				if (!distance.keySet().contains(neighbor))
				{
					q.add(neighbor);
					distance.put(neighbor, distance.getInt(current) + 1);
					for (final Unit u : neighbor.getUnits())
					{
						if (unitCondition.match(u))
							unitDistance.put(u, distance.getInt(neighbor));
					}
				}
			}
		}
		return unitDistance;
	}
	
	/**
	 * Find the Route to the nearest Territory
	 * 
	 * @param start
	 *            - starting territory
	 * @param endCondition
	 *            - condition for the ending Territory
	 * @param routeCondition
	 *            - condition for each Territory in Route
	 * @param data
	 * @return
	 */
	public static Route findNearest(final Territory start, final Match<Territory> endCondition, final Match<Territory> routeCondition, final GameData data)
	{
		final Match<Territory> canGo = new CompositeMatchOr<Territory>(endCondition, routeCondition);
		final Map<Territory, Territory> visited = new HashMap<Territory, Territory>();
		final Queue<Territory> q = new LinkedList<Territory>();
		final List<Territory> route = new ArrayList<Territory>();
		// changing to exclude checking start
		q.addAll(data.getMap().getNeighbors(start, canGo));
		Territory current = null;
		visited.put(start, null);
		for (final Territory t : q)
			visited.put(t, start);
		while (!q.isEmpty())
		{
			current = q.remove();
			if (endCondition.match(current))
				break;
			else
			{
				for (final Territory neighbor : data.getMap().getNeighbors(current, canGo))
				{
					if (!visited.containsKey(neighbor))
					{
						q.add(neighbor);
						visited.put(neighbor, current);
					}
				}
			}
		}
		if (current == null || !endCondition.match(current))
		{
			return null;
		}
		for (Territory t = current; t != null; t = visited.get(t))
		{
			route.add(t);
		}
		Collections.reverse(route);
		return new Route(route);
	}
	
	/**
	 * Finds list of territories at exactly distance from the start
	 * 
	 * @param start
	 * @param endCondition
	 *            condition that all end points must satisfy
	 * @param routeCondition
	 *            condition that all traversed internal territories must satisy
	 * @param distance
	 * @param data
	 * @return
	 */
	public static List<Territory> findFontier(final Territory start, final Match<Territory> endCondition, final Match<Territory> routeCondition, final int distance, final GameData data)
	{
		final Match<Territory> canGo = new CompositeMatchOr<Territory>(endCondition, routeCondition);
		final IntegerMap<Territory> visited = new IntegerMap<Territory>();
		final Queue<Territory> q = new LinkedList<Territory>();
		final List<Territory> frontier = new ArrayList<Territory>();
		q.addAll(data.getMap().getNeighbors(start, canGo));
		Territory current = null;
		visited.put(start, 0);
		for (final Territory t : q)
		{
			visited.put(t, 1);
			if (1 == distance && endCondition.match(t))
				frontier.add(t);
		}
		while (!q.isEmpty())
		{
			current = q.remove();
			if (visited.getInt(current) == distance)
				break;
			else
			{
				for (final Territory neighbor : data.getMap().getNeighbors(current, canGo))
				{
					if (!visited.keySet().contains(neighbor))
					{
						q.add(neighbor);
						final int dist = visited.getInt(current) + 1;
						visited.put(neighbor, dist);
						if (dist == distance && endCondition.match(neighbor))
						{
							frontier.add(neighbor);
						}
					}
				}
			}
		}
		return frontier;
	}
	
	/**
	 * Find the Route to the nearest Territory
	 * 
	 * @param start
	 *            - starting territory
	 * @param endCondition
	 *            - condition for the ending Territory
	 * @param routeCondition
	 *            - condition for each Territory in Route
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
	 * 
	 * @param start
	 *            - initial territory
	 * @param endCondition
	 *            - final territory must match this
	 * @param routeCondition
	 *            - all territories on route must match this
	 * @param unitCondition
	 *            - units must match this
	 * @param maxUnits
	 *            - how many units were found there
	 * @return - Route to the endCondition
	 */
	public static Route findNearestMaxContaining(final Territory start, final Match<Territory> endCondition, final Match<Territory> routeCondition, final Match<Unit> unitCondition,
				final int maxUnits, final GameData data)
	{
		final Match<Territory> condition = new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				return t.getUnits().getMatches(unitCondition).size() > maxUnits;
			}
		};
		return findNearest(start, new CompositeMatchAnd<Territory>(endCondition, condition), routeCondition, data);
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
	
	public static Route findNearestNotEmpty(final Territory start, final Match<Territory> endCondition, final Match<Territory> routeCondition, final GameData data)
	{
		Route r = findNearest(start, new CompositeMatchAnd<Territory>(endCondition, Matches.TerritoryIsEmpty.invert()), routeCondition, data);
		if (r == null)
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
	public static boolean hasLandRouteToEnemyOwnedCapitol(final Territory t, final PlayerID player, final GameData data)
	{
		for (final PlayerID ePlayer : data.getPlayerList().getPlayers())
		{
			final Territory capitol = TerritoryAttachment.getCapital(ePlayer, data);
			if (capitol == null || data.getRelationshipTracker().isAllied(player, capitol.getOwner()))
				continue;
			if (data.getMap().getDistance(t, capitol, Matches.TerritoryIsNotImpassableToLandUnits(player, data)) != -1)
			{
				return true;
			}
		}
		return false;
	}
	
	public static boolean airUnitIsLandableOnCarrier(final Unit u, final Territory source, final Territory target, final PlayerID player, final GameData data)
	{
		// Warning: THIS DOES NOT VERIFY THE # OF PLANES PLANNING TO LAND on the AC
		// Calling program must verify that there is room on the AC
		if (!Matches.UnitCanLandOnCarrier.match(u))
			return false;
		final Match<Unit> ownedCarrier = new CompositeMatchAnd<Unit>(Matches.UnitIsCarrier, Matches.unitIsOwnedBy(player));
		final Match<Territory> condition = new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				return t.getUnits().someMatch(ownedCarrier);
			}
		};
		final int rDist = data.getMap().getDistance(source, target, SUtils.TerritoryIsNotImpassableToAirUnits(data));
		boolean landable = false;
		if (MoveValidator.hasEnoughMovement(u, rDist))
			;
		{
			final Route r = findNearest(target, condition, SUtils.TerritoryIsNotImpassableToAirUnits(data), data);
			if (r == null)
				return false;
			final int rDist2 = r.getLength();
			if (MoveValidator.hasEnoughMovement(u, rDist + rDist2))
			{
				landable = true;
			}
		}
		return landable;
	}
	
	public static boolean airUnitIsLandable(final Unit u, final Territory source, final Territory target, final PlayerID owner, final GameData data)
	{
		final Match<Territory> condition = new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				return Matches.isTerritoryAllied(owner, data).match(t) && !t.isWater();
			}
		};
		final int rDist = data.getMap().getDistance(source, target, SUtils.TerritoryIsNotImpassableToAirUnits(data));
		boolean landable = false;
		if (MoveValidator.hasEnoughMovement(u, rDist))
			;
		{
			final Route r = findNearest(target, condition, SUtils.TerritoryIsNotImpassableToAirUnits(data), data);
			if (r == null)
				return false;
			final int rDist2 = r.getLength();
			if (MoveValidator.hasEnoughMovement(u, rDist + rDist2))
			{
				landable = true;
			}
		}
		return landable;
	}
	
	/**
	 * Returns a list of all territories containing owned AirCraft Carriers
	 */
	public static List<Territory> ACTerritory(final PlayerID player, final GameData data)
	{ // Return Territories containing AirCraft Carriers
		final List<Territory> carriers = new ArrayList<Territory>();
		final CompositeMatch<Unit> ourCarrier = new CompositeMatchAnd<Unit>(Matches.UnitIsCarrier, Matches.unitIsOwnedBy(player));
		for (final Territory t : data.getMap().getTerritories())
		{
			if (t.getUnits().someMatch(ourCarrier))
				carriers.add(t);
		}
		return carriers;
	}
	
	public static void addUnitCollection(final List<Collection<Unit>> aUnits, final Collection<Unit> bUnits)
	{
		final Collection<Unit> cUnits = new ArrayList<Unit>();
		cUnits.addAll(bUnits);
		aUnits.add(cUnits);
	}
	
	/**
	 * Determine the available strength of a single air unit
	 * Primarily useful for a sea battle needing support
	 * Moved from AIUtils to improve portability with changes by others
	 */
	public static float airstrength(final Unit airunit, final boolean attacking)
	{
		float airstrength = 0.0F;
		final Unit u = airunit;
		final UnitAttachment unitAttachment = UnitAttachment.get(u.getType());
		if (unitAttachment.getIsAir())
		{
			airstrength += 1.00F;
			if (attacking)
				airstrength += unitAttachment.getAttack(u.getOwner());
			else
				airstrength += unitAttachment.getDefense(u.getOwner());
		}
		return airstrength;
	}
	
	/**
	 * Determine the strength of a collection of airUnits
	 * Caller should guarantee units are all air.
	 */
	public static float allairstrength(final Collection<Unit> units, final boolean attacking)
	{
		float airstrength = 0.0F;
		for (final Unit u : units)
		{
			final UnitAttachment unitAttachment = UnitAttachment.get(u.getType());
			airstrength += 1.00F;
			if (attacking)
				airstrength += unitAttachment.getAttack(u.getOwner());
			else
				airstrength += unitAttachment.getDefense(u.getOwner());
		}
		return airstrength;
	}
	
	/**
	 * Determine the strength of a single unit
	 */
	public static float uStrength(final Unit units, final boolean attacking, final boolean sea, final boolean transportsFirst)
	{
		float strength = 0.0F;
		final Unit u = units;
		final UnitAttachment unitAttachment = UnitAttachment.get(u.getType());
		if (unitAttachment.getIsInfrastructure())
		{
			// nothing
		}
		else if (unitAttachment.getIsSea() == sea)
		{
			strength += 1.00F;
			// the number of pips on the dice
			if (attacking)
				strength += unitAttachment.getAttack(u.getOwner()) * (unitAttachment.getIsTwoHit() ? 2 : 1) * unitAttachment.getAttackRolls(u.getOwner());
			else
				strength += unitAttachment.getDefense(u.getOwner()) * (unitAttachment.getIsTwoHit() ? 2 : 1);
			if (attacking)
			{
				if (unitAttachment.getAttack(u.getOwner()) == 0)
					strength -= 0.50F; // adjusted KDM
			}
			if (unitAttachment.getTransportCapacity() > 0 && !transportsFirst)
				strength -= 0.50F; // only allow transport to have 0.35 on defense; none on attack
		}
		else if (unitAttachment.getIsAir() & sea) // we can count airplanes in sea attack
		{
			strength += 1.00F;
			if (attacking)
				strength += unitAttachment.getAttack(u.getOwner()) * unitAttachment.getAttackRolls(u.getOwner());
			else
				strength += unitAttachment.getDefense(u.getOwner());
		}
		return strength;
	}
	
	/**
	 * Get a quick and dirty estimate of the strength of some units in a battle.
	 * <p>
	 * 
	 * @param units
	 *            - the units to measure
	 * @param attacking
	 *            - are the units on attack or defense
	 * @param sea
	 *            - calculate the strength of the units in a sea or land battle?
	 * @return
	 */
	public static float strength(final Collection<Unit> units, final boolean attacking, final boolean sea, final boolean transportsFirst)
	{
		float strength = 0.0F;
		if (units.isEmpty())
			return strength;
		if (attacking && Match.noneMatch(units, Matches.unitHasAttackValueOfAtLeast(1)))
			return strength;
		else if (!attacking && Match.noneMatch(units, Matches.unitHasDefendValueOfAtLeast(1)))
			return strength;
		for (final Unit u : units)
		{
			final UnitAttachment unitAttachment = UnitAttachment.get(u.getType());
			if (unitAttachment.getIsInfrastructure())
				continue;
			else if (unitAttachment.getIsSea() == sea)
			{
				final int unitAttack = unitAttachment.getAttack(u.getOwner());
				// BB = 6.0; AC=2.0/4.0; SUB=3.0; DS=4.0; TR=0.50/2.0; F=4.0/5.0; B=5.0/2.0;
				strength += 1.00F; // played with this value a good bit
				if (attacking)
					strength += unitAttack * (unitAttachment.getIsTwoHit() ? 2 : 1);
				else
					strength += unitAttachment.getDefense(u.getOwner()) * (unitAttachment.getIsTwoHit() ? 2 : 1);
				if (attacking)
				{
					if (unitAttack == 0)
						strength -= 0.50F;
				}
				if (unitAttack == 0 && unitAttachment.getTransportCapacity() > 0 && !transportsFirst)
					strength -= 0.50F; // only allow transport to have 0.35 on defense; none on attack
			}
			else if (unitAttachment.getIsAir() == sea)
			{
				strength += 1.00F;
				if (attacking)
					strength += unitAttachment.getAttack(u.getOwner()) * unitAttachment.getAttackRolls(u.getOwner());
				else
					strength += unitAttachment.getDefense(u.getOwner());
			}
		}
		if (attacking && !sea)
		{
			final int art = Match.countMatches(units, Matches.UnitIsArtillery);
			final int artSupport = Match.countMatches(units, Matches.UnitIsArtillerySupportable);
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
	 * 
	 * @param data
	 * @param player
	 * @param risk
	 *            - not really used...should pass a relative risk back
	 * @param buyfactory
	 * @return
	 */
	public static Territory findFactoryTerritory(final GameData data, final PlayerID player, final float risk, boolean buyfactory, final boolean onWater)
	{
		final BattleDelegate delegate = DelegateFinder.battleDelegate(data);
		final CompositeMatch<Territory> enemyNoWater = new CompositeMatchAnd<Territory>(Matches.TerritoryIsNotImpassableToLandUnits(player, data),
					Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data));
		final CompositeMatch<Territory> alliedNoWater = new CompositeMatchAnd<Territory>(Matches.TerritoryIsNotImpassableToLandUnits(player, data), Matches.isTerritoryAllied(player, data));
		final CompositeMatch<Territory> endConditionEnemyLand = new CompositeMatchAnd<Territory>(Matches.isTerritoryEnemy(player, data), Matches.TerritoryIsNotImpassable, Matches.TerritoryIsLand);
		final CompositeMatch<Territory> routeConditionLand = new CompositeMatchAnd<Territory>(Matches.isTerritoryAllied(player, data), Matches.TerritoryIsNotImpassable, Matches.TerritoryIsLand);
		final List<Territory> owned = allOurTerritories(data, player);
		final List<Territory> existingFactories = SUtils.findTersWithUnitsMatching(data, player, Matches.UnitCanProduceUnits); // TODO: maybe use Matches.UnitCanProduceUnitsAndIsConstruction
		owned.removeAll(existingFactories);
		final List<Territory> isWaterConvoy = SUtils.onlyWaterTerr(data, owned);
		owned.removeAll(isWaterConvoy);
		final List<Territory> cloneFactTerritories = new ArrayList<Territory>(owned);
		for (final Territory deleteBad : cloneFactTerritories) // removed just conquered territories (for combat before purchase games) (veqryn)
		{
			if (delegate.getBattleTracker().wasConquered(deleteBad))
				owned.remove(deleteBad);
		}
		Collections.shuffle(owned);
		if (onWater)
		{
			final List<Territory> waterOwned = SUtils.stripLandLockedTerr(data, owned);
			owned.retainAll(waterOwned);
			if (owned.isEmpty())
				return null;
			final IntegerMap<Territory> terrProd = new IntegerMap<Territory>();
			for (final Territory prodTerr : owned)
			{
				// sorting territories to have ones with greatest production and closeness to enemy first (by land, then by sea) (veqryn)
				int territoryValue = 0;
				if (hasLandRouteToEnemyOwnedCapitol(prodTerr, player, data))
					territoryValue += 2;
				if (findNearest(prodTerr, Matches.territoryIsEnemyNonNeutralAndHasEnemyUnitMatching(data, player, Matches.UnitCanProduceUnits),
							Matches.TerritoryIsNotImpassableToLandUnits(player, data), data) != null)
					territoryValue += 2;
				Route r = findNearest(prodTerr, endConditionEnemyLand, routeConditionLand, data);
				if (r != null)
				{
					territoryValue += 10 - r.getLength();
				}
				else
				{
					r = findNearest(prodTerr, endConditionEnemyLand, Matches.TerritoryIsWater, data);
					if (r != null)
						territoryValue += 8 - r.getLength();
					else
						territoryValue -= 115;
				}
				territoryValue += 4 * TerritoryAttachment.get(prodTerr).getProduction();
				final List<Territory> weOwnAll = getNeighboringEnemyLandTerritories(data, player, prodTerr);
				final List<Territory> isWater = SUtils.onlyWaterTerr(data, weOwnAll);
				weOwnAll.removeAll(isWater);
				final Iterator<Territory> weOwnAllIter = weOwnAll.iterator();
				while (weOwnAllIter.hasNext())
				{
					final Territory tempFact = weOwnAllIter.next();
					if (Matches.TerritoryIsNeutralButNotWater.match(tempFact) || Matches.TerritoryIsImpassable.match(tempFact))
						weOwnAllIter.remove();
				}
				territoryValue -= 15 * weOwnAll.size();
				if (TerritoryAttachment.get(prodTerr).getProduction() < 2 || Matches.TerritoryIsImpassable.match(prodTerr))
					territoryValue -= 100;
				if (TerritoryAttachment.get(prodTerr).getProduction() < 1 || Matches.TerritoryIsImpassable.match(prodTerr))
					territoryValue -= 100;
				terrProd.put(prodTerr, territoryValue);
			}
			SUtils.reorder(owned, terrProd, true);
			return owned.get(0); // TODO: cleanup this to buy the best possible location
		}
		// TODO: we need to put the territories in an order that is a mix between high production and closeness to the enemy
		// because currently this entire factory location picker just picks the first good territory it finds. (veqryn)
		final IntegerMap<Territory> terrProd = new IntegerMap<Territory>();
		for (final Territory prodTerr : owned)
		{
			// sorting territories to have ones with greatest production and closeness to enemy first (by land, then by sea) (veqryn)
			int territoryValue = 0;
			if (hasLandRouteToEnemyOwnedCapitol(prodTerr, player, data))
				territoryValue += 3;
			if (findNearest(prodTerr, Matches.territoryIsEnemyNonNeutralAndHasEnemyUnitMatching(data, player, Matches.UnitCanProduceUnits), Matches.TerritoryIsNotImpassableToLandUnits(player, data),
						data) != null)
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
		// float minRisk = 1.0F;
		// risk = 1.0F;
		final IntegerMap<Territory> prodMap = new IntegerMap<Territory>();
		for (final Territory factTerr : existingFactories)
			prodMap.put(factTerr, TerritoryAttachment.get(factTerr).getProduction());
		for (final Territory t : owned)
		{
			final int puValue = TerritoryAttachment.get(t).getProduction();
			if (puValue < 2 || Matches.territoryIsOwnedAndHasOwnedUnitMatching(data, player, Matches.UnitCanProduceUnits).match(t))
				continue;
			final List<Territory> weOwnAll = getNeighboringEnemyLandTerritories(data, player, t);
			final List<Territory> isWater = SUtils.onlyWaterTerr(data, weOwnAll);
			weOwnAll.removeAll(isWater);
			final Iterator<Territory> weOwnAllIter = weOwnAll.iterator();
			while (weOwnAllIter.hasNext())
			{
				final Territory tempFact = weOwnAllIter.next();
				if (Matches.TerritoryIsNeutralButNotWater.match(tempFact) || Matches.TerritoryIsImpassable.match(tempFact))
					weOwnAllIter.remove();
			}
			if (weOwnAll.size() > 0)
				continue;
			/*int numOnContinent = 0;
			for (Territory factTerr : existingFactories)
			{
				Route fRoute = data.getMap().getRoute(t, factTerr, Matches.TerritoryIsNotImpassableToLandUnits(player));
				if (fRoute != null && fRoute.getEnd() != null)
					numOnContinent = prodMap.getInt(factTerr);
			}*/
			// This prevents purchasing a factory in a map like NWO
			// if (numOnContinent >= 6)
			// continue;
			final List<Territory> twoAway = getExactNeighbors(t, 2, player, data, false);
			final List<Territory> threeAway = getExactNeighbors(t, 3, player, data, false);
			final List<Territory> closeAllies = SUtils.getNeighboringLandTerritories(data, player, t);
			float tStrength = strength(t.getUnits().getMatches(Matches.unitIsOwnedBy(player)), false, false, false);
			for (final Territory cA : closeAllies)
			{
				tStrength += SUtils.strength(cA.getUnits().getMatches(Matches.alliedUnit(player, data)), false, false, false);
			}
			boolean badIdea = false;
			float twoCheckStrength = 0.0F, threeCheckStrength = 0.0F;
			for (final Territory twoCheck : twoAway)
			{
				if (Matches.territoryHasEnemyUnits(player, data).match(twoCheck))
					twoCheckStrength += strength(twoCheck.getUnits().getMatches(Matches.enemyUnit(player, data)), true, false, false);
			}
			if (twoCheckStrength > (puValue * 3.0F + tStrength))
				badIdea = true;
			/* TODO: (veqryn) this portion of the code counts naval vessels and any other enemy units
					 within 3 spaces of the territory that will have a factory.  It only compares it to the
					 ai's units on the territory, and does not take into account any of the ai's units in other
					 territories nearby.  This needs to only care about LAND and AIR units and not care about SEA units.
					 And it needs to take into account friendly land and air within 2 spaces of the territory.
			*/
			for (final Territory threeCheck : threeAway)
			{
				if (Matches.territoryHasEnemyUnits(player, data).match(threeCheck))
				{ // only count it if it has a path
					// Route d1 = data.getMap().getLandRoute(threeCheck, t);
					threeCheckStrength += strength(threeCheck.getUnits().getMatches(Matches.enemyUnit(player, data)), true, false, false);
				}
			}
			if ((twoCheckStrength + threeCheckStrength) > (puValue * 8.0F + tStrength) * 4) // take at least 2 moves to invade (veqryn multiplying friendly for now)
			{
				badIdea = true;
			}
			if (badIdea)
				continue;
			final Route nearEnemyRoute = findNearest(t, enemyNoWater, alliedNoWater, data);
			final Route factoryRoute = findNearest(t, Matches.territoryIsEnemyNonNeutralAndHasEnemyUnitMatching(data, player, Matches.UnitCanProduceUnits),
						Matches.TerritoryIsNotImpassableToLandUnits(player, data), data);
			if (nearEnemyRoute == null || factoryRoute == null)
				continue;
			// int routeLength = nearEnemyRoute.getLength();
			final int factoryLength = factoryRoute.getLength();
			if (buyfactory && hasLandRouteToEnemyOwnedCapitol(t, player, data) && factoryLength <= 8) // try to keep Britain from building in SA
			{
				minTerr = t;
				// risk = 0.00F;
				return minTerr;
			}
		}
		// risk = minRisk;
		buyfactory = false;
		return minTerr;
	}
	
	/**
	 * Gets the neighbors which are exactly a certain # of territories away (distance)
	 * Removes the inner circle neighbors
	 * neutral - whether to include neutral countries
	 */
	@SuppressWarnings("unchecked")
	public static List<Territory> getExactNeighbors(final Territory territory, final int distance, final PlayerID player, final GameData data, final boolean neutral)
	{
		// old functionality retained, i.e. no route condition is imposed.
		// feel free to change, if you are confortable all calls to this function conform.
		final CompositeMatch<Territory> endCond = new CompositeMatchAnd<Territory>(Matches.TerritoryIsImpassable.invert());
		if (!neutral || Properties.getNeutralsImpassable(data))
			endCond.add(Matches.TerritoryIsNeutralButNotWater.invert());
		return findFontier(territory, endCond, Match.ALWAYS_MATCH, distance, data);
		/*
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
		// return startClone;
	}
	
	/**
	 * Does this Route contain water anywhere
	 * Differs from route.crossesWater in that it checks the beginning and end
	 * 
	 * @param testRoute
	 * @return - true if water does not exist...false if water does exist
	 */
	public static boolean RouteHasNoWater(final Route testRoute)
	{// simply...does the route contain a water territory
		final int routeLength = testRoute.getLength();
		boolean nowater = true;
		for (int i = 0; i < routeLength; i++)
		{
			final Territory t = testRoute.getTerritories().get(i);
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
	 * @param landTerr
	 *            - factory territory
	 * @param tFirst
	 *            - can transports be killed during battle
	 * 
	 *            Should be modified to include the list of units which will be dropped (for strength measurement)
	 */
	public static Territory findASeaTerritoryToPlaceOn(final Territory landTerr, final GameData data, final PlayerID player, final boolean tFirst)
	{
		final CompositeMatch<Territory> ourSeaTerr = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasUnitsOwnedBy(player));
		final CompositeMatch<Unit> seaUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsSea);
		final CompositeMatch<Unit> airUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsAir);
		final CompositeMatch<Unit> seaAirUnit = new CompositeMatchOr<Unit>(seaUnit, airUnit);
		Territory seaPlaceAt = null, bestSeaPlaceAt = null;
		Territory xPlace = null;
		if (landTerr == null)
			return seaPlaceAt;
		final Set<Territory> seaNeighbors = data.getMap().getNeighbors(landTerr, ourSeaTerr);
		// float eStrength = 0.0F;
		float minStrength = 1000.0F, maxStrength = -1000.0F;
		for (final Territory t : seaNeighbors) // give preference to territory with units
		{
			float enemyStrength = getStrengthOfPotentialAttackers(t, data, player, tFirst, true, null);
			final float extraEnemy = SUtils.strength(t.getUnits().getMatches(Matches.enemyUnit(player, data)), true, true, tFirst);
			enemyStrength += extraEnemy;
			float ourStrength = strength(t.getUnits().getMatches(seaAirUnit), false, true, tFirst);
			final float existingStrength = strength(t.getUnits().getMatches(Matches.alliedUnit(player, data)), false, true, tFirst);
			ourStrength += existingStrength;
			final float strengthDiff = enemyStrength - ourStrength;
			if (strengthDiff < minStrength && ourStrength > 0.0F)
			{
				seaPlaceAt = t;
				minStrength = strengthDiff;
			}
			if (strengthDiff > maxStrength && strengthDiff < 3.0F && (ourStrength > 0.0F || existingStrength > 0.0F))
			{
				bestSeaPlaceAt = t;
				maxStrength = strengthDiff;
			}
		}
		if (seaPlaceAt == null && bestSeaPlaceAt == null)
		{
			final Set<Territory> seaNeighbors2 = data.getMap().getNeighbors(landTerr, Matches.TerritoryIsWater);
			for (final Territory t : seaNeighbors2) // find Terr away from enemy units
			{
				final float enemyStrength = getStrengthOfPotentialAttackers(t, data, player, tFirst, true, null);
				final float ourStrength = strength(t.getUnits().getMatches(seaAirUnit), false, true, tFirst);
				if (t.getUnits().someMatch(Matches.enemyUnit(player, data)))
				{
					xPlace = t; // try to avoid Territories with enemy Units
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
			seaPlaceAt = xPlace; // this will be null if there are no water territories
		if (bestSeaPlaceAt == null)
			return seaPlaceAt;
		else
			return bestSeaPlaceAt;
	}
	
	/*
	 * Invite escorts ships purely for bombarding territory
	 * Does not use strength method because it will add in 1.0F for each unit and the escorts cannot take a loss
	 */
	public static float inviteBBEscort(final Territory goTerr, final float remainingStrengthNeeded, final Collection<Unit> unitsAlreadyMoved, final List<Collection<Unit>> moveUnits,
				final List<Route> moveRoutes, final GameData data, final PlayerID player)
	{
		final CompositeMatch<Unit> BBUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.unitCanBombard(player), Matches.UnitCanMove, Matches.unitHasMovementLeft);
		float BBStrength = 0.0F;
		final List<Territory> BBTerr = findOurShips(goTerr, data, player, BBUnit);
		for (final Territory BBT : BBTerr)
		{
			final List<Unit> BBUnits = BBT.getUnits().getMatches(BBUnit);
			BBUnits.removeAll(unitsAlreadyMoved);
			if (BBUnits.isEmpty())
				continue;
			final List<Unit> BBAddUnits = new ArrayList<Unit>();
			if (BBT == goTerr)
			{
				final Iterator<Unit> BBIter = BBUnits.iterator();
				while (BBIter.hasNext() && BBStrength < remainingStrengthNeeded)
				{
					final Unit BB = BBIter.next();
					final UnitAttachment ua = UnitAttachment.get(BB.getType());
					BBStrength += ua.getAttack(player) * ua.getAttackRolls(player);
					unitsAlreadyMoved.add(BB);
				}
				continue;
			}
			final int BBDistance = MoveValidator.getLeastMovement(BBUnits);
			final Route BBRoute = getMaxSeaRoute(data, BBT, goTerr, player, true, BBDistance);
			if (BBRoute == null || BBRoute.getEnd() == null || BBRoute.getEnd() != goTerr)
				continue;
			final Iterator<Unit> BBIter = BBUnits.iterator();
			while (BBIter.hasNext() && BBStrength < remainingStrengthNeeded)
			{
				final Unit BB = BBIter.next();
				final UnitAttachment ua = UnitAttachment.get(BB.getType());
				BBStrength += ua.getAttack(player) * ua.getAttackRolls(player);
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
	
	public static float inviteShipAttack(final Territory eTerr, final float remainingStrengthNeeded, final Collection<Unit> unitsAlreadyMoved, final List<Collection<Unit>> moveUnits,
				final List<Route> moveRoutes, final GameData data, final PlayerID player, final boolean attacking, final boolean tFirst, final boolean includeTransports)
	{
		return inviteShipAttack(eTerr, remainingStrengthNeeded, unitsAlreadyMoved, moveUnits, moveRoutes, data, player, attacking, tFirst, includeTransports, null);
	}
	
	/**
	 * Invite Ship Attack to a certain sea territory (enemy)
	 * Air on a carrier will be included
	 * Transports will only be included if tFirst is true
	 * Units to be moved will be placed in moveUnits and routes in moveRoutes
	 * carrier & fighters will be moved as a single unit
	 */
	public static float inviteShipAttack(final Territory eTerr, final float remainingStrengthNeeded, final Collection<Unit> unitsAlreadyMoved, final List<Collection<Unit>> moveUnits,
				final List<Route> moveRoutes, final GameData data, final PlayerID player, final boolean attacking, final boolean tFirst, final boolean includeTransports, final Match<Unit> types)
	{
		final BattleDelegate battleD = DelegateFinder.battleDelegate(data);
		CompositeMatch<Unit> ownedSeaUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsSea, Matches.UnitCanMove, Matches.unitHasMovementLeft);
		CompositeMatch<Unit> ownedAirUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsAir, Matches.UnitCanLandOnCarrier, Matches.UnitCanMove,
					Matches.unitHasMovementLeft);
		if (types != null)
		{
			ownedSeaUnit = new CompositeMatchAnd<Unit>(types, ownedSeaUnit);
			ownedAirUnit = new CompositeMatchAnd<Unit>(types, ownedAirUnit);
		}
		final CompositeMatch<Unit> carrierUnit = new CompositeMatchAnd<Unit>(ownedSeaUnit, Matches.UnitIsCarrier, Matches.UnitCanMove, Matches.unitHasMovementLeft);
		final CompositeMatch<Unit> carrierAndFighters = new CompositeMatchOr<Unit>(carrierUnit, ownedAirUnit);
		final CompositeMatch<Unit> ownedSeaUnitSansTransports = new CompositeMatchAnd<Unit>(ownedSeaUnit, Matches.UnitIsNotTransport, Matches.UnitCanMove, Matches.unitHasMovementLeft);
		if (types != null)
			ownedSeaUnit = new CompositeMatchAnd<Unit>(types, ownedSeaUnit);
		final HashMap<Territory, Float> attackShipMap = new HashMap<Territory, Float>();
		final HashMap<Territory, List<Unit>> attackUnitMap = new HashMap<Territory, List<Unit>>();
		final HashMap<Territory, List<Unit>> carrierUnitMap = new HashMap<Territory, List<Unit>>();
		final List<Territory> possibleShipTerr = SUtils.findTersWithUnitsMatching(data, player, ownedSeaUnit);
		final Iterator<Territory> pSIter = possibleShipTerr.iterator();
		while (pSIter.hasNext())
		{// Remove if: 1) Land; 2) No Sea Units; 3) Battle Already Fought in Sea Zone
			final Territory countX = pSIter.next();
			if (Matches.TerritoryIsLand.match(countX) || battleD.getBattleTracker().wasBattleFought(countX))
				pSIter.remove();
		}
		final Iterator<Territory> pSIter2 = possibleShipTerr.iterator();
		while (pSIter2.hasNext())
		{
			final Territory shipTerr = pSIter2.next();
			float terrStrength = 0.0F;
			final List<Unit> attackShips = new ArrayList<Unit>();
			final List<Unit> carrierShips = new ArrayList<Unit>();
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
			final Route thisRoute = getMaxSeaRoute(data, shipTerr, eTerr, player, attacking, maxShipDistance);
			if (thisRoute == null || thisRoute.getEnd() != eTerr)
			{
				pSIter2.remove();
				continue;
			}
			if (carrierShips.size() > 0 && MoveValidator.hasEnoughMovement(carrierShips, thisRoute))
			{
				terrStrength += strength(carrierShips, attacking, true, tFirst);
			}
			final Iterator<Unit> aSIter = attackShips.iterator();
			while (aSIter.hasNext())
			{
				final Unit attackShip = aSIter.next();
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
		// now that they are ordered, add them in whole groups
		float unitStrength = 0.0F;
		for (final Territory addShipTerr : possibleShipTerr)
		{
			final List<Unit> theseUnits = attackUnitMap.get(addShipTerr);
			final List<Unit> otherUnits = carrierUnitMap.get(addShipTerr);
			if (theseUnits.isEmpty() && otherUnits.isEmpty())
				continue;
			int maxUnitDistance = 0;
			if (!theseUnits.isEmpty())
				maxUnitDistance = MoveValidator.getMaxMovement(theseUnits);
			else
				maxUnitDistance = MoveValidator.getMaxMovement(otherUnits);
			final Route newRoute = getMaxSeaRoute(data, addShipTerr, eTerr, player, attacking, maxUnitDistance);
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
	 * @param noncombat
	 *            - is this in noncombat
	 * @param target
	 *            - Land Territory needing units
	 * @param remainingStrengthNeeded
	 *            - how many units we needed moved to this location
	 * @param unitsAlreadyMoved
	 *            - List of Units which is not available for further movement
	 */
	public static float inviteTransports(final boolean noncombat, final Territory target, final float remainingStrengthNeeded, final Collection<Unit> unitsAlreadyMoved,
				final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final GameData data, final PlayerID player, final boolean tFirst, final boolean allowEnemy,
				final List<Territory> seaTerrAttacked)
	{ // needs a check for remainingStrength
		final TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();
		final CompositeMatch<Unit> airUnits = new CompositeMatchAnd<Unit>(Matches.UnitCanLandOnCarrier, Matches.unitIsOwnedBy(player), Matches.UnitCanMove, Matches.unitHasMovementLeft);
		final CompositeMatch<Unit> escortShip = new CompositeMatchAnd<Unit>(Matches.UnitIsSea, Matches.UnitIsNotTransport, Matches.unitIsOwnedBy(player), Matches.UnitCanMove,
					Matches.unitHasMovementLeft);
		final CompositeMatch<Unit> escortUnit = new CompositeMatchOr<Unit>(airUnits, escortShip);
		// Inviting an empty transport is useless, so only get ones with units on them
		final CompositeMatch<Unit> transportingUnitWithLoad = new CompositeMatchAnd<Unit>(Matches.UnitIsTransport, Matches.unitIsOwnedBy(player), Matches.UnitCanMove,
					Matches.unitHasMovementLeft, Matches.transportIsTransporting());
		final Set<Territory> tCopy = data.getMap().getNeighbors(target, 3);
		final List<Territory> testCapNeighbors = new ArrayList<Territory>(tCopy);
		final List<Territory> waterNeighbors = new ArrayList<Territory>();
		final List<Territory> alreadyMovedFrom = new ArrayList<Territory>();
		final List<Territory> myFactories = findTersWithUnitsMatching(data, player, Matches.UnitCanProduceUnits);
		final List<Territory> waterFactoryNeighbors = new ArrayList<Territory>();
		for (final Territory myFactory : myFactories)
		{
			final Set<Territory> wFN = data.getMap().getNeighbors(myFactory, Matches.TerritoryIsWater);
			waterFactoryNeighbors.addAll(wFN);
		}
		for (final Territory firstWaterCheck : tCopy)
		{
			if (!firstWaterCheck.isWater())
				testCapNeighbors.remove(firstWaterCheck);
			else if (data.getMap().getDistance(target, firstWaterCheck) == 1)
			{
				waterNeighbors.add(firstWaterCheck);
				testCapNeighbors.remove(firstWaterCheck);
			}
		}
		final int waterSize = waterNeighbors.size();
		for (int i = 0; i < waterSize - 1; i++)
		{
			for (int j = i + 1; j < waterSize; j++)
			{
				final Territory iTerr = waterNeighbors.get(i);
				final Territory jTerr = waterNeighbors.get(j);
				if (!waterFactoryNeighbors.contains(iTerr) && waterFactoryNeighbors.contains(jTerr))
				{
					waterNeighbors.remove(jTerr);
					waterNeighbors.remove(iTerr);
					waterNeighbors.add(i, jTerr);
					waterNeighbors.add(j, iTerr);
				}
			}
		}
		// boolean transportsForAttack = false;
		// Territory firstLocation = null;
		float unitStrength = 0.0F;
		for (final Territory waterCheck : waterNeighbors)
		{
			final float strengthAtTarget = getStrengthOfPotentialAttackers(waterCheck, data, player, tFirst, false, seaTerrAttacked);
			float shipStrength = 0.0F;
			if (Matches.territoryHasOwnedTransportingUnits(player).match(waterCheck))
			{
				// int xminDist = 0;
				final List<Unit> tUnits = new ArrayList<Unit>();
				final List<Unit> tranUnits = waterCheck.getUnits().getMatches(transportingUnitWithLoad);
				tranUnits.removeAll(unitsAlreadyMoved);
				final List<Unit> escorts = waterCheck.getUnits().getMatches(escortUnit);
				escorts.removeAll(unitsAlreadyMoved);
				for (final Unit xTran : tranUnits)
				{
					if (remainingStrengthNeeded > unitStrength)
					{
						final Collection<Unit> loadOne = tracker.transporting(xTran);
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
					for (final Unit testEscort : escorts)
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
					unitsAlreadyMoved.addAll(tUnits); // no actual move needed...just stay here
					// transportsForAttack = true;
				}
			}
			for (final Territory otherSource : testCapNeighbors)
			{
				if (alreadyMovedFrom.contains(otherSource))
					continue;
				alreadyMovedFrom.add(otherSource);
				final List<Unit> tranUnits = otherSource.getUnits().getMatches(transportingUnitWithLoad);
				tranUnits.removeAll(unitsAlreadyMoved);
				if (tranUnits.isEmpty())
					continue;
				final int maxDistance = MoveValidator.getMaxMovement(tranUnits);
				final Route sRoute = getMaxSeaRoute(data, otherSource, waterCheck, player, allowEnemy, maxDistance);
				if (sRoute == null || sRoute.getEnd() != waterCheck)
					continue;
				final int newDist = sRoute.getLength();
				final Iterator<Unit> tranIter = tranUnits.iterator();
				while (tranIter.hasNext())
				{
					final Unit thisTran = tranIter.next();
					final TripleAUnit ta = TripleAUnit.get(thisTran);
					if (ta.getMovementLeft() < newDist)
						tranIter.remove();
					else if (!tracker.isTransporting(thisTran))
						tranIter.remove();
				}
				final List<Unit> escorts = otherSource.getUnits().getMatches(escortUnit);
				escorts.removeAll(unitsAlreadyMoved);
				final Iterator<Unit> escortIter = escorts.iterator();
				while (escortIter.hasNext())
				{
					final Unit thisEscort = escortIter.next();
					if (Matches.unitHasMoved.match(thisEscort))
						escortIter.remove();
				}
				final List<Unit> allUnits = new ArrayList<Unit>();
				for (final Unit xTran : tranUnits)
				{
					if (remainingStrengthNeeded > unitStrength)
					{
						final Collection<Unit> loadOne = tracker.transporting(xTran);
						unitStrength += strength(loadOne, true, false, false);
						allUnits.add(xTran);
						allUnits.addAll(loadOne);
						if (tFirst)
							shipStrength += uStrength(xTran, false, true, tFirst);
					}
				}
				if (shipStrength < strengthAtTarget)
				{
					for (final Unit eUnit : escorts)
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
					// transportsForAttack = true;
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
	 * @param fightersOnly
	 *            - ignore anything that cannot land on AC
	 * @param withCarrier
	 *            - fighters on Carriers are accompanied by carrier
	 * @param target
	 *            - target territory
	 * @param remainingStrengthNeeded
	 *            - use to determine how many to bring
	 * @param unitsAlreadyMoved
	 *            - Units not available for further movement
	 */
	public static float invitePlaneAttack(final boolean noncombat, final boolean fightersOnly, final Territory target, float remainingStrengthNeeded, final Collection<Unit> unitsAlreadyMoved,
				final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final GameData data, final PlayerID player)
	{
		final CompositeMatch<Unit> airUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsAir, Matches.UnitCanMove, Matches.unitHasMovementLeft);
		final CompositeMatch<Unit> fighterUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanLandOnCarrier, Matches.UnitCanMove, Matches.unitHasMovementLeft);
		final CompositeMatch<Unit> carrierUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsCarrier, Matches.UnitCanMove, Matches.unitHasMovementLeft);
		final List<Territory> planeTerr = findOurPlanes(target, data, player);
		final List<Territory> planeOnWater = new ArrayList<Territory>(planeTerr);
		for (final Territory qP : planeTerr)
		{
			if (Matches.TerritoryIsLand.match(qP))
				planeOnWater.remove(qP);
		}
		int availSpace = 0;
		final List<Unit> ACUnits = target.getUnits().getMatches(carrierUnit);
		final List<Unit> fightersOnAC = target.getUnits().getMatches(fighterUnit);
		final boolean isWater = target.isWater();
		float committedStrength = 0.0F;
		if (isWater)
		{
			if (noncombat)
				availSpace = ACUnits.size() * 2 - fightersOnAC.size();
			for (final Territory owned : planeOnWater) // make sure that these planes are not already involved in an attack
			{
				if (noncombat && availSpace <= 0)
					continue;
				final List<Unit> tmpUnits2 = new ArrayList<Unit>();
				if (remainingStrengthNeeded > committedStrength && (!Matches.territoryHasEnemyUnits(player, data).match(owned) || noncombat))
				{
					final Route thisRoute = data.getMap().getRoute(owned, target, Matches.TerritoryIsNotImpassable);
					if (thisRoute == null)
						continue;
					final int rDist = thisRoute.getLength();
					final List<Unit> allAirUnits = owned.getUnits().getMatches(fighterUnit);
					for (final Unit u2 : allAirUnits)
					{
						if (u2 != null && noncombat == availSpace > 0)
						{
							if (MoveValidator.hasEnoughMovement(u2, rDist) && !unitsAlreadyMoved.contains(u2) && remainingStrengthNeeded > committedStrength)
							{
								boolean canLand = airUnitIsLandable(u2, owned, target, player, data);
								if (noncombat && fightersOnly && availSpace > 0)
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
		for (final Territory owned : planeTerr) // make sure that these planes are not already involved in an attack
		{
			if (noncombat && isWater && availSpace <= 0)
				continue;
			final List<Unit> tmpUnits2 = new ArrayList<Unit>();
			if (remainingStrengthNeeded > committedStrength && !Matches.territoryHasEnemyUnits(player, data).match(owned))
			{
				final Route thisRoute = data.getMap().getRoute(owned, target, Matches.TerritoryIsNotImpassable);
				if (thisRoute == null)
					continue;
				final int rDist = thisRoute.getLength();
				List<Unit> allAirUnits = new ArrayList<Unit>();
				if (fightersOnly)
					allAirUnits.addAll(owned.getUnits().getMatches(fighterUnit));
				else
					allAirUnits = owned.getUnits().getMatches(airUnit);
				for (final Unit u2 : allAirUnits)
				{
					if (u2 != null && MoveValidator.hasEnoughMovement(u2, rDist) && !unitsAlreadyMoved.contains(u2))
					{
						boolean canLand = airUnitIsLandable(u2, owned, target, player, data);
						if (noncombat && !isWater)
							canLand = true;
						else if (noncombat && fightersOnly && availSpace > 0)
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
	 * 
	 * @param enemy
	 *            - territory to be invaded
	 * @param remainingStrengthNeeded
	 *            - total strength of units needed - stop adding when this reaches 0.0
	 */
	public static float inviteLandAttack(final boolean nonCombat, final Territory enemy, final float remainingStrengthNeeded, final Collection<Unit> unitsAlreadyMoved,
				final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final GameData data, final PlayerID player, final boolean attacking, final boolean forced,
				final List<Territory> alreadyAttacked)
	{
		final CompositeMatch<Unit> landUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsLand, Matches.UnitIsNotAA, Matches.UnitCanMove,
					Matches.UnitIsNotInfrastructure, Matches.unitHasMovementLeft, Matches.UnitCanNotMoveDuringCombatMove.invert());
		final List<Territory> ourLandNeighbors = getNeighboringLandTerritories(data, player, enemy);
		float totStrength = 0.0F;
		final int totList = ourLandNeighbors.size();
		// reorder with the terr with neighbors having fewest enemy units first
		for (int i = 0; i < totList - 1; i++)
		{
			for (int j = i + 1; j < totList; j++)
			{
				final Territory iTerr = ourLandNeighbors.get(i);
				final Territory jTerr = ourLandNeighbors.get(j);
				int jUnits = 0, iUnits = 0;
				final Set<Territory> jTerrNeighbors = data.getMap().getNeighbors(jTerr, Matches.territoryHasEnemyUnits(player, data));
				jTerrNeighbors.removeAll(alreadyAttacked);
				final Set<Territory> iTerrNeighbors = data.getMap().getNeighbors(iTerr, Matches.territoryHasEnemyUnits(player, data));
				iTerrNeighbors.removeAll(alreadyAttacked);
				for (final Territory jT : jTerrNeighbors)
					jUnits += jT.getUnits().countMatches(Matches.enemyUnit(player, data));
				for (final Territory iT : iTerrNeighbors)
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
		// interleave units using transport sorter so that infantry and artillery are next to each other
		for (final Territory invadeFrom : ourLandNeighbors)
		{
			final List<Unit> ourAttackUnits = invadeFrom.getUnits().getMatches(landUnit);
			ourAttackUnits.removeAll(unitsAlreadyMoved);
			final List<Unit> ourSortedUnits = sortTransportUnits(ourAttackUnits);
			final List<Unit> attackUnits = new ArrayList<Unit>();
			final Route attackRoute = data.getMap().getLandRoute(invadeFrom, enemy);
			if (attackRoute == null)
				continue;
			final Iterator<Unit> sortIter = ourSortedUnits.iterator();
			while (sortIter.hasNext() && remainingStrengthNeeded > totStrength)
			{
				float aStrength = 0.0F;
				final Unit attackUnit = sortIter.next();
				if (Matches.UnitTypeIsInfantry.match(attackUnit.getType())) // look at the next unit
				{
					if (sortIter.hasNext())
					{
						final Unit attackUnit2 = sortIter.next();
						final List<Unit> twoUnits = new ArrayList<Unit>();
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
			if (attackUnits.isEmpty())
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
	public static float inviteBlitzAttack(final boolean nonCombat, final Territory enemy, final float remainingStrengthNeeded, final Collection<Unit> unitsAlreadyMoved,
				final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final GameData data, final PlayerID player, final boolean attacking, final boolean forced)
	{// Blitz through owned into enemy
		final CompositeMatch<Unit> blitzUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanBlitz, Matches.UnitCanMove, Matches.unitHasMovementLeft);
		final CompositeMatch<Territory> alliedAndNotWater = new CompositeMatchAnd<Territory>(Matches.isTerritoryAllied(player, data), Matches.TerritoryIsNotImpassableToLandUnits(player, data));
		final CompositeMatch<Territory> noEnemyUnitsAndNotWater = new CompositeMatchAnd<Territory>(Matches.territoryHasNoEnemyUnits(player, data), Matches.TerritoryIsNotImpassableToLandUnits(player,
					data));
		// Are there blitzable units available?
		final List<Territory> blitzFrom = getExactNeighbors(enemy, 2, player, data, false);
		final List<Territory> blitzCopy = new ArrayList<Territory>(blitzFrom);
		Route tRoute = null;
		float totStrength = 0.0F;
		for (final Territory t : blitzCopy)
		{
			if (nonCombat)
				tRoute = getTwoRoute(t, enemy, alliedAndNotWater, null, data);
			else
				tRoute = getTwoRoute(t, enemy, noEnemyUnitsAndNotWater, null, data);
			if (tRoute == null || tRoute.getLength() > 2)
				blitzFrom.remove(t);
		}
		final List<Unit> blitzUnits = new ArrayList<Unit>();
		float bStrength = 0.0F;
		if (forced) // if a route is available, bring in the units no matter what
		{
			for (final Territory blitzTerr : blitzFrom)
			{
				blitzUnits.clear();
				final List<Unit> tmpBlitz = new ArrayList<Unit>();
				blitzUnits.addAll(blitzTerr.getUnits().getMatches(blitzUnit));
				if (blitzUnits.isEmpty())
					continue;
				final Route blitzRoute = getTwoRoute(blitzTerr, enemy, noEnemyUnitsAndNotWater, null, data);
				if (blitzRoute != null)
				{
					for (final Unit blitzer : blitzUnits)
					{
						if (remainingStrengthNeeded > totStrength)
						{
							bStrength = uStrength(blitzer, attacking, false, false);
							totStrength += bStrength;
							tmpBlitz.add(blitzer);
						}
					}
					if (tmpBlitz.isEmpty())
						continue;
					moveUnits.add(tmpBlitz);
					moveRoutes.add(blitzRoute);
					unitsAlreadyMoved.addAll(tmpBlitz);
				}
				blitzUnits.clear();
			}
		}
		else
		// the source territory must not have enemy Units around it
		{
			for (final Territory blitzTerr : blitzFrom)
			{
				blitzUnits.clear();
				final Set<Territory> badTerr = data.getMap().getNeighbors(blitzTerr, Matches.territoryHasEnemyLandUnits(player, data));
				if (badTerr.isEmpty())
				{
					blitzUnits.addAll(blitzTerr.getUnits().getMatches(blitzUnit));
					if (blitzUnits.isEmpty())
						continue;
					final List<Unit> tmpBlitz = new ArrayList<Unit>();
					final Route blitzRoute = getTwoRoute(blitzTerr, enemy, noEnemyUnitsAndNotWater, null, data);
					if (blitzRoute != null)
					{
						for (final Unit blitzer : blitzUnits)
						{
							if (remainingStrengthNeeded > totStrength && !unitsAlreadyMoved.contains(blitzer))
							{
								tmpBlitz.add(blitzer);
								bStrength = uStrength(blitzer, attacking, false, false);
								totStrength += bStrength;
							}
						}
						if (tmpBlitz.isEmpty())
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
	 * 
	 * @param ourTerr
	 * @param byLand
	 *            - force the route to be traced by Land
	 * @return Territory closest or null if none has a land route
	 */
	public static Territory closestToEnemyCapital(final List<Territory> ourTerr, final GameData data, final PlayerID player, final boolean byLand)
	{
		final List<Territory> enemyCap = getEnemyCapitals(data, player);
		int thisDist = 0, capDist = 100;
		Territory returnTerr = null;
		for (final Territory checkTerr : ourTerr)
		{
			for (final Territory eCap : enemyCap)
			{
				if (byLand)
					thisDist = data.getMap().getDistance(checkTerr, eCap, Matches.TerritoryIsNotImpassableToLandUnits(player, data));
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
	 * 
	 * @param blockedTerr
	 *            - List of Territories the route cannot pass through
	 */
	public static Route getTwoRoute(final Territory t1, final Territory t2, final Match<Territory> condition, final List<Territory> blockedTerr, final GameData data)
	{
		if (t1.equals(t2))
			return null;
		Route r = new Route();
		r.setStart(t1);
		final Set<Territory> circleMatch = data.getMap().getNeighbors(t1, condition);
		if (circleMatch.contains(t2)) // neighbors
			return null;
		final Set<Territory> checkTerr = data.getMap().getNeighbors(t2, condition);
		circleMatch.retainAll(checkTerr);
		boolean routeCompleted = false;
		final Iterator<Territory> circleIter = circleMatch.iterator();
		while (circleIter.hasNext() && !routeCompleted)
		{
			final Territory t3 = circleIter.next();
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
	 * 
	 * @param blitzHere
	 *            - Territory expecting to be blitzed
	 * @param blitzTerr
	 *            - Territory which is being blitzed through (not guaranteed to be all possible route territories!)
	 * @param data
	 * @param ePlayer
	 *            - the enemy Player
	 * @return actual strength of enemy units (armor)
	 */
	public static float determineEnemyBlitzStrength(final Territory blitzHere, final List<Route> blitzTerrRoutes, final List<Territory> blockTerr, final GameData data, final PlayerID ePlayer)
	{
		final HashSet<Integer> ignore = new HashSet<Integer>();
		ignore.add(Integer.valueOf(1));
		final CompositeMatch<Unit> blitzUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(ePlayer), Matches.UnitCanBlitz, Matches.UnitCanMove);
		final CompositeMatch<Territory> validBlitzRoute = new CompositeMatchAnd<Territory>(Matches.territoryHasNoEnemyUnits(ePlayer, data), Matches.TerritoryIsNotImpassableToLandUnits(ePlayer, data));
		final List<Route> routes = new ArrayList<Route>();
		final List<Unit> blitzUnits = findAttackers(blitzHere, 2, ignore, ePlayer, data, blitzUnit, validBlitzRoute, blockTerr, routes, false);
		if (routes != null)
			for (final Route r : routes)
			{
				if (r.getLength() == 2)
					blitzTerrRoutes.add(r);
			}
		return strength(blitzUnits, true, false, true);
		/*
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
		*/
	}
	
	/**
	 * Determine if a list has something other than a transport
	 * Use for verifying sea attack
	 * 
	 * @param unitList
	 *            - List of units
	 * @return true - has a non-transport unit
	 */
	public static boolean ListContainsOtherThanTransports(final List<Unit> unitList)
	{
		final Iterator<Unit> unitIter = unitList.iterator();
		boolean hasNonTransport = false;
		while (unitIter.hasNext() && !hasNonTransport)
		{
			final Unit unit = unitIter.next();
			hasNonTransport = Matches.UnitIsNotTransport.match(unit);
		}
		return hasNonTransport;
	}
	
	/**
	 * Verifies an entire set of moves
	 * 1st: Repair routes that are invalid
	 * 2nd: Remove transport only attacks
	 * 
	 * @param moveUnits
	 * @param moveRoutes
	 * @param data
	 * @param player
	 */
	public static void verifyMoves(final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final GameData data, final PlayerID player)
	{
		final List<Unit> alreadyMoved = new ArrayList<Unit>();
		final Iterator<Collection<Unit>> moveIter = moveUnits.iterator();
		final Iterator<Route> routeIter = moveRoutes.iterator();
		final HashMap<Territory, Collection<Unit>> attackMap = new HashMap<Territory, Collection<Unit>>();
		final HashMap<Integer, Territory> routeMap = new HashMap<Integer, Territory>(); // used to track the routes of a set of units
		final HashMap<Integer, Route> insertRoutes = new HashMap<Integer, Route>();
		int listCount = 0;
		while (moveIter.hasNext() && routeIter.hasNext())
		{
			listCount++;
			final Collection<Unit> attackUnit = moveIter.next();
			final Route attackRoute = routeIter.next();
			if (attackRoute == null || attackRoute.getEnd() == null)
			{
				moveIter.remove();
				routeIter.remove();
				continue;
			}
			final Route newRoute = repairRoute(attackUnit, attackRoute, data, player);
			if (newRoute != null)
			{
				routeIter.remove();
				insertRoutes.put(listCount, newRoute);
			}
		}
		if (insertRoutes.size() > 0)
		{
			final Set<Integer> placeValues = insertRoutes.keySet();
			for (final Integer thisone : placeValues)
			{
				final Route thisRoute = insertRoutes.get(thisone);
				moveRoutes.add(thisone, thisRoute);
			}
		}
		// generate attackMap
		final Iterator<Collection<Unit>> moveIter2 = moveUnits.iterator();
		final Iterator<Route> routeIter2 = moveRoutes.iterator();
		Integer routeCounter = 0;
		while (moveIter2.hasNext() && routeIter2.hasNext())
		{
			routeCounter++;
			final Collection<Unit> currentUnits = new ArrayList<Unit>();
			final Collection<Unit> theseUnits = moveIter2.next();
			final Route thisRoute = routeIter2.next();
			final Territory target = thisRoute.getEnd();
			if (attackMap.containsKey(target))
			{
				final Collection<Unit> addUnits = attackMap.get(target);
				if (!addUnits.isEmpty())
					currentUnits.addAll(addUnits);
			}
			currentUnits.addAll(theseUnits);
			attackMap.put(target, currentUnits);
			routeMap.put(routeCounter, target);
		}
		// has a collection of all units moving to a given target
		// is this a good move??
		// Check a transport only attack
		final Set<Territory> targetTerrs = attackMap.keySet();
		for (final Territory targetTerr : targetTerrs)
		{
			final List<Unit> enemyUnits = targetTerr.getUnits().getMatches(Matches.enemyUnit(player, data));
			final Collection<Unit> ourUnits = attackMap.get(targetTerr);
			final boolean enemyUnitsExist = enemyUnits.size() > 0;
			if (enemyUnitsExist && targetTerr.isWater())
			{
				final Iterator<Unit> unitIter = ourUnits.iterator();
				boolean nonTransport = false;
				while (unitIter.hasNext() && !nonTransport)
				{
					final Unit thisUnit = unitIter.next();
					if (alreadyMoved.contains(thisUnit))
					{
						continue;
					}
					if (Matches.UnitIsNotTransport.match(thisUnit))
						nonTransport = true;
				}
				if (!nonTransport || ourUnits.isEmpty()) // move of transports into an attack
				{
					int routeCounter2 = 0;
					final List<Integer> deleteValues = new ArrayList<Integer>();
					for (final Route xRoute : moveRoutes)
					{
						if (xRoute.getEnd() == targetTerr)
							deleteValues.add(routeCounter2);
						routeCounter2++;
					}
					for (final int delOne : deleteValues)
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
	 * 
	 * @param moveUnits
	 * @param moveRoute
	 * @return - new Route which allows for movement or null if Move is valid
	 */
	public static Route repairRoute(final Collection<Unit> moveUnits, final Route moveRoute, final GameData data, final PlayerID player)
	{
		final boolean canMove = MoveValidator.hasEnoughMovement(moveUnits, moveRoute);
		if (!canMove)
		{
			final Route newRoute = new Route();
			final Iterator<Territory> routeIter = moveRoute.iterator();
			newRoute.setStart(routeIter.next());
			final boolean routeDone = false;
			while (routeIter.hasNext() && !routeDone)
			{
				final Territory nextTerr = routeIter.next();
				final Route oldRoute = newRoute;
				newRoute.add(nextTerr);
				if (MoveValidator.hasEnoughMovement(moveUnits, newRoute))
				{
					if (!MoveValidator.noEnemyUnitsOnPathMiddleSteps(newRoute, player, data))
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
	 * 
	 * @param checkTerr
	 * @return true if water exists, false if it doesn't
	 */
	public static boolean isWaterAt(final Territory checkTerr, final GameData data)
	{
		final boolean Water = Matches.territoryHasWaterNeighbor(data).match(checkTerr);
		return Water;
	}
	
	/**
	 * Map a list of units
	 * 
	 * @param units
	 * @return
	 */
	public static IntegerMap<UnitType> convertListToMap(final Collection<Unit> units)
	{
		final IntegerMap<UnitType> ourList = new IntegerMap<UnitType>();
		for (final Unit u : units)
		{
			final UnitType uT = u.getType();
			ourList.put(uT, 0);
		}
		final Set<UnitType> ourTypeList = ourList.keySet();
		for (final UnitType u2 : ourTypeList)
		{
			int count = 0;
			for (final Unit u3 : units)
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
	 * @param attacker
	 *            - IntegerMap of number of attacking units (returns adjusted #s)
	 * @param defender
	 *            - IntegerMap of number of defending units
	 * @return - true if Attacker probably wins
	 */
	public static boolean quickBattleEstimator(final IntegerMap<UnitType> attacker, final IntegerMap<UnitType> defender, final PlayerID aPlayer, final PlayerID dPlayer, final boolean sea,
				final boolean subRestricted)
	{
		try
		{
			return quickBattleEstimatorInternal(attacker, defender, aPlayer, dPlayer, sea, subRestricted);
		} catch (final StackOverflowError e)
		{
			// bug 2968146 NWO 1.7.7 on Hard AI
			e.printStackTrace(System.out);
			return false;
		}
	}
	
	private static boolean quickBattleEstimatorInternal(final IntegerMap<UnitType> attacker, final IntegerMap<UnitType> defender, final PlayerID aPlayer, final PlayerID dPlayer, final boolean sea,
				final boolean subRestricted)
	{
		int totAttack = 0, totDefend = 0, deadA = 0, deadD = 0, deadModA = 0, deadModD = 0, countInf = 0, countArt = 0, planeAttack = 0, subDefend = 0;
		boolean planesOnly = true;
		boolean destroyerPresent = false;
		boolean subsOnly = true;
		final Set<UnitType> attackingUnits = attacker.keySet();
		final Set<UnitType> defendingUnits = defender.keySet();
		for (final UnitType aUnit : attackingUnits)
		{
			final UnitAttachment ua = UnitAttachment.get(aUnit);
			totAttack += ua.getAttackRolls(aPlayer) * ua.getAttack(aPlayer) * attacker.getInt(aUnit);
			countInf += Matches.UnitTypeIsInfantry.match(aUnit) ? 1 : 0;
			countArt += Matches.UnitTypeIsArtillery.match(aUnit) ? 1 : 0;
			if (Matches.UnitTypeIsNotAir.match(aUnit))
				planesOnly = false;
			else
				planeAttack = ua.getAttackRolls(aPlayer) * ua.getAttack(aPlayer) * attacker.getInt(aUnit);
			if (Matches.UnitTypeIsDestroyer.match(aUnit))
				destroyerPresent = true;
		}
		totAttack += Math.min(countInf, countArt);
		deadD = totAttack / 6;
		deadModA = totAttack % 6;
		for (final UnitType dUnit : defendingUnits)
		{
			final UnitAttachment ua = UnitAttachment.get(dUnit);
			totDefend += ua.getDefenseRolls(dPlayer) * ua.getDefense(dPlayer) * defender.getInt(dUnit);
			if (Matches.UnitTypeIsSub.match(dUnit) && planesOnly)
				totDefend -= ua.getDefenseRolls(dPlayer) * ua.getDefense(dPlayer) * defender.getInt(dUnit);
			if (Matches.UnitTypeIsSub.invert().match(dUnit))
				subsOnly = false;
			else
				subDefend += ua.getDefenseRolls(dPlayer) * ua.getDefense(dPlayer) * defender.getInt(dUnit);
		}
		if (subRestricted && subsOnly && !destroyerPresent)
			totAttack -= planeAttack;
		if (planesOnly)
			totDefend -= subDefend;
		deadA = totDefend / 6;
		deadModD = totDefend % 6;
		if (deadD == 0 && deadA == 0 && deadModA <= 2 && deadModD <= 2 && deadModA == deadModD)
		{ // declare it a tie when equal attack/defend and 2 or less
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
			int testD = 0, testA = 0; // give an estimate at prob
			while (testD == 0 && testA == 0 && (deadModD > 0 || deadModA > 0))
			{
				for (int i = 1; i <= 6; i++)
				{
					testD += (deadModD * 100 >= Math.random() * 600) ? 1 : 0;
					testA += (i > 1 && deadModA * 100 >= Math.random() * 600) ? 1 : 0;
				}
			}
			deadA += testD >= 4 ? 1 : 0;
			deadD += testA >= 4 ? 1 : 0;
		}
		final IntegerMap<UnitType> newAttacker = removeUnits(attacker, true, deadA, aPlayer, sea);
		final IntegerMap<UnitType> newDefender = removeUnits(defender, false, deadD, dPlayer, sea);
		if (newAttacker.totalValues() > 0 && newDefender.totalValues() > 0)
			quickBattleEstimatorInternal(newAttacker, newDefender, aPlayer, dPlayer, sea, subRestricted);
		for (final UnitType nA : attackingUnits)
			attacker.put(nA, newAttacker.getInt(nA));
		for (final UnitType nD : defendingUnits)
			defender.put(nD, newDefender.getInt(nD));
		boolean weWin = false;
		for (final UnitType AA : attackingUnits)
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
	 * @param attacking
	 *            - attacking strength or defending
	 * @param sea
	 * @param tFirst
	 * @param allied
	 *            - allied = true - all allied units --> false - owned units only
	 * @return
	 */
	public static float strengthOfTerritory(final GameData data, final Territory thisTerr, final PlayerID player, final boolean attacking, final boolean sea, final boolean tFirst, final boolean allied)
	{
		final List<Unit> theUnits = new ArrayList<Unit>();
		if (allied)
			theUnits.addAll(thisTerr.getUnits().getMatches(Matches.alliedUnit(player, data)));
		else
			theUnits.addAll(thisTerr.getUnits().getMatches(Matches.unitIsOwnedBy(player)));
		final float theStrength = SUtils.strength(theUnits, attacking, sea, tFirst);
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
	public static float verifyPlaneAttack(final GameData data, final List<Collection<Unit>> xMoveUnits, final List<Route> xMoveRoutes, final PlayerID player, final List<Territory> alreadyAttacked)
	{
		final Iterator<Collection<Unit>> xMoveIter = xMoveUnits.iterator();
		final int routeNo = 0;
		// float removeStrength = 0.0F;
		final HashMap<Territory, List<Integer>> badRouteMap = new HashMap<Territory, List<Integer>>();
		final HashMap<Territory, Float> strengthDiffMap = new HashMap<Territory, Float>();
		final List<Integer> emptyList = new ArrayList<Integer>();
		for (final Territory alliedTerr : SUtils.allAlliedTerritories(data, player))
		{
			final float eStrength = SUtils.getStrengthOfPotentialAttackers(alliedTerr, data, player, false, false, alreadyAttacked);
			float ourStrength = SUtils.strengthOfTerritory(data, alliedTerr, player, false, false, false, true);
			if (Matches.territoryIsAlliedAndHasAlliedUnitMatching(data, player, Matches.UnitCanProduceUnits).match(alliedTerr))
				ourStrength += ourStrength * 0.25F;
			if (ourStrength > 3.0F)
				strengthDiffMap.put(alliedTerr, eStrength * 0.85F - ourStrength);
			else if (eStrength > 3.0F)
				strengthDiffMap.put(alliedTerr, (eStrength * 1.25F + 3.0F) - ourStrength); // avoid empty territories
			else if (eStrength < 3.0F)
				strengthDiffMap.put(alliedTerr, -ourStrength - 3.0F);
			else
				strengthDiffMap.put(alliedTerr, eStrength - ourStrength);
			badRouteMap.put(alliedTerr, emptyList);
		}
		while (xMoveIter.hasNext())
		{
			final Collection<Unit> xMoves = xMoveIter.next();
			final Route goRoute = xMoveRoutes.get(routeNo);
			final int routeLength = goRoute.getLength();
			final Territory endTerr = goRoute.getEnd();
			final Iterator<Unit> xMIter = xMoves.iterator();
			while (xMIter.hasNext())
			{
				final Unit plane = xMIter.next();
				boolean safePlane = false;
				if (Matches.UnitIsAir.match(plane))
				{
					int moveAvailable = TripleAUnit.get(plane).getMovementLeft();
					moveAvailable -= routeLength;
					final List<Territory> endNeighbors = new ArrayList<Territory>(data.getMap().getNeighbors(endTerr, moveAvailable));
					final Iterator<Territory> eIter = endNeighbors.iterator();
					while (eIter.hasNext())
					{
						final Territory newTerr = eIter.next();
						if (Matches.TerritoryIsWater.match(newTerr) || Matches.isTerritoryAllied(player, data).invert().match(newTerr))
							eIter.remove();
					}
					SUtils.reorder(endNeighbors, strengthDiffMap, false);
					final Iterator<Territory> eIter2 = endNeighbors.iterator();
					while (eIter.hasNext() && !safePlane)
					{
						final Territory newTerr = eIter2.next();
						if (strengthDiffMap.containsKey(newTerr))
						{
							final float strengthDiff = strengthDiffMap.get(newTerr) - SUtils.uStrength(plane, false, false, false);
							strengthDiffMap.put(newTerr, strengthDiff);
							if (strengthDiff <= 0.0F)
								safePlane = true;
							else
							{
								final List<Integer> RouteNos = badRouteMap.get(newTerr);
								RouteNos.add(routeNo);
								badRouteMap.put(newTerr, RouteNos);
							}
						}
					}
				}
			}
		}
		final List<Territory> badMoveTerrs = new ArrayList<Territory>(badRouteMap.keySet());
		float strengthEliminated = 0.0F;
		for (final Territory checkTerr : badMoveTerrs)
		{
			final float strengthDiff = strengthDiffMap.get(checkTerr);
			if (strengthDiff > 0.0F)
				continue;
			final List<Integer> routeNumber = badRouteMap.get(checkTerr);
			for (final Integer killRoute : routeNumber)
			{
				final Collection<Unit> killUnits = xMoveUnits.get(killRoute);
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
	 * @param units
	 *            - Units and #
	 * @param attacking
	 * @param killNum
	 *            - # of units to kill
	 * @return
	 */
	public static IntegerMap<UnitType> removeUnits(final IntegerMap<UnitType> units, final boolean attacking, int killNum, final PlayerID player, final boolean sea)
	{
		final IntegerMap<UnitType> finalList = new IntegerMap<UnitType>();
		final Set<UnitType> unitList = units.keySet();
		final List<UnitType> orderedUnitList = new ArrayList<UnitType>(unitList);
		for (int i = 0; i < orderedUnitList.size(); i++)
		{
			final UnitType unit1 = orderedUnitList.get(i);
			final boolean isInf1 = Matches.UnitTypeIsInfantry.match(unit1);
			final boolean isArt1 = Matches.UnitTypeIsArtillery.match(unit1);
			final boolean isTank1 = UnitAttachment.get(unit1).getCanBlitz(player);
			if (!sea && Matches.unitTypeCanBombard(player).match(unit1))
			{
				orderedUnitList.remove(i);
				i--;
				continue;
			}
			int ipip = 0;
			final UnitAttachment ua = UnitAttachment.get(unit1);
			if (attacking)
				ipip = ua.getAttack(player);
			else
				ipip = ua.getDefense(player);
			// TODO: we should interleave artillery and infantry when they both have same base attack value
			for (int j = i + 1; j < orderedUnitList.size(); j++)
			{
				final UnitType unit2 = orderedUnitList.get(j);
				final boolean isInf2 = Matches.UnitTypeIsInfantry.match(unit2);
				final boolean isArt2 = Matches.UnitTypeIsArtillery.match(unit2);
				final boolean isTank2 = UnitAttachment.get(unit2).getCanBlitz(player);
				final UnitAttachment ua2 = UnitAttachment.get(unit2);
				int ipip2 = 0;
				if (attacking)
					ipip2 = ua2.getAttack(player);
				else
					ipip2 = ua2.getDefense(player);
				if (ipip > ipip2 || (ipip == ipip2 && (((isInf1 || isArt1) && (!isInf2 || !isArt2)) || (isTank1 && !isInf2 && !isArt2 && !isTank2))))
				{
					final UnitType itemp = orderedUnitList.get(i);
					final UnitType itemp2 = orderedUnitList.get(j);
					// we know that i < j always
					orderedUnitList.remove(i);
					orderedUnitList.remove(j - 1);
					orderedUnitList.add(i, itemp2);
					orderedUnitList.add(j, itemp);
				}
			}
		}
		for (final UnitType unitKill : orderedUnitList)
		{
			final int minusNum = Math.min(units.getInt(unitKill), killNum);
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
	 * @param checkTerr
	 *            - territory to be checked
	 * @param data
	 * @param player
	 * @param attackAdv
	 *            - total advantage the enemy has
	 * @param tFirst
	 *            - can transports be killed before other units
	 * @return
	 */
	public static int shipThreatToTerr(final Territory checkTerr, final GameData data, final PlayerID player, final boolean tFirst)
	{
		final CompositeMatchAnd<Unit> enemyUnit = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data));
		final CompositeMatchAnd<Unit> enemySeaUnit = new CompositeMatchAnd<Unit>(enemyUnit, Matches.UnitIsSea, Matches.UnitIsNotTransport);
		final CompositeMatchAnd<Unit> enemyAirUnit = new CompositeMatchAnd<Unit>(enemyUnit, Matches.UnitIsAir);
		final CompositeMatchAnd<Unit> enemyBBUnit = new CompositeMatchAnd<Unit>(enemyUnit, Matches.UnitIsBB);
		final CompositeMatchAnd<Unit> enemyTransportUnit = new CompositeMatchAnd<Unit>(enemyUnit, Matches.UnitIsTransport);
		final CompositeMatchAnd<Unit> alliedSeaUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsSea, Matches.UnitIsNotTransport);
		final CompositeMatchAnd<Unit> alliedTransport = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsTransport);
		final CompositeMatchAnd<Unit> alliedAirUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitCanLandOnCarrier);
		final CompositeMatchAnd<Unit> alliedBBUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsBB);
		final boolean isWater = isWaterAt(checkTerr, data);
		if (!isWater) // no way to get here
			return -1;
		final Set<Territory> waterNeighbors = data.getMap().getNeighbors(checkTerr, Matches.TerritoryIsWater);
		final Set<Territory> shipNeighbors = data.getMap().getNeighbors(checkTerr, 4);
		int totAttackCount = 0;
		int totTransCount = 0;
		final List<Territory> checkThese = new ArrayList<Territory>();
		final List<Territory> checkThese2 = new ArrayList<Territory>();
		PlayerID ePlayer = null;
		final List<PlayerID> ePlayers = getEnemyPlayers(data, player);
		if (!ePlayers.isEmpty())
			ePlayer = ePlayers.get(0); // doesn't matter which one
		for (final Territory shipTerr : shipNeighbors)
		{
			final List<Unit> allShips = shipTerr.getUnits().getMatches(Matches.UnitIsSea);
			if (allShips.isEmpty())
				continue;
			final int shipDistance = MoveValidator.getLeastMovement(allShips);
			final Iterator<Territory> waterIter = waterNeighbors.iterator();
			while (waterIter.hasNext()) // verify it is in range
			{
				final Territory waterTerr = waterIter.next();
				final Route testERoute = getMaxSeaRoute(data, shipTerr, waterTerr, ePlayer, true, shipDistance);
				final Route testARoute = getMaxSeaRoute(data, shipTerr, waterTerr, player, true, shipDistance);
				if (testERoute != null)
				{
					final int testLength = testERoute.getLength();
					if (shipTerr.isWater() && testLength <= (shipDistance + 1))
					{
						checkThese.add(shipTerr);
					}
				}
				if (testARoute != null)
				{
					final int testLength = testARoute.getLength();
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
		for (final Territory sT : checkThese)
		{
			totAttackCount += sT.getUnits().countMatches(enemySeaUnit) + sT.getUnits().countMatches(enemyAirUnit);
			totAttackCount += sT.getUnits().countMatches(enemyBBUnit);
			totTransCount += sT.getUnits().countMatches(enemyTransportUnit);
		}
		for (final Territory sT : checkThese2)
		{
			totAttackCount -= sT.getUnits().countMatches(alliedSeaUnit) - sT.getUnits().countMatches(alliedAirUnit);
			totAttackCount -= sT.getUnits().countMatches(alliedBBUnit);
			totTransCount -= sT.getUnits().countMatches(alliedTransport);
		}
		if (tFirst)
			totAttackCount += totTransCount / 2; // treat transport as half an attack ship
		return totAttackCount;
	}
	
	/**
	 * Remove any territories which cannot be amphibiously invaded
	 * 
	 * @param territories
	 * @param data
	 */
	public static void removeNonAmphibTerritories(final List<Territory> territories, final GameData data)
	{
		if (territories.isEmpty())
			return;
		final Iterator<Territory> tIter = territories.iterator();
		while (tIter.hasNext())
		{
			final Territory checkTerr = tIter.next();
			if (Matches.territoryHasWaterNeighbor(data).invert().match(checkTerr))
				tIter.remove();
		}
		return;
	}
	
	@SuppressWarnings("unchecked")
	public static void reorder(final List<?> reorder, @SuppressWarnings("rawtypes") final IntegerMap map, final boolean greaterThan)
	{
		if (!map.keySet().containsAll(reorder))
		{
			throw new IllegalArgumentException("Not all of:" + reorder + " in:" + map.keySet());
		}
		Collections.sort(reorder, new Comparator<Object>()
		{
			public int compare(final Object o1, final Object o2)
			{
				// get int returns 0 if no value
				int v1 = map.getInt(o1);
				int v2 = map.getInt(o2);
				if (greaterThan)
				{
					final int t = v1;
					v1 = v2;
					v2 = t;
				}
				if (v1 > v2)
				{
					return 1;
				}
				else if (v1 == v2)
				{
					return 0;
				}
				else
				{
					return -1;
				}
			}
		});
	}
	
	public static void reorder(final List<?> reorder, final Map<?, ? extends Number> map, final boolean greaterThan)
	{
		Collections.sort(reorder, new Comparator<Object>()
		{
			public int compare(final Object o1, final Object o2)
			{
				double v1 = safeGet(map, o1);
				double v2 = safeGet(map, o2);
				if (greaterThan)
				{
					final double t = v1;
					v1 = v2;
					v2 = t;
				}
				if (v1 > v2)
				{
					return 1;
				}
				else if (v1 == v2)
				{
					return 0;
				}
				else
				{
					return -1;
				}
			}
			
			private double safeGet(final Map<?, ? extends Number> map, final Object o1)
			{
				if (!map.containsKey(o1))
				{
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
	public static boolean findPurchaseMix(final IntegerMap<ProductionRule> bestAttack, final IntegerMap<ProductionRule> bestDefense, final IntegerMap<ProductionRule> bestTransport,
				final IntegerMap<ProductionRule> bestMaxUnits, final IntegerMap<ProductionRule> bestMobileAttack, final List<ProductionRule> rules, final int totPU, final int maxUnits,
				final GameData data, final PlayerID player, final int fighters)
	{
		// Resource key = data.getResourceList().getResource(Constants.PUS);
		final IntegerMap<String> parameters = new IntegerMap<String>();
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
		parameters.put("maxUnits", maxUnits); // never changed
		parameters.put("maxCost", totPU); // never changed
		parameters.put("infantry", 0);
		parameters.put("nonInfantry", 0);
		final HashMap<ProductionRule, Boolean> infMap = new HashMap<ProductionRule, Boolean>();
		final HashMap<ProductionRule, Boolean> nonInfMap = new HashMap<ProductionRule, Boolean>();
		final HashMap<ProductionRule, Boolean> supportableInfMap = new HashMap<ProductionRule, Boolean>();
		final Iterator<ProductionRule> prodIter = rules.iterator();
		final HashMap<ProductionRule, Boolean> transportMap = new HashMap<ProductionRule, Boolean>();
		// int minCost = 10000;
		// ProductionRule minCostRule = null;
		while (prodIter.hasNext())
		{
			final ProductionRule rule = prodIter.next();
			bestAttack.put(rule, 0); // initialize with 0
			bestDefense.put(rule, 0);
			bestMaxUnits.put(rule, 0);
			bestTransport.put(rule, 0);
			final UnitType x = (UnitType) rule.getResults().keySet().iterator().next();
			supportableInfMap.put(rule, UnitAttachment.get(x).getArtillerySupportable());
			transportMap.put(rule, Matches.UnitTypeCanBeTransported.match(x));
			infMap.put(rule, Matches.UnitTypeIsInfantry.match(x));
			nonInfMap.put(rule, Matches.UnitTypeCanBeTransported.match(x) && Matches.UnitTypeIsInfantry.invert().match(x) && Matches.UnitTypeIsAAforAnything.invert().match(x));
		}
		final int countNum = 1;
		final int goodLoop = purchaseLoop(parameters, countNum, bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack, transportMap, infMap, nonInfMap, supportableInfMap, data,
					player, fighters);
		if (goodLoop > 0 && bestAttack.size() > 0 && bestDefense.size() > 0)
			return true;
		else
			return false;
	}
	
	/**
	 * Recursive routine to determine the bestAttack and bestDefense set of purchase
	 * Expects bestAttack to already be filled with the rules
	 * 
	 * @param parameters
	 *            - set of parameters to be used (8 of them)
	 * @param ruleNum
	 *            - which rule should the routine use
	 * @param bestAttack
	 *            - list of the rules and the number to be purchased (optimized for attack)
	 * @param bestDefense
	 *            - list of the rules and the number to be purchased (optimized for defense)
	 * @param bestTransport
	 *            - list of the rules and the number to be purchased (optimized for transporting)
	 * @param bestMaxUnits
	 *            - list of the rules and the number to be purchased (optimized for attack and max units)
	 * @param bestTransport
	 *            - list of the rules and the number to be purchased (optimized for transport)
	 * @return - integer which is 1 if bestAttack has changed, 2 if bestDefense has changed, 3 if both have changed
	 */
	public static int purchaseLoop(final IntegerMap<String> parameters, final int ruleNum, final IntegerMap<ProductionRule> bestAttack, final IntegerMap<ProductionRule> bestDefense,
				final IntegerMap<ProductionRule> bestTransport, final IntegerMap<ProductionRule> bestMaxUnits, final IntegerMap<ProductionRule> bestMobileAttack,
				final HashMap<ProductionRule, Boolean> transportMap, final HashMap<ProductionRule, Boolean> infMap, final HashMap<ProductionRule, Boolean> nonInfMap,
				final HashMap<ProductionRule, Boolean> supportableInfMap, final GameData data, final PlayerID player, final int fighters)
	{
		final long start = System.currentTimeMillis();
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
		final Resource key = data.getResourceList().getResource(Constants.PUS);
		final Set<ProductionRule> ruleCheck = bestAttack.keySet();
		final Iterator<ProductionRule> ruleIter = ruleCheck.iterator();
		int counter = 1;
		ProductionRule rule = null;
		while (counter <= ruleNum && ruleIter.hasNext())
		{
			rule = ruleIter.next();
			counter++;
		}
		if (rule == null)
			return 0;
		Integer totAttack = parameters.getInt("attack");
		Integer totDefense = parameters.getInt("defense");
		Integer totCost = parameters.getInt("totcost");
		Integer totMovement = parameters.getInt("totMovement");
		final Integer maxCost = parameters.getInt("maxCost");
		final Integer maxUnits = parameters.getInt("maxUnits");
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
		final UnitType x = (UnitType) rule.getResults().keySet().iterator().next();
		final UnitAttachment u = UnitAttachment.get(x);
		final boolean thisIsSupportableInf = supportableInfMap.get(rule);
		final boolean thisIsInf = infMap.get(rule);
		final boolean thisIsNonInf = nonInfMap.get(rule);
		final boolean thisIsArt = u.getArtillery();
		final int uMovement = u.getMovement(player);
		int uAttack = u.getAttack(player);
		int uDefense = u.getDefense(player);
		final int aRolls = u.getAttackRolls(player);
		final int cost = rule.getCosts().getInt(key);
		// Discourage buying submarines, since the AI has no clue how to use them (veqryn)
		final boolean thisIsSub = u.getIsSub();
		if (thisIsSub && uAttack >= 1)
			uAttack--;
		else if (thisIsSub && uDefense >= 1)
			uDefense--;
		// Encourage buying balanced units. Added by veqryn, to decrease the rate at which the AI buys walls, fortresses, and mortars, among other specialty units that should not be bought often if at all.
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
		// TODO: stop it from buying zero movement units under all circumstances. Also, lessen the number of artillery type units bought slightly. And lessen sub purchases, or eliminate entirely. (veqryn)
		// TODO: some transport ships have large capacity, others have a small capacity and are made for fighting. Make sure if the AI is buying transports, it chooses high capacity transports even if more expensive and less att/def than normal ships
		int fightersremaining = fighters;
		int usableMaxUnits = maxUnits;
		if (usableMaxUnits * ruleCheck.size() > 1000 && Math.random() <= 0.50)
			usableMaxUnits = usableMaxUnits / 2;
		for (int i = 0; i <= (usableMaxUnits - totUnits); i++)
		{
			if (i > 0) // allow 0 so that this unit might be skipped...due to low value...consider special capabilities later
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
				// give bonus of 1 hit per 2 units and if fighters are on the capital, a bonus for carrier equal to fighter attack or defense
				int carrierLoad = Math.min(u.getCarrierCapacity(), fightersremaining);
				if (carrierLoad < 0)
					carrierLoad = 0;
				int bonusAttack = (u.getIsTwoHit() ? uAttack : 0) + (uAttack > 0 && (i % 2) == 0 ? 1 : 0) + carrierLoad * 3;
				if (thisIsArt && i <= supportableInfCount)
					bonusAttack++; // add one bonus for each artillery purchased with supportable infantry
				final int bonusDefense = (u.getIsTwoHit() ? uDefense : 0) + (uDefense > 0 && (i % 2) == 0 ? 1 : 0) + (carrierLoad * 4);
				fightersremaining -= carrierLoad;
				totUnits++;
				totAttack += uAttack * aRolls + bonusAttack;
				totDefense += uDefense * aRolls + bonusDefense;
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
				parametersChanged = purchaseLoop(parameters, counter, bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack, transportMap, infMap, nonInfMap, supportableInfMap, data,
							player, fighters);
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
				if (System.currentTimeMillis() - start > PURCHASE_LOOP_MAX_TIME_MILLIS)
				{
					break;
				}
			}
			if (totCost == 0)
				continue;
			// parameters changed: 001: attack, 010: defense, 100: maxUnits, 1000: transport, 10000: mobileAttack
			if (parametersChanged > 0) // change forced by another rule
			{
				if ((parametersChanged - 3) % 4 == 0)
				{
					bestAttack.put(rule, i);
					bestDefense.put(rule, i);
					thisParametersChanged = 3;
					parametersChanged -= 3;
				}
				else if ((parametersChanged - 1) % 4 == 0)
				{
					bestAttack.put(rule, i);
					if (thisParametersChanged % 2 == 0)
						thisParametersChanged += 1;
					parametersChanged -= 1;
				}
				else if ((parametersChanged - 2) % 4 == 0)
				{
					bestDefense.put(rule, i);
					if ((thisParametersChanged + 2) % 4 != 0 && (thisParametersChanged + 1) % 4 != 0)
						thisParametersChanged += 2;
					parametersChanged -= 2;
				}
				if ((parametersChanged > 0) && (parametersChanged - 4) % 8 == 0)
				{
					bestMaxUnits.put(rule, i);
					if (thisParametersChanged == 0 || (thisParametersChanged - 4) % 8 != 0)
						thisParametersChanged += 4;
					parametersChanged -= 4;
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
				final Iterator<ProductionRule> changeIter = ruleCheck.iterator();
				ProductionRule changeThis = null;
				int countThis = 1;
				while (changeIter.hasNext()) // have to clear the rules below this rule
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
				final Iterator<ProductionRule> changeIter = ruleCheck.iterator();
				ProductionRule changeThis = null;
				int countThis = 1;
				while (changeIter.hasNext()) // have to clear the rules below this rule
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
				final Iterator<ProductionRule> changeIter = ruleCheck.iterator();
				ProductionRule changeThis = null;
				int countThis = 1;
				while (changeIter.hasNext()) // have to clear the rules below this rule
				{
					changeThis = changeIter.next();
					if (countThis >= counter)
						bestMaxUnits.put(changeThis, 0);
					countThis++;
				}
			}
			if (totAttack > maxTransAttack && (infCount <= nonInfCount + 1 && infCount >= nonInfCount - 1))
			{
				maxTransAttack = totAttack;
				maxTransCost = totCost;
				parameters.put("maxTransAttack", totAttack);
				parameters.put("maxTransCost", maxTransCost);
				bestTransport.put(rule, i);
				if ((thisParametersChanged + 8) % 16 != 0)
					thisParametersChanged += 8;
				final Iterator<ProductionRule> changeIter = ruleCheck.iterator();
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
			if ((totAttack >= maxMobileAttack && (totMovement > maxMovement)) || (totAttack > maxMobileAttack && (totMovement >= maxMovement)))
			{
				maxMobileAttack = totAttack;
				maxMovement = totMovement;
				parameters.put("maxMobileAttack", maxMobileAttack);
				parameters.put("maxMovement", maxMovement);
				bestMobileAttack.put(rule, i);
				if (thisParametersChanged < 16)
					thisParametersChanged += 16;
				final Iterator<ProductionRule> changeIter = ruleCheck.iterator();
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
			*/
		}
		return thisParametersChanged;
	}
	
	public static List<PlayerID> getAlliedPlayers(final GameData data, final PlayerID player)
	{
		final Collection<PlayerID> playerList = data.getPlayerList().getPlayers();
		final List<PlayerID> aPlayers = new ArrayList<PlayerID>(playerList);
		final List<PlayerID> ePlayers = getEnemyPlayers(data, player);
		aPlayers.removeAll(ePlayers);
		return aPlayers;
	}
	
	/**
	 * use TUV for a set of allies and their enemies to determine if TUV superiority indicates that a player can be more
	 * aggressive in an attack.
	 * 
	 * @param data
	 * @param player
	 * @param aggressiveFactor
	 *            - float which will set how much more TUV is needed to allow aggressive
	 * @return
	 */
	public static boolean determineAggressiveAttack(final GameData data, final PlayerID player, final float aggressiveFactor)
	{
		final int alliedTUV = getAlliedEnemyTUV(data, player, true);
		final int enemyTUV = getAlliedEnemyTUV(data, player, false);
		return (alliedTUV * 100) > (enemyTUV * 100 * aggressiveFactor);
	}
	
	/**
	 * Determine TUV for allies/enemies
	 * 
	 * @param data
	 * @param player
	 * @param allied
	 *            - boolean indicating for which set to gather TUV
	 * @return
	 */
	public static int getAlliedEnemyTUV(final GameData data, final PlayerID player, final boolean allied)
	{
		final IntegerMap<PlayerID> unitMap = getPlayerTUV(data);
		int TUV = 0;
		if (allied)
		{
			final List<PlayerID> aPlayers = getAlliedPlayers(data, player);
			for (final PlayerID aP : aPlayers)
				TUV += unitMap.getInt(aP);
		}
		else
		{
			final List<PlayerID> ePlayers = getEnemyPlayers(data, player);
			for (final PlayerID eP : ePlayers)
				TUV += unitMap.getInt(eP);
		}
		return TUV;
	}
	
	public static Route getMaxSeaRoute(final GameData data, final Territory start, final Territory destination, final PlayerID player, final boolean attacking, final int maxDistance)
	{
		// note this does not care if subs are submerged or not
		// should it? does submerging affect movement of enemies?
		if (start == null || destination == null || !start.isWater() || !destination.isWater())
		{
			return null;
		}
		final CompositeMatch<Unit> ignore = new CompositeMatchAnd<Unit>(Matches.UnitIsInfrastructure.invert(), Matches.alliedUnit(player, data).invert());
		final CompositeMatch<Unit> sub = new CompositeMatchAnd<Unit>(Matches.UnitIsSub.invert());
		final CompositeMatch<Unit> transport = new CompositeMatchAnd<Unit>(Matches.UnitIsTransport.invert(), Matches.UnitIsLand.invert());
		final CompositeMatch<Unit> unitCond = ignore;
		if (Properties.getIgnoreTransportInMovement(data))
			unitCond.add(transport);
		if (Properties.getIgnoreSubInMovement(data))
			unitCond.add(sub);
		final CompositeMatch<Territory> routeCond = new CompositeMatchAnd<Territory>(Matches.territoryHasUnitsThatMatch(unitCond).invert(), Matches.TerritoryIsWater);
		CompositeMatch<Territory> routeCondition;
		if (attacking)
		{
			routeCondition = new CompositeMatchOr<Territory>(Matches.territoryIs(destination), routeCond);
		}
		else
			routeCondition = routeCond;
		Route r = data.getMap().getRoute(start, destination, routeCondition);
		if (r == null || r.getEnd() == null)
			return null;
		// cheating because can't do stepwise calculation with canals
		// shouldn't be a huge problem
		// if we fail due to canal, then don't go near any enemy canals
		if (MoveValidator.validateCanal(r, null, player, data) != null)
			r = data.getMap().getRoute(start, destination, new CompositeMatchAnd<Territory>(routeCondition, Matches.territoryHasNonAllowedCanal(player, null, data).invert()));
		if (r == null || r.getEnd() == null)
			return null;
		final int rDist = r.getLength();
		Route route2 = new Route();
		if (rDist <= maxDistance)
			route2 = r;
		else
		{
			route2.setStart(start);
			for (int i = 1; i <= maxDistance; i++)
				route2.add(r.getTerritories().get(i));
		}
		return route2;
		/*
		
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
		
		*/
	}
	
	/**
	 * Returns the players current pus available
	 * 
	 * @param data
	 * @param player
	 * @return
	 */
	public static int getLeftToSpend(final GameData data, final PlayerID player)
	{
		final Resource pus = data.getResourceList().getResource(Constants.PUS);
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
	public static boolean territoryHasThreatenedAlliedFactoryNeighbor(final GameData data, final Territory eTerr, final PlayerID player)
	{
		if (Matches.territoryHasAlliedNeighborWithAlliedUnitMatching(data, player, Matches.UnitCanProduceUnits).invert().match(eTerr))
			return false;
		final Set<Territory> aNeighbors = data.getMap().getNeighbors(eTerr);
		final List<Territory> factTerr = new ArrayList<Territory>();
		for (final Territory checkTerr : aNeighbors)
		{
			if (Matches.territoryIsAlliedAndHasAlliedUnitMatching(data, player, Matches.UnitCanProduceUnits).match(checkTerr))
				factTerr.add(checkTerr);
		}
		boolean isThreatened = false;
		for (final Territory factory : factTerr)
		{
			float eStrength = SUtils.getStrengthOfPotentialAttackers(factory, data, player, false, true, null);
			eStrength += eStrength * 1.15F + (eStrength > 2.0F ? 3.0F : 0.0F);
			float myStrength = SUtils.strength(factory.getUnits().getUnits(), false, false, false);
			if (eStrength > myStrength)
			{
				final Set<Territory> factNeighbors = data.getMap().getNeighbors(factory, Matches.isTerritoryAllied(player, data));
				float addStrength = 0.0F;
				for (final Territory fNTerr : factNeighbors)
				{
					addStrength += SUtils.strengthOfTerritory(data, fNTerr, player, false, false, false, true);
				}
				myStrength += addStrength * 0.50F;
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
	 * @param waterBased
	 *            - attack is Water Based - Remove all terr with no avail water
	 * @param nonCombat
	 *            - if nonCombat, emphasize threatened factories over their neighbors
	 * @return HashMap ranking of Territories
	 */
	public static HashMap<Territory, Float> rankTerritories(final GameData data, final List<Territory> ourFriendlyTerr, final List<Territory> ourEnemyTerr, final List<Territory> ignoreTerr,
				final PlayerID player, final boolean tFirst, final boolean waterBased, final boolean nonCombat)
	{
		long last, now, start;
		last = System.currentTimeMillis();
		start = last;
		final HashMap<Territory, Float> landRankMap = new HashMap<Territory, Float>();
		final HashMap<Territory, Float> landStrengthMap = new HashMap<Territory, Float>();
		final CompositeMatch<Territory> noEnemyOrWater = new CompositeMatchAnd<Territory>(Matches.TerritoryIsNotImpassableToLandUnits(player, data), Matches.isTerritoryAllied(player, data));
		final CompositeMatch<Territory> enemyAndNoWater = new CompositeMatchAnd<Territory>(Matches.TerritoryIsNotImpassableToLandUnits(player, data),
					Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data));
		// TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();
		final List<PlayerID> ePlayers = getEnemyPlayers(data, player);
		final PlayerID ePlayer = ePlayers.get(0);
		final List<Territory> enemyCapitals = SUtils.getEnemyCapitals(data, player);
		final Territory myCapital = TerritoryAttachment.getCapital(player, data);
		int minDist = 1000;
		final int playerPUs = getLeftToSpend(data, player);
		final Iterator<Territory> eCapsIter = enemyCapitals.iterator();
		while (eCapsIter.hasNext())
		{
			final Territory eCap = eCapsIter.next();
			if (Matches.isTerritoryFriendly(player, data).match(eCap) && Matches.territoryHasAlliedUnits(player, data).match(eCap) && !Matches.territoryHasEnemyLandNeighbor(data, player).match(eCap))
			{
				eCapsIter.remove();
				continue;
			}
			final int dist = data.getMap().getDistance(myCapital, eCap);
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
		final List<Territory> alliedFactories = SUtils.getEnemyCapitals(data, ePlayer);
		final Iterator<Territory> aFIter = alliedFactories.iterator();
		while (aFIter.hasNext())
		{
			final Territory aFTerr = aFIter.next();
			final float aFPotential = SUtils.getStrengthOfPotentialAttackers(aFTerr, data, player, tFirst, true, null);
			final float alliedStrength = SUtils.strengthOfTerritory(data, aFTerr, player, false, false, tFirst, true);
			if (aFPotential < alliedStrength * 0.75F || aFPotential < 1.0F || !Matches.TerritoryIsPassableAndNotRestricted(player, data).match(aFTerr)
						|| (Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data).match(aFTerr) && Matches.territoryHasEnemyLandNeighbor(data, player).match(aFTerr)))
				aFIter.remove();
		}
		final List<Territory> aFNeighbors = new ArrayList<Territory>();
		for (final Territory aF : alliedFactories)
		{
			aFNeighbors.addAll(data.getMap().getNeighbors(aF, Matches.isTerritoryAllied(player, data)));
		}
		for (final Territory eTerr : data.getMap().getTerritories())
		{
			if (eTerr.isWater() || Matches.TerritoryIsImpassable.match(eTerr) || !Matches.TerritoryIsPassableAndNotRestricted(player, data).match(eTerr))
				continue;
			final float alliedPotential = getStrengthOfPotentialAttackers(eTerr, data, ePlayer, tFirst, true, null);
			final float rankStrength = getStrengthOfPotentialAttackers(eTerr, data, player, tFirst, true, ignoreTerr);
			final float productionValue = TerritoryAttachment.get(eTerr).getProduction();
			float eTerrValue = 0.0F;
			final boolean island = !SUtils.doesLandExistAt(eTerr, data, false);
			eTerrValue += Matches.TerritoryIsVictoryCity.match(eTerr) ? 2.0F : 0.0F;
			final boolean lRCap = hasLandRouteToEnemyOwnedCapitol(eTerr, player, data);
			eTerrValue += lRCap ? 16.0F : 0.0F; // 16 might be too much, consider changing to 8
			if (lRCap && (!Matches.territoryIsEnemyNonNeutralAndHasEnemyUnitMatching(data, player, Matches.UnitCanProduceUnits).match(eTerr)
						&& !Matches.territoryIsAlliedAndHasAlliedUnitMatching(data, player, Matches.UnitCanProduceUnits).match(eTerr)))
			{
				final Route eCapRoute = findNearest(eTerr, Matches.territoryIsEnemyNonNeutralAndHasEnemyUnitMatching(data, player, Matches.UnitCanProduceUnits),
							Matches.TerritoryIsNotImpassableToLandUnits(player, data), data);
				if (eCapRoute != null)
					eTerrValue = Math.max(eTerrValue - 8, eTerrValue - (eCapRoute.getLength() - 1)); // 8 might be too much, consider changing to 4
			}
			eTerrValue += Matches.territoryHasEnemyNonNeutralNeighborWithEnemyUnitMatching(data, player, Matches.UnitCanProduceUnits).match(eTerr) ? 3.0F : 0.0F;
			int eMinDist = 1000;
			for (final Territory eTerrCap : enemyCapitals)
			{
				final int eDist = data.getMap().getDistance(eTerr, eTerrCap, Matches.TerritoryIsNotImpassable);
				eMinDist = Math.min(eMinDist, eDist);
			}
			eTerrValue -= eMinDist - 1;
			// eTerrValue += (eMinDist < minDist - 1) ? 4.0F : 0.0F; //bonus for general closeness to enemy Capital
			if (Matches.TerritoryIsLand.match(eTerr) && Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data).match(eTerr))
			{
				ourEnemyTerr.add(eTerr);
				eTerrValue += productionValue * 2;
				final float eTerrStrength = strength(eTerr.getUnits().getMatches(Matches.enemyUnit(player, data)), false, false, tFirst);
				eTerrValue += alliedPotential > (rankStrength + eTerrStrength) ? productionValue : 0.0F;
				if (island)
					eTerrValue += 5.0F;
				eTerrValue += eTerr.getUnits().countMatches(Matches.UnitIsAir) * 2; // bonus for killing air units
				eTerrValue += Matches.territoryIsEnemyNonNeutralAndHasEnemyUnitMatching(data, player, Matches.UnitCanProduceUnits).match(eTerr) ? 4.0F : 0.0F;
				eTerrValue += Matches.territoryHasAlliedNeighborWithAlliedUnitMatching(data, player, Matches.UnitCanProduceUnits).match(eTerr) ? 8.0F : 0.0F;
				// eTerrValue += (data.getMap().getNeighbors(eTerr, Matches.territoryHasAlliedFactory(data, player)).size() > 0 ? 3.0F : 0.0F);
				eTerrValue += Matches.territoryHasEnemyLandNeighbor(data, player).invert().match(eTerr) ? productionValue + 1 : 0.0F;
				/*				if (data.getSequence().getRound() == 1)
								{
									eTerrValue += SUtils.doesLandExistAt(eTerr, data) ? 0.0F : 50.0F;
								}
				*/
				final float netStrength = (eTerrStrength - alliedPotential + 0.5F * rankStrength);
				landStrengthMap.put(eTerr, netStrength);
				landRankMap.put(eTerr, eTerrValue + netStrength * 0.25F);
			}
			else if (Matches.isTerritoryAllied(player, data).match(eTerr) && Matches.TerritoryIsNotNeutralButCouldBeWater.match(eTerr))
			{
				final boolean hasENeighbors = Matches.territoryHasEnemyLandNeighbor(data, player).match(eTerr);
				final Route testERoute = findNearest(eTerr, enemyAndNoWater, noEnemyOrWater, data);
				if (island)
					eTerrValue += -5.0F;
				eTerrValue += (hasENeighbors ? 2.0F : -2.0F);
				eTerrValue += (aFNeighbors.contains(eTerr)) ? 8.0F : 0.0F;
				eTerrValue += (testERoute == null ? -20.0F : Math.max(-10.0F, -(testERoute.getLength() - 2))); // -20 and -10 might be too much, consider changing to -8 and -4
				eTerrValue += (testERoute != null ? productionValue : 0.0F);
				final float aTerrStrength = strength(eTerr.getUnits().getMatches(Matches.alliedUnit(player, data)), false, false, tFirst);
				// bonus for allied factory and allied factory with enemy neighbor
				final boolean hasAlliedFactory = Matches.territoryIsAlliedAndHasAlliedUnitMatching(data, player, Matches.UnitCanProduceUnits).match(eTerr);
				if (hasAlliedFactory)
				{
					eTerrValue += 4.0F + (hasENeighbors && rankStrength > 5.0F ? 3.0F : 0.0F);
					alliedFactories.add(eTerr);
				}
				final float netStrength = rankStrength - aTerrStrength - 0.5F * alliedPotential;
				landStrengthMap.put(eTerr, netStrength);
				landRankMap.put(eTerr, eTerrValue + netStrength * 0.50F);
				if ((netStrength > -15.0F && rankStrength > 2.0F) || hasENeighbors || testERoute != null)
					ourFriendlyTerr.add(eTerr);
			}
			else if (Matches.TerritoryIsNeutralButNotWater.match(eTerr))
			{
				if (Matches.TerritoryIsNotImpassable.match(eTerr) && (Matches.isTerritoryFreeNeutral(data).match(eTerr) || Properties.getNeutralCharge(data) <= playerPUs))
				{
					eTerrValue += -100.0F; // Make sure most neutral territories have lower priorities than enemy territories.
					final boolean hasENeighbors = Matches.territoryHasEnemyLandNeighbor(data, player).match(eTerr);
					final Route testERoute = findNearest(eTerr, enemyAndNoWater, noEnemyOrWater, data);
					eTerrValue += (hasENeighbors ? 1.0F : -1.0F);
					eTerrValue += (testERoute == null ? -1.0F : -(testERoute.getLength() - 1));
					eTerrValue += productionValue > 0 ? productionValue : -5.0F;
					final float netStrength = rankStrength - 0.5F * alliedPotential;
					landStrengthMap.put(eTerr, netStrength);
					landRankMap.put(eTerr, eTerrValue + netStrength * 0.50F);
				}
			}
			// Currently there are a lot of territories that don't make it into the list, especially if the politics involves neutral nations. we should add them here.
		}
		if (nonCombat)
		{
			final CompositeMatch<Territory> alliedLandTerr = new CompositeMatchAnd<Territory>(Matches.isTerritoryAllied(player, data), Matches.TerritoryIsLand, Matches.TerritoryIsNotImpassable);
			// Set<Territory> terrList = landRankMap.keySet();
			for (final Territory terr1 : alliedFactories)
			{
				if (!landRankMap.containsKey(terr1))
					continue;
				float landRank = landRankMap.get(terr1);
				if (Matches.territoryHasEnemyLandNeighbor(data, player).match(terr1))
				{
					for (final Territory neighbor : data.getMap().getNeighbors(terr1, alliedLandTerr))
					{
						if (!landRankMap.containsKey(neighbor)) // Match when adding ters to rank map is more strict than this match (alliedLandTer)
							continue;
						final float thisRank = landRankMap.get(neighbor);
						landRank = Math.max(landRank, thisRank);
					}
					landRank += 1.0F;
					landRankMap.put(terr1, landRank);
				}
			}
		}
		now = System.currentTimeMillis();
		s_logger.finest("Time Takenrank " + (now - start));
		return landRankMap;
	}
	
	public static float twoAwayStrengthNotCounted(final GameData data, final PlayerID player, final Territory eTerr)
	{
		final List<Territory> blitzers = SUtils.possibleBlitzTerritories(eTerr, data, player);
		float nonBlitzStrength = 0.0F; // blitzStrength has already been included in the rankStrength...add in 2 away
		final List<Territory> checkTerrs = new ArrayList<Territory>();
		for (final Territory bTerr : blitzers)
		{
			final List<Territory> bTNeighbors = new ArrayList<Territory>(data.getMap().getNeighbors(bTerr));
			bTNeighbors.removeAll(blitzers);
			bTNeighbors.remove(eTerr);
			for (final Territory bT : bTNeighbors)
			{
				if (!checkTerrs.contains(bT))
					checkTerrs.add(bT);
			}
		}
		final CompositeMatch<Territory> landPassable = new CompositeMatchAnd<Territory>(Matches.TerritoryIsLand, Matches.TerritoryIsNotImpassable);
		final Iterator<Territory> bIter = checkTerrs.iterator();
		while (bIter.hasNext())
		{
			final Territory newBTerr = bIter.next();
			final Set<Territory> newBNeighbors = data.getMap().getNeighbors(newBTerr, landPassable);
			boolean blitzCounted = false;
			final Iterator<Territory> newBNIter = newBNeighbors.iterator();
			while (!blitzCounted && newBNIter.hasNext())
			{
				final Territory bCheck = newBNIter.next();
				if (blitzers.contains(bCheck) && bCheck.getUnits().getMatches(Matches.alliedUnit(player, data)).isEmpty())
					blitzCounted = true;
			}
			if (!blitzCounted && Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data).match(newBTerr))
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
	public static HashMap<Territory, Float> rankAmphibReinforcementTerritories(final GameData data, final List<Territory> ignoreTerr, final PlayerID player, final boolean tFirst)
	{
		final HashMap<Territory, Float> landRankMap = new HashMap<Territory, Float>();
		final HashMap<Territory, Float> landStrengthMap = new HashMap<Territory, Float>();
		final CompositeMatch<Territory> noEnemyOrWater = new CompositeMatchAnd<Territory>(Matches.TerritoryIsNotImpassableToLandUnits(player, data), Matches.isTerritoryAllied(player, data));
		final CompositeMatch<Territory> enemyAndNoWater = new CompositeMatchAnd<Territory>(Matches.TerritoryIsNotImpassableToLandUnits(player, data),
					Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data));
		final List<PlayerID> ePlayers = getEnemyPlayers(data, player);
		final PlayerID ePlayer = ePlayers.get(0);
		final List<Territory> enemyCapitals = SUtils.getEnemyCapitals(data, player);
		final Territory myCapital = TerritoryAttachment.getCapital(player, data);
		int minDist = 1000;
		// int playerIPCs = getLeftToSpend(data, player);
		Territory targetCap = null;
		for (final Territory eCapTerr : enemyCapitals)
		{
			final int dist = data.getMap().getDistance(myCapital, eCapTerr);
			if (minDist > dist)
			{
				minDist = dist;
				targetCap = eCapTerr;
			}
		}
		final CompositeMatch<Territory> continentTerr = new CompositeMatchAnd<Territory>(Matches.isTerritoryAllied(player, data), Matches.territoryHasValidLandRouteTo(data, targetCap));
		/**
		 * Send units because:
		 * 1) Production Value
		 * 2) Victory City
		 * 3) Has a Land Route to Enemy Capitol
		 * 4) Has enemy factory
		 * 5) Is close to enemy
		 * 6) Is close to a threatened allied capital
		 */
		final List<Territory> alliedFactories = new ArrayList<Territory>();
		for (final Territory aTerr : data.getMap().getTerritories())
		{
			if (!continentTerr.match(aTerr) || Matches.isTerritoryEnemy(player, data).match(aTerr) || Matches.TerritoryIsImpassable.match(aTerr)
						|| Matches.territoryHasWaterNeighbor(data).invert().match(aTerr))
				continue;
			final float alliedPotential = getStrengthOfPotentialAttackers(aTerr, data, ePlayer, tFirst, true, null);
			final float localStrength = SUtils.strength(aTerr.getUnits().getUnits(), false, false, tFirst);
			final float rankStrength = getStrengthOfPotentialAttackers(aTerr, data, player, tFirst, true, ignoreTerr);
			final float productionValue = TerritoryAttachment.get(aTerr).getProduction();
			float aTerrValue = 0.0F;
			aTerrValue += Matches.TerritoryIsVictoryCity.match(aTerr) ? 2.0F : 0.0F;
			aTerrValue += Matches.territoryHasEnemyNonNeutralNeighborWithEnemyUnitMatching(data, player, Matches.UnitCanProduceUnits).match(aTerr) ? 2.0F : 0.0F;
			aTerrValue -= data.getMap().getDistance(aTerr, targetCap, Matches.TerritoryIsNotImpassable) - 1;
			final Territory capTerr = aTerr;
			/*if (Matches.territoryHasAlliedFactoryNeighbor(data, player).match(aTerr))
			{
				Set<Territory> neighbors = data.getMap().getNeighbors(aTerr, Matches.territoryHasAlliedFactory(data, player));
				if (!neighbors.isEmpty())
					capTerr = neighbors.iterator().next();
			}*/
			if (Matches.territoryHasAlliedNeighborWithAlliedUnitMatching(data, player, Matches.UnitCanProduceUnits).match(capTerr)) // does this need reinforcing?
			{
				final float addCapValue = aTerr.equals(capTerr) ? 5.0F : 0.0F;
				if (rankStrength > alliedPotential + localStrength)
					aTerrValue += 10.0F + addCapValue;
				else
				{
					final float xValue = SUtils.twoAwayStrengthNotCounted(data, player, aTerr);
					if (rankStrength + xValue > (alliedPotential + localStrength) * 1.05F)
						aTerrValue += 10.0F + addCapValue;
				}
			}
			final boolean hasENeighbors = Matches.territoryHasEnemyLandNeighbor(data, player).match(aTerr);
			final Route testERoute = findNearest(aTerr, enemyAndNoWater, noEnemyOrWater, data);
			aTerrValue += (hasENeighbors ? 1.0F : -1.0F);
			aTerrValue += (hasENeighbors && Matches.territoryHasAlliedNeighborWithAlliedUnitMatching(data, player, Matches.UnitCanProduceUnits).match(aTerr)) ? 5.0F : 0.0F;
			aTerrValue += (testERoute == null ? -1.0F : -(testERoute.getLength() - 1));
			aTerrValue += (testERoute != null ? productionValue : 0.0F);
			final float aTerrStrength = strength(aTerr.getUnits().getMatches(Matches.alliedUnit(player, data)), false, false, tFirst);
			// bonus for allied factory and allied factory with enemy neighbor
			final boolean hasAlliedFactory = Matches.territoryIsAlliedAndHasAlliedUnitMatching(data, player, Matches.UnitCanProduceUnits).match(aTerr);
			if (hasAlliedFactory)
			{
				aTerrValue += 4.0F + (hasENeighbors && rankStrength > 5.0F ? 3.0F : 0.0F);
				alliedFactories.add(aTerr);
			}
			boolean worthTroopDrop = (aTerrStrength + alliedPotential) > (rankStrength - 3.0F) * 0.80F;
			worthTroopDrop = worthTroopDrop && (aTerrStrength + 0.80F * alliedPotential) < 1.25F * (rankStrength + 3.0F);
			aTerrValue += worthTroopDrop ? 5.0F : -2.0F;
			final float netStrength = rankStrength - aTerrStrength - 0.8F * alliedPotential;
			landStrengthMap.put(aTerr, netStrength);
			landRankMap.put(aTerr, aTerrValue);
		}
		return landRankMap;
	}
}
