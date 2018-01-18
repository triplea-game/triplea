package games.strategy.triplea.ai.proAI.simulate;

import java.util.Properties;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.gamePlayer.IRemotePlayer;
import games.strategy.engine.history.DelegateHistoryWriter;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.engine.random.PlainRandomSource;
import games.strategy.sound.HeadlessSoundChannel;
import games.strategy.sound.ISound;
import games.strategy.triplea.ai.proAI.ProAi;
import games.strategy.triplea.delegate.MustFightBattle;
import games.strategy.triplea.ui.display.HeadlessDisplay;
import games.strategy.triplea.ui.display.ITripleADisplay;

public class ProDummyDelegateBridge implements IDelegateBridge {
  private final PlainRandomSource randomSource = new PlainRandomSource();
  private final ITripleADisplay display = new HeadlessDisplay();
  private final ISound soundChannel = new HeadlessSoundChannel();
  private final PlayerID player;
  private final ProAi proAi;
  private final DelegateHistoryWriter writer = new DelegateHistoryWriter(new ProDummyGameModifiedChannel());
  private final GameData gameData;
  private final CompositeChange allChanges = new CompositeChange();
  private MustFightBattle battle = null;

  public ProDummyDelegateBridge(final ProAi proAi, final PlayerID player, final GameData data) {
    this.proAi = proAi;
    gameData = data;
    this.player = player;
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
    return proAi;
  }

  @Override
  public IRemotePlayer getRemotePlayer() {
    return proAi;
  }

  @Override
  public int[] getRandom(final int max, final int count, final PlayerID player, final DiceType diceType,
      final String annotation) {
    return randomSource.getRandom(max, count, annotation);
  }

  @Override
  public int getRandom(final int max, final PlayerID player, final DiceType diceType, final String annotation) {
    return randomSource.getRandom(max, annotation);
  }

  @Override
  public PlayerID getPlayerId() {
    return player;
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
    allChanges.add(change);
    gameData.performChange(change);
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
