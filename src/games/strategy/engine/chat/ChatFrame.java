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

import com.sun.corba.se.internal.core.SendingContextServiceContext;

import games.strategy.engine.sound.ClipPlayer;
import games.strategy.net.IMessenger;
import games.strategy.triplea.sound.SoundPath;

/**
 * @author Sean Bridges
 */
public class ChatFrame extends JFrame
{

    private JTextPane m_text;
    private JScrollPane m_scrollPane;
    private JTextField m_nextMessage;
    private JList m_players;
    private JButton m_send;
    private Chat m_chat;
    private DefaultListModel m_listModel;

    SimpleAttributeSet bold = new SimpleAttributeSet();
    SimpleAttributeSet normal = new SimpleAttributeSet();

    /** Creates a new instance of ChatFrame */
    public ChatFrame(IMessenger messenger)
    {

        super("Chat");

        setIconImage(games.strategy.engine.framework.GameRunner.getGameIcon(this));

        createComponents();
        layoutComponents();

        m_chat = new Chat(messenger, this);

        StyleConstants.setBold(bold, true);
        setSize(300, 200);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    }

    public Chat getChat()
    {
        return m_chat;
    }

    private void layoutComponents()
    {

        Container content = getContentPane();
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
        
        Action slap = new AbstractAction("Slap " + playerName)
        {
            public void actionPerformed(ActionEvent e)
            {
               m_chat.slap(playerName);
            }
        };
        
        JPopupMenu menu = new JPopupMenu();
        menu.add(slap);
        menu.show(m_players, e.getX(), e.getY());
        
        
    }
    
    /**
     * 
     * 
     * /** thread safe.
     */
    void addMessage(ChatMessage msg, String from)
    {

        addMessage(msg.getMessage(), from);
   

    }

    /** thread safe */
    void addMessage(final String message, final String from)
    {

        Runnable runner = new Runnable()
        {

            public void run()
            {

                try
                {
                    Document doc = m_text.getDocument();
                    doc.insertString(doc.getLength(), from, bold);
                    doc.insertString(doc.getLength(), " : ", bold);
                    doc.insertString(doc.getLength(), message + "\n", normal);
                } catch (BadLocationException ble)
                {
                    ble.printStackTrace();
                }
                if (!isVisible())
                    setVisible(true);
   
                toFront();
                BoundedRangeModel scrollModel = m_scrollPane.getVerticalScrollBar().getModel();
                scrollModel.setValue(scrollModel.getValue());
                
                ClipPlayer.getInstance().playClip(SoundPath.MESSAGE, SoundPath.class);                
            }
        };

        //invoke in the swing event thread
        if (SwingUtilities.isEventDispatchThread())
            runner.run();
        else
            SwingUtilities.invokeLater(runner);
    }

    /**
     * @arg players - a collection of Strings representing player names.
     */
    synchronized void updatePlayerList(final Collection players)
    {

        Runnable runner = new Runnable()
        {

            public void run()
            {

                m_listModel.clear();

                Iterator iter = players.iterator();
                while (iter.hasNext())
                {
                    String name = (String) iter.next();
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

            ChatMessage msg = new ChatMessage(m_nextMessage.getText());
            m_chat.sendMessage(msg);
            m_nextMessage.setText("");
        }
    };
}