/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
/*
 * Game.java
 * 
 * Created on October 27, 2001, 6:39 PM
 */
package games.strategy.engine.framework;

import games.strategy.common.ui.InGameLobbyWatcherWrapper;
import games.strategy.debug.Console;
import games.strategy.engine.GameOverException;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.PlayerManager;
import games.strategy.engine.delegate.AutoSave;
import games.strategy.engine.delegate.DefaultDelegateBridge;
import games.strategy.engine.delegate.DelegateExecutionManager;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.delegate.IPersistentDelegate;
import games.strategy.engine.framework.startup.mc.IObserverWaitingToJoin;
import games.strategy.engine.framework.ui.SaveGameFileChooser;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.history.DelegateHistoryWriter;
import games.strategy.engine.history.Event;
import games.strategy.engine.history.EventChild;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Step;
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
import games.strategy.triplea.ai.Dynamix_AI.CommandCenter.CachedInstanceCenter;
import games.strategy.triplea.ui.ErrorHandler;

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

/**
 * 
 * @author Sean Bridges
 * 
 *         Represents a running game.
 *         Lookups to get a GamePlayer from PlayerId and the current Delegate.
 */
public class ServerGame extends AbstractGame
{
	public static final RemoteName SERVER_REMOTE = new RemoteName("games.strategy.engine.framework.ServerGame.SERVER_REMOTE", IServerRemote.class);
	// maps PlayerID->GamePlayer
	private final RandomStats m_randomStats;
	private IRandomSource m_randomSource = new PlainRandomSource();
	private IRandomSource m_delegateRandomSource;
	private final DelegateExecutionManager m_delegateExecutionManager = new DelegateExecutionManager();
	private InGameLobbyWatcherWrapper m_inGameLobbyWatcher;
	private boolean m_needToInitialize = true;
	/**
	 * When the delegate execution is stopped, we countdown on this latch to prevent the startgame(...) method from returning.
	 * <p>
	 */
	private final CountDownLatch m_delegateExecutionStoppedLatch = new CountDownLatch(1);
	/**
	 * Has the delegate signaled that delegate execution should stop.
	 */
	private volatile boolean m_delegateExecutionStopped = false;
	private final IServerRemote m_serverRemote = new IServerRemote()
	{
		public byte[] getSavedGame()
		{
			final ByteArrayOutputStream sink = new ByteArrayOutputStream(5000);
			try
			{
				saveGame(sink);
			} catch (final IOException e)
			{
				e.printStackTrace();
				throw new IllegalStateException(e);
			}
			return sink.toByteArray();
		}
	};
	
	/**
	 * 
	 * @param data
	 *            game data
	 * @param localPlayers
	 *            Set - A set of GamePlayers
	 * @param remotePlayerMapping
	 *            Map
	 * @param messengers
	 *            IServerMessenger
	 */
	public ServerGame(final GameData data, final Set<IGamePlayer> localPlayers, final Map<String, INode> remotePlayerMapping, final Messengers messengers)
	{
		super(data, localPlayers, remotePlayerMapping, messengers);
		m_gameModifiedChannel = new IGameModifiedChannel()
		{
			public void gameDataChanged(final Change aChange)
			{
				assertCorrectCaller();
				m_changePerformer.perform(aChange);
				m_data.getHistory().getHistoryWriter().addChange(aChange);
			}
			
			private void assertCorrectCaller()
			{
				if (!MessageContext.getSender().equals(getMessenger().getServerNode()))
				{
					throw new IllegalStateException("Only server can change game data");
				}
			}
			
			public void startHistoryEvent(final String event, final Object renderingData)
			{
				startHistoryEvent(event);
				if (renderingData != null)
					setRenderingData(renderingData);
			}
			
			public void startHistoryEvent(final String event)
			{
				assertCorrectCaller();
				m_data.getHistory().getHistoryWriter().startEvent(event);
			}
			
			public void addChildToEvent(final String text, final Object renderingData)
			{
				assertCorrectCaller();
				m_data.getHistory().getHistoryWriter().addChildToEvent(new EventChild(text, renderingData));
			}
			
			protected void setRenderingData(final Object renderingData)
			{
				assertCorrectCaller();
				m_data.getHistory().getHistoryWriter().setRenderingData(renderingData);
			}
			
			public void stepChanged(final String stepName, final String delegateName, final PlayerID player, final int round, final String displayName, final boolean loadedFromSavedGame)
			{
				assertCorrectCaller();
				if (loadedFromSavedGame)
					return;
				m_data.getHistory().getHistoryWriter().startNextStep(stepName, delegateName, player, displayName);
			}
			
			// nothing to do, we call this
			public void shutDown()
			{
			}
		};
		m_channelMessenger.registerChannelSubscriber(m_gameModifiedChannel, IGame.GAME_MODIFICATION_CHANNEL);
		CachedInstanceCenter.CachedGameData = data;
		setupDelegateMessaging(data);
		m_randomStats = new RandomStats(m_remoteMessenger);
		m_remoteMessenger.registerRemote(m_serverRemote, SERVER_REMOTE);
	}
	
