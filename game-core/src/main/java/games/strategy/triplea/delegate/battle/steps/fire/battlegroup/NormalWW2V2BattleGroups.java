package games.strategy.triplea.delegate.battle.steps.fire.battlegroup;

import static games.strategy.triplea.delegate.battle.BattleState.Side.DEFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.FIRST_STRIKE_UNITS;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.UNITS;

import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.triplea.java.PredicateBuilder;

@UtilityClass
class NormalWW2V2BattleGroups {

  /**
   * Creates battle groups for the WW2V2 first strike phase
   *
   * <p>In WW2V2, isFirstStrike always fire as a separate battle group. If a side has an
   * isDestroyer, then its casualties can return fire.
   */
  static BattleGroup createFirstStrike(
      final Collection<UnitType> unitTypes, final GameProperties properties) {

    final List<FiringSquadron> firingSquadrons =
        FiringSquadron.createWithTargetInformation(unitTypes).stream()
            .map(
                firingSquadron -> {
                  return createFiringSquadron(firingSquadron, unitIsFirstStrike(properties))
                      .name(FIRST_STRIKE_UNITS)
                      .build();
                })
            .collect(Collectors.toList());

    return BattleGroup.builder()
        .offenseSquadrons(firingSquadrons)
        .defenseSquadrons(firingSquadrons)
        .fireType(BattleState.FireType.NORMAL)
        .casualtiesOnOffenseReturnFirePredicate(destroyerPresent())
        .casualtiesOnDefenseReturnFirePredicate(destroyerPresent())
        .build();
  }

  private FiringSquadron.FiringSquadronBuilder createFiringSquadron(
      final FiringSquadron firingSquadron,
      final Predicate<FiringSquadron.FiringUnitFilterData> firingUnitPredicate) {
    return firingSquadron.toBuilder()
        .firingUnits(firingSquadron.getFiringUnits().and(firingUnitPredicate))
        .targetUnits(
            firingSquadron
                .getTargetUnits()
                .and(FiringSquadron.filterOutSuicideUnits())
                .and(
                    targetUnitFilterData ->
                        Matches.unitIsNotInfrastructure()
                            .test(targetUnitFilterData.getTargetUnit())));
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

  private Predicate<Collection<Unit>> destroyerPresent() {
    return units -> units.stream().anyMatch(Matches.unitIsDestroyer());
  }

  /**
   * Creates battle groups for the WW2V2 general phase
   *
   * <p>In WW2V2, isFirstStrike always fire as a separate battle group.
   */
  static BattleGroup createGeneral(
      final Collection<UnitType> unitTypes, final GameProperties properties) {

    final List<FiringSquadron> firingSquadrons =
        FiringSquadron.createWithTargetInformation(unitTypes).stream()
            .map(
                firingSquadron -> {
                  return createFiringSquadron(
                          firingSquadron, Predicate.not(unitIsFirstStrike(properties)))
                      .name(UNITS)
                      .build();
                })
            .collect(Collectors.toList());

    final BattleGroup.BattleGroupBuilder battleGroupBuilder =
        BattleGroup.builder()
            .offenseSquadrons(firingSquadrons)
            .defenseSquadrons(firingSquadrons)
            .fireType(BattleState.FireType.NORMAL);

    return battleGroupBuilder.build();
  }
}
