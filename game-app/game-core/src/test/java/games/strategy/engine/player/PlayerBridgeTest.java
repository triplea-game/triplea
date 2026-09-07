package games.strategy.engine.player;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.GameOverException;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.framework.IGame;
import games.strategy.triplea.delegate.TestDelegate;
import java.util.Properties;
import org.junit.jupiter.api.Test;

/**
 * Pins the live game-sequence reads that replaced the cached step-name field: 'getStepName' must
 * report the sequence's current step at call time, and 'getRemoteDelegate' must reject a step with
 * no resolvable delegate rather than dereference it.
 */
class PlayerBridgeTest {
  private final GameData gameData = new GameData();

  private GameStep newStep(final String name) {
    final IDelegate delegate = new TestDelegate();
    delegate.initialize(name + "Delegate", name + "DelegateDisplay");
    return new GameStep(name, name + "Display", null, delegate, gameData, new Properties());
  }

  private PlayerBridge newBridgeReadingLiveData() {
    final IGame game = mock(IGame.class);
    when(game.getData()).thenReturn(gameData);
    return new PlayerBridge(game);
  }

  @Test
  void getStepNameTracksTheSequenceStepLiveRatherThanCaching() {
    final PlayerBridge bridge = newBridgeReadingLiveData();
    gameData.getSequence().addStep(newStep("stepA"));
    gameData.getSequence().addStep(newStep("stepB"));

    assertThat(bridge.getStepName(), is("stepA"));

    gameData.getSequence().next();

    assertThat(
        "advancing the sequence must change what getStepName reports",
        bridge.getStepName(),
        is("stepB"));
  }

  @Test
  void getStepNameIsNullWhenSequenceStepIsNull() {
    final PlayerBridge bridge = newBridgeReadingLiveData();
    gameData.getSequence().addStep(null);

    assertThat(bridge.getStepName(), is(nullValue()));
  }

  @Test
  void getRemoteDelegateThrowsIllegalStateWhenSequenceHasNoDelegate() {
    final PlayerBridge bridge = newBridgeReadingLiveData();
    gameData.getSequence().addStep(null);

    assertThrows(IllegalStateException.class, bridge::getRemoteDelegate);
  }

  @Test
  void getRemoteDelegateThrowsGameOverWhenGameIsOver() {
    final IGame game = mock(IGame.class);
    when(game.isGameOver()).thenReturn(true);
    final PlayerBridge bridge = new PlayerBridge(game);

    assertThrows(GameOverException.class, bridge::getRemoteDelegate);
  }
}
