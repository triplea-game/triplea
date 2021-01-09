package games.strategy.triplea.delegate.battle.steps.fire.battlegroup;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.triplea.java.PredicateBuilder;

/** Holds predicates for finding units in a squadron and what they target */
@Value
@Builder(toBuilder = true)
public class FiringSquadron {
  /** Name that will be displayed in the Battle UI */
  @NonNull String name;
  /** Matches the units that will be firing */
  Predicate<FiringUnitFilterData> firingUnits;
  /** Matches the units that are targeted */
  Predicate<TargetUnitFilterData> targetUnits;
  /** This FiringSquadron can only fire if the friendly unit requirements are met */
  Predicate<Collection<Unit>> friendlyUnitRequirements;
  /** This FiringSquadron can only fire if the enemy unit requirements are met */
  Predicate<Collection<Unit>> enemyUnitRequirements;
  /** This FiringSquadron can only fire if the battle state requirements are met */
  Predicate<BattleState> battleStateRequirements;

  public Predicate<FiringUnitFilterData> getFiringUnits() {
    // ensure that transported units can not fire
    return firingUnits.and(firingUnitFilterData -> notCargo(firingUnitFilterData.getFiringUnit()));
  }

  private boolean notCargo(final Unit unit) {
    return unit.getTransportedBy() == null;
  }

  public Predicate<TargetUnitFilterData> getTargetUnits() {
    // ensure that transported units can not be targeted
    return targetUnits.and(targetUnitFilterData -> notCargo(targetUnitFilterData.getTargetUnit()));
  }

  @Value
  @Builder
  public static class FiringUnitFilterData {
    @NonNull Unit firingUnit;
    @NonNull BattleState.Side side;
    @NonNull Collection<Unit> enemyUnits;
    @NonNull BattleState.BattleStatus battleStatus;
  }

  @Value
  @Builder
  public static class TargetUnitFilterData {
    @NonNull Unit targetUnit;
    @NonNull BattleState.Side side;
    @NonNull Collection<Unit> friendlyUnits;
    @NonNull Territory battleSite;
  }

  /**
   * Override the Lombok created Builder so that default lambda values can be set and to add the
   * firingUnitsAnd method.
   */
  public static class FiringSquadronBuilder {
    private Predicate<FiringUnitFilterData> firingUnits = k -> true;
    private Predicate<TargetUnitFilterData> targetUnits = k -> true;
    private Predicate<Collection<Unit>> friendlyUnitRequirements = k -> true;
    private Predicate<Collection<Unit>> enemyUnitRequirements = k -> true;
    private Predicate<BattleState> battleStateRequirements = k -> true;

    /** Adds additional predicates to the firingUnits predicate. */
    public FiringSquadronBuilder firingUnitsAnd(final Predicate<FiringUnitFilterData> firingUnits) {
      this.firingUnits = this.firingUnits.and(firingUnits);
      return this;
    }
  }

