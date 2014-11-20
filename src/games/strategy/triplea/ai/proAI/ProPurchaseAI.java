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
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.ai.proAI.util.LogUtils;
import games.strategy.triplea.ai.proAI.util.ProAttackOptionsUtils;
import games.strategy.triplea.ai.proAI.util.ProBattleUtils;
import games.strategy.triplea.ai.proAI.util.ProMatches;
import games.strategy.triplea.ai.proAI.util.ProMoveUtils;
import games.strategy.triplea.ai.proAI.util.ProPurchaseUtils;
import games.strategy.triplea.ai.proAI.util.ProTerritoryValueUtils;
import games.strategy.triplea.ai.proAI.util.ProTransportUtils;
import games.strategy.triplea.ai.proAI.util.ProUtils;
import games.strategy.triplea.attatchments.RulesAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.BattleCalculator;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveValidator;
import games.strategy.triplea.delegate.dataObjects.PlaceableUnits;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Pro purchase AI.
 * 
 * <ol>
 * <li>Add logic to consider 2 turn transport attacks</li>
 * <li>Consider V1 rules (unlimited production)</li>
 * <li>Improve consideration of whether factory should be sea vs land</li>
 * <li>Improve trying to spend remaining PUs</li>
 * </ol>
 * 
 * @author Ron Murhammer
 * @since 2014
 */
public class ProPurchaseAI
{
	public final static double WIN_PERCENTAGE = 95.0;
	
	// Utilities
	private final ProAI ai;
	private final ProUtils utils;
	private final ProBattleUtils battleUtils;
	private final ProTransportUtils transportUtils;
	private final ProAttackOptionsUtils attackOptionsUtils;
	private final ProMoveUtils moveUtils;
	private final ProTerritoryValueUtils territoryValueUtils;
	private final ProPurchaseUtils purchaseUtils;
	
	// Current data
	private GameData data;
	private PlayerID player;
	private Territory myCapital;
	private List<Territory> allTerritories;
	private Map<Unit, Territory> unitTerritoryMap;
	
	public ProPurchaseAI(final ProAI ai, final ProUtils utils, final ProBattleUtils battleUtils, final ProTransportUtils transportUtils, final ProAttackOptionsUtils attackOptionsUtils,
				final ProMoveUtils moveUtils, final ProTerritoryValueUtils territoryValueUtils, final ProPurchaseUtils purchaseUtils)
	{
		this.ai = ai;
		this.utils = utils;
		this.battleUtils = battleUtils;
		this.transportUtils = transportUtils;
		this.attackOptionsUtils = attackOptionsUtils;
		this.moveUtils = moveUtils;
		this.territoryValueUtils = territoryValueUtils;
		this.purchaseUtils = purchaseUtils;
	}
	
	public void bid(final int PUsToSpend, final IPurchaseDelegate purchaseDelegate, final GameData data, final PlayerID player)
	{
		LogUtils.log(Level.FINE, "Starting bid purchase phase");
		
		// Current data at the start of combat move
		this.data = data;
		this.player = player;
		myCapital = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
		allTerritories = data.getMap().getTerritories();
	}
	
	public Map<Territory, ProPurchaseTerritory> purchase(final int PUsToSpend, final IPurchaseDelegate purchaseDelegate, final GameData data, final PlayerID player)
	{
		LogUtils.log(Level.FINE, "Starting purchase phase");
		
		// Current data at the start of combat move
		this.data = data;
		this.player = player;
		myCapital = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
		allTerritories = data.getMap().getTerritories();
		int PUsRemaining = PUsToSpend;
		
		// Repair factories
		PUsRemaining = repairFactories(PUsRemaining, purchaseDelegate);
		
		// Find all purchase options
		final List<ProPurchaseOption> specialPurchaseOptions = new ArrayList<ProPurchaseOption>();
		final List<ProPurchaseOption> factoryPurchaseOptions = new ArrayList<ProPurchaseOption>();
		final List<ProPurchaseOption> landPurchaseOptions = new ArrayList<ProPurchaseOption>();
		final List<ProPurchaseOption> airPurchaseOptions = new ArrayList<ProPurchaseOption>();
		final List<ProPurchaseOption> seaPurchaseOptions = new ArrayList<ProPurchaseOption>();
		purchaseUtils.findPurchaseOptions(player, landPurchaseOptions, airPurchaseOptions, seaPurchaseOptions, factoryPurchaseOptions, specialPurchaseOptions);
		
		// Find all purchase territories
		final Map<Territory, ProPurchaseTerritory> purchaseTerritories = findPurchaseTerritories();
		final Set<Territory> placeTerritories = new HashSet<Territory>();
		placeTerritories.addAll(Match.getMatches(data.getMap().getTerritoriesOwnedBy(player), Matches.TerritoryIsLand));
		for (final Territory t : purchaseTerritories.keySet())
		{
			for (final ProPlaceTerritory ppt : purchaseTerritories.get(t).getCanPlaceTerritories())
			{
				placeTerritories.add(ppt.getTerritory());
			}
		}
		
		// Determine max enemy attack units and current allied defenders
		final Map<Territory, ProAttackTerritoryData> enemyAttackMap = new HashMap<Territory, ProAttackTerritoryData>();
		attackOptionsUtils.findMaxEnemyAttackUnits(player, new ArrayList<Territory>(), new ArrayList<Territory>(placeTerritories), enemyAttackMap);
		findDefendersInPlaceTerritories(purchaseTerritories);
		
		// Prioritize land territories that need defended and purchase additional defenders
		final List<ProPlaceTerritory> needToDefendLandTerritories = prioritizeTerritoriesToDefend(purchaseTerritories, enemyAttackMap, true);
		PUsRemaining = purchaseDefenders(purchaseTerritories, enemyAttackMap, needToDefendLandTerritories, PUsRemaining, landPurchaseOptions, true);
		
		// Find strategic value for each territory
		LogUtils.log(Level.FINE, "Find strategic value for place territories");
		final Map<Territory, Double> territoryValueMap = territoryValueUtils.findTerritoryValues(player, new ArrayList<Territory>());
		for (final Territory t : purchaseTerritories.keySet())
		{
			for (final ProPlaceTerritory ppt : purchaseTerritories.get(t).getCanPlaceTerritories())
			{
				ppt.setStrategicValue(territoryValueMap.get(ppt.getTerritory()));
				LogUtils.log(Level.FINER, ppt.getTerritory() + ", strategicValue=" + territoryValueMap.get(ppt.getTerritory()));
			}
		}
		
		// Prioritize land place options purchase AA then land units
		final List<ProPlaceTerritory> prioritizedLandTerritories = prioritizeLandTerritories(purchaseTerritories);
		PUsRemaining = purchaseAAUnits(purchaseTerritories, enemyAttackMap, prioritizedLandTerritories, PUsRemaining, specialPurchaseOptions);
		PUsRemaining = purchaseLandUnits(purchaseTerritories, enemyAttackMap, prioritizedLandTerritories, PUsRemaining, landPurchaseOptions);
		
		// Prioritize sea territories that need defended and purchase additional defenders
		final List<ProPlaceTerritory> needToDefendSeaTerritories = prioritizeTerritoriesToDefend(purchaseTerritories, enemyAttackMap, false);
		PUsRemaining = purchaseDefenders(purchaseTerritories, enemyAttackMap, needToDefendSeaTerritories, PUsRemaining, seaPurchaseOptions, false);
		
		// Determine whether to purchase new land factory
		final Map<Territory, ProPurchaseTerritory> factoryPurchaseTerritories = new HashMap<Territory, ProPurchaseTerritory>();
		PUsRemaining = purchaseFactory(factoryPurchaseTerritories, enemyAttackMap, PUsRemaining, factoryPurchaseOptions, purchaseTerritories, prioritizedLandTerritories, landPurchaseOptions, false);
		
		// Prioritize sea place options and purchase units
		final List<ProPlaceTerritory> prioritizedSeaTerritories = prioritizeSeaTerritories(purchaseTerritories);
		PUsRemaining = purchaseSeaUnits(purchaseTerritories, enemyAttackMap, prioritizedSeaTerritories, territoryValueMap, PUsRemaining, seaPurchaseOptions, landPurchaseOptions);
		
		// Try to use any remaining PUs on high value units
		PUsRemaining = purchaseAttackUnitsWithRemainingProduction(purchaseTerritories, PUsRemaining, landPurchaseOptions, airPurchaseOptions);
		PUsRemaining = upgradeUnitsWithRemainingPUs(purchaseTerritories, PUsRemaining, landPurchaseOptions, airPurchaseOptions);
		
		// Try to purchase land/sea factory with remaining PUs
		PUsRemaining = purchaseFactory(factoryPurchaseTerritories, enemyAttackMap, PUsRemaining, factoryPurchaseOptions, purchaseTerritories, prioritizedLandTerritories, landPurchaseOptions, true);
		
		// Add factory purchase territory to list if not empty
		if (!factoryPurchaseTerritories.isEmpty())
			purchaseTerritories.putAll(factoryPurchaseTerritories);
		
		// Determine final count of each production rule
		final IntegerMap<ProductionRule> purchaseMap = populateProductionRuleMap(purchaseTerritories, landPurchaseOptions, airPurchaseOptions, seaPurchaseOptions, factoryPurchaseOptions,
					specialPurchaseOptions);
		
		// Purchase units
		purchaseDelegate.purchase(purchaseMap);
		
		return purchaseTerritories;
	}
	
