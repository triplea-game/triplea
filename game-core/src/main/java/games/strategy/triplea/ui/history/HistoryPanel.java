package games.strategy.triplea.ui.history;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
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
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Step;
import games.strategy.triplea.ui.UiContext;

/**
 * Shows the history as a tree.
 */
public class HistoryPanel extends JPanel {
  private static final long serialVersionUID = -8353246449552215276L;
  private final GameData data;
  private final JTree tree;
  private final IHistoryDetailsPanel details;
  private HistoryNode currentPopupNode;
  private final JPopupMenu popup;

  // private boolean m_lockBefore;
  public HistoryPanel(final GameData data, final IHistoryDetailsPanel details, final JPopupMenu popup,
      final UiContext uiContext) {
    mouseOverPanel = false;
    mouseWasOverPanel = false;
    final MouseListener mouseFocusListener = new MouseListener() {
      @Override
      public void mouseReleased(final MouseEvent e) {}

      @Override
      public void mousePressed(final MouseEvent e) {}

      @Override
      public void mouseClicked(final MouseEvent e) {}

      @Override
      public void mouseExited(final MouseEvent e) {
        mouseOverPanel = false;
      }

      @Override
      public void mouseEntered(final MouseEvent e) {
        mouseOverPanel = true;
      }
    };
    addMouseListener(mouseFocusListener);
    this.data = data;
    this.details = details;
    setLayout(new BorderLayout());
    if (!this.data.areChangesOnlyInSwingEventThread()) {
      throw new IllegalStateException();
    }
    tree = new JTree(this.data.getHistory());
    this.data.getHistory().setTreePanel(this);
    tree.expandRow(0);
    this.popup = popup;
    tree.add(this.popup);
    this.popup.addPopupMenuListener(new PopupMenuListener() {
      @Override
      public void popupMenuCanceled(final PopupMenuEvent pme) {
        currentPopupNode = null;
      }

      @Override
      public void popupMenuWillBecomeInvisible(final PopupMenuEvent pme) {}

      @Override
      public void popupMenuWillBecomeVisible(final PopupMenuEvent pme) {}
    });
    final HistoryTreeCellRenderer renderer = new HistoryTreeCellRenderer(uiContext);
    renderer.setLeafIcon(null);
    renderer.setClosedIcon(null);
    renderer.setOpenIcon(null);
    renderer.setBackgroundNonSelectionColor(getBackground());
    tree.setCellRenderer(renderer);
    tree.setBackground(getBackground());
    final JScrollPane scroll = new JScrollPane(tree);
    scroll.addMouseListener(mouseFocusListener);
    for (final Component comp : scroll.getComponents()) {
      comp.addMouseListener(mouseFocusListener);
    }
    scroll.setBorder(null);
    scroll.setViewportBorder(null);
    add(scroll, BorderLayout.CENTER);
    tree.setEditable(false);
    final HistoryNode node = this.data.getHistory().getLastNode();
    this.data.getHistory().gotoNode(node);
    tree.expandPath(new TreePath(node.getPath()));
    tree.setSelectionPath(new TreePath(node.getPath()));
    currentPopupNode = null;
    final JButton previousButton = new JButton("<-Back");
    previousButton.addMouseListener(mouseFocusListener);
    previousButton.addActionListener(e -> previous());
    final JButton nextButton = new JButton("Next->");
    nextButton.addMouseListener(mouseFocusListener);
    nextButton.addActionListener(e -> next());
    final JPanel buttons = new JPanel();
    buttons.setLayout(new GridBagLayout());
    buttons.add(previousButton, new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.WEST,
        GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    buttons.add(nextButton, new GridBagConstraints(1, 0, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH,
        new Insets(0, 0, 0, 0), 0, 0));
    add(buttons, BorderLayout.SOUTH);
    tree.addMouseListener(new MouseListener() {
      @Override
      public void mouseClicked(final MouseEvent me) {
        if (SwingUtilities.isRightMouseButton(me)) {
          currentPopupNode =
              (HistoryNode) tree.getClosestPathForLocation(me.getX(), me.getY()).getLastPathComponent();
          HistoryPanel.this.popup.show(me.getComponent(), me.getX(), me.getY());
        } else if (mouseWasOverPanel) {
          final TreePath clickedPath = new TreePath(
              ((HistoryNode) tree.getClosestPathForLocation(me.getX(), me.getY()).getLastPathComponent()).getPath());
          adaptStayExpandedPathsOnClickedPath(clickedPath);
        }
      }

      private void adaptStayExpandedPathsOnClickedPath(final TreePath clickedPath) {
        if (stayExpandedPaths.contains(clickedPath)) {
          stayExpandedPaths.remove(clickedPath);
          tree.collapsePath(clickedPath);
        } else {
          stayExpandedPaths.add(clickedPath);
          tree.expandPath(clickedPath);
        }
      }

      @Override
      public void mouseEntered(final MouseEvent me) {
        mouseOverPanel = true;
      }

      @Override
      public void mouseExited(final MouseEvent me) {
        mouseOverPanel = false;
      }

      @Override
      public void mousePressed(final MouseEvent me) {}

      @Override
      public void mouseReleased(final MouseEvent me) {}
    });
    tree.addTreeSelectionListener(e -> treeSelectionChanged(e));
  }

  private void previous() {
    if (tree.getSelectionCount() == 0) {
      tree.setSelectionInterval(0, 0);
      return;
    }
    final TreePath path = tree.getSelectionPath();
    final TreeNode selected = (TreeNode) path.getLastPathComponent();
    @SuppressWarnings("unchecked")
    final Enumeration<TreeNode> nodeEnum =
        ((DefaultMutableTreeNode) tree.getModel().getRoot()).depthFirstEnumeration();
    TreeNode previous = null;
    while (nodeEnum.hasMoreElements()) {
      final TreeNode current = nodeEnum.nextElement();
      if (current == selected) {
        break;
      } else if (current.getParent() instanceof Step) {
        previous = current;
      }
    }
    if (previous != null) {
      navigateTo(previous);
    }
  }

  private void navigateTo(final TreeNode target) {
    final TreeNode[] nodes = ((DefaultMutableTreeNode) target).getPath();
    final TreePath newPath = new TreePath(nodes);
    tree.expandPath(newPath);
    tree.setSelectionPath(newPath);
    final int row = tree.getRowForPath(newPath);
    if (row == -1) {
      return;
    }
    final Rectangle bounds = tree.getRowBounds(row);
    if (bounds == null) {
      return;
    }
    // scroll to the far left
    bounds.x = 0;
    bounds.width = 10;
    tree.scrollRectToVisible(bounds);
  }

  private void next() {
    if (tree.getSelectionCount() == 0) {
      tree.setSelectionInterval(0, 0);
      return;
    }
    final TreePath path = tree.getSelectionPath();
    final TreeNode selected = (TreeNode) path.getLastPathComponent();
    @SuppressWarnings("unchecked")
    final Enumeration<TreeNode> nodeEnum = ((DefaultMutableTreeNode) tree.getModel().getRoot()).preorderEnumeration();
    TreeNode next = null;
    boolean foundSelected = false;
    while (nodeEnum.hasMoreElements()) {
      final TreeNode current = nodeEnum.nextElement();
      if (current == selected) {
        foundSelected = true;
      } else if (foundSelected) {
        if (current.getParent() instanceof Step) {
          next = current;
          break;
        }
      }
    }
    if (next != null) {
      navigateTo(next);
    }
  }

  private void treeSelectionChanged(final TreeSelectionEvent e) {
    if (!SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("Wrong thread");
    }
    // move the game to the state of the selected node
    final HistoryNode node = (HistoryNode) e.getPath().getLastPathComponent();
    gotoNode(node);
  }

  private void gotoNode(final HistoryNode node) {
    if (!SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("Not EDT");
    }
    if (details != null) {
      details.render(node);
    }
    data.getHistory().gotoNode(node);
  }

  public HistoryNode getCurrentNode() {
    final TreePath path = tree.getSelectionPath();
    final HistoryNode curNode = (HistoryNode) path.getLastPathComponent();
    return curNode;
  }

  public HistoryNode getCurrentPopupNode() {
    return currentPopupNode;
  }

  public void clearCurrentPopupNode() {
    currentPopupNode = null;
  }

  // remember which paths were expanded
  final Collection<TreePath> stayExpandedPaths = new ArrayList<>();
  private boolean mouseOverPanel = false;
  // to distinguish the first mouse over panel event from the others
  boolean mouseWasOverPanel = false;
  // remember where to start collapsing
  TreePath lastParent = null;

  private boolean addToStayExpanded(final Enumeration<TreePath> paths) {
    final Collection<TreePath> expandPaths = new ArrayList<>();
    while (paths.hasMoreElements()) {
      expandPaths.add(paths.nextElement());
    }
    return stayExpandedPaths.addAll(expandPaths);
  }

  /**
   * collapses parents of last path if it is not in the list of expanded path until the new path is a descendant.
   *
   * @param newPath
   *        new path
   */
  private void collapseUpFromLastParent(final TreePath newPath) {
    TreePath currentParent = lastParent;
    while ((currentParent != null) && !currentParent.isDescendant(newPath)
        && !stayExpandedContainsDescendantOf(currentParent)) {
      tree.collapsePath(currentParent);
      currentParent = currentParent.getParentPath();
    }
  }

  /**
   * @param parentPath
   *        tree path for which descendants should be check.
   * @return whether the expanded path list contains a descendant of parentPath
   */
  private boolean stayExpandedContainsDescendantOf(final TreePath parentPath) {
    for (final TreePath currentPath : stayExpandedPaths) {
      if (parentPath.isDescendant(currentPath)) {
        return true;
      }
    }
    return false;
  }

  /**
   * collapses expanded paths except if new path is a descendant.
   *
   * @param newPath
   *        new path
   */
  private void collapseExpanded(final TreePath newPath) {
    if (!stayExpandedPaths.isEmpty()) {
      // get enumeration of expanded nodes
      TreePath root = newPath;
      while (root.getPathCount() > 1) {
        root = root.getParentPath();
      }
      final Enumeration<TreePath> expandedDescendants = tree.getExpandedDescendants(root);
      final TreePath selectedPath = tree.getSelectionPath();
      // fill stack with nodes that should be collapsed
      final Deque<TreePath> collapsePaths = new ArrayDeque<>();
      while (expandedDescendants.hasMoreElements()) {
        final TreePath currentDescendant = expandedDescendants.nextElement();
        if (!currentDescendant.isDescendant(newPath)
            && ((selectedPath == null) || !currentDescendant.isDescendant(selectedPath))) {
          collapsePaths.add(currentDescendant);
        }
      }
      // collapse found paths
      if (!collapsePaths.isEmpty()) {
        for (final TreePath currentPath : collapsePaths) {
          tree.collapsePath(currentPath);
        }
        stayExpandedPaths.removeAll(collapsePaths);
      }
    }
  }

  public void goToEnd() {
    final HistoryNode last;
    try {
      data.acquireWriteLock();
      last = data.getHistory().getLastNode();
    } finally {
      data.releaseWriteLock();
    }
    final TreePath path = new TreePath(last.getPath());
    final TreePath parent = path.getParentPath();
    if (!mouseOverPanel) {
      // make sure we undo our change of the lock property

      gotoNode(last);
      if (lastParent == null) {
        lastParent = tree.getSelectionPath();
      }
      tree.setSelectionPath(path);
      collapseExpanded(path);
      collapseUpFromLastParent(parent);
      final Rectangle rect = tree.getPathBounds(path);
      rect.setRect(0, rect.getY(), rect.getWidth(), rect.getHeight());
      tree.scrollRectToVisible(rect);
    } else {
      if (!mouseWasOverPanel) {
        // save the lock property so that we can undo it

        TreePath root = parent;
        while (root.getPathCount() > 1) {
          root = root.getParentPath();
        }
        final Enumeration<TreePath> expandedDescendants = tree.getExpandedDescendants(root);
        addToStayExpanded(expandedDescendants);
      } else {
        collapseUpFromLastParent(parent);
      }
      tree.expandPath(parent);
    }
    mouseWasOverPanel = mouseOverPanel;
    lastParent = parent;
  }

  private static final class HistoryTreeCellRenderer extends DefaultTreeCellRenderer {
    private static final long serialVersionUID = -72258573320689596L;
    private final ImageIcon icon = new ImageIcon();
    private final UiContext uiContext;

    HistoryTreeCellRenderer(final UiContext uiContext) {
      this.uiContext = uiContext;
    }

    @Override
    public Component getTreeCellRendererComponent(final JTree tree, final Object value, final boolean sel,
        final boolean expanded, final boolean leaf, final int row, final boolean haveFocus) {
      if (value instanceof Step) {
        final PlayerID player = ((Step) value).getPlayerId();
        if (player != null) {
          if (uiContext != null) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, haveFocus);
            icon.setImage(uiContext.getFlagImageFactory().getSmallFlag(player));
            setIcon(icon);
          } else {
            final String text = value.toString() + " (" + player.getName() + ")";
            super.getTreeCellRendererComponent(tree, text, sel, expanded, leaf, row, haveFocus);
          }
        } else {
          super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, haveFocus);
        }
      } else {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, haveFocus);
      }
      return this;
    }
  }
}
