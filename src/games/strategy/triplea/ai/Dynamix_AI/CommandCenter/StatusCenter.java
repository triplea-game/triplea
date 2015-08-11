package games.strategy.triplea.ai.Dynamix_AI.CommandCenter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ai.Dynamix_AI.Others.TerritoryStatus;
import games.strategy.util.Match;

public class StatusCenter {
  private static HashMap<PlayerID, StatusCenter> s_SCInstances = new HashMap<PlayerID, StatusCenter>();

  public static StatusCenter get(final GameData data, final PlayerID player) {
    if (!s_SCInstances.containsKey(player)) {
      s_SCInstances.put(player, create(data, player));
    }
    return s_SCInstances.get(player);
  }

  private static StatusCenter create(final GameData data, final PlayerID player) {
    return new StatusCenter(data, player);
  }

  public static void ClearStaticInstances() {
    s_SCInstances.clear();
  }

  public static void NotifyStartOfRound() {
    s_SCInstances.clear();
  }

  @SuppressWarnings("unused")
  private GameData m_data = null;
  @SuppressWarnings("unused")
  private PlayerID m_player = null;

  public StatusCenter(final GameData data, final PlayerID player) {
    m_data = data;
    m_player = player;
  }

  private final HashMap<String, TerritoryStatus> TerritoryStatuses = new HashMap<String, TerritoryStatus>();

  public TerritoryStatus GetStatusOfTerritory(final Territory ter) {
    return GetStatusOfTerritory(ter.getName());
  }

  public TerritoryStatus GetStatusOfTerritory(final String terName) {
    if (!TerritoryStatuses.containsKey(terName)) {
      TerritoryStatuses.put(terName, new TerritoryStatus());
    }
    return TerritoryStatuses.get(terName);
  }

  public List<Territory> GetTerritoriesThatHaveStatusesMatching(final GameData data,
      final Match<TerritoryStatus> match) {
    final List<Territory> result = new ArrayList<Territory>();
    for (final String key : TerritoryStatuses.keySet()) {
      final TerritoryStatus status = TerritoryStatuses.get(key);
      if (status != null && match.match(status)) {
        result.add(data.getMap().getTerritory(key));
      }
    }
    return result;
  }
}
