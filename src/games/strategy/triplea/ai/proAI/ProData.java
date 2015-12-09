package games.strategy.triplea.ai.proAI;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ai.proAI.data.ProPurchaseOptionMap;
import games.strategy.triplea.ai.proAI.util.ProPurchaseUtils;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.List;

/**
 * Pro AI data.
 */
public class ProData {

  private static ProAI proAI;
  private static GameData data;
  private static PlayerID player;

  // Default values
  public static double winPercentage = 95;
  public static double minWinPercentage = 75;
  public static boolean areNeutralsPassableByAir = false;
  public static Territory myCapital = null;
  public static List<Territory> myUnitTerritories = new ArrayList<Territory>();
  public static ProPurchaseOptionMap purchaseOptions = null;
  public static double minCostPerHitPoint = Double.MAX_VALUE;

  public static void initialize(final ProAI proAI, final GameData data, final PlayerID player) {
    ProData.proAI = proAI;
    ProData.data = data;
    ProData.player = player;

    if (!games.strategy.triplea.Properties.getLow_Luck(data)) {
      winPercentage = 90;
      minWinPercentage = 65;
    }
    areNeutralsPassableByAir = (Properties.getNeutralFlyoverAllowed(data) && !Properties.getNeutralsImpassable(data));
    myCapital = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
    myUnitTerritories = Match.getMatches(data.getMap().getTerritories(), Matches.territoryHasUnitsOwnedBy(player));
    purchaseOptions = new ProPurchaseOptionMap(player, data);
    minCostPerHitPoint = ProPurchaseUtils.getMinCostPerHitPoint(player, purchaseOptions.getLandOptions());
  }

  public static ProAI getProAI() {
    return proAI;
  }

  public static GameData getData() {
    return data;
  }

  public static PlayerID getPlayer() {
    return player;
  }

}
