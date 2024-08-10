package games.strategy.triplea.delegate.battle.casualty;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.TransportTracker;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CasualtyUtil {
  /** Find total remaining hit points of units. */
  public static int getTotalHitpointsLeft(final Collection<Unit> units) {
    if (units == null || units.isEmpty()) {
      return 0;
    }
    int totalHitPoints = 0;
    for (final Unit u : units) {
      final UnitAttachment ua = u.getUnitAttachment();
      if (!ua.isInfrastructure()) {
        totalHitPoints += ua.getHitPoints();
        totalHitPoints -= u.getHits();
      }
    }
    return totalHitPoints;
  }

  public static Map<Unit, Collection<Unit>> getDependents(final Collection<Unit> targets) {
    // just worry about transports
    final Map<Unit, Collection<Unit>> dependents = new HashMap<>();
    for (final Unit target : targets) {
      dependents.put(target, TransportTracker.transportingAndUnloaded(target));
    }
    return dependents;
  }
}
