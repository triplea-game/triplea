package games.strategy.engine.stats;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.triplea.ui.mapdata.MapData;
import java.text.DecimalFormat;
import org.jetbrains.annotations.Nls;

/**
 * A game statistic, such as total resources, total unit value, etc. Statistics can be obtained per
 * player or for all players in an alliance.
 */
public interface IStat {
  DecimalFormat DECIMAL_FORMAT = new DecimalFormat("##0.##");

  @Nls
  String getName();

  double getValue(GamePlayer player, GameData data, MapData mapData);

  default double getValue(final String alliance, final GameData data, MapData mapData) {
    return data.getAllianceTracker().getPlayersInAlliance(alliance).stream()
        .mapToDouble(player -> getValue(player, data, mapData))
        .sum();
  }
}
