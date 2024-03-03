package tools.image;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nullable;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import tools.util.ToolArguments;

@UtilityClass
@Slf4j
public class MapFolderLocationSystemProperty {

  public @Nullable Path read() {
    final String value = System.getProperty(ToolArguments.MAP_FOLDER);
    if (value != null && value.length() > 0) {
      final Path mapFolder = Path.of(value);
      if (Files.exists(mapFolder)) {
        return mapFolder;
      } else {
        log.info("Could not find directory: " + value);
      }
    }
    return null;
  }
}
