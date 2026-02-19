package games.strategy.engine.history;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * DefaultTreeModel is not serializable across jdk versions Instead we use an instance of this class
 * to store our data.
 */
class SerializedHistory implements Serializable {
  private static final long serialVersionUID = -5808427923253751651L;

  private final List<SerializationWriter> writers = new ArrayList<>();
  private final GameData gameData;

  SerializedHistory(final History history, final GameData data, final List<Change> changes) {
    gameData = data;
    final Enumeration<?> enumeration =
        ((DefaultMutableTreeNode) history.getRoot()).preorderEnumeration();
    enumeration.nextElement();
    int changeIndex = 0;
    while (enumeration.hasMoreElements()) {
      final HistoryNode node = (HistoryNode) enumeration.nextElement();
      // write the changes to the start of the node
      if (node instanceof IndexedHistoryNode indexedHistoryNode) {
        while (changeIndex < indexedHistoryNode.getChangeStartIndex()) {
          writers.add(new ChangeSerializationWriter(changes.get(changeIndex)));
          changeIndex++;
        }
      }
      // write the node itself
      writers.add(node.getWriter());
    }
    // write out remaining changes
    while (changeIndex < changes.size()) {
      writers.add(new ChangeSerializationWriter(changes.get(changeIndex)));
      changeIndex++;
    }
  }

  public Object readResolve() {
    final History history = new History(gameData);
    final HistoryWriter historyWriter = history.getHistoryWriter();
    for (final SerializationWriter element : writers) {
      element.write(historyWriter);
    }
    return history;
  }
}
