package games.strategy.triplea.delegate;

import java.util.ArrayList;
import java.util.Collection;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attachments.TerritoryAttachment;

/**
 * Tracks the original owner of things.
 * Needed since territories and factories must revert
 * to their original owner when captured from the enemy.
 */
public class OriginalOwnerTracker implements java.io.Serializable {
  private static final long serialVersionUID = 8462432412106180906L;

  /** Creates new OriginalOwnerTracker. */
  public OriginalOwnerTracker() {}

  public static Change addOriginalOwnerChange(final Territory t, final PlayerID player) {
    return ChangeFactory.attachmentPropertyChange(TerritoryAttachment.get(t), player, Constants.ORIGINAL_OWNER);
  }

  public static Change addOriginalOwnerChange(final Unit unit, final PlayerID player) {
    return ChangeFactory.unitPropertyChange(unit, player, Constants.ORIGINAL_OWNER);
  }

  public static Change addOriginalOwnerChange(final Collection<Unit> units, final PlayerID player) {
    final CompositeChange change = new CompositeChange();
    for (final Unit unit : units) {
      change.add(addOriginalOwnerChange(unit, player));
    }
    return change;
  }

  public static PlayerID getOriginalOwner(final Unit unit) {
    return TripleAUnit.get(unit).getOriginalOwner();
  }

  public static PlayerID getOriginalOwner(final Territory t) {
    final TerritoryAttachment ta = TerritoryAttachment.get(t);
    if (ta == null) {
      return null;
    }
    return ta.getOriginalOwner();
  }

  public static Collection<Territory> getOriginallyOwned(final GameData data, final PlayerID player) {
    final Collection<Territory> rVal = new ArrayList<>();
    for (final Territory t : data.getMap()) {
      PlayerID originalOwner = getOriginalOwner(t);
      if (originalOwner == null) {
        originalOwner = PlayerID.NULL_PLAYERID;
      }
      if (originalOwner.equals(player)) {
        rVal.add(t);
      }
    }
    return rVal;
  }
}
