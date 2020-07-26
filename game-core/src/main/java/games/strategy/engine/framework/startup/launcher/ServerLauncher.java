package games.strategy.engine.framework.startup.launcher;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.framework.startup.mc.ClientModel;
import games.strategy.engine.framework.startup.mc.IClientChannel;
import games.strategy.engine.framework.startup.mc.IObserverWaitingToJoin;
import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.engine.framework.startup.ui.InGameLobbyWatcherWrapper;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import games.strategy.engine.message.ConnectionLostException;
import games.strategy.engine.message.MessengerException;
import games.strategy.engine.player.Player;
import games.strategy.engine.random.CryptoRandomSource;
import games.strategy.net.INode;
import games.strategy.net.Messengers;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.settings.ClientSetting;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import lombok.extern.java.Log;
import org.triplea.java.Interruptibles;
import org.triplea.lobby.common.GameDescription;

/** Implementation of {@link ILauncher} for a headed or headless network server game. */
@Log
public class ServerLauncher implements ILauncher {
  private final GameData gameData;
  private final GameSelectorModel gameSelectorModel;
  private final LaunchAction launchAction;
  private final int clientCount;
  private final Messengers messengers;
  private final PlayerListing playerListing;
  private final Map<String, INode> remotePlayers;
  private final ServerModel serverModel;
  private ServerGame serverGame;
  private ServerReady serverReady;
  private final CountDownLatch errorLatch = new CountDownLatch(1);
  private volatile boolean isLaunching = true;
  private volatile boolean abortLaunch = false;
  private volatile boolean gameStopped = false;
  // a list of observers that tried to join the game during startup
  // we need to track these, because when we lose connections to them we can ignore the connection
  // lost
  private final List<INode> observersThatTriedToJoinDuringStartup =
      Collections.synchronizedList(new ArrayList<>());
  private InGameLobbyWatcherWrapper inGameLobbyWatcher;

  public ServerLauncher(
      final int clientCount,
      final Messengers messengers,
      final GameSelectorModel gameSelectorModel,
      final PlayerListing playerListing,
      final Map<String, INode> remotePlayers,
      final ServerModel serverModel,
      final LaunchAction launchAction) {
    this.gameSelectorModel = gameSelectorModel;
    this.launchAction = launchAction;
    this.clientCount = clientCount;
    this.messengers = messengers;
    this.playerListing = playerListing;
    this.remotePlayers = remotePlayers;
    this.serverModel = serverModel;
    this.gameData = gameSelectorModel.getGameData();
  }

  public void setInGameLobbyWatcher(final InGameLobbyWatcherWrapper watcher) {
    inGameLobbyWatcher = watcher;
  }

  private boolean testShouldWeAbort() {
    if (abortLaunch || gameData == null || serverModel == null) {
      return true;
    }

    final Map<String, String> players = serverModel.getPlayersToNodeListing();
    return players == null
        || players.isEmpty()
        || players.containsValue(null)
        || (serverGame != null
            && serverGame.getPlayerManager() != null
            && serverGame.getPlayerManager().isEmpty());
  }

  @Override
  public void launch() {
    try {
      loadGame();
    } catch (final Exception e) {
      log.log(Level.SEVERE, "Error when loading game", e);
      abortLaunch = true;
    }
    new Thread(this::launchInternal).start();
  }

  private void loadGame() {
    try {
      // the order of this stuff does matter
      serverModel.setServerLauncher(this);
      serverReady = new ServerReady(clientCount);
      if (inGameLobbyWatcher != null) {
        inGameLobbyWatcher.setGameStatus(GameDescription.GameStatus.LAUNCHING, null);
      }
      serverModel.allowRemoveConnections();
      log.info("Game Status: Launching");
      messengers.registerRemote(serverReady, ClientModel.CLIENT_READY_CHANNEL);
      gameData.doPreGameStartDataModifications(playerListing);
      abortLaunch = testShouldWeAbort();
      final byte[] gameDataAsBytes = gameData.toBytes();
      final Set<Player> localPlayerSet =
          gameData.getGameLoader().newPlayers(playerListing.getLocalPlayerTypeMap());
      serverGame =
          new ServerGame(gameData, localPlayerSet, remotePlayers, messengers, launchAction);
      serverGame.setInGameLobbyWatcher(inGameLobbyWatcher);
      launchAction.onLaunch(serverGame);
      // tell the clients to start, later we will wait for them to all signal that they are ready.
      ((IClientChannel) messengers.getChannelBroadcaster(IClientChannel.CHANNEL_NAME))
          .doneSelectingPlayers(gameDataAsBytes, serverGame.getPlayerManager().getPlayerMapping());

      final boolean useSecureRandomSource = !remotePlayers.isEmpty();
      if (useSecureRandomSource) {
        // server game.
        // try to find an opponent to be the other side of the crypto random source.
        final GamePlayer remotePlayer =
            serverGame.getPlayerManager().getRemoteOpponent(messengers.getLocalNode(), gameData);
        final CryptoRandomSource randomSource = new CryptoRandomSource(remotePlayer, serverGame);
        serverGame.setRandomSource(randomSource);
      }
      try {
        gameData
            .getGameLoader()
            .startGame(
                serverGame, localPlayerSet, launchAction, serverModel.getChatModel().getChat());
      } catch (final Exception e) {
        log.log(Level.SEVERE, "Failed to launch", e);
        abortLaunch = true;
      }
      log.info(
          "Game Successfully Loaded. " + (abortLaunch ? "Aborting Launch." : "Starting Game."));
      if (abortLaunch) {
        serverReady.countDownAll();
      }
      if (!serverReady.await(
          ClientSetting.serverStartGameSyncWaitTime.getValueOrThrow(), TimeUnit.SECONDS)) {
        log.warning("Aborting launch - waiting for clients to be ready timed out!");
        abortLaunch = true;
      }
      messengers.unregisterRemote(ClientModel.CLIENT_READY_CHANNEL);
    } finally {
      if (inGameLobbyWatcher != null) {
        inGameLobbyWatcher.setGameStatus(GameDescription.GameStatus.IN_PROGRESS, serverGame);
      }
      log.info("Game Status: In Progress");
    }
  }

