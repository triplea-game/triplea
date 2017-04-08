package games.strategy.engine.data;

import java.util.ArrayList;
import java.util.List;

/**
 * A Change made of several changes.
 */
public class CompositeChange extends Change {
  private static final long serialVersionUID = 8152962976769419486L;
  private final List<Change> m_changes;

  public CompositeChange(final Change... changes) {
    this();
    add(changes);
  }

  public CompositeChange() {
    m_changes = new ArrayList<>();
  }

  public CompositeChange(final List<Change> changes) {
    m_changes = new ArrayList<>(changes);
  }

  public void add(final Change... changes) {
    for (final Change aChange : changes) {
      if (!aChange.isEmpty()) {
        m_changes.add(aChange);
      }
    }
  }

  @Override
  public Change invert() {
    final List<Change> newChanges = new ArrayList<>();
    // to invert a list of changes, process the opposite of
    // each change in the reverse order of the original list
    for (int i = m_changes.size() - 1; i >= 0; i--) {
      final Change current = m_changes.get(i);
      newChanges.add(current.invert());
    }
    return new CompositeChange(newChanges);
  }

  @Override
  protected void perform(final GameData data) {
    for (final Change current : m_changes) {
      current.perform(data);
    }
  }

  /**
   * @return true if this change is empty, or composed of empty changes.
   */
  @Override
  public boolean isEmpty() {
    for (final Change c : m_changes) {
      if (!c.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  public List<Change> getChanges() {
    return new ArrayList<>(m_changes);
  }

  @Override
  public String toString() {
    return "CompositeChange <" + (m_changes == null ? "null" : m_changes.toString()) + ">";
  }
}
