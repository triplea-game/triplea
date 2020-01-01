package games.strategy.triplea.odds.calculator;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.framework.IGameModifiedChannel;

class DummyGameModifiedChannel implements IGameModifiedChannel {
  @Override
  public void addChildToEvent(final String text, final Object renderingData) {}

  @Override
  public void gameDataChanged(final Change change) {}

  @Override
  public void shutDown() {}

  @Override
  public void startHistoryEvent(final String event) {}

  @Override
  public void startHistoryEvent(final String event, final Object renderingData) {}

  @Override
  public void stepChanged(
      final String stepName,
      final String delegateName,
      final GamePlayer player,
      final int round,
      final String displayName,
      final boolean loadedFromSavedGame) {}
}
