package games.strategy.triplea.settings;

public final class IntegerValueRange implements ValueRange {

  public final int lowerValue;
  public final int upperValue;
  public final int defaultValue;

  public IntegerValueRange(final int lowerValue, final int upperValue, final int defaultValue) {
    this.lowerValue = lowerValue;
    this.upperValue = upperValue;
    this.defaultValue = defaultValue;
  }

  @Override
  public String getDescription() {
    return String.format("%d - %d\ndefault: %d", lowerValue, upperValue, defaultValue);
  }
}
