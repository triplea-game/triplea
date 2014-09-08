package games.strategy.triplea.ai.proAI.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ai.proAI.ProAI;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.Match;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

/**
 * Pro AI battle utilities.
 * 
 * @author Ron Murhammer
 * @since 2014
 */
public class ProTerritoryValueUtils
{
	private final ProAI ai;
	private final ProUtils utils;
	
	public ProTerritoryValueUtils(final ProAI ai, final ProUtils utils)
	{
		this.ai = ai;
		this.utils = utils;
	}
	
	public Map<Territory, Double> findTerritoryValues(final PlayerID player, final List<Territory> territoriesThatCantBeHeld)
	{
		final GameData data = ai.getGameData();
		final List<Territory> allTerritories = data.getMap().getTerritories();
		
		// Get all enemy factories and determine a value for them
		final Map<Territory, Double> enemyCapitalsAndFactoriesMap = new HashMap<Territory, Double>();
		final Set<Territory> enemyCapitalsAndFactories = new HashSet<Territory>();
		enemyCapitalsAndFactories.addAll(Match.getMatches(allTerritories, ProMatches.territoryHasInfraFactoryAndIsEnemyLand(player, data)));
		enemyCapitalsAndFactories.addAll(utils.getLiveEnemyCapitals(data, player));
		for (final Territory t : enemyCapitalsAndFactories)
		{
			// Get factory production if factory
			int factoryProduction = 0;
			if (Matches.territoryHasUnitsThatMatch(Matches.UnitCanProduceUnits).match(t))
				factoryProduction = TerritoryAttachment.getProduction(t);
			
			// Get player production if capital
			double playerProduction = 0;
			final TerritoryAttachment ta = TerritoryAttachment.get(t);
			if (ta != null && ta.isCapital())
			{
				playerProduction = utils.getPlayerProduction(t.getOwner(), data);
			}
			
			// Check if neutral
			final int isNeutral = t.getOwner().isNull() ? 1 : 0;
			
			// Calculate value
			final double value = factoryProduction * 4 / (1 + isNeutral) + playerProduction;
			enemyCapitalsAndFactoriesMap.put(t, value);
		}
		
		// Determine value for land territories
		final Map<Territory, Double> territoryValueMap = new HashMap<Territory, Double>();
		for (final Territory t : allTerritories)
		{
			if (!territoriesThatCantBeHeld.contains(t) && !t.isWater())
			{
				// Determine value based on enemy factory land distance
				double capitalOrFactoryValue = 0;
				for (final Territory enemyCapitalOrFactory : enemyCapitalsAndFactoriesMap.keySet())
				{
					final int distance = data.getMap().getDistance(t, enemyCapitalOrFactory, ProMatches.territoryCanMoveLandUnits(player, data, true));
					if (distance > 0)
					{
						capitalOrFactoryValue += (enemyCapitalsAndFactoriesMap.get(enemyCapitalOrFactory) / Math.pow(2, distance));
					}
				}
				
				// Determine value based on nearby territory production
				double nearbyEnemyValue = 0;
				final Set<Territory> nearbyTerritories = data.getMap().getNeighbors(t, 2, ProMatches.territoryCanMoveLandUnits(player, data, true));
				final List<Territory> nearbyEnemyTerritories = Match.getMatches(nearbyTerritories, ProMatches.territoryIsEnemyOrCantBeHeld(player, data, territoriesThatCantBeHeld));
				for (final Territory nearbyEnemyTerritory : nearbyEnemyTerritories)
				{
					final int distance = data.getMap().getDistance(t, nearbyEnemyTerritory, ProMatches.territoryCanMoveLandUnits(player, data, true));
					if (distance > 0)
					{
						final int isNeutral = nearbyEnemyTerritory.getOwner().isNull() ? 1 : 0;
						nearbyEnemyValue += TerritoryAttachment.getProduction(nearbyEnemyTerritory) / (isNeutral + 1) / Math.pow(2, distance);
					}
				}
				final double value = capitalOrFactoryValue + nearbyEnemyValue;
				territoryValueMap.put(t, value);
			}
			else if (!t.isWater())
			{
				territoryValueMap.put(t, 0.0);
			}
		}
		
		// Determine value for water territories
		for (final Territory t : allTerritories)
		{
			if (!territoriesThatCantBeHeld.contains(t) && t.isWater())
			{
				// Determine value based on enemy factory distance
				double capitalOrFactoryValue = 0;
				for (final Territory enemyCapitalOrFactory : enemyCapitalsAndFactoriesMap.keySet())
				{
					final int distance = data.getMap().getDistance_IgnoreEndForCondition(t, enemyCapitalOrFactory, ProMatches.territoryCanMoveSeaUnits(player, data, false));
					if (distance > 0)
					{
						capitalOrFactoryValue += (enemyCapitalsAndFactoriesMap.get(enemyCapitalOrFactory) / Math.pow(3, distance));
					}
				}
				
				// Determine value based on nearby territory production
				double nearbyLandValue = 0;
				final Set<Territory> nearbyTerritories = data.getMap().getNeighbors(t, 3);
				final List<Territory> nearbyLandTerritories = Match.getMatches(nearbyTerritories, ProMatches.territoryCanMoveLandUnits(player, data, false));
				for (final Territory nearbyLandTerritory : nearbyLandTerritories)
				{
					final int distance = data.getMap().getDistance_IgnoreEndForCondition(t, nearbyLandTerritory, ProMatches.territoryCanMoveSeaUnits(player, data, false));
					if (distance > 0 && distance <= 3)
					{
						final int isNeutral = nearbyLandTerritory.getOwner().isNull() ? 1 : 0;
						if (ProMatches.territoryIsEnemyOrCantBeHeld(player, data, territoriesThatCantBeHeld).match(nearbyLandTerritory))
							nearbyLandValue += (double) TerritoryAttachment.getProduction(nearbyLandTerritory) / (isNeutral + 1);
						nearbyLandValue += territoryValueMap.get(nearbyLandTerritory);
					}
				}
				final double value = capitalOrFactoryValue + nearbyLandValue;
				// LogUtils.log(Level.FINEST, t + ", strategicValue=" + value + ", factoryValue=" + capitalOrFactoryValue + ", nearbyValue=" + nearbyLandValue + ", nearbyTerritories="
				// + nearbyLandTerritories);
				territoryValueMap.put(t, value);
			}
			else if (t.isWater())
			{
				territoryValueMap.put(t, 0.0);
			}
		}
		return territoryValueMap;
	}
	
}
