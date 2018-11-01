package games.strategy.engine.data.properties;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import games.strategy.engine.ClientFileSystemHelper;

/**
 * Implementation of {@link IEditableProperty} for a double-precision floating-point value.
 */
public class DoubleProperty extends AEditableProperty {
  private static final long serialVersionUID = 5521967819500867581L;

  private final double max;
  private final double min;
  private double value;
  private final int places;

  public DoubleProperty(final String name, final String description, final double max, final double min,
      final double def, final int numberOfPlaces) {
    super(name, description);
    if (max < min) {
      throw new IllegalThreadStateException("Max must be greater than min");
    }
    if (def > max || def < min) {
      throw new IllegalThreadStateException("Default value out of range");
    }
    this.max = max;
    this.min = min;
    places = numberOfPlaces;
    value = roundToPlace(def, numberOfPlaces, RoundingMode.FLOOR);
  }

  private static double roundToPlace(final double number, final int places, final RoundingMode roundingMode) {
    BigDecimal bd = new BigDecimal(number);
    bd = bd.setScale(places, roundingMode);
    return bd.doubleValue();
  }

  @Override
  public Double getValue() {
    return value;
  }

  @Override
  public void setValue(final Object value) throws ClassCastException {
    if (value instanceof String) {
      // warn developer which have run with the option cache when Number properties were stored as strings
      // todo (kg) remove at a later point
      throw new RuntimeException("Double and Number properties are no longer stored as Strings. "
          + "You should delete your option cache, located at "
          + new File(ClientFileSystemHelper.getUserRootFolder(), "optionCache").toString());
    }
    this.value = roundToPlace((Double) value, places, RoundingMode.FLOOR);
  }

  @Override
  public JComponent getEditorComponent() {
    final JSpinner field = new JSpinner(new SpinnerNumberModel(value, min, max, 1.0));

    // NB: Workaround for JSpinner default sizing algorithm when min/max values have very large magnitudes
    // (see: https://implementsblog.com/2012/11/26/java-gotcha-jspinner-preferred-size/)
    final JComponent fieldEditor = field.getEditor();
    if (fieldEditor instanceof JSpinner.DefaultEditor) {
      ((JSpinner.DefaultEditor) fieldEditor).getTextField().setColumns(10);
    }

    field.addChangeListener(e -> value = (double) field.getValue());
    return field;
  }

  @Override
  public boolean validate(final Object value) {
    if (value instanceof Double) {
      final double d;
      try {
        d = roundToPlace((Double) value, places, RoundingMode.FLOOR);
      } catch (final Exception e) {
        return false;
      }
      return d <= max && d >= min;
    }
    return false;
  }
}
