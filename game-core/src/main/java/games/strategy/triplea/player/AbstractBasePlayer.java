package games.strategy.triplea.player;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.util.Interruptibles;
import lombok.Getter;
import lombok.ToString;

/**
 * As a rule, nothing that changes GameData should be in here (it should be in a delegate, and done through an IDelegate
 * using a change).
 */
@ToString(exclude = "playerBridge")
public abstract class AbstractBasePlayer implements IGamePlayer {

  @Getter
  private final String name; // what nation are we playing? ex: "Americans"
  @Getter
  private PlayerID playerId;
  @Getter
  private IPlayerBridge playerBridge;
  private boolean isStoppedGame = false;


  public AbstractBasePlayer(final String name) {
    this.name = name;
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
        if (i > 30 && !shownErrorMessage) {
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
}
