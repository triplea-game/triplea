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

import java.util.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.text.*;

import games.strategy.engine.message.*;
import games.strategy.engine.sound.ClipPlayer;
import games.strategy.net.IMessenger;
import games.strategy.triplea.sound.SoundPath;

/**
 * A Chat window.  
 * 
 * Mutiple chat panels can be connected to the same Chat.<p>
 * 
 * We can change the chat we are connected to using the setChat(...) method.
 * 
 * @author Sean Bridges
 */
public class ChatPanel extends JPanel implements IChatListener
{
    private JTextPane m_text;
    private JScrollPane m_scrollPane;
    private JTextField m_nextMessage;
    private JList m_players;
    private JButton m_send;
    private Chat m_chat;
    private DefaultListModel m_listModel;

    private final SimpleAttributeSet bold = new SimpleAttributeSet();
    private final SimpleAttributeSet normal = new SimpleAttributeSet();
    
    public static final String ME = "/me ";
    public static boolean isThirdPerson(String msg)
    {
        return msg.toLowerCase().startsWith(ME);
    }    
    
    /** Creates a new instance of ChatFrame */
    public ChatPanel(IMessenger messenger, IChannelMessenger channelMessenger, IRemoteMessenger remoteMessenger, String chatName)
    {
        init();
        
        Chat chat  = new Chat(messenger, chatName, channelMessenger, remoteMessenger  );
        chat.init();
        
        setChat(chat);
    }
    
    public void setPlayerRenderer(ListCellRenderer renderer)
    {
        m_players.setCellRenderer(renderer);
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

        StyleConstants.setBold(bold, true);
        setSize(300, 200);
    }
    
    
    
    public void setChat(Chat chat)
    {
        if(m_chat != null)
            m_chat.removeChatListener(this);
        
        m_chat = chat;
        
        if(m_chat != null)
        {
           m_chat.addChatListener(this);
           m_send.setEnabled(true);
           m_text.setEnabled(true);
           
           synchronized(m_chat.getMutex())
           {
               m_text.setText("");
               for(ChatMessage message : m_chat.getChatHistory())
               {
                   addChatMessage(message.getMessage(), message.getFrom(), message.isMeMessage());
               }
           }
           
        }
        else
        {
            m_send.setEnabled(false);
            m_text.setEnabled(false);
            updatePlayerList(Collections.<String>emptyList());
        }
    }
    

    public Chat getChat()
    {
        return m_chat;
    }

    private void layoutComponents()
    {

        Container content = this;
        content.setLayout(new BorderLayout());
        m_scrollPane = new JScrollPane(m_text);
        
        content.add(m_scrollPane, BorderLayout.CENTER);

        JScrollPane scrollPlayers = new JScrollPane(m_players);
        content.add(scrollPlayers, BorderLayout.EAST);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setLeftComponent(m_scrollPane);
        split.setRightComponent(scrollPlayers);
        split.setOneTouchExpandable(false);
        split.setDividerSize(5);
        split.setResizeWeight(1);

        content.add(split, BorderLayout.CENTER);

        JPanel sendPanel = new JPanel();
        sendPanel.setLayout(new BorderLayout());
        sendPanel.add(m_nextMessage, BorderLayout.CENTER);
        sendPanel.add(m_send, BorderLayout.WEST);

        content.add(sendPanel, BorderLayout.SOUTH);
    }

