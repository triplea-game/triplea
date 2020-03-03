package org.triplea.game.client.parser;

import games.strategy.engine.framework.ui.GameChooserEntry;
import games.strategy.engine.framework.ui.GameChooserModel;
import java.util.Set;
import java.util.function.Consumer;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class DefaultGameDetector extends Service<Set<GameChooserEntry>> implements GameDetector {

  @Override
  public void discoverGames(final Consumer<Set<GameChooserEntry>> uiCreator) {
    start();
    setOnSucceeded(event -> uiCreator.accept(getValue()));
    setOnFailed(event -> uiCreator.accept(Set.of()));
  }

  @Override
  protected Task<Set<GameChooserEntry>> createTask() {
    return new Task<>() {
      @Override
      protected Set<GameChooserEntry> call() {
        return GameChooserModel.parseMapFiles();
      }
    };
  }
}
