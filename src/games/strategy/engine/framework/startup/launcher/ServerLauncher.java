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
import games.strategy.debug.Console;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.framework.GameDataManager;
import games.strategy.engine.framework.HeadlessGameServer;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.startup.mc.ClientModel;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.startup.mc.IClientChannel;
import games.strategy.engine.framework.startup.mc.IObserverWaitingToJoin;
import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.engine.framework.ui.SaveGameFileChooser;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.lobby.server.GameDescription;
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
	private final Map<String, String> m_localPlayerMapping;
	private final Map<String, INode> m_remotelPlayers;
	private final ServerModel m_serverModel;
	private ServerGame m_serverGame;
	private Component m_ui;
	private final CountDownLatch m_erroLatch = new CountDownLatch(1);
	private volatile boolean m_isLaunching = true;
	private ServerReady m_serverReady;
	private volatile boolean m_abortLaunch = false;
	// a list of observers that tried to join the game during starup
	// we need to track these, because when we loose connections to them
	// we can ignore the connection lost
	private final List<INode> m_observersThatTriedToJoinDuringStartup = Collections.synchronizedList(new ArrayList<INode>());
	private InGameLobbyWatcherWrapper m_inGameLobbyWatcher;
	
	public ServerLauncher(final int clientCount, final IRemoteMessenger remoteMessenger, final IChannelMessenger channelMessenger, final IMessenger messenger,
				final GameSelectorModel gameSelectorModel, final Map<String, String> localPlayerMapping, final Map<String, INode> remotelPlayers, final ServerModel serverModel, final boolean headless)
	{
		super(gameSelectorModel, headless);
		m_clientCount = clientCount;
		m_remoteMessenger = remoteMessenger;
		m_channelMessenger = channelMessenger;
		m_messenger = messenger;
		m_localPlayerMapping = localPlayerMapping;
		m_remotelPlayers = remotelPlayers;
		m_serverModel = serverModel;
	}
	
	public void setInGameLobbyWatcher(final InGameLobbyWatcherWrapper watcher)
	{
		m_inGameLobbyWatcher = watcher;
	}
	
	@Override
	protected void launchInNewThread(final Component parent)
	{
		try
		{
			if (m_inGameLobbyWatcher != null)
			{
				m_inGameLobbyWatcher.setGameStatus(GameDescription.GameStatus.LAUNCHING, null);
				try
				{
					Thread.sleep(2000);
				} catch (final InterruptedException e1)
				{
				}
			}
			m_ui = parent;
			m_serverModel.setServerLauncher(this);
			s_logger.fine("Starting server");
			m_serverReady = new ServerReady(m_clientCount);
			m_remoteMessenger.registerRemote(m_serverReady, ClientModel.CLIENT_READY_CHANNEL);
			byte[] gameDataAsBytes;
			try
			{
				gameDataAsBytes = gameDataToBytes(m_gameData);
			} catch (final IOException e)
			{
				e.printStackTrace();
				throw new IllegalStateException(e.getMessage());
			}
			final Set<IGamePlayer> localPlayerSet = m_gameData.getGameLoader().createPlayers(m_localPlayerMapping);
			final Messengers messengers = new Messengers(m_messenger, m_remoteMessenger, m_channelMessenger);
			m_serverGame = new ServerGame(m_gameData, localPlayerSet, m_remotelPlayers, messengers);
			m_serverGame.setInGameLobbyWatcher(m_inGameLobbyWatcher);
			// tell the clients to start,
			// later we will wait for them to all
			// signal that they are ready.
			((IClientChannel) m_channelMessenger.getChannelBroadcastor(IClientChannel.CHANNEL_NAME)).doneSelectingPlayers(gameDataAsBytes, m_serverGame.getPlayerManager().getPlayerMapping());
			final boolean useSecureRandomSource = !m_remotelPlayers.isEmpty() && !m_localPlayerMapping.isEmpty();
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
			} catch (final IllegalStateException e)
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
				
			} catch (final Exception e)
			{
				e.printStackTrace();
				m_abortLaunch = true;
			}
			if (m_headless)
			{
				HeadlessGameServer.setServerGame(m_serverGame);
			}
			m_serverReady.await();
			m_remoteMessenger.unregisterRemote(ClientModel.CLIENT_READY_CHANNEL);
			final Thread t = new Thread("Triplea, start server game")
			{
				@Override
				public void run()
				{
					try
					{
						m_isLaunching = false;
						if (!m_abortLaunch)
						{
							if (useSecureRandomSource)
							{
								warmUpCryptoRandomSource();
							}
							if (m_gameLoadingWindow != null)
								m_gameLoadingWindow.doneWait();
							m_serverGame.startGame();
						}
						else
						{
							m_serverGame.stopGame();
							if (!m_headless)
							{
								SwingUtilities.invokeLater(new Runnable()
								{
									public void run()
									{
										JOptionPane.showMessageDialog(m_ui, "Error during startup, game aborted.");
									}
								});
							}
							else
								System.out.println("Error during startup, game aborted.");
						}
					} catch (final MessengerException me)
					{
						me.printStackTrace(System.out);
						// we lost a connection
						// wait for the connection handler to notice, and shut us down
						try
						{
							// we are already aborting the launch
							if (!m_abortLaunch)
								m_erroLatch.await();
						} catch (final InterruptedException e)
						{
							e.printStackTrace();
						}
					} catch (final Exception e)
					{
						// TODO: figure out why we are getting any errors that make us end up here! We should NEVER end up here.
						e.printStackTrace(System.err);
						if (m_headless)
							System.out.println(Console.getThreadDumps());
						m_serverGame.stopGame();
					}
					// either game ended, or aborted, or a player left or disconnected
					if (m_headless)
					{
						System.out.println("Game ended, going back to waiting.");
						try
						{
							Thread.sleep(250);
						} catch (final InterruptedException e)
						{
						}
						m_gameSelectorModel.load(new File(SaveGameFileChooser.DEFAULT_DIRECTORY, SaveGameFileChooser.getAutoSaveFileName()), null);
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
					m_serverGame.getRandomSource().getRandom(m_gameData.getDiceSides(), 2, "Warming up crpyto random source");
				} catch (final RuntimeException re)
				{
					re.printStackTrace(System.out);
				}
			}
		};
		t.start();
	}
	
	public void addObserver(final IObserverWaitingToJoin observer, final INode newNode)
	{
		if (m_isLaunching)
		{
			m_observersThatTriedToJoinDuringStartup.add(newNode);
			observer.cannotJoinGame("Game is launching, try again soon");
			return;
		}
		m_serverGame.addObserver(observer);
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
		if (m_isLaunching)
		{
			// this is expected, we told the observer
			// he couldnt join, so now we loose the connection
			if (m_observersThatTriedToJoinDuringStartup.remove(node))
				return;
			// a player has dropped out
			// abort
			m_serverReady.clientReady();
			m_abortLaunch = true;
			return;
		}
		// if we loose a connection to a player, shut down
		// the game (after saving) and go back to the main screen
		if (m_serverGame.getPlayerManager().isPlaying(node))
		{
			if (m_serverGame.isGameSequenceRunning())
				saveAndEndGame(node);
			else
				m_serverGame.stopGame();
			// if the game already exited do to a networking error
			// we need to let them continue
			m_erroLatch.countDown();
		}
		else
		{
			// nothing to do
			// we just lost a connection to an observer
			// which is ok.
		}
	}
	
	private void saveAndEndGame(final INode node)
	{
		final DateFormat format = new SimpleDateFormat("MMM_dd_'at'_HH_mm");
		SaveGameFileChooser.ensureDefaultDirExists();
		// a hack, if headless save to the autosave to avoid polluting our savegames folder with a million saves
		final File f;
		if (m_headless)
			f = new File(SaveGameFileChooser.DEFAULT_DIRECTORY, SaveGameFileChooser.getAutoSaveFileName());
		else
			f = new File(SaveGameFileChooser.DEFAULT_DIRECTORY, "connection_lost_on_" + format.format(new Date()) + ".tsvg");
		try
		{
			m_serverGame.saveGame(f);
		} catch (final Exception e)
		{
			e.printStackTrace();
			// TODO: Veqryn: we seem to be occassionally getting this in our headless game server hostbots, and I have no idea why.
			// Symptoms and/or causes include the client not having any buttons in their action tab, followed by them leaving (connection lost) the game out of frustration,
			// followed by a "Could not lock delegate execution" error, followed by a "IllegalMonitorStateException" error in savegame.
		}
		m_serverGame.stopGame();
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
	
	ServerReady(final int waitCount)
	{
		m_latch = new CountDownLatch(waitCount);
	}
	
	public void clientReady()
	{
		m_latch.countDown();
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
}
