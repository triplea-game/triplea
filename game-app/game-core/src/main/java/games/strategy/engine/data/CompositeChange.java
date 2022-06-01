package games.strategy.engine.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** A Change made of several changes. */
public class CompositeChange extends Change {
  private static final long serialVersionUID = 8152962976769419486L;

  private final List<Change> changes;
  private boolean inverted = false;

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

  public CompositeChange(final List<Change> changes, final boolean inverted) {
    this.changes = new ArrayList<>(changes);
    this.inverted = inverted;
  }

  /**
   * Flattens the list of changes so that there are no CompositeChanges
   *
   * <p>If there is a child CompositeChange, its children are added to the list and it is removed
   * from the list. This will recursively go through any CompositeChange children.
   *
   * @return A new CompositeChange that doesn't have any CompositeChange children
   */
  public CompositeChange flatten() {
    return new CompositeChange(
        changes.stream()
            .map(
                change ->
                    change instanceof CompositeChange
                        ? ((CompositeChange) change).flatten().getChanges()
                        : List.of(change))
            .flatMap(Collection::stream)
            .collect(Collectors.toList()),
        inverted);
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
    // Important: We can't invert the sub-changes upfront, because some revert() implementations,
    // like RemoveUnits.invert() which calls new AddUnits(), rely on the GameData state and will
    // cause errors if revert() is called while at a different node.
    // Instead, we construct a CompositeChange with inverted set to true.
    return new CompositeChange(changes, true);
  }

  @Override
  protected void perform(final GameState data) {
    if (inverted) {
      // Perform inverted changes in reverse order.
      for (int i = changes.size() - 1; i >= 0; i--) {
        final Change current = changes.get(i).invert();
        current.perform(data);
      }
    } else {
      for (final Change current : changes) {
        current.perform(data);
      }
    }
  }

  /** Returns true if this change is empty, or composed of empty changes. */
  @Override
  public boolean isEmpty() {
    return changes.stream().allMatch(Change::isEmpty);
  }

  public List<Change> getChanges() {
    return Collections.unmodifiableList(changes);
  }

  @Override
  public String toString() {
    return "CompositeChange <" + changes + ">";
  }
}
