package games.strategy.triplea.ai.proAI;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.triplea.Properties;

/**
 * Pro AI data.
 */
public class ProData {

  public static double winPercentage = 95;
  public static double minWinPercentage = 75;
  public static boolean areNeutralsPassableByAir = false;

  private static GameData data;
  private static ProAI proAI;

  public static void setData(final GameData data) {
    ProData.data = data;

    // Set optimal and win percentages lower if not LL
    if (!games.strategy.triplea.Properties.getLow_Luck(data)) {
      winPercentage = 90;
      minWinPercentage = 65;
    }

    // Set map properties
    areNeutralsPassableByAir = (Properties.getNeutralFlyoverAllowed(data) && !Properties.getNeutralsImpassable(data));
  }

  public static void setProAI(final ProAI proAI) {
    ProData.proAI = proAI;
  }

  public static GameData getData() {
    return data;
  }

  public static ProAI getProAI() {
    return proAI;
  }

  public static PlayerID getPlayer() {
    return proAI.getPlayerID();
  }

}
