package games.strategy.triplea.delegate.battle.casualty;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.player.Player;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ai.weak.WeakAi;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.BaseEditDelegate;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.UnitBattleComparator;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import games.strategy.triplea.delegate.data.CasualtyList;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.util.TuvUtils;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeparator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import lombok.extern.java.Log;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Tuple;

/**
 * Utility class for determining casualties and selecting casualties. The code was being duplicated
 * all over the place.
 */
@Log
public class CasualtySelector {
  private static final Map<String, List<UnitType>> oolCache = new ConcurrentHashMap<>();

  private CasualtySelector() {}

  public static void clearOolCache() {
    oolCache.clear();
  }

  /**
   * Selects casualties for the specified battle.
   *
   * @param battleId may be null if we are not in a battle (eg, if this is an aa fire due to
   *     moving).
   */
  public static CasualtyDetails selectCasualties(
      final GamePlayer player,
      final Collection<Unit> targetsToPickFrom,
      final Collection<Unit> friendlyUnits,
      final Collection<Unit> enemyUnits,
      final boolean amphibious,
      final Collection<Unit> amphibiousLandAttackers,
      final Territory battlesite,
      final Collection<TerritoryEffect> territoryEffects,
      final IDelegateBridge bridge,
      final String text,
      final DiceRoll dice,
      final boolean defending,
      final UUID battleId,
      final boolean headLess,
      final int extraHits,
      final boolean allowMultipleHitsPerUnit) {
    if (targetsToPickFrom.isEmpty()) {
      return new CasualtyDetails();
    }
    if (!friendlyUnits.containsAll(targetsToPickFrom)) {
      throw new IllegalStateException(
          "friendlyUnits should but does not contain all units from targetsToPickFrom"
              + ", battlesite: "
              + battlesite
              + ", friendlyUnits: "
              + friendlyUnits
              + ", targetsToPickFrom: "
              + targetsToPickFrom);
    }
    final GameData data = bridge.getData();
    final boolean isEditMode = BaseEditDelegate.getEditMode(data);
    final Player tripleaPlayer =
        player.isNull() ? new WeakAi(player.getName()) : bridge.getRemotePlayer(player);
    final Map<Unit, Collection<Unit>> dependents =
        headLess ? Map.of() : CasualtyUtil.getDependents(targetsToPickFrom);
    if (isEditMode && !headLess) {
      final CasualtyDetails editSelection =
          tripleaPlayer.selectCasualties(
              targetsToPickFrom,
              dependents,
              0,
              text,
              dice,
              player,
              friendlyUnits,
              enemyUnits,
              amphibious,
              amphibiousLandAttackers,
              new CasualtyList(),
              battleId,
              battlesite,
              allowMultipleHitsPerUnit);
      final List<Unit> killed = editSelection.getKilled();
      // if partial retreat is possible, kill amphibious units first
      if (Properties.getPartialAmphibiousRetreat(data)) {
        killAmphibiousFirst(killed, targetsToPickFrom);
      }
      return editSelection;
    }
    if (dice.getHits() == 0) {
      return new CasualtyDetails(List.of(), List.of(), true);
    }
    int hitsRemaining = dice.getHits();
    if (Properties.getTransportCasualtiesRestricted(data)) {
      hitsRemaining = extraHits;
    }
    if (!isEditMode && allTargetsOneTypeOneHitPoint(targetsToPickFrom, dependents)) {
      final List<Unit> killed = new ArrayList<>();
      final Iterator<Unit> iter = targetsToPickFrom.iterator();
      for (int i = 0; i < hitsRemaining; i++) {
        if (i >= targetsToPickFrom.size()) {
          break;
        }
        killed.add(iter.next());
      }
      return new CasualtyDetails(killed, List.of(), true);
    }
    // Create production cost map, Maybe should do this elsewhere, but in case prices change, we do
    // it here.
    final IntegerMap<UnitType> costs = TuvUtils.getCostsForTuv(player, data);
    final Tuple<CasualtyList, List<Unit>> defaultCasualtiesAndSortedTargets =
        getDefaultCasualties(
            targetsToPickFrom,
            hitsRemaining,
            defending,
            player,
            enemyUnits,
            amphibious,
            amphibiousLandAttackers,
            battlesite,
            costs,
            territoryEffects,
            data,
            allowMultipleHitsPerUnit);
    final CasualtyList defaultCasualties = defaultCasualtiesAndSortedTargets.getFirst();
    final List<Unit> sortedTargetsToPickFrom = defaultCasualtiesAndSortedTargets.getSecond();
    if (sortedTargetsToPickFrom.size() != targetsToPickFrom.size()
        || !targetsToPickFrom.containsAll(sortedTargetsToPickFrom)
        || !sortedTargetsToPickFrom.containsAll(targetsToPickFrom)) {
      throw new IllegalStateException(
          "sortedTargetsToPickFrom must contain the same units as targetsToPickFrom list");
    }
    final int totalHitpoints =
        (allowMultipleHitsPerUnit
            ? CasualtyUtil.getTotalHitpointsLeft(sortedTargetsToPickFrom)
            : sortedTargetsToPickFrom.size());
    final CasualtyDetails casualtySelection;
    if (hitsRemaining >= totalHitpoints) {
      casualtySelection = new CasualtyDetails(defaultCasualties, true);
    } else {
      casualtySelection =
          tripleaPlayer.selectCasualties(
              sortedTargetsToPickFrom,
              dependents,
              hitsRemaining,
              text,
              dice,
              player,
              friendlyUnits,
              enemyUnits,
              amphibious,
              amphibiousLandAttackers,
              defaultCasualties,
              battleId,
              battlesite,
              allowMultipleHitsPerUnit);
    }
    final List<Unit> killed = casualtySelection.getKilled();
    // if partial retreat is possible, kill amphibious units first
    if (Properties.getPartialAmphibiousRetreat(data)) {
      killAmphibiousFirst(killed, sortedTargetsToPickFrom);
    }
    final List<Unit> damaged = casualtySelection.getDamaged();
    int numhits = killed.size();
    if (!allowMultipleHitsPerUnit) {
      damaged.clear();
    } else {
      for (final Unit unit : killed) {
        final UnitAttachment ua = UnitAttachment.get(unit.getType());
        final int damageToUnit = Collections.frequency(damaged, unit);
        // allowed damage
        numhits += Math.max(0, Math.min(damageToUnit, (ua.getHitPoints() - (1 + unit.getHits()))));
        // remove from damaged list, since they will die
        damaged.removeIf(unit::equals);
      }
    }
    // check right number
    if (!isEditMode && numhits + damaged.size() != Math.min(hitsRemaining, totalHitpoints)) {
      tripleaPlayer.reportError("Wrong number of casualties selected");
      if (headLess) {
        log.severe(
            "Possible Infinite Loop: Wrong number of casualties selected: number of hits on units "
                + (numhits + damaged.size())
                + " != number of hits to take "
                + Math.min(hitsRemaining, totalHitpoints)
                + ", for "
                + casualtySelection.toString());
      }
      return selectCasualties(
          player,
          sortedTargetsToPickFrom,
          friendlyUnits,
          enemyUnits,
          amphibious,
          amphibiousLandAttackers,
          battlesite,
          territoryEffects,
          bridge,
          text,
          dice,
          defending,
          battleId,
          headLess,
          extraHits,
          allowMultipleHitsPerUnit);
    }
    // check we have enough of each type
    if (!sortedTargetsToPickFrom.containsAll(killed)
        || !sortedTargetsToPickFrom.containsAll(damaged)) {
      tripleaPlayer.reportError("Cannot remove enough units of those types");
      if (headLess) {
        log.severe(
            "Possible Infinite Loop: Cannot remove enough units of those types: targets "
                + MyFormatter.unitsToTextNoOwner(sortedTargetsToPickFrom)
                + ", for "
                + casualtySelection.toString());
      }
      return selectCasualties(
          player,
          sortedTargetsToPickFrom,
          friendlyUnits,
          enemyUnits,
          amphibious,
          amphibiousLandAttackers,
          battlesite,
          territoryEffects,
          bridge,
          text,
          dice,
          defending,
          battleId,
          headLess,
          extraHits,
          allowMultipleHitsPerUnit);
    }
    return casualtySelection;
  }

