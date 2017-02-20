package games.strategy.engine.framework;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.message.IRemote;

public interface IGameStepAdvancer extends IRemote {
  /**
   * A server calls this methods on client game when a player
   * starts a certain step.
   * The method should not return until the player has finished the step.
   */
  void startPlayerStep(String stepName, PlayerID player);
}
