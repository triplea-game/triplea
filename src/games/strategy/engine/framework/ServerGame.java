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
 * Game.java
 *
 * Created on October 27, 2001, 6:39 PM
 */

package games.strategy.engine.framework;

import java.io.*;

import java.util.*;

import games.strategy.util.ListenerList;
import games.strategy.engine.data.*;
import games.strategy.engine.data.events.*;
import games.strategy.engine.delegate.*;
import games.strategy.engine.gamePlayer.*;
import games.strategy.engine.message.*;
import games.strategy.net.*;

import games.strategy.engine.framework.ui.SaveGameFileChooser;
import games.strategy.engine.history.*;
import games.strategy.engine.random.*;
import games.strategy.engine.vault.Vault;

/**
 *
 * @author  Sean Bridges
 *
 * Represents a running game.
 * Lookups to get a GamePlayer from PlayerId and the current Delegate.
 */
public class ServerGame implements IGame
{
    private ListenerList m_gameStepListeners = new ListenerList();
    private final GameData m_data;

    //maps PlayerID->GamePlayer
    private final Map m_gamePlayers = new HashMap();

    private final IServerMessenger m_messenger;
    private final IMessageManager m_messageManager;
    private final ChangePerformer m_changePerformer;
    
    private final IRemoteMessenger m_remoteMessenger;
    private final IChannelMessenger m_channelMessenger;
    private final Vault m_vault;

    //maps playerName -> INode
    //only for remote nodes
    private final Map m_remotePlayers;
    private final Object m_remotePlayerStepLock = new Object();
    private final RandomStats m_randomStats;

    private IRandomSource m_randomSource = new PlainRandomSource();

    /**
     *
     * @param localPlayers Set - A set of GamePlayers
     * @param messenger IServerMessenger
     * @param remotePlayerMapping Map
     */
    public ServerGame(GameData data, Set localPlayers, IServerMessenger messenger, Map remotePlayerMapping, IChannelMessenger channelMessenger)
    {
        m_data = data;

        m_messenger = messenger;
        m_messenger.addMessageListener(m_messageListener);
        m_messageManager = new MessageManager(m_messenger);
        m_remoteMessenger = new RemoteMessenger(m_messageManager, m_messenger);
        m_channelMessenger = channelMessenger;
        m_vault = new Vault(m_channelMessenger);
        
        m_remotePlayers = new HashMap(remotePlayerMapping);


        setupLocalPlayers(localPlayers);

        //add a null destination for the null player.
        IDestination nullDest = new IDestination()
        {
            public Message sendMessage(Message message)
            {
                return null;
            }

            public String getName()
            {
                return PlayerID.NULL_PLAYERID.getName();
            }
        };

        m_messageManager.addDestination(nullDest);

        setupDelegateMessaging(data);

        m_changePerformer = new ChangePerformer(m_data);
        m_randomStats = new RandomStats(m_messageManager);
    }

    /**
     * @param localPlayers
     */
    private void setupLocalPlayers(Set localPlayers)
    {
  
        Iterator localPlayersIter = localPlayers.iterator();
        while (localPlayersIter.hasNext())
        {
            IGamePlayer gp = (IGamePlayer) localPlayersIter.next();
            PlayerID player = m_data.getPlayerList().getPlayerID(gp.getName());
            m_gamePlayers.put(player, gp);
            IPlayerBridge bridge = new DefaultPlayerBridge(this);
            gp.initialize(bridge, player);
            m_messageManager.addDestination(gp);
            m_remoteMessenger.registerRemote(gp.getRemotePlayerType(), gp, getRemoteName(gp.getID()));

        }
    }

    private void setupDelegateMessaging(GameData data)
    {
        Iterator delegateIter = data.getDelegateList().iterator();
        while (delegateIter.hasNext())
        {
            IDelegate delegate = (IDelegate) delegateIter.next();
            
            Class remoteType = delegate.getRemoteType();
            //if its null then it shouldnt be added as an IRemote
            if(remoteType == null)
                continue;
            m_remoteMessenger.registerRemote(delegate.getRemoteType(), delegate, getRemoteName(delegate));
        }
    }
    
    public static String getRemoteName(IDelegate delegate)
    {
        return "games.strategy.engine.framework.ServerGame.DELEGATE_REMOTE." + delegate.getName();
    }
    
    public static String getRemoteName(PlayerID id)
    {
        return "games.strategy.engine.framework.ServerGame.PLAYER_REMOTE." + id.getName();
    }

    public static String getRemoteRandomName(PlayerID id)
    {
        return "games.strategy.engine.framework.ServerGame.PLAYER_RANDOM_REMOTE" + id.getName();
    }
    
    public GameData getData()
    {
        return m_data;
    }

    private GameStep getCurrentStep()
    {
        return m_data.getSequence().getStep();
        // m_data.getSequence().getStep(m_currentStepIndex);
    }

