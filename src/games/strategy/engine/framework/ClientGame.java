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

import games.strategy.engine.data.*;
import games.strategy.engine.data.events.GameStepListener;
import games.strategy.engine.display.*;
import games.strategy.engine.gamePlayer.*;
import games.strategy.engine.history.EventChild;
import games.strategy.engine.random.*;
import games.strategy.engine.vault.Vault;
import games.strategy.net.*;
import games.strategy.util.ListenerList;

import java.util.*;

/**
 *
 * @author  Sean Bridges
 */
public class ClientGame implements IGame
{
  private ListenerList m_gameStepListeners = new ListenerList();
  private final GameData m_data;
  private final IMessenger m_messenger;
  private final IRemoteMessenger m_remoteMessenger;
  private final IChannelMessenger m_channelMessenger;
  private final ChangePerformer m_changePerformer;
  
  //maps PlayerID->GamePlayer
  private Map m_gamePlayers = new HashMap();
  private final Vault m_vault;
  
  public static final String getRemoteStepAdvancerName(INode node)
  {
      return "games.strategy.engine.framework.ClientGame.REMOTE_STEP_ADVANCER:" + node.getName();  
  }

  public ClientGame(GameData data, Set gamePlayers, IMessenger messenger, IChannelMessenger channelMessenger, IRemoteMessenger remoteMessenger)
  {
    m_data = data;
    
    m_messenger = messenger;
    
    m_remoteMessenger = remoteMessenger;
    m_channelMessenger = channelMessenger;
    m_vault = new Vault(m_channelMessenger);
    
    m_channelMessenger.registerChannelSubscriber(m_gameModificationChannelListener, IGame.GAME_MODIFICATION_CHANNEL);
    m_remoteMessenger.registerRemote(IGameStepAdvancer.class, m_gameStepAdvancer, getRemoteStepAdvancerName(m_channelMessenger.getLocalNode()));


    Iterator iter = gamePlayers.iterator();
    while(iter.hasNext())
    {
      IGamePlayer gp = (IGamePlayer) iter.next();
      PlayerID player = m_data.getPlayerList().getPlayerID(gp.getName());
      m_gamePlayers.put(player, gp);

      IPlayerBridge bridge = new DefaultPlayerBridge(this);
      gp.initialize(bridge, player);

      m_remoteMessenger.registerRemote(gp.getRemotePlayerType(), gp, ServerGame.getRemoteName(gp.getID()));
      
      IRemoteRandom remoteRandom = new RemoteRandom(this);
      m_remoteMessenger.registerRemote(IRemoteRandom.class, remoteRandom, ServerGame.getRemoteRandomName(player));
      
    }

    m_changePerformer = new ChangePerformer(m_data);
  }
  
  private IGameModifiedChannel m_gameModificationChannelListener = new IGameModifiedChannel()
  {

    public void gameDataChanged(Change aChange)
    {
        m_changePerformer.perform(aChange);
        m_data.getHistory().getHistoryWriter().addChange(aChange);
    }

    public void startHistoryEvent(String event)
    {
        m_data.getHistory().getHistoryWriter().startEvent(event);
        
    }

    public void addChildToEvent(String text, Object renderingData)
    {
        m_data.getHistory().getHistoryWriter().addChildToEvent(new EventChild(text, renderingData));
        
    }

    public void setRenderingData(Object renderingData)
    {
        m_data.getHistory().getHistoryWriter().setRenderingData(renderingData);
        
    }

    public void stepChanged(String stepName, String delegateName, PlayerID player, int round, String displayName)
    {
        //we want to skip the first iteration, since that simply advances us to step 0
        if(m_firstRun)
          m_firstRun = false;
        else
        {
          m_data.getSequence().next();
          while (!m_data.getSequence().getStep().getName().equals(stepName))
          {
            m_data.getSequence().next();
          }
        }
        Iterator iter = m_gameStepListeners.iterator();
        while(iter.hasNext())
        {
          GameStepListener listener = (GameStepListener) iter.next();
          listener.gameStepChanged(stepName, delegateName, player, round, displayName);
        }
        m_data.getHistory().getHistoryWriter().startNextStep(stepName, delegateName, player, displayName);
        
    }
     
  };
 
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


  public void addChange(Change aChange)
  {
    throw new UnsupportedOperationException();
  }


  
  

  private IGameStepAdvancer m_gameStepAdvancer = new IGameStepAdvancer()
  {
      public void startPlayerStep(String stepName, PlayerID player)
    {
      
        
        //make sure we are in the correct step
        //steps are advanced on a different channel, and the
        //message advancing the step may be being processed in a different thread
        while(!m_data.getSequence().getStep().getName().equals(stepName))
        {
            try
            {
                Thread.sleep(50);
            } catch (InterruptedException e)
            {
                //no worries mate
            }
        }
        
        IGamePlayer gp = (IGamePlayer) m_gamePlayers.get(player);

        if(gp == null)
          throw new IllegalStateException("Game player not found. Player:" + player + " on:" + m_channelMessenger.getLocalNode());

        gp.start(stepName);

        
       
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

    /* 
     * @see games.strategy.engine.framework.IGame#addDisplay(games.strategy.engine.display.IDisplay)
     */
    public void addDisplay(IDisplay display)
    {
        display.initialize(new DefaultDisplayBridge(m_data));
        m_channelMessenger.registerChannelSubscriber(display, ServerGame.DISPLAY_CHANNEL);
        
    }

    /* 
     * @see games.strategy.engine.framework.IGame#removeDisplay(games.strategy.engine.display.IDisplay)
     */
    public void removeDisplay(IDisplay display)
    {
        m_channelMessenger.unregisterChannelSubscriber(display, ServerGame.DISPLAY_CHANNEL);
    }
	
	
}
