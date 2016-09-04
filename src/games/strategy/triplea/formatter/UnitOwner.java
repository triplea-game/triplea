package games.strategy.triplea.formatter;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.UnitType;

class UnitOwner {
  public UnitType type;
  public PlayerID owner;

  UnitOwner(final UnitType type, final PlayerID id) {
    this.type = type;
    this.owner = id;
  }

  @Override
  public boolean equals(final Object o) {
    if (o == null) {
      return false;
    }
    final UnitOwner other = (UnitOwner) o;
    return other.type.equals(this.type) && other.owner.equals(this.owner);
  }

  @Override
  public int hashCode() {
    return type.hashCode() ^ owner.hashCode();
  }
}
