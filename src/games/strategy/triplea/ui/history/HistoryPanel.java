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

import games.strategy.engine.data.*;
import games.strategy.engine.history.*;

import games.strategy.triplea.ui.UIContext;

import java.awt.*;
import java.awt.event.*;
import java.util.Enumeration;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

/**
 * Shows the history as a tree.
 */
public class HistoryPanel extends JPanel
{
    private final GameData m_data;

    private final JTree m_tree;

    private final HistoryDetailsPanel m_details;

    private HistoryNode m_currentPopupNode;

    private JPopupMenu m_popup;

    public HistoryPanel(GameData data, HistoryDetailsPanel details, JPopupMenu popup, UIContext uiContext)
    {
        m_data = data;
        m_details = details;

        setLayout(new BorderLayout());

        if(!m_data.areChangesOnlyInSwingEventThread()) {
            throw new IllegalStateException();
        }
        
        m_tree = new JTree(m_data.getHistory());
        m_popup = popup;
        m_tree.add(m_popup);
        m_popup.addPopupMenuListener(new PopupMenuListener() {

            public void popupMenuCanceled(PopupMenuEvent pme)
            {
                m_currentPopupNode = null;
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent pme)
            {
            }

            public void popupMenuWillBecomeVisible(PopupMenuEvent pme)
            {
            }

        });
        HistoryTreeCellRenderer renderer = new HistoryTreeCellRenderer(uiContext);
        renderer.setLeafIcon(null);
        renderer.setClosedIcon(null);
        renderer.setOpenIcon(null);
        renderer.setBackgroundNonSelectionColor(getBackground());
        m_tree.setCellRenderer(renderer);
        m_tree.setBackground(getBackground());

        JScrollPane scroll = new JScrollPane(m_tree);
        scroll.setBorder(null);
        scroll.setViewportBorder(null);
        add(scroll, BorderLayout.CENTER);
        m_tree.setEditable(false);

        HistoryNode node = m_data.getHistory().getLastNode();
        m_data.getHistory().gotoNode(node);
        m_tree.expandPath(new TreePath(node.getPath()));
        m_tree.setSelectionPath(new TreePath(node.getPath()));
        m_currentPopupNode = null;
        JButton previousButton = new JButton("<-Back");
        previousButton.addActionListener(new ActionListener()
        {

            public void actionPerformed(ActionEvent e)
            {
                previous();
            }

        });

        JButton nextButton = new JButton("Next->");
        nextButton.addActionListener(new ActionListener()
        {

            public void actionPerformed(ActionEvent e)
            {
                next();
            }

        });

        JPanel buttons = new JPanel();
        buttons.setLayout(new GridBagLayout());

        buttons.add(previousButton, new GridBagConstraints(0,0, 1,1 ,1,1,GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0,0));
        buttons.add(nextButton, new GridBagConstraints(1,0, 1,1 ,1,1,GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0,0));

        add(buttons, BorderLayout.SOUTH);

        m_tree.getModel().addTreeModelListener(new TreeModelListener()
        {
            public void treeNodesChanged(TreeModelEvent e)
            {
                goToEnd();
            }

            public void treeNodesInserted(TreeModelEvent e)
            {
                goToEnd();
            }

            public void treeNodesRemoved(TreeModelEvent e)
            {
            }

            public void treeStructureChanged(TreeModelEvent e)
            {
                goToEnd();
            }

        });
        m_tree.addMouseListener(new MouseListener() {

            public void mouseClicked(MouseEvent me)
            {
                if(SwingUtilities.isRightMouseButton(me))
                {
                    m_currentPopupNode = (HistoryNode)m_tree.getClosestPathForLocation(me.getX(), me.getY()).getLastPathComponent();
                    m_popup.show(me.getComponent(), me.getX(), me.getY());
                }
            }

            public void mouseEntered(MouseEvent me)
            {
            }

            public void mouseExited(MouseEvent me)
            {
            }

            public void mousePressed(MouseEvent me)
            {
            }

            public void mouseReleased(MouseEvent me)
            {
            }

        });
        m_tree.addTreeSelectionListener(new TreeSelectionListener()
        {

            public void valueChanged(TreeSelectionEvent e)
            {
                treeSelectionChanged(e);
            }
        });
    }

