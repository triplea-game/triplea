package games.strategy.engine.data.properties;

import static com.google.common.base.Preconditions.checkArgument;

import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.Getter;

/** Implementation of {@link IEditableProperty} for a double-precision floating-point value. */
public class DoubleProperty extends AbstractEditableProperty<Double> {
  private static final long serialVersionUID = 5521967819500867581L;

  @Getter private final double max;
  @Getter private final double min;
  private double value;
  private final int places;

  /**
   * Initializes a new instance of the {@link DoubleProperty} class.
   *
   * @throws IllegalArgumentException If {@code max} is less than {@code min}; if {@code def} is
   *     less than {@code min}; or if {@code def} is greater than {@code max}.
   */
  public DoubleProperty(
      final String name,
      final String description,
      final double max,
      final double min,
      final double def,
      final int numberOfPlaces) {
    super(name, description);

    checkArgument(max >= min, "Max must be greater than min");
    checkArgument((def >= min) && (def <= max), "Default value out of range");

    this.max = max;
    this.min = min;
    places = numberOfPlaces;
    value = roundToPlace(def, numberOfPlaces);
  }

  private static double roundToPlace(final double number, final int places) {
    return BigDecimal.valueOf(number).setScale(places, RoundingMode.FLOOR).doubleValue();
  }

  @Override
  public Double getValue() {
    return value;
  }

  @Override
  public void setValue(final Double value) {
    this.value = roundToPlace(value, places);
  }

  @Override
  public boolean validate(final Object value) {
    if (value instanceof Double doubleValue) {
      final double d = roundToPlace(doubleValue, places);
      return d <= max && d >= min;
    }
    return false;
  }
}
