package games.strategy.triplea.printgenerator;

import games.strategy.engine.data.GameData;
import java.io.File;

class PrintGenerationData {
  private File outDir;
  private GameData gameData;

  PrintGenerationData() {}

  File getOutDir() {
    return outDir;
  }

  void setOutDir(final File outDir) {
    this.outDir = outDir;
  }

  protected GameData getData() {
    return gameData;
  }

  protected void setData(final GameData data) {
    gameData = data;
  }
}
