package games.strategy.triplea.settings;

import java.io.File;
import java.util.function.Function;

public class InputValidator implements Function<String, Boolean> {

  public static final InputValidator IS_DIRECTORY =
      new InputValidator(((string) -> (new File(string)).isDirectory()), "directory must exist");

  private static final InputValidator IS_INTEGER = new InputValidator(((string) -> {
    try {
      Integer.parseInt(string);
      return true;
    } catch (final Exception e) {
      return false;
    }
  }), "not a number");

  /**
   * Verifies a value is an integer and falls inside of a given range (inclusive)
   */
  static InputValidator inRange(final int min, final int max) {
    return new InputValidator((value) -> {
      if (!IS_INTEGER.apply(value)) {
        return false;
      }
      final int intValue = Integer.parseInt(value);
      return intValue >= min && intValue <= max;
    }, "not in range: " + min + " - " + max);
  }

  String getErrorMessage() {
    return errorMessage;
  }

  private final String errorMessage;
  private final Function<String, Boolean> validator;

  private InputValidator(final Function<String, Boolean> validationFunction, final String errorMessage) {
    this.errorMessage = errorMessage;
    this.validator = validationFunction;
  }

  @Override
  public Boolean apply(final String input) {
    return validator.apply(input);
  }
}
