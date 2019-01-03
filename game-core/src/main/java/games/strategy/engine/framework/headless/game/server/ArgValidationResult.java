package games.strategy.engine.framework.headless.game.server;

import java.util.List;

import lombok.Singular;
import lombok.Value;

/**
 * Class that tracks validation of CLI params, the error message list when non empty means there are
 * errors, the error list is human-friendly-readable.
 */
@Value
public class ArgValidationResult {
  @Singular private final List<String> errorMessages;

  public boolean isValid() {
    return errorMessages.isEmpty();
  }
}
