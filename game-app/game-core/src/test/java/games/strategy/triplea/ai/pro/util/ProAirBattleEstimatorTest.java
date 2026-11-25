package games.strategy.triplea.ai.pro.util;

import static games.strategy.triplea.delegate.GameDataTestUtil.bomber;
import static games.strategy.triplea.delegate.GameDataTestUtil.fighter;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.italians;
import static games.strategy.triplea.delegate.GameDataTestUtil.russians;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Constants;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class ProAirBattleEstimatorTest {
  private final GameData gameData = TestMapGameData.AIR_BATTLE.getGameData();

  private ProAirBattleEstimator.Outcome estimate(
      final List<Unit> attackers, final List<Unit> defenders) {
    return ProAirBattleEstimator.estimate(gameData, attackers, defenders, u -> 1, new Random(1));
  }

  @Test
  void manyEscortsKillALoneInterceptorAndLoseNothing() {
    final List<Unit> escorts = fighter(gameData).create(30, italians(gameData));
    final List<Unit> interceptors = fighter(gameData).create(1, russians(gameData));

    final var outcome = estimate(escorts, interceptors);

    assertThat(outcome.defendersLost()).isEqualTo(interceptors);
    assertThat(outcome.attackersLost()).isEmpty();
  }

  @Test
  void aLoneEscortDiesToManyInterceptors() {
    final List<Unit> escorts = fighter(gameData).create(1, italians(gameData));
    final List<Unit> interceptors = fighter(gameData).create(30, russians(gameData));

    final var outcome = estimate(escorts, interceptors);

    assertThat(outcome.attackersLost()).isEqualTo(escorts);
    assertThat(outcome.defendersLost()).isEmpty();
  }

  @Test
  void onlyTheCheapestUnitsAreLost() {
    final List<Unit> escorts = fighter(gameData).create(6, italians(gameData));
    final List<Unit> interceptors = fighter(gameData).create(3, russians(gameData));
    final Unit expensive = interceptors.get(0);

    final var outcome =
        ProAirBattleEstimator.estimate(
            gameData, escorts, interceptors, u -> u.equals(expensive) ? 10 : 1, new Random(1));

    assertThat(outcome.defendersLost()).hasSize(1).doesNotContain(expensive);
  }

  @Test
  void nothingHappensWhenThePropertyIsOff() {
    gameData.getProperties().set(Constants.BATTLES_MAY_BE_PRECEEDED_BY_AIR_BATTLES, false);

    final var outcome =
        estimate(
            fighter(gameData).create(30, italians(gameData)),
            fighter(gameData).create(1, russians(gameData)));

    assertThat(outcome.isEmpty()).isTrue();
  }

  @Test
  void unitsThatCannotAirBattleDoNotTakePart() {
    final var bombersAttacking =
        estimate(
            bomber(gameData).create(30, italians(gameData)),
            fighter(gameData).create(1, russians(gameData)));
    final var groundDefenders =
        estimate(
            fighter(gameData).create(30, italians(gameData)),
            infantry(gameData).create(5, russians(gameData)));

    assertThat(bombersAttacking.isEmpty()).isTrue();
    assertThat(groundDefenders.isEmpty()).isTrue();
  }
}
