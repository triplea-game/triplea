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
package games.strategy.engine.framework.startup.ui;

import games.strategy.debug.HeartBeat;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.events.GameStepListener;
import games.strategy.engine.framework.GameRunner2;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.lobby.server.GameDescription;
import games.strategy.engine.lobby.server.GameDescription.GameStatus;
import games.strategy.engine.lobby.server.ILobbyGameController;
import games.strategy.engine.lobby.server.LobbyServer;
import games.strategy.engine.lobby.server.login.LobbyLoginValidator;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.RemoteMessenger;
import games.strategy.engine.message.UnifiedMessenger;
import games.strategy.net.ClientMessenger;
import games.strategy.net.GUID;
import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.IConnectionLogin;
import games.strategy.net.IMessenger;
import games.strategy.net.IMessengerErrorListener;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.net.MacFinder;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * 
 * Watches a game in progress, and updates the Lobby with the state of the game.
 * <p>
 * 
 * This class opens its own connection to the lobby, and its own messenger.
 * <p>
 * 
 * @author sgb
 * 
 */
public class InGameLobbyWatcher
{
	public static final String LOBBY_WATCHER_NAME = "lobby_watcher";
	// this is the messenger used by the game
	// it is different than the messenger we use to connect to
	// the game lobby
	private final IServerMessenger m_gameMessenger;
	private boolean m_shutdown = false;
	private final GUID m_gameID = new GUID();
	private GameSelectorModel m_gameSelectorModel;
	private final Observer m_gameSelectorModelObserver = new Observer()
	{
		public void update(final Observable o, final Object arg)
		{
			gameSelectorModelUpdated();
		}
	};
	private IGame m_game;
	private final GameStepListener m_gameStepListener = new GameStepListener()
	{
		public void gameStepChanged(final String stepName, final String delegateName, final PlayerID player, final int round, final String displayName)
		{
			InGameLobbyWatcher.this.gameStepChanged(stepName, round);
		}
	};
	// we create this messenger, and use it to connect to the
	// game lobby
	private final IMessenger m_messenger;
	private final IRemoteMessenger m_remoteMessenger;
	private final GameDescription m_gameDescription;
	private final Object m_mutex = new Object();
	private final IConnectionChangeListener m_connectionChangeListener;
	
