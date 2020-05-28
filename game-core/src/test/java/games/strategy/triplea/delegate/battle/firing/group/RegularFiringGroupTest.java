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

class RegularFiringGroupTest {

  private static final GameData FIRING_GROUPS_GAME_DATA =
      TestMapGameData.FIRING_GROUPS.getGameData();
  private static final GamePlayer BRITAIN = GameDataTestUtil.britain(FIRING_GROUPS_GAME_DATA);
  private static final GamePlayer GERMANY = GameDataTestUtil.germany(FIRING_GROUPS_GAME_DATA);

  @Test
  @DisplayName("Verify regular firing group has all units and their targets.")
  void getFiringGroups() {
    final Collection<Unit> attackingUnits =
        GameDataTestUtil.tank(FIRING_GROUPS_GAME_DATA).create(1, BRITAIN);
    final Collection<Unit> defendingUnits =
        GameDataTestUtil.tank(FIRING_GROUPS_GAME_DATA).create(1, GERMANY);

    final List<FiringGroup> result =
        RegularFiringGroup.builder()
            .firingUnits(attackingUnits)
            .attackableUnits(defendingUnits)
            .defending(false)
            .build()
            .getFiringGroups();

    final FiringGroup expected = FiringGroup.of(attackingUnits, defendingUnits, false, "");
    assertThat(result, is(List.of(expected)));
  }

  @Test
  @DisplayName("Verify regular firing group ignores suicide on defense")
  void getFiringGroupsWithoutSuicide() {
    final Collection<Unit> attackingUnits =
        GameDataTestUtil.tank(FIRING_GROUPS_GAME_DATA).create(1, BRITAIN);
    final List<Unit> defendingUnits =
        GameDataTestUtil.suicideDefender(FIRING_GROUPS_GAME_DATA).create(1, GERMANY);
    defendingUnits.addAll(GameDataTestUtil.tank(FIRING_GROUPS_GAME_DATA).create(1, GERMANY));

    final List<FiringGroup> result =
        RegularFiringGroup.builder()
            .firingUnits(attackingUnits)
            .attackableUnits(defendingUnits)
            .defending(false)
            .build()
            .getFiringGroups();

    final FiringGroup expected =
        FiringGroup.of(attackingUnits, List.of(defendingUnits.get(1)), false, "");
    assertThat(result, is(List.of(expected)));
  }

  @Test
  @DisplayName("Verify suicide and non suicide units make two firing groups")
  void getFiringGroupsWithSuicideIsFirst() {
    final List<Unit> attackingUnits =
        GameDataTestUtil.tank(FIRING_GROUPS_GAME_DATA).create(1, BRITAIN);
    attackingUnits.addAll(GameDataTestUtil.missile(FIRING_GROUPS_GAME_DATA).create(1, BRITAIN));
    final List<Unit> defendingUnits =
        GameDataTestUtil.tank(FIRING_GROUPS_GAME_DATA).create(1, GERMANY);

    final List<FiringGroup> result =
        RegularFiringGroup.builder()
            .firingUnits(attackingUnits)
            .attackableUnits(defendingUnits)
            .defending(false)
            .build()
            .getFiringGroups();

    final List<FiringGroup> expected =
        List.of(
            FiringGroup.of(List.of(attackingUnits.get(1)), defendingUnits, true, ""),
            FiringGroup.of(List.of(attackingUnits.get(0)), defendingUnits, false, ""));
    assertThat(result, is(expected));
  }

  @Test
  @DisplayName(
      "Verify canNotTarget units are in different firing groups and also split by isSuicide")
  void getFiringGroupsWithNonTargetableUnitsAndSuicideUnits() {
    final Unit tank = GameDataTestUtil.tank(FIRING_GROUPS_GAME_DATA).create(BRITAIN);
    final Unit missile = GameDataTestUtil.missile(FIRING_GROUPS_GAME_DATA).create(BRITAIN);
    final Unit spyKiller = GameDataTestUtil.spyKiller(FIRING_GROUPS_GAME_DATA).create(BRITAIN);
    final Unit spySuicider = GameDataTestUtil.spySuicider(FIRING_GROUPS_GAME_DATA).create(BRITAIN);
    final List<Unit> attackingUnits = List.of(tank, missile, spyKiller, spySuicider);
    final Unit defendingTank = GameDataTestUtil.tank(FIRING_GROUPS_GAME_DATA).create(GERMANY);
    final Unit spy = GameDataTestUtil.spy(FIRING_GROUPS_GAME_DATA).create(GERMANY);
    final List<Unit> defendingUnits = List.of(defendingTank, spy);

    final List<FiringGroup> result =
        RegularFiringGroup.builder()
            .firingUnits(attackingUnits)
            .attackableUnits(defendingUnits)
            .defending(false)
            .build()
            .getFiringGroups();

    final List<FiringGroup> expected =
        List.of(
            FiringGroup.of(List.of(spySuicider), List.of(spy), true, ""),
            FiringGroup.of(List.of(spyKiller), List.of(spy), false, ""),
            FiringGroup.of(List.of(missile), List.of(defendingTank), true, ""),
            FiringGroup.of(List.of(tank), List.of(defendingTank), false, ""));
    assertThat(result, is(expected));
  }
}
