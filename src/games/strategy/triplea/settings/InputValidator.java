package games.strategy.triplea.settings;

import java.io.File;
import java.util.function.Function;

public class InputValidator implements Function<String, Boolean> {

  public static final InputValidator IS_DIRECTORY = new InputValidator( ((string) -> (new File(string)).isDirectory()), "directory must exist");

  private static final InputValidator IS_INTEGER = new InputValidator( ((string) -> {
    try {
      Integer.parseInt(string);
      return true;
    } catch (Exception e) {
      return false;
    }
  }), "not a number");

  /**
   * Verifies a value is an integer and falls inside of a given range (inclusive)
   */
  static InputValidator inRange(int min, int max) {
    return new InputValidator((value) -> {
      if(!IS_INTEGER.apply(value)) {
        return false;
      }
      int intValue = Integer.parseInt(value);
      return intValue  >= min && intValue <= max ;
    }, "not in range: " + min + " - " + max);
  }

  String getErrorMessage() {
    return errorMessage;
  }

  private final String errorMessage;
  private Function<String,Boolean> validator;

  private InputValidator(Function<String, Boolean> validationFunction, String errorMessage) {
    this.errorMessage = errorMessage;
    this.validator = validationFunction;
  }

  @Override
  public Boolean apply(String input) {
    return validator.apply(input);
  }
}
