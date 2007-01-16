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

package games.strategy.engine.lobby.server.ui;

import java.awt.BorderLayout;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.*;

import games.strategy.net.*;

public class AllUsersPanel extends JPanel
{
    
    private final IMessenger m_messenger;
    private JList m_nodes;
    private DefaultListModel m_nodesModel;
    private LobbyAdminStatPanel m_statPane;
    private final SortedSet<INode> m_orderedNodes;

    public AllUsersPanel(IMessenger messenger)
    {
        m_messenger = messenger;
        
        m_orderedNodes = new TreeSet<INode>(new Comparator<INode>()
        {
        
            public int compare(INode o1, INode o2)
            {
                return o1.toString().compareTo(o2.toString());
            }
        
        });
        
        createComponents();
        layoutComponents();
        setupListeners();
        setWidgetActivation();
    }

    private void createComponents()
    {
        m_nodesModel = new DefaultListModel();
        m_nodes = new JList(m_nodesModel);
        m_statPane = new LobbyAdminStatPanel(m_messenger);

    }

    private void layoutComponents()
    {
        setLayout(new BorderLayout());
        add(new JScrollPane(m_nodes), BorderLayout.CENTER);
        add(m_statPane, BorderLayout.SOUTH);

    }

    private void setupListeners()
    {
        ((IServerMessenger)m_messenger).addConnectionChangeListener(new IConnectionChangeListener()
        {
        
            public void connectionRemoved(final INode to)
            {
                SwingUtilities.invokeLater(new Runnable()
                {
                
                    public void run()
                    {
                      m_orderedNodes.remove(to);
                      refreshModel();
                      
                    }

                
                });
        
            }
        
            public void connectionAdded(final INode to)
            {
                SwingUtilities.invokeLater(new Runnable()
                {
                
                    public void run()
                    {
                      m_orderedNodes.add(to);
                      refreshModel();
                
                    }
                
                });

        
            }
        
        });
        
    }
    
    private void refreshModel()
    {
        m_nodesModel.clear();
        for(INode node : m_orderedNodes) {
            m_nodesModel.addElement(node);
        }
        
    }


    private void setWidgetActivation()
    {

    }
    
}



