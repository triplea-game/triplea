package games.strategy.engine.history;

import com.google.common.base.Preconditions;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.triplea.ui.history.HistoryPanel;
import games.strategy.ui.Util;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

/// A history of the game.
///
/// The history is stored as a tree with the following structure:
///
/// Root → Round → Step → Event → Child
///
/// - **Round**: The current round in the game, for example, 1, 2, or 3.
/// - **Step**: The current game step, for example, "Britain Combat Move".
/// - **Event**: An event that occurred during the game, for example, "Russia buys 8 infantry".
public class History extends DefaultTreeModel {
  @Serial private static final long serialVersionUID = -1769876896869L;

  private final HistoryWriter writer = new HistoryWriter(this);
  private final List<Change> changes = new ArrayList<>();
  private final GameData gameData;
  private HistoryPanel panel;
  // Index at which point we are in history. Only valid if seekingEnabled is true.
  private int nextChangeIndex;
  private boolean seekingEnabled = false;

  public History(final GameData data) {
    super(new RootHistoryNode("Game History"));
    gameData = data;
  }

  private static HistoryNode copyNode(final HistoryNode source) {
    final HistoryNode copy = (HistoryNode) source.clone();

    for (int i = 0; i < source.getChildCount(); i++) {
      copy.add(copyNode((HistoryNode) source.getChildAt(i)));
    }

    return copy;
  }

  public void cloneNodesFromHistory(History baseHistory) {
    HistoryNode root = (HistoryNode) getRoot();
    root.removeAllChildren(); //
    ((HistoryNode) baseHistory.getRoot())
        .children()
        .asIterator()
        .forEachRemaining(originalNode -> root.add(copyNode((HistoryNode) originalNode)));
    changes.clear();
    changes.addAll(baseHistory.changes);
    nextChangeIndex = baseHistory.nextChangeIndex;
  }

  private void assertCorrectThread() {
    if (gameData.areChangesOnlyInSwingEventThread()) {
      Util.ensureOnEventDispatchThread();
    }
  }

  public HistoryWriter getHistoryWriter() {
    return writer;
  }

  public HistoryNode enableSeeking(final HistoryPanel panel) {
    Preconditions.checkState(!seekingEnabled);
    this.panel = panel;
    nextChangeIndex = changes.size();
    seekingEnabled = true;
    HistoryNode lastNode = getLastNode();
    gotoNode(lastNode);
    return lastNode;
  }

  public void goToEnd() {
    if (panel != null) {
      panel.goToEnd();
    }
  }

  public HistoryNode getLastNode() {
    assertCorrectThread();
    return getLastChildInternal((HistoryNode) getRoot());
  }

  private HistoryNode getLastChildInternal(final HistoryNode node) {
    if (node.getChildCount() == 0) {
      return node;
    }
    return getLastChildInternal((HistoryNode) node.getLastChild());
  }

  private int getNextChange(final HistoryNode node) {
    int lastChangeIndex;
    if (node == getRoot()) {
      lastChangeIndex = 0;
    } else if (node instanceof Event event) {
      lastChangeIndex = event.getChangeEndIndex();
    } else if (node instanceof EventChild eventChild) {
      lastChangeIndex = ((Event) eventChild.getParent()).getChangeEndIndex();
    } else if (node instanceof IndexedHistoryNode indexedHistoryNode) {
      lastChangeIndex = indexedHistoryNode.getChangeEndIndex();
      // If this node is still current, or comes from an old save game where we didn't set it, get
      // the last change index from its last child node.
      if (lastChangeIndex == -1 && indexedHistoryNode.getChildCount() > 0) {
        lastChangeIndex = getNextChange((HistoryNode) indexedHistoryNode.getLastChild());
      }
    } else {
      lastChangeIndex = 0;
    }
    if (lastChangeIndex == -1) {
      return changes.size();
    }
    return lastChangeIndex;
  }

  private Change getDeltaTo(int changeIndex) {
    final List<Change> deltaChanges =
        changes.subList(
            Math.min(nextChangeIndex, changeIndex), Math.max(nextChangeIndex, changeIndex));
    final Change compositeChange = new CompositeChange(deltaChanges);
    return (changeIndex >= nextChangeIndex) ? compositeChange : compositeChange.invert();
  }

  /// Changes the game state to reflect the historical state at {@code node}. */
  public synchronized void gotoNode(final HistoryNode node) {
    assertCorrectThread();
    Preconditions.checkNotNull(node);
    Preconditions.checkState(seekingEnabled);
    try (GameData.Unlocker ignored = gameData.acquireWriteLock()) {
      final int nodeChangeIndex = getNextChange(node);
      if (nodeChangeIndex != nextChangeIndex) {
        gameData.performChange(getDeltaTo(nodeChangeIndex));
        nextChangeIndex = nodeChangeIndex;
      }
    }
  }

