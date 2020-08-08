package games.strategy.engine.stats;

import com.google.common.collect.Table;
import games.strategy.engine.history.Round;
import java.util.HashMap;
import java.util.Map;
import lombok.Value;

/**
 * Data class class that holds statistics information like "PUs over time per player".
 *
 * <p>Instances are typically created by {@link StatisticsAggregator}.
 */
@Value
public class Statistics {
  private final Map<OverTimeStatisticType, Table<String, Round, Double>> overTimeStatistics =
      new HashMap<>();
}
