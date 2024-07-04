package games.strategy.triplea.delegate.data;

import static java.util.function.Predicate.not;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.util.UnitOwner;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A casualty list that also tracks whether or not casualties should be automatically calculated.
 */
public class CasualtyDetails extends CasualtyList {
  private static final long serialVersionUID = 2261683015991514918L;
  private final boolean autoCalculated;

  /**
   * Creates new CasualtyDetails.
   *
   * @param killed killed units
   * @param damaged damaged units (Can have multiple of the same unit, to show multiple hits to that
   *     unit.)
   * @param autoCalculated whether casualties should be selected automatically
   */
  public CasualtyDetails(
      final Collection<Unit> killed, final Collection<Unit> damaged, final boolean autoCalculated) {
    super(killed, damaged);
    this.autoCalculated = autoCalculated;
  }

  public CasualtyDetails(final CasualtyList casualties, final boolean autoCalculated) {
    super(
        (casualties == null ? null : casualties.getKilled()),
        (casualties == null ? null : casualties.getDamaged()));
    this.autoCalculated = autoCalculated;
  }

  public CasualtyDetails(final boolean autoCalculated) {
    this.autoCalculated = autoCalculated;
  }

  /** Empty details, with autoCalculated as true. */
  public CasualtyDetails() {
    autoCalculated = true;
  }

  public boolean getAutoCalculated() {
    return autoCalculated;
  }

  /**
   * replaces the units in <code>killed</code> that match the <code>matcher</code> by the same
   * number of units in <code>targets</code> that match the <code>matcher</code> and are first
   * according to <code>shouldBeKilledFirst</code>
   */
  public void ensureUnitsAreKilledFirst(
      final Collection<Unit> targets,
      final Predicate<Unit> matcher,
      final Comparator<Unit> shouldBeKilledFirst) {

    final Map<UnitOwner, List<Unit>> targetsGroupedByOwnerAndType =
        targets.stream().collect(Collectors.groupingBy(UnitOwner::new, Collectors.toList()));

    final List<Unit> killedWithCorrectOrder =
        ensureUnitsAreKilledFirst(
            shouldBeKilledFirst,
            targetsGroupedByOwnerAndType,
            getKilled().stream()
                .filter(matcher)
                .collect(Collectors.groupingBy(UnitOwner::new, Collectors.toList())));

    killed.addAll(
        killedWithCorrectOrder.stream()
            .filter(unit -> !killed.contains(unit))
            .collect(Collectors.toList()));
    killed.removeIf(matcher.and(not(killedWithCorrectOrder::contains)));
  }

  /**
   * sort all of the units of the same owner/type by the shouldBeKilledFirst and then take the top N
   * units (where N is the number of oldUnits)
   *
   * <p>Every key in oldUnitsGroupedByOwnerAndType should be in allUnitsGroupedByOwnerAndType and
   * the values of oldUnitsGroupedByOwnerAndType should be a subset of the values in
   * allUnitsGroupedByOwnerAndType
   */
  private List<Unit> ensureUnitsAreKilledFirst(
      final Comparator<Unit> shouldBeKilledFirst,
      final Map<UnitOwner, List<Unit>> allUnitsGroupedByOwnerAndType,
      final Map<UnitOwner, List<Unit>> oldUnitsGroupedByOwnerAndType) {

    return oldUnitsGroupedByOwnerAndType.entrySet().stream()
        .flatMap(
            entry ->
                allUnitsGroupedByOwnerAndType.get(entry.getKey()).stream()
                    .sorted(shouldBeKilledFirst)
                    .limit(entry.getValue().size()))
        .collect(Collectors.toList());
  }

