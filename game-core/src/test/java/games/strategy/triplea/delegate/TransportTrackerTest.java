package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.GameDataTestUtil.americans;
import static games.strategy.triplea.delegate.GameDataTestUtil.armour;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.GameDataTestUtil.transport;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import org.junit.jupiter.api.Test;

class TransportTrackerTest {
  private final GameData gameData = TestMapGameData.REVISED.getGameData();
  private final Territory sz18 = territory("18 Sea Zone", gameData);
  private final Unit transport = transport(gameData).create(americans(gameData));
  private final Unit tank = armour(gameData).create(americans(gameData));

  @Test
  void testIsTransporting() {
    addTo(sz18, List.of(transport));
    assertThat(TransportTracker.isTransporting(transport), is(false));

    addTo(sz18, List.of(tank));
    final Change change = TransportTracker.loadTransportChange(transport, tank);
    gameData.performChange(change);
    assertThat(TransportTracker.isTransporting(transport), is(true));
  }
}
