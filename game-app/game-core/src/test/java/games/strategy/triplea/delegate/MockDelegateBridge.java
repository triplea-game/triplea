package games.strategy.triplea.delegate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.history.DelegateHistoryWriter;
import games.strategy.engine.player.Player;
import lombok.experimental.UtilityClass;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.OngoingStubbing;
import org.mockito.verification.VerificationMode;
import org.triplea.sound.ISound;

@UtilityClass
public final class MockDelegateBridge {

  /**
   * Returns a new delegate bridge suitable for testing the given game data and player.
   *
   * @return A mock that can be configured using standard Mockito idioms.
   */
  public static IDelegateBridge newDelegateBridge(final GamePlayer gamePlayer) {
    final GameData gameData = gamePlayer.getData();
    final IDelegateBridge delegateBridge = mock(IDelegateBridge.class);
    doAnswer(
            invocation -> {
              final Change change = invocation.getArgument(0);
              gameData.performChange(change);
              return null;
            })
        .when(delegateBridge)
        .addChange(any());
    when(delegateBridge.getData()).thenReturn(gameData);
    when(delegateBridge.getDisplayChannelBroadcaster()).thenReturn(mock(IDisplay.class));
    when(delegateBridge.getHistoryWriter())
        .thenReturn(DelegateHistoryWriter.createNoOpImplementation());
    when(delegateBridge.getGamePlayer()).thenReturn(gamePlayer);
    final Player remotePlayer = mock(Player.class);
    when(delegateBridge.getRemotePlayer()).thenReturn(remotePlayer);
    when(delegateBridge.getRemotePlayer(any())).thenReturn(remotePlayer);
    when(delegateBridge.getSoundChannelBroadcaster()).thenReturn(mock(ISound.class));
    when(delegateBridge.getCostsForTuv(any())).thenCallRealMethod();
    return delegateBridge;
  }

  public static OngoingStubbing<int[]> whenGetRandom(final IDelegateBridge delegateBridge) {
    return when(delegateBridge.getRandom(anyInt(), anyInt(), any(), any(), anyString()));
  }

  public static Answer<int[]> withValues(final int... values) {
    return invocation -> {
      final int count = invocation.getArgument(1);
      assertThat("count of requested random values does not match", count, is(values.length));
      return values;
    };
  }

  public static Answer<int[]> withDiceValues(final int... values) {
    for (int i = 0; i < values.length; i++) {
      // A die roll of N corresponds to a random value of N - 1.
      values[i] -= 1;
    }
    return withValues(values);
  }

  public static void thenGetRandomShouldHaveBeenCalled(
      final IDelegateBridge delegateBridge, final VerificationMode verificationMode) {
    verify(delegateBridge, verificationMode)
        .getRandom(anyInt(), anyInt(), any(), any(), anyString());
  }

  public static void advanceToStep(final IDelegateBridge delegateBridge, final String stepName) {
    final GameData gameData = delegateBridge.getData();
    try (GameData.Unlocker ignored = gameData.acquireWriteLock()) {
      final int length = gameData.getSequence().size();
      int i = 0;
      while ((i < length) && !gameData.getSequence().getStep().getName().contains(stepName)) {
        gameData.getSequence().next();
        i++;
      }
      if ((i > length) && !gameData.getSequence().getStep().getName().contains(stepName)) {
        throw new IllegalArgumentException("Step not found: " + stepName);
      }
    }
  }
}
