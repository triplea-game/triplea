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

import games.strategy.debug.Console;
import games.strategy.engine.GameOverException;
import games.strategy.engine.data.*;
import games.strategy.engine.data.events.GameStepListener;
import games.strategy.engine.delegate.*;
import games.strategy.engine.display.*;
import games.strategy.engine.framework.startup.mc.IObserverWaitingToJoin;
import games.strategy.engine.framework.startup.ui.InGameLobbyWatcher;
import games.strategy.engine.framework.ui.*;
import games.strategy.engine.gamePlayer.*;
import games.strategy.engine.history.*;
import games.strategy.engine.message.*;
import games.strategy.engine.random.*;
import games.strategy.engine.vault.Vault;
import games.strategy.net.*;
import games.strategy.triplea.ui.ErrorHandler;
import games.strategy.util.ListenerList;

import java.io.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;


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
    public static final RemoteName SERVER_REMOTE = new RemoteName("games.strategy.engine.framework.ServerGame.SERVER_REMOTE", IServerRemote.class);
    
    private ListenerList<GameStepListener> m_gameStepListeners = new ListenerList<GameStepListener>();
    private final GameData m_data;

    //maps PlayerID->GamePlayer
    private final Map<PlayerID, IGamePlayer> m_gamePlayers = new HashMap<PlayerID, IGamePlayer>();

    private final IServerMessenger m_messenger;
    private final ChangePerformer m_changePerformer;
    
    private final IRemoteMessenger m_remoteMessenger;
    private final IChannelMessenger m_channelMessenger;
    private final Vault m_vault;

    
    private final RandomStats m_randomStats;

    private IRandomSource m_randomSource = new PlainRandomSource();
    private IRandomSource m_delegateRandomSource;
    
    private DelegateExecutionManager m_delegateExecutionManager = new DelegateExecutionManager();
    private volatile boolean m_isGameOver = false;
    
    private InGameLobbyWatcher m_inGameLobbyWatcher;
    
    /**
     * When the delegate execution is stopped, we countdown on this latch to prevent the startgame(...) method from returning.<p>
     */
    private final CountDownLatch m_delegateExecutionStoppedLatch = new CountDownLatch(1);
    
    /**
     * Has the delegate signalled that delegate execution should stop.
     */
    private volatile boolean m_delegateExecutionStopped = false;
    
    private IServerRemote m_serverRemote = new IServerRemote()
    {
    
        public byte[] getSavedGame()
        {
            ByteArrayOutputStream sink = new ByteArrayOutputStream(5000);
            try
            {
                saveGame(sink);
            } catch (IOException e)
            {
                e.printStackTrace();
                throw new IllegalStateException(e);
            }
            return sink.toByteArray();
        }
    
    };
    
    public static RemoteName getDisplayChannel(GameData data)
    {
        return new RemoteName(DISPLAY_CHANNEL, data.getGameLoader().getDisplayType());
    }
    
    private final PlayerManager m_players;
    
    /**
     *
     * @param localPlayers Set - A set of GamePlayers
     * @param messenger IServerMessenger
     * @param remotePlayerMapping Map
     */
    public ServerGame(GameData data, Set<IGamePlayer> localPlayers, Map<String,INode> remotePlayerMapping, Messengers messengers)
    {
        m_data = data;
        m_messenger = (IServerMessenger) messengers.getMessenger();
        
        
        m_remoteMessenger = messengers.getRemoteMessenger();
        m_channelMessenger = messengers.getChannelMessenger();
        
        
        m_vault = new Vault(m_channelMessenger, m_remoteMessenger);

        Map<String, INode> allPlayers = new HashMap<String,INode>(remotePlayerMapping);
        for(IGamePlayer player : localPlayers)
        {
            allPlayers.put(player.getName(), m_messenger.getLocalNode());
        }
        m_players = new PlayerManager(allPlayers);

        m_channelMessenger.registerChannelSubscriber(m_gameModifiedChannel, IGame.GAME_MODIFICATION_CHANNEL);
        
        
        
        setupLocalPlayers(localPlayers);

        setupDelegateMessaging(data);

        m_changePerformer = new ChangePerformer(data);
        m_randomStats = new RandomStats(m_remoteMessenger);
        
        m_remoteMessenger.registerRemote(m_serverRemote, SERVER_REMOTE);
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
            
            RemoteName descriptor = getRemoteName(gp.getID(), m_data) ;
            m_remoteMessenger.registerRemote(gp, descriptor);

        }
    }
    
    public void addObserver(IObserverWaitingToJoin observer)
    {
        try
        {
            if(!m_delegateExecutionManager.blockDelegateExecution(2000))
            {
                observer.cannotJoinGame("Could not block delegate execution");
                return;
            }
        } catch (InterruptedException e)
        {
            observer.cannotJoinGame(e.getMessage());
            return;
        }
        try
        {
            ByteArrayOutputStream sink = new ByteArrayOutputStream(1000);
            saveGame(sink);
            observer.joinGame(sink.toByteArray(), m_players.getPlayerMapping());
            
        }
        catch(IOException ioe)
        {
            observer.cannotJoinGame(ioe.getMessage());
            return;
        }
        finally
        {
            m_delegateExecutionManager.resumeDelegateExecution();
        }
    }

    private void setupDelegateMessaging(GameData data)
    {
        for(IDelegate delegate : data.getDelegateList()) 
        {
            addDelegateMessenger(delegate);
        }
    }
    
    public void addDelegateMessenger(IDelegate delegate)
    {
        Class<? extends IRemote> remoteType = delegate.getRemoteType();
        //if its null then it shouldn't be added as an IRemote
        if(remoteType == null)
            return;
        
        Object wrappedDelegate = m_delegateExecutionManager.createInboundImplementation(delegate, new Class[] {delegate.getRemoteType()});
        RemoteName descriptor = getRemoteName(delegate);
        m_remoteMessenger.registerRemote(wrappedDelegate, descriptor );

        
    }

    public static RemoteName getRemoteName(IDelegate delegate)
    {
        return new RemoteName("games.strategy.engine.framework.ServerGame.DELEGATE_REMOTE." + delegate.getName(), delegate.getRemoteType());
    }
    
    public static RemoteName getRemoteName(PlayerID id, GameData data)
    {
        return  new RemoteName(
                "games.strategy.engine.framework.ServerGame.PLAYER_REMOTE." + id.getName(),
                data.getGameLoader().getRemotePlayerType() );
    }

    public static RemoteName getRemoteRandomName(PlayerID id)
    {
        return new RemoteName("games.strategy.engine.framework.ServerGame.PLAYER_RANDOM_REMOTE" + id.getName(), IRemoteRandom.class);
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

    
    private final static String GAME_HAS_BEEN_SAVED_PROPERTY = "games.strategy.engine.framework.ServerGame.GameHasBeenSaved";
    /**
     * And here we go.
     * Starts the game in a new thread
     */
    public void startGame()
    {
        
        try
        {
            //we dont want to notify that the step has been saved when reloading a saved game, since
            //in fact the step hasnt changed, we are just resuming where we left off
            boolean gameHasBeenSaved =  m_data.getProperties().get(GAME_HAS_BEEN_SAVED_PROPERTY, false);
            
            if(!gameHasBeenSaved)
                m_data.getProperties().set(GAME_HAS_BEEN_SAVED_PROPERTY, Boolean.TRUE);
            
            startPersistentDelegates();

            if(gameHasBeenSaved)
            {
                runStep(gameHasBeenSaved);
            }
            
            while (!m_isGameOver) {
                if(m_delegateExecutionStopped) 
                {
                    //the delegate has told us to stop stepping through game steps
                    try
                    {
                        //dont let this method return, as this method returning signals
                        //that the game is over.
                        m_delegateExecutionStoppedLatch.await();
                    } catch (InterruptedException e) 
                    {
                        //ignore
                    }
                } else {
                    runStep(false);
                }
            }
        }
        catch(GameOverException goe)
        {
            if(!m_isGameOver)
                goe.printStackTrace();
            return;
                
        }
        
    }

    public void stopGame()
    {
        //we have already shut down
        if(m_isGameOver)
            return;
        
        m_isGameOver = true;        
        ErrorHandler.setGameOver(true);
        m_delegateExecutionStoppedLatch.countDown();
        
        //block delegate execution to prevent outbound messages to the players
        //while we shut down.
        try
        {
            if(!m_delegateExecutionManager.blockDelegateExecution(4000)) {
               Console.getConsole().dumpStacks();
               System.exit(0);
            }
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        
        //shutdown
        try
        {
        
            m_delegateExecutionManager.setGameOver();            
            getGameModifiedBroadcaster().shutDown();
            m_randomStats.shutDown();
            m_channelMessenger.unregisterChannelSubscriber(m_gameModifiedChannel, IGame.GAME_MODIFICATION_CHANNEL);
            m_remoteMessenger.unregisterRemote(SERVER_REMOTE);
            m_vault.shutDown();
            
            Iterator<IGamePlayer> localPlayersIter = m_gamePlayers.values().iterator();
            while (localPlayersIter.hasNext())
            {
                IGamePlayer gp = localPlayersIter.next();
                m_remoteMessenger.unregisterRemote(getRemoteName(gp.getID(), m_data));
            }
            
            Iterator delegateIter = m_data.getDelegateList().iterator();
            while (delegateIter.hasNext())
            {
                IDelegate delegate = (IDelegate) delegateIter.next();
                
                Class<? extends IRemote> remoteType = delegate.getRemoteType();
                //if its null then it shouldnt be added as an IRemote
                if(remoteType == null)
                    continue;
                            
                m_remoteMessenger.unregisterRemote(getRemoteName(delegate));
            }            
            
        }
        catch(RuntimeException re)
        {
            re.printStackTrace();
        }
        finally
        {
            m_delegateExecutionManager.resumeDelegateExecution();
        }
        
        
        
        
        m_data.getGameLoader().shutDown();        
    }

    private void autoSave() 
    {
        FileOutputStream out = null;
        try
        {
            SaveGameFileChooser.ensureDefaultDirExists();
            File autosaveFile = new File(SaveGameFileChooser.DEFAULT_DIRECTORY, SaveGameFileChooser.AUTOSAVE_FILE_NAME);
            
            
            out = new FileOutputStream(autosaveFile);
            saveGame(out);
            
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if(out != null)
                    out.close();
            } catch (IOException e)
            {
              
                e.printStackTrace();
            }
        }
    }

    
    public void saveGame(File f)
    {
        FileOutputStream fout = null;
        try
        {
            fout = new FileOutputStream(f);
            saveGame(fout);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            if(fout != null)
            {
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
    
    
    public void saveGame(OutputStream out) throws IOException
    {
        try
        {
            if(!m_delegateExecutionManager.blockDelegateExecution(3000))
            {
                new IOException("Could not lock delegate execution").printStackTrace();
            }
        }
        catch(InterruptedException ie)
        {
            throw new IOException(ie.getMessage());
        }
        
        
        try
        {
            new GameDataManager().saveGame(out, m_data);
        }
        
        finally
        {
            m_delegateExecutionManager.resumeDelegateExecution();
        }
    }

  

    private void runStep(boolean stepIsRestoredFromSavedGame)
    {
        if (getCurrentStep().hasReachedMaxRunCount())
        {
            m_data.getSequence().next();
            return;
        }

        if(m_isGameOver)
            return;
        
        startStep(stepIsRestoredFromSavedGame);

        if(m_isGameOver)
            return;
        
        waitForPlayerToFinishStep();

        if(m_isGameOver)
            return;        
        
        boolean autoSaveAfterDelegateDone = endStep();
        
        
        if(m_isGameOver)
            return;
        
        if(m_data.getSequence().next())
        {
            m_data.getHistory().getHistoryWriter().startNextRound(m_data.getSequence().getRound());
        }
        
        //save after the step has advanced
        //otherwise, the delegate will execute again.
        if(autoSaveAfterDelegateDone)
            autoSave();

    }

    /**
     * 
     * @return true if the step should autosave
     */
    private boolean endStep() 
    {
        m_delegateExecutionManager.enterDelegateExecution();
        try
        {
            getCurrentStep().getDelegate().end();
        }
        finally
        {
            m_delegateExecutionManager.leaveDelegateExecution();
        }
        
        getCurrentStep().incrementRunCount();    
        
        if (m_data.getSequence().getStep().getDelegate().getClass().isAnnotationPresent(AutoSave.class))
        {
            if(m_data.getSequence().getStep().getDelegate().getClass().getAnnotation(AutoSave.class).afterStepEnd())
                return true;
        }
        return false;
    }

    private void startPersistentDelegates()
    {
        Iterator<IDelegate> delegateIter = m_data.getDelegateList().iterator();
        while (delegateIter.hasNext())
        {
            IDelegate delegate = delegateIter.next();
            if(!(delegate instanceof IPersistentDelegate)) {
                continue;
            }
            
            DefaultDelegateBridge bridge = new DefaultDelegateBridge(
                    m_data, 
                    this, 
                    new DelegateHistoryWriter(m_channelMessenger),
                    m_randomStats, m_delegateExecutionManager
                    );
            
            if(m_delegateRandomSource == null)
            {
                m_delegateRandomSource = (IRandomSource) m_delegateExecutionManager.createOutboundImplementation(m_randomSource, new Class[] {IRandomSource.class});
            }
            
            bridge.setRandomSource(m_delegateRandomSource);
            
            m_delegateExecutionManager.enterDelegateExecution();
            try
            {
                delegate.start(bridge, m_data);
            }
            finally
            {
                m_delegateExecutionManager.leaveDelegateExecution();
            }
        }
    }

    private void startStep(boolean stepIsRestoredFromSavedGame) 
    {
        //dont save if we just loaded
        if(!stepIsRestoredFromSavedGame)
        {
            if (m_data.getSequence().getStep().getDelegate().getClass().isAnnotationPresent(AutoSave.class))
            {
                if(m_data.getSequence().getStep().getDelegate().getClass().getAnnotation(AutoSave.class).beforeStepStart())
                    autoSave();
            }
        }
        
        DefaultDelegateBridge bridge = new DefaultDelegateBridge(
                m_data, 
                this, 
                new DelegateHistoryWriter(m_channelMessenger),
                m_randomStats, m_delegateExecutionManager
                );
        
        if(m_delegateRandomSource == null)
        {
            m_delegateRandomSource = (IRandomSource) m_delegateExecutionManager.createOutboundImplementation(m_randomSource, new Class[] {IRandomSource.class});
        }
        
        bridge.setRandomSource(m_delegateRandomSource);
        
        notifyGameStepChanged(stepIsRestoredFromSavedGame);
        
        m_delegateExecutionManager.enterDelegateExecution();
        try
        {
            getCurrentStep().getDelegate().start(bridge, m_data);
        }
        finally
        {
            m_delegateExecutionManager.leaveDelegateExecution();
        }
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
            INode destination = m_players.getNode(playerID.getName());
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

    private void notifyGameStepChanged(boolean loadedFromSavedGame)
    {
        String stepName = getCurrentStep().getName();
        String delegateName = getCurrentStep().getDelegate().getName();
        String displayName = getCurrentStep().getDisplayName();
        PlayerID id = getCurrentStep().getPlayerID();
                
        getGameModifiedBroadcaster().stepChanged(stepName, delegateName, id, m_data.getSequence().getRound(), displayName, loadedFromSavedGame);        
               
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

   
    public IRandomSource getRandomSource()
    {
      return m_randomSource;
    }

    public void setRandomSource(IRandomSource randomSource)
    {
      m_randomSource = randomSource;
      m_delegateRandomSource = null;
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
            assertCorrectCaller();
            m_changePerformer.perform(aChange);
            m_data.getHistory().getHistoryWriter().addChange(aChange);
        }

        private void assertCorrectCaller()
        {
            if(!MessageContext.getSender().equals(m_messenger.getServerNode())) {
                throw new IllegalStateException("Only server can change game data");
            }
        }

        public void startHistoryEvent(String event)
        {
            assertCorrectCaller();
            m_data.getHistory().getHistoryWriter().startEvent(event);
            
        }

        public void addChildToEvent(String text, Object renderingData)
        {
            assertCorrectCaller();
            m_data.getHistory().getHistoryWriter().addChildToEvent(new EventChild(text, renderingData));
            
        }

        public void setRenderingData(Object renderingData)
        {
            assertCorrectCaller();
            m_data.getHistory().getHistoryWriter().setRenderingData(renderingData);
            
        }

        public void stepChanged(String stepName, String delegateName, PlayerID player, int round, String displayName, boolean loadedFromSavedGame)
        {
            assertCorrectCaller();
            if(loadedFromSavedGame)
                return;
            
            m_data.getHistory().getHistoryWriter().startNextStep(stepName, delegateName, player, displayName);
            
        }

        //nothing to do, we call this
        public void shutDown()
        {}
        
    };

    /* 
     * @see games.strategy.engine.framework.IGame#addDisplay(games.strategy.engine.display.IDisplay)
     */
    public void addDisplay(IDisplay display)
    {
       display.initialize(new DefaultDisplayBridge(m_data));
       m_channelMessenger.registerChannelSubscriber(display, ServerGame.getDisplayChannel(getData()));
        
        
    }

    /* 
     * @see games.strategy.engine.framework.IGame#removeDisplay(games.strategy.engine.display.IDisplay)
     */
    public void removeDisplay(IDisplay display)
    {
        m_channelMessenger.unregisterChannelSubscriber(display, ServerGame.getDisplayChannel(getData()));
    }

    public boolean isGameOver()
    {
        return m_isGameOver;
    }

    public PlayerManager getPlayerManager()
    {
        return m_players;
    }

    public InGameLobbyWatcher getInGameLobbyWatcher()
    {
        return m_inGameLobbyWatcher;
    }

    public void setInGameLobbyWatcher(InGameLobbyWatcher inGameLobbyWatcher)
    {
        m_inGameLobbyWatcher = inGameLobbyWatcher;
    }

    public void stopGameSequence()
    {
       m_delegateExecutionStopped = true;
    }
    
    public boolean isGameSequenceRunning()
    {
        return !m_delegateExecutionStopped;
    }
    
}

interface IServerRemote extends IRemote 
{
    public byte[] getSavedGame();
}
