package games.strategy.engine.data.properties;

import java.io.Serializable;
import java.util.Objects;
import javax.swing.JComponent;

/**
 * Superclass for all implementations of {@link IEditableProperty}.
 *
 * @param <T> The generic Type of the value being stored.
 */
public abstract class AbstractEditableProperty<T>
    implements IEditableProperty<T>, Serializable, Comparable<AbstractEditableProperty<?>> {
  private static final long serialVersionUID = -5005729898242568847L;

  private final String name;
  private final String description;

  public AbstractEditableProperty(final String name, final String description) {
    this.name = name;
    this.description = description;
  }

  @Override
  public int getRowsNeeded() {
    return 1;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public JComponent getViewComponent() {
    final JComponent component = getEditorComponent();
    component.setEnabled(false);
    return component;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name);
  }

  @Override
  public boolean equals(final Object other) {
    return other instanceof AbstractEditableProperty
        && ((AbstractEditableProperty<?>) other).name.equals(name);
  }

  @Override
  public int compareTo(final AbstractEditableProperty<?> other) {
    return name.compareTo(other.getName());
  }

  @Override
  public String toString() {
    return getName() + "=" + getValue().toString();
  }
}
