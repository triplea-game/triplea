package games.strategy.engine.framework;

import javax.swing.SwingUtilities;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.history.EventChild;

/**
 * Synchronizes a GameData by listening on the history channel for messages.
 * All modifications to the History are done in the SwingEventThread, so
 * this class can be used to display a history tree to the user.
 */
public class HistorySynchronizer {
  // Note the GameData here and the game are not the same
  // we are keeping m_data in synch with the history of the game by listening
  // for changes
  // we do this because our data can change depending where in the history we
  // are
  // we want to be able to do this without changing the data for the game
  private final GameData m_data;
  private int m_currentRound;
  private final IGame m_game;

  public HistorySynchronizer(final GameData data, final IGame game) {
    // this is not the way to use this.
    if (game.getData() == data) {
      throw new IllegalStateException(
          "You dont need a history synchronizer to synchronize game data that is managed by an IGame");
    }
    m_data = data;
    m_data.forceChangesOnlyInSwingEventThread();
    data.acquireReadLock();
    try {
      m_currentRound = data.getSequence().getRound();
    } finally {
      data.releaseReadLock();
    }
    m_game = game;
    m_game.getChannelMessenger().registerChannelSubscriber(m_gameModifiedChannelListener,
        IGame.GAME_MODIFICATION_CHANNEL);
  }

  private final IGameModifiedChannel m_gameModifiedChannelListener = new IGameModifiedChannel() {
    @Override
    public void gameDataChanged(final Change aChange) {
      SwingUtilities.invokeLater(() -> {
        final Change localizedChange = (Change) translateIntoMyData(aChange);
        m_data.getHistory().getHistoryWriter().addChange(localizedChange);
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
      SwingUtilities.invokeLater(() -> m_data.getHistory().getHistoryWriter().startEvent(event));
    }

    @Override
    public void addChildToEvent(final String text, final Object renderingData) {
      SwingUtilities.invokeLater(() -> {
        final Object translatedRenderingData = translateIntoMyData(renderingData);
        m_data.getHistory().getHistoryWriter().addChildToEvent(new EventChild(text, translatedRenderingData));
      });
    }

    protected void setRenderingData(final Object renderingData) {
      SwingUtilities.invokeLater(() -> {
        final Object translatedRenderingData = translateIntoMyData(renderingData);
        m_data.getHistory().getHistoryWriter().setRenderingData(translatedRenderingData);
      });
    }

    @Override
    public void stepChanged(final String stepName, final String delegateName, final PlayerID player, final int round,
        final String displayName, final boolean loadedFromSavedGame) {
      // we dont need to advance the game step in this case
      if (loadedFromSavedGame) {
        return;
      }
      SwingUtilities.invokeLater(() -> {
        if (m_currentRound != round) {
          m_currentRound = round;
          m_data.getHistory().getHistoryWriter().startNextRound(m_currentRound);
        }
        m_data.getHistory().getHistoryWriter().startNextStep(stepName, delegateName, player, displayName);
      });
    }

    @Override
    public void shutDown() {}
  };

  public void deactivate() {
    m_game.getChannelMessenger().unregisterChannelSubscriber(m_gameModifiedChannelListener,
        IGame.GAME_MODIFICATION_CHANNEL);
  }

  /**
   * Serializes the object and then deserializes it, resolving object
   * references into m_data. Note the the history we are synching may refer to
   * a different game data than the GaneData held by the IGame. A clone is
   * made so that we can walk up and down the history without changing the
   * game.
   */
  private Object translateIntoMyData(final Object msg) {
    return GameDataUtils.translateIntoOtherGameData(msg, m_data);
  }
}
