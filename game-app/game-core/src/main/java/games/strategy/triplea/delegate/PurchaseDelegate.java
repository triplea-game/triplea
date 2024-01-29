package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.posted.game.pbem.PbemMessagePoster;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.AbstractTriggerAttachment;
import games.strategy.triplea.attachments.FireTriggerParams;
import games.strategy.triplea.attachments.ICondition;
import games.strategy.triplea.attachments.TechAbilityAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.TriggerAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.attachments.UnitTypeComparator;
import games.strategy.triplea.delegate.remote.IAbstractForumPosterDelegate;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.Getter;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;

/**
 * Logic for purchasing units. Subclasses can override canAfford(...) to test if a purchase can be
 * made Subclasses can over ride addToPlayer(...) and removeFromPlayer(...) to change how the adding
 * or removing of resources is done.
 */
public class PurchaseDelegate extends BaseTripleADelegate
    implements IPurchaseDelegate, IAbstractForumPosterDelegate {
  public static final String NOT_ENOUGH_RESOURCES = "Not enough resources";
  private static final Comparator<RepairRule> repairRuleComparator =
      Comparator.comparing(o -> (UnitType) o.getAnyResultKey(), new UnitTypeComparator());

  private boolean needToInitialize = true;
  @Getter private IntegerMap<ProductionRule> pendingProductionRules;

  @Override
  public void start() {
    super.start();
    final GameState data = getData();
    if (needToInitialize) {
      if (Properties.getTriggers(data.getProperties())) {
        // First set up a match for what we want to have fire as a default in this delegate. List
        // out as a composite
        // match OR.
        // use 'null, null' because this is the Default firing location for any trigger that does
        // NOT have 'when' set.
        final Predicate<TriggerAttachment> purchaseDelegateTriggerMatch =
            AbstractTriggerAttachment.availableUses
                .and(AbstractTriggerAttachment.whenOrDefaultMatch(null, null))
                .and(
                    TriggerAttachment.prodMatch()
                        .or(TriggerAttachment.prodFrontierEditMatch())
                        .or(TriggerAttachment.purchaseMatch()));
        // get all possible triggers based on this match.
        final Set<TriggerAttachment> toFirePossible =
            TriggerAttachment.collectForAllTriggersMatching(
                Set.of(player), purchaseDelegateTriggerMatch);
        if (!toFirePossible.isEmpty()) {
          // get all conditions possibly needed by these triggers, and then test them.
          final Map<ICondition, Boolean> testedConditions =
              TriggerAttachment.collectTestsForAllTriggers(toFirePossible, bridge);
          // get all triggers that are satisfied based on the tested conditions.
          final Set<TriggerAttachment> toFireTestedAndSatisfied =
              new HashSet<>(
                  CollectionUtils.getMatches(
                      toFirePossible,
                      AbstractTriggerAttachment.isSatisfiedMatch(testedConditions)));
          // now list out individual types to fire, once for each of the matches above.
          final FireTriggerParams fireTriggerParams =
              new FireTriggerParams(null, null, true, true, true, true);
          TriggerAttachment.triggerProductionChange(
              toFireTestedAndSatisfied, bridge, fireTriggerParams);
          TriggerAttachment.triggerProductionFrontierEditChange(
              toFireTestedAndSatisfied, bridge, fireTriggerParams);
          TriggerAttachment.triggerPurchase(toFireTestedAndSatisfied, bridge, fireTriggerParams);
        }
      }
      needToInitialize = false;
    }
  }

  @Override
  public void end() {
    super.end();
    pendingProductionRules = null;
    needToInitialize = true;
  }

  @Override
  public Serializable saveState() {
    final PurchaseExtendedDelegateState state = new PurchaseExtendedDelegateState();
    state.superState = super.saveState();
    state.needToInitialize = needToInitialize;
    state.pendingProductionRules = pendingProductionRules;
    return state;
  }

  @Override
  public void loadState(final Serializable state) {
    final PurchaseExtendedDelegateState s = (PurchaseExtendedDelegateState) state;
    super.loadState(s.superState);
    needToInitialize = s.needToInitialize;
    pendingProductionRules = s.pendingProductionRules;
  }

  @Override
  public boolean delegateCurrentlyRequiresUserInput() {
    if (!canWePurchaseOrRepair()) {
      return false;
    }
    // if my capital is captured, I can't produce, but I may have PUs if I captured someone else's
    // capital
    return TerritoryAttachment.doWeHaveEnoughCapitalsToProduce(player, getData().getMap());
  }

  protected boolean canWePurchaseOrRepair() {
    if (player.getProductionFrontier() != null
        && player.getProductionFrontier().getRules() != null) {
      for (final ProductionRule rule : player.getProductionFrontier().getRules()) {
        if (player.getResources().has(rule.getCosts())) {
          return true;
        }
      }
    }
    if (player.getRepairFrontier() != null && player.getRepairFrontier().getRules() != null) {
      for (final RepairRule rule : player.getRepairFrontier().getRules()) {
        if (player.getResources().has(rule.getCosts())) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * subclasses can over ride this method to use different restrictions as to what a player can buy.
   */
  protected boolean canAfford(final IntegerMap<Resource> costs, final GamePlayer player) {
    return player.getResources().has(costs);
  }

  @Override
  public String purchase(final IntegerMap<ProductionRule> productionRules) {
    final IntegerMap<Resource> costs = getCosts(productionRules);
    final IntegerMap<NamedAttachable> results = getResults(productionRules);
    if (!canAfford(costs, player)) {
      return NOT_ENOUGH_RESOURCES;
    }
    // check to see if player has too many of any building with a building limit
    for (final NamedAttachable next : results.keySet()) {
      if (!(next instanceof Resource)) {
        final UnitType type = (UnitType) next;
        final int quantity = results.getInt(type);
        final UnitAttachment ua = type.getUnitAttachment();
        final int maxBuilt = ua.getMaxBuiltPerPlayer();
        if (maxBuilt == 0) {
          return "May not build any of this unit right now: " + type.getName();
        } else if (maxBuilt > 0) {
          // count how many units are yet to be placed or are in the field
          int currentlyBuilt = player.getUnitCollection().countMatches(Matches.unitIsOfType(type));

          final Predicate<Unit> unitTypeOwnedBy =
              Matches.unitIsOfType(type).and(Matches.unitIsOwnedBy(player));
          final Collection<Territory> allTerrs = getData().getMap().getTerritories();
          for (final Territory t : allTerrs) {
            currentlyBuilt += t.getUnitCollection().countMatches(unitTypeOwnedBy);
          }

          final int allowedBuild = maxBuilt - currentlyBuilt;
          if (allowedBuild - quantity < 0) {
            return String.format(
                "May only build %s of %s this turn, may only build %s total",
                allowedBuild, type.getName(), maxBuilt);
          }
        }
      }
    }
    // remove first, since add logs PUs remaining
    final Collection<Unit> totalUnits = new ArrayList<>();
    final Collection<UnitType> totalUnitTypes = new ArrayList<>();
    final Collection<Resource> totalResources = new ArrayList<>();
    final CompositeChange changes = new CompositeChange();
    // add changes for added resources
    // and find all added units
    for (final NamedAttachable next : results.keySet()) {
      if (next instanceof Resource) {
        final Resource resource = (Resource) next;
        final int quantity = results.getInt(resource);
        final Change change = ChangeFactory.changeResourcesChange(player, resource, quantity);
        changes.add(change);
        for (int i = 0; i < quantity; i++) {
          totalResources.add(resource);
        }
      } else {
        final UnitType type = (UnitType) next;
        final int quantity = results.getInt(type);
        final Collection<Unit> units = type.create(quantity, player);
        totalUnits.addAll(units);
        for (int i = 0; i < quantity; i++) {
          totalUnitTypes.add(type);
        }
      }
    }
    final Collection<NamedAttachable> totalAll = new ArrayList<>();
    totalAll.addAll(totalUnitTypes);
    totalAll.addAll(totalResources);
    // add changes for added units
    if (!totalUnits.isEmpty()) {
      final Change change = ChangeFactory.addUnits(player, totalUnits);
      changes.add(change);
    }
    // add changes for spent resources
    final String remaining = removeFromPlayer(costs, changes);
    // add history event
    final String transcriptText;
    if (!totalUnits.isEmpty()) {
      transcriptText =
          player.getName()
              + " buy "
              + MyFormatter.defaultNamedToTextList(totalAll, ", ", true)
              + "; "
              + remaining;
    } else {
      transcriptText = player.getName() + " buy nothing; " + remaining;
    }
    bridge.getHistoryWriter().startEvent(transcriptText, totalUnits);
    // commit changes
    bridge.addChange(changes);
    return null;
  }

  @Override
  public String purchaseRepair(final Map<Unit, IntegerMap<RepairRule>> repairRules) {
    final IntegerMap<Resource> costs = getRepairCosts(repairRules, player);
    if (!canAfford(costs, player)) {
      return NOT_ENOUGH_RESOURCES;
    }
    if (!Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(
        getData().getProperties())) {
      return null;
    }
    // Get the map of the factories that were repaired and how much for each
    final IntegerMap<Unit> repairMap = getUnitRepairs(repairRules);
    if (repairMap.isEmpty()) {
      return null;
    }
    // remove first, since add logs PUs remaining
    final CompositeChange changes = new CompositeChange();
    final Set<Unit> repairUnits = new HashSet<>(repairMap.keySet());
    final IntegerMap<Unit> damageMap = new IntegerMap<>();
    for (final Unit unit : repairUnits) {
      final int repairCount = repairMap.getInt(unit);
      // Display appropriate damaged/repaired factory and factory damage totals
      if (repairCount > 0) {
        final int newDamageTotal = Math.max(0, unit.getUnitDamage() - repairCount);
        if (newDamageTotal != unit.getUnitDamage()) {
          damageMap.put(unit, newDamageTotal);
        }
      }
    }
    if (!damageMap.isEmpty()) {
      changes.add(
          ChangeFactory.bombingUnitDamage(
              damageMap,
              bridge.getData().getMap().getTerritories().stream()
                  .filter(
                      territory -> !Collections.disjoint(territory.getUnits(), damageMap.keySet()))
                  .collect(Collectors.toList())));
    }
    // add changes for spent resources
    final String remaining = removeFromPlayer(costs, changes);
    // add history event
    final String transcriptText;
    if (!damageMap.isEmpty()) {
      transcriptText =
          player.getName()
              + " repair damage of "
              + MyFormatter.integerUnitMapToString(repairMap, ", ", "x ", true)
              + "; "
              + remaining;
    } else {
      transcriptText = player.getName() + " repair nothing; " + remaining;
    }
    bridge.getHistoryWriter().startEvent(transcriptText, new HashSet<>(damageMap.keySet()));
    // commit changes
    if (!changes.isEmpty()) {
      bridge.addChange(changes);
    }
    return null;
  }

  private static IntegerMap<Unit> getUnitRepairs(
      final Map<Unit, IntegerMap<RepairRule>> repairRules) {
    final IntegerMap<Unit> repairMap = new IntegerMap<>();
    for (final Unit u : repairRules.keySet()) {
      final IntegerMap<RepairRule> rules = repairRules.get(u);
      final TreeSet<RepairRule> repRules = new TreeSet<>(repairRuleComparator);
      repRules.addAll(rules.keySet());
      for (final RepairRule repairRule : repRules) {
        final int quantity = rules.getInt(repairRule) * repairRule.getResults().getInt(u.getType());
        repairMap.add(u, quantity);
      }
    }
    return repairMap;
  }

  private static IntegerMap<Resource> getCosts(final IntegerMap<ProductionRule> productionRules) {
    final IntegerMap<Resource> costs = new IntegerMap<>();
    for (final ProductionRule rule : productionRules.keySet()) {
      costs.addMultiple(rule.getCosts(), productionRules.getInt(rule));
    }
    return costs;
  }

  private IntegerMap<Resource> getRepairCosts(
      final Map<Unit, IntegerMap<RepairRule>> repairRules, final GamePlayer player) {
    final IntegerMap<Resource> costs = new IntegerMap<>();
    for (final IntegerMap<RepairRule> map : repairRules.values()) {
      for (final RepairRule rule : map.keySet()) {
        costs.addMultiple(rule.getCosts(), map.getInt(rule));
      }
    }

    final double discount =
        TechAbilityAttachment.getRepairDiscount(
            TechTracker.getCurrentTechAdvances(player, getData().getTechnologyFrontier()));
    if (discount != 1.0D) {
      costs.multiplyAllValuesBy(discount);
    }
    return costs;
  }

  private static IntegerMap<NamedAttachable> getResults(
      final IntegerMap<ProductionRule> productionRules) {
    final IntegerMap<NamedAttachable> costs = new IntegerMap<>();
    for (final ProductionRule rule : productionRules.keySet()) {
      costs.addMultiple(rule.getResults(), productionRules.getInt(rule));
    }
    return costs;
  }

  @Override
  public Class<IPurchaseDelegate> getRemoteType() {
    return IPurchaseDelegate.class;
  }

  protected String removeFromPlayer(
      final IntegerMap<Resource> costs, final CompositeChange changes) {
    final StringBuilder returnString = new StringBuilder("Remaining resources: ");
    final IntegerMap<Resource> left = player.getResources().getResourcesCopy();
    left.subtract(costs);
    for (final Entry<Resource, Integer> entry : left.entrySet()) {
      returnString
          .append(entry.getValue())
          .append(" ")
          .append(entry.getKey().getName())
          .append("; ");
    }
    for (final Resource resource : costs.keySet()) {
      final float quantity = costs.getInt(resource);
      final int cost = (int) quantity;
      final Change change = ChangeFactory.changeResourcesChange(player, resource, -cost);
      changes.add(change);
    }
    return returnString.toString();
  }

  public void setPendingProductionRules(IntegerMap<ProductionRule> pendingProductionRules) {
    this.pendingProductionRules = pendingProductionRules;
  }

  @Override
  public void setHasPostedTurnSummary(final boolean hasPostedTurnSummary) {
    // nothing for now
  }

  @Override
  public boolean getHasPostedTurnSummary() {
    return false;
  }

  @Override
  public boolean postTurnSummary(final PbemMessagePoster poster, final String title) {
    return poster.post(bridge.getHistoryWriter(), title);
  }
}
