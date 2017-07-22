package games.strategy.triplea.util;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;

public class UnitOwner {
  private final UnitType type;
  private final PlayerID owner;

  public UnitOwner(final Unit unit) {
    type = unit.getType();
    owner = unit.getOwner();
  }

  public UnitOwner(final UnitType type, final PlayerID owner) {
    this.type = type;
    this.owner = owner;
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
    return other.type.equals(this.type) && other.owner.equals(this.owner);
  }

  @Override
  public int hashCode() {
    return type.hashCode() ^ owner.hashCode();
  }

  @Override
  public String toString() {
    return "Unit owner:" + owner.getName() + " type:" + type.getName();
  }

  public UnitType getType() {
    return type;
  }

  public PlayerID getOwner() {
    return owner;
  }
}
