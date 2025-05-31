package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Territory;
import javax.annotation.Nullable;

/** Changes ownership of a territory. */
class OwnerChange extends Change {
  private static final long serialVersionUID = -5938125380623744929L;

  /** Either new or old owner can be null. */
  private final String oldOwnerName;

  private final @Nullable String newOwnerName;
  private final String territoryName;

  /** newOwner can be null. */
  OwnerChange(final Territory territory, final @Nullable GamePlayer newOwner) {
    territoryName = territory.getName();
    if (newOwner == null) {
      newOwnerName = null;
    } else {
      newOwnerName = newOwner.getName();
    }
    oldOwnerName = territory.getOwner().getName();
  }

  private OwnerChange(
      final String territoryName, final String newOwnerName, final String oldOwnerName) {
    this.territoryName = territoryName;
    this.newOwnerName = newOwnerName;
    this.oldOwnerName = oldOwnerName;
  }

  @Override
  public Change invert() {
    return new OwnerChange(territoryName, oldOwnerName, newOwnerName);
  }

  @Override
  protected void perform(final GameState data) {
    final GamePlayer newOwner;
    if (newOwnerName == null) {
      newOwner = null;
    } else {
      newOwner = data.getPlayerList().getPlayerId(newOwnerName);
    }
    data.getMap().getTerritoryOrThrow(territoryName).setOwner(newOwner);
  }

  @Override
  public String toString() {
    return newOwnerName + " takes " + territoryName + " from " + oldOwnerName;
  }
}
