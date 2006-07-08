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

package games.strategy.engine.framework.startup.launcher;

import games.strategy.engine.data.*;
import games.strategy.engine.framework.*;
import games.strategy.engine.framework.startup.mc.*;
import games.strategy.engine.framework.startup.ui.InGameLobbyWatcher;
import games.strategy.engine.framework.ui.*;
import games.strategy.engine.framework.ui.background.WaitWindow;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.lobby.server.GameDescription;
import games.strategy.engine.message.*;
import games.strategy.engine.random.CryptoRandomSource;
import games.strategy.net.*;
import games.strategy.triplea.Constants;

import java.awt.Component;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.logging.*;

import javax.swing.*;


public class ServerLauncher implements ILauncher
{

    private static final Logger s_logger = Logger.getLogger(ServerLauncher.class.getName());    
    
    private final int m_clientCount;
    private final IRemoteMessenger m_remoteMessenger;
    private final IChannelMessenger m_channelMessenger;
    private final IMessenger m_messenger;
    private final GameData m_gameData;
    private final Map<String,String> m_localPlayerMapping;
    private final Map<String,INode> m_remotelPlayers;
    private final GameSelectorModel m_gameSelectorModel;
    private final ServerModel m_serverModel; 
    private ServerGame m_serverGame;
    private Component m_ui;
    private final CountDownLatch m_erroLatch = new CountDownLatch(1);
    private boolean m_isLaunching = true;
    private ServerReady m_serverReady;
    private volatile boolean m_abortLaunch = false;
    //a list of observers that tried to join the game during starup
    //we need to track these, because when we loose connections to them
    //we can ignore the connection lost
    private List<INode> m_observersThatTriedToJoinDuringStartup = Collections.synchronizedList(new ArrayList<INode>());
    
    private WaitWindow m_gameLoadingWindow = new WaitWindow("Loading game, please wait.");
    private InGameLobbyWatcher m_inGameLobbyWatcher;
    
    
    public ServerLauncher(int clientCount, IRemoteMessenger remoteMessenger, IChannelMessenger channelMessenger, IMessenger messenger, GameSelectorModel gameSelectorModel, Map<String, String> localPlayerMapping, Map<String, INode> remotelPlayers, ServerModel serverModel)
    {
        m_clientCount = clientCount;
        m_remoteMessenger = remoteMessenger;
        m_channelMessenger = channelMessenger;
        m_messenger = messenger;
        m_gameData = gameSelectorModel.getGameData();
        m_gameSelectorModel = gameSelectorModel;
        m_localPlayerMapping = localPlayerMapping;
        m_remotelPlayers = remotelPlayers;
        m_serverModel = serverModel;
    }

    public void setInGameLobbyWatcher(InGameLobbyWatcher watcher)
    {
        m_inGameLobbyWatcher = watcher;
    }
    
