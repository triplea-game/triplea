package games.strategy.triplea.odds.calculator;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitHitsChange;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.history.DelegateHistoryWriter;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.engine.player.Player;
import games.strategy.engine.random.IRandomStats;
import games.strategy.engine.random.PlainRandomSource;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import games.strategy.triplea.ui.display.HeadlessDisplay;
import java.util.List;
import java.util.Properties;
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
  private final DelegateHistoryWriter writer =
      new DelegateHistoryWriter(new DummyGameModifiedChannel());
  private final CompositeChange allChanges;
  private final GameData gameData;
  private MustFightBattle battle = null;

  public DummyDelegateBridge(
      final GamePlayer attacker,
      final GameData data,
      final CompositeChange allChanges,
      final List<Unit> attackerOrderOfLosses,
      final List<Unit> defenderOrderOfLosses,
      final boolean attackerKeepOneLandUnit,
      final int retreatAfterRound,
      final int retreatAfterXUnitsLeft,
      final boolean retreatWhenOnlyAirLeft) {
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
  }

  @Override
  public GameData getData() {
    return gameData;
  }

  @Override
  public void leaveDelegateExecution() {}

  @Override
  public Properties getStepProperties() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getStepName() {
    throw new UnsupportedOperationException();
  }

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
    if (change instanceof UnitHitsChange) {
      allChanges.add(change);
      gameData.performChange(change);
    } else if (change instanceof CompositeChange) {
      ((CompositeChange) change).getChanges().forEach(this::addChange);
    }
  }

  @Override
  public void stopGameSequence() {}

  public MustFightBattle getBattle() {
    return battle;
  }

  public void setBattle(final MustFightBattle battle) {
    this.battle = battle;
  }
}
