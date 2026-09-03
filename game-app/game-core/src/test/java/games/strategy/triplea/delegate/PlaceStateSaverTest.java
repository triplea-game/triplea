package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.serializer.GameDataOracle;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PlaceStateSaverTest {

  private final GameData gameData = TestMapGameData.REVISED.getGameData();

  private PlaceExtendedDelegateState sampleState() {
    final PlaceExtendedDelegateState s = new PlaceExtendedDelegateState();
    final BaseDelegateState base = new BaseDelegateState();
    base.startBaseStepsFinished = true;
    s.superState = base;

    // produced: territory -> units, exercising territory-by-name and unit-by-UUID references.
    final Unit unit = gameData.getUnits().getUnits().iterator().next();
    final Map<Territory, Collection<Unit>> produced = new LinkedHashMap<>();
    produced.put(gameData.getMap().getTerritories().get(0), List.of(unit));
    s.produced = produced;

    // placements (UndoablePlacement graph) stays a nested blob; null exercises the "none" path.
    s.placements = null;
    return s;
  }

  @Test
  void saverRoundTripsState() {
    GameDataOracle.assertStateRoundTrips(new PlaceStateSaver(), sampleState(), gameData);
  }
}
