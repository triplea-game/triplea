package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.bomber;
import static games.strategy.triplea.delegate.GameDataTestUtil.british;
import static games.strategy.triplea.delegate.GameDataTestUtil.fighter;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.getDelegateBridge;
import static games.strategy.triplea.delegate.GameDataTestUtil.makeGameLowLuck;
import static games.strategy.triplea.delegate.GameDataTestUtil.setSelectAACasualties;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.random.ScriptedRandomSource;
import games.strategy.net.GUID;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.delegate.dataObjects.CasualtyList;
import games.strategy.triplea.util.DummyTripleAPlayer;
import games.strategy.triplea.xml.LoadGameUtil;
import games.strategy.util.Match;

public class BattleCalculatorTest {
  private ITestDelegateBridge m_bridge;

  @Before
  public void setUp() throws Exception {
    final GameData data = LoadGameUtil.loadTestGame("revised_test.xml");
    m_bridge = getDelegateBridge(british(data), data);
  }

  @Test
  public void testAACasualtiesLowLuck() {
    final GameData data = m_bridge.getData();
    makeGameLowLuck(data);
    setSelectAACasualties(data, false);
    final DiceRoll roll = new DiceRoll(new int[] {0}, 1, 1, false);
    final Collection<Unit> planes = bomber(data).create(5, british(data));
    final Collection<Unit> defendingAA = territory("Germany", data).getUnits().getMatches(Matches.UnitIsAAforAnything);
    final ScriptedRandomSource randomSource = new ScriptedRandomSource(new int[] {0, ScriptedRandomSource.ERROR});
    m_bridge.setRandomSource(randomSource);
    final Collection<Unit> casualties = BattleCalculator.getAACasualties(false, planes, planes, defendingAA,
        defendingAA, roll, m_bridge, null, null, null, territory("Germany", data), null, false, null).getKilled();
    assertEquals(casualties.size(), 1);
    assertEquals(1, randomSource.getTotalRolled());
  }

  @Test
  public void testAACasualtiesLowLuckDifferentMovementLetf() {
    final GameData data = m_bridge.getData();
    makeGameLowLuck(data);
    setSelectAACasualties(data, false);
    final DiceRoll roll = new DiceRoll(new int[] {0}, 1, 1, false);
    final List<Unit> planes = bomber(data).create(5, british(data));
    final Collection<Unit> defendingAA = territory("Germany", data).getUnits().getMatches(Matches.UnitIsAAforAnything);
    final ScriptedRandomSource randomSource = new ScriptedRandomSource(new int[] {0, ScriptedRandomSource.ERROR});
    m_bridge.setRandomSource(randomSource);
    TripleAUnit.get(planes.get(0)).setAlreadyMoved(1);
    final Collection<Unit> casualties = BattleCalculator.getAACasualties(false, planes, planes, defendingAA,
        defendingAA, roll, m_bridge, null, null, null, territory("Germany", data), null, false, null).getKilled();
    assertEquals(casualties.size(), 1);
  }

