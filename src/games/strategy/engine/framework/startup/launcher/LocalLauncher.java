package games.strategy.engine.framework.startup.launcher;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.message.*;
import games.strategy.engine.random.*;
import games.strategy.net.*;

import java.awt.Component;
import java.util.*;

import javax.swing.*;

public class LocalLauncher implements ILauncher
{
    private final GameData m_gameData;
    private IRandomSource m_randomSource;
    private final Map<String,String> m_playerTypes;
    private final GameSelectorModel m_gameSelectorModel;
    
    public LocalLauncher(GameSelectorModel gameSelectorModel, IRandomSource randomSource, Map<String,String> playerTypes)
    {
        m_gameSelectorModel = gameSelectorModel;
        m_gameData = gameSelectorModel.getGameData();
        m_randomSource = randomSource;
        m_playerTypes = playerTypes;
    }
    
    public void launch(final Component parent)
    {
        Runnable runner = new Runnable()
        {
            public void run()
            {
                IServerMessenger messenger = new DummyMessenger();
                
                
                UnifiedMessenger unifiedMessenger = new UnifiedMessenger(messenger);
                ChannelMessenger channelMessenger = new ChannelMessenger(unifiedMessenger);
                RemoteMessenger remoteMessenger = new RemoteMessenger(unifiedMessenger);

               Set<IGamePlayer> gamePlayers = m_gameData.getGameLoader().createPlayers(m_playerTypes);
               ServerGame game = new ServerGame(m_gameData, gamePlayers, messenger, new HashMap<String,INode>(), channelMessenger, remoteMessenger);
                

               game.setRandomSource(m_randomSource);
                
                //for debugging, we can use a scripted random source
                if(ScriptedRandomSource.useScriptedRandom())
                {
                    game.setRandomSource(new ScriptedRandomSource());
                }

                m_gameData.getGameLoader().startGame(game, gamePlayers);

                game.startGame();

                
                m_gameSelectorModel.loadDefaultGame(parent);
                
                SwingUtilities.invokeLater(new Runnable()
                {
                
                    public void run()
                    {
                        JOptionPane.getFrameForComponent(parent).setVisible(true);
                    }
                
                });
                
            }
        };
        
        
        Thread thread = new Thread(runner, "Triplea start local thread");
        thread.start();
        
        

    }

}
