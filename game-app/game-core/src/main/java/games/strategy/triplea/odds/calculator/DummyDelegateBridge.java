package games.strategy.triplea.odds.calculator;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.units.UnitDamageReceivedChange;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.history.DelegateHistoryWriter;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.engine.player.Player;
import games.strategy.engine.random.IRandomStats;
import games.strategy.engine.random.PlainRandomSource;
import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import games.strategy.triplea.ui.display.HeadlessDisplay;
import games.strategy.triplea.util.TuvCostsCalculator;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;
import org.triplea.java.collections.IntegerMap;
import org.triplea.sound.HeadlessSoundChannel;
import org.triplea.sound.ISound;

/** Delegate bridge implementation with minimum valid behavior. */
public class DummyDelegateBridge implements IDelegateBridge {
  private final PlainRandomSource randomSource = new PlainRandomSource();
  private final IDisplay display = new HeadlessDisplay();
  private final ISound soundChannel = new HeadlessSoundChannel();
  private final DummyPlayer attackingPlayer;
  private final DummyPlayer defendingPlayer;
  private final GamePlayer attacker;
  private final DelegateHistoryWriter writer = DelegateHistoryWriter.createNoOpImplementation();
  private final CompositeChange allChanges;
  private final GameData gameData;
  @Getter private MustFightBattle battle = null;
  private final TuvCostsCalculator tuvCalculator;

  public DummyDelegateBridge(
      final GamePlayer attacker,
      final GameData data,
      final CompositeChange allChanges,
      final List<Unit> attackerOrderOfLosses,
      final List<Unit> defenderOrderOfLosses,
      final boolean attackerKeepOneLandUnit,
      final int retreatAfterRound,
      final int retreatAfterXUnitsLeft,
      final boolean retreatWhenOnlyAirLeft,
      final TuvCostsCalculator tuvCalculator) {
    attackingPlayer =
        new DummyPlayer(
            this,
            true,
            "battle calc dummy",
            attackerOrderOfLosses,
            attackerKeepOneLandUnit,
            retreatAfterRound,
            retreatAfterXUnitsLeft,
            retreatWhenOnlyAirLeft);
    defendingPlayer =
        new DummyPlayer(
            this,
            false,
            "battle calc dummy",
            defenderOrderOfLosses,
            false,
            retreatAfterRound,
            -1,
            false);
    gameData = data;
    this.attacker = attacker;
    this.allChanges = allChanges;
    this.tuvCalculator = tuvCalculator;
  }

  @Override
  public GameData getData() {
    return gameData;
  }

  @Override
  public void sendMessage(final WebSocketMessage webSocketMessage) {}

  @Override
  public Optional<ResourceLoader> getResourceLoader() {
    throw new UnsupportedOperationException(
        "DummyDelegateBridge#getResourceLoader() should never be called");
  }

  @Override
  public void leaveDelegateExecution() {}

  @Override
  public Player getRemotePlayer(final GamePlayer gamePlayer) {
    return gamePlayer.equals(attacker) ? attackingPlayer : defendingPlayer;
  }

  @Override
  public Player getRemotePlayer() {
    // the current player is attacker
    return attackingPlayer;
  }

  @Override
  public int[] getRandom(
      final int max,
      final int count,
      final GamePlayer player,
      final IRandomStats.DiceType diceType,
      final String annotation) {
    return randomSource.getRandom(max, count, annotation);
  }

  @Override
  public int getRandom(
      final int max,
      final GamePlayer player,
      final IRandomStats.DiceType diceType,
      final String annotation) {
    return randomSource.getRandom(max, annotation);
  }

  @Override
  public GamePlayer getGamePlayer() {
    return attacker;
  }

  @Override
  public IDelegateHistoryWriter getHistoryWriter() {
    return writer;
  }

  @Override
  public IDisplay getDisplayChannelBroadcaster() {
    return display;
  }

  @Override
  public ISound getSoundChannelBroadcaster() {
    return soundChannel;
  }

  @Override
  public void enterDelegateExecution() {}

  @Override
  public void addChange(final Change change) {
    if (change instanceof UnitDamageReceivedChange) {
      allChanges.add(change);
      gameData.performChange(change);
    } else if (change instanceof CompositeChange) {
      ((CompositeChange) change).getChanges().forEach(this::addChange);
    }
  }

  @Override
  public void stopGameSequence(String status, String title) {}

  public void setBattle(final MustFightBattle battle) {
    this.battle = battle;
  }

  @Override
  public IntegerMap<UnitType> getCostsForTuv(final GamePlayer player) {
    return tuvCalculator.getCostsForTuv(player);
  }
}
