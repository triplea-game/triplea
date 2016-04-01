package games.strategy.triplea.ai.proAI.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.proAI.ProData;
import games.strategy.triplea.ai.proAI.data.ProTerritory;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.delegate.UnitBattleComparator;

/**
 * Pro AI attack options utilities.
 */
public class ProSortMoveOptionsUtils {

  public static Map<Unit, Set<Territory>> sortUnitMoveOptions(final PlayerID player,
      final Map<Unit, Set<Territory>> unitAttackOptions) {

    final List<Map.Entry<Unit, Set<Territory>>> list = new LinkedList<>(unitAttackOptions.entrySet());
    Collections.sort(list, new Comparator<Map.Entry<Unit, Set<Territory>>>() {
      @Override
      public int compare(final Map.Entry<Unit, Set<Territory>> o1, final Map.Entry<Unit, Set<Territory>> o2) {

        // Sort by number of move options then cost of unit then unit type
        if (o1.getValue().size() != o2.getValue().size()) {
          return (o1.getValue().size() - o2.getValue().size());
        } else if (ProData.unitValueMap.getInt(o1.getKey().getType()) != ProData.unitValueMap.getInt(o2.getKey()
            .getType())) {
          return (ProData.unitValueMap.getInt(o1.getKey().getType()) - ProData.unitValueMap.getInt(o2.getKey()
              .getType()));
        }
        return o1.getKey().getType().getName().compareTo(o2.getKey().getType().getName());
      }
    });
    final Map<Unit, Set<Territory>> sortedUnitAttackOptions = new LinkedHashMap<>();
    for (final Map.Entry<Unit, Set<Territory>> entry : list) {
      sortedUnitAttackOptions.put(entry.getKey(), entry.getValue());
    }
    return sortedUnitAttackOptions;
  }

  public static Map<Unit, Set<Territory>> sortUnitNeededOptions(final PlayerID player,
      final Map<Unit, Set<Territory>> unitAttackOptions, final Map<Territory, ProTerritory> attackMap,
      final ProOddsCalculator calc) {
    final GameData data = ProData.getData();

    final List<Map.Entry<Unit, Set<Territory>>> list = new LinkedList<>(unitAttackOptions.entrySet());
    Collections.sort(list, new Comparator<Map.Entry<Unit, Set<Territory>>>() {
      @Override
      public int compare(final Map.Entry<Unit, Set<Territory>> o1, final Map.Entry<Unit, Set<Territory>> o2) {

        // Find number of territories that still need units
        int numOptions1 = 0;
        for (final Territory t : o1.getValue()) {
          final ProTerritory patd = attackMap.get(t);
          if (patd.getBattleResult() == null) {
            patd.setBattleResult(calc.estimateAttackBattleResults(player, t, patd.getUnits(),
                patd.getMaxEnemyDefenders(player, data), patd.getBombardTerritoryMap().keySet()));
          }
          if (!patd.isCurrentlyWins()) {
            numOptions1++;
          }
        }
        int numOptions2 = 0;
        for (final Territory t : o2.getValue()) {
          final ProTerritory patd = attackMap.get(t);
          if (patd.getBattleResult() == null) {
            patd.setBattleResult(calc.estimateAttackBattleResults(player, t, patd.getUnits(),
                patd.getMaxEnemyDefenders(player, data), patd.getBombardTerritoryMap().keySet()));
          }
          if (!patd.isCurrentlyWins()) {
            numOptions2++;
          }
        }

        // Sort by number of move options then cost of unit then unit type
        if (numOptions1 != numOptions2) {
          return (numOptions1 - numOptions2);
        }
        if (ProData.unitValueMap.getInt(o1.getKey().getType()) != ProData.unitValueMap.getInt(o2.getKey().getType())) {
          return (ProData.unitValueMap.getInt(o1.getKey().getType()) - ProData.unitValueMap.getInt(o2.getKey()
              .getType()));
        }
        return o1.getKey().getType().getName().compareTo(o2.getKey().getType().getName());
      }
    });
    final Map<Unit, Set<Territory>> sortedUnitAttackOptions = new LinkedHashMap<>();
    for (final Map.Entry<Unit, Set<Territory>> entry : list) {
      sortedUnitAttackOptions.put(entry.getKey(), entry.getValue());
    }
    return sortedUnitAttackOptions;
  }

