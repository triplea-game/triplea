package games.strategy.triplea.delegate.reinforcement;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** One map-defined fixed reinforcement delivery. */
public record FixedReinforcementRule(
    int round, String territoryName, String unitTypeName, int quantity) implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  public FixedReinforcementRule {
    if (round < 1) {
      throw new IllegalArgumentException("reinforcement round must be positive");
    }
    territoryName = requireName(territoryName, "territoryName");
    unitTypeName = requireName(unitTypeName, "unitTypeName");
    if (quantity < 1) {
      throw new IllegalArgumentException("reinforcement quantity must be positive");
    }
  }

  FixedReinforcementOrder toOrder() {
    return new FixedReinforcementOrder(round, territoryName, unitTypeName, quantity);
  }

  private static String requireName(final String value, final String field) {
    final String normalized = Objects.requireNonNull(value, field).trim();
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return normalized;
  }
}
