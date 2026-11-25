package games.strategy.triplea.ai.pro.util;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.Matches;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

/** Helpers for units whose damage only heals at a repair facility. */
@UtilityClass
public final class ProRepairUtils {

  /**
   * Returns the {@code candidates} where {@code unit} would be repaired at the end of the turn.
   * Empty if the map repairs damage without facilities or the unit is undamaged.
   */
  public static Set<Territory> findRepairTerritories(
      final GamePlayer player, final Unit unit, final Collection<Territory> candidates) {
    if (!Properties.getTwoHitPointUnitsRequireRepairFacilities(player.getData().getProperties())) {
      return Set.of();
    }
    return candidates.stream()
        .filter(t -> Matches.unitCanBeRepairedByFacilitiesInItsTerritory(t, player).test(unit))
        .collect(Collectors.toSet());
  }
}
