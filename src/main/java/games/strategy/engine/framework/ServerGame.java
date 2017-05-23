package games.strategy.engine.framework;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import games.strategy.debug.ClientLogger;
import games.strategy.debug.ErrorConsole;
import games.strategy.engine.ClientContext;
import games.strategy.engine.GameOverException;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.PlayerManager;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.AutoSave;
import games.strategy.engine.delegate.DefaultDelegateBridge;
import games.strategy.engine.delegate.DelegateExecutionManager;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.delegate.IPersistentDelegate;
import games.strategy.engine.framework.headlessGameServer.HeadlessGameServer;
import games.strategy.engine.framework.startup.mc.IObserverWaitingToJoin;
import games.strategy.engine.framework.startup.ui.InGameLobbyWatcherWrapper;
import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.engine.framework.ui.SaveGameFileChooser;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.history.DelegateHistoryWriter;
import games.strategy.engine.history.Event;
import games.strategy.engine.history.EventChild;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Step;
import games.strategy.engine.message.ConnectionLostException;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.MessageContext;
import games.strategy.engine.message.RemoteName;
import games.strategy.engine.random.IRandomSource;
import games.strategy.engine.random.IRemoteRandom;
import games.strategy.engine.random.PlainRandomSource;
import games.strategy.engine.random.RandomStats;
import games.strategy.net.INode;
import games.strategy.net.Messengers;
import games.strategy.triplea.TripleAPlayer;
import games.strategy.triplea.delegate.MoveDelegate;

/**
 * Represents a running game.
 * Lookups to get a GamePlayer from PlayerId and the current Delegate.
 */
public class ServerGame extends AbstractGame {
  public static final RemoteName SERVER_REMOTE =
      new RemoteName("games.strategy.engine.framework.ServerGame.SERVER_REMOTE", IServerRemote.class);

  public static final String GAME_HAS_BEEN_SAVED_PROPERTY =
      "games.strategy.engine.framework.ServerGame.GameHasBeenSaved";

  // maps PlayerID->GamePlayer
  private final RandomStats m_randomStats;
  private IRandomSource m_randomSource = new PlainRandomSource();
  private IRandomSource m_delegateRandomSource;
  private final DelegateExecutionManager m_delegateExecutionManager = new DelegateExecutionManager();
  private InGameLobbyWatcherWrapper m_inGameLobbyWatcher;
  private boolean m_needToInitialize = true;
  /**
   * When the delegate execution is stopped, we countdown on this latch to prevent the startgame(...) method from
   * returning.
   */
  private final CountDownLatch m_delegateExecutionStoppedLatch = new CountDownLatch(1);
  /**
   * Has the delegate signaled that delegate execution should stop.
   */
  private volatile boolean m_delegateExecutionStopped = false;

  /**
   * @param data
   *        game data.
   * @param localPlayers
   *        Set - A set of GamePlayers
   * @param remotePlayerMapping
   *        Map
   * @param messengers
   *        IServerMessenger
   */
  public ServerGame(final GameData data, final Set<IGamePlayer> localPlayers,
      final Map<String, INode> remotePlayerMapping, final Messengers messengers) {
    super(data, localPlayers, remotePlayerMapping, messengers);
    m_gameModifiedChannel = new IGameModifiedChannel() {
      @Override
      public void gameDataChanged(final Change aChange) {
        assertCorrectCaller();
        m_data.performChange(aChange);
        m_data.getHistory().getHistoryWriter().addChange(aChange);
      }

      private void assertCorrectCaller() {
        if (!MessageContext.getSender().equals(getMessenger().getServerNode())) {
          throw new IllegalStateException("Only server can change game data");
        }
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
        assertCorrectCaller();
        m_data.getHistory().getHistoryWriter().startEvent(event);
      }

      @Override
      public void addChildToEvent(final String text, final Object renderingData) {
        assertCorrectCaller();
        m_data.getHistory().getHistoryWriter().addChildToEvent(new EventChild(text, renderingData));
      }

      protected void setRenderingData(final Object renderingData) {
        assertCorrectCaller();
        m_data.getHistory().getHistoryWriter().setRenderingData(renderingData);
      }

      @Override
      public void stepChanged(final String stepName, final String delegateName, final PlayerID player, final int round,
          final String displayName, final boolean loadedFromSavedGame) {
        assertCorrectCaller();
        if (loadedFromSavedGame) {
          return;
        }
        m_data.getHistory().getHistoryWriter().startNextStep(stepName, delegateName, player, displayName);
      }

      // nothing to do, we call this
      @Override
      public void shutDown() {}
    };
    m_channelMessenger.registerChannelSubscriber(m_gameModifiedChannel, IGame.GAME_MODIFICATION_CHANNEL);
    setupDelegateMessaging(data);
    m_randomStats = new RandomStats(m_remoteMessenger);
    final IServerRemote m_serverRemote = () -> {
      final ByteArrayOutputStream sink = new ByteArrayOutputStream(5000);
      try {
        saveGame(sink);
      } catch (final IOException e) {
        ClientLogger.logQuietly(e);
        throw new IllegalStateException(e);
      }
      return sink.toByteArray();
    };
    m_remoteMessenger.registerRemote(m_serverRemote, SERVER_REMOTE);
  }

