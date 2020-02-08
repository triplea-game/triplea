package games.strategy.engine.framework.startup.mc;

import games.strategy.engine.data.GameData;

public interface GameSelector {
  GameData getGameData();

  void onGameEnded();

  static GameSelector fromGameData(final GameData gameData) {
    return new GameSelector() {
      @Override
      public GameData getGameData() {
        return gameData;
      }

      @Override
      public void onGameEnded() {}
    };
  }
}
