package games.strategy.triplea.delegate.dataObjects;

import java.util.Collection;
import java.util.Map;

import games.strategy.engine.data.Unit;

/**
 * A response to a must move query.
 * Returns a mapping of unit -> collection of units.
 * Units that must move are land units in transports,
 * and friendly aircraft that must move with carriers.
 */
public class MustMoveWithDetails implements java.io.Serializable {
  private static final long serialVersionUID = 936060269327534445L;
  /**
   * Maps Unit -> Collection of units.
   */
  private final Map<Unit, Collection<Unit>> m_mapping;

  /**
   * Creates new MustMoveWithDetails.
   *
   * @param mapping
   *        a mapping of unit (that must move) -> collection of units
   */
  public MustMoveWithDetails(final Map<Unit, Collection<Unit>> mapping) {
    m_mapping = mapping;
  }

  public Map<Unit, Collection<Unit>> getMustMoveWith() {
    return m_mapping;
  }
}