	public void place(final Map<Territory, ProPurchaseTerritory> purchaseTerritories, final IAbstractPlaceDelegate placeDelegate, final GameData data, final PlayerID player)
	{
		LogUtils.log(Level.FINE, "Starting place phase");
		
		// Place all units calculated during purchase phase (land then sea to reduce failed placements)
		for (final Territory t : purchaseTerritories.keySet())
		{
			for (final ProPlaceTerritory ppt : purchaseTerritories.get(t).getCanPlaceTerritories())
			{
				if (!ppt.getTerritory().isWater())
				{
					final Collection<Unit> myUnits = player.getUnits().getUnits();
					final List<Unit> unitsToPlace = new ArrayList<Unit>();
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
					LogUtils.log(Level.FINER, ppt.getTerritory() + " placed units: " + unitsToPlace);
				}
			}
		}
		for (final Territory t : purchaseTerritories.keySet())
		{
			for (final ProPlaceTerritory ppt : purchaseTerritories.get(t).getCanPlaceTerritories())
			{
				if (ppt.getTerritory().isWater())
				{
					final Collection<Unit> myUnits = player.getUnits().getUnits();
					final List<Unit> unitsToPlace = new ArrayList<Unit>();
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
					LogUtils.log(Level.FINER, ppt.getTerritory() + " placed units: " + unitsToPlace);
				}
			}
		}
		
		// Place remaining units (currently only implemented to handle land units, ex. WW2v3 China)
		if (player.getUnits().getUnits().isEmpty())
			return;
		
		// Current data at the start of place
		LogUtils.log(Level.FINER, "Remaining units to place: " + player.getUnits().getUnits());
		this.data = data;
		this.player = player;
		myCapital = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
		allTerritories = data.getMap().getTerritories();
		
		// Find all place territories
		final Map<Territory, ProPurchaseTerritory> placeNonConstructionTerritories = findPurchaseTerritories();
		
		// Determine max enemy attack units and current allied defenders
		final Map<Territory, ProAttackTerritoryData> enemyAttackMap = new HashMap<Territory, ProAttackTerritoryData>();
		attackOptionsUtils.findMaxEnemyAttackUnits(player, new ArrayList<Territory>(), new ArrayList<Territory>(placeNonConstructionTerritories.keySet()), enemyAttackMap);
		findDefendersInPlaceTerritories(placeNonConstructionTerritories);
		
		// Prioritize land territories that need defended and purchase additional defenders
		final List<ProPlaceTerritory> needToDefendLandTerritories = prioritizeTerritoriesToDefend(placeNonConstructionTerritories, enemyAttackMap, true);
		placeDefenders(placeNonConstructionTerritories, enemyAttackMap, needToDefendLandTerritories, placeDelegate);
		
		// Find strategic value for each territory
		LogUtils.log(Level.FINE, "Find strategic value for place territories");
		final Map<Territory, Double> territoryValueMap = territoryValueUtils.findTerritoryValues(player, new ArrayList<Territory>());
		for (final Territory t : placeNonConstructionTerritories.keySet())
		{
			for (final ProPlaceTerritory ppt : placeNonConstructionTerritories.get(t).getCanPlaceTerritories())
			{
				ppt.setStrategicValue(territoryValueMap.get(ppt.getTerritory()));
				LogUtils.log(Level.FINER, ppt.getTerritory() + ", strategicValue=" + territoryValueMap.get(ppt.getTerritory()));
			}
		}
		
		// Prioritize land place territories, add all territories, and then place units
		final List<ProPlaceTerritory> prioritizedLandTerritories = prioritizeLandTerritories(placeNonConstructionTerritories);
		for (final ProPurchaseTerritory ppt : placeNonConstructionTerritories.values())
		{
			for (final ProPlaceTerritory placeTerritory : ppt.getCanPlaceTerritories())
			{
				final Territory t = placeTerritory.getTerritory();
				if (!t.isWater() && !prioritizedLandTerritories.contains(placeTerritory))
					prioritizedLandTerritories.add(placeTerritory);
			}
		}
		placeLandUnits(placeNonConstructionTerritories, enemyAttackMap, prioritizedLandTerritories, placeDelegate);
	}
	
