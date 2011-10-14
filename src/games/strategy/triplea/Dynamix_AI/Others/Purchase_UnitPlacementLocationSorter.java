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

package games.strategy.triplea.Dynamix_AI.Others;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Dynamix_AI.DMatches;
import games.strategy.triplea.Dynamix_AI.DSorting;
import games.strategy.triplea.Dynamix_AI.DUtils;
import games.strategy.triplea.Dynamix_AI.Dynamix_AI;
import games.strategy.triplea.Dynamix_AI.CommandCenter.CachedCalculationCenter;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.oddsCalculator.ta.AggregateResults;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * 
 * @author Stephen
 */
public class Purchase_UnitPlacementLocationSorter
{
	public static List<Territory> CalculateAndSortUnitPlacementLocations(Dynamix_AI ai, boolean purchaseForBid, GameData data, PlayerID player)
	{
		List<Territory> result = new ArrayList<Territory>();
		
		List<Territory> possibles = Match.getMatches(data.getMap().getTerritories(), DMatches.territoryCanHaveUnitsPlacedOnIt(data, player));
		HashMap<Territory, Integer> scores = new HashMap<Territory, Integer>();
		List<Territory> ourCaps = DUtils.GetAllOurCaps_ThatWeOwn(data, player);
		List<Territory> ownedCaps = DUtils.GetAllCapsOwnedBy(data, player);
		
		Collections.shuffle(possibles); // Shuffle list so if there are equal-scored ters, they don't always show up in the same order
		for (Territory ter : possibles)
		{
			if (DMatches.territoryIsIsolated(data).match(ter))
				continue; // We never place units on an isolated territory
			if (!DMatches.territoryCanHaveUnitsPlacedOnIt(data, player).match(ter))
				continue;
			
			List<Unit> possibleAttackers = DUtils.GetSPNNEnemyUnitsThatCanReach(data, ter, player, Matches.TerritoryIsLandOrWater);
			AggregateResults results = DUtils.GetBattleResults(possibleAttackers, DUtils.GetTerUnitsAtEndOfTurn(data, player, ter), ter, data, 500, true);
			
			int score = 0;
			// If ter is our cap and our cap has some danger
			if (ourCaps.contains(ter) && results.getAttackerWinPercent() > .1F)
				score += 1000;
			if (ownedCaps.contains(ter))
				score += 100;
			
			score += DUtils.GetValueOfLandTer(ter, data, player);
			
			Territory target = NCM_TargetCalculator.CalculateNCMTargetForTerritory(data, player, ter, ter.getUnits().getUnits(), new ArrayList<NCM_Task>());
			if (target != null)
			{
				Route terToTargetRoute = CachedCalculationCenter.GetPassableLandRoute(data, ter, target);
				if (terToTargetRoute != null)
					score -= terToTargetRoute.getLength() * 5; // We like to place units at factories closer to our ncm target
			}
			
			Territory closestEnemy = DUtils.GetClosestTerMatchingXAndHavingRouteMatchingY(data, ter, DMatches.territoryIsOwnedByNNEnemy(data, player), Matches.TerritoryIsLandOrWater);
			if (closestEnemy != null)
			{
				Route terToClosestEnemyRoute = CachedCalculationCenter.GetPassableLandRoute(data, ter, closestEnemy);
				if (terToClosestEnemyRoute != null)
					score -= terToClosestEnemyRoute.getLength() * 10; // We like to place units at factories closer to the enemy
			}
			
			// If this ter is in danger, but not to much
			if (results.getAttackerWinPercent() > .30F && results.getAttackerWinPercent() < .70F)
				score += (results.getAttackerWinPercent() * 10);
			else if (results.getAttackerWinPercent() > .70F && results.getAttackerWinPercent() < .95F)
				score -= (results.getAttackerWinPercent() * 10);
			else if (results.getAttackerWinPercent() > .95F)
				score -= 10000; // If this ter's gonna get taken over, lower score a ton
				
			if (DMatches.territoryIsOnSmallIsland(data).match(ter))
				score -= 10000; // Atm, never place on islands unless we have to
				
			// We multiply the scores by a random number between 100%-110%, so slightly lower ranked ters get units placed on them sometimes
			int randNum = 100 + new Random().nextInt(10);
			double randomMultiplyAmount = (randNum / 100.0F);
			score = (int) (score * randomMultiplyAmount);
			
			result.add(ter);
			scores.put(ter, score);
		}
		
		result = DSorting.SortListByScores_HashMap_D(result, scores);
		if (purchaseForBid && result.size() > 0)
			return result.subList(0, 1); // Atm, put all bid units on best ter(almost always cap)...
			
		return result;
	}
}
