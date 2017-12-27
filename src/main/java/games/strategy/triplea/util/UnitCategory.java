package games.strategy.triplea.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.attachments.UnitTypeComparator;
import games.strategy.util.Util;

public class UnitCategory implements Comparable<Object> {
  private final UnitType type;
  // Collection of UnitOwners, the type of our dependents, not the dependents
  private Collection<UnitOwner> dependents;
  // movement of the units
  private final int movement;
  // movement of the units
  private final int transportCost;
  // movement of the units
  // private final Territory m_originatingTerr;
  private final PlayerID owner;
  // the units in the category, may be duplicates.
  private final List<Unit> units = new ArrayList<>();
  private int damaged = 0;
  private int bombingDamage = 0;
  private boolean disabled = false;

  public UnitCategory(final UnitType type, final PlayerID owner) {
    this.type = type;
    dependents = Collections.emptyList();
    movement = -1;
    transportCost = -1;
    this.owner = owner;
  }

  UnitCategory(final Unit unit, final Collection<Unit> dependents, final int movement, final int damaged,
      final int bombingDamage, final boolean disabled, final int transportCost) {
    type = unit.getType();
    this.movement = movement;
    this.transportCost = transportCost;
    owner = unit.getOwner();
    this.damaged = damaged;
    this.bombingDamage = bombingDamage;
    this.disabled = disabled;
    units.add(unit);
    createDependents(dependents);
  }

  public int getDamaged() {
    return damaged;
  }

  public int getBombingDamage() {
    return bombingDamage;
  }

  public boolean hasDamageOrBombingUnitDamage() {
    return damaged > 0 || bombingDamage > 0;
  }

  public boolean getDisabled() {
    return disabled;
  }

  public int getHitPoints() {
    return UnitAttachment.get(type).getHitPoints();
  }

  private void createDependents(final Collection<Unit> dependents) {
    this.dependents = new ArrayList<>();
    if (dependents == null) {
      return;
    }
    for (final Unit current : dependents) {
      this.dependents.add(new UnitOwner(current));
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (o == null) {
      return false;
    }
    if (!(o instanceof UnitCategory)) {
      return false;
    }
    final UnitCategory other = (UnitCategory) o;
    // equality of categories does not compare the number
    // of units in the category, so don't compare on units
    final boolean equalsIgnoreDamaged = equalsIgnoreDamagedAndBombingDamageAndDisabled(other);
    // return equalsIgnoreDamaged && other.m_damaged == this.m_damaged;
    return equalsIgnoreDamaged && other.damaged == this.damaged && other.bombingDamage == this.bombingDamage
        && other.disabled == this.disabled;
  }

  private boolean equalsIgnoreDamagedAndBombingDamageAndDisabled(final UnitCategory other) {
    final boolean equalsIgnoreDamaged = other.type.equals(this.type) && other.movement == this.movement
        && other.owner.equals(this.owner) && Util.equals(this.dependents, other.dependents);
    return equalsIgnoreDamaged;
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, owner);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("Entry type:").append(type.getName()).append(" owner:").append(owner.getName()).append(" damaged:")
        .append(damaged).append(" bombingUnitDamage:").append(bombingDamage).append(" disabled:").append(disabled)
        .append(" dependents:").append(dependents).append(" movement:").append(movement);
    return sb.toString();
  }

  /**
   * Collection of UnitOwners, the type of our dependents, not the dependents.
   */
  public Collection<UnitOwner> getDependents() {
    return dependents;
  }

  public List<Unit> getUnits() {
    return units;
  }

  public int getMovement() {
    return movement;
  }

  public int getTransportCost() {
    return transportCost;
  }

  public PlayerID getOwner() {
    return owner;
  }

  public void addUnit(final Unit unit) {
    units.add(unit);
  }

  public UnitType getType() {
    return type;
  }

  @Override
  public int compareTo(final Object o) {
    if (o == null) {
      return -1;
    }
    final UnitCategory other = (UnitCategory) o;
    if (!other.owner.equals(this.owner)) {
      return this.owner.getName().compareTo(other.owner.getName());
    }
    final int typeCompare = new UnitTypeComparator().compare(this.getType(), other.getType());
    if (typeCompare != 0) {
      return typeCompare;
    }
    if (movement != other.movement) {
      return movement - other.movement;
    }
    if (!Util.equals(this.dependents, other.dependents)) {
      return dependents.toString().compareTo(other.dependents.toString());
    }
    if (this.damaged != other.damaged) {
      return this.damaged - other.damaged;
    }
    if (this.bombingDamage != other.bombingDamage) {
      return this.bombingDamage - other.bombingDamage;
    }
    if (this.disabled != other.disabled) {
      if (disabled) {
        return 1;
      }
      return -1;
    }
    return 0;
  }
}
