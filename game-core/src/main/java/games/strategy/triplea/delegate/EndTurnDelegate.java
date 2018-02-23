package games.strategy.triplea.delegate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.AutoSave;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.MapSupport;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.AbstractConditionsAttachment;
import games.strategy.triplea.attachments.AbstractTriggerAttachment;
import games.strategy.triplea.attachments.ICondition;
import games.strategy.triplea.attachments.RulesAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.TriggerAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.CollectionUtils;
import games.strategy.util.IntegerMap;
import games.strategy.util.Interruptibles;

/**
 * At the end of the turn collect income.
 */
@MapSupport
@AutoSave(afterStepStart = true)
public class EndTurnDelegate extends AbstractEndTurnDelegate {
  @Override
  protected String doNationalObjectivesAndOtherEndTurnEffects(final IDelegateBridge bridge) {
    final StringBuilder endTurnReport = new StringBuilder();

    // do national objectives
    if (isNationalObjectives()) {
      final String nationalObjectivesText = determineNationalObjectives(bridge);
      if (nationalObjectivesText.trim().length() > 0) {
        endTurnReport.append(nationalObjectivesText).append("<br />");
      }
    }

    // create resources if any owned units have the ability
    final String unitCreatedResourcesText = addUnitCreatedResources(bridge);
    if (unitCreatedResourcesText.trim().length() > 0) {
      endTurnReport.append(unitCreatedResourcesText).append("<br />");
    }

    // create units if any owned units have the ability
    final String createsUnitsText = createUnits(bridge);
    if (createsUnitsText.trim().length() > 0) {
      endTurnReport.append(createsUnitsText).append("<br />");
    }

    return endTurnReport.toString();
  }

  private String createUnits(final IDelegateBridge bridge) {
    final StringBuilder endTurnReport = new StringBuilder();
    final GameData data = getData();
    final PlayerID player = data.getSequence().getStep().getPlayerId();
    final Predicate<Unit> myCreatorsMatch = Matches.unitIsOwnedBy(player).and(Matches.unitCreatesUnits());
    final CompositeChange change = new CompositeChange();
    for (final Territory t : data.getMap().getTerritories()) {
      final Collection<Unit> myCreators = CollectionUtils.getMatches(t.getUnits().getUnits(), myCreatorsMatch);
      if ((myCreators != null) && !myCreators.isEmpty()) {
        final Collection<Unit> toAdd = new ArrayList<>();
        final Collection<Unit> toAddSea = new ArrayList<>();
        final Collection<Unit> toAddLand = new ArrayList<>();
        for (final Unit u : myCreators) {
          final UnitAttachment ua = UnitAttachment.get(u.getType());
          final IntegerMap<UnitType> createsUnitsMap = ua.getCreatesUnitsList();
          final Collection<UnitType> willBeCreated = createsUnitsMap.keySet();
          for (final UnitType ut : willBeCreated) {
            if (UnitAttachment.get(ut).getIsSea() && Matches.territoryIsLand().test(t)) {
              toAddSea.addAll(ut.create(createsUnitsMap.getInt(ut), player));
            } else if (!UnitAttachment.get(ut).getIsSea() && !UnitAttachment.get(ut).getIsAir()
                && Matches.territoryIsWater().test(t)) {
              toAddLand.addAll(ut.create(createsUnitsMap.getInt(ut), player));
            } else {
              toAdd.addAll(ut.create(createsUnitsMap.getInt(ut), player));
            }
          }
        }
        if (!toAdd.isEmpty()) {
          final String transcriptText =
              player.getName() + " creates " + MyFormatter.unitsToTextNoOwner(toAdd) + " in " + t.getName();
          bridge.getHistoryWriter().startEvent(transcriptText, toAdd);
          endTurnReport.append(transcriptText).append("<br />");
          final Change place = ChangeFactory.addUnits(t, toAdd);
          change.add(place);
        }
        if (!toAddSea.isEmpty()) {
          final Predicate<Territory> myTerrs = Matches.territoryIsWater();
          final Collection<Territory> waterNeighbors = data.getMap().getNeighbors(t, myTerrs);
          if ((waterNeighbors != null) && !waterNeighbors.isEmpty()) {
            final Territory tw = getRandomTerritory(waterNeighbors, bridge);
            final String transcriptText =
                player.getName() + " creates " + MyFormatter.unitsToTextNoOwner(toAddSea) + " in " + tw.getName();
            bridge.getHistoryWriter().startEvent(transcriptText, toAddSea);
            endTurnReport.append(transcriptText).append("<br />");
            final Change place = ChangeFactory.addUnits(tw, toAddSea);
            change.add(place);
          }
        }
        if (!toAddLand.isEmpty()) {
          final Predicate<Territory> myTerrs = Matches.isTerritoryOwnedBy(player).and(Matches.territoryIsLand());
          final Collection<Territory> landNeighbors = data.getMap().getNeighbors(t, myTerrs);
          if ((landNeighbors != null) && !landNeighbors.isEmpty()) {
            final Territory tl = getRandomTerritory(landNeighbors, bridge);
            final String transcriptText =
                player.getName() + " creates " + MyFormatter.unitsToTextNoOwner(toAddLand) + " in " + tl.getName();
            bridge.getHistoryWriter().startEvent(transcriptText, toAddLand);
            endTurnReport.append(transcriptText).append("<br />");
            final Change place = ChangeFactory.addUnits(tl, toAddLand);
            change.add(place);
          }
        }
      }
    }
    if (!change.isEmpty()) {
      bridge.addChange(change);
    }
    return endTurnReport.toString();
  }

