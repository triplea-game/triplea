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
import games.strategy.util.Interruptibles;

public class ClientGame extends AbstractGame {
  public static RemoteName getRemoteStepAdvancerName(final INode node) {
    return new RemoteName(ClientGame.class.getName() + ".REMOTE_STEP_ADVANCER:" + node.getName(),
        IGameStepAdvancer.class);
  }

  public ClientGame(final GameData data, final Set<IGamePlayer> gamePlayers,
      final Map<String, INode> remotePlayerMapping, final Messengers messengers) {
    super(data, gamePlayers, remotePlayerMapping, messengers);
    gameModifiedChannel = new IGameModifiedChannel() {
      @Override
      public void gameDataChanged(final Change change) {
        gameData.performChange(change);
        gameData.getHistory().getHistoryWriter().addChange(change);
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
        gameData.getHistory().getHistoryWriter().startEvent(event);
      }

      @Override
      public void addChildToEvent(final String text, final Object renderingData) {
        gameData.getHistory().getHistoryWriter().addChildToEvent(new EventChild(text, renderingData));
      }

      protected void setRenderingData(final Object renderingData) {
        gameData.getHistory().getHistoryWriter().setRenderingData(renderingData);
      }

      @Override
      public void stepChanged(final String stepName, final String delegateName, final PlayerID player, final int round,
          final String displayName, final boolean loadedFromSavedGame) {
        // we want to skip the first iteration, since that simply advances us to step 0
        if (firstRun) {
          firstRun = false;
        } else {
          gameData.acquireWriteLock();
          try {
            gameData.getSequence().next();
            final int ourOriginalCurrentRound = gameData.getSequence().getRound();
            int currentRound = ourOriginalCurrentRound;
            if (gameData.getSequence().testWeAreOnLastStep()) {
              gameData.getHistory().getHistoryWriter().startNextRound(++currentRound);
            }
            while (!gameData.getSequence().getStep().getName().equals(stepName)
                || !gameData.getSequence().getStep().getPlayerId().equals(player)
                || !gameData.getSequence().getStep().getDelegate().getName().equals(delegateName)) {
              gameData.getSequence().next();
              if (gameData.getSequence().testWeAreOnLastStep()) {
                gameData.getHistory().getHistoryWriter().startNextRound(++currentRound);
              }
            }
            // TODO: this is causing problems if the very last step is a client step. we end up creating a new round
            // before the host's
            // rounds has started.
            // right now, fixing it with a hack. but in reality we probably need to have a better way of determining
            // when a new round has
            // started (like with a roundChanged listener).
            if ((((currentRound - 1) > round) && (ourOriginalCurrentRound >= round))
                || ((currentRound > round) && (ourOriginalCurrentRound < round))) {
              System.err.println("Cannot create more rounds that host currently has. Host Round:" + round
                  + " and new Client Round:" + currentRound);
              throw new IllegalStateException("Cannot create more rounds that host currently has. Host Round:" + round
                  + " and new Client Round:" + currentRound);
            }
          } finally {
            gameData.releaseWriteLock();
          }
        }
        if (!loadedFromSavedGame) {
          gameData.getHistory().getHistoryWriter().startNextStep(stepName, delegateName, player, displayName);
        }
        notifyGameStepListeners(stepName, delegateName, player, round, displayName);
      }

      @Override
      public void shutDown() {
        ClientGame.this.shutDown();
      }
    };
    channelMessenger.registerChannelSubscriber(gameModifiedChannel, IGame.GAME_MODIFICATION_CHANNEL);
    final IGameStepAdvancer gameStepAdvancer = (stepName, player) -> {
      if (isGameOver) {
        return;
      }
      // make sure we are in the correct step
      // steps are advanced on a different channel, and the
      // message advancing the step may be being processed in a different thread
      {
        int i = 0;
        boolean shownErrorMessage = false;
        while (true) {
          gameData.acquireReadLock();
          try {
            if (gameData.getSequence().getStep().getName().equals(stepName) || isGameOver) {
              break;
            }
          } finally {
            gameData.releaseReadLock();
          }
          if (!Interruptibles.sleep(100)) {
            break;
          }
          i++;
          if ((i > 300) && !shownErrorMessage) {
            System.err.println("Waited more than 30 seconds for step to update. Something wrong.");
            shownErrorMessage = true;
            // TODO: should we throw an illegal state error? or just return? or a game over exception? should we
            // request the server to
            // send the step update again or something?
          }
        }
      }
      if (isGameOver) {
        return;
      }
      final IGamePlayer gp = this.gamePlayers.get(player);
      if (gp == null) {
        throw new IllegalStateException(
            "Game player not found. Player:" + player + " on:" + channelMessenger.getLocalNode());
      }
      gp.start(stepName);
    };
    remoteMessenger.registerRemote(gameStepAdvancer, getRemoteStepAdvancerName(channelMessenger.getLocalNode()));
    for (final PlayerID player : this.gamePlayers.keySet()) {
      final IRemoteRandom remoteRandom = new RemoteRandom(this);
      remoteMessenger.registerRemote(remoteRandom, ServerGame.getRemoteRandomName(player));
    }
  }

  public void shutDown() {
    if (isGameOver) {
      return;
    }
    isGameOver = true;
    try {
      channelMessenger.unregisterChannelSubscriber(gameModifiedChannel, IGame.GAME_MODIFICATION_CHANNEL);
      remoteMessenger.unregisterRemote(getRemoteStepAdvancerName(channelMessenger.getLocalNode()));
      vault.shutDown();
      for (final IGamePlayer gp : gamePlayers.values()) {
        PlayerID player;
        gameData.acquireReadLock();
        try {
          player = gameData.getPlayerList().getPlayerId(gp.getName());
        } finally {
          gameData.releaseReadLock();
        }
        gamePlayers.put(player, gp);
        remoteMessenger.unregisterRemote(ServerGame.getRemoteName(gp.getPlayerId(), gameData));
        remoteMessenger.unregisterRemote(ServerGame.getRemoteRandomName(player));
      }
    } catch (final RuntimeException e) {
      ClientLogger.logQuietly("Failed to shut down client game", e);
    }
    gameData.getGameLoader().shutDown();
  }

  @Override
  public void addChange(final Change change) {
    throw new UnsupportedOperationException();
  }

  @Override
  public IRandomSource getRandomSource() {
    return null;
  }

  @Override
  public void saveGame(final File f) {
    final IServerRemote server = (IServerRemote) remoteMessenger.getRemote(ServerGame.SERVER_REMOTE);
    final byte[] bytes = server.getSavedGame();
    try (FileOutputStream fout = new FileOutputStream(f)) {
      fout.write(bytes);
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
