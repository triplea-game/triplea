package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.engine.player.Player;
import games.strategy.engine.random.IRandomStats.DiceType;
import java.util.Properties;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;
import org.triplea.sound.ISound;

/** TripleA implementation of DelegateBridge. */
public class GameDelegateBridge implements IDelegateBridge {
  private final IDelegateBridge bridge;
  private final GameDelegateHistoryWriter historyWriter;

  public GameDelegateBridge(final IDelegateBridge bridge) {
    this.bridge = bridge;
    historyWriter = new GameDelegateHistoryWriter(this.bridge.getHistoryWriter(), getData());
  }

  @Override
  public GameData getData() {
    return bridge.getData();
  }

  @Override
  public void sendMessage(final WebSocketMessage webSocketMessage) {
    bridge.sendMessage(webSocketMessage);
  }

  /** Return our custom historyWriter instead of the default one. */
  @Override
  public IDelegateHistoryWriter getHistoryWriter() {
    return historyWriter;
  }

  @Override
  public GamePlayer getGamePlayer() {
    return bridge.getGamePlayer();
  }

  /**
   * All delegates should use random data that comes from both players so that neither player
   * cheats.
   */
  @Override
  public int getRandom(
      final int max, final GamePlayer player, final DiceType diceType, final String annotation) {
    return bridge.getRandom(max, player, diceType, annotation);
  }

  @Override
  public int[] getRandom(
      final int max,
      final int count,
      final GamePlayer player,
      final DiceType diceType,
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
  public Player getRemotePlayer() {
    return bridge.getRemotePlayer();
  }

  @Override
  public Player getRemotePlayer(final GamePlayer gamePlayer) {
    return bridge.getRemotePlayer(gamePlayer);
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
