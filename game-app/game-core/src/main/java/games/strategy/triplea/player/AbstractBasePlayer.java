package games.strategy.triplea.player;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.player.Player;
import games.strategy.engine.player.PlayerBridge;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * As a rule, nothing that changes GameData should be in here (it should be in a delegate, and done
 * through an IDelegate using a change).
 */
@Slf4j
@ToString(exclude = "playerBridge")
public abstract class AbstractBasePlayer implements Player {

  @Getter(onMethod_ = {@Override})
  private final String name; // what nation are we playing? ex: "Americans"

  private final String playerLabel;

  @Getter(onMethod_ = {@Override})
  private GamePlayer gamePlayer;

  @Getter private PlayerBridge playerBridge;

  public AbstractBasePlayer(final String name, final String playerLabel) {
    this.name = name;
    this.playerLabel = playerLabel;
  }

  /** Anything that overrides this MUST call super.initialize(playerBridge, playerId); */
  @Override
  public void initialize(final PlayerBridge playerBridge, final GamePlayer gamePlayer) {
    this.playerBridge = playerBridge;
    this.gamePlayer = gamePlayer;
  }

  @Override
  public String getPlayerLabel() {
    return playerLabel;
  }

  /** Get the GameData for the game. */
  public GameData getGameData() {
    return playerBridge.getGameData();
  }

  /** The given phase has started. We parse the phase name and call the appropriate method. */
  @Override
  public void start(final String stepName) {
    // The caller (ServerGame.waitForPlayerToFinishStep / ClientGame.gameStepAdvancer) only
    // invokes start() once the game sequence has already advanced to stepName, so the bridge's
    // live view of gameData is guaranteed to be in sync. No wait is needed here.
  }

  @Override
  public void stopGame() {}
}