  public void addObserver(final IObserverWaitingToJoin blockingObserver,
      final IObserverWaitingToJoin nonBlockingObserver, final INode newNode) {
    try {
      if (!m_delegateExecutionManager.blockDelegateExecution(2000)) {
        nonBlockingObserver.cannotJoinGame("Could not block delegate execution");
        return;
      }
    } catch (final InterruptedException e) {
      nonBlockingObserver.cannotJoinGame(e.getMessage());
      return;
    }
    try {
      final CountDownLatch waitOnObserver = new CountDownLatch(1);
      final ByteArrayOutputStream sink = new ByteArrayOutputStream(1000);
      saveGame(sink);
      (new Thread(() -> {
        try {
          blockingObserver.joinGame(sink.toByteArray(), m_playerManager.getPlayerMapping());
          waitOnObserver.countDown();
        } catch (final ConnectionLostException cle) {
          System.out.println("Connection lost to observer while joining: " + newNode.getName());
        } catch (final Exception e) {
          ClientLogger.logQuietly(e);
        }
      }, "Waiting on observer to finish joining: " + newNode.getName())).start();
      try {
        if (!waitOnObserver.await(GameRunner.getServerObserverJoinWaitTime(), TimeUnit.SECONDS)) {
          nonBlockingObserver.cannotJoinGame("Taking too long to join.");
        }
      } catch (final InterruptedException e) {
        ClientLogger.logQuietly(e);
        nonBlockingObserver.cannotJoinGame(e.getMessage());
      }
    } catch (final Exception e) {
      ClientLogger.logQuietly(e);
      nonBlockingObserver.cannotJoinGame(e.getMessage());
    } finally {
      m_delegateExecutionManager.resumeDelegateExecution();
    }
  }

  private void setupDelegateMessaging(final GameData data) {
    for (final IDelegate delegate : data.getDelegateList()) {
      addDelegateMessenger(delegate);
    }
  }

  public void addDelegateMessenger(final IDelegate delegate) {
    final Class<? extends IRemote> remoteType = delegate.getRemoteType();
    // if its null then it shouldn't be added as an IRemote
    if (remoteType == null) {
      return;
    }
    final Object wrappedDelegate =
        m_delegateExecutionManager.createInboundImplementation(delegate, new Class<?>[] {delegate.getRemoteType()});
    final RemoteName descriptor = getRemoteName(delegate);
    m_remoteMessenger.registerRemote(wrappedDelegate, descriptor);
  }

  public static RemoteName getRemoteName(final IDelegate delegate) {
    return new RemoteName("games.strategy.engine.framework.ServerGame.DELEGATE_REMOTE." + delegate.getName(),
        delegate.getRemoteType());
  }

  public static RemoteName getRemoteName(final PlayerID id, final GameData data) {
    return new RemoteName("games.strategy.engine.framework.ServerGame.PLAYER_REMOTE." + id.getName(),
        data.getGameLoader().getRemotePlayerType());
  }

