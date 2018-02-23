package games.strategy.engine.data.properties;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import games.strategy.engine.ClientFileSystemHelper;

public class DoubleProperty extends AEditableProperty {
  private static final long serialVersionUID = 5521967819500867581L;
  private final double m_max;
  private final double m_min;
  private double m_value;
  private final int m_places;

  public DoubleProperty(final String name, final String description, final double max, final double min,
      final double def, final int numberOfPlaces) {
    super(name, description);
    if (max < min) {
      throw new IllegalThreadStateException("Max must be greater than min");
    }
    if ((def > max) || (def < min)) {
      throw new IllegalThreadStateException("Default value out of range");
    }
    m_max = max;
    m_min = min;
    m_places = numberOfPlaces;
    m_value = roundToPlace(def, numberOfPlaces, RoundingMode.FLOOR);
  }

  private static double roundToPlace(final double number, final int places, final RoundingMode roundingMode) {
    BigDecimal bd = new BigDecimal(number);
    bd = bd.setScale(places, roundingMode);
    return bd.doubleValue();
  }

  @Override
  public Double getValue() {
    return m_value;
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
    m_value = roundToPlace((Double) value, m_places, RoundingMode.FLOOR);
  }

  @Override
  public JComponent getEditorComponent() {
    final JSpinner field = new JSpinner(new SpinnerNumberModel(m_value, m_min, m_max, 1.0));

    // NB: Workaround for JSpinner default sizing algorithm when min/max values have very large magnitudes
    // (see: https://implementsblog.com/2012/11/26/java-gotcha-jspinner-preferred-size/)
    final JComponent fieldEditor = field.getEditor();
    if (fieldEditor instanceof JSpinner.DefaultEditor) {
      ((JSpinner.DefaultEditor) fieldEditor).getTextField().setColumns(10);
    }

    field.addChangeListener(e -> m_value = (double) field.getValue());
    return field;
  }

  @Override
  public boolean validate(final Object value) {
    if (value instanceof Double) {
      final double d;
      try {
        d = roundToPlace((Double) value, m_places, RoundingMode.FLOOR);
      } catch (final Exception e) {
        return false;
      }
      return (d <= m_max) && (d >= m_min);
    }
    return false;
  }
}
