package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.*;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Unit;
import games.strategy.engine.random.ScriptedRandomSource;
import games.strategy.net.GUID;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.util.DummyTripleAPlayer;
import games.strategy.triplea.xml.LoadGameUtil;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

public class BattleCalculatorTest extends TestCase 
{
	
	private GameData m_data;

    @Override
    protected void setUp() throws Exception
    {        
        m_data = LoadGameUtil.loadGame("revised", "revised.xml");
    }
	
	public void testAACasualtiesLowLuck() 
	{
		makeGameLowLuck(m_data);
		setSelectAACasualties(m_data, false);
		
		DiceRoll roll = new DiceRoll(new int[] {0}, 1, 1, false);
		Collection<Unit> planes = bomber(m_data).create(5, british(m_data));
		Collection<Unit> casualties = BattleCalculator.getAACasualties(planes, roll, null, null, null, m_data, null, territory("Germany",m_data));
		assertEquals(casualties.size(), 1);
	}

	public void testAACasualtiesLowLuckMixed() 
	{
		makeGameLowLuck(m_data);
		setSelectAACasualties(m_data, false);
		
		//6 bombers and 6 fighters
		Collection<Unit> planes = bomber(m_data).create(6, british(m_data));
		planes.addAll(fighter(m_data).create(6, british(m_data)));

		ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
		//don't allow rolling, 6 of each is deterministic
		bridge.setRandomSource(new ScriptedRandomSource(new int[] {ScriptedRandomSource.ERROR}));
		
		DiceRoll roll = DiceRoll.rollAA(planes, bridge, territory("Germany", m_data), m_data);
		
		Collection<Unit> casualties = BattleCalculator.getAACasualties(planes, roll, null, null, null, m_data, null,  territory("Germany",m_data));
		assertEquals(casualties.size(), 2);
		
		//should be 1 fighter and 1 bomber
		assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber), 1);
		assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber.invert()), 1);
	}
	
	public void testAACasualtiesLowLuckMixedWithChooseAACasualties() 
	{
		makeGameLowLuck(m_data);
		setSelectAACasualties(m_data, true);
		
		//6 bombers and 6 fighters
		Collection<Unit> planes = bomber(m_data).create(6, british(m_data));
		planes.addAll(fighter(m_data).create(6, british(m_data)));

		ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
		bridge.setRemote(new DummyTripleAPlayer() {

			@Override
			public CasualtyDetails selectCasualties(
					Collection<Unit> selectFrom,
					Map<Unit, Collection<Unit>> dependents, int count,
					String message, DiceRoll dice, PlayerID hit,
					List<Unit> defaultCasualties, GUID battleID) {
				
				List<Unit> selected = Match.getNMatches(selectFrom, count, Matches.UnitIsStrategicBomber);
				return new CasualtyDetails(selected, new ArrayList<Unit>(), false);
				
			}
			
		});
		//don't allow rolling, 6 of each is deterministic
		bridge.setRandomSource(new ScriptedRandomSource(new int[] {ScriptedRandomSource.ERROR}));
		
		DiceRoll roll = DiceRoll.rollAA(planes, bridge, territory("Germany", m_data), m_data);		
		
		Collection<Unit> casualties = BattleCalculator.getAACasualties(planes, roll, bridge, germans(m_data), british(m_data), m_data, null,  territory("Germany",m_data));
		assertEquals(casualties.size(), 2);
		
		//we selected all bombers
		assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber), 2);
		assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber.invert()), 0);
	}
	
	public void testAACasualtiesLowLuckMixedWithChooseAACasualtiesRoll() 
	{
		makeGameLowLuck(m_data);
		setSelectAACasualties(m_data, true);
		
		//7 bombers and 7 fighters
		Collection<Unit> planes = bomber(m_data).create(7, british(m_data));
		planes.addAll(fighter(m_data).create(7, british(m_data)));

		ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
		bridge.setRemote(new DummyTripleAPlayer() {

			@Override
			public CasualtyDetails selectCasualties(
					Collection<Unit> selectFrom,
					Map<Unit, Collection<Unit>> dependents, int count,
					String message, DiceRoll dice, PlayerID hit,
					List<Unit> defaultCasualties, GUID battleID) {
				
				List<Unit> selected = Match.getNMatches(selectFrom, count, Matches.UnitIsStrategicBomber);
				return new CasualtyDetails(selected, new ArrayList<Unit>(), false);
				
			}
			
		});
		//only 1 roll, a hit
		bridge.setRandomSource(new ScriptedRandomSource(new int[] {0, ScriptedRandomSource.ERROR}));
		
		DiceRoll roll = DiceRoll.rollAA(planes, bridge, territory("Germany", m_data), m_data);		
		
		Collection<Unit> casualties = BattleCalculator.getAACasualties(planes, roll, bridge, germans(m_data), british(m_data), m_data, null,  territory("Germany",m_data));
		assertEquals(casualties.size(), 3);
		
		//we selected all bombers
		assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber), 3);
		assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber.invert()), 0);
	}
	
	public void testAACasualtiesLowLuckMixedWithRolling() 
	{
		makeGameLowLuck(m_data);
		setSelectAACasualties(m_data, false);
		
		//7 bombers and 7 fighters
		Collection<Unit> planes = bomber(m_data).create(7, british(m_data));
		planes.addAll(fighter(m_data).create(7, british(m_data)));

		ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
		//two rolls, both hits
		ScriptedRandomSource randomSource = new ScriptedRandomSource(new int[] {0,0});
		bridge.setRandomSource(randomSource);
		
		DiceRoll roll = DiceRoll.rollAA(planes, bridge, territory("Germany", m_data), m_data);
		
		//make sure we rolled twice
		assertEquals(2, randomSource.getTotalRolled());
		
		Collection<Unit> casualties = BattleCalculator.getAACasualties(planes, roll, null, null, null, m_data, null,  territory("Germany",m_data));
		assertEquals(casualties.size(), 4);
		
		//should be 2 fighters and 2 bombers
		assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber), 2);
		assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber.invert()), 2);
	}
	
	public void testAACasualtiesLowLuckMixedWithRollingMiss() 
	{
		makeGameLowLuck(m_data);
		setSelectAACasualties(m_data, false);
		
		//7 bombers and 7 fighters
		Collection<Unit> planes = bomber(m_data).create(7, british(m_data));
		planes.addAll(fighter(m_data).create(7, british(m_data)));

		ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
		//two rolls, both miss
		ScriptedRandomSource randomSource = new ScriptedRandomSource(new int[] {5,5});
		bridge.setRandomSource(randomSource);
		
		DiceRoll roll = DiceRoll.rollAA(planes, bridge, territory("Germany", m_data), m_data);
		
		//make sure we rolled twice
		assertEquals(2, randomSource.getTotalRolled());
		
		Collection<Unit> casualties = BattleCalculator.getAACasualties(planes, roll, null, null, null, m_data, null,  territory("Germany",m_data));
		assertEquals(casualties.size(), 2);
		
		//should be 2 fighters and 2 bombers
		assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber), 1);
		assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber.invert()), 1);
	}
	
	
	public void testAACasualtiesLowLuckMixedWithRollingForBombers() 
	{
		makeGameLowLuck(m_data);
		setSelectAACasualties(m_data, false);
		
		//6 bombers, 7 fighters
		Collection<Unit> planes = bomber(m_data).create(6, british(m_data));
		planes.addAll(fighter(m_data).create(7, british(m_data)));

		ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
		//1 roll for the extra fighter
		ScriptedRandomSource randomSource = new ScriptedRandomSource(new int[] {0,ScriptedRandomSource.ERROR});
		bridge.setRandomSource(randomSource);
		
		DiceRoll roll = DiceRoll.rollAA(planes, bridge, territory("Germany", m_data), m_data);
		
		//make sure we rolled once
		assertEquals(1, randomSource.getTotalRolled());
		
		Collection<Unit> casualties = BattleCalculator.getAACasualties(planes, roll, null, null, null, m_data, null,  territory("Germany",m_data));
		assertEquals(casualties.size(), 3);
		
		//should be 2 fighters and 1 bombers
		assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber), 1);
		assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber.invert()), 2);
	}
	
	public void testAACasualtiesLowLuckMixedRadar() 
	{
		makeGameLowLuck(m_data);
		setSelectAACasualties(m_data, false);
		givePlayerRadar(germans(m_data));
		
		//3 bombers and 3 fighters
		Collection<Unit> planes = bomber(m_data).create(3, british(m_data));
		planes.addAll(fighter(m_data).create(3, british(m_data)));

		ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
		//don't allow rolling, 6 of each is deterministic
		bridge.setRandomSource(new ScriptedRandomSource(new int[] {ScriptedRandomSource.ERROR}));
		
		DiceRoll roll = DiceRoll.rollAA(planes, bridge, territory("Germany", m_data), m_data);
		
		Collection<Unit> casualties = BattleCalculator.getAACasualties(planes, roll, null, null, null, m_data, null,  territory("Germany",m_data));
		assertEquals(casualties.size(), 2);
		
		//should be 1 fighter and 1 bomber
		assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber), 1);
		assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber.invert()), 1);
	}
	
	public void testAACasualtiesLowLuckMixedWithRollingRadar() 
	{
		makeGameLowLuck(m_data);
		setSelectAACasualties(m_data, false);
		givePlayerRadar(germans(m_data));
		
		//4 bombers and 4 fighters
		Collection<Unit> planes = bomber(m_data).create(4, british(m_data));
		planes.addAll(fighter(m_data).create(4, british(m_data)));

		ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
		//two rolls, both hits
		ScriptedRandomSource randomSource = new ScriptedRandomSource(new int[] {0,0});
		bridge.setRandomSource(randomSource);
		
		DiceRoll roll = DiceRoll.rollAA(planes, bridge, territory("Germany", m_data), m_data);
		
		//make sure we rolled twice
		assertEquals(2, randomSource.getTotalRolled());
		
		Collection<Unit> casualties = BattleCalculator.getAACasualties(planes, roll, null, null, null, m_data, null,  territory("Germany",m_data));
		assertEquals(casualties.size(), 4);
		
		//should be 2 fighters and 2 bombers
		assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber), 2);
		assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber.invert()), 2);
	}
	
	public void testAACasualtiesLowLuckMixedWithRollingMissRadar() 
	{
		makeGameLowLuck(m_data);
		setSelectAACasualties(m_data, false);
		givePlayerRadar(germans(m_data));
		
		//4 bombers and 4 fighters
		Collection<Unit> planes = bomber(m_data).create(4, british(m_data));
		planes.addAll(fighter(m_data).create(4, british(m_data)));


		ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
		//two rolls, both miss
		ScriptedRandomSource randomSource = new ScriptedRandomSource(new int[] {5,5});
		bridge.setRandomSource(randomSource);
		
		DiceRoll roll = DiceRoll.rollAA(planes, bridge, territory("Germany", m_data), m_data);
		
		//make sure we rolled twice
		assertEquals(2, randomSource.getTotalRolled());
		
		Collection<Unit> casualties = BattleCalculator.getAACasualties(planes, roll, null, null, null, m_data, null,  territory("Germany",m_data));
		assertEquals(casualties.size(), 2);
		
		//should be 2 fighters and 2 bombers
		assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber), 1);
		assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber.invert()), 1);
	}
	
	
}
