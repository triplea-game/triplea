package games.strategy.triplea.ai.Dynamix_AI.CommandCenter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ai.Dynamix_AI.Group.PurchaseGroup;


public class FactoryCenter {
  private static HashMap<PlayerID, FactoryCenter> s_FCInstances = new HashMap<PlayerID, FactoryCenter>();

  public static FactoryCenter get(final GameData data, final PlayerID player) {
    if (!s_FCInstances.containsKey(player)) {
      s_FCInstances.put(player, create(data, player));
    }
    return s_FCInstances.get(player);
  }

  private static FactoryCenter create(final GameData data, final PlayerID player) {
    return new FactoryCenter(data, player);
  }

  public static void ClearStaticInstances() {
    s_FCInstances.clear();
  }

  public static void NotifyStartOfRound() {
    s_FCInstances.clear();
  }

  @SuppressWarnings("unused")
  private GameData m_data = null;
  @SuppressWarnings("unused")
  private PlayerID m_player = null;

  public FactoryCenter(final GameData data, final PlayerID player) {
    m_data = data;
    m_player = player;
  }

  public List<Territory> ChosenFactoryTerritories = new ArrayList<Territory>();
  public List<Territory> ChosenAAPlaceTerritories = new ArrayList<Territory>();
  public HashMap<Territory, PurchaseGroup> TurnTerritoryPurchaseGroups = new HashMap<Territory, PurchaseGroup>();
  public List<PurchaseGroup> FactoryPurchaseGroups = new ArrayList<PurchaseGroup>();
}