  private static Territory getRandomTerritory(final Collection<Territory> territories, final IDelegateBridge bridge) {
    if ((territories == null) || territories.isEmpty()) {
      return null;
    }
    if (territories.size() == 1) {
      return territories.iterator().next();
    }
    // there is an issue with maps that have lots of rolls without any pause between them: they are causing the cypted
    // random source (ie:
    // live and pbem games) to lock up or error out
    // so we need to slow them down a bit, until we come up with a better solution (like aggregating all the chances
    // together, then getting
    // a ton of random numbers at once instead of one at a time)
    Interruptibles.sleep(100);
    final List<Territory> list = new ArrayList<>(territories);
    final int random =
        // ZERO BASED
        bridge.getRandom(list.size(), null, DiceType.ENGINE, "Random territory selection for creating units");
    return list.get(random);
  }

  private String addUnitCreatedResources(final IDelegateBridge bridge) {

    // Find total unit generated resources for all owned units
    final GameData data = getData();
    final PlayerID player = data.getSequence().getStep().getPlayerId();
    final IntegerMap<Resource> resourceTotalsMap = findUnitCreatedResources(player, data);

    // Add resource changes and create end turn report string
    final StringBuilder endTurnReport = new StringBuilder();
    final CompositeChange change = new CompositeChange();
    for (final Resource resource : resourceTotalsMap.keySet()) {
      int toAdd = resourceTotalsMap.getInt(resource);
      if (toAdd == 0) {
        continue;
      }
      int total = player.getResources().getQuantity(resource) + toAdd;
      if (total < 0) {
        toAdd -= total;
        total = 0;
      }
      final String transcriptText = "Units generate " + toAdd + " " + resource.getName() + "; " + player.getName()
          + " end with " + total + " " + resource.getName();
      bridge.getHistoryWriter().startEvent(transcriptText);
      endTurnReport.append(transcriptText).append("<br />");
      final Change resources = ChangeFactory.changeResourcesChange(player, resource, toAdd);
      change.add(resources);
    }
    if (!change.isEmpty()) {
      bridge.addChange(change);
    }

    return endTurnReport.toString();
  }

