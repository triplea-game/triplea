package games.strategy.engine.framework.startup.mc;

import games.strategy.engine.data.GameData;

public interface GameSelector {
  GameData getGameData();

  void onGameEnded();
}
