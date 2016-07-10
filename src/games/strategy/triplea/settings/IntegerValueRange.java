package games.strategy.triplea.settings;

public class IntegerValueRange {

  public final int lowerValue;
  public final int upperValue;
  public final int defaultValue;

  public IntegerValueRange(int lowerValue, int upperValue, int defaultValue) {
    this.lowerValue = lowerValue;
    this.upperValue = upperValue;
    this.defaultValue = defaultValue;
  }

}
