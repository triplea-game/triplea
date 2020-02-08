package org.triplea.game.client.parser;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.framework.ui.GameChooserEntry;
import java.util.function.Consumer;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class DefaultGameLoader extends Service<GameData> implements GameLoader {
  private GameChooserEntry gameChooserEntry;

  @Override
  public void loadGame(
      final GameChooserEntry gameChooserEntry,
      final Consumer<GameData> onLoad,
      final Consumer<Throwable> errorHandler) {
    this.gameChooserEntry = gameChooserEntry;
    reset();
    start();
    setOnSucceeded(value -> onLoad.accept(getValue()));
    setOnFailed(value -> errorHandler.accept(getException()));
  }

  @Override
  protected Task<GameData> createTask() {
    return new Task<>() {
      @Override
      protected GameData call() throws GameParseException {
        return gameChooserEntry
            .getCompleteGameData()
            .orElseThrow(() -> new GameParseException("Missing file!"));
      }
    };
  }
}
