package games.strategy.engine.data.properties;

import javax.swing.JComponent;

public abstract class AEditableProperty implements IEditableProperty, java.io.Serializable, Comparable<Object> {
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
    final JComponent rVal = getEditorComponent();
    rVal.setEnabled(false);
    return rVal;
  }

  @Override
  public int hashCode() {
    return m_name.hashCode();
  }

  @Override
  public boolean equals(final Object other) {
    if (other instanceof AEditableProperty) {
      return ((AEditableProperty) other).m_name.equals(this.m_name);
    }
    return false;
  }

  @Override
  public int compareTo(final Object other) {
    if (other instanceof AEditableProperty) {
      return this.m_name.compareTo(((AEditableProperty) other).getName());
    }
    return -1;
  }

  @Override
  public String toString() {
    return getName() + "=" + getValue().toString();
  }
}