    /**
     * And here we go.
     * Starts the game in a new thread
     */
    public void startGame()
    {
        while (true)
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
        if (getCurrentStep().hasReachedMaxRunCount())
        {
            m_data.getSequence().next();
            startNextStep();
            return;
        }

        
        DefaultDelegateBridge bridge = new DefaultDelegateBridge(
                m_data, 
                getCurrentStep(), this, 
                new DelegateHistoryWriter(m_data.getHistory().getHistoryWriter(), m_messenger),
                m_randomStats
                );
        bridge.setRandomSource(m_randomSource);


        notifyGameStepChanged();
        getCurrentStep().getDelegate().start(bridge, m_data);

        waitForPlayerToFinishStep();
        getCurrentStep().getDelegate().end();
        getCurrentStep().incrementRunCount();
        if(m_data.getSequence().next())
        {
            m_data.getHistory().getHistoryWriter().startNextRound(m_data.getSequence().getRound());
        }

        try
        {
            if (m_data.getSequence().getStep().getName().indexOf("EndTurn") != -1)
            {
                SaveGameFileChooser.ensureDefaultDirExists();
                File autosaveFile = new File(SaveGameFileChooser.DEFAULT_DIRECTORY, SaveGameFileChooser.AUTOSAVE_FILE_NAME);
                System.out.print("Autosaving...");
                new GameDataManager().saveGame(autosaveFile, m_data);
                System.out.println("done");
            }
            

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    private void waitForPlayerToFinishStep()
    {
        PlayerID playerID = getCurrentStep().getPlayerID();
        //no player specified for the given step
        if (playerID == null)
            return;

        IGamePlayer player = (IGamePlayer) m_gamePlayers.get(playerID);

        if (player != null)
        {
            //a local player
            player.start(getCurrentStep().getName());
        }
        else
        {
            //a remote player
            INode destination = (INode) m_remotePlayers.get(playerID.getName());

            synchronized (m_remotePlayerStepLock)
            {
                PlayerStartStepMessage msg = new PlayerStartStepMessage(getCurrentStep().getName(), playerID);

                m_messenger.send(msg, destination);
                try
                {
                    m_remotePlayerStepLock.wait();
                }
                catch (InterruptedException ie)
                {
                    ie.printStackTrace();
                }
            }
        }
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
        String displayName = getCurrentStep().getDisplayName();
        PlayerID id = getCurrentStep().getPlayerID();

        m_data.getHistory().getHistoryWriter().startNextStep(stepName, delegateName, id, displayName);

        Iterator iter = m_gameStepListeners.iterator();
        while (iter.hasNext())
        {
            GameStepListener listener = (GameStepListener) iter.next();
            listener.gameStepChanged(stepName, delegateName, id, m_data.getSequence().getRound(), getCurrentStep().getDisplayName());
        }

        StepChangedMessage msg = new StepChangedMessage(stepName, delegateName, id, m_data.getSequence().getRound(), displayName);
        
        Set destinations = new HashSet();
        
        iter = m_messenger.getNodes().iterator();
        while(iter.hasNext())
        {
            INode node = (INode) iter.next();
            if(!node.equals(m_messenger.getLocalNode()))
            {
                destinations.add(node.toString() + ClientGame.STEP_CHANGE_LISTENER_DESTINATION);
            }
        }
        
        //we want to wait for everyone here, otherwise the client may receive and act upon the message 
        //to start the next step before he has finished processing the step change message
        m_messageManager.broadcastAndWait(msg, destinations);
        
        
        
    }

    public IMessenger getMessenger()
    {
        return m_messenger;
    }

    public IMessageManager getMessageManager()
    {
        return m_messageManager;
    }
    
    public IChannelMessenger getChannelMessenger()
    {
        return m_channelMessenger;
    }
    public IRemoteMessenger getRemoteMessenger()
    {
        return m_remoteMessenger;
    }

    public void addChange(Change aChange)
    {
        m_changePerformer.perform(aChange);
        ChangeMessage msg = new ChangeMessage(aChange);
        m_messenger.broadcast(msg);
        m_data.getHistory().getHistoryWriter().addChange(aChange);
    }

    private IMessageListener m_messageListener = new IMessageListener()
    {
        public void messageReceived(Serializable msg, INode from)
        {
            if (msg instanceof PlayerStepEndedMessage)
            {
                synchronized (m_remotePlayerStepLock)
                {
                    m_remotePlayerStepLock.notifyAll();
                }
            }
        }
    };

    public boolean canSave()
    {
        return true;
    }

    public void shutdown()
    {
        m_messenger.shutDown();
    }

    public IRandomSource getRandomSource()
    {
      return m_randomSource;
    }

    public void setRandomSource(IRandomSource randomSource)
    {
      m_randomSource = randomSource;
    }

    /* 
     * @see games.strategy.engine.framework.IGame#getVault()
     */
    public Vault getVault()
    {
        return m_vault;
    }
}
