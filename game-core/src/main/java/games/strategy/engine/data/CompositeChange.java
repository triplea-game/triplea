package games.strategy.engine.data;

import java.util.ArrayList;
import java.util.List;

/** A Change made of several changes. */
public class CompositeChange extends Change {
  private static final long serialVersionUID = 8152962976769419486L;

  private final List<Change> changes;

  public CompositeChange(final Change... changes) {
    this();
    add(changes);
  }

  public CompositeChange() {
    changes = new ArrayList<>();
  }

  public CompositeChange(final List<Change> changes) {
    this.changes = new ArrayList<>(changes);
  }

  public void add(final Change... changes) {
    for (final Change change : changes) {
      if (!change.isEmpty()) {
        this.changes.add(change);
      }
    }
  }

  @Override
  public Change invert() {
    final List<Change> newChanges = new ArrayList<>();
    // to invert a list of changes, process the opposite of each change in the reverse order of the
    // original list
    for (int i = changes.size() - 1; i >= 0; i--) {
      final Change current = changes.get(i);
      newChanges.add(current.invert());
    }
    return new CompositeChange(newChanges);
  }

  @Override
  protected void perform(final GameData data) {
    for (final Change current : changes) {
      current.perform(data);
    }
  }

  /** Returns true if this change is empty, or composed of empty changes. */
  @Override
  public boolean isEmpty() {
    for (final Change c : changes) {
      if (!c.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  public List<Change> getChanges() {
    return new ArrayList<>(changes);
  }

  @Override
  public String toString() {
    return "CompositeChange <" + (changes == null ? "null" : changes.toString()) + ">";
  }
}
