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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Stack;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
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
		m_mouseOverPanel = false;
		m_mouseWasOverPanel = false;
		final MouseListener mouseFocusListener = new MouseListener()
		{
			public void mouseReleased(final MouseEvent e)
			{
			}
			
			public void mousePressed(final MouseEvent e)
			{
			}
			
			public void mouseClicked(final MouseEvent e)
			{
			}
			
			public void mouseExited(final MouseEvent e)
			{
				m_mouseOverPanel = false;
			}
			
			public void mouseEntered(final MouseEvent e)
			{
				m_mouseOverPanel = true;
			}
		};
		addMouseListener(mouseFocusListener);
		m_data = data;
		m_details = details;
		setLayout(new BorderLayout());
		if (!m_data.areChangesOnlyInSwingEventThread())
		{
			throw new IllegalStateException();
		}
		m_tree = new JTree(m_data.getHistory());
		m_data.getHistory().setTreePanel(this);
		m_tree.expandRow(0);
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
		scroll.addMouseListener(mouseFocusListener);
		for (final Component comp : scroll.getComponents())
			comp.addMouseListener(mouseFocusListener);
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
		previousButton.addMouseListener(mouseFocusListener);
		previousButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				previous();
			}
		});
		final JButton nextButton = new JButton("Next->");
		nextButton.addMouseListener(mouseFocusListener);
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
		m_tree.addMouseListener(new MouseListener()
		{
			public void mouseClicked(final MouseEvent me)
			{
				if (SwingUtilities.isRightMouseButton(me))
				{
					m_currentPopupNode = (HistoryNode) m_tree.getClosestPathForLocation(me.getX(), me.getY()).getLastPathComponent();
					m_popup.show(me.getComponent(), me.getX(), me.getY());
				}
				else if (m_mouseWasOverPanel)
				{
					TreePath clickedPath = new TreePath(((HistoryNode) m_tree.getClosestPathForLocation(me.getX(), me.getY()).getLastPathComponent()).getPath());
					adaptStayExpandedPathsOnClickedPath(clickedPath);
				}
			}
			
			private void adaptStayExpandedPathsOnClickedPath(TreePath clickedPath)
			{
				if (m_stayExpandedPaths.contains(clickedPath))
				{
					m_stayExpandedPaths.remove(clickedPath);
					m_tree.collapsePath(clickedPath);
				}
				else
				{
					m_stayExpandedPaths.add(clickedPath);
					m_tree.expandPath(clickedPath);
				}
			}

			public void mouseEntered(final MouseEvent me)
			{
				m_mouseOverPanel = true;
			}
			
			public void mouseExited(final MouseEvent me)
			{
				m_mouseOverPanel = false;
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
		final Enumeration<TreeNode> nodeEnum = ((DefaultMutableTreeNode) m_tree.getModel().getRoot()).depthFirstEnumeration();
		TreeNode previous = null;
		while (nodeEnum.hasMoreElements())
		{
			final TreeNode current = nodeEnum.nextElement();
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
		final Enumeration<TreeNode> nodeEnum = ((DefaultMutableTreeNode) m_tree.getModel().getRoot()).preorderEnumeration();
		TreeNode next = null;
		boolean foundSelected = false;
		while (nodeEnum.hasMoreElements())
		{
			final TreeNode current = nodeEnum.nextElement();
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
	
	final Collection<TreePath> m_stayExpandedPaths = new ArrayList<TreePath>(); // remember which paths were expanded
	private boolean m_mouseOverPanel = false;
	boolean m_mouseWasOverPanel = false; // to distinguish the first mouse over panel event from the others
	TreePath m_lastParent = null; // remember where to start collapsing

	private boolean addToStayExpanded(Enumeration<TreePath> paths)
	{
		Collection<TreePath> expandPaths = new ArrayList<TreePath>();
		while (paths.hasMoreElements())
			expandPaths.add(paths.nextElement());
		return m_stayExpandedPaths.addAll(expandPaths);
	}
	
	/**
	 * collapses parents of last path if it is not in the list of expanded path until the new path is a descendant
	 * 
	 * @param newPath
	 *            new path
	 */
	private void collapseUpFromLastParent(TreePath newPath)
	{
		TreePath currentParent = m_lastParent;
		while (currentParent != null && !currentParent.isDescendant(newPath) && !stayExpandedContainsDescendantOf(currentParent))
		{
			m_tree.collapsePath(currentParent);
			currentParent = currentParent.getParentPath();
		}
	}

	/**
	 * @param parentPath
	 *            tree path for which descendants should be check
	 * @return whether the expanded path list contains a descendant of parentPath
	 */
	private boolean stayExpandedContainsDescendantOf(TreePath parentPath)
	{
		for (TreePath currentPath : m_stayExpandedPaths)
		{
			if (parentPath.isDescendant(currentPath))
				return true;
		}
		return false;
	}
	
	/**
	 * collapses expanded paths except if new path is a descendant
	 * 
	 * @param newPath
	 *            new path
	 */
	private void collapseExpanded(TreePath newPath)
	{
		if (!m_stayExpandedPaths.isEmpty())
		{
			// get enumeration of expanded nodes
			TreePath root = newPath;
			while (root.getPathCount() > 1)
			{
				root = root.getParentPath();
			}
			Enumeration<TreePath> expandedDescendants = m_tree.getExpandedDescendants(root);
			TreePath selectedPath = m_tree.getSelectionPath();
			// fill stack with nodes that should be collapsed
			Stack<TreePath> collapsePaths = new Stack<TreePath>();
			while (expandedDescendants.hasMoreElements())
			{
				TreePath currentDescendant = expandedDescendants.nextElement();
				if (!currentDescendant.isDescendant(newPath) && (selectedPath == null || !currentDescendant.isDescendant(selectedPath)))
				{
					collapsePaths.add(currentDescendant);
				}
			}
			// collapse found paths
			if (!collapsePaths.isEmpty())
			{
				for (TreePath currentPath : collapsePaths)
					m_tree.collapsePath(currentPath);
				m_stayExpandedPaths.removeAll(collapsePaths);
			}
		}
	}

	public void goToEnd()
	{
		final HistoryNode last;
		try
		{
			m_data.acquireWriteLock();
			last = m_data.getHistory().getLastNode();
		} finally
		{
			m_data.releaseWriteLock();
		}
		
		final TreePath path = new TreePath(last.getPath());
		final TreePath parent = path.getParentPath();
		if (!m_mouseOverPanel)
		{
			gotoNode(last);
			m_tree.setSelectionPath(path);
			collapseExpanded(path);
			collapseUpFromLastParent(parent);
			m_tree.scrollPathToVisible(path);
			final Rectangle rect = m_tree.getVisibleRect();
			rect.setRect(0, rect.getY(), rect.getWidth(), rect.getHeight());
			m_tree.scrollRectToVisible(rect);
		}
		else
		{
			if (m_mouseWasOverPanel == false)
			{
				TreePath root = parent;
				while (root.getPathCount() > 1)
				{
					root = root.getParentPath();
				}
				Enumeration<TreePath> expandedDescendants = m_tree.getExpandedDescendants(root);
				addToStayExpanded(expandedDescendants);
			}
			else
			{
				collapseUpFromLastParent(parent);
			}
			m_tree.expandPath(parent);
		}
		m_mouseWasOverPanel = m_mouseOverPanel;
		m_lastParent = parent;
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
