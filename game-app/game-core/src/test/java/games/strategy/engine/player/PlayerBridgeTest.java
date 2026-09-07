package games.strategy.engine.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.GameOverException;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.Messengers;
import games.strategy.triplea.delegate.TestDelegate;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Pins {@link PlayerBridge}'s live game-sequence reads: {@code getStepName} must report the
 * sequence's current step at call time, and {@code getRemoteDelegate} must resolve whichever step's
 * delegate the sequence has already advanced to, both without polling.
 */
class PlayerBridgeTest {
  private final GameData gameData = new GameData();

  private GameStep newStep(final String name) {
    final IDelegate delegate = new TestDelegate();
    delegate.initialize(name + "Delegate", name + "DelegateDisplay");
    return newStepUsing(name, delegate);
  }

  private GameStep newStepUsing(final String name, final IDelegate delegate) {
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

    assertThat(bridge.getStepName()).isEqualTo("stepA");

    gameData.getSequence().next();

    assertThat(bridge.getStepName())
        .as("advancing the sequence must change what getStepName reports")
        .isEqualTo("stepB");
  }

  @Test
  void getRemoteDelegateResolvesWhicheverStepTheSequenceHasAlreadyAdvancedToBeforeTheCall() {
    // Models the invariant this fix relies on: ServerGame/ClientGame both advance the live
    // sequence to the target step before invoking Player.start(stepName), so a same-thread read
    // taken after an advance (no polling) must already resolve the new step's delegate.
    final IDelegate delegateA = new TestDelegate();
    delegateA.initialize("delegateA", "delegateADisplay");
    final IDelegate delegateB = new TestDelegate();
    delegateB.initialize("delegateB", "delegateBDisplay");
    gameData.addDelegate(delegateA);
    gameData.addDelegate(delegateB);
    gameData.getSequence().addStep(newStepUsing("stepA", delegateA));
    gameData.getSequence().addStep(newStepUsing("stepB", delegateB));

    final IGame game = mock(IGame.class);
    when(game.getData()).thenReturn(gameData);
    final Messengers messengers = mock(Messengers.class);
    when(game.getMessengers()).thenReturn(messengers);
    when(messengers.getRemote(any(RemoteName.class))).thenReturn(mock(IRemote.class));
    final PlayerBridge bridge = new PlayerBridge(game);
    final ArgumentCaptor<RemoteName> requestedRemote = ArgumentCaptor.forClass(RemoteName.class);

    bridge.getRemoteDelegate();
    gameData.getSequence().next();
    bridge.getRemoteDelegate();

    verify(messengers, times(2)).getRemote(requestedRemote.capture());
    assertThat(requestedRemote.getAllValues())
        .as("getRemoteDelegate must resolve the step the sequence has advanced to, in order")
        .extracting(RemoteName::getName)
        .containsExactly(
            ServerGame.getRemoteName(delegateA).getName(),
            ServerGame.getRemoteName(delegateB).getName());
  }

  @Test
  void getRemoteDelegateThrowsGameOverWhenGameIsOver() {
    final IGame game = mock(IGame.class);
    when(game.isGameOver()).thenReturn(true);
    final PlayerBridge bridge = new PlayerBridge(game);

    assertThrows(GameOverException.class, bridge::getRemoteDelegate);
  }
}
