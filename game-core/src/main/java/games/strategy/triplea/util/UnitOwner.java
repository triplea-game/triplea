package games.strategy.triplea.util;

import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/** The combination of a unit type and its owner. */
@EqualsAndHashCode
@Getter
public final class UnitOwner {
  private final UnitType type;
  private final PlayerId owner;

  public UnitOwner(final Unit unit) {
    checkNotNull(unit);

    type = unit.getType();
    owner = unit.getOwner();
  }

  @Override
  public String toString() {
    return "Unit owner:" + owner.getName() + " type:" + type.getName();
  }
}
