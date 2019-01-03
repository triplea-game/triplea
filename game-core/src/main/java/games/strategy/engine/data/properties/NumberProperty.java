package games.strategy.engine.data.properties;

import javax.swing.JComponent;

import games.strategy.ui.IntTextField;

/** Implementation of {@link IEditableProperty} for an integer value. */
public class NumberProperty extends AbstractEditableProperty<Integer> {
  private static final long serialVersionUID = 6826763550643504789L;

  // Keep this in sync with the matching property name, used by reflection.
  public static final String MAX_PROPERTY_NAME = "max";
  private final int max;

  // Keep this in sync with the matching property name, used by reflection.
  public static final String MIN_PROPERTY_NAME = "min";
  private final int min;

  private int value;

  public NumberProperty(
      final String name, final String description, final int max, final int min, final int def) {
    super(name, description);
    if (max < min) {
      throw new IllegalThreadStateException("Max must be greater than min");
    }
    if (def > max || def < min) {
      throw new IllegalThreadStateException("Default value out of range");
    }
    this.max = max;
    this.min = min;
    value = def;
  }

  @Override
  public Integer getValue() {
    return value;
  }

  @Override
  public void setValue(final Integer value) {
    this.value = value;
  }

  @Override
  public JComponent getEditorComponent() {
    final IntTextField field = new IntTextField(min, max);
    field.setValue(value);
    field.addChangeListener(aField -> value = aField.getValue());
    return field;
  }

  @Override
  public boolean validate(final Object value) {
    if (value instanceof Integer) {
      final int i = (int) value;
      return i <= max && i >= min;
    }
    return false;
  }
}
