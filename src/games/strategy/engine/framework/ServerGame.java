/*
 * Game.java
 *
 * Created on October 27, 2001, 6:39 PM
 */

package games.strategy.engine.framework;

import java.io.*;
import java.net.URL;
import java.util.*;

import org.xml.sax.SAXException;

import games.strategy.util.ListenerList;
import games.strategy.engine.data.*;
import games.strategy.engine.data.events.*;
import games.strategy.engine.delegate.*;
import games.strategy.engine.gamePlayer.*;
import games.strategy.engine.message.*;
import games.strategy.engine.xml.*;
import games.strategy.net.*;
import games.strategy.engine.transcript.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * Represents a running game.
 * Lookups to get a GamePlayer from PlayerId and the current Delegate.
 */
public class ServerGame implements IGame
{
	private ListenerList m_gameStepListeners = new ListenerList();
	private GameData m_data;
	//maps PlayerID->GamePlayer
	private Map m_gamePlayers = new HashMap(); 
	private int m_currentStepIndex = -1;
	private IServerMessenger m_messenger;
	private IMessageManager m_messageManager;
	private ChangePerformer m_changePerformer;
	//maps playerName -> INode
	//only for remote nodes
	private Map m_playerMapping;
	private Object m_remotePlayerStepLock = new Object();
	private Transcript m_transcript;
	
	
	/** Creates new Game */
    public ServerGame(GameData data, Set gamePlayers, IServerMessenger messenger, Map playerMapping) 
	{
		m_data = data;
		
		m_messenger = messenger;
		m_messenger.addMessageListener(m_messageListener);
		
		m_transcript = new Transcript(m_messenger);
		
		m_playerMapping = new HashMap(playerMapping);
		
		m_messageManager = new MessageManager(m_messenger);
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
		
		iter = data.getDelegateList().iterator();
		while(iter.hasNext())
		{
			Delegate delegate = (Delegate) iter.next();
			m_messageManager.addDestination(delegate);
		}
		
		m_changePerformer = new ChangePerformer(m_data);
    }

	public GameData getData()
	{
		return m_data;
	}
	
	private GamePlayer getPlayer(PlayerID aPlayer)
	{
		return (GamePlayer) m_gamePlayers.get(aPlayer);
	}

	private GameStep getCurrentStep()
	{
		return m_data.getSequence().getStep(m_currentStepIndex);
	}

	/**
	 * And here we go.
	 * Starts the game in a new thread
	 */
	public void startGame()
	{
		if(m_currentStepIndex != -1)
			throw new IllegalStateException("Game can only be started once");
		else
			while(true)
				startNextStep();
	}
	
	public void stopGame()
	{
		getCurrentStep().getDelegate().end();
	}

	public void endStep()
	{		
		getCurrentStep().getDelegate().end();
		startNextStep();
	}
	
	private void startNextStep()
	{
		m_currentStepIndex++;
		if(m_currentStepIndex == m_data.getSequence().size())
			m_currentStepIndex = 0;
		
		DelegateBridge bridge = new DefaultDelegateBridge(m_data, getCurrentStep(), this);
		getCurrentStep().getDelegate().start(bridge,m_data );
		notifyGameStepChanged();
	
		waitForPlayerToFinishStep();
		getCurrentStep().getDelegate().end();
	}
	
	private void waitForPlayerToFinishStep()
	{
		PlayerID playerID = getCurrentStep().getPlayerID();
		//no player specified for the given step
		if(playerID == null)
			return;
				
		GamePlayer player = (GamePlayer) m_gamePlayers.get(playerID);
		
		
		if(player != null)
		{
			//a local player
			player.start(getCurrentStep().getName());	
		}
		else
		{
			//a remote player
			INode destination = (INode) m_playerMapping.get(playerID.getName());
			
			synchronized(m_remotePlayerStepLock)
			{				
				PlayerStartStepMessage msg = new PlayerStartStepMessage(getCurrentStep().getName(), playerID);
				
				m_messenger.send(msg, destination);
				try
				{
					m_remotePlayerStepLock.wait();
				} catch(InterruptedException ie)
				{
					ie.printStackTrace();
				}
			}
		}
	}

	public Transcript getTranscript()
	{
		return m_transcript;
	}
	
	public void addGameStepListener(GameStepListener listener)
	{	
		m_gameStepListeners.add(listener);
	}
	
	public void removeGameStepListener(GameStepListener listener)
	{
		m_gameStepListeners.remove(listener);
	}
	
	private void notifyGameStepChanged()
	{
		String stepName = getCurrentStep().getName();
		String delegateName = getCurrentStep().getDelegate().getName();
		PlayerID id = getCurrentStep().getPlayerID();
		
		Iterator iter = m_gameStepListeners.iterator();
		while(iter.hasNext())
		{
			GameStepListener listener = (GameStepListener) iter.next();
			listener.gameStepChanged(stepName, delegateName, id);
		}
		
		StepChangedMessage msg = new StepChangedMessage(stepName, delegateName, id);
		m_messenger.broadcast(msg);
	}
	
	public IMessenger getMessenger()
	{
		return m_messenger;
	}
	
	public IMessageManager getMessageManager()
	{
		return m_messageManager;
	}	
	
	public void addChange(Change aChange)
	{
		m_changePerformer.perform(aChange);
		ChangeMessage msg = new ChangeMessage(aChange);
		m_messenger.broadcast(msg);
	}
	
	private IMessageListener m_messageListener = new IMessageListener()
	{
		public void messageReceived(Serializable msg, INode from)
		{
			if(msg instanceof PlayerStepEndedMessage)
			{
				synchronized(m_remotePlayerStepLock)
				{
					m_remotePlayerStepLock.notifyAll();
				} 
			}
		}
	};
	
}