package games.strategy.triplea.delegate.battle.end;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FindSubsVsOnlyAirTest {

  private static final GameData GLOBAL_1940_GAME_DATA = TestMapGameData.GLOBAL1940.getGameData();
  private static final GamePlayer BRITISH = GameDataTestUtil.british(GLOBAL_1940_GAME_DATA);
  private static final GamePlayer GERMANS = GameDataTestUtil.germans(GLOBAL_1940_GAME_DATA);

  @Test
  @DisplayName("Verify that subs are found if all enemy units are air")
  void allEnemyUnitsAreAir() {
    final List<Unit> friendlyUnits =
        GameDataTestUtil.submarine(GLOBAL_1940_GAME_DATA).create(1, BRITISH);
    final List<Unit> enemyUnits =
        GameDataTestUtil.fighter(GLOBAL_1940_GAME_DATA).create(1, GERMANS);

    final List<Unit> subs =
        FindSubsVsOnlyAir.builder()
            .friendlyUnits(friendlyUnits)
            .enemyUnits(enemyUnits)
            .build()
            .find();

    assertThat(subs, is(friendlyUnits));
  }

  @Test
  @DisplayName("Verify that no subs are found if some enemy units are not air")
  void allEnemyUnitsAreNotAir() {
    final List<Unit> friendlyUnits =
        GameDataTestUtil.submarine(GLOBAL_1940_GAME_DATA).create(1, BRITISH);
    final List<Unit> enemyUnits =
        GameDataTestUtil.fighter(GLOBAL_1940_GAME_DATA).create(1, GERMANS);
    enemyUnits.addAll(GameDataTestUtil.battleship(GLOBAL_1940_GAME_DATA).create(1, GERMANS));

    final List<Unit> subs =
        FindSubsVsOnlyAir.builder()
            .friendlyUnits(friendlyUnits)
            .enemyUnits(enemyUnits)
            .build()
            .find();

    assertThat(subs, is(List.of()));
  }

  @Test
  @DisplayName("Verify that only subs are found if all enemy units are air")
  void onlySubsAreFound() {
    final List<Unit> friendlyUnits =
        GameDataTestUtil.submarine(GLOBAL_1940_GAME_DATA).create(1, BRITISH);
    friendlyUnits.addAll(GameDataTestUtil.battleship(GLOBAL_1940_GAME_DATA).create(1, BRITISH));
    final List<Unit> enemyUnits =
        GameDataTestUtil.fighter(GLOBAL_1940_GAME_DATA).create(1, GERMANS);

    final List<Unit> subs =
        FindSubsVsOnlyAir.builder()
            .friendlyUnits(friendlyUnits)
            .enemyUnits(enemyUnits)
            .build()
            .find();

    assertThat(subs, is(List.of(friendlyUnits.get(0))));
  }
}
