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

import games.strategy.engine.history.*;
import games.strategy.engine.random.*;
import games.strategy.engine.vault.Vault;

/**
 *
 * @author  Sean Bridges
 */
public class ClientGame implements IGame
{
  private ListenerList m_gameStepListeners = new ListenerList();
  private final GameData m_data;
  private final IMessenger m_messenger;
  private final IMessageManager m_messageManager;
  private final IRemoteMessenger m_remoteMessenger;
  private final IChannelMessenger m_channelMessenger;
  private final ChangePerformer m_changePerformer;
  private final INode m_serverNode;
  //maps PlayerID->GamePlayer
  private Map m_gamePlayers = new HashMap();
  private final Vault m_vault;
  
  public static final String STEP_CHANGE_LISTENER_DESTINATION = "_StepChangeListener_";

  private int m_currentRound = -1;

  public ClientGame(GameData data, Set gamePlayers, IMessenger messenger, INode server, IChannelMessenger channelMessenger)
  {
    m_data = data;
    m_serverNode = server;
    m_messenger = messenger;
    m_messenger.addMessageListener(m_messageListener);
    m_messageManager = new MessageManager(m_messenger);
    m_remoteMessenger = new RemoteMessenger(m_messageManager, m_messenger);
    m_channelMessenger = channelMessenger;
    m_vault = new Vault(m_channelMessenger);
    
    m_messageManager.addDestination(m_stepChangeDestination);


    Iterator iter = gamePlayers.iterator();
    while(iter.hasNext())
    {
      IGamePlayer gp = (IGamePlayer) iter.next();
      PlayerID player = m_data.getPlayerList().getPlayerID(gp.getName());
      m_gamePlayers.put(player, gp);

      IPlayerBridge bridge = new DefaultPlayerBridge(this);
      gp.initialize(bridge, player);

      m_messageManager.addDestination(gp);
      m_remoteMessenger.registerRemote(gp.getRemotePlayerType(), gp, ServerGame.getRemoteName(gp.getID()));
      
      IRemoteRandom remoteRandom = new RemoteRandom(this);
      m_remoteMessenger.registerRemote(IRemoteRandom.class, remoteRandom, ServerGame.getRemoteRandomName(player));
      
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

  public IChannelMessenger getChannelMessenger()
  {
      return m_channelMessenger;
  }
  public IRemoteMessenger getRemoteMessenger()
  {
      return m_remoteMessenger;
  }
  
  public void addGameStepListener(GameStepListener listener)
  {
    m_gameStepListeners.add(listener);
  }

  public void removeGameStepListener(GameStepListener listener)
  {
    m_gameStepListeners.remove(listener);
  }

  private boolean m_firstRun = true;
  private void notifyGameStepChanged(StepChangedMessage msg)
  {
    //we want to skip the first iteration, since that simply advances us to step 0
    if(m_firstRun)
      m_firstRun = false;
    else
    {
      m_data.getSequence().next();
      while (!m_data.getSequence().getStep().getName().equals(msg.getStepName()))
      {
        m_data.getSequence().next();
      }
    }
    Iterator iter = m_gameStepListeners.iterator();
    while(iter.hasNext())
    {
      GameStepListener listener = (GameStepListener) iter.next();
      listener.gameStepChanged(msg.getStepName(), msg.getDelegateName(), msg.getPlayer(), msg.getRound(), msg.getDisplayName());
    }
  }

  public void addChange(Change aChange)
  {
    throw new UnsupportedOperationException();
  }

  private IDestination m_stepChangeDestination = new IDestination()
  {

    public Message sendMessage(Message message)
    {
        if(message instanceof StepChangedMessage)
        {
          StepChangedMessage stepChange = (StepChangedMessage) message;

          if(m_currentRound != stepChange.getRound())
          {
              m_currentRound = stepChange.getRound();
              m_data.getHistory().getHistoryWriter().startNextRound(m_currentRound);
          }
          m_data.getHistory().getHistoryWriter().startNextStep(stepChange.getStepName(), stepChange.getDelegateName(), stepChange.getPlayer(), stepChange.getDisplayName());

          notifyGameStepChanged(stepChange);
          return null;
        }
        else 
            throw new IllegalStateException("Message not recognized:" + message);
    }

    public String getName()
    {
        return m_messenger.getLocalNode() + STEP_CHANGE_LISTENER_DESTINATION;
    }
      
  };
  
  

  private IMessageListener m_messageListener = new IMessageListener()
  {
    public void messageReceived(Serializable msg, INode from)
    {
      if(msg instanceof ChangeMessage)
      {
        ChangeMessage changeMessage = (ChangeMessage) msg;
        m_data.getHistory().getHistoryWriter().addChange(changeMessage.getChange());
        m_changePerformer.perform(changeMessage.getChange());

      }
      else if(msg instanceof PlayerStartStepMessage)
      {
        PlayerStartStepMessage playerStart = (PlayerStartStepMessage) msg;
        IGamePlayer gp = (IGamePlayer) m_gamePlayers.get(playerStart.getPlayerID());

        if(gp == null)
          throw new IllegalStateException("Game player not found" + playerStart);

        gp.start(playerStart.getStepName());

        PlayerStepEndedMessage response = new PlayerStepEndedMessage(playerStart.getStepName());
        m_messenger.send(response, m_serverNode);
      }
      else if(msg instanceof RemoteHistoryMessage)
      {
          ((RemoteHistoryMessage) msg).perform(m_data.getHistory().getHistoryWriter());
      }
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

  public void shutdown()
  {
    m_messenger.shutDown();
  }
  public IRandomSource getRandomSource()
  {
    return null;
  }

	/* 
	 * @see games.strategy.engine.framework.IGame#getVault()
	 */
	public Vault getVault()
	{
	    return m_vault;
	}


}
