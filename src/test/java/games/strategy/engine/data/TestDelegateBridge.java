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
  private final GameData m_data;
  private PlayerID m_id;
  private String m_stepName = "no name specified";
  private IDisplay m_dummyDisplay;
  private final ISound m_soundChannel = mock(ISound.class);
  private IRandomSource m_randomSource;
  private final IDelegateHistoryWriter m_historyWriter;
  private IRemotePlayer m_remote;

  /** Creates new TestDelegateBridge. */
  public TestDelegateBridge(final GameData data, final PlayerID id, final IDisplay dummyDisplay) {
    m_data = data;
    m_id = id;
    m_dummyDisplay = dummyDisplay;
    final History history = new History(m_data);
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
    m_historyWriter = new DelegateHistoryWriter(channelMessenger);
  }

  @Override
  public void setDisplay(final ITripleADisplay display) {
    m_dummyDisplay = display;
  }

  /**
   * Delegates should not use random data that comes from any other source.
   */
  @Override
  public int getRandom(final int max, final PlayerID player, final DiceType diceType, final String annotation) {
    return m_randomSource.getRandom(max, annotation);
  }

  @Override
  public int[] getRandom(final int max, final int count, final PlayerID player, final DiceType diceType,
      final String annotation) {
    return m_randomSource.getRandom(max, count, annotation);
  }

  /**
   * Changing the player has the effect of commiting the current transaction.
   * Player is initialized to the player specified in the xml data.
   */
  @Override
  public void setPlayerID(final PlayerID aPlayer) {
    m_id = aPlayer;
  }

  public boolean inTransaction() {
    return false;
  }

  @Override
  public PlayerID getPlayerID() {
    return m_id;
  }

  @Override
  public void addChange(final Change aChange) {
    m_data.performChange(aChange);
  }

  @Override
  public void setStepName(final String name) {
    setStepName(name, false);
  }

  @Override
  public void setStepName(final String name, final boolean doNotChangeSequence) {
    m_stepName = name;
    if (!doNotChangeSequence) {
      m_data.acquireWriteLock();
      try {
        final int length = m_data.getSequence().size();
        int i = 0;
        while (i < length && m_data.getSequence().getStep().getName().indexOf(name) == -1) {
          m_data.getSequence().next();
          i++;
        }
        if (i > +length && m_data.getSequence().getStep().getName().indexOf(name) == -1) {
          throw new IllegalStateException("Step not found: " + name);
        }
      } finally {
        m_data.releaseWriteLock();
      }
    }
  }

  @Override
  public String getStepName() {
    return m_stepName;
  }

  @Override
  public IDelegateHistoryWriter getHistoryWriter() {
    return m_historyWriter;
  }

  @Override
  public IRemotePlayer getRemotePlayer() {
    return m_remote;
  }

  @Override
  public IRemotePlayer getRemotePlayer(final PlayerID id) {
    return m_remote;
  }

  @Override
  public IDisplay getDisplayChannelBroadcaster() {
    return m_dummyDisplay;
  }

  @Override
  public ISound getSoundChannelBroadcaster() {
    return m_soundChannel;
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
    m_randomSource = randomSource;
  }

  @Override
  public void setRemote(final IRemotePlayer remote) {
    m_remote = remote;
  }

  @Override
  public void stopGameSequence() {}

  @Override
  public GameData getData() {
    return m_data;
  }
}
