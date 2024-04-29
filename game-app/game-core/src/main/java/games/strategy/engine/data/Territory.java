package games.strategy.engine.data;

import java.util.Optional;
import javax.annotation.Nullable;
import lombok.Getter;

/** A territory on a map. */
@Getter
public class Territory extends NamedAttachable implements NamedUnitHolder, Comparable<Territory> {
  private static final long serialVersionUID = -6390555051736721082L;

  private final boolean water;

  /**
   * The territory owner; defaults to {@link PlayerList#getNullPlayer()} if the territory is not
   * owned.
   */
  private GamePlayer owner;

  @Getter(onMethod_ = {@Override})
  private final UnitCollection unitCollection;

  public Territory(final String name, final GameData data) {
    this(name, false, data);
    owner = data.getPlayerList().getNullPlayer();
  }

  public Territory(final String name, final boolean water, final GameData data) {
    super(name, data);
    this.water = water;
    owner = data.getPlayerList().getNullPlayer();
    unitCollection = new UnitCollection(this, getData());
  }

  public void setOwner(final @Nullable GamePlayer owner) {
    this.owner = Optional.ofNullable(owner).orElse(getData().getPlayerList().getNullPlayer());
    getData().notifyTerritoryOwnerChanged(this);
  }

  public final boolean isOwnedBy(final GamePlayer player) {
    // Use getOwner() to allow test mocks to override that method.
    return getOwner().equals(player);
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
