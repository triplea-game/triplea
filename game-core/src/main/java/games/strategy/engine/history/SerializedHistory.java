package games.strategy.engine.history;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;

/**
 * DefaultTreeModel is not serializable across jdk versions
 * Instead we use an instance of this class to store our data.
 */
class SerializedHistory implements Serializable {
  private static final long serialVersionUID = -5808427923253751651L;
  private final List<SerializationWriter> m_Writers = new ArrayList<>();
  private final GameData m_data;

  public SerializedHistory(final History history, final GameData data, final List<Change> changes) {
    m_data = data;
    int changeIndex = 0;
    final Enumeration<?> enumeration = ((DefaultMutableTreeNode) history.getRoot()).preorderEnumeration();
    enumeration.nextElement();
    while (enumeration.hasMoreElements()) {
      final HistoryNode node = (HistoryNode) enumeration.nextElement();
      // write the changes to the start of the node
      if (node instanceof IndexedHistoryNode) {
        while (changeIndex < ((IndexedHistoryNode) node).getChangeStartIndex()) {
          m_Writers.add(new ChangeSerializationWriter(changes.get(changeIndex)));
          changeIndex++;
        }
      }
      // write the node itself
      m_Writers.add(node.getWriter());
    }
    // write out remaining changes
    while (changeIndex < changes.size()) {
      m_Writers.add(new ChangeSerializationWriter(changes.get(changeIndex)));
      changeIndex++;
    }
  }

  public Object readResolve() {
    final History history = new History(m_data);
    final HistoryWriter historyWriter = history.getHistoryWriter();
    for (final SerializationWriter element : m_Writers) {
      element.write(historyWriter);
    }
    return history;
  }
}
