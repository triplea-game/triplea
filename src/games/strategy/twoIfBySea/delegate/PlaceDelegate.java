package games.strategy.twoIfBySea.delegate;

import java.util.Collection;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.MapSupport;
import games.strategy.triplea.delegate.AbstractPlaceDelegate;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.Match;

/**
 * Logic for placing units.
 */
@MapSupport
public class PlaceDelegate extends AbstractPlaceDelegate {
  /**
   * @return gets the production of the territory, ignores whether the territory was an original factory
   */
  @Override
  protected int getProduction(final Territory territory) {
    final Collection<Unit> allUnits = territory.getUnits().getUnits();
    final int factoryCount = Match.countMatches(allUnits, Matches.UnitCanProduceUnits);
    return 5 * factoryCount;
  }
}
