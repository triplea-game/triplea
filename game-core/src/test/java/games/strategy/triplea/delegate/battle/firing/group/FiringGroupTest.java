package games.strategy.triplea.delegate.battle.firing.group;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FiringGroupTest {

  private static final GameData TWW_GAME_DATA = TestMapGameData.TWW.getGameData();
  private static final GamePlayer BRITAIN = GameDataTestUtil.britain(TWW_GAME_DATA);
  private static final GamePlayer GERMANY = GameDataTestUtil.germany(TWW_GAME_DATA);

  @Test
  @DisplayName("Verify non suicide units are one firing group")
  void noSuicideFiringGroup() {
    final Collection<Unit> attackingUnits =
        GameDataTestUtil.britishInfantry(TWW_GAME_DATA).create(1, BRITAIN);
    final Collection<Unit> defendingUnits =
        GameDataTestUtil.germanInfantry(TWW_GAME_DATA).create(1, GERMANY);

    final List<FiringGroup> result =
        FiringGroup.newFiringUnitGroups(attackingUnits, units -> defendingUnits, "");

    assertThat(result.size(), is(1));
    assertThat(result.get(0).getFiringUnits(), is(attackingUnits));
    assertThat(result.get(0).getValidTargets(), is(defendingUnits));
  }

  @Test
  @DisplayName("Verify suicide units are before regular units")
  void suicideFiringGroups() {
    final List<Unit> attackingUnits =
        GameDataTestUtil.germanInfantry(TWW_GAME_DATA).create(1, GERMANY);
    attackingUnits.addAll(GameDataTestUtil.germanMine(TWW_GAME_DATA).create(1, GERMANY));
    final Collection<Unit> defendingUnits =
        GameDataTestUtil.britishInfantry(TWW_GAME_DATA).create(1, BRITAIN);

    final List<FiringGroup> result =
        FiringGroup.newFiringUnitGroups(attackingUnits, units -> defendingUnits, "");

    assertThat(result.size(), is(2));
    assertThat(result.get(0).getFiringUnits(), is(List.of(attackingUnits.get(1))));
    assertThat(result.get(1).getFiringUnits(), is(List.of(attackingUnits.get(0))));
  }
}
