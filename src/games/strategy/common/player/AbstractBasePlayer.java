package games.strategy.common.player;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.util.ThreadUtil;

/**
 * As a rule, nothing that changes GameData should be in here (it should be in a delegate, and done through an IDelegate
 * using a change).
 */
public abstract class AbstractBasePlayer implements IGamePlayer {
  // what nation are we playing? ex: "Americans"
  private final String m_name;
  // what are we? ex: "Human", or "Moore N. Able (AI)"
  private final String m_type;
  private PlayerID m_playerID;
  private IPlayerBridge m_iPlayerBridge;
  private boolean m_stoppedGame = false;

  /**
   * @param name
   *        - the name of the player.
   */
  public AbstractBasePlayer(final String name, final String type) {
    m_name = name;
    m_type = type;
  }

  /**
   * Anything that overrides this MUST call super.initialize(iPlayerBridge, playerID);
   */
  @Override
  public void initialize(final IPlayerBridge iPlayerBridge, final PlayerID playerID) {
    m_iPlayerBridge = iPlayerBridge;
    m_playerID = playerID;
  }

  /**
   * Get the GameData for the game.
   */
  public GameData getGameData() {
    return m_iPlayerBridge.getGameData();
  }

  /**
   * Get the IPlayerBridge for this game player.
   * (This is not a delegate bridge, and we can not send changes on this. Changes should only be done within a delegate,
   * never through a
   * player.)
   */
  protected final IPlayerBridge getPlayerBridge() {
    return m_iPlayerBridge;
  }

  @Override
  public final String getName() {
    return m_name;
  }

  @Override
  public final String getType() {
    return m_type;
  }

  @Override
  public final PlayerID getPlayerID() {
    return m_playerID;
  }

  @Override
  public String toString() {
    return (m_playerID == null || m_playerID.getName() == null || !m_playerID.getName().equals(m_name))
        ? (m_type + ":" + m_name + ":" + (m_playerID == null ? "NullID" : m_playerID.getName()))
        : (m_type + ":" + m_name);
  }

  /**
   * The given phase has started. We parse the phase name and call the appropriate method.
   */
  @Override
  public void start(final String stepName) {
    if (stepName != null) {
      // PlayerBridge is on a different thread than this one, and so it will be updated asynchronously. Need to wait for
      // it.
      String bridgeStep = getPlayerBridge().getStepName();
      int i = 0;
      boolean shownErrorMessage = false;
      while (!stepName.equals(bridgeStep)) {
        ThreadUtil.sleep(100);
        i++;
        if (i > 30 && !shownErrorMessage) {
          System.out.println("Start step: " + stepName + " does not match player bridge step: " + bridgeStep
              + ". Player Bridge GameOver=" + getPlayerBridge().isGameOver() + ", PlayerID: "
              + getPlayerID().getName() + ", Game: " + getGameData().getGameName()
              + ". Something wrong or very laggy. Will keep trying for 30 more seconds. ");
          shownErrorMessage = true;
        }
        // TODO: what is the right amount of time to wait before we give up?
        if (i > 310) {
          System.err.println("Start step: " + stepName + " still does not match player bridge step: " + bridgeStep
              + " even after waiting more than 30 seconds. This will probably result in a ClassCastException very soon. Player Bridge GameOver="
              + getPlayerBridge().isGameOver() + ", PlayerID: " + getPlayerID().getName() + ", Game: "
              + getGameData().getGameName());
          // getPlayerBridge().printErrorStatus();
          // waited more than 30 seconds, so just let stuff run (an error will pop up surely...)
          break;
        }
        bridgeStep = getPlayerBridge().getStepName();
      }
    }
  }

  @Override
  public void stopGame() {
    m_stoppedGame = true;
  }

  public boolean isGameStopped() {
    return m_stoppedGame;
  }
  // public abstract Class<?> getRemotePlayerType();
}
