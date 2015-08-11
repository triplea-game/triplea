package games.strategy.engine.data.events;

import games.strategy.engine.data.PlayerID;

public interface GameStepListener {
  public void gameStepChanged(String stepName, String delegateName, PlayerID player, int round, String displayName);
}
