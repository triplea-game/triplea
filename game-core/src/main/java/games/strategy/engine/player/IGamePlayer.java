package games.strategy.engine.player;

import games.strategy.engine.data.PlayerId;
import games.strategy.engine.framework.startup.ui.PlayerType;

/**
 * A player of the game.
 *
 * <p>Game players communicate to the game through a PlayerBridge.
 */
public interface IGamePlayer extends IRemotePlayer {
  /** Called before the game starts. */
  void initialize(IPlayerBridge bridge, PlayerId id);

  /** Returns the nation name. */
  String getName();

  PlayerType getPlayerType();

  /**
   * Start the given step. stepName appears as it does in the game xml file. The game step will
   * finish executing when this method returns.
   */
  void start(String stepName);

  /** Called when the game is stopped (like if we are closing the window or leaving the game). */
  void stopGame();
}