    public void launch(final Component parent)
    {
        if(!SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("Wrong thread");
        
        Runnable r = new Runnable()
        {
        
            public void run()
            {
                try
                {
                    launchInNewThread(parent);
                }
                finally
                {
                    m_gameLoadingWindow.doneWait();
                    
                    if(m_inGameLobbyWatcher!= null)
                    {
                        m_inGameLobbyWatcher.setGameStatus(GameDescription.GameStatus.IN_PROGRESS, m_serverGame);
                    }
                    
                }

            }
        
        };
        Thread t = new Thread(r);
        
        
        m_gameLoadingWindow.setLocationRelativeTo(JOptionPane.getFrameForComponent(parent));
        m_gameLoadingWindow.setVisible(true);
        m_gameLoadingWindow.showWait();
        
        JOptionPane.getFrameForComponent(parent).setVisible(false);
        
        t.start();
    }
    
    private void launchInNewThread(final Component parent)
    {
        
        if(m_inGameLobbyWatcher!= null)
        {
            m_inGameLobbyWatcher.setGameStatus(GameDescription.GameStatus.LAUNCHING, null);
        }
        
        m_ui = parent;
     
        m_serverModel.setServerLauncher(this);
        
        s_logger.fine("Starting server");
        m_serverReady = new ServerReady(m_clientCount);
        m_remoteMessenger.registerRemote(IServerReady.class, m_serverReady, ClientModel.CLIENT_READY_CHANNEL);

        byte[] gameDataAsBytes;
        try
        {
            gameDataAsBytes = gameDataToBytes(m_gameData);
        } catch (IOException e)
        {
            e.printStackTrace();
            throw new IllegalStateException(e.getMessage());
        }
                
        Set<IGamePlayer> localPlayerSet = m_gameData.getGameLoader().createPlayers(m_localPlayerMapping);

        Messengers messengers = new Messengers(m_messenger, m_remoteMessenger, m_channelMessenger );
        m_serverGame = new ServerGame(m_gameData, localPlayerSet, m_remotelPlayers, messengers);
        
        //tell the clients to start,
        //later we will wait for them to all
        //signal that they are ready.
        ((IClientChannel) m_channelMessenger.getChannelBroadcastor(IClientChannel.CHANNEL_NAME)).doneSelectingPlayers(gameDataAsBytes, m_serverGame.getPlayerManager().getPlayerMapping());

        
        boolean useSecureRandomSource = !m_remotelPlayers.isEmpty() && !m_localPlayerMapping.isEmpty();
        if (useSecureRandomSource)
        {
            //server game.
            //try to find an opponent to be the other side of the crypto random source.
            PlayerID remotePlayer = m_serverGame.getPlayerManager().getRemoteOpponent(m_messenger.getLocalNode(), m_gameData); 

            CryptoRandomSource randomSource = new CryptoRandomSource(remotePlayer, m_serverGame);
            m_serverGame.setRandomSource(randomSource);
        }
        
        m_gameData.getGameLoader().startGame(m_serverGame, localPlayerSet);

        m_serverReady.await();
        m_remoteMessenger.unregisterRemote(ClientModel.CLIENT_READY_CHANNEL);

        if (useSecureRandomSource)
        {
            //the first roll takes a while, initialize
            //here in the background so that the user doesnt notice
            Thread t = new Thread("Warming up crypto random source")
            {
                public void run()
                {
                    m_serverGame.getRandomSource().getRandom(Constants.MAX_DICE, 2, "Warming up crpyto random source");
                }
            };
            t.start();
        }

        Thread t = new Thread("Triplea, start server game")
        {
            public void run()
            {
                try
                {
                    m_isLaunching = false;
                    if(!m_abortLaunch)
                    {
                        m_gameLoadingWindow.doneWait();
                        m_serverGame.startGame();    
                    }
                    else
                    {
                        m_serverGame.stopGame();
                        SwingUtilities.invokeLater(new Runnable()
                        {
                            public void run()
                            {
                                JOptionPane.showMessageDialog(m_ui, "Connection lost to player during startup, game aborted.");
                            }
                        });
                    }
                    
                } catch(MessengerException me)
                {
                    me.printStackTrace(System.out);

                    //we lost a connection 
                    //wait for the connection handler to notice, and shut us down
                    try
                    {
                        //we are already aborting the launch
                        if(!m_abortLaunch)
                            m_erroLatch.await();
                    } catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }
                
                m_gameSelectorModel.loadDefaultGame(parent);
                
                
                
                SwingUtilities.invokeLater(new Runnable()
                        {
                        
                            public void run()
                            {
                                JOptionPane.getFrameForComponent(parent).setVisible(true);
                            }
                        });
                
                m_serverModel.setServerLauncher(null);
                m_serverModel.newGame();
                if(m_inGameLobbyWatcher != null)
                {
                    m_inGameLobbyWatcher.setGameStatus(GameDescription.GameStatus.WAITING_FOR_PLAYERS, null);
                }
            }
        };
        t.start();
    }
    
    public void addObserver(IObserverWaitingToJoin observer, INode newNode)
    {
        if(m_isLaunching)
        {
            m_observersThatTriedToJoinDuringStartup.add(newNode);
            observer.cannotJoinGame("Game is launching, try again soon");
            return;
        }
        
        m_serverGame.addObserver(observer);
    }
    
    public static byte[] gameDataToBytes(GameData data) throws IOException
    {
        ByteArrayOutputStream sink = new ByteArrayOutputStream(25000);

        new GameDataManager().saveGame(sink, data);
        sink.flush();
        sink.close();
        return sink.toByteArray();
    }

    public void connectionLost(final INode node)
    {
        if(m_isLaunching)
        {
            //this is expected, we told the observer
            //he couldnt join, so now we loose the connection
            if(m_observersThatTriedToJoinDuringStartup.remove(node))
                return;
            
            //a player has dropped out
            //abort
            m_serverReady.clientReady();
            m_abortLaunch = true;
            return;
        }
        
        //if we loose a connection to a player, shut down
        //the game (after saving) and go back to the main screen
        if(m_serverGame.getPlayerManager().isPlaying(node))
        {
            saveAndEndGame(node);
            
            //if the game already exited do to a networking error 
            //we need to let them continue
            m_erroLatch.countDown();
        }
        else
        {
            //nothing to do
            //we just lost a connection to an observer
            //which is ok.
        }
        
    }

    private void saveAndEndGame(final INode node)
    {
        DateFormat format = new SimpleDateFormat("MMM_dd_'at'_HH_mm");

        SaveGameFileChooser.ensureDefaultDirExists();
        
        final File f = new File(SaveGameFileChooser.DEFAULT_DIRECTORY, "connection_lost_on_" + format.format(new Date()) + ".tsvg");
        m_serverGame.saveGame(f);
        m_serverGame.stopGame();
        
        SwingUtilities.invokeLater(new Runnable()
        {
        
            public void run()
            {
                String message = "Connection lost to:" + node.getName() + " game is over.  Game saved to:" + f.getName();
                JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(m_ui), message);
            }
        
        });
    }

}



class ServerReady implements IServerReady
{

    private final CountDownLatch m_latch;
    
    ServerReady(int waitCount)
    {
        m_latch = new CountDownLatch(waitCount);
    }
    
    public void clientReady()
    {
        m_latch.countDown();
    }
    
    public void await()
    {
        try
        {
            m_latch.await();
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }
    
    
}


