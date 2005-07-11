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

import games.strategy.engine.data.*;
import games.strategy.engine.data.events.GameStepListener;
import games.strategy.engine.delegate.*;
import games.strategy.engine.display.*;
import games.strategy.engine.framework.ui.SaveGameFileChooser;
import games.strategy.engine.gamePlayer.*;
import games.strategy.engine.history.*;
import games.strategy.engine.message.*;
import games.strategy.engine.random.*;
import games.strategy.engine.vault.Vault;
import games.strategy.net.*;
import games.strategy.util.ListenerList;

import java.io.File;
import java.util.*;

/**
 *
 * @author  Sean Bridges
 *
 * Represents a running game.
 * Lookups to get a GamePlayer from PlayerId and the current Delegate.
 */
public class ServerGame implements IGame
{
    public static final String DISPLAY_CHANNEL = "games.strategy.engine.framework.ServerGame.DISPLAY_CHANNEL";
    
    private ListenerList<GameStepListener> m_gameStepListeners = new ListenerList<GameStepListener>();
    private final GameData m_data;

    //maps PlayerID->GamePlayer
    private final Map<PlayerID, IGamePlayer> m_gamePlayers = new HashMap<PlayerID, IGamePlayer>();

    private final IServerMessenger m_messenger;
    private final ChangePerformer m_changePerformer;
    
    private final IRemoteMessenger m_remoteMessenger;
    private final IChannelMessenger m_channelMessenger;
    private final Vault m_vault;

    //maps playerName -> INode
    //only for remote nodes
    private final Map<String,INode> m_remotePlayers;
    
    private final RandomStats m_randomStats;

    private IRandomSource m_randomSource = new PlainRandomSource();

    /**
     *
     * @param localPlayers Set - A set of GamePlayers
     * @param messenger IServerMessenger
     * @param remotePlayerMapping Map
     */
    public ServerGame(GameData data, Set<IGamePlayer> localPlayers, IServerMessenger messenger, Map<String,INode> remotePlayerMapping, IChannelMessenger channelMessenger, IRemoteMessenger remoteMessenger)
    {
        m_data = data;

        m_messenger = messenger;
        
        
        m_remoteMessenger = remoteMessenger;
        m_channelMessenger = channelMessenger;
        m_vault = new Vault(m_channelMessenger, m_remoteMessenger);
        
        m_remotePlayers = new HashMap<String,INode>(remotePlayerMapping);

        m_channelMessenger.createChannel(IGameModifiedChannel.class, IGame.GAME_MODIFICATION_CHANNEL);
        m_channelMessenger.registerChannelSubscriber(m_gameModifiedChannel, IGame.GAME_MODIFICATION_CHANNEL);
        
        m_channelMessenger.createChannel(data.getGameLoader().getDisplayType(), DISPLAY_CHANNEL);
        
        
        setupLocalPlayers(localPlayers);

        setupDelegateMessaging(data);

        m_changePerformer = new ChangePerformer(data);
        m_randomStats = new RandomStats(m_remoteMessenger);
    }

    /**
     * @param localPlayers
     */
    private void setupLocalPlayers(Set<IGamePlayer> localPlayers)
    {
  
        Iterator<IGamePlayer> localPlayersIter = localPlayers.iterator();
        while (localPlayersIter.hasNext())
        {
            IGamePlayer gp = localPlayersIter.next();
            PlayerID player = m_data.getPlayerList().getPlayerID(gp.getName());
            m_gamePlayers.put(player, gp);
            IPlayerBridge bridge = new DefaultPlayerBridge(this);
            gp.initialize(bridge, player);
            
            m_remoteMessenger.registerRemote(m_data.getGameLoader().getRemotePlayerType(), gp, getRemoteName(gp.getID()));

        }
    }

