package games.strategy.engine.stats;

import java.text.NumberFormat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;

/**
 * A game statistic, such as total resources, total unit value, etc. Statistics can be obtained per
 * player or for all players in an alliance.
 */
public interface IStat {
  String getName();

  double getValue(PlayerId player, GameData data);

  double getValue(String alliance, GameData data);

  NumberFormat getFormatter();
}
