package games.strategy.engine.gamePlayer;

import games.strategy.engine.data.PlayerID;

/**
 * A player of the game.
 * <p>
 * Game players communicate to the game through a PlayerBridge.
 */
public interface IGamePlayer extends IRemotePlayer {
  /**
   * Called before the game starts.
   */
  void initialize(IPlayerBridge bridge, PlayerID id);

  /**
   * @return the name of the game player (what nation we are)
   */
  String getName();

  /**
   * @return the type of player we are (human or a kind of ai)
   */
  String getType();

  /**
   * Start the given step. stepName appears as it does in the game xml file.
   * The game step will finish executing when this method returns.
   */
  void start(String stepName);

  /**
   * Called when the game is stopped (like if we are closing the window or leaving the game).
   */
  void stopGame();
  /*
   * (now in superclass)
   *
   * @return the id of this player. This id is initialized by the initialize method in IGamePlayer.
   * public PlayerID getPlayerID();
   */
}
