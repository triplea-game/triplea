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
  private final IDelegateBridge bridge;
  private final GameDelegateHistoryWriter historyWriter;

  /**
   * Creates new TripleADelegateBridge to wrap an existing IDelegateBridge.
   *
   * @param bridge
   *        delegate bridge
   */
  public GameDelegateBridge(final IDelegateBridge bridge) {
    this.bridge = bridge;
    historyWriter = new GameDelegateHistoryWriter(this.bridge.getHistoryWriter(), getData());
  }

  @Override
  public GameData getData() {
    return bridge.getData();
  }

  /**
   * Return our custom historyWriter instead of the default one.
   */
  @Override
  public IDelegateHistoryWriter getHistoryWriter() {
    return historyWriter;
  }

  @Override
  public PlayerID getPlayerId() {
    return bridge.getPlayerId();
  }

  /**
   * All delegates should use random data that comes from both players so that
   * neither player cheats.
   */
  @Override
  public int getRandom(final int max, final PlayerID player, final DiceType diceType, final String annotation) {
    return bridge.getRandom(max, player, diceType, annotation);
  }

  @Override
  public int[] getRandom(final int max, final int count, final PlayerID player, final DiceType diceType,
      final String annotation) {
    return bridge.getRandom(max, count, player, diceType, annotation);
  }

  @Override
  public void addChange(final Change change) {
    bridge.addChange(change);
  }

  @Override
  public String getStepName() {
    return bridge.getStepName();
  }

  @Override
  public IRemotePlayer getRemotePlayer() {
    return bridge.getRemotePlayer();
  }

  @Override
  public IRemotePlayer getRemotePlayer(final PlayerID id) {
    return bridge.getRemotePlayer(id);
  }

  @Override
  public IDisplay getDisplayChannelBroadcaster() {
    return bridge.getDisplayChannelBroadcaster();
  }

  @Override
  public ISound getSoundChannelBroadcaster() {
    return bridge.getSoundChannelBroadcaster();
  }

  @Override
  public Properties getStepProperties() {
    return bridge.getStepProperties();
  }

  @Override
  public void leaveDelegateExecution() {
    bridge.leaveDelegateExecution();
  }

  @Override
  public void enterDelegateExecution() {
    bridge.enterDelegateExecution();
  }

  @Override
  public void stopGameSequence() {
    bridge.stopGameSequence();
  }
}
