package games.strategy.engine.framework.startup.launcher;

import java.awt.Component;
import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.headlessGameServer.HeadlessGameServer;
import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.framework.startup.mc.ClientModel;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.startup.mc.IClientChannel;
import games.strategy.engine.framework.startup.mc.IObserverWaitingToJoin;
import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.engine.framework.startup.ui.InGameLobbyWatcherWrapper;
import games.strategy.engine.framework.ui.SaveGameFileChooser;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.lobby.server.GameDescription;
import games.strategy.engine.message.ConnectionLostException;
import games.strategy.engine.message.IChannelMessenger;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.MessengerException;
import games.strategy.engine.random.CryptoRandomSource;
import games.strategy.net.IMessenger;
import games.strategy.net.INode;
import games.strategy.net.Messengers;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.util.Interruptibles;
import lombok.extern.java.Log;

@Log
public class ServerLauncher extends AbstractLauncher {
  private final int clientCount;
  private final IRemoteMessenger remoteMessenger;
  private final IChannelMessenger channelMessenger;
  private final IMessenger messenger;
  private final PlayerListing playerListing;
  private final Map<String, INode> remotePlayers;
  private final ServerModel serverModel;
  private ServerGame serverGame;
  private Component ui;
  private ServerReady serverReady;
  private final CountDownLatch errorLatch = new CountDownLatch(1);
  private volatile boolean isLaunching = true;
  private volatile boolean abortLaunch = false;
  private volatile boolean gameStopped = false;
  // a list of observers that tried to join the game during starup
  // we need to track these, because when we loose connections to them
  // we can ignore the connection lost
  private final List<INode> observersThatTriedToJoinDuringStartup = Collections.synchronizedList(new ArrayList<>());
  private InGameLobbyWatcherWrapper inGameLobbyWatcher;

  public ServerLauncher(final int clientCount, final IRemoteMessenger remoteMessenger,
      final IChannelMessenger channelMessenger, final IMessenger messenger, final GameSelectorModel gameSelectorModel,
      final PlayerListing playerListing, final Map<String, INode> remotePlayers, final ServerModel serverModel,
      final boolean headless) {
    super(gameSelectorModel, headless);
    this.clientCount = clientCount;
    this.remoteMessenger = remoteMessenger;
    this.channelMessenger = channelMessenger;
    this.messenger = messenger;
    this.playerListing = playerListing;
    this.remotePlayers = remotePlayers;
    this.serverModel = serverModel;
  }

  public void setInGameLobbyWatcher(final InGameLobbyWatcherWrapper watcher) {
    inGameLobbyWatcher = watcher;
  }

  private boolean testShouldWeAbort() {
    if (abortLaunch) {
      return true;
    }
    if (gameData == null || serverModel == null) {
      return true;
    }

    final Map<String, String> players = serverModel.getPlayersToNodeListing();
    if (players == null || players.isEmpty() || players.containsValue(null)) {
      return true;
    }

    return serverGame != null && serverGame.getPlayerManager() != null && serverGame.getPlayerManager().isEmpty();
  }

