package games.strategy.engine.stats;

import java.text.NumberFormat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;

public interface IStat {
  String getName();

  double getValue(PlayerID player, GameData data);

  double getValue(String alliance, GameData data);

  NumberFormat getFormatter();
}
