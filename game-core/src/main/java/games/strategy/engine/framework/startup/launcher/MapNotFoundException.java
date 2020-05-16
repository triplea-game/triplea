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

  public MapNotFoundException(final String mapName, final List<File> candidatePaths) {
    super(
        "Could not find map: "
            + mapName
            + "\nTypically this will be because the map is not downloaded."
            + "\n\nIf you are *sure* you have the map, double check that the"
            + "\ngame XML can be found under one of the locations listed below"
            + "\nand the 'mapName' XML attribute in the XML file has the correct name."
            + "\nSearched these locations:\n"
            + candidatePaths.stream().map(File::getAbsolutePath).collect(Collectors.joining("\n")));
  }
}
