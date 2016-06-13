package games.strategy.triplea.settings.validators;

import java.io.File;
import java.util.function.Function;

public interface InputValidator extends Function<String,Boolean> {

  InputValidator NOT_EMPTY = ((string) -> !string.isEmpty());
  InputValidator PATH_EXISTS = ((string) -> (new File(string)).exists());
  InputValidator IS_DIRECTORY = ((string) -> (new File(string)).isDirectory());



}
