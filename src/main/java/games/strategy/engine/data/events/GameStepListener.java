package games.strategy.engine.data.events;

import games.strategy.engine.data.PlayerID;

public interface GameStepListener {
  void gameStepChanged(String stepName, String delegateName, PlayerID player, int round, String displayName);
}
