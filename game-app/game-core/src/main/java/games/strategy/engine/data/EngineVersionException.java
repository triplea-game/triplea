package games.strategy.engine.data;

import java.io.File;
import org.triplea.injection.Injections;

/** A checked exception that indicates a game engine is not compatible with a map. */
public final class EngineVersionException extends Exception {
  private static final long serialVersionUID = 8800415601463715772L;

  public EngineVersionException(final String error) {
    super(error);
  }

  public EngineVersionException(final String minimumVersionFound, final File xmlFileBeingParsed) {
    super(
        String.format(
            "Current engine version: %s, is not compatible with version: %s, "
                + "required by game-XML: %s",
            Injections.getInstance().getEngineVersion(),
            minimumVersionFound,
            xmlFileBeingParsed.getAbsolutePath()));
  }
}
