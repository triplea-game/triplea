package games.strategy.engine.framework;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.history.EventChild;
import games.strategy.engine.message.RemoteName;
import games.strategy.engine.random.IRandomSource;
import games.strategy.engine.random.IRemoteRandom;
import games.strategy.engine.random.RemoteRandom;
import games.strategy.net.INode;
import games.strategy.net.Messengers;
import games.strategy.util.ThreadUtil;

public class ClientGame extends AbstractGame {
  public static RemoteName getRemoteStepAdvancerName(final INode node) {
    return new RemoteName(ClientGame.class.getName() + ".REMOTE_STEP_ADVANCER:" + node.getName(),
        IGameStepAdvancer.class);
  }

  public ClientGame(final GameData data, final Set<IGamePlayer> gamePlayers,
      final Map<String, INode> remotePlayerMapping, final Messengers messengers) {
    super(data, gamePlayers, remotePlayerMapping, messengers);
    m_gameModifiedChannel = new IGameModifiedChannel() {
      @Override
      public void gameDataChanged(final Change aChange) {
        m_data.performChange(aChange);
        m_data.getHistory().getHistoryWriter().addChange(aChange);
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
        m_data.getHistory().getHistoryWriter().startEvent(event);
      }

      @Override
      public void addChildToEvent(final String text, final Object renderingData) {
        m_data.getHistory().getHistoryWriter().addChildToEvent(new EventChild(text, renderingData));
      }

      protected void setRenderingData(final Object renderingData) {
        m_data.getHistory().getHistoryWriter().setRenderingData(renderingData);
      }

      @Override
      public void stepChanged(final String stepName, final String delegateName, final PlayerID player, final int round,
          final String displayName, final boolean loadedFromSavedGame) {
        // we want to skip the first iteration, since that simply advances us to step 0
        if (m_firstRun) {
          m_firstRun = false;
        } else {
          m_data.acquireWriteLock();
          try {
            m_data.getSequence().next();
            final int ourOriginalCurrentRound = m_data.getSequence().getRound();
            int currentRound = ourOriginalCurrentRound;
            if (m_data.getSequence().testWeAreOnLastStep()) {
              m_data.getHistory().getHistoryWriter().startNextRound(++currentRound);
            }
            while (!m_data.getSequence().getStep().getName().equals(stepName)
                || !m_data.getSequence().getStep().getPlayerID().equals(player)
                || !m_data.getSequence().getStep().getDelegate().getName().equals(delegateName)) {
              m_data.getSequence().next();
              if (m_data.getSequence().testWeAreOnLastStep()) {
                m_data.getHistory().getHistoryWriter().startNextRound(++currentRound);
              }
            }
            // TODO: this is causing problems if the very last step is a client step. we end up creating a new round
            // before the host's
            // rounds has started.
            // right now, fixing it with a hack. but in reality we probably need to have a better way of determining
            // when a new round has
            // started (like with a roundChanged listener).
            if ((currentRound - 1 > round && ourOriginalCurrentRound >= round)
                || (currentRound > round && ourOriginalCurrentRound < round)) {
              System.err.println("Cannot create more rounds that host currently has. Host Round:" + round
                  + " and new Client Round:" + currentRound);
              throw new IllegalStateException("Cannot create more rounds that host currently has. Host Round:" + round
                  + " and new Client Round:" + currentRound);
            }
          } finally {
            m_data.releaseWriteLock();
          }
        }
        if (!loadedFromSavedGame) {
          m_data.getHistory().getHistoryWriter().startNextStep(stepName, delegateName, player, displayName);
        }
        notifyGameStepListeners(stepName, delegateName, player, round, displayName);
      }

      @Override
      public void shutDown() {
        ClientGame.this.shutDown();
      }
    };
    m_channelMessenger.registerChannelSubscriber(m_gameModifiedChannel, IGame.GAME_MODIFICATION_CHANNEL);
    IGameStepAdvancer m_gameStepAdvancer = new IGameStepAdvancer() {
      @Override
      public void startPlayerStep(final String stepName, final PlayerID player) {
        if (m_isGameOver) {
          return;
        }
        // make sure we are in the correct step
        // steps are advanced on a different channel, and the
        // message advancing the step may be being processed in a different thread
        {
          int i = 0;
          boolean shownErrorMessage = false;
          while (true) {
            m_data.acquireReadLock();
            try {
              if (m_data.getSequence().getStep().getName().equals(stepName) || m_isGameOver) {
                break;
              }
            } finally {
              m_data.releaseReadLock();
            }
            ThreadUtil.sleep(100);
            i++;
            if (i > 300 && !shownErrorMessage) {
              System.err.println("Waited more than 30 seconds for step to update. Something wrong.");
              shownErrorMessage = true;
              // TODO: should we throw an illegal state error? or just return? or a game over exception? should we
              // request the server to
              // send the step update again or something?
            }
          }
        }
        if (m_isGameOver) {
          return;
        }
        final IGamePlayer gp = m_gamePlayers.get(player);
        if (gp == null) {
          throw new IllegalStateException(
              "Game player not found. Player:" + player + " on:" + m_channelMessenger.getLocalNode());
        }
        gp.start(stepName);
      }
    };
    m_remoteMessenger.registerRemote(m_gameStepAdvancer, getRemoteStepAdvancerName(m_channelMessenger.getLocalNode()));
    for (final PlayerID player : m_gamePlayers.keySet()) {
      final IRemoteRandom remoteRandom = new RemoteRandom(this);
      m_remoteMessenger.registerRemote(remoteRandom, ServerGame.getRemoteRandomName(player));
    }
  }

  public void shutDown() {
    if (m_isGameOver) {
      return;
    }
    m_isGameOver = true;
    try {
      m_channelMessenger.unregisterChannelSubscriber(m_gameModifiedChannel, IGame.GAME_MODIFICATION_CHANNEL);
      m_remoteMessenger.unregisterRemote(getRemoteStepAdvancerName(m_channelMessenger.getLocalNode()));
      m_vault.shutDown();
      for (final IGamePlayer gp : m_gamePlayers.values()) {
        PlayerID player;
        m_data.acquireReadLock();
        try {
          player = m_data.getPlayerList().getPlayerID(gp.getName());
        } finally {
          m_data.releaseReadLock();
        }
        m_gamePlayers.put(player, gp);
        m_remoteMessenger.unregisterRemote(ServerGame.getRemoteName(gp.getPlayerID(), m_data));
        m_remoteMessenger.unregisterRemote(ServerGame.getRemoteRandomName(player));
      }
    } catch (final RuntimeException e) {
      ClientLogger.logQuietly(e);
    }
    m_data.getGameLoader().shutDown();
  }

  @Override
  public void addChange(final Change aChange) {
    throw new UnsupportedOperationException();
  }

  /**
   * Clients cant save because they do not have the delegate data.
   * It would be easy to get the server to save the game, and send the
   * data to the client, I just havent bothered.
   */
  @Override
  public boolean canSave() {
    return false;
  }

  @Override
  public IRandomSource getRandomSource() {
    return null;
  }

  @Override
  public void saveGame(final File f) {
    final IServerRemote server = (IServerRemote) m_remoteMessenger.getRemote(ServerGame.SERVER_REMOTE);
    final byte[] bytes = server.getSavedGame();
    try (FileOutputStream fout = new FileOutputStream(f)) {
      fout.write(bytes);
    } catch (final IOException e) {
      ClientLogger.logQuietly(e);
      throw new IllegalStateException(e.getMessage());
    }
  }
}
