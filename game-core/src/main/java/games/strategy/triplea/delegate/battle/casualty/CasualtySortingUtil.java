package games.strategy.triplea.delegate.battle.casualty;

import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.UnitComparator;
import java.util.Comparator;
import java.util.List;

public class CasualtySortingUtil {
  /**
   * In an amphibious assault, sort on who is unloading from transports first as this will allow the
   * marines with higher scores to get killed last.
   */
  public static void sortAmphib(final List<Unit> units, final List<Unit> amphibiousLandAttackers) {
    final Comparator<Unit> decreasingMovement =
        UnitComparator.getLowestToHighestMovementComparator();
    units.sort(
        Comparator.comparing(Unit::getType, Comparator.comparing(UnitType::getName))
            .thenComparing(
                (u1, u2) -> {
                  final UnitAttachment ua = UnitAttachment.get(u1.getType());
                  final UnitAttachment ua2 = UnitAttachment.get(u2.getType());
                  if (ua.getIsMarine() != 0 && ua2.getIsMarine() != 0) {
                    return compareAccordingToAmphibious(u1, u2, amphibiousLandAttackers);
                  }
                  return 0;
                })
            .thenComparing(decreasingMovement));
  }

  private static int compareAccordingToAmphibious(
      final Unit u1, final Unit u2, final List<Unit> amphibiousLandAttackers) {
    if (amphibiousLandAttackers.contains(u1) && !amphibiousLandAttackers.contains(u2)) {
      return -1;
    } else if (amphibiousLandAttackers.contains(u2) && !amphibiousLandAttackers.contains(u1)) {
      return 1;
    }
    final int m1 = UnitAttachment.get(u1.getType()).getIsMarine();
    final int m2 = UnitAttachment.get(u2.getType()).getIsMarine();
    return m2 - m1;
  }

  /**
   * Sort in a determined way so that the dice results appear in a logical order. Also sort by
   * movement, so casualties will be chosen as the units with least movement.
   */
  public static void sortPreBattle(final List<Unit> units) {
    units.sort(
        Comparator.comparing(Unit::getType, Comparator.comparing(UnitType::getName))
            .thenComparing(UnitComparator.getLowestToHighestMovementComparator()));
  }
}