  private static void killAmphibiousFirst(final List<Unit> killed, final Collection<Unit> targets) {
    // Get a list of all selected killed units that are NOT amphibious
    final Predicate<Unit> match = Matches.unitIsLand().and(Matches.unitWasNotAmphibious());
    final Collection<Unit> killedNonAmphibUnits =
        new ArrayList<>(CollectionUtils.getMatches(killed, match));
    // If all killed units are amphibious, just return them
    if (killedNonAmphibUnits.isEmpty()) {
      return;
    }
    // Get a list of all units that are amphibious and remove those that are killed
    final Collection<Unit> allAmphibUnits =
        new ArrayList<>(CollectionUtils.getMatches(targets, Matches.unitWasAmphibious()));
    allAmphibUnits.removeAll(CollectionUtils.getMatches(killed, Matches.unitWasAmphibious()));
    // Get a collection of the unit types of the amphib units
    final Collection<UnitType> amphibTypes = new ArrayList<>();
    for (final Unit unit : allAmphibUnits) {
      final UnitType ut = unit.getType();
      if (!amphibTypes.contains(ut)) {
        amphibTypes.add(ut);
      }
    }
    // For each killed unit- see if there is an amphib unit that can be killed instead
    for (final Unit unit : killedNonAmphibUnits) {
      if (amphibTypes.contains(unit.getType())) { // add a unit from the collection
        final List<Unit> oneAmphibUnit =
            CollectionUtils.getNMatches(allAmphibUnits, 1, Matches.unitIsOfType(unit.getType()));
        if (!oneAmphibUnit.isEmpty()) {
          final Unit amphibUnit = oneAmphibUnit.iterator().next();
          killed.remove(unit);
          killed.add(amphibUnit);
          allAmphibUnits.remove(amphibUnit);
        } else { // If there are no more units of that type, remove the type from the collection
          amphibTypes.remove(unit.getType());
        }
      }
    }
  }

