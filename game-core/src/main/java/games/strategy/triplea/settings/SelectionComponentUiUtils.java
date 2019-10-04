package games.strategy.triplea.settings;

import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.file.Path;
import java.util.Optional;

/**
 * A collection of methods useful for implementing the UI component of a {@link SelectionComponent}.
 */
public final class SelectionComponentUiUtils {
  private SelectionComponentUiUtils() {}

  /** Converts {@code path} into a string suitable for display in the UI. */
  public static String toString(final Optional<Path> path) {
    checkNotNull(path);

    return path.map(it -> it.toAbsolutePath().toString()).orElse("");
  }
}
