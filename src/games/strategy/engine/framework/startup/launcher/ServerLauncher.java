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
package games.strategy.engine.framework.startup.launcher;

import games.strategy.common.ui.InGameLobbyWatcherWrapper;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.framework.GameDataManager;
import games.strategy.engine.framework.GameRunner2;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.headlessGameServer.HeadlessGameServer;
import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.framework.startup.mc.ClientModel;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.startup.mc.IClientChannel;
import games.strategy.engine.framework.startup.mc.IObserverWaitingToJoin;
import games.strategy.engine.framework.startup.mc.ServerModel;
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

import java.awt.Component;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class ServerLauncher extends AbstractLauncher
{
	private static final Logger s_logger = Logger.getLogger(ServerLauncher.class.getName());
	public static final String SERVER_ROOT_DIR_PROPERTY = "triplea.server.root.dir";
	private final int m_clientCount;
	private final IRemoteMessenger m_remoteMessenger;
	private final IChannelMessenger m_channelMessenger;
	private final IMessenger m_messenger;
	private final PlayerListing m_playerListing;
	private final Map<String, INode> m_remotelPlayers;
	private final ServerModel m_serverModel;
	private ServerGame m_serverGame;
	private Component m_ui;
	private ServerReady m_serverReady;
	private final CountDownLatch m_errorLatch = new CountDownLatch(1);
	private volatile boolean m_isLaunching = true;
	private volatile boolean m_abortLaunch = false;
	private volatile boolean m_gameStopped = false;
	// a list of observers that tried to join the game during starup
	// we need to track these, because when we loose connections to them
	// we can ignore the connection lost
	private final List<INode> m_observersThatTriedToJoinDuringStartup = Collections.synchronizedList(new ArrayList<INode>());
	private InGameLobbyWatcherWrapper m_inGameLobbyWatcher;
	
	public ServerLauncher(final int clientCount, final IRemoteMessenger remoteMessenger, final IChannelMessenger channelMessenger, final IMessenger messenger,
				final GameSelectorModel gameSelectorModel, final PlayerListing playerListing, final Map<String, INode> remotelPlayers, final ServerModel serverModel, final boolean headless)
	{
		super(gameSelectorModel, headless);
		m_clientCount = clientCount;
		m_remoteMessenger = remoteMessenger;
		m_channelMessenger = channelMessenger;
		m_messenger = messenger;
		m_playerListing = playerListing;
		m_remotelPlayers = remotelPlayers;
		m_serverModel = serverModel;
	}
	
	public void setInGameLobbyWatcher(final InGameLobbyWatcherWrapper watcher)
	{
		m_inGameLobbyWatcher = watcher;
	}
	
	private boolean testShouldWeAbort()
	{
		if (m_abortLaunch)
			return true;
		if (m_gameData == null || m_serverModel == null)
			return true;
		else
		{
			final Map<String, String> players = m_serverModel.getPlayersToNodeListing();
			if (players == null || players.isEmpty())
				return true;
			else
			{
				for (final String player : players.keySet())
				{
					if (players.get(player) == null)
						return true;
				}
			}
		}
		if (m_serverGame != null && m_serverGame.getPlayerManager() != null)
		{
			if (m_serverGame.getPlayerManager().isEmpty())
				return true;
		}
		return false;
	}
	
	@Override
	protected void launchInNewThread(final Component parent)
	{
		try
		{
			// the order of this stuff does matter
			m_serverModel.setServerLauncher(this);
			m_serverReady = new ServerReady(m_clientCount);
			if (m_inGameLobbyWatcher != null)
			{
				m_inGameLobbyWatcher.setGameStatus(GameDescription.GameStatus.LAUNCHING, null);
			}
			m_serverModel.allowRemoveConnections();
			m_ui = parent;
			if (m_headless)
				HeadlessGameServer.log("Game Status: Launching");
			m_remoteMessenger.registerRemote(m_serverReady, ClientModel.CLIENT_READY_CHANNEL);
			m_gameData.doPreGameStartDataModifications(m_playerListing);
			s_logger.fine("Starting server");
			m_abortLaunch = testShouldWeAbort();
			byte[] gameDataAsBytes;
			try
			{
				gameDataAsBytes = gameDataToBytes(m_gameData);
			} catch (final IOException e)
			{
				e.printStackTrace();
				throw new IllegalStateException(e.getMessage());
			}
			final Set<IGamePlayer> localPlayerSet = m_gameData.getGameLoader().createPlayers(m_playerListing.getLocalPlayerTypes());
			final Messengers messengers = new Messengers(m_messenger, m_remoteMessenger, m_channelMessenger);
			m_serverGame = new ServerGame(m_gameData, localPlayerSet, m_remotelPlayers, messengers);
			m_serverGame.setInGameLobbyWatcher(m_inGameLobbyWatcher);
			if (m_headless)
			{
				HeadlessGameServer.setServerGame(m_serverGame);
			}
			// tell the clients to start,
			// later we will wait for them to all
			// signal that they are ready.
			((IClientChannel) m_channelMessenger.getChannelBroadcastor(IClientChannel.CHANNEL_NAME)).doneSelectingPlayers(gameDataAsBytes, m_serverGame.getPlayerManager().getPlayerMapping());
			final boolean useSecureRandomSource = !m_remotelPlayers.isEmpty();// && !m_localPlayerMapping.isEmpty(); // do we need to have a local player? i don't think we do...
			if (useSecureRandomSource)
			{
				// server game.
				// try to find an opponent to be the other side of the crypto random source.
				final PlayerID remotePlayer = m_serverGame.getPlayerManager().getRemoteOpponent(m_messenger.getLocalNode(), m_gameData);
				final CryptoRandomSource randomSource = new CryptoRandomSource(remotePlayer, m_serverGame);
				m_serverGame.setRandomSource(randomSource);
			}
			try
			{
				m_gameData.getGameLoader().startGame(m_serverGame, localPlayerSet, m_headless);
			} catch (final Exception e)
			{
				m_abortLaunch = true;
				Throwable error = e;
				while (error.getMessage() == null)
					error = error.getCause();
				final String message = error.getMessage();
				if (m_gameLoadingWindow != null)
					m_gameLoadingWindow.doneWait();
				if (!m_headless)
				{
					SwingUtilities.invokeLater(new Runnable()
					{
						public void run()
						{
							JOptionPane.showMessageDialog(null, message, "Warning", JOptionPane.WARNING_MESSAGE);
						}
					});
				}
				else
					System.out.println(message);
				
			}
			if (m_headless)
				HeadlessGameServer.log("Game Successfully Loaded. " + (m_abortLaunch ? "Aborting Launch." : "Starting Game."));
			if (m_abortLaunch)
				m_serverReady.countDownAll();
			if (!m_serverReady.await(GameRunner2.getServerStartGameSyncWaitTime(), TimeUnit.SECONDS))
			{
				System.out.println("Waiting for clients to be ready timed out!");
				m_abortLaunch = true;
			}
			m_remoteMessenger.unregisterRemote(ClientModel.CLIENT_READY_CHANNEL);
			final Thread t = new Thread("Triplea, start server game")
			{
				@Override
				public void run()
				{
					try
					{
						m_isLaunching = false;
						m_abortLaunch = testShouldWeAbort();
						if (!m_abortLaunch)
						{
							if (useSecureRandomSource)
							{
								warmUpCryptoRandomSource();
							}
							if (m_gameLoadingWindow != null)
								m_gameLoadingWindow.doneWait();
							if (m_headless)
								HeadlessGameServer.log("Starting Game Delegates.");
							m_serverGame.startGame();
						}
						else
						{
							stopGame();
							if (!m_headless)
							{
								SwingUtilities.invokeLater(new Runnable()
								{
									public void run()
									{
										JOptionPane.showMessageDialog(m_ui, "Problem during startup, game aborted.");
									}
								});
							}
							else
								System.out.println("Problem during startup, game aborted.");
						}
					} catch (final MessengerException me)
					{
						// if just connection lost, no need to scare the user with some giant stack trace
						if (me instanceof ConnectionLostException)
							System.out.println("Game Player disconnection: " + me.getMessage());
						else
							me.printStackTrace(System.out);
						// we lost a connection
						// wait for the connection handler to notice, and shut us down
						try
						{
							// we are already aborting the launch
							if (!m_abortLaunch)
								if (!m_errorLatch.await(GameRunner2.getServerObserverJoinWaitTime() + 10, TimeUnit.SECONDS))
									System.err.println("Waiting on error latch timed out!");
						} catch (final InterruptedException e)
						{
							e.printStackTrace();
						}
						stopGame();
					} catch (final Exception e)
					{
						e.printStackTrace(System.err);
						if (m_headless)
							System.out.println(games.strategy.debug.DebugUtils.getThreadDumps());
						stopGame();
					}
					try
					{
						Thread.sleep(200); // having an oddball issue with the zip stream being closed while parsing to load default game. might be caused by closing of stream while unloading map resources.
					} catch (final InterruptedException e)
					{
					}
					// either game ended, or aborted, or a player left or disconnected
					if (m_headless)
					{
						try
						{
							System.out.println("Game ended, going back to waiting.");
							if (m_serverModel != null)
								m_serverModel.setAllPlayersToNullNodes(); // if we do not do this, we can get into an infinite loop of launching a game, then crashing out, then launching, etc.
							final File f1 = new File(SaveGameFileChooser.DEFAULT_DIRECTORY, SaveGameFileChooser.getAutoSaveFileName());
							final File f2 = new File(SaveGameFileChooser.DEFAULT_DIRECTORY, SaveGameFileChooser.getAutoSave2FileName());
							final File f;
							if (!f1.exists() && !f2.exists())
								m_gameSelectorModel.resetGameDataToNull();
							else
							{
								if (!f1.exists() || f1.lastModified() < f2.lastModified())
									f = f2;
								else
									f = f1;
								m_gameSelectorModel.load(f, null);
							}
						} catch (final Exception e)
						{
							e.printStackTrace();
							m_gameSelectorModel.resetGameDataToNull();
						}
					}
					else
					{
						m_gameSelectorModel.loadDefaultGame(parent);
					}
					if (parent != null)
					{
						SwingUtilities.invokeLater(new Runnable()
						{
							public void run()
							{
								JOptionPane.getFrameForComponent(parent).setVisible(true);
							}
						});
					}
					m_serverModel.setServerLauncher(null);
					m_serverModel.newGame();
					if (m_inGameLobbyWatcher != null)
					{
						m_inGameLobbyWatcher.setGameStatus(GameDescription.GameStatus.WAITING_FOR_PLAYERS, null);
					}
					if (m_headless)
					{
						// tell headless server to wait for new connections:
						HeadlessGameServer.waitForUsersHeadlessInstance();
						HeadlessGameServer.log("Game Status: Waiting For Players");
					}
				}
			};
			t.start();
		} finally
		{
			if (m_gameLoadingWindow != null)
				m_gameLoadingWindow.doneWait();
			if (m_inGameLobbyWatcher != null)
			{
				m_inGameLobbyWatcher.setGameStatus(GameDescription.GameStatus.IN_PROGRESS, m_serverGame);
			}
			if (m_headless)
				HeadlessGameServer.log("Game Status: In Progress");
		}
	}
	
	private void warmUpCryptoRandomSource()
	{
		// the first roll takes a while, initialize
		// here in the background so that the user doesnt notice
		final Thread t = new Thread("Warming up crypto random source")
		{
			@Override
			public void run()
			{
				try
				{
					m_serverGame.getRandomSource().getRandom(m_gameData.getDiceSides(), 2, "Warming up crypto random source");
				} catch (final RuntimeException re)
				{
					re.printStackTrace(System.out);
				}
			}
		};
		t.start();
	}
	
	public void addObserver(final IObserverWaitingToJoin blockingObserver, final IObserverWaitingToJoin nonBlockingObserver, final INode newNode)
	{
		if (m_isLaunching)
		{
			m_observersThatTriedToJoinDuringStartup.add(newNode);
			nonBlockingObserver.cannotJoinGame("Game is launching, try again soon");
			return;
		}
		m_serverGame.addObserver(blockingObserver, nonBlockingObserver, newNode);
	}
	
	public static byte[] gameDataToBytes(final GameData data) throws IOException
	{
		final ByteArrayOutputStream sink = new ByteArrayOutputStream(25000);
		new GameDataManager().saveGame(sink, data);
		sink.flush();
		sink.close();
		return sink.toByteArray();
	}
	
	public void connectionLost(final INode node)
	{
		// System.out.println("Connection lost to: " + node);
		if (m_isLaunching)
		{
			// this is expected, we told the observer
			// he couldnt join, so now we loose the connection
			if (m_observersThatTriedToJoinDuringStartup.remove(node))
				return;
			// a player has dropped out, abort
			m_abortLaunch = true;
			m_serverReady.countDownAll();
			return;
		}
		// if we loose a connection to a player, shut down
		// the game (after saving) and go back to the main screen
		if (m_serverGame.getPlayerManager().isPlaying(node))
		{
			if (m_serverGame.isGameSequenceRunning())
				saveAndEndGame(node);
			else
				stopGame();
			// if the game already exited do to a networking error
			// we need to let them continue
			m_errorLatch.countDown();
		}
		else
		{
			// nothing to do
			// we just lost a connection to an observer
			// which is ok.
		}
	}
	
	private void stopGame()
	{
		if (!m_gameStopped)
		{
			m_gameStopped = true;
			if (m_serverGame != null)
				m_serverGame.stopGame();
		}
	}
	
	private void saveAndEndGame(final INode node)
	{
		final DateFormat format = new SimpleDateFormat("MMM_dd_'at'_HH_mm");
		SaveGameFileChooser.ensureDefaultDirExists();
		// a hack, if headless save to the autosave to avoid polluting our savegames folder with a million saves
		final File f;
		if (m_headless)
		{
			final File f1 = new File(SaveGameFileChooser.DEFAULT_DIRECTORY, SaveGameFileChooser.getAutoSaveFileName());
			final File f2 = new File(SaveGameFileChooser.DEFAULT_DIRECTORY, SaveGameFileChooser.getAutoSave2FileName());
			if (f1.lastModified() > f2.lastModified())
				f = f2;
			else
				f = f1;
		}
		else
			f = new File(SaveGameFileChooser.DEFAULT_DIRECTORY, "connection_lost_on_" + format.format(new Date()) + ".tsvg");
		try
		{
			m_serverGame.saveGame(f);
		} catch (final Exception e)
		{
			e.printStackTrace();
			if (m_headless && HeadlessGameServer.getInstance() != null)
				HeadlessGameServer.getInstance().printThreadDumpsAndStatus();
			// TODO: We seem to be getting this bug once a week (1.8.0.1 and previous versions). Trying a fix for 1.8.0.3, need to see if it works.
		}
		stopGame();
		if (!m_headless)
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					final String message = "Connection lost to:" + node.getName() + " game is over.  Game saved to:" + f.getName();
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(m_ui), message);
				}
			});
		}
		else
		{
			System.out.println("Connection lost to:" + node.getName() + " game is over.  Game saved to:" + f.getName());
		}
	}
}


class ServerReady implements IServerReady
{
	private final CountDownLatch m_latch;
	private final int m_clients;
	
	ServerReady(final int waitCount)
	{
		m_clients = waitCount;
		m_latch = new CountDownLatch(m_clients);
	}
	
	public void clientReady()
	{
		m_latch.countDown();
	}
	
	public void countDownAll()
	{
		for (int i = 0; i < m_clients; i++)
		{
			m_latch.countDown();
		}
	}
	
	public void await()
	{
		try
		{
			m_latch.await();
		} catch (final InterruptedException e)
		{
			e.printStackTrace();
		}
	}
	
	public boolean await(final long timeout, final TimeUnit timeUnit)
	{
		boolean didNotTimeOut = false;
		try
		{
			didNotTimeOut = m_latch.await(timeout, timeUnit);
		} catch (final InterruptedException e)
		{
			e.printStackTrace();
		}
		return didNotTimeOut;
	}
}
