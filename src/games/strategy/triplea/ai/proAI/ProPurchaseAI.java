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
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ai.proAI.util.LogUtils;
import games.strategy.triplea.ai.proAI.util.ProAttackOptionsUtils;
import games.strategy.triplea.ai.proAI.util.ProBattleUtils;
import games.strategy.triplea.ai.proAI.util.ProMatches;
import games.strategy.triplea.ai.proAI.util.ProMoveUtils;
import games.strategy.triplea.ai.proAI.util.ProTerritoryValueUtils;
import games.strategy.triplea.ai.proAI.util.ProTransportUtils;
import games.strategy.triplea.ai.proAI.util.ProUtils;
import games.strategy.triplea.attatchments.RulesAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.AbstractMoveDelegate;
import games.strategy.triplea.delegate.BattleCalculator;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Pro purchase AI.
 * 
 * @author Ron Murhammer
 * @since 2014
 */
public class ProPurchaseAI
{
	public final static double WIN_PERCENTAGE = 95.0;
	
	// Utilities
	private final ProUtils utils;
	private final ProBattleUtils battleUtils;
	private final ProTransportUtils transportUtils;
	private final ProAttackOptionsUtils attackOptionsUtils;
	private final ProMoveUtils moveUtils;
	private final ProTerritoryValueUtils territoryValueUtils;
	
	// Current map settings
	private boolean areNeutralsPassableByAir;
	
	// Current data
	private GameData data;
	private PlayerID player;
	private Territory myCapital;
	private List<Territory> allTerritories;
	private Map<Unit, Territory> unitTerritoryMap;
	
	// AI data shared across phases
	private Map<Territory, ProPurchaseTerritory> purchaseTerritories;
	
	public ProPurchaseAI(final ProUtils utils, final ProBattleUtils battleUtils, final ProTransportUtils transportUtils, final ProAttackOptionsUtils attackOptionsUtils,
				final ProMoveUtils moveUtils, final ProTerritoryValueUtils territoryValueUtils)
	{
		this.utils = utils;
		this.battleUtils = battleUtils;
		this.transportUtils = transportUtils;
		this.attackOptionsUtils = attackOptionsUtils;
		this.moveUtils = moveUtils;
		this.territoryValueUtils = territoryValueUtils;
		purchaseTerritories = new HashMap<Territory, ProPurchaseTerritory>();
	}
	
	public void bid(final int PUsToSpend, final IPurchaseDelegate purchaseDelegate, final GameData data, final PlayerID player)
	{
		LogUtils.log(Level.FINE, "Starting bid purchase phase");
		
		// Current data at the start of combat move
		this.data = data;
		this.player = player;
		areNeutralsPassableByAir = (Properties.getNeutralFlyoverAllowed(data) && !Properties.getNeutralsImpassable(data));
		myCapital = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
		allTerritories = data.getMap().getTerritories();
	}
	
	public void purchase(final int PUsToSpend, final IPurchaseDelegate purchaseDelegate, final GameData data, final PlayerID player)
	{
		LogUtils.log(Level.FINE, "Starting purchase phase");
		
		// Current data at the start of combat move
		this.data = data;
		this.player = player;
		areNeutralsPassableByAir = (Properties.getNeutralFlyoverAllowed(data) && !Properties.getNeutralsImpassable(data));
		myCapital = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
		allTerritories = data.getMap().getTerritories();
		int PUsRemaining = PUsToSpend;
		
		// Find all purchase options
		final List<ProPurchaseOption> specialPurchaseOptions = new ArrayList<ProPurchaseOption>();
		final List<ProPurchaseOption> factoryPurchaseOptions = new ArrayList<ProPurchaseOption>();
		final List<ProPurchaseOption> landPurchaseOptions = new ArrayList<ProPurchaseOption>();
		final List<ProPurchaseOption> airPurchaseOptions = new ArrayList<ProPurchaseOption>();
		final List<ProPurchaseOption> seaPurchaseOptions = new ArrayList<ProPurchaseOption>();
		findPurchaseOptions(landPurchaseOptions, airPurchaseOptions, seaPurchaseOptions, factoryPurchaseOptions, specialPurchaseOptions);
		
		// Find all purchase territories
		purchaseTerritories = findPurchaseTerritories();
		final Set<Territory> placeTerritories = new HashSet<Territory>();
		for (final Territory t : purchaseTerritories.keySet())
		{
			for (final ProPlaceTerritory ppt : purchaseTerritories.get(t).getCanPlaceTerritories())
			{
				placeTerritories.add(ppt.getTerritory());
			}
		}
		
		// Determine max enemy attack units
		final Map<Territory, ProAttackTerritoryData> enemyAttackMap = new HashMap<Territory, ProAttackTerritoryData>();
		attackOptionsUtils.findMaxEnemyAttackUnits(player, new ArrayList<Territory>(), new ArrayList<Territory>(placeTerritories), enemyAttackMap);
		
		// Prioritize territories that need defended and purchase additional defenders
		findDefendersInPlaceTerritories(purchaseTerritories);
		final List<ProPlaceTerritory> needToDefendTerritories = prioritizeTerritoriesToDefend(purchaseTerritories, enemyAttackMap);
		PUsRemaining = purchaseDefenders(purchaseTerritories, enemyAttackMap, needToDefendTerritories, PUsRemaining, landPurchaseOptions);
		
		// Find strategic value for each territory
		final Map<Territory, Double> territoryValueMap = territoryValueUtils.findTerritoryValues(player, placeTerritories, new ArrayList<Territory>());
		for (final Territory t : purchaseTerritories.keySet())
		{
			for (final ProPlaceTerritory ppt : purchaseTerritories.get(t).getCanPlaceTerritories())
			{
				ppt.setStrategicValue(territoryValueMap.get(ppt.getTerritory()));
			}
		}
		
		// Prioritize land place options and purchase units
		final List<ProPlaceTerritory> prioritizedLandTerritories = prioritizeLandTerritories(purchaseTerritories);
		PUsRemaining = purchaseLandUnits(purchaseTerritories, enemyAttackMap, prioritizedLandTerritories, PUsRemaining, landPurchaseOptions);
		
		// Prioritize sea place options and purchase units
		final List<ProPlaceTerritory> prioritizedSeaTerritories = prioritizeSeaTerritories(purchaseTerritories);
		PUsRemaining = purchaseSeaUnits(purchaseTerritories, enemyAttackMap, prioritizedSeaTerritories, PUsRemaining, seaPurchaseOptions, landPurchaseOptions);
		
		// TODO: Determine whether to purchase new factory
		
		// Try to use any remaining PUs to upgrade land units to planes
		PUsRemaining = spendRemainingPUs(purchaseTerritories, PUsRemaining, landPurchaseOptions, airPurchaseOptions);
		
		// Determine final count of each production rule
		final IntegerMap<ProductionRule> purchaseMap = populateProductionRuleMap(purchaseTerritories, landPurchaseOptions, airPurchaseOptions, seaPurchaseOptions);
		
		// Purchase units
		purchaseDelegate.purchase(purchaseMap);
	}
	
