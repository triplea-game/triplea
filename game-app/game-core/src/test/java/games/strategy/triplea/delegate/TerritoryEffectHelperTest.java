package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.xml.TestMapGameData;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TerritoryEffectHelperTest extends AbstractDelegateTestCase {
  private final GameState twwGameData = TestMapGameData.TWW.getGameData();
  private final GamePlayer germanPlayer = GameDataTestUtil.germany(twwGameData);
  private final Territory sicily = territory("Sicily", twwGameData);

  @Test
  void testGetMaxMovementCostZero() {
    final BigDecimal result =
        TerritoryEffectHelper.getMaxMovementCost(
            sicily,
            GameDataTestUtil.unitType("germanInfantry", twwGameData).create(1, germanPlayer));
    assertThat(result.compareTo(BigDecimal.ZERO))
        .as("Expect German infantry to have 0 movement cost for Sicily island territory effect")
        .isEqualTo(0);
  }

  @Test
  void testGetMaxMovementCostDecimal() {
    final BigDecimal result =
        TerritoryEffectHelper.getMaxMovementCost(
            sicily,
            GameDataTestUtil.unitType("germanAlpineInfantry", twwGameData).create(1, germanPlayer));
    assertThat(result.compareTo(new BigDecimal("0.5")))
        .as("Expect German alpine to have 0.5 movement cost for Sicily island territory effect")
        .isEqualTo(0);
  }

  @Test
  void testGetMaxMovementCostTwo() {
    final BigDecimal result =
        TerritoryEffectHelper.getMaxMovementCost(
            sicily,
            GameDataTestUtil.unitType("germanCombatEngineer", twwGameData).create(1, germanPlayer));
    assertThat(result.compareTo(new BigDecimal("2")))
        .as(
            "Expect German combat engineer to have 2 movement cost for Sicily island territory"
                + " effect")
        .isEqualTo(0);
  }

  @Test
  void testGetMaxMovementCostNoEffect() {
    final BigDecimal result =
        TerritoryEffectHelper.getMaxMovementCost(
            sicily, GameDataTestUtil.unitType("germanMarine", twwGameData).create(1, germanPlayer));
    assertThat(result.compareTo(BigDecimal.ONE))
        .as("Expect German marine to have 1 movement cost for no territory effects")
        .isEqualTo(0);
  }

  @Test
  void testGetMaxMovementCostMultipleUnits() {
    final Collection<Unit> units = new ArrayList<>();
    units.addAll(GameDataTestUtil.unitType("germanInfantry", twwGameData).create(1, germanPlayer));
    units.addAll(
        GameDataTestUtil.unitType("germanAlpineInfantry", twwGameData).create(1, germanPlayer));
    units.addAll(
        GameDataTestUtil.unitType("germanCombatEngineer", twwGameData).create(1, germanPlayer));
    units.addAll(GameDataTestUtil.unitType("germanMarine", twwGameData).create(1, germanPlayer));
    final BigDecimal result = TerritoryEffectHelper.getMaxMovementCost(sicily, units);
    assertThat(result.compareTo(new BigDecimal("2")))
        .as("Expect German units to have 2 movement cost as that is max across all units")
        .isEqualTo(0);
  }

  @Test
  void testGetMaxMovementCostForNoUnits() {
    final BigDecimal result = TerritoryEffectHelper.getMaxMovementCost(sicily, Set.of());
    assertThat(result.compareTo(BigDecimal.ONE))
        .as("Expect 1 movement cost when no units are passed in")
        .isEqualTo(0);
  }
}
