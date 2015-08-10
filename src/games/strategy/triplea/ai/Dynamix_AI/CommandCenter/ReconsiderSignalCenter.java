package games.strategy.triplea.ai.Dynamix_AI.CommandCenter;

import java.util.HashMap;
import java.util.HashSet;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;


public class ReconsiderSignalCenter {
  private static HashMap<PlayerID, ReconsiderSignalCenter> s_RSCInstances = new HashMap<PlayerID, ReconsiderSignalCenter>();

  public static ReconsiderSignalCenter get(final GameData data, final PlayerID player) {
    if (!s_RSCInstances.containsKey(player)) {
      s_RSCInstances.put(player, create(data, player));
    }
    return s_RSCInstances.get(player);
  }

  private static ReconsiderSignalCenter create(final GameData data, final PlayerID player) {
    return new ReconsiderSignalCenter(data, player);
  }

  public static void ClearStaticInstances() {
    s_RSCInstances.clear();
  }

  public static void NotifyStartOfRound() {
    s_RSCInstances.clear();
  }

  @SuppressWarnings("unused")
  private GameData m_data = null;
  @SuppressWarnings("unused")
  private PlayerID m_player = null;

  public ReconsiderSignalCenter(final GameData data, final PlayerID player) {
    m_data = data;
    m_player = player;
  }

  public HashSet<Object> ObjectsToReconsider = new HashSet<Object>();
}
