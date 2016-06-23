package games.strategy.triplea.delegate;

import games.strategy.triplea.MapSupport;

/**
 * This extended delegate exists soley to do everything PlaceDelegate does, but NOT check for air that can't land.
 */
@MapSupport
public class NoAirCheckPlaceDelegate extends games.strategy.triplea.delegate.PlaceDelegate {
  @Override
  protected void removeAirThatCantLand() {
    // Nothing, on purpose.
  }
}
