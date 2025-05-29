package games.strategy.triplea.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.UnitAttachment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.triplea.java.collections.IntegerMap;

public class TuvCostsCalculator {
  @Nullable private IntegerMap<UnitType> costsAll;
  private final Map<GamePlayer, IntegerMap<UnitType>> costsPerPlayer = new HashMap<>();

  /**
   * Return map where keys are unit types and values are PU costs of that unit type, based on a
   * player. Any production rule that produces multiple units (like artillery in NWO, costs 7 but
   * makes 2 artillery, meaning effective price is 3.5 each) will have their costs rounded up on a
   * per unit basis (so NWO artillery will become 4). Therefore, this map should NOT be used for
   * Purchasing information!
   *
   * @param player The player to get costs schedule for
   * @return a map of unit types to PU cost
   */
  public IntegerMap<UnitType> getCostsForTuv(final GamePlayer player) {
    return costsPerPlayer.computeIfAbsent(player, this::computeCostsForTuv);
  }

  public IntegerMap<UnitType> computeCostsForTuv(final GamePlayer player) {
    final IntegerMap<UnitType> costs = computeBaseCostsForPlayer(player);
    // since our production frontier may not cover all the units we control, and not the enemy
    // units, we will add any unit types not in our list, based on the list for everyone
    if (costsAll == null) {
      costsAll = getCostsForTuvForAllPlayersMergedAndAveraged(player.getData());
    }
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

  private IntegerMap<UnitType> computeBaseCostsForPlayer(GamePlayer player) {
    final Resource pus = player.getData().getResourceList().getResource(Constants.PUS).orElse(null);
    final IntegerMap<UnitType> costs = new IntegerMap<>();
    final ProductionFrontier frontier = player.getProductionFrontier();
    if (frontier != null) {
      for (final ProductionRule rule : frontier.getRules()) {
        final NamedAttachable resourceOrUnit = rule.getAnyResultKey();
        if (!(resourceOrUnit instanceof UnitType)) {
          continue;
        }
        final UnitType type = (UnitType) resourceOrUnit;
        final int costPerGroup = rule.getCosts().getInt(pus);
        final int numberProduced = rule.getResults().getInt(type);
        // we average the cost for a single unit, rounding up
        final int roundedCostPerSingle =
            (int) Math.ceil((double) costPerGroup / (double) numberProduced);
        costs.put(type, roundedCostPerSingle);
      }
    }
    return costs;
  }

  /**
   * Return a map where key are unit types and values are the AVERAGED for all RULES (not for all
   * players). Any production rule that produces multiple units (like artillery in NWO, costs 7 but
   * makes 2 artillery, meaning effective price is 3.5 each) will have their costs rounded up on a
   * per unit basis. Therefore, this map should NOT be used for Purchasing information!
   */
  private static IntegerMap<UnitType> getCostsForTuvForAllPlayersMergedAndAveraged(
      final GameData data) {
    final Resource pus = data.getResourceList().getResource(Constants.PUS).orElse(null);
    final IntegerMap<UnitType> costs = new IntegerMap<>();
    final Map<UnitType, List<Integer>> differentCosts = new HashMap<>();
    for (final ProductionRule rule : data.getProductionRuleList().getProductionRules()) {
      // only works for the first result, so we are assuming each purchase frontier only gives one
      // type of unit
      final NamedAttachable resourceOrUnit = rule.getAnyResultKey();
      if (!(resourceOrUnit instanceof UnitType)) {
        continue;
      }
      final UnitType ut = (UnitType) resourceOrUnit;
      final int numberProduced = rule.getResults().getInt(ut);
      final int costPerGroup = rule.getCosts().getInt(pus);
      // we round up the cost
      final int roundedCostPerSingle =
          (int) Math.ceil((double) costPerGroup / (double) numberProduced);
      differentCosts.computeIfAbsent(ut, key -> new ArrayList<>()).add(roundedCostPerSingle);
    }
    for (final UnitType ut : differentCosts.keySet()) {
      int totalCosts = 0;
      final List<Integer> costsForType = differentCosts.get(ut);
      for (final int cost : costsForType) {
        totalCosts += cost;
      }
      final int averagedCost =
          (int) Math.round(((double) totalCosts / (double) costsForType.size()));
      costs.put(ut, averagedCost);
    }

    // Add any units that have XML TUV even if they aren't purchasable
    for (final UnitType unitType : data.getUnitTypeList()) {
      final UnitAttachment ua = unitType.getUnitAttachment();
      if (ua.getTuv() > -1) {
        costs.put(unitType, ua.getTuv());
      }
    }

    return costs;
  }

  private static int getTotalTuv(
      final UnitType unitType, final IntegerMap<UnitType> costs, final Set<UnitType> alreadyAdded) {
    final UnitAttachment ua = unitType.getUnitAttachment();
    if (ua.getTuv() > -1) {
      return ua.getTuv();
    }
    int tuv = costs.getInt(unitType);
    if (ua.getConsumesUnits().isEmpty() || alreadyAdded.contains(unitType)) {
      return tuv;
    }
    alreadyAdded.add(unitType);
    for (final UnitType ut : ua.getConsumesUnits().keySet()) {
      tuv += ua.getConsumesUnits().getInt(ut) * getTotalTuv(ut, costs, alreadyAdded);
    }
    alreadyAdded.remove(unitType);
    return tuv;
  }
}
