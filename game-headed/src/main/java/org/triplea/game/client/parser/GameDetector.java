package org.triplea.game.client.parser;

import games.strategy.engine.framework.ui.GameChooserEntry;
import java.util.Set;
import java.util.function.Consumer;

@FunctionalInterface
public interface GameDetector {
  void discoverGames(Consumer<Set<GameChooserEntry>> uiCreator);
}
