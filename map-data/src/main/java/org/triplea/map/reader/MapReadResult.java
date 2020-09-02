package org.triplea.map.reader;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Singular;
import org.triplea.map.data.elements.Game;

/**
 * Value object representing the result of reading a map. Either we will have read all of the map or
 * there will have been validation errors.
 */
@Builder
public class MapReadResult {
  @Nullable private final Game game;
  @Singular @Nullable private final List<String> errorMessages;

  public Optional<Game> getGame() {
    return Optional.ofNullable(game);
  }

  public List<String> getErrorMessages() {
    return errorMessages == null ? List.of() : errorMessages;
  }
}
