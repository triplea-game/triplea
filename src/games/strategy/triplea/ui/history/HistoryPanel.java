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


package games.strategy.triplea.ui.history;

import javax.swing.*;
import java.util.*;
import games.strategy.engine.history.*;
import java.awt.*;
import games.strategy.engine.data.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import games.strategy.triplea.image.*;

/**
 * Shows the history as a tree.
 */
public class HistoryPanel extends JPanel
{
    private final GameData m_data;
    private final JTree m_tree;
    private HistoryNode m_currentNode;
    private final ChangePerformer m_changePerformer;
    private final HistoryDetailsPanel m_details;

    public HistoryPanel(GameData data, HistoryDetailsPanel details)
    {
        m_data = data;
        m_details = details;
        m_changePerformer = new ChangePerformer(m_data);
        setLayout(new BorderLayout());

        m_tree = new JTree(m_data.getHistory());

        HistoryTreeCellRenderer renderer = new HistoryTreeCellRenderer();
        renderer.setLeafIcon(null);
        renderer.setClosedIcon(null);
        renderer.setOpenIcon(null);
        renderer.setBackgroundNonSelectionColor(getBackground());
        m_tree.setCellRenderer(renderer);
        m_tree.setBackground(getBackground());


        add(new JScrollPane(m_tree), BorderLayout.CENTER);
        m_tree.setEditable(false);

        m_currentNode = m_data.getHistory().getLastNode();
        m_tree.expandPath(new TreePath(m_currentNode.getPath() ));
        m_tree.setSelectionPath( new TreePath( m_currentNode.getPath() ));

        m_tree.addTreeSelectionListener(
        new TreeSelectionListener()
        {

            public void valueChanged(TreeSelectionEvent e)
            {
                treeSelectionChanged(e);
            }
        }
        );
    }

    private void treeSelectionChanged(TreeSelectionEvent e)
    {
        //move the game to the state of the selected node
       HistoryNode node = (HistoryNode)  e.getPath().getLastPathComponent();
       gotoNode(node);
    }

    private void gotoNode(HistoryNode node)
    {
        Change dataChange = m_data.getHistory().getDelta(m_currentNode, node);
        m_currentNode = node;

        m_details.render(m_currentNode);
        if(dataChange != null)
            m_changePerformer.perform(dataChange);


    }

    public void goToEnd()
    {
        HistoryNode last = m_data.getHistory().getLastNode();
        gotoNode(last);
    }

}

class HistoryTreeCellRenderer extends DefaultTreeCellRenderer
  {
    private ImageIcon icon = new ImageIcon();

    public Component getTreeCellRendererComponent(
                        JTree tree,
                        Object value,
                        boolean sel,
                        boolean expanded,
                        boolean leaf,
                        int row,
                        boolean hasFocus) {

        super.getTreeCellRendererComponent(
                        tree, value, sel,
                        expanded, leaf, row,
                        hasFocus);
        if (value instanceof Step)
        {

           PlayerID player = ((Step) value).getPlayerID();
           if(player != null)
           {
             icon.setImage(FlagIconImageFactory.instance().getSmallFlag(player));
             setIcon(icon);
           }
        }
        return this;
    }





  }
