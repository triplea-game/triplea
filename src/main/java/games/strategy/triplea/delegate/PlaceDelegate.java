package games.strategy.triplea.delegate;

import games.strategy.engine.data.Territory;
import games.strategy.triplea.MapSupport;
import games.strategy.triplea.attachments.TerritoryAttachment;

/**
 * Logic for placing units.
 *
 * <p>
 * Known limitations.
 * Doesnt take into account limits on number of factories that can be produced.
 * The situation where one has two non original factories a,b each with production 2.
 * If sea zone e neighbors a,b and sea zone f neighbors b. Then producing 2 in e
 * could make it such that you cannot produce in f
 * The reason is that the production in e could be assigned to the
 * factory in b, leaving no capacity to produce in f.
 * If anyone ever accidently runs into this situation then they can
 * undo the production, produce in f first, and then produce in e.
 * </p>
 */
@MapSupport
public class PlaceDelegate extends AbstractPlaceDelegate {
  /**
   * @return gets the production of the territory.
   */
  @Override
  protected int getProduction(final Territory territory) {
    // Can be null!
    final TerritoryAttachment ta = TerritoryAttachment.get(territory);
    if (ta != null) {
      return ta.getProduction();
    }
    return 0;
  }
}
