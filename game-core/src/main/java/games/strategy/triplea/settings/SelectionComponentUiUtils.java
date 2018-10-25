package games.strategy.triplea.settings;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.util.Optional;

/**
 * A collection of methods useful for implementing the UI component of a {@link SelectionComponent}.
 */
public final class SelectionComponentUiUtils {
  private SelectionComponentUiUtils() {}

  /**
   * Converts {@code file} into a string suitable for display in the UI.
   */
  public static String toString(final Optional<File> file) {
    checkNotNull(file);

    return file.map(File::getAbsolutePath).orElse("");
  }
}
