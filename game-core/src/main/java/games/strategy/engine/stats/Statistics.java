package games.strategy.engine.stats;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import games.strategy.engine.history.Round;
import lombok.Value;

/**
 * Data class class that holds statistics information like "PUs over time per player".
 *
 * <p>Instances are typically created by {@link StatisticsAggregator}.
 */
@Value
public class Statistics {
  private final Table<String, Round, Double> productionOfPlayerInRound = HashBasedTable.create();
}
