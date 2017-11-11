package games.strategy.triplea.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;

/**
 * The combination of a unit type and its owner.
 */
public final class UnitOwner {
  private final UnitType type;
  private final PlayerID owner;

  public UnitOwner(final Unit unit) {
    checkNotNull(unit);

    type = unit.getType();
    owner = unit.getOwner();
  }

  @Override
  public boolean equals(final Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof UnitOwner)) {
      return false;
    }
    final UnitOwner other = (UnitOwner) o;
    return Objects.equals(type, other.type) && Objects.equals(owner, other.owner);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, owner);
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
