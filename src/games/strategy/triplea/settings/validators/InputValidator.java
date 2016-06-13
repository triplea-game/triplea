package games.strategy.triplea.settings.validators;

import java.io.File;
import java.util.function.Function;

public interface InputValidator extends Function<String, Boolean> {

  InputValidator NOT_EMPTY = ((string) -> !string.isEmpty());
  InputValidator PATH_EXISTS = ((string) -> (new File(string)).exists());
  InputValidator IS_DIRECTORY = ((string) -> (new File(string)).isDirectory());
  InputValidator IS_INTEGER = ((string) -> {
    try {
      Integer.parseInt(string);
      return true;
    } catch (Exception e) {
      return false;
    }
  });

  static InputValidator inRange(int min, int max) {
    return (value) -> {
      if(!IS_INTEGER.apply(value)) {
        return false;
      }
      int intValue = Integer.parseInt(value);
      return intValue  >= min && intValue <= intValue ;
    };
  }

}
