package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.serializer.GameDataOracle;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Oracle-gated round-trip test for {@link TechnologyStateSaver}. */
class TechnologyStateSaverTest {

  private final GameData gameData = TestMapGameData.REVISED.getGameData();

  private TechnologyExtendedDelegateState sampleState() {
    final BaseDelegateState base = new BaseDelegateState();
    base.startBaseStepsFinished = true;

    final Map<GamePlayer, Collection<TechAdvance>> techs = new HashMap<>();
    techs.put(
        gameData.getPlayerList().getPlayers().get(0),
        new java.util.ArrayList<>(gameData.getTechnologyFrontier().getTechs()));

    final TechnologyExtendedDelegateState state = new TechnologyExtendedDelegateState();
    state.superState = base;
    state.needToInitialize = true;
    state.techs = techs;
    return state;
  }

  @Test
  void saverRoundTripsState() {
    GameDataOracle.assertStateRoundTrips(new TechnologyStateSaver(), sampleState(), gameData);
  }
}