  /**
   * Find all of the resources that will be created by units on the map.
   */
  public static IntegerMap<Resource> findUnitCreatedResources(final PlayerID player, final GameData data) {
    final IntegerMap<Resource> resourceTotalsMap = new IntegerMap<>();
    final Predicate<Unit> myCreatorsMatch = Matches.unitIsOwnedBy(player).and(Matches.unitCreatesResources());
    for (final Territory t : data.getMap().getTerritories()) {
      final Collection<Unit> myCreators = CollectionUtils.getMatches(t.getUnits().getUnits(), myCreatorsMatch);
      for (final Unit unit : myCreators) {
        final IntegerMap<Resource> generatedResourcesMap = UnitAttachment.get(unit.getType()).getCreatesResourcesList();
        resourceTotalsMap.add(generatedResourcesMap);
      }
    }
    final Resource pus = new Resource(Constants.PUS, data);
    if (resourceTotalsMap.containsKey(pus)) {
      resourceTotalsMap.put(pus, resourceTotalsMap.getInt(pus) * Properties.getPuMultiplier(data));
    }
    return resourceTotalsMap;
  }

  /**
   * Find the PU value of national objectives that are currently met.
   */
  public static int findNationalObjectivePus(final PlayerID player, final GameData data, final IDelegateBridge bridge) {
    final List<RulesAttachment> objectives = new ArrayList<>();
    final HashMap<ICondition, Boolean> testedConditions =
        testNationalObjectivesAndTriggers(player, data, bridge, new HashSet<>(), objectives);
    int pus = 0;
    for (final RulesAttachment rule : objectives) {
      final int uses = rule.getUses();
      if ((uses == 0) || !rule.isSatisfied(testedConditions)) {
        continue;
      }
      pus += (rule.getObjectiveValue() * rule.getEachMultiple() * Properties.getPuMultiplier(data));
    }
    return pus;
  }

  /**
   * Determine if National Objectives have been met, and then do them.
   */
  private String determineNationalObjectives(final IDelegateBridge bridge) {
    final GameData data = getData();
    final PlayerID player = data.getSequence().getStep().getPlayerId();

    // Find and test all the conditions for triggers and national objectives
    final Set<TriggerAttachment> triggers = new HashSet<>();
    final List<RulesAttachment> objectives = new ArrayList<>();
    final HashMap<ICondition, Boolean> testedConditions =
        testNationalObjectivesAndTriggers(player, data, bridge, triggers, objectives);

    // Execute triggers
    final StringBuilder endTurnReport = new StringBuilder();
    final boolean useTriggers = Properties.getTriggers(data);
    if (useTriggers && !triggers.isEmpty()) {
      final Set<TriggerAttachment> toFireTestedAndSatisfied = new HashSet<>(
          CollectionUtils.getMatches(triggers, AbstractTriggerAttachment.isSatisfiedMatch(testedConditions)));
      endTurnReport.append(TriggerAttachment.triggerResourceChange(toFireTestedAndSatisfied, bridge, null, null, true,
          true, true, true)).append("<br />");
    }

    // Execute national objectives
    for (final RulesAttachment rule : objectives) {
      int uses = rule.getUses();
      if ((uses == 0) || !rule.isSatisfied(testedConditions)) {
        continue;
      }
      int toAdd = rule.getObjectiveValue();
      toAdd *= Properties.getPuMultiplier(data);
      toAdd *= rule.getEachMultiple();
      int total = player.getResources().getQuantity(Constants.PUS) + toAdd;
      if (total < 0) {
        toAdd -= total;
        total = 0;
      }
      final Change change =
          ChangeFactory.changeResourcesChange(player, data.getResourceList().getResource(Constants.PUS), toAdd);
      bridge.addChange(change);
      if (uses > 0) {
        uses--;
        final Change use = ChangeFactory.attachmentPropertyChange(rule, Integer.toString(uses), "uses");
        bridge.addChange(use);
      }
      final String puMessage = MyFormatter.attachmentNameToText(rule.getName()) + ": " + player.getName()
          + " met a national objective for an additional " + toAdd + MyFormatter.pluralize(" PU", toAdd) + "; end with "
          + total + MyFormatter.pluralize(" PU", total);
      bridge.getHistoryWriter().startEvent(puMessage);
      endTurnReport.append(puMessage).append("<br />");
    }
    return endTurnReport.toString();
  }