    private void setupDelegateMessaging(GameData data)
    {
        Iterator delegateIter = data.getDelegateList().iterator();
        while (delegateIter.hasNext())
        {
            IDelegate delegate = (IDelegate) delegateIter.next();
            
            Class<? extends IRemote> remoteType = delegate.getRemoteType();
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
            runStep();
    }

    public void stopGame()
    {
        getCurrentStep().getDelegate().end();
    }

	private void autoSave() {
		try
        {
            SaveGameFileChooser.ensureDefaultDirExists();
            File autosaveFile = new File(SaveGameFileChooser.DEFAULT_DIRECTORY, SaveGameFileChooser.AUTOSAVE_FILE_NAME);
            System.out.print("Autosaving...");
            new GameDataManager().saveGame(autosaveFile, m_data);
            System.out.println("done");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
	}

  

    private void runStep()
    {
        if (getCurrentStep().hasReachedMaxRunCount())
        {
            m_data.getSequence().next();
            return;
        }

    	startStep();
    	
        waitForPlayerToFinishStep();
        
        endStep();
        
        
        
        
        if(m_data.getSequence().next())
        {
            m_data.getHistory().getHistoryWriter().startNextRound(m_data.getSequence().getRound());
        }

    }

	private void endStep() 
	{
		getCurrentStep().getDelegate().end();
		getCurrentStep().incrementRunCount();    
        
    	if (m_data.getSequence().getStep().getDelegate().getClass().isAnnotationPresent(AutoSave.class))
    	{
    		if(m_data.getSequence().getStep().getDelegate().getClass().getAnnotation(AutoSave.class).afterStepEnd())
    			autoSave();
    	}
	}

	private void startStep() 
	{
		if (m_data.getSequence().getStep().getDelegate().getClass().isAnnotationPresent(AutoSave.class))
    	{
    		if(m_data.getSequence().getStep().getDelegate().getClass().getAnnotation(AutoSave.class).beforeStepStart())
    			autoSave();
    	}
        
        DefaultDelegateBridge bridge = new DefaultDelegateBridge(
                m_data, 
                getCurrentStep(), this, 
                new DelegateHistoryWriter(m_channelMessenger),
                m_randomStats
                );
        bridge.setRandomSource(m_randomSource);

        
        notifyGameStepChanged();
        getCurrentStep().getDelegate().start(bridge, m_data);
	}

    private void waitForPlayerToFinishStep()
    {
        PlayerID playerID = getCurrentStep().getPlayerID();
        //no player specified for the given step
        if (playerID == null)
            return;

        IGamePlayer player = m_gamePlayers.get(playerID);

        if (player != null)
        {
            //a local player
            player.start(getCurrentStep().getName());
        }
        else
        {
            //a remote player
            INode destination = m_remotePlayers.get(playerID.getName());
            IGameStepAdvancer advancer = (IGameStepAdvancer) m_remoteMessenger.getRemote(ClientGame.getRemoteStepAdvancerName(destination));
            advancer.startPlayerStep(getCurrentStep().getName(), playerID);
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
        
        getGameModifiedBroadcaster().stepChanged(stepName, delegateName, id, m_data.getSequence().getRound(), displayName);        
               
        Iterator<GameStepListener> iter = m_gameStepListeners.iterator();
        while (iter.hasNext())
        {
            GameStepListener listener = iter.next();
            listener.gameStepChanged(stepName, delegateName, id, m_data.getSequence().getRound(), getCurrentStep().getDisplayName());
        }        
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

    private IGameModifiedChannel getGameModifiedBroadcaster()
    {
        return (IGameModifiedChannel) m_channelMessenger.getChannelBroadcastor(IGame.GAME_MODIFICATION_CHANNEL); 
    }
    
    public void addChange(Change aChange)
    {
        getGameModifiedBroadcaster().gameDataChanged(aChange);
        //let our channel subscribor do the change, 
        //that way all changes will happen in the same thread
    }

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
    
    private IGameModifiedChannel m_gameModifiedChannel = new IGameModifiedChannel()
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
            m_data.getHistory().getHistoryWriter().startNextStep(stepName, delegateName, player, displayName);
            
        }
        
    };

    /* 
     * @see games.strategy.engine.framework.IGame#addDisplay(games.strategy.engine.display.IDisplay)
     */
    public void addDisplay(IDisplay display)
    {
       display.initialize(new DefaultDisplayBridge(m_data));
       m_channelMessenger.registerChannelSubscriber(display, DISPLAY_CHANNEL);
        
        
    }

    /* 
     * @see games.strategy.engine.framework.IGame#removeDisplay(games.strategy.engine.display.IDisplay)
     */
    public void removeDisplay(IDisplay display)
    {
        m_channelMessenger.unregisterChannelSubscriber(display, DISPLAY_CHANNEL);
    }
}
