package games.strategy.triplea.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.CollectionUtils;
import games.strategy.util.IntegerMap;

/**
 * Utility class with static methods to assist in determining TUV.
 */
public class TuvUtils {

  private TuvUtils() {}

  /**
   * Return map where keys are unit types and values are PU costs of that unit type, based on a player.
   * Any production rule that produces multiple units
   * (like artillery in NWO, costs 7 but makes 2 artillery, meaning effective price is 3.5 each)
   * will have their costs rounded up on a per unit basis (so NWO artillery will become 4).
   * Therefore, this map should NOT be used for Purchasing information!
   *
   * @param player
   *        The player to get costs schedule for
   * @param data
   *        The game data.
   * @return a map of unit types to PU cost
   */
  public static IntegerMap<UnitType> getCostsForTuv(final PlayerID player, final GameData data) {
    data.acquireReadLock();
    final Resource pus;
    try {
      pus = data.getResourceList().getResource(Constants.PUS);
    } finally {
      data.releaseReadLock();
    }
    final IntegerMap<UnitType> costs = new IntegerMap<>();
    final ProductionFrontier frontier = player.getProductionFrontier();
    // any one will do then
    if (frontier == null) {
      return TuvUtils.getCostsForTuvForAllPlayersMergedAndAveraged(data);
    }
    for (final ProductionRule rule : frontier.getRules()) {
      final NamedAttachable resourceOrUnit = rule.getResults().keySet().iterator().next();
      if (!(resourceOrUnit instanceof UnitType)) {
        continue;
      }
      final UnitType type = (UnitType) resourceOrUnit;
      final int costPerGroup = rule.getCosts().getInt(pus);
      final int numberProduced = rule.getResults().getInt(type);
      // we average the cost for a single unit, rounding up
      final int roundedCostPerSingle = (int) Math.ceil((double) costPerGroup / (double) numberProduced);
      costs.put(type, roundedCostPerSingle);
    }
    // since our production frontier may not cover all the units we control, and not the enemy units,
    // we will add any unit types not in our list, based on the list for everyone
    final IntegerMap<UnitType> costsAll = TuvUtils.getCostsForTuvForAllPlayersMergedAndAveraged(data);
    for (final UnitType ut : costsAll.keySet()) {
      if (!costs.keySet().contains(ut)) {
        costs.put(ut, costsAll.getInt(ut));
      }
    }

    // Override with XML TUV or consumesUnit sum
    final IntegerMap<UnitType> result = new IntegerMap<>(costs);
    for (final UnitType unitType : costs.keySet()) {
      result.put(unitType, getTotalTuv(unitType, costs, new HashSet<>()));
    }

    return result;
  }

  /**
   * Return a map where key are unit types and values are the AVERAGED for all RULES (not for all players).
   * Any production rule that produces multiple units
   * (like artillery in NWO, costs 7 but makes 2 artillery, meaning effective price is 3.5 each)
   * will have their costs rounded up on a per unit basis.
   * Therefore, this map should NOT be used for Purchasing information!
   */
  private static IntegerMap<UnitType> getCostsForTuvForAllPlayersMergedAndAveraged(final GameData data) {
    data.acquireReadLock();
    final Resource pus;
    try {
      pus = data.getResourceList().getResource(Constants.PUS);
    } finally {
      data.releaseReadLock();
    }
    final IntegerMap<UnitType> costs = new IntegerMap<>();
    final HashMap<UnitType, List<Integer>> differentCosts = new HashMap<>();
    for (final ProductionRule rule : data.getProductionRuleList().getProductionRules()) {
      // only works for the first result, so we are assuming each purchase frontier only gives one type of unit
      final NamedAttachable resourceOrUnit = rule.getResults().keySet().iterator().next();
      if (!(resourceOrUnit instanceof UnitType)) {
        continue;
      }
      final UnitType ut = (UnitType) resourceOrUnit;
      final int numberProduced = rule.getResults().getInt(ut);
      final int costPerGroup = rule.getCosts().getInt(pus);
      // we round up the cost
      final int roundedCostPerSingle = (int) Math.ceil((double) costPerGroup / (double) numberProduced);
      if (differentCosts.containsKey(ut)) {
        differentCosts.get(ut).add(roundedCostPerSingle);
      } else {
        final List<Integer> listTemp = new ArrayList<>();
        listTemp.add(roundedCostPerSingle);
        differentCosts.put(ut, listTemp);
      }
    }
    for (final UnitType ut : differentCosts.keySet()) {
      int totalCosts = 0;
      final List<Integer> costsForType = differentCosts.get(ut);
      for (final int cost : costsForType) {
        totalCosts += cost;
      }
      final int averagedCost = (int) Math.round(((double) totalCosts / (double) costsForType.size()));
      costs.put(ut, averagedCost);
    }
    return costs;
  }

