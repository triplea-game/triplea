/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * ClientGame.java
 *
 * Created on December 14, 2001, 12:48 PM
 */

package games.strategy.engine.framework;

import java.util.*;
import java.io.Serializable;

import games.strategy.engine.data.*;
import games.strategy.engine.data.events.GameStepListener;
import games.strategy.engine.gamePlayer.*;
import games.strategy.engine.message.*;
import games.strategy.net.*;
import games.strategy.util.ListenerList;
import games.strategy.engine.transcript.*;

/**
 *
 * @author  Sean Bridges
 */
public class ClientGame implements IGame
{
	private ListenerList m_gameStepListeners = new ListenerList();
	private GameData m_data;
	private IMessenger m_messenger;
	private IMessageManager m_messageManager;
	private ChangePerformer m_changePerformer;
	private INode m_serverNode;
	//maps PlayerID->GamePlayer
	private Map m_gamePlayers = new HashMap(); 
	private Transcript m_transcript;
	
	public ClientGame(GameData data, Set gamePlayers, IMessenger messenger, INode server) 
	{
		m_data = data;
		m_serverNode = server;
		m_messenger = messenger;
		m_messenger.addMessageListener(m_messageListener);
		m_messageManager = new MessageManager(m_messenger);
		m_transcript = new Transcript(m_messenger);
		
		Iterator iter = gamePlayers.iterator();
		while(iter.hasNext())
		{
			GamePlayer gp = (GamePlayer) iter.next();
			PlayerID player = m_data.getPlayerList().getPlayerID(gp.getName());
			m_gamePlayers.put(player, gp);
			
			PlayerBridge bridge = new DefaultPlayerBridge(this, gp);
			gp.initialize(bridge, player);
			
			m_messageManager.addDestination(gp);
		}
		
		m_changePerformer = new ChangePerformer(m_data);
    }
	
	public IMessageManager getMessageManager()
	{
		return m_messageManager;
	}	

	public GameData getData()
	{
		return m_data;
	}
	
	public IMessenger getMessenger()
	{
		return m_messenger;
	}
	
	public void addGameStepListener(GameStepListener listener)
	{	
		m_gameStepListeners.add(listener);
	}
	
	public void removeGameStepListener(GameStepListener listener)
	{
		m_gameStepListeners.remove(listener);
	}
	
	private void notifyGameStepChanged(String stepName, String delegateName, PlayerID id)
	{
		Iterator iter = m_gameStepListeners.iterator();
		while(iter.hasNext())
		{
			GameStepListener listener = (GameStepListener) iter.next();
			listener.gameStepChanged(stepName, delegateName, id);
		}
	}
	
	public void addChange(Change aChange)
	{
		throw new UnsupportedOperationException();
	}
	
	public Transcript getTranscript()
	{
		return m_transcript;
	}
	
	private IMessageListener m_messageListener = new IMessageListener()
	{
		public void messageReceived(Serializable msg, INode from)
		{
			if(msg instanceof StepChangedMessage)
			{
				StepChangedMessage stepChange = (StepChangedMessage) msg;
				notifyGameStepChanged(stepChange.getStepName(), stepChange.getDelegateName(), stepChange.getPlayer());
			}
			else if(msg instanceof ChangeMessage)
			{
				ChangeMessage changeMessage = (ChangeMessage) msg;
				m_changePerformer.perform(changeMessage.getChange());
			}
			else if(msg instanceof PlayerStartStepMessage)
			{
				PlayerStartStepMessage playerStart = (PlayerStartStepMessage) msg;
				GamePlayer gp = (GamePlayer) m_gamePlayers.get(playerStart.getPlayerID());
				
				if(gp == null)
					throw new IllegalStateException("Game player not found" + playerStart);
	
				gp.start(playerStart.getStepName());
				
				PlayerStepEndedMessage response = new PlayerStepEndedMessage(playerStart.getStepName());
				m_messenger.send(response, m_serverNode);			
			}
		}
	};
	

}
