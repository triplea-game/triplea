package games.strategy.engine.stats;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import java.text.NumberFormat;

/**
 * A game statistic, such as total resources, total unit value, etc. Statistics can be obtained per
 * player or for all players in an alliance.
 */
public interface IStat {
  String getName();

  double getValue(GamePlayer player, GameData data);

  double getValue(String alliance, GameData data);

  NumberFormat getFormatter();
}
