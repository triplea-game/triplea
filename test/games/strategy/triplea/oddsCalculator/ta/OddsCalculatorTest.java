package games.strategy.triplea.oddsCalculator.ta;

import static games.strategy.triplea.delegate.GameDataTestUtil.americans;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.submarine;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.GameDataTestUtil.transport;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.xml.LoadGameUtil;

public class OddsCalculatorTest {
  private GameData m_data;

  @Before
  public void setUp() throws Exception {
    m_data = LoadGameUtil.loadTestGame("revised_test.xml");
  }

  @Test
  public void testUnbalancedFight() {
    final Territory germany = m_data.getMap().getTerritory("Germany");
    final List<Unit> defendingUnits = new ArrayList<>(germany.getUnits().getUnits());
    final PlayerID russians = GameDataTestUtil.russians(m_data);
    final PlayerID germans = GameDataTestUtil.germans(m_data);
    final List<Unit> attackingUnits = GameDataTestUtil.infantry(m_data).create(100, russians);
    final List<Unit> bombardingUnits = Collections.emptyList();
    final IOddsCalculator calculator = new OddsCalculator(m_data);
    final AggregateResults results = calculator.setCalculateDataAndCalculate(russians, germans, germany, attackingUnits,
        defendingUnits, bombardingUnits, TerritoryEffectHelper.getEffects(germany), 200);
    calculator.shutdown();
    assertTrue(results.getAttackerWinPercent() > 0.99);
    assertTrue(results.getDefenderWinPercent() < 0.1);
    assertTrue(results.getDrawPercent() < 0.1);
  }

  @Test
  public void testKeepOneAttackingLand() {
    // 1 bomber and 1 infantry attacking
    // 1 fighter
    // if one attacking inf must live, the odds
    // much worse
    final PlayerID germans = GameDataTestUtil.germans(m_data);
    final PlayerID british = GameDataTestUtil.british(m_data);
    final Territory eastCanada = m_data.getMap().getTerritory("Eastern Canada");
    final List<Unit> defendingUnits = GameDataTestUtil.fighter(m_data).create(1, british, false);
    final List<Unit> attackingUnits = GameDataTestUtil.infantry(m_data).create(1, germans, false);
    attackingUnits.addAll(GameDataTestUtil.bomber(m_data).create(1, germans, false));
    final List<Unit> bombardingUnits = Collections.emptyList();
    final OddsCalculator calculator = new OddsCalculator(m_data);
    calculator.setKeepOneAttackingLandUnit(true);
    final AggregateResults results = calculator.setCalculateDataAndCalculate(germans, british, eastCanada,
        attackingUnits, defendingUnits, bombardingUnits, TerritoryEffectHelper.getEffects(eastCanada), 1000);
    calculator.shutdown();
    assertEquals(0.8, results.getAttackerWinPercent(), 0.10);
    assertEquals(0.16, results.getDefenderWinPercent(), 0.10);
  }

  @Test
  public void testAttackingTransports() {
    final Territory sz1 = territory("1 Sea Zone", m_data);
    final List<Unit> attacking = transport(m_data).create(2, americans(m_data));
    final List<Unit> defending = submarine(m_data).create(2, germans(m_data));
    final OddsCalculator calculator = new OddsCalculator(m_data);
    calculator.setKeepOneAttackingLandUnit(false);
    final AggregateResults results = calculator.setCalculateDataAndCalculate(americans(m_data), germans(m_data), sz1,
        attacking, defending, Collections.<Unit>emptyList(), TerritoryEffectHelper.getEffects(sz1), 1);
    calculator.shutdown();
    assertEquals(results.getAttackerWinPercent(), 0.0, 0.0);
    assertEquals(results.getDefenderWinPercent(), 1.0, 0.0);
  }

  @Test
  public void testDefendingTransports() {
    // use v3 rule set
    m_data = LoadGameUtil.loadTestGame("ww2v3_1942_test.xml");
    final Territory sz1 = territory("1 Sea Zone", m_data);
    final List<Unit> attacking = submarine(m_data).create(2, americans(m_data));
    final List<Unit> defending = transport(m_data).create(2, germans(m_data));
    final OddsCalculator calculator = new OddsCalculator(m_data);
    calculator.setKeepOneAttackingLandUnit(false);
    final AggregateResults results = calculator.setCalculateDataAndCalculate(americans(m_data), germans(m_data), sz1,
        attacking, defending, Collections.<Unit>emptyList(), TerritoryEffectHelper.getEffects(sz1), 1);
    calculator.shutdown();
    assertEquals(results.getAttackerWinPercent(), 1.0, 0.0);
    assertEquals(results.getDefenderWinPercent(), 0.0, 0.0);
  }
}
