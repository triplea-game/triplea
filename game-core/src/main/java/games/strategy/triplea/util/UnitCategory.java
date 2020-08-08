package games.strategy.triplea.util;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.attachments.UnitTypeComparator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.triplea.java.collections.CollectionUtils;

/**
 * A category of units.
 *
 * <p>Primarily used to group units by type and owner, but units may also be categorized by the
 * following attributes:
 *
 * <ul>
 *   <li>Available movement
 *   <li>Transport cost
 *   <li>Damage
 *   <li>Bombing damage
 *   <li>Disabled state
 *   <li>Dependents
 * </ul>
 */
public class UnitCategory implements Comparable<UnitCategory> {
  private final UnitType type;
  // Collection of UnitOwners, the type of our dependents, not the dependents
  private Collection<UnitOwner> dependents;
  // movement of the units
  private final BigDecimal movement;
  // movement of the units
  private final int transportCost;
  private final GamePlayer owner;
  // the units in the category, may be duplicates.
  private final List<Unit> units = new ArrayList<>();
  private int damaged = 0;
  private int bombingDamage = 0;
  private boolean disabled = false;

  public UnitCategory(final UnitType type, final GamePlayer owner) {
    this.type = type;
    dependents = List.of();
    movement = new BigDecimal(-1);
    transportCost = -1;
    this.owner = owner;
  }

  UnitCategory(
      final Unit unit,
      final Collection<Unit> dependents,
      final BigDecimal movement,
      final int damaged,
      final int bombingDamage,
      final boolean disabled,
      final int transportCost) {
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
    if (!(o instanceof UnitCategory)) {
      return false;
    }
    final UnitCategory other = (UnitCategory) o;
    // equality of categories does not compare the number of units in the category, so don't compare
    // on units
    final boolean equalsIgnoreDamaged = equalsIgnoreDamagedAndBombingDamageAndDisabled(other);
    return equalsIgnoreDamaged
        && other.damaged == this.damaged
        && other.bombingDamage == this.bombingDamage
        && other.disabled == this.disabled;
  }

  private boolean equalsIgnoreDamagedAndBombingDamageAndDisabled(final UnitCategory other) {
    return other.type.equals(this.type)
        && other.movement.compareTo(this.movement) == 0
        && other.owner.equals(this.owner)
        && CollectionUtils.haveEqualSizeAndEquivalentElements(this.dependents, other.dependents);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, owner);
  }

  @Override
  public String toString() {
    return "Entry type:"
        + type.getName()
        + " owner:"
        + owner.getName()
        + " damaged:"
        + damaged
        + " bombingUnitDamage:"
        + bombingDamage
        + " disabled:"
        + disabled
        + " dependents:"
        + dependents
        + " movement:"
        + movement;
  }

  /** Collection of UnitOwners, the type of our dependents, not the dependents. */
  public Collection<UnitOwner> getDependents() {
    return dependents;
  }

  public List<Unit> getUnits() {
    return units;
  }

  public BigDecimal getMovement() {
    return movement;
  }

  public int getTransportCost() {
    return transportCost;
  }

  public GamePlayer getOwner() {
    return owner;
  }

  public void addUnit(final Unit unit) {
    units.add(unit);
  }

  public UnitType getType() {
    return type;
  }

  @Override
  public int compareTo(final UnitCategory other) {
    return Comparator.nullsLast(
            Comparator.comparing(UnitCategory::getOwner, Comparator.comparing(GamePlayer::getName))
                .thenComparing(UnitCategory::getType, new UnitTypeComparator())
                .thenComparing(UnitCategory::getMovement)
                .thenComparing(
                    UnitCategory::getDependents,
                    (o1, o2) -> {
                      if (CollectionUtils.haveEqualSizeAndEquivalentElements(o1, o2)) {
                        return 0;
                      }
                      return o1.toString().compareTo(o2.toString());
                    })
                .thenComparingInt(UnitCategory::getDamaged)
                .thenComparingInt(UnitCategory::getBombingDamage)
                .thenComparing(UnitCategory::getDisabled))
        .compare(this, other);
  }
}
