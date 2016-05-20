package games.strategy.triplea.ai.proAI;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.ai.proAI.logging.ProLogger;
import games.strategy.triplea.ai.proAI.util.ProUtils;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveValidator;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.triplea.delegate.dataObjects.PlaceableUnits;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.IntegerMap;
import games.strategy.util.InverseMatch;
import games.strategy.util.Match;

/**
 * Pro bid AI.
 */
public class ProBidAI {

  private final static int PURCHASE_LOOP_MAX_TIME_MILLIS = 150 * 1000;

  private GameData data;

  public void bid(int PUsToSpend, final IPurchaseDelegate purchaseDelegate, final GameData data, final PlayerID player) {
    ProLogger.info("Starting bid purchase phase");

    // Current data at the start of combat move
    this.data = data;
    if (PUsToSpend == 0 && player.getResources().getQuantity(data.getResourceList().getResource(Constants.PUS)) == 0) {
      return;
    }

    // breakdown Rules by type and cost
    int highPrice = 0;
    final List<ProductionRule> rules = player.getProductionFrontier().getRules();
    final IntegerMap<ProductionRule> purchase = new IntegerMap<>();
    final List<ProductionRule> landProductionRules = new ArrayList<>();
    final List<ProductionRule> airProductionRules = new ArrayList<>();
    final List<ProductionRule> seaProductionRules = new ArrayList<>();
    final List<ProductionRule> transportProductionRules = new ArrayList<>();
    final List<ProductionRule> subProductionRules = new ArrayList<>();
    final IntegerMap<ProductionRule> bestAttack = new IntegerMap<>();
    final IntegerMap<ProductionRule> bestDefense = new IntegerMap<>();
    final IntegerMap<ProductionRule> bestTransport = new IntegerMap<>();
    final IntegerMap<ProductionRule> bestMaxUnits = new IntegerMap<>();
    final IntegerMap<ProductionRule> bestMobileAttack = new IntegerMap<>();

    ProductionRule carrierRule = null, fighterRule = null;
    int carrierFighterLimit = 0, maxFighterAttack = 0;
    float averageSeaMove = 0;
    final Resource pus = data.getResourceList().getResource(Constants.PUS);
    final boolean isAmphib = isAmphibAttack(player, true);
    for (final ProductionRule ruleCheck : rules) {
      final int costCheck = ruleCheck.getCosts().getInt(pus);
      final NamedAttachable resourceOrUnit = ruleCheck.getResults().keySet().iterator().next();
      if (!(resourceOrUnit instanceof UnitType)) {
        continue;
      }
      final UnitType x = (UnitType) resourceOrUnit;
      // Remove from consideration any unit with Zero Movement
      if (UnitAttachment.get(x).getMovement(player) < 1 && !(UnitAttachment.get(x).getCanProduceUnits())) {
        continue;
      }
      // Remove from consideration any unit with Zero defense, or 3 or more attack/defense than defense/attack, that is
      // not a
      // transport/factory/aa unit
      if (((UnitAttachment.get(x).getAttack(player) - UnitAttachment.get(x).getDefense(player) >= 3 || UnitAttachment
          .get(x).getDefense(player) - UnitAttachment.get(x).getAttack(player) >= 3) || UnitAttachment.get(x)
          .getDefense(player) < 1)
          && !(UnitAttachment.get(x).getCanProduceUnits() || (UnitAttachment.get(x).getTransportCapacity() > 0 && Matches.UnitTypeIsSea
              .match(x)))) {
        // maybe the map only has weird units. make sure there is at least one of each type before we decide not to use
        // it (we are relying
        // on the fact that map makers generally put specialty units AFTER useful units in their production lists [ie:
        // bombers listed after
        // fighters, mortars after artillery, etc.])
        if (Matches.UnitTypeIsAir.match(x) && !airProductionRules.isEmpty()) {
          continue;
        }
        if (Matches.UnitTypeIsSea.match(x) && !seaProductionRules.isEmpty()) {
          continue;
        }
        if (!Matches.UnitTypeCanProduceUnits.match(x) && !landProductionRules.isEmpty()
            && !Matches.UnitTypeIsAir.match(x) && !Matches.UnitTypeIsSea.match(x)) {
          continue;
        }
      }
      // Remove from consideration any unit which has maxBuiltPerPlayer
      if (Matches.UnitTypeHasMaxBuildRestrictions.match(x)) {
        continue;
      }
      // Remove from consideration any unit which has consumesUnits
      if (Matches.UnitTypeConsumesUnitsOnCreation.match(x)) {
        continue;
      }
      if (Matches.UnitTypeIsAir.match(x)) {
        airProductionRules.add(ruleCheck);
      } else if (Matches.UnitTypeIsSea.match(x)) {
        seaProductionRules.add(ruleCheck);
        averageSeaMove += UnitAttachment.get(x).getMovement(player);
      } else if (!Matches.UnitTypeCanProduceUnits.match(x)) {
        if (costCheck > highPrice) {
          highPrice = costCheck;
        }
        landProductionRules.add(ruleCheck);
      }
      if (Matches.UnitTypeCanTransport.match(x) && Matches.UnitTypeIsSea.match(x)) {
        // might be more than 1 transport rule... use ones that can hold at least "2" capacity (we should instead check
        // for median transport
        // cost, and then add all those at or above that capacity)
        if (UnitAttachment.get(x).getTransportCapacity() > 1) {
          transportProductionRules.add(ruleCheck);
        }
      }
      if (Matches.UnitTypeIsSub.match(x)) {
        subProductionRules.add(ruleCheck);
      }
      if (Matches.UnitTypeIsCarrier.match(x)) // might be more than 1 carrier rule...use the one which will hold the
                                              // most fighters
      {
        final int thisFighterLimit = UnitAttachment.get(x).getCarrierCapacity();
        if (thisFighterLimit >= carrierFighterLimit) {
          carrierRule = ruleCheck;
          carrierFighterLimit = thisFighterLimit;
        }
      }
      if (Matches.UnitTypeCanLandOnCarrier.match(x)) // might be more than 1 fighter...use the one with the best attack
      {
        final int thisFighterAttack = UnitAttachment.get(x).getAttack(player);
        if (thisFighterAttack > maxFighterAttack) {
          fighterRule = ruleCheck;
          maxFighterAttack = thisFighterAttack;
        }
      }
    }
    if (averageSeaMove / seaProductionRules.size() >= 1.8) // most sea units move at least 2 movement, so remove any sea
                                                           // units with 1
                                                           // movement (dumb t-boats) (some maps like 270BC have mostly
                                                           // 1 movement sea
                                                           // units, so we must be sure not to remove those)
    {
      final List<ProductionRule> seaProductionRulesCopy = new ArrayList<>(seaProductionRules);
      for (final ProductionRule seaRule : seaProductionRulesCopy) {
        final NamedAttachable resourceOrUnit = seaRule.getResults().keySet().iterator().next();
        if (!(resourceOrUnit instanceof UnitType)) {
          continue;
        }
        final UnitType x = (UnitType) resourceOrUnit;
        if (UnitAttachment.get(x).getMovement(player) < 2) {
          seaProductionRules.remove(seaRule);
        }
      }
    }
    if (subProductionRules.size() > 0 && seaProductionRules.size() > 0) {
      if (subProductionRules.size() / seaProductionRules.size() < 0.3) // remove submarines from consideration, unless
                                                                       // we are mostly subs
      {
        seaProductionRules.removeAll(subProductionRules);
      }
    }
    int buyLimit = PUsToSpend / 3;
    if (buyLimit == 0) {
      buyLimit = 1;
    }
    boolean landPurchase = true, goTransports = false;
    // boolean alreadyBought = false;
    final List<Territory> enemyTerritoryBorderingOurTerrs = getNeighboringEnemyLandTerritories(data, player);
    if (enemyTerritoryBorderingOurTerrs.isEmpty()) {
      landPurchase = false;
    }
    if (Math.random() > 0.25) {
      seaProductionRules.removeAll(subProductionRules);
    }
    if (PUsToSpend < 25) {
      if ((!isAmphib || Math.random() < 0.15) && landPurchase) {
        findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack, landProductionRules,
            PUsToSpend, buyLimit, data, player, 2);
      } else {
        landPurchase = false;
        buyLimit = PUsToSpend / 5; // assume a larger threshhold
        if (Math.random() > 0.40) {
          findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack, seaProductionRules,
              PUsToSpend, buyLimit, data, player, 2);
        } else {
          goTransports = true;
        }
      }
    } else if ((!isAmphib || Math.random() < 0.15) && landPurchase) {
      if (Math.random() > 0.80) {
        findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack, landProductionRules,
            PUsToSpend, buyLimit, data, player, 2);
      }
    } else if (Math.random() < 0.35) {
      if (Math.random() > 0.55 && carrierRule != null && fighterRule != null) {// force a carrier purchase if enough
                                                                               // available $$ for it and
                                                                               // at least 1 fighter
        final int cost = carrierRule.getCosts().getInt(pus);
        final int fighterCost = fighterRule.getCosts().getInt(pus);
        if ((cost + fighterCost) <= PUsToSpend) {
          purchase.add(carrierRule, 1);
          purchase.add(fighterRule, 1);
          carrierFighterLimit--;
          PUsToSpend -= (cost + fighterCost);
          while ((PUsToSpend >= fighterCost) && carrierFighterLimit > 0) { // max out the carrier
            purchase.add(fighterRule, 1);
            carrierFighterLimit--;
            PUsToSpend -= fighterCost;
          }
        }
      }
      final int airPUs = PUsToSpend / 6;
      findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack, airProductionRules,
          airPUs, buyLimit, data, player, 2);
      final boolean buyAttack = Math.random() > 0.50;
      for (final ProductionRule rule1 : airProductionRules) {
        int buyThese = bestAttack.getInt(rule1);
        final int cost = rule1.getCosts().getInt(pus);
        if (!buyAttack) {
          buyThese = bestDefense.getInt(rule1);
        }
        PUsToSpend -= cost * buyThese;
        while (PUsToSpend < 0 && buyThese > 0) {
          buyThese--;
          PUsToSpend += cost;
        }
        if (buyThese > 0) {
          purchase.add(rule1, buyThese);
        }
      }
      final int landPUs = PUsToSpend;
      buyLimit = landPUs / 3;
      bestAttack.clear();
      bestDefense.clear();
      bestMaxUnits.clear();
      bestMobileAttack.clear();
      findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack, landProductionRules,
          landPUs, buyLimit, data, player, 2);
    } else {
      landPurchase = false;
      buyLimit = PUsToSpend / 8; // assume higher end purchase
      seaProductionRules.addAll(airProductionRules);
      if (Math.random() > 0.45) {
        findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack, seaProductionRules,
            PUsToSpend, buyLimit, data, player, 2);
      } else {
        goTransports = true;
      }
    }
    final List<ProductionRule> processRules = new ArrayList<>();
    if (landPurchase) {
      processRules.addAll(landProductionRules);
    } else {
      if (goTransports) {
        processRules.addAll(transportProductionRules);
      } else {
        processRules.addAll(seaProductionRules);
      }
    }
    final boolean buyAttack = Math.random() > 0.25;
    int buyThese = 0;
    for (final ProductionRule rule1 : processRules) {
      final int cost = rule1.getCosts().getInt(pus);
      if (goTransports) {
        buyThese = PUsToSpend / cost;
      } else if (buyAttack) {
        buyThese = bestAttack.getInt(rule1);
      } else if (Math.random() <= 0.25) {
        buyThese = bestDefense.getInt(rule1);
      } else {
        buyThese = bestMaxUnits.getInt(rule1);
      }
      PUsToSpend -= cost * buyThese;
      while (buyThese > 0 && PUsToSpend < 0) {
        buyThese--;
        PUsToSpend += cost;
      }
      if (buyThese > 0) {
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
      findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack, landProductionRules,
          PUsToSpend, buyLimit, data, player, 2);
      for (final ProductionRule rule2 : landProductionRules) {
        final int cost = rule2.getCosts().getInt(pus);
        buyThese = bestDefense.getInt(rule2);
        PUsToSpend -= cost * buyThese;
        while (buyThese > 0 && PUsToSpend < 0) {
          buyThese--;
          PUsToSpend += cost;
        }
        if (buyThese > 0) {
          purchase.add(rule2, buyThese);
        }
      }
    }
    purchaseDelegate.purchase(purchase);
  }

  private boolean isAmphibAttack(final PlayerID player, final boolean requireWaterFactory) {
    final Territory capitol = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
    if (capitol == null || !capitol.getOwner().equals(player)) {
      return false;
    }
    if (requireWaterFactory) {
      final List<Territory> factories = findTersWithUnitsMatching(data, player, Matches.UnitCanProduceUnits);
      final List<Territory> waterFactories = stripLandLockedTerr(data, factories);
      if (waterFactories.isEmpty()) {
        return false;
      }
    }
    // find a land route to an enemy territory from our capitol
    boolean amphibPlayer = !hasLandRouteToEnemyOwnedCapitol(capitol, player, data);
    int totProduction = 0, allProduction = 0;
    if (amphibPlayer) {
      final List<Territory> allFactories = findTersWithUnitsMatching(data, player, Matches.UnitCanProduceUnits);
      // allFactories.remove(capitol);
      for (final Territory checkFactory : allFactories) {
        final boolean isLandRoute = hasLandRouteToEnemyOwnedCapitol(checkFactory, player, data);
        final int factProduction =
            TripleAUnit.getProductionPotentialOfTerritory(checkFactory.getUnits().getUnits(), checkFactory, player,
                data, false, true);
        allProduction += factProduction;
        if (isLandRoute) {
          totProduction += factProduction;
        }
      }
    }
    // if the land based production is greater than 2/5 (used to be 1/3) of all factory production, turn off amphib
    // works better on NWO where Brits start with factories in North Africa
    amphibPlayer = amphibPlayer ? (totProduction * 5 < allProduction * 2) : false;
    return amphibPlayer;
  }

  // TODO: Rewrite this as its from the Moore AI
  public void bidPlace(final IAbstractPlaceDelegate placeDelegate, final GameData data, final PlayerID player) {
    ProLogger.info("Starting bid place phase");
    // if we have purchased a factory, it will be a priority for placing units
    // should place most expensive on it
    // need to be able to handle AA purchase
    if (player.getUnits().isEmpty()) {
      return;
    }
    final Collection<Territory> impassableTerrs = new ArrayList<>();
    for (final Territory t : data.getMap().getTerritories()) {
      if (Matches.TerritoryIsPassableAndNotRestricted(player, data).invert().match(t)
          && Matches.TerritoryIsLand.match(t)) {
        impassableTerrs.add(t);
      }
    }
    final boolean tFirst = !games.strategy.triplea.Properties.getTransportCasualtiesRestricted(data);
    final CompositeMatch<Unit> ownedUnit = new CompositeMatchAnd<>(Matches.unitIsOwnedBy(player));
    final CompositeMatch<Unit> attackUnit = new CompositeMatchAnd<>(Matches.UnitIsSea, Matches.UnitIsNotTransport);
    final CompositeMatch<Unit> enemyUnit = new CompositeMatchAnd<>(Matches.enemyUnit(player, data));
    final CompositeMatch<Unit> enemyAttackUnit = new CompositeMatchAnd<>(attackUnit, enemyUnit);
    // CompositeMatch<Unit> enemyTransUnit = new CompositeMatchAnd<Unit>(transUnit, enemyUnit);
    final CompositeMatch<Unit> ourFactory = new CompositeMatchAnd<>(ownedUnit, Matches.UnitCanProduceUnits);
    // CompositeMatch<Territory> ourLandTerr = new CompositeMatchAnd<Territory>(Matches.isTerritoryOwnedBy(player),
    // Matches.TerritoryIsLand);
    final Territory capitol = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
    final List<Territory> factoryTerritories =
        Match.getMatches(findUnitTerr(data, player, ourFactory), Matches.isTerritoryOwnedBy(player));
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
    final List<Territory> ourFriendlyTerr = new ArrayList<>();
    final List<Territory> ourEnemyTerr = new ArrayList<>();
    final List<Territory> ourSemiRankedBidTerrs = new ArrayList<>();
    final List<Territory> ourTerrs = allOurTerritories(data, player);
    ourTerrs.remove(capitol); // we'll check the cap last
    final HashMap<Territory, Float> rankMap =
        rankTerritories(data, ourFriendlyTerr, ourEnemyTerr, null, player, tFirst, false, true);
    final List<Territory> ourTerrWithEnemyNeighbors = getTerritoriesWithEnemyNeighbor(data, player, false, false);
    reorder(ourTerrWithEnemyNeighbors, rankMap, true);
    // ourFriendlyTerr.retainAll(ourTerrs);
    if (ourTerrWithEnemyNeighbors.contains(capitol)) {
      ourTerrWithEnemyNeighbors.remove(capitol);
      ourTerrWithEnemyNeighbors.add(capitol); // move capitol to the end of the list, if it is touching enemies
    }
    Territory bidLandTerr = null;
    if (ourTerrWithEnemyNeighbors.size() > 0) {
      bidLandTerr = ourTerrWithEnemyNeighbors.get(0);
    }
    if (bidLandTerr == null) {
      bidLandTerr = capitol;
    }
    if (player.getUnits().someMatch(Matches.UnitIsSea)) {
      Territory bidSeaTerr = null, bidTransTerr = null;
      // CompositeMatch<Territory> enemyWaterTerr = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater,
      // Matches.territoryHasEnemyUnits(player, data));
      final CompositeMatch<Territory> waterFactoryWaterTerr =
          new CompositeMatchAnd<>(Matches.TerritoryIsWater, Matches.territoryHasOwnedNeighborWithOwnedUnitMatching(
              data, player, Matches.UnitCanProduceUnits));
      final List<Territory> enemySeaTerr = findUnitTerr(data, player, enemyAttackUnit);
      final List<Territory> isWaterTerr = onlyWaterTerr(data, enemySeaTerr);
      enemySeaTerr.retainAll(isWaterTerr);
      Territory maxEnemySeaTerr = null;
      int maxUnits = 0;
      for (final Territory seaTerr : enemySeaTerr) {
        final int unitCount = seaTerr.getUnits().countMatches(enemyAttackUnit);
        if (unitCount > maxUnits) {
          maxUnits = unitCount;
          maxEnemySeaTerr = seaTerr;
        }
      }
      final Route seaRoute = findNearest(maxEnemySeaTerr, waterFactoryWaterTerr, Matches.TerritoryIsWater, data);
      if (seaRoute != null) {
        final Territory checkSeaTerr = seaRoute.getEnd();
        if (checkSeaTerr != null) {
          final float seaStrength = getStrengthOfPotentialAttackers(checkSeaTerr, data, player, tFirst, false, null);
          final float aStrength = strength(checkSeaTerr.getUnits().getUnits(), false, true, tFirst);
          final float bStrength = strength(player.getUnits().getMatches(attackUnit), false, true, tFirst);
          final float totStrength = aStrength + bStrength;
          if (totStrength > 0.9F * seaStrength) {
            bidSeaTerr = checkSeaTerr;
          }
        }
      }
      for (final Territory factCheck : factoryTerritories) {
        if (bidSeaTerr == null) {
          bidSeaTerr = findASeaTerritoryToPlaceOn(factCheck, data, player, tFirst);
        }
        if (bidTransTerr == null) {
          bidTransTerr = findASeaTerritoryToPlaceOn(factCheck, data, player, tFirst);
        }
      }
      placeSeaUnits(true, data, bidSeaTerr, bidSeaTerr, placeDelegate, player);
    }
    if (player.getUnits().someMatch(Matches.UnitIsNotSea)) {
      ourSemiRankedBidTerrs.addAll(ourTerrWithEnemyNeighbors);
      ourTerrs.removeAll(ourTerrWithEnemyNeighbors);
      Collections.shuffle(ourTerrs);
      ourSemiRankedBidTerrs.addAll(ourTerrs);
      // need to remove places like greenland, iceland and west indies that have no route to the enemy, but somehow keep
      // places like borneo,
      // gibralter, etc.
      for (final Territory noRouteTerr : ourTerrs) {
        // do not place bids on areas that have no direct land access to an enemy, unless the value is 3 or greater
        if (distanceToEnemy(noRouteTerr, data, player, false) < 1 && TerritoryAttachment.getProduction(noRouteTerr) < 3) {
          ourSemiRankedBidTerrs.remove(noRouteTerr);
        }
      }
      final List<Territory> isWaterTerr = onlyWaterTerr(data, ourSemiRankedBidTerrs);
      ourSemiRankedBidTerrs.removeAll(isWaterTerr);
      ourSemiRankedBidTerrs.removeAll(impassableTerrs);
      // This will bid a max of 5 units to ALL territories except for the capitol. The capitol gets units last, and gets
      // unlimited units
      // (veqryn)
      final int maxBidPerTerritory = 5;
      int bidCycle = 0;
      while (!(player.getUnits().isEmpty()) && bidCycle < maxBidPerTerritory) {
        for (int i = 0; i <= ourSemiRankedBidTerrs.size() - 1; i++) {
          bidLandTerr = ourSemiRankedBidTerrs.get(i);
          placeAllWeCanOn(true, data, null, bidLandTerr, placeDelegate, player);
        }
        bidCycle++;
      }
      if (!player.getUnits().isEmpty()) {
        placeAllWeCanOn(true, data, null, capitol, placeDelegate, player);
      }
    }
  }

  private void placeSeaUnits(final boolean bid, final GameData data, final Territory seaPlaceAttack,
      final Territory seaPlaceTrans, final IAbstractPlaceDelegate placeDelegate, final PlayerID player) {
    final CompositeMatch<Unit> attackUnit = new CompositeMatchAnd<>(Matches.UnitIsSea, Matches.UnitIsNotTransport);
    final List<Unit> seaUnits = player.getUnits().getMatches(attackUnit);
    final List<Unit> transUnits = player.getUnits().getMatches(Matches.UnitIsTransport);
    final List<Unit> airUnits = player.getUnits().getMatches(Matches.UnitCanLandOnCarrier);
    final List<Unit> carrierUnits = player.getUnits().getMatches(Matches.UnitIsCarrier);
    if (carrierUnits.size() > 0 && airUnits.size() > 0
        && (Properties.getProduceFightersOnCarriers(data) || Properties.getLHTRCarrierProductionRules(data) || bid)) {
      int carrierSpace = 0;
      for (final Unit carrier1 : carrierUnits) {
        carrierSpace += UnitAttachment.get(carrier1.getType()).getCarrierCapacity();
      }
      final Iterator<Unit> airIter = airUnits.iterator();
      while (airIter.hasNext() && carrierSpace > 0) {
        final Unit airPlane = airIter.next();
        seaUnits.add(airPlane);
        carrierSpace -= UnitAttachment.get(airPlane.getType()).getCarrierCost();
      }
    }
    if (bid) {
      if (!seaUnits.isEmpty()) {
        doPlace(seaPlaceAttack, seaUnits, placeDelegate);
      }
      if (!transUnits.isEmpty()) {
        doPlace(seaPlaceTrans, transUnits, placeDelegate);
      }
      return;
    }
    if (seaUnits.isEmpty() && transUnits.isEmpty()) {
      return;
    }
    if (seaPlaceAttack == seaPlaceTrans) {
      seaUnits.addAll(transUnits);
      transUnits.clear();
    }
    final PlaceableUnits pu = placeDelegate.getPlaceableUnits(seaUnits, seaPlaceAttack);
    int pLeft = 0;
    if (pu.getErrorMessage() != null) {
      return;
    }
    if (!seaUnits.isEmpty()) {
      pLeft = pu.getMaxUnits();
      if (pLeft == -1) {
        pLeft = Integer.MAX_VALUE;
      }
      final int numPlace = Math.min(pLeft, seaUnits.size());
      pLeft -= numPlace;
      final Collection<Unit> toPlace = seaUnits.subList(0, numPlace);
      doPlace(seaPlaceAttack, toPlace, placeDelegate);
    }
    if (!transUnits.isEmpty()) {
      final PlaceableUnits pu2 = placeDelegate.getPlaceableUnits(transUnits, seaPlaceTrans);
      if (pu2.getErrorMessage() != null) {
        return;
      }
      pLeft = pu2.getMaxUnits();
      if (pLeft == -1) {
        pLeft = Integer.MAX_VALUE;
      }
      final int numPlace = Math.min(pLeft, transUnits.size());
      final Collection<Unit> toPlace = transUnits.subList(0, numPlace);
      doPlace(seaPlaceTrans, toPlace, placeDelegate);
    }
  }

  private void placeAllWeCanOn(final boolean bid, final GameData data, final Territory factoryPlace,
      final Territory placeAt, final IAbstractPlaceDelegate placeDelegate, final PlayerID player) {
    final CompositeMatch<Unit> landOrAir = new CompositeMatchOr<>(Matches.UnitIsAir, Matches.UnitIsLand);
    if (factoryPlace != null) // place a factory?
    {
      final Collection<Unit> toPlace =
          new ArrayList<>(player.getUnits().getMatches(Matches.UnitCanProduceUnitsAndIsConstruction));
      if (toPlace.size() == 1) // only 1 may have been purchased...anything greater is wrong
      {
        doPlace(factoryPlace, toPlace, placeDelegate);
        return;
      } else if (toPlace.size() > 1) {
        return;
      }
    }
    final List<Unit> landUnits = player.getUnits().getMatches(landOrAir);
    final Territory capitol = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
    final PlaceableUnits pu3 = placeDelegate.getPlaceableUnits(landUnits, placeAt);
    if (pu3.getErrorMessage() != null) {
      return;
    }
    int placementLeft3 = pu3.getMaxUnits();
    if (placementLeft3 == -1) {
      placementLeft3 = Integer.MAX_VALUE;
    }
    // allow placing only 1 unit per territory if a bid, unless it is the capitol (water is handled in placeseaunits)
    if (bid) {
      placementLeft3 = 1;
    }
    if (bid && (placeAt == capitol)) {
      placementLeft3 = 1000;
    }
    if (!landUnits.isEmpty()) {
      final int landPlaceCount = Math.min(placementLeft3, landUnits.size());
      placementLeft3 -= landPlaceCount;
      final Collection<Unit> toPlace = landUnits.subList(0, landPlaceCount);
      doPlace(placeAt, toPlace, placeDelegate);
    }
  }

  private void doPlace(final Territory t, final Collection<Unit> toPlace, final IAbstractPlaceDelegate del) {
    for (final Unit unit : toPlace) {
      final List<Unit> unitList = new ArrayList<>();
      unitList.add(unit);
      final String message = del.placeUnits(unitList, t);
      if (message != null) {
        ProLogger.warn(message);
        ProLogger.warn("Attempt was at: " + t + " with: " + unit);
      }
    }
    ProUtils.pause();
  }

  /**
   * All the territories that border one of our territories
   */
  private static List<Territory> getNeighboringEnemyLandTerritories(final GameData data, final PlayerID player) {
    final ArrayList<Territory> rVal = new ArrayList<>();
    for (final Territory t : data.getMap()) {
      if (Matches.isTerritoryEnemy(player, data).match(t) && Matches.TerritoryIsLand.match(t)
          && Matches.TerritoryIsNotImpassable.match(t)) {
        if (!data.getMap().getNeighbors(t, Matches.isTerritoryOwnedBy(player)).isEmpty()) {
          rVal.add(t);
        }
      }
    }
    return rVal;
  }

  /**
   * Take the mix of Production Rules and determine the best purchase set for attack, defense or transport
   * So much more that can be done with this...track units and try to minimize or maximize the # purchased
   */
  private static boolean findPurchaseMix(final IntegerMap<ProductionRule> bestAttack,
      final IntegerMap<ProductionRule> bestDefense, final IntegerMap<ProductionRule> bestTransport,
      final IntegerMap<ProductionRule> bestMaxUnits, final IntegerMap<ProductionRule> bestMobileAttack,
      final List<ProductionRule> rules, final int totPU, final int maxUnits, final GameData data,
      final PlayerID player, final int fighters) {
    // Resource key = data.getResourceList().getResource(Constants.PUS);
    final IntegerMap<String> parameters = new IntegerMap<>();
    parameters.put("attack", 0);
    parameters.put("defense", 0);
    parameters.put("maxAttack", 0);
    parameters.put("maxDefense", 0);
    parameters.put("maxUnitAttack", 0);
    parameters.put("maxTransAttack", 0);
    parameters.put("maxMobileAttack", 0);
    parameters.put("maxTransCost", 100000);
    parameters.put("maxAttackCost", 100000);
    parameters.put("maxUnitCount", 0);
    parameters.put("maxDefenseCost", 100000);
    parameters.put("maxUnitCost", 100000);
    parameters.put("totcost", 0);
    parameters.put("totUnit", 0);
    parameters.put("totMovement", 0);
    parameters.put("maxMovement", 0);
    // never changed
    parameters.put("maxUnits", maxUnits);
    // never changed
    parameters.put("maxCost", totPU);
    parameters.put("infantry", 0);
    parameters.put("nonInfantry", 0);
    final HashMap<ProductionRule, Boolean> infMap = new HashMap<>();
    final HashMap<ProductionRule, Boolean> nonInfMap = new HashMap<>();
    final HashMap<ProductionRule, Boolean> supportableInfMap = new HashMap<>();
    final Iterator<ProductionRule> prodIter = rules.iterator();
    final HashMap<ProductionRule, Boolean> transportMap = new HashMap<>();
    // int minCost = 10000;
    // ProductionRule minCostRule = null;
    while (prodIter.hasNext()) {
      final ProductionRule rule = prodIter.next();
      // initialize with 0
      bestAttack.put(rule, 0);
      bestDefense.put(rule, 0);
      bestMaxUnits.put(rule, 0);
      bestTransport.put(rule, 0);
      final NamedAttachable resourceOrUnit = rule.getResults().keySet().iterator().next();
      if (!(resourceOrUnit instanceof UnitType)) {
        continue;
      }
      final UnitType x = (UnitType) resourceOrUnit;
      supportableInfMap.put(rule, UnitAttachment.get(x).getArtillerySupportable());
      transportMap.put(rule, Matches.UnitTypeCanBeTransported.match(x));
      infMap.put(rule, Matches.UnitTypeIsInfantry.match(x));
      nonInfMap.put(rule, Matches.UnitTypeCanBeTransported.match(x) && Matches.UnitTypeIsInfantry.invert().match(x)
          && Matches.UnitTypeIsAAforAnything.invert().match(x));
    }
    final int countNum = 1;
    final int goodLoop =
        purchaseLoop(parameters, countNum, bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack,
            transportMap, infMap, nonInfMap, supportableInfMap, data, player, fighters);
    if (goodLoop > 0 && bestAttack.size() > 0 && bestDefense.size() > 0) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Recursive routine to determine the bestAttack and bestDefense set of purchase
   * Expects bestAttack to already be filled with the rules
   *
   * @param parameters
   *        - set of parameters to be used (8 of them)
   * @param ruleNum
   *        - which rule should the routine use
   * @param bestAttack
   *        - list of the rules and the number to be purchased (optimized for attack)
   * @param bestDefense
   *        - list of the rules and the number to be purchased (optimized for defense)
   * @param bestTransport
   *        - list of the rules and the number to be purchased (optimized for transporting)
   * @param bestMaxUnits
   *        - list of the rules and the number to be purchased (optimized for attack and max units)
   * @param bestTransport
   *        - list of the rules and the number to be purchased (optimized for transport)
   * @return - integer which is 1 if bestAttack has changed, 2 if bestDefense has changed, 3 if both have changed
   */
  private static int purchaseLoop(final IntegerMap<String> parameters, final int ruleNum,
      final IntegerMap<ProductionRule> bestAttack, final IntegerMap<ProductionRule> bestDefense,
      final IntegerMap<ProductionRule> bestTransport, final IntegerMap<ProductionRule> bestMaxUnits,
      final IntegerMap<ProductionRule> bestMobileAttack, final HashMap<ProductionRule, Boolean> transportMap,
      final HashMap<ProductionRule, Boolean> infMap, final HashMap<ProductionRule, Boolean> nonInfMap,
      final HashMap<ProductionRule, Boolean> supportableInfMap, final GameData data, final PlayerID player,
      final int fighters) {
    final long start = System.currentTimeMillis();
    /*
     * It is expected that this is called with a subset of possible units (i.e. just land Units or just Air Units)
     * Routine has the potential to be very costly if the number of rules is high
     * Computation cost is exponential with the number of rules: maxUnits^(number of rules(i.e. different Units))
     * Germany on revised map has maxunits of 14 and ships size is 5 --> 14^5 potential iterations (537824)
     * Becomes 1.4 billion if there are 8 units
     * intended to be self-nesting for each rule in bestAttack
     * countMax tells us which rule we are on...it should increase each time it is passed
     * parametersChanged tells us if the next call changed the parameters (forcing a change at this level)
     * thisParametersChanged tells us if this routine changed parameters either way (by calculation or by return from a
     * nested call)
     * Assumptions: 1) artillery purchased with infantry has a bonus
     * 2) fighters have attack: 3 and defense: 4 TODO: Recode this to use fighter attack/defense and to handle tech
     * bonus
     */
    final Resource key = data.getResourceList().getResource(Constants.PUS);
    final Set<ProductionRule> ruleCheck = bestAttack.keySet();
    final Iterator<ProductionRule> ruleIter = ruleCheck.iterator();
    int counter = 1;
    ProductionRule rule = null;
    while (counter <= ruleNum && ruleIter.hasNext()) {
      rule = ruleIter.next();
      counter++;
    }
    if (rule == null) {
      return 0;
    }
    Integer totAttack = parameters.getInt("attack");
    Integer totDefense = parameters.getInt("defense");
    Integer totCost = parameters.getInt("totcost");
    Integer totMovement = parameters.getInt("totMovement");
    final Integer maxCost = parameters.getInt("maxCost");
    final Integer maxUnits = parameters.getInt("maxUnits");
    Integer totUnits = parameters.getInt("totUnits");
    Integer maxAttack = parameters.getInt("maxAttack");
    Integer maxDefense = parameters.getInt("maxDefense");
    Integer maxTransAttack = parameters.getInt("maxTransAttack");
    Integer maxTransCost = parameters.getInt("maxTransCost");
    Integer maxAttackCost = parameters.getInt("maxAttackCost");
    Integer maxDefenseCost = parameters.getInt("maxDefenseCost");
    Integer maxUnitAttack = parameters.getInt("maxUnitAttack");
    Integer maxUnitCost = parameters.getInt("maxUnitCost");
    Integer maxUnitCount = parameters.getInt("maxUnitCount");
    Integer maxMobileAttack = parameters.getInt("maxMobileAttack");
    Integer maxMovement = parameters.getInt("maxMovement");
    Integer supportableInfCount = parameters.getInt("supportableInfCount");
    Integer infCount = parameters.getInt("infantry");
    Integer nonInfCount = parameters.getInt("nonInfantry");
    int parametersChanged = 0, thisParametersChanged = 0;
    final NamedAttachable resourceOrUnit = rule.getResults().keySet().iterator().next();
    if (!(resourceOrUnit instanceof UnitType)) {
      return 0;
    }
    final UnitType x = (UnitType) resourceOrUnit;
    final UnitAttachment u = UnitAttachment.get(x);
    final boolean thisIsSupportableInf = supportableInfMap.get(rule);
    final boolean thisIsInf = infMap.get(rule);
    final boolean thisIsNonInf = nonInfMap.get(rule);
    final boolean thisIsArt = u.getArtillery();
    final int uMovement = u.getMovement(player);
    int uAttack = u.getAttack(player);
    int uDefense = u.getDefense(player);
    final int aRolls = u.getAttackRolls(player);
    final int cost = rule.getCosts().getInt(key);
    // Discourage buying submarines, since the AI has no clue how to use them (veqryn)
    final boolean thisIsSub = u.getIsSub();
    if (thisIsSub && uAttack >= 1) {
      uAttack--;
    } else if (thisIsSub && uDefense >= 1) {
      uDefense--;
    }
    // Encourage buying balanced units. Added by veqryn, to decrease the rate at which the AI buys walls, fortresses,
    // and mortars, among
    // other specialty units that should not be bought often if at all.
    if (u.getMovement(player) == 0) {
      uAttack = 0;
    }
    if ((u.getAttack(player) == 0 || u.getDefense(player) - u.getAttack(player) >= 4) && u.getDefense(player) >= 1) {
      uDefense--;
      if (u.getDefense(player) - u.getAttack(player) >= 4) {
        uDefense--;
      }
    }
    if ((u.getDefense(player) == 0 || u.getAttack(player) - u.getDefense(player) >= 4) && u.getAttack(player) >= 1) {
      uAttack--;
      if (u.getAttack(player) - u.getDefense(player) >= 4) {
        uAttack--;
      }
    }
    // TODO: stop it from buying zero movement units under all circumstances. Also, lessen the number of artillery type
    // units bought
    // slightly. And lessen sub purchases, or eliminate entirely. (veqryn)
    // TODO: some transport ships have large capacity, others have a small capacity and are made for fighting. Make sure
    // if the AI is buying
    // transports, it chooses high capacity transports even if more expensive and less att/def than normal ships
    int fightersremaining = fighters;
    int usableMaxUnits = maxUnits;
    if (usableMaxUnits * ruleCheck.size() > 1000 && Math.random() <= 0.50) {
      usableMaxUnits = usableMaxUnits / 2;
    }
    for (int i = 0; i <= (usableMaxUnits - totUnits); i++) {
      if (i > 0) // allow 0 so that this unit might be skipped...due to low value...consider special capabilities later
      {
        totCost += cost;
        if (totCost > maxCost) {
          continue;
        }
        if (thisIsInf) {
          infCount++;
        } else if (thisIsNonInf) {
          nonInfCount++;
        }
        if (thisIsSupportableInf) {
          supportableInfCount++;
        }
        // give bonus of 1 hit per 2 units and if fighters are on the capital, a bonus for carrier equal to fighter
        // attack or defense
        int carrierLoad = Math.min(u.getCarrierCapacity(), fightersremaining);
        if (carrierLoad < 0) {
          carrierLoad = 0;
        }
        int bonusAttack = ((u.getHitPoints() - 1) * uAttack) + (uAttack > 0 && (i % 2) == 0 ? 1 : 0) + carrierLoad * 3;
        if (thisIsArt && i <= supportableInfCount) {
          // add one bonus for each artillery purchased with supportable infantry
          bonusAttack++;
        }
        final int bonusDefense =
            ((u.getHitPoints() - 1) * uDefense) + (uDefense > 0 && (i % 2) == 0 ? 1 : 0) + (carrierLoad * 4);
        fightersremaining -= carrierLoad;
        totUnits++;
        totAttack += uAttack * aRolls + bonusAttack;
        totDefense += uDefense * aRolls + bonusDefense;
        totMovement += uMovement;
      }
      if (totUnits <= maxUnits && ruleIter.hasNext()) {
        parameters.put("attack", totAttack);
        parameters.put("defense", totDefense);
        parameters.put("totcost", totCost);
        parameters.put("totUnits", totUnits);
        parameters.put("totMovement", totMovement);
        parameters.put("infantry", infCount);
        parameters.put("nonInfantry", nonInfCount);
        parameters.put("supportableInfCount", supportableInfCount);
        parametersChanged =
            purchaseLoop(parameters, counter, bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack,
                transportMap, infMap, nonInfMap, supportableInfMap, data, player, fighters);
        maxAttack = parameters.getInt("maxAttack");
        maxTransAttack = parameters.getInt("maxTransAttack");
        maxTransCost = parameters.getInt("maxTransCost");
        maxDefense = parameters.getInt("maxDefense");
        maxAttackCost = parameters.getInt("maxAttackCost");
        maxDefenseCost = parameters.getInt("maxDefenseCost");
        maxUnitCost = parameters.getInt("maxUnitCost");
        maxUnitAttack = parameters.getInt("maxUnitAttack");
        maxMobileAttack = parameters.getInt("maxMobileAttack");
        maxMovement = parameters.getInt("maxMovement");
        if (System.currentTimeMillis() - start > PURCHASE_LOOP_MAX_TIME_MILLIS) {
          break;
        }
      }
      if (totCost == 0) {
        continue;
      }
      // parameters changed: 001: attack, 010: defense, 100: maxUnits, 1000: transport, 10000: mobileAttack
      if (parametersChanged > 0) // change forced by another rule
      {
        if ((parametersChanged - 3) % 4 == 0) {
          bestAttack.put(rule, i);
          bestDefense.put(rule, i);
          thisParametersChanged = 3;
          parametersChanged -= 3;
        } else if ((parametersChanged - 1) % 4 == 0) {
          bestAttack.put(rule, i);
          if (thisParametersChanged % 2 == 0) {
            thisParametersChanged += 1;
          }
          parametersChanged -= 1;
        } else if ((parametersChanged - 2) % 4 == 0) {
          bestDefense.put(rule, i);
          if ((thisParametersChanged + 2) % 4 != 0 && (thisParametersChanged + 1) % 4 != 0) {
            thisParametersChanged += 2;
          }
          parametersChanged -= 2;
        }
        if ((parametersChanged > 0) && (parametersChanged - 4) % 8 == 0) {
          bestMaxUnits.put(rule, i);
          if (thisParametersChanged == 0 || (thisParametersChanged - 4) % 8 != 0) {
            thisParametersChanged += 4;
          }
          parametersChanged -= 4;
        }
        if ((parametersChanged - 8) % 16 == 0) {
          bestTransport.put(rule, i);
          if (thisParametersChanged == 0 || (thisParametersChanged - 8) % 16 != 0) {
            thisParametersChanged += 8;
          }
        }
        if (parametersChanged >= 16) {
          bestMobileAttack.put(rule, i);
          if (thisParametersChanged < 16) {
            thisParametersChanged += 16;
          }
        }
        parametersChanged = 0;
        continue;
      }
      if ((totAttack > maxAttack) || (totAttack == maxAttack && (Math.random() < 0.50))) {
        maxAttack = totAttack;
        maxAttackCost = totCost;
        parameters.put("maxAttack", maxAttack);
        parameters.put("maxAttackCost", maxAttackCost);
        bestAttack.put(rule, i);
        if (thisParametersChanged % 2 == 0) {
          thisParametersChanged += 1;
        }
        final Iterator<ProductionRule> changeIter = ruleCheck.iterator();
        ProductionRule changeThis = null;
        int countThis = 1;
        while (changeIter.hasNext()) // have to clear the rules below this rule
        {
          changeThis = changeIter.next();
          if (countThis >= counter) {
            bestAttack.put(changeThis, 0);
          }
          countThis++;
        }
      }
      if ((totDefense > maxDefense) || (totDefense == maxDefense && (Math.random() < 0.50))) {
        maxDefense = totDefense;
        maxDefenseCost = totCost;
        parameters.put("maxDefense", maxDefense);
        parameters.put("maxDefenseCost", maxDefenseCost);
        bestDefense.put(rule, i);
        if ((thisParametersChanged + 2) % 4 != 0 && (thisParametersChanged + 1) % 4 != 0) {
          thisParametersChanged += 2;
        }
        final Iterator<ProductionRule> changeIter = ruleCheck.iterator();
        ProductionRule changeThis = null;
        int countThis = 1;
        while (changeIter.hasNext()) // have to clear the rules below this rule
        {
          changeThis = changeIter.next();
          if (countThis >= counter) {
            bestDefense.put(changeThis, 0);
          }
          countThis++;
        }
      }
      if (totAttack > maxUnitAttack && totUnits >= maxUnitCount) {
        maxUnitAttack = totAttack;
        maxUnitCount = totUnits;
        maxUnitCost = totCost;
        parameters.put("maxUnitAttack", maxUnitAttack);
        parameters.put("maxUnitCount", maxUnitCount);
        parameters.put("maxUnitCost", maxUnitCost);
        bestMaxUnits.put(rule, i);
        if ((thisParametersChanged + 4) % 8 != 0) {
          thisParametersChanged += 4;
        }
        final Iterator<ProductionRule> changeIter = ruleCheck.iterator();
        ProductionRule changeThis = null;
        int countThis = 1;
        while (changeIter.hasNext()) // have to clear the rules below this rule
        {
          changeThis = changeIter.next();
          if (countThis >= counter) {
            bestMaxUnits.put(changeThis, 0);
          }
          countThis++;
        }
      }
      if (totAttack > maxTransAttack && (infCount <= nonInfCount + 1 && infCount >= nonInfCount - 1)) {
        maxTransAttack = totAttack;
        maxTransCost = totCost;
        parameters.put("maxTransAttack", totAttack);
        parameters.put("maxTransCost", maxTransCost);
        bestTransport.put(rule, i);
        if ((thisParametersChanged + 8) % 16 != 0) {
          thisParametersChanged += 8;
        }
        final Iterator<ProductionRule> changeIter = ruleCheck.iterator();
        ProductionRule changeThis = null;
        int countThis = 1;
        while (changeIter.hasNext()) {
          changeThis = changeIter.next();
          if (countThis >= counter) {
            bestTransport.put(changeThis, 0);
          }
          countThis++;
        }
      }
      if ((totAttack >= maxMobileAttack && (totMovement > maxMovement))
          || (totAttack > maxMobileAttack && (totMovement >= maxMovement))) {
        maxMobileAttack = totAttack;
        maxMovement = totMovement;
        parameters.put("maxMobileAttack", maxMobileAttack);
        parameters.put("maxMovement", maxMovement);
        bestMobileAttack.put(rule, i);
        if (thisParametersChanged < 16) {
          thisParametersChanged += 16;
        }
        final Iterator<ProductionRule> changeIter = ruleCheck.iterator();
        ProductionRule changeThis = null;
        int countThis = 1;
        while (changeIter.hasNext()) {
          changeThis = changeIter.next();
          if (countThis >= counter) {
            bestMobileAttack.put(changeThis, 0);
          }
          countThis++;
        }
      }
    }
    return thisParametersChanged;
  }

  /**
   * Return all territories that have units matching unitCondition and owned by us.
   *
   * @return List of territories
   */
  private static List<Territory> findTersWithUnitsMatching(final GameData data, final PlayerID player,
      final Match<Unit> unitCondition) {
    final CompositeMatch<Unit> unitMatch = new CompositeMatchAnd<>(unitCondition, Matches.unitIsOwnedBy(player));
    final List<Territory> result = new ArrayList<>();
    final Collection<Territory> allTers = data.getMap().getTerritories();
    for (final Territory ter : allTers) {
      if (ter.getUnits().someMatch(unitMatch)) {
        result.add(ter);
      }
    }
    return result;
  }

  /**
   * Returns a List of all territories with a water neighbor
   *
   * @param allTerr - List of Territories
   */
  private static List<Territory> stripLandLockedTerr(final GameData data, final List<Territory> allTerr) {
    final List<Territory> waterTerrs = new ArrayList<>(allTerr);
    final Iterator<Territory> wFIter = waterTerrs.iterator();
    while (wFIter.hasNext()) {
      final Territory waterFact = wFIter.next();
      if (Matches.territoryHasWaterNeighbor(data).invert().match(waterFact)) {
        wFIter.remove();
      }
    }
    return waterTerrs;
  }

  /**
   * true or false...does a land route exist from territory to any enemy owned capitol?
   */
  private static boolean hasLandRouteToEnemyOwnedCapitol(final Territory t, final PlayerID player, final GameData data) {
    for (final PlayerID ePlayer : data.getPlayerList().getPlayers()) {
      for (final Territory capital : TerritoryAttachment.getAllCapitals(ePlayer, data)) {
        if (data.getRelationshipTracker().isAtWar(player, capital.getOwner())
            && data.getMap().getDistance(t, capital, Matches.TerritoryIsNotImpassableToLandUnits(player, data)) != -1) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Return Territories containing any unit depending on unitCondition
   * Differs from findCertainShips because it doesn't require the units be owned
   */
  private static List<Territory> findUnitTerr(final GameData data, final PlayerID player,
      final Match<Unit> unitCondition) {
    // Return territories containing a certain unit or set of Units
    final CompositeMatch<Unit> limitShips = new CompositeMatchAnd<>(unitCondition);
    final List<Territory> shipTerr = new ArrayList<>();
    final Collection<Territory> tNeighbors = data.getMap().getTerritories();
    for (final Territory t2 : tNeighbors) {
      if (t2.getUnits().someMatch(limitShips)) {
        shipTerr.add(t2);
      }
    }
    return shipTerr;
  }

  /**
   * Territories we actually own in a modifiable List
   */
  private static List<Territory> allOurTerritories(final GameData data, final PlayerID player) {
    final Collection<Territory> ours = data.getMap().getTerritoriesOwnedBy(player);
    final List<Territory> ours2 = new ArrayList<>();
    ours2.addAll(ours);
    return ours2;
  }

  /**
   * Territory ranking system
   *
   * @param waterBased - attack is Water Based - Remove all terr with no avail water
   * @param nonCombat - if nonCombat, emphasize threatened factories over their neighbors
   * @return HashMap ranking of Territories
   */
  private static HashMap<Territory, Float> rankTerritories(final GameData data, final List<Territory> ourFriendlyTerr,
      final List<Territory> ourEnemyTerr, final List<Territory> ignoreTerr, final PlayerID player,
      final boolean tFirst, final boolean waterBased, final boolean nonCombat) {
    final HashMap<Territory, Float> landRankMap = new HashMap<>();
    final HashMap<Territory, Float> landStrengthMap = new HashMap<>();
    final CompositeMatch<Territory> noEnemyOrWater =
        new CompositeMatchAnd<>(Matches.TerritoryIsNotImpassableToLandUnits(player, data), Matches.isTerritoryAllied(
            player, data));
    final CompositeMatch<Territory> enemyAndNoWater =
        new CompositeMatchAnd<>(Matches.TerritoryIsNotImpassableToLandUnits(player, data),
            Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data));
    final List<PlayerID> ePlayers = getEnemyPlayers(data, player);
    final PlayerID ePlayer = ePlayers.get(0);
    final List<Territory> enemyCapitals = getEnemyCapitals(data, player);
    int minDist = 1000;
    final int playerPUs = getLeftToSpend(data, player);
    final List<Territory> myCapitals = TerritoryAttachment.getAllCurrentlyOwnedCapitals(player, data);
    if (myCapitals.isEmpty()) {
      myCapitals.addAll(TerritoryAttachment.getAllCapitals(player, data));
    }
    if (myCapitals.isEmpty()) {
      myCapitals.addAll(Match.getMatches(
          data.getMap().getTerritories(),
          new CompositeMatchAnd<>(Matches.TerritoryIsNotImpassableToLandUnits(player, data), Matches
              .territoryHasUnitsThatMatch(Matches.unitIsLandAndOwnedBy(player)))));
    }
    for (final Territory myCapital : myCapitals) {
      final Iterator<Territory> eCapsIter = enemyCapitals.iterator();
      while (eCapsIter.hasNext()) {
        final Territory eCap = eCapsIter.next();
        if (Matches.isTerritoryFriendly(player, data).match(eCap)
            && Matches.territoryHasAlliedUnits(player, data).match(eCap)
            && !Matches.territoryHasEnemyLandNeighbor(data, player).match(eCap)) {
          eCapsIter.remove();
          continue;
        }
        final int dist = data.getMap().getDistance(myCapital, eCap);
        minDist = Math.min(minDist, dist);
      }
    }
    /**
     * Send units because:
     * 1) Production Value
     * 2) Victory City
     * 3) Has a Land Route to Enemy Capitol
     * 4) Has enemy factory
     * 5) Is close to enemy
     * 6) Is closer than half the distance from cap to Enemy cap
     */
    final List<Territory> alliedFactories = getEnemyCapitals(data, ePlayer);
    final Iterator<Territory> aFIter = alliedFactories.iterator();
    while (aFIter.hasNext()) {
      final Territory aFTerr = aFIter.next();
      final float aFPotential = getStrengthOfPotentialAttackers(aFTerr, data, player, tFirst, true, null);
      final float alliedStrength = strengthOfTerritory(data, aFTerr, player, false, false, tFirst, true);
      if (aFPotential < alliedStrength * 0.75F
          || aFPotential < 1.0F
          || !Matches.TerritoryIsPassableAndNotRestricted(player, data).match(aFTerr)
          || (Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data).match(aFTerr) && Matches
              .territoryHasEnemyLandNeighbor(data, player).match(aFTerr))) {
        aFIter.remove();
      }
    }
    final List<Territory> aFNeighbors = new ArrayList<>();
    for (final Territory aF : alliedFactories) {
      aFNeighbors.addAll(data.getMap().getNeighbors(aF, Matches.isTerritoryAllied(player, data)));
    }
    for (final Territory eTerr : data.getMap().getTerritories()) {
      if (eTerr.isWater() || Matches.TerritoryIsImpassable.match(eTerr)
          || !Matches.TerritoryIsPassableAndNotRestricted(player, data).match(eTerr)) {
        continue;
      }
      final float alliedPotential = getStrengthOfPotentialAttackers(eTerr, data, ePlayer, tFirst, true, null);
      final float rankStrength = getStrengthOfPotentialAttackers(eTerr, data, player, tFirst, true, ignoreTerr);
      final TerritoryAttachment ta = TerritoryAttachment.get(eTerr);
      if (ta == null) {
        continue;
      }
      final float productionValue = ta.getProduction();
      float eTerrValue = 0.0F;
      final boolean island = !doesLandExistAt(eTerr, data, false);
      eTerrValue += ta.getVictoryCity() > 0 ? 2.0F : 0.0F;
      final boolean lRCap = hasLandRouteToEnemyOwnedCapitol(eTerr, player, data);
      // 16 might be too much, consider changing to 8
      eTerrValue += lRCap ? 16.0F : 0.0F;
      if (lRCap
          && (!Matches.territoryIsEnemyNonNeutralAndHasEnemyUnitMatching(data, player, Matches.UnitCanProduceUnits)
              .match(eTerr) && !Matches.territoryIsAlliedAndHasAlliedUnitMatching(data, player,
              Matches.UnitCanProduceUnits).match(eTerr))) {
        final Route eCapRoute =
            findNearest(eTerr,
                Matches.territoryIsEnemyNonNeutralAndHasEnemyUnitMatching(data, player, Matches.UnitCanProduceUnits),
                Matches.TerritoryIsNotImpassableToLandUnits(player, data), data);
        if (eCapRoute != null) {
          // 8 might be too much, consider changing to 4
          eTerrValue = Math.max(eTerrValue - 8, eTerrValue - (eCapRoute.numberOfSteps() - 1));
        }
      }
      eTerrValue +=
          Matches.territoryHasEnemyNonNeutralNeighborWithEnemyUnitMatching(data, player, Matches.UnitCanProduceUnits)
              .match(eTerr) ? 3.0F : 0.0F;
      int eMinDist = 1000;
      for (final Territory eTerrCap : enemyCapitals) {
        final int eDist = data.getMap().getDistance(eTerr, eTerrCap, Matches.TerritoryIsNotImpassable);
        eMinDist = Math.min(eMinDist, eDist);
      }
      eTerrValue -= eMinDist - 1;
      // bonus for general closeness to enemy Capital
      // eTerrValue += (eMinDist < minDist - 1) ? 4.0F : 0.0F;
      if (Matches.TerritoryIsLand.match(eTerr)
          && Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data).match(eTerr)) {
        ourEnemyTerr.add(eTerr);
        eTerrValue += productionValue * 2;
        final float eTerrStrength =
            strength(eTerr.getUnits().getMatches(Matches.enemyUnit(player, data)), false, false, tFirst);
        eTerrValue += alliedPotential > (rankStrength + eTerrStrength) ? productionValue : 0.0F;
        if (island) {
          eTerrValue += 5.0F;
        }
        // bonus for killing air units
        eTerrValue += eTerr.getUnits().countMatches(Matches.UnitIsAir) * 2;
        eTerrValue +=
            Matches.territoryIsEnemyNonNeutralAndHasEnemyUnitMatching(data, player, Matches.UnitCanProduceUnits).match(
                eTerr) ? 4.0F : 0.0F;
        eTerrValue +=
            Matches.territoryHasAlliedNeighborWithAlliedUnitMatching(data, player, Matches.UnitCanProduceUnits).match(
                eTerr) ? 8.0F : 0.0F;
        eTerrValue +=
            Matches.territoryHasEnemyLandNeighbor(data, player).invert().match(eTerr) ? productionValue + 1 : 0.0F;
        final float netStrength = (eTerrStrength - alliedPotential + 0.5F * rankStrength);
        landStrengthMap.put(eTerr, netStrength);
        landRankMap.put(eTerr, eTerrValue + netStrength * 0.25F);
      } else if (Matches.isTerritoryAllied(player, data).match(eTerr)
          && Matches.TerritoryIsNotNeutralButCouldBeWater.match(eTerr)) {
        final boolean hasENeighbors = Matches.territoryHasEnemyLandNeighbor(data, player).match(eTerr);
        final Route testERoute = findNearest(eTerr, enemyAndNoWater, noEnemyOrWater, data);
        if (island) {
          eTerrValue += -5.0F;
        }
        eTerrValue += (hasENeighbors ? 2.0F : -2.0F);
        eTerrValue += (aFNeighbors.contains(eTerr)) ? 8.0F : 0.0F;
        // -20 and -10 might be too much,
        // consider changing to -8 and -4
        eTerrValue += (testERoute == null ? -20.0F : Math.max(-10.0F, -(testERoute.numberOfSteps() - 2)));
        eTerrValue += (testERoute != null ? productionValue : 0.0F);
        final float aTerrStrength =
            strength(eTerr.getUnits().getMatches(Matches.alliedUnit(player, data)), false, false, tFirst);
        // bonus for allied factory and allied factory with enemy neighbor
        final boolean hasAlliedFactory =
            Matches.territoryIsAlliedAndHasAlliedUnitMatching(data, player, Matches.UnitCanProduceUnits).match(eTerr);
        if (hasAlliedFactory) {
          eTerrValue += 4.0F + (hasENeighbors && rankStrength > 5.0F ? 3.0F : 0.0F);
          alliedFactories.add(eTerr);
        }
        final float netStrength = rankStrength - aTerrStrength - 0.5F * alliedPotential;
        landStrengthMap.put(eTerr, netStrength);
        landRankMap.put(eTerr, eTerrValue + netStrength * 0.50F);
        if ((netStrength > -15.0F && rankStrength > 2.0F) || hasENeighbors || testERoute != null) {
          ourFriendlyTerr.add(eTerr);
        }
      } else if (Matches.TerritoryIsNeutralButNotWater.match(eTerr)) {
        if (Matches.TerritoryIsNotImpassable.match(eTerr)
            && (Matches.isTerritoryFreeNeutral(data).match(eTerr) || Properties.getNeutralCharge(data) <= playerPUs)) {
          // Make sure most neutral territories have lower priorities than enemy territories.
          eTerrValue += -100.0F;
          final boolean hasENeighbors = Matches.territoryHasEnemyLandNeighbor(data, player).match(eTerr);
          final Route testERoute = findNearest(eTerr, enemyAndNoWater, noEnemyOrWater, data);
          eTerrValue += (hasENeighbors ? 1.0F : -1.0F);
          eTerrValue += (testERoute == null ? -1.0F : -(testERoute.numberOfSteps() - 1));
          eTerrValue += productionValue > 0 ? productionValue : -5.0F;
          final float netStrength = rankStrength - 0.5F * alliedPotential;
          landStrengthMap.put(eTerr, netStrength);
          landRankMap.put(eTerr, eTerrValue + netStrength * 0.50F);
        }
      }
      // Currently there are a lot of territories that don't make it into the list, especially if the politics involves
      // neutral nations. we
      // should add them here.
    }
    if (nonCombat) {
      final CompositeMatch<Territory> alliedLandTerr =
          new CompositeMatchAnd<>(Matches.isTerritoryAllied(player, data), Matches.TerritoryIsLand,
              Matches.TerritoryIsNotImpassable);
      // Set<Territory> terrList = landRankMap.keySet();
      for (final Territory terr1 : alliedFactories) {
        if (!landRankMap.containsKey(terr1)) {
          continue;
        }
        float landRank = landRankMap.get(terr1);
        if (Matches.territoryHasEnemyLandNeighbor(data, player).match(terr1)) {
          for (final Territory neighbor : data.getMap().getNeighbors(terr1, alliedLandTerr)) {
            if (!landRankMap.containsKey(neighbor)) {
              continue;
            }
            final float thisRank = landRankMap.get(neighbor);
            landRank = Math.max(landRank, thisRank);
          }
          landRank += 1.0F;
          landRankMap.put(terr1, landRank);
        }
      }
    }
    return landRankMap;
  }

  /**
   * Returns a list of all enemy players
   */
  private static List<PlayerID> getEnemyPlayers(final GameData data, final PlayerID player) {
    final List<PlayerID> enemyPlayers = new ArrayList<>();
    for (final PlayerID players : data.getPlayerList().getPlayers()) {
      if (!data.getRelationshipTracker().isAllied(player, players)) {
        enemyPlayers.add(players);
      }
    }
    return enemyPlayers;
  }

  /**
   * List containing the enemy Capitals
   */
  private static List<Territory> getEnemyCapitals(final GameData data, final PlayerID player) { // generate a list of
                                                                                                // all enemy capitals
    final List<Territory> enemyCapitals = new ArrayList<>();
    final List<PlayerID> ePlayers = getEnemyPlayers(data, player);
    for (final PlayerID otherPlayer : ePlayers) {
      for (final Territory capital : TerritoryAttachment.getAllCapitals(otherPlayer, data)) {
        if (capital != null && Matches.TerritoryIsNotImpassableToLandUnits(player, data).match(capital)) {
          enemyCapitals.add(capital);
        }
      }
    }
    return enemyCapitals;
  }

  /**
   * Returns the players current pus available
   */
  private static int getLeftToSpend(final GameData data, final PlayerID player) {
    final Resource pus = data.getResourceList().getResource(Constants.PUS);
    return player.getResources().getQuantity(pus);
  }

  /**
   * Returns the strength of all attackers to a territory
   * differentiates between sea and land attack
   * determines all transports within range of territory
   * determines all air units within range of territory (using 2 for fighters and 3 for bombers)
   * does not check for extended range fighters or bombers
   *
   * @param tFirst
   *        - can transports be killed before other sea units
   * @param ignoreOnlyPlanes
   *        - if true, returns 0.0F if only planes can attack the territory
   */
  private static float getStrengthOfPotentialAttackers(final Territory location, final GameData data,
      final PlayerID player, final boolean tFirst, final boolean ignoreOnlyPlanes, final List<Territory> ignoreTerr) {
    PlayerID ePlayer = null;
    final List<PlayerID> qID = getEnemyPlayers(data, player);
    final HashMap<PlayerID, Float> ePAttackMap = new HashMap<>();
    final Iterator<PlayerID> playerIter = qID.iterator();
    if (location == null) {
      return -1000.0F;
    }
    boolean nonTransportsInAttack = false;
    final boolean onWater = location.isWater();
    if (!onWater) {
      nonTransportsInAttack = true;
    }
    final Set<Territory> waterTerr = data.getMap().getNeighbors(location, Matches.TerritoryIsWater);
    while (playerIter.hasNext()) {
      float seaStrength = 0.0F, firstStrength = 0.0F, secondStrength = 0.0F, blitzStrength = 0.0F, strength = 0.0F, airStrength =
          0.0F;
      ePlayer = playerIter.next();
      final CompositeMatch<Unit> enemyPlane =
          new CompositeMatchAnd<>(Matches.UnitIsAir, Matches.unitIsOwnedBy(ePlayer), Matches.UnitCanMove);
      final CompositeMatch<Unit> enemyTransport =
          new CompositeMatchAnd<>(Matches.unitIsOwnedBy(ePlayer), Matches.UnitIsSea, Matches.UnitIsTransport,
              Matches.UnitCanMove);
      final CompositeMatch<Unit> enemyShip =
          new CompositeMatchAnd<>(Matches.unitIsOwnedBy(ePlayer), Matches.UnitIsSea, Matches.UnitCanMove);
      final CompositeMatch<Unit> enemyTransportable =
          new CompositeMatchAnd<>(Matches.unitIsOwnedBy(ePlayer), Matches.UnitCanBeTransported,
              Matches.UnitIsNotAA, Matches.UnitCanMove);
      final CompositeMatch<Unit> aTransport =
          new CompositeMatchAnd<>(Matches.UnitIsSea, Matches.UnitIsTransport, Matches.UnitCanMove);
      final List<Territory> eFTerrs = findUnitTerr(data, ePlayer, enemyPlane);
      int maxFighterDistance = 0, maxBomberDistance = 0;
      // should change this to read production frontier and tech
      // reality is 99% of time units considered will have full move.
      // and likely player will have at least 1 max move plane.
      for (final Territory eFTerr : eFTerrs) {
        final List<Unit> eFUnits = eFTerr.getUnits().getMatches(enemyPlane);
        maxFighterDistance = Math.max(maxFighterDistance, MoveValidator.getMaxMovement(eFUnits));
      }
      // must be able to land...we will miss fighters who have a Carrier that can reach same sea zone...C'est la vie
      maxFighterDistance--;
      if (maxFighterDistance < 0) {
        maxFighterDistance = 0;
      }
      // must be able to land...won't miss anything here...unless special bombers that can land on carrier per above
      maxBomberDistance--;
      if (maxBomberDistance < 0) {
        maxBomberDistance = 0;
      }
      final List<Territory> eTTerrs = findUnitTerr(data, ePlayer, aTransport);
      int maxTransportDistance = 0;
      for (final Territory eTTerr : eTTerrs) {
        final List<Unit> eTUnits = eTTerr.getUnits().getMatches(aTransport);
        maxTransportDistance = Math.max(maxTransportDistance, MoveValidator.getMaxMovement(eTUnits));
      }
      final List<Unit> alreadyLoaded = new ArrayList<>();
      final List<Route> blitzTerrRoutes = new ArrayList<>();
      final List<Territory> checked = new ArrayList<>();
      final List<Unit> enemyWaterUnits = new ArrayList<>();
      for (final Territory t : data.getMap().getNeighbors(location,
          onWater ? Matches.TerritoryIsWater : Matches.TerritoryIsLand)) {
        if (ignoreTerr != null && ignoreTerr.contains(t)) {
          continue;
        }
        final List<Unit> enemies = t.getUnits().getMatches(Matches.unitIsOwnedBy(ePlayer));
        enemyWaterUnits.addAll(enemies);
        firstStrength += strength(enemies, true, onWater, tFirst);
        checked.add(t);
      }
      if (Matches.TerritoryIsLand.match(location)) {
        blitzStrength = determineEnemyBlitzStrength(location, blitzTerrRoutes, null, data, ePlayer);
      } else
      // get ships attack strength
      { // old assumed fleets won't split up, new lets them. no biggie.
        // assumes max ship movement is 3.
        // note, both old and new implementations
        // allow units to be calculated that are in
        // territories we have already assaulted
        // this can be easily changed
        final HashSet<Integer> ignore = new HashSet<>();
        ignore.add(Integer.valueOf(1));
        final List<Route> r = new ArrayList<>();
        final List<Unit> ships =
            findAttackers(location, 3, ignore, ePlayer, data, enemyShip, Matches.territoryIsBlockedSea(ePlayer, data),
                ignoreTerr, r, true);
        secondStrength = strength(ships, true, true, tFirst);
        enemyWaterUnits.addAll(ships);
      }
      final List<Unit> attackPlanes =
          findPlaneAttackersThatCanLand(location, maxFighterDistance, ePlayer, data, ignoreTerr, checked);
      airStrength += allairstrength(attackPlanes, true);
      if (Matches.territoryHasWaterNeighbor(data).match(location) && Matches.TerritoryIsLand.match(location)) {
        for (final Territory t4 : data.getMap().getNeighbors(location, maxTransportDistance)) {
          if (!t4.isWater()) {
            continue;
          }
          boolean transportsCounted = false;
          final Iterator<Territory> iterTerr = waterTerr.iterator();
          while (!transportsCounted && iterTerr.hasNext()) {
            final Territory waterCheck = iterTerr.next();
            if (ePlayer == null) {
              continue;
            }
            final List<Unit> transports = t4.getUnits().getMatches(enemyTransport);
            if (transports.isEmpty()) {
              continue;
            }
            if (!t4.equals(waterCheck)) {
              final Route seaRoute = getMaxSeaRoute(data, t4, waterCheck, ePlayer, true, maxTransportDistance);
              if (seaRoute == null || seaRoute.getEnd() == null || seaRoute.getEnd() != waterCheck) {
                continue;
              }
            }
            final List<Unit> loadedUnits = new ArrayList<>();
            int availInf = 0, availOther = 0;
            for (final Unit xTrans : transports) {
              final Collection<Unit> thisTransUnits = TransportTracker.transporting(xTrans);
              if (thisTransUnits == null) {
                availInf += 2;
                availOther += 1;
                continue;
              } else {
                int Inf = 2, Other = 1;
                for (final Unit checkUnit : thisTransUnits) {
                  if (Matches.UnitIsInfantry.match(checkUnit)) {
                    Inf--;
                  }
                  if (Matches.UnitIsNotInfantry.match(checkUnit)) {
                    Inf--;
                    Other--;
                  }
                  loadedUnits.add(checkUnit);
                }
                availInf += Inf;
                availOther += Other;
              }
            }
            final Set<Territory> transNeighbors =
                data.getMap().getNeighbors(t4, Matches.isTerritoryAllied(ePlayer, data));
            for (final Territory xN : transNeighbors) {
              final List<Unit> aTransUnits = xN.getUnits().getMatches(enemyTransportable);
              aTransUnits.removeAll(alreadyLoaded);
              final List<Unit> availTransUnits = sortTransportUnits(aTransUnits);
              for (final Unit aTUnit : availTransUnits) {
                if (availInf > 0 && Matches.UnitIsInfantry.match(aTUnit)) {
                  availInf--;
                  loadedUnits.add(aTUnit);
                  alreadyLoaded.add(aTUnit);
                }
                if (availInf > 0 && availOther > 0 && Matches.UnitIsNotInfantry.match(aTUnit)) {
                  availInf--;
                  availOther--;
                  loadedUnits.add(aTUnit);
                  alreadyLoaded.add(aTUnit);
                }
              }
            }
            seaStrength += strength(loadedUnits, true, false, tFirst);
            transportsCounted = true;
          }
        }
      }
      strength = seaStrength + blitzStrength + firstStrength + secondStrength;
      if (!ignoreOnlyPlanes || strength > 0.0F) {
        strength += airStrength;
      }
      if (onWater) {
        final Iterator<Unit> eWaterIter = enemyWaterUnits.iterator();
        while (eWaterIter.hasNext() && !nonTransportsInAttack) {
          if (Matches.UnitIsNotTransport.match(eWaterIter.next())) {
            nonTransportsInAttack = true;
          }
        }
      }
      if (!nonTransportsInAttack) {
        strength = 0.0F;
      }
      ePAttackMap.put(ePlayer, strength);
    }
    float maxStrength = 0.0F;
    for (final PlayerID xP : qID) {
      if (ePAttackMap.get(xP) > maxStrength) {
        ePlayer = xP;
        maxStrength = ePAttackMap.get(xP);
      }
    }
    for (final PlayerID xP : qID) {
      if (ePlayer != xP) {
        // give 40% of other players...this is will affect a lot of decisions by AI
        maxStrength += ePAttackMap.get(xP) * 0.40F;
      }
    }
    return maxStrength;
  }

  /**
   * Find the Route to the nearest Territory
   *
   * @param start - starting territory
   * @param endCondition - condition for the ending Territory
   * @param routeCondition - condition for each Territory in Route
   */
  private static Route findNearest(final Territory start, final Match<Territory> endCondition,
      final Match<Territory> routeCondition, final GameData data) {
    final Match<Territory> canGo = new CompositeMatchOr<>(endCondition, routeCondition);
    final Map<Territory, Territory> visited = new HashMap<>();
    final Queue<Territory> q = new LinkedList<>();
    final List<Territory> route = new ArrayList<>();
    // changing to exclude checking start
    q.addAll(data.getMap().getNeighbors(start, canGo));
    Territory current = null;
    visited.put(start, null);
    for (final Territory t : q) {
      visited.put(t, start);
    }
    while (!q.isEmpty()) {
      current = q.remove();
      if (endCondition.match(current)) {
        break;
      } else {
        for (final Territory neighbor : data.getMap().getNeighbors(current, canGo)) {
          if (!visited.containsKey(neighbor)) {
            q.add(neighbor);
            visited.put(neighbor, current);
          }
        }
      }
    }
    if (current == null || !endCondition.match(current)) {
      return null;
    }
    for (Territory t = current; t != null; t = visited.get(t)) {
      route.add(t);
    }
    Collections.reverse(route);
    return new Route(route);
  }

  /**
   * Get a quick and dirty estimate of the strength of some units in a battle.
   *
   * @param units - the units to measure
   * @param attacking - are the units on attack or defense
   * @param sea - calculate the strength of the units in a sea or land battle?
   */
  private static float strength(final Collection<Unit> units, final boolean attacking, final boolean sea,
      final boolean transportsFirst) {
    float strength = 0.0F;
    if (units.isEmpty()) {
      return strength;
    }
    if (attacking && Match.noneMatch(units, Matches.unitHasAttackValueOfAtLeast(1))) {
      return strength;
    } else if (!attacking && Match.noneMatch(units, Matches.unitHasDefendValueOfAtLeast(1))) {
      return strength;
    }
    for (final Unit u : units) {
      final UnitAttachment unitAttachment = UnitAttachment.get(u.getType());
      if (unitAttachment.getIsInfrastructure()) {
        continue;
      } else if (unitAttachment.getIsSea() == sea) {
        final int unitAttack = unitAttachment.getAttack(u.getOwner());
        // BB = 6.0; AC=2.0/4.0; SUB=3.0; DS=4.0; TR=0.50/2.0; F=4.0/5.0; B=5.0/2.0;
        // played with this value a good bit
        strength += 1.00F;
        if (attacking) {
          strength += unitAttack * unitAttachment.getHitPoints();
        } else {
          strength += unitAttachment.getDefense(u.getOwner()) * unitAttachment.getHitPoints();
        }
        if (attacking) {
          if (unitAttack == 0) {
            strength -= 0.50F;
          }
        }
        if (unitAttack == 0 && unitAttachment.getTransportCapacity() > 0 && !transportsFirst) {
          // only allow transport to have 0.35 on defense; none on attack
          strength -= 0.50F;
        }
      } else if (unitAttachment.getIsAir() == sea) {
        strength += 1.00F;
        if (attacking) {
          strength += unitAttachment.getAttack(u.getOwner()) * unitAttachment.getAttackRolls(u.getOwner());
        } else {
          strength += unitAttachment.getDefense(u.getOwner());
        }
      }
    }
    if (attacking && !sea) {
      final int art = Match.countMatches(units, Matches.UnitIsArtillery);
      final int artSupport = Match.countMatches(units, Matches.UnitIsArtillerySupportable);
      strength += Math.min(art, artSupport);
    }
    return strength;
  }

  /**
   * Determine the enemy potential for blitzing a territory - all enemies are combined
   *
   * @param blitzHere
   *        - Territory expecting to be blitzed
   * @param blitzTerr
   *        - Territory which is being blitzed through (not guaranteed to be all possible route territories!)
   * @param data
   * @param ePlayer
   *        - the enemy Player
   * @return actual strength of enemy units (armor)
   */
  private static float determineEnemyBlitzStrength(final Territory blitzHere, final List<Route> blitzTerrRoutes,
      final List<Territory> blockTerr, final GameData data, final PlayerID ePlayer) {
    final HashSet<Integer> ignore = new HashSet<>();
    ignore.add(Integer.valueOf(1));
    final CompositeMatch<Unit> blitzUnit =
        new CompositeMatchAnd<>(Matches.unitIsOwnedBy(ePlayer), Matches.UnitCanBlitz, Matches.UnitCanMove);
    final CompositeMatch<Territory> validBlitzRoute =
        new CompositeMatchAnd<>(Matches.territoryHasNoEnemyUnits(ePlayer, data),
            Matches.TerritoryIsNotImpassableToLandUnits(ePlayer, data));
    final List<Route> routes = new ArrayList<>();
    final List<Unit> blitzUnits =
        findAttackers(blitzHere, 2, ignore, ePlayer, data, blitzUnit, validBlitzRoute, blockTerr, routes, false);
    for (final Route r : routes) {
      if (r.numberOfSteps() == 2) {
        blitzTerrRoutes.add(r);
      }
    }
    return strength(blitzUnits, true, false, true);
  }

  private static List<Unit> findAttackers(final Territory start, final int maxDistance,
      final HashSet<Integer> ignoreDistance, final PlayerID player, final GameData data,
      final Match<Unit> unitCondition, final Match<Territory> routeCondition, final List<Territory> blocked,
      final List<Route> routes, final boolean sea) {
    final IntegerMap<Territory> distance = new IntegerMap<>();
    final Map<Territory, Territory> visited = new HashMap<>();
    final List<Unit> units = new ArrayList<>();
    final Queue<Territory> q = new LinkedList<>();
    q.add(start);
    Territory current = null;
    distance.put(start, 0);
    visited.put(start, null);
    while (!q.isEmpty()) {
      current = q.remove();
      if (distance.getInt(current) == maxDistance) {
        break;
      }
      for (final Territory neighbor : data.getMap().getNeighbors(current)) {
        if (!distance.keySet().contains(neighbor)) {
          if (!neighbor.getUnits().someMatch(unitCondition)) {
            if (!routeCondition.match(neighbor)) {
              continue;
            }
          }
          if (sea) {
            final Route r = new Route();
            r.setStart(neighbor);
            r.add(current);
            if (MoveValidator.validateCanal(r, null, player, data) != null) {
              continue;
            }
          }
          distance.put(neighbor, distance.getInt(current) + 1);
          visited.put(neighbor, current);
          if (blocked != null && blocked.contains(neighbor)) {
            continue;
          }
          q.add(neighbor);
          final Integer dist = Integer.valueOf(distance.getInt(neighbor));
          if (ignoreDistance.contains(dist)) {
            continue;
          }
          for (final Unit u : neighbor.getUnits()) {
            Route route1 = new Route();
            for (Route r : routes){
              route1 = Route.join(route1,r);
            }
            if (unitCondition.match(u) && Matches.UnitHasEnoughMovementForRoute(route1).match(u)) {
              units.add(u);
            }
          }
        }
      }
    }
    // pain in the ass, should just redesign stop blitz attack
    for (final Territory t : visited.keySet()) {
      final Route r = new Route();
      Territory t2 = t;
      r.setStart(t);
      while (t2 != null) {
        t2 = visited.get(t2);
        if (t2 != null) {
          r.add(t2);
        }
      }
      routes.add(r);
    }
    return units;
  }

  /**
   * does not count planes already in the starting territory
   */
  private static List<Unit> findPlaneAttackersThatCanLand(final Territory start, final int maxDistance,
      final PlayerID player, final GameData data, final List<Territory> ignore, final List<Territory> checked) {
    final IntegerMap<Territory> distance = new IntegerMap<>();
    final IntegerMap<Unit> unitDistance = new IntegerMap<>();
    final List<Unit> units = new ArrayList<>();
    final Queue<Territory> q = new LinkedList<>();
    Territory lz = null, ac = null;
    final CompositeMatch<Unit> enemyPlane =
        new CompositeMatchAnd<>(Matches.UnitIsAir, Matches.unitIsOwnedBy(player), Matches.UnitCanMove);
    final CompositeMatch<Unit> enemyCarrier =
        new CompositeMatchAnd<>(Matches.UnitIsCarrier, Matches.unitIsOwnedBy(player), Matches.UnitCanMove);
    q.add(start);
    Territory current = null;
    distance.put(start, 0);
    while (!q.isEmpty()) {
      current = q.remove();
      if (distance.getInt(current) == maxDistance) {
        break;
      }
      for (final Territory neighbor : data.getMap().getNeighbors(current, TerritoryIsNotImpassableToAirUnits(data))) {
        if (!distance.keySet().contains(neighbor)) {
          q.add(neighbor);
          distance.put(neighbor, distance.getInt(current) + 1);
          if (lz == null && Matches.isTerritoryAllied(player, data).match(neighbor) && !neighbor.isWater()) {
            lz = neighbor;
          }
          if ((ignore != null && ignore.contains(neighbor)) || (checked != null && checked.contains(neighbor))) {
            for (final Unit u : neighbor.getUnits()) {
              if (ac == null && enemyCarrier.match(u)) {
                ac = neighbor;
              }
            }
          } else {
            for (final Unit u : neighbor.getUnits()) {
              if (ac == null && enemyCarrier.match(u)) {
                ac = neighbor;
              }
              if (enemyPlane.match(u)) {
                unitDistance.put(u, distance.getInt(neighbor));
              }
            }
          }
        }
      }
    }
    for (final Unit u : unitDistance.keySet()) {
      if (lz != null && Matches.UnitHasEnoughMovementForRoute(new Route(checked)).match(u)) {
        units.add(u);
      } else if (ac != null && Matches.UnitCanLandOnCarrier.match(u)
          && Matches.UnitHasEnoughMovementForRoute(new Route(checked)).match(u)) {
        units.add(u);
      }
    }
    return units;
  }

  /**
   * Determine the strength of a collection of airUnits
   * Caller should guarantee units are all air.
   */
  private static float allairstrength(final Collection<Unit> units, final boolean attacking) {
    float airstrength = 0.0F;
    for (final Unit u : units) {
      final UnitAttachment unitAttachment = UnitAttachment.get(u.getType());
      airstrength += 1.00F;
      if (attacking) {
        airstrength += unitAttachment.getAttack(u.getOwner());
      } else {
        airstrength += unitAttachment.getDefense(u.getOwner());
      }
    }
    return airstrength;
  }

  private static Route getMaxSeaRoute(final GameData data, final Territory start, final Territory destination,
      final PlayerID player, final boolean attacking, final int maxDistance) {
    // note this does not care if subs are submerged or not
    // should it? does submerging affect movement of enemies?
    if (start == null || destination == null || !start.isWater() || !destination.isWater()) {
      return null;
    }
    final CompositeMatch<Unit> ignore =
        new CompositeMatchAnd<>(Matches.UnitIsInfrastructure.invert(), Matches.alliedUnit(player, data).invert());
    final CompositeMatch<Unit> sub = new CompositeMatchAnd<>(Matches.UnitIsSub.invert());
    final CompositeMatch<Unit> transport =
        new CompositeMatchAnd<>(Matches.UnitIsTransport.invert(), Matches.UnitIsLand.invert());
    final CompositeMatch<Unit> unitCond = ignore;
    if (Properties.getIgnoreTransportInMovement(data)) {
      unitCond.add(transport);
    }
    if (Properties.getIgnoreSubInMovement(data)) {
      unitCond.add(sub);
    }
    final CompositeMatch<Territory> routeCond =
        new CompositeMatchAnd<>(Matches.territoryHasUnitsThatMatch(unitCond).invert(), Matches.TerritoryIsWater);
    CompositeMatch<Territory> routeCondition;
    if (attacking) {
      routeCondition = new CompositeMatchOr<>(Matches.territoryIs(destination), routeCond);
    } else {
      routeCondition = routeCond;
    }
    Route r = data.getMap().getRoute(start, destination, routeCondition);
    if (r == null || r.getEnd() == null) {
      return null;
    }
    // cheating because can't do stepwise calculation with canals
    // shouldn't be a huge problem
    // if we fail due to canal, then don't go near any enemy canals
    if (MoveValidator.validateCanal(r, null, player, data) != null) {
      r =
          data.getMap().getRoute(
              start,
              destination,
              new CompositeMatchAnd<>(routeCondition, Matches.territoryHasNonAllowedCanal(player, null, data)
                  .invert()));
    }
    if (r == null || r.getEnd() == null) {
      return null;
    }
    final int rDist = r.numberOfSteps();
    Route route2 = new Route();
    if (rDist <= maxDistance) {
      route2 = r;
    } else {
      route2.setStart(start);
      for (int i = 1; i <= maxDistance; i++) {
        route2.add(r.getAllTerritories().get(i));
      }
    }
    return route2;
  }

  /**
   * All allied Territories which have a Land Enemy Neighbor
   *
   * @neutral - include neutral territories
   * @allied - include allied territories
   *         return - List of territories
   */
  private static List<Territory> getTerritoriesWithEnemyNeighbor(final GameData data, final PlayerID player,
      final boolean allied, final boolean neutral) {
    final List<Territory> ourTerr = new ArrayList<>();
    final List<Territory> enemyLandTerr = allEnemyTerritories(data, player);
    if (!neutral) {
      final Iterator<Territory> eIter = enemyLandTerr.iterator();
      while (eIter.hasNext()) {
        final Territory checkTerr = eIter.next();
        if (Matches.TerritoryIsNeutralButNotWater.match(checkTerr)) {
          eIter.remove();
        }
      }
    }
    final Iterator<Territory> eIter = enemyLandTerr.iterator();
    while (eIter.hasNext()) {
      final Territory enemy = eIter.next();
      if (doesLandExistAt(enemy, data, false)) {
        final List<Territory> newTerrs = new ArrayList<>();
        if (allied) {
          newTerrs.addAll(getNeighboringLandTerritories(data, player, enemy));
        } else {
          newTerrs.addAll(data.getMap().getNeighbors(enemy, Matches.isTerritoryOwnedBy(player)));
        }
        for (final Territory nT : newTerrs) {
          if (!ourTerr.contains(nT)) {
            ourTerr.add(nT);
          }
        }
      }
    }
    return ourTerr;
  }

  private static void reorder(final List<?> reorder, final Map<?, ? extends Number> map, final boolean greaterThan) {
    Collections.sort(reorder, new Comparator<Object>() {
      @Override
      public int compare(final Object o1, final Object o2) {
        double v1 = safeGet(map, o1);
        double v2 = safeGet(map, o2);
        if (greaterThan) {
          final double t = v1;
          v1 = v2;
          v2 = t;
        }
        if (v1 > v2) {
          return 1;
        } else if (v1 == v2) {
          return 0;
        } else {
          return -1;
        }
      }

      private double safeGet(final Map<?, ? extends Number> map, final Object o1) {
        if (!map.containsKey(o1)) {
          return 0;
        }
        return map.get(o1).doubleValue();
      }
    });
  }

  /**
   * All Enemy Territories in a modifiable List
   */
  private static List<Territory> allEnemyTerritories(final GameData data, final PlayerID player) {
    final List<Territory> badGuys = new ArrayList<>();
    for (final Territory t : data.getMap().getTerritories()) {
      if (Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data).match(t)) {
        badGuys.add(t);
      }
    }
    return badGuys;
  }

  /**
   * returns all territories that are water territories. used to remove convoy zones from places the ai will put a
   * factory
   */
  private static List<Territory> onlyWaterTerr(final GameData data, final List<Territory> allTerr) {
    final List<Territory> water = new ArrayList<>(allTerr);
    final Iterator<Territory> wFIter = water.iterator();
    while (wFIter.hasNext()) {
      final Territory waterFact = wFIter.next();
      if (!Matches.TerritoryIsWater.match(waterFact)) {
        wFIter.remove();
      }
    }
    return water;
  }

  /**
   * Look for an available sea Territory to place sea Units
   * if other owned sea units exist, place them with these units
   * Otherwise, look for the location which is least likely to get them killed
   *
   * @param landTerr
   *        - factory territory
   * @param tFirst
   *        - can transports be killed during battle
   *        Should be modified to include the list of units which will be dropped (for strength measurement)
   */
  private static Territory findASeaTerritoryToPlaceOn(final Territory landTerr, final GameData data,
      final PlayerID player, final boolean tFirst) {
    final CompositeMatch<Territory> ourSeaTerr =
        new CompositeMatchAnd<>(Matches.TerritoryIsWater, Matches.territoryHasUnitsOwnedBy(player));
    final CompositeMatch<Unit> seaUnit = new CompositeMatchAnd<>(Matches.unitIsOwnedBy(player), Matches.UnitIsSea);
    final CompositeMatch<Unit> airUnit = new CompositeMatchAnd<>(Matches.unitIsOwnedBy(player), Matches.UnitIsAir);
    final CompositeMatch<Unit> seaAirUnit = new CompositeMatchOr<>(seaUnit, airUnit);
    Territory seaPlaceAt = null, bestSeaPlaceAt = null;
    Territory xPlace = null;
    if (landTerr == null) {
      return seaPlaceAt;
    }
    final Set<Territory> seaNeighbors = data.getMap().getNeighbors(landTerr, ourSeaTerr);
    // float eStrength = 0.0F;
    float minStrength = 1000.0F, maxStrength = -1000.0F;
    for (final Territory t : seaNeighbors) // give preference to territory with units
    {
      float enemyStrength = getStrengthOfPotentialAttackers(t, data, player, tFirst, true, null);
      final float extraEnemy = strength(t.getUnits().getMatches(Matches.enemyUnit(player, data)), true, true, tFirst);
      enemyStrength += extraEnemy;
      float ourStrength = strength(t.getUnits().getMatches(seaAirUnit), false, true, tFirst);
      final float existingStrength =
          strength(t.getUnits().getMatches(Matches.alliedUnit(player, data)), false, true, tFirst);
      ourStrength += existingStrength;
      final float strengthDiff = enemyStrength - ourStrength;
      if (strengthDiff < minStrength && ourStrength > 0.0F) {
        seaPlaceAt = t;
        minStrength = strengthDiff;
      }
      if (strengthDiff > maxStrength && strengthDiff < 3.0F && (ourStrength > 0.0F || existingStrength > 0.0F)) {
        bestSeaPlaceAt = t;
        maxStrength = strengthDiff;
      }
    }
    if (seaPlaceAt == null && bestSeaPlaceAt == null) {
      final Set<Territory> seaNeighbors2 = data.getMap().getNeighbors(landTerr, Matches.TerritoryIsWater);
      for (final Territory t : seaNeighbors2) // find Terr away from enemy units
      {
        final float enemyStrength = getStrengthOfPotentialAttackers(t, data, player, tFirst, true, null);
        final float ourStrength = strength(t.getUnits().getMatches(seaAirUnit), false, true, tFirst);
        if (t.getUnits().someMatch(Matches.enemyUnit(player, data))) {
          // try to avoid Territories with enemy Units
          xPlace = t;
          continue;
        }
        if ((enemyStrength - ourStrength) < minStrength) {
          seaPlaceAt = t;
          minStrength = enemyStrength - ourStrength;
        }
      }
    }
    if (seaPlaceAt == null && bestSeaPlaceAt == null && xPlace != null) {
      // this will be null if there are no water territories
      seaPlaceAt = xPlace;
    }
    if (bestSeaPlaceAt == null) {
      return seaPlaceAt;
    } else {
      return bestSeaPlaceAt;
    }
  }

  /**
   * distance to the closest enemy
   * just uses findNearest
   */
  private static int distanceToEnemy(final Territory t, final GameData data, final PlayerID player, final boolean sea) {
    // note: neutrals are enemies
    // also note: if sea, you are finding distance to enemy sea units, not to enemy land over sea
    if (Matches.TerritoryIsImpassable.match(t)) {
      return 0;
    }
    Match<Territory> endCondition;
    Match<Territory> routeCondition;
    if (sea) {
      endCondition = new CompositeMatchAnd<>(Matches.TerritoryIsWater, Matches.territoryHasEnemyUnits(player, data));
      routeCondition = Matches.TerritoryIsWater;
    } else {
      endCondition =
          new CompositeMatchAnd<>(Matches.isTerritoryEnemy(player, data), Matches.TerritoryIsNotImpassable,
              Matches.TerritoryIsLand);
      routeCondition =
          new CompositeMatchAnd<>(Matches.isTerritoryAllied(player, data), Matches.TerritoryIsNotImpassable,
              Matches.TerritoryIsLand);
    }
    final Route r = findNearest(t, endCondition, routeCondition, data);
    if (r == null) {
      return 0;
    } else {
      return r.numberOfSteps();
    }
  }

  /**
   * Determine the strength of a territory
   *
   * @param attacking - attacking strength or defending
   * @param allied - allied = true - all allied units --> false - owned units only
   */
  private static float strengthOfTerritory(final GameData data, final Territory thisTerr, final PlayerID player,
      final boolean attacking, final boolean sea, final boolean tFirst, final boolean allied) {
    final List<Unit> theUnits = new ArrayList<>();
    if (allied) {
      theUnits.addAll(thisTerr.getUnits().getMatches(Matches.alliedUnit(player, data)));
    } else {
      theUnits.addAll(thisTerr.getUnits().getMatches(Matches.unitIsOwnedBy(player)));
    }
    final float theStrength = strength(theUnits, attacking, sea, tFirst);
    return theStrength;
  }

  /**
   * Does this territory have any land? i.e. it isn't an island
   *
   * @neutral - count an attackable neutral as a land neighbor
   * @return boolean (true if a land territory is a neighbor to t
   */
  private static boolean doesLandExistAt(final Territory t, final GameData data, final boolean neutral) { // simply: is
                                                                                                          // this
                                                                                                          // territory
                                                                                                          // surrounded
                                                                                                          // by water
    boolean isLand = false;
    final Set<Territory> checkList = data.getMap().getNeighbors(t, Matches.TerritoryIsLand);
    if (!neutral) {
      final Iterator<Territory> nIter = checkList.iterator();
      while (nIter.hasNext()) {
        final Territory nTerr = nIter.next();
        if (Matches.TerritoryIsNeutralButNotWater.match(nTerr)) {
          nIter.remove();
        }
      }
    }
    for (final Territory checkNeutral : checkList) {
      if (Matches.TerritoryIsNotImpassable.match(checkNeutral)) {
        isLand = true;
      }
    }
    return isLand;
  }

  /**
   * Interleave infantry and artillery/armor for loading on transports
   */
  private static List<Unit> sortTransportUnits(final List<Unit> transUnits) {
    final List<Unit> sorted = new ArrayList<>();
    final List<Unit> infantry = new ArrayList<>();
    final List<Unit> artillery = new ArrayList<>();
    final List<Unit> armor = new ArrayList<>();
    final List<Unit> others = new ArrayList<>();
    for (final Unit x : transUnits) {
      if (Matches.UnitIsArtillerySupportable.match(x)) {
        infantry.add(x);
      } else if (Matches.UnitIsArtillery.match(x)) {
        artillery.add(x);
      } else if (Matches.UnitCanBlitz.match(x)) {
        armor.add(x);
      } else {
        others.add(x);
      }
    }
    int artilleryCount = artillery.size();
    int armorCount = armor.size();
    final int infCount = infantry.size();
    int othersCount = others.size();
    for (int j = 0; j < infCount; j++) // interleave the artillery and armor with inf
    {
      sorted.add(infantry.get(j));
      // this should be based on combined attack and defense powers, not on attachments like blitz
      if (armorCount > 0) {
        sorted.add(armor.get(armorCount - 1));
        armorCount--;
      } else if (artilleryCount > 0) {
        sorted.add(artillery.get(artilleryCount - 1));
        artilleryCount--;
      } else if (othersCount > 0) {
        sorted.add(others.get(othersCount - 1));
        othersCount--;
      }
    }
    if (artilleryCount > 0) {
      for (int j2 = 0; j2 < artilleryCount; j2++) {
        sorted.add(artillery.get(j2));
      }
    }
    if (othersCount > 0) {
      for (int j4 = 0; j4 < othersCount; j4++) {
        sorted.add(others.get(j4));
      }
    }
    if (armorCount > 0) {
      for (int j3 = 0; j3 < armorCount; j3++) {
        sorted.add(armor.get(j3));
      }
    }
    return sorted;
  }

  private final static Match<Territory> TerritoryIsNotImpassableToAirUnits(final GameData data) {
    return new InverseMatch<>(TerritoryIsImpassableToAirUnits(data));
  }

  /**
   * Assumes that water is passable to air units always
   */
  private static Match<Territory> TerritoryIsImpassableToAirUnits(final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        if (Matches.TerritoryIsLand.match(t) && Matches.TerritoryIsImpassable.match(t)) {
          return true;
        }
        return false;
      }
    };
  }

  /**
   * All Allied Territories which neighbor a territory
   * This duplicates getNeighbors(check, Matches.isTerritoryAllied(player, data))
   */
  private static List<Territory> getNeighboringLandTerritories(final GameData data, final PlayerID player,
      final Territory check) {
    final ArrayList<Territory> rVal = new ArrayList<>();
    final List<Territory> checkList = getExactNeighbors(check, 1, player, data, false);
    for (final Territory t : checkList) {
      if (Matches.isTerritoryAllied(player, data).match(t)
          && Matches.TerritoryIsNotImpassableToLandUnits(player, data).match(t)) {
        rVal.add(t);
      }
    }
    return rVal;
  }

  /**
   * Gets the neighbors which are exactly a certain # of territories away (distance)
   * Removes the inner circle neighbors
   * neutral - whether to include neutral countries
   */
  @SuppressWarnings("unchecked")
  private static List<Territory> getExactNeighbors(final Territory territory, final int distance,
      final PlayerID player, final GameData data, final boolean neutral) {
    // old functionality retained, i.e. no route condition is imposed.
    // feel free to change, if you are confortable all calls to this function conform.
    final CompositeMatch<Territory> endCond = new CompositeMatchAnd<>(Matches.TerritoryIsImpassable.invert());
    if (!neutral || Properties.getNeutralsImpassable(data)) {
      endCond.add(Matches.TerritoryIsNeutralButNotWater.invert());
    }
    return findFontier(territory, endCond, Match.ALWAYS_MATCH, distance, data);
  }

  /**
   * Finds list of territories at exactly distance from the start
   *
   * @param start
   * @param endCondition
   *        condition that all end points must satisfy
   * @param routeCondition
   *        condition that all traversed internal territories must satisy
   * @param distance
   * @param data
   */
  private static List<Territory> findFontier(final Territory start, final Match<Territory> endCondition,
      final Match<Territory> routeCondition, final int distance, final GameData data) {
    final Match<Territory> canGo = new CompositeMatchOr<>(endCondition, routeCondition);
    final IntegerMap<Territory> visited = new IntegerMap<>();
    final Queue<Territory> q = new LinkedList<>();
    final List<Territory> frontier = new ArrayList<>();
    q.addAll(data.getMap().getNeighbors(start, canGo));
    Territory current = null;
    visited.put(start, 0);
    for (final Territory t : q) {
      visited.put(t, 1);
      if (1 == distance && endCondition.match(t)) {
        frontier.add(t);
      }
    }
    while (!q.isEmpty()) {
      current = q.remove();
      if (visited.getInt(current) == distance) {
        break;
      } else {
        for (final Territory neighbor : data.getMap().getNeighbors(current, canGo)) {
          if (!visited.keySet().contains(neighbor)) {
            q.add(neighbor);
            final int dist = visited.getInt(current) + 1;
            visited.put(neighbor, dist);
            if (dist == distance && endCondition.match(neighbor)) {
              frontier.add(neighbor);
            }
          }
        }
      }
    }
    return frontier;
  }

}
