package games.strategy.triplea.delegate;

/**
 * This extended delegate exists solely to do everything PlaceDelegate does, but NOT check for air
 * that can't land.
 */
public class NoAirCheckPlaceDelegate extends PlaceDelegate {
  @Override
  protected void removeAirThatCantLand() {
    // Nothing, on purpose.
  }
}
