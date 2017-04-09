package games.strategy.engine.delegate;

import java.util.Properties;

import games.strategy.engine.GameOverException;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.framework.AbstractGame;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.gamePlayer.IRemotePlayer;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.engine.message.MessengerException;
import games.strategy.engine.random.IRandomSource;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.engine.random.RandomStats;
import games.strategy.sound.ISound;

/**
 * Default implementation of DelegateBridge.
 */
public class DefaultDelegateBridge implements IDelegateBridge {
  private final GameData m_data;
  private final IGame m_game;
  private final IDelegateHistoryWriter m_historyWriter;
  private final RandomStats m_randomStats;
  private final DelegateExecutionManager m_delegateExecutionManager;
  private IRandomSource m_randomSource;

  /** Creates new DefaultDelegateBridge. */
  public DefaultDelegateBridge(final GameData data, final IGame game, final IDelegateHistoryWriter historyWriter,
      final RandomStats randomStats, final DelegateExecutionManager delegateExecutionManager) {
    m_data = data;
    m_game = game;
    m_historyWriter = historyWriter;
    m_randomStats = randomStats;
    m_delegateExecutionManager = delegateExecutionManager;
  }

  @Override
  public GameData getData() {
    return m_data;
  }

  @Override
  public PlayerID getPlayerID() {
    return m_data.getSequence().getStep().getPlayerID();
  }

  public void setRandomSource(final IRandomSource randomSource) {
    m_randomSource = randomSource;
  }

  /**
   * All delegates should use random data that comes from both players so that
   * neither player cheats.
   */
  @Override
  public int getRandom(final int max, final PlayerID player, final DiceType diceType, final String annotation)
      throws IllegalArgumentException, IllegalStateException {
    final int random = m_randomSource.getRandom(max, annotation);
    m_randomStats.addRandom(random, player, diceType);
    return random;
  }

  /**
   * Delegates should not use random data that comes from any other source.
   */
  @Override
  public int[] getRandom(final int max, final int count, final PlayerID player, final DiceType diceType,
      final String annotation) throws IllegalArgumentException, IllegalStateException {
    final int[] rVal = m_randomSource.getRandom(max, count, annotation);
    m_randomStats.addRandom(rVal, player, diceType);
    return rVal;
  }

  @Override
  public void addChange(final Change aChange) {
    if (aChange instanceof CompositeChange) {
      final CompositeChange c = (CompositeChange) aChange;
      if (c.getChanges().size() == 1) {
        addChange(c.getChanges().get(0));
        return;
      }
    }
    if (!aChange.isEmpty()) {
      m_game.addChange(aChange);
    }
  }

  /**
   * Returns the current step name.
   */
  @Override
  public String getStepName() {
    return m_data.getSequence().getStep().getName();
  }

  @Override
  public IDelegateHistoryWriter getHistoryWriter() {
    return m_historyWriter;
  }

  private Object getOutbound(final Object o) {
    final Class<?>[] interfaces = o.getClass().getInterfaces();
    return m_delegateExecutionManager.createOutboundImplementation(o, interfaces);
  }

  @Override
  public IRemotePlayer getRemotePlayer() {
    return getRemotePlayer(getPlayerID());
  }

  @Override
  public IRemotePlayer getRemotePlayer(final PlayerID id) {
    try {
      final Object implementor = m_game.getRemoteMessenger().getRemote(ServerGame.getRemoteName(id, m_data));
      return (IRemotePlayer) getOutbound(implementor);
    } catch (final MessengerException me) {
      throw new GameOverException("Game Over!");
    }
  }

  @Override
  public IDisplay getDisplayChannelBroadcaster() {
    final Object implementor =
        m_game.getChannelMessenger().getChannelBroadcastor(AbstractGame.getDisplayChannel(m_data));
    return (IDisplay) getOutbound(implementor);
  }

  @Override
  public ISound getSoundChannelBroadcaster() {
    final Object implementor = m_game.getChannelMessenger().getChannelBroadcastor(AbstractGame.getSoundChannel(m_data));
    return (ISound) getOutbound(implementor);
  }

  @Override
  public Properties getStepProperties() {
    return m_data.getSequence().getStep().getProperties();
  }

  @Override
  public void leaveDelegateExecution() {
    m_delegateExecutionManager.leaveDelegateExecution();
  }

  @Override
  public void enterDelegateExecution() {
    m_delegateExecutionManager.enterDelegateExecution();
  }

  @Override
  public void stopGameSequence() {
    ((ServerGame) m_game).stopGameSequence();
  }
}
