package org.triplea.ai.benchmark.runner;

import games.strategy.engine.framework.startup.ui.PlayerTypes;
import games.strategy.engine.player.Player;
import games.strategy.triplea.ai.pro.ProAi;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The AIs a kata or the command line can name. Adding one is one more entry here. */
public final class AiRegistry {
  private static final Map<String, PlayerTypes.Type> TYPES = new LinkedHashMap<>();

  static {
    TYPES.put("Hard", PlayerTypes.PRO_AI);
    TYPES.put(
        "Hard Beta",
        new PlayerTypes.Type("Hard Beta (AI)") {
          @Override
          public Player newPlayerWithName(final String name) {
            return new ProAi(name, getLabel(), true);
          }
        });
    TYPES.put("Fast", PlayerTypes.FAST_AI);
    TYPES.put("Easy", PlayerTypes.WEAK_AI);
  }

  private AiRegistry() {}

  public static List<String> names() {
    return List.copyOf(TYPES.keySet());
  }

  public static PlayerTypes.Type type(final String name) {
    final PlayerTypes.Type type = TYPES.get(name);
    if (type == null) {
      throw new IllegalArgumentException("Unknown AI '" + name + "'; known: " + names());
    }
    return type;
  }
}
