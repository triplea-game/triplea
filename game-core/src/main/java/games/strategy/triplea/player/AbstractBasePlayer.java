package games.strategy.triplea.player;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.util.Interruptibles;

/**
 * As a rule, nothing that changes GameData should be in here (it should be in a delegate, and done through an IDelegate
 * using a change).
 */
public abstract class AbstractBasePlayer implements IGamePlayer {

  private final String name; // what nation are we playing? ex: "Americans"
  private final String type; // what are we? ex: "Human" or "AI"
  private PlayerID playerId;
  private IPlayerBridge playerBridge;
  private boolean isStoppedGame = false;

  /**
   * @param name
   *        - the name of the player.
   */
  public AbstractBasePlayer(final String name, final String type) {
    this.name = name;
    this.type = type;
  }

  /**
   * Anything that overrides this MUST call super.initialize(playerBridge, playerId);
   */
  @Override
  public void initialize(final IPlayerBridge playerBridge, final PlayerID playerId) {
    this.playerBridge = playerBridge;
    this.playerId = playerId;
  }

  /**
   * Get the GameData for the game.
   */
  public GameData getGameData() {
    return playerBridge.getGameData();
  }

  /**
   * Get the IPlayerBridge for this game player.
   * (This is not a delegate bridge, and we cannot send changes on this. Changes should only be done within a delegate,
   * never through a
   * player.)
   */
  protected final IPlayerBridge getPlayerBridge() {
    return playerBridge;
  }

  @Override
  public final String getName() {
    return name;
  }

  @Override
  public final String getType() {
    return type;
  }

  @Override
  public final PlayerID getPlayerId() {
    return playerId;
  }

  @Override
  public String toString() {
    return ((playerId == null) || (playerId.getName() == null) || !playerId.getName().equals(name))
        ? (type + ":" + name + ":" + ((playerId == null) ? "NullID" : playerId.getName()))
        : (type + ":" + name);
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
        Interruptibles.sleep(100);
        i++;
        if ((i > 30) && !shownErrorMessage) {
          System.out.println("Start step: " + stepName + " does not match player bridge step: " + bridgeStep
              + ". Player Bridge GameOver=" + getPlayerBridge().isGameOver() + ", PlayerID: " + getPlayerId().getName()
              + ", Game: " + getGameData().getGameName()
              + ". Something wrong or very laggy. Will keep trying for 30 more seconds. ");
          shownErrorMessage = true;
        }
        // TODO: what is the right amount of time to wait before we give up?
        if (i > 310) {
          System.err.println("Start step: " + stepName + " still does not match player bridge step: " + bridgeStep
              + " even after waiting more than 30 seconds. This will probably result in a ClassCastException very "
              + "soon. Player Bridge GameOver=" + getPlayerBridge().isGameOver()
              + ", PlayerID: " + getPlayerId().getName() + ", Game: " + getGameData().getGameName());
          // waited more than 30 seconds, so just let stuff run (an error will pop up surely...)
          break;
        }
        bridgeStep = getPlayerBridge().getStepName();
      }
    }
  }

  @Override
  public void stopGame() {
    isStoppedGame = true;
  }

  public boolean isGameStopped() {
    return isStoppedGame;
  }
}
