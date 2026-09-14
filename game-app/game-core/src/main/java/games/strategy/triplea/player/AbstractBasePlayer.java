package games.strategy.triplea.player;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.player.Player;
import games.strategy.engine.player.PlayerBridge;
import lombok.Getter;
import lombok.ToString;

/**
 * As a rule, nothing that changes GameData should be in here (it should be in a delegate, and done
 * through an IDelegate using a change).
 */
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

  /**
   * Invoked when the game sequence reaches the given step. Subclasses parse {@code stepName} and
   * dispatch to the matching phase handler; the base implementation is an empty hook.
   */
  @Override
  public void start(final String stepName) {
    // 'ServerGame.waitForPlayerToFinishStep' and 'ClientGame.gameStepAdvancer' advance the game
    // sequence to 'stepName' before calling start(), so the bridge's live read of 'gameData'
    // already reflects this step.
  }

  @Override
  public void stopGame() {}
}
