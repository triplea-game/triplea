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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;

public class ChatPlayerPanel extends JPanel implements IChatListener
{
	private static final Icon s_ignoreIcon;
	static
	{
		URL ignore = ChatPlayerPanel.class.getResource("ignore.png");
		if(ignore == null) {
			throw new IllegalStateException("Could not find ignore icon");
		}
		Image img;
		try {
			img = ImageIO.read(ignore);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		s_ignoreIcon = new ImageIcon(img);
	}
	
    private JList m_players;
    private DefaultListModel m_listModel;
    private Chat m_chat;
    public Set<String> m_hiddenPlayers = new HashSet<String>();
    
    private IStatusListener m_statusListener;
    
    //if our renderer is overridden
    //we do not set this directly on the JList,
    //instead we feed it the node name and staus as a string
    private ListCellRenderer m_setCellRenderer = new DefaultListCellRenderer();
    
    private List<IPlayerActionFactory> m_actionFactories = new ArrayList<IPlayerActionFactory>();
    
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

    public void addHiddenPlayerName(String name)
    {
        m_hiddenPlayers.add(name);
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
        m_players.setFocusable(false);
        
        
        m_players.setCellRenderer(new ListCellRenderer()
        {
        	
        	
        	
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
            {
                
                if(m_setCellRenderer == null) {
                    return new JLabel();
                }
                
                INode node = (INode) value;
                DefaultListCellRenderer renderer = (DefaultListCellRenderer) m_setCellRenderer.getListCellRendererComponent(list, getDisplayString(node), index, isSelected, cellHasFocus);
                
            
                if(m_chat.isIgnored(node)) 
                {                
                	renderer.setIcon(s_ignoreIcon);
                } 
                return renderer;
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

        
        m_actionFactories.add(new IPlayerActionFactory()
        {
            public List<Action> mouseOnPlayer(final INode clickedOn)
            {
                //you can't slap or ignore yourself
                if(clickedOn.equals(m_chat.getLocalNode()))
                        return Collections.emptyList();
                
                
                final boolean isIgnored = m_chat.isIgnored(clickedOn);
                Action ignore = new AbstractAction(isIgnored ?  "Stop Ignoring" : "Ignore")
                {
                    public void actionPerformed(ActionEvent event)
                	{
                	    m_chat.setIgnored(clickedOn, !isIgnored);
                	    repaint();
                    }
                };
                
                
                Action slap = new AbstractAction("Slap " + clickedOn.getName())
                {
                    public void actionPerformed(ActionEvent event)
                    {
                       m_chat.sendSlap(clickedOn.getName());
                    }
                };
                
                return Arrays.asList(slap,ignore);
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
        
        JPopupMenu menu = new JPopupMenu();
        boolean hasActions = false;
        for(IPlayerActionFactory factory : m_actionFactories) 
        {
            List<Action> actions = factory.mouseOnPlayer(player);
            if(actions != null && !actions.isEmpty()) 
            {
                if(hasActions) 
                {
                    menu.addSeparator();
                }
                hasActions = true;
                
                for(Action a : actions)
                {
                    menu.add(a);
                }
            }
        }
        

        if(hasActions)
            menu.show(m_players, e.getX(), e.getY());
    }
    
    /**
     * @param players - a collection of Strings representing player names.
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
                    if(!m_hiddenPlayers.contains(name.getName()))
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
        if(m_chat == null)
            return "";
        String status = m_chat.getStatusManager().getStatus(node);
        if(status == null || status.length() == 0)
        {
            return node.getName();
        }
        
        return node.getName() + " (" + status + ")";
    }

    public void addStatusMessage(String message)
    {
    }

    /**
     * Add an action factory that will be used to populate the pop up meny when 
     * right clicking on a player in the chat panel.
     */
    public void addActionFactory(IPlayerActionFactory actionFactory)
    {
        m_actionFactories.add(actionFactory);
    }
    
    public void remove(IPlayerActionFactory actionFactory)
    {
        m_actionFactories.remove(actionFactory);
    }
    
}