  private static HashMap<ICondition, Boolean> testNationalObjectivesAndTriggers(final PlayerID player,
      final GameData data, final IDelegateBridge bridge, final Set<TriggerAttachment> triggers,
      final List<RulesAttachment> objectives) {

    // First figure out all the conditions that will be tested, so we can test them all at the same time.
    final HashSet<ICondition> allConditionsNeeded = new HashSet<>();
    final boolean useTriggers = Properties.getTriggers(data);
    if (useTriggers) {

      // Add conditions required for triggers
      final Predicate<TriggerAttachment> endTurnDelegateTriggerMatch = AbstractTriggerAttachment.availableUses
          .and(AbstractTriggerAttachment.whenOrDefaultMatch(null, null))
          .and(TriggerAttachment.resourceMatch());
      triggers.addAll(TriggerAttachment.collectForAllTriggersMatching(new HashSet<>(Collections.singleton(player)),
          endTurnDelegateTriggerMatch));
      allConditionsNeeded
          .addAll(AbstractConditionsAttachment.getAllConditionsRecursive(new HashSet<>(triggers), null));
    }

    // Add conditions required for national objectives (nat objs that have uses left)
    objectives.addAll(CollectionUtils.getMatches(RulesAttachment.getNationalObjectives(player), availableUses));
    allConditionsNeeded.addAll(AbstractConditionsAttachment.getAllConditionsRecursive(new HashSet<>(objectives), null));
    if (allConditionsNeeded.isEmpty()) {
      return new HashMap<>();
    }

    // Now test all the conditions
    return AbstractConditionsAttachment.testAllConditionsRecursive(allConditionsNeeded, null, bridge);
  }

  private boolean isNationalObjectives() {
    return Properties.getNationalObjectives(getData());
  }

  private static final Predicate<RulesAttachment> availableUses = ra -> ra.getUses() != 0;

  @Override
  protected String addOtherResources(final IDelegateBridge bridge) {
    final StringBuilder endTurnReport = new StringBuilder();
    final GameData data = bridge.getData();
    final CompositeChange change = new CompositeChange();
    final Collection<Territory> territories = data.getMap().getTerritoriesOwnedBy(player);
    final IntegerMap<Resource> production = getResourceProduction(territories, data);
    for (final Entry<Resource, Integer> resource : production.entrySet()) {
      final Resource r = resource.getKey();
      int toAdd = resource.getValue();
      int total = player.getResources().getQuantity(r) + toAdd;
      if (total < 0) {
        toAdd -= total;
        total = 0;
      }
      final String resourceText =
          player.getName() + " collects " + toAdd + " " + MyFormatter.pluralize(r.getName(), toAdd) + "; ends with "
              + total + " " + MyFormatter.pluralize(r.getName(), total) + " total";
      bridge.getHistoryWriter().startEvent(resourceText);
      endTurnReport.append(resourceText).append("<br />");
      change.add(ChangeFactory.changeResourcesChange(player, r, toAdd));
    }
    if (!change.isEmpty()) {
      bridge.addChange(change);
    }
    return endTurnReport.toString();
  }

  /**
   * Since territory resource may contain any resource except PUs (PUs use "getProduction" instead),
   * we will now figure out the total production of non-PUs resources.
   */
  public static IntegerMap<Resource> getResourceProduction(final Collection<Territory> territories,
      final GameData data) {
    final IntegerMap<Resource> resources = new IntegerMap<>();
    for (final Territory current : territories) {
      final TerritoryAttachment attachment = TerritoryAttachment.get(current);
      if (attachment == null) {
        throw new IllegalStateException("No attachment for owned territory:" + current.getName());
      }
      final ResourceCollection toAdd = attachment.getResources();
      if (toAdd == null) {
        continue;
      }
      // Match will check if territory is originally owned convoy center, or if contested
      if (Matches.territoryCanCollectIncomeFrom(current.getOwner(), data).test(current)) {
        resources.add(toAdd.getResourcesCopy());
      }
    }
    return resources;
  }
}
