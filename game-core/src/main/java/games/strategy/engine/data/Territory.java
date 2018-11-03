package games.strategy.engine.data;

import java.util.Optional;

import javax.annotation.Nullable;

/**
 * A territory on a map.
 */
public class Territory extends NamedAttachable implements NamedUnitHolder, Comparable<Territory> {
  private static final long serialVersionUID = -6390555051736721082L;

  private final boolean water;
  private PlayerID owner = PlayerID.NULL_PLAYERID;
  private final UnitCollection units;
  // In a grid-based game, stores the coordinate of the Territory
  @SuppressWarnings("unused")
  private final int[] coordinate;

  public Territory(final String name, final GameData data) {
    this(name, false, data);
  }

  /** Creates new Territory. */
  public Territory(final String name, final boolean water, final GameData data) {
    super(name, data);
    this.water = water;
    units = new UnitCollection(this, getData());
    coordinate = null;
  }

  /** Creates new Territory. */
  public Territory(final String name, final boolean water, final GameData data, final int... coordinate) {
    super(name, data);
    this.water = water;
    units = new UnitCollection(this, getData());
    if (data.getMap().isCoordinateValid(coordinate)) {
      this.coordinate = coordinate;
    } else {
      throw new IllegalArgumentException("Invalid coordinate: " + coordinate[0] + "," + coordinate[1]);
    }
  }

  public boolean isWater() {
    return water;
  }

  /**
   * Returns the territory owner; will be {@link PlayerID#NULL_PLAYERID} if the territory is not owned.
   */
  public PlayerID getOwner() {
    return owner;
  }

  public void setOwner(final @Nullable PlayerID owner) {
    this.owner = Optional.ofNullable(owner).orElse(PlayerID.NULL_PLAYERID);
    getData().notifyTerritoryOwnerChanged(this);
  }

  /**
   * Get the units in this territory.
   */
  @Override
  public UnitCollection getUnits() {
    return units;
  }

  /**
   * refers to unit holder being changed.
   */
  @Override
  public void notifyChanged() {
    getData().notifyTerritoryUnitsChanged(this);
  }

  /**
   * refers to attachment changing, and therefore needing a redraw on the map in case something like the production
   * number is now different.
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
