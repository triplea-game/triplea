package tools.image;

import java.nio.file.Path;
import javax.annotation.Nullable;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import tools.util.ToolArguments;

@UtilityClass
@Slf4j
public class MapFolderLocationSystemProperty {

  public @Nullable Path read() {
    return ToolArguments.getPropertyMapFolderPath()
        .orElse(null); // TODO: Should not return null, but Optional if needed
  }
}
