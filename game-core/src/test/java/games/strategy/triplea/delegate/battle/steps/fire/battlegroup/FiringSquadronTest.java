package games.strategy.triplea.delegate.battle.steps.fire.battlegroup;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FiringSquadronTest {

  private final FiringSquadron.FiringUnitFilterData firingUnitFilterData =
      FiringSquadron.FiringUnitFilterData.builder()
          .firingUnit(mock(Unit.class))
          .side(BattleState.Side.OFFENSE)
          .enemyUnits(List.of())
          .battleStatus(BattleState.BattleStatus.of(1, 10, false, false, false))
          .build();

  private final FiringSquadron.TargetUnitFilterData targetUnitFilterData =
      FiringSquadron.TargetUnitFilterData.builder()
          .targetUnit(mock(Unit.class))
          .side(BattleState.Side.OFFENSE)
          .friendlyUnits(List.of())
          .battleSite(mock(Territory.class))
          .build();

  @Nested
  class Builder {
    @Test
    void defaultFiringUnitsPredicate() {
      final FiringSquadron firingSquadron = FiringSquadron.builder().name("test").build();

      assertThat(
          "The default getFiringUnits should return true",
          firingSquadron.getFiringUnits().test(firingUnitFilterData),
          is(true));
    }

    @Test
    void firingUnitsAndMethodHelper() {
      final FiringSquadron firingSquadron =
          FiringSquadron.builder()
              .name("test")
              .firingUnitsAnd(firingUnitFilterData -> false)
              .build();

      assertThat(
          "firingUnitsAnd should cause the result to be false",
          firingSquadron.getFiringUnits().test(firingUnitFilterData),
          is(false));
    }

    @Nested
    class MultipleAndedFiringUnits {

      @Test
      void twoAndedClauseThatAreTrue() {
        final FiringSquadron firingSquadron =
            FiringSquadron.builder()
                .name("test")
                .firingUnitsAnd(
                    firingUnitFilterData ->
                        firingUnitFilterData.getSide() == BattleState.Side.OFFENSE)
                .firingUnitsAnd(
                    firingUnitFilterData -> firingUnitFilterData.getBattleStatus().isFirstRound())
                .build();

        assertThat(
            "Both of the And clauses are true, so the result is true",
            firingSquadron
                .getFiringUnits()
                .test(
                    FiringSquadron.FiringUnitFilterData.builder()
                        .firingUnit(mock(Unit.class))
                        .enemyUnits(List.of())
                        .side(BattleState.Side.OFFENSE)
                        .battleStatus(BattleState.BattleStatus.of(1, 10, false, false, false))
                        .build()),
            is(true));
      }

      @Test
      void oneTrueClauseAndOneFalseClause() {
        final FiringSquadron firingSquadron =
            FiringSquadron.builder()
                .name("test")
                .firingUnitsAnd(
                    firingUnitFilterData ->
                        firingUnitFilterData.getSide() == BattleState.Side.OFFENSE)
                .firingUnitsAnd(
                    firingUnitFilterData -> firingUnitFilterData.getBattleStatus().isFirstRound())
                .build();

        assertThat(
            "One of the clauses is false, so the result is false",
            firingSquadron
                .getFiringUnits()
                .test(
                    FiringSquadron.FiringUnitFilterData.builder()
                        .firingUnit(mock(Unit.class))
                        .enemyUnits(List.of())
                        .side(BattleState.Side.DEFENSE)
                        .battleStatus(BattleState.BattleStatus.of(1, 10, false, false, false))
                        .build()),
            is(false));
      }
    }

    @Test
    void defaultTargetUnitsPredicate() {
      final FiringSquadron firingSquadron = FiringSquadron.builder().name("test").build();

      assertThat(
          "The default getTargetUnits should return true",
          firingSquadron.getTargetUnits().test(targetUnitFilterData),
          is(true));
    }

    @Test
    void defaultFriendlyUnitRequirements() {
      final FiringSquadron firingSquadron = FiringSquadron.builder().name("test").build();

      assertThat(
          "The default getFriendlyUnitRequirements should return true",
          firingSquadron.getFriendlyUnitRequirements().test(List.of()),
          is(true));
    }

    @Test
    void defaultEnemyUnitRequirements() {
      final FiringSquadron firingSquadron = FiringSquadron.builder().name("test").build();

      assertThat(
          "The default getEnemyUnitRequirements should return true",
          firingSquadron.getEnemyUnitRequirements().test(List.of()),
          is(true));
    }

    @Test
    void defaultBattleStateRequirements() {
      final FiringSquadron firingSquadron = FiringSquadron.builder().name("test").build();

      assertThat(
          "The default getBattleStateRequirements should return true",
          firingSquadron.getBattleStateRequirements().test(mock(BattleState.class)),
          is(true));
    }
  }
}
