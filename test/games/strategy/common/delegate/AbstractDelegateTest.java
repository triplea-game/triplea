package games.strategy.common.delegate;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Properties;

import org.junit.Test;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.gamePlayer.IRemotePlayer;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.sound.ISound;

public class AbstractDelegateTest {

  final TestAbstractDelegate delegate = new TestAbstractDelegate();

  @Test
  public void initialize() {
    final String testAbstractDelegateName = "testAbstractDelegateName";
    final String testAbstractDelegateDisplayName = "testAbstractDelegateDisplayName";
    delegate.initialize(testAbstractDelegateName, testAbstractDelegateDisplayName);
    assertThat(delegate.getName(), is(equalTo(testAbstractDelegateName)));
    assertThat(delegate.getDisplayName(), is(equalTo(testAbstractDelegateDisplayName)));
  }

  @Test
  public void setDelegateBridgeAndPlayer() {
    final IDelegateBridge iDelegateBridge = new TestDelegateBridge();
    delegate.setDelegateBridgeAndPlayer(iDelegateBridge);
    assertThat(delegate.getBridge(), is(equalTo(iDelegateBridge)));
    assertThat(delegate.getPlayer(), is(equalTo(iDelegateBridge.getPlayerID())));
  }

  class TestAbstractDelegate extends AbstractDelegate {

    @Override
    public boolean delegateCurrentlyRequiresUserInput() {
      return false;
    }

    @Override
    public Class<? extends IRemote> getRemoteType() {
      return null;
    }

    @Override
    public PlayerID getPlayer() {
      return super.getPlayer();
    }

  }

  class TestDelegateBridge implements IDelegateBridge {

    final PlayerID playerID = PlayerID.NULL_PLAYERID;

    @Override
    public IRemotePlayer getRemotePlayer() {
      return null;
    }

    @Override
    public IRemotePlayer getRemotePlayer(final PlayerID id) {
      return null;
    }

    @Override
    public PlayerID getPlayerID() {
      return playerID;
    }

    @Override
    public String getStepName() {
      return null;
    }

    @Override
    public void addChange(final Change aChange) {

    }

    @Override
    public int getRandom(final int max, final PlayerID player, final DiceType diceType, final String annotation) {
      return 0;
    }

    @Override
    public int[] getRandom(final int max, final int count, final PlayerID player, final DiceType diceType,
        final String annotation) {
      return null;
    }

    @Override
    public IDelegateHistoryWriter getHistoryWriter() {
      return null;
    }

    @Override
    public IDisplay getDisplayChannelBroadcaster() {
      return null;
    }

    @Override
    public ISound getSoundChannelBroadcaster() {
      return null;
    }

    @Override
    public Properties getStepProperties() {
      return null;
    }

    @Override
    public void stopGameSequence() {}

    @Override
    public void leaveDelegateExecution() {}

    @Override
    public void enterDelegateExecution() {}

    @Override
    public GameData getData() {
      return null;
    }

  }
}
