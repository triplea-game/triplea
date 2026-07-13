package games.strategy.triplea.delegate.battle.simulation;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.history.DelegateHistoryWriter;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.engine.player.Player;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.ui.display.HeadlessDisplay;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;
import org.triplea.sound.HeadlessSoundChannel;
import org.triplea.sound.ISound;

/** Minimal deterministic delegate bridge for executing one restored battle without a UI. */
final class SimulationDelegateBridge implements IDelegateBridge {
  private final GameData gameData;
  private final GamePlayer currentPlayer;
  private final BattleDecisionController decisionController;
  private final Random random;
  private final Map<GamePlayer, Player> players = new HashMap<>();
  private final IDisplay display = new HeadlessDisplay();
  private final ISound sound = new HeadlessSoundChannel();
  private final IDelegateHistoryWriter historyWriter =
      DelegateHistoryWriter.createNoOpImplementation();

  SimulationDelegateBridge(
      final GameData gameData,
      final GamePlayer currentPlayer,
      final BattleDecisionController decisionController,
      final long seed) {
    this.gameData = gameData;
    this.currentPlayer = currentPlayer;
    this.decisionController = decisionController;
    random = new Random(seed);
  }

  @Override
  public Player getRemotePlayer() {
    return getRemotePlayer(currentPlayer);
  }

  @Override
  public Player getRemotePlayer(final GamePlayer gamePlayer) {
    return players.computeIfAbsent(
        gamePlayer, player -> new SimulationPlayer(decisionController, player));
  }

  @Override
  public GamePlayer getGamePlayer() {
    return currentPlayer;
  }

  @Override
  public void addChange(final Change change) {
    gameData.performChange(change);
  }

  @Override
  public int getRandom(
      final int max, final GamePlayer player, final DiceType diceType, final String annotation) {
    return random.nextInt(max);
  }

  @Override
  public int[] getRandom(
      final int max,
      final int count,
      final GamePlayer player,
      final DiceType diceType,
      final String annotation) {
    final int[] values = new int[count];
    for (int i = 0; i < count; i++) {
      values[i] = random.nextInt(max);
    }
    return values;
  }

  @Override
  public IDelegateHistoryWriter getHistoryWriter() {
    return historyWriter;
  }

  @Override
  public IDisplay getDisplayChannelBroadcaster() {
    return display;
  }

  @Override
  public ISound getSoundChannelBroadcaster() {
    return sound;
  }

  @Override
  public void stopGameSequence(final String status, final String title) {}

  @Override
  public void leaveDelegateExecution() {}

  @Override
  public void enterDelegateExecution() {}

  @Override
  public GameData getData() {
    return gameData;
  }

  @Override
  public void sendMessage(final WebSocketMessage webSocketMessage) {}

  @Override
  public Optional<ResourceLoader> getResourceLoader() {
    return Optional.empty();
  }
}