  /// Changes the game state to reflect the historical state at {@code removeAfterNode}, and then
  /// removes all changes that occurred after this node.
  public synchronized void removeAllHistoryAfterNode(final HistoryNode removeAfterNode) {
    assertCorrectThread();
    if (!seekingEnabled) {
      seekingEnabled = true;
    }
    HistoryNode targetNode =
        getNearestLeafAtOrBefore(removeAfterNode).orElse((HistoryNode) getRoot());
    gotoNode(targetNode);
    try (GameData.Unlocker ignored = gameData.acquireWriteLock()) {
      if (changes.size() > nextChangeIndex) {
        changes.subList(nextChangeIndex, changes.size()).clear();
      }
      final List<HistoryNode> nodesToRemove =
          collectNodesFromChange((HistoryNode) getRoot(), nextChangeIndex);
      removeNodesFromTheirParents(nodesToRemove);
    }
  }

  private List<HistoryNode> collectNodesFromChange(
      HistoryNode startNode, int startChangeIndexToCollect) {
    final List<HistoryNode> nodesAfter = new ArrayList<>();

    Iterator<TreeNode> subNodeIterator = startNode.children().asIterator();
    while (subNodeIterator.hasNext()) {
      if (subNodeIterator.next() instanceof IndexedHistoryNode subIndexNode) {
        if (subIndexNode.getChangeStartIndex() >= startChangeIndexToCollect) {
          nodesAfter.add(subIndexNode);
          continue;
        }
        int changeEndIndex = subIndexNode.getChangeEndIndex();
        if (changeEndIndex < 0 || startChangeIndexToCollect < changeEndIndex) {
          nodesAfter.addAll(collectNodesFromChange(subIndexNode, startChangeIndexToCollect));
        }
      }
    }

    return nodesAfter;
  }

  /// Mass remove nodes similar to {@link DefaultTreeModel#removeNodeFromParent(MutableTreeNode)}.
  ///
  /// @param nodesToRemove List of nodes to be removed (without any of their subnodes)
  private void removeNodesFromTheirParents(List<HistoryNode> nodesToRemove) {

    final Map<MutableTreeNode, List<HistoryNode>> nodesByParent =
        nodesToRemove.stream()
            .collect(
                Collectors.groupingBy(
                    node -> (MutableTreeNode) node.getParent(),
                    LinkedHashMap::new,
                    Collectors.toList()));

    nodesByParent.forEach(
        (parent, childNodes) -> {
          final int countChildNodes = childNodes.size();
          int[] childIndices = new int[countChildNodes];
          Object[] removedArray = new Object[countChildNodes];
          for (int currentIndex = 0; currentIndex < countChildNodes; ++currentIndex) {
            HistoryNode historyNode = childNodes.get(currentIndex);
            childIndices[currentIndex] = parent.getIndex(historyNode);
            removedArray[currentIndex] = historyNode;
          }
          for (int i = childIndices.length - 1; i >= 0; i--) {
            parent.remove(childIndices[i]);
          }
          nodesWereRemoved(parent, childIndices, removedArray);
        });
  }

  /// Returns the current player, accounting for the fact that we may be looking at a previous node
  /// in history, unlike {@code data.getSequence().getStep().getPlayerId()}.
  public Optional<GamePlayer> getCurrentPlayer() {
    Optional<GamePlayer> optionalCurrentPlayer = Optional.empty();
    final Enumeration<?> enumeration = ((DefaultMutableTreeNode) getRoot()).preorderEnumeration();
    while (enumeration.hasMoreElements()) {
      final HistoryNode node = (HistoryNode) enumeration.nextElement();
      if (node instanceof Step step) {
        optionalCurrentPlayer = step.getPlayerId();
      }
      if (node.isLeaf()) {
        // Don't do this logic on non-leaf nodes as getNextChange() will return
        // the next change after this non-leaf, skipping all the child nodes.
        int nodeChangeIndex = getNextChange(node);
        if (seekingEnabled && nodeChangeIndex > nextChangeIndex) {
          break;
        }
      }
    }
    return optionalCurrentPlayer;
  }

  public Optional<HistoryNode> getNearestLeafAtOrBefore(HistoryNode node) {
    if (node.isLeaf()) {
      return Optional.of(node);
    }
    return Optional.ofNullable((HistoryNode) node.getPreviousLeaf());
  }

  synchronized void changeAdded(final Change change) {
    changes.add(change);
    if (seekingEnabled && nextChangeIndex == changes.size() - 1) {
      gameData.performChange(change);
      nextChangeIndex = changes.size();
    }
  }

  private Object writeReplace() {
    return new SerializedHistory(this, gameData, changes);
  }

  List<Change> getChanges() {
    return Collections.unmodifiableList(changes);
  }

  GameData getGameData() {
    return gameData;
  }
}
