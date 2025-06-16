package games.strategy.engine.data.properties;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serial;
import javax.swing.JComponent;
import lombok.Getter;
import org.triplea.swing.IntTextField;

/** Implementation of {@link IEditableProperty} for an integer value. */
public class NumberProperty extends AbstractEditableProperty<Integer> {
  @Serial private static final long serialVersionUID = 6826763550643504789L;

  @Getter private final int max;
  @Getter private final int min;
  private int value;

  /**
   * Initializes a new instance of the {@link NumberProperty} class.
   *
   * @throws IllegalArgumentException If {@code max} is less than {@code min}; if {@code def} is
   *     less than {@code min}; or if {@code def} is greater than {@code max}.
   */
  public NumberProperty(
      final String name, final String description, final int max, final int min, final int def) {
    super(name, description);

    checkArgument(max >= min, "Max %s must be greater than min %s", max, min);
    checkArgument(
        (def >= min) && (def <= max), "Default %s value out of range, %s - %s", def, min, max);

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
    final IntTextField intTextField = new IntTextField(min, max);
    intTextField.setValue(value);
    intTextField.addChangeListener(field -> value = field.getValue());
    return intTextField;
  }

  @Override
  public boolean validate(final Object value) {
    if (value instanceof Integer) {
      final int i = (int) value;
      return i <= max && i >= min;
    }
    return false;
  }

  /**
   * Copy method.
   *
   * @param newName New name of the cloned instance
   * @return Cloned instance with a new name
   */
  public NumberProperty cloneAs(final String newName) {
    return new NumberProperty(
        newName, this.getDescription(), this.getMax(), this.getMin(), this.getValue());
  }
}
