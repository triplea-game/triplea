package games.strategy.engine.framework;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameDataEvent;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.history.EventChild;
import games.strategy.engine.message.RemoteName;
import games.strategy.engine.player.Player;
import games.strategy.engine.random.IRandomSource;
import games.strategy.engine.random.IRemoteRandom;
import games.strategy.engine.random.RemoteRandom;
import games.strategy.net.INode;
import games.strategy.net.Messengers;
import games.strategy.net.websocket.ClientNetworkBridge;
import games.strategy.triplea.delegate.battle.casualty.CasualtySelector;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.Interruptibles;

/** Implementation of {@link IGame} for a network client node. */
@Slf4j
public class ClientGame extends AbstractGame {
  public ClientGame(
      final GameData data,
      final Set<Player> gamePlayers,
      final Map<String, INode> remotePlayerMapping,
      final Messengers messengers,
      final ClientNetworkBridge clientNetworkBridge) {
    super(data, gamePlayers, remotePlayerMapping, messengers, clientNetworkBridge);
    gameModifiedChannel =
        new IGameModifiedChannel() {
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
            gameData
                .getHistory()
                .getHistoryWriter()
                .addChildToEvent(new EventChild(text, renderingData));
          }

          private void setRenderingData(final Object renderingData) {
            gameData.getHistory().getHistoryWriter().setRenderingData(renderingData);
          }

          @Override
          public void stepChanged(
              final String stepName,
              final String delegateName,
              final GamePlayer player,
              final int round,
              final String displayName,
              final boolean loadedFromSavedGame) {
            // we want to skip the first iteration, since that simply advances us to step 0
            if (firstRun) {
              firstRun = false;
            } else {
              try (GameData.Unlocker ignored = gameData.acquireWriteLock()) {
                gameData.getSequence().next();
                final int ourOriginalCurrentRound = gameData.getSequence().getRound();
                int currentRound = ourOriginalCurrentRound;
                if (gameData.getSequence().testWeAreOnLastStep()) {
                  gameData.getHistory().getHistoryWriter().startNextRound(++currentRound);
                }
                while (!gameData.getSequence().getStep().getName().equals(stepName)
                    || !gameData.getSequence().getStep().getPlayerId().equals(player)
                    || !gameData
                        .getSequence()
                        .getStep()
                        .getDelegate()
                        .getName()
                        .equals(delegateName)) {
                  gameData.getSequence().next();
                  if (gameData.getSequence().testWeAreOnLastStep()) {
                    gameData.getHistory().getHistoryWriter().startNextRound(++currentRound);
                  }
                }
                // TODO: this is causing problems if the very last step is a client step. we end up
                // creating a new round
                // before the host's rounds has started.
                // right now, fixing it with a hack. but in reality we probably need to have a
                // better way of determining
                // when a new round has started (like with a roundChanged listener).
                if ((currentRound - 1 > round && ourOriginalCurrentRound >= round)
                    || (currentRound > round && ourOriginalCurrentRound < round)) {
                  throw new IllegalStateException(
                      "Cannot create more rounds that host currently has. Host Round:"
                          + round
                          + " and new Client Round:"
                          + currentRound);
                }
              }
            }
            if (!loadedFromSavedGame) {
              gameData
                  .getHistory()
                  .getHistoryWriter()
                  .startNextStep(stepName, delegateName, player, displayName);
            }
            CasualtySelector.clearOolCache();
            gameData.fireGameDataEvent(GameDataEvent.GAME_STEP_CHANGED);
          }

          @Override
          public void shutDown() {
            ClientGame.this.shutDown();
          }
        };
    messengers.registerChannelSubscriber(gameModifiedChannel, IGame.GAME_MODIFICATION_CHANNEL);
    final IGameStepAdvancer gameStepAdvancer =
        (stepName, player) -> {
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
              try (GameData.Unlocker ignored = gameData.acquireReadLock()) {
                if (gameData.getSequence().getStep().getName().equals(stepName) || isGameOver) {
                  break;
                }
              }
              if (!Interruptibles.sleep(100)) {
                break;
              }
              i++;
              if (i > 300 && !shownErrorMessage) {
                log.error("Waited more than 30 seconds for step to update. Something wrong.");
                shownErrorMessage = true;
                // TODO: should we throw an illegal state error? or just return? or a game over
                // exception? should we
                // request the server to send the step update again or something?
              }
            }
          }
          if (isGameOver) {
            return;
          }
          final Player gp = this.gamePlayers.get(player);
          if (gp == null) {
            throw new IllegalStateException(
                "Game player not found. Player: " + player + " on: " + messengers.getLocalNode());
          }
          gp.start(stepName);
        };
    messengers.registerRemote(
        gameStepAdvancer, getRemoteStepAdvancerName(messengers.getLocalNode()));
    for (final GamePlayer player : this.gamePlayers.keySet()) {
      final IRemoteRandom remoteRandom = new RemoteRandom(this);
      messengers.registerRemote(remoteRandom, ServerGame.getRemoteRandomName(player));
    }
  }

  public static RemoteName getRemoteStepAdvancerName(final INode node) {
    return new RemoteName(
        ClientGame.class.getName() + ".REMOTE_STEP_ADVANCER:" + node.getPlayerName(),
        IGameStepAdvancer.class);
  }

  /**
   * Shuts down this client node. May or may not affect the server depending on the role of the
   * player associated with this node.
   */
  public void shutDown() {
    if (isGameOver) {
      return;
    }
    isGameOver = true;
    try {
      messengers.unregisterChannelSubscriber(gameModifiedChannel, IGame.GAME_MODIFICATION_CHANNEL);
      messengers.unregisterRemote(getRemoteStepAdvancerName(messengers.getLocalNode()));
      vault.shutDown();
      for (final Player gp : gamePlayers.values()) {
        final GamePlayer player;
        try (GameData.Unlocker ignored = gameData.acquireReadLock()) {
          player = gameData.getPlayerList().getPlayerId(gp.getName());
        }
        gamePlayers.put(player, gp);
        messengers.unregisterRemote(ServerGame.getRemoteName(gp.getGamePlayer()));
        messengers.unregisterRemote(ServerGame.getRemoteRandomName(player));
      }
    } catch (final RuntimeException e) {
      log.error("Failed to shut down client game", e);
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
  public void saveGame(final Path f) {
    final IServerRemote server = (IServerRemote) messengers.getRemote(ServerGame.SERVER_REMOTE);
    final byte[] bytes = server.getSavedGame();
    try (OutputStream fout = Files.newOutputStream(f)) {
      fout.write(bytes);
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
