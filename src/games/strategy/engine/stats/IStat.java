package games.strategy.engine.stats;

import java.text.NumberFormat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;

public interface IStat {
  public String getName();

  public double getValue(PlayerID player, GameData data);

  public double getValue(String alliance, GameData data);

  public NumberFormat getFormatter();
}
