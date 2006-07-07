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

package games.strategy.engine.chat;

import games.strategy.net.INode;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

public class ChatPlayerPanel extends JPanel implements IChatListener
{
    private JList m_players;
    private DefaultListModel m_listModel;
    private Chat m_chat;
    
    private IStatusListener m_statusListener;
    
    //if our renderer is overridden
    //we do not set this directly on the JList,
    //instead we feed it the node name and staus as a string
    private ListCellRenderer m_setCellRenderer = new DefaultListCellRenderer();
    
    public ChatPlayerPanel(Chat chat)
    {
        
        createComponents();
        layoutComponents();
        setupListeners();
        setWidgetActivation();
        
        m_statusListener = new IStatusListener()
        {
            public void statusChanged(INode node, String newStatus)
            {
                repaint();
            }
        
        };
        
        setChat(chat);
    }

    public void setChat(Chat chat)
    {
        if(m_chat != null)
        {
            m_chat.removeChatListener(this);
            m_chat.getStatusManager().removeStatusListener(m_statusListener);
        }
        m_chat = chat;
        
        if(chat != null)
        {
            chat.addChatListener(this);
            m_chat.getStatusManager().addStatusListener(m_statusListener);
        }
        else
        {
            //empty our player list
            updatePlayerList(Collections.<INode>emptyList());
        }
        
        repaint();
        
    }

    private void createComponents()
    {
        m_listModel = new DefaultListModel();
        m_players = new JList(m_listModel);
        
        
        m_players.setCellRenderer(new ListCellRenderer()
        {
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
            {
                INode node = (INode) value;
                return m_setCellRenderer.getListCellRendererComponent(list, getDisplayString(node), index, isSelected, cellHasFocus);
            }
            
        });
        
        
    }

    private void layoutComponents()
    {
        setLayout(new BorderLayout());
        add(new JScrollPane(m_players), BorderLayout.CENTER);
        
    }

    private void setupListeners()
    {
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

    private void setWidgetActivation()
    {

    }
    
    /**
     * 
     * The renderer will be passed in a string
     */
    public void setPlayerRenderer(ListCellRenderer renderer)
    {
        m_setCellRenderer = renderer;
    }
    
    private void mouseOnPlayersList(MouseEvent e)
    {
        if(!e.isPopupTrigger())
            return;
     
        int index = m_players.locationToIndex(e.getPoint());
        if(index == -1)
            return;
        INode player = (INode) m_listModel.get(index);
        final String playerName = player.getName();
        //you cant slap yourself
        if(player.equals(m_chat.getLocalNode()))
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
    
    /**
     * @arg players - a collection of Strings representing player names.
     */
    public synchronized void updatePlayerList(final Collection<INode> players)
    {

        Runnable runner = new Runnable()
        {
            public void run()
            {

                m_listModel.clear();

                Iterator<INode> iter = players.iterator();
                while (iter.hasNext())
                {
                    INode name = iter.next();
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

    public void addMessage(String message, String from, boolean thirdperson)
    {
       
        
    }
    
    private String getDisplayString(INode node)
    {
        String status = m_chat.getStatusManager().getStatus(node);
        if(status == null || status.length() == 0)
        {
            return node.getName();
        }
        
        return node.getName() + " (" + status + ")";
    }
    
    
}
