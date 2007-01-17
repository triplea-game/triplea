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

import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.IMessenger;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

public class AllUsersPanel extends JPanel
{
    
    private final IMessenger m_messenger;
    private JList m_nodes;
    private DefaultListModel m_nodesModel;
    private LobbyAdminStatPanel m_statPane;
    private final List<INode> m_orderedNodes;

    public AllUsersPanel(IMessenger messenger)
    {
        m_messenger = messenger;
        
        m_orderedNodes = new ArrayList<INode>();
        
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
        Collections.sort(m_orderedNodes);
        m_nodesModel.clear();
        for(INode node : m_orderedNodes) {
            m_nodesModel.addElement(node);
        }
        
    }


    private void setWidgetActivation()
    {

    }
    
}