  private static int getTotalTuv(final UnitType unitType, final IntegerMap<UnitType> costs,
      final Set<UnitType> alreadyAdded) {
    final UnitAttachment ua = UnitAttachment.get(unitType);
    if ((ua != null) && (ua.getTuv() > 0)) {
      return ua.getTuv();
    }
    int tuv = costs.getInt(unitType);
    if ((ua == null) || ua.getConsumesUnits().isEmpty() || alreadyAdded.contains(unitType)) {
      return tuv;
    }
    alreadyAdded.add(unitType);
    for (final UnitType ut : ua.getConsumesUnits().keySet()) {
      tuv += ua.getConsumesUnits().getInt(ut) * getTotalTuv(ut, costs, alreadyAdded);
    }
    alreadyAdded.remove(unitType);
    return tuv;
  }

  /**
   * Return map where keys are unit types and values are resource costs of that unit type, based on a player.
   * Any production rule that produces multiple units
   * (like artillery in NWO, costs 7 but makes 2 artillery, meaning effective price is 3.5 each)
   * will have their costs rounded up on a per unit basis.
   * Therefore, this map should NOT be used for Purchasing information!
   */
  public static Map<PlayerID, Map<UnitType, ResourceCollection>> getResourceCostsForTuv(final GameData data,
      final boolean includeAverageForMissingUnits) {
    final HashMap<PlayerID, Map<UnitType, ResourceCollection>> result = new LinkedHashMap<>();
    final Map<UnitType, ResourceCollection> average = includeAverageForMissingUnits
        ? TuvUtils.getResourceCostsForTuvForAllPlayersMergedAndAveraged(data)
        : new HashMap<>();
    final List<PlayerID> players = data.getPlayerList().getPlayers();
    players.add(PlayerID.NULL_PLAYERID);
    for (final PlayerID p : players) {
      final ProductionFrontier frontier = p.getProductionFrontier();
      // any one will do then
      if (frontier == null) {
        result.put(p, average);
        continue;
      }
      Map<UnitType, ResourceCollection> current = result.get(p);
      if (current == null) {
        current = new LinkedHashMap<>();
        result.put(p, current);
      }
      for (final ProductionRule rule : frontier.getRules()) {
        if ((rule == null) || (rule.getResults() == null) || rule.getResults().isEmpty() || (rule.getCosts() == null)
            || rule.getCosts().isEmpty()) {
          continue;
        }
        final IntegerMap<NamedAttachable> unitMap = rule.getResults();
        final ResourceCollection costPerGroup = new ResourceCollection(data, rule.getCosts());
        final Set<UnitType> units = new HashSet<>();
        for (final NamedAttachable resourceOrUnit : unitMap.keySet()) {
          if (!(resourceOrUnit instanceof UnitType)) {
            continue;
          }
          units.add((UnitType) resourceOrUnit);
        }
        if (units.isEmpty()) {
          continue;
        }
        final int totalProduced = unitMap.totalValues();
        if (totalProduced == 1) {
          current.put(units.iterator().next(), costPerGroup);
        } else if (totalProduced > 1) {
          costPerGroup.discount((double) 1 / (double) totalProduced);
          for (final UnitType ut : units) {
            current.put(ut, costPerGroup);
          }
        }
      }
      // since our production frontier may not cover all the units we control, and not the enemy units,
      // we will add any unit types not in our list, based on the list for everyone
      for (final UnitType ut : average.keySet()) {
        if (!current.keySet().contains(ut)) {
          current.put(ut, average.get(ut));
        }
      }
    }
    result.put(null, average);
    return result;
  }

