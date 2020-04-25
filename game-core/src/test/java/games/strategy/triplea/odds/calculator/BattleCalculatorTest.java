package games.strategy.triplea.odds.calculator;

import static games.strategy.triplea.delegate.GameDataTestUtil.americans;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.submarine;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.GameDataTestUtil.transport;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;

class BattleCalculatorTest {
  private GameData gameData = TestMapGameData.REVISED.getGameData();

  @Test
  void testUnbalancedFight() {
    final Territory germany = gameData.getMap().getTerritory("Germany");
    final Collection<Unit> defendingUnits = new ArrayList<>(germany.getUnits());
    final GamePlayer russians = GameDataTestUtil.russians(gameData);
    final GamePlayer germans = GameDataTestUtil.germans(gameData);
    final List<Unit> attackingUnits = GameDataTestUtil.infantry(gameData).create(100, russians);
    final List<Unit> bombardingUnits = List.of();
    final BattleCalculator calculator = new BattleCalculator();
    calculator.setGameData(gameData);
    final AggregateResults results =
        calculator.calculate(
            russians,
            germans,
            germany,
            attackingUnits,
            defendingUnits,
            bombardingUnits,
            TerritoryEffectHelper.getEffects(germany),
            200);
    assertTrue(results.getAttackerWinPercent() > 0.99);
    assertTrue(results.getDefenderWinPercent() < 0.1);
    assertTrue(results.getDrawPercent() < 0.1);
  }

  @Test
  void testKeepOneAttackingLand() {
    // 1 bomber and 1 infantry attacking
    // 1 fighter
    // if one attacking inf must live, the odds much worse
    final GamePlayer germans = GameDataTestUtil.germans(gameData);
    final GamePlayer british = GameDataTestUtil.british(gameData);
    final Territory eastCanada = gameData.getMap().getTerritory("Eastern Canada");
    final List<Unit> defendingUnits = GameDataTestUtil.fighter(gameData).create(1, british, false);
    final List<Unit> attackingUnits = GameDataTestUtil.infantry(gameData).create(1, germans, false);
    attackingUnits.addAll(GameDataTestUtil.bomber(gameData).create(1, germans, false));
    final List<Unit> bombardingUnits = List.of();
    final BattleCalculator calculator = new BattleCalculator();
    calculator.setGameData(gameData);
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
            1000);
    assertEquals(0.8, results.getAttackerWinPercent(), 0.10);
    assertEquals(0.16, results.getDefenderWinPercent(), 0.10);
  }

  @Test
  void testAttackingTransports() {
    final Territory sz1 = territory("1 Sea Zone", gameData);
    final List<Unit> attacking = transport(gameData).create(2, americans(gameData));
    final List<Unit> defending = submarine(gameData).create(2, germans(gameData));
    final BattleCalculator calculator = new BattleCalculator();
    calculator.setGameData(gameData);
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
            1);
    assertEquals(0.0, results.getAttackerWinPercent());
    assertEquals(1.0, results.getDefenderWinPercent());
  }

  @Test
  void testDefendingTransports() {
    // use v3 rule set
    gameData = TestMapGameData.WW2V3_1942.getGameData();
    final Territory sz1 = territory("1 Sea Zone", gameData);
    final List<Unit> attacking = submarine(gameData).create(2, americans(gameData));
    final List<Unit> defending = transport(gameData).create(2, germans(gameData));
    final BattleCalculator calculator = new BattleCalculator();
    calculator.setGameData(gameData);
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
            1);
    assertEquals(1.0, results.getAttackerWinPercent());
    assertEquals(0.0, results.getDefenderWinPercent());
  }
}
