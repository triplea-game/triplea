package games.strategy.engine.chat;
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


import games.strategy.engine.sound.ClipPlayer;
import games.strategy.net.INode;
import games.strategy.triplea.sound.SoundPath;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoundedRangeModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/**
 * A Chat window.  
 * 
 * Mutiple chat panels can be connected to the same Chat.<p>
 * 
 * We can change the chat we are connected to using the setChat(...) method.
 * 
 * @author Sean Bridges
 */
public class ChatMessagePanel extends JPanel implements IChatListener
{
    private ChatFloodControl floodControl = new ChatFloodControl();
    private static final int MAX_LINES = 5000;
    private JTextPane m_text;
    private JScrollPane m_scrollPane;
    private JTextField m_nextMessage;
    
    private JButton m_send;
    private JButton m_setStatus;
    private Chat m_chat;
    
    private boolean m_showTime=false;
    
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("'('HH:mm:ss')'");

    private final SimpleAttributeSet bold = new SimpleAttributeSet();
    private final SimpleAttributeSet italic = new SimpleAttributeSet();
    private final SimpleAttributeSet normal = new SimpleAttributeSet();
    
    public static final String ME = "/me ";
    public static boolean isThirdPerson(String msg)
    {
        return msg.toLowerCase().startsWith(ME);
    }    
    
 
    public ChatMessagePanel(Chat chat)
    {
        init();
        setChat(chat);
    }
    
    private void init()
    {
        createComponents();
        layoutComponents();

        StyleConstants.setBold(bold, true);
        StyleConstants.setItalic(italic, true);
        setSize(300, 200);
    }
    
    
    
    public void setChat(Chat chat)
    {
        if(m_chat != null)
        {
            m_chat.removeChatListener(this);
            cleanupKeyMap();
        }
        
        m_chat = chat;
        
        if(m_chat != null)
        {
           setupKeyMap();
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
            updatePlayerList(Collections.<INode>emptyList());
        }
    }
    

    public Chat getChat()
    {
        return m_chat;
    }
    
    public void setShowTime(boolean showTime)
    {
        m_showTime = showTime;
    }

    private void layoutComponents()
    {

        Container content = this;
        content.setLayout(new BorderLayout());
        m_scrollPane = new JScrollPane(m_text);
        
        content.add(m_scrollPane, BorderLayout.CENTER);

        
        content.add(m_scrollPane, BorderLayout.CENTER);

        JPanel sendPanel = new JPanel();
        sendPanel.setLayout(new BorderLayout());
        sendPanel.add(m_nextMessage, BorderLayout.CENTER);
        sendPanel.add(m_send, BorderLayout.WEST);
        sendPanel.add(m_setStatus, BorderLayout.EAST);

        content.add(sendPanel, BorderLayout.SOUTH);
    }

    private void createComponents()
    {

        m_text = new JTextPane();
        m_text.setEditable(false);
        m_text.setFocusable(false);

        m_nextMessage = new JTextField(10);
        //when enter is pressed, send the message
        

        m_setStatus = new JButton(m_setStatusAction);
        m_setStatus.setFocusable(false);
        

        Insets inset = new Insets(3, 3, 3, 3);
        m_send = new JButton(m_sendAction);
        m_send.setMargin(inset);
        m_send.setFocusable(false);

        
    }


    private void setupKeyMap()
    {
        InputMap nextMessageKeymap = m_nextMessage.getInputMap();
        nextMessageKeymap.put(KeyStroke.getKeyStroke('\n'), m_sendAction);
        nextMessageKeymap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_UP,0,false), m_UpAction);
        nextMessageKeymap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DOWN,0,false), m_DownAction);
    }
    

    private void cleanupKeyMap()
    {
        InputMap nextMessageKeymap = m_nextMessage.getInputMap();
        nextMessageKeymap.remove(KeyStroke.getKeyStroke('\n') );
        nextMessageKeymap.remove(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_UP,0,false));
        nextMessageKeymap.remove(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DOWN,0,false));
    }
    
    


    
    /** thread safe */
    public void addMessage(final String message, final String from, final boolean thirdperson)
    {
        Runnable runner = new Runnable()
        {
            public void run()
            {
                if(!floodControl.allow(from, System.currentTimeMillis())) 
                {
                    if(from.equals(m_chat.getLocalNode().getName())) 
                    {
                        addChatMessage("MESSAGE LIMIT EXCEEDED, TRY AGAIN LATER", "ADMIN_FLOOD_CONTROL", false);
                    }
                    return;
                }
                addChatMessage(message, from, thirdperson);
             
                SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            BoundedRangeModel scrollModel = m_scrollPane.getVerticalScrollBar().getModel();
                            scrollModel.setValue(scrollModel.getMaximum());            
                        }
                });
                
                ClipPlayer.getInstance().playClip(SoundPath.MESSAGE, SoundPath.class);                
            }

           
        };

        //invoke in the swing event thread
        if (SwingUtilities.isEventDispatchThread())
            runner.run();
        else
            SwingUtilities.invokeLater(runner);
    }

    private void addChatMessage(String originalMessage, final String from, final boolean thirdperson)
    {
        final String message = trimMessage(originalMessage);
        try
        {
            String time=simpleDateFormat.format(new Date());
            Document doc = m_text.getDocument();
            if(thirdperson)
                doc.insertString(doc.getLength(), (m_showTime ? "* "+time+" "+from : "* "+from), bold);
            else
                doc.insertString(doc.getLength(), (m_showTime ? time +" "+ from+": " : from+": "), bold);
            doc.insertString(doc.getLength()," "+message + "\n", normal);
            
            //don't let the chat get too big
            trimLines(doc, MAX_LINES);
            
            
        } catch (BadLocationException ble)
        {
            ble.printStackTrace();
        }
    }
    
    public void addStatusMessage(final String message)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
        
            public void run()
            {
                try
                {
                    Document doc = m_text.getDocument();
                    
                    doc.insertString(doc.getLength(), message + "\n", italic);
                    
                    //don't let the chat get too big
                    trimLines(doc, MAX_LINES);
                    
                    
                } catch (BadLocationException ble)
                {
                    ble.printStackTrace();
                }
        
            }
        
        });
        
    }
    

    /**
     * Show only the first n lines
     */
    public static void trimLines(Document doc, int lineCount)
    {
        if(doc.getLength() < lineCount)
            return;
        
        try
        {
            String text = doc.getText(0, doc.getLength());
            int returnsFound = 0;
            
            for(int i = text.length() - 1; i >= 0; i--)
            {
                if(text.charAt(i) == '\n')
                {
                    returnsFound ++;
                }
                if(returnsFound == lineCount)
                {
                    doc.remove(0, i);
                    return;
                }
                
            }
        } catch (BadLocationException e)
        {
            e.printStackTrace();
        }
        
        
        
        
    }

    private String trimMessage(String originalMessage)
    {
        //dont allow messages that are too long
        if(originalMessage.length() > 200)
        {
            return originalMessage.substring(0, 199) + "...";
        }
        else
        {
            return originalMessage;
        }
        
    }
    
 
    private Action m_setStatusAction = new AbstractAction("Status...")
    {
        public void actionPerformed(ActionEvent e)
        {
            String status = JOptionPane.showInputDialog(JOptionPane.getFrameForComponent(ChatMessagePanel.this), "Enter Status Text (leave blank for no status)", "");
            if(status != null)
            {
                if(status.trim().length() == 0)
                    status = null;
                m_chat.getStatusManager().setStatus(status);
            }
        }
    };
    

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
    
    public void updatePlayerList(Collection<INode> players)
    {}
}

