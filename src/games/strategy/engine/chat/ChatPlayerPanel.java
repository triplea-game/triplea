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

import java.awt.BorderLayout;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

public class ChatPlayerPanel extends JPanel implements IChatListener
{
    private JList m_players;
    private DefaultListModel m_listModel;
    private Chat m_chat;
    
    public ChatPlayerPanel(Chat chat)
    {
        
        createComponents();
        layoutComponents();
        setupListeners();
        setWidgetActivation();
        
        setChat(chat);
    }

    public void setChat(Chat chat)
    {
        if(m_chat != null)
        {
            m_chat.removeChatListener(this);
        }
        m_chat = chat;
        
        if(chat != null)
        {
            chat.addChatListener(this);
        }
        else
        {
            //empty our player list
            updatePlayerList(Collections.<String>emptyList());
        }
        
    }

    private void createComponents()
    {
        m_listModel = new DefaultListModel();
        m_players = new JList(m_listModel);
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
    
    
    public void setPlayerRenderer(ListCellRenderer renderer)
    {
        m_players.setCellRenderer(renderer);
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

    public void addMessage(String message, String from, boolean thirdperson)
    {
       
        
    }
    
    
}