  /**
   * A unit with two hitpoints will be listed twice if they will die. The first time they are listed
   * it is as damaged. The second time they are listed, it is dead.
   */
  private static Tuple<CasualtyList, List<Unit>> getDefaultCasualties(
      final Collection<Unit> targetsToPickFrom,
      final int hits,
      final boolean defending,
      final GamePlayer player,
      final Collection<Unit> enemyUnits,
      final boolean amphibious,
      final Collection<Unit> amphibiousLandAttackers,
      final Territory battlesite,
      final IntegerMap<UnitType> costs,
      final Collection<TerritoryEffect> territoryEffects,
      final GameData data,
      final boolean allowMultipleHitsPerUnit) {
    final CasualtyList defaultCasualtySelection = new CasualtyList();
    // Sort units by power and cost in ascending order
    final List<Unit> sorted;
    sorted =
        sortUnitsForCasualtiesWithSupport(
            targetsToPickFrom,
            defending,
            player,
            enemyUnits,
            amphibious,
            amphibiousLandAttackers,
            battlesite,
            costs,
            territoryEffects,
            data,
            true);
    // Remove two hit bb's selecting them first for default casualties
    int numSelectedCasualties = 0;
    if (allowMultipleHitsPerUnit) {
      for (final Unit unit : sorted) {
        // Stop if we have already selected as many hits as there are targets
        if (numSelectedCasualties >= hits) {
          return Tuple.of(defaultCasualtySelection, sorted);
        }
        final UnitAttachment ua = UnitAttachment.get(unit.getType());
        final int extraHitPoints =
            Math.min((hits - numSelectedCasualties), (ua.getHitPoints() - (1 + unit.getHits())));
        for (int i = 0; i < extraHitPoints; i++) {
          numSelectedCasualties++;
          defaultCasualtySelection.addToDamaged(unit);
        }
      }
    }
    // Select units
    for (final Unit unit : sorted) {
      // Stop if we have already selected as many hits as there are targets
      if (numSelectedCasualties >= hits) {
        return Tuple.of(defaultCasualtySelection, sorted);
      }
      defaultCasualtySelection.addToKilled(unit);
      numSelectedCasualties++;
    }
    return Tuple.of(defaultCasualtySelection, sorted);
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
  private static List<Unit> sortUnitsForCasualtiesWithSupport(
      final Collection<Unit> targetsToPickFrom,
      final boolean defending,
      final GamePlayer player,
      final Collection<Unit> enemyUnits,
      final boolean amphibious,
      final Collection<Unit> amphibiousLandAttackers,
      final Territory battlesite,
      final IntegerMap<UnitType> costs,
      final Collection<TerritoryEffect> territoryEffects,
      final GameData data,
      final boolean bonus) {

    // Convert unit lists to unit type lists
    final List<UnitType> targetTypes = new ArrayList<>();
    for (final Unit u : targetsToPickFrom) {
      targetTypes.add(u.getType());
    }
    final List<UnitType> amphibTypes = new ArrayList<>();
    if (amphibiousLandAttackers != null) {
      for (final Unit u : amphibiousLandAttackers) {
        amphibTypes.add(u.getType());
      }
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
        player.getName()
            + "|"
            + battlesite.getName()
            + "|"
            + defending
            + "|"
            + amphibious
            + "|"
            + targetsHashCode
            + "|"
            + amphibHashCode;
    // Check OOL cache
    final List<UnitType> stored = oolCache.get(key);
    if (stored != null) {
      final List<Unit> result = new ArrayList<>();
      final List<Unit> selectFrom = new ArrayList<>(targetsToPickFrom);
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
    final List<Unit> sortedUnitsList = new ArrayList<>(targetsToPickFrom);
    sortedUnitsList.sort(
        new UnitBattleComparator(defending, costs, territoryEffects, data, bonus, false)
            .reversed());
    // Sort units starting with strongest so that support gets added to them first
    final UnitBattleComparator unitComparatorWithoutPrimaryPower =
        new UnitBattleComparator(defending, costs, territoryEffects, data, bonus, true);
    final Map<Unit, IntegerMap<Unit>> unitSupportPowerMap = new HashMap<>();
    final Map<Unit, IntegerMap<Unit>> unitSupportRollsMap = new HashMap<>();
    final Map<Unit, Tuple<Integer, Integer>> unitPowerAndRollsMap =
        DiceRoll.getUnitPowerAndRollsForNormalBattles(
            sortedUnitsList,
            new ArrayList<>(enemyUnits),
            sortedUnitsList,
            defending,
            data,
            battlesite,
            territoryEffects,
            amphibious,
            amphibiousLandAttackers,
            unitSupportPowerMap,
            unitSupportRollsMap);
    // Sort units starting with weakest for finding the worst units
    Collections.reverse(sortedUnitsList);
    final List<Unit> sortedWellEnoughUnitsList = new ArrayList<>();
    final Map<Unit, Tuple<Integer, Integer>> originalUnitPowerAndRollsMap =
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
        int power = DiceRoll.getTotalPower(Map.of(u, originalUnitPowerAndRollsMap.get(u)), data);
        // Add any support power that it provides to other units
        final IntegerMap<Unit> unitSupportPowerMapForUnit = unitSupportPowerMap.get(u);
        if (unitSupportPowerMapForUnit != null) {
          for (final Unit supportedUnit : unitSupportPowerMapForUnit.keySet()) {
            Tuple<Integer, Integer> strengthAndRolls = unitPowerAndRollsMap.get(supportedUnit);
            if (strengthAndRolls == null) {
              continue;
            }
            // Remove any rolls provided by this support so they aren't counted twice
            final IntegerMap<Unit> unitSupportRollsMapForUnit = unitSupportRollsMap.get(u);
            if (unitSupportRollsMapForUnit != null) {
              strengthAndRolls =
                  Tuple.of(
                      strengthAndRolls.getFirst(),
                      strengthAndRolls.getSecond()
                          - unitSupportRollsMapForUnit.getInt(supportedUnit));
            }
            // If one roll then just add the power
            if (strengthAndRolls.getSecond() == 1) {
              power += unitSupportPowerMapForUnit.getInt(supportedUnit);
              continue;
            }
            // Find supported unit power with support
            final Map<Unit, Tuple<Integer, Integer>> supportedUnitMap = new HashMap<>();
            supportedUnitMap.put(supportedUnit, strengthAndRolls);
            final int powerWithSupport = DiceRoll.getTotalPower(supportedUnitMap, data);
            // Find supported unit power without support
            final int strengthWithoutSupport =
                strengthAndRolls.getFirst() - unitSupportPowerMapForUnit.getInt(supportedUnit);
            final Tuple<Integer, Integer> strengthAndRollsWithoutSupport =
                Tuple.of(strengthWithoutSupport, strengthAndRolls.getSecond());
            supportedUnitMap.put(supportedUnit, strengthAndRollsWithoutSupport);
            final int powerWithoutSupport = DiceRoll.getTotalPower(supportedUnitMap, data);
            // Add the actual power provided by the support
            final int addedPower = powerWithSupport - powerWithoutSupport;
            power += addedPower;
          }
        }
        // Add any power from support rolls that it provides to other units
        final IntegerMap<Unit> unitSupportRollsMapForUnit = unitSupportRollsMap.get(u);
        if (unitSupportRollsMapForUnit != null) {
          for (final Unit supportedUnit : unitSupportRollsMapForUnit.keySet()) {
            final Tuple<Integer, Integer> strengthAndRolls =
                unitPowerAndRollsMap.get(supportedUnit);
            if (strengthAndRolls == null) {
              continue;
            }
            // Find supported unit power with support
            final Map<Unit, Tuple<Integer, Integer>> supportedUnitMap = new HashMap<>();
            supportedUnitMap.put(supportedUnit, strengthAndRolls);
            final int powerWithSupport = DiceRoll.getTotalPower(supportedUnitMap, data);
            // Find supported unit power without support
            final int rollsWithoutSupport =
                strengthAndRolls.getSecond() - unitSupportRollsMap.get(u).getInt(supportedUnit);
            final Tuple<Integer, Integer> strengthAndRollsWithoutSupport =
                Tuple.of(strengthAndRolls.getFirst(), rollsWithoutSupport);
            supportedUnitMap.put(supportedUnit, strengthAndRollsWithoutSupport);
            final int powerWithoutSupport = DiceRoll.getTotalPower(supportedUnitMap, data);
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
          final Tuple<Integer, Integer> strengthAndRolls = unitPowerAndRollsMap.get(supportedUnit);
          if (strengthAndRolls == null) {
            continue;
          }
          final int strengthWithoutSupport =
              strengthAndRolls.getFirst() - unitSupportPowerMapForUnit.getInt(supportedUnit);
          final Tuple<Integer, Integer> strengthAndRollsWithoutSupport =
              Tuple.of(strengthWithoutSupport, strengthAndRolls.getSecond());
          unitPowerAndRollsMap.put(supportedUnit, strengthAndRollsWithoutSupport);
          sortedUnitsList.remove(supportedUnit);
          sortedUnitsList.add(0, supportedUnit);
        }
      }
      final IntegerMap<Unit> unitSupportRollsMapForUnit = unitSupportRollsMap.get(worstUnit);
      if (unitSupportRollsMapForUnit != null) {
        for (final Unit supportedUnit : unitSupportRollsMapForUnit.keySet()) {
          final Tuple<Integer, Integer> strengthAndRolls = unitPowerAndRollsMap.get(supportedUnit);
          if (strengthAndRolls == null) {
            continue;
          }
          final int rollsWithoutSupport =
              strengthAndRolls.getSecond() - unitSupportRollsMapForUnit.getInt(supportedUnit);
          final Tuple<Integer, Integer> strengthAndRollsWithoutSupport =
              Tuple.of(strengthAndRolls.getFirst(), rollsWithoutSupport);
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
          player.getName()
              + "|"
              + battlesite.getName()
              + "|"
              + defending
              + "|"
              + amphibious
              + "|"
              + targetsHashCode
              + "|"
              + amphibHashCode;
      it.remove();
    }
    return sortedWellEnoughUnitsList;
  }

  /**
   * Checks if the given collections target are all of one category as defined by
   * UnitSeparator.categorize and they are not two hit units.
   *
   * @param targets a collection of target units
   * @param dependents map of depend units for target units
   */
  private static boolean allTargetsOneTypeOneHitPoint(
      final Collection<Unit> targets, final Map<Unit, Collection<Unit>> dependents) {
    final Set<UnitCategory> categorized =
        UnitSeparator.categorize(targets, dependents, false, false);
    if (categorized.size() == 1) {
      final UnitCategory unitCategory = categorized.iterator().next();
      return unitCategory.getHitPoints() - unitCategory.getDamaged() <= 1;
    }
    return false;
  }
}
