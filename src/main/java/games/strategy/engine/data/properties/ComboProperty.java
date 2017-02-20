package games.strategy.engine.data.properties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JComponent;

/**
 * A String property that uses a list for selecting the value
 */
public class ComboProperty<T> extends AEditableProperty {
  private static final long serialVersionUID = -3098612299805630587L;
  public static final String POSSIBLE_VALUES_FIELD_NAME = "possibleValues";
  private final List<T> possibleValues;
  private T value;

  /**
   * @param name
   *        name of the property
   * @param defaultValue
   *        default string value
   * @param possibleValues
   *        collection of Strings
   */
  public ComboProperty(final String name, final String description, final T defaultValue,
      final Collection<T> possibleValues) {
    this(name, description, defaultValue, possibleValues, false);
  }

  @SuppressWarnings("unchecked")
  public ComboProperty(final String name, final String description, final T defaultValue,
      final Collection<T> possibleValues, final boolean allowNone) {
    super(name, description);
    if (!allowNone && !possibleValues.contains(defaultValue) && defaultValue == null) {
      throw new IllegalStateException("possible values does not contain default");
    } else if (allowNone && !possibleValues.contains(defaultValue) && !possibleValues.isEmpty()) {
      value = possibleValues.iterator().next();
    } else if (allowNone && !possibleValues.contains(defaultValue)) {
      try {
        value = (T) "";
      } catch (final Exception e) {
        value = null;
      }
    } else {
      value = defaultValue;
    }
    this.possibleValues = new ArrayList<>(possibleValues);
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
    final JComboBox<T> box = new JComboBox<>(new Vector<>(possibleValues));
    box.setSelectedItem(value);
    box.addActionListener(e -> value = box.getItemAt(box.getSelectedIndex()));
    return box;
  }

  @Override
  public boolean validate(final Object value) {
    if (possibleValues == null || possibleValues.isEmpty()) {
      return false;
    }
    try {
      if (possibleValues.contains(value)) {
        return true;
      }
    } catch (final ClassCastException e) {
      return false;
    } catch (final NullPointerException e) {
      return false;
    }
    return false;
  }
}
