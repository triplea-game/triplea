package games.strategy.triplea.oddsCalculator.ta;

import static games.strategy.triplea.delegate.GameDataTestUtil.americans;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.submarine;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.GameDataTestUtil.transports;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.xml.LoadGameUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

public class OddsCalculatorTest extends TestCase
{
	
	private GameData m_data;
	
	@Override
	protected void setUp() throws Exception
	{
		m_data = LoadGameUtil.loadGame("revised", "revised.xml");
	}
	
	@Override
	protected void tearDown() throws Exception
	{
		m_data = null;
	}
	
	public void testUnbalancedFight()
	{
		Territory germany = m_data.getMap().getTerritory("Germany");
		List<Unit> defendingUnits = new ArrayList<Unit>(germany.getUnits().getUnits());
		PlayerID russians = m_data.getPlayerList().getPlayerID("Russians");
		PlayerID germans = m_data.getPlayerList().getPlayerID("Germans");
		List<Unit> attackingUnits = m_data.getUnitTypeList().getUnitType("infantry").create(100, russians);
		List<Unit> bombardingUnits = Collections.emptyList();
		
		OddsCalculator calculator = new OddsCalculator();
		AggregateResults results = calculator.calculate(m_data, russians, germans, germany, attackingUnits, defendingUnits, bombardingUnits, 5000);
		
		assertTrue(results.getAttackerWinPercent() > 0.99);
		assertTrue(results.getDefenderWinPercent() < 0.1);
		assertTrue(results.getDrawPercent() < 0.1);
	}
	
	public void testBalancedFight()
	{
		// 1 british tank in eastern canada, defending one german tank
		// odds for win/loss/tie are all equal
		
		Territory eastCanada = m_data.getMap().getTerritory("Eastern Canada");
		List<Unit> defendingUnits = new ArrayList<Unit>(eastCanada.getUnits().getUnits());
		
		PlayerID germans = m_data.getPlayerList().getPlayerID("Germans");
		PlayerID british = m_data.getPlayerList().getPlayerID("British");
		List<Unit> attackingUnits = m_data.getUnitTypeList().getUnitType("armour").create(1, germans, false);
		List<Unit> bombardingUnits = Collections.emptyList();
		
		OddsCalculator calculator = new OddsCalculator();
		AggregateResults results = calculator.calculate(m_data, germans, british, eastCanada, attackingUnits, defendingUnits, bombardingUnits, 10000);
		
		assertEquals(0.33, results.getAttackerWinPercent(), 0.05);
		assertEquals(0.33, results.getDefenderWinPercent(), 0.05);
		assertEquals(0.33, results.getDrawPercent(), 0.05);
		
	}
	
	public void testKeepOneAttackingLand()
	{
		// 1 bomber and 1 infantry attacking
		// 1 fighter
		// if one attacking inf must live, the odds
		// much worse
		
		PlayerID germans = m_data.getPlayerList().getPlayerID("Germans");
		PlayerID british = m_data.getPlayerList().getPlayerID("British");
		
		Territory eastCanada = m_data.getMap().getTerritory("Eastern Canada");
		List<Unit> defendingUnits = m_data.getUnitTypeList().getUnitType("fighter").create(1, british, false);
		
		List<Unit> attackingUnits = m_data.getUnitTypeList().getUnitType("infantry").create(1, germans, false);
		attackingUnits.addAll(m_data.getUnitTypeList().getUnitType("bomber").create(1, germans, false));
		List<Unit> bombardingUnits = Collections.emptyList();
		
		OddsCalculator calculator = new OddsCalculator();
		calculator.setKeepOneAttackingLandUnit(true);
		AggregateResults results = calculator.calculate(m_data, germans, british, eastCanada, attackingUnits, defendingUnits, bombardingUnits, 10000);
		
		assertEquals(0.8, results.getAttackerWinPercent(), 0.02);
		assertEquals(0.16, results.getDefenderWinPercent(), 0.02);
		
	}
	
