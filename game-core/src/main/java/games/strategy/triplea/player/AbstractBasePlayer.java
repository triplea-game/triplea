package games.strategy.triplea.player;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.player.IPlayerBridge;
import games.strategy.engine.player.Player;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.java.Log;
import org.triplea.java.Interruptibles;

/**
 * As a rule, nothing that changes GameData should be in here (it should be in a delegate, and done
 * through an IDelegate using a change).
 */
@Log
@ToString(exclude = "playerBridge")
public abstract class AbstractBasePlayer implements Player {

  @Getter(onMethod_ = {@Override})
  private final String name; // what nation are we playing? ex: "Americans"

  @Getter(onMethod_ = {@Override})
  private GamePlayer gamePlayer;

  @Getter private IPlayerBridge playerBridge;

  public AbstractBasePlayer(final String name) {
    this.name = name;
  }

  /** Anything that overrides this MUST call super.initialize(playerBridge, playerId); */
  @Override
  public void initialize(final IPlayerBridge playerBridge, final GamePlayer gamePlayer) {
    this.playerBridge = playerBridge;
    this.gamePlayer = gamePlayer;
  }

  /** Get the GameData for the game. */
  public GameData getGameData() {
    return playerBridge.getGameData();
  }

  /** The given phase has started. We parse the phase name and call the appropriate method. */
  @Override
  public void start(final String stepName) {
    if (stepName != null) {
      // PlayerBridge is on a different thread than this one, and so it will be updated
      // asynchronously. Need to wait for
      // it.
      String bridgeStep = getPlayerBridge().getStepName();
      int i = 0;
      while (!stepName.equals(bridgeStep)) {
        Interruptibles.sleep(100);
        i++;
        if (i == 30) {
          log.severe(
              "Start step: "
                  + stepName
                  + " does not match player bridge step: "
                  + bridgeStep
                  + ". Player Bridge GameOver="
                  + getPlayerBridge().isGameOver()
                  + ", PlayerId: "
                  + this.getGamePlayer().getName()
                  + ", Game: "
                  + getGameData().getGameName()
                  + ". Something wrong or very laggy. Will keep trying for 30 more seconds. ");
        }
        if (i > 310) {
          log.severe(
              "Start step: "
                  + stepName
                  + " still does not match player bridge step: "
                  + bridgeStep
                  + " even after waiting more than 30 seconds. This will probably result in a "
                  + "ClassCastException very soon. Player Bridge GameOver="
                  + getPlayerBridge().isGameOver()
                  + ", PlayerId: "
                  + this.getGamePlayer().getName()
                  + ", Game: "
                  + getGameData().getGameName());
          // waited more than 30 seconds, so just let stuff run (an error will pop up surely...)
          break;
        }
        bridgeStep = getPlayerBridge().getStepName();
      }
    }
  }

  @Override
  public void stopGame() {}
}