	public void addObserver(final IObserverWaitingToJoin observer)
	{
		try
		{
			if (!m_delegateExecutionManager.blockDelegateExecution(2000))
			{
				observer.cannotJoinGame("Could not block delegate execution");
				return;
			}
		} catch (final InterruptedException e)
		{
			observer.cannotJoinGame(e.getMessage());
			return;
		}
		try
		{
			final ByteArrayOutputStream sink = new ByteArrayOutputStream(1000);
			saveGame(sink);
			observer.joinGame(sink.toByteArray(), m_playerManager.getPlayerMapping());
		} catch (final IOException ioe)
		{
			observer.cannotJoinGame(ioe.getMessage());
			return;
		} finally
		{
			m_delegateExecutionManager.resumeDelegateExecution();
		}
	}
	
	private void setupDelegateMessaging(final GameData data)
	{
		for (final IDelegate delegate : data.getDelegateList())
		{
			addDelegateMessenger(delegate);
		}
	}
	
	public void addDelegateMessenger(final IDelegate delegate)
	{
		final Class<? extends IRemote> remoteType = delegate.getRemoteType();
		// if its null then it shouldn't be added as an IRemote
		if (remoteType == null)
			return;
		final Object wrappedDelegate = m_delegateExecutionManager.createInboundImplementation(delegate, new Class[] { delegate.getRemoteType() });
		final RemoteName descriptor = getRemoteName(delegate);
		m_remoteMessenger.registerRemote(wrappedDelegate, descriptor);
	}
	
	public static RemoteName getRemoteName(final IDelegate delegate)
	{
		return new RemoteName("games.strategy.engine.framework.ServerGame.DELEGATE_REMOTE." + delegate.getName(), delegate.getRemoteType());
	}
	
	public static RemoteName getRemoteName(final PlayerID id, final GameData data)
	{
		return new RemoteName("games.strategy.engine.framework.ServerGame.PLAYER_REMOTE." + id.getName(), data.getGameLoader().getRemotePlayerType());
	}
	
	public static RemoteName getRemoteRandomName(final PlayerID id)
	{
		return new RemoteName("games.strategy.engine.framework.ServerGame.PLAYER_RANDOM_REMOTE" + id.getName(), IRemoteRandom.class);
	}
	
	private GameStep getCurrentStep()
	{
		return m_data.getSequence().getStep();
		// m_data.getSequence().getStep(m_currentStepIndex);
	}
	
	private final static String GAME_HAS_BEEN_SAVED_PROPERTY = "games.strategy.engine.framework.ServerGame.GameHasBeenSaved";
	
