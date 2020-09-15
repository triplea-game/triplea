package tools.image;

import java.io.File;
import javax.annotation.Nullable;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;
import tools.util.ToolArguments;

@UtilityClass
@Log
public class MapFolderLocationSystemProperty {

  @Nullable
  public File read() {
    final String value = System.getProperty(ToolArguments.MAP_FOLDER);
    if (value != null && value.length() > 0) {
      final File mapFolder = new File(value);
      if (mapFolder.exists()) {
        return mapFolder;
      } else {
        log.info("Could not find directory: " + value);
      }
    }
    return null;
  }
}
