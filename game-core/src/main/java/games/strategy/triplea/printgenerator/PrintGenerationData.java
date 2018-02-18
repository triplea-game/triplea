package games.strategy.triplea.printgenerator;

import java.io.File;

import games.strategy.engine.data.GameData;

public class PrintGenerationData {
  private File outDir;
  private GameData gameData;

  /**
   * General Constructor.
   */
  PrintGenerationData() {}

  /**
   * @return The outDir.
   */
  File getOutDir() {
    return outDir;
  }

  /**
   * @param outDir
   *        the outDir to set.
   */
  void setOutDir(final File outDir) {
    this.outDir = outDir;
  }

  /**
   * @return The data.
   */
  protected GameData getData() {
    return gameData;
  }

  /**
   * @param data
   *        the data to set.
   */
  protected void setData(final GameData data) {
    gameData = data;
  }
}
