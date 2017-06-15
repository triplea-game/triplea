package games.strategy.engine.config;

import com.google.common.base.Preconditions;

public class MaxMemorySetting {

  static final MaxMemorySetting NOT_SET = new MaxMemorySetting();

  public final boolean isSet;
  public final long value;

  private MaxMemorySetting() {
    isSet = false;
    value = -1L;
  }

  private MaxMemorySetting(final long value) {
    isSet = true;
    this.value = value;

    if (value < 1L) {
      throw new IllegalArgumentException("Invalid value for max memory, must be positive, but found: " + value);
    }
  }

  static MaxMemorySetting of(final String value) {
    Preconditions.checkNotNull(value,
        "Value should not be null, if null, we consider the memory setting to not be "
            + "set and should not be not be executing this code path..");
    try {
      // truncate any decimal values here - keep it simple.
      // Accept the user supplied value, but drop the decimal values (extra bytes).
      final long longValue = (long) Double.parseDouble(value);

      return new MaxMemorySetting(longValue);
    } catch (final NumberFormatException e) {
      throw new IllegalArgumentException(
          "Invalid number for max memory, must be a positive number, but found: " + value, e);
    }
  }

}
