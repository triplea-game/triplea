package games.strategy.engine.framework.startup.launcher;

import java.awt.Component;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import games.strategy.debug.ClientLogger;
import games.strategy.debug.DebugUtils;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.framework.GameDataFileUtils;
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
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.util.Interruptibles;

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

  public void signalGameStart(final byte[] bytes) {
    ((IClientChannel) channelMessenger.getChannelBroadcastor(IClientChannel.CHANNEL_NAME))
        .doneSelectingPlayers(bytes, serverGame.getPlayerManager().getPlayerMapping());
  }

  public void setInGameLobbyWatcher(final InGameLobbyWatcherWrapper watcher) {
    inGameLobbyWatcher = watcher;
  }

  private boolean testShouldWeAbort() {
    if (abortLaunch) {
      return true;
    }
    if ((gameData == null) || (serverModel == null)) {
      return true;
    }

    final Map<String, String> players = serverModel.getPlayersToNodeListing();
    if ((players == null) || players.isEmpty() || players.containsValue(null)) {
      return true;
    }

    if ((serverGame != null) && (serverGame.getPlayerManager() != null)) {
      return serverGame.getPlayerManager().isEmpty();
    }
    return false;
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
          gameData.getGameLoader().createPlayers(playerListing.getLocalPlayerTypes());
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
        gameData.getGameLoader().startGame(serverGame, localPlayerSet, headless);
      } catch (final Exception e) {
        ClientLogger.logError("Failed to launch", e);
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
      if (!serverReady.await(ClientSetting.SERVER_START_GAME_SYNC_WAIT_TIME.intValue(), TimeUnit.SECONDS)) {
        System.out.println("Waiting for clients to be ready timed out!");
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
            if (!headless) {
              SwingUtilities.invokeLater(
                  () -> JOptionPane.showMessageDialog(ui, "Problem during startup, game aborted."));
            } else {
              System.out.println("Problem during startup, game aborted.");
            }
          }
        } catch (final MessengerException me) {
          // if just connection lost, no need to scare the user with some giant stack trace
          if (me instanceof ConnectionLostException) {
            System.out.println("Game Player disconnection: " + me.getMessage());
          } else {
            me.printStackTrace(System.out);
          }
          // we lost a connection
          // wait for the connection handler to notice, and shut us down
          try {
            // we are already aborting the launch
            if (!abortLaunch) {
              if (!errorLatch.await(ClientSetting.SERVER_OBSERVER_JOIN_WAIT_TIME.intValue(), TimeUnit.SECONDS)) {
                System.err.println("Waiting on error latch timed out!");
              }
            }
          } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          stopGame();
        } catch (final Exception e) {
          e.printStackTrace(System.err);
          if (headless) {
            System.out.println(DebugUtils.getThreadDumps());
            HeadlessGameServer.sendChat("If this is a repeatable issue or error, please make a copy of this savegame "
                + "and contact a Mod and/or file a bug report.");
          }
          stopGame();
        }
        // having an oddball issue with the zip stream being closed while parsing to load default game. might be
        // caused by closing of stream while unloading map resources.
        Interruptibles.sleep(200);
        // either game ended, or aborted, or a player left or disconnected
        if (headless) {
          try {
            System.out.println("Game ended, going back to waiting.");
            // if we do not do this, we can get into an infinite loop of launching a game,
            // then crashing out, then launching, etc.
            serverModel.setAllPlayersToNullNodes();
            final File f1 = new File(
                ClientSetting.SAVE_GAMES_FOLDER_PATH.value(),
                SaveGameFileChooser.getAutoSaveFileName());
            if (f1.exists()) {
              gameSelectorModel.load(f1, null);
            } else {
              gameSelectorModel.resetGameDataToNull();
            }
          } catch (final Exception e1) {
            ClientLogger.logQuietly("Failed to load game", e1);
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
      } catch (final RuntimeException re) {
        re.printStackTrace(System.out);
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
    // System.out.println("Connection lost to: " + node);
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
    } else {
      // nothing to do
      // we just lost a connection to an observer
      // which is ok.
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
        ? new File(ClientSetting.SAVE_GAMES_FOLDER_PATH.value(), SaveGameFileChooser.getAutoSaveFileName())
        : new File(ClientSetting.SAVE_GAMES_FOLDER_PATH.value(), getConnectionLostFileName());
    try {
      serverGame.saveGame(f);
    } catch (final Exception e) {
      ClientLogger.logQuietly("Failed to save game: " + f.getAbsolutePath(), e);
      if (headless && (HeadlessGameServer.getInstance() != null)) {
        HeadlessGameServer.getInstance().printThreadDumpsAndStatus();
        // TODO: We seem to be getting this bug once a week (1.8.0.1 and previous versions). Trying a fix for 1.8.0.3,
        // need to see if it
        // works.
      }
    }
    stopGame();
    if (!headless) {
      SwingUtilities.invokeLater(() -> {
        final String message =
            "Connection lost to:" + node.getName() + " game is over.  Game saved to:" + f.getName();
        JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(ui), message);
      });
    } else {
      System.out.println("Connection lost to:" + node.getName() + " game is over.  Game saved to:" + f.getName());
    }
  }

  private static String getConnectionLostFileName() {
    return GameDataFileUtils.addExtension(
        "connection_lost_on_" + DateTimeFormatter.ofPattern("MMM_dd_'at'_HH_mm").format(LocalDateTime.now()));
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

