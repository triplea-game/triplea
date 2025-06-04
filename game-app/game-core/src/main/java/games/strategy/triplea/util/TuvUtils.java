package games.strategy.triplea.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.Matches;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;

/** Utility class with static methods to assist in determining TUV. */
@UtilityClass
public class TuvUtils {
  /**
   * Return map where keys are unit types and values are resource costs of that unit type, based on
   * a player. Any production rule that produces multiple units (like artillery in NWO, costs 7 but
   * makes 2 artillery, meaning effective price is 3.5 each) will have their costs rounded up on a
   * per unit basis. Therefore, this map should NOT be used for Purchasing information!
   */
  public static Map<GamePlayer, Map<UnitType, ResourceCollection>> getResourceCostsForTuv(
      final GameData data, final boolean includeAverageForMissingUnits) {
    final Map<GamePlayer, Map<UnitType, ResourceCollection>> result = new LinkedHashMap<>();
    final Map<UnitType, ResourceCollection> average =
        includeAverageForMissingUnits
            ? TuvUtils.getResourceCostsForTuvForAllPlayersMergedAndAveraged(data)
            : new HashMap<>();
    final List<GamePlayer> players = data.getPlayerList().getPlayers();
    players.add(data.getPlayerList().getNullPlayer());
    for (final GamePlayer p : players) {
      final ProductionFrontier frontier = p.getProductionFrontier();
      // any one will do then
      if (frontier == null) {
        result.put(p, average);
        continue;
      }
      final Map<UnitType, ResourceCollection> current =
          result.computeIfAbsent(p, k -> new LinkedHashMap<>());
      for (final ProductionRule rule : frontier.getRules()) {
        if (rule == null
            || rule.getResults() == null
            || rule.getResults().isEmpty()
            || rule.getCosts() == null
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
          current.put(CollectionUtils.getAny(units), costPerGroup);
        } else if (totalProduced > 1) {
          costPerGroup.discount((double) 1 / (double) totalProduced);
          for (final UnitType ut : units) {
            current.put(ut, costPerGroup);
          }
        }
      }
      // since our production frontier may not cover all the units we control, and not the enemy
      // units,
      // we will add any unit types not in our list, based on the list for everyone
      for (final UnitType ut : average.keySet()) {
        if (!current.containsKey(ut)) {
          current.put(ut, average.get(ut));
        }
      }
    }
    result.put(null, average);
    return result;
  }

  /**
   * Return a map where key are unit types and values are the AVERAGED for all players. Any
   * production rule that produces multiple units (like artillery in NWO, costs 7 but makes 2
   * artillery, meaning effective price is 3.5 each) will have their costs rounded up on a per unit
   * basis. Therefore, this map should NOT be used for Purchasing information!
   */
  private static Map<UnitType, ResourceCollection>
      getResourceCostsForTuvForAllPlayersMergedAndAveraged(final GameData data) {
    final Map<UnitType, ResourceCollection> average = new HashMap<>();
    final Resource pus;
    try (GameData.Unlocker ignored = data.acquireReadLock()) {
      pus = data.getResourceList().getResourceOrThrow(Constants.PUS);
    }
    final IntegerMap<Resource> defaultMap = new IntegerMap<>();
    defaultMap.put(pus, 1);
    final ResourceCollection defaultResources = new ResourceCollection(data, defaultMap);
    final Map<UnitType, List<ResourceCollection>> backups = new HashMap<>();
    final Map<UnitType, ResourceCollection> backupAveraged = new HashMap<>();
    for (final ProductionRule rule : data.getProductionRuleList().getProductionRules()) {
      if (rule == null
          || rule.getResults() == null
          || rule.getResults().isEmpty()
          || rule.getCosts() == null
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
        final UnitType ut = CollectionUtils.getAny(units);
        final List<ResourceCollection> current =
            backups.computeIfAbsent(ut, k -> new ArrayList<>());
        current.add(costPerGroup);
      } else if (totalProduced > 1) {
        costPerGroup.discount((double) 1 / (double) totalProduced);
        for (final UnitType ut : units) {
          final List<ResourceCollection> current =
              backups.computeIfAbsent(ut, k -> new ArrayList<>());
          current.add(costPerGroup);
        }
      }
    }
    for (final Entry<UnitType, List<ResourceCollection>> entry : backups.entrySet()) {
      final ResourceCollection avgCost =
          new ResourceCollection(entry.getValue().toArray(new ResourceCollection[0]), data);
      if (entry.getValue().size() > 1) {
        avgCost.discount((double) 1 / (double) entry.getValue().size());
      }
      backupAveraged.put(entry.getKey(), avgCost);
    }
    final Map<GamePlayer, Map<UnitType, ResourceCollection>> allPlayersCurrent =
        getResourceCostsForTuv(data, false);
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
        costs.add(Objects.requireNonNullElse(backup, defaultResources));
      }
      final ResourceCollection avgCost =
          new ResourceCollection(costs.toArray(new ResourceCollection[0]), data);
      if (costs.size() > 1) {
        avgCost.discount((double) 1 / (double) costs.size());
      }
      average.put(ut, avgCost);
    }
    return average;
  }

  /**
   * Return the total unit value.
   *
   * @param units A collection of units
   * @param costs An integer map of unit types to costs.
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
   * Return the total unit value for a certain player and his allies.
   *
   * @param units A collection of units
   * @param player The player to calculate the TUV for.
   * @param costs An integer map of unit types to costs
   * @return the total unit value.
   */
  public static int getTuv(
      final Collection<Unit> units,
      final GamePlayer player,
      final IntegerMap<UnitType> costs,
      final GameState data) {
    final Collection<Unit> playerUnits =
        CollectionUtils.getMatches(units, Matches.alliedUnit(player));
    return getTuv(playerUnits, costs);
  }
}