  @Override
  protected void launchInNewThread(final Component parent) {
    try {
      // the order of this stuff does matter
      serverModel.setServerLauncher(this);
      serverReady = new ServerReady(clientCount);
      if (inGameLobbyWatcher != null) {
        inGameLobbyWatcher.setGameStatus(GameDescription.GameStatus.LAUNCHING, null);
      }
      serverModel.allowRemoveConnections();
      ui = parent;
      if (headless) {
        HeadlessGameServer.log("Game Status: Launching");
      }
      remoteMessenger.registerRemote(serverReady, ClientModel.CLIENT_READY_CHANNEL);
      gameData.doPreGameStartDataModifications(playerListing);
      abortLaunch = testShouldWeAbort();
      final byte[] gameDataAsBytes = gameData.toBytes();
      final Set<IGamePlayer> localPlayerSet =
          gameData.getGameLoader().createPlayers(playerListing.getLocalPlayerTypeMap());
      final Messengers messengers = new Messengers(messenger, remoteMessenger, channelMessenger);
      serverGame = new ServerGame(gameData, localPlayerSet, remotePlayers, messengers);
      serverGame.setInGameLobbyWatcher(inGameLobbyWatcher);
      if (headless) {
        HeadlessGameServer.setServerGame(serverGame);
      }
      // tell the clients to start,
      // later we will wait for them to all
      // signal that they are ready.
      ((IClientChannel) channelMessenger.getChannelBroadcastor(IClientChannel.CHANNEL_NAME))
          .doneSelectingPlayers(gameDataAsBytes, serverGame.getPlayerManager().getPlayerMapping());

      final boolean useSecureRandomSource = !remotePlayers.isEmpty();
      if (useSecureRandomSource) {
        // server game.
        // try to find an opponent to be the other side of the crypto random source.
        final PlayerID remotePlayer =
            serverGame.getPlayerManager().getRemoteOpponent(messenger.getLocalNode(), gameData);
        final CryptoRandomSource randomSource = new CryptoRandomSource(remotePlayer, serverGame);
        serverGame.setRandomSource(randomSource);
      }
      try {
        gameData.getGameLoader().startGame(serverGame, localPlayerSet, headless, serverModel.getChatPanel().getChat());
      } catch (final Exception e) {
        log.log(Level.SEVERE, "Failed to launch", e);
        abortLaunch = true;

        if (gameLoadingWindow != null) {
          gameLoadingWindow.doneWait();
        }
      }
      if (headless) {
        HeadlessGameServer.log("Game Successfully Loaded. " + (abortLaunch ? "Aborting Launch." : "Starting Game."));
      }
      if (abortLaunch) {
        serverReady.countDownAll();
      }
      if (!serverReady.await(ClientSetting.serverStartGameSyncWaitTime.intValue(), TimeUnit.SECONDS)) {
        log.warning("Aborting launch - waiting for clients to be ready timed out!");
        abortLaunch = true;
      }
      remoteMessenger.unregisterRemote(ClientModel.CLIENT_READY_CHANNEL);
      new Thread(() -> {
        try {
          isLaunching = false;
          abortLaunch = testShouldWeAbort();
          if (!abortLaunch) {
            if (useSecureRandomSource) {
              warmUpCryptoRandomSource();
            }
            if (gameLoadingWindow != null) {
              gameLoadingWindow.doneWait();
            }
            if (headless) {
              HeadlessGameServer.log("Starting Game Delegates.");
            }
            serverGame.startGame();
          } else {
            stopGame();
          }
        } catch (final ConnectionLostException e) {
          // no-op, this is a simple player disconnect, no need to scare the user with some giant stack trace
        } catch (final MessengerException me) {
          // we lost a connection
          // wait for the connection handler to notice, and shut us down
          Interruptibles.await(() -> {
            if (!abortLaunch
                && !errorLatch.await(ClientSetting.serverObserverJoinWaitTime.intValue(), TimeUnit.SECONDS)) {
              log.warning("Waiting on error latch timed out!");
            }
          });
          stopGame();
        } catch (final RuntimeException e) {
          final String errorMessage = "Unrecognized error occurred: " + e.getMessage() + ", if this is a repeatable "
              + "error please make a copy of this savegame and report to:\n" + UrlConstants.GITHUB_ISSUES;
          log.log(Level.SEVERE, errorMessage, e);
          if (headless) {
            HeadlessGameServer.sendChat(errorMessage);
          }
          stopGame();
        }
        // having an oddball issue with the zip stream being closed while parsing to load default game. might be
        // caused by closing of stream while unloading map resources.
        Interruptibles.sleep(200);
        // either game ended, or aborted, or a player left or disconnected
        if (headless) {
          try {
            log.info("Game ended, going back to waiting.");
            // if we do not do this, we can get into an infinite loop of launching a game,
            // then crashing out, then launching, etc.
            serverModel.setAllPlayersToNullNodes();
            final File f1 = SaveGameFileChooser.getHeadlessAutoSaveFile();
            if (f1.exists()) {
              gameSelectorModel.load(f1);
            } else {
              gameSelectorModel.resetGameDataToNull();
            }
          } catch (final Exception e1) {
            log.log(Level.SEVERE, "Failed to load game", e1);
            gameSelectorModel.resetGameDataToNull();
          }
        } else {
          gameSelectorModel.loadDefaultGameNewThread();
        }
        if (parent != null) {
          SwingUtilities.invokeLater(() -> JOptionPane.getFrameForComponent(parent).setVisible(true));
        }
        serverModel.setServerLauncher(null);
        serverModel.newGame();
        if (inGameLobbyWatcher != null) {
          inGameLobbyWatcher.setGameStatus(GameDescription.GameStatus.WAITING_FOR_PLAYERS, null);
        }
        if (headless) {
          // tell headless server to wait for new connections:
          HeadlessGameServer.waitForUsersHeadlessInstance();
          HeadlessGameServer.log("Game Status: Waiting For Players");
        }
      }, "Triplea, start server game").start();
    } finally {
      if (gameLoadingWindow != null) {
        gameLoadingWindow.doneWait();
      }
      if (inGameLobbyWatcher != null) {
        inGameLobbyWatcher.setGameStatus(GameDescription.GameStatus.IN_PROGRESS, serverGame);
      }
      if (headless) {
        HeadlessGameServer.log("Game Status: In Progress");
      }
    }
  }

