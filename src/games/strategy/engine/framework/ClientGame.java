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
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.history.EventChild;
import games.strategy.engine.message.RemoteName;
import games.strategy.engine.random.IRandomSource;
import games.strategy.engine.random.IRemoteRandom;
import games.strategy.engine.random.RemoteRandom;
import games.strategy.net.INode;
import games.strategy.net.Messengers;
import games.strategy.triplea.ui.ErrorHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author Sean Bridges
 */
public class ClientGame extends AbstractGame
{
	public static final RemoteName getRemoteStepAdvancerName(final INode node)
	{
		return new RemoteName("games.strategy.engine.framework.ClientGame.REMOTE_STEP_ADVANCER:" + node.getName(), IGameStepAdvancer.class);
	}
	
	public ClientGame(final GameData data, final Set<IGamePlayer> gamePlayers, final Map<String, INode> remotePlayerMapping, final Messengers messengers)
	{
		super(data, gamePlayers, remotePlayerMapping, messengers);
		m_gameModifiedChannel = new IGameModifiedChannel()
		{
			public void gameDataChanged(final Change aChange)
			{
				m_changePerformer.perform(aChange);
				m_data.getHistory().getHistoryWriter().addChange(aChange);
			}
			
			public void startHistoryEvent(final String event, final Object renderingData)
			{
				startHistoryEvent(event);
				if (renderingData != null)
					setRenderingData(renderingData);
			}
			
			public void startHistoryEvent(final String event)
			{
				m_data.getHistory().getHistoryWriter().startEvent(event);
			}
			
			public void addChildToEvent(final String text, final Object renderingData)
			{
				m_data.getHistory().getHistoryWriter().addChildToEvent(new EventChild(text, renderingData));
			}
			
			protected void setRenderingData(final Object renderingData)
			{
				m_data.getHistory().getHistoryWriter().setRenderingData(renderingData);
			}
			
			public void stepChanged(final String stepName, final String delegateName, final PlayerID player, final int round, final String displayName, final boolean loadedFromSavedGame)
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
						final int ourOriginalCurrentRound = m_data.getSequence().getRound();
						int currentRound = ourOriginalCurrentRound;
						if (m_data.getSequence().testWeAreOnLastStep())
							m_data.getHistory().getHistoryWriter().startNextRound(++currentRound);
						while (!m_data.getSequence().getStep().getName().equals(stepName) || !m_data.getSequence().getStep().getPlayerID().equals(player)
									|| !m_data.getSequence().getStep().getDelegate().getName().equals(delegateName))
						{
							m_data.getSequence().next();
							if (m_data.getSequence().testWeAreOnLastStep())
								m_data.getHistory().getHistoryWriter().startNextRound(++currentRound);
						}
						// TODO: this is causing problems if the very last step is a client step. we end up creating a new round before the host's rounds has started.
						// right now, fixing it with a hack. but in reality we probably need to have a better way of determining when a new round has started (like with a roundChanged listener).
						if ((currentRound - 1 > round && ourOriginalCurrentRound >= round) || (currentRound > round && ourOriginalCurrentRound < round))
						{
							System.err.println("Can not create more rounds that host currently has. Host Round:" + round + " and new Client Round:" + currentRound);
							throw new IllegalStateException("Can not create more rounds that host currently has. Host Round:" + round + " and new Client Round:" + currentRound);
						}
					} finally
					{
						m_data.releaseWriteLock();
					}
				}
				if (!loadedFromSavedGame)
					m_data.getHistory().getHistoryWriter().startNextStep(stepName, delegateName, player, displayName);
				notifyGameStepListeners(stepName, delegateName, player, round, displayName);
			}
			
			public void shutDown()
			{
				ClientGame.this.shutDown();
			}
		};
		m_channelMessenger.registerChannelSubscriber(m_gameModifiedChannel, IGame.GAME_MODIFICATION_CHANNEL);
		m_remoteMessenger.registerRemote(m_gameStepAdvancer, getRemoteStepAdvancerName(m_channelMessenger.getLocalNode()));
		for (final PlayerID player : m_gamePlayers.keySet())
		{
			final IRemoteRandom remoteRandom = new RemoteRandom(this);
			m_remoteMessenger.registerRemote(remoteRandom, ServerGame.getRemoteRandomName(player));
		}
	}
	
	public void shutDown()
	{
		if (m_isGameOver)
			return;
		m_isGameOver = true;
		ErrorHandler.setGameOver(true);
		try
		{
			m_channelMessenger.unregisterChannelSubscriber(m_gameModifiedChannel, IGame.GAME_MODIFICATION_CHANNEL);
			m_remoteMessenger.unregisterRemote(getRemoteStepAdvancerName(m_channelMessenger.getLocalNode()));
			m_vault.shutDown();
			for (final IGamePlayer gp : m_gamePlayers.values())
			{
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
				m_remoteMessenger.unregisterRemote(ServerGame.getRemoteName(gp.getPlayerID(), m_data));
				m_remoteMessenger.unregisterRemote(ServerGame.getRemoteRandomName(player));
			}
		} catch (final RuntimeException re)
		{
			re.printStackTrace();
		}
		m_data.getGameLoader().shutDown();
	}
	
	public void addChange(final Change aChange)
	{
		throw new UnsupportedOperationException();
	}
	
	private final IGameStepAdvancer m_gameStepAdvancer = new IGameStepAdvancer()
	{
		public void startPlayerStep(final String stepName, final PlayerID player)
		{
			// make sure we are in the correct step
			// steps are advanced on a different channel, and the
			// message advancing the step may be being processed in a different thread
			{
				int i = 0;
				boolean shownErrorMessage = false;
				while (true)
				{
					m_data.acquireReadLock();
					try
					{
						if (m_data.getSequence().getStep().getName().equals(stepName))
							break;
					} finally
					{
						m_data.releaseReadLock();
					}
					try
					{
						Thread.sleep(50);
						i++;
						if (i > 1000 && !shownErrorMessage)
						{
							System.err.println("Waited more than 30 seconds for step to update. Something wrong.");
							shownErrorMessage = true;
							// TODO: should we throw an illegal state error? or just return? or a game over exception?
							// TODO: should we request the server to send the step update again or something?
						}
					} catch (final InterruptedException e)
					{
						// no worries mate
					}
				}
			}
			final IGamePlayer gp = m_gamePlayers.get(player);
			if (gp == null)
				throw new IllegalStateException("Game player not found. Player:" + player + " on:" + m_channelMessenger.getLocalNode());
			if (HeadlessGameServer.headless())
			{ // TODO: can delete this after we discover why game freezes/hangs with a ClassCastException in start method
				System.out.println("Client local player step: " + stepName + " for PlayerID: " + player.getName() + ", player name: " + gp.getName() + ", player type: "
							+ gp.getType() + ". All local players: " + m_gamePlayers + ". All players: " + m_playerManager);
			}
			// try
			// {
			gp.start(stepName);
			/*} catch (final ClassCastException e)
			{
				e.printStackTrace();
				// TODO: Veqryn: We are getting a ClassCastException here occasionally for hosts. This is doubly weird because we are getting it for host-bots (HeadlessGameServer's) who are not even playing.
				// Which for the bot means that it is neither a client, nor playing any players (not even AI), so we should NEVER end up here (doubly never).
				// My only guess is that someone disconnects at some very sensitive point, and then the host runs the wrong player step as a client step for the wrong node (the server node).
				// I am hoping that we are in the middle of getting a connection lost error, and therefore we can just print the stack trace and ignore, letting the connection lost error do the work.
			}*/
		}
	};
	
	/**
	 * Clients cant save because they do not have the delegate data.
	 * It would be easy to get the server to save the game, and send the
	 * data to the client, I just havent bothered.
	 */
	public boolean canSave()
	{
		return false;
	}
	
	public IRandomSource getRandomSource()
	{
		return null;
	}
	
	public void saveGame(final File f)
	{
		final IServerRemote server = (IServerRemote) m_remoteMessenger.getRemote(ServerGame.SERVER_REMOTE);
		final byte[] bytes = server.getSavedGame();
		FileOutputStream fout = null;
		try
		{
			fout = new FileOutputStream(f);
			fout.write(bytes);
		} catch (final IOException e)
		{
			e.printStackTrace();
			throw new IllegalStateException(e.getMessage());
		} finally
		{
			if (fout != null)
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
