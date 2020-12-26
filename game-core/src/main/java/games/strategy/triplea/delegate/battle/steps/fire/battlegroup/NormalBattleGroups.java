package games.strategy.triplea.delegate.battle.steps.fire.battlegroup;

import static games.strategy.triplea.delegate.battle.BattleState.Side.DEFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.FIRST_STRIKE_UNITS;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.UNITS;

import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;
import org.triplea.java.PredicateBuilder;

/**
 * Creates battle groups for units that use attack/defense attributes.
 *
 * <p>There is a FirstStrike battle group and a General battle group.
 */
@UtilityClass
class NormalBattleGroups {

  /**
   * Creates battle groups for the first strike phase
   *
   * <p>If an opposing isDestroyer unit is present, then the phase for that side will not occur (see
   * {@link #noDestroyerPresent} for both firing squadrons)
   */
  static BattleGroup createFirstStrike(
      final Collection<UnitType> unitTypes, final GameProperties properties) {

    final List<FiringSquadron> firingSquadrons =
        FiringSquadron.createWithTargetInformation(unitTypes).stream()
            .map(
                firingSquadron -> {
                  return createFiringSquadron(
                          firingSquadron, unitIsFirstStrike(properties), noDestroyerPresent())
                      .name(FIRST_STRIKE_UNITS)
                      .build();
                })
            .collect(Collectors.toList());

    final BattleGroup.BattleGroupBuilder battleGroupBuilder =
        BattleGroup.builder()
            .offenseSquadrons(firingSquadrons)
            .fireType(BattleState.FireType.NORMAL)
            .casualtiesOnDefenseReturnFire(false);

    if (Properties.getDefendingSubsSneakAttack(properties)) {
      battleGroupBuilder.defenseSquadrons(firingSquadrons).casualtiesOnOffenseReturnFire(false);
    }
    return battleGroupBuilder.build();
  }

  private FiringSquadron.FiringSquadronBuilder createFiringSquadron(
      final FiringSquadron firingSquadron,
      final Predicate<FiringSquadron.FiringUnitFilterData> firingUnitPredicate,
      final Predicate<Collection<Unit>> enemyUnitPredicate) {
    return firingSquadron.toBuilder()
        .firingUnits(firingSquadron.getFiringUnits().and(firingUnitPredicate))
        .targetUnits(
            firingSquadron
                .getTargetUnits()
                .and(FiringSquadron.filterOutSuicideUnits())
                .and(
                    targetUnitFilterData ->
                        Matches.unitIsNotInfrastructure()
                            .test(targetUnitFilterData.getTargetUnit())))
        .enemyUnitRequirements(enemyUnitPredicate);
  }

  private Predicate<FiringSquadron.FiringUnitFilterData> unitIsFirstStrike(
      final GameProperties properties) {
    return firingUnitFilterData ->
        PredicateBuilder.<Unit>trueBuilder()
            .andIf(firingUnitFilterData.getSide() == OFFENSE, Matches.unitIsFirstStrike())
            .andIf(
                firingUnitFilterData.getSide() == DEFENSE,
                Matches.unitIsFirstStrikeOnDefense(properties))
            .build()
            .test(firingUnitFilterData.getFiringUnit());
  }

  private Predicate<Collection<Unit>> noDestroyerPresent() {
    return units -> units.stream().noneMatch(Matches.unitIsDestroyer());
  }

  /**
   * Creates battle groups for the general phase
   *
   * <p>Since some units may fire during the first strike phase depending on the presence of an
   * isDestroyer, this creates two sets of FiringSquadrons. One set includes all units but only
   * fires if there is an isDestroyer, the other set only includes non isFirstStrike units but only
   * fires if there is not an isDestroyer.
   */
  static BattleGroup createGeneral(
      final Collection<UnitType> unitTypes, final GameProperties properties) {

    final List<FiringSquadron> firingSquadronsWithoutFirstStrikeUnits =
        FiringSquadron.createWithTargetInformation(unitTypes).stream()
            .map(
                firingSquadron -> {
                  return createFiringSquadron(
                          firingSquadron,
                          Predicate.not(unitIsFirstStrike(properties)),
                          noDestroyerPresent())
                      .name(UNITS)
                      .build();
                })
            .collect(Collectors.toList());

    final List<FiringSquadron> firingSquadronsWithFirstStrikeUnits =
        FiringSquadron.createWithTargetInformation(unitTypes).stream()
            .map(
                firingSquadron -> {
                  return createFiringSquadron(
                          firingSquadron,
                          firingUnitFilterData -> true,
                          Predicate.not(noDestroyerPresent()))
                      .name(UNITS)
                      .build();
                })
            .collect(Collectors.toList());

    final List<FiringSquadron> offenseSquadrons =
        Stream.of(firingSquadronsWithFirstStrikeUnits, firingSquadronsWithoutFirstStrikeUnits)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

    final List<FiringSquadron> defenseSquadrons;
    if (Properties.getDefendingSubsSneakAttack(properties)) {
      defenseSquadrons =
          Stream.of(firingSquadronsWithFirstStrikeUnits, firingSquadronsWithoutFirstStrikeUnits)
              .flatMap(Collection::stream)
              .collect(Collectors.toList());
    } else {
      defenseSquadrons =
          FiringSquadron.createWithTargetInformation(unitTypes).stream()
              .map(
                  firingSquadron -> {
                    return createFiringSquadron(
                            firingSquadron, firingUnitFilterData -> true, units -> true)
                        .name(UNITS)
                        .build();
                  })
              .collect(Collectors.toList());
    }

    final BattleGroup.BattleGroupBuilder battleGroupBuilder =
        BattleGroup.builder()
            .offenseSquadrons(offenseSquadrons)
            .defenseSquadrons(defenseSquadrons)
            .fireType(BattleState.FireType.NORMAL);

    return battleGroupBuilder.build();
  }
}
