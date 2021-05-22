package games.strategy.triplea.printgenerator;

import games.strategy.engine.data.GameData;
import java.nio.file.Path;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
class PrintGenerationData {
  private final Path outDir;
  private final GameData data;
}
