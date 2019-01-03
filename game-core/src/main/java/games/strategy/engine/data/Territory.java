package games.strategy.engine.data;

import java.util.Optional;

import javax.annotation.Nullable;

/** A territory on a map. */
public class Territory extends NamedAttachable implements NamedUnitHolder, Comparable<Territory> {
  private static final long serialVersionUID = -6390555051736721082L;

  private final boolean water;
  private PlayerId owner = PlayerId.NULL_PLAYERID;
  private final UnitCollection units;

  public Territory(final String name, final GameData data) {
    this(name, false, data);
  }

  public Territory(final String name, final boolean water, final GameData data) {
    super(name, data);
    this.water = water;
    units = new UnitCollection(this, getData());
  }

  public boolean isWater() {
    return water;
  }

  /**
   * Returns the territory owner; will be {@link PlayerId#NULL_PLAYERID} if the territory is not
   * owned.
   */
  public PlayerId getOwner() {
    return owner;
  }

  public void setOwner(final @Nullable PlayerId owner) {
    this.owner = Optional.ofNullable(owner).orElse(PlayerId.NULL_PLAYERID);
    getData().notifyTerritoryOwnerChanged(this);
  }

  /** Get the units in this territory. */
  @Override
  public UnitCollection getUnits() {
    return units;
  }

  /** refers to unit holder being changed. */
  @Override
  public void notifyChanged() {
    getData().notifyTerritoryUnitsChanged(this);
  }

  /**
   * refers to attachment changing, and therefore needing a redraw on the map in case something like
   * the production number is now different.
   */
  public void notifyAttachmentChanged() {
    getData().notifyTerritoryAttachmentChanged(this);
  }

  @Override
  public String toString() {
    return getName();
  }

  @Override
  public int compareTo(final Territory o) {
    return getName().compareTo(o.getName());
  }

  @Override
  public String getType() {
    return UnitHolder.TERRITORY;
  }
}