  public static Map<Unit, Set<Territory>> sortUnitNeededOptionsThenAttack(final PlayerID player,
      final Map<Unit, Set<Territory>> unitAttackOptions, final Map<Territory, ProTerritory> attackMap,
      final Map<Unit, Territory> unitTerritoryMap, final ProOddsCalculator calc) {
    final GameData data = ProData.getData();

    final List<Map.Entry<Unit, Set<Territory>>> list = new LinkedList<>(unitAttackOptions.entrySet());
    Collections.sort(list, new Comparator<Map.Entry<Unit, Set<Territory>>>() {
      @Override
      public int compare(final Map.Entry<Unit, Set<Territory>> o1, final Map.Entry<Unit, Set<Territory>> o2) {

        // Sort by number of territories that still need units
        int numOptions1 = 0;
        for (final Territory t : o1.getValue()) {
          final ProTerritory patd = attackMap.get(t);
          if (patd.getBattleResult() == null) {
            patd.setBattleResult(calc.estimateAttackBattleResults(player, t, patd.getUnits(),
                patd.getMaxEnemyDefenders(player, data), patd.getBombardTerritoryMap().keySet()));
          }
          if (!patd.isCurrentlyWins()) {
            numOptions1++;
          }
        }
        int numOptions2 = 0;
        for (final Territory t : o2.getValue()) {
          final ProTerritory patd = attackMap.get(t);
          if (patd.getBattleResult() == null) {
            patd.setBattleResult(calc.estimateAttackBattleResults(player, t, patd.getUnits(),
                patd.getMaxEnemyDefenders(player, data), patd.getBombardTerritoryMap().keySet()));
          }
          if (!patd.isCurrentlyWins()) {
            numOptions2++;
          }
        }
        if (numOptions1 != numOptions2) {
          return (numOptions1 - numOptions2);
        }
        if (numOptions1 == 0) {
          return 0;
        }

        // Sort by attack efficiency
        int minPower1 = Integer.MAX_VALUE;
        for (final Territory t : o1.getValue()) {
          if (!attackMap.get(t).isCurrentlyWins()) {
            final List<Unit> defendingUnits = t.getUnits().getMatches(Matches.enemyUnit(player, data));
            final List<Unit> sortedUnitsList = new ArrayList<>(attackMap.get(t).getUnits());
            Collections.sort(sortedUnitsList, new UnitBattleComparator(false, ProData.unitValueMap,
                TerritoryEffectHelper.getEffects(t), data, false, false));
            Collections.reverse(sortedUnitsList);
            final int powerWithout =
                DiceRoll.getTotalPower(DiceRoll.getUnitPowerAndRollsForNormalBattles(sortedUnitsList, defendingUnits,
                    false, false, data, t, TerritoryEffectHelper.getEffects(t), false, null), data);
            sortedUnitsList.add(o1.getKey());
            Collections.sort(sortedUnitsList, new UnitBattleComparator(false, ProData.unitValueMap,
                TerritoryEffectHelper.getEffects(t), data, false, false));
            Collections.reverse(sortedUnitsList);
            final int powerWith =
                DiceRoll.getTotalPower(DiceRoll.getUnitPowerAndRollsForNormalBattles(sortedUnitsList, defendingUnits,
                    false, false, data, t, TerritoryEffectHelper.getEffects(t), false, null), data);
            final int power = powerWith - powerWithout;
            if (power < minPower1) {
              minPower1 = power;
            }
          }
        }
        final UnitAttachment ua1 = UnitAttachment.get(o1.getKey().getType());
        if (ua1.getIsAir()) {
          minPower1 *= 10;
        }
        final double attackEfficiency1 = (double) minPower1 / ProData.unitValueMap.getInt(o1.getKey().getType());
        int minPower2 = Integer.MAX_VALUE;
        for (final Territory t : o2.getValue()) {
          if (!attackMap.get(t).isCurrentlyWins()) {
            final List<Unit> defendingUnits = t.getUnits().getMatches(Matches.enemyUnit(player, data));
            final List<Unit> sortedUnitsList = new ArrayList<>(attackMap.get(t).getUnits());
            Collections.sort(sortedUnitsList, new UnitBattleComparator(false, ProData.unitValueMap,
                TerritoryEffectHelper.getEffects(t), data, false, false));
            Collections.reverse(sortedUnitsList);
            final int powerWithout =
                DiceRoll.getTotalPower(DiceRoll.getUnitPowerAndRollsForNormalBattles(sortedUnitsList, defendingUnits,
                    false, false, data, t, TerritoryEffectHelper.getEffects(t), false, null), data);
            sortedUnitsList.add(o2.getKey());
            Collections.sort(sortedUnitsList, new UnitBattleComparator(false, ProData.unitValueMap,
                TerritoryEffectHelper.getEffects(t), data, false, false));
            Collections.reverse(sortedUnitsList);
            final int powerWith =
                DiceRoll.getTotalPower(DiceRoll.getUnitPowerAndRollsForNormalBattles(sortedUnitsList, defendingUnits,
                    false, false, data, t, TerritoryEffectHelper.getEffects(t), false, null), data);
            final int power = powerWith - powerWithout;
            if (power < minPower2) {
              minPower2 = power;
            }
          }
        }
        final UnitAttachment ua2 = UnitAttachment.get(o2.getKey().getType());
        if (ua2.getIsAir()) {
          minPower2 *= 10;
        }
        final double attackEfficiency2 = (double) minPower2 / ProData.unitValueMap.getInt(o2.getKey().getType());
        if (attackEfficiency1 != attackEfficiency2) {
          if (attackEfficiency1 < attackEfficiency2) {
            return 1;
          } else {
            return -1;
          }
        }

        // Check if unit types are equal and is air then sort by average distance
        if (o1.getKey().getType().equals(o2.getKey().getType())) {
          final boolean isAirUnit = UnitAttachment.get(o1.getKey().getType()).getIsAir();
          if (isAirUnit) {
            int distance1 = 0;
            for (final Territory t : o1.getValue()) {
              if (!attackMap.get(t).isCurrentlyWins()) {
                distance1 +=
                    data.getMap().getDistance_IgnoreEndForCondition(unitTerritoryMap.get(o1.getKey()), t,
                        ProMatches.territoryCanMoveAirUnitsAndNoAA(player, data, true));
              }
            }
            int distance2 = 0;
            for (final Territory t : o2.getValue()) {
              if (!attackMap.get(t).isCurrentlyWins()) {
                distance2 +=
                    data.getMap().getDistance_IgnoreEndForCondition(unitTerritoryMap.get(o2.getKey()), t,
                        ProMatches.territoryCanMoveAirUnitsAndNoAA(player, data, true));
              }
            }
            if (distance1 != distance2) {
              return distance1 - distance2;
            }
          }
        }
        return o1.getKey().getType().getName().compareTo(o2.getKey().getType().getName());
      }
    });
    final Map<Unit, Set<Territory>> sortedUnitAttackOptions = new LinkedHashMap<>();
    for (final Map.Entry<Unit, Set<Territory>> entry : list) {
      sortedUnitAttackOptions.put(entry.getKey(), entry.getValue());
    }
    return sortedUnitAttackOptions;
  }

}
