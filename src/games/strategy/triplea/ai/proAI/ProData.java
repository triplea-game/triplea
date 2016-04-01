package games.strategy.triplea.ai.proAI;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ai.proAI.data.ProPurchaseOption;
import games.strategy.triplea.ai.proAI.data.ProPurchaseOptionMap;
import games.strategy.triplea.ai.proAI.util.ProUtils;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.delegate.BattleCalculator;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pro AI data.
 */
public class ProData {

  private static ProAI proAI;
  private static GameData data;
  private static PlayerID player;

  // Default values
  public static boolean isSimulation = false;
  public static double winPercentage = 95;
  public static double minWinPercentage = 75;
  public static boolean areNeutralsPassableByAir = false;
  public static Territory myCapital = null;
  public static List<Territory> myUnitTerritories = new ArrayList<>();
  public static Map<Unit, Territory> unitTerritoryMap = new HashMap<>();
  public static IntegerMap<UnitType> unitValueMap = new IntegerMap<>();
  public static ProPurchaseOptionMap purchaseOptions = null;
  public static double minCostPerHitPoint = Double.MAX_VALUE;

  public static void initialize(final ProAI proAI) {
    hiddenInitialize(proAI, proAI.getGameData(), proAI.getPlayerID(), false);
  }

  public static void initializeSimulation(final ProAI proAI, final GameData data, final PlayerID player) {
    hiddenInitialize(proAI, data, player, true);
  }

  private static void hiddenInitialize(final ProAI proAI, final GameData data, final PlayerID player,
      final boolean isSimulation) {
    ProData.proAI = proAI;
    ProData.data = data;
    ProData.player = player;
    ProData.isSimulation = isSimulation;

    if (!games.strategy.triplea.Properties.getLow_Luck(data)) {
      winPercentage = 90;
      minWinPercentage = 65;
    }
    areNeutralsPassableByAir = (Properties.getNeutralFlyoverAllowed(data) && !Properties.getNeutralsImpassable(data));
    myCapital = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
    myUnitTerritories = Match.getMatches(data.getMap().getTerritories(), Matches.territoryHasUnitsOwnedBy(player));
    unitTerritoryMap = ProUtils.createUnitTerritoryMap();
    unitValueMap = BattleCalculator.getCostsForTUV(player, data);
    purchaseOptions = new ProPurchaseOptionMap(player, data);
    minCostPerHitPoint = getMinCostPerHitPoint(player, purchaseOptions.getLandOptions());
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

  private static double getMinCostPerHitPoint(final PlayerID player, final List<ProPurchaseOption> landPurchaseOptions) {
    double minCostPerHitPoint = Double.MAX_VALUE;
    for (final ProPurchaseOption ppo : landPurchaseOptions) {
      if (ppo.getCostPerHitPoint() < minCostPerHitPoint) {
        minCostPerHitPoint = ppo.getCostPerHitPoint();
      }
    }
    return minCostPerHitPoint;
  }

}
