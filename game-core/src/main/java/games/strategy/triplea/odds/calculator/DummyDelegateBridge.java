package games.strategy.triplea.odds.calculator;

import java.util.List;
import java.util.Properties;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitHitsChange;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.history.DelegateHistoryWriter;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.engine.player.IRemotePlayer;
import games.strategy.engine.random.IRandomStats;
import games.strategy.engine.random.PlainRandomSource;
import games.strategy.sound.HeadlessSoundChannel;
import games.strategy.sound.ISound;
import games.strategy.triplea.delegate.MustFightBattle;
import games.strategy.triplea.ui.display.HeadlessDisplay;
import games.strategy.triplea.ui.display.ITripleADisplay;

/**
 * Delegate bridge implementation with minimum valid behavior.
 */
public class DummyDelegateBridge implements IDelegateBridge {
  private final PlainRandomSource randomSource = new PlainRandomSource();
  private final ITripleADisplay display = new HeadlessDisplay();
  private final ISound soundChannel = new HeadlessSoundChannel();
  private final DummyPlayer attackingPlayer;
  private final DummyPlayer defendingPlayer;
  private final PlayerID attacker;
  private final DelegateHistoryWriter writer = new DelegateHistoryWriter(new DummyGameModifiedChannel());
  private final CompositeChange allChanges;
  private final GameData gameData;
  private MustFightBattle battle = null;

  public DummyDelegateBridge(final PlayerID attacker, final GameData data, final CompositeChange allChanges,
      final List<Unit> attackerOrderOfLosses, final List<Unit> defenderOrderOfLosses,
      final boolean attackerKeepOneLandUnit, final int retreatAfterRound, final int retreatAfterXUnitsLeft,
      final boolean retreatWhenOnlyAirLeft) {
    attackingPlayer = new DummyPlayer(this, true, "battle calc dummy", attackerOrderOfLosses,
        attackerKeepOneLandUnit, retreatAfterRound, retreatAfterXUnitsLeft, retreatWhenOnlyAirLeft);
    defendingPlayer = new DummyPlayer(this, false, "battle calc dummy", defenderOrderOfLosses, false,
        retreatAfterRound, -1, false);
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
  public IRemotePlayer getRemotePlayer(final PlayerID id) {
    return id.equals(attacker) ? attackingPlayer : defendingPlayer;
  }

  @Override
  public IRemotePlayer getRemotePlayer() {
    // the current player is attacker
    return attackingPlayer;
  }

  @Override
  public int[] getRandom(final int max, final int count, final PlayerID player, final IRandomStats.DiceType diceType,
      final String annotation) {
    return randomSource.getRandom(max, count, annotation);
  }

  @Override
  public int getRandom(
      final int max,
      final PlayerID player,
      final IRandomStats.DiceType diceType,
      final String annotation) {
    return randomSource.getRandom(max, annotation);
  }

  @Override
  public PlayerID getPlayerId() {
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