	/**
	 * Reads SystemProperties to see if we should connect to a lobby server
	 * <p>
	 * 
	 * After creation, those properties are cleared, since we should watch the first start game.
	 * <p>
	 * 
	 * @return null if no watcher should be created
	 */
	public static InGameLobbyWatcher newInGameLobbyWatcher(final IServerMessenger gameMessenger, final JComponent parent)
	{
		final String host = System.getProperties().getProperty(GameRunner2.LOBBY_HOST);
		final String port = System.getProperties().getProperty(GameRunner2.LOBBY_PORT);
		final String hostedBy = System.getProperties().getProperty(GameRunner2.LOBBY_GAME_HOSTED_BY);
		if (host == null || port == null)
		{
			return null;
		}
		// clear the properties
		System.getProperties().remove(GameRunner2.LOBBY_HOST);
		System.getProperties().remove(GameRunner2.LOBBY_PORT);
		System.getProperties().remove(GameRunner2.LOBBY_GAME_HOSTED_BY);
		final IConnectionLogin login = new IConnectionLogin()
		{
			public void notifyFailedLogin(final String message)
			{
			}
			
			public Map<String, String> getProperties(final Map<String, String> challengProperties)
			{
				final Map<String, String> rVal = new HashMap<String, String>();
				rVal.put(LobbyLoginValidator.ANONYMOUS_LOGIN, Boolean.TRUE.toString());
				rVal.put(LobbyLoginValidator.LOBBY_VERSION, LobbyServer.LOBBY_VERSION.toString());
				rVal.put(LobbyLoginValidator.LOBBY_WATCHER_LOGIN, Boolean.TRUE.toString());
				return rVal;
			}
		};
		try
		{
			System.out.println("host:" + host + " port:" + port);
			final String mac = MacFinder.GetHashedMacAddress();
			final ClientMessenger messenger = new ClientMessenger(host, Integer.parseInt(port), getRealName(hostedBy) + "_" + LOBBY_WATCHER_NAME, mac, login);
			final UnifiedMessenger um = new UnifiedMessenger(messenger);
			final RemoteMessenger rm = new RemoteMessenger(um);
			final HeartBeat h = new HeartBeat(messenger.getServerNode());
			rm.registerRemote(h, HeartBeat.getHeartBeatName(um.getLocalNode()));
			return new InGameLobbyWatcher(messenger, rm, gameMessenger, parent);
		} catch (final Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	private static String getRealName(final String uniqueName)
	{
		// Remove any (n) that is added to distinguish duplicate names
		final String name = uniqueName.split(" ")[0];
		return name;
	}
	
	public void setGame(final IGame game)
	{
		if (m_game != null)
		{
			m_game.removeGameStepListener(m_gameStepListener);
		}
		m_game = game;
		if (game != null)
		{
			game.addGameStepListener(m_gameStepListener);
			gameStepChanged(game.getData().getSequence().getStep().getName(), game.getData().getSequence().getRound());
		}
	}
	
	private void gameStepChanged(final String stepName, final int round)
	{
		synchronized (m_mutex)
		{
			if (!m_gameDescription.getRound().equals(Integer.toString(round)))
			{
				m_gameDescription.setRound(round + "");
			}
			postUpdate();
		}
	}
	
	private void gameSelectorModelUpdated()
	{
		synchronized (m_mutex)
		{
			m_gameDescription.setGameName(m_gameSelectorModel.getGameName());
			postUpdate();
		}
	}
	
	public InGameLobbyWatcher(final IMessenger messenger, final IRemoteMessenger remoteMessenger, final IServerMessenger serverMessenger, final JComponent parent)
	{
		m_messenger = messenger;
		m_remoteMessenger = remoteMessenger;
		m_gameMessenger = serverMessenger;
		m_gameDescription = new GameDescription(m_messenger.getLocalNode(), m_gameMessenger.getLocalNode().getPort(), new Date(), "???", 1, GameStatus.WAITING_FOR_PLAYERS, "-", m_gameMessenger
					.getLocalNode().getName(), System.getProperty(GameRunner2.LOBBY_GAME_COMMENTS));
		final ILobbyGameController controller = (ILobbyGameController) m_remoteMessenger.getRemote(ILobbyGameController.GAME_CONTROLLER_REMOTE);
		synchronized (m_mutex)
		{
			controller.postGame(m_gameID, (GameDescription) m_gameDescription.clone());
		}
		// if we loose our connection, then shutdown
		m_messenger.addErrorListener(new IMessengerErrorListener()
		{
			public void messengerInvalid(final IMessenger messenger, final Exception reason)
			{
				shutDown();
			}
		});
		m_connectionChangeListener = new IConnectionChangeListener()
		{
			public void connectionRemoved(final INode to)
			{
				updatePlayerCount();
			}
			
			public void connectionAdded(final INode to)
			{
				updatePlayerCount();
			}
		};
		// when players join or leave the game
		// update the connection count
		m_gameMessenger.addConnectionChangeListener(m_connectionChangeListener);
		final Runnable r = new Runnable()
		{
			public void run()
			{
				final String addressUsed = controller.testGame(m_gameID);
				// if the server cannot connect to us, then quit
				if (addressUsed != null)
				{
					if (isActive())
					{
						shutDown();
						SwingUtilities.invokeLater(new Runnable()
						{
							public void run()
							{
								final String message = "Your computer is not reachable from the internet.\n" + "Please check your firewall or router configuration.\n"
											+ "See 'How To Host...' in the help menu, at the top of the lobby screen.\n" + "The server tried to connect to " + addressUsed;
								JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(parent), message, "Could Not Host", JOptionPane.ERROR_MESSAGE);
								System.exit(-1);
							}
						});
					}
				}
			}
		};
		new Thread(r).start();
	}
	
	public void setGameSelectorModel(final GameSelectorModel model)
	{
		cleanUpGameModelListener();
		if (model != null)
		{
			m_gameSelectorModel = model;
			m_gameSelectorModel.addObserver(m_gameSelectorModelObserver);
			gameSelectorModelUpdated();
		}
	}
	
	private void cleanUpGameModelListener()
	{
		if (m_gameSelectorModel != null)
		{
			m_gameSelectorModel.deleteObserver(m_gameSelectorModelObserver);
		}
	}
	
	protected void updatePlayerCount()
	{
		synchronized (m_mutex)
		{
			m_gameDescription.setPlayerCount(m_gameMessenger.getNodes().size());
			postUpdate();
		}
	}
	
	private void postUpdate()
	{
		if (m_shutdown)
			return;
		synchronized (m_mutex)
		{
			final ILobbyGameController controller = (ILobbyGameController) m_remoteMessenger.getRemote(ILobbyGameController.GAME_CONTROLLER_REMOTE);
			controller.updateGame(m_gameID, (GameDescription) m_gameDescription.clone());
		}
	}
	
	public void shutDown()
	{
		m_shutdown = true;
		m_messenger.shutDown();
		m_gameMessenger.removeConnectionChangeListener(m_connectionChangeListener);
		cleanUpGameModelListener();
	}
	
	public boolean isActive()
	{
		return !m_shutdown;
	}
	
	public void setGameStatus(final GameDescription.GameStatus status, final IGame game)
	{
		synchronized (m_mutex)
		{
			m_gameDescription.setStatus(status);
			if (game == null)
			{
				m_gameDescription.setRound("-");
			}
			else
			{
				m_gameDescription.setRound(game.getData().getSequence().getRound() + "");
			}
			setGame(game);
			postUpdate();
		}
	}
	
	public String getComments()
	{
		return m_gameDescription.getComment();
	}
	
	public void setGameComments(final String comments)
	{
		synchronized (m_mutex)
		{
			m_gameDescription.setComment(comments);
			postUpdate();
		}
	}
}
