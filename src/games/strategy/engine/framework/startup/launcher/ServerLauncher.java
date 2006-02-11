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
import games.strategy.engine.framework.ui.*;
import games.strategy.engine.gamePlayer.IGamePlayer;
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

    public static final String CLIENT_READY_CHANNEL = "games.strategy.engine.framework.ui.LauncherFrame.CLIENT_READY_CHANNEL";
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

    public void launch(final Component parent)
    {
        //TODO - there is a dangerous period here
        //after the game is launched, but before it reall starts,
        //observers trying to come in at this time will mess things up
     
        m_ui = parent;
        
        s_logger.fine("Starting server");
        ServerReady serverReady = new ServerReady(m_clientCount);
        m_remoteMessenger.registerRemote(IServerReady.class, serverReady, CLIENT_READY_CHANNEL);

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

        m_serverGame = new ServerGame(m_gameData, localPlayerSet, (IServerMessenger) m_messenger, m_remotelPlayers, m_channelMessenger,
                m_remoteMessenger);
        
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
            
        m_serverModel.setInGame(true, this);
        m_gameData.getGameLoader().startGame(m_serverGame, localPlayerSet);

        serverReady.await();
        m_remoteMessenger.unregisterRemote(CLIENT_READY_CHANNEL);

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
                m_serverGame.startGame();
                
                m_gameSelectorModel.loadDefaultGame(parent);
                
                SwingUtilities.invokeLater(new Runnable()
                        {
                        
                            public void run()
                            {
                                JOptionPane.getFrameForComponent(parent).setVisible(true);
                            }
                        });
                
                m_serverModel.setInGame(false, null);
                m_serverModel.newGame();
            }
        };
        t.start();
    }
    
    public void addObserver(IObserverWaitingToJoin observer)
    {
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
        //if we loose a connection to a player, shut down
        //the game (after saving) and go back to the main screen
        if(m_serverGame.getPlayerManager().isPlaying(node))
        {
            DateFormat format = new SimpleDateFormat("MMM_dd_'at'_HH_mm");
            
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
        else
        {
            //nothing to do
            //we just lost a connection to an observer
            //which is ok.
        }
        
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


