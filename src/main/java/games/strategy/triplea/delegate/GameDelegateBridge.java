package games.strategy.triplea.delegate;

import java.util.Properties;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.gamePlayer.IRemotePlayer;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.sound.ISound;

/**
 * TripleA implementation of DelegateBridge.
 */
public class GameDelegateBridge implements IDelegateBridge {
  private final IDelegateBridge m_bridge;
  private final GameDelegateHistoryWriter m_historyWriter;

  /**
   * Creates new TripleADelegateBridge to wrap an existing IDelegateBridge.
   *
   * @param bridge
   *        delegate bridge
   */
  public GameDelegateBridge(final IDelegateBridge bridge) {
    m_bridge = bridge;
    m_historyWriter = new GameDelegateHistoryWriter(m_bridge.getHistoryWriter(), getData());
  }

  @Override
  public GameData getData() {
    return m_bridge.getData();
  }

  /**
   * Return our custom historyWriter instead of the default one.
   */
  @Override
  public IDelegateHistoryWriter getHistoryWriter() {
    return m_historyWriter;
  }

  @Override
  public PlayerID getPlayerID() {
    return m_bridge.getPlayerID();
  }

  /**
   * All delegates should use random data that comes from both players so that
   * neither player cheats.
   */
  @Override
  public int getRandom(final int max, final PlayerID player, final DiceType diceType, final String annotation) {
    return m_bridge.getRandom(max, player, diceType, annotation);
  }

  /**
   * Delegates should not use random data that comes from any other source.
   */
  @Override
  public int[] getRandom(final int max, final int count, final PlayerID player, final DiceType diceType,
      final String annotation) {
    return m_bridge.getRandom(max, count, player, diceType, annotation);
  }

  @Override
  public void addChange(final Change change) {
    m_bridge.addChange(change);
  }

  @Override
  public String getStepName() {
    return m_bridge.getStepName();
  }

  @Override
  public IRemotePlayer getRemotePlayer() {
    return m_bridge.getRemotePlayer();
  }

  @Override
  public IRemotePlayer getRemotePlayer(final PlayerID id) {
    return m_bridge.getRemotePlayer(id);
  }

  @Override
  public IDisplay getDisplayChannelBroadcaster() {
    return m_bridge.getDisplayChannelBroadcaster();
  }

  @Override
  public ISound getSoundChannelBroadcaster() {
    return m_bridge.getSoundChannelBroadcaster();
  }

  @Override
  public Properties getStepProperties() {
    return m_bridge.getStepProperties();
  }

  @Override
  public void leaveDelegateExecution() {
    m_bridge.leaveDelegateExecution();
  }

  @Override
  public void enterDelegateExecution() {
    m_bridge.enterDelegateExecution();
  }

  @Override
  public void stopGameSequence() {
    m_bridge.stopGameSequence();
  }
}
