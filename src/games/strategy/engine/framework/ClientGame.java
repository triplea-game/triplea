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
 * ClientGame.java
 * 
 * Created on December 14, 2001, 12:48 PM
 */

package games.strategy.engine.framework;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangePerformer;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.PlayerManager;
import games.strategy.engine.data.events.GameStepListener;
import games.strategy.engine.display.DefaultDisplayBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.gamePlayer.DefaultPlayerBridge;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.engine.history.EventChild;
import games.strategy.engine.message.IChannelMessenger;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.RemoteName;
import games.strategy.engine.random.IRandomSource;
import games.strategy.engine.random.IRemoteRandom;
import games.strategy.engine.random.RemoteRandom;
import games.strategy.engine.vault.Vault;
import games.strategy.net.IMessenger;
import games.strategy.net.INode;
import games.strategy.net.Messengers;
import games.strategy.triplea.ui.ErrorHandler;
import games.strategy.util.ListenerList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author Sean Bridges
 */
public class ClientGame implements IGame
{
	private ListenerList<GameStepListener> m_gameStepListeners = new ListenerList<GameStepListener>();
	private final GameData m_data;
	private final IMessenger m_messenger;
	private final IRemoteMessenger m_remoteMessenger;
	private final IChannelMessenger m_channelMessenger;
	private final Messengers m_messengers;
	private final ChangePerformer m_changePerformer;
	private volatile boolean m_isGameOver = false;
	
	// maps PlayerID->GamePlayer
	private final Map<PlayerID, IGamePlayer> m_gamePlayers = new HashMap<PlayerID, IGamePlayer>();
	private final Vault m_vault;
	private final PlayerManager m_playerManager;
	
	public static final RemoteName getRemoteStepAdvancerName(INode node)
	{
		
		return new RemoteName("games.strategy.engine.framework.ClientGame.REMOTE_STEP_ADVANCER:" + node.getName(), IGameStepAdvancer.class);
	}
	
	public ClientGame(GameData data, Set<IGamePlayer> gamePlayers, PlayerManager playerManager, Messengers messengers)
	{
		m_data = data;
		
		m_playerManager = playerManager;
		if (m_playerManager == null)
			throw new IllegalArgumentException("Player manager cant be null");
		
		m_messengers = messengers;
		m_messenger = m_messengers.getMessenger();
		
		m_remoteMessenger = m_messengers.getRemoteMessenger();
		m_channelMessenger = m_messengers.getChannelMessenger();
		m_vault = new Vault(m_channelMessenger);
		
		m_channelMessenger.registerChannelSubscriber(m_gameModificationChannelListener, IGame.GAME_MODIFICATION_CHANNEL);
		m_remoteMessenger.registerRemote(m_gameStepAdvancer, getRemoteStepAdvancerName(m_channelMessenger.getLocalNode()));
		
		Iterator<IGamePlayer> iter = gamePlayers.iterator();
		while (iter.hasNext())
		{
			IGamePlayer gp = iter.next();
			PlayerID player = m_data.getPlayerList().getPlayerID(gp.getName());
			m_gamePlayers.put(player, gp);
			
			IPlayerBridge bridge = new DefaultPlayerBridge(this);
			gp.initialize(bridge, player);
			
			m_remoteMessenger.registerRemote(gp, ServerGame.getRemoteName(gp.getID(), data));
			
			IRemoteRandom remoteRandom = new RemoteRandom(this);
			m_remoteMessenger.registerRemote(remoteRandom, ServerGame.getRemoteRandomName(player));
		}
		
		m_changePerformer = new ChangePerformer(m_data);
	}
	
	private IGameModifiedChannel m_gameModificationChannelListener = new IGameModifiedChannel()
	{
		
		@Override
		public void gameDataChanged(Change aChange)
	{
		m_changePerformer.perform(aChange);
		m_data.getHistory().getHistoryWriter().addChange(aChange);
	}
		
		@Override
		public void startHistoryEvent(String event)
	{
		m_data.getHistory().getHistoryWriter().startEvent(event);
		
	}
		
		@Override
		public void addChildToEvent(String text, Object renderingData)
	{
		m_data.getHistory().getHistoryWriter().addChildToEvent(new EventChild(text, renderingData));
		
	}
		
		@Override
		public void setRenderingData(Object renderingData)
	{
		m_data.getHistory().getHistoryWriter().setRenderingData(renderingData);
		
	}
		
		@Override
		public void stepChanged(String stepName, String delegateName, PlayerID player, int round, String displayName, boolean loadedFromSavedGame)
	{
		
		// we want to skip the first iteration, since that simply advances us to step 0
		if (m_firstRun)
			m_firstRun = false;
		else
		{
			m_data.acquireWriteLock();
			try
			{
				m_data.getSequence().next();
				while (!m_data.getSequence().getStep().getName().equals(stepName))
				{
					m_data.getSequence().next();
				}
			}
				finally
				{
					m_data.releaseWriteLock();
				}
			}
			
			Iterator<GameStepListener> iter = m_gameStepListeners.iterator();
			while (iter.hasNext())
		{
			GameStepListener listener = iter.next();
			listener.gameStepChanged(stepName, delegateName, player, round, displayName);
		}
		
		if (!loadedFromSavedGame)
			m_data.getHistory().getHistoryWriter().startNextStep(stepName, delegateName, player, displayName);
	}
		
		@Override
		public void shutDown()
	{
		ClientGame.this.shutDown();
	}
		
	};
	
