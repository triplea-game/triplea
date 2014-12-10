package games.strategy.triplea.ai.proAI;

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
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.ai.proAI.util.LogUtils;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class ProPurchaseOptionMap
{
	private final List<ProPurchaseOption> landFodderOptions;
	private final List<ProPurchaseOption> landAttackOptions;
	private final List<ProPurchaseOption> landDefenseOptions;
	private final List<ProPurchaseOption> airOptions;
	private final List<ProPurchaseOption> seaDefenseOptions;
	private final List<ProPurchaseOption> seaTransportOptions;
	private final List<ProPurchaseOption> seaCarrierOptions;
	private final List<ProPurchaseOption> seaSubOptions;
	private final List<ProPurchaseOption> aaOptions;
	private final List<ProPurchaseOption> factoryOptions;
	private final List<ProPurchaseOption> specialOptions;
	
	public ProPurchaseOptionMap(final PlayerID player, final GameData data)
	{
		LogUtils.log(Level.FINE, "Purchase Options");
		
		// Initialize lists
		landFodderOptions = new ArrayList<ProPurchaseOption>();
		landAttackOptions = new ArrayList<ProPurchaseOption>();
		landDefenseOptions = new ArrayList<ProPurchaseOption>();
		airOptions = new ArrayList<ProPurchaseOption>();
		seaDefenseOptions = new ArrayList<ProPurchaseOption>();
		seaTransportOptions = new ArrayList<ProPurchaseOption>();
		seaCarrierOptions = new ArrayList<ProPurchaseOption>();
		seaSubOptions = new ArrayList<ProPurchaseOption>();
		aaOptions = new ArrayList<ProPurchaseOption>();
		factoryOptions = new ArrayList<ProPurchaseOption>();
		specialOptions = new ArrayList<ProPurchaseOption>();
		
		// Add each production rule to appropriate list(s)
		final List<ProductionRule> rules = player.getProductionFrontier().getRules();
		final List<ProPurchaseOption> landOptions = new ArrayList<ProPurchaseOption>();
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
						|| UnitAttachment.get(unitType).getIsSuicide())
			{
				final ProPurchaseOption purchaseOption = new ProPurchaseOption(rule, unitType, player, data);
				specialOptions.add(purchaseOption);
				LogUtils.log(Level.FINER, "Special: " + purchaseOption);
			}
			else if (Matches.UnitTypeCanProduceUnits.match(unitType) && Matches.UnitTypeIsInfrastructure.match(unitType))
			{
				final ProPurchaseOption purchaseOption = new ProPurchaseOption(rule, unitType, player, data);
				factoryOptions.add(purchaseOption);
				LogUtils.log(Level.FINER, "Factory: " + purchaseOption);
			}
			else if (Matches.UnitTypeIsAAforBombingThisUnitOnly.match(unitType))
			{
				final ProPurchaseOption purchaseOption = new ProPurchaseOption(rule, unitType, player, data);
				aaOptions.add(purchaseOption);
				LogUtils.log(Level.FINER, "AA: " + purchaseOption);
			}
			else if (Matches.UnitTypeIsLand.match(unitType))
			{
				final ProPurchaseOption purchaseOption = new ProPurchaseOption(rule, unitType, player, data);
				landOptions.add(purchaseOption);
				LogUtils.log(Level.FINER, "Land: " + purchaseOption);
			}
			else if (Matches.UnitTypeIsAir.match(unitType))
			{
				final ProPurchaseOption purchaseOption = new ProPurchaseOption(rule, unitType, player, data);
				airOptions.add(purchaseOption);
				LogUtils.log(Level.FINER, "Air: " + purchaseOption);
			}
			else if (Matches.UnitTypeIsSea.match(unitType))
			{
				final ProPurchaseOption purchaseOption = new ProPurchaseOption(rule, unitType, player, data);
				if (!purchaseOption.isSub())
					seaDefenseOptions.add(purchaseOption);
				if (purchaseOption.isTransport())
					seaTransportOptions.add(purchaseOption);
				if (purchaseOption.isCarrier())
					seaCarrierOptions.add(purchaseOption);
				if (purchaseOption.isSub())
					seaSubOptions.add(purchaseOption);
				LogUtils.log(Level.FINER, "Sea: " + purchaseOption);
			}
		}
		
		// Find min cost per hitpoints
		double minCostPerHitPoint = Double.POSITIVE_INFINITY;
		for (final ProPurchaseOption ppo : landOptions)
		{
			if (ppo.getCostPerHitPoint() < minCostPerHitPoint)
				minCostPerHitPoint = ppo.getCostPerHitPoint();
		}
		
		// Divide land options into sub categories
		for (final ProPurchaseOption ppo : landOptions)
		{
			if (ppo.getCostPerHitPoint() <= minCostPerHitPoint)
				landFodderOptions.add(ppo);
			if (ppo.getAttack() >= ppo.getDefense() || ppo.getMovement() > 1)
				landAttackOptions.add(ppo);
			if (ppo.getDefense() >= ppo.getAttack())
				landDefenseOptions.add(ppo);
		}
		if (landAttackOptions.isEmpty())
			landAttackOptions.addAll(landDefenseOptions);
		if (landDefenseOptions.isEmpty())
			landDefenseOptions.addAll(landAttackOptions);
		
		// Print categorized options
		LogUtils.log(Level.FINE, "Purchase Categories");
		logOptions(landFodderOptions, "Land Fodder Options: ");
		logOptions(landAttackOptions, "Land Attack Options: ");
		logOptions(landDefenseOptions, "Land Defense Options: ");
		logOptions(airOptions, "Air Options: ");
		logOptions(seaDefenseOptions, "Sea Defense Options: ");
		logOptions(seaTransportOptions, "Sea Transport Options: ");
		logOptions(seaCarrierOptions, "Sea Carrier Options: ");
		logOptions(seaSubOptions, "Sea Sub Options: ");
		logOptions(aaOptions, "AA Options: ");
		logOptions(factoryOptions, "Factory Options: ");
		logOptions(specialOptions, "Special Options: ");
	}
	
	public List<ProPurchaseOption> getAllOptions()
	{
		final Set<ProPurchaseOption> allOptions = new HashSet<ProPurchaseOption>();
		allOptions.addAll(getLandOptions());
		allOptions.addAll(airOptions);
		allOptions.addAll(getSeaOptions());
		allOptions.addAll(aaOptions);
		allOptions.addAll(factoryOptions);
		allOptions.addAll(specialOptions);
		return new ArrayList<ProPurchaseOption>(allOptions);
	}
	
	public List<ProPurchaseOption> getLandOptions()
	{
		final Set<ProPurchaseOption> landOptions = new HashSet<ProPurchaseOption>();
		landOptions.addAll(landFodderOptions);
		landOptions.addAll(landAttackOptions);
		landOptions.addAll(landDefenseOptions);
		return new ArrayList<ProPurchaseOption>(landOptions);
	}
	
	public List<ProPurchaseOption> getSeaOptions()
	{
		final Set<ProPurchaseOption> seaOptions = new HashSet<ProPurchaseOption>();
		seaOptions.addAll(seaDefenseOptions);
		seaOptions.addAll(seaTransportOptions);
		seaOptions.addAll(seaCarrierOptions);
		seaOptions.addAll(seaSubOptions);
		return new ArrayList<ProPurchaseOption>(seaOptions);
	}
	
	public List<ProPurchaseOption> getLandFodderOptions()
	{
		return landFodderOptions;
	}
	
	public List<ProPurchaseOption> getLandAttackOptions()
	{
		return landAttackOptions;
	}
	
	public List<ProPurchaseOption> getLandDefenseOptions()
	{
		return landDefenseOptions;
	}
	
	public List<ProPurchaseOption> getAirOptions()
	{
		return airOptions;
	}
	
	public List<ProPurchaseOption> getSeaDefenseOptions()
	{
		return seaDefenseOptions;
	}
	
	public List<ProPurchaseOption> getSeaTransportOptions()
	{
		return seaTransportOptions;
	}
	
	public List<ProPurchaseOption> getSeaCarrierOptions()
	{
		return seaCarrierOptions;
	}
	
	public List<ProPurchaseOption> getSeaSubOptions()
	{
		return seaSubOptions;
	}
	
	public List<ProPurchaseOption> getAAOptions()
	{
		return aaOptions;
	}
	
	public List<ProPurchaseOption> getFactoryOptions()
	{
		return factoryOptions;
	}
	
	public List<ProPurchaseOption> getSpecialOptions()
	{
		return specialOptions;
	}
	
	private void logOptions(final List<ProPurchaseOption> purchaseOptions, final String name)
	{
		final StringBuilder sb = new StringBuilder(name);
		for (final ProPurchaseOption ppo : purchaseOptions)
		{
			sb.append(ppo.getUnitType().getName());
			sb.append(", ");
		}
		sb.delete(sb.length() - 2, sb.length());
		LogUtils.log(Level.FINER, sb.toString());
	}
	
}
