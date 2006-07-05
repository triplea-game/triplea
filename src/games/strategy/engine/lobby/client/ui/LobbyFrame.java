package games.strategy.engine.lobby.client.ui;

import java.awt.BorderLayout;
import java.awt.event.*;

import games.strategy.engine.chat.*;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.lobby.client.LobbyClient;
import games.strategy.engine.lobby.server.LobbyServer;
import games.strategy.net.*;

import javax.swing.*;

public class LobbyFrame extends JFrame
{
    
    private final LobbyClient m_client;
    
    
    public LobbyFrame(LobbyClient client)
    {
        super("TripleA Lobby");
        setIconImage(GameRunner.getGameIcon(this));
        m_client = client;
        setJMenuBar(new LobbyMenu(this));
        
        ChatPanel chatPanel = new ChatPanel(m_client.getMessenger(), m_client.getChannelMessenger(), m_client.getRemoteMessenger(), LobbyServer.LOBBY_CHAT);
        
        add(chatPanel, BorderLayout.CENTER);
        
        pack();
        setLocationRelativeTo(null);
        
        
        m_client.getMessenger().addConnectionChangeListener(new IConnectionChangeListener()
        {
        
            public void connectionRemoved(INode to)
            {
                if(to.equals(m_client.getMessenger().getServerNode()))
                {
                    JOptionPane.showMessageDialog(LobbyFrame.this, "Connection to Server Lost", "Connection Lost", JOptionPane.ERROR_MESSAGE, null);
                }
        
            }
        
            public void connectionAdded(INode to)
            {}
        
        });
        
        addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                shutdown();
            }
        });
        
        
    }
    
    public LobbyClient getLobbyClient()
    {
        return m_client;
    }
    
    
    void shutdown()
    {
//        int rVal = JOptionPane.showConfirmDialog(this, "Are you sure you want to exit?\nUnsaved game data will be lost.", "Exit" , JOptionPane.YES_NO_OPTION);
//        if(rVal != JOptionPane.OK_OPTION)
//            return;

        System.exit(0);
    }

}
