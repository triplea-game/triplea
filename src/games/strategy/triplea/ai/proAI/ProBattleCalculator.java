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
package games.strategy.triplea.ai.proAI;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.net.GUID;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.BattleCalculator;
import games.strategy.triplea.delegate.BattleDelegate;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.IBattle;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.dataObjects.CasualtyList;
import games.strategy.util.IntegerMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pro AI battle calculator.
 * 
 * @author Ron Murhammer
 * @since 2014
 */
public class ProBattleCalculator
{
	
	public static CasualtyList getDefaultCasualties(final Collection<Unit> targets, final int hits, final PlayerID player, final IntegerMap<UnitType> costs,
				final Collection<TerritoryEffect> territoryEffects, final GameData data, final boolean allowMultipleHitsPerUnit, final GUID battleID)
	{
		final CasualtyList defaultCasualtySelection = new CasualtyList();
		
		// Remove extra hitpoints from units first
		int numSelectedCasualties = 0;
		if (allowMultipleHitsPerUnit)
		{
			for (final Unit unit : targets)
			{
				// Stop if we have already selected as many hits as there are targets
				if (numSelectedCasualties >= hits)
				{
					return defaultCasualtySelection;
				}
				final UnitAttachment ua = UnitAttachment.get(unit.getType());
				final int extraHP = Math.min((hits - numSelectedCasualties), (ua.getHitPoints() - (1 + unit.getHits())));
				for (int i = 0; i < extraHP; i++)
				{
					numSelectedCasualties++;
					defaultCasualtySelection.addToDamaged(unit);
				}
			}
		}
		
		// Get battle data
		final BattleDelegate battleDelegate = DelegateFinder.battleDelegate(data);
		final IBattle battle = battleDelegate.getBattleTracker().getPendingBattle(battleID);
		final boolean defending = battle.getDefender().equals(player);
		final Territory t = battle.getTerritory();
		final boolean isAmphibiousBattle = battle.isAmphibious();
		final Collection<Unit> amphibiousLandAttackers = battle.getAmphibiousLandAttackers();
		PlayerID enemyPlayer = battle.getDefender();
		Collection<Unit> enemyUnits = battle.getDefendingUnits();
		if (defending)
		{
			enemyPlayer = battle.getAttacker();
			enemyUnits = battle.getAttackingUnits();
		}
		
		// Select optimal units to kill
		final Collection<Unit> units = new ArrayList<Unit>(targets);
		for (int i = numSelectedCasualties; i < hits; i++)
		{
			final Unit u = getOptimalCasualty(units, enemyUnits, defending, player, enemyPlayer, data, t, territoryEffects, isAmphibiousBattle, amphibiousLandAttackers, costs);
			defaultCasualtySelection.addToKilled(u);
			units.remove(u);
		}
		
		return defaultCasualtySelection;
	}
	
	private static Unit getOptimalCasualty(final Collection<Unit> targets, final Collection<Unit> enemyUnits, final boolean defending, final PlayerID player, final PlayerID enemyPlayer,
				final GameData data, final Territory t, final Collection<TerritoryEffect> territoryEffects, final boolean isAmphibiousBattle, final Collection<Unit> amphibiousLandAttackers,
				final IntegerMap<UnitType> costs)
	{
		// Loop through all target units to find the best unit to take as casualty
		Unit maxUnit = null;
		int maxPowerDifference = Integer.MIN_VALUE;
		int maxCost = Integer.MIN_VALUE;
		int maxReversePower = Integer.MIN_VALUE;
		final Set<UnitType> unitTypes = new HashSet<UnitType>();
		for (final Unit u : targets)
		{
			// Only check each unit type once
			if (unitTypes.contains(u.getType()))
				continue;
			
			// Find my power without current unit
			unitTypes.add(u.getType());
			final List<Unit> units = new ArrayList<Unit>(targets);
			units.remove(u);
			final List<Unit> enemyUnitList = new ArrayList<Unit>(enemyUnits);
			final int power = DiceRoll.getTotalPowerAndRolls(DiceRoll.getUnitPowerAndRollsForNormalBattles(units, units, enemyUnitList, defending, false,
						player, data, t, territoryEffects, isAmphibiousBattle, amphibiousLandAttackers), data).getFirst();
			
			// Find enemy power without current unit (need to consider this since supports can decrease enemy attack/defense)
			final int enemyPower = DiceRoll.getTotalPowerAndRolls(DiceRoll.getUnitPowerAndRollsForNormalBattles(enemyUnitList, enemyUnitList, units, !defending, false,
						enemyPlayer, data, t, territoryEffects, isAmphibiousBattle, amphibiousLandAttackers), data).getFirst();
			
			// Check if unit has higher power
			final int powerDifference = power - enemyPower;
			if (powerDifference > maxPowerDifference)
			{
				maxUnit = u;
				maxPowerDifference = powerDifference;
				maxCost = costs.getInt(u.getType());
				maxReversePower = BattleCalculator.getUnitPowerForSorting(u, !defending, data, territoryEffects);
			}
			else if (powerDifference == maxPowerDifference)
			{
				// Check if unit has higher cost
				if (costs.getInt(u.getType()) < maxCost)
				{
					maxUnit = u;
					maxCost = costs.getInt(u.getType());
					maxReversePower = BattleCalculator.getUnitPowerForSorting(u, !defending, data, territoryEffects);
				}
				else if (costs.getInt(u.getType()) == maxCost)
				{
					// Check if unit has higher reverse power
					if (BattleCalculator.getUnitPowerForSorting(u, !defending, data, territoryEffects) < maxReversePower)
					{
						maxUnit = u;
						maxReversePower = BattleCalculator.getUnitPowerForSorting(u, !defending, data, territoryEffects);
					}
				}
			}
		}
		
		// Check for best transport
		if (Matches.transportIsTransporting().match(maxUnit))
		{
			for (final Unit u : targets)
			{
				if (u.getType().equals(maxUnit.getType()) && !Matches.transportIsTransporting().match(u))
				{
					maxUnit = u;
					break;
				}
			}
		}
		
		return maxUnit;
	}
	
}
