package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.bomber;
import static games.strategy.triplea.delegate.GameDataTestUtil.british;
import static games.strategy.triplea.delegate.GameDataTestUtil.fighter;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.getDelegateBridge;
import static games.strategy.triplea.delegate.GameDataTestUtil.givePlayerRadar;
import static games.strategy.triplea.delegate.GameDataTestUtil.makeGameLowLuck;
import static games.strategy.triplea.delegate.GameDataTestUtil.setSelectAACasualties;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Unit;
import games.strategy.engine.random.ScriptedRandomSource;
import games.strategy.net.GUID;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.delegate.dataObjects.CasualtyList;
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
	private ITestDelegateBridge m_bridge;
	
	@Override
	protected void setUp() throws Exception
	{
		final GameData data = LoadGameUtil.loadGame("revised", "revised.xml");
		m_bridge = getDelegateBridge(british(data));
	}
	
	public void testAACasualtiesLowLuck()
	{
		final GameData data = m_bridge.getData();
		makeGameLowLuck(data);
		setSelectAACasualties(data, false);
		final DiceRoll roll = new DiceRoll(new int[] { 0 }, 1, 1, false);
		final Collection<Unit> planes = bomber(data).create(5, british(data));
		final ScriptedRandomSource randomSource = new ScriptedRandomSource(new int[] { 0, ScriptedRandomSource.ERROR });
		m_bridge.setRandomSource(randomSource);
		final Collection<Unit> casualties = BattleCalculator.getAACasualties(planes, roll, m_bridge, null, null, null, territory("Germany", data), Matches.UnitIsAAforAnything);
		assertEquals(casualties.size(), 1);
		assertEquals(1, randomSource.getTotalRolled());
	}
	
	public void testAACasualtiesLowLuckDifferentMovementLetf()
	{
		final GameData data = m_bridge.getData();
		makeGameLowLuck(data);
		setSelectAACasualties(data, false);
		final DiceRoll roll = new DiceRoll(new int[] { 0 }, 1, 1, false);
		final List<Unit> planes = bomber(data).create(5, british(data));
		final ScriptedRandomSource randomSource = new ScriptedRandomSource(new int[] { 0, ScriptedRandomSource.ERROR });
		m_bridge.setRandomSource(randomSource);
		TripleAUnit.get(planes.get(0)).setAlreadyMoved(1);
		final Collection<Unit> casualties = BattleCalculator.getAACasualties(planes, roll, m_bridge, null, null, null, territory("Germany", data), Matches.UnitIsAAforAnything);
		assertEquals(casualties.size(), 1);
	}
	
	public void testAACasualtiesLowLuckMixed()
	{
		final GameData data = m_bridge.getData();
		makeGameLowLuck(data);
		setSelectAACasualties(data, false);
		// 6 bombers and 6 fighters
		final Collection<Unit> planes = bomber(data).create(6, british(data));
		planes.addAll(fighter(data).create(6, british(data)));
		// don't allow rolling, 6 of each is deterministic
		m_bridge.setRandomSource(new ScriptedRandomSource(new int[] { ScriptedRandomSource.ERROR }));
		final DiceRoll roll = DiceRoll.rollAA(planes, m_bridge, territory("Germany", data), Matches.UnitIsAAforAnything);
		final Collection<Unit> casualties = BattleCalculator.getAACasualties(planes, roll, m_bridge, null, null, null, territory("Germany", data), Matches.UnitIsAAforAnything);
		assertEquals(casualties.size(), 2);
		// should be 1 fighter and 1 bomber
		assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber), 1);
		assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber.invert()), 1);
	}
	
	public void testAACasualtiesLowLuckMixedMultipleDiceRolled()
	{
		final GameData data = m_bridge.getData();
		makeGameLowLuck(data);
		setSelectAACasualties(data, false);
		// 5 bombers and 5 fighters
		final Collection<Unit> planes = bomber(data).create(5, british(data));
		planes.addAll(fighter(data).create(5, british(data)));
		// should roll once, a hit
		final ScriptedRandomSource randomSource = new ScriptedRandomSource(new int[] { 0, 1, 1, ScriptedRandomSource.ERROR });
		m_bridge.setRandomSource(randomSource);
		final DiceRoll roll = DiceRoll.rollAA(planes, m_bridge, territory("Germany", data), Matches.UnitIsAAforAnything);
		assertEquals(1, randomSource.getTotalRolled());
		final Collection<Unit> casualties = BattleCalculator.getAACasualties(planes, roll, m_bridge, null, null, null, territory("Germany", data), Matches.UnitIsAAforAnything);
		assertEquals(casualties.size(), 2);
		// two extra rolls to pick which units are hit
		assertEquals(3, randomSource.getTotalRolled());
		// should be 1 fighter and 1 bomber
		assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber), 0);
		assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber.invert()), 2);
	}
	
	public void testAACasualtiesLowLuckMixedWithChooseAACasualties()
	{
		final GameData data = m_bridge.getData();
		makeGameLowLuck(data);
		setSelectAACasualties(data, true);
		// 6 bombers and 6 fighters
		final Collection<Unit> planes = bomber(data).create(6, british(data));
		planes.addAll(fighter(data).create(6, british(data)));
		m_bridge.setRemote(new DummyTripleAPlayer()
		{
			@Override
			public CasualtyDetails selectCasualties(final Collection<Unit> selectFrom, final Map<Unit, Collection<Unit>> dependents, final int count, final String message, final DiceRoll dice,
						final PlayerID hit, final CasualtyList defaultCasualties, final GUID battleID)
			{
				final List<Unit> selected = Match.getNMatches(selectFrom, count, Matches.UnitIsStrategicBomber);
				return new CasualtyDetails(selected, new ArrayList<Unit>(), false);
			}
		});
		// don't allow rolling, 6 of each is deterministic
		m_bridge.setRandomSource(new ScriptedRandomSource(new int[] { ScriptedRandomSource.ERROR }));
		final DiceRoll roll = DiceRoll.rollAA(planes, m_bridge, territory("Germany", data), Matches.UnitIsAAforAnything);
		final Collection<Unit> casualties = BattleCalculator.getAACasualties(planes, roll, m_bridge, germans(data), british(data), null, territory("Germany", data), Matches.UnitIsAAforAnything);
		assertEquals(casualties.size(), 2);
		// we selected all bombers
		assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber), 2);
		assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber.invert()), 0);
	}
	
	public void testAACasualtiesLowLuckMixedWithChooseAACasualtiesRoll()
	{
		final GameData data = m_bridge.getData();
		makeGameLowLuck(data);
		setSelectAACasualties(data, true);
		// 7 bombers and 7 fighters
		final Collection<Unit> planes = bomber(data).create(7, british(data));
		planes.addAll(fighter(data).create(7, british(data)));
		m_bridge.setRemote(new DummyTripleAPlayer()
		{
			@Override
			public CasualtyDetails selectCasualties(final Collection<Unit> selectFrom, final Map<Unit, Collection<Unit>> dependents, final int count, final String message, final DiceRoll dice,
						final PlayerID hit, final CasualtyList defaultCasualties, final GUID battleID)
			{
				final List<Unit> selected = Match.getNMatches(selectFrom, count, Matches.UnitIsStrategicBomber);
				return new CasualtyDetails(selected, new ArrayList<Unit>(), false);
			}
		});
		// only 1 roll, a hit
		m_bridge.setRandomSource(new ScriptedRandomSource(new int[] { 0, ScriptedRandomSource.ERROR }));
		final DiceRoll roll = DiceRoll.rollAA(planes, m_bridge, territory("Germany", data), Matches.UnitIsAAforAnything);
		final Collection<Unit> casualties = BattleCalculator.getAACasualties(planes, roll, m_bridge, germans(data), british(data), null, territory("Germany", data), Matches.UnitIsAAforAnything);
		assertEquals(casualties.size(), 3);
		// we selected all bombers
		assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber), 3);
		assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber.invert()), 0);
	}
	
	public void testAACasualtiesLowLuckMixedWithRolling()
	{
		final GameData data = m_bridge.getData();
		makeGameLowLuck(data);
		setSelectAACasualties(data, false);
		// 7 bombers and 7 fighters
		// 2 extra units, roll once
		final Collection<Unit> planes = bomber(data).create(7, british(data));
		planes.addAll(fighter(data).create(7, british(data)));
		// one roll, a hit
		final ScriptedRandomSource randomSource = new ScriptedRandomSource(new int[] { 0 });
		m_bridge.setRandomSource(randomSource);
		final DiceRoll roll = DiceRoll.rollAA(planes, m_bridge, territory("Germany", data), Matches.UnitIsAAforAnything);
		// make sure we rolled once
		assertEquals(1, randomSource.getTotalRolled());
		final Collection<Unit> casualties = BattleCalculator.getAACasualties(planes, roll, m_bridge, null, null, null, territory("Germany", data), Matches.UnitIsAAforAnything);
		assertEquals(casualties.size(), 3);
		// a second roll for choosing which unit
		assertEquals(2, randomSource.getTotalRolled());
		// should be 2 fighters and 1 bombers
		assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber), 1);
		assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber.invert()), 2);
	}
	
	public void testAACasualtiesLowLuckMixedWithRollingMiss()
	{
		final GameData data = m_bridge.getData();
		makeGameLowLuck(data);
		setSelectAACasualties(data, false);
		// 7 bombers and 7 fighters
		// 2 extra units, roll once
		final Collection<Unit> planes = bomber(data).create(7, british(data));
		planes.addAll(fighter(data).create(7, british(data)));
		// one roll, a miss
		final ScriptedRandomSource randomSource = new ScriptedRandomSource(new int[] { 2 });
		m_bridge.setRandomSource(randomSource);
		final DiceRoll roll = DiceRoll.rollAA(planes, m_bridge, territory("Germany", data), Matches.UnitIsAAforAnything);
		// make sure we rolled once
		assertEquals(1, randomSource.getTotalRolled());
		final Collection<Unit> casualties = BattleCalculator.getAACasualties(planes, roll, m_bridge, null, null, null, territory("Germany", data), Matches.UnitIsAAforAnything);
		assertEquals(casualties.size(), 2);
		assertEquals(1, randomSource.getTotalRolled());
		// should be 1 fighter and 1 bomber
		assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber), 1);
		assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber.invert()), 1);
	}
	
	public void testAACasualtiesLowLuckMixedWithRollingForBombers()
	{
		final GameData data = m_bridge.getData();
		makeGameLowLuck(data);
		setSelectAACasualties(data, false);
		// 6 bombers, 7 fighters
		final Collection<Unit> planes = bomber(data).create(6, british(data));
		planes.addAll(fighter(data).create(7, british(data)));
		// 1 roll for the extra fighter
		final ScriptedRandomSource randomSource = new ScriptedRandomSource(new int[] { 0, ScriptedRandomSource.ERROR });
		m_bridge.setRandomSource(randomSource);
		final DiceRoll roll = DiceRoll.rollAA(planes, m_bridge, territory("Germany", data), Matches.UnitIsAAforAnything);
		// make sure we rolled once
		assertEquals(1, randomSource.getTotalRolled());
		final Collection<Unit> casualties = BattleCalculator.getAACasualties(planes, roll, m_bridge, null, null, null, territory("Germany", data), Matches.UnitIsAAforAnything);
		assertEquals(casualties.size(), 3);
		// should be 2 fighters and 1 bombers
		assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber), 1);
		assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber.invert()), 2);
	}
	
	public void testAACasualtiesLowLuckMixedRadar()
	{
		final GameData data = m_bridge.getData();
		makeGameLowLuck(data);
		setSelectAACasualties(data, false);
		givePlayerRadar(germans(data));
		// 3 bombers and 3 fighters
		final Collection<Unit> planes = bomber(data).create(3, british(data));
		planes.addAll(fighter(data).create(3, british(data)));
		// don't allow rolling, 6 of each is deterministic
		m_bridge.setRandomSource(new ScriptedRandomSource(new int[] { ScriptedRandomSource.ERROR }));
		final DiceRoll roll = DiceRoll.rollAA(planes, m_bridge, territory("Germany", data), Matches.UnitIsAAforAnything);
		final Collection<Unit> casualties = BattleCalculator.getAACasualties(planes, roll, m_bridge, null, null, null, territory("Germany", data), Matches.UnitIsAAforAnything);
		assertEquals(casualties.size(), 2);
		// should be 1 fighter and 1 bomber
		assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber), 1);
		assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber.invert()), 1);
	}
	
	public void testAACasualtiesLowLuckMixedWithRollingRadar()
	{
		final GameData data = m_bridge.getData();
		makeGameLowLuck(data);
		setSelectAACasualties(data, false);
		givePlayerRadar(germans(data));
		// 4 bombers and 4 fighters
		final Collection<Unit> planes = bomber(data).create(4, british(data));
		planes.addAll(fighter(data).create(4, british(data)));
		// 1 roll, a hit
		// then a dice to select the casualty
		final ScriptedRandomSource randomSource = new ScriptedRandomSource(new int[] { 0, 1 });
		m_bridge.setRandomSource(randomSource);
		final DiceRoll roll = DiceRoll.rollAA(planes, m_bridge, territory("Germany", data), Matches.UnitIsAAforAnything);
		// make sure we rolled once
		assertEquals(1, randomSource.getTotalRolled());
		final Collection<Unit> casualties = BattleCalculator.getAACasualties(planes, roll, m_bridge, null, null, null, territory("Germany", data), Matches.UnitIsAAforAnything);
		assertEquals(casualties.size(), 3);
		// should be 1 fighter and 2 bombers
		assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber), 2);
		assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber.invert()), 1);
	}
	
	public void testAACasualtiesLowLuckMixedWithRollingMissRadar()
	{
		final GameData data = m_bridge.getData();
		makeGameLowLuck(data);
		setSelectAACasualties(data, false);
		givePlayerRadar(germans(data));
		// 4 bombers and 4 fighters
		final Collection<Unit> planes = bomber(data).create(4, british(data));
		planes.addAll(fighter(data).create(4, british(data)));
		// 1 roll, a miss
		// then a dice to select the casualty
		final ScriptedRandomSource randomSource = new ScriptedRandomSource(new int[] { 5, ScriptedRandomSource.ERROR });
		m_bridge.setRandomSource(randomSource);
		final DiceRoll roll = DiceRoll.rollAA(planes, m_bridge, territory("Germany", data), Matches.UnitIsAAforAnything);
		assertEquals(roll.getHits(), 2);
		// make sure we rolled once
		assertEquals(1, randomSource.getTotalRolled());
		final Collection<Unit> casualties = BattleCalculator.getAACasualties(planes, roll, m_bridge, null, null, null, territory("Germany", data), Matches.UnitIsAAforAnything);
		assertEquals(casualties.size(), 2);
		// should be 1 fighter and 2 bombers
		assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber), 1);
		assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber.invert()), 1);
	}
}
