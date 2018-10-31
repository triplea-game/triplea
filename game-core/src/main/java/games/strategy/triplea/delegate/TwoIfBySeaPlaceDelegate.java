package games.strategy.triplea.delegate;

import java.util.Collection;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.util.CollectionUtils;

/**
 * Logic for placing units in a Two If By Sea game.
 *
 * @deprecated Required for map compatibility. Remove upon next map-incompatible release.
 */
@Deprecated
public class TwoIfBySeaPlaceDelegate extends AbstractPlaceDelegate {
  /**
   * Returns the production of the territory, ignores whether the territory was an original factory.
   */
  protected int getProduction(final Territory territory) {
    final Collection<Unit> allUnits = territory.getUnits().getUnits();
    final int factoryCount = CollectionUtils.countMatches(allUnits, Matches.unitCanProduceUnits());
    return 5 * factoryCount;
  }
}
