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

package games.strategy.engine.lobby.client.ui;

import games.strategy.engine.chat.Chat;
import games.strategy.engine.chat.ChatMessagePanel;
import games.strategy.engine.chat.ChatPlayerPanel;
import games.strategy.engine.chat.IPlayerActionFactory;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.lobby.client.LobbyClient;
import games.strategy.engine.lobby.client.login.LobbyServerProperties;
import games.strategy.engine.lobby.server.IModeratorController;
import games.strategy.engine.lobby.server.LobbyServer;
import games.strategy.engine.lobby.server.ModeratorController;
import games.strategy.net.IMessenger;
import games.strategy.net.IMessengerErrorListener;
import games.strategy.net.INode;
import games.strategy.util.MD5Crypt;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;

public class LobbyFrame extends JFrame
{
    
    private final LobbyClient m_client;
    
    private final ChatMessagePanel m_chatMessagePanel;
    
    
    public LobbyFrame(LobbyClient client, LobbyServerProperties props)
    {
        super("TripleA Lobby");
        setIconImage(GameRunner.getGameIcon(this));
        m_client = client;
        setJMenuBar(new LobbyMenu(this));
        
        Chat chat = new Chat(m_client.getMessenger(), LobbyServer.LOBBY_CHAT, m_client.getChannelMessenger(), m_client.getRemoteMessenger());
        
        m_chatMessagePanel = new ChatMessagePanel(chat);
        showServerMessage(props);
        
        m_chatMessagePanel.setShowTime(true);
        
        ChatPlayerPanel chatPlayers = new ChatPlayerPanel(null);
        chatPlayers.addIgnoredPlayerName(LobbyServer.ADMIN_USERNAME);
        chatPlayers.setChat(chat);
        chatPlayers.setPreferredSize(new Dimension(200,600 ));
        chatPlayers.addActionFactory(new IPlayerActionFactory()
        {
        
            public List<Action> mouseOnPlayer(INode clickedOn)
            {
                return createAdminActions(clickedOn);
            }
        
        });
    
        LobbyGamePanel gamePanel = new LobbyGamePanel(m_client.getMessengers());
        
    
        JSplitPane leftSplit = new JSplitPane( );
        leftSplit.setOrientation(JSplitPane.VERTICAL_SPLIT);
        leftSplit.setTopComponent(gamePanel);
        leftSplit.setBottomComponent(m_chatMessagePanel);
        
        leftSplit.setResizeWeight(0.8);
        gamePanel.setPreferredSize(new Dimension(700,200 ));
        m_chatMessagePanel.setPreferredSize(new Dimension(700,400 ));
        
        
        JSplitPane mainSplit = new JSplitPane();
        mainSplit.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setLeftComponent(leftSplit);
        mainSplit.setRightComponent(chatPlayers);
        
        add(mainSplit, BorderLayout.CENTER);
        
        pack();
        setLocationRelativeTo(null);
        
      
        
        m_client.getMessenger().addErrorListener(new IMessengerErrorListener()
        {
        
            public void messengerInvalid(IMessenger messenger, Exception reason)
            {
                connectionToServerLost();
        
            }
        
        });
        
        addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                shutdown();
            }
        });
        
        
    }

    private void showServerMessage(LobbyServerProperties props)
    {
        if(props.getServerMessage() != null && props.getServerMessage().length() > 0) 
        {
            m_chatMessagePanel.addMessage(props.getServerMessage(), "SERVER", false);
        }
    }
    
    private List<Action> createAdminActions(final INode clickedOn)
    {
        if(!m_client.isAdmin()) 
            return Collections.emptyList();
        
        if(clickedOn.equals(m_client.getMessenger().getLocalNode())) 
            return Collections.emptyList();
        
        
        final IModeratorController controller = (IModeratorController) m_client.getRemoteMessenger().getRemote(ModeratorController.getModeratorControllerName());
        List<Action> rVal = new ArrayList<Action>();
        rVal.add(new AbstractAction("Boot " + clickedOn.getName())
        {
        
            public void actionPerformed(ActionEvent e)
            {
                if(!confirm("Boot " + clickedOn.getName())) 
                {
                    return;
                }
                
                controller.boot(clickedOn);
            }
        
        });
        
        rVal.add(new AbstractAction("Ban IP for 1 day")
        {
            
            public void actionPerformed(ActionEvent e)
            {
                if(!confirm("Ban ip for 1 day?")) 
                {
                    return;
                }
                
                long expire = System.currentTimeMillis() +
                              24 * 60 * 60 * 1000;
                controller.banIp(clickedOn, new Date(expire));
            }        
        });

        rVal.add(new AbstractAction("Ban IP forever")
        {
            
            public void actionPerformed(ActionEvent e)
            {
                if(!confirm("Ban ip forever?")) 
                {
                    return;
                }
                
                controller.banIp(clickedOn, null);
            }        
        });

        rVal.add(new AbstractAction("Reset password")
        {
            
            public void actionPerformed(ActionEvent e)
            {
                String newPassword = JOptionPane.showInputDialog(JOptionPane.getFrameForComponent(LobbyFrame.this), "Enter new password");
                if(newPassword == null || newPassword.length() < 2)
                    return;                
                                
                boolean set = controller.setPassword(clickedOn,MD5Crypt.crypt(newPassword));
                String msg = set ? "Password set" : "Password not set";
                
                JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(LobbyFrame.this), msg);
            }
        });
        
        
        return rVal;
    }
    
    private boolean confirm(String question) 
    {
        int rVal = JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(this), question, "Question", JOptionPane.OK_CANCEL_OPTION);
        return rVal == JOptionPane.OK_OPTION;
    }

    public LobbyClient getLobbyClient()
    {
        return m_client;
    }
    
    void setShowChatTime(boolean showTime)
    {
        if (m_chatMessagePanel != null)
            m_chatMessagePanel.setShowTime(showTime);
    }
    
    
    void shutdown()
    {
//        int rVal = JOptionPane.showConfirmDialog(this, "Are you sure you want to exit?\nUnsaved game data will be lost.", "Exit" , JOptionPane.YES_NO_OPTION);
//        if(rVal != JOptionPane.OK_OPTION)
//            return;

        System.exit(0);
    }

    private void connectionToServerLost()
    {
        JOptionPane.showMessageDialog(LobbyFrame.this, "Connection to Server Lost", "Connection Lost", JOptionPane.ERROR_MESSAGE, null);
    }

}
