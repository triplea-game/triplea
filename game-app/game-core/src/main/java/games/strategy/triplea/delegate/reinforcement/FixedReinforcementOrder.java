package games.strategy.triplea.delegate.reinforcement;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** A reinforcement delivery that is due now or waiting in the placement queue. */
public record FixedReinforcementOrder(
    int scheduledRound, String territoryName, String unitTypeName, int quantity)
    implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  public FixedReinforcementOrder {
    if (scheduledRound < 1) {
      throw new IllegalArgumentException("scheduledRound must be positive");
    }
    territoryName = requireName(territoryName, "territoryName");
    unitTypeName = requireName(unitTypeName, "unitTypeName");
    if (quantity < 1) {
      throw new IllegalArgumentException("quantity must be positive");
    }
  }

  FixedReinforcementOrder withQuantity(final int remainingQuantity) {
    return new FixedReinforcementOrder(
        scheduledRound, territoryName, unitTypeName, remainingQuantity);
  }

  private static String requireName(final String value, final String field) {
    final String normalized = Objects.requireNonNull(value, field).trim();
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return normalized;
  }
}
