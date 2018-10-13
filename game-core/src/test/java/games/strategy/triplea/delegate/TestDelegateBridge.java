package games.strategy.triplea.delegate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.gamePlayer.IRemotePlayer;
import games.strategy.engine.history.DelegateHistoryWriter;
import games.strategy.engine.history.History;
import games.strategy.engine.history.HistoryWriter;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.engine.message.ChannelMessenger;
import games.strategy.engine.message.unifiedmessenger.UnifiedMessenger;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.net.IServerMessenger;
import games.strategy.net.Node;
import games.strategy.sound.ISound;
import games.strategy.triplea.player.ITripleAPlayer;

/**
 * Not for actual use, suitable for testing. Never returns messages, but can get
 * random and implements changes immediately.
 *
 * @deprecated Use mock objects instead. This TestDelegateBridge object has a substantial amount of implementation and
 *             coupling to the rest of the system, do not build on it.
 */
@Deprecated
class TestDelegateBridge implements ITestDelegateBridge {
  private final GameData gameData;
  private String stepName = "no name specified";
  private final IDelegateHistoryWriter delegateHistoryWriter;

  TestDelegateBridge(final GameData data) {
    gameData = data;
    final History history = new History(gameData);
    final HistoryWriter historyWriter = new HistoryWriter(history);
    historyWriter.startNextStep("", "", PlayerID.NULL_PLAYERID, "");
    final IServerMessenger messenger = mock(IServerMessenger.class);
    try {
      when(messenger.getLocalNode()).thenReturn(new Node("dummy", InetAddress.getLocalHost(), 0));
    } catch (final UnknownHostException e) {
      throw new IllegalStateException("test cannot run without network interface", e);
    }
    when(messenger.isServer()).thenReturn(true);
    final ChannelMessenger channelMessenger =
        new ChannelMessenger(new UnifiedMessenger(messenger));
    delegateHistoryWriter = new DelegateHistoryWriter(channelMessenger);
  }

  @Override
  public int getRandom(final int max, final PlayerID player, final DiceType diceType, final String annotation) {
    return 0;
  }

  @Override
  public int[] getRandom(final int max, final int count, final PlayerID player, final DiceType diceType,
      final String annotation) {
    return null;
  }

  @Override
  public PlayerID getPlayerId() {
    return null;
  }

  @Override
  public void addChange(final Change change) {
    gameData.performChange(change);
  }

  @Override
  public void setStepName(final String name) {
    stepName = name;
    gameData.acquireWriteLock();
    try {
      final int length = gameData.getSequence().size();
      int i = 0;
      while (i < length && gameData.getSequence().getStep().getName().indexOf(name) == -1) {
        gameData.getSequence().next();
        i++;
      }
      if (i > +length && gameData.getSequence().getStep().getName().indexOf(name) == -1) {
        throw new IllegalStateException("Step not found: " + name);
      }
    } finally {
      gameData.releaseWriteLock();
    }
  }

  @Override
  public String getStepName() {
    return stepName;
  }

  @Override
  public IDelegateHistoryWriter getHistoryWriter() {
    return delegateHistoryWriter;
  }

  @Override
  public ITripleAPlayer getRemotePlayer() {
    return null;
  }

  @Override
  public IRemotePlayer getRemotePlayer(final PlayerID id) {
    return null;
  }

  @Override
  public IDisplay getDisplayChannelBroadcaster() {
    return null;
  }

  @Override
  public ISound getSoundChannelBroadcaster() {
    return null;
  }

  @Override
  public Properties getStepProperties() {
    return null;
  }

  @Override
  public void leaveDelegateExecution() {}

  @Override
  public void enterDelegateExecution() {}

  @Override
  public void stopGameSequence() {}

  @Override
  public GameData getData() {
    return gameData;
  }
}
