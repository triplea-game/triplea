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
package games.strategy.triplea.Dynamix_AI.CommandCenter;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Dynamix_AI.DUtils;
import games.strategy.triplea.Dynamix_AI.Others.PhaseType;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.util.IntegerMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 
 * @author Stephen
 */
public class GlobalCenter
{
	public static boolean IsPaused = false;
	public static final Object IsPaused_Object = new Object();
	public static PhaseType FirstDynamixPhase;
	public static PlayerID FirstDynamixPlayer;
	public static int GameRound;
	private static Resource PUResource;
	
	public static Resource GetPUResource()
	{
		return PUResource;
	}
	
	public static void Initialize(final GameData data)
	{
		// This part just resets all the static variables to their default value so that other code will be able to fill in the real info
		// (For example, the Dynamix_AI class sets the FirstDynamixPhase variable)
		FirstDynamixPhase = PhaseType.Unknown;
		FirstDynamixPlayer = null;
		GameRound = 0;
		PUResource = null;
		CurrentPlayer = null;
		MapTerCount = 0;
		MapTerCountScale = 1.0F;
		CurrentPhaseType = PhaseType.Unknown;
		IsFFAGame = false;
		FastestUnitMovement = 0;
		FastestLandUnitMovement = 0;
		HighestTerProduction = -1;
		PUsAtEndOfLastTurn = 0;
		MergedAndAveragedProductionFronter = null;
		AllMapUnitTypes = null;
		// Now we 'initialize' by filling in some of the values (the rest are filled in somewhere else)
		PUResource = data.getResourceList().getResource(Constants.PUS);
		MapTerCount = data.getMap().getTerritories().size();
		// 75 is considered the 'base' map ter count (For comparison, Great Lakes War has 90)
		MapTerCountScale = (data.getMap().getTerritories().size() / 75.0F);
		IsFFAGame = true;
		for (final String alliance : data.getAllianceTracker().getAlliances()) // TODO: update this for looking into relationships instead of alliances.
		{
			final List<PlayerID> playersInAlliance = DUtils.ToList(data.getAllianceTracker().getPlayersInAlliance(alliance));
			if (playersInAlliance.size() > 1)
			{
				IsFFAGame = false;
				break;
			}
		}
		HighestTerProduction = DUtils.GetHighestTerProduction(data);
		GenerateMergedAndAveragedProductionFrontier(data);
	}
	
	public static PlayerID CurrentPlayer;
	public static int MapTerCount;
	/** Please use this for all hard-coded values. (Multiply the hard-coded value by this float, and the hard-coded value will scale up or down with the maps */
	public static float MapTerCountScale;
	public static PhaseType CurrentPhaseType;
	public static boolean IsFFAGame;
	public static int FastestUnitMovement;
	public static int FastestLandUnitMovement;
	public static int HighestTerProduction;
	public static int PUsAtEndOfLastTurn;
	private static ProductionFrontier MergedAndAveragedProductionFronter;
	public static List<UnitType> AllMapUnitTypes;
	
	/**
	 * Generates a merged and averaged production frontier that can be used to determine TUV of units even when player is neutral or unknown.
	 * This method also sets the global FastestUnitMovement value and the AllMapUnitTypes list.
	 */
	private static void GenerateMergedAndAveragedProductionFrontier(final GameData data)
	{
		MergedAndAveragedProductionFronter = new ProductionFrontier("Merged and averaged global production frontier", data);
		AllMapUnitTypes = new ArrayList<UnitType>();
		final HashMap<UnitType, Integer> purchaseCountsForUnit = new HashMap<UnitType, Integer>();
		final HashMap<UnitType, List<Integer>> differentCosts = new HashMap<UnitType, List<Integer>>();
		for (final PlayerID player : data.getPlayerList().getPlayers())
		{
			if (player.getProductionFrontier() == null)
				continue;
			for (final ProductionRule rule : player.getProductionFrontier())
			{
				final UnitType ut = (UnitType) rule.getResults().keySet().iterator().next();
				final UnitAttachment ua = UnitAttachment.get(ut);
				DUtils.AddObjToListValueForKeyInMap(differentCosts, ut, rule.getCosts().getInt(PUResource));
				purchaseCountsForUnit.put(ut, rule.getResults().keySet().size());
				final int movement = ua.getMovement(player);
				if (movement > FastestUnitMovement)
					FastestUnitMovement = movement;
				if (movement > FastestLandUnitMovement && !ua.getIsSea() && !ua.getIsAir())
					FastestLandUnitMovement = movement;
				AllMapUnitTypes.add(ut);
			}
		}
		for (final UnitType unitType : differentCosts.keySet())
		{
			int totalCosts = 0;
			final List<Integer> costs = differentCosts.get(unitType);
			for (final int cost : costs)
			{
				totalCosts += cost;
			}
			final int averagedCost = (int) ((float) totalCosts / (float) costs.size());
			final IntegerMap<NamedAttachable> results = new IntegerMap<NamedAttachable>();
			results.put(unitType, purchaseCountsForUnit.get(unitType));
			final IntegerMap<Resource> cost = new IntegerMap<Resource>();
			cost.put(PUResource, averagedCost);
			final ProductionRule rule = new ProductionRule("Averaged production rule for unit " + unitType.getName(), data, results, cost);
			MergedAndAveragedProductionFronter.addRule(rule);
		}
	}
	
	public static ProductionFrontier GetMergedAndAveragedProductionFrontier()
	{
		return MergedAndAveragedProductionFronter;
	}
}