  private void warmUpCryptoRandomSource() {
    // the first roll takes a while, initialize
    // here in the background so that the user doesnt notice
    new Thread(() -> {
      try {
        serverGame.getRandomSource().getRandom(gameData.getDiceSides(), 2, "Warming up crypto random source");
      } catch (final RuntimeException e) {
        log.log(Level.SEVERE, "Failed to warm up crypto random source", e);
      }
    }, "Warming up crypto random source").start();
  }

  public void addObserver(final IObserverWaitingToJoin blockingObserver,
      final IObserverWaitingToJoin nonBlockingObserver, final INode newNode) {
    if (isLaunching) {
      observersThatTriedToJoinDuringStartup.add(newNode);
      nonBlockingObserver.cannotJoinGame("Game is launching, try again soon");
      return;
    }
    serverGame.addObserver(blockingObserver, nonBlockingObserver, newNode);
  }

  public void connectionLost(final INode node) {
    if (isLaunching) {
      // this is expected, we told the observer
      // he couldnt join, so now we loose the connection
      if (observersThatTriedToJoinDuringStartup.remove(node)) {
        return;
      }
      // a player has dropped out, abort
      abortLaunch = true;
      serverReady.countDownAll();
      return;
    }
    // if we loose a connection to a player, shut down
    // the game (after saving) and go back to the main screen
    if (serverGame.getPlayerManager().isPlaying(node)) {
      if (serverGame.isGameSequenceRunning()) {
        saveAndEndGame(node);
      } else {
        stopGame();
      }
      // if the game already exited do to a networking error
      // we need to let them continue
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
    // a hack, if headless save to the autosave to avoid polluting our savegames folder with a million saves
    final File f = headless
        ? SaveGameFileChooser.getHeadlessAutoSaveFile()
        : SaveGameFileChooser.getLostConnectionAutoSaveFile(LocalDateTime.now());
    try {
      serverGame.saveGame(f);
    } catch (final Exception e) {
      log.log(Level.SEVERE, "Failed to save game: " + f.getAbsolutePath(), e);
    }

    stopGame();

    final String message = "Connection lost to:" + node.getName() + " game is over.  Game saved to:" + f.getName();
    if (headless) {
      log.info(message);
    } else {
      SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(ui), message));
    }
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

    public void countDownAll() {
      for (int i = 0; i < clients; i++) {
        latch.countDown();
      }
    }

    public boolean await(final long timeout, final TimeUnit timeUnit) {
      boolean didNotTimeOut = false;
      try {
        didNotTimeOut = latch.await(timeout, timeUnit);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      return didNotTimeOut;
    }
  }
}

