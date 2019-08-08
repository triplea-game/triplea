package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.net.GUID;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** Changes ownership of a unit. */
class PlayerOwnerChange extends Change {
  private static final long serialVersionUID = -9154938431233632882L;

  private final Map<GUID, String> oldOwnerNamesByUnitId;
  private final Map<GUID, String> newOwnerNamesByUnitId;
  private final String territoryName;

  PlayerOwnerChange(
      final Collection<Unit> units, final PlayerId newOwner, final Territory territory) {
    oldOwnerNamesByUnitId = new HashMap<>();
    newOwnerNamesByUnitId = new HashMap<>();
    territoryName = territory.getName();
    for (final Unit unit : units) {
      oldOwnerNamesByUnitId.put(unit.getId(), unit.getOwner().getName());
      newOwnerNamesByUnitId.put(unit.getId(), newOwner.getName());
    }
  }

  PlayerOwnerChange(
      final Map<GUID, String> newOwnerNamesByUnitId,
      final Map<GUID, String> oldOwnerNamesByUnitId,
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
  protected void perform(final GameData data) {
    for (final GUID id : newOwnerNamesByUnitId.keySet()) {
      final Unit unit = data.getUnits().get(id);
      if (!oldOwnerNamesByUnitId.get(id).equals(unit.getOwner().getName())) {
        throw new IllegalStateException(
            "Wrong owner, expecting"
                + oldOwnerNamesByUnitId.get(id)
                + " but got "
                + unit.getOwner());
      }
      final String owner = newOwnerNamesByUnitId.get(id);
      final PlayerId player = data.getPlayerList().getPlayerId(owner);
      unit.setOwner(player);
    }
    data.getMap().getTerritory(territoryName).notifyChanged();
  }

  @Override
  public String toString() {
    return "Some units change owners in territory " + territoryName;
  }
}
