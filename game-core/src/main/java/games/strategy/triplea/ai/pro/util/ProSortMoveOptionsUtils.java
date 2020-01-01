package games.strategy.triplea.ai.pro.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.pro.ProData;
import games.strategy.triplea.ai.pro.data.ProTerritory;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.delegate.battle.UnitBattleComparator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Pro AI attack options utilities. */
public final class ProSortMoveOptionsUtils {
  private ProSortMoveOptionsUtils() {}

  /**
   * Returns a copy of {@code unitAttackOptions} sorted by number of move options, then by cost of
   * unit, then by unit type name.
   */
  public static Map<Unit, Set<Territory>> sortUnitMoveOptions(
      final ProData proData, final Map<Unit, Set<Territory>> unitAttackOptions) {

    final List<Map.Entry<Unit, Set<Territory>>> list =
        new ArrayList<>(unitAttackOptions.entrySet());
    list.sort(
        (o1, o2) -> {

          // Sort by number of move options then cost of unit then unit type
          if (o1.getValue().size() != o2.getValue().size()) {
            return (o1.getValue().size() - o2.getValue().size());
          } else if (proData.getUnitValue(o1.getKey().getType())
              != proData.getUnitValue(o2.getKey().getType())) {
            return (proData.getUnitValue(o1.getKey().getType())
                - proData.getUnitValue(o2.getKey().getType()));
          }
          return o1.getKey().getType().getName().compareTo(o2.getKey().getType().getName());
        });
    final Map<Unit, Set<Territory>> sortedUnitAttackOptions = new LinkedHashMap<>();
    for (final Map.Entry<Unit, Set<Territory>> entry : list) {
      sortedUnitAttackOptions.put(entry.getKey(), entry.getValue());
    }
    return sortedUnitAttackOptions;
  }

  /**
   * Returns a copy of {@code unitAttackOptions} sorted by number of move options, then by cost of
   * unit, then by unit type name. The number of move options are calculated based on pending
   * battles which require additional units for the attacker to be successful.
   */
  public static Map<Unit, Set<Territory>> sortUnitNeededOptions(
      final ProData proData,
      final GamePlayer player,
      final Map<Unit, Set<Territory>> unitAttackOptions,
      final Map<Territory, ProTerritory> attackMap,
      final ProOddsCalculator calc) {
    final List<Map.Entry<Unit, Set<Territory>>> list =
        new ArrayList<>(unitAttackOptions.entrySet());
    list.sort(
        (o1, o2) -> {

          // Find number of territories that still need units
          int numOptions1 = 0;
          for (final Territory t : o1.getValue()) {
            final ProTerritory patd = attackMap.get(t);
            if (patd.getBattleResult() == null) {
              patd.estimateBattleResult(calc, player);
            }
            if (!patd.isCurrentlyWins()) {
              numOptions1++;
            }
          }
          int numOptions2 = 0;
          for (final Territory t : o2.getValue()) {
            final ProTerritory patd = attackMap.get(t);
            if (patd.getBattleResult() == null) {
              patd.estimateBattleResult(calc, player);
            }
            if (!patd.isCurrentlyWins()) {
              numOptions2++;
            }
          }

          // Sort by number of move options then cost of unit then unit type
          if (numOptions1 != numOptions2) {
            return (numOptions1 - numOptions2);
          }
          if (proData.getUnitValue(o1.getKey().getType())
              != proData.getUnitValue(o2.getKey().getType())) {
            return (proData.getUnitValue(o1.getKey().getType())
                - proData.getUnitValue(o2.getKey().getType()));
          }
          return o1.getKey().getType().getName().compareTo(o2.getKey().getType().getName());
        });
    final Map<Unit, Set<Territory>> sortedUnitAttackOptions = new LinkedHashMap<>();
    for (final Map.Entry<Unit, Set<Territory>> entry : list) {
      sortedUnitAttackOptions.put(entry.getKey(), entry.getValue());
    }
    return sortedUnitAttackOptions;
  }