	/**
	 * And here we go.
	 * Starts the game in a new thread
	 */
	public void startGame()
	{
		try
		{
			// we dont want to notify that the step has been saved when reloading a saved game, since
			// in fact the step hasnt changed, we are just resuming where we left off
			final boolean gameHasBeenSaved = m_data.getProperties().get(GAME_HAS_BEEN_SAVED_PROPERTY, false);
			if (!gameHasBeenSaved)
				m_data.getProperties().set(GAME_HAS_BEEN_SAVED_PROPERTY, Boolean.TRUE);
			startPersistentDelegates();
			if (gameHasBeenSaved)
			{
				runStep(gameHasBeenSaved);
			}
			while (!m_isGameOver)
			{
				if (m_delegateExecutionStopped)
				{
					// the delegate has told us to stop stepping through game steps
					try
					{
						// dont let this method return, as this method returning signals
						// that the game is over.
						m_delegateExecutionStoppedLatch.await();
					} catch (final InterruptedException e)
					{
						// ignore
					}
				}
				else
				{
					runStep(false);
				}
			}
		} catch (final GameOverException goe)
		{
			if (!m_isGameOver)
				goe.printStackTrace();
			return;
		}
	}
	
	public void stopGame()
	{
		stopGame(false);
	}
	
	public void stopGame(final boolean forceRetry)
	{
		// we have already shut down
		if (m_isGameOver)
		{
			if (HeadlessGameServer.headless())
				System.out.println("Game previously stopped." + (forceRetry ? " Forcing re-try re-stop." : " Can not stop again."));
			if (!forceRetry)
				return;
		}
		else if (HeadlessGameServer.headless())
		{
			System.out.println("Attempting to stop game.");
		}
		m_isGameOver = true;
		ErrorHandler.setGameOver(true);
		m_delegateExecutionStoppedLatch.countDown();
		// block delegate execution to prevent outbound messages to the players
		// while we shut down.
		try
		{
			if (!m_delegateExecutionManager.blockDelegateExecution(4000))
			{
				System.err.println("Could not stop delegate execution.");
				if (HeadlessGameServer.headless())
					System.out.println(games.strategy.debug.DebugUtils.getThreadDumps());
				else
					Console.getConsole().dumpStacks();
				System.exit(0);
			}
		} catch (final InterruptedException e)
		{
			e.printStackTrace();
		}
		// shutdown
		try
		{
			m_delegateExecutionManager.setGameOver();
			getGameModifiedBroadcaster().shutDown();
			m_randomStats.shutDown();
			m_channelMessenger.unregisterChannelSubscriber(m_gameModifiedChannel, IGame.GAME_MODIFICATION_CHANNEL);
			m_remoteMessenger.unregisterRemote(SERVER_REMOTE);
			m_vault.shutDown();
			final Iterator<IGamePlayer> localPlayersIter = m_gamePlayers.values().iterator();
			while (localPlayersIter.hasNext())
			{
				final IGamePlayer gp = localPlayersIter.next();
				m_remoteMessenger.unregisterRemote(getRemoteName(gp.getPlayerID(), m_data));
			}
			final Iterator<IDelegate> delegateIter = m_data.getDelegateList().iterator();
			while (delegateIter.hasNext())
			{
				final IDelegate delegate = delegateIter.next();
				final Class<? extends IRemote> remoteType = delegate.getRemoteType();
				// if its null then it shouldnt be added as an IRemote
				if (remoteType == null)
					continue;
				m_remoteMessenger.unregisterRemote(getRemoteName(delegate));
			}
		} catch (final RuntimeException re)
		{
			re.printStackTrace();
		} finally
		{
			m_delegateExecutionManager.resumeDelegateExecution();
		}
		m_data.getGameLoader().shutDown();
		if (HeadlessGameServer.headless())
		{
			System.out.println("StopGame successful.");
		}
	}
	
