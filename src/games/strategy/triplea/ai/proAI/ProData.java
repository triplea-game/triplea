package games.strategy.triplea.ai.proAI;

import games.strategy.engine.data.GameData;

/**
 * Pro AI data.
 */
public class ProData {

  private static GameData data;
  private static ProAI proAI;

  public static GameData getData() {
    return data;
  }

  public static void setData(final GameData data) {
    ProData.data = data;
  }

  public static ProAI getProAI() {
    return proAI;
  }

  public static void setProAI(final ProAI proAI) {
    ProData.proAI = proAI;
  }

}