  public static RemoteName getRemoteRandomName(final PlayerID id) {
    return new RemoteName("games.strategy.engine.framework.ServerGame.PLAYER_RANDOM_REMOTE" + id.getName(),
        IRemoteRandom.class);
  }

  private GameStep getCurrentStep() {
    return m_data.getSequence().getStep();
    // m_data.getSequence().getStep(m_currentStepIndex);
  }


  /**
   * And here we go.
   * Starts the game in a new thread
   */
  public void startGame() {
    try {
      // we dont want to notify that the step has been saved when reloading a saved game, since
      // in fact the step hasnt changed, we are just resuming where we left off
      final boolean gameHasBeenSaved = m_data.getProperties().get(GAME_HAS_BEEN_SAVED_PROPERTY, false);
      if (!gameHasBeenSaved) {
        m_data.getProperties().set(GAME_HAS_BEEN_SAVED_PROPERTY, Boolean.TRUE);
      }
      startPersistentDelegates();
      if (gameHasBeenSaved) {
        runStep(gameHasBeenSaved);
      }
      while (!m_isGameOver) {
        if (m_delegateExecutionStopped) {
          // the delegate has told us to stop stepping through game steps
          try {
            // dont let this method return, as this method returning signals
            // that the game is over.
            m_delegateExecutionStoppedLatch.await();
          } catch (final InterruptedException e) {
            // ignore
          }
        } else {
          runStep(false);
        }
      }
    } catch (final GameOverException e) {
      if (!m_isGameOver) {
        ClientLogger.logQuietly(e);
      }
    }
  }

  public void stopGame() {
    // we have already shut down
    if (m_isGameOver) {
      System.out.println("Game previously stopped, cannot stop again.");
      return;
    } else if (HeadlessGameServer.headless()) {
      System.out.println("Attempting to stop game.");
    }
    m_isGameOver = true;
    m_delegateExecutionStoppedLatch.countDown();
    // tell the players (especially the AI's) that the game is stopping, so stop doing stuff.
    for (final IGamePlayer player : m_gamePlayers.values()) {
      // not sure whether to put this before or after we delegate execution block, but definitely before the game loader
      // shutdown
      player.stopGame();
    }
    // block delegate execution to prevent outbound messages to the players while we shut down.
    try {
      if (!m_delegateExecutionManager.blockDelegateExecution(16000)) {
        System.err.println("Could not stop delegate execution.");
        if (HeadlessGameServer.getInstance() != null) {
          HeadlessGameServer.getInstance().printThreadDumpsAndStatus();
        } else {
          ErrorConsole.getConsole().dumpStacks();
        }
        // Try one more time
        if (!m_delegateExecutionManager.blockDelegateExecution(16000)) {
          System.err.println("Exiting...");
          System.exit(-1);
        }
      }
    } catch (final InterruptedException e) {
      ClientLogger.logQuietly(e);
    }
    // shutdown
    try {
      m_delegateExecutionManager.setGameOver();
      getGameModifiedBroadcaster().shutDown();
      m_randomStats.shutDown();
      m_channelMessenger.unregisterChannelSubscriber(m_gameModifiedChannel, IGame.GAME_MODIFICATION_CHANNEL);
      m_remoteMessenger.unregisterRemote(SERVER_REMOTE);
      m_vault.shutDown();
      final Iterator<IGamePlayer> localPlayersIter = m_gamePlayers.values().iterator();
      while (localPlayersIter.hasNext()) {
        final IGamePlayer gp = localPlayersIter.next();
        m_remoteMessenger.unregisterRemote(getRemoteName(gp.getPlayerID(), m_data));
      }
      final Iterator<IDelegate> delegateIter = m_data.getDelegateList().iterator();
      while (delegateIter.hasNext()) {
        final IDelegate delegate = delegateIter.next();
        final Class<? extends IRemote> remoteType = delegate.getRemoteType();
        // if its null then it shouldnt be added as an IRemote
        if (remoteType == null) {
          continue;
        }
        m_remoteMessenger.unregisterRemote(getRemoteName(delegate));
      }
    } catch (final RuntimeException e) {
      ClientLogger.logQuietly(e);
    } finally {
      m_delegateExecutionManager.resumeDelegateExecution();
    }
    m_data.getGameLoader().shutDown();
    if (HeadlessGameServer.headless()) {
      System.out.println("StopGame successful.");
    }
  }

