package games.strategy.engine.data.events;

import games.strategy.engine.data.PlayerId;

/**
 * A listener of game step events.
 */
public interface GameStepListener {
  void gameStepChanged(String stepName, String delegateName, PlayerId player, int round, String displayName);
}