	public void place(final IAbstractPlaceDelegate placeDelegate, final GameData data, final PlayerID player)
	{
		LogUtils.log(Level.FINE, "Starting place phase");
		
		for (final Territory t : purchaseTerritories.keySet())
		{
			for (final ProPlaceTerritory ppt : purchaseTerritories.get(t).getCanPlaceTerritories())
			{
				final Collection<Unit> myUnits = player.getUnits().getUnits();
				final Collection<Unit> unitsToPlace = new ArrayList<Unit>();
				for (final Unit placeUnit : ppt.getPlaceUnits())
				{
					for (final Unit myUnit : myUnits)
					{
						if (myUnit.getUnitType().equals(placeUnit.getUnitType()) && !unitsToPlace.contains(myUnit))
						{
							unitsToPlace.add(myUnit);
							break;
						}
					}
				}
				doPlace(data.getMap().getTerritory(ppt.getTerritory().getName()), unitsToPlace, placeDelegate);
				// doPlace(ppt.getTerritory(), unitsToPlace, placeDelegate);
				LogUtils.log(Level.FINER, ppt.getTerritory() + " placed units: " + unitsToPlace);
			}
		}
	}
	
	private void findPurchaseOptions(final List<ProPurchaseOption> landPurchaseOptions, final List<ProPurchaseOption> airPurchaseOptions,
				final List<ProPurchaseOption> seaPurchaseOptions, final List<ProPurchaseOption> factoryPurchaseOptions, final List<ProPurchaseOption> specialPurchaseOptions)
	{
		LogUtils.log(Level.FINE, "Find all purchase options");
		
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
	
	private Map<Territory, ProPurchaseTerritory> findPurchaseTerritories()
	{
		LogUtils.log(Level.FINE, "Find all purchase territories");
		
		// Find all territories that I can place units on
		final RulesAttachment ra = (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
		List<Territory> canPlaceUnitsTerritories = new ArrayList<Territory>();
		if (ra != null && ra.getPlacementAnyTerritory()) // make them all available for placing
		{
			canPlaceUnitsTerritories = data.getMap().getTerritoriesOwnedBy(player);
		}
		else
		{
			final Match<Unit> ourFactories = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanProduceUnits);
			final List<Territory> territoriesWithOurFactories = new ArrayList<Territory>();
			for (final Territory t : data.getMap().getTerritories())
			{
				if (t.getUnits().someMatch(ourFactories) && !AbstractMoveDelegate.getBattleTracker(data).wasConquered(t))
				{
					territoriesWithOurFactories.add(t);
				}
			}
			canPlaceUnitsTerritories = Match.getMatches(territoriesWithOurFactories, Matches.isTerritoryOwnedBy(player));
		}
		purchaseTerritories = new HashMap<Territory, ProPurchaseTerritory>();
		for (final Territory t : canPlaceUnitsTerritories)
		{
			final ProPurchaseTerritory ppt = new ProPurchaseTerritory(t, data, player);
			purchaseTerritories.put(t, ppt);
			LogUtils.log(Level.FINER, ppt.toString());
		}
		
		return purchaseTerritories;
	}
	
	private void findDefendersInPlaceTerritories(final Map<Territory, ProPurchaseTerritory> purchaseTerritories)
	{
		LogUtils.log(Level.FINE, "Find defenders in possible place territories");
		
		final Match<Unit> myUnitMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player));
		final Match<Unit> alliedUnitMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player).invert(), Matches.isUnitAllied(player, data));
		final Match<Unit> myUnitOrAlliedMatch = new CompositeMatchOr<Unit>(myUnitMatch, alliedUnitMatch);
		
		for (final ProPurchaseTerritory ppt : purchaseTerritories.values())
		{
			for (final ProPlaceTerritory placeTerritory : ppt.getCanPlaceTerritories())
			{
				final Territory t = placeTerritory.getTerritory();
				final List<Unit> units = t.getUnits().getMatches(myUnitOrAlliedMatch);
				placeTerritory.setDefendingUnits(units);
				LogUtils.log(Level.FINER, t + " has numDefenders=" + units.size());
			}
		}
	}
	
	private List<ProPlaceTerritory> prioritizeTerritoriesToDefend(final Map<Territory, ProPurchaseTerritory> purchaseTerritories, final Map<Territory, ProAttackTerritoryData> enemyAttackMap)
	{
		LogUtils.log(Level.FINE, "Prioritize territories to defend");
		
		// Determine which territories need defended
		final List<ProPlaceTerritory> needToDefendTerritories = new ArrayList<ProPlaceTerritory>();
		for (final ProPurchaseTerritory ppt : purchaseTerritories.values())
		{
			// Check if any of the place territories can't be held with current defenders
			for (final ProPlaceTerritory placeTerritory : ppt.getCanPlaceTerritories())
			{
				final Territory t = placeTerritory.getTerritory();
				if (enemyAttackMap.get(t) == null || t.isWater())
					continue;
				
				// Find current battle result
				final Set<Unit> enemyAttackingUnits = new HashSet<Unit>(enemyAttackMap.get(t).getMaxUnits());
				enemyAttackingUnits.addAll(enemyAttackMap.get(t).getMaxAmphibUnits());
				final ProBattleResultData result = battleUtils.estimateBattleResults(player, t, new ArrayList<Unit>(enemyAttackingUnits), placeTerritory.getDefendingUnits(), false);
				placeTerritory.setMinBattleResult(result);
				
				// If it can't currently be held then add to list
				if (result.isHasLandUnitRemaining() || result.getTUVSwing() > 0)
					needToDefendTerritories.add(placeTerritory);
			}
		}
		
		// Calculate value of defending territory
		final IntegerMap<UnitType> playerCostMap = BattleCalculator.getCostsForTUV(player, data);
		for (final ProPlaceTerritory placeTerritory : needToDefendTerritories)
		{
			final Territory t = placeTerritory.getTerritory();
			
			// Determine if it is my capital or adjacent to my capital
			int isMyCapital = 0;
			if (t.equals(myCapital))
				isMyCapital = 1;
			
			// Determine if it has a factory
			int isFactory = 0;
			if (t.getUnits().someMatch(Matches.UnitCanProduceUnits))
				isFactory = 1;
			
			// Determine production value and if it is an enemy capital
			int production = 0;
			final TerritoryAttachment ta = TerritoryAttachment.get(t);
			if (ta != null)
			{
				production = ta.getProduction();
			}
			
			// Calculate defense value for prioritization
			final int defendingUnitValue = BattleCalculator.getTUV(placeTerritory.getDefendingUnits(), playerCostMap);
			final double territoryValue = (2 * production + 5 * isFactory + 0.5 * defendingUnitValue) * (1 + 10 * isMyCapital);
			placeTerritory.setDefenseValue(territoryValue);
		}
		
		// Sort territories by value
		Collections.sort(needToDefendTerritories, new Comparator<ProPlaceTerritory>()
		{
			public int compare(final ProPlaceTerritory t1, final ProPlaceTerritory t2)
			{
				final double value1 = t1.getDefenseValue();
				final double value2 = t2.getDefenseValue();
				return Double.compare(value2, value1);
			}
		});
		
		for (final ProPlaceTerritory placeTerritory : needToDefendTerritories)
			LogUtils.log(Level.FINER, placeTerritory.toString() + " defenseValue=" + placeTerritory.getDefenseValue());
		
		return needToDefendTerritories;
	}
	
	private int purchaseDefenders(final Map<Territory, ProPurchaseTerritory> purchaseTerritories, final Map<Territory, ProAttackTerritoryData> enemyAttackMap,
				final List<ProPlaceTerritory> needToDefendTerritories, int PUsRemaining, final List<ProPurchaseOption> landPurchaseOptions)
	{
		LogUtils.log(Level.FINE, "Purchase defenders with PUsRemaining=" + PUsRemaining);
		
		// Loop through prioritized territories and purchase defenders
		for (final ProPlaceTerritory placeTerritory : needToDefendTerritories)
		{
			final Territory t = placeTerritory.getTerritory();
			
			// Determine most cost efficient defender that can be produced in this territory
			final List<ProPurchaseOption> purchaseOptionsForTerritory = findPurchaseOptionsForTerritory(landPurchaseOptions, t);
			ProPurchaseOption bestDefenseOption = null;
			double maxDefenseEfficiency = 0;
			for (final ProPurchaseOption ppo : purchaseOptionsForTerritory)
			{
				if (ppo.getDefenseEfficiency() > maxDefenseEfficiency && ppo.getCost() <= PUsRemaining)
				{
					bestDefenseOption = ppo;
					maxDefenseEfficiency = ppo.getDefenseEfficiency();
				}
			}
			
			int remainingUnitProduction = purchaseTerritories.get(t).getUnitProduction();
			int PUsSpent = 0;
			ProBattleResultData finalResult = new ProBattleResultData();
			while (true)
			{
				// If out of PUs or production then break
				if (bestDefenseOption == null || bestDefenseOption.getCost() > (PUsRemaining - PUsSpent) || remainingUnitProduction < bestDefenseOption.getQuantity())
					break;
				
				// Create new temp defenders
				PUsSpent += bestDefenseOption.getCost();
				remainingUnitProduction -= bestDefenseOption.getQuantity();
				placeTerritory.getPlaceUnits().addAll(bestDefenseOption.getUnitType().create(bestDefenseOption.getQuantity(), player, true));
				
				// Find current battle result
				final Set<Unit> enemyAttackingUnits = new HashSet<Unit>(enemyAttackMap.get(t).getMaxUnits());
				enemyAttackingUnits.addAll(enemyAttackMap.get(t).getMaxAmphibUnits());
				finalResult = battleUtils.estimateBattleResults(player, t, new ArrayList<Unit>(enemyAttackingUnits), placeTerritory.getAllDefenders(), false);
				
				// If it can't currently be held then add to list
				if (!finalResult.isHasLandUnitRemaining() && finalResult.getTUVSwing() <= 0)
					break;
			}
			
			// Check to see if its worth trying to defend the territory
			// if (!finalResult.isHasLandUnitRemaining() || finalResult.getTUVSwing() < placeTerritory.getMinBattleResult().getTUVSwing() || placeTerritory.equals(myCapital))
			PUsRemaining -= PUsSpent;
			// else
			// placeTerritory.getPlaceUnits().clear();
		}
		for (final ProPlaceTerritory placeTerritory : needToDefendTerritories)
			LogUtils.log(Level.FINER, placeTerritory.toString() + " placedUnits=" + placeTerritory.getPlaceUnits());
		
		return PUsRemaining;
	}
	
	private List<ProPlaceTerritory> prioritizeLandTerritories(final Map<Territory, ProPurchaseTerritory> purchaseTerritories)
	{
		LogUtils.log(Level.FINE, "Prioritize land territories to place");
		
		// Get all land place territories
		final List<ProPlaceTerritory> prioritizedLandTerritories = new ArrayList<ProPlaceTerritory>();
		for (final ProPurchaseTerritory ppt : purchaseTerritories.values())
		{
			for (final ProPlaceTerritory placeTerritory : ppt.getCanPlaceTerritories())
			{
				final Territory t = placeTerritory.getTerritory();
				if (!t.isWater() && placeTerritory.getStrategicValue() > 0)
					prioritizedLandTerritories.add(placeTerritory);
			}
		}
		
		// Sort territories by value
		Collections.sort(prioritizedLandTerritories, new Comparator<ProPlaceTerritory>()
		{
			public int compare(final ProPlaceTerritory t1, final ProPlaceTerritory t2)
			{
				final double value1 = t1.getStrategicValue();
				final double value2 = t2.getStrategicValue();
				return Double.compare(value2, value1);
			}
		});
		
		for (final ProPlaceTerritory placeTerritory : prioritizedLandTerritories)
			LogUtils.log(Level.FINER, placeTerritory.toString() + " strategicValue=" + placeTerritory.getStrategicValue());
		
		return prioritizedLandTerritories;
	}
	
	private int purchaseLandUnits(final Map<Territory, ProPurchaseTerritory> purchaseTerritories, final Map<Territory, ProAttackTerritoryData> enemyAttackMap,
				final List<ProPlaceTerritory> prioritizedLandTerritories, int PUsRemaining, final List<ProPurchaseOption> landPurchaseOptions)
	{
		LogUtils.log(Level.FINE, "Purchase land units with PUsRemaining=" + PUsRemaining);
		
		// Loop through prioritized territories and purchase land units
		for (final ProPlaceTerritory placeTerritory : prioritizedLandTerritories)
		{
			final Territory t = placeTerritory.getTerritory();
			
			// Determine units that can be produced in this territory
			final List<ProPurchaseOption> purchaseOptionsForTerritory = findPurchaseOptionsForTerritory(landPurchaseOptions, t);
			
			// Determine most cost efficient units
			ProPurchaseOption bestHitPointOption = null;
			double maxHitPointEfficiency = 0;
			ProPurchaseOption bestAttackOption = null;
			double maxAttackEfficiency = 0;
			ProPurchaseOption bestTwoMoveOption = null;
			double maxTwoMoveEfficiency = 0;
			ProPurchaseOption bestThreeMoveOption = null;
			double maxThreeMoveEfficiency = 0;
			for (final ProPurchaseOption ppo : purchaseOptionsForTerritory)
			{
				if (ppo.getCost() <= PUsRemaining)
				{
					if (ppo.getHitPointEfficiency() > maxHitPointEfficiency)
					{
						bestHitPointOption = ppo;
						maxHitPointEfficiency = ppo.getHitPointEfficiency();
					}
					if (ppo.getAttackEfficiency() > maxAttackEfficiency)
					{
						bestAttackOption = ppo;
						maxAttackEfficiency = ppo.getAttackEfficiency();
					}
					if (ppo.getMovement() >= 2 && ppo.getAttackEfficiency() > maxTwoMoveEfficiency)
					{
						bestTwoMoveOption = ppo;
						maxTwoMoveEfficiency = ppo.getAttackEfficiency();
					}
					if (ppo.getMovement() >= 3 && ppo.getAttackEfficiency() > maxThreeMoveEfficiency)
					{
						bestThreeMoveOption = ppo;
						maxThreeMoveEfficiency = ppo.getAttackEfficiency();
					}
				}
			}
			
			// Check if there aren't enough PUs to buy any units
			if (bestHitPointOption == null || bestAttackOption == null)
				continue;
			
			LogUtils.log(Level.FINER, "Best hit point unit: " + bestHitPointOption.getUnitType().getName());
			LogUtils.log(Level.FINER, "Best attack unit:" + bestAttackOption.getUnitType().getName());
			if (bestTwoMoveOption != null)
				LogUtils.log(Level.FINER, "Best two move unit: " + bestTwoMoveOption.getUnitType().getName());
			if (bestThreeMoveOption != null)
				LogUtils.log(Level.FINER, "Best three move unit: " + bestThreeMoveOption.getUnitType().getName());
			
			// Get optimal unit combo based on distance from enemy
			final int enemyDistance = utils.getClosestEnemyLandTerritoryDistance(data, player, t);
			if (enemyDistance <= 0)
				continue;
			int hitPointPercent = 65;
			int attackPercent = 35;
			int twoMovePercent = 0;
			int threeMovePercent = 0;
			if (bestThreeMoveOption != null && enemyDistance > 4)
				threeMovePercent = 50;
			else if (bestTwoMoveOption != null)
				twoMovePercent = 10 * enemyDistance;
			if (threeMovePercent + twoMovePercent > attackPercent)
			{
				hitPointPercent = 100 - (threeMovePercent + twoMovePercent);
				attackPercent = 0;
			}
			else
			{
				attackPercent = attackPercent - (threeMovePercent + twoMovePercent);
			}
			LogUtils.log(Level.FINEST, t + ", enemyDistance=" + enemyDistance + ", hitPointPercent=" + hitPointPercent + ", attackPercent=" + attackPercent + ", twoMovePercent=" + twoMovePercent
						+ " threeMovePercent=" + threeMovePercent);
			
			// Find optimal units for remaining production
			int remainingUnitProduction = purchaseTerritories.get(t).getRemainingUnitProduction();
			final int numHitPointUnits = (int) Math.ceil(hitPointPercent / 100.0 * remainingUnitProduction);
			final int numAttackUnits = (int) Math.ceil(attackPercent / 100.0 * remainingUnitProduction);
			final int numTwoMoveUnits = (int) Math.ceil(twoMovePercent / 100.0 * remainingUnitProduction);
			final int numThreeMoveUnits = (int) Math.ceil(threeMovePercent / 100.0 * remainingUnitProduction);
			
			// Purchase as many units as possible
			int addedHitPointUnits = 0;
			int addedAttackUnits = 0;
			int addedTwoMoveUnits = 0;
			int addedThreeMoveUnits = 0;
			final List<Unit> unitsToPlace = new ArrayList<Unit>();
			while (true)
			{
				// Find current purchase option
				ProPurchaseOption ppo = bestHitPointOption;
				if (addedHitPointUnits < numHitPointUnits && bestHitPointOption.getCost() <= PUsRemaining && remainingUnitProduction >= bestHitPointOption.getQuantity())
				{
					addedHitPointUnits += ppo.getQuantity();
				}
				else if (addedThreeMoveUnits < numThreeMoveUnits && bestThreeMoveOption.getCost() <= PUsRemaining && remainingUnitProduction >= bestThreeMoveOption.getQuantity())
				{
					ppo = bestThreeMoveOption;
					addedThreeMoveUnits += ppo.getQuantity();
				}
				else if (addedTwoMoveUnits < numTwoMoveUnits && bestTwoMoveOption.getCost() <= PUsRemaining && remainingUnitProduction >= bestTwoMoveOption.getQuantity())
				{
					ppo = bestTwoMoveOption;
					addedTwoMoveUnits += ppo.getQuantity();
				}
				else if (addedAttackUnits < numAttackUnits && bestAttackOption.getCost() <= PUsRemaining && remainingUnitProduction >= bestAttackOption.getQuantity())
				{
					ppo = bestAttackOption;
					addedAttackUnits += ppo.getQuantity();
				}
				else
				{
					break;
				}
				
				// Create new temp units
				PUsRemaining -= ppo.getCost();
				remainingUnitProduction -= ppo.getQuantity();
				unitsToPlace.addAll(ppo.getUnitType().create(ppo.getQuantity(), player, true));
			}
			
			// Add units to place territory
			placeTerritory.getPlaceUnits().addAll(unitsToPlace);
			LogUtils.log(Level.FINEST, t + ", placedUnits=" + unitsToPlace);
		}
		
		return PUsRemaining;
	}
	
	private List<ProPlaceTerritory> prioritizeSeaTerritories(final Map<Territory, ProPurchaseTerritory> purchaseTerritories)
	{
		LogUtils.log(Level.FINE, "Prioritize sea territories");
		
		// Determine which territories can be held
		final Set<ProPlaceTerritory> seaPlaceTerritoriesSet = new HashSet<ProPlaceTerritory>();
		for (final ProPurchaseTerritory ppt : purchaseTerritories.values())
		{
			for (final ProPlaceTerritory placeTerritory : ppt.getCanPlaceTerritories())
			{
				final Territory t = placeTerritory.getTerritory();
				if (t.isWater())
					seaPlaceTerritoriesSet.add(placeTerritory);
			}
		}
		final List<ProPlaceTerritory> seaPlaceTerritories = new ArrayList<ProPlaceTerritory>(seaPlaceTerritoriesSet);
		
		// Calculate value of defending territory
		final IntegerMap<UnitType> playerCostMap = BattleCalculator.getCostsForTUV(player, data);
		for (final ProPlaceTerritory placeTerritory : seaPlaceTerritories)
		{
			// Calculate defense value for prioritization
			final int defendingUnitValue = BattleCalculator.getTUV(placeTerritory.getDefendingUnits(), playerCostMap);
			final double territoryValue = defendingUnitValue + placeTerritory.getStrategicValue();
			placeTerritory.setStrategicValue(territoryValue);
		}
		
		// Sort territories by value
		Collections.sort(seaPlaceTerritories, new Comparator<ProPlaceTerritory>()
		{
			public int compare(final ProPlaceTerritory t1, final ProPlaceTerritory t2)
			{
				final double value1 = t1.getStrategicValue();
				final double value2 = t2.getStrategicValue();
				return Double.compare(value2, value1);
			}
		});
		
		for (final ProPlaceTerritory placeTerritory : seaPlaceTerritories)
			LogUtils.log(Level.FINER, placeTerritory.toString() + " seaValue=" + placeTerritory.getStrategicValue());
		
		return seaPlaceTerritories;
	}
	
	private int purchaseSeaUnits(final Map<Territory, ProPurchaseTerritory> purchaseTerritories, final Map<Territory, ProAttackTerritoryData> enemyAttackMap,
				final List<ProPlaceTerritory> prioritizedSeaTerritories, int PUsRemaining, final List<ProPurchaseOption> seaPurchaseOptions, final List<ProPurchaseOption> landPurchaseOptions)
	{
		LogUtils.log(Level.FINE, "Purchase sea units with PUsRemaining=" + PUsRemaining);
		
		// Loop through prioritized territories and purchase sea units
		for (final ProPlaceTerritory placeTerritory : prioritizedSeaTerritories)
		{
			final Territory t = placeTerritory.getTerritory();
			
			// Determine units that can be produced in this territory
			final List<ProPurchaseOption> seaPurchaseOptionsForTerritory = findPurchaseOptionsForTerritory(seaPurchaseOptions, t);
			final List<ProPurchaseOption> landPurchaseOptionsForTerritory = findPurchaseOptionsForTerritory(landPurchaseOptions, t);
			
			// Determine most cost efficient sea units
			ProPurchaseOption bestTransportOption = null;
			double maxTransportEfficiency = 0;
			ProPurchaseOption bestDefenseOption = null;
			double maxDefenseEfficiency = 0;
			for (final ProPurchaseOption ppo : seaPurchaseOptionsForTerritory)
			{
				if (ppo.getCost() <= PUsRemaining)
				{
					if (ppo.getTransportEfficiency() * ppo.getMovement() > maxTransportEfficiency)
					{
						bestTransportOption = ppo;
						maxTransportEfficiency = ppo.getTransportEfficiency() * ppo.getMovement();
					}
					if (!ppo.isSub() && ppo.getDefenseEfficiency() * ppo.getMovement() > maxDefenseEfficiency)
					{
						bestDefenseOption = ppo;
						maxDefenseEfficiency = ppo.getDefenseEfficiency() * ppo.getMovement();
					}
				}
			}
			
			// Determine most cost efficient amphib units (based on best hit point efficiency for now)
			ProPurchaseOption bestAmphibOption = null;
			double maxAmphibEfficiency = 0;
			for (final ProPurchaseOption ppo : landPurchaseOptionsForTerritory)
			{
				if (ppo.getCost() <= PUsRemaining)
				{
					if (ppo.getHitPointEfficiency() > maxAmphibEfficiency)
					{
						bestAmphibOption = ppo;
						maxAmphibEfficiency = ppo.getHitPointEfficiency();
					}
				}
			}
			
			// Check if there aren't enough PUs to buy any units
			if (bestTransportOption == null && bestDefenseOption == null && bestAmphibOption == null)
				continue;
			
			if (bestTransportOption != null)
				LogUtils.log(Level.FINER, "Best transport unit: " + bestTransportOption.getUnitType().getName());
			if (bestDefenseOption != null)
				LogUtils.log(Level.FINER, "Best defense unit: " + bestDefenseOption.getUnitType().getName());
			if (bestAmphibOption != null)
				LogUtils.log(Level.FINER, "Best amphib unit: " + bestAmphibOption.getUnitType().getName());
			
			// If I don't have enough PUs remaining to purchase anything then break
			if ((bestTransportOption == null || bestTransportOption.getCost() > PUsRemaining)
						&& (bestDefenseOption == null || bestDefenseOption.getCost() > PUsRemaining)
						&& (bestAmphibOption == null || bestAmphibOption.getCost() > PUsRemaining))
				break;
			
			// Find remaining production
			int remainingUnitProduction = 0;
			for (final Territory purchaseTerritory : purchaseTerritories.keySet())
			{
				for (final ProPlaceTerritory ppt : purchaseTerritories.get(purchaseTerritory).getCanPlaceTerritories())
				{
					if (t.equals(ppt.getTerritory()))
					{
						remainingUnitProduction += purchaseTerritories.get(purchaseTerritory).getRemainingUnitProduction();
					}
				}
			}
			LogUtils.log(Level.FINER, t + ", remainingUnitProduction=" + remainingUnitProduction);
			
			// If any enemy attackers then purchase sea defenders until it can be held
			if (enemyAttackMap.get(t) != null)
			{
				int PUsSpent = 0;
				final List<Unit> unitsToPlace = new ArrayList<Unit>();
				ProBattleResultData finalResult = battleUtils.estimateBattleResults(player, t, enemyAttackMap.get(t).getMaxUnits(), placeTerritory.getAllDefenders(), false);
				while (true)
				{
					// If out of PUs or production then break
					if (bestDefenseOption == null || bestDefenseOption.getCost() > (PUsRemaining - PUsSpent) || remainingUnitProduction < bestDefenseOption.getQuantity())
						break;
					
					// If it can be held then break
					if (finalResult.getTUVSwing() < 0 || finalResult.getWinPercentage() < WIN_PERCENTAGE)
						break;
					
					// Create new temp defenders
					PUsSpent += bestDefenseOption.getCost();
					remainingUnitProduction -= bestDefenseOption.getQuantity();
					unitsToPlace.addAll(bestDefenseOption.getUnitType().create(bestDefenseOption.getQuantity(), player, true));
					
					// Find current battle result
					final List<Unit> defendingUnits = new ArrayList<Unit>(placeTerritory.getAllDefenders());
					defendingUnits.addAll(unitsToPlace);
					finalResult = battleUtils.estimateBattleResults(player, t, enemyAttackMap.get(t).getMaxUnits(), defendingUnits, false);
				}
				
				// Check to see if its worth trying to defend the territory
				if (finalResult.getTUVSwing() < 0 || finalResult.getWinPercentage() < WIN_PERCENTAGE)
				{
					PUsRemaining -= PUsSpent;
					placeTerritory.getPlaceUnits().addAll(unitsToPlace);
					LogUtils.log(Level.FINER, t + ", placedSeaDefenders=" + unitsToPlace);
				}
				else
				{
					LogUtils.log(Level.FINER, t + ", can't defend TUVSwing=" + finalResult.getTUVSwing() + ", win%=" + finalResult.getWinPercentage());
					continue;
				}
			}
			
			// Make sure to have local naval superiority
			final List<PlayerID> enemyPlayers = utils.getEnemyPlayers(player);
			final Match<Unit> enemyAirUnitMatch = new CompositeMatchAnd<Unit>(Matches.UnitIsAir, Matches.unitIsOwnedByOfAnyOfThesePlayers(enemyPlayers));
			final Match<Unit> enemySeaUnitMatch = new CompositeMatchAnd<Unit>(Matches.UnitIsSea, Matches.unitIsOwnedByOfAnyOfThesePlayers(enemyPlayers));
			final Match<Unit> enemyAirOrSeaUnitMatch = new CompositeMatchOr<Unit>(enemyAirUnitMatch, enemySeaUnitMatch);
			final Match<Unit> myUnitMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player));
			final Match<Unit> alliedUnitMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player).invert(), Matches.isUnitAllied(player, data));
			final Match<Unit> myUnitOrAlliedMatch = new CompositeMatchOr<Unit>(myUnitMatch, alliedUnitMatch);
			final Match<Unit> alliedAirOrSeaUnitMatch = new CompositeMatchAnd<Unit>(Matches.UnitIsNotLand, myUnitOrAlliedMatch);
			final Set<Territory> nearbyTerritories = data.getMap().getNeighbors(t, 4);
			final List<Territory> nearbyLandTerritories = Match.getMatches(nearbyTerritories, Matches.TerritoryIsLand);
			final Set<Territory> nearbySeaTerritories = data.getMap().getNeighbors(t, 4, Matches.TerritoryIsWater);
			nearbySeaTerritories.add(t);
			final List<Unit> enemyUnits = new ArrayList<Unit>();
			final List<Unit> alliedUnits = new ArrayList<Unit>();
			for (final Territory nearbyLandTerritory : nearbyLandTerritories)
			{
				enemyUnits.addAll(nearbyLandTerritory.getUnits().getMatches(enemyAirUnitMatch));
			}
			for (final Territory nearbySeaTerritory : nearbySeaTerritories)
			{
				enemyUnits.addAll(nearbySeaTerritory.getUnits().getMatches(enemyAirOrSeaUnitMatch));
				alliedUnits.addAll(nearbySeaTerritory.getUnits().getMatches(alliedAirOrSeaUnitMatch));
			}
			while (true)
			{
				// If out of PUs or production then break
				if (bestDefenseOption == null || bestDefenseOption.getCost() > PUsRemaining || remainingUnitProduction < bestDefenseOption.getQuantity())
					break;
				
				// Find current strength difference
				final List<Unit> currentDefenders = new ArrayList<Unit>(alliedUnits);
				currentDefenders.addAll(placeTerritory.getPlaceUnits());
				final double strengthDifference = battleUtils.estimateStrengthDifference(t, enemyUnits, currentDefenders);
				LogUtils.log(Level.FINER, t + ", current enemy naval strengthDifference=" + strengthDifference + ", enemySize=" + enemyUnits.size() + ", alliedSize=" + currentDefenders.size());
				if (strengthDifference <= 50)
					break;
				
				// Create new temp units
				placeTerritory.getPlaceUnits().addAll(bestDefenseOption.getUnitType().create(bestDefenseOption.getQuantity(), player, true));
				PUsRemaining -= bestDefenseOption.getCost();
				remainingUnitProduction -= bestDefenseOption.getQuantity();
				LogUtils.log(Level.FINER, t + ", added sea defender for naval superiority: " + bestDefenseOption.getUnitType().getName());
			}
			
			// If no amphib option then break
			if (bestAmphibOption == null)
				continue;
			
			// Determine whether transports, amphib units, or both are needed
			final Match<Unit> myTransport = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsTransport);
			final List<Unit> transports = Match.getMatches(t.getUnits().getUnits(), myTransport);
			int transportCapacity = 0;
			for (final Unit transport : transports)
			{
				transportCapacity += UnitAttachment.get(transport.getType()).getTransportCapacity() / bestAmphibOption.getTransportCost();
			}
			final int numUnitsToLoad = transportUtils.findNumUnitsThatCanBeTransported(player, t);
			int numNeededUnits = transportCapacity - numUnitsToLoad;
			LogUtils.log(Level.FINER, t + ", numNeededAmphibUnits=" + numNeededUnits + " = " + transportCapacity + " - " + numUnitsToLoad);
			
			// Purchase transports and amphib units
			final List<Unit> amphibUnitsToPlace = new ArrayList<Unit>();
			final List<Unit> transportUnitsToPlace = new ArrayList<Unit>();
			while (true)
			{
				// Find current purchase option
				ProPurchaseOption ppo = bestAmphibOption;
				if (numNeededUnits >= 0 && bestAmphibOption.getCost() <= PUsRemaining && remainingUnitProduction >= bestAmphibOption.getQuantity())
				{
					numNeededUnits -= bestAmphibOption.getQuantity();
					amphibUnitsToPlace.addAll(ppo.getUnitType().create(ppo.getQuantity(), player, true));
				}
				else if (bestTransportOption != null && numNeededUnits < 0 && bestTransportOption.getCost() <= PUsRemaining && remainingUnitProduction >= bestTransportOption.getQuantity())
				{
					ppo = bestTransportOption;
					numNeededUnits += bestTransportOption.getTransportCapacity() * bestTransportOption.getQuantity() / bestAmphibOption.getTransportCost();
					transportUnitsToPlace.addAll(ppo.getUnitType().create(ppo.getQuantity(), player, true));
				}
				else
				{
					break;
				}
				
				// Create new temp units
				PUsRemaining -= ppo.getCost();
				remainingUnitProduction -= ppo.getQuantity();
			}
			
			// Add transport units to place territory and amphib units to adjacent factories
			for (final Territory purchaseTerritory : purchaseTerritories.keySet())
			{
				for (final ProPlaceTerritory ppt : purchaseTerritories.get(purchaseTerritory).getCanPlaceTerritories())
				{
					// If place territory is equal to the current place territory and has remaining production
					if (t.equals(ppt.getTerritory()) && purchaseTerritories.get(purchaseTerritory).getRemainingUnitProduction() > 0)
					{
						// Place transports
						final int numUnits = Math.min(purchaseTerritories.get(purchaseTerritory).getRemainingUnitProduction(), transportUnitsToPlace.size());
						final List<Unit> units = transportUnitsToPlace.subList(0, numUnits);
						ppt.getPlaceUnits().addAll(units);
						LogUtils.log(Level.FINER, t + ", transportUnits=" + transportUnitsToPlace);
						units.clear();
						
						for (final ProPlaceTerritory ppt2 : purchaseTerritories.get(purchaseTerritory).getCanPlaceTerritories())
						{
							// Find factory place territory
							if (purchaseTerritory.equals(ppt2.getTerritory()))
							{
								// Place amphib units
								final int numUnits2 = Math.min(purchaseTerritories.get(purchaseTerritory).getRemainingUnitProduction(), amphibUnitsToPlace.size());
								final List<Unit> units2 = amphibUnitsToPlace.subList(0, numUnits2);
								ppt2.getPlaceUnits().addAll(units2);
								LogUtils.log(Level.FINER, t + ", amphibUnits placed at " + purchaseTerritory + ": " + units);
								units2.clear();
								break;
							}
						}
					}
				}
			}
		}
		
		return PUsRemaining;
	}
	
	private int spendRemainingPUs(final Map<Territory, ProPurchaseTerritory> purchaseTerritories, int PUsRemaining, final List<ProPurchaseOption> landPurchaseOptions,
				final List<ProPurchaseOption> airPurchaseOptions)
	{
		LogUtils.log(Level.FINE, "Spend remaining PUs with PUsRemaining=" + PUsRemaining);
		
		// Loop through prioritized territories and purchase land units
		for (final Territory t : purchaseTerritories.keySet())
		{
			// Get place territory
			ProPlaceTerritory placeTerritory = null;
			for (final ProPlaceTerritory pt : purchaseTerritories.get(t).getCanPlaceTerritories())
			{
				if (pt.getTerritory().equals(t))
					placeTerritory = pt;
			}
			if (placeTerritory == null || placeTerritory.getTerritory().isWater())
				continue;
			
			// Determine units that can be produced in this territory
			final List<ProPurchaseOption> purchaseOptionsForTerritory = findPurchaseOptionsForTerritory(airPurchaseOptions, t);
			LogUtils.log(Level.FINER, "Air attack options: " + purchaseOptionsForTerritory.size());
			
			// Determine most cost efficient units
			ProPurchaseOption bestAirAttackOption = null;
			double maxAirAttackEfficiency = 0;
			for (final ProPurchaseOption ppo : purchaseOptionsForTerritory)
			{
				if (ppo.getCost() <= PUsRemaining)
				{
					if (ppo.getQuantity() == 1 && ppo.getAttackEfficiency() > maxAirAttackEfficiency)
					{
						bestAirAttackOption = ppo;
						maxAirAttackEfficiency = ppo.getAttackEfficiency();
					}
				}
			}
			
			// Check if there aren't enough PUs to buy any units
			if (bestAirAttackOption == null)
				continue;
			
			LogUtils.log(Level.FINER, "Best air attack unit: " + bestAirAttackOption.getUnitType().getName());
			
			// Replace land units with air units
			final int maxAirUnits = purchaseTerritories.get(t).getUnitProduction() / 3;
			final List<Unit> unitsToPlace = new ArrayList<Unit>();
			while (true)
			{
				// Find current purchase option
				final ProPurchaseOption ppo = bestAirAttackOption;
				if (unitsToPlace.size() < maxAirUnits && !placeTerritory.getPlaceUnits().isEmpty() && bestAirAttackOption.getCost() <= PUsRemaining)
				{
					// Find unit to replace with air unit
					Unit unitToRemove = null;
					for (final Unit u : placeTerritory.getPlaceUnits())
					{
						for (final ProPurchaseOption ppo2 : landPurchaseOptions)
						{
							if (u.getType().equals(ppo2.getUnitType()) && ppo2.getQuantity() == 1 && ppo.getCost() > ppo2.getCost())
							{
								final List<Unit> newUnit = ppo.getUnitType().create(ppo.getQuantity(), player, true);
								unitsToPlace.addAll(newUnit);
								PUsRemaining -= (ppo.getCost() - ppo2.getCost());
								unitToRemove = u;
								LogUtils.log(Level.FINEST, t + ", addedUnit=" + newUnit + ", removedUnit=" + unitToRemove);
								break;
							}
						}
						if (unitToRemove != null)
							break;
					}
					if (unitToRemove != null)
						placeTerritory.getPlaceUnits().remove(unitToRemove);
				}
				else
				{
					break;
				}
			}
			
			// Add units to place territory
			placeTerritory.getPlaceUnits().addAll(unitsToPlace);
		}
		
		return PUsRemaining;
	}
	
	private IntegerMap<ProductionRule> populateProductionRuleMap(final Map<Territory, ProPurchaseTerritory> purchaseTerritories, final List<ProPurchaseOption> landPurchaseOptions,
				final List<ProPurchaseOption> airPurchaseOptions, final List<ProPurchaseOption> seaPurchaseOptions)
	{
		LogUtils.log(Level.FINE, "Populate production rule map");
		
		final List<ProPurchaseOption> purchaseOptions = new ArrayList<ProPurchaseOption>(landPurchaseOptions);
		purchaseOptions.addAll(airPurchaseOptions);
		purchaseOptions.addAll(seaPurchaseOptions);
		final IntegerMap<ProductionRule> purchaseMap = new IntegerMap<ProductionRule>();
		for (final ProPurchaseOption ppo : purchaseOptions)
		{
			int numUnits = 0;
			for (final Territory t : purchaseTerritories.keySet())
			{
				for (final ProPlaceTerritory ppt : purchaseTerritories.get(t).getCanPlaceTerritories())
				{
					for (final Unit u : ppt.getPlaceUnits())
					{
						if (u.getUnitType().equals(ppo.getUnitType()))
							numUnits++;
					}
				}
			}
			if (numUnits > 0)
			{
				final int numProductionRule = numUnits / ppo.getQuantity();
				purchaseMap.put(ppo.getProductionRule(), numProductionRule);
				LogUtils.log(Level.FINE, numProductionRule + " " + ppo.getProductionRule());
			}
		}
		return purchaseMap;
	}
	
	private void doPlace(final Territory where, final Collection<Unit> toPlace, final IAbstractPlaceDelegate del)
	{
		final String message = del.placeUnits(new ArrayList<Unit>(toPlace), where);
		if (message != null)
		{
			LogUtils.log(Level.WARNING, message);
			LogUtils.log(Level.WARNING, "Attempt was at: " + where + " with: " + toPlace);
		}
		utils.pause();
	}
	
	private List<ProPurchaseOption> findPurchaseOptionsForTerritory(final List<ProPurchaseOption> purchaseOptions, final Territory t)
	{
		final List<ProPurchaseOption> result = new ArrayList<ProPurchaseOption>();
		for (final ProPurchaseOption ppo : purchaseOptions)
		{
			final List<Unit> units = ppo.getUnitType().create(ppo.getQuantity(), player, true);
			if (!Properties.getUnitPlacementRestrictions(data) || Match.someMatch(units, ProMatches.unitWhichRequiresUnitsHasRequiredUnits(player, t)))
			{
				result.add(ppo);
			}
		}
		return result;
	}
	
}
