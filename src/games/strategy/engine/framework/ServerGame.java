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
    public ServerGame(GameData data, Set localPlayers, IServerMessenger messenger, Map remotePlayerMapping)
    {
        m_data = data;

        m_messenger = messenger;
        m_messenger.addMessageListener(m_messageListener);
        m_messageManager = new MessageManager(m_messenger);

        m_remotePlayers = new HashMap(remotePlayerMapping);

        //add a random destination for the null player
        RandomDestination rnd_dest = new RandomDestination(PlayerID.NULL_PLAYERID.getName());
        m_messageManager.addDestination(rnd_dest);

        Iterator localPlayersIter = localPlayers.iterator();
        while (localPlayersIter.hasNext())
        {
            GamePlayer gp = (GamePlayer) localPlayersIter.next();
            PlayerID player = m_data.getPlayerList().getPlayerID(gp.getName());
            m_gamePlayers.put(player, gp);
            PlayerBridge bridge = new DefaultPlayerBridge(this);
            gp.initialize(bridge, player);
            m_messageManager.addDestination(gp);

            // Add a random destination for this GamePlayer
            rnd_dest = new RandomDestination(gp.getName());

            m_messageManager.addDestination(rnd_dest);
        }

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

        localPlayersIter = data.getDelegateList().iterator();
        while (localPlayersIter.hasNext())
        {
            Delegate delegate = (Delegate) localPlayersIter.next();
            m_messageManager.addDestination(delegate);
        }

        m_changePerformer = new ChangePerformer(m_data);
        m_randomStats = new RandomStats(m_messageManager);
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

        GamePlayer player = (GamePlayer) m_gamePlayers.get(playerID);

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
}