	private void autoSave()
	{
		FileOutputStream out = null;
		try
		{
			SaveGameFileChooser.ensureDefaultDirExists();
			final File autosaveFile = new File(SaveGameFileChooser.DEFAULT_DIRECTORY, SaveGameFileChooser.getAutoSaveFileName());
			out = new FileOutputStream(autosaveFile);
			saveGame(out);
		} catch (final Exception e)
		{
			e.printStackTrace();
		} finally
		{
			try
			{
				if (out != null)
					out.close();
			} catch (final IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	private void autoSaveRound()
	{
		FileOutputStream out = null;
		try
		{
			SaveGameFileChooser.ensureDefaultDirExists();
			File autosaveFile;
			if (m_data.getSequence().getRound() % 2 == 0)
				autosaveFile = new File(SaveGameFileChooser.DEFAULT_DIRECTORY, SaveGameFileChooser.getAutoSaveEvenFileName());
			else
				autosaveFile = new File(SaveGameFileChooser.DEFAULT_DIRECTORY, SaveGameFileChooser.getAutoSaveOddFileName());
			out = new FileOutputStream(autosaveFile);
			saveGame(out);
		} catch (final Exception e)
		{
			e.printStackTrace();
		} finally
		{
			try
			{
				if (out != null)
					out.close();
			} catch (final IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public void saveGame(final File f)
	{
		FileOutputStream fout = null;
		try
		{
			fout = new FileOutputStream(f);
			saveGame(fout);
		} catch (final IOException e)
		{
			e.printStackTrace();
		} finally
		{
			if (fout != null)
			{
				try
				{
					fout.close();
				} catch (final IOException e)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	public void saveGame(final OutputStream out) throws IOException
	{
		try
		{
			if (!m_delegateExecutionManager.blockDelegateExecution(4000))
			{
				new IOException("Could not lock delegate execution").printStackTrace();
			}
		} catch (final InterruptedException ie)
		{
			throw new IOException(ie.getMessage());
		}
		try
		{
			new GameDataManager().saveGame(out, m_data);
		} finally
		{
			m_delegateExecutionManager.resumeDelegateExecution();
		}
	}
	
	private void runStep(final boolean stepIsRestoredFromSavedGame)
	{
		if (getCurrentStep().hasReachedMaxRunCount())
		{
			m_data.getSequence().next();
			return;
		}
		if (m_isGameOver)
			return;
		startStep(stepIsRestoredFromSavedGame);
		if (m_isGameOver)
			return;
		waitForPlayerToFinishStep();
		if (m_isGameOver)
			return;
		final boolean autoSaveAfterDelegateDone = endStep();
		if (m_isGameOver)
			return;
		if (m_data.getSequence().next())
		{
			m_data.getHistory().getHistoryWriter().startNextRound(m_data.getSequence().getRound());
			autoSaveRound();
		}
		// save after the step has advanced
		// otherwise, the delegate will execute again.
		if (autoSaveAfterDelegateDone)
			autoSave();
	}
	
	/**
	 * 
	 * @return true if the step should autosave
	 */
	private boolean endStep()
	{
		m_delegateExecutionManager.enterDelegateExecution();
		try
		{
			getCurrentStep().getDelegate().end();
		} finally
		{
			m_delegateExecutionManager.leaveDelegateExecution();
		}
		getCurrentStep().incrementRunCount();
		if (m_data.getSequence().getStep().getDelegate().getClass().isAnnotationPresent(AutoSave.class))
		{
			if (m_data.getSequence().getStep().getDelegate().getClass().getAnnotation(AutoSave.class).afterStepEnd())
				return true;
		}
		return false;
	}
	
	private void startPersistentDelegates()
	{
		final Iterator<IDelegate> delegateIter = m_data.getDelegateList().iterator();
		while (delegateIter.hasNext())
		{
			final IDelegate delegate = delegateIter.next();
			if (!(delegate instanceof IPersistentDelegate))
			{
				continue;
			}
			final DefaultDelegateBridge bridge = new DefaultDelegateBridge(m_data, this, new DelegateHistoryWriter(m_channelMessenger), m_randomStats, m_delegateExecutionManager);
			CachedInstanceCenter.CachedDelegateBridge = bridge;
			if (m_delegateRandomSource == null)
			{
				m_delegateRandomSource = (IRandomSource) m_delegateExecutionManager.createOutboundImplementation(m_randomSource, new Class[] { IRandomSource.class });
			}
			bridge.setRandomSource(m_delegateRandomSource);
			m_delegateExecutionManager.enterDelegateExecution();
			try
			{
				delegate.setDelegateBridgeAndPlayer(bridge);
				delegate.start();
			} finally
			{
				m_delegateExecutionManager.leaveDelegateExecution();
			}
		}
	}
	
	private void startStep(final boolean stepIsRestoredFromSavedGame)
	{
		// dont save if we just loaded
		if (!stepIsRestoredFromSavedGame)
		{
			if (m_data.getSequence().getStep().getDelegate().getClass().isAnnotationPresent(AutoSave.class))
			{
				if (m_data.getSequence().getStep().getDelegate().getClass().getAnnotation(AutoSave.class).beforeStepStart())
					autoSave();
			}
		}
		final DefaultDelegateBridge bridge = new DefaultDelegateBridge(m_data, this, new DelegateHistoryWriter(m_channelMessenger), m_randomStats, m_delegateExecutionManager);
		CachedInstanceCenter.CachedDelegateBridge = bridge;
		CachedInstanceCenter.CachedGameData = m_data;
		if (m_delegateRandomSource == null)
		{
			m_delegateRandomSource = (IRandomSource) m_delegateExecutionManager.createOutboundImplementation(m_randomSource, new Class[] { IRandomSource.class });
		}
		bridge.setRandomSource(m_delegateRandomSource);
		// do any initialization of game data for all players here (not based on a delegate, and should not be)
		// we can not do this the very first run through, because there are no history nodes yet. We should do after first node is created.
		if (m_needToInitialize)
		{
			addPlayerTypesToGameData(m_gamePlayers.values(), m_playerManager, bridge);
		}
		notifyGameStepChanged(stepIsRestoredFromSavedGame);
		m_delegateExecutionManager.enterDelegateExecution();
		try
		{
			final IDelegate delegate = getCurrentStep().getDelegate();
			delegate.setDelegateBridgeAndPlayer(bridge);
			delegate.start();
		} finally
		{
			m_delegateExecutionManager.leaveDelegateExecution();
		}
	}
	
	private void waitForPlayerToFinishStep()
	{
		final PlayerID playerID = getCurrentStep().getPlayerID();
		// no player specified for the given step
		if (playerID == null)
			return;
		if (!getCurrentStep().getDelegate().delegateCurrentlyRequiresUserInput())
			return;
		final IGamePlayer player = m_gamePlayers.get(playerID);
		if (player != null)
		{
			// a local player
			/* if (HeadlessGameServer.headless())
			{
				System.out.println("Local Player step: " + getCurrentStep().getName() + " for PlayerID: " + playerID.getName() + ", player name: " + player.getName() + ", player type: "
							+ player.getType() + ". All local players: " + m_gamePlayers + ". All players: " + m_playerManager);
			}*/
			player.start(getCurrentStep().getName());
		}
		else
		{
			// a remote player
			final INode destination = m_playerManager.getNode(playerID.getName());
			final IGameStepAdvancer advancer = (IGameStepAdvancer) m_remoteMessenger.getRemote(ClientGame.getRemoteStepAdvancerName(destination));
			/* if (HeadlessGameServer.headless())
			{
				System.out.println("Remote Player step: " + getCurrentStep().getName() + " for PlayerID: " + playerID.getName() + ", Player Node: " + destination + ". All local players: "
							+ m_gamePlayers + ". All players: " + m_playerManager);
			}*/
			advancer.startPlayerStep(getCurrentStep().getName(), playerID);
		}
	}
	
	private void notifyGameStepChanged(final boolean loadedFromSavedGame)
	{
		final GameStep currentStep = getCurrentStep();
		final String stepName = currentStep.getName();
		final String delegateName = currentStep.getDelegate().getName();
		final String displayName = currentStep.getDisplayName();
		final int round = m_data.getSequence().getRound();
		final PlayerID id = currentStep.getPlayerID();
		notifyGameStepListeners(stepName, delegateName, id, round, displayName);
		getGameModifiedBroadcaster().stepChanged(stepName, delegateName, id, round, displayName, loadedFromSavedGame);
	}
	
	private void addPlayerTypesToGameData(final Collection<IGamePlayer> localPlayers, final PlayerManager allPlayers, final IDelegateBridge aBridge)
	{
		final GameData data = aBridge.getData();
		// potential bugs with adding changes to a game that has not yet started and has no history nodes yet. So wait for the first delegate to start before making changes.
		if (getCurrentStep() == null || getCurrentStep().getPlayerID() == null || (m_firstRun)) // && data.getPlayerList().getPlayers().iterator().next().getWhoAmI().equals("null:no_one")
		{
			m_firstRun = false;
			return;
		}
		// we can't add a new event or add new changes if we are not in a step.
		final HistoryNode curNode = data.getHistory().getLastNode();
		if (!(curNode instanceof Step) && !(curNode instanceof Event) && !(curNode instanceof EventChild))
			return;
		final CompositeChange change = new CompositeChange();
		final Set<String> allPlayersString = allPlayers.getPlayers();
		aBridge.getHistoryWriter().startEvent("Game Loaded");
		for (final IGamePlayer player : localPlayers)
		{
			allPlayersString.remove(player.getName());
			final boolean isHuman = player instanceof TripleAPlayer;
			aBridge.getHistoryWriter().addChildToEvent(player.getName() + ((player.getName().endsWith("s") || player.getName().endsWith("ese") || player.getName().endsWith("ish")) ? " are" : " is")
						+ " now being played by: " + player.getType());
			final PlayerID p = data.getPlayerList().getPlayerID(player.getName());
			final String newWhoAmI = ((isHuman ? "Human" : "AI") + ":" + player.getType());
			if (!p.getWhoAmI().equals(newWhoAmI))
				change.add(ChangeFactory.changePlayerWhoAmIChange(p, newWhoAmI));
		}
		final Iterator<String> playerIter = allPlayersString.iterator();
		while (playerIter.hasNext())
		{
			final String player = playerIter.next();
			playerIter.remove();
			aBridge.getHistoryWriter().addChildToEvent(player + ((player.endsWith("s") || player.endsWith("ese") || player.endsWith("ish")) ? " are" : " is") + " now being played by: Human:Client");
			final PlayerID p = data.getPlayerList().getPlayerID(player);
			final String newWhoAmI = "Human:Client";
			if (!p.getWhoAmI().equals(newWhoAmI))
				change.add(ChangeFactory.changePlayerWhoAmIChange(p, newWhoAmI));
		}
		if (!change.isEmpty())
			aBridge.addChange(change);
		m_needToInitialize = false;
		if (!allPlayersString.isEmpty())
			throw new IllegalStateException("Not all Player Types (ai/human/client) could be added to game data.");
	}
	
	private IGameModifiedChannel getGameModifiedBroadcaster()
	{
		return (IGameModifiedChannel) m_channelMessenger.getChannelBroadcastor(IGame.GAME_MODIFICATION_CHANNEL);
	}
	
	public void addChange(final Change aChange)
	{
		getGameModifiedBroadcaster().gameDataChanged(aChange);
		// let our channel subscribor do the change,
		// that way all changes will happen in the same thread
	}
	
	public boolean canSave()
	{
		return true;
	}
	
	public IRandomSource getRandomSource()
	{
		return m_randomSource;
	}
	
	public void setRandomSource(final IRandomSource randomSource)
	{
		m_randomSource = randomSource;
		m_delegateRandomSource = null;
	}
	
	public InGameLobbyWatcherWrapper getInGameLobbyWatcher()
	{
		return m_inGameLobbyWatcher;
	}
	
	public void setInGameLobbyWatcher(final InGameLobbyWatcherWrapper inGameLobbyWatcher)
	{
		m_inGameLobbyWatcher = inGameLobbyWatcher;
	}
	
	public void stopGameSequence()
	{
		m_delegateExecutionStopped = true;
	}
	
	public boolean isGameSequenceRunning()
	{
		return !m_delegateExecutionStopped;
	}
}


interface IServerRemote extends IRemote
{
	public byte[] getSavedGame();
}
