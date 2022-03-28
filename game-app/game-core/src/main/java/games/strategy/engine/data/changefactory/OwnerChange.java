package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.attachments.UnitAttachment;

/** Changes ownership of a territory. */
class OwnerChange extends Change {
  private static final long serialVersionUID = -5938125380623744929L;

  /** Either new or old owner can be null. */
  private final String oldOwnerName;

  private final String newOwnerName;
  private final String territoryName;

  /** newOwner can be null. */
  OwnerChange(final Territory territory, final GamePlayer newOwner) {
    territoryName = territory.getName();
    newOwnerName = getName(newOwner);
    oldOwnerName = getName(territory.getOwner());
  }

  private OwnerChange(
      final String territoryName, final String newOwnerName, final String oldOwnerName) {
    this.territoryName = territoryName;
    this.newOwnerName = newOwnerName;
    this.oldOwnerName = oldOwnerName;
  }

  private static String getName(final GamePlayer player) {
    if (player == null) {
      return null;
    }
    return player.getName();
  }

  private static GamePlayer getPlayerId(final String name, final GameState data) {
    if (name == null) {
      return null;
    }
    return data.getPlayerList().getPlayerId(name);
  }

  @Override
  public Change invert() {
    return new OwnerChange(territoryName, oldOwnerName, newOwnerName);
  }

  @Override
  protected void perform(final GameState data) {
    // both names could be null
    final Territory territory = data.getMap().getTerritory(territoryName);
    final GamePlayer oldOwner = getPlayerId(oldOwnerName, data);
    final GamePlayer newOwner = getPlayerId(newOwnerName, data);
    territory.getUnits().stream()
        .filter(
            unit -> {
              final UnitAttachment ua = unit.getUnitAttachment();
              return !ua.getIsLandTransportable()
                  && !ua.getIsAirTransportable()
                  && ua.getMovement(oldOwner) == 0;
            })
        .forEach(unit -> unit.setOwner(newOwner));
    territory.setOwner(newOwner);
  }

  @Override
  public String toString() {
    return newOwnerName + " takes " + territoryName + " from " + oldOwnerName;
  }
}
