package games.strategy.engine.data;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.gamePlayer.IRemotePlayer;
import games.strategy.engine.history.DelegateHistoryWriter;
import games.strategy.engine.history.History;
import games.strategy.engine.history.HistoryWriter;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.engine.message.ChannelMessenger;
import games.strategy.engine.message.unifiedmessenger.UnifiedMessenger;
import games.strategy.engine.random.IRandomSource;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.net.IServerMessenger;
import games.strategy.net.Node;
import games.strategy.sound.ISound;
import games.strategy.triplea.ui.display.ITripleADisplay;

/**
 * Not for actual use, suitable for testing. Never returns messages, but can get
 * random and implements changes immediately.
 *
 * @deprecated Use mock objects instead. This TestDelegateBridge object has a substantial amount of implementation and
 *             coupling to the rest of the system, do not build on it.
 */
@Deprecated
public class TestDelegateBridge implements ITestDelegateBridge {
  private final GameData gameData;
  private PlayerID playerId;
  private String stepName = "no name specified";
  private IDisplay dummyDisplay;
  private final ISound soundChannel = mock(ISound.class);
  private IRandomSource randomSource;
  private final IDelegateHistoryWriter delegateHistoryWriter;
  private IRemotePlayer remotePlayer;

  /** Creates new TestDelegateBridge. */
  public TestDelegateBridge(final GameData data, final PlayerID id, final IDisplay dummyDisplay) {
    gameData = data;
    playerId = id;
    this.dummyDisplay = dummyDisplay;
    final History history = new History(gameData);
    final HistoryWriter historyWriter = new HistoryWriter(history);
    historyWriter.startNextStep("", "", PlayerID.NULL_PLAYERID, "");
    final IServerMessenger messenger = mock(IServerMessenger.class);
    try {
      when(messenger.getLocalNode()).thenReturn(new Node("dummy", InetAddress.getLocalHost(), 0));
    } catch (final UnknownHostException e) {
      ClientLogger.logQuietly(e);
    }
    when(messenger.isServer()).thenReturn(true);
    final ChannelMessenger channelMessenger =
        new ChannelMessenger(new UnifiedMessenger(messenger));
    delegateHistoryWriter = new DelegateHistoryWriter(channelMessenger);
  }

  @Override
  public void setDisplay(final ITripleADisplay display) {
    dummyDisplay = display;
  }

  /**
   * Delegates should not use random data that comes from any other source.
   */
  @Override
  public int getRandom(final int max, final PlayerID player, final DiceType diceType, final String annotation) {
    return randomSource.getRandom(max, annotation);
  }

  @Override
  public int[] getRandom(final int max, final int count, final PlayerID player, final DiceType diceType,
      final String annotation) {
    return randomSource.getRandom(max, count, annotation);
  }

  /**
   * Changing the player has the effect of commiting the current transaction.
   * Player is initialized to the player specified in the xml data.
   */
  @Override
  public void setPlayerId(final PlayerID playerId) {
    this.playerId = playerId;
  }

  public boolean inTransaction() {
    return false;
  }

  @Override
  public PlayerID getPlayerID() {
    return playerId;
  }

  @Override
  public void addChange(final Change change) {
    gameData.performChange(change);
  }

  @Override
  public void setStepName(final String name) {
    setStepName(name, false);
  }

  @Override
  public void setStepName(final String name, final boolean doNotChangeSequence) {
    stepName = name;
    if (!doNotChangeSequence) {
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
  public IRemotePlayer getRemotePlayer() {
    return remotePlayer;
  }

  @Override
  public IRemotePlayer getRemotePlayer(final PlayerID id) {
    return remotePlayer;
  }

  @Override
  public IDisplay getDisplayChannelBroadcaster() {
    return dummyDisplay;
  }

  @Override
  public ISound getSoundChannelBroadcaster() {
    return soundChannel;
  }

  @Override
  public Properties getStepProperties() {
    return new Properties();
  }

  @Override
  public void leaveDelegateExecution() {}

  @Override
  public void enterDelegateExecution() {}

  @Override
  public void setRandomSource(final IRandomSource randomSource) {
    this.randomSource = randomSource;
  }

  @Override
  public void setRemote(final IRemotePlayer remote) {
    remotePlayer = remote;
  }

  @Override
  public void stopGameSequence() {}

  @Override
  public GameData getData() {
    return gameData;
  }
}