    private void previous()
    {
        if (m_tree.getSelectionCount() == 0)
        {
            m_tree.setSelectionInterval(0, 0);
            return;
        }

        TreePath path = m_tree.getSelectionPath();

        TreeNode selected = (TreeNode) path.getLastPathComponent();

        Enumeration nodeEnum = ((DefaultMutableTreeNode) m_tree.getModel().getRoot()).depthFirstEnumeration();

        TreeNode previous = null;

        while (nodeEnum.hasMoreElements())
        {
            TreeNode current = (TreeNode) nodeEnum.nextElement();
            if (current == selected)
            {
                break;
            }
            else if (current.getParent() instanceof Step)
            {
                previous = current;
            }    

            
        }

        if (previous != null)
        {
            navigateTo(previous);
        }
    }

    private void navigateTo(TreeNode target)
    {
        TreeNode[] nodes = ((DefaultMutableTreeNode) target).getPath();
        TreePath newPath = new TreePath(nodes);
        m_tree.expandPath(newPath);
        m_tree.setSelectionPath(newPath);
        
        int row = m_tree.getRowForPath(newPath);
        if(row == -1)
            return;
        
        Rectangle bounds = m_tree.getRowBounds(row);
        if(bounds == null)
            return;
        //scroll to the far left
        bounds.x = 0;
        bounds.width = 10;
        m_tree.scrollRectToVisible(bounds);
    }

    private void next()
    {
        if (m_tree.getSelectionCount() == 0)
        {
            m_tree.setSelectionInterval(0, 0);
            return;
        }

        TreePath path = m_tree.getSelectionPath();

        TreeNode selected = (TreeNode) path.getLastPathComponent();

        Enumeration nodeEnum = ((DefaultMutableTreeNode) m_tree.getModel().getRoot()).preorderEnumeration();

        TreeNode next = null;
        boolean foundSelected = false;
        while (nodeEnum.hasMoreElements())
        {
            TreeNode current = (TreeNode) nodeEnum.nextElement();
            if (current == selected)
            {
                foundSelected = true;
            }
            else if(foundSelected)
            {
                if(current.getParent() instanceof Step)
                {
                    next = current;
                    break;
                }
                    
            }
        }

        if (next != null)
        {
            navigateTo(next);
        }

    }

    private void treeSelectionChanged(TreeSelectionEvent e)
    {
        if(!SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("Wrong thread");
        
        // move the game to the state of the selected node
        HistoryNode node = (HistoryNode) e.getPath().getLastPathComponent();
        gotoNode(node);
    }

    private void gotoNode(HistoryNode node)
    {
        if(!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Not EDT");
        }
        
        m_details.render(node);
        m_data.getHistory().gotoNode(node);
    }

    public HistoryNode getCurrentNode()
    {
        TreePath path = m_tree.getSelectionPath();
        HistoryNode curNode = (HistoryNode)path.getLastPathComponent();
        return curNode;
    }

    public HistoryNode getCurrentPopupNode()
    {
        return m_currentPopupNode;
    }

    public void clearCurrentPopupNode()
    {
        m_currentPopupNode = null;
    }

    public void goToEnd()
    {
        Runnable r = new Runnable()
        {
            public void run()
            {
                HistoryNode last = m_data.getHistory().getLastNode();
                gotoNode(last);

                TreePath path = new TreePath(last.getPath());
                m_tree.expandPath(path);
                m_tree.setSelectionPath(path);
            }

        };
        if (SwingUtilities.isEventDispatchThread())
            r.run();
        else
            SwingUtilities.invokeLater(r);

    }

}

class HistoryTreeCellRenderer extends DefaultTreeCellRenderer
{
    private ImageIcon icon = new ImageIcon();

    private final UIContext m_uiContext;

    public HistoryTreeCellRenderer(UIContext context)
    {
        m_uiContext = context;
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean haveFocus)
    {

        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, haveFocus);
        if (value instanceof Step)
        {
            PlayerID player = ((Step) value).getPlayerID();
            if (player != null)
            {
                icon.setImage(m_uiContext.getFlagImageFactory().getSmallFlag(player));
                setIcon(icon);
            }
        }
        return this;
    }

}
