package games.strategy.triplea.util;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;

public class UnitOwner {
  private final UnitType m_type;
  private final PlayerID m_owner;

  public UnitOwner(final Unit unit) {
    m_type = unit.getType();
    m_owner = unit.getOwner();
  }

  public UnitOwner(final UnitType type, final PlayerID owner) {
    m_type = type;
    m_owner = owner;
  }

  @Override
  public boolean equals(final Object o) {
    if (o == null) {
      return false;
    }
    if (!(o instanceof UnitOwner)) {
      return false;
    }
    final UnitOwner other = (UnitOwner) o;
    return other.m_type.equals(this.m_type) && other.m_owner.equals(this.m_owner);
  }

  @Override
  public int hashCode() {
    return m_type.hashCode() ^ m_owner.hashCode();
  }

  @Override
  public String toString() {
    return "Unit owner:" + m_owner.getName() + " type:" + m_type.getName();
  }

  public UnitType getType() {
    return m_type;
  }

  public PlayerID getOwner() {
    return m_owner;
  }
}
