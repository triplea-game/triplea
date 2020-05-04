package games.strategy.triplea.delegate.battle.casualty;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.DiceRoll.TotalPowerAndTotalRolls;
import games.strategy.triplea.delegate.battle.UnitBattleComparator;
import games.strategy.triplea.delegate.battle.UnitBattleComparator.CombatModifiers;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.UtilityClass;
import org.triplea.java.collections.IntegerMap;
import org.triplea.performance.PerfTimer;

@UtilityClass
public class CasualtyOrderOfLosses {
  private final Map<String, List<UnitType>> oolCache = new ConcurrentHashMap<>();

  void clearOolCache() {
    oolCache.clear();
  }

  @Builder
  @Value
  public static class Parameters {
    @Nonnull Collection<Unit> targetsToPickFrom;
    @Nonnull GamePlayer player;
    @Nonnull Collection<Unit> enemyUnits;
    @Nonnull Collection<Unit> amphibiousLandAttackers;
    @Nonnull Territory battlesite;
    @Nonnull IntegerMap<UnitType> costs;
    @Nonnull CombatModifiers combatModifiers;
    @Nonnull GameData data;
    int hits;
  }

  /**
   * The purpose of this is to return a list in the PERFECT order of which units should be selected
   * to die first, And that means that certain units MUST BE INTERLEAVED. This list assumes that you
   * have already taken any extra hit points away from any 2 hitpoint units. Example: You have a 1
   * attack Artillery unit that supports, and a 1 attack infantry unit that can receive support. The
   * best selection of units to die is first to take whichever unit has excess, then cut that down
   * til they are both the same size, then to take 1 artillery followed by 1 infantry, followed by 1
   * artillery, then 1 inf, etc, until everyone is dead. If you just return all infantry followed by
   * all artillery, or the other way around, you will be missing out on some important support
   * provided. (Veqryn)
   */
  List<Unit> sortUnitsForCasualtiesWithSupport(final Parameters parameters) {
    PerfTimer.time("OLD", () -> oldsortUnitsForCasualtiesWithSupport(parameters));

    return PerfTimer.time("NEW", () -> OrderOfLossesCalculatorByUnitGroup.builder()
        .parameters(parameters)
        .build()
        .sortUnitsForCasualtiesWithSupport());
  }

