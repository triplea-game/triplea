package games.strategy.engine.data.properties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JComponent;

import games.strategy.ui.SwingComponents;

/**
 * A property that uses a list for selecting the value.
 *
 * @param <T> The type of the property value.
 */
public class ComboProperty<T> extends AEditableProperty {
  private static final long serialVersionUID = -3098612299805630587L;
  public static final String POSSIBLE_VALUES_FIELD_NAME = "possibleValues";
  private final List<T> possibleValues;
  private T value;

  /**
   * @param name name of the property.
   * @param defaultValue default string value
   * @param possibleValues collection of values
   */
  public ComboProperty(final String name,
      final String description,
      final T defaultValue,
      final Collection<T> possibleValues) {
    super(name, description);
    this.possibleValues = new ArrayList<>(possibleValues);
    if ((defaultValue == null) && !possibleValues.contains(defaultValue)) {
      throw new IllegalStateException("possible values does not contain default");
    }
    value = defaultValue;
  }

  @Override
  public Object getValue() {
    return value;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void setValue(final Object value) throws ClassCastException {
    this.value = (T) value;
  }

  public void setValueT(final T value) {
    this.value = value;
  }

  @Override
  public JComponent getEditorComponent() {
    final JComboBox<T> box = new JComboBox<>(SwingComponents.newComboBoxModel(possibleValues));
    box.setSelectedItem(value);
    box.addActionListener(e -> value = box.getItemAt(box.getSelectedIndex()));
    return box;
  }

  @Override
  public boolean validate(final Object value) {
    return (possibleValues != null) && possibleValues.contains(value);
  }
}
