package games.strategy.triplea.printgenerator;

import java.io.File;
import java.util.Map;

import games.strategy.engine.data.GameData;

public class PrintGenerationData {
  private File m_outDir;
  private Map<Integer, Integer> m_SamePUMap;
  private GameData m_data;

  /**
   * General Constructor
   */
  protected PrintGenerationData() {}

  /**
   * @return the outDir
   */
  protected File getOutDir() {
    return m_outDir;
  }

  /**
   * @param outDir
   *        the outDir to set
   */
  protected void setOutDir(final File outDir) {
    m_outDir = outDir;
  }

  /**
   * @return the samePUMap
   */
  protected Map<Integer, Integer> getSamePUMap() {
    return m_SamePUMap;
  }

  /**
   * @param samePUMap
   *        the samePUMap to set
   */
  protected void setSamePUMap(final Map<Integer, Integer> samePUMap) {
    m_SamePUMap = samePUMap;
  }

  /**
   * @return the data
   */
  protected GameData getData() {
    return m_data;
  }

  /**
   * @param data
   *        the data to set
   */
  protected void setData(final GameData data) {
    m_data = data;
  }
}
