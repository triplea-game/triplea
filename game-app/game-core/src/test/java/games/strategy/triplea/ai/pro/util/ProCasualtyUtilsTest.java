package games.strategy.triplea.ai.pro.util;

import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import org.junit.jupiter.api.Test;

class ProCasualtyUtilsTest {
  private final GameData data = TestMapGameData.BIG_WORLD_1942_V3.getGameData();
  private final GamePlayer british = data.getPlayerList().getPlayerId("British");
  private final ToDoubleFunction<Unit> cost =
      u ->
          Map.of("marine", 4, "infantry", 3, "fighter", 10, "bomber", 12)
              .get(u.getType().getName());

  private List<Unit> create(final String type, final int count) {
    return data.getUnitTypeList().getUnitTypeOrThrow(type).create(count, british);
  }

  @Test
  void losesTheCheapestAirToKeepTheLastLandUnit() {
    final Unit marine = create("marine", 1).get(0);
    final Unit fighter = create("fighter", 1).get(0);
    final List<Unit> attackers = new ArrayList<>(create("bomber", 2));
    attackers.add(fighter);
    attackers.add(marine);

    final var swap =
        ProCasualtyUtils.findKeepLandSwap(List.of(marine), attackers, attackers, Set.of(), cost);

    assertThat(swap).isPresent();
    assertThat(swap.get().apply(List.of(marine))).containsExactly(fighter);
  }

  @Test
  void keepsTheMostValuableLandUnitWhenEveryLandUnitIsHit() {
    final Unit infantry = create("infantry", 1).get(0);
    final Unit marine = create("marine", 1).get(0);
    final Unit bomber = create("bomber", 1).get(0);
    final List<Unit> attackers = List.of(infantry, marine, bomber);

    final var swap =
        ProCasualtyUtils.findKeepLandSwap(
            List.of(infantry, marine), attackers, attackers, Set.of(), cost);

    assertThat(swap.get().apply(List.of(infantry, marine))).containsExactly(infantry, bomber);
  }

  @Test
  void noSwapWhileAnotherLandUnitSurvives() {
    final List<Unit> infantry = create("infantry", 2);
    final List<Unit> attackers = new ArrayList<>(infantry);
    attackers.addAll(create("bomber", 1));

    assertThat(
            ProCasualtyUtils.findKeepLandSwap(
                List.of(infantry.get(0)), attackers, attackers, Set.of(), cost))
        .isEmpty();
  }

  @Test
  void noSwapWhenTheHitsCannotTakeAir() {
    final Unit marine = create("marine", 1).get(0);
    final List<Unit> attackers = new ArrayList<>(create("bomber", 1));
    attackers.add(marine);

    assertThat(
            ProCasualtyUtils.findKeepLandSwap(
                List.of(marine), attackers, List.of(marine), Set.of(), cost))
        .isEmpty();
  }

  @Test
  void noSwapForAnAirUnitTheseHitsOnlyDamage() {
    final Unit marine = create("marine", 1).get(0);
    final Unit bomber = create("bomber", 1).get(0);
    final List<Unit> attackers = List.of(bomber, marine);

    assertThat(
            ProCasualtyUtils.findKeepLandSwap(
                List.of(marine), attackers, attackers, Set.of(bomber), cost))
        .isEmpty();
  }

  @Test
  void aTradedTwoPuTerritoryIsWorthLessThanAFighterOverAnInfantry() {
    final double value = ProCasualtyUtils.captureValue(2, 0, false, 0, false, 30, false, false);

    assertThat(value).isLessThan(10 - 3);
  }

  @Test
  void aTradedFactoryAnAllyCanRetakeIsWorthLessThanAFighterOverAnInfantry() {
    final double value = ProCasualtyUtils.captureValue(2, 2, true, 0, false, 30, false, true);

    assertThat(value).isLessThan(10 - 3);
  }

  @Test
  void aFactoryOnlyTheOwnerCanRetakeDeniesItsPlacement() {
    final double value = ProCasualtyUtils.captureValue(16, 8, false, 0, false, 60, false, false);

    assertThat(value).isEqualTo(16 + 8 + 0.2 * 16);
  }

  @Test
  void aCapitalIsWorthItsTreasuryEvenIfRetaken() {
    final double value = ProCasualtyUtils.captureValue(10, 0, true, 80, true, 40, false, true);

    assertThat(value).isEqualTo(10 + 80 + 0.2 * (10 + 5 + 40));
  }
}
