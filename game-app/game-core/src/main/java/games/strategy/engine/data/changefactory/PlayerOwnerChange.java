package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Changes ownership of a unit. */
class PlayerOwnerChange extends Change {
  private static final long serialVersionUID = -9154938431233632882L;

  private final Map<UUID, String> oldOwnerNamesByUnitId;
  private final Map<UUID, String> newOwnerNamesByUnitId;
  private final String territoryName;

  PlayerOwnerChange(
      final Collection<Unit> units, final GamePlayer newOwner, final Territory territory) {
    oldOwnerNamesByUnitId = new HashMap<>();
    newOwnerNamesByUnitId = new HashMap<>();
    territoryName = territory.getName();
    for (final Unit unit : units) {
      oldOwnerNamesByUnitId.put(unit.getId(), unit.getOwner().getName());
      newOwnerNamesByUnitId.put(unit.getId(), newOwner.getName());
    }
  }

  PlayerOwnerChange(
      final Map<UUID, String> newOwnerNamesByUnitId,
      final Map<UUID, String> oldOwnerNamesByUnitId,
      final String territoryName) {
    this.oldOwnerNamesByUnitId = oldOwnerNamesByUnitId;
    this.newOwnerNamesByUnitId = newOwnerNamesByUnitId;
    this.territoryName = territoryName;
  }

  @Override
  public Change invert() {
    return new PlayerOwnerChange(oldOwnerNamesByUnitId, newOwnerNamesByUnitId, territoryName);
  }

  @Override
  protected void perform(final GameState data) {
    newOwnerNamesByUnitId.forEach(
        (uuid, newOwnerName) -> {
          final Unit unit = data.getUnits().get(uuid);
          if (!oldOwnerNamesByUnitId.get(uuid).equals(unit.getOwner().getName())) {
            throw new IllegalStateException(
                "Wrong "
                    + unit.getType().getName()
                    + " owner, expecting "
                    + oldOwnerNamesByUnitId.get(uuid)
                    + " but got "
                    + unit.getOwner());
          }
          final GamePlayer newOwner = data.getPlayerList().getPlayerId(newOwnerName);
          unit.setOwner(newOwner);
        });
    data.getMap().getTerritoryOrThrow(territoryName).notifyChanged();
  }

  @Override
  public String toString() {
    return "Some units change owners in territory " + territoryName;
  }
}
