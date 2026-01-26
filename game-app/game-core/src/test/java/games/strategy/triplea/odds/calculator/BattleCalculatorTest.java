package games.strategy.triplea.odds.calculator;

import static games.strategy.triplea.delegate.GameDataTestUtil.americans;
import static games.strategy.triplea.delegate.GameDataTestUtil.bomber;
import static games.strategy.triplea.delegate.GameDataTestUtil.british;
import static games.strategy.triplea.delegate.GameDataTestUtil.fighter;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.russians;
import static games.strategy.triplea.delegate.GameDataTestUtil.submarine;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.GameDataTestUtil.transport;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;

class BattleCalculatorTest extends AbstractClientSettingTestCase {
  @Test
  void testUnbalancedFight() {
    final GameData gameData = TestMapGameData.REVISED.getGameData();
    final Territory germany = gameData.getMap().getTerritoryOrNull("Germany");
    final Collection<Unit> defendingUnits = new ArrayList<>(germany.getUnits());
    final GamePlayer russians = russians(gameData);
    final GamePlayer germans = germans(gameData);
    final List<Unit> attackingUnits = infantry(gameData).create(100, russians);
    final List<Unit> bombardingUnits = List.of();
    final BattleCalculator calculator = new BattleCalculator(gameData);
    final AggregateResults results =
        calculator.calculate(
            russians,
            germans,
            germany,
            attackingUnits,
            defendingUnits,
            bombardingUnits,
            TerritoryEffectHelper.getEffects(germany),
            false,
            200);
    assertTrue(results.getAttackerWinPercent() > 0.99);
    assertTrue(results.getDefenderWinPercent() < 0.1);
    assertTrue(results.getDrawPercent() < 0.1);
  }

  @Test
  void testKeepOneAttackingLand() {
    final GameData gameData = TestMapGameData.REVISED.getGameData();
    // 1 bomber and 1 infantry attacking
    // 1 fighter
    // if one attacking inf must live, the odds much worse
    final GamePlayer germans = germans(gameData);
    final GamePlayer british = british(gameData);
    final Territory eastCanada = gameData.getMap().getTerritoryOrNull("Eastern Canada");
    final List<Unit> defendingUnits = fighter(gameData).create(1, british);
    final List<Unit> attackingUnits = infantry(gameData).create(1, germans);
    attackingUnits.addAll(bomber(gameData).create(1, germans));
    final List<Unit> bombardingUnits = List.of();
    final BattleCalculator calculator = new BattleCalculator(gameData);
    calculator.setKeepOneAttackingLandUnit(true);
    final AggregateResults results =
        calculator.calculate(
            germans,
            british,
            eastCanada,
            attackingUnits,
            defendingUnits,
            bombardingUnits,
            TerritoryEffectHelper.getEffects(eastCanada),
            false,
            1000);
    assertEquals(0.8, results.getAttackerWinPercent(), 0.10);
    assertEquals(0.16, results.getDefenderWinPercent(), 0.10);
  }

  @Test
  void testAttackingTransports() {
    final GameData gameData = TestMapGameData.REVISED.getGameData();
    final Territory sz1 = territory("1 Sea Zone", gameData);
    final List<Unit> attacking = transport(gameData).create(2, americans(gameData));
    final List<Unit> defending = submarine(gameData).create(2, germans(gameData));
    final BattleCalculator calculator = new BattleCalculator(gameData);
    calculator.setKeepOneAttackingLandUnit(false);
    final AggregateResults results =
        calculator.calculate(
            americans(gameData),
            germans(gameData),
            sz1,
            attacking,
            defending,
            List.of(),
            TerritoryEffectHelper.getEffects(sz1),
            false,
            1);
    assertEquals(0.0, results.getAttackerWinPercent());
    assertEquals(1.0, results.getDefenderWinPercent());
  }

  @Test
  void testDefendingTransports() {
    // use v3 rule set
    final GameData gameData = TestMapGameData.WW2V3_1942.getGameData();
    final Territory sz1 = territory("1 Sea Zone", gameData);
    final List<Unit> attacking = submarine(gameData).create(2, americans(gameData));
    final List<Unit> defending = transport(gameData).create(2, germans(gameData));
    final BattleCalculator calculator = new BattleCalculator(gameData);
    calculator.setKeepOneAttackingLandUnit(false);
    final AggregateResults results =
        calculator.calculate(
            americans(gameData),
            germans(gameData),
            sz1,
            attacking,
            defending,
            List.of(),
            TerritoryEffectHelper.getEffects(sz1),
            false,
            1);
    assertEquals(1.0, results.getAttackerWinPercent());
    assertEquals(0.0, results.getDefenderWinPercent());
  }
}
