package games.strategy.triplea.oddsCalculator.ta;

import static games.strategy.triplea.delegate.GameDataTestUtil.americans;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.submarine;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.GameDataTestUtil.transport;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.oddscalc.OddsCalculatorParameters;
import games.strategy.triplea.xml.TestMapGameData;

public class OddsCalculatorTest {
  private GameData gameData;

  @BeforeEach
  public void setUp() throws Exception {
    gameData = TestMapGameData.REVISED.getGameData();
  }

  @Test
  public void testUnbalancedFight() {
    final Territory germany = gameData.getMap().getTerritory("Germany");
    final PlayerID russians = GameDataTestUtil.russians(gameData);

    final AggregateResults results = new OddsCalculator().calculate(OddsCalculatorParameters.builder()
        .location(germany)
        .defender(GameDataTestUtil.germans(gameData))
        .defending(new ArrayList<>(germany.getUnits().getUnits()))
        .attacker(GameDataTestUtil.russians(gameData))
        .attacking(GameDataTestUtil.armour(gameData).create(50, russians))
        .bombarding(Collections.emptyList())
        .territoryEffects(TerritoryEffectHelper.getEffects(germany))
        .runCount(20)
        .keepOneAttackingLandUnit(false)
        .gameData(gameData)
        .build());

    assertTrue(results.getAttackerWinPercent() > 0.99, "attacker win percentage: " + results.getAttackerWinPercent());
    assertTrue(results.getDefenderWinPercent() < 0.1, "defender win percentage: " + results.getDefenderWinPercent());
    assertTrue(results.getDrawPercent() < 0.1, "draw percentage: " + results.getDrawPercent());
  }

  @Test
  public void testKeepOneAttackingLand() {
    // 1 bomber and 1 infantry attacking
    // 1 fighter
    // if one attacking inf must live, the odds much worse
    final PlayerID germans = GameDataTestUtil.germans(gameData);
    final PlayerID british = GameDataTestUtil.british(gameData);
    final Territory eastCanada = gameData.getMap().getTerritory("Eastern Canada");
    final List<Unit> attackingUnits = GameDataTestUtil.infantry(gameData).create(1, germans, false);
    attackingUnits.addAll(GameDataTestUtil.bomber(gameData).create(1, germans, false));

    final AggregateResults results = new OddsCalculator().calculate(OddsCalculatorParameters.builder()
        .attacker(GameDataTestUtil.germans(gameData))
        .defender(GameDataTestUtil.british(gameData))
        .location(eastCanada)
        .attacking(attackingUnits)
        .defending(GameDataTestUtil.fighter(gameData).create(1, british, false))
        .bombarding(Collections.emptyList())
        .territoryEffects(TerritoryEffectHelper.getEffects(eastCanada))
        .runCount(100)
        .keepOneAttackingLandUnit(true)
        .gameData(gameData)
        .build());

    assertEquals(0.8, results.getAttackerWinPercent(), 0.20);
    assertEquals(0.28, results.getDefenderWinPercent(), 0.20);
  }

  @Test
  public void testAttackingTransports() {
    final Territory sz1 = territory("1 Sea Zone", gameData);
    final List<Unit> attacking = transport(gameData).create(2, americans(gameData));
    final List<Unit> defending = submarine(gameData).create(2, germans(gameData));
    final IOddsCalculator calculator = new OddsCalculator();

    final AggregateResults results = calculator.calculate(OddsCalculatorParameters.builder()
        .attacker(americans(gameData))
        .defender(germans(gameData))
        .location(sz1)
        .attacking(attacking)
        .defending(defending)
        .bombarding(Collections.emptyList())
        .territoryEffects(TerritoryEffectHelper.getEffects(sz1))
        .runCount(1)
        .keepOneAttackingLandUnit(false)
        .gameData(gameData)
        .build());
    assertEquals(results.getAttackerWinPercent(), 0.0);
    assertEquals(results.getDefenderWinPercent(), 1.0);
  }

  @Test
  public void testDefendingTransports() throws Exception {
    // use v3 rule set
    gameData = TestMapGameData.WW2V3_1942.getGameData();
    final Territory sz1 = territory("1 Sea Zone", gameData);
    final List<Unit> attacking = submarine(gameData).create(2, americans(gameData));
    final List<Unit> defending = transport(gameData).create(2, germans(gameData));
    final IOddsCalculator calculator = new OddsCalculator();

    final AggregateResults results = calculator.calculate(OddsCalculatorParameters.builder()
        .attacker(americans(gameData))
        .defender(germans(gameData))
        .location(sz1)
        .attacking(attacking)
        .defending(defending)
        .bombarding(Collections.emptyList())
        .territoryEffects(TerritoryEffectHelper.getEffects(sz1))
        .runCount(1)
        .keepOneAttackingLandUnit(false)
        .gameData(gameData)
        .build());
    assertEquals(results.getAttackerWinPercent(), 1.0);
    assertEquals(results.getDefenderWinPercent(), 0.0);
  }
}
