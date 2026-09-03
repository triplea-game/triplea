package games.strategy.engine.message.wire;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

import com.google.gson.Gson;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.xml.TestMapGameData;
import org.junit.jupiter.api.Test;

/**
 * Confirms an entity reference survives a JSON round-trip (as it would over the wire) and then
 * re-resolves to the same live object against a {@link GameData}.
 */
class EntityRefTest {
  private static final Gson gson = new Gson();

  private final GameData gameData = TestMapGameData.REVISED.getGameData();

  private static EntityRef wireRoundTrip(final EntityRef reference) {
    return gson.fromJson(gson.toJson(reference), EntityRef.class);
  }

  @Test
  void resolvesUnitByUuid() {
    final GamePlayer owner = gameData.getPlayerList().getPlayers().get(0);
    final UnitType unitType = gameData.getUnitTypeList().getAllUnitTypes().iterator().next();
    final Unit unit = new Unit(unitType, owner, gameData);
    gameData.getUnits().put(unit);

    final EntityRef reference = wireRoundTrip(EntityRef.of(unit));

    assertThat(reference.resolveUnit(gameData), is(sameInstance(unit)));
  }

  @Test
  void resolvesTerritoryByName() {
    final Territory territory = gameData.getMap().getTerritories().get(0);

    final EntityRef reference = wireRoundTrip(EntityRef.of(territory));

    assertThat(reference.resolveTerritory(gameData), is(sameInstance(territory)));
  }

  @Test
  void resolvesPlayerByName() {
    final GamePlayer player = gameData.getPlayerList().getPlayers().get(0);

    final EntityRef reference = wireRoundTrip(EntityRef.of(player));

    assertThat(reference.resolvePlayer(gameData), is(sameInstance(player)));
  }

  @Test
  void resolvesResourceByName() {
    final Resource resource = gameData.getResourceList().getResources().iterator().next();

    final EntityRef reference = wireRoundTrip(EntityRef.of(resource));

    assertThat(reference.resolveResource(gameData), is(sameInstance(resource)));
  }
}