  /**
   * Returns a copy of {@code unitAttackOptions} sorted by number of move options, then by attack
   * efficiency, then by air unit average movement distance, then by unit type name. The number of
   * move options are calculated based on pending battles which require additional units for the
   * attacker to be successful.
   */
  public static Map<Unit, Set<Territory>> sortUnitNeededOptionsThenAttack(
      final ProData proData,
      final GamePlayer player,
      final Map<Unit, Set<Territory>> unitAttackOptions,
      final Map<Territory, ProTerritory> attackMap,
      final ProOddsCalculator calc) {
    final GameData data = proData.getData();
    final Map<Unit, Territory> unitTerritoryMap = proData.getUnitTerritoryMap();

    final List<Map.Entry<Unit, Set<Territory>>> list =
        new ArrayList<>(unitAttackOptions.entrySet());
    list.sort(
        (o1, o2) -> {

          // Sort by number of territories that still need units
          int numOptions1 = 0;
          for (final Territory t : o1.getValue()) {
            final ProTerritory patd = attackMap.get(t);
            if (patd.getBattleResult() == null) {
              patd.estimateBattleResult(calc, player);
            }
            if (!patd.isCurrentlyWins()) {
              numOptions1++;
            }
          }
          int numOptions2 = 0;
          for (final Territory t : o2.getValue()) {
            final ProTerritory patd = attackMap.get(t);
            if (patd.getBattleResult() == null) {
              patd.estimateBattleResult(calc, player);
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
              final List<Unit> defendingUnits =
                  t.getUnitCollection().getMatches(Matches.enemyUnit(player, data));
              final List<Unit> sortedUnitsList = new ArrayList<>(attackMap.get(t).getUnits());
              sortedUnitsList.sort(
                  new UnitBattleComparator(
                          false,
                          proData.getUnitValueMap(),
                          TerritoryEffectHelper.getEffects(t),
                          data,
                          false,
                          false)
                      .reversed());
              final int powerWithout =
                  DiceRoll.getTotalPower(
                      DiceRoll.getUnitPowerAndRollsForNormalBattles(
                          sortedUnitsList,
                          defendingUnits,
                          false,
                          data,
                          t,
                          TerritoryEffectHelper.getEffects(t),
                          false,
                          null),
                      data);
              sortedUnitsList.add(o1.getKey());
              sortedUnitsList.sort(
                  new UnitBattleComparator(
                          false,
                          proData.getUnitValueMap(),
                          TerritoryEffectHelper.getEffects(t),
                          data,
                          false,
                          false)
                      .reversed());
              final int powerWith =
                  DiceRoll.getTotalPower(
                      DiceRoll.getUnitPowerAndRollsForNormalBattles(
                          sortedUnitsList,
                          defendingUnits,
                          false,
                          data,
                          t,
                          TerritoryEffectHelper.getEffects(t),
                          false,
                          null),
                      data);
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
          final double attackEfficiency1 =
              (double) minPower1 / proData.getUnitValue(o1.getKey().getType());
          int minPower2 = Integer.MAX_VALUE;
          for (final Territory t : o2.getValue()) {
            if (!attackMap.get(t).isCurrentlyWins()) {
              final List<Unit> defendingUnits =
                  t.getUnitCollection().getMatches(Matches.enemyUnit(player, data));
              final List<Unit> sortedUnitsList = new ArrayList<>(attackMap.get(t).getUnits());
              sortedUnitsList.sort(
                  new UnitBattleComparator(
                          false,
                          proData.getUnitValueMap(),
                          TerritoryEffectHelper.getEffects(t),
                          data,
                          false,
                          false)
                      .reversed());
              final int powerWithout =
                  DiceRoll.getTotalPower(
                      DiceRoll.getUnitPowerAndRollsForNormalBattles(
                          sortedUnitsList,
                          defendingUnits,
                          false,
                          data,
                          t,
                          TerritoryEffectHelper.getEffects(t),
                          false,
                          null),
                      data);
              sortedUnitsList.add(o2.getKey());
              sortedUnitsList.sort(
                  new UnitBattleComparator(
                          false,
                          proData.getUnitValueMap(),
                          TerritoryEffectHelper.getEffects(t),
                          data,
                          false,
                          false)
                      .reversed());
              final int powerWith =
                  DiceRoll.getTotalPower(
                      DiceRoll.getUnitPowerAndRollsForNormalBattles(
                          sortedUnitsList,
                          defendingUnits,
                          false,
                          data,
                          t,
                          TerritoryEffectHelper.getEffects(t),
                          false,
                          null),
                      data);
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
          final double attackEfficiency2 =
              (double) minPower2 / proData.getUnitValue(o2.getKey().getType());
          if (attackEfficiency1 != attackEfficiency2) {
            return (attackEfficiency1 < attackEfficiency2) ? 1 : -1;
          }

          // Check if unit types are equal and is air then sort by average distance
          if (o1.getKey().getType().equals(o2.getKey().getType())) {
            final boolean isAirUnit = UnitAttachment.get(o1.getKey().getType()).getIsAir();
            if (isAirUnit) {
              int distance1 = 0;
              for (final Territory t : o1.getValue()) {
                if (!attackMap.get(t).isCurrentlyWins()) {
                  distance1 +=
                      data.getMap()
                          .getDistance_IgnoreEndForCondition(
                              unitTerritoryMap.get(o1.getKey()),
                              t,
                              ProMatches.territoryCanMoveAirUnitsAndNoAa(player, data, true));
                }
              }
              int distance2 = 0;
              for (final Territory t : o2.getValue()) {
                if (!attackMap.get(t).isCurrentlyWins()) {
                  distance2 +=
                      data.getMap()
                          .getDistance_IgnoreEndForCondition(
                              unitTerritoryMap.get(o2.getKey()),
                              t,
                              ProMatches.territoryCanMoveAirUnitsAndNoAa(player, data, true));
                }
              }
              if (distance1 != distance2) {
                return distance1 - distance2;
              }
            }
          }
          return o1.getKey().getType().getName().compareTo(o2.getKey().getType().getName());
        });
    final Map<Unit, Set<Territory>> sortedUnitAttackOptions = new LinkedHashMap<>();
    for (final Map.Entry<Unit, Set<Territory>> entry : list) {
      sortedUnitAttackOptions.put(entry.getKey(), entry.getValue());
    }
    return sortedUnitAttackOptions;
  }
}