	public void shutDown()
	{
		if (m_isGameOver)
			return;
		m_isGameOver = true;
		ErrorHandler.setGameOver(true);
		m_channelMessenger.unregisterChannelSubscriber(m_gameModificationChannelListener, IGame.GAME_MODIFICATION_CHANNEL);
		m_remoteMessenger.unregisterRemote(getRemoteStepAdvancerName(m_channelMessenger.getLocalNode()));
		m_vault.shutDown();
		
		Iterator<IGamePlayer> iter = m_gamePlayers.values().iterator();
		while (iter.hasNext())
		{
			IGamePlayer gp = iter.next();
			PlayerID player;
			m_data.acquireReadLock();
			try
			{
				player = m_data.getPlayerList().getPlayerID(gp.getName());
			} finally
			{
				m_data.releaseReadLock();
			}
			m_gamePlayers.put(player, gp);
			
			m_remoteMessenger.unregisterRemote(ServerGame.getRemoteName(gp.getID(), m_data));
			
			m_remoteMessenger.unregisterRemote(ServerGame.getRemoteRandomName(player));
			
		}
		
		m_data.getGameLoader().shutDown();
		
	}
	
	@Override
	public GameData getData()
	{
		return m_data;
	}
	
	@Override
	public IMessenger getMessenger()
	{
		return m_messenger;
	}
	
	@Override
	public IChannelMessenger getChannelMessenger()
	{
		return m_channelMessenger;
	}
	
	@Override
	public IRemoteMessenger getRemoteMessenger()
	{
		return m_remoteMessenger;
	}
	
	@Override
	public void addGameStepListener(GameStepListener listener)
	{
		m_gameStepListeners.add(listener);
	}
	
	@Override
	public void removeGameStepListener(GameStepListener listener)
	{
		m_gameStepListeners.remove(listener);
	}
	
	private boolean m_firstRun = true;
	
	@Override
	public void addChange(Change aChange)
	{
		throw new UnsupportedOperationException();
	}
	
	private IGameStepAdvancer m_gameStepAdvancer = new IGameStepAdvancer()
	{
		@Override
		public void startPlayerStep(String stepName, PlayerID player)
	{
		
		// make sure we are in the correct step
		// steps are advanced on a different channel, and the
		// message advancing the step may be being processed in a different thread
		while (true)
		{
			m_data.acquireReadLock();
			try
			{
				if (m_data.getSequence().getStep().getName().equals(stepName))
					break;
			}
				finally
				{
					m_data.releaseReadLock();
				}
				
				try
				{
					Thread.sleep(50);
				} catch (InterruptedException e)
			{
				// no worries mate
			}
			
		}
		
		IGamePlayer gp = m_gamePlayers.get(player);
		
		if (gp == null)
			throw new IllegalStateException("Game player not found. Player:" + player + " on:" + m_channelMessenger.getLocalNode());
		
		gp.start(stepName);
		
	}
		
	};
	
	/**
	 * Clients cant save because they do not have the delegate data.
	 * It would be easy to get the server to save the game, and send the
	 * data to the client, I just havent bothered.
	 */
	@Override
	public boolean canSave()
	{
		return false;
	}
	
	@Override
	public IRandomSource getRandomSource()
	{
		return null;
	}
	
	/* 
	 * @see games.strategy.engine.framework.IGame#getVault()
	 */
	@Override
	public Vault getVault()
	{
		return m_vault;
	}
	
	/* 
	 * @see games.strategy.engine.framework.IGame#addDisplay(games.strategy.engine.display.IDisplay)
	 */
	@Override
	public void addDisplay(IDisplay display)
	{
		display.initialize(new DefaultDisplayBridge(m_data));
		m_channelMessenger.registerChannelSubscriber(display, ServerGame.getDisplayChannel(getData()));
		
	}
	
	/* 
	 * @see games.strategy.engine.framework.IGame#removeDisplay(games.strategy.engine.display.IDisplay)
	 */
	@Override
	public void removeDisplay(IDisplay display)
	{
		m_channelMessenger.unregisterChannelSubscriber(display, ServerGame.getDisplayChannel(getData()));
	}
	
	@Override
	public boolean isGameOver()
	{
		return m_isGameOver;
	}
	
	@Override
	public PlayerManager getPlayerManager()
	{
		return m_playerManager;
	}
	
	@Override
	public void saveGame(File f)
	{
		IServerRemote server = (IServerRemote) m_remoteMessenger.getRemote(ServerGame.SERVER_REMOTE);
		byte[] bytes = server.getSavedGame();
		FileOutputStream fout = null;
		try
		{
			fout = new FileOutputStream(f);
			fout.write(bytes);
		} catch (IOException e)
		{
			e.printStackTrace();
			throw new IllegalStateException(e.getMessage());
		} finally
		{
			if (fout != null)
				try
				{
					fout.close();
				} catch (IOException e)
				{
					e.printStackTrace();
				}
		}
	}
	
}