  /**
   * Return a map where key are unit types and values are the AVERAGED for all players.
   * Any production rule that produces multiple units
   * (like artillery in NWO, costs 7 but makes 2 artillery, meaning effective price is 3.5 each)
   * will have their costs rounded up on a per unit basis.
   * Therefore, this map should NOT be used for Purchasing information!
   */
  private static Map<UnitType, ResourceCollection> getResourceCostsForTuvForAllPlayersMergedAndAveraged(
      final GameData data) {
    final Map<UnitType, ResourceCollection> average = new HashMap<>();
    final Resource pus;
    data.acquireReadLock();
    try {
      pus = data.getResourceList().getResource(Constants.PUS);
    } finally {
      data.releaseReadLock();
    }
    final IntegerMap<Resource> defaultMap = new IntegerMap<>();
    defaultMap.put(pus, 1);
    final ResourceCollection defaultResources = new ResourceCollection(data, defaultMap);
    final Map<UnitType, List<ResourceCollection>> backups = new HashMap<>();
    final Map<UnitType, ResourceCollection> backupAveraged = new HashMap<>();
    for (final ProductionRule rule : data.getProductionRuleList().getProductionRules()) {
      if ((rule == null) || (rule.getResults() == null) || rule.getResults().isEmpty() || (rule.getCosts() == null)
          || rule.getCosts().isEmpty()) {
        continue;
      }
      final IntegerMap<NamedAttachable> unitMap = rule.getResults();
      final ResourceCollection costPerGroup = new ResourceCollection(data, rule.getCosts());
      final Set<UnitType> units = new HashSet<>();
      for (final NamedAttachable resourceOrUnit : unitMap.keySet()) {
        if (!(resourceOrUnit instanceof UnitType)) {
          continue;
        }
        units.add((UnitType) resourceOrUnit);
      }
      if (units.isEmpty()) {
        continue;
      }
      final int totalProduced = unitMap.totalValues();
      if (totalProduced == 1) {
        final UnitType ut = units.iterator().next();
        List<ResourceCollection> current = backups.get(ut);
        if (current == null) {
          current = new ArrayList<>();
          backups.put(ut, current);
        }
        current.add(costPerGroup);
      } else if (totalProduced > 1) {
        costPerGroup.discount((double) 1 / (double) totalProduced);
        for (final UnitType ut : units) {
          List<ResourceCollection> current = backups.get(ut);
          if (current == null) {
            current = new ArrayList<>();
            backups.put(ut, current);
          }
          current.add(costPerGroup);
        }
      }
    }
    for (final Entry<UnitType, List<ResourceCollection>> entry : backups.entrySet()) {
      final ResourceCollection avgCost =
          new ResourceCollection(entry.getValue().toArray(new ResourceCollection[entry.getValue().size()]), data);
      if (entry.getValue().size() > 1) {
        avgCost.discount((double) 1 / (double) entry.getValue().size());
      }
      backupAveraged.put(entry.getKey(), avgCost);
    }
    final Map<PlayerID, Map<UnitType, ResourceCollection>> allPlayersCurrent = getResourceCostsForTuv(data, false);
    allPlayersCurrent.remove(null);
    for (final UnitType ut : data.getUnitTypeList().getAllUnitTypes()) {
      final List<ResourceCollection> costs = new ArrayList<>();
      for (final Map<UnitType, ResourceCollection> entry : allPlayersCurrent.values()) {
        if (entry.get(ut) != null) {
          costs.add(entry.get(ut));
        }
      }
      if (costs.isEmpty()) {
        final ResourceCollection backup = backupAveraged.get(ut);
        if (backup != null) {
          costs.add(backup);
        } else {
          costs.add(defaultResources);
        }
      }
      final ResourceCollection avgCost =
          new ResourceCollection(costs.toArray(new ResourceCollection[costs.size()]), data);
      if (costs.size() > 1) {
        avgCost.discount((double) 1 / (double) costs.size());
      }
      average.put(ut, avgCost);
    }
    return average;
  }

  /**
   * Return the total unit value
   *
   * @param units
   *        A collection of units
   * @param costs
   *        An integer map of unit types to costs.
   * @return the total unit value.
   */
  public static int getTuv(final Collection<Unit> units, final IntegerMap<UnitType> costs) {
    int tuv = 0;
    for (final Unit u : units) {
      final int unitValue = costs.getInt(u.getType());
      tuv += unitValue;
    }
    return tuv;
  }

  /**
   * Return the total unit value for a certain player and his allies
   *
   * @param units
   *        A collection of units
   * @param player
   *        The player to calculate the TUV for.
   * @param costs
   *        An integer map of unit types to costs
   * @return the total unit value.
   */
  public static int getTuv(final Collection<Unit> units, final PlayerID player, final IntegerMap<UnitType> costs,
      final GameData data) {
    final Collection<Unit> playerUnits = CollectionUtils.getMatches(units, Matches.alliedUnit(player, data));
    return getTuv(playerUnits, costs);
  }

}
