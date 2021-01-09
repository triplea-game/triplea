package games.strategy.triplea.delegate.battle.steps.fire.battlegroup;

import static games.strategy.triplea.delegate.battle.BattleStepStrings.NAVAL_BOMBARD;

import games.strategy.engine.data.properties.GameProperties;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.List;
import java.util.function.Predicate;
import lombok.experimental.UtilityClass;

/** Constructs battle groups for the bombardment phase of a battle */
@UtilityClass
class BombardmentBattleGroups {
  static BattleGroup create(final GameProperties properties) {

    final FiringSquadron bombardment =
        FiringSquadron.builder()
            .name(NAVAL_BOMBARD)
            .firingUnits(unitCanBombard())
            .battleStateRequirements(battleCanHaveBombardment())
            .targetUnits(
                FiringSquadron.filterOutNonTargets()
                    .and(unitIsNotInfrastructureAndNotCapturedOnEntering(properties)))
            .build();

    return BattleGroup.builder()
        .offenseSquadrons(List.of(bombardment))
        .fireType(BattleState.FireType.BOMBARDMENT)
        .casualtiesOnDefenseReturnFire(!Properties.getNavalBombardCasualtiesReturnFire(properties))
        .build();
  }

  private Predicate<FiringSquadron.FiringUnitFilterData> unitCanBombard() {
    return firingUnitFilterData ->
        firingUnitFilterData
            .getFiringUnit()
            .getUnitAttachment()
            .getCanBombard(firingUnitFilterData.getFiringUnit().getOwner());
  }

  /**
   * Bombardment only happens on the first round, in a land battle, and there needs to be bombarding
   * units
   */
  private Predicate<BattleState> battleCanHaveBombardment() {
    return battleState ->
        battleState.getStatus().isFirstRound()
            && !battleState.getBattleSite().isWater()
            && !battleState.getBombardingUnits().isEmpty();
  }

  private Predicate<FiringSquadron.TargetUnitFilterData>
      unitIsNotInfrastructureAndNotCapturedOnEntering(final GameProperties properties) {
    return targetUnitFilterData ->
        Matches.unitIsNotInfrastructureAndNotCapturedOnEntering(
                targetUnitFilterData.getTargetUnit().getOwner(),
                targetUnitFilterData.getBattleSite(),
                properties)
            .test(targetUnitFilterData.getTargetUnit());
  }
}
