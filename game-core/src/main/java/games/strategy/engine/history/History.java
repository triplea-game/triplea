package games.strategy.engine.history;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.triplea.ui.history.HistoryPanel;

/**
 * A history of the game.
 * Stored as a tree, the data is organized as
 * Root
 * - Round
 * - Step
 * - Event
 * - Child
 * Round - the current round in the game, eg 1, 2, 3
 * Step - the current step, eg Britian Combat Move
 * Event - an event that happened in the game, eg Russia buys 8 inf.
 */
public class History extends DefaultTreeModel {
  private static final long serialVersionUID = -1769876896869L;

  private final HistoryWriter writer = new HistoryWriter(this);
  private final List<Change> changes = new ArrayList<>();
  private final GameData gameData;
  private HistoryNode currentNode;
  private HistoryPanel panel = null;

  private void assertCorrectThread() {
    if (gameData.areChangesOnlyInSwingEventThread() && !SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("Wrong thread");
    }
  }

  public History(final GameData data) {
    super(new RootHistoryNode("Game History"));
    gameData = data;
  }

  public HistoryWriter getHistoryWriter() {
    return writer;
  }

  public void setTreePanel(final HistoryPanel panel) {
    this.panel = panel;
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

  private int getLastChange(final HistoryNode node) {
    final int lastChangeIndex;
    if (node == getRoot()) {
      lastChangeIndex = 0;
    } else if (node instanceof Event) {
      lastChangeIndex = ((Event) node).getChangeEndIndex();
    } else if (node instanceof EventChild) {
      lastChangeIndex = ((Event) node.getParent()).getChangeEndIndex();
    } else if (node instanceof IndexedHistoryNode) {
      lastChangeIndex = ((IndexedHistoryNode) node).getChangeStartIndex();
    } else {
      lastChangeIndex = 0;
    }
    if (lastChangeIndex == -1) {
      return changes.size();
    }
    return lastChangeIndex;
  }

  public Change getDelta(final HistoryNode start, final HistoryNode end) {
    assertCorrectThread();
    final int firstChange = getLastChange(start);
    final int lastChange = getLastChange(end);
    if (firstChange == lastChange) {
      return null;
    }
    final List<Change> deltaChanges =
        changes.subList(Math.min(firstChange, lastChange), Math.max(firstChange, lastChange));
    final Change compositeChange = new CompositeChange(deltaChanges);
    return (lastChange >= firstChange) ? compositeChange : compositeChange.invert();
  }

  public synchronized void gotoNode(final HistoryNode node) {
    assertCorrectThread();
    getGameData().acquireWriteLock();
    try {
      if (currentNode == null) {
        currentNode = getLastNode();
      }
      final Change dataChange = getDelta(currentNode, node);
      currentNode = node;
      if (dataChange != null) {
        gameData.performChange(dataChange);
      }
    } finally {
      getGameData().releaseWriteLock();
    }
  }

  public synchronized void removeAllHistoryAfterNode(final HistoryNode removeAfterNode) {
    gotoNode(removeAfterNode);
    assertCorrectThread();
    getGameData().acquireWriteLock();
    try {
      final int lastChange = getLastChange(removeAfterNode) + 1;
      while (changes.size() > lastChange) {
        changes.remove(lastChange);
      }
      final List<HistoryNode> nodesToRemove = new ArrayList<>();
      final Enumeration<?> enumeration = ((DefaultMutableTreeNode) this.getRoot()).preorderEnumeration();
      enumeration.nextElement();
      boolean startRemoving = false;
      while (enumeration.hasMoreElements()) {
        final HistoryNode node = (HistoryNode) enumeration.nextElement();
        if (node instanceof IndexedHistoryNode) {
          final int index = ((IndexedHistoryNode) node).getChangeStartIndex();
          if (index >= lastChange) {
            startRemoving = true;
          }
          if (startRemoving) {
            nodesToRemove.add(node);
          }
        }
      }
      while (!nodesToRemove.isEmpty()) {
        this.removeNodeFromParent(nodesToRemove.remove(0));
      }
    } finally {
      getGameData().releaseWriteLock();
    }
  }

  synchronized void changeAdded(final Change change) {
    changes.add(change);
    if (currentNode == null) {
      return;
    }
    if (currentNode == getLastNode()) {
      gameData.performChange(change);
    }
  }

  private Object writeReplace() {
    return new SerializedHistory(this, gameData, changes);
  }

  List<Change> getChanges() {
    return changes;
  }

  GameData getGameData() {
    return gameData;
  }
}
