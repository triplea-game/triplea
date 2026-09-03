package games.strategy.engine.framework;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.serializer.GameDataOracle;
import games.strategy.triplea.xml.TestMapGameData;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Hardens the delegate savers by running the full text save/load pipeline over several real game
 * maps. {@link GameDataOracle#assertReconstructs} both round-trips the whole game and, for every
 * delegate whose state is written natively, asserts the serialized state survives the trip. This
 * exercises all registered savers against varied delegate sets, entity names, and map definitions.
 */
class TextGameDataMultiMapRoundTripTest {

  @ParameterizedTest
  @EnumSource(
      value = TestMapGameData.class,
      names = {"REVISED", "LHTR", "WW2V3_1941", "IRON_BLITZ", "BIG_WORLD_1942"})
  void fullTextRoundTrip(final TestMapGameData map) {
    final GameData gameData = map.getGameData();

    final GameData reloaded = GameDataOracle.assertReconstructs(gameData);

    assertThat(reloaded.getGameName(), is(gameData.getGameName()));
    assertThat(reloaded.getDelegates(), hasSize(gameData.getDelegates().size()));
  }
}