  private void autoSave(final String fileName) {
    SaveGameFileChooser.ensureMapsFolderExists();
    final File autoSaveDir = new File(ClientContext.folderSettings().getSaveGamePath()
        + (SystemProperties.isWindows() ? "\\" : "/" + "autoSave"));
    if (!autoSaveDir.exists()) {
      autoSaveDir.mkdirs();
    }
    saveGame(new File(autoSaveDir, fileName));
  }

  private void autoSaveBefore(final IDelegate currentDelegate) {
    final String stepName = currentDelegate.getName();
    autoSave("autosaveBefore" + stepName.substring(0,1).toUpperCase() + stepName.substring(1) + ".tsvg");
  }

  @Override
  public void saveGame(final File f) {
    try (FileOutputStream fout = new FileOutputStream(f)) {
      saveGame(fout);
    } catch (final IOException e) {
      ClientLogger.logQuietly(e);
    }
  }

  private void saveGame(final OutputStream out) throws IOException {
    try {
      if (!m_delegateExecutionManager.blockDelegateExecution(6000)) {
        throw new IOException("Could not lock delegate execution");
      }
    } catch (final InterruptedException ie) {
      throw new IOException(ie.getMessage());
    }
    try {
      new GameDataManager().saveGame(out, m_data);
    } finally {
      m_delegateExecutionManager.resumeDelegateExecution();
    }
  }

  private void runStep(final boolean stepIsRestoredFromSavedGame) {
    if (getCurrentStep().hasReachedMaxRunCount()) {
      m_data.getSequence().next();
      return;
    }
    if (m_isGameOver) {
      return;
    }
    final GameStep currentStep = m_data.getSequence().getStep();
    final IDelegate currentDelegate = currentStep.getDelegate();
    if (!stepIsRestoredFromSavedGame
        && currentDelegate.getClass().isAnnotationPresent(AutoSave.class)
        && currentDelegate.getClass().getAnnotation(AutoSave.class).beforeStepStart()) {
      autoSaveBefore(currentDelegate);
    }
    startStep(stepIsRestoredFromSavedGame);
    if (!stepIsRestoredFromSavedGame
        && currentDelegate.getClass().isAnnotationPresent(AutoSave.class)
        && currentDelegate.getClass().getAnnotation(AutoSave.class).afterStepStart()) {
      autoSaveBefore(currentDelegate);
    }
    if (m_isGameOver) {
      return;
    }
    waitForPlayerToFinishStep();
    if (m_isGameOver) {
      return;
    }
    // save after the step has advanced
    // otherwise, the delegate will execute again.
    final boolean autoSaveThisDelegate = currentDelegate.getClass().isAnnotationPresent(AutoSave.class)
        && currentDelegate.getClass().getAnnotation(AutoSave.class).afterStepEnd();
    if (autoSaveThisDelegate && currentStep.getName().endsWith("Move")) {
      autoSave("autosaveAfter" + currentStep.getName().substring(0,1).toUpperCase()
          + currentStep.getName().substring(1) + ".tsvg");
    }
    endStep();
    if (m_isGameOver) {
      return;
    }
    if (m_data.getSequence().next()) {
      m_data.getHistory().getHistoryWriter().startNextRound(m_data.getSequence().getRound());
      autoSave(m_data.getSequence().getRound() % 2 == 0
          ? SaveGameFileChooser.getAutoSaveEvenFileName() : SaveGameFileChooser.getAutoSaveOddFileName());
    }
    if (autoSaveThisDelegate && !currentStep.getName().endsWith("Move")) {
      final String typeName = currentDelegate.getClass().getTypeName();
      final String phaseName = typeName.substring(typeName.lastIndexOf('.') + 1).replaceFirst("Delegate$","");
      autoSave("autosaveAfter" + phaseName.substring(0,1).toUpperCase() + phaseName.substring(1) + ".tsvg");
    }
  }

