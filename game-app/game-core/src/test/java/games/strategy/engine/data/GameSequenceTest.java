package games.strategy.engine.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import games.strategy.engine.delegate.IDelegate;
import games.strategy.triplea.delegate.TestDelegate;
import java.util.Properties;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NonNls;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class GameSequenceTest {
  @Nested
  final class SetRoundAndStepTest {
    @NonNls private static final String GAME_STEP_NAME = "gameStep";
    @NonNls private static final String OTHER_GAME_STEP_NAME = "otherGameStep";

    private final GameData gameData = new GameData();
    private final GamePlayer player = new GamePlayer("player", gameData);
    private final GameSequence gameSequence = new GameSequence(gameData);

    private GameStep newGameStep(final String displayName, final @Nullable GamePlayer player) {
      final IDelegate delegate = new TestDelegate();
      delegate.initialize("delegateName", "delegateDisplayName");
      return new GameStep("stepName", displayName, player, delegate, gameData, new Properties());
    }

    @Test
    void shouldSetRound() {
      gameSequence.addStep(newGameStep(GAME_STEP_NAME, player));

      final int round = 42;
      gameSequence.setRoundAndStep(round, null, null);

      assertThat(gameSequence.getRound(), is(round));
    }

    @Test
    void shouldSetStepIndexWhenPlayerIsNullAndStepExists() {
      gameSequence.addStep(newGameStep(GAME_STEP_NAME, null));
      gameSequence.addStep(newGameStep(OTHER_GAME_STEP_NAME, null));

      gameSequence.setRoundAndStep(1, OTHER_GAME_STEP_NAME, null);

      assertThat(gameSequence.getStepIndex(), is(1));
    }

    @Test
    void shouldSetStepIndexToZeroWhenPlayerIsNullAndStepNotExists() {
      gameSequence.addStep(newGameStep(GAME_STEP_NAME, null));

      gameSequence.setRoundAndStep(1, OTHER_GAME_STEP_NAME, null);

      assertThat(gameSequence.getStepIndex(), is(0));
    }

    @Test
    void shouldSetStepIndexWhenPlayerIsNotNullAndStepExists() {
      gameSequence.addStep(newGameStep(GAME_STEP_NAME, player));
      gameSequence.addStep(newGameStep(OTHER_GAME_STEP_NAME, player));

      gameSequence.setRoundAndStep(1, OTHER_GAME_STEP_NAME, player);

      assertThat(gameSequence.getStepIndex(), is(1));
    }

    @Test
    void shouldSetStepIndexToZeroWhenPlayerIsNotNullAndStepNotExists() {
      gameSequence.addStep(newGameStep(GAME_STEP_NAME, player));

      gameSequence.setRoundAndStep(1, GAME_STEP_NAME, player);

      assertThat(gameSequence.getStepIndex(), is(0));
    }
  }
}
