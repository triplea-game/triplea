package games.strategy.triplea.ai.proAI.simulate;

import static org.mockito.Mockito.mock;

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
import games.strategy.sound.ISound;
import games.strategy.triplea.ai.proAI.ProAI;
import games.strategy.triplea.delegate.MustFightBattle;
import games.strategy.triplea.ui.display.ITripleADisplay;

public class ProDummyDelegateBridge implements IDelegateBridge {
  private final PlainRandomSource m_randomSource = new PlainRandomSource();
  private final ITripleADisplay m_display = mock(ITripleADisplay.class);
  private final ISound m_soundChannel = mock(ISound.class);
  private final PlayerID m_player;
  private final ProAI m_proAI;
  private final DelegateHistoryWriter m_writer = new DelegateHistoryWriter(new ProDummyGameModifiedChannel());
  private final GameData m_data;
  private final CompositeChange m_allChanges = new CompositeChange();
  private MustFightBattle m_battle = null;

  public ProDummyDelegateBridge(final ProAI proAI, final PlayerID player, final GameData data) {
    m_proAI = proAI;
    m_data = data;
    m_player = player;
  }

  @Override
  public GameData getData() {
    return m_data;
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
    return m_proAI;
  }

  @Override
  public IRemotePlayer getRemotePlayer() {
    return m_proAI;
  }

  @Override
  public int[] getRandom(final int max, final int count, final PlayerID player, final DiceType diceType,
      final String annotation) {
    return m_randomSource.getRandom(max, count, annotation);
  }

  @Override
  public int getRandom(final int max, final PlayerID player, final DiceType diceType, final String annotation) {
    return m_randomSource.getRandom(max, annotation);
  }

  @Override
  public PlayerID getPlayerID() {
    return m_player;
  }

  @Override
  public IDelegateHistoryWriter getHistoryWriter() {
    return m_writer;
  }

  @Override
  public IDisplay getDisplayChannelBroadcaster() {
    return m_display;
  }

  @Override
  public ISound getSoundChannelBroadcaster() {
    return m_soundChannel;
  }

  @Override
  public void enterDelegateExecution() {}

  @Override
  public void addChange(final Change aChange) {
    m_allChanges.add(aChange);
    m_data.performChange(aChange);
  }

  @Override
  public void stopGameSequence() {}

  public MustFightBattle getBattle() {
    return m_battle;
  }

  public void setBattle(final MustFightBattle battle) {
    m_battle = battle;
  }
}