  @Test
  public void testAACasualtiesLowLuckMixed() {
    final GameData data = m_bridge.getData();
    makeGameLowLuck(data);
    setSelectAACasualties(data, false);
    // 6 bombers and 6 fighters
    final Collection<Unit> planes = bomber(data).create(6, british(data));
    planes.addAll(fighter(data).create(6, british(data)));
    final Collection<Unit> defendingAA = territory("Germany", data).getUnits().getMatches(Matches.UnitIsAAforAnything);
    // don't allow rolling, 6 of each is deterministic
    m_bridge.setRandomSource(new ScriptedRandomSource(new int[] {ScriptedRandomSource.ERROR}));
    final DiceRoll roll =
        DiceRoll
            .rollAA(
                Match.getMatches(planes,
                    Matches
                        .unitIsOfTypes(UnitAttachment.get(defendingAA.iterator().next().getType()).getTargetsAA(data))),
                defendingAA, m_bridge, territory("Germany", data), true);
    final Collection<Unit> casualties = BattleCalculator.getAACasualties(false, planes, planes, defendingAA,
        defendingAA, roll, m_bridge, null, null, null, territory("Germany", data), null, false, null).getKilled();
    assertEquals(casualties.size(), 2);
    // should be 1 fighter and 1 bomber
    assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber), 1);
    assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber.invert()), 1);
  }

  @Test
  public void testAACasualtiesLowLuckMixedMultipleDiceRolled() {
    final GameData data = m_bridge.getData();
    makeGameLowLuck(data);
    setSelectAACasualties(data, false);
    // 5 bombers and 5 fighters
    final Collection<Unit> planes = bomber(data).create(5, british(data));
    planes.addAll(fighter(data).create(5, british(data)));
    final Collection<Unit> defendingAA = territory("Germany", data).getUnits().getMatches(Matches.UnitIsAAforAnything);
    // should roll once, a hit
    final ScriptedRandomSource randomSource = new ScriptedRandomSource(new int[] {0, 1, 1, ScriptedRandomSource.ERROR});
    m_bridge.setRandomSource(randomSource);
    final DiceRoll roll =
        DiceRoll
            .rollAA(
                Match.getMatches(planes,
                    Matches
                        .unitIsOfTypes(UnitAttachment.get(defendingAA.iterator().next().getType()).getTargetsAA(data))),
                defendingAA, m_bridge, territory("Germany", data), true);
    assertEquals(1, randomSource.getTotalRolled());
    final Collection<Unit> casualties = BattleCalculator.getAACasualties(false, planes, planes, defendingAA,
        defendingAA, roll, m_bridge, null, null, null, territory("Germany", data), null, false, null).getKilled();
    assertEquals(casualties.size(), 2);
    // two extra rolls to pick which units are hit
    assertEquals(3, randomSource.getTotalRolled());
    // should be 1 fighter and 1 bomber
    assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber), 0);
    assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber.invert()), 2);
  }

  @Test
  public void testAACasualtiesLowLuckMixedWithChooseAACasualties() {
    final GameData data = m_bridge.getData();
    makeGameLowLuck(data);
    setSelectAACasualties(data, true);
    // 6 bombers and 6 fighters
    final Collection<Unit> planes = bomber(data).create(6, british(data));
    planes.addAll(fighter(data).create(6, british(data)));
    final Collection<Unit> defendingAA = territory("Germany", data).getUnits().getMatches(Matches.UnitIsAAforAnything);
    m_bridge.setRemote(new DummyTripleAPlayer() {
      @Override
      public CasualtyDetails selectCasualties(final Collection<Unit> selectFrom,
          final Map<Unit, Collection<Unit>> dependents, final int count, final String message, final DiceRoll dice,
          final PlayerID hit, final Collection<Unit> friendlyUnits, final PlayerID enemyPlayer,
          final Collection<Unit> enemyUnits, final boolean amphibious, final Collection<Unit> amphibiousLandAttackers,
          final CasualtyList defaultCasualties, final GUID battleID, final Territory battlesite,
          final boolean allowMultipleHitsPerUnit) {
        final List<Unit> selected = Match.getNMatches(selectFrom, count, Matches.UnitIsStrategicBomber);
        return new CasualtyDetails(selected, new ArrayList<>(), false);
      }
    });
    // don't allow rolling, 6 of each is deterministic
    m_bridge.setRandomSource(new ScriptedRandomSource(new int[] {ScriptedRandomSource.ERROR}));
    final DiceRoll roll =
        DiceRoll
            .rollAA(
                Match.getMatches(planes,
                    Matches
                        .unitIsOfTypes(UnitAttachment.get(defendingAA.iterator().next().getType()).getTargetsAA(data))),
                defendingAA, m_bridge, territory("Germany", data), true);
    final Collection<Unit> casualties =
        BattleCalculator.getAACasualties(false, planes, planes, defendingAA, defendingAA, roll, m_bridge, germans(data),
            british(data), null, territory("Germany", data), null, false, null).getKilled();
    assertEquals(casualties.size(), 2);
    // we selected all bombers
    assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber), 2);
    assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber.invert()), 0);
  }

  @Test
  public void testAACasualtiesLowLuckMixedWithChooseAACasualtiesRoll() {
    final GameData data = m_bridge.getData();
    makeGameLowLuck(data);
    setSelectAACasualties(data, true);
    // 7 bombers and 7 fighters
    final Collection<Unit> planes = bomber(data).create(7, british(data));
    planes.addAll(fighter(data).create(7, british(data)));
    final Collection<Unit> defendingAA = territory("Germany", data).getUnits().getMatches(Matches.UnitIsAAforAnything);
    m_bridge.setRemote(new DummyTripleAPlayer() {
      @Override
      public CasualtyDetails selectCasualties(final Collection<Unit> selectFrom,
          final Map<Unit, Collection<Unit>> dependents, final int count, final String message, final DiceRoll dice,
          final PlayerID hit, final Collection<Unit> friendlyUnits, final PlayerID enemyPlayer,
          final Collection<Unit> enemyUnits, final boolean amphibious, final Collection<Unit> amphibiousLandAttackers,
          final CasualtyList defaultCasualties, final GUID battleID, final Territory battlesite,
          final boolean allowMultipleHitsPerUnit) {
        final List<Unit> selected = Match.getNMatches(selectFrom, count, Matches.UnitIsStrategicBomber);
        return new CasualtyDetails(selected, new ArrayList<>(), false);
      }
    });
    // only 1 roll, a hit
    m_bridge.setRandomSource(new ScriptedRandomSource(new int[] {0, ScriptedRandomSource.ERROR}));
    final DiceRoll roll =
        DiceRoll
            .rollAA(
                Match.getMatches(planes,
                    Matches
                        .unitIsOfTypes(UnitAttachment.get(defendingAA.iterator().next().getType()).getTargetsAA(data))),
                defendingAA, m_bridge, territory("Germany", data), true);
    final Collection<Unit> casualties =
        BattleCalculator.getAACasualties(false, planes, planes, defendingAA, defendingAA, roll, m_bridge, germans(data),
            british(data), null, territory("Germany", data), null, false, null).getKilled();
    assertEquals(casualties.size(), 3);
    // we selected all bombers
    assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber), 3);
    assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber.invert()), 0);
  }

  @Test
  public void testAACasualtiesLowLuckMixedWithRolling() {
    final GameData data = m_bridge.getData();
    makeGameLowLuck(data);
    setSelectAACasualties(data, false);
    // 7 bombers and 7 fighters
    // 2 extra units, roll once
    final Collection<Unit> planes = bomber(data).create(7, british(data));
    planes.addAll(fighter(data).create(7, british(data)));
    final Collection<Unit> defendingAA = territory("Germany", data).getUnits().getMatches(Matches.UnitIsAAforAnything);
    // one roll, a hit
    final ScriptedRandomSource randomSource = new ScriptedRandomSource(new int[] {0});
    m_bridge.setRandomSource(randomSource);
    final DiceRoll roll =
        DiceRoll
            .rollAA(
                Match.getMatches(planes,
                    Matches
                        .unitIsOfTypes(UnitAttachment.get(defendingAA.iterator().next().getType()).getTargetsAA(data))),
                defendingAA, m_bridge, territory("Germany", data), true);
    // make sure we rolled once
    assertEquals(1, randomSource.getTotalRolled());
    final Collection<Unit> casualties = BattleCalculator.getAACasualties(false, planes, planes, defendingAA,
        defendingAA, roll, m_bridge, null, null, null, territory("Germany", data), null, false, null).getKilled();
    assertEquals(casualties.size(), 3);
    // a second roll for choosing which unit
    assertEquals(2, randomSource.getTotalRolled());
    // should be 2 fighters and 1 bombers
    assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber), 1);
    assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber.invert()), 2);
  }

  @Test
  public void testAACasualtiesLowLuckMixedWithRollingMiss() {
    final GameData data = m_bridge.getData();
    makeGameLowLuck(data);
    setSelectAACasualties(data, false);
    // 7 bombers and 7 fighters
    // 2 extra units, roll once
    final Collection<Unit> planes = bomber(data).create(7, british(data));
    planes.addAll(fighter(data).create(7, british(data)));
    final Collection<Unit> defendingAA = territory("Germany", data).getUnits().getMatches(Matches.UnitIsAAforAnything);
    // one roll, a miss
    final ScriptedRandomSource randomSource =
        new ScriptedRandomSource(new int[] {2, 0, 0, 0, ScriptedRandomSource.ERROR});
    m_bridge.setRandomSource(randomSource);
    final DiceRoll roll =
        DiceRoll
            .rollAA(
                Match.getMatches(planes,
                    Matches
                        .unitIsOfTypes(UnitAttachment.get(defendingAA.iterator().next().getType()).getTargetsAA(data))),
                defendingAA, m_bridge, territory("Germany", data), true);
    // make sure we rolled once
    assertEquals(1, randomSource.getTotalRolled());
    final Collection<Unit> casualties = BattleCalculator.getAACasualties(false, planes, planes, defendingAA,
        defendingAA, roll, m_bridge, null, null, null, territory("Germany", data), null, false, null).getKilled();
    assertEquals(casualties.size(), 2);
    assertEquals(4, randomSource.getTotalRolled());
    // should be 1 fighter and 1 bomber
    assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber), 1);
    assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber.invert()), 1);
  }

  @Test
  public void testAACasualtiesLowLuckMixedWithRollingForBombers() {
    final GameData data = m_bridge.getData();
    makeGameLowLuck(data);
    setSelectAACasualties(data, false);
    // 6 bombers, 7 fighters
    final Collection<Unit> planes = bomber(data).create(6, british(data));
    planes.addAll(fighter(data).create(7, british(data)));
    final Collection<Unit> defendingAA = territory("Germany", data).getUnits().getMatches(Matches.UnitIsAAforAnything);
    // 1 roll for the extra fighter
    final ScriptedRandomSource randomSource = new ScriptedRandomSource(new int[] {0, ScriptedRandomSource.ERROR});
    m_bridge.setRandomSource(randomSource);
    final DiceRoll roll =
        DiceRoll
            .rollAA(
                Match.getMatches(planes,
                    Matches
                        .unitIsOfTypes(UnitAttachment.get(defendingAA.iterator().next().getType()).getTargetsAA(data))),
                defendingAA, m_bridge, territory("Germany", data), true);
    // make sure we rolled once
    assertEquals(1, randomSource.getTotalRolled());
    final Collection<Unit> casualties = BattleCalculator.getAACasualties(false, planes, planes, defendingAA,
        defendingAA, roll, m_bridge, null, null, null, territory("Germany", data), null, false, null).getKilled();
    assertEquals(casualties.size(), 3);
    // should be 2 fighters and 1 bombers
    assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber), 1);
    assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber.invert()), 2);
  }
  // Radar AA tests removed, because "revised" does not have radar tech.
}
