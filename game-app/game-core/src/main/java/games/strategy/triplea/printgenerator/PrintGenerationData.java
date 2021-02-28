package games.strategy.triplea.printgenerator;

import games.strategy.engine.data.GameData;
import java.io.File;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
class PrintGenerationData {
  private final File outDir;
  private final GameData data;
}
