package games.strategy.engine.framework.startup.launcher;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An unchecked exception indicating the map associated with a save game was not found and must be
 * installed before game play can begin.
 */
public class MapNotFoundException extends IllegalStateException {
  private static final long serialVersionUID = -1027460394367073991L;

  public MapNotFoundException() {}

  public MapNotFoundException(final String mapName, final List<File> candidatePaths) {
    super(
        "Could not find map: "
            + mapName
            + "\nNot found, searched these locations:\n"
            + candidatePaths.stream().map(File::getAbsolutePath).collect(Collectors.joining("\n")));
  }
}
