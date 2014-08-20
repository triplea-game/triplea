package games.strategy.triplea.ai.proAI.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ai.proAI.ProAI;
import games.strategy.triplea.ai.proAI.ProPurchaseOption;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.List;
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
 * Pro AI purchase utilities.
 * 
 * @author Ron Murhammer
 * @since 2014
 */
public class ProPurchaseUtils
{
	private final ProAI ai;
	
	public ProPurchaseUtils(final ProAI proAI)
	{
		ai = proAI;
	}
	
	public void findPurchaseOptions(final PlayerID player, final List<ProPurchaseOption> landPurchaseOptions, final List<ProPurchaseOption> airPurchaseOptions,
				final List<ProPurchaseOption> seaPurchaseOptions, final List<ProPurchaseOption> factoryPurchaseOptions, final List<ProPurchaseOption> specialPurchaseOptions)
	{
		LogUtils.log(Level.FINE, "Find all purchase options");
		
		final GameData data = ai.getGameData();
		
		final List<ProductionRule> rules = player.getProductionFrontier().getRules();
		for (final ProductionRule rule : rules)
		{
			// Check if rule is for a unit
			final NamedAttachable resourceOrUnit = rule.getResults().keySet().iterator().next();
			if (!(resourceOrUnit instanceof UnitType))
				continue;
			final UnitType unitType = (UnitType) resourceOrUnit;
			
			// Add rule to appropriate purchase option list
			if ((UnitAttachment.get(unitType).getMovement(player) <= 0 && !(UnitAttachment.get(unitType).getCanProduceUnits()))
						|| Matches.UnitTypeHasMaxBuildRestrictions.match(unitType)
						|| Matches.UnitTypeConsumesUnitsOnCreation.match(unitType)
						|| Matches.UnitTypeCanNotMoveDuringCombatMove.match(unitType))
			{
				final ProPurchaseOption purchaseOption = new ProPurchaseOption(rule, unitType, player, data);
				specialPurchaseOptions.add(purchaseOption);
				LogUtils.log(Level.FINER, "Special: " + purchaseOption);
			}
			else if (Matches.UnitTypeCanProduceUnits.match(unitType))
			{
				final ProPurchaseOption purchaseOption = new ProPurchaseOption(rule, unitType, player, data);
				factoryPurchaseOptions.add(purchaseOption);
				LogUtils.log(Level.FINER, "Factory: " + purchaseOption);
			}
			else if (Matches.UnitTypeIsLand.match(unitType))
			{
				final ProPurchaseOption purchaseOption = new ProPurchaseOption(rule, unitType, player, data);
				landPurchaseOptions.add(purchaseOption);
				LogUtils.log(Level.FINER, "Land: " + purchaseOption);
			}
			else if (Matches.UnitTypeIsAir.match(unitType))
			{
				final ProPurchaseOption purchaseOption = new ProPurchaseOption(rule, unitType, player, data);
				airPurchaseOptions.add(purchaseOption);
				LogUtils.log(Level.FINER, "Air: " + purchaseOption);
			}
			else if (Matches.UnitTypeIsSea.match(unitType))
			{
				final ProPurchaseOption purchaseOption = new ProPurchaseOption(rule, unitType, player, data);
				seaPurchaseOptions.add(purchaseOption);
				LogUtils.log(Level.FINER, "Sea: " + purchaseOption);
			}
		}
	}
	
	public List<ProPurchaseOption> findPurchaseOptionsForTerritory(final PlayerID player, final List<ProPurchaseOption> purchaseOptions, final Territory t)
	{
		final List<ProPurchaseOption> result = new ArrayList<ProPurchaseOption>();
		for (final ProPurchaseOption ppo : purchaseOptions)
		{
			if (canTerritoryUsePurchaseOption(player, ppo, t))
			{
				result.add(ppo);
			}
		}
		return result;
	}
	
	public boolean canTerritoryUsePurchaseOption(final PlayerID player, final ProPurchaseOption ppo, final Territory t)
	{
		final GameData data = ai.getGameData();
		if (ppo == null)
			return false;
		final List<Unit> units = ppo.getUnitType().create(ppo.getQuantity(), player, true);
		final boolean result = !Properties.getUnitPlacementRestrictions(data) || Match.someMatch(units, ProMatches.unitWhichRequiresUnitsHasRequiredUnits(player, t));
		return result;
	}
	
}
