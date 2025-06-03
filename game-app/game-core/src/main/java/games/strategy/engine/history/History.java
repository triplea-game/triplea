package games.strategy.engine.history;

import com.google.common.base.Preconditions;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.triplea.ui.history.HistoryPanel;
import games.strategy.ui.Util;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/**
 * A history of the game. Stored as a tree, the data is organized as Root - Round - Step - Event -
 * Child
 *
 * <dl>
 *   <dt>Round
 *   <dd>the current round in the game, eg 1, 2, 3
 *   <dt>Step
 *   <dd>the current step, e.g. Britain Combat Move
 *   <dt>Event
 *   <dd>an event that happened in the game, e.gj Russia buys 8 inf
 * </dl>
 */
public class History extends DefaultTreeModel {
  private static final long serialVersionUID = -1769876896869L;

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
    } else if (node instanceof Event) {
      lastChangeIndex = ((Event) node).getChangeEndIndex();
    } else if (node instanceof EventChild) {
      lastChangeIndex = ((Event) node.getParent()).getChangeEndIndex();
    } else if (node instanceof IndexedHistoryNode) {
      lastChangeIndex = ((IndexedHistoryNode) node).getChangeEndIndex();
      // If this node is still current, or comes from an old save game where we didn't set it, get
      // the last change index from its last child node.
      if (lastChangeIndex == -1 && node.getChildCount() > 0) {
        lastChangeIndex = getNextChange((HistoryNode) node.getLastChild());
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

  /** Changes the game state to reflect the historical state at {@code node}. */
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

  /**
   * Changes the game state to reflect the historical state at {@code removeAfterNode}, and then
   * removes all changes that occurred after this node.
   */
  public synchronized void removeAllHistoryAfterNode(final HistoryNode removeAfterNode) {
    assertCorrectThread();
    if (!seekingEnabled) {
      nextChangeIndex = changes.size();
      seekingEnabled = true;
    }
    gotoNode(getNearestLeafAtOrBefore(removeAfterNode).orElse((HistoryNode) getRoot()));
    try (GameData.Unlocker ignored = gameData.acquireWriteLock()) {
      if (changes.size() > nextChangeIndex) {
        changes.subList(nextChangeIndex, changes.size()).clear();
      }
      final Enumeration<?> enumeration =
          ((DefaultMutableTreeNode) this.getRoot()).preorderEnumeration();
      enumeration.nextElement();
      boolean startRemoving = false;
      final List<HistoryNode> nodesToRemove = new ArrayList<>();
      while (enumeration.hasMoreElements()) {
        final HistoryNode node = (HistoryNode) enumeration.nextElement();
        if (node instanceof IndexedHistoryNode) {
          final int index = ((IndexedHistoryNode) node).getChangeStartIndex();
          if (index >= nextChangeIndex) {
            startRemoving = true;
          }
          if (startRemoving) {
            nodesToRemove.add(node);
          }
        }
      }
      for (HistoryNode node : nodesToRemove) {
        removeNodeFromParent(node);
      }
    }
  }

  /**
   * Returns the current player, accounting for the fact that we may be looking at a previous node
   * in history, unlike data.getSequence().getStep().getPlayerId().
   */
  public @Nullable GamePlayer getCurrentPlayer() {
    GamePlayer player = null;
    final Enumeration<?> enumeration = ((DefaultMutableTreeNode) getRoot()).preorderEnumeration();
    while (enumeration.hasMoreElements()) {
      final HistoryNode node = (HistoryNode) enumeration.nextElement();
      if (node instanceof Step) {
        player = ((Step) node).getPlayerId().orElse(null);
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
    return player;
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