  private void launchInternal() {
    try {
      isLaunching = false;
      abortLaunch = testShouldWeAbort();
      if (!abortLaunch) {
        warmUpCryptoRandomSource();
        log.info("Launching game - starting game delegates.");
        serverGame.startGame();
      } else {
        log.info("Aborting game launch");
        stopGame();
      }
    } catch (final RuntimeException e) {
      log.log(Level.INFO, "Exception while launching", e);

      // no-op, this is a simple player disconnect, no need to scare the user with some giant stack
      // trace
      final var cause = e.getCause();
      if (!(cause instanceof ConnectionLostException)) {
        if (cause instanceof MessengerException) {
          // we lost a connection
          // wait for the connection handler to notice, and shut us down
          Interruptibles.await(
              () -> {
                if (!abortLaunch
                    && !errorLatch.await(
                        ClientSetting.serverObserverJoinWaitTime.getValueOrThrow(),
                        TimeUnit.SECONDS)) {
                  log.warning("Waiting on error latch timed out!");
                }
              });
          stopGame();
        } else {
          final String errorMessage =
              "Unrecognized error occurred. If this is a repeatable error, "
                  + "please make a copy of this savegame and report to:\n"
                  + UrlConstants.GITHUB_ISSUES;
          log.log(Level.SEVERE, errorMessage, e);
          stopGame();
        }
      }
    }
    // having an oddball issue with the zip stream being closed while parsing to load default game.
    // might be
    // caused by closing of stream while unloading map resources.
    Interruptibles.sleep(200);
    // either game ended, or aborted, or a player left or disconnected
    launchAction.handleGameInterruption(gameSelectorModel, serverModel);
    serverModel.setServerLauncher(null);
    serverModel.newGame();
    launchAction.onGameInterrupt();
    if (inGameLobbyWatcher != null) {
      log.info("Game Status: Waiting For Players");
      inGameLobbyWatcher.setGameStatus(GameDescription.GameStatus.WAITING_FOR_PLAYERS, null);
    }
  }

  private void warmUpCryptoRandomSource() {
    // the first roll takes a while, initialize here in the background so that the user doesn't
    // notice
    new Thread(
            () -> {
              try {
                serverGame
                    .getRandomSource()
                    .getRandom(gameData.getDiceSides(), 2, "Warming up crypto random source");
              } catch (final RuntimeException e) {
                log.log(Level.SEVERE, "Failed to warm up crypto random source", e);
              }
            },
            "Warming up crypto random source")
        .start();
  }

  public void addObserver(
      final IObserverWaitingToJoin blockingObserver,
      final IObserverWaitingToJoin nonBlockingObserver,
      final INode newNode) {
    if (isLaunching) {
      observersThatTriedToJoinDuringStartup.add(newNode);
      nonBlockingObserver.cannotJoinGame("Game is launching, try again soon");
      return;
    }
    serverGame.addObserver(blockingObserver, nonBlockingObserver, newNode);
  }

  /**
   * Invoked when the connection to the specified node has been lost. Updates the game state
   * appropriately depending on the role of the player associated with the specified node. For
   * example, a disconnected participant will cause the game to be stopped, while a disconnected
   * observer will have no effect.
   */
  public void connectionLost(final INode node) {
    if (isLaunching) {
      // this is expected, we told the observer he couldn't join, so now we lose the connection
      if (observersThatTriedToJoinDuringStartup.remove(node)) {
        return;
      }
      // a player has dropped out, abort
      abortLaunch = true;
      serverReady.countDownAll();
      return;
    }
    // if we lose a connection to a player, shut down the game (after saving) and go back to the
    // main screen
    if (serverGame.getPlayerManager().isPlaying(node)) {
      if (serverGame.isGameSequenceRunning()) {
        saveAndEndGame(node);
      } else {
        stopGame();
      }
      // if the game already exited do to a networking error we need to let them continue
      errorLatch.countDown();
    }
  }

  private void stopGame() {
    if (!gameStopped) {
      gameStopped = true;
      if (serverGame != null) {
        serverGame.stopGame();
      }
    }
  }

  private void saveAndEndGame(final INode node) {
    // a hack, if headless save to the autosave to avoid polluting our savegames folder with a
    // million saves
    final File f = launchAction.getAutoSaveFile();
    try {
      serverGame.saveGame(f);
    } catch (final Exception e) {
      log.log(Level.SEVERE, "Failed to save game: " + f.getAbsolutePath(), e);
    }

    stopGame();

    final String message =
        "Connection lost to:" + node.getName() + " game is over.  Game saved to:" + f.getName();
    launchAction.onEnd(message);
  }

  static class ServerReady implements IServerReady {
    private final CountDownLatch latch;
    private final int clients;

    ServerReady(final int waitCount) {
      clients = waitCount;
      latch = new CountDownLatch(clients);
    }

    @Override
    public void clientReady() {
      latch.countDown();
    }

    void countDownAll() {
      for (int i = 0; i < clients; i++) {
        latch.countDown();
      }
    }

    public boolean await(final long timeout, final TimeUnit timeUnit) {
      return Interruptibles.await(() -> latch.await(timeout, timeUnit));
    }
  }
}
