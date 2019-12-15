package games.strategy.triplea.delegate.data;

import games.strategy.engine.data.Unit;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A response to a must move query. Returns a mapping of unit -> collection of units. Units that
 * must move are land units in transports, and friendly aircraft that must move with carriers.
 */
public class MustMoveWithDetails implements Serializable {
  private static final long serialVersionUID = 936060269327534445L;

  private final Map<Unit, Collection<Unit>> mapping;

  /**
   * Creates new MustMoveWithDetails.
   *
   * @param mapping a mapping of unit (that must move) -> collection of units
   */
  public MustMoveWithDetails(final Map<Unit, Collection<Unit>> mapping) {
    this.mapping = mapping;
  }

  public Map<Unit, Collection<Unit>> getMustMoveWith() {
    return mapping;
  }

  public Collection<Unit> getMustMoveWithForUnit(final Unit unit) {
    return mapping.getOrDefault(unit, List.of());
  }
}
