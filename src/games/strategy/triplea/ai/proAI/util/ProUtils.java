package games.strategy.triplea.ai.proAI.util;

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
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ai.proAI.ProAI;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.ui.AbstractUIContext;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pro AI utilities (these are very general and maybe should be moved into delegate or engine).
 * 
 * @author Ron Murhammer
 * @since 2014
 */
public class ProUtils
{
	private final ProAI ai;
	
	public ProUtils(final ProAI ai)
	{
		this.ai = ai;
	}
	
	public Map<Unit, Territory> createUnitTerritoryMap(final PlayerID player)
	{
		final List<Territory> allTerritories = ai.getGameData().getMap().getTerritories();
		final List<Territory> myUnitTerritories = Match.getMatches(allTerritories, Matches.territoryHasUnitsOwnedBy(player));
		final Map<Unit, Territory> unitTerritoryMap = new HashMap<Unit, Territory>();
		for (final Territory t : myUnitTerritories)
		{
			final List<Unit> myUnits = t.getUnits().getMatches(Matches.unitIsOwnedBy(player));
			for (final Unit u : myUnits)
				unitTerritoryMap.put(u, t);
		}
		return unitTerritoryMap;
	}
	
	public List<PlayerID> getEnemyPlayers(final PlayerID player)
	{
		final GameData data = ai.getGameData();
		final List<PlayerID> enemyPlayers = new ArrayList<PlayerID>();
		for (final PlayerID players : data.getPlayerList().getPlayers())
		{
			if (!data.getRelationshipTracker().isAllied(player, players))
				enemyPlayers.add(players);
		}
		return enemyPlayers;
	}
	
	public double getPlayerProduction(final PlayerID player, final GameData data)
	{
		int rVal = 0;
		for (final Territory place : data.getMap().getTerritories())
		{
			/* Match will Check if terr is a Land Convoy Route and check ownership of neighboring Sea Zone, or if contested */
			if (place.getOwner().equals(player) && Matches.territoryCanCollectIncomeFrom(player, data).match(place))
			{
				rVal += TerritoryAttachment.getProduction(place);
			}
		}
		rVal *= Properties.getPU_Multiplier(data);
		return rVal;
	}
	
	public List<Territory> getLiveEnemyCapitals(final GameData data, final PlayerID player)
	{ // generate a list of all enemy capitals
		final List<Territory> enemyCapitals = new ArrayList<Territory>();
		final List<PlayerID> ePlayers = getEnemyPlayers(player);
		for (final PlayerID otherPlayer : ePlayers)
		{
			enemyCapitals.addAll(TerritoryAttachment.getAllCurrentlyOwnedCapitals(otherPlayer, data));
		}
		enemyCapitals.retainAll(Match.getMatches(enemyCapitals, Matches.TerritoryIsNotImpassableToLandUnits(player, data)));
		enemyCapitals.retainAll(Match.getMatches(enemyCapitals, Matches.isTerritoryEnemy(player, data)));
		return enemyCapitals;
	}
	
	public int getClosestEnemyLandTerritoryDistance(final GameData data, final PlayerID player, final Territory t)
	{
		final Set<Territory> landTerritories = data.getMap().getNeighbors(t, 9, ProMatches.territoryCanMoveLandUnits(player, data, true));
		final List<Territory> enemyLandTerritories = Match.getMatches(landTerritories, ProMatches.territoryIsEnemyNotNeutralLand(player, data));
		int minDistance = 10;
		for (final Territory enemyLandTerritory : enemyLandTerritories)
		{
			final int distance = data.getMap().getDistance(t, enemyLandTerritory, ProMatches.territoryCanMoveLandUnits(player, data, true));
			if (distance < minDistance)
				minDistance = distance;
		}
		if (minDistance < 10)
			return minDistance;
		else
			return -1;
	}
	
	/**
	 * Pause the game to allow the human player to see what is going on.
	 */
	public void pause()
	{
		try
		{
			Thread.sleep(AbstractUIContext.getAIPauseDuration());
		} catch (final InterruptedException e)
		{
			e.printStackTrace();
		} catch (final Exception e)
		{
			e.printStackTrace();
		}
	}
	
}
