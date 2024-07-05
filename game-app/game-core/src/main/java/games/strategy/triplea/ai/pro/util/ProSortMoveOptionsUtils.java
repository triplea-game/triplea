package games.strategy.triplea.ai.pro.util;

import com.google.common.base.Preconditions;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ai.pro.ProData;
import games.strategy.triplea.ai.pro.data.ProTerritory;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.power.calculator.CombatValueBuilder;
import games.strategy.triplea.delegate.power.calculator.PowerStrengthAndRolls;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
          final Collection<Territory> territories1 =
              removeWinningTerritories(o1.getValue(), player, attackMap, calc);
          final Collection<Territory> territories2 =
              removeWinningTerritories(o2.getValue(), player, attackMap, calc);

          // Sort by number of territories that still need units
          if (territories1.size() != territories2.size()) {
            return territories1.size() - territories2.size();
          }
          final UnitType unitType1 = o1.getKey().getType();
          final UnitType unitType2 = o2.getKey().getType();
          final int value1 = proData.getUnitValue(unitType1);
          final int value2 = proData.getUnitValue(unitType2);
          if (value1 != value2) {
            return value1 - value2;
          }
          return unitType1.getName().compareTo(unitType2.getName());
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
    final GameState data = proData.getData();
    final Map<Unit, Territory> unitTerritoryMap = proData.getUnitTerritoryMap();

    final List<Map.Entry<Unit, Set<Territory>>> list =
        new ArrayList<>(unitAttackOptions.entrySet());
    final Map<Object, Double> attackEfficiencyCache = new HashMap<>();
    list.sort(
        (o1, o2) -> {
          final Collection<Territory> territories1 =
              removeWinningTerritories(o1.getValue(), player, attackMap, calc);
          final Collection<Territory> territories2 =
              removeWinningTerritories(o2.getValue(), player, attackMap, calc);

          // Sort by number of territories that still need units
          if (territories1.size() != territories2.size()) {
            return territories1.size() - territories2.size();
          }
          if (territories1.isEmpty()) {
            return 0;
          }

          final Unit u1 = o1.getKey();
          final Unit u2 = o2.getKey();

          // Sort by attack efficiency
          final double attackEfficiency1 =
              attackEfficiencyCache.computeIfAbsent(
                  o1, k -> calculateAttackEfficiency(proData, player, attackMap, territories1, u1));
          final double attackEfficiency2 =
              attackEfficiencyCache.computeIfAbsent(
                  o2, k -> calculateAttackEfficiency(proData, player, attackMap, territories2, u2));
          if (attackEfficiency1 != attackEfficiency2) {
            return (attackEfficiency1 < attackEfficiency2) ? 1 : -1;
          }

          final UnitType unitType1 = u1.getType();
          final UnitType unitType2 = u2.getType();

          // If unit types are equal and are air, then sort by average distance.
          if (unitType1.equals(unitType2) && unitType1.getUnitAttachment().getIsAir()) {
            final Predicate<Territory> predicate =
                ProMatches.territoryCanMoveAirUnitsAndNoAa(data, player, true);
            final Territory territory1 = unitTerritoryMap.get(u1);
            final Territory territory2 = unitTerritoryMap.get(u2);
            int distance1 = 0;
            for (final Territory t : territories1) {
              distance1 += data.getMap().getDistanceIgnoreEndForCondition(territory1, t, predicate);
            }
            int distance2 = 0;
            for (final Territory t : territories2) {
              distance2 += data.getMap().getDistanceIgnoreEndForCondition(territory2, t, predicate);
            }
            if (distance1 != distance2) {
              return distance1 - distance2;
            }
          }

          return unitType1.getName().compareTo(unitType2.getName());
        });
    final Map<Unit, Set<Territory>> sortedUnitAttackOptions = new LinkedHashMap<>();
    for (final Map.Entry<Unit, Set<Territory>> entry : list) {
      sortedUnitAttackOptions.put(entry.getKey(), entry.getValue());
    }
    return sortedUnitAttackOptions;
  }

  private static Collection<Territory> removeWinningTerritories(
      final Collection<Territory> territories,
      final GamePlayer player,
      final Map<Territory, ProTerritory> attackMap,
      final ProOddsCalculator calc) {
    return territories.stream()
        .filter(
            t -> {
              final ProTerritory patd = attackMap.get(t);
              if (patd.getBattleResult() == null) {
                patd.estimateBattleResult(calc, player);
              }
              return !patd.isCurrentlyWins();
            })
        .collect(Collectors.toList());
  }

  private static double calculateAttackEfficiency(
      final ProData proData,
      final GamePlayer player,
      final Map<Territory, ProTerritory> attackMap,
      final Collection<Territory> territories,
      final Unit unit) {
    final GameData data = proData.getData();

    int minPower = Integer.MAX_VALUE;
    for (final Territory t : territories) {
      final List<Unit> defendingUnits = t.getMatches(Matches.enemyUnit(player));
      final Collection<Unit> attackingUnits = new ArrayList<>(attackMap.get(t).getUnits());
      // Compare the difference in total power when including the unit or not.
      int powerDifference = 0;
      for (final boolean includeUnit : new boolean[] {false, true}) {
        if (includeUnit) {
          attackingUnits.add(unit);
        }
        powerDifference +=
            (includeUnit ? 1 : -1)
                * PowerStrengthAndRolls.build(
                        attackingUnits,
                        CombatValueBuilder.mainCombatValue()
                            .enemyUnits(defendingUnits)
                            .friendlyUnits(attackingUnits)
                            .side(BattleState.Side.OFFENSE)
                            .gameSequence(data.getSequence())
                            .supportAttachments(data.getUnitTypeList().getSupportRules())
                            .lhtrHeavyBombers(Properties.getLhtrHeavyBombers(data.getProperties()))
                            .gameDiceSides(data.getDiceSides())
                            .territoryEffects(TerritoryEffectHelper.getEffects(t))
                            .build())
                    .calculateTotalPower();
      }
      if (powerDifference < minPower) {
        minPower = powerDifference;
      }
    }

    if (unit.getUnitAttachment().getIsAir()) {
      minPower *= 10;
    }
    double result = (double) minPower / proData.getUnitValue(unit.getType());
    Preconditions.checkState(Double.isFinite(result));
    return result;
  }
}
