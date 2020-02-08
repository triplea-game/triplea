package org.triplea.game.client.parser;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.ui.GameChooserEntry;
import java.util.function.Consumer;

public interface GameLoader {
  void loadGame(
      GameChooserEntry gameChooserEntry,
      Consumer<GameData> onLoad,
      Consumer<Throwable> errorHandler);
}