  /**
   * @return true if the step should autosave.
   */
  private void endStep() {
    m_delegateExecutionManager.enterDelegateExecution();
    try {
      getCurrentStep().getDelegate().end();
    } finally {
      m_delegateExecutionManager.leaveDelegateExecution();
    }
    getCurrentStep().incrementRunCount();
  }

  private void startPersistentDelegates() {
    final Iterator<IDelegate> delegateIter = m_data.getDelegateList().iterator();
    while (delegateIter.hasNext()) {
      final IDelegate delegate = delegateIter.next();
      if (!(delegate instanceof IPersistentDelegate)) {
        continue;
      }
      final DefaultDelegateBridge bridge = new DefaultDelegateBridge(m_data, this,
          new DelegateHistoryWriter(m_channelMessenger), m_randomStats, m_delegateExecutionManager);
      if (m_delegateRandomSource == null) {
        m_delegateRandomSource = (IRandomSource) m_delegateExecutionManager.createOutboundImplementation(m_randomSource,
            new Class<?>[] {IRandomSource.class});
      }
      bridge.setRandomSource(m_delegateRandomSource);
      m_delegateExecutionManager.enterDelegateExecution();
      try {
        delegate.setDelegateBridgeAndPlayer(bridge);
        delegate.start();
      } finally {
        m_delegateExecutionManager.leaveDelegateExecution();
      }
    }
  }

  private void startStep(final boolean stepIsRestoredFromSavedGame) {
    // dont save if we just loaded
    final DefaultDelegateBridge bridge = new DefaultDelegateBridge(m_data, this,
        new DelegateHistoryWriter(m_channelMessenger), m_randomStats, m_delegateExecutionManager);
    if (m_delegateRandomSource == null) {
      m_delegateRandomSource = (IRandomSource) m_delegateExecutionManager.createOutboundImplementation(m_randomSource,
          new Class<?>[] {IRandomSource.class});
    }
    bridge.setRandomSource(m_delegateRandomSource);
    // do any initialization of game data for all players here (not based on a delegate, and should not be)
    // we cannot do this the very first run through, because there are no history nodes yet. We should do after first
    // node is created.
    if (m_needToInitialize) {
      addPlayerTypesToGameData(m_gamePlayers.values(), m_playerManager, bridge);
    }
    notifyGameStepChanged(stepIsRestoredFromSavedGame);
    m_delegateExecutionManager.enterDelegateExecution();
    try {
      final IDelegate delegate = getCurrentStep().getDelegate();
      delegate.setDelegateBridgeAndPlayer(bridge);
      delegate.start();
    } finally {
      m_delegateExecutionManager.leaveDelegateExecution();
    }
  }

  private void waitForPlayerToFinishStep() {
    final PlayerID playerID = getCurrentStep().getPlayerID();
    // no player specified for the given step
    if (playerID == null) {
      return;
    }
    if (!getCurrentStep().getDelegate().delegateCurrentlyRequiresUserInput()) {
      return;
    }
    final IGamePlayer player = m_gamePlayers.get(playerID);
    if (player != null) {
      // a local player
      player.start(getCurrentStep().getName());
    } else {
      // a remote player
      final INode destination = m_playerManager.getNode(playerID.getName());
      final IGameStepAdvancer advancer =
          (IGameStepAdvancer) m_remoteMessenger.getRemote(ClientGame.getRemoteStepAdvancerName(destination));
      advancer.startPlayerStep(getCurrentStep().getName(), playerID);
    }
  }

  private void notifyGameStepChanged(final boolean loadedFromSavedGame) {
    final GameStep currentStep = getCurrentStep();
    final String stepName = currentStep.getName();
    final String delegateName = currentStep.getDelegate().getName();
    final String displayName = currentStep.getDisplayName();
    final int round = m_data.getSequence().getRound();
    final PlayerID id = currentStep.getPlayerID();
    notifyGameStepListeners(stepName, delegateName, id, round, displayName);
    getGameModifiedBroadcaster().stepChanged(stepName, delegateName, id, round, displayName, loadedFromSavedGame);
  }

