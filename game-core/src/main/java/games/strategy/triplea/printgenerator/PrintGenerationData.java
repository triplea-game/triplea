package games.strategy.triplea.printgenerator;

import java.io.File;

import games.strategy.engine.data.GameData;

class PrintGenerationData {
  private File outDir;
  private GameData gameData;

  /**
   * General Constructor.
   */
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