	public void testNoUnitsInTerritory()
	{
		// fight 1 tank against 1 tank,
		// where none of the defending units are in the territry
		// and we ignore some units that are in the territory
		
		Territory uk = m_data.getMap().getTerritory("United Kingdom");
		
		PlayerID germans = m_data.getPlayerList().getPlayerID("Germans");
		List<Unit> attackingUnits = m_data.getUnitTypeList().getUnitType("armour").create(1, germans);
		List<Unit> bombardingUnits = Collections.emptyList();
		
		PlayerID british = m_data.getPlayerList().getPlayerID("British");
		List<Unit> defendingUnits = m_data.getUnitTypeList().getUnitType("armour").create(1, british);
		
		OddsCalculator calculator = new OddsCalculator();
		AggregateResults results = calculator.calculate(m_data, germans, british, uk, attackingUnits, defendingUnits, bombardingUnits, 5000);
		
		assertEquals(0.33, results.getAttackerWinPercent(), 0.05);
		assertEquals(0.33, results.getDefenderWinPercent(), 0.05);
		assertEquals(0.33, results.getDrawPercent(), 0.05);
	}
	
	public void testSeaBattleWithTransport()
	{
		
		// Attack a battleship with a battleship and a transport
		
		Territory sz2 = m_data.getMap().getTerritory("2 Sea Zone");
		
		PlayerID germans = m_data.getPlayerList().getPlayerID("Germans");
		List<Unit> attackingUnits = m_data.getUnitTypeList().getUnitType("battleship").create(1, germans);
		attackingUnits.addAll(m_data.getUnitTypeList().getUnitType("transport").create(1, germans));
		List<Unit> bombardingUnits = Collections.emptyList();
		
		PlayerID british = m_data.getPlayerList().getPlayerID("British");
		List<Unit> defendingUnits = m_data.getUnitTypeList().getUnitType("battleship").create(1, british);
		
		OddsCalculator calc = new OddsCalculator();
		AggregateResults results = calc.calculate(m_data, germans, british, sz2, attackingUnits, defendingUnits, bombardingUnits, 1000);
		
		assertTrue(results.getAttackerWinPercent() > 0.65);
	}
	
	public void testSubInfLoop()
	{
		m_data = LoadGameUtil.loadGame("AA50", "ww2v3_1942.xml");
		
		Territory sz1 = territory("1 Sea Zone", m_data);
		List<Unit> attacking = submarine(m_data).create(2, americans(m_data));
		List<Unit> defending = submarine(m_data).create(2, germans(m_data));
		
		OddsCalculator calc = new OddsCalculator();
		calc.setKeepOneAttackingLandUnit(false);
		calc.calculate(m_data, americans(m_data), germans(m_data), sz1, attacking, defending, Collections.<Unit> emptyList(), 5000);
		
	}
	
	public void testAttackingTransports()
	{
		m_data = LoadGameUtil.loadGame("AA50", "ww2v3_1942.xml");
		
		Territory sz1 = territory("1 Sea Zone", m_data);
		List<Unit> attacking = transports(m_data).create(2, americans(m_data));
		List<Unit> defending = submarine(m_data).create(2, germans(m_data));
		
		OddsCalculator calc = new OddsCalculator();
		calc.setKeepOneAttackingLandUnit(false);
		AggregateResults results = calc.calculate(m_data, americans(m_data), germans(m_data), sz1, attacking, defending, Collections.<Unit> emptyList(), 1);
		assertEquals(results.getAttackerWinPercent(), 0.0);
		assertEquals(results.getDefenderWinPercent(), 1.0);
		
	}
	
	public void testDefendingTransports()
	{
		m_data = LoadGameUtil.loadGame("AA50", "ww2v3_1942.xml");
		
		Territory sz1 = territory("1 Sea Zone", m_data);
		List<Unit> attacking = submarine(m_data).create(2, americans(m_data));
		List<Unit> defending = transports(m_data).create(2, germans(m_data));
		
		OddsCalculator calc = new OddsCalculator();
		calc.setKeepOneAttackingLandUnit(false);
		AggregateResults results = calc.calculate(m_data, americans(m_data), germans(m_data), sz1, attacking, defending, Collections.<Unit> emptyList(), 1);
		assertEquals(results.getAttackerWinPercent(), 1.0);
		assertEquals(results.getDefenderWinPercent(), 0.0);
		
	}
	
}