  private void addPlayerTypesToGameData(final Collection<IGamePlayer> localPlayers, final PlayerManager allPlayers,
      final IDelegateBridge aBridge) {
    final GameData data = aBridge.getData();
    // potential bugs with adding changes to a game that has not yet started and has no history nodes yet. So wait for
    // the first delegate to
    // start before making changes.
    if (getCurrentStep() == null || getCurrentStep().getPlayerID() == null || (m_firstRun)) {
      m_firstRun = false;
      return;
    }
    // we can't add a new event or add new changes if we are not in a step.
    final HistoryNode curNode = data.getHistory().getLastNode();
    if (!(curNode instanceof Step) && !(curNode instanceof Event) && !(curNode instanceof EventChild)) {
      return;
    }
    final CompositeChange change = new CompositeChange();
    final Set<String> allPlayersString = allPlayers.getPlayers();
    aBridge.getHistoryWriter().startEvent("Game Loaded");
    for (final IGamePlayer player : localPlayers) {
      allPlayersString.remove(player.getName());
      final boolean isHuman = player instanceof TripleAPlayer;
      aBridge.getHistoryWriter()
          .addChildToEvent(
              player.getName()
                  + ((player.getName().endsWith("s") || player.getName().endsWith("ese")
                      || player.getName().endsWith("ish")) ? " are" : " is")
                  + " now being played by: " + player.getType());
      final PlayerID p = data.getPlayerList().getPlayerID(player.getName());
      final String newWhoAmI = ((isHuman ? "Human" : "AI") + ":" + player.getType());
      if (!p.getWhoAmI().equals(newWhoAmI)) {
        change.add(ChangeFactory.changePlayerWhoAmIChange(p, newWhoAmI));
      }
    }
    final Iterator<String> playerIter = allPlayersString.iterator();
    while (playerIter.hasNext()) {
      final String player = playerIter.next();
      playerIter.remove();
      aBridge.getHistoryWriter().addChildToEvent(
          player + ((player.endsWith("s") || player.endsWith("ese") || player.endsWith("ish")) ? " are" : " is")
              + " now being played by: Human:Client");
      final PlayerID p = data.getPlayerList().getPlayerID(player);
      final String newWhoAmI = "Human:Client";
      if (!p.getWhoAmI().equals(newWhoAmI)) {
        change.add(ChangeFactory.changePlayerWhoAmIChange(p, newWhoAmI));
      }
    }
    if (!change.isEmpty()) {
      aBridge.addChange(change);
    }
    m_needToInitialize = false;
    if (!allPlayersString.isEmpty()) {
      throw new IllegalStateException("Not all Player Types (ai/human/client) could be added to game data.");
    }
  }

  private IGameModifiedChannel getGameModifiedBroadcaster() {
    return (IGameModifiedChannel) m_channelMessenger.getChannelBroadcastor(IGame.GAME_MODIFICATION_CHANNEL);
  }

  @Override
  public void addChange(final Change aChange) {
    getGameModifiedBroadcaster().gameDataChanged(aChange);
    // let our channel subscribor do the change,
    // that way all changes will happen in the same thread
  }

  @Override
  public boolean canSave() {
    return true;
  }

  @Override
  public IRandomSource getRandomSource() {
    return m_randomSource;
  }

  public void setRandomSource(final IRandomSource randomSource) {
    m_randomSource = randomSource;
    m_delegateRandomSource = null;
  }

  public InGameLobbyWatcherWrapper getInGameLobbyWatcher() {
    return m_inGameLobbyWatcher;
  }

  public void setInGameLobbyWatcher(final InGameLobbyWatcherWrapper inGameLobbyWatcher) {
    m_inGameLobbyWatcher = inGameLobbyWatcher;
  }

  public void stopGameSequence() {
    m_delegateExecutionStopped = true;
  }

  public boolean isGameSequenceRunning() {
    return !m_delegateExecutionStopped;
  }
}
