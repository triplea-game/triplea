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

        final ServerGame serverGame = new ServerGame(m_gameData, localPlayerSet, (IServerMessenger) m_messenger, m_remotelPlayers, m_channelMessenger,
                m_remoteMessenger);
        
        //tell the clients to start,
        //later we will wait for them to all
        //signal that they are ready.
        ((IClientChannel) m_channelMessenger.getChannelBroadcastor(IClientChannel.CHANNEL_NAME)).doneSelectingPlayers(gameDataAsBytes);

        
        boolean useSecureRandomSource = !m_remotelPlayers.isEmpty() && !m_localPlayerMapping.isEmpty();
        if (useSecureRandomSource)
        {
            //server game.
            //if we have two players, use a crypto random source.
            PlayerID remotePlayer = m_gameData.getPlayerList().getPlayerID((String) m_remotelPlayers.keySet().iterator().next());

            CryptoRandomSource randomSource = new CryptoRandomSource(remotePlayer, serverGame);
            serverGame.setRandomSource(randomSource);
            
        }
            

        m_gameData.getGameLoader().startGame(serverGame, localPlayerSet);

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
                    serverGame.getRandomSource().getRandom(Constants.MAX_DICE, 2, "Warming up crpyto random source");
                }
            };
            t.start();

        }

        Thread t = new Thread("Triplea, start server game")
        {
            public void run()
            {
                serverGame.startGame();
                
                m_gameSelectorModel.loadDefaultGame(parent);
                
                SwingUtilities.invokeLater(new Runnable()
                        {
                        
                            public void run()
                            {
                                JOptionPane.getFrameForComponent(parent).setVisible(true);
                            }
                        });
                
                m_serverModel.newGame();
            }
        };
        t.start();
    }
    
    public static byte[] gameDataToBytes(GameData data) throws IOException
    {
        ByteArrayOutputStream sink = new ByteArrayOutputStream(25000);

        new GameDataManager().saveGame(sink, data);
        sink.flush();
        sink.close();
        return sink.toByteArray();
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


