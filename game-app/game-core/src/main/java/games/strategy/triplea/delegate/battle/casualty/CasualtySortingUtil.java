package games.strategy.triplea.delegate.battle.casualty;

import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.UnitComparator;
import java.util.Comparator;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CasualtySortingUtil {
  /** In an amphibious assault, sort marines with higher scores last */
  public static void sortPreBattle(final List<Unit> units) {
    units.sort(
        Comparator.comparing(Unit::getType, Comparator.comparing(UnitType::getName))
            .thenComparing(compareMarines())
            .thenComparing(UnitComparator.getLowestToHighestMovementComparator()));
  }

  public static Comparator<Unit> compareMarines() {
    return (u1, u2) -> {
      final int result = Boolean.compare(u1.getWasAmphibious(), u2.getWasAmphibious());
      if (result != 0) {
        return result;
      }

      final UnitAttachment ua1 = UnitAttachment.get(u1.getType());
      final UnitAttachment ua2 = UnitAttachment.get(u2.getType());
      return Integer.compare(ua1.getIsMarine(), ua2.getIsMarine());
    };
  }
}
