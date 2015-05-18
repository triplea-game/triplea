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
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.ai.proAI.util.LogUtils;
import games.strategy.triplea.ai.proAI.util.ProAttackOptionsUtils;
import games.strategy.triplea.ai.proAI.util.ProBattleUtils;
import games.strategy.triplea.ai.proAI.util.ProMatches;
import games.strategy.triplea.ai.proAI.util.ProMetricUtils;
import games.strategy.triplea.ai.proAI.util.ProMoveUtils;
import games.strategy.triplea.ai.proAI.util.ProPurchaseUtils;
import games.strategy.triplea.ai.proAI.util.ProTerritoryValueUtils;
import games.strategy.triplea.ai.proAI.util.ProTransportUtils;
import games.strategy.triplea.ai.proAI.util.ProUtils;
import games.strategy.triplea.ai.strongAI.SUtils;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.BattleCalculator;
import games.strategy.triplea.delegate.BattleDelegate;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveValidator;
import games.strategy.triplea.delegate.dataObjects.PlaceableUnits;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.util.CompositeMatch;
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
	private GameData startOfTurnData; // Used to count current units on map for maxBuiltPerPlayer
	private PlayerID player;
	private Territory myCapital;
	private double minCostPerHitPoint;
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
	
	public void bid(int PUsToSpend, final IPurchaseDelegate purchaseDelegate, final GameData data, final PlayerID player)
	{
		LogUtils.log(Level.FINE, "Starting bid purchase phase");
		
		// Current data at the start of combat move
		this.data = data;
		this.player = player;
		myCapital = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
		allTerritories = data.getMap().getTerritories();
		
		if (PUsToSpend == 0 && player.getResources().getQuantity(data.getResourceList().getResource(Constants.PUS)) == 0) // Check whether the player has ANY PU's to spend...
			return;
		
		// breakdown Rules by type and cost
		int highPrice = 0;
		final List<ProductionRule> rules = player.getProductionFrontier().getRules();
		final IntegerMap<ProductionRule> purchase = new IntegerMap<ProductionRule>();
		final List<ProductionRule> landProductionRules = new ArrayList<ProductionRule>();
		final List<ProductionRule> airProductionRules = new ArrayList<ProductionRule>();
		final List<ProductionRule> seaProductionRules = new ArrayList<ProductionRule>();
		final List<ProductionRule> transportProductionRules = new ArrayList<ProductionRule>();
		final List<ProductionRule> subProductionRules = new ArrayList<ProductionRule>();
		final IntegerMap<ProductionRule> bestAttack = new IntegerMap<ProductionRule>();
		final IntegerMap<ProductionRule> bestDefense = new IntegerMap<ProductionRule>();
		final IntegerMap<ProductionRule> bestTransport = new IntegerMap<ProductionRule>();
		final IntegerMap<ProductionRule> bestMaxUnits = new IntegerMap<ProductionRule>();
		final IntegerMap<ProductionRule> bestMobileAttack = new IntegerMap<ProductionRule>();
		// ProductionRule highRule = null;
		ProductionRule carrierRule = null, fighterRule = null;
		int carrierFighterLimit = 0, maxFighterAttack = 0;
		float averageSeaMove = 0;
		final Resource pus = data.getResourceList().getResource(Constants.PUS);
		final boolean isAmphib = isAmphibAttack(player, true);
		for (final ProductionRule ruleCheck : rules)
		{
			final int costCheck = ruleCheck.getCosts().getInt(pus);
			final NamedAttachable resourceOrUnit = ruleCheck.getResults().keySet().iterator().next();
			if (!(resourceOrUnit instanceof UnitType))
				continue;
			final UnitType x = (UnitType) resourceOrUnit;
			// Remove from consideration any unit with Zero Movement
			if (UnitAttachment.get(x).getMovement(player) < 1 && !(UnitAttachment.get(x).getCanProduceUnits()))
				continue;
			// Remove from consideration any unit with Zero defense, or 3 or more attack/defense than defense/attack, that is not a transport/factory/aa unit
			if (((UnitAttachment.get(x).getAttack(player) - UnitAttachment.get(x).getDefense(player) >= 3 || UnitAttachment.get(x).getDefense(player) - UnitAttachment.get(x).getAttack(player) >= 3) || UnitAttachment
						.get(x).getDefense(player) < 1)
						&& !(UnitAttachment.get(x).getCanProduceUnits() || (UnitAttachment.get(x).getTransportCapacity() > 0 && Matches.UnitTypeIsSea.match(x))))
			{
				// maybe the map only has weird units. make sure there is at least one of each type before we decide not to use it (we are relying on the fact that map makers generally put specialty units AFTER useful units in their production lists [ie: bombers listed after fighters, mortars after artillery, etc.])
				if (Matches.UnitTypeIsAir.match(x) && !airProductionRules.isEmpty())
					continue;
				if (Matches.UnitTypeIsSea.match(x) && !seaProductionRules.isEmpty())
					continue;
				if (!Matches.UnitTypeCanProduceUnits.match(x) && !landProductionRules.isEmpty() && !Matches.UnitTypeIsAir.match(x) && !Matches.UnitTypeIsSea.match(x))
					continue;
			}
			// Remove from consideration any unit which has maxBuiltPerPlayer
			if (Matches.UnitTypeHasMaxBuildRestrictions.match(x))
				continue;
			// Remove from consideration any unit which has consumesUnits
			if (Matches.UnitTypeConsumesUnitsOnCreation.match(x))
				continue;
			if (Matches.UnitTypeIsAir.match(x))
			{
				airProductionRules.add(ruleCheck);
			}
			else if (Matches.UnitTypeIsSea.match(x))
			{
				seaProductionRules.add(ruleCheck);
				averageSeaMove += UnitAttachment.get(x).getMovement(player);
			}
			else if (!Matches.UnitTypeCanProduceUnits.match(x))
			{
				if (costCheck > highPrice)
				{
					highPrice = costCheck;
				}
				landProductionRules.add(ruleCheck);
			}
			if (Matches.UnitTypeCanTransport.match(x) && Matches.UnitTypeIsSea.match(x))
			{
				// might be more than 1 transport rule... use ones that can hold at least "2" capacity (we should instead check for median transport cost, and then add all those at or above that capacity)
				if (UnitAttachment.get(x).getTransportCapacity() > 1)
					transportProductionRules.add(ruleCheck);
			}
			if (Matches.UnitTypeIsSub.match(x))
				subProductionRules.add(ruleCheck);
			if (Matches.UnitTypeIsCarrier.match(x)) // might be more than 1 carrier rule...use the one which will hold the most fighters
			{
				final int thisFighterLimit = UnitAttachment.get(x).getCarrierCapacity();
				if (thisFighterLimit >= carrierFighterLimit)
				{
					carrierRule = ruleCheck;
					carrierFighterLimit = thisFighterLimit;
				}
			}
			if (Matches.UnitTypeCanLandOnCarrier.match(x)) // might be more than 1 fighter...use the one with the best attack
			{
				final int thisFighterAttack = UnitAttachment.get(x).getAttack(player);
				if (thisFighterAttack > maxFighterAttack)
				{
					fighterRule = ruleCheck;
					maxFighterAttack = thisFighterAttack;
				}
			}
		}
		if (averageSeaMove / seaProductionRules.size() >= 1.8) // most sea units move at least 2 movement, so remove any sea units with 1 movement (dumb t-boats) (some maps like 270BC have mostly 1 movement sea units, so we must be sure not to remove those)
		{
			final List<ProductionRule> seaProductionRulesCopy = new ArrayList<ProductionRule>(seaProductionRules);
			for (final ProductionRule seaRule : seaProductionRulesCopy)
			{
				final NamedAttachable resourceOrUnit = seaRule.getResults().keySet().iterator().next();
				if (!(resourceOrUnit instanceof UnitType))
					continue;
				final UnitType x = (UnitType) resourceOrUnit;
				if (UnitAttachment.get(x).getMovement(player) < 2)
					seaProductionRules.remove(seaRule);
			}
		}
		if (subProductionRules.size() > 0 && seaProductionRules.size() > 0)
		{
			if (subProductionRules.size() / seaProductionRules.size() < 0.3) // remove submarines from consideration, unless we are mostly subs
			{
				seaProductionRules.removeAll(subProductionRules);
			}
		}
		
		int buyLimit = PUsToSpend / 3;
		if (buyLimit == 0)
			buyLimit = 1;
		boolean landPurchase = true, goTransports = false;
		// boolean alreadyBought = false;
		final List<Territory> enemyTerritoryBorderingOurTerrs = SUtils.getNeighboringEnemyLandTerritories(data, player);
		if (enemyTerritoryBorderingOurTerrs.isEmpty())
			landPurchase = false;
		if (Math.random() > 0.25)
			seaProductionRules.removeAll(subProductionRules);
		if (PUsToSpend < 25)
		{
			if ((!isAmphib || Math.random() < 0.15) && landPurchase)
			{
				SUtils.findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack, landProductionRules, PUsToSpend, buyLimit, data, player, 2);
			}
			else
			{
				landPurchase = false;
				buyLimit = PUsToSpend / 5; // assume a larger threshhold
				if (Math.random() > 0.40)
				{
					SUtils.findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack, seaProductionRules, PUsToSpend, buyLimit, data, player, 2);
				}
				else
				{
					goTransports = true;
				}
			}
		}
		else if ((!isAmphib || Math.random() < 0.15) && landPurchase)
		{
			if (Math.random() > 0.80)
			{
				SUtils.findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack, landProductionRules, PUsToSpend, buyLimit, data, player, 2);
			}
		}
		else if (Math.random() < 0.35)
		{
			if (Math.random() > 0.55 && carrierRule != null && fighterRule != null)
			{// force a carrier purchase if enough available $$ for it and at least 1 fighter
				final int cost = carrierRule.getCosts().getInt(pus);
				final int fighterCost = fighterRule.getCosts().getInt(pus);
				if ((cost + fighterCost) <= PUsToSpend)
				{
					purchase.add(carrierRule, 1);
					purchase.add(fighterRule, 1);
					carrierFighterLimit--;
					PUsToSpend -= (cost + fighterCost);
					while ((PUsToSpend >= fighterCost) && carrierFighterLimit > 0)
					{ // max out the carrier
						purchase.add(fighterRule, 1);
						carrierFighterLimit--;
						PUsToSpend -= fighterCost;
					}
				}
			}
			final int airPUs = PUsToSpend / 6;
			SUtils.findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack, airProductionRules, airPUs, buyLimit, data, player, 2);
			final boolean buyAttack = Math.random() > 0.50;
			for (final ProductionRule rule1 : airProductionRules)
			{
				int buyThese = bestAttack.getInt(rule1);
				final int cost = rule1.getCosts().getInt(pus);
				if (!buyAttack)
					buyThese = bestDefense.getInt(rule1);
				PUsToSpend -= cost * buyThese;
				while (PUsToSpend < 0 && buyThese > 0)
				{
					buyThese--;
					PUsToSpend += cost;
				}
				if (buyThese > 0)
					purchase.add(rule1, buyThese);
			}
			final int landPUs = PUsToSpend;
			buyLimit = landPUs / 3;
			bestAttack.clear();
			bestDefense.clear();
			bestMaxUnits.clear();
			bestMobileAttack.clear();
			SUtils.findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack, landProductionRules, landPUs, buyLimit, data, player, 2);
		}
		else
		{
			landPurchase = false;
			buyLimit = PUsToSpend / 8; // assume higher end purchase
			seaProductionRules.addAll(airProductionRules);
			if (Math.random() > 0.45)
				SUtils.findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack, seaProductionRules, PUsToSpend, buyLimit, data, player, 2);
			else
			{
				goTransports = true;
			}
		}
		final List<ProductionRule> processRules = new ArrayList<ProductionRule>();
		if (landPurchase)
			processRules.addAll(landProductionRules);
		else
		{
			if (goTransports)
				processRules.addAll(transportProductionRules);
			else
				processRules.addAll(seaProductionRules);
		}
		final boolean buyAttack = Math.random() > 0.25;
		int buyThese = 0, numBought = 0;
		for (final ProductionRule rule1 : processRules)
		{
			final int cost = rule1.getCosts().getInt(pus);
			if (goTransports)
				buyThese = PUsToSpend / cost;
			else if (buyAttack)
				buyThese = bestAttack.getInt(rule1);
			else if (Math.random() <= 0.25)
				buyThese = bestDefense.getInt(rule1);
			else
				buyThese = bestMaxUnits.getInt(rule1);
			PUsToSpend -= cost * buyThese;
			while (buyThese > 0 && PUsToSpend < 0)
			{
				buyThese--;
				PUsToSpend += cost;
			}
			if (buyThese > 0)
			{
				numBought += buyThese;
				purchase.add(rule1, buyThese);
			}
		}
		bestAttack.clear();
		bestDefense.clear();
		bestTransport.clear();
		bestMaxUnits.clear();
		bestMobileAttack.clear();
		if (PUsToSpend > 0) // verify a run through the land units
		{
			buyLimit = PUsToSpend / 2;
			SUtils.findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack, landProductionRules, PUsToSpend, buyLimit, data, player, 2);
			for (final ProductionRule rule2 : landProductionRules)
			{
				final int cost = rule2.getCosts().getInt(pus);
				buyThese = bestDefense.getInt(rule2);
				PUsToSpend -= cost * buyThese;
				while (buyThese > 0 && PUsToSpend < 0)
				{
					buyThese--;
					PUsToSpend += cost;
				}
				if (buyThese > 0)
					purchase.add(rule2, buyThese);
			}
		}
		purchaseDelegate.purchase(purchase);
	}
	
	private boolean isAmphibAttack(final PlayerID player, final boolean requireWaterFactory)
	{
		final Territory capitol = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
		if (capitol == null || !capitol.getOwner().equals(player))
			return false;
		if (requireWaterFactory)
		{
			final List<Territory> factories = SUtils.findTersWithUnitsMatching(data, player, Matches.UnitCanProduceUnits);
			final List<Territory> waterFactories = SUtils.stripLandLockedTerr(data, factories);
			if (waterFactories.isEmpty())
				return false;
		}
		// find a land route to an enemy territory from our capitol
		boolean amphibPlayer = !SUtils.hasLandRouteToEnemyOwnedCapitol(capitol, player, data);
		int totProduction = 0, allProduction = 0;
		if (amphibPlayer)
		{
			final List<Territory> allFactories = SUtils.findTersWithUnitsMatching(data, player, Matches.UnitCanProduceUnits);
			// allFactories.remove(capitol);
			for (final Territory checkFactory : allFactories)
			{
				final boolean isLandRoute = SUtils.hasLandRouteToEnemyOwnedCapitol(checkFactory, player, data);
				final int factProduction = TripleAUnit.getProductionPotentialOfTerritory(checkFactory.getUnits().getUnits(), checkFactory, player, data, false, true);
				allProduction += factProduction;
				if (isLandRoute)
					totProduction += factProduction;
			}
		}
		// if the land based production is greater than 2/5 (used to be 1/3) of all factory production, turn off amphib
		// works better on NWO where Brits start with factories in North Africa
		amphibPlayer = amphibPlayer ? (totProduction * 5 < allProduction * 2) : false;
		return amphibPlayer;
	}
	
	public int repair(int PUsRemaining, final IPurchaseDelegate purchaseDelegate, final GameData data, final PlayerID player)
	{
		LogUtils.log(Level.FINE, "Repairing factories with PUsRemaining=" + PUsRemaining);
		
		// Current data at the start of combat move
		this.data = data;
		this.player = player;
		myCapital = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
		allTerritories = data.getMap().getTerritories();
		
		final CompositeMatch<Unit> ourFactories = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanProduceUnits, Matches.UnitIsInfrastructure);
		final List<Territory> rfactories = Match.getMatches(data.getMap().getTerritories(), ProMatches.territoryHasInfraFactoryAndIsNotConqueredOwnedLand(player, data));
		if (player.getRepairFrontier() != null && games.strategy.triplea.Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data)) // figure out if anything needs to be repaired
		{
			LogUtils.log(Level.FINER, "Factories can be damaged");
			
			final Map<Unit, Territory> unitsThatCanProduceNeedingRepair = new HashMap<Unit, Territory>();
			for (final Territory fixTerr : rfactories)
			{
				if (!Matches.territoryIsOwnedAndHasOwnedUnitMatching(data, player, Matches.UnitCanProduceUnitsAndCanBeDamaged).match(fixTerr))
					continue;
				final Unit possibleFactoryNeedingRepair = TripleAUnit.getBiggestProducer(Match.getMatches(fixTerr.getUnits().getUnits(), ourFactories), fixTerr, player, data, false);
				if (Matches.UnitHasTakenSomeBombingUnitDamage.match(possibleFactoryNeedingRepair))
					unitsThatCanProduceNeedingRepair.put(possibleFactoryNeedingRepair, fixTerr);
			}
			
			LogUtils.log(Level.FINER, "Factories that need repaired: " + unitsThatCanProduceNeedingRepair);
			
			final List<RepairRule> rrules = player.getRepairFrontier().getRules();
			for (final RepairRule rrule : rrules)
			{
				for (final Unit fixUnit : unitsThatCanProduceNeedingRepair.keySet())
				{
					if (fixUnit == null || !fixUnit.getType().equals(rrule.getResults().keySet().iterator().next()))
						continue;
					if (!Matches.territoryIsOwnedAndHasOwnedUnitMatching(data, player, Matches.UnitCanProduceUnitsAndCanBeDamaged).match(unitsThatCanProduceNeedingRepair.get(fixUnit)))
						continue;
					final TripleAUnit taUnit = (TripleAUnit) fixUnit;
					final int diff = taUnit.getUnitDamage();
					if (diff > 0)
					{
						final IntegerMap<RepairRule> repairMap = new IntegerMap<RepairRule>();
						repairMap.add(rrule, diff);
						final HashMap<Unit, IntegerMap<RepairRule>> repair = new HashMap<Unit, IntegerMap<RepairRule>>();
						repair.put(fixUnit, repairMap);
						PUsRemaining -= diff;
						LogUtils.log(Level.FINER, "Repairing factory=" + fixUnit + ", damage=" + diff + ", repairRule=" + rrule);
						purchaseDelegate.purchaseRepair(repair);
					}
				}
			}
		}
		return PUsRemaining;
	}
	
	public Map<Territory, ProPurchaseTerritory> purchase(int PUsRemaining, final IPurchaseDelegate purchaseDelegate, final GameData data, final GameData startOfTurnData, final PlayerID player)
	{
		LogUtils.log(Level.FINE, "Starting purchase phase with PUsRemaining=" + PUsRemaining);
		if (!player.getUnits().getUnits().isEmpty())
			LogUtils.log(Level.FINE, "Starting purchase phase with unplaced units=" + player.getUnits().getUnits());
		
		// Current data fields
		this.data = data;
		this.startOfTurnData = startOfTurnData;
		this.player = player;
		myCapital = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
		allTerritories = data.getMap().getTerritories();
		
		// Find all purchase options
		final ProPurchaseOptionMap purchaseOptions = new ProPurchaseOptionMap(player, data);
		minCostPerHitPoint = purchaseUtils.getMinCostPerHitPoint(player, purchaseOptions.getLandOptions());
		
		// Find all purchase/place territories
		final Map<Territory, ProPurchaseTerritory> purchaseTerritories = purchaseUtils.findPurchaseTerritories(player);
		final Set<Territory> placeTerritories = new HashSet<Territory>();
		placeTerritories.addAll(Match.getMatches(data.getMap().getTerritoriesOwnedBy(player), Matches.TerritoryIsLand));
		for (final Territory t : purchaseTerritories.keySet())
		{
			for (final ProPlaceTerritory ppt : purchaseTerritories.get(t).getCanPlaceTerritories())
				placeTerritories.add(ppt.getTerritory());
		}
		
		// Determine max enemy attack units and current allied defenders
		final Map<Territory, ProAttackTerritoryData> enemyAttackMap = new HashMap<Territory, ProAttackTerritoryData>();
		attackOptionsUtils.findMaxEnemyAttackUnits(player, new ArrayList<Territory>(), new ArrayList<Territory>(placeTerritories), enemyAttackMap);
		findDefendersInPlaceTerritories(purchaseTerritories);
		
		// Prioritize land territories that need defended and purchase additional defenders
		final List<ProPlaceTerritory> needToDefendLandTerritories = prioritizeTerritoriesToDefend(purchaseTerritories, enemyAttackMap, true);
		PUsRemaining = purchaseDefenders(purchaseTerritories, enemyAttackMap, needToDefendLandTerritories, PUsRemaining, purchaseOptions.getLandFodderOptions(), purchaseOptions.getAirOptions(), true);
		
		// Find strategic value for each territory
		LogUtils.log(Level.FINE, "Find strategic value for place territories");
		final Map<Territory, Double> territoryValueMap = territoryValueUtils.findTerritoryValues(player, minCostPerHitPoint, new ArrayList<Territory>(), new ArrayList<Territory>());
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
		PUsRemaining = purchaseAAUnits(purchaseTerritories, enemyAttackMap, prioritizedLandTerritories, PUsRemaining, purchaseOptions.getAAOptions());
		PUsRemaining = purchaseLandUnits(purchaseTerritories, enemyAttackMap, prioritizedLandTerritories, PUsRemaining, purchaseOptions);
		
		// Prioritize sea territories that need defended and purchase additional defenders
		final List<ProPlaceTerritory> needToDefendSeaTerritories = prioritizeTerritoriesToDefend(purchaseTerritories, enemyAttackMap, false);
		PUsRemaining = purchaseDefenders(purchaseTerritories, enemyAttackMap, needToDefendSeaTerritories, PUsRemaining, purchaseOptions.getSeaDefenseOptions(), purchaseOptions.getAirOptions(), false);
		
		// Determine whether to purchase new land factory
		final Map<Territory, ProPurchaseTerritory> factoryPurchaseTerritories = new HashMap<Territory, ProPurchaseTerritory>();
		PUsRemaining = purchaseFactory(factoryPurchaseTerritories, enemyAttackMap, PUsRemaining, purchaseTerritories, prioritizedLandTerritories, purchaseOptions, false);
		
		// Prioritize sea place options and purchase units
		final List<ProPlaceTerritory> prioritizedSeaTerritories = prioritizeSeaTerritories(purchaseTerritories);
		PUsRemaining = purchaseSeaAndAmphibUnits(purchaseTerritories, enemyAttackMap, prioritizedSeaTerritories, territoryValueMap, PUsRemaining, purchaseOptions);
		
		// Try to use any remaining PUs on high value units
		PUsRemaining = purchaseUnitsWithRemainingProduction(purchaseTerritories, PUsRemaining, purchaseOptions.getLandOptions(), purchaseOptions.getAirOptions());
		PUsRemaining = upgradeUnitsWithRemainingPUs(purchaseTerritories, PUsRemaining, purchaseOptions);
		
		// Try to purchase land/sea factory with extra PUs
		PUsRemaining = purchaseFactory(factoryPurchaseTerritories, enemyAttackMap, PUsRemaining, purchaseTerritories, prioritizedLandTerritories, purchaseOptions, true);
		
		// Add factory purchase territory to list if not empty
		if (!factoryPurchaseTerritories.isEmpty())
			purchaseTerritories.putAll(factoryPurchaseTerritories);
		
		// Determine final count of each production rule
		final IntegerMap<ProductionRule> purchaseMap = populateProductionRuleMap(purchaseTerritories, purchaseOptions);
		
		// Purchase units
		ProMetricUtils.collectPurchaseStats(purchaseMap);
		final String error = purchaseDelegate.purchase(purchaseMap);
		if (error != null)
			LogUtils.log(Level.WARNING, "Purchase error: " + error);
		
		return purchaseTerritories;
	}
	
	// TODO: Rewrite this as its from the Moore AI
	public void bidPlace(final Map<Territory, ProPurchaseTerritory> purchaseTerritories, final IAbstractPlaceDelegate placeDelegate, final GameData data, final PlayerID player)
	{
		LogUtils.log(Level.FINE, "Starting bid place phase");
		
		// if we have purchased a factory, it will be a priority for placing units
		// should place most expensive on it
		// need to be able to handle AA purchase
		if (player.getUnits().isEmpty())
			return;
		final Collection<Territory> impassableTerrs = new ArrayList<Territory>();
		for (final Territory t : data.getMap().getTerritories())
		{
			if (Matches.TerritoryIsPassableAndNotRestricted(player, data).invert().match(t) && Matches.TerritoryIsLand.match(t))
				impassableTerrs.add(t);
		}
		final BattleDelegate delegate = DelegateFinder.battleDelegate(data);
		final boolean tFirst = !games.strategy.triplea.Properties.getTransportCasualtiesRestricted(data);
		final CompositeMatch<Unit> ownedUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player));
		final CompositeMatch<Unit> attackUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsSea, Matches.UnitIsNotTransport);
		final CompositeMatch<Unit> transUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsTransport);
		final CompositeMatch<Unit> enemyUnit = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data));
		final CompositeMatch<Unit> enemyAttackUnit = new CompositeMatchAnd<Unit>(attackUnit, enemyUnit);
		// CompositeMatch<Unit> enemyTransUnit = new CompositeMatchAnd<Unit>(transUnit, enemyUnit);
		final CompositeMatch<Unit> ourFactory = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitCanProduceUnits);
		final CompositeMatch<Unit> landUnit = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitIsLand, Matches.UnitIsNotInfrastructure, Matches.UnitCanNotProduceUnits);
		// CompositeMatch<Territory> ourLandTerr = new CompositeMatchAnd<Territory>(Matches.isTerritoryOwnedBy(player), Matches.TerritoryIsLand);
		final Territory capitol = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
		final List<Territory> factoryTerritories = Match.getMatches(SUtils.findUnitTerr(data, player, ourFactory), Matches.isTerritoryOwnedBy(player));
		factoryTerritories.removeAll(impassableTerrs);
		/**
		 * Bid place with following criteria:
		 * 1) Has an enemy Neighbor
		 * 2) Has the largest combination value:
		 * a) enemy Terr
		 * b) our Terr
		 * c) other Terr neighbors to our Terr
		 * d) + 2 for each of these which are victory cities
		 */
		
		final List<Territory> ourFriendlyTerr = new ArrayList<Territory>();
		final List<Territory> ourEnemyTerr = new ArrayList<Territory>();
		final List<Territory> ourSemiRankedBidTerrs = new ArrayList<Territory>();
		final List<Territory> ourTerrs = SUtils.allOurTerritories(data, player);
		ourTerrs.remove(capitol); // we'll check the cap last
		final HashMap<Territory, Float> rankMap = SUtils.rankTerritories(data, ourFriendlyTerr, ourEnemyTerr, null, player, tFirst, false, true);
		final List<Territory> ourTerrWithEnemyNeighbors = SUtils.getTerritoriesWithEnemyNeighbor(data, player, false, false);
		SUtils.reorder(ourTerrWithEnemyNeighbors, rankMap, true);
		// ourFriendlyTerr.retainAll(ourTerrs);
		if (ourTerrWithEnemyNeighbors.contains(capitol))
		{
			ourTerrWithEnemyNeighbors.remove(capitol);
			ourTerrWithEnemyNeighbors.add(capitol); // move capitol to the end of the list, if it is touching enemies
		}
		Territory bidLandTerr = null;
		if (ourTerrWithEnemyNeighbors.size() > 0)
			bidLandTerr = ourTerrWithEnemyNeighbors.get(0);
		if (bidLandTerr == null)
			bidLandTerr = capitol;
		if (player.getUnits().someMatch(Matches.UnitIsSea))
		{
			Territory bidSeaTerr = null, bidTransTerr = null;
			// CompositeMatch<Territory> enemyWaterTerr = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasEnemyUnits(player, data));
			final CompositeMatch<Territory> waterFactoryWaterTerr = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasOwnedNeighborWithOwnedUnitMatching(data, player,
							Matches.UnitCanProduceUnits));
			final List<Territory> enemySeaTerr = SUtils.findUnitTerr(data, player, enemyAttackUnit);
			final List<Territory> isWaterTerr = SUtils.onlyWaterTerr(data, enemySeaTerr);
			enemySeaTerr.retainAll(isWaterTerr);
			Territory maxEnemySeaTerr = null;
			int maxUnits = 0;
			for (final Territory seaTerr : enemySeaTerr)
			{
				final int unitCount = seaTerr.getUnits().countMatches(enemyAttackUnit);
				if (unitCount > maxUnits)
				{
					maxUnits = unitCount;
					maxEnemySeaTerr = seaTerr;
				}
			}
			final Route seaRoute = SUtils.findNearest(maxEnemySeaTerr, waterFactoryWaterTerr, Matches.TerritoryIsWater, data);
			if (seaRoute != null)
			{
				final Territory checkSeaTerr = seaRoute.getEnd();
				if (checkSeaTerr != null)
				{
					final float seaStrength = SUtils.getStrengthOfPotentialAttackers(checkSeaTerr, data, player, tFirst, false, null);
					final float aStrength = SUtils.strength(checkSeaTerr.getUnits().getUnits(), false, true, tFirst);
					final float bStrength = SUtils.strength(player.getUnits().getMatches(attackUnit), false, true, tFirst);
					final float totStrength = aStrength + bStrength;
					if (totStrength > 0.9F * seaStrength)
						bidSeaTerr = checkSeaTerr;
				}
			}
			for (final Territory factCheck : factoryTerritories)
			{
				if (bidSeaTerr == null)
					bidSeaTerr = SUtils.findASeaTerritoryToPlaceOn(factCheck, data, player, tFirst);
				if (bidTransTerr == null)
					bidTransTerr = SUtils.findASeaTerritoryToPlaceOn(factCheck, data, player, tFirst);
			}
			placeSeaUnits(true, data, bidSeaTerr, bidSeaTerr, placeDelegate, player);
		}
		if (player.getUnits().someMatch(Matches.UnitIsNotSea))
		{
			ourSemiRankedBidTerrs.addAll(ourTerrWithEnemyNeighbors);
			ourTerrs.removeAll(ourTerrWithEnemyNeighbors);
			Collections.shuffle(ourTerrs);
			ourSemiRankedBidTerrs.addAll(ourTerrs);
			// need to remove places like greenland, iceland and west indies that have no route to the enemy, but somehow keep places like borneo, gibralter, etc.
			for (final Territory noRouteTerr : ourTerrs)
			{
				// do not place bids on areas that have no direct land access to an enemy, unless the value is 3 or greater
				if (SUtils.distanceToEnemy(noRouteTerr, data, player, false) < 1 && TerritoryAttachment.getProduction(noRouteTerr) < 3)
				{
					ourSemiRankedBidTerrs.remove(noRouteTerr);
				}
			}
			/* Currently the place delegate does not accept bids by the AI to territories that it does not own. If that gets fixed we can add the following code in order to bid to allied territories that contain our units (like Libya in ww2v3) (veqryn)
			for(Territory alliedTerr : ourFriendlyTerr)
			{
			    if(!Matches.isTerritoryOwnedBy(player).match(alliedTerr) && alliedTerr.getUnits().getMatches(Matches.unitIsOwnedBy(player)).size() > 0)
			    {
			    	ourSemiRankedBidTerrs.add(alliedTerr);
			    }
			}
			*/
			final List<Territory> isWaterTerr = SUtils.onlyWaterTerr(data, ourSemiRankedBidTerrs);
			ourSemiRankedBidTerrs.removeAll(isWaterTerr);
			ourSemiRankedBidTerrs.removeAll(impassableTerrs);
			// This will bid a max of 5 units to ALL territories except for the capitol. The capitol gets units last, and gets unlimited units (veqryn)
			final int maxBidPerTerritory = 5;
			int bidCycle = 0;
			while (!(player.getUnits().isEmpty()) && bidCycle < maxBidPerTerritory)
			{
				for (int i = 0; i <= ourSemiRankedBidTerrs.size() - 1; i++)
				{
					bidLandTerr = ourSemiRankedBidTerrs.get(i);
					placeAllWeCanOn(true, data, null, bidLandTerr, placeDelegate, player);
				}
				bidCycle++;
			}
			if (!player.getUnits().isEmpty())
				placeAllWeCanOn(true, data, null, capitol, placeDelegate, player);
		}
	}
	
	private void placeSeaUnits(final boolean bid, final GameData data, final Territory seaPlaceAttack, final Territory seaPlaceTrans, final IAbstractPlaceDelegate placeDelegate, final PlayerID player)
	{
		final CompositeMatch<Unit> attackUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsSea, Matches.UnitIsNotTransport);
		final List<Unit> seaUnits = player.getUnits().getMatches(attackUnit);
		final List<Unit> transUnits = player.getUnits().getMatches(Matches.UnitIsTransport);
		final List<Unit> airUnits = player.getUnits().getMatches(Matches.UnitCanLandOnCarrier);
		final List<Unit> carrierUnits = player.getUnits().getMatches(Matches.UnitIsCarrier);
		if (carrierUnits.size() > 0 && airUnits.size() > 0 && (Properties.getProduceFightersOnCarriers(data) || Properties.getLHTRCarrierProductionRules(data) || bid))
		{
			int carrierSpace = 0;
			for (final Unit carrier1 : carrierUnits)
				carrierSpace += UnitAttachment.get(carrier1.getType()).getCarrierCapacity();
			final Iterator<Unit> airIter = airUnits.iterator();
			while (airIter.hasNext() && carrierSpace > 0)
			{
				final Unit airPlane = airIter.next();
				seaUnits.add(airPlane);
				carrierSpace -= UnitAttachment.get(airPlane.getType()).getCarrierCost();
			}
		}
		if (bid)
		{
			if (!seaUnits.isEmpty())
				doPlace(seaPlaceAttack, seaUnits, placeDelegate);
			if (!transUnits.isEmpty())
				doPlace(seaPlaceTrans, transUnits, placeDelegate);
			return;
		}
		if (seaUnits.isEmpty() && transUnits.isEmpty())
			return;
		if (seaPlaceAttack == seaPlaceTrans)
		{
			seaUnits.addAll(transUnits);
			transUnits.clear();
		}
		final PlaceableUnits pu = placeDelegate.getPlaceableUnits(seaUnits, seaPlaceAttack);
		int pLeft = 0;
		if (pu.getErrorMessage() != null)
			return;
		if (!seaUnits.isEmpty())
		{
			pLeft = pu.getMaxUnits();
			if (pLeft == -1)
				pLeft = Integer.MAX_VALUE;
			final int numPlace = Math.min(pLeft, seaUnits.size());
			pLeft -= numPlace;
			final Collection<Unit> toPlace = seaUnits.subList(0, numPlace);
			doPlace(seaPlaceAttack, toPlace, placeDelegate);
		}
		if (!transUnits.isEmpty())
		{
			final PlaceableUnits pu2 = placeDelegate.getPlaceableUnits(transUnits, seaPlaceTrans);
			if (pu2.getErrorMessage() != null)
				return;
			pLeft = pu2.getMaxUnits();
			if (pLeft == -1)
				pLeft = Integer.MAX_VALUE;
			final int numPlace = Math.min(pLeft, transUnits.size());
			final Collection<Unit> toPlace = transUnits.subList(0, numPlace);
			doPlace(seaPlaceTrans, toPlace, placeDelegate);
		}
	}
	
	private void placeAllWeCanOn(final boolean bid, final GameData data, final Territory factoryPlace, final Territory placeAt, final IAbstractPlaceDelegate placeDelegate, final PlayerID player)
	{
		final CompositeMatch<Unit> landOrAir = new CompositeMatchOr<Unit>(Matches.UnitIsAir, Matches.UnitIsLand);
		if (factoryPlace != null) // place a factory?
		{
			final Collection<Unit> toPlace = new ArrayList<Unit>(player.getUnits().getMatches(Matches.UnitCanProduceUnitsAndIsConstruction));
			if (toPlace.size() == 1) // only 1 may have been purchased...anything greater is wrong
			{
				doPlace(factoryPlace, toPlace, placeDelegate);
				return;
			}
			else if (toPlace.size() > 1)
				return;
		}
		final List<Unit> landUnits = player.getUnits().getMatches(landOrAir);
		final Territory capitol = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
		final PlaceableUnits pu3 = placeDelegate.getPlaceableUnits(landUnits, placeAt);
		if (pu3.getErrorMessage() != null)
			return;
		int placementLeft3 = pu3.getMaxUnits();
		if (placementLeft3 == -1)
			placementLeft3 = Integer.MAX_VALUE;
		// allow placing only 1 unit per territory if a bid, unless it is the capitol (water is handled in placeseaunits)
		if (bid)
			placementLeft3 = 1;
		if (bid && (placeAt == capitol))
			placementLeft3 = 1000;
		if (!landUnits.isEmpty())
		{
			final int landPlaceCount = Math.min(placementLeft3, landUnits.size());
			placementLeft3 -= landPlaceCount;
			final Collection<Unit> toPlace = landUnits.subList(0, landPlaceCount);
			doPlace(placeAt, toPlace, placeDelegate);
		}
	}
	
	public void place(final Map<Territory, ProPurchaseTerritory> purchaseTerritories, final IAbstractPlaceDelegate placeDelegate, final GameData data, final PlayerID player)
	{
		LogUtils.log(Level.FINE, "Starting place phase");
		
		if (purchaseTerritories != null)
		{
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
		final Map<Territory, ProPurchaseTerritory> placeNonConstructionTerritories = purchaseUtils.findPurchaseTerritories(player);
		
		// Determine max enemy attack units and current allied defenders
		final Map<Territory, ProAttackTerritoryData> enemyAttackMap = new HashMap<Territory, ProAttackTerritoryData>();
		attackOptionsUtils.findMaxEnemyAttackUnits(player, new ArrayList<Territory>(), new ArrayList<Territory>(placeNonConstructionTerritories.keySet()), enemyAttackMap);
		findDefendersInPlaceTerritories(placeNonConstructionTerritories);
		
		// Prioritize land territories that need defended and place additional defenders
		final List<ProPlaceTerritory> needToDefendLandTerritories = prioritizeTerritoriesToDefend(placeNonConstructionTerritories, enemyAttackMap, true);
		placeDefenders(placeNonConstructionTerritories, enemyAttackMap, needToDefendLandTerritories, placeDelegate);
		
		// Find strategic value for each territory
		LogUtils.log(Level.FINE, "Find strategic value for place territories");
		final Map<Territory, Double> territoryValueMap = territoryValueUtils.findTerritoryValues(player, minCostPerHitPoint, new ArrayList<Territory>(), new ArrayList<Territory>());
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
		
		// Place regular land units
		placeLandUnits(placeNonConstructionTerritories, enemyAttackMap, prioritizedLandTerritories, placeDelegate, false);
		
		// Place isConstruction land units (needs separated since placeDelegate.getPlaceableUnits doesn't handle them combined)
		placeLandUnits(placeNonConstructionTerritories, enemyAttackMap, prioritizedLandTerritories, placeDelegate, true);
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
				final ProBattleResultData result = battleUtils.calculateBattleResults(player, t, new ArrayList<Unit>(enemyAttackingUnits), placeTerritory.getDefendingUnits(), enemyAttackMap.get(t)
							.getMaxBombardUnits(), false);
				placeTerritory.setMinBattleResult(result);
				LogUtils.log(Level.FINEST, t.getName() + " TUVSwing=" + result.getTUVSwing() + ", win%=" + result.getWinPercentage() + ", hasLandUnitRemaining=" + result.isHasLandUnitRemaining()
							+ ", enemyAttackers=" + enemyAttackingUnits + ", defenders=" + placeTerritory.getDefendingUnits());
				
				// If it can't currently be held then add to list
				final boolean isLandAndCanOnlyBeAttackedByAir = !t.isWater() && Match.allMatch(enemyAttackingUnits, Matches.UnitIsAir);
				if (result.isHasLandUnitRemaining() || result.getTUVSwing() > 0 || (t.equals(myCapital) && !isLandAndCanOnlyBeAttackedByAir && result.getWinPercentage() > (100 - WIN_PERCENTAGE)))
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
				final List<ProPlaceTerritory> needToDefendTerritories, int PUsRemaining, final List<ProPurchaseOption> defensePurchaseOptions, final List<ProPurchaseOption> airPurchaseOptions,
				final boolean isLand)
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
			
			// Find local owned units
			final List<Unit> ownedLocalUnits = t.getUnits().getMatches(Matches.unitIsOwnedBy(player));
			Set<Territory> nearbyTerritories = data.getMap().getNeighbors(t, 2, ProMatches.territoryCanMoveAirUnits(player, data, false));
			nearbyTerritories = new HashSet<Territory>(Match.getMatches(nearbyTerritories, Matches.TerritoryIsLand));
			nearbyTerritories.add(t);
			final List<Unit> ownedNearbyUnits = new ArrayList<Unit>();
			for (final Territory nearbyTerritory : nearbyTerritories)
				ownedNearbyUnits.addAll(nearbyTerritory.getUnits().getMatches(Matches.unitIsOwnedBy(player)));
			int unusedCarrierCapacity = Math.min(0, transportUtils.getUnusedCarrierCapacity(player, t, ownedNearbyUnits));
			
			// Determine if need destroyer
			boolean needDestroyer = false;
			if (Match.someMatch(enemyAttackMap.get(t).getMaxUnits(), Matches.UnitIsSub) && Match.noneMatch(ownedLocalUnits, Matches.UnitIsDestroyer))
				needDestroyer = true;
			
			// Find all purchase territories for place territory
			int PUsSpent = 0;
			final List<Unit> unitsToPlace = new ArrayList<Unit>();
			ProBattleResultData finalResult = new ProBattleResultData();
			final List<ProPurchaseTerritory> selectedPurchaseTerritories = getPurchaseTerritories(placeTerritory, purchaseTerritories);
			for (final ProPurchaseTerritory purchaseTerritory : selectedPurchaseTerritories)
			{
				// Check remaining production
				int remainingUnitProduction = purchaseTerritory.getRemainingUnitProduction();
				LogUtils.log(Level.FINER, purchaseTerritory.getTerritory() + ", remainingUnitProduction=" + remainingUnitProduction);
				if (remainingUnitProduction <= 0)
					continue;
				
				// Find defenders that can be produced in this territory
				final List<ProPurchaseOption> purchaseOptionsForTerritory = purchaseUtils.findPurchaseOptionsForTerritory(player, defensePurchaseOptions, t);
				purchaseOptionsForTerritory.addAll(airPurchaseOptions);
				
				// Purchase necessary defenders
				while (true)
				{
					// Remove options that cost too much PUs or production
					purchaseUtils.removePurchaseOptionsByCostAndProductionAndLimits(player, startOfTurnData, purchaseOptionsForTerritory, PUsRemaining - PUsSpent, remainingUnitProduction,
								unitsToPlace, purchaseTerritories);
					if (purchaseOptionsForTerritory.isEmpty())
						break;
					
					// Select purchase option
					final Map<ProPurchaseOption, Double> defenseEfficiencies = new HashMap<ProPurchaseOption, Double>();
					for (final ProPurchaseOption ppo : purchaseOptionsForTerritory)
					{
						if (isLand)
							defenseEfficiencies.put(ppo, ppo.getDefenseEfficiency2(1, data, ownedLocalUnits, unitsToPlace));
						else
							defenseEfficiencies.put(ppo, ppo.getSeaDefenseEfficiency(data, ownedLocalUnits, unitsToPlace, needDestroyer, unusedCarrierCapacity));
					}
					final ProPurchaseOption selectedOption = purchaseUtils.randomizePurchaseOption(defenseEfficiencies, "Defense");
					if (selectedOption.isDestroyer())
						needDestroyer = false;
					
					// Create new temp units
					PUsSpent += selectedOption.getCost();
					remainingUnitProduction -= selectedOption.getQuantity();
					unitsToPlace.addAll(selectedOption.getUnitType().create(selectedOption.getQuantity(), player, true));
					if (selectedOption.isCarrier() || selectedOption.isAir())
					{
						final List<Unit> nearbyOwnedAndPlaceUnits = new ArrayList<Unit>(ownedNearbyUnits);
						nearbyOwnedAndPlaceUnits.addAll(unitsToPlace);
						unusedCarrierCapacity = transportUtils.getUnusedCarrierCapacity(player, t, nearbyOwnedAndPlaceUnits);
					}
					LogUtils.log(Level.FINEST, "Selected unit=" + selectedOption.getUnitType().getName());
					
					// Find current battle result
					final Set<Unit> enemyAttackingUnits = new HashSet<Unit>(enemyAttackMap.get(t).getMaxUnits());
					enemyAttackingUnits.addAll(enemyAttackMap.get(t).getMaxAmphibUnits());
					final List<Unit> defenders = new ArrayList<Unit>(placeTerritory.getDefendingUnits());
					defenders.addAll(unitsToPlace);
					finalResult = battleUtils.calculateBattleResults(player, t, new ArrayList<Unit>(enemyAttackingUnits), defenders, enemyAttackMap.get(t).getMaxBombardUnits(), false);
					
					// Break if it can be held
					if ((!t.equals(myCapital) && !finalResult.isHasLandUnitRemaining() && finalResult.getTUVSwing() <= 0) ||
								(t.equals(myCapital) && finalResult.getWinPercentage() < (100 - WIN_PERCENTAGE) && finalResult.getTUVSwing() <= 0))
						break;
				}
			}
			
			// Check to see if its worth trying to defend the territory
			final boolean hasLocalSuperiority = battleUtils.territoryHasLocalLandSuperiority(t, 2, player, purchaseTerritories);
			if (!finalResult.isHasLandUnitRemaining() || (finalResult.getTUVSwing() - PUsSpent / 2) < placeTerritory.getMinBattleResult().getTUVSwing() || t.equals(myCapital)
						|| (!t.isWater() && hasLocalSuperiority))
			{
				PUsRemaining -= PUsSpent;
				LogUtils.log(Level.FINEST, t + ", placedUnits=" + unitsToPlace + ", TUVSwing=" + finalResult.getTUVSwing() + ", hasLandUnitRemaining=" + finalResult.isHasLandUnitRemaining()
							+ ", hasLocalSuperiority=" + hasLocalSuperiority);
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
				if (!t.isWater() && placeTerritory.getStrategicValue() >= 1 && placeTerritory.isCanHold())
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
			
			// Remove options that cost too much PUs or production
			final List<ProPurchaseOption> purchaseOptionsForTerritory = purchaseUtils.findPurchaseOptionsForTerritory(player, specialPurchaseOptions, t);
			purchaseUtils.removePurchaseOptionsByCostAndProductionAndLimits(player, startOfTurnData, purchaseOptionsForTerritory, PUsRemaining, remainingUnitProduction,
						new ArrayList<Unit>(), purchaseTerritories);
			if (purchaseOptionsForTerritory.isEmpty())
				continue;
			
			// Determine most cost efficient units that can be produced in this territory
			ProPurchaseOption bestAAOption = null;
			int minCost = Integer.MAX_VALUE;
			for (final ProPurchaseOption ppo : purchaseOptionsForTerritory)
			{
				final boolean isAAForBombing = Matches.UnitTypeIsAAforBombingThisUnitOnly.match(ppo.getUnitType());
				if (isAAForBombing && ppo.getCost() < minCost && !Matches.UnitTypeConsumesUnitsOnCreation.match(ppo.getUnitType()))
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
				final List<ProPlaceTerritory> prioritizedLandTerritories, int PUsRemaining, final ProPurchaseOptionMap purchaseOptions)
	{
		final List<Unit> unplacedUnits = player.getUnits().getMatches(Matches.UnitIsNotSea);
		if (PUsRemaining == 0 && unplacedUnits.isEmpty())
			return PUsRemaining;
		LogUtils.log(Level.FINE, "Purchase land units with PUsRemaining=" + PUsRemaining);
		if (!unplacedUnits.isEmpty())
			LogUtils.log(Level.FINE, "Purchase land units with unplaced units=" + unplacedUnits);
		
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
			final List<ProPurchaseOption> landFodderOptions = purchaseUtils.findPurchaseOptionsForTerritory(player, purchaseOptions.getLandFodderOptions(), t);
			final List<ProPurchaseOption> landAttackOptions = purchaseUtils.findPurchaseOptionsForTerritory(player, purchaseOptions.getLandAttackOptions(), t);
			final List<ProPurchaseOption> landDefenseOptions = purchaseUtils.findPurchaseOptionsForTerritory(player, purchaseOptions.getLandDefenseOptions(), t);
			
			// Determine enemy distance and locally owned units
			int enemyDistance = utils.getClosestEnemyLandTerritoryDistance(data, player, t);
			if (enemyDistance <= 0)
				enemyDistance = 10;
			final int fodderPercent = 80 - enemyDistance * 5;
			LogUtils.log(Level.FINER, t + ", enemyDistance=" + enemyDistance + ", fodderPercent=" + fodderPercent);
			final Set<Territory> neighbors = data.getMap().getNeighbors(t, 2, ProMatches.territoryCanMoveLandUnits(player, data, false));
			neighbors.add(t);
			final List<Unit> ownedLocalUnits = new ArrayList<Unit>();
			for (final Territory neighbor : neighbors)
				ownedLocalUnits.addAll(neighbor.getUnits().getMatches(Matches.unitIsOwnedBy(player)));
			
			// Check for unplaced units
			final List<Unit> unitsToPlace = new ArrayList<Unit>();
			for (final Iterator<Unit> it = unplacedUnits.iterator(); it.hasNext();)
			{
				final Unit u = it.next();
				if (remainingUnitProduction > 0 && purchaseUtils.canUnitsBePlaced(Collections.singletonList(u), player, t))
				{
					remainingUnitProduction--;
					unitsToPlace.add(u);
					it.remove();
					LogUtils.log(Level.FINEST, "Selected unplaced unit=" + u);
				}
			}
			
			// Purchase as many units as possible
			int addedFodderUnits = 0;
			double attackAndDefenseDifference = 0;
			boolean selectFodderUnit = true;
			while (true)
			{
				// Remove options that cost too much PUs or production
				purchaseUtils.removePurchaseOptionsByCostAndProductionAndLimits(player, startOfTurnData, landFodderOptions, PUsRemaining, remainingUnitProduction, unitsToPlace, purchaseTerritories);
				purchaseUtils.removePurchaseOptionsByCostAndProductionAndLimits(player, startOfTurnData, landAttackOptions, PUsRemaining, remainingUnitProduction, unitsToPlace, purchaseTerritories);
				purchaseUtils.removePurchaseOptionsByCostAndProductionAndLimits(player, startOfTurnData, landDefenseOptions, PUsRemaining, remainingUnitProduction, unitsToPlace, purchaseTerritories);
				
				// Select purchase option
				ProPurchaseOption selectedOption = null;
				if (!selectFodderUnit && attackAndDefenseDifference > 0 && !landDefenseOptions.isEmpty())
				{
					final Map<ProPurchaseOption, Double> defenseEfficiencies = new HashMap<ProPurchaseOption, Double>();
					for (final ProPurchaseOption ppo : landDefenseOptions)
						defenseEfficiencies.put(ppo, ppo.getDefenseEfficiency2(enemyDistance, data, ownedLocalUnits, unitsToPlace));
					selectedOption = purchaseUtils.randomizePurchaseOption(defenseEfficiencies, "Land Defense");
				}
				else if (!selectFodderUnit && !landAttackOptions.isEmpty())
				{
					final Map<ProPurchaseOption, Double> attackEfficiencies = new HashMap<ProPurchaseOption, Double>();
					for (final ProPurchaseOption ppo : landAttackOptions)
						attackEfficiencies.put(ppo, ppo.getAttackEfficiency2(enemyDistance, data, ownedLocalUnits, unitsToPlace));
					selectedOption = purchaseUtils.randomizePurchaseOption(attackEfficiencies, "Land Attack");
				}
				else if (!landFodderOptions.isEmpty())
				{
					final Map<ProPurchaseOption, Double> fodderEfficiencies = new HashMap<ProPurchaseOption, Double>();
					for (final ProPurchaseOption ppo : landFodderOptions)
						fodderEfficiencies.put(ppo, ppo.getFodderEfficiency(enemyDistance, data, ownedLocalUnits, unitsToPlace));
					selectedOption = purchaseUtils.randomizePurchaseOption(fodderEfficiencies, "Land Fodder");
					addedFodderUnits += selectedOption.getQuantity();
				}
				else
				{
					break;
				}
				
				// Create new temp units
				PUsRemaining -= selectedOption.getCost();
				remainingUnitProduction -= selectedOption.getQuantity();
				unitsToPlace.addAll(selectedOption.getUnitType().create(selectedOption.getQuantity(), player, true));
				attackAndDefenseDifference += (selectedOption.getAttack() - selectedOption.getDefense());
				selectFodderUnit = ((double) addedFodderUnits / unitsToPlace.size() * 100) <= fodderPercent;
				LogUtils.log(Level.FINEST, "Selected unit=" + selectedOption.getUnitType().getName());
			}
			
			// Add units to place territory
			placeTerritory.getPlaceUnits().addAll(unitsToPlace);
			LogUtils.log(Level.FINER, t + ", placedUnits=" + unitsToPlace);
		}
		
		return PUsRemaining;
	}
	
	private int purchaseFactory(final Map<Territory, ProPurchaseTerritory> factoryPurchaseTerritories, final Map<Territory, ProAttackTerritoryData> enemyAttackMap, int PUsRemaining,
				final Map<Territory, ProPurchaseTerritory> purchaseTerritories, final List<ProPlaceTerritory> prioritizedLandTerritories,
				final ProPurchaseOptionMap purchaseOptions, final boolean hasExtraPUs)
	{
		if (PUsRemaining == 0)
			return PUsRemaining;
		LogUtils.log(Level.FINE, "Purchase factory with PUsRemaining=" + PUsRemaining + ", hasExtraPUs=" + hasExtraPUs);
		
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
			// Only consider territories with production of at least 3 unless there are still remaining PUs
			final int production = TerritoryAttachment.get(t).getProduction();
			if ((production < 3 && !hasExtraPUs) || production < 2)
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
				final ProBattleResultData result = battleUtils.estimateDefendBattleResults(player, t, new ArrayList<Unit>(enemyAttackingUnits), defenders, enemyAttackMap.get(t).getMaxBombardUnits());
				
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
		if (!hasExtraPUs)
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
		final Map<Territory, Double> territoryValueMap = territoryValueUtils.findTerritoryValues(player, minCostPerHitPoint, territoriesThatCantBeHeld, new ArrayList<Territory>());
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
			if (value > maxValue && ((numNearbyEnemyTerritories >= 4 && territoryValueMap.get(t) >= 1)
						|| (isAdjacentToSea && hasExtraPUs)))
			{
				maxValue = value;
				maxTerritory = t;
			}
		}
		LogUtils.log(Level.FINER, "Try to purchase factory for territory: " + maxTerritory);
		
		// Determine whether to purchase factory
		if (maxTerritory != null)
		{
			// Find most expensive placed land unit to consider removing for a factory
			int maxPlacedCost = 0;
			ProPlaceTerritory maxPlacedTerritory = null;
			Unit maxPlacedUnit = null;
			for (final ProPlaceTerritory placeTerritory : prioritizedLandTerritories)
			{
				for (final Unit u : placeTerritory.getPlaceUnits())
				{
					for (final ProPurchaseOption ppo : purchaseOptions.getLandOptions())
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
			
			// Determine units that can be produced in this territory
			final List<ProPurchaseOption> purchaseOptionsForTerritory = purchaseUtils.findPurchaseOptionsForTerritory(player, purchaseOptions.getFactoryOptions(), maxTerritory);
			purchaseUtils.removePurchaseOptionsByCostAndProductionAndLimits(player, startOfTurnData, purchaseOptionsForTerritory, PUsRemaining + maxPlacedCost, 1, new ArrayList<Unit>(),
						purchaseTerritories);
			
			// Determine most expensive factory option (currently doesn't buy mobile factories)
			ProPurchaseOption bestFactoryOption = null;
			double maxFactoryEfficiency = 0;
			for (final ProPurchaseOption ppo : purchaseOptionsForTerritory)
			{
				if (ppo.getMovement() == 0 && ppo.getCost() > maxFactoryEfficiency)
				{
					bestFactoryOption = ppo;
					maxFactoryEfficiency = ppo.getCost();
				}
			}
			
			// Check if there are enough PUs to buy a factory
			if (bestFactoryOption != null)
			{
				LogUtils.log(Level.FINER, "Best factory unit: " + bestFactoryOption.getUnitType().getName());
				
				final ProPurchaseTerritory factoryPurchaseTerritory = new ProPurchaseTerritory(maxTerritory, data, player, 0);
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
		LogUtils.log(Level.FINER, "Determine sea place value:");
		for (final ProPlaceTerritory placeTerritory : seaPlaceTerritories)
		{
			// Calculate defense value for prioritization
			final List<Unit> units = new ArrayList<Unit>(placeTerritory.getDefendingUnits());
			units.addAll(placeTerritory.getPlaceUnits());
			final List<Unit> myUnits = Match.getMatches(units, Matches.unitIsOwnedBy(player));
			final int numMyTransports = Match.countMatches(myUnits, Matches.UnitIsTransport);
			final int numSeaDefenders = Match.countMatches(units, Matches.UnitIsNotTransport);
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
	
	private int purchaseSeaAndAmphibUnits(final Map<Territory, ProPurchaseTerritory> purchaseTerritories, final Map<Territory, ProAttackTerritoryData> enemyAttackMap,
				final List<ProPlaceTerritory> prioritizedSeaTerritories, final Map<Territory, Double> territoryValueMap, int PUsRemaining, final ProPurchaseOptionMap purchaseOptions)
	{
		if (PUsRemaining == 0)
			return PUsRemaining;
		LogUtils.log(Level.FINE, "Purchase sea and amphib units with PUsRemaining=" + PUsRemaining);
		
		// Loop through prioritized territories and purchase sea units
		for (final ProPlaceTerritory placeTerritory : prioritizedSeaTerritories)
		{
			final Territory t = placeTerritory.getTerritory();
			LogUtils.log(Level.FINER, "Checking sea place for " + t.getName());
			
			// Find all purchase territories for place territory
			final List<ProPurchaseTerritory> selectedPurchaseTerritories = getPurchaseTerritories(placeTerritory, purchaseTerritories);
			
			// Find local owned units
			final Set<Territory> neighbors = data.getMap().getNeighbors(t, 2, ProMatches.territoryCanMoveSeaUnits(player, data, false));
			neighbors.add(t);
			final List<Unit> ownedLocalUnits = new ArrayList<Unit>();
			for (final Territory neighbor : neighbors)
				ownedLocalUnits.addAll(neighbor.getUnits().getMatches(Matches.unitIsOwnedBy(player)));
			Set<Territory> nearbyAirTerritories = data.getMap().getNeighbors(t, 2, ProMatches.territoryCanMoveAirUnits(player, data, false));
			nearbyAirTerritories = new HashSet<Territory>(Match.getMatches(nearbyAirTerritories, Matches.TerritoryIsLand));
			nearbyAirTerritories.add(t);
			final List<Unit> ownedNearbyUnits = new ArrayList<Unit>();
			for (final Territory nearbyTerritory : nearbyAirTerritories)
				ownedNearbyUnits.addAll(nearbyTerritory.getUnits().getMatches(Matches.unitIsOwnedBy(player)));
			int unusedCarrierCapacity = Math.min(0, transportUtils.getUnusedCarrierCapacity(player, t, ownedNearbyUnits));
			boolean needDestroyer = false;
			
			// If any enemy attackers then purchase sea defenders until it can be held
			if (enemyAttackMap.get(t) != null)
			{
				// Determine if need destroyer
				if (Match.someMatch(enemyAttackMap.get(t).getMaxUnits(), Matches.UnitIsSub) && Match.noneMatch(t.getUnits().getMatches(Matches.unitIsOwnedBy(player)), Matches.UnitIsDestroyer))
					needDestroyer = true;
				LogUtils.log(Level.FINEST, t + ", needDestroyer=" + needDestroyer + ", checking defense since has enemy attackers: " + enemyAttackMap.get(t).getMaxUnits());
				
				int PUsSpent = 0;
				final List<Unit> unitsToPlace = new ArrayList<Unit>();
				final List<Unit> initialDefendingUnits = new ArrayList<Unit>(placeTerritory.getDefendingUnits());
				initialDefendingUnits.addAll(getPlaceUnits(t, purchaseTerritories));
				ProBattleResultData result = battleUtils.calculateBattleResults(player, t, enemyAttackMap.get(t).getMaxUnits(), initialDefendingUnits, enemyAttackMap.get(t).getMaxBombardUnits(),
							false);
				boolean hasOnlyRetreatingSubs = Properties.getSubRetreatBeforeBattle(data) && Match.allMatch(initialDefendingUnits, Matches.UnitIsSub)
							&& Match.noneMatch(enemyAttackMap.get(t).getMaxUnits(), Matches.UnitIsDestroyer);
				for (final ProPurchaseTerritory purchaseTerritory : selectedPurchaseTerritories)
				{
					// Check remaining production
					int remainingUnitProduction = purchaseTerritory.getRemainingUnitProduction();
					LogUtils.log(Level.FINEST, t + ", purchaseTerritory=" + purchaseTerritory.getTerritory() + ", remainingUnitProduction=" + remainingUnitProduction);
					if (remainingUnitProduction <= 0)
						continue;
					
					// Determine sea and transport units that can be produced in this territory
					final List<ProPurchaseOption> seaPurchaseOptionsForTerritory = purchaseUtils.findPurchaseOptionsForTerritory(player, purchaseOptions.getSeaDefenseOptions(), t);
					seaPurchaseOptionsForTerritory.addAll(purchaseOptions.getAirOptions());
					
					// Purchase enough sea defenders to hold territory
					while (true)
					{
						// Remove options that cost too much PUs or production
						purchaseUtils.removePurchaseOptionsByCostAndProductionAndLimits(player, startOfTurnData, seaPurchaseOptionsForTerritory, PUsRemaining - PUsSpent, remainingUnitProduction,
									unitsToPlace, purchaseTerritories);
						if (seaPurchaseOptionsForTerritory.isEmpty())
							break;
						
						// If it can be held then break
						if (!hasOnlyRetreatingSubs && (result.getTUVSwing() < -1 || result.getWinPercentage() < WIN_PERCENTAGE))
							break;
						
						// Select purchase option
						final Map<ProPurchaseOption, Double> defenseEfficiencies = new HashMap<ProPurchaseOption, Double>();
						for (final ProPurchaseOption ppo : seaPurchaseOptionsForTerritory)
							defenseEfficiencies.put(ppo, ppo.getSeaDefenseEfficiency(data, ownedLocalUnits, unitsToPlace, needDestroyer, unusedCarrierCapacity));
						final ProPurchaseOption selectedOption = purchaseUtils.randomizePurchaseOption(defenseEfficiencies, "Sea Defense");
						if (selectedOption.isDestroyer())
							needDestroyer = false;
						
						// Create new temp defenders
						PUsSpent += selectedOption.getCost();
						remainingUnitProduction -= selectedOption.getQuantity();
						unitsToPlace.addAll(selectedOption.getUnitType().create(selectedOption.getQuantity(), player, true));
						if (selectedOption.isCarrier() || selectedOption.isAir())
						{
							final List<Unit> nearbyOwnedAndPlaceUnits = new ArrayList<Unit>(ownedNearbyUnits);
							nearbyOwnedAndPlaceUnits.addAll(unitsToPlace);
							unusedCarrierCapacity = transportUtils.getUnusedCarrierCapacity(player, t, nearbyOwnedAndPlaceUnits);
						}
						LogUtils.log(Level.FINEST, t + ", added sea defender for defense: " + selectedOption.getUnitType().getName() + ", TUVSwing=" + result.getTUVSwing() + ", win%="
									+ result.getWinPercentage() + ", unusedCarrierCapacity=" + unusedCarrierCapacity);
						
						// Find current battle result
						final List<Unit> defendingUnits = new ArrayList<Unit>(placeTerritory.getDefendingUnits());
						defendingUnits.addAll(getPlaceUnits(t, purchaseTerritories));
						defendingUnits.addAll(unitsToPlace);
						result = battleUtils.estimateDefendBattleResults(player, t, enemyAttackMap.get(t).getMaxUnits(), defendingUnits, enemyAttackMap.get(t).getMaxBombardUnits());
						hasOnlyRetreatingSubs = Properties.getSubRetreatBeforeBattle(data) && Match.allMatch(defendingUnits, Matches.UnitIsSub)
									&& Match.noneMatch(enemyAttackMap.get(t).getMaxUnits(), Matches.UnitIsDestroyer);
					}
				}
				
				// Check to see if its worth trying to defend the territory
				if (result.getTUVSwing() < 0 || result.getWinPercentage() < WIN_PERCENTAGE)
				{
					PUsRemaining -= PUsSpent;
					LogUtils.log(Level.FINEST, t + ", placedUnits=" + unitsToPlace + ", TUVSwing=" + result.getTUVSwing() + ", hasLandUnitRemaining=" + result.isHasLandUnitRemaining());
					addUnitsToPlaceTerritory(placeTerritory, unitsToPlace, purchaseTerritories);
				}
				else
				{
					setCantHoldPlaceTerritory(placeTerritory, purchaseTerritories);
					LogUtils.log(Level.FINEST, t + ", can't defend TUVSwing=" + result.getTUVSwing() + ", win%=" + result.getWinPercentage() + ", tried to placeDefenders="
								+ unitsToPlace + ", enemyAttackers=" + enemyAttackMap.get(t).getMaxUnits());
					continue;
				}
			}
			
			// Check to see if local naval superiority
			int landDistance = utils.getClosestEnemyLandTerritoryDistanceOverWater(data, player, t);
			if (landDistance <= 0)
				landDistance = 10;
			final int enemyDistance = Math.max(4, (landDistance + 1));
			final int alliedDistance = (enemyDistance + 1) / 2;
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
			
			// Purchase naval defenders until I have local naval superiority
			final List<Unit> unitsToPlace = new ArrayList<Unit>();
			for (final ProPurchaseTerritory purchaseTerritory : selectedPurchaseTerritories)
			{
				// Check remaining production
				int remainingUnitProduction = purchaseTerritory.getRemainingUnitProduction();
				LogUtils.log(Level.FINEST, t + ", purchaseTerritory=" + purchaseTerritory.getTerritory() + ", remainingUnitProduction=" + remainingUnitProduction);
				if (remainingUnitProduction <= 0)
					continue;
				
				// Determine sea and transport units that can be produced in this territory
				final List<ProPurchaseOption> seaPurchaseOptionsForTerritory = purchaseUtils.findPurchaseOptionsForTerritory(player, purchaseOptions.getSeaDefenseOptions(), t);
				seaPurchaseOptionsForTerritory.addAll(purchaseOptions.getAirOptions());
				
				while (true)
				{
					// Remove options that cost too much PUs or production
					purchaseUtils.removePurchaseOptionsByCostAndProductionAndLimits(player, startOfTurnData, seaPurchaseOptionsForTerritory, PUsRemaining, remainingUnitProduction, unitsToPlace,
								purchaseTerritories);
					if (seaPurchaseOptionsForTerritory.isEmpty())
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
					
					// Select purchase option
					final Map<ProPurchaseOption, Double> defenseEfficiencies = new HashMap<ProPurchaseOption, Double>();
					for (final ProPurchaseOption ppo : seaPurchaseOptionsForTerritory)
						defenseEfficiencies.put(ppo, ppo.getSeaDefenseEfficiency(data, ownedLocalUnits, unitsToPlace, needDestroyer, unusedCarrierCapacity));
					final ProPurchaseOption selectedOption = purchaseUtils.randomizePurchaseOption(defenseEfficiencies, "Sea Defense");
					if (selectedOption.isDestroyer())
						needDestroyer = false;
					
					// Create new temp units
					PUsRemaining -= selectedOption.getCost();
					remainingUnitProduction -= selectedOption.getQuantity();
					unitsToPlace.addAll(selectedOption.getUnitType().create(selectedOption.getQuantity(), player, true));
					if (selectedOption.isCarrier() || selectedOption.isAir())
					{
						final List<Unit> nearbyOwnedAndPlaceUnits = new ArrayList<Unit>(ownedNearbyUnits);
						nearbyOwnedAndPlaceUnits.addAll(unitsToPlace);
						unusedCarrierCapacity = transportUtils.getUnusedCarrierCapacity(player, t, nearbyOwnedAndPlaceUnits);
					}
					LogUtils.log(Level.FINEST, t + ", added sea defender for naval superiority: " + selectedOption.getUnitType().getName() + ", unusedCarrierCapacity=" + unusedCarrierCapacity);
				}
			}
			
			// Add sea defender units to place territory
			addUnitsToPlaceTerritory(placeTerritory, unitsToPlace, purchaseTerritories);
			
			// Loop through adjacent purchase territories and purchase transport/amphib units
			final int distance = transportUtils.findMaxMovementForTransports(purchaseOptions.getSeaTransportOptions());
			LogUtils.log(Level.FINEST, t + ", transportMovement=" + distance);
			for (final ProPurchaseTerritory purchaseTerritory : selectedPurchaseTerritories)
			{
				final Territory landTerritory = purchaseTerritory.getTerritory();
				
				// Check if territory can produce units and has remaining production
				int remainingUnitProduction = purchaseTerritory.getRemainingUnitProduction();
				LogUtils.log(Level.FINEST, t + ", purchaseTerritory=" + landTerritory + ", remainingUnitProduction=" + remainingUnitProduction);
				if (remainingUnitProduction <= 0)
					continue;
				
				// Find local owned units
				final List<Unit> ownedLocalAmphibUnits = landTerritory.getUnits().getMatches(Matches.unitIsOwnedBy(player));
				
				// Determine sea and transport units that can be produced in this territory
				final List<ProPurchaseOption> seaTransportPurchaseOptionsForTerritory = purchaseUtils.findPurchaseOptionsForTerritory(player, purchaseOptions.getSeaTransportOptions(), t);
				final List<ProPurchaseOption> amphibPurchaseOptionsForTerritory = purchaseUtils.findPurchaseOptionsForTerritory(player, purchaseOptions.getLandOptions(), landTerritory);
				
				// Find transports that need loaded and units to ignore that are already paired up
				final List<Unit> transportsThatNeedUnits = new ArrayList<Unit>();
				final Set<Unit> potentialUnitsToLoad = new HashSet<Unit>();
				final Set<Territory> seaTerritories = data.getMap().getNeighbors(landTerritory, distance, ProMatches.territoryCanMoveSeaUnits(player, data, false));
				for (final Territory seaTerritory : seaTerritories)
				{
					final List<Unit> unitsInTerritory = getPlaceUnits(seaTerritory, purchaseTerritories);
					unitsInTerritory.addAll(seaTerritory.getUnits().getUnits());
					final List<Unit> transports = Match.getMatches(unitsInTerritory, ProMatches.unitIsOwnedTransport(player));
					for (final Unit transport : transports)
					{
						transportsThatNeedUnits.add(transport);
						final Set<Territory> territoriesToLoadFrom = new HashSet<Territory>(data.getMap().getNeighbors(seaTerritory, distance));
						for (final Iterator<Territory> it = territoriesToLoadFrom.iterator(); it.hasNext();)
						{
							final Territory potentialTerritory = it.next();
							if (potentialTerritory.isWater() || territoryValueMap.get(potentialTerritory) > 0.25)
								it.remove();
						}
						final List<Unit> units = transportUtils.getUnitsToTransportFromTerritories(player, transport, territoriesToLoadFrom, new ArrayList<Unit>(potentialUnitsToLoad));
						potentialUnitsToLoad.addAll(units);
					}
				}
				
				// Determine whether transports, amphib units, or both are needed
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
				LogUtils.log(Level.FINEST, t + ", potentialUnitsToLoad=" + potentialUnitsToLoad + ", transportsThatNeedUnits=" + transportsThatNeedUnits);
				
				// Purchase transports and amphib units
				final List<Unit> amphibUnitsToPlace = new ArrayList<Unit>();
				final List<Unit> transportUnitsToPlace = new ArrayList<Unit>();
				while (true)
				{
					if (!transportsThatNeedUnits.isEmpty())
					{
						// Get next empty transport and find its capacity
						final Unit transport = transportsThatNeedUnits.get(0);
						int transportCapacity = UnitAttachment.get(transport.getType()).getTransportCapacity();
						
						// Find any existing units that can be transported
						final List<Unit> selectedUnits = transportUtils.selectUnitsToTransportFromList(transport, new ArrayList<Unit>(potentialUnitsToLoad));
						if (!selectedUnits.isEmpty())
						{
							potentialUnitsToLoad.removeAll(selectedUnits);
							transportCapacity -= transportUtils.findUnitsTransportCost(selectedUnits);
						}
						
						// Purchase units until transport is full
						while (transportCapacity > 0)
						{
							// Remove options that cost too much PUs or production
							purchaseUtils.removePurchaseOptionsByCostAndProductionAndLimits(player, startOfTurnData, amphibPurchaseOptionsForTerritory, PUsRemaining, remainingUnitProduction,
										amphibUnitsToPlace, purchaseTerritories);
							if (amphibPurchaseOptionsForTerritory.isEmpty())
								break;
							
							// Find amphib purchase option
							final Map<ProPurchaseOption, Double> amphibEfficiencies = new HashMap<ProPurchaseOption, Double>();
							for (final ProPurchaseOption ppo : amphibPurchaseOptionsForTerritory)
							{
								if (ppo.getTransportCost() <= transportCapacity)
									amphibEfficiencies.put(ppo, ppo.getAmphibEfficiency(data, ownedLocalAmphibUnits, amphibUnitsToPlace));
							}
							if (amphibEfficiencies.isEmpty())
								break;
							
							// Select amphib purchase option and add units
							final ProPurchaseOption ppo = purchaseUtils.randomizePurchaseOption(amphibEfficiencies, "Amphib");
							final List<Unit> amphibUnits = ppo.getUnitType().create(ppo.getQuantity(), player, true);
							amphibUnitsToPlace.addAll(amphibUnits);
							PUsRemaining -= ppo.getCost();
							remainingUnitProduction -= ppo.getQuantity();
							transportCapacity -= ppo.getTransportCost();
							LogUtils.log(Level.FINEST, "Selected unit=" + ppo.getUnitType().getName());
						}
						transportsThatNeedUnits.remove(transport);
					}
					else
					{
						// Remove options that cost too much PUs or production
						purchaseUtils.removePurchaseOptionsByCostAndProductionAndLimits(player, startOfTurnData, seaTransportPurchaseOptionsForTerritory, PUsRemaining, remainingUnitProduction,
									transportUnitsToPlace, purchaseTerritories);
						if (seaTransportPurchaseOptionsForTerritory.isEmpty())
							break;
						
						// Select purchase option
						final Map<ProPurchaseOption, Double> transportEfficiencies = new HashMap<ProPurchaseOption, Double>();
						for (final ProPurchaseOption ppo : seaTransportPurchaseOptionsForTerritory)
							transportEfficiencies.put(ppo, ppo.getTransportEfficiency(data));
						final ProPurchaseOption ppo = purchaseUtils.randomizePurchaseOption(transportEfficiencies, "Sea Transport");
						
						// Add transports
						final List<Unit> transports = ppo.getUnitType().create(ppo.getQuantity(), player, true);
						transportUnitsToPlace.addAll(transports);
						PUsRemaining -= ppo.getCost();
						remainingUnitProduction -= ppo.getQuantity();
						transportsThatNeedUnits.addAll(transports);
						LogUtils.log(Level.FINEST, "Selected unit=" + ppo.getUnitType().getName() + ", potentialUnitsToLoad=" + potentialUnitsToLoad + ", transportsThatNeedUnits="
									+ transportsThatNeedUnits);
					}
				}
				
				// Add transport units to sea place territory and amphib units to land place territory
				for (final ProPlaceTerritory ppt : purchaseTerritory.getCanPlaceTerritories())
				{
					if (landTerritory.equals(ppt.getTerritory()))
						ppt.getPlaceUnits().addAll(amphibUnitsToPlace);
					else if (placeTerritory.equals(ppt))
						ppt.getPlaceUnits().addAll(transportUnitsToPlace);
				}
				LogUtils.log(Level.FINEST, t + ", purchaseTerritory=" + landTerritory + ", transportUnitsToPlace=" + transportUnitsToPlace + ", amphibUnitsToPlace=" + amphibUnitsToPlace);
			}
		}
		
		return PUsRemaining;
	}
	
	private int purchaseUnitsWithRemainingProduction(final Map<Territory, ProPurchaseTerritory> purchaseTerritories, int PUsRemaining, final List<ProPurchaseOption> landPurchaseOptions,
				final List<ProPurchaseOption> airPurchaseOptions)
	{
		if (PUsRemaining == 0)
			return PUsRemaining;
		LogUtils.log(Level.FINE, "Purchase units in territories with remaining production with PUsRemaining=" + PUsRemaining);
		
		// Get all safe/unsafe land place territories with remaining production
		final List<ProPlaceTerritory> prioritizedLandTerritories = new ArrayList<ProPlaceTerritory>();
		final List<ProPlaceTerritory> prioritizedCantHoldLandTerritories = new ArrayList<ProPlaceTerritory>();
		for (final ProPurchaseTerritory ppt : purchaseTerritories.values())
		{
			for (final ProPlaceTerritory placeTerritory : ppt.getCanPlaceTerritories())
			{
				final Territory t = placeTerritory.getTerritory();
				if (!t.isWater() && placeTerritory.isCanHold() && purchaseTerritories.get(t).getRemainingUnitProduction() > 0)
					prioritizedLandTerritories.add(placeTerritory);
				else if (!t.isWater() && purchaseTerritories.get(t).getRemainingUnitProduction() > 0)
					prioritizedCantHoldLandTerritories.add(placeTerritory);
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
				// Remove options that cost too much PUs or production
				purchaseUtils.removePurchaseOptionsByCostAndProductionAndLimits(player, startOfTurnData, purchaseOptionsForTerritory, PUsRemaining, remainingUnitProduction,
							new ArrayList<Unit>(), purchaseTerritories);
				if (purchaseOptionsForTerritory.isEmpty())
					break;
				
				// Determine best long range attack option (prefer air units)
				ProPurchaseOption bestAttackOption = null;
				double maxAttackEfficiency = 0;
				for (final ProPurchaseOption ppo : purchaseOptionsForTerritory)
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
		
		// Sort territories by value
		Collections.sort(prioritizedCantHoldLandTerritories, new Comparator<ProPlaceTerritory>()
		{
			public int compare(final ProPlaceTerritory t1, final ProPlaceTerritory t2)
			{
				final double value1 = t1.getDefenseValue();
				final double value2 = t2.getDefenseValue();
				return Double.compare(value2, value1);
			}
		});
		LogUtils.log(Level.FINER, "Sorted can't hold land territories with remaining production: " + prioritizedCantHoldLandTerritories);
		
		// Loop through territories and purchase defense units
		for (final ProPlaceTerritory placeTerritory : prioritizedCantHoldLandTerritories)
		{
			final Territory t = placeTerritory.getTerritory();
			LogUtils.log(Level.FINER, "Checking territory: " + t);
			
			// Find local owned units
			final List<Unit> ownedLocalUnits = t.getUnits().getMatches(Matches.unitIsOwnedBy(player));
			
			// Determine units that can be produced in this territory
			final List<ProPurchaseOption> airAndLandPurchaseOptions = new ArrayList<ProPurchaseOption>(airPurchaseOptions);
			airAndLandPurchaseOptions.addAll(landPurchaseOptions);
			final List<ProPurchaseOption> purchaseOptionsForTerritory = purchaseUtils.findPurchaseOptionsForTerritory(player, airAndLandPurchaseOptions, t);
			
			// Purchase defense units for any remaining production
			int remainingUnitProduction = purchaseTerritories.get(t).getRemainingUnitProduction();
			while (true)
			{
				// Remove options that cost too much PUs or production
				purchaseUtils.removePurchaseOptionsByCostAndProductionAndLimits(player, startOfTurnData, purchaseOptionsForTerritory, PUsRemaining, remainingUnitProduction,
							new ArrayList<Unit>(), purchaseTerritories);
				if (purchaseOptionsForTerritory.isEmpty())
					break;
				
				// Select purchase option
				final Map<ProPurchaseOption, Double> defenseEfficiencies = new HashMap<ProPurchaseOption, Double>();
				for (final ProPurchaseOption ppo : purchaseOptionsForTerritory)
					defenseEfficiencies.put(ppo, Math.pow(ppo.getCost(), 2) * ppo.getDefenseEfficiency2(1, data, ownedLocalUnits, placeTerritory.getPlaceUnits()));
				final ProPurchaseOption selectedOption = purchaseUtils.randomizePurchaseOption(defenseEfficiencies, "Defense");
				
				// Purchase unit
				PUsRemaining -= selectedOption.getCost();
				remainingUnitProduction -= selectedOption.getQuantity();
				final List<Unit> newUnit = selectedOption.getUnitType().create(selectedOption.getQuantity(), player, true);
				placeTerritory.getPlaceUnits().addAll(newUnit);
				LogUtils.log(Level.FINEST, t + ", addedUnit=" + newUnit);
			}
		}
		
		return PUsRemaining;
	}
	
	private int upgradeUnitsWithRemainingPUs(final Map<Territory, ProPurchaseTerritory> purchaseTerritories, int PUsRemaining, final ProPurchaseOptionMap purchaseOptions)
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
			final List<ProPurchaseOption> airAndLandPurchaseOptions = new ArrayList<ProPurchaseOption>(purchaseOptions.getAirOptions());
			airAndLandPurchaseOptions.addAll(purchaseOptions.getLandOptions());
			final List<ProPurchaseOption> purchaseOptionsForTerritory = purchaseUtils.findPurchaseOptionsForTerritory(player, airAndLandPurchaseOptions, t);
			
			// Purchase long range attack units for any remaining production
			int remainingUpgradeUnits = purchaseTerritories.get(t).getUnitProduction() / 3;
			while (true)
			{
				if (remainingUpgradeUnits <= 0)
					break;
				
				// Find cheapest placed purchase option
				int maxPlacedCost = Integer.MIN_VALUE;
				ProPurchaseOption maxPurchaseOption = null;
				for (final Unit u : placeTerritory.getPlaceUnits())
				{
					for (final ProPurchaseOption ppo : airAndLandPurchaseOptions)
					{
						if (u.getType().equals(ppo.getUnitType()) && ppo.getCost() > maxPlacedCost)
						{
							maxPlacedCost = ppo.getCost();
							maxPurchaseOption = ppo;
						}
					}
				}
				if (maxPurchaseOption == null)
					break;
				
				// Remove options that cost too much PUs or production
				purchaseUtils.removePurchaseOptionsByCostAndProductionAndLimits(player, startOfTurnData, purchaseOptionsForTerritory, PUsRemaining + maxPlacedCost, 1,
							new ArrayList<Unit>(), purchaseTerritories);
				if (purchaseOptionsForTerritory.isEmpty())
					break;
				
				// Determine best long range attack option (prefer air units)
				ProPurchaseOption bestAttackOption = null;
				double maxAttackEfficiency = maxPurchaseOption.getAttackEfficiency() * maxPurchaseOption.getMovement() * maxPurchaseOption.getCost() / maxPurchaseOption.getQuantity();
				for (final ProPurchaseOption ppo : purchaseOptionsForTerritory)
				{
					if (ppo.getCost() > maxPlacedCost && (ppo.isAir() || placeTerritory.getStrategicValue() >= 0.25 || ppo.getTransportCost() <= maxPurchaseOption.getTransportCost()))
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
				{
					airAndLandPurchaseOptions.remove(maxPurchaseOption);
					continue;
				}
				
				// Find units to remove
				final List<Unit> unitsToRemove = new ArrayList<Unit>();
				int numUnitsToRemove = maxPurchaseOption.getQuantity();
				for (final Unit u : placeTerritory.getPlaceUnits())
				{
					if (numUnitsToRemove <= 0)
						break;
					if (u.getType().equals(maxPurchaseOption.getUnitType()))
					{
						unitsToRemove.add(u);
						numUnitsToRemove--;
					}
				}
				if (numUnitsToRemove > 0)
				{
					airAndLandPurchaseOptions.remove(maxPurchaseOption);
					continue;
				}
				
				// Replace units
				PUsRemaining += maxPurchaseOption.getCost();
				remainingUpgradeUnits -= maxPurchaseOption.getQuantity();
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
	
	private IntegerMap<ProductionRule> populateProductionRuleMap(final Map<Territory, ProPurchaseTerritory> purchaseTerritories, final ProPurchaseOptionMap purchaseOptions)
	{
		LogUtils.log(Level.FINE, "Populate production rule map");
		
		final List<Unit> unplacedUnits = player.getUnits().getMatches(Matches.UnitIsNotSea);
		final IntegerMap<ProductionRule> purchaseMap = new IntegerMap<ProductionRule>();
		for (final ProPurchaseOption ppo : purchaseOptions.getAllOptions())
		{
			int numUnits = 0;
			for (final Territory t : purchaseTerritories.keySet())
			{
				for (final ProPlaceTerritory ppt : purchaseTerritories.get(t).getCanPlaceTerritories())
				{
					for (final Unit u : ppt.getPlaceUnits())
					{
						if (u.getUnitType().equals(ppo.getUnitType()) && !unplacedUnits.contains(u))
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
			final PlaceableUnits placeableUnits = placeDelegate.getPlaceableUnits(player.getUnits().getMatches(Matches.UnitIsNotConstruction), t);
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
				finalResult = battleUtils.calculateBattleResults(player, t, new ArrayList<Unit>(enemyAttackingUnits), defenders, enemyAttackMap.get(t).getMaxBombardUnits(), false);
				
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
				final List<ProPlaceTerritory> prioritizedLandTerritories, final IAbstractPlaceDelegate placeDelegate, final boolean isConstruction)
	{
		LogUtils.log(Level.FINE, "Place land with isConstruction=" + isConstruction + ", units=" + player.getUnits().getUnits());
		
		Match<Unit> unitMatch = Matches.UnitIsNotConstruction;
		if (isConstruction)
			unitMatch = Matches.UnitIsConstruction;
		
		// Loop through prioritized territories and place land units
		for (final ProPlaceTerritory placeTerritory : prioritizedLandTerritories)
		{
			final Territory t = placeTerritory.getTerritory();
			LogUtils.log(Level.FINER, "Checking land place for " + t.getName());
			
			// Check if any units can be placed
			final PlaceableUnits placeableUnits = placeDelegate.getPlaceableUnits(player.getUnits().getMatches(unitMatch), t);
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
			
			// Place as many units as possible
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
	
	private List<ProPurchaseTerritory> getPurchaseTerritories(final ProPlaceTerritory placeTerritory, final Map<Territory, ProPurchaseTerritory> purchaseTerritories)
	{
		final List<ProPurchaseTerritory> territories = new ArrayList<ProPurchaseTerritory>();
		for (final Territory t : purchaseTerritories.keySet())
		{
			for (final ProPlaceTerritory ppt : purchaseTerritories.get(t).getCanPlaceTerritories())
			{
				if (placeTerritory.equals(ppt))
					territories.add(purchaseTerritories.get(t));
			}
		}
		return territories;
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
