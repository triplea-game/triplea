package games.strategy.triplea.delegate.reinforcement;

import java.util.List;

/** Strategic-agent view of reinforcement progress and future deliveries. */
public record FixedReinforcementObservation(
    int schemaVersion,
    String player,
    int currentRound,
    int lastProcessedRound,
    List<Entry> pending,
    List<Entry> scheduled) {
  public static final int SCHEMA_VERSION = 1;

  public FixedReinforcementObservation {
    pending = List.copyOf(pending);
    scheduled = List.copyOf(scheduled);
  }

  public record Entry(int round, String territory, String unitType, int quantity) {
    static Entry from(final FixedReinforcementOrder order) {
      return new Entry(
          order.scheduledRound(), order.territoryName(), order.unitTypeName(), order.quantity());
    }

    static Entry from(final FixedReinforcementRule rule) {
      return new Entry(rule.round(), rule.territoryName(), rule.unitTypeName(), rule.quantity());
    }
  }
}
