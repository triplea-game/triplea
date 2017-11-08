package games.strategy.engine.data.properties;

import java.io.Serializable;
import java.util.Objects;

import javax.swing.JComponent;

public abstract class AEditableProperty implements IEditableProperty, Serializable, Comparable<Object> {
  private static final long serialVersionUID = -5005729898242568847L;
  private final String m_name;
  private final String m_description;

  public AEditableProperty(final String name, final String description) {
    m_name = name;
    m_description = description;
  }

  @Override
  public int getRowsNeeded() {
    return 1;
  }

  @Override
  public String getName() {
    return m_name;
  }

  @Override
  public String getDescription() {
    return m_description;
  }

  @Override
  public JComponent getViewComponent() {
    final JComponent component = getEditorComponent();
    component.setEnabled(false);
    return component;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(m_name);
  }

  @Override
  public boolean equals(final Object other) {
    if (other instanceof AEditableProperty) {
      return ((AEditableProperty) other).m_name.equals(m_name);
    }
    return false;
  }

  @Override
  public int compareTo(final Object other) {
    if (other instanceof AEditableProperty) {
      return m_name.compareTo(((AEditableProperty) other).getName());
    }
    return -1;
  }

  @Override
  public String toString() {
    return getName() + "=" + getValue().toString();
  }
}
