package games.strategy.triplea.ai.pro;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ai.pro.data.ProPurchaseOption;
import games.strategy.triplea.ai.pro.data.ProPurchaseOptionMap;
import games.strategy.triplea.ai.pro.util.ProUtils;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.util.TuvUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;

/** Pro AI data. */
public final class ProData {
  // Default values
  public static boolean isSimulation = false;
  public static double winPercentage = 95;
  public static double minWinPercentage = 75;
  public static Territory myCapital = null;
  public static List<Territory> myUnitTerritories = new ArrayList<>();
  public static Map<Unit, Territory> unitTerritoryMap = new HashMap<>();
  public static IntegerMap<UnitType> unitValueMap = new IntegerMap<>();
  public static ProPurchaseOptionMap purchaseOptions = null;
  public static double minCostPerHitPoint = Double.MAX_VALUE;

  private static ProAi proAi;
  private static GameData data;
  private static GamePlayer player;

  private ProData() {}

  public static void initialize(final ProAi proAi) {
    hiddenInitialize(proAi, proAi.getGameData(), proAi.getGamePlayer(), false);
  }

  public static void initializeSimulation(
      final ProAi proAi, final GameData data, final GamePlayer player) {
    hiddenInitialize(proAi, data, player, true);
  }

  private static void hiddenInitialize(
      final ProAi proAi, final GameData data, final GamePlayer player, final boolean isSimulation) {
    ProData.proAi = proAi;
    ProData.data = data;
    ProData.player = player;
    ProData.isSimulation = isSimulation;

    if (!Properties.getLowLuck(data)) {
      winPercentage = 90;
      minWinPercentage = 65;
    }
    myCapital = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
    myUnitTerritories =
        CollectionUtils.getMatches(
            data.getMap().getTerritories(), Matches.territoryHasUnitsOwnedBy(player));
    unitTerritoryMap = ProUtils.newUnitTerritoryMap();
    unitValueMap = TuvUtils.getCostsForTuv(player, data);
    purchaseOptions = new ProPurchaseOptionMap(player, data);
    minCostPerHitPoint = getMinCostPerHitPoint(purchaseOptions.getLandOptions());
  }

  public static ProAi getProAi() {
    return proAi;
  }

  public static GameData getData() {
    return data;
  }

  public static GamePlayer getPlayer() {
    return player;
  }

  private static double getMinCostPerHitPoint(final List<ProPurchaseOption> landPurchaseOptions) {
    double minCostPerHitPoint = Double.MAX_VALUE;
    for (final ProPurchaseOption ppo : landPurchaseOptions) {
      if (ppo.getCostPerHitPoint() < minCostPerHitPoint) {
        minCostPerHitPoint = ppo.getCostPerHitPoint();
      }
    }
    return minCostPerHitPoint;
  }
}