    private void createComponents()
    {

        m_text = new JTextPane();
        m_text.setEditable(false);

        m_nextMessage = new JTextField(10);
        //when enter is pressed, send the message
        Keymap nextMessageKeymap = m_nextMessage.getKeymap();
        nextMessageKeymap.addActionForKeyStroke(KeyStroke.getKeyStroke('\n'), m_sendAction);
		nextMessageKeymap.addActionForKeyStroke(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_UP,0,false), m_UpAction);
		nextMessageKeymap.addActionForKeyStroke(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DOWN,0,false), m_DownAction);

		
        m_listModel = new DefaultListModel();
        m_players = new JList(m_listModel);

        Insets inset = new Insets(3, 3, 3, 3);
        m_send = new JButton(m_sendAction);
        m_send.setMargin(inset);

        m_players.addMouseListener(new MouseAdapter()
        {

            public void mouseClicked(MouseEvent e)
            {
                mouseOnPlayersList(e);
            }

            public void mousePressed(MouseEvent e)
            {
                mouseOnPlayersList(e);
            }

            public void mouseReleased(MouseEvent e)
            {
                mouseOnPlayersList(e);
            }

        });

    }

    private void mouseOnPlayersList(MouseEvent e)
    {
        if(!e.isPopupTrigger())
            return;
     
        int index = m_players.locationToIndex(e.getPoint());
        if(index == -1)
            return;
        final String playerName = m_listModel.get(index).toString();
        //you cant slap yourself
        if(playerName.equals(m_chat.getLocalNode().getName()))
                return;
        
        Action slap = new AbstractAction("Slap " + playerName)
        {
            public void actionPerformed(ActionEvent event)
            {
               m_chat.sendSlap(playerName);
            }
        };
        
        JPopupMenu menu = new JPopupMenu();
        menu.add(slap);
        menu.show(m_players, e.getX(), e.getY());
        
        
    }
    
    /** thread safe */
    public void addMessage(final String message, final String from, final boolean thirdperson)
    {
        Runnable runner = new Runnable()
        {
            public void run()
            {
                addChatMessage(message, from, thirdperson);
             
                BoundedRangeModel scrollModel = m_scrollPane.getVerticalScrollBar().getModel();
                scrollModel.setValue(scrollModel.getMaximum());
                
                ClipPlayer.getInstance().playClip(SoundPath.MESSAGE, SoundPath.class);                
            }

           
        };

        //invoke in the swing event thread
        if (SwingUtilities.isEventDispatchThread())
            runner.run();
        else
            SwingUtilities.invokeLater(runner);
    }

    private void addChatMessage(final String message, final String from, final boolean thirdperson)
    {
        try
        {
            Document doc = m_text.getDocument();
            if(thirdperson)
                doc.insertString(doc.getLength(), "*"+from, bold);
            else
                doc.insertString(doc.getLength(), from+": ", bold);
            doc.insertString(doc.getLength()," "+message + "\n", normal);
        } catch (BadLocationException ble)
        {
            ble.printStackTrace();
        }
    }
    
    /**
     * @arg players - a collection of Strings representing player names.
     */
    public synchronized void updatePlayerList(final Collection<String> players)
    {

        Runnable runner = new Runnable()
        {
            public void run()
            {

                m_listModel.clear();

                Iterator<String> iter = players.iterator();
                while (iter.hasNext())
                {
                    String name = iter.next();
                    m_listModel.addElement(name);
                }
            }
        };

        //invoke in the swing event thread
        if (SwingUtilities.isEventDispatchThread())
            runner.run();
        else
            SwingUtilities.invokeLater(runner);
    }

    private Action m_sendAction = new AbstractAction("Send")
    {

        public void actionPerformed(ActionEvent e)
        {
            if (m_nextMessage.getText().trim().length() == 0)
                return;
         
            
            if(isThirdPerson(m_nextMessage.getText()))
            {
                m_chat.sendMessage(m_nextMessage.getText().substring(ME.length()), true);
            	
            } else 
            {
            	m_chat.sendMessage(m_nextMessage.getText(), false);
            }
			
            m_nextMessage.setText("");
        }
    };
	private Action m_DownAction = new AbstractAction()
	{

		public void actionPerformed(ActionEvent e)
		{
            if(m_chat == null)
                return;
            
            m_chat.getSentMessagesHistory().next();
		    m_nextMessage.setText(m_chat.getSentMessagesHistory().current());

		}
	};
	private Action m_UpAction = new AbstractAction()
	{

		public void actionPerformed(ActionEvent e)
		{
            if(m_chat == null)
                return;
            
            m_chat.getSentMessagesHistory().prev();
		    m_nextMessage.setText(m_chat.getSentMessagesHistory().current());
		}
	};
}