  /**
   * replaces the entries in <code>damaged</code> that match the <code>matcher</code> by the same
   * number of entries in <code>targets</code> that match the <code>matcher</code> and are first
   * according to <code>shouldBeKilledFirst</code>
   */
  public void ensureUnitsAreDamagedFirst(
      final Collection<Unit> targets,
      final Predicate<Unit> matcher,
      final Comparator<Unit> shouldTakeHitsFirst) {
    final Map<UnitOwner, List<Unit>> targetsGroupedByOwnerAndType =
        targets.stream().collect(Collectors.groupingBy(UnitOwner::new, Collectors.toList()));

    final Map<UnitOwner, List<Unit>> oldTargetsToTakeHits =
        getDamaged().stream()
            .filter(matcher)
            .collect(Collectors.groupingBy(UnitOwner::new, Collectors.toList()));

    final List<Unit> targetsHitWithCorrectOrder = new ArrayList<>();
    for (final Map.Entry<UnitOwner, List<Unit>> oldTargetsOfOneOwnerAndType :
        oldTargetsToTakeHits.entrySet()) {
      final List<Unit> allTargetsOfOwnerAndTypeThatCanTakeHits =
          new ArrayList<>(targetsGroupedByOwnerAndType.get(oldTargetsOfOneOwnerAndType.getKey()));

      redistributeHits(
          oldTargetsOfOneOwnerAndType.getValue(),
          allTargetsOfOwnerAndTypeThatCanTakeHits,
          shouldTakeHitsFirst,
          targetsHitWithCorrectOrder);
      // Note: Although removeAll() removes all duplicates entries, but in this case it's not a
      // problem given how we're grouping things above.
      damaged.removeAll(oldTargetsOfOneOwnerAndType.getValue());
    }

    damaged.addAll(targetsHitWithCorrectOrder);
  }

  /**
   * redistributes the hits from <code>unitsWithHitsBeforeRedistribution</code> among <code>
   * unitsThatCanTakeHits</code> according to which units <code>shouldTakeHitsFirst</code>
   *
   * @param targetsWithHitsBeforeRedistribution contains a unit once for each hit it should take
   *     without considering which units should take hits first
   * @param targets are the units among which the hits should be distributed
   * @param shouldTakeHitsFirst determines which units should take hits first
   * @param targetsHitWithCorrectOrder collects units that correctly take hits, containing a unit
   *     once for each hit it should take
   */
  private static void redistributeHits(
      final List<Unit> targetsWithHitsBeforeRedistribution,
      final List<Unit> targets,
      final Comparator<Unit> shouldTakeHitsFirst,
      final List<Unit> targetsHitWithCorrectOrder) {
    targets.sort(shouldTakeHitsFirst);

    // have allTargetsOfOwnerAndTypeThatCanTakeHits in sort order
    // collect the hits that are currently distributed to oldTargetsOfOneOwnerAndType
    int hitsToRedistributeToUnit;
    final Iterator<Unit> unitIterator = targets.iterator();
    // Don't change how many units take 1 hit vs. 2 hits by counting how many hits each unit takes
    // and re-assigning at that level.
    final Iterator<Long> numberOfHitsPerUnitIterator =
        targetsWithHitsBeforeRedistribution.stream()
            .collect(Collectors.groupingBy(e -> e, Collectors.counting()))
            .values()
            .stream()
            .sorted(Comparator.reverseOrder()) // Sort in descending order
            .collect(Collectors.toList())
            .iterator();
    int hitsToRedistribute = 0;
    while (numberOfHitsPerUnitIterator.hasNext() || hitsToRedistribute > 0) {
      if (numberOfHitsPerUnitIterator.hasNext()) {
        hitsToRedistribute += numberOfHitsPerUnitIterator.next();
      }

      final Unit unit = unitIterator.next();
      hitsToRedistributeToUnit =
          Math.min(unit.hitsUnitCanTakeHitWithoutBeingKilled(), hitsToRedistribute);
      // Note: Since the above may result in fewer hits assigned, keep track of the remainder.
      hitsToRedistribute -= hitsToRedistributeToUnit;

      for (int i = 0; i < hitsToRedistributeToUnit; ++i) {
        targetsHitWithCorrectOrder.add(unit);
      }
    }
  }

  /**
   * Ensure that any killed or damaged units have no better marine effect than others of the same
   * type
   *
   * @param units should be a superset of the union of killed and damaged.
   */
  public void ensureUnitsWithPositiveMarineBonusAreKilledLast(final Collection<Unit> units) {
    final Predicate<Unit> isMarine = unit -> unit.getUnitAttachment().getIsMarine() != 0;

    final Comparator<Unit> positiveMarineEffectFirstNegativeMarineEffectLast =
        (Unit unit1, Unit unit2) -> {
          // wasAmphibious marines should be removed last if marine bonus is positive, otherwise
          // should be removed first
          if (unit1.getUnitAttachment().getIsMarine() > 0) {
            return Boolean.compare(unit1.getWasAmphibious(), unit2.getWasAmphibious());
          } else {
            return Boolean.compare(unit2.getWasAmphibious(), unit1.getWasAmphibious());
          }
        };

    ensureUnitsAreKilledFirst(units, isMarine, positiveMarineEffectFirstNegativeMarineEffectLast);
  }
}
