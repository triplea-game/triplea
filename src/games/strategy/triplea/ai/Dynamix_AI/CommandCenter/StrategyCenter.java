package games.strategy.triplea.ai.Dynamix_AI.CommandCenter;

import java.util.HashMap;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.triplea.ai.Dynamix_AI.DUtils;
import games.strategy.triplea.ai.Dynamix_AI.Others.StrategyType;

public class StrategyCenter {
  private static HashMap<PlayerID, StrategyCenter> s_SCInstances = new HashMap<PlayerID, StrategyCenter>();

  public static StrategyCenter get(final GameData data, final PlayerID player) {
    if (!s_SCInstances.containsKey(player)) {
      s_SCInstances.put(player, create(data, player));
    }
    return s_SCInstances.get(player);
  }

  private static StrategyCenter create(final GameData data, final PlayerID player) {
    return new StrategyCenter(data, player);
  }

  public static void ClearStaticInstances() {
    s_SCInstances.clear();
  }

  public static void NotifyStartOfRound() {
    s_SCInstances.clear();
  }

  private GameData m_data = null;
  private PlayerID m_player = null;

  public StrategyCenter(final GameData data, final PlayerID player) {
    m_data = data;
    m_player = player;
  }

  private HashMap<PlayerID, StrategyType> CalculatedStrategyAssignments = new HashMap<PlayerID, StrategyType>();

  public HashMap<PlayerID, StrategyType> GetCalculatedStrategyAssignments() {
    if (CalculatedStrategyAssignments == null || CalculatedStrategyAssignments.isEmpty()) {
      CalculatedStrategyAssignments = DUtils.CalculateStrategyAssignments(m_data, m_player);
    }
    return CalculatedStrategyAssignments;
  }
}
