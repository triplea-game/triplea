package games.strategy.triplea.util;

import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import java.util.Objects;
import lombok.Getter;

/** The combination of a unit type and its owner. */
@Getter
public final class UnitOwner {
  private final UnitType type;
  private final GamePlayer owner;

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
    return "Unit owner: " + owner.getName() + " type: " + type.getName();
  }
}
