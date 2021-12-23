package games.strategy.engine.framework;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.history.EventChild;
import javax.swing.SwingUtilities;

/**
 * Synchronizes a GameData by listening on the history channel for messages. All modifications to
 * the History are done in the SwingEventThread, so this class can be used to display a history tree
 * to the user.
 */
public class HistorySynchronizer {
  // Note the GameData here and the game are not the same we are keeping gameData in sync with the
  // history of the game
  // by listening for changes we do this because our data can change depending where in the history
  // we are we want to be
  // able to do this without changing the data for the game
  private final GameData gameData;
  private int currentRound;
  private final IGame game;
  private final IGameModifiedChannel gameModifiedChannelListener =
      new IGameModifiedChannel() {
        @Override
        public void gameDataChanged(final Change change) {
          SwingUtilities.invokeLater(
              () -> {
                final Change localizedChange = (Change) translateIntoMyData(change);
                gameData.getHistory().getHistoryWriter().addChange(localizedChange);
              });
        }

        @Override
        public void startHistoryEvent(final String event, final Object renderingData) {
          startHistoryEvent(event);
          if (renderingData != null) {
            setRenderingData(renderingData);
          }
        }

        @Override
        public void startHistoryEvent(final String event) {
          SwingUtilities.invokeLater(
              () -> gameData.getHistory().getHistoryWriter().startEvent(event));
        }

        @Override
        public void addChildToEvent(final String text, final Object renderingData) {
          SwingUtilities.invokeLater(
              () -> {
                final Object translatedRenderingData = translateIntoMyData(renderingData);
                gameData
                    .getHistory()
                    .getHistoryWriter()
                    .addChildToEvent(new EventChild(text, translatedRenderingData));
              });
        }

        void setRenderingData(final Object renderingData) {
          SwingUtilities.invokeLater(
              () -> {
                final Object translatedRenderingData = translateIntoMyData(renderingData);
                gameData.getHistory().getHistoryWriter().setRenderingData(translatedRenderingData);
              });
        }

        @Override
        public void stepChanged(
            final String stepName,
            final String delegateName,
            final GamePlayer player,
            final int round,
            final String displayName,
            final boolean loadedFromSavedGame) {
          // we dont need to advance the game step in this case
          if (loadedFromSavedGame) {
            return;
          }
          SwingUtilities.invokeLater(
              () -> {
                if (currentRound != round) {
                  currentRound = round;
                  gameData.getHistory().getHistoryWriter().startNextRound(currentRound);
                }
                gameData
                    .getHistory()
                    .getHistoryWriter()
                    .startNextStep(stepName, delegateName, player, displayName);
              });
        }

        @Override
        public void shutDown() {}
      };

  public HistorySynchronizer(final GameData data, final IGame game) {
    // this is not the way to use this.
    if (game.getData() == data) {
      throw new IllegalStateException(
          "You dont need a history synchronizer to synchronize game data that is "
              + "managed by an IGame");
    }
    gameData = data;
    gameData.forceChangesOnlyInSwingEventThread();
    data.acquireReadLock();
    try {
      currentRound = data.getSequence().getRound();
    } finally {
      data.releaseReadLock();
    }
    this.game = game;
    this.game
        .getMessengers()
        .registerChannelSubscriber(gameModifiedChannelListener, IGame.GAME_MODIFICATION_CHANNEL);
  }

  public void deactivate() {
    game.getMessengers()
        .unregisterChannelSubscriber(gameModifiedChannelListener, IGame.GAME_MODIFICATION_CHANNEL);
  }

  /**
   * Serializes the object and then deserializes it, resolving object references into gameData. Note
   * the the history we are syncing may refer to a different game data than the GaneData held by the
   * IGame. A clone is made so that we can walk up and down the history without changing the game.
   */
  private Object translateIntoMyData(final Object msg) {
    return GameDataUtils.translateIntoOtherGameData(msg, gameData);
  }
}
