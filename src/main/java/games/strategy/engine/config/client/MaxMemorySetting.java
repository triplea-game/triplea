package games.strategy.engine.config.client;

import com.google.common.base.Preconditions;

/**
 * Data object that captures the values we parsed from the client engine configuration.
 *
 * <p>
 * If there was no value set, then 'isSet' will be false, otherwise it is true and
 * the user set value will be set.
 * </p>
 *
 * <p>
 * If a value is set, it must be valid, or we will fail relatively early with an error
 * message informing the user. If there is an override, the user would want it to
 * take effect, so the behavior is to fail early in that case if the parsed value is invalid.
 * </p>
 */
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
