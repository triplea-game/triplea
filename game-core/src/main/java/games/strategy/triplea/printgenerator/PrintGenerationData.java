package games.strategy.triplea.printgenerator;

import games.strategy.engine.data.GameData;
import java.io.File;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class PrintGenerationData {
  private File outDir;
  private GameData data;
}
