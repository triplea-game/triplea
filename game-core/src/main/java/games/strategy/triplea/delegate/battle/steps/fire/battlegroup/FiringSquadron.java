package games.strategy.triplea.delegate.battle.steps.fire.battlegroup;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
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
  @Builder.Default
  Predicate<FiringUnitFilterData> firingUnits = FiringSquadron::defaultFiringUnitPredicate;
  /** Matches the units that are targeted */
  @Builder.Default
  Predicate<TargetUnitFilterData> targetUnits = FiringSquadron::defaultTargetUnitPredicate;
  /** This FiringSquadron can only fire if the friendly unit requirements are met */
  @Builder.Default
  Predicate<Collection<Unit>> friendlyUnitRequirements = FiringSquadron::defaultUnitsRequirements;
  /** This FiringSquadron can only fire if the enemy unit requirements are met */
  @Builder.Default
  Predicate<Collection<Unit>> enemyUnitRequirements = FiringSquadron::defaultUnitsRequirements;
  /** This FiringSquadron can only fire if the battle state requirements are met */
  @Builder.Default
  Predicate<BattleState> battleStateRequirements = FiringSquadron::defaultBattleStateRequirements;

  /** ErrorProne static analysis doesn't like lambda style @Builder.Default values */
  @SuppressWarnings("unused")
  private static boolean defaultFiringUnitPredicate(final FiringUnitFilterData unused) {
    return true;
  }

  /** ErrorProne static analysis doesn't like lambda style @Builder.Default values */
  @SuppressWarnings("unused")
  private static boolean defaultTargetUnitPredicate(final TargetUnitFilterData unused) {
    return true;
  }

  /** ErrorProne static analysis doesn't like lambda style @Builder.Default values */
  @SuppressWarnings("unused")
  private static boolean defaultUnitsRequirements(final Collection<Unit> unused) {
    return true;
  }

  /** ErrorProne static analysis doesn't like lambda style @Builder.Default values */
  @SuppressWarnings("unused")
  private static boolean defaultBattleStateRequirements(final BattleState unused) {
    return true;
  }

  Predicate<FiringUnitFilterData> getFiringUnits() {
    // ensure that transported units can not fire
    return firingUnits.and(
        firingUnitFilterData ->
            Matches.unitIsBeingTransported().negate().test(firingUnitFilterData.getFiringUnit()));
  }

  Predicate<TargetUnitFilterData> getTargetUnits() {
    // ensure that transported units can not be targeted
    return targetUnits.and(
        targetUnitFilterData ->
            Predicate.not(Matches.unitIsBeingTransported())
                .test(targetUnitFilterData.getTargetUnit()));
  }

  /** Builds a predicate that can be used for targetUnits if suicide units need to be ignored */
  static Predicate<TargetUnitFilterData> filterOutSuicideUnits() {
    return targetUnitFilterData ->
        PredicateBuilder.<Unit>trueBuilder()
            .andIf(
                targetUnitFilterData.side == BattleState.Side.OFFENSE,
                Matches.unitIsSuicideOnDefense().negate())
            .andIf(
                targetUnitFilterData.side == BattleState.Side.DEFENSE,
                Matches.unitIsSuicideOnAttack().negate())
            .build()
            .test(targetUnitFilterData.getTargetUnit());
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
        .sorted(
            Comparator.comparingInt(
                groupedTargetInformation -> groupedTargetInformation.targetInformation.size()))
        .map(
            groupedTargetInformation ->
                FiringSquadron.builder()
                    .firingUnits(
                        firingUnitFilterData ->
                            groupedTargetInformation
                                .getFiringUnitTypes()
                                .contains(firingUnitFilterData.getFiringUnit().getType()))
                    .targetUnits(groupedTargetInformation.targetInformation::canTarget)
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

    int size() {
      return unitTypesThatCanNotBeTargeted.size()
          + unitTypesThatCanNotBeTargetedUnlessDestroyer.size();
    }

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
  }
}
