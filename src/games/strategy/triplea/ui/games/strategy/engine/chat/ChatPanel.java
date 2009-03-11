/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

/*
 * ChatFrame.java Swing ui for chatting.
 * 
 * Created on January 14, 2002, 11:08 AM
 */

package games.strategy.engine.chat;

import games.strategy.engine.message.*;
import games.strategy.net.IMessenger;

import java.awt.*;

import javax.swing.*;

/**
 * A Chat window.  
 * 
 * Mutiple chat panels can be connected to the same Chat.<p>
 * 
 * We can change the chat we are connected to using the setChat(...) method.
 * 
 * @author Sean Bridges
 */
public class ChatPanel extends JPanel 
{
   private ChatPlayerPanel m_chatPlayerPanel;
   private ChatMessagePanel m_chatMessagePanel;
    
    
    /** Creates a new instance of ChatFrame */
    public ChatPanel(IMessenger messenger, IChannelMessenger channelMessenger, IRemoteMessenger remoteMessenger, String chatName)
    {
        init();
        Chat chat  = new Chat(messenger, chatName, channelMessenger, remoteMessenger  );
        setChat(chat);
    }
    

    
    public ChatPanel(Chat chat)
    {
        init();
        setChat(chat);
    }
    
    private void init()
    {
        createComponents();
        layoutComponents();

        setSize(300, 200);
    }
    
    
    
    public void setChat(Chat chat)
    {
        m_chatMessagePanel.setChat(chat);
        m_chatPlayerPanel.setChat(chat);
    }
    

    public Chat getChat()
    {
        return m_chatMessagePanel.getChat();
    }

    private void layoutComponents()
    {

        Container content = this;
        content.setLayout(new BorderLayout());

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setLeftComponent(m_chatMessagePanel);
        split.setRightComponent(m_chatPlayerPanel);
        
        split.setOneTouchExpandable(false);
        split.setDividerSize(5);
        split.setResizeWeight(1);

        content.add(split, BorderLayout.CENTER);
    }

    private void createComponents()
    {

        m_chatPlayerPanel = new ChatPlayerPanel(null);
        m_chatMessagePanel = new ChatMessagePanel(null);
    }



    public void setPlayerRenderer(DefaultListCellRenderer renderer)
    {
        m_chatPlayerPanel.setPlayerRenderer(renderer);
        
    }
    
    public void setShowChatTime(boolean showTime)
    {
        m_chatMessagePanel.setShowTime(showTime);
    }


  
}

