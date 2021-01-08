package games.strategy.triplea.delegate.battle.steps.fire.battlegroup;

import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.UnitTypeList;
import games.strategy.triplea.attachments.TechAbilityAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TechTracker;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.triplea.java.PredicateBuilder;

/** Constructs battle groups for the AA phase of a battle */
@UtilityClass
class AaBattleGroups {
  static BattleGroup create(
      final UnitTypeList unitTypeList, final TechnologyFrontier technologyFrontier) {
    final List<String> typeAasBySuicideOnHit =
        unitTypeList.stream()
            .map(unitType -> UnitAttachment.get(unitType).getTypeAa())
            .filter(Predicate.not(String::isBlank))
            .collect(Collectors.toList());

    final Collection<FiringSquadron> offenseAa =
        typeAasBySuicideOnHit.stream()
            .map(
                typeAa ->
                    createFiringSquadron(
                        BattleState.Side.OFFENSE, typeAa, unitTypeList, technologyFrontier))
            .collect(Collectors.toList());
    final Collection<FiringSquadron> defenseAa =
        typeAasBySuicideOnHit.stream()
            .map(
                typeAa ->
                    createFiringSquadron(
                        BattleState.Side.DEFENSE, typeAa, unitTypeList, technologyFrontier))
            .collect(Collectors.toList());

    return BattleGroup.builder()
        .fireType(BattleState.FireType.AA)
        .casualtiesOnOffenseReturnFire(false)
        .casualtiesOnDefenseReturnFire(false)
        .offenseSquadrons(offenseAa)
        .defenseSquadrons(defenseAa)
        .build();
  }

  private static FiringSquadron createFiringSquadron(
      final BattleState.Side side,
      final String typeAa,
      final UnitTypeList unitTypeList,
      final TechnologyFrontier technologyFrontier) {
    return FiringSquadron.builder()
        .name(typeAa)
        .firingUnits(predicateForFiringUnits(side, typeAa))
        .targetUnits(predicateForTargetUnits(side, typeAa, unitTypeList, technologyFrontier))
        .build();
  }

  /** Predicate to get the units that has AA fire on the requested side for the requested typeAa */
  private static Predicate<FiringSquadron.FiringUnitFilterData> predicateForFiringUnits(
      final BattleState.Side side, final String typeAa) {
    return firingUnitFilterData ->
        PredicateBuilder.of(Matches.unitIsAaForCombatOnly())
            .and(unit -> unit.getUnitAttachment().getTypeAa().equals(typeAa))
            .and(
                Matches.unitIsAaThatCanFireOnRound(
                    firingUnitFilterData.getBattleStatus().getRound()))
            .andIf(
                side == BattleState.Side.DEFENSE,
                Matches.unitAttackAaIsGreaterThanZeroAndMaxAaAttacksIsNotZero())
            .andIf(
                side == BattleState.Side.OFFENSE,
                Matches.unitOffensiveAttackAaIsGreaterThanZeroAndMaxAaAttacksIsNotZero())
            .and(enemyUnitsThatPreventFiringAreNotPresent(firingUnitFilterData.getEnemyUnits()))
            .build()
            .test(firingUnitFilterData.getFiringUnit());
  }

  private static Predicate<Unit> enemyUnitsThatPreventFiringAreNotPresent(
      final Collection<Unit> enemyUnits) {
    return unit ->
        enemyUnits.stream()
            .map(Unit::getType)
            .noneMatch(
                unitType -> unit.getUnitAttachment().getWillNotFireIfPresent().contains(unitType));
  }

  private static Predicate<FiringSquadron.TargetUnitFilterData> predicateForTargetUnits(
      final BattleState.Side side,
      final String typeAa,
      final UnitTypeList unitTypeList,
      final TechnologyFrontier technologyFrontier) {
    return targetUnitFilterData -> {
      // AA that fires together should all have the same targetsAa
      final Set<UnitType> targetTypes =
          targetUnitFilterData.getFriendlyUnits().stream()
              .map(Unit::getUnitAttachment)
              .map(unitAttachment -> unitAttachment.getTargetsAa(unitTypeList))
              .findFirst()
              .orElse(Set.of());

      final Map<String, Set<UnitType>> airborneTechTargetsAllowed =
          side == BattleState.Side.DEFENSE
              ? TechAbilityAttachment.getAirborneTargettedByAa(
                  TechTracker.getCurrentTechAdvances(
                      targetUnitFilterData.getTargetUnit().getOwner(), technologyFrontier))
              : Map.of();

      // airborneForces technology allows land units to "fly" from the airborneBases.
      // these units are only specified in the airborneTargettedByAA option
      return PredicateBuilder.of(Matches.unitIsOfTypes(targetTypes))
          .or(
              Matches.unitIsAirborne()
                  .and(Matches.unitIsOfTypes(airborneTechTargetsAllowed.get(typeAa))))
          .build()
          .test(targetUnitFilterData.getTargetUnit());
    };
  }
}
