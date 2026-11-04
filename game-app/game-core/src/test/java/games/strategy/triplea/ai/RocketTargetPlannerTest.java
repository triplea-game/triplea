package games.strategy.triplea.ai;

import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.Map;
import java.util.Set;
import java.util.function.ToIntFunction;
import org.junit.jupiter.api.Test;

class RocketTargetPlannerTest {
  private final GameData gameData = TestMapGameData.REVISED.getGameData();
  private final Territory launcherA = territory("1 Sea Zone", gameData);
  private final Territory launcherB = territory("2 Sea Zone", gameData);
  private final Territory launcherC = territory("3 Sea Zone", gameData);
  private final Territory launcherD = territory("4 Sea Zone", gameData);
  private final Territory targetW = territory("5 Sea Zone", gameData);
  private final Territory targetX = territory("6 Sea Zone", gameData);
  private final Territory targetY = territory("7 Sea Zone", gameData);
  private final Territory targetZ = territory("8 Sea Zone", gameData);

  private final ToIntFunction<Territory> sameProduction = t -> 1;

  @Test
  void launcherSharingATargetLeavesItToTheLauncherWithNoOtherOption() {
    final var plan =
        RocketTargetPlanner.plan(
            Map.of(
                launcherA, Set.of(targetX, targetY),
                launcherB, Set.of(targetX)),
            1,
            t -> t.equals(targetX) ? 10 : 1);

    assertThat(plan).containsEntry(launcherA, targetY).containsEntry(launcherB, targetX);
  }

  @Test
  void idleLauncherTakesATargetWhoseHolderCanMoveToAnUnhitOne() {
    // Greedy leaves C idle: A->z, B->y (richer than x), D->w. Moving B to x frees y for C.
    final var plan =
        RocketTargetPlanner.plan(
            Map.of(
                launcherA, Set.of(targetZ),
                launcherB, Set.of(targetX, targetY),
                launcherC, Set.of(targetZ, targetY),
                launcherD, Set.of(targetW, targetX)),
            1,
            t -> t.equals(targetY) ? 3 : 1);

    assertThat(plan)
        .containsEntry(launcherA, targetZ)
        .containsEntry(launcherB, targetX)
        .containsEntry(launcherC, targetY)
        .containsEntry(launcherD, targetW);
  }

  @Test
  void prefersHigherProductionAmongEqualCoverage() {
    final var plan =
        RocketTargetPlanner.plan(
            Map.of(launcherA, Set.of(targetW, targetX)), 1, t -> t.equals(targetX) ? 5 : 2);

    assertThat(plan).containsEntry(launcherA, targetX);
  }

  @Test
  void breaksEqualProductionByTerritoryName() {
    final var plan =
        RocketTargetPlanner.plan(Map.of(launcherA, Set.of(targetX, targetW)), 1, sameProduction);

    assertThat(plan).containsEntry(launcherA, targetW);
  }

  @Test
  void capOfOneLeavesSecondLauncherIdle() {
    final var plan =
        RocketTargetPlanner.plan(
            Map.of(launcherA, Set.of(targetX), launcherB, Set.of(targetX)), 1, sameProduction);

    assertThat(plan).hasSize(1);
  }

  @Test
  void capOfTwoLetsTwoLaunchersShareATarget() {
    final var plan =
        RocketTargetPlanner.plan(
            Map.of(launcherA, Set.of(targetX), launcherB, Set.of(targetX)), 2, sameProduction);

    assertThat(plan).containsEntry(launcherA, targetX).containsEntry(launcherB, targetX);
  }

  @Test
  void negativeCapMeansUnlimited() {
    final var plan =
        RocketTargetPlanner.plan(
            Map.of(
                launcherA, Set.of(targetX),
                launcherB, Set.of(targetX),
                launcherC, Set.of(targetX)),
            -1,
            sameProduction);

    assertThat(plan).hasSize(3);
  }

  @Test
  void noLaunchersMeansEmptyPlan() {
    assertThat(RocketTargetPlanner.plan(Map.of(), 1, sameProduction)).isEmpty();
  }
}
