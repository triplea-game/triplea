/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package games.strategy.triplea.ui.history;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Step;
import games.strategy.triplea.ui.UIContext;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Enumeration;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 * Shows the history as a tree.
 */
public class HistoryPanel extends JPanel
{
	private final GameData m_data;
	private final JTree m_tree;
	private final HistoryDetailsPanel m_details;
	private HistoryNode m_currentPopupNode;
	private final JPopupMenu m_popup;
	
	public HistoryPanel(final GameData data, final HistoryDetailsPanel details, final JPopupMenu popup, final UIContext uiContext)
	{
		m_data = data;
		m_details = details;
		setLayout(new BorderLayout());
		if (!m_data.areChangesOnlyInSwingEventThread())
		{
			throw new IllegalStateException();
		}
		m_tree = new JTree(m_data.getHistory());
		m_popup = popup;
		m_tree.add(m_popup);
		m_popup.addPopupMenuListener(new PopupMenuListener()
		{
			public void popupMenuCanceled(final PopupMenuEvent pme)
			{
				m_currentPopupNode = null;
			}
			
			public void popupMenuWillBecomeInvisible(final PopupMenuEvent pme)
			{
			}
			
			public void popupMenuWillBecomeVisible(final PopupMenuEvent pme)
			{
			}
		});
		final HistoryTreeCellRenderer renderer = new HistoryTreeCellRenderer(uiContext);
		renderer.setLeafIcon(null);
		renderer.setClosedIcon(null);
		renderer.setOpenIcon(null);
		renderer.setBackgroundNonSelectionColor(getBackground());
		m_tree.setCellRenderer(renderer);
		m_tree.setBackground(getBackground());
		final JScrollPane scroll = new JScrollPane(m_tree);
		scroll.setBorder(null);
		scroll.setViewportBorder(null);
		add(scroll, BorderLayout.CENTER);
		m_tree.setEditable(false);
		final HistoryNode node = m_data.getHistory().getLastNode();
		m_data.getHistory().gotoNode(node);
		m_tree.expandPath(new TreePath(node.getPath()));
		m_tree.setSelectionPath(new TreePath(node.getPath()));
		m_currentPopupNode = null;
		final JButton previousButton = new JButton("<-Back");
		previousButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				previous();
			}
		});
		final JButton nextButton = new JButton("Next->");
		nextButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				next();
			}
		});
		final JPanel buttons = new JPanel();
		buttons.setLayout(new GridBagLayout());
		buttons.add(previousButton, new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		buttons.add(nextButton, new GridBagConstraints(1, 0, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		add(buttons, BorderLayout.SOUTH);
		m_tree.getModel().addTreeModelListener(new TreeModelListener()
		{
			public void treeNodesChanged(final TreeModelEvent e)
			{
				goToEnd();
			}
			
			public void treeNodesInserted(final TreeModelEvent e)
			{
				goToEnd();
			}
			
			public void treeNodesRemoved(final TreeModelEvent e)
			{
			}
			
			public void treeStructureChanged(final TreeModelEvent e)
			{
				goToEnd();
			}
		});
		m_tree.addMouseListener(new MouseListener()
		{
			public void mouseClicked(final MouseEvent me)
			{
				if (SwingUtilities.isRightMouseButton(me))
				{
					m_currentPopupNode = (HistoryNode) m_tree.getClosestPathForLocation(me.getX(), me.getY()).getLastPathComponent();
					m_popup.show(me.getComponent(), me.getX(), me.getY());
				}
			}
			
			public void mouseEntered(final MouseEvent me)
			{
			}
			
			public void mouseExited(final MouseEvent me)
			{
			}
			
			public void mousePressed(final MouseEvent me)
			{
			}
			
			public void mouseReleased(final MouseEvent me)
			{
			}
		});
		m_tree.addTreeSelectionListener(new TreeSelectionListener()
		{
			public void valueChanged(final TreeSelectionEvent e)
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
		final TreePath path = m_tree.getSelectionPath();
		final TreeNode selected = (TreeNode) path.getLastPathComponent();
		final Enumeration nodeEnum = ((DefaultMutableTreeNode) m_tree.getModel().getRoot()).depthFirstEnumeration();
		TreeNode previous = null;
		while (nodeEnum.hasMoreElements())
		{
			final TreeNode current = (TreeNode) nodeEnum.nextElement();
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
	
	private void navigateTo(final TreeNode target)
	{
		final TreeNode[] nodes = ((DefaultMutableTreeNode) target).getPath();
		final TreePath newPath = new TreePath(nodes);
		m_tree.expandPath(newPath);
		m_tree.setSelectionPath(newPath);
		final int row = m_tree.getRowForPath(newPath);
		if (row == -1)
			return;
		final Rectangle bounds = m_tree.getRowBounds(row);
		if (bounds == null)
			return;
		// scroll to the far left
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
		final TreePath path = m_tree.getSelectionPath();
		final TreeNode selected = (TreeNode) path.getLastPathComponent();
		final Enumeration nodeEnum = ((DefaultMutableTreeNode) m_tree.getModel().getRoot()).preorderEnumeration();
		TreeNode next = null;
		boolean foundSelected = false;
		while (nodeEnum.hasMoreElements())
		{
			final TreeNode current = (TreeNode) nodeEnum.nextElement();
			if (current == selected)
			{
				foundSelected = true;
			}
			else if (foundSelected)
			{
				if (current.getParent() instanceof Step)
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
	
	private void treeSelectionChanged(final TreeSelectionEvent e)
	{
		if (!SwingUtilities.isEventDispatchThread())
			throw new IllegalStateException("Wrong thread");
		// move the game to the state of the selected node
		final HistoryNode node = (HistoryNode) e.getPath().getLastPathComponent();
		gotoNode(node);
	}
	
	private void gotoNode(final HistoryNode node)
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			throw new IllegalStateException("Not EDT");
		}
		m_details.render(node);
		m_data.getHistory().gotoNode(node);
	}
	
	public HistoryNode getCurrentNode()
	{
		final TreePath path = m_tree.getSelectionPath();
		final HistoryNode curNode = (HistoryNode) path.getLastPathComponent();
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
		final Runnable r = new Runnable()
		{
			public void run()
			{
				final HistoryNode last = m_data.getHistory().getLastNode();
				gotoNode(last);
				final TreePath path = new TreePath(last.getPath());
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
	private final ImageIcon icon = new ImageIcon();
	private final UIContext m_uiContext;
	
	public HistoryTreeCellRenderer(final UIContext context)
	{
		m_uiContext = context;
	}
	
	@Override
	public Component getTreeCellRendererComponent(final JTree tree, final Object value, final boolean sel, final boolean expanded, final boolean leaf, final int row, final boolean haveFocus)
	{
		super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, haveFocus);
		if (value instanceof Step)
		{
			final PlayerID player = ((Step) value).getPlayerID();
			if (player != null)
			{
				icon.setImage(m_uiContext.getFlagImageFactory().getSmallFlag(player));
				setIcon(icon);
			}
		}
		return this;
	}
}
