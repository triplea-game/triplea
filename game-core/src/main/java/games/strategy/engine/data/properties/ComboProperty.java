package games.strategy.engine.data.properties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.swing.JComboBox;
import javax.swing.JComponent;

import org.triplea.swing.SwingComponents;

/**
 * A property that uses a list for selecting the value.
 *
 * @param <T> The type of the property value.
 */
public class ComboProperty<T> extends AbstractEditableProperty<T> {
  private static final long serialVersionUID = -3098612299805630587L;

  private final Collection<T> possibleValues;
  private T value;

  /**
   * Initializes a new instance of the ComboProperty class.
   *
   * @param name name of the property.
   * @param defaultValue default string value
   * @param possibleValues collection of values
   */
  public ComboProperty(final String name,
      final String description,
      final T defaultValue,
      final Collection<T> possibleValues) {
    super(name, description);
    this.possibleValues = Collections.unmodifiableCollection(new ArrayList<>(possibleValues));
    if (defaultValue != null && !possibleValues.contains(defaultValue)) {
      throw new IllegalStateException("possible values does not contain default");
    }
    value = defaultValue;
  }

  @Override
  public T getValue() {
    return value;
  }

  @Override
  public void setValue(final T value) {
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
    return possibleValues != null && possibleValues.contains(value);
  }

  /**
   * Returns an immutable view of the possible values for the property.
   */
  public Collection<T> getPossibleValues() {
    return possibleValues;
  }
}
