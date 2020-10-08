package games.strategy.triplea.delegate.power.calculator;

import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

class TotalPowerAndTotalRollsTest {

  @Nested
  @ExtendWith(MockitoExtension.class)
  class SortAaHighToLowTest {

    private final GameData gameData = TestMapGameData.LHTR.getGameData();

    @Mock private Unit unit1;
    @Mock private Unit unit2;
    @Mock private Unit unit3;
    @Mock private Unit unit4;
    @Mock private Unit unit5;

    private final List<Unit> units = new ArrayList<>();

    private UnitAttachment setupUnitAttachment(final Unit unit) {
      final UnitType unitTypeMock = mock(UnitType.class);
      final UnitAttachment unitAttachment = mock(UnitAttachment.class);
      when(unitTypeMock.getAttachment(any())).thenReturn(unitAttachment);
      when(unit.getType()).thenReturn(unitTypeMock);
      return unitAttachment;
    }

    @BeforeEach
    void setUp() {
      units.addAll(List.of(unit1, unit2, unit3, unit4, unit5));
    }

    @Test
    void testAttacking() {
      int index = 4;
      for (final var unit : units) {
        final var unitAttachment = setupUnitAttachment(unit);
        // We're integer dividing the index at this point to get duplicate sorting keys
        // in order to reach some edge cases
        when(unitAttachment.getOffensiveAttackAa(any())).thenReturn(index / 2);
        index--;
      }
      TotalPowerAndTotalRolls.sortAaHighToLow(units, gameData, false, new HashMap<>());
      assertThat(units.get(0), is(unit1));
      assertThat(units.get(1), is(unit2));
      assertThat(units.get(2), is(unit3));
      assertThat(units.get(3), is(unit4));
      assertThat(units.get(4), is(unit5));
    }

    @Test
    void testDefending() {
      int index = 0;
      for (final var unit : units) {
        final var unitAttachment = setupUnitAttachment(unit);
        // We're integer dividing the index at this point to get duplicate sorting keys
        // in order to reach some edge cases
        when(unitAttachment.getAttackAa(any())).thenReturn(index / 2);
        index++;
      }
      TotalPowerAndTotalRolls.sortAaHighToLow(units, gameData, true, new HashMap<>());
      assertThat(units.get(0), is(unit5));
      assertThat(units.get(1), is(unit3));
      assertThat(units.get(2), is(unit4));
      assertThat(units.get(3), is(unit1));
      assertThat(units.get(4), is(unit2));
    }
  }

  @Test
  void testGetTotalPowerForSupportBonusTypeCount() {
    final GameData twwGameData = TestMapGameData.TWW.getGameData();

    // Move regular units
    final GamePlayer germans = GameDataTestUtil.germany(twwGameData);
    final Territory berlin = territory("Berlin", twwGameData);
    final List<Unit> attackers = new ArrayList<>();

    attackers.addAll(GameDataTestUtil.germanInfantry(twwGameData).create(1, germans));
    attackers.addAll(GameDataTestUtil.germanArtillery(twwGameData).create(1, germans));
    int attackPower =
        TotalPowerAndTotalRolls.getTotalPower(
            TotalPowerAndTotalRolls.getUnitPowerAndRollsForNormalBattles(
                attackers,
                new ArrayList<>(),
                attackers,
                false,
                twwGameData,
                berlin,
                new ArrayList<>()),
            twwGameData);
    assertEquals(attackPower, 6, "1 artillery should provide +1 support to the infantry");

    attackers.addAll(GameDataTestUtil.germanArtillery(twwGameData).create(1, germans));
    attackPower =
        TotalPowerAndTotalRolls.getTotalPower(
            TotalPowerAndTotalRolls.getUnitPowerAndRollsForNormalBattles(
                attackers,
                new ArrayList<>(),
                attackers,
                false,
                twwGameData,
                berlin,
                new ArrayList<>()),
            twwGameData);
    assertEquals(
        attackPower,
        10,
        "2 artillery should provide +2 support to the infantry as stack count is 2");

    attackers.addAll(GameDataTestUtil.germanArtillery(twwGameData).create(1, germans));
    attackPower =
        TotalPowerAndTotalRolls.getTotalPower(
            TotalPowerAndTotalRolls.getUnitPowerAndRollsForNormalBattles(
                attackers,
                new ArrayList<>(),
                attackers,
                false,
                twwGameData,
                berlin,
                new ArrayList<>()),
            twwGameData);
    assertEquals(
        attackPower,
        13,
        "3 artillery should provide +2 support to the infantry as can't provide more than 2");
  }
}
