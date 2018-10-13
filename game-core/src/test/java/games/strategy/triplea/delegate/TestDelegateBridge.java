package games.strategy.triplea.delegate;

import java.util.Properties;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.gamePlayer.IRemotePlayer;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.sound.ISound;
import games.strategy.triplea.player.ITripleAPlayer;

/**
 * Not for actual use, suitable for testing. Never returns messages, but can get
 * random and implements changes immediately.
 *
 * @deprecated Use mock objects instead. This TestDelegateBridge object has a substantial amount of implementation and
 *             coupling to the rest of the system, do not build on it.
 */
@Deprecated
class TestDelegateBridge implements ITestDelegateBridge {
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
  public PlayerID getPlayerId() {
    return null;
  }

  @Override
  public void addChange(final Change change) {}

  @Override
  public void setStepName(final String name) {
    final GameData gameData = getData();
    gameData.acquireWriteLock();
    try {
      final int length = gameData.getSequence().size();
      int i = 0;
      while (i < length && gameData.getSequence().getStep().getName().indexOf(name) == -1) {
        gameData.getSequence().next();
        i++;
      }
      if (i > +length && gameData.getSequence().getStep().getName().indexOf(name) == -1) {
        throw new IllegalStateException("Step not found: " + name);
      }
    } finally {
      gameData.releaseWriteLock();
    }
  }

  @Override
  public String getStepName() {
    return null;
  }

  @Override
  public IDelegateHistoryWriter getHistoryWriter() {
    return null;
  }

  @Override
  public ITripleAPlayer getRemotePlayer() {
    return null;
  }

  @Override
  public IRemotePlayer getRemotePlayer(final PlayerID id) {
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
  public void leaveDelegateExecution() {}

  @Override
  public void enterDelegateExecution() {}

  @Override
  public void stopGameSequence() {}

  @Override
  public GameData getData() {
    return null;
  }
}
