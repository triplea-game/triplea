package games.strategy.triplea.ai.proAI;

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
import games.strategy.triplea.ai.strongAI.SUtils;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.BattleDelegate;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.triplea.delegate.Matches;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Pro bid AI.
 */
public class ProBidAI {

  private GameData data;
  private PlayerID player;

  public void bid(int PUsToSpend, final IPurchaseDelegate purchaseDelegate, final GameData data, final PlayerID player) {
    ProLogger.info("Starting bid purchase phase");

    // Current data at the start of combat move
    this.data = data;
    this.player = player;
    if (PUsToSpend == 0 && player.getResources().getQuantity(data.getResourceList().getResource(Constants.PUS)) == 0) {
      return;
    }

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
      final List<ProductionRule> seaProductionRulesCopy = new ArrayList<ProductionRule>(seaProductionRules);
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
    final List<Territory> enemyTerritoryBorderingOurTerrs = SUtils.getNeighboringEnemyLandTerritories(data, player);
    if (enemyTerritoryBorderingOurTerrs.isEmpty()) {
      landPurchase = false;
    }
    if (Math.random() > 0.25) {
      seaProductionRules.removeAll(subProductionRules);
    }
    if (PUsToSpend < 25) {
      if ((!isAmphib || Math.random() < 0.15) && landPurchase) {
        SUtils.findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack,
            landProductionRules, PUsToSpend, buyLimit, data, player, 2);
      } else {
        landPurchase = false;
        buyLimit = PUsToSpend / 5; // assume a larger threshhold
        if (Math.random() > 0.40) {
          SUtils.findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack,
              seaProductionRules, PUsToSpend, buyLimit, data, player, 2);
        } else {
          goTransports = true;
        }
      }
    } else if ((!isAmphib || Math.random() < 0.15) && landPurchase) {
      if (Math.random() > 0.80) {
        SUtils.findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack,
            landProductionRules, PUsToSpend, buyLimit, data, player, 2);
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
      SUtils.findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack,
          airProductionRules, airPUs, buyLimit, data, player, 2);
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
      SUtils.findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack,
          landProductionRules, landPUs, buyLimit, data, player, 2);
    } else {
      landPurchase = false;
      buyLimit = PUsToSpend / 8; // assume higher end purchase
      seaProductionRules.addAll(airProductionRules);
      if (Math.random() > 0.45) {
        SUtils.findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack,
            seaProductionRules, PUsToSpend, buyLimit, data, player, 2);
      } else {
        goTransports = true;
      }
    }
    final List<ProductionRule> processRules = new ArrayList<ProductionRule>();
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
      SUtils.findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack,
          landProductionRules, PUsToSpend, buyLimit, data, player, 2);
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
      final List<Territory> factories = SUtils.findTersWithUnitsMatching(data, player, Matches.UnitCanProduceUnits);
      final List<Territory> waterFactories = SUtils.stripLandLockedTerr(data, factories);
      if (waterFactories.isEmpty()) {
        return false;
      }
    }
    // find a land route to an enemy territory from our capitol
    boolean amphibPlayer = !SUtils.hasLandRouteToEnemyOwnedCapitol(capitol, player, data);
    int totProduction = 0, allProduction = 0;
    if (amphibPlayer) {
      final List<Territory> allFactories = SUtils.findTersWithUnitsMatching(data, player, Matches.UnitCanProduceUnits);
      // allFactories.remove(capitol);
      for (final Territory checkFactory : allFactories) {
        final boolean isLandRoute = SUtils.hasLandRouteToEnemyOwnedCapitol(checkFactory, player, data);
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
    final Collection<Territory> impassableTerrs = new ArrayList<Territory>();
    for (final Territory t : data.getMap().getTerritories()) {
      if (Matches.TerritoryIsPassableAndNotRestricted(player, data).invert().match(t)
          && Matches.TerritoryIsLand.match(t)) {
        impassableTerrs.add(t);
      }
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
    final CompositeMatch<Unit> landUnit =
        new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitIsLand, Matches.UnitIsNotInfrastructure,
            Matches.UnitCanNotProduceUnits);
    // CompositeMatch<Territory> ourLandTerr = new CompositeMatchAnd<Territory>(Matches.isTerritoryOwnedBy(player),
    // Matches.TerritoryIsLand);
    final Territory capitol = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
    final List<Territory> factoryTerritories =
        Match.getMatches(SUtils.findUnitTerr(data, player, ourFactory), Matches.isTerritoryOwnedBy(player));
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
    final HashMap<Territory, Float> rankMap =
        SUtils.rankTerritories(data, ourFriendlyTerr, ourEnemyTerr, null, player, tFirst, false, true);
    final List<Territory> ourTerrWithEnemyNeighbors =
        SUtils.getTerritoriesWithEnemyNeighbor(data, player, false, false);
    SUtils.reorder(ourTerrWithEnemyNeighbors, rankMap, true);
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
          new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater,
              Matches.territoryHasOwnedNeighborWithOwnedUnitMatching(data, player, Matches.UnitCanProduceUnits));
      final List<Territory> enemySeaTerr = SUtils.findUnitTerr(data, player, enemyAttackUnit);
      final List<Territory> isWaterTerr = SUtils.onlyWaterTerr(data, enemySeaTerr);
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
      final Route seaRoute = SUtils.findNearest(maxEnemySeaTerr, waterFactoryWaterTerr, Matches.TerritoryIsWater, data);
      if (seaRoute != null) {
        final Territory checkSeaTerr = seaRoute.getEnd();
        if (checkSeaTerr != null) {
          final float seaStrength =
              SUtils.getStrengthOfPotentialAttackers(checkSeaTerr, data, player, tFirst, false, null);
          final float aStrength = SUtils.strength(checkSeaTerr.getUnits().getUnits(), false, true, tFirst);
          final float bStrength = SUtils.strength(player.getUnits().getMatches(attackUnit), false, true, tFirst);
          final float totStrength = aStrength + bStrength;
          if (totStrength > 0.9F * seaStrength) {
            bidSeaTerr = checkSeaTerr;
          }
        }
      }
      for (final Territory factCheck : factoryTerritories) {
        if (bidSeaTerr == null) {
          bidSeaTerr = SUtils.findASeaTerritoryToPlaceOn(factCheck, data, player, tFirst);
        }
        if (bidTransTerr == null) {
          bidTransTerr = SUtils.findASeaTerritoryToPlaceOn(factCheck, data, player, tFirst);
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
        if (SUtils.distanceToEnemy(noRouteTerr, data, player, false) < 1
            && TerritoryAttachment.getProduction(noRouteTerr) < 3) {
          ourSemiRankedBidTerrs.remove(noRouteTerr);
        }
      }
      final List<Territory> isWaterTerr = SUtils.onlyWaterTerr(data, ourSemiRankedBidTerrs);
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
    final CompositeMatch<Unit> attackUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsSea, Matches.UnitIsNotTransport);
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
    final CompositeMatch<Unit> landOrAir = new CompositeMatchOr<Unit>(Matches.UnitIsAir, Matches.UnitIsLand);
    if (factoryPlace != null) // place a factory?
    {
      final Collection<Unit> toPlace =
          new ArrayList<Unit>(player.getUnits().getMatches(Matches.UnitCanProduceUnitsAndIsConstruction));
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
      final List<Unit> unitList = new ArrayList<Unit>();
      unitList.add(unit);
      final String message = del.placeUnits(unitList, t);
      if (message != null) {
        ProLogger.warn(message);
        ProLogger.warn("Attempt was at: " + t + " with: " + unit);
      }
    }
    ProUtils.pause();
  }
}
