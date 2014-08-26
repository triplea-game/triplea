package games.strategy.triplea.ai.proAI.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.proAI.ProAI;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.Match;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

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
	
	public Map<Territory, Double> findTerritoryValues(final PlayerID player, final Set<Territory> territories, final List<Territory> territoriesThatCantBeHeld)
	{
		LogUtils.log(Level.FINE, "Determine move value for each territory");
		
		final GameData data = ai.getGameData();
		final List<Territory> allTerritories = data.getMap().getTerritories();
		
		// Matches
		final Match<Territory> canMoveLandTerritoryMatch = new CompositeMatchAnd<Territory>(Matches.territoryDoesNotCostMoneyToEnter(data),
					Matches.TerritoryIsPassableAndNotRestrictedAndOkByRelationships(player, data, true, true, false, false, false));
		final Match<Territory> canMoveSeaTerritoryMatch = new CompositeMatchAnd<Territory>(Matches.territoryDoesNotCostMoneyToEnter(data),
					Matches.TerritoryIsPassableAndNotRestrictedAndOkByRelationships(player, data, false, false, true, false, false));
		final Match<Territory> enemyFactories = new CompositeMatchAnd<Territory>(Matches.isTerritoryEnemy(player, data),
					Matches.territoryHasUnitsThatMatch(new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data), Matches.UnitCanProduceUnits)));
		
		// Get all enemy factories and determine a value for them
		final Map<Territory, Double> enemyCapitalsAndFactoriesMap = new HashMap<Territory, Double>();
		final Set<Territory> enemyCapitalsAndFactories = new HashSet<Territory>();
		enemyCapitalsAndFactories.addAll(Match.getMatches(allTerritories, enemyFactories));
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
		
		// Determine value for each territory I can hold
		final Map<Territory, Double> territoryValueMap = new HashMap<Territory, Double>();
		for (final Territory t : territories)
		{
			if (!territoriesThatCantBeHeld.contains(t) && !t.isWater())
			{
				// Determine value based on enemy factory land distance
				double capitalOrFactoryValue = 0;
				for (final Territory enemyCapitalOrFactory : enemyCapitalsAndFactoriesMap.keySet())
				{
					final int distance = data.getMap().getDistance(t, enemyCapitalOrFactory, canMoveLandTerritoryMatch);
					if (distance > 0)
					{
						capitalOrFactoryValue += (enemyCapitalsAndFactoriesMap.get(enemyCapitalOrFactory) / Math.pow(2, distance));
					}
				}
				
				// Determine value based on nearby territory production
				double nearbyEnemyValue = 0;
				final Set<Territory> nearbyTerritories = data.getMap().getNeighbors(t, 2, canMoveLandTerritoryMatch);
				final Match<Territory> territoryIsEnemyOrCantBeHeld = new CompositeMatchOr<Territory>(Matches.isTerritoryEnemy(player, data), Matches.territoryIsInList(territoriesThatCantBeHeld));
				final List<Territory> nearbyEnemyTerritories = Match.getMatches(nearbyTerritories, territoryIsEnemyOrCantBeHeld);
				for (final Territory nearbyEnemyTerritory : nearbyEnemyTerritories)
				{
					final int distance = data.getMap().getDistance(t, nearbyEnemyTerritory, canMoveLandTerritoryMatch);
					if (distance > 0)
					{
						final int isNeutral = nearbyEnemyTerritory.getOwner().isNull() ? 1 : 0;
						nearbyEnemyValue += TerritoryAttachment.getProduction(nearbyEnemyTerritory) / (isNeutral + 1) / Math.pow(2, distance);
					}
				}
				final double value = capitalOrFactoryValue + nearbyEnemyValue;
				territoryValueMap.put(t, value);
				// moveMap.get(t).setValue(value);
				// LogUtils.log(Level.FINER, "Land value: " + value + " = " + capitalOrFactoryValue + " + " + nearbyEnemyValue + " for " + t.getName());
			}
			else if (!territoriesThatCantBeHeld.contains(t) && t.isWater())
			{
				// Determine value based on enemy factory distance
				double capitalOrFactoryValue = 0;
				for (final Territory enemyCapitalOrFactory : enemyCapitalsAndFactoriesMap.keySet())
				{
					final int distance = data.getMap().getDistance(t, enemyCapitalOrFactory);
					if (distance > 0)
					{
						capitalOrFactoryValue += (enemyCapitalsAndFactoriesMap.get(enemyCapitalOrFactory) / Math.pow(3, distance));
					}
				}
				
				// Determine value based on nearby territory production
				double nearbyEnemyValue = 0;
				final Set<Territory> nearbyTerritories = data.getMap().getNeighbors(t, 3);
				final Match<Territory> territoryIsEnemyOrCantBeHeld = new CompositeMatchOr<Territory>(Matches.isTerritoryEnemy(player, data), Matches.territoryIsInList(territoriesThatCantBeHeld));
				final List<Territory> nearbyEnemyTerritories = Match.getMatches(nearbyTerritories, territoryIsEnemyOrCantBeHeld);
				for (final Territory nearbyEnemyTerritory : nearbyEnemyTerritories)
				{
					final int distance = data.getMap().getDistance_IgnoreEndForCondition(t, nearbyEnemyTerritory, canMoveSeaTerritoryMatch);
					if (distance <= 3)
					{
						final int isNeutral = nearbyEnemyTerritory.getOwner().isNull() ? 1 : 0;
						nearbyEnemyValue += TerritoryAttachment.getProduction(nearbyEnemyTerritory) / (isNeutral + 1) / 2;
					}
				}
				final double value = capitalOrFactoryValue + nearbyEnemyValue;
				territoryValueMap.put(t, value);
				// LogUtils.log(Level.FINER, "Water value: " + value + " = " + capitalOrFactoryValue + " + " + nearbyEnemyValue + " for " + t.getName());
			}
			else
			{
				territoryValueMap.put(t, 0.0);
			}
		}
		return territoryValueMap;
	}
	
}
