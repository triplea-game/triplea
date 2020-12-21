package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameDataInjections;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.TerritoryAttachment;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import javax.annotation.Nullable;

/**
 * Tracks the original owner of things. Needed since territories and factories must revert to their
 * original owner when captured from the enemy.
 */
public class OriginalOwnerTracker implements Serializable {
  private static final long serialVersionUID = 8462432412106180906L;

  public static Change addOriginalOwnerChange(final Territory t, final GamePlayer player) {
    return ChangeFactory.attachmentPropertyChange(
        TerritoryAttachment.get(t), player, Constants.ORIGINAL_OWNER);
  }

  public static Change addOriginalOwnerChange(final Unit unit, final GamePlayer player) {
    return ChangeFactory.unitPropertyChange(unit, player, Constants.ORIGINAL_OWNER);
  }

  public static Change addOriginalOwnerChange(
      final Collection<Unit> units, final GamePlayer player) {
    final CompositeChange change = new CompositeChange();
    for (final Unit unit : units) {
      change.add(addOriginalOwnerChange(unit, player));
    }
    return change;
  }

  public static @Nullable GamePlayer getOriginalOwner(final Territory t) {
    final TerritoryAttachment ta = TerritoryAttachment.get(t);
    if (ta == null) {
      return null;
    }
    return ta.getOriginalOwner();
  }

  /** Returns the territories originally owned by the specified player. */
  public static Collection<Territory> getOriginallyOwned(
      final GameDataInjections data, final GamePlayer player) {
    final Collection<Territory> territories = new ArrayList<>();
    for (final Territory t : data.getMap()) {
      GamePlayer originalOwner = getOriginalOwner(t);
      if (originalOwner == null) {
        originalOwner = GamePlayer.NULL_PLAYERID;
      }
      if (originalOwner.equals(player)) {
        territories.add(t);
      }
    }
    return territories;
  }
}