	private int repairFactories(int PUsRemaining, final IPurchaseDelegate purchaseDelegate)
	{
		LogUtils.log(Level.FINE, "Repairing factories with PUsRemaining=" + PUsRemaining);
		
		final int totPU = PUsRemaining;
		List<RepairRule> rrules = Collections.emptyList();
		final CompositeMatch<Unit> ourFactories = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanProduceUnits, Matches.UnitIsInfrastructure);
		final List<Territory> rfactories = Match.getMatches(data.getMap().getTerritories(), ProMatches.territoryHasInfraFactoryAndIsNotConqueredOwnedLand(player, data));
		if (player.getRepairFrontier() != null && games.strategy.triplea.Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data)) // figure out if anything needs to be repaired
		{
			rrules = player.getRepairFrontier().getRules();
			final IntegerMap<RepairRule> repairMap = new IntegerMap<RepairRule>();
			final HashMap<Unit, IntegerMap<RepairRule>> repair = new HashMap<Unit, IntegerMap<RepairRule>>();
			final Map<Unit, Territory> unitsThatCanProduceNeedingRepair = new HashMap<Unit, Territory>();
			final Collection<Unit> unitsThatAreDisabledNeedingRepair = new ArrayList<Unit>();
			final CompositeMatchAnd<Unit> ourDisabled = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsDisabled);
			final int minimumUnitPrice = 3;
			int diff = 0;
			int totalDamage = 0;
			int capDamage = 0;
			int capProduction = 0;
			Unit capUnit = null;
			Territory capUnitTerritory = null;
			int maxUnits = (totPU - 1) / minimumUnitPrice;
			int currentProduction = 0;
			int maxProduction = 0;
			Collections.shuffle(rfactories); // we should sort this
			for (final Territory fixTerr : rfactories)
			{
				if (!Matches.territoryIsOwnedAndHasOwnedUnitMatching(data, player, Matches.UnitCanProduceUnitsAndCanBeDamaged).match(fixTerr))
					continue;
				final Unit possibleFactoryNeedingRepair = TripleAUnit.getBiggestProducer(Match.getMatches(fixTerr.getUnits().getUnits(), ourFactories), fixTerr, player, data, false);
				if (Matches.UnitHasTakenSomeBombingUnitDamage.match(possibleFactoryNeedingRepair))
					unitsThatCanProduceNeedingRepair.put(possibleFactoryNeedingRepair, fixTerr);
				unitsThatAreDisabledNeedingRepair.addAll(Match.getMatches(fixTerr.getUnits().getUnits(), ourDisabled));
				final TripleAUnit taUnit = (TripleAUnit) possibleFactoryNeedingRepair;
				maxProduction += TripleAUnit.getHowMuchCanUnitProduce(possibleFactoryNeedingRepair, fixTerr, player, data, false, true);
				diff = taUnit.getUnitDamage();
				totalDamage += diff;
				if (fixTerr == myCapital)
				{
					capDamage += diff;
					capProduction = TripleAUnit.getHowMuchCanUnitProduce(possibleFactoryNeedingRepair, fixTerr, player, data, true, true);
					capUnit = possibleFactoryNeedingRepair;
					capUnitTerritory = fixTerr;
				}
				currentProduction += TripleAUnit.getHowMuchCanUnitProduce(possibleFactoryNeedingRepair, fixTerr, player, data, true, true);
			}
			rfactories.remove(myCapital);
			unitsThatCanProduceNeedingRepair.remove(capUnit);
			// assume minimum unit price is 3, and that we are buying only that... if we over repair, oh well, that is better than under-repairing
			// goal is to be able to produce all our units, and at least half of that production in the capitol
			if ((capProduction <= maxUnits / 2 || rfactories.isEmpty()) && capUnit != null) // if capitol is super safe, we don't have to do this. and if capitol is under siege, we should repair enough to place all our units here
			{
				for (final RepairRule rrule : rrules)
				{
					if (!capUnit.getUnitType().equals(rrule.getResults().keySet().iterator().next()))
						continue;
					if (!Matches.territoryIsOwnedAndHasOwnedUnitMatching(data, player, Matches.UnitCanProduceUnitsAndCanBeDamaged).match(myCapital))
						continue;
					final TripleAUnit taUnit = (TripleAUnit) capUnit;
					diff = taUnit.getUnitDamage();
					final int unitProductionAllowNegative = TripleAUnit.getHowMuchCanUnitProduce(capUnit, capUnitTerritory, player, data, false, true) - diff;
					if (!rfactories.isEmpty())
						diff = Math.min(diff, (maxUnits / 2 - unitProductionAllowNegative) + 1);
					else
						diff = Math.min(diff, (maxUnits - unitProductionAllowNegative));
					diff = Math.min(diff, PUsRemaining - minimumUnitPrice);
					if (diff > 0)
					{
						if (unitProductionAllowNegative >= 0)
							currentProduction += diff;
						else
							currentProduction += diff + unitProductionAllowNegative;
						repairMap.add(rrule, diff);
						repair.put(capUnit, repairMap);
						PUsRemaining -= diff;
						purchaseDelegate.purchaseRepair(repair);
						repair.clear();
						repairMap.clear();
						maxUnits = (PUsRemaining - 1) / minimumUnitPrice; // ideally we would adjust this after each single PU spent, then re-evaluate everything.
					}
				}
			}
			int i = 0;
			while (currentProduction < maxUnits && i < 2)
			{
				for (final RepairRule rrule : rrules)
				{
					for (final Unit fixUnit : unitsThatCanProduceNeedingRepair.keySet())
					{
						if (fixUnit == null || !fixUnit.getType().equals(rrule.getResults().keySet().iterator().next()))
							continue;
						if (!Matches.territoryIsOwnedAndHasOwnedUnitMatching(data, player, Matches.UnitCanProduceUnitsAndCanBeDamaged).match(unitsThatCanProduceNeedingRepair.get(fixUnit)))
							continue;
						// we will repair the first territories in the list as much as we can, until we fulfill the condition, then skip all other territories
						if (currentProduction >= maxUnits)
							continue;
						final TripleAUnit taUnit = (TripleAUnit) fixUnit;
						diff = taUnit.getUnitDamage();
						final int unitProductionAllowNegative = TripleAUnit.getHowMuchCanUnitProduce(fixUnit, unitsThatCanProduceNeedingRepair.get(fixUnit), player, data, false, true) - diff;
						if (i == 0)
						{
							if (unitProductionAllowNegative < 0)
								diff = Math.min(diff, (maxUnits - currentProduction) - unitProductionAllowNegative);
							else
								diff = Math.min(diff, (maxUnits - currentProduction));
						}
						diff = Math.min(diff, PUsRemaining - minimumUnitPrice);
						if (diff > 0)
						{
							if (unitProductionAllowNegative >= 0)
								currentProduction += diff;
							else
								currentProduction += diff + unitProductionAllowNegative;
							repairMap.add(rrule, diff);
							repair.put(fixUnit, repairMap);
							PUsRemaining -= diff;
							purchaseDelegate.purchaseRepair(repair);
							repair.clear();
							repairMap.clear();
							maxUnits = (PUsRemaining - 1) / minimumUnitPrice; // ideally we would adjust this after each single PU spent, then re-evaluate everything.
						}
					}
				}
				rfactories.add(myCapital);
				if (capUnit != null)
					unitsThatCanProduceNeedingRepair.put(capUnit, capUnitTerritory);
				i++;
			}
		}
		return PUsRemaining;
	}
	
	private Map<Territory, ProPurchaseTerritory> findPurchaseTerritories()
	{
		LogUtils.log(Level.FINE, "Find all purchase territories");
		
		// Find all territories that I can place units on
		final RulesAttachment ra = (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
		List<Territory> ownedAndNotConqueredFactoryTerritories = new ArrayList<Territory>();
		if (ra != null && ra.getPlacementAnyTerritory()) // make them all available for placing
			ownedAndNotConqueredFactoryTerritories = data.getMap().getTerritoriesOwnedBy(player);
		else
			ownedAndNotConqueredFactoryTerritories = Match.getMatches(data.getMap().getTerritories(), ProMatches.territoryHasInfraFactoryAndIsNotConqueredOwnedLand(player, data));
		ownedAndNotConqueredFactoryTerritories = Match.getMatches(ownedAndNotConqueredFactoryTerritories, ProMatches.territoryCanMoveLandUnits(player, data, false));
		
		// Create purchase territory holder for each factory territory
		final Map<Territory, ProPurchaseTerritory> purchaseTerritories = new HashMap<Territory, ProPurchaseTerritory>();
		for (final Territory t : ownedAndNotConqueredFactoryTerritories)
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
		
		for (final ProPurchaseTerritory ppt : purchaseTerritories.values())
		{
			for (final ProPlaceTerritory placeTerritory : ppt.getCanPlaceTerritories())
			{
				final Territory t = placeTerritory.getTerritory();
				final List<Unit> units = t.getUnits().getMatches(Matches.isUnitAllied(player, data));
				placeTerritory.setDefendingUnits(units);
				LogUtils.log(Level.FINER, t + " has numDefenders=" + units.size());
			}
		}
	}
	
	private List<ProPlaceTerritory> prioritizeTerritoriesToDefend(final Map<Territory, ProPurchaseTerritory> purchaseTerritories, final Map<Territory, ProAttackTerritoryData> enemyAttackMap,
				final boolean isLand)
	{
		LogUtils.log(Level.FINE, "Prioritize territories to defend with isLand=" + isLand);
		
		// Determine which territories need defended
		final Set<ProPlaceTerritory> needToDefendTerritories = new HashSet<ProPlaceTerritory>();
		for (final ProPurchaseTerritory ppt : purchaseTerritories.values())
		{
			// Check if any of the place territories can't be held with current defenders
			for (final ProPlaceTerritory placeTerritory : ppt.getCanPlaceTerritories())
			{
				final Territory t = placeTerritory.getTerritory();
				if (enemyAttackMap.get(t) == null || (t.isWater() && placeTerritory.getDefendingUnits().isEmpty()) || (isLand && t.isWater()) || (!isLand && !t.isWater()))
					continue;
				
				// Find current battle result
				final Set<Unit> enemyAttackingUnits = new HashSet<Unit>(enemyAttackMap.get(t).getMaxUnits());
				enemyAttackingUnits.addAll(enemyAttackMap.get(t).getMaxAmphibUnits());
				final ProBattleResultData result = battleUtils.calculateBattleResults(player, t, new ArrayList<Unit>(enemyAttackingUnits), placeTerritory.getDefendingUnits(), false);
				placeTerritory.setMinBattleResult(result);
				LogUtils.log(Level.FINEST, t.getName() + " TUVSwing=" + result.getTUVSwing() + ", win%=" + result.getWinPercentage() + ", hasLandUnitRemaining=" + result.isHasLandUnitRemaining());
				
				// If it can't currently be held then add to list
				if (result.isHasLandUnitRemaining() || result.getTUVSwing() > 0 || (t.equals(myCapital) && result.getWinPercentage() > (100 - WIN_PERCENTAGE)))
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
			if (ProMatches.territoryHasInfraFactoryAndIsOwnedLand(player).match(t))
				isFactory = 1;
			
			// Determine production value and if it is an enemy capital
			int production = 0;
			final TerritoryAttachment ta = TerritoryAttachment.get(t);
			if (ta != null)
				production = ta.getProduction();
			
			// Determine defending unit value
			double defendingUnitValue = BattleCalculator.getTUV(placeTerritory.getDefendingUnits(), playerCostMap);
			if (t.isWater() && Match.noneMatch(placeTerritory.getDefendingUnits(), Matches.unitIsOwnedBy(player)))
				defendingUnitValue = 0;
			
			// Calculate defense value for prioritization
			final double territoryValue = (2 * production + 4 * isFactory + 0.5 * defendingUnitValue) * (1 + isFactory) * (1 + 10 * isMyCapital);
			placeTerritory.setDefenseValue(territoryValue);
		}
		
		// Remove any territories with negative defense value
		for (final Iterator<ProPlaceTerritory> it = needToDefendTerritories.iterator(); it.hasNext();)
		{
			final ProPlaceTerritory ppt = it.next();
			if (ppt.getDefenseValue() <= 0)
				it.remove();
		}
		
		// Sort territories by value
		final List<ProPlaceTerritory> sortedTerritories = new ArrayList<ProPlaceTerritory>(needToDefendTerritories);
		Collections.sort(sortedTerritories, new Comparator<ProPlaceTerritory>()
		{
			public int compare(final ProPlaceTerritory t1, final ProPlaceTerritory t2)
			{
				final double value1 = t1.getDefenseValue();
				final double value2 = t2.getDefenseValue();
				return Double.compare(value2, value1);
			}
		});
		
		for (final ProPlaceTerritory placeTerritory : sortedTerritories)
			LogUtils.log(Level.FINER, placeTerritory.toString() + " defenseValue=" + placeTerritory.getDefenseValue());
		
		return sortedTerritories;
	}
	
	private int purchaseDefenders(final Map<Territory, ProPurchaseTerritory> purchaseTerritories, final Map<Territory, ProAttackTerritoryData> enemyAttackMap,
				final List<ProPlaceTerritory> needToDefendTerritories, int PUsRemaining, final List<ProPurchaseOption> defensePurchaseOptions, final boolean isLand)
	{
		if (PUsRemaining == 0)
			return PUsRemaining;
		LogUtils.log(Level.FINE, "Purchase defenders with PUsRemaining=" + PUsRemaining + ", isLand=" + isLand);
		
		// Loop through prioritized territories and purchase defenders
		for (final ProPlaceTerritory placeTerritory : needToDefendTerritories)
		{
			final Territory t = placeTerritory.getTerritory();
			LogUtils.log(Level.FINER, "Purchasing defenders for " + t.getName() + ", enemyAttackers=" + enemyAttackMap.get(t).getMaxUnits() + ", amphibEnemyAttackers="
						+ enemyAttackMap.get(t).getMaxAmphibUnits() + ", defenders=" + placeTerritory.getDefendingUnits());
			
			// Determine most cost efficient defender that can be produced in this territory
			final List<ProPurchaseOption> purchaseOptionsForTerritory = purchaseUtils.findPurchaseOptionsForTerritory(player, defensePurchaseOptions, t);
			ProPurchaseOption bestDefenseOption = null;
			double maxDefenseEfficiency = 0;
			for (final ProPurchaseOption ppo : purchaseOptionsForTerritory)
			{
				if (ppo.getCost() <= PUsRemaining)
				{
					if (isLand && ppo.getDefenseEfficiency() > maxDefenseEfficiency && ppo.getCost() <= PUsRemaining)
					{
						bestDefenseOption = ppo;
						maxDefenseEfficiency = ppo.getDefenseEfficiency();
					}
					else if (!isLand && !ppo.isSub() && (ppo.getDefenseEfficiency() * ppo.getMovement()) > maxDefenseEfficiency)
					{
						bestDefenseOption = ppo;
						maxDefenseEfficiency = ppo.getDefenseEfficiency() * ppo.getMovement();
					}
				}
			}
			if (bestDefenseOption == null)
				continue;
			LogUtils.log(Level.FINEST, "Best defense option: " + bestDefenseOption.getUnitType().getName());
			
			// Find remaining production
			int remainingUnitProduction = 0;
			if (isLand)
			{
				remainingUnitProduction = purchaseTerritories.get(t).getUnitProduction();
			}
			else
			{
				remainingUnitProduction = 0;
				for (final Territory purchaseTerritory : purchaseTerritories.keySet())
				{
					for (final ProPlaceTerritory ppt : purchaseTerritories.get(purchaseTerritory).getCanPlaceTerritories())
					{
						if (placeTerritory.equals(ppt))
						{
							if (purchaseUtils.canTerritoryUsePurchaseOption(player, bestDefenseOption, t))
								remainingUnitProduction += purchaseTerritories.get(purchaseTerritory).getRemainingUnitProduction();
						}
					}
				}
			}
			LogUtils.log(Level.FINEST, t + ", remainingUnitProduction=" + remainingUnitProduction);
			
			// Purchase defenders and check battle results
			int PUsSpent = 0;
			final List<Unit> unitsToPlace = new ArrayList<Unit>();
			ProBattleResultData finalResult = new ProBattleResultData();
			while (true)
			{
				// If out of PUs or production then break
				if (bestDefenseOption == null || bestDefenseOption.getCost() > (PUsRemaining - PUsSpent) || remainingUnitProduction < bestDefenseOption.getQuantity())
					break;
				
				// Create new temp defenders
				PUsSpent += bestDefenseOption.getCost();
				remainingUnitProduction -= bestDefenseOption.getQuantity();
				unitsToPlace.addAll(bestDefenseOption.getUnitType().create(bestDefenseOption.getQuantity(), player, true));
				
				// Find current battle result
				final Set<Unit> enemyAttackingUnits = new HashSet<Unit>(enemyAttackMap.get(t).getMaxUnits());
				enemyAttackingUnits.addAll(enemyAttackMap.get(t).getMaxAmphibUnits());
				final List<Unit> defenders = new ArrayList<Unit>(placeTerritory.getDefendingUnits());
				defenders.addAll(unitsToPlace);
				finalResult = battleUtils.calculateBattleResults(player, t, new ArrayList<Unit>(enemyAttackingUnits), defenders, false);
				
				// Break if it can be held
				if ((!t.equals(myCapital) && !finalResult.isHasLandUnitRemaining() && finalResult.getTUVSwing() <= 0) ||
							(t.equals(myCapital) && finalResult.getWinPercentage() < (100 - WIN_PERCENTAGE) && finalResult.getTUVSwing() <= 0))
					break;
			}
			
			// Check to see if its worth trying to defend the territory
			if (!finalResult.isHasLandUnitRemaining() || (finalResult.getTUVSwing() - PUsSpent / 2) < placeTerritory.getMinBattleResult().getTUVSwing() || t.equals(myCapital))
			{
				PUsRemaining -= PUsSpent;
				LogUtils.log(Level.FINEST, t + ", placedUnits=" + unitsToPlace + ", TUVSwing=" + finalResult.getTUVSwing());
				addUnitsToPlaceTerritory(placeTerritory, unitsToPlace, purchaseTerritories);
			}
			else
			{
				setCantHoldPlaceTerritory(placeTerritory, purchaseTerritories);
				LogUtils.log(Level.FINEST, t + ", unable to defend with placedUnits=" + unitsToPlace + ", TUVSwing=" + finalResult.getTUVSwing() + ", minTUVSwing="
							+ placeTerritory.getMinBattleResult().getTUVSwing() + ", PUsSpent=" + PUsSpent);
			}
		}
		
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
				if (!t.isWater() && placeTerritory.getStrategicValue() >= 0.25 && placeTerritory.isCanHold())
				{
					final boolean hasEnemyNeighbors = !data.getMap().getNeighbors(t, ProMatches.territoryIsEnemyLand(player, data)).isEmpty();
					final Set<Territory> nearbyLandTerritories = data.getMap().getNeighbors(t, 9, ProMatches.territoryCanMoveLandUnits(player, data, false));
					final int numNearbyEnemyTerritories = Match.countMatches(nearbyLandTerritories, Matches.isTerritoryEnemy(player, data));
					final boolean hasLocalLandSuperiority = battleUtils.territoryHasLocalLandSuperiority(t, 2, player);
					if (hasEnemyNeighbors || numNearbyEnemyTerritories >= 3 || !hasLocalLandSuperiority)
						prioritizedLandTerritories.add(placeTerritory);
				}
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
	
	private int purchaseAAUnits(final Map<Territory, ProPurchaseTerritory> purchaseTerritories, final Map<Territory, ProAttackTerritoryData> enemyAttackMap,
				final List<ProPlaceTerritory> prioritizedLandTerritories, int PUsRemaining, final List<ProPurchaseOption> specialPurchaseOptions)
	{
		if (PUsRemaining == 0)
			return PUsRemaining;
		LogUtils.log(Level.FINE, "Purchase AA units with PUsRemaining=" + PUsRemaining);
		
		// Loop through prioritized territories and purchase AA units
		for (final ProPlaceTerritory placeTerritory : prioritizedLandTerritories)
		{
			final Territory t = placeTerritory.getTerritory();
			LogUtils.log(Level.FINER, "Checking AA place for " + t);
			
			// Check if any enemy attackers
			if (enemyAttackMap.get(t) == null)
				continue;
			
			// Check remaining production
			int remainingUnitProduction = purchaseTerritories.get(t).getRemainingUnitProduction();
			LogUtils.log(Level.FINER, t + ", remainingUnitProduction=" + remainingUnitProduction);
			if (remainingUnitProduction <= 0)
				continue;
			
			// Check if territory needs AA
			final boolean enemyCanBomb = Match.someMatch(enemyAttackMap.get(t).getMaxUnits(), Matches.UnitIsStrategicBomber);
			final boolean territoryCanBeBombed = t.getUnits().someMatch(Matches.UnitCanProduceUnitsAndCanBeDamaged);
			final boolean hasAABombingDefense = t.getUnits().someMatch(Matches.UnitIsAAforBombingThisUnitOnly);
			LogUtils.log(Level.FINER, t + ", enemyCanBomb=" + enemyCanBomb + ", territoryCanBeBombed=" + territoryCanBeBombed + ", hasAABombingDefense=" + hasAABombingDefense);
			if (!enemyCanBomb || !territoryCanBeBombed || hasAABombingDefense)
				continue;
			
			// Determine most cost efficient units that can be produced in this territory
			final List<ProPurchaseOption> purchaseOptionsForTerritory = purchaseUtils.findPurchaseOptionsForTerritory(player, specialPurchaseOptions, t);
			ProPurchaseOption bestAAOption = null;
			int minCost = Integer.MAX_VALUE;
			for (final ProPurchaseOption ppo : purchaseOptionsForTerritory)
			{
				final boolean isAAForBombing = Matches.UnitTypeIsAAforBombingThisUnitOnly.match(ppo.getUnitType());
				if (ppo.getCost() <= PUsRemaining && isAAForBombing && ppo.getQuantity() <= remainingUnitProduction && ppo.getCost() < minCost
							&& !Matches.UnitTypeHasMaxBuildRestrictions.match(ppo.getUnitType()) && !Matches.UnitTypeConsumesUnitsOnCreation.match(ppo.getUnitType()))
				{
					bestAAOption = ppo;
					minCost = ppo.getCost();
				}
			}
			
			// Check if there aren't any available units
			if (bestAAOption == null)
				continue;
			LogUtils.log(Level.FINEST, "Best AA unit: " + bestAAOption.getUnitType().getName());
			
			// Create new temp units
			PUsRemaining -= bestAAOption.getCost();
			remainingUnitProduction -= bestAAOption.getQuantity();
			final List<Unit> unitsToPlace = bestAAOption.getUnitType().create(bestAAOption.getQuantity(), player, true);
			placeTerritory.getPlaceUnits().addAll(unitsToPlace);
			LogUtils.log(Level.FINEST, t + ", placedUnits=" + unitsToPlace);
		}
		
		return PUsRemaining;
	}
	
	private int purchaseLandUnits(final Map<Territory, ProPurchaseTerritory> purchaseTerritories, final Map<Territory, ProAttackTerritoryData> enemyAttackMap,
				final List<ProPlaceTerritory> prioritizedLandTerritories, int PUsRemaining, final List<ProPurchaseOption> landPurchaseOptions)
	{
		if (PUsRemaining == 0)
			return PUsRemaining;
		LogUtils.log(Level.FINE, "Purchase land units with PUsRemaining=" + PUsRemaining);
		
		// Loop through prioritized territories and purchase land units
		for (final ProPlaceTerritory placeTerritory : prioritizedLandTerritories)
		{
			final Territory t = placeTerritory.getTerritory();
			LogUtils.log(Level.FINER, "Checking land place for " + t.getName());
			
			// Check remaining production
			int remainingUnitProduction = purchaseTerritories.get(t).getRemainingUnitProduction();
			LogUtils.log(Level.FINER, t + ", remainingUnitProduction=" + remainingUnitProduction);
			if (remainingUnitProduction <= 0)
				continue;
			
			// Determine most cost efficient units that can be produced in this territory
			final List<ProPurchaseOption> purchaseOptionsForTerritory = purchaseUtils.findPurchaseOptionsForTerritory(player, landPurchaseOptions, t);
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
			
			// Check if there aren't any available units
			if (bestHitPointOption == null || bestAttackOption == null)
				continue;
			LogUtils.log(Level.FINEST, "Best hit point unit: " + bestHitPointOption.getUnitType().getName());
			LogUtils.log(Level.FINEST, "Best attack unit:" + bestAttackOption.getUnitType().getName());
			if (bestTwoMoveOption != null)
				LogUtils.log(Level.FINEST, "Best two move unit: " + bestTwoMoveOption.getUnitType().getName());
			if (bestThreeMoveOption != null)
				LogUtils.log(Level.FINEST, "Best three move unit: " + bestThreeMoveOption.getUnitType().getName());
			
			// Get optimal unit combo based on distance from enemy
			int enemyDistance = utils.getClosestEnemyLandTerritoryDistance(data, player, t);
			if (enemyDistance <= 0)
				enemyDistance = 9;
			int hitPointPercent = 65;
			int attackPercent = 35;
			int twoMovePercent = 0;
			int threeMovePercent = 0;
			if (bestThreeMoveOption != null && enemyDistance > 4)
				threeMovePercent = 50;
			else if (bestTwoMoveOption != null)
				twoMovePercent = Math.min(50, 10 * enemyDistance);
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
	
	private int purchaseFactory(final Map<Territory, ProPurchaseTerritory> factoryPurchaseTerritories, final Map<Territory, ProAttackTerritoryData> enemyAttackMap, int PUsRemaining,
				final List<ProPurchaseOption> factoryPurchaseOptions, final Map<Territory, ProPurchaseTerritory> purchaseTerritories, final List<ProPlaceTerritory> prioritizedLandTerritories,
				final List<ProPurchaseOption> landPurchaseOptions, final boolean allowSeaFactoryPurchase)
	{
		if (PUsRemaining == 0)
			return PUsRemaining;
		LogUtils.log(Level.FINE, "Purchase factory with PUsRemaining=" + PUsRemaining + ", purchaseSeaFactory=" + allowSeaFactoryPurchase);
		
		// Only try to purchase a factory if all production was used in prioritized land territories
		for (final ProPlaceTerritory placeTerritory : prioritizedLandTerritories)
		{
			for (final Territory t : purchaseTerritories.keySet())
			{
				if (placeTerritory.getTerritory().equals(t) && purchaseTerritories.get(t).getRemainingUnitProduction() > 0)
				{
					LogUtils.log(Level.FINER, "Not purchasing a factory since remaining land production in " + t);
					return PUsRemaining;
				}
			}
		}
		
		// Find all owned land territories that weren't conquered and don't already have a factory
		final List<Territory> possibleFactoryTerritories = Match.getMatches(data.getMap().getTerritories(), ProMatches.territoryHasNoInfraFactoryAndIsNotConqueredOwnedLand(player, data));
		possibleFactoryTerritories.removeAll(factoryPurchaseTerritories.keySet());
		final Set<Territory> purchaseFactoryTerritories = new HashSet<Territory>();
		final List<Territory> territoriesThatCantBeHeld = new ArrayList<Territory>();
		for (final Territory t : possibleFactoryTerritories)
		{
			// Only consider territories with production of 3 or more
			final int production = TerritoryAttachment.get(t).getProduction();
			if (production <= 2)
				continue;
			
			// Check if no enemy attackers and that it wasn't conquered this turn
			if (enemyAttackMap.get(t) == null)
			{
				purchaseFactoryTerritories.add(t);
				LogUtils.log(Level.FINEST, "Possible factory since no enemy attackers: " + t.getName());
			}
			else
			{
				// Find current battle result
				final List<Unit> defenders = t.getUnits().getMatches(Matches.isUnitAllied(player, data));
				final Set<Unit> enemyAttackingUnits = new HashSet<Unit>(enemyAttackMap.get(t).getMaxUnits());
				enemyAttackingUnits.addAll(enemyAttackMap.get(t).getMaxAmphibUnits());
				final ProBattleResultData result = battleUtils.estimateDefendBattleResults(player, t, new ArrayList<Unit>(enemyAttackingUnits), defenders);
				
				// Check if it can't be held or if it can then that it wasn't conquered this turn
				if (result.isHasLandUnitRemaining() || result.getTUVSwing() > 0)
				{
					territoriesThatCantBeHeld.add(t);
					LogUtils.log(Level.FINEST, "Can't hold territory: " + t.getName() + ", hasLandUnitRemaining=" + result.isHasLandUnitRemaining() + ", TUVSwing=" + result.getTUVSwing()
								+ ", enemyAttackers=" + enemyAttackingUnits.size() + ", myDefenders=" + defenders.size());
				}
				else
				{
					purchaseFactoryTerritories.add(t);
					LogUtils.log(Level.FINEST, "Possible factory: " + t.getName() + ", hasLandUnitRemaining=" + result.isHasLandUnitRemaining() + ", TUVSwing=" + result.getTUVSwing()
								+ ", enemyAttackers=" + enemyAttackingUnits.size() + ", myDefenders=" + defenders.size());
				}
			}
		}
		LogUtils.log(Level.FINER, "Possible factory territories: " + purchaseFactoryTerritories);
		
		// Remove any territories that don't have local land superiority
		if (!allowSeaFactoryPurchase)
		{
			for (final Iterator<Territory> it = purchaseFactoryTerritories.iterator(); it.hasNext();)
			{
				final Territory t = it.next();
				if (!battleUtils.territoryHasLocalLandSuperiority(t, 3, player, purchaseTerritories))
					it.remove();
			}
			LogUtils.log(Level.FINER, "Possible factory territories that have land superiority: " + purchaseFactoryTerritories);
		}
		
		// Find strategic value for each territory
		final Map<Territory, Double> territoryValueMap = territoryValueUtils.findTerritoryValues(player, territoriesThatCantBeHeld);
		double maxValue = 0.0;
		Territory maxTerritory = null;
		for (final Territory t : purchaseFactoryTerritories)
		{
			final int production = TerritoryAttachment.get(t).getProduction();
			final double value = territoryValueMap.get(t) * production + 0.1 * production;
			final boolean isAdjacentToSea = Matches.territoryHasNeighborMatching(data, Matches.TerritoryIsWater).match(t);
			final Set<Territory> nearbyLandTerritories = data.getMap().getNeighbors(t, 9, ProMatches.territoryCanMoveLandUnits(player, data, false));
			final int numNearbyEnemyTerritories = Match.countMatches(nearbyLandTerritories, Matches.isTerritoryEnemy(player, data));
			LogUtils.log(Level.FINEST, t + ", strategic value=" + territoryValueMap.get(t) + ", value=" + value + ", numNearbyEnemyTerritories=" + numNearbyEnemyTerritories);
			if (value > maxValue && ((numNearbyEnemyTerritories >= 4 && territoryValueMap.get(t) >= 0.25)
						|| (isAdjacentToSea && allowSeaFactoryPurchase)))
			{
				maxValue = value;
				maxTerritory = t;
			}
		}
		LogUtils.log(Level.FINER, "Try to purchase factory for territory: " + maxTerritory);
		
		// Determine whether to purchase factory
		if (maxTerritory != null)
		{
			// Determine units that can be produced in this territory
			final List<ProPurchaseOption> purchaseOptionsForTerritory = purchaseUtils.findPurchaseOptionsForTerritory(player, factoryPurchaseOptions, maxTerritory);
			
			// Find most expensive placed land unit to consider removing for a factory
			int maxPlacedCost = 0;
			ProPlaceTerritory maxPlacedTerritory = null;
			Unit maxPlacedUnit = null;
			for (final ProPlaceTerritory placeTerritory : prioritizedLandTerritories)
			{
				for (final Unit u : placeTerritory.getPlaceUnits())
				{
					for (final ProPurchaseOption ppo : landPurchaseOptions)
					{
						if (u.getType().equals(ppo.getUnitType()) && ppo.getQuantity() == 1 && ppo.getCost() >= maxPlacedCost)
						{
							maxPlacedCost = ppo.getCost();
							maxPlacedTerritory = placeTerritory;
							maxPlacedUnit = u;
						}
					}
				}
			}
			
			// Determine most expensive factory option (currently doesn't buy mobile factories)
			ProPurchaseOption bestFactoryOption = null;
			double maxFactoryEfficiency = 0;
			for (final ProPurchaseOption ppo : purchaseOptionsForTerritory)
			{
				if (ppo.getCost() <= (PUsRemaining + maxPlacedCost) && ppo.getMovement() == 0)
				{
					if (ppo.getCost() > maxFactoryEfficiency)
					{
						bestFactoryOption = ppo;
						maxFactoryEfficiency = ppo.getCost();
					}
				}
			}
			
			// Check if there are enough PUs to buy a factory
			if (bestFactoryOption != null)
			{
				LogUtils.log(Level.FINER, "Best factory unit: " + bestFactoryOption.getUnitType().getName());
				
				final ProPurchaseTerritory factoryPurchaseTerritory = new ProPurchaseTerritory(maxTerritory, data, player);
				factoryPurchaseTerritories.put(maxTerritory, factoryPurchaseTerritory);
				for (final ProPlaceTerritory ppt : factoryPurchaseTerritory.getCanPlaceTerritories())
				{
					if (ppt.getTerritory().equals(maxTerritory))
					{
						final List<Unit> factory = bestFactoryOption.getUnitType().create(bestFactoryOption.getQuantity(), player, true);
						ppt.getPlaceUnits().addAll(factory);
						if (PUsRemaining >= bestFactoryOption.getCost())
						{
							PUsRemaining -= bestFactoryOption.getCost();
							LogUtils.log(Level.FINER, maxTerritory + ", placedFactory=" + factory);
						}
						else
						{
							PUsRemaining -= (bestFactoryOption.getCost() - maxPlacedCost);
							maxPlacedTerritory.getPlaceUnits().remove(maxPlacedUnit);
							LogUtils.log(Level.FINER, maxTerritory + ", placedFactory=" + factory + ", removedUnit=" + maxPlacedUnit);
						}
					}
				}
			}
		}
		
		return PUsRemaining;
	}
	
	private List<ProPlaceTerritory> prioritizeSeaTerritories(final Map<Territory, ProPurchaseTerritory> purchaseTerritories)
	{
		LogUtils.log(Level.FINE, "Prioritize sea territories");
		
		// Determine which sea territories can be placed in
		final Set<ProPlaceTerritory> seaPlaceTerritories = new HashSet<ProPlaceTerritory>();
		for (final ProPurchaseTerritory ppt : purchaseTerritories.values())
		{
			for (final ProPlaceTerritory placeTerritory : ppt.getCanPlaceTerritories())
			{
				final Territory t = placeTerritory.getTerritory();
				if (t.isWater() && placeTerritory.getStrategicValue() > 0 && placeTerritory.isCanHold())
					seaPlaceTerritories.add(placeTerritory);
			}
		}
		
		// Calculate value of territory
		// final IntegerMap<UnitType> playerCostMap = BattleCalculator.getCostsForTUV(player, data);
		LogUtils.log(Level.FINER, "Determine sea place value:");
		for (final ProPlaceTerritory placeTerritory : seaPlaceTerritories)
		{
			// Calculate defense value for prioritization
			final List<Unit> units = new ArrayList<Unit>(placeTerritory.getDefendingUnits());
			units.addAll(placeTerritory.getPlaceUnits());
			final List<Unit> myUnits = Match.getMatches(units, Matches.unitIsOwnedBy(player));
			final int numMyTransports = Match.countMatches(myUnits, Matches.UnitIsTransport);
			final int numSeaDefenders = Match.countMatches(units, Matches.UnitIsNotTransport);
			// final int defendingUnitValue = BattleCalculator.getTUV(placeTerritory.getDefendingUnits(), playerCostMap);
			final double territoryValue = placeTerritory.getStrategicValue() * (1 + numMyTransports + 0.1 * numSeaDefenders);
			LogUtils.log(Level.FINER, placeTerritory.toString() + ", seaValue=" + placeTerritory.getStrategicValue() + ", strategicValue=" + placeTerritory.getStrategicValue() + ", numMyTransports="
						+ numMyTransports + ", numSeaDefenders=" + numSeaDefenders);
			placeTerritory.setStrategicValue(territoryValue);
		}
		
		// Sort territories by value
		final List<ProPlaceTerritory> sortedTerritories = new ArrayList<ProPlaceTerritory>(seaPlaceTerritories);
		Collections.sort(sortedTerritories, new Comparator<ProPlaceTerritory>()
		{
			public int compare(final ProPlaceTerritory t1, final ProPlaceTerritory t2)
			{
				final double value1 = t1.getStrategicValue();
				final double value2 = t2.getStrategicValue();
				return Double.compare(value2, value1);
			}
		});
		LogUtils.log(Level.FINER, "Sorted sea territories:");
		for (final ProPlaceTerritory placeTerritory : sortedTerritories)
			LogUtils.log(Level.FINER, placeTerritory.toString() + " seaValue=" + placeTerritory.getStrategicValue());
		
		return sortedTerritories;
	}
	
	private int purchaseSeaUnits(final Map<Territory, ProPurchaseTerritory> purchaseTerritories, final Map<Territory, ProAttackTerritoryData> enemyAttackMap,
				final List<ProPlaceTerritory> prioritizedSeaTerritories, final Map<Territory, Double> territoryValueMap, int PUsRemaining, final List<ProPurchaseOption> seaPurchaseOptions,
				final List<ProPurchaseOption> landPurchaseOptions)
	{
		if (PUsRemaining == 0)
			return PUsRemaining;
		LogUtils.log(Level.FINE, "Purchase sea units with PUsRemaining=" + PUsRemaining);
		
		// Loop through prioritized territories and purchase sea units
		for (final ProPlaceTerritory placeTerritory : prioritizedSeaTerritories)
		{
			final Territory t = placeTerritory.getTerritory();
			LogUtils.log(Level.FINER, "Checking sea place for " + t.getName());
			
			// Determine units that can be produced in this territory
			final List<ProPurchaseOption> seaPurchaseOptionsForTerritory = purchaseUtils.findPurchaseOptionsForTerritory(player, seaPurchaseOptions, t);
			final Set<ProPurchaseOption> landPurchaseOptionsForTerritory = new HashSet<ProPurchaseOption>();
			for (final Territory purchaseTerritory : purchaseTerritories.keySet())
			{
				for (final ProPlaceTerritory ppt : purchaseTerritories.get(purchaseTerritory).getCanPlaceTerritories())
				{
					// If place territory is equal to the current place territory
					if (placeTerritory.equals(ppt) && purchaseTerritories.get(purchaseTerritory).getRemainingUnitProduction() > 0)
						landPurchaseOptionsForTerritory.addAll(purchaseUtils.findPurchaseOptionsForTerritory(player, landPurchaseOptions, purchaseTerritory));
				}
			}
			
			// Determine most cost efficient sea units
			ProPurchaseOption bestDefenseOption = null;
			double maxDefenseEfficiency = 0;
			for (final ProPurchaseOption ppo : seaPurchaseOptionsForTerritory)
			{
				if (ppo.getCost() <= PUsRemaining)
				{
					if (!ppo.isSub() && ppo.getDefenseEfficiency() * ppo.getMovement() > maxDefenseEfficiency)
					{
						bestDefenseOption = ppo;
						maxDefenseEfficiency = ppo.getDefenseEfficiency() * ppo.getMovement();
					}
				}
			}
			
			// Check if there aren't enough PUs to buy any units
			if (bestDefenseOption == null)
				continue;
			LogUtils.log(Level.FINEST, "Best sea defense unit: " + bestDefenseOption.getUnitType().getName());
			
			// Find remaining defense production
			int remainingDefenseProduction = 0;
			for (final Territory purchaseTerritory : purchaseTerritories.keySet())
			{
				for (final ProPlaceTerritory ppt : purchaseTerritories.get(purchaseTerritory).getCanPlaceTerritories())
				{
					if (placeTerritory.equals(ppt))
					{
						if (purchaseUtils.canTerritoryUsePurchaseOption(player, bestDefenseOption, t))
							remainingDefenseProduction += purchaseTerritories.get(purchaseTerritory).getRemainingUnitProduction();
					}
				}
			}
			LogUtils.log(Level.FINEST, t + ", remainingDefenseProduction=" + remainingDefenseProduction);
			
			// If any enemy attackers then purchase sea defenders until it can be held
			int usedProduction = 0;
			final List<Unit> unitsToPlace = new ArrayList<Unit>();
			if (enemyAttackMap.get(t) != null)
			{
				int PUsSpent = 0;
				final List<Unit> initialDefendingUnits = new ArrayList<Unit>(placeTerritory.getDefendingUnits());
				initialDefendingUnits.addAll(getPlaceUnits(t, purchaseTerritories));
				ProBattleResultData finalResult = battleUtils.calculateBattleResults(player, t, enemyAttackMap.get(t).getMaxUnits(), initialDefendingUnits, false);
				while (true)
				{
					// If out of PUs or production then break
					if (bestDefenseOption == null || bestDefenseOption.getCost() > (PUsRemaining - PUsSpent) || (remainingDefenseProduction - usedProduction) < bestDefenseOption.getQuantity())
						break;
					
					// If it can be held then break
					if (finalResult.getTUVSwing() < -1 || finalResult.getWinPercentage() < WIN_PERCENTAGE)
						break;
					
					// Create new temp defenders
					PUsSpent += bestDefenseOption.getCost();
					usedProduction += bestDefenseOption.getQuantity();
					unitsToPlace.addAll(bestDefenseOption.getUnitType().create(bestDefenseOption.getQuantity(), player, true));
					LogUtils.log(Level.FINEST, t + ", added sea defender for defense: " + bestDefenseOption.getUnitType().getName() + ", TUVSwing=" + finalResult.getTUVSwing() + ", win%="
								+ finalResult.getWinPercentage());
					
					// Find current battle result
					final List<Unit> defendingUnits = new ArrayList<Unit>(placeTerritory.getDefendingUnits());
					defendingUnits.addAll(getPlaceUnits(t, purchaseTerritories));
					defendingUnits.addAll(unitsToPlace);
					finalResult = battleUtils.estimateDefendBattleResults(player, t, enemyAttackMap.get(t).getMaxUnits(), defendingUnits);
				}
				
				// Check to see if its worth trying to defend the territory
				if (finalResult.getTUVSwing() < 0 || finalResult.getWinPercentage() < WIN_PERCENTAGE)
				{
					PUsRemaining -= PUsSpent;
				}
				else
				{
					LogUtils.log(Level.FINEST, t + ", can't defend TUVSwing=" + finalResult.getTUVSwing() + ", win%=" + finalResult.getWinPercentage() + ", tried to placeDefenders=" + unitsToPlace
								+ ", enemyAttackers=" + enemyAttackMap.get(t).getMaxUnits());
					continue;
				}
			}
			
			// Find nearby naval/air units
			int landDistance = utils.getClosestEnemyLandTerritoryDistanceOverWater(data, player, t);
			if (landDistance <= 0)
				landDistance = 10;
			final int enemyDistance = Math.max(4, (landDistance + 2));
			final int alliedDistance = enemyDistance / 2;
			final Set<Territory> nearbyTerritories = data.getMap().getNeighbors(t, enemyDistance);
			final List<Territory> nearbyLandTerritories = Match.getMatches(nearbyTerritories, Matches.TerritoryIsLand);
			final Set<Territory> nearbyEnemySeaTerritories = data.getMap().getNeighbors(t, enemyDistance, Matches.TerritoryIsWater);
			nearbyEnemySeaTerritories.add(t);
			final Set<Territory> nearbyAlliedSeaTerritories = data.getMap().getNeighbors(t, alliedDistance, Matches.TerritoryIsWater);
			nearbyAlliedSeaTerritories.add(t);
			final List<Unit> enemyUnitsInSeaTerritories = new ArrayList<Unit>();
			final List<Unit> enemyUnitsInLandTerritories = new ArrayList<Unit>();
			final List<Unit> myUnitsInSeaTerritories = new ArrayList<Unit>();
			final List<Unit> alliedUnitsInSeaTerritories = new ArrayList<Unit>();
			for (final Territory nearbyLandTerritory : nearbyLandTerritories)
				enemyUnitsInLandTerritories.addAll(nearbyLandTerritory.getUnits().getMatches(ProMatches.unitIsEnemyAir(player, data)));
			for (final Territory nearbySeaTerritory : nearbyEnemySeaTerritories)
			{
				final List<Unit> enemySeaUnits = nearbySeaTerritory.getUnits().getMatches(ProMatches.unitIsEnemyNotLand(player, data));
				if (enemySeaUnits.isEmpty())
					continue;
				final Route route = data.getMap().getRoute_IgnoreEnd(t, nearbySeaTerritory, Matches.TerritoryIsWater);
				if (route == null)
					continue;
				if (MoveValidator.validateCanal(route, enemySeaUnits, enemySeaUnits.get(0).getOwner(), data) != null)
					continue;
				final int routeLength = route.numberOfSteps();
				if (routeLength <= enemyDistance)
					enemyUnitsInSeaTerritories.addAll(enemySeaUnits);
			}
			for (final Territory nearbySeaTerritory : nearbyAlliedSeaTerritories)
			{
				myUnitsInSeaTerritories.addAll(nearbySeaTerritory.getUnits().getMatches(ProMatches.unitIsOwnedNotLand(player, data)));
				myUnitsInSeaTerritories.addAll(getPlaceUnits(nearbySeaTerritory, purchaseTerritories));
				alliedUnitsInSeaTerritories.addAll(nearbySeaTerritory.getUnits().getMatches(ProMatches.unitIsAlliedNotOwned(player, data)));
			}
			LogUtils.log(Level.FINEST, t + ", enemyDistance=" + enemyDistance + ", alliedDistance=" + alliedDistance + ", enemyAirUnits=" + enemyUnitsInLandTerritories
						+ ", enemySeaUnits=" + enemyUnitsInSeaTerritories + ", mySeaUnits=" + myUnitsInSeaTerritories);
			
			// Purchase naval defenders unit I have local naval superiority
			while (true)
			{
				// If out of PUs or production then break
				if (bestDefenseOption == null || bestDefenseOption.getCost() > PUsRemaining || (remainingDefenseProduction - usedProduction) < bestDefenseOption.getQuantity())
					break;
				
				// Find current naval defense strength
				final List<Unit> myUnits = new ArrayList<Unit>(myUnitsInSeaTerritories);
				myUnits.addAll(unitsToPlace);
				myUnits.addAll(alliedUnitsInSeaTerritories);
				final List<Unit> enemyAttackers = new ArrayList<Unit>(enemyUnitsInSeaTerritories);
				enemyAttackers.addAll(enemyUnitsInLandTerritories);
				final double defenseStrengthDifference = battleUtils.estimateStrengthDifference(t, enemyAttackers, myUnits);
				LogUtils.log(Level.FINEST, t + ", current enemy naval attack strengthDifference=" + defenseStrengthDifference + ", enemySize=" + enemyAttackers.size() + ", alliedSize="
							+ myUnits.size());
				
				// Find current naval attack strength
				double attackStrengthDifference = battleUtils.estimateStrengthDifference(t, myUnits, enemyUnitsInSeaTerritories);
				attackStrengthDifference += 0.5 * battleUtils.estimateStrengthDifference(t, alliedUnitsInSeaTerritories, enemyUnitsInSeaTerritories);
				LogUtils.log(Level.FINEST, t + ", current allied naval attack strengthDifference=" + attackStrengthDifference + ", alliedSize=" + myUnits.size() + ", enemySize="
							+ enemyUnitsInSeaTerritories.size());
				
				// If I have naval attack/defense superiority then break
				if (defenseStrengthDifference < 50 && attackStrengthDifference > 50)
					break;
				
				// Create new temp units
				unitsToPlace.addAll(bestDefenseOption.getUnitType().create(bestDefenseOption.getQuantity(), player, true));
				PUsRemaining -= bestDefenseOption.getCost();
				usedProduction += bestDefenseOption.getQuantity();
				LogUtils.log(Level.FINEST, t + ", added sea defender for naval superiority: " + bestDefenseOption.getUnitType().getName());
			}
			
			// Add sea defender units to place territory
			addUnitsToPlaceTerritory(placeTerritory, unitsToPlace, purchaseTerritories);
			
			// Determine most cost efficient transport units
			ProPurchaseOption bestTransportOption = null;
			double maxTransportEfficiency = 0;
			for (final ProPurchaseOption ppo : seaPurchaseOptionsForTerritory)
			{
				if (ppo.getCost() <= PUsRemaining)
				{
					if (ppo.getTransportEfficiency() * ppo.getMovement() > maxTransportEfficiency)
					{
						bestTransportOption = ppo;
						maxTransportEfficiency = ppo.getTransportEfficiency() * ppo.getMovement();
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
			
			// Check if there aren't enough PUs to buy any amphib units
			if (bestAmphibOption == null)
				continue;
			LogUtils.log(Level.FINEST, "Best amphib unit: " + bestAmphibOption.getUnitType().getName());
			if (bestTransportOption != null)
				LogUtils.log(Level.FINEST, "Best transport unit: " + bestTransportOption.getUnitType().getName());
			
			// Loop through adjacent purchase territories and purchase transport/amphib units
			int distance = 2;
			if (bestTransportOption != null)
				distance = bestTransportOption.getMovement();
			final List<Unit> unitsToIgnore = new ArrayList<Unit>();
			for (final Territory purchaseTerritory : purchaseTerritories.keySet())
			{
				// Get place territory for the current factory
				ProPlaceTerritory purchasePlaceTerritory = null;
				for (final ProPlaceTerritory ppt : purchaseTerritories.get(purchaseTerritory).getCanPlaceTerritories())
				{
					if (purchaseTerritory.equals(ppt.getTerritory()))
						purchasePlaceTerritory = ppt;
				}
				
				// Loop through place territories to find matching sea zone
				for (final ProPlaceTerritory ppt : purchaseTerritories.get(purchaseTerritory).getCanPlaceTerritories())
				{
					if (placeTerritory.equals(ppt))
					{
						// Check if territory can produce units and has remaining production
						final int remainingUnitProduction = purchaseTerritories.get(purchaseTerritory).getRemainingUnitProduction();
						if (remainingUnitProduction <= 0)
							break;
						
						// Find transports that need loaded
						final List<Unit> transportsThatNeedUnits = new ArrayList<Unit>();
						final Set<Territory> seaTerritories = data.getMap().getNeighbors(purchaseTerritory, distance, ProMatches.territoryCanMoveSeaUnits(player, data, false));
						for (final Territory seaTerritory : seaTerritories)
						{
							final List<Unit> unitsInTerritory = getPlaceUnits(seaTerritory, purchaseTerritories);
							unitsInTerritory.addAll(seaTerritory.getUnits().getUnits());
							final List<Unit> transports = Match.getMatches(unitsInTerritory, ProMatches.unitIsOwnedTransport(player));
							for (final Unit transport : transports)
							{
								if (unitsToIgnore.contains(transport))
									continue;
								
								final Set<Territory> territoriesToLoadFrom = new HashSet<Territory>(data.getMap().getNeighbors(seaTerritory, distance));
								for (final Iterator<Territory> it = territoriesToLoadFrom.iterator(); it.hasNext();)
								{
									final Territory potentialTerritory = it.next();
									if (potentialTerritory.isWater() || territoryValueMap.get(potentialTerritory) > 0.25)
										it.remove();
								}
								final List<Unit> units = transportUtils.getUnitsToTransportFromTerritories(player, transport, territoriesToLoadFrom, unitsToIgnore);
								if (units.isEmpty())
								{
									transportsThatNeedUnits.add(transport);
								}
								else
								{
									unitsToIgnore.add(transport);
									unitsToIgnore.addAll(units);
								}
							}
						}
						LogUtils.log(Level.FINEST, t + ", unitsToIgnore=" + unitsToIgnore);
						
						// Determine whether transports, amphib units, or both are needed
						int transportCapacity = 0;
						for (final Unit transport : transportsThatNeedUnits)
							transportCapacity += UnitAttachment.get(transport.getType()).getTransportCapacity() / bestAmphibOption.getTransportCost();
						final List<Unit> potentialUnitsToLoad = new ArrayList<Unit>();
						final Set<Territory> landNeighbors = data.getMap().getNeighbors(t, Matches.TerritoryIsLand);
						for (final Territory neighbor : landNeighbors)
						{
							if (territoryValueMap.get(neighbor) <= 0.25)
							{
								final List<Unit> unitsInTerritory = new ArrayList<Unit>(neighbor.getUnits().getUnits());
								unitsInTerritory.addAll(getPlaceUnits(neighbor, purchaseTerritories));
								potentialUnitsToLoad.addAll(Match.getMatches(unitsInTerritory, ProMatches.unitIsOwnedCombatTransportableUnit(player)));
							}
						}
						potentialUnitsToLoad.removeAll(unitsToIgnore);
						int numNeededUnits = transportCapacity - potentialUnitsToLoad.size();
						LogUtils.log(Level.FINEST, t + ", numNeededAmphibUnits=" + numNeededUnits + " = " + transportCapacity + " - " + potentialUnitsToLoad.size());
						
						// Purchase transports and amphib units
						final List<Unit> amphibUnitsToPlace = new ArrayList<Unit>();
						final List<Unit> transportUnitsToPlace = new ArrayList<Unit>();
						Unit loadingTransport = null;
						int neededUnitsForTransport = 0;
						int bestTransportOptionUnits = 1;
						if (bestTransportOption != null)
							bestTransportOptionUnits = bestTransportOption.getTransportCapacity() * bestTransportOption.getQuantity() / bestAmphibOption.getTransportCost();
						while (true)
						{
							if (!transportsThatNeedUnits.isEmpty() && loadingTransport == null)
							{
								loadingTransport = transportsThatNeedUnits.get(0);
								neededUnitsForTransport = UnitAttachment.get(loadingTransport.getType()).getTransportCapacity() / bestAmphibOption.getTransportCost();
							}
							
							// Find current purchase option
							ProPurchaseOption ppo = bestTransportOption;
							if (bestTransportOption != null && numNeededUnits < 0 && bestTransportOption.getCost() <= PUsRemaining
										&& (remainingUnitProduction - usedProduction) >= bestTransportOption.getQuantity()
										&& purchaseUtils.canTerritoryUsePurchaseOption(player, bestTransportOption, t))
							{
								numNeededUnits += bestTransportOptionUnits;
								final List<Unit> transports = ppo.getUnitType().create(ppo.getQuantity(), player, true);
								transportUnitsToPlace.addAll(transports);
								transportsThatNeedUnits.addAll(transports);
							}
							else if (numNeededUnits >= -bestTransportOptionUnits && bestAmphibOption.getCost() <= PUsRemaining
										&& (remainingUnitProduction - usedProduction) >= bestAmphibOption.getQuantity()
										&& purchaseUtils.canTerritoryUsePurchaseOption(player, bestAmphibOption, purchaseTerritory))
							{
								ppo = bestAmphibOption;
								numNeededUnits -= bestAmphibOption.getQuantity();
								neededUnitsForTransport -= bestAmphibOption.getQuantity();
								amphibUnitsToPlace.addAll(ppo.getUnitType().create(ppo.getQuantity(), player, true));
								if (neededUnitsForTransport <= 0)
								{
									transportsThatNeedUnits.remove(loadingTransport);
									unitsToIgnore.add(loadingTransport);
									loadingTransport = null;
								}
							}
							else
							{
								break;
							}
							
							// Create new temp units
							PUsRemaining -= ppo.getCost();
							usedProduction += ppo.getQuantity();
						}
						
						// Add transport units to place territory and amphib units to factory
						ppt.getPlaceUnits().addAll(transportUnitsToPlace);
						purchasePlaceTerritory.getPlaceUnits().addAll(amphibUnitsToPlace);
						LogUtils.log(Level.FINEST, t + ", transportUnitsToPlace=" + transportUnitsToPlace + ", purchaseTerritory=" + purchaseTerritory + ", amphibUnitsToPlace=" + amphibUnitsToPlace);
					}
				}
			}
		}
		
		return PUsRemaining;
	}
	
	private int purchaseAttackUnitsWithRemainingProduction(final Map<Territory, ProPurchaseTerritory> purchaseTerritories, int PUsRemaining, final List<ProPurchaseOption> landPurchaseOptions,
				final List<ProPurchaseOption> airPurchaseOptions)
	{
		if (PUsRemaining == 0)
			return PUsRemaining;
		LogUtils.log(Level.FINE, "Purchase attack units in territories with remaining production with PUsRemaining=" + PUsRemaining);
		
		// Get all safe land place territories with remaining production
		final List<ProPlaceTerritory> prioritizedLandTerritories = new ArrayList<ProPlaceTerritory>();
		for (final ProPurchaseTerritory ppt : purchaseTerritories.values())
		{
			for (final ProPlaceTerritory placeTerritory : ppt.getCanPlaceTerritories())
			{
				final Territory t = placeTerritory.getTerritory();
				if (!t.isWater() && placeTerritory.isCanHold() && purchaseTerritories.get(t).getRemainingUnitProduction() > 0)
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
		LogUtils.log(Level.FINER, "Sorted land territories with remaining production: " + prioritizedLandTerritories);
		
		// Loop through territories and purchase long range attack units
		for (final ProPlaceTerritory placeTerritory : prioritizedLandTerritories)
		{
			final Territory t = placeTerritory.getTerritory();
			LogUtils.log(Level.FINER, "Checking territory: " + t);
			
			// Determine units that can be produced in this territory
			final List<ProPurchaseOption> airAndLandPurchaseOptions = new ArrayList<ProPurchaseOption>(airPurchaseOptions);
			airAndLandPurchaseOptions.addAll(landPurchaseOptions);
			final List<ProPurchaseOption> purchaseOptionsForTerritory = purchaseUtils.findPurchaseOptionsForTerritory(player, airAndLandPurchaseOptions, t);
			
			// Purchase long range attack units for any remaining production
			int remainingUnitProduction = purchaseTerritories.get(t).getRemainingUnitProduction();
			while (true)
			{
				// Determine best long range attack option (prefer air units)
				ProPurchaseOption bestAttackOption = null;
				double maxAttackEfficiency = 0;
				for (final ProPurchaseOption ppo : purchaseOptionsForTerritory)
				{
					if (ppo.getCost() <= PUsRemaining && ppo.getQuantity() <= remainingUnitProduction)
					{
						double attackEfficiency = ppo.getAttackEfficiency() * ppo.getMovement() / ppo.getQuantity();
						if (ppo.isAir())
							attackEfficiency *= 10;
						if (attackEfficiency > maxAttackEfficiency)
						{
							bestAttackOption = ppo;
							maxAttackEfficiency = attackEfficiency;
						}
					}
				}
				if (bestAttackOption == null)
					break;
				
				// Purchase unit
				PUsRemaining -= bestAttackOption.getCost();
				remainingUnitProduction -= bestAttackOption.getQuantity();
				final List<Unit> newUnit = bestAttackOption.getUnitType().create(bestAttackOption.getQuantity(), player, true);
				placeTerritory.getPlaceUnits().addAll(newUnit);
				LogUtils.log(Level.FINEST, t + ", addedUnit=" + newUnit);
			}
		}
		
		return PUsRemaining;
	}
	
	private int upgradeUnitsWithRemainingPUs(final Map<Territory, ProPurchaseTerritory> purchaseTerritories, int PUsRemaining, final List<ProPurchaseOption> landPurchaseOptions,
				final List<ProPurchaseOption> airPurchaseOptions)
	{
		if (PUsRemaining == 0)
			return PUsRemaining;
		LogUtils.log(Level.FINE, "Upgrade units with PUsRemaining=" + PUsRemaining);
		
		// Get all safe land place territories
		final List<ProPlaceTerritory> prioritizedLandTerritories = new ArrayList<ProPlaceTerritory>();
		for (final ProPurchaseTerritory ppt : purchaseTerritories.values())
		{
			for (final ProPlaceTerritory placeTerritory : ppt.getCanPlaceTerritories())
			{
				final Territory t = placeTerritory.getTerritory();
				if (!t.isWater() && placeTerritory.isCanHold())
					prioritizedLandTerritories.add(placeTerritory);
			}
		}
		
		// Sort territories by ascending value (try upgrading units in far away territories first)
		Collections.sort(prioritizedLandTerritories, new Comparator<ProPlaceTerritory>()
		{
			public int compare(final ProPlaceTerritory t1, final ProPlaceTerritory t2)
			{
				final double value1 = t1.getStrategicValue();
				final double value2 = t2.getStrategicValue();
				return Double.compare(value1, value2);
			}
		});
		LogUtils.log(Level.FINER, "Sorted land territories: " + prioritizedLandTerritories);
		
		// Loop through territories and upgrade units to long range attack units
		for (final ProPlaceTerritory placeTerritory : prioritizedLandTerritories)
		{
			final Territory t = placeTerritory.getTerritory();
			LogUtils.log(Level.FINER, "Checking territory: " + t);
			
			// Determine units that can be produced in this territory
			final List<ProPurchaseOption> airAndLandPurchaseOptions = new ArrayList<ProPurchaseOption>(airPurchaseOptions);
			airAndLandPurchaseOptions.addAll(landPurchaseOptions);
			final List<ProPurchaseOption> purchaseOptionsForTerritory = purchaseUtils.findPurchaseOptionsForTerritory(player, airAndLandPurchaseOptions, t);
			
			// Purchase long range attack units for any remaining production
			int remainingUpgradeUnits = purchaseTerritories.get(t).getUnitProduction() / 3;
			while (true)
			{
				if (remainingUpgradeUnits <= 0)
					break;
				
				// Find cheapest placed purchase option
				int minPlacedCost = Integer.MAX_VALUE;
				ProPurchaseOption minPurchaseOption = null;
				for (final Unit u : placeTerritory.getPlaceUnits())
				{
					for (final ProPurchaseOption ppo : airAndLandPurchaseOptions)
					{
						if (u.getType().equals(ppo.getUnitType()) && ppo.getCost() < minPlacedCost)
						{
							minPlacedCost = ppo.getCost();
							minPurchaseOption = ppo;
						}
					}
				}
				if (minPurchaseOption == null)
					break;
				
				// Determine best long range attack option (prefer air units)
				ProPurchaseOption bestAttackOption = null;
				double maxAttackEfficiency = minPurchaseOption.getAttackEfficiency() * minPurchaseOption.getMovement() * minPurchaseOption.getCost() / minPurchaseOption.getQuantity();
				for (final ProPurchaseOption ppo : purchaseOptionsForTerritory)
				{
					if (ppo.getCost() > minPlacedCost && ppo.getCost() <= (PUsRemaining + minPlacedCost) && ppo.getQuantity() == 1)
					{
						double attackEfficiency = ppo.getAttackEfficiency() * ppo.getMovement() * ppo.getCost() / ppo.getQuantity();
						if (ppo.isAir())
							attackEfficiency *= 10;
						if (attackEfficiency > maxAttackEfficiency)
						{
							bestAttackOption = ppo;
							maxAttackEfficiency = attackEfficiency;
						}
					}
				}
				if (bestAttackOption == null)
					break;
				
				// Find units to remove
				final List<Unit> unitsToRemove = new ArrayList<Unit>();
				int numUnitsToRemove = minPurchaseOption.getQuantity();
				for (final Unit u : placeTerritory.getPlaceUnits())
				{
					if (numUnitsToRemove <= 0)
						break;
					if (u.getType().equals(minPurchaseOption.getUnitType()))
					{
						unitsToRemove.add(u);
						numUnitsToRemove--;
					}
				}
				if (numUnitsToRemove > 0)
					break;
				
				// Replace units
				PUsRemaining += minPurchaseOption.getCost();
				remainingUpgradeUnits -= minPurchaseOption.getQuantity();
				placeTerritory.getPlaceUnits().removeAll(unitsToRemove);
				LogUtils.log(Level.FINEST, t + ", removedUnits=" + unitsToRemove);
				for (int i = 0; i < unitsToRemove.size(); i++)
				{
					if (PUsRemaining >= bestAttackOption.getCost())
					{
						PUsRemaining -= bestAttackOption.getCost();
						final List<Unit> newUnit = bestAttackOption.getUnitType().create(bestAttackOption.getQuantity(), player, true);
						placeTerritory.getPlaceUnits().addAll(newUnit);
						LogUtils.log(Level.FINEST, t + ", addedUnit=" + newUnit);
					}
				}
			}
		}
		
		return PUsRemaining;
	}
	
	private IntegerMap<ProductionRule> populateProductionRuleMap(final Map<Territory, ProPurchaseTerritory> purchaseTerritories, final List<ProPurchaseOption> landPurchaseOptions,
				final List<ProPurchaseOption> airPurchaseOptions, final List<ProPurchaseOption> seaPurchaseOptions, final List<ProPurchaseOption> factoryPurchaseOptions,
				final List<ProPurchaseOption> specialPurchaseOptions)
	{
		LogUtils.log(Level.FINE, "Populate production rule map");
		
		final List<ProPurchaseOption> purchaseOptions = new ArrayList<ProPurchaseOption>(landPurchaseOptions);
		purchaseOptions.addAll(airPurchaseOptions);
		purchaseOptions.addAll(seaPurchaseOptions);
		purchaseOptions.addAll(factoryPurchaseOptions);
		purchaseOptions.addAll(specialPurchaseOptions);
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
	
	private void placeDefenders(final Map<Territory, ProPurchaseTerritory> placeNonConstructionTerritories, final Map<Territory, ProAttackTerritoryData> enemyAttackMap,
				final List<ProPlaceTerritory> needToDefendTerritories, final IAbstractPlaceDelegate placeDelegate)
	{
		LogUtils.log(Level.FINE, "Place defenders with units=" + player.getUnits().getUnits());
		
		// Loop through prioritized territories and purchase defenders
		for (final ProPlaceTerritory placeTerritory : needToDefendTerritories)
		{
			final Territory t = placeTerritory.getTerritory();
			LogUtils.log(Level.FINER, "Placing defenders for " + t.getName() + ", enemyAttackers=" + enemyAttackMap.get(t).getMaxUnits() + ", amphibEnemyAttackers="
						+ enemyAttackMap.get(t).getMaxAmphibUnits() + ", defenders=" + placeTerritory.getDefendingUnits());
			
			// Check if any units can be placed
			final PlaceableUnits placeableUnits = placeDelegate.getPlaceableUnits(player.getUnits().getUnits(), t);
			if (placeableUnits.isError())
			{
				LogUtils.log(Level.FINEST, t + " can't place units with error: " + placeableUnits.getErrorMessage());
				continue;
			}
			
			// Find remaining unit production
			int remainingUnitProduction = placeableUnits.getMaxUnits();
			if (remainingUnitProduction == -1)
				remainingUnitProduction = Integer.MAX_VALUE;
			LogUtils.log(Level.FINEST, t + ", remainingUnitProduction=" + remainingUnitProduction);
			
			// Place defenders and check battle results
			final List<Unit> unitsThatCanBePlaced = new ArrayList<Unit>(placeableUnits.getUnits());
			final int landPlaceCount = Math.min(remainingUnitProduction, unitsThatCanBePlaced.size());
			final List<Unit> unitsToPlace = new ArrayList<Unit>();
			ProBattleResultData finalResult = new ProBattleResultData();
			for (int i = 0; i < landPlaceCount; i++)
			{
				// Add defender
				unitsToPlace.add(unitsThatCanBePlaced.get(i));
				
				// Find current battle result
				final Set<Unit> enemyAttackingUnits = new HashSet<Unit>(enemyAttackMap.get(t).getMaxUnits());
				enemyAttackingUnits.addAll(enemyAttackMap.get(t).getMaxAmphibUnits());
				final List<Unit> defenders = new ArrayList<Unit>(placeTerritory.getDefendingUnits());
				defenders.addAll(unitsToPlace);
				finalResult = battleUtils.calculateBattleResults(player, t, new ArrayList<Unit>(enemyAttackingUnits), defenders, false);
				
				// Break if it can be held
				if ((!t.equals(myCapital) && !finalResult.isHasLandUnitRemaining() && finalResult.getTUVSwing() <= 0) ||
							(t.equals(myCapital) && finalResult.getWinPercentage() < (100 - WIN_PERCENTAGE) && finalResult.getTUVSwing() <= 0))
					break;
			}
			
			// Check to see if its worth trying to defend the territory
			if (!finalResult.isHasLandUnitRemaining() || finalResult.getTUVSwing() < placeTerritory.getMinBattleResult().getTUVSwing() || t.equals(myCapital))
			{
				LogUtils.log(Level.FINEST, t + ", placedUnits=" + unitsToPlace + ", TUVSwing=" + finalResult.getTUVSwing());
				doPlace(t, unitsToPlace, placeDelegate);
			}
			else
			{
				setCantHoldPlaceTerritory(placeTerritory, placeNonConstructionTerritories);
				LogUtils.log(Level.FINEST, t + ", unable to defend with placedUnits=" + unitsToPlace + ", TUVSwing=" + finalResult.getTUVSwing() + ", minTUVSwing="
							+ placeTerritory.getMinBattleResult().getTUVSwing());
			}
		}
	}
	
	private void placeLandUnits(final Map<Territory, ProPurchaseTerritory> placeNonConstructionTerritories, final Map<Territory, ProAttackTerritoryData> enemyAttackMap,
				final List<ProPlaceTerritory> prioritizedLandTerritories, final IAbstractPlaceDelegate placeDelegate)
	{
		LogUtils.log(Level.FINE, "Place land units=" + player.getUnits().getUnits());
		
		// Loop through prioritized territories and purchase land units
		for (final ProPlaceTerritory placeTerritory : prioritizedLandTerritories)
		{
			final Territory t = placeTerritory.getTerritory();
			LogUtils.log(Level.FINER, "Checking land place for " + t.getName());
			
			// Check if any units can be placed
			final PlaceableUnits placeableUnits = placeDelegate.getPlaceableUnits(player.getUnits().getUnits(), t);
			if (placeableUnits.isError())
			{
				LogUtils.log(Level.FINEST, t + " can't place units with error: " + placeableUnits.getErrorMessage());
				continue;
			}
			
			// Find remaining unit production
			int remainingUnitProduction = placeableUnits.getMaxUnits();
			if (remainingUnitProduction == -1)
				remainingUnitProduction = Integer.MAX_VALUE;
			LogUtils.log(Level.FINEST, t + ", remainingUnitProduction=" + remainingUnitProduction);
			
			// Purchase as many units as possible
			final List<Unit> unitsThatCanBePlaced = new ArrayList<Unit>(placeableUnits.getUnits());
			final int landPlaceCount = Math.min(remainingUnitProduction, unitsThatCanBePlaced.size());
			final List<Unit> unitsToPlace = unitsThatCanBePlaced.subList(0, landPlaceCount);
			LogUtils.log(Level.FINEST, t + ", placedUnits=" + unitsToPlace);
			doPlace(t, unitsToPlace, placeDelegate);
		}
	}
	
	private void addUnitsToPlaceTerritory(final ProPlaceTerritory placeTerritory, final List<Unit> unitsToPlace, final Map<Territory, ProPurchaseTerritory> purchaseTerritories)
	{
		// Add units to place territory
		for (final Territory purchaseTerritory : purchaseTerritories.keySet())
		{
			for (final ProPlaceTerritory ppt : purchaseTerritories.get(purchaseTerritory).getCanPlaceTerritories())
			{
				// If place territory is equal to the current place territory and has remaining production
				if (placeTerritory.equals(ppt) && purchaseTerritories.get(purchaseTerritory).getRemainingUnitProduction() > 0)
				{
					// Place max number of units
					final int numUnits = Math.min(purchaseTerritories.get(purchaseTerritory).getRemainingUnitProduction(), unitsToPlace.size());
					final List<Unit> units = unitsToPlace.subList(0, numUnits);
					ppt.getPlaceUnits().addAll(units);
					units.clear();
				}
			}
		}
	}
	
	private void setCantHoldPlaceTerritory(final ProPlaceTerritory placeTerritory, final Map<Territory, ProPurchaseTerritory> purchaseTerritories)
	{
		// Add units to place territory
		for (final Territory purchaseTerritory : purchaseTerritories.keySet())
		{
			for (final ProPlaceTerritory ppt : purchaseTerritories.get(purchaseTerritory).getCanPlaceTerritories())
			{
				// If place territory is equal to the current place territory
				if (placeTerritory.equals(ppt))
					ppt.setCanHold(false);
			}
		}
	}
	
	private List<Unit> getPlaceUnits(final Territory t, final Map<Territory, ProPurchaseTerritory> purchaseTerritories)
	{
		final List<Unit> placeUnits = new ArrayList<Unit>();
		for (final Territory purchaseTerritory : purchaseTerritories.keySet())
		{
			for (final ProPlaceTerritory ppt : purchaseTerritories.get(purchaseTerritory).getCanPlaceTerritories())
			{
				if (t.equals(ppt.getTerritory()))
					placeUnits.addAll(ppt.getPlaceUnits());
			}
		}
		return placeUnits;
	}
	
	private void doPlace(final Territory t, final Collection<Unit> toPlace, final IAbstractPlaceDelegate del)
	{
		for (final Unit unit : toPlace)
		{
			final List<Unit> unitList = new ArrayList<Unit>();
			unitList.add(unit);
			final String message = del.placeUnits(unitList, t);
			if (message != null)
			{
				LogUtils.log(Level.WARNING, message);
				LogUtils.log(Level.WARNING, "Attempt was at: " + t + " with: " + unit);
			}
		}
		utils.pause();
	}
	
}
