package games.strategy.triplea.delegate.scoring;

import games.strategy.engine.data.Territory;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/** One conditional point award, evaluated against a set of territories. */
public record ScoringBonus(int points, List<Territory> territories) implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  public ScoringBonus {
    territories = List.copyOf(territories);
    if (territories.isEmpty()) {
      throw new IllegalArgumentException("a scoring bonus must list at least one territory");
    }
  }
}
