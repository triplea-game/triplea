package games.strategy.engine.framework;

import games.strategy.engine.data.PlayerId;
import games.strategy.engine.message.IRemote;

interface IGameStepAdvancer extends IRemote {
  /**
   * A server calls this methods on client game when a player
   * starts a certain step.
   * The method should not return until the player has finished the step.
   */
  void startPlayerStep(String stepName, PlayerId player);
}