  List<Unit> oldsortUnitsForCasualtiesWithSupport(final Parameters parameters) {
    // Convert unit lists to unit type lists
    final List<UnitType> targetTypes = new ArrayList<>();
    for (final Unit u : parameters.targetsToPickFrom) {
      targetTypes.add(u.getType());
    }
    final List<UnitType> amphibTypes = new ArrayList<>();
    for (final Unit u : parameters.amphibiousLandAttackers) {
      amphibTypes.add(u.getType());
    }
    // Calculate hashes and cache key
    int targetsHashCode = 1;
    for (final UnitType ut : targetTypes) {
      targetsHashCode += ut.hashCode();
    }
    targetsHashCode *= 31;
    int amphibHashCode = 1;
    for (final UnitType ut : amphibTypes) {
      amphibHashCode += ut.hashCode();
    }
    amphibHashCode *= 31;
    String key =
        parameters.player.getName()
            + "|"
            + parameters.battlesite.getName()
            + "|"
            + parameters.combatModifiers.isDefending()
            + "|"
            + parameters.combatModifiers.isAmphibious()
            + "|"
            + targetsHashCode
            + "|"
            + amphibHashCode;
    // Check OOL cache
    final List<UnitType> stored = oolCache.get(key);
    if (stored != null) {
      final List<Unit> result = new ArrayList<>();
      final List<Unit> selectFrom = new ArrayList<>(parameters.targetsToPickFrom);
      for (final UnitType ut : stored) {
        for (final Iterator<Unit> it = selectFrom.iterator(); it.hasNext(); ) {
          final Unit u = it.next();
          if (ut.equals(u.getType())) {
            result.add(u);
            it.remove();
          }
        }
      }
      return result;
    }
    // Sort enough units to kill off
    final List<Unit> sortedUnitsList = new ArrayList<>(parameters.targetsToPickFrom);
    sortedUnitsList.sort(
        new UnitBattleComparator(
                parameters.combatModifiers.isDefending(),
                parameters.costs,
                parameters.combatModifiers.getTerritoryEffects(),
                parameters.data,
                true,
                false,
                parameters.combatModifiers.isAmphibious())
            .reversed());
    // Sort units starting with strongest so that support gets added to them first
    final UnitBattleComparator unitComparatorWithoutPrimaryPower =
        new UnitBattleComparator(
            parameters.combatModifiers.isDefending(),
            parameters.costs,
            parameters.combatModifiers.getTerritoryEffects(),
            parameters.data,
            true,
            true,
            parameters.combatModifiers.isAmphibious());
    final Map<Unit, IntegerMap<Unit>> unitSupportPowerMap = new HashMap<>();
    final Map<Unit, IntegerMap<Unit>> unitSupportRollsMap = new HashMap<>();
    final Map<Unit, TotalPowerAndTotalRolls> unitPowerAndRollsMap =
        DiceRoll.getUnitPowerAndRollsForNormalBattles(
            sortedUnitsList,
            new ArrayList<>(parameters.enemyUnits),
            sortedUnitsList,
            parameters.combatModifiers.isDefending(),
            parameters.data,
            parameters.battlesite,
            parameters.combatModifiers.getTerritoryEffects(),
            parameters.combatModifiers.isAmphibious(),
            parameters.amphibiousLandAttackers,
            unitSupportPowerMap,
            unitSupportRollsMap);

    // Sort units starting with weakest for finding the worst units
    Collections.reverse(sortedUnitsList);
    final List<Unit> sortedWellEnoughUnitsList = new ArrayList<>();
    final Map<Unit, TotalPowerAndTotalRolls> originalUnitPowerAndRollsMap =
        new HashMap<>(unitPowerAndRollsMap);
    for (int i = 0; i < sortedUnitsList.size(); ++i) {
      // Loop through all target units to find the best unit to take as casualty
      Unit worstUnit = null;
      int minPower = Integer.MAX_VALUE;
      final Set<UnitType> unitTypes = new HashSet<>();
      for (final Unit u : sortedUnitsList) {
        if (unitTypes.contains(u.getType())) {
          continue;
        }
        unitTypes.add(u.getType());
        // Find unit power
        int power =
            DiceRoll.getTotalPower(Map.of(u, originalUnitPowerAndRollsMap.get(u)), parameters.data);
        // Add any support power that it provides to other units
        final IntegerMap<Unit> unitSupportPowerMapForUnit = unitSupportPowerMap.get(u);
        if (unitSupportPowerMapForUnit != null) {
          for (final Unit supportedUnit : unitSupportPowerMapForUnit.keySet()) {
            TotalPowerAndTotalRolls strengthAndRolls = unitPowerAndRollsMap.get(supportedUnit);
            if (strengthAndRolls == null) {
              continue;
            }
            // Remove any rolls provided by this support so they aren't counted twice
            final IntegerMap<Unit> unitSupportRollsMapForUnit = unitSupportRollsMap.get(u);
            if (unitSupportRollsMapForUnit != null) {
              strengthAndRolls =
                  strengthAndRolls.subtractRolls(unitSupportRollsMapForUnit.getInt(supportedUnit));
            }
            // If one roll then just add the power
            if (strengthAndRolls.getTotalRolls() == 1) {
              power += unitSupportPowerMapForUnit.getInt(supportedUnit);
              continue;
            }
            // Find supported unit power with support
            final Map<Unit, TotalPowerAndTotalRolls> supportedUnitMap = new HashMap<>();
            supportedUnitMap.put(supportedUnit, strengthAndRolls);
            final int powerWithSupport = DiceRoll.getTotalPower(supportedUnitMap, parameters.data);
            // Find supported unit power without support

            final TotalPowerAndTotalRolls strengthAndRollsWithoutSupport =
                strengthAndRolls.subtractPower(unitSupportPowerMapForUnit.getInt(supportedUnit));
            supportedUnitMap.put(supportedUnit, strengthAndRollsWithoutSupport);
            final int powerWithoutSupport =
                DiceRoll.getTotalPower(supportedUnitMap, parameters.data);
            // Add the actual power provided by the support
            final int addedPower = powerWithSupport - powerWithoutSupport;
            power += addedPower;
          }
        }
        // Add any power from support rolls that it provides to other units
        final IntegerMap<Unit> unitSupportRollsMapForUnit = unitSupportRollsMap.get(u);
        if (unitSupportRollsMapForUnit != null) {
          for (final Unit supportedUnit : unitSupportRollsMapForUnit.keySet()) {
            final TotalPowerAndTotalRolls strengthAndRolls =
                unitPowerAndRollsMap.get(supportedUnit);
            if (strengthAndRolls == null) {
              continue;
            }
            // Find supported unit power with support
            final Map<Unit, TotalPowerAndTotalRolls> supportedUnitMap = new HashMap<>();
            supportedUnitMap.put(supportedUnit, strengthAndRolls);
            final int powerWithSupport = DiceRoll.getTotalPower(supportedUnitMap, parameters.data);
            // Find supported unit power without support
            final TotalPowerAndTotalRolls strengthAndRollsWithoutSupport =
                strengthAndRolls.subtractRolls(unitSupportRollsMap.get(u).getInt(supportedUnit));
            supportedUnitMap.put(supportedUnit, strengthAndRollsWithoutSupport);
            final int powerWithoutSupport =
                DiceRoll.getTotalPower(supportedUnitMap, parameters.data);
            // Add the actual power provided by the support
            final int addedPower = powerWithSupport - powerWithoutSupport;
            power += addedPower;
          }
        }
        // Check if unit has lower power
        if (power < minPower
            || (power == minPower && unitComparatorWithoutPrimaryPower.compare(u, worstUnit) < 0)) {
          worstUnit = u;
          minPower = power;
        }
      }
      // Add worst unit to sorted list, update any units it supported, and remove from other
      // collections
      final IntegerMap<Unit> unitSupportPowerMapForUnit = unitSupportPowerMap.get(worstUnit);
      if (unitSupportPowerMapForUnit != null) {
        for (final Unit supportedUnit : unitSupportPowerMapForUnit.keySet()) {
          final TotalPowerAndTotalRolls strengthAndRolls = unitPowerAndRollsMap.get(supportedUnit);
          if (strengthAndRolls == null) {
            continue;
          }
          final TotalPowerAndTotalRolls strengthAndRollsWithoutSupport =
              strengthAndRolls.subtractPower(unitSupportPowerMapForUnit.getInt(supportedUnit));
          unitPowerAndRollsMap.put(supportedUnit, strengthAndRollsWithoutSupport);
          sortedUnitsList.remove(supportedUnit);
          sortedUnitsList.add(0, supportedUnit);
        }
      }
      final IntegerMap<Unit> unitSupportRollsMapForUnit = unitSupportRollsMap.get(worstUnit);
      if (unitSupportRollsMapForUnit != null) {
        for (final Unit supportedUnit : unitSupportRollsMapForUnit.keySet()) {
          final TotalPowerAndTotalRolls strengthAndRolls = unitPowerAndRollsMap.get(supportedUnit);
          if (strengthAndRolls == null) {
            continue;
          }
          final TotalPowerAndTotalRolls strengthAndRollsWithoutSupport =
              strengthAndRolls.subtractRolls(unitSupportRollsMapForUnit.getInt(supportedUnit));
          unitPowerAndRollsMap.put(supportedUnit, strengthAndRollsWithoutSupport);
          sortedUnitsList.remove(supportedUnit);
          sortedUnitsList.add(0, supportedUnit);
        }
      }
      sortedWellEnoughUnitsList.add(worstUnit);
      sortedUnitsList.remove(worstUnit);
      unitPowerAndRollsMap.remove(worstUnit);
      unitSupportPowerMap.remove(worstUnit);
      unitSupportRollsMap.remove(worstUnit);
    }
    sortedWellEnoughUnitsList.addAll(sortedUnitsList);
    // Cache result and all subsets of the result
    final List<UnitType> unitTypes = new ArrayList<>();
    for (final Unit u : sortedWellEnoughUnitsList) {
      unitTypes.add(u.getType());
    }
    for (final Iterator<UnitType> it = unitTypes.iterator(); it.hasNext(); ) {
      oolCache.put(key, new ArrayList<>(unitTypes));
      final UnitType unitTypeToRemove = it.next();
      targetTypes.remove(unitTypeToRemove);
      if (Collections.frequency(targetTypes, unitTypeToRemove)
          < Collections.frequency(amphibTypes, unitTypeToRemove)) {
        amphibTypes.remove(unitTypeToRemove);
      }
      targetsHashCode = 1;
      for (final UnitType ut : targetTypes) {
        targetsHashCode += ut.hashCode();
      }
      targetsHashCode *= 31;
      amphibHashCode = 1;
      for (final UnitType ut : amphibTypes) {
        amphibHashCode += ut.hashCode();
      }
      amphibHashCode *= 31;
      key =
          parameters.player.getName()
              + "|"
              + parameters.battlesite.getName()
              + "|"
              + parameters.combatModifiers.isDefending()
              + "|"
              + parameters.combatModifiers.isAmphibious()
              + "|"
              + targetsHashCode
              + "|"
              + amphibHashCode;
      it.remove();
    }
    return sortedWellEnoughUnitsList;
  }
}
