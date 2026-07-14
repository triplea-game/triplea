package games.strategy.triplea.delegate.battle;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.delegate.battle.AirControlTracker.ControlEntry;
import javax.annotation.Nullable;

/** A serializable game-data change that updates air control for one territory. */
final class AirControlChange extends Change {
  private static final long serialVersionUID = 2402266251658577161L;

  private final String territoryName;
  private final @Nullable ControlEntry oldControl;
  private final @Nullable ControlEntry newControl;

  AirControlChange(
      final String territoryName,
      final @Nullable ControlEntry oldControl,
      final @Nullable ControlEntry newControl) {
    this.territoryName = territoryName;
    this.oldControl = oldControl;
    this.newControl = newControl;
  }

  @Override
  protected void perform(final GameState data) {
    final Territory territory = data.getMap().getTerritoryOrNull(territoryName);
    if (territory == null) {
      throw new IllegalStateException("Unknown air-control territory: " + territoryName);
    }
    AirControlTracker.getOrCreate(data).setEntry(territoryName, newControl);
    territory.notifyChanged();
  }

  @Override
  public Change invert() {
    return new AirControlChange(territoryName, newControl, oldControl);
  }

  @Override
  public String toString() {
    return "Air control in "
        + territoryName
        + " changes from "
        + controllerName(oldControl)
        + " to "
        + controllerName(newControl);
  }

  private static String controllerName(final @Nullable ControlEntry entry) {
    return entry == null ? "none" : entry.playerName();
  }
}