  /**
   * Creates a list of FiringSquadrons using canNotTarget and canNotBeTargetedBy
   *
   * <p>canNotTarget is used to indicate which unit types can't be hit by the unit type.
   * canNotBeTargetedBy is used to indicate which unit types can't hit the unit type unless an
   * isDestroyer is present.
   *
   * <p>If unit types have identical possible target unit types, then those will be grouped together
   * in the same firing squadrons.
   */
  public static List<FiringSquadron> createWithTargetInformation(
      final Collection<UnitType> unitTypes) {

    final Map<UnitType, TargetInformationWithUnitType> unitTypeTargetData = new HashMap<>();

    // get the canNotTarget information for all unitTypes
    for (final UnitType unitType : unitTypes) {
      unitTypeTargetData.put(
          unitType,
          new TargetInformationWithUnitType(
              unitType,
              new TargetInformation(
                  UnitAttachment.get(unitType).getCanNotTarget(), new ArrayList<>())));
    }

    // add the canNotBeTargetedBy information
    for (final UnitType unitType : unitTypes) {
      final UnitAttachment unitAttachment = UnitAttachment.get(unitType);
      if (unitAttachment.getCanNotBeTargetedBy().isEmpty()) {
        continue;
      }
      unitAttachment
          .getCanNotBeTargetedBy()
          .forEach(
              unitTypeThatCanNotTarget ->
                  unitTypeTargetData
                      .get(unitTypeThatCanNotTarget)
                      .targetInformation
                      .unitTypesThatCanNotBeTargetedUnlessDestroyer
                      .add(unitType));
    }

    // group unit types that have the same target information
    final Map<TargetInformation, GroupedTargetInformation> unitTypeTargetDataGroupedByTargets =
        unitTypeTargetData.values().stream()
            .collect(
                Collectors.groupingBy(
                    TargetInformationWithUnitType::getTargetInformation,
                    Collectors.reducing(
                        new GroupedTargetInformation(),
                        GroupedTargetInformation::new,
                        GroupedTargetInformation::merge)));

    // create FiringSquadrons for each of the grouped unit types
    return unitTypeTargetDataGroupedByTargets.values().stream()
        .map(
            groupedTargetInformation ->
                FiringSquadron.builder()
                    .firingUnits(groupedTargetInformation::containsUnitType)
                    .targetUnits(
                        filterOutNonTargets()
                            .and(groupedTargetInformation.targetInformation::canTarget))
                    .build())
        .collect(Collectors.toList());
  }

  @Value
  private static class TargetInformationWithUnitType {
    UnitType firingUnitType;
    TargetInformation targetInformation;
  }

  @Value
  private static class TargetInformation {
    Collection<UnitType> unitTypesThatCanNotBeTargeted;
    Collection<UnitType> unitTypesThatCanNotBeTargetedUnlessDestroyer;

    /**
     * Determines if the possible target unit is in this target information
     *
     * <p>If an isDestroyer is present, then it will also test the
     * unitTypesThatCanNotBeTargetedUnlessDestroyer.
     */
    boolean canTarget(final TargetUnitFilterData targetUnitFilterData) {
      final Collection<UnitType> allTargets = new ArrayList<>(unitTypesThatCanNotBeTargeted);
      if (targetUnitFilterData.getFriendlyUnits().stream().anyMatch(Matches.unitIsDestroyer())) {
        allTargets.addAll(unitTypesThatCanNotBeTargetedUnlessDestroyer);
      }
      return allTargets.contains(targetUnitFilterData.getTargetUnit().getType());
    }
  }

  @Value
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  private static class GroupedTargetInformation {
    Collection<UnitType> firingUnitTypes;
    TargetInformation targetInformation;

    GroupedTargetInformation() {
      this.firingUnitTypes = new ArrayList<>();
      this.targetInformation = new TargetInformation(List.of(), List.of());
    }

    GroupedTargetInformation(final TargetInformationWithUnitType initial) {
      this.firingUnitTypes = List.of(initial.getFiringUnitType());
      this.targetInformation = initial.getTargetInformation();
    }

    GroupedTargetInformation merge(final GroupedTargetInformation other) {
      final Collection<UnitType> firingUnitTypes = new ArrayList<>(this.firingUnitTypes);
      firingUnitTypes.addAll(other.firingUnitTypes);
      return new GroupedTargetInformation(firingUnitTypes, this.targetInformation);
    }

    boolean containsUnitType(final FiringUnitFilterData firingUnitFilterData) {
      return firingUnitTypes.contains(firingUnitFilterData.getFiringUnit().getType());
    }
  }

  /**
   * Builds a predicate that can be used for targetUnits if suicide units and infrastructure need to
   * be ignored
   */
  static Predicate<TargetUnitFilterData> filterOutNonTargets() {
    return targetUnitFilterData ->
        PredicateBuilder.<Unit>trueBuilder()
            .andIf(
                targetUnitFilterData.side == BattleState.Side.OFFENSE,
                Matches.unitIsSuicideOnDefense().negate())
            .andIf(
                targetUnitFilterData.side == BattleState.Side.DEFENSE,
                Matches.unitIsSuicideOnAttack().negate())
            .and(Matches.unitIsNotInfrastructure())
            .build()
            .test(targetUnitFilterData.getTargetUnit());
  }
}
