package games.strategy.triplea.ai.Dynamix_AI.CommandCenter;

import java.util.HashMap;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;

public class KnowledgeCenter {
  private static HashMap<PlayerID, KnowledgeCenter> s_KCInstances = new HashMap<PlayerID, KnowledgeCenter>();

  public static KnowledgeCenter get(final GameData data, final PlayerID player) {
    if (!s_KCInstances.containsKey(player)) {
      s_KCInstances.put(player, create(data, player));
    }
    return s_KCInstances.get(player);
  }

  private static KnowledgeCenter create(final GameData data, final PlayerID player) {
    return new KnowledgeCenter(data, player);
  }

  public static void ClearStaticInstances() {
    s_KCInstances.clear();
  }

  public static void NotifyStartOfRound() {
    s_KCInstances.clear();
  }

  @SuppressWarnings("unused")
  private GameData m_data = null;
  @SuppressWarnings("unused")
  private PlayerID m_player = null;

  public KnowledgeCenter(final GameData data, final PlayerID player) {
    m_data = data;
    m_player = player;
  }
}
