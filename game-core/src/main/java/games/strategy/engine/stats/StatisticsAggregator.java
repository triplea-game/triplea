package games.strategy.engine.stats;

import games.strategy.engine.data.GameData;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;

/**
 * Analyzes a game's history and aggregates interesting statistics in a {@link Statistics} object.
 */
@Log
@UtilityClass
public class StatisticsAggregator {
  public static Statistics aggregate(@NonNull final GameData game) {
    log.info("Aggregating statistics for game " + game.getGameName());
    return new Statistics();
  }
}
