package games.strategy.triplea.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.xml.LoadGameUtil;
import games.strategy.util.PropertyUtil;

class TestUnit {
  static final Map<TripleAUnit, TestUnit> m_map = new HashMap<>();
  TripleAUnit m_unit;
  String m_name;

  public static TripleAUnit createUnit(final String name, final UnitType type, final PlayerID owner) {
    final TripleAUnit u = (TripleAUnit) type.create(owner);
    add(name, u);
    return u;
  }

  public static TestUnit add(final String name, final Unit unit) {
    final TestUnit testUnit = new TestUnit(name, unit);
    m_map.put((TripleAUnit) unit, testUnit);
    return testUnit;
  }

  public static TestUnit get(final String name) {
    for (final TestUnit testUnit : m_map.values()) {
      if (testUnit.getName().equals(name)) {
        return testUnit;
      }
    }
    return null;
  }

  public static TestUnit get(final Unit unit) {
    return m_map.get(unit);
  }

  protected static Set<TestUnit> createSet(final Collection<Unit> units) {
    final Set<TestUnit> testUnitSet = new HashSet<>();
    for (final Unit unit : units) {
      TestUnit testUnit = TestUnit.get(unit);
      if (testUnit == null) {
        testUnit = new TestUnit(unit);
      }
      testUnitSet.add(testUnit);
    }
    return testUnitSet;
  }

  public TestUnit(final Unit unit) {
    m_unit = (TripleAUnit) unit;
    m_name = m_unit.getType().getName() + ":" + m_unit.hashCode();
  }

  public TestUnit(final String name, final Unit unit) {
    m_unit = (TripleAUnit) unit;
    m_name = name;
  }

  public String getName() {
    return m_name;
  }

  @Override
  public String toString() {
    return m_name;
  }

  @Override
  public int hashCode() {
    return m_unit.hashCode();
  }

  @Override
  public boolean equals(final Object o) {
    return m_unit.equals(o);
  }
}


public class UnitAutoChooserTest {
  private GameData m_data;
  private PlayerID british;
  private UnitType infantry;
  private UnitType armour;
  private UnitType transport;
  private UnitType battleship;

  @Before
  public void setUp() throws Exception {
    m_data = LoadGameUtil.loadTestGame("revised_test.xml");
    british = m_data.getPlayerList().getPlayerID(Constants.PLAYER_NAME_BRITISH);
    armour = m_data.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_ARMOUR);
    infantry = m_data.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_INFANTRY);
    transport = m_data.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_TRANSPORT);
    battleship = m_data.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_BATTLESHIP);
  }

  protected ITestDelegateBridge getDelegateBridge(final PlayerID player) {
    return GameDataTestUtil.getDelegateBridge(player, m_data);
  }

  private void loadTransport(final Map<Unit, Collection<Unit>> mustMoveWith, final Unit trn, final Unit... units) {
    // the transport determines which unit it is transporting by
    // looking at the units in the same territory it is in
    // so we must place the units in the same territory
    final Territory t = m_data.getMap().getTerritory("11 Sea Zone");
    ensureIn(t, trn);
    t.getUnits().getUnits();
    final List<Unit> transporting = new ArrayList<>();
    for (final Unit u : units) {
      ensureIn(t, u);
      transporting.add(u);
      PropertyUtil.set(TripleAUnit.TRANSPORTED_BY, trn, u);
    }
    mustMoveWith.put(trn, transporting);
    assertTrue(TripleAUnit.get(trn).getTransporting().size() == units.length);
    assertTrue(TripleAUnit.get(trn).getTransporting().containsAll(Arrays.asList(units)));
  }

  private void ensureIn(final Territory t, final Unit u) {
    // make sure the given unit is in the given territory
    if (t.getUnits().getUnits().contains(u)) {
      return;
    }
    final Change c = ChangeFactory.addUnits(t, Collections.singleton(u));
    m_data.performChange(c);
  }

  private void setUnits(final Collection<Unit> c, final Unit... objects) {
    c.clear();
    Collections.addAll(c, objects);
  }

  // The MovePanel and EditPanel use the UnitAutoChooser in this mode
  // when finding initial solutions
  @Test
  public void testUnitAutoChooserWithImplicitDependentsNoMovementCategorized() {
    final List<Unit> allUnits = new ArrayList<>();
    final List<Unit> chosenUnits = new ArrayList<>();
    final List<Unit> expectedCandidateUnits = new ArrayList<>();
    final List<Unit> expectedSelectedUnitsWithDependents = new ArrayList<>();
    final List<Unit> expectedSelectedUnits = new ArrayList<>();
    UnitAutoChooser autoChooser = null;
    final Map<Unit, Collection<Unit>> mustMoveWith = new HashMap<>();
    final boolean bImplicitDependents = true;
    final boolean bCategorizeMovement = false;
    // Setup units/dependencies
    final TripleAUnit bb1 = TestUnit.createUnit("bb1", battleship, british);
    final TripleAUnit t1 = TestUnit.createUnit("t1", transport, british);
    final TripleAUnit a1 = TestUnit.createUnit("a1", armour, british);
    final TripleAUnit i1 = TestUnit.createUnit("i1", infantry, british);
    loadTransport(mustMoveWith, t1, a1, i1);
    final TripleAUnit bb2 = TestUnit.createUnit("bb2", battleship, british);
    final TripleAUnit t2 = TestUnit.createUnit("t2", transport, british);
    final TripleAUnit a2 = TestUnit.createUnit("a2", armour, british);
    final TripleAUnit i2 = TestUnit.createUnit("i2", infantry, british);
    loadTransport(mustMoveWith, t2, a2, i2);
    // make this trn only have 1 movement left
    TripleAUnit.get(t2).setAlreadyMoved(1);
    final TripleAUnit t3 = TestUnit.createUnit("t3", transport, british);
    final TripleAUnit i3 = TestUnit.createUnit("i3", infantry, british);
    loadTransport(mustMoveWith, t3, i3);
    final TripleAUnit t4 = TestUnit.createUnit("t4", transport, british);
    final TripleAUnit a4 = TestUnit.createUnit("a4", armour, british);
    loadTransport(mustMoveWith, t4, a4);
    final TripleAUnit t5 = TestUnit.createUnit("t5", transport, british);
    final TripleAUnit t6 = TestUnit.createUnit("t6", transport, british);
    final TripleAUnit t7 = TestUnit.createUnit("t7", transport, british);
    final TripleAUnit i7 = TestUnit.createUnit("i7", infantry, british);
    final TripleAUnit I7 = TestUnit.createUnit("I7", infantry, british);
    loadTransport(mustMoveWith, t7, i7, I7);
    // BEGIN TESTS
    // implicitDependents:
    // YES
    // categorizeMovement:
    // NO
    // allUnits:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t4[a4, ],t5[ , ],t6[ , ],bb1
    // chosenUnits:
    // trn, trn, inf, inf, arm, arm, bb
    // candidateCompositeCategories:
    // [trn[arm,inf],trn[arm,inf]] => exact
    // candidateUnits:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t4[a4, ],t5[ , ],t6[ , ],bb1
    // exactSolutions:
    // t1[a1,i1], t2[a2,i2], bb1
    // greedySolutions:
    // none
    // incompleteSolution:
    // none
    // exactSolutionCount:
    // 1
    // solutionCount:
    // 1
    setUnits(allUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, t5, t6, bb1);
    setUnits(chosenUnits, t1, t5, a1, a4, i2, i3, bb1);
    autoChooser = new UnitAutoChooser(allUnits, chosenUnits, mustMoveWith, bImplicitDependents, bCategorizeMovement);
    assertEquals(1, autoChooser.exactSolutionCount());
    assertEquals(1, autoChooser.solutionCount());
    assertTrue(autoChooser.foundCompleteSolution());
    setUnits(expectedCandidateUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, t5, t6, bb1);
    assertEquals(TestUnit.createSet(expectedCandidateUnits), TestUnit.createSet(autoChooser.getCandidateUnits(true)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t2, a2, i2, bb1);
    setUnits(expectedSelectedUnits, t1, a1, i1, t2, a2, i2, bb1);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(0, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(0, false)));
    // implicitDependents:
    // YES
    // categorizeMovement:
    // NO
    // allUnits:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t4[a4, ],t5[ , ],t6[ , ],bb1,bb2
    // chosenUnits:
    // trn, trn, trn, inf, inf, arm, arm
    // candidateCompositeCategories:
    // [trn[inf,arm],trn[inf,arm],trn] => exact
    // [trn[inf,arm],trn[inf],trn[arm]] => exact
    // [trn[inf,arm],trn[inf,arm],trn[inf]] => greedy
    // [trn[inf,arm],trn[inf,arm],trn[arm]] => greedy
    // candidateUnits:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t4[a4, ],t5[ , ],t6[ , ]
    // exactSolutions:
    // t1[a1,i1],t2[a2,i2],t5[ , ]
    // t1[a1,i1],t3[ ,i3],t4[a4, ]
    // greedySolutions:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3]
    // t1[a1,i1],t2[a2,i2],t4[a4, ]
    // incompleteSolution:
    // none
    // exactSolutionCount:
    // 2
    // solutionCount:
    // 4
    setUnits(allUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, t5, t6, bb1, bb2);
    setUnits(chosenUnits, t1, a2, i2, t5, a1, i3, t6);
    autoChooser = new UnitAutoChooser(allUnits, chosenUnits, mustMoveWith, bImplicitDependents, bCategorizeMovement);
    assertEquals(2, autoChooser.exactSolutionCount());
    assertEquals(4, autoChooser.solutionCount());
    assertTrue(autoChooser.foundCompleteSolution());
    setUnits(expectedCandidateUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, t5, t6);
    assertEquals(TestUnit.createSet(expectedCandidateUnits), TestUnit.createSet(autoChooser.getCandidateUnits(true)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t2, a2, i2, t5);
    setUnits(expectedSelectedUnits, t1, a1, i1, t2, a2, i2, t5);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(0, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(0, false)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t3, i3, t4, a4);
    setUnits(expectedSelectedUnits, t1, a1, i1, t3, i3, t4, a4);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(1, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(1, false)));
    // setUnits(expectedSelectedUnitsWithDependents ,t1,a1,i1,t2,a2,i2,t3,i3);
    // setUnits(expectedSelectedUnits ,t1,a1,i1,t2,a2,i2,t3);
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t2, a2, i2, t4, a4);
    setUnits(expectedSelectedUnits, t1, a1, i1, t2, a2, i2, t4);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(2, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(2, false)));
    // setUnits(expectedSelectedUnitsWithDependents ,t1,a1,i1,t2,a2,i2,t4,a4);
    // setUnits(expectedSelectedUnits ,t1,a1,i1,t2,a2,i2,t4);
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t2, a2, i2, t3, i3);
    setUnits(expectedSelectedUnits, t1, a1, i1, t2, a2, i2, t3);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(3, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(3, false)));
    // implicitDependents:
    // YES
    // categorizeMovement:
    // NO
    // allUnits:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t4[a4, ],t5[ , ],t6[ , ],bb1,bb2
    // chosenUnits:
    // bb
    // candidateCompositeCategories:
    // none
    // candidateUnits:
    // bb1,bb2
    // exactSolutions:
    // bb1
    // greedySolutions:
    // none
    // incompleteSolutions:
    // none
    // exactSolutionCount:
    // 1
    // solutionCount:
    // 1
    setUnits(allUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, t5, t6, bb1, bb2);
    setUnits(chosenUnits, bb2);
    autoChooser = new UnitAutoChooser(allUnits, chosenUnits, mustMoveWith, bImplicitDependents, bCategorizeMovement);
    assertEquals(1, autoChooser.exactSolutionCount());
    assertEquals(1, autoChooser.solutionCount());
    assertTrue(autoChooser.foundCompleteSolution());
    setUnits(expectedCandidateUnits, bb1, bb2);
    assertEquals(TestUnit.createSet(expectedCandidateUnits), TestUnit.createSet(autoChooser.getCandidateUnits(true)));
    setUnits(expectedSelectedUnitsWithDependents, bb1);
    setUnits(expectedSelectedUnits, bb1);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(0, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(0, false)));
    // implicitDependents:
    // YES
    // categorizeMovement:
    // NO
    // allUnits:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t4[a4, ],t5[ , ],t6[ , ],bb1,bb2
    // chosenUnits:
    // bb,bb
    // candidateCompositeCategories:
    // none
    // candidateUnits:
    // bb1,bb2
    // exactSolutions:
    // bb1,bb2
    // greedySolutions:
    // none
    // incompleteSolution:
    // none
    // exactSolutionCount:
    // 1
    // solutionCount:
    // 1
    setUnits(allUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, t5, t6, bb1, bb2);
    setUnits(chosenUnits, bb2, bb1);
    autoChooser = new UnitAutoChooser(allUnits, chosenUnits, mustMoveWith, bImplicitDependents, bCategorizeMovement);
    assertEquals(1, autoChooser.exactSolutionCount());
    assertEquals(1, autoChooser.solutionCount());
    assertTrue(autoChooser.foundCompleteSolution());
    setUnits(expectedCandidateUnits, bb1, bb2);
    assertEquals(TestUnit.createSet(expectedCandidateUnits), TestUnit.createSet(autoChooser.getCandidateUnits(true)));
    setUnits(expectedSelectedUnitsWithDependents, bb1, bb2);
    setUnits(expectedSelectedUnits, bb1, bb2);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(0, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(0, false)));
    // implicitDependents:
    // YES
    // categorizeMovement:
    // NO
    // allUnits:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t4[a4, ],t5[ , ],t6[ , ],bb1,bb2
    // chosenUnits:
    // trn,trn,bb,bb
    // candidateCompositeCategories:
    // [trn,trn] => exact
    // [trn[arm,inf],trn[arm,inf]] => greedy
    // [trn[arm,inf],trn[arm]] => greedy
    // [trn[arm,inf],trn[inf]] => greedy
    // [trn[arm,inf],trn] => greedy
    // [trn[inf],trn[arm]] => greedy
    // [trn[inf],trn] => greedy
    // [trn[arm],trn] => greedy
    // candidateUnits:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t4[a4, ],t5[ , ],t6[ , ],bb1,bb2
    // exactSolutions:
    // t5[ , ],t6[ , ],bb1,bb2
    // greedySolutions:
    // t1[a1,i1],t2[a2,i2],bb1,bb2
    // t1[a1,i1],t3[ ,i3],bb1,bb2
    // t1[a1,i1],t4[a4, ],bb1,bb2
    // t1[a1,i1],t5[ , ],bb1,bb2
    // t3[ ,i3],t4[a4, ],bb1,bb2
    // t3[ ,i3],t5[ , ],bb1,bb2
    // t4[a4, ],t5[ , ],bb1,bb2
    // incompleteSolution:
    // none
    // exactSolutionCount:
    // 1
    // solutionCount:
    // 8
    setUnits(allUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, t5, t6, bb1, bb2);
    setUnits(chosenUnits, t2, t4, bb1, bb2);
    autoChooser = new UnitAutoChooser(allUnits, chosenUnits, mustMoveWith, bImplicitDependents, bCategorizeMovement);
    assertEquals(1, autoChooser.exactSolutionCount());
    assertEquals(8, autoChooser.solutionCount());
    assertTrue(autoChooser.foundCompleteSolution());
    setUnits(expectedCandidateUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, t5, t6, bb1, bb2);
    assertEquals(TestUnit.createSet(expectedCandidateUnits), TestUnit.createSet(autoChooser.getCandidateUnits(true)));
    setUnits(expectedSelectedUnitsWithDependents, t5, t6, bb1, bb2);
    setUnits(expectedSelectedUnits, t5, t6, bb1, bb2);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(0, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(0, false)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t2, a2, i2, bb1, bb2);
    setUnits(expectedSelectedUnits, t1, t2, bb1, bb2);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(1, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(1, false)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t4, a4, bb1, bb2);
    setUnits(expectedSelectedUnits, t1, t4, bb1, bb2);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(2, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(2, false)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t3, i3, bb1, bb2);
    setUnits(expectedSelectedUnits, t1, t3, bb1, bb2);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(3, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(3, false)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t5, bb1, bb2);
    setUnits(expectedSelectedUnits, t1, t5, bb1, bb2);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(4, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(4, false)));
    setUnits(expectedSelectedUnitsWithDependents, t3, i3, t4, a4, bb1, bb2);
    setUnits(expectedSelectedUnits, t3, t4, bb1, bb2);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(5, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(5, false)));
    setUnits(expectedSelectedUnitsWithDependents, t4, a4, t5, bb1, bb2);
    setUnits(expectedSelectedUnits, t4, t5, bb1, bb2);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(6, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(6, false)));
    setUnits(expectedSelectedUnitsWithDependents, t3, i3, t5, bb1, bb2);
    setUnits(expectedSelectedUnits, t3, t5, bb1, bb2);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(7, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(7, false)));
    // implicitDependents:
    // YES
    // categorizeMovement:
    // NO
    // allUnits:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t4[a4, ],t5[ , ],t6[ , ],bb1,bb2
    // chosenUnits:
    // trn, trn, arm, arm
    // candidateCompositeCategories:
    // [trn[arm,inf],trn[arm,inf]] => greedy
    // [trn[arm,inf],trn[arm]] => greedy
    // candidateUnits:
    // t1[a1,i1],t2[a2,i2],t4[ ,i3],t4[a4, ],t5[ , ],t6[ , ]
    // exactSolutions:
    // none
    // greedySolutions:
    // t1[a1,i1],t2[a2,i2]
    // t1[a1,i1],t4[a4, ]
    // incompleteSolution:
    // none
    // exactSolutionCount:
    // 0
    // solutionCount:
    // 2
    setUnits(allUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, t5, t6, bb1, bb2);
    setUnits(chosenUnits, t1, a1, t2, a2);
    autoChooser = new UnitAutoChooser(allUnits, chosenUnits, mustMoveWith, bImplicitDependents, bCategorizeMovement);
    assertEquals(0, autoChooser.exactSolutionCount());
    assertEquals(2, autoChooser.solutionCount());
    assertTrue(autoChooser.foundCompleteSolution());
    setUnits(expectedCandidateUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, t5, t6);
    assertEquals(TestUnit.createSet(expectedCandidateUnits), TestUnit.createSet(autoChooser.getCandidateUnits(true)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t2, a2, i2);
    setUnits(expectedSelectedUnits, t1, a1, t2, a2);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(0, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(0, false)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t4, a4);
    setUnits(expectedSelectedUnits, t1, a1, t4, a4);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(1, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(1, false)));
    // implicitDependents:
    // YES
    // categorizeMovement:
    // NO
    // allUnits:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t4[a4, ],bb1,bb2
    // chosenUnits:
    // trn
    // candidateCompositeCategories:
    // [trn[arm,inf]] => greedy
    // [trn[inf]] => greedy
    // [trn[arm]] => greedy
    // candidateUnits:
    // t1[a1,i1], t2[a2,i2], t3[ ,i3], t4[a4, ]
    // exactSolutions:
    // none
    // greedySolutions:
    // t1[a1,i1]
    // t3[ ,i3]
    // t4[a4, ]
    // incompleteSolution:
    // none
    // exactSolutionCount:
    // 0
    // solutionCount:
    // 3
    setUnits(allUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, bb1, bb2);
    setUnits(chosenUnits, t4);
    autoChooser = new UnitAutoChooser(allUnits, chosenUnits, mustMoveWith, bImplicitDependents, bCategorizeMovement);
    assertEquals(0, autoChooser.exactSolutionCount());
    assertEquals(3, autoChooser.solutionCount());
    assertTrue(autoChooser.foundCompleteSolution());
    setUnits(expectedCandidateUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4);
    assertEquals(TestUnit.createSet(expectedCandidateUnits), TestUnit.createSet(autoChooser.getCandidateUnits(true)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1);
    setUnits(expectedSelectedUnits, t1);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(0, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(0, false)));
    setUnits(expectedSelectedUnitsWithDependents, t4, a4);
    setUnits(expectedSelectedUnits, t4);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(1, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(1, false)));
    setUnits(expectedSelectedUnitsWithDependents, t3, i3);
    setUnits(expectedSelectedUnits, t3);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(2, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(2, false)));
    // implicitDependents:
    // YES
    // categorizeMovement:
    // NO
    // allUnits:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t4[a4, ],t5[ , ],bb1,bb2
    // chosenUnits:
    // trn
    // candidateCompositeCategories:
    // [trn] => exact
    // [trn[arm,inf]] => greedy
    // [trn[inf]] => greedy
    // [trn[arm]] => greedy
    // candidateUnits:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t4[a4, ],t5[ , ]
    // exactSolutions:
    // t5[ , ]
    // greedySolutions:
    // t1[a1,i1]
    // t3[ ,i3]
    // t4[a4, ]
    // incompleteSolution:
    // none
    // exactSolutionCount:
    // 1
    // solutionCount:
    // 4
    setUnits(allUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, t5, bb1, bb2);
    setUnits(chosenUnits, t4);
    autoChooser = new UnitAutoChooser(allUnits, chosenUnits, mustMoveWith, bImplicitDependents, bCategorizeMovement);
    assertEquals(1, autoChooser.exactSolutionCount());
    assertEquals(4, autoChooser.solutionCount());
    assertTrue(autoChooser.foundCompleteSolution());
    setUnits(expectedCandidateUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, t5);
    assertEquals(TestUnit.createSet(expectedCandidateUnits), TestUnit.createSet(autoChooser.getCandidateUnits(true)));
    setUnits(expectedSelectedUnitsWithDependents, t5);
    setUnits(expectedSelectedUnits, t5);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(0, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(0, false)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1);
    setUnits(expectedSelectedUnits, t1);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(1, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(1, false)));
    setUnits(expectedSelectedUnitsWithDependents, t4, a4);
    setUnits(expectedSelectedUnits, t4);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(2, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(2, false)));
    setUnits(expectedSelectedUnitsWithDependents, t3, i3);
    setUnits(expectedSelectedUnits, t3);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(3, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(3, false)));
    // implicitDependents:
    // YES
    // categorizeMovement:
    // NO
    // allUnits:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t4[a4, ],t5[ , ],t6[ , ],bb1,bb2
    // chosenUnits:
    // trn, trn, trn, trn, trn
    // candidateCompositeCategories:
    // [trn[arm,inf],trn[arm,inf],trn[inf],trn[arm],trn] => greedy
    // [trn[arm,inf],trn[arm,inf],trn[inf],trn,trn] => greedy
    // [trn[arm,inf],trn[arm,inf],trn[arm],trn,trn] => greedy
    // [trn[arm,inf],trn[inf],trn[arm],trn,trn] => greedy
    // candidateUnits:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t4[a4, ],t5[ , ],t6[ , ]
    // exactSolutions:
    // none
    // greedySolutions:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t4[a4, ],t5[ , ]
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t5[ , ],t6[ , ]
    // t1[a1,i1],t2[a2,i2],t4[a4, ],t5[ , ],t6[ , ]
    // t1[a1,i1],t3[ ,i3],t4[a4, ],t5[ , ],t6[ , ]
    // incompleteSolution:
    // none
    // exactSolutionCount:
    // 0
    // solutionCount:
    // 4
    setUnits(allUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, t5, t6, bb1, bb2);
    setUnits(chosenUnits, t1, t2, t3, t4, t5);
    autoChooser = new UnitAutoChooser(allUnits, chosenUnits, mustMoveWith, bImplicitDependents, bCategorizeMovement);
    assertEquals(0, autoChooser.exactSolutionCount());
    assertEquals(4, autoChooser.solutionCount());
    assertTrue(autoChooser.foundCompleteSolution());
    setUnits(expectedCandidateUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, t5, t6);
    assertEquals(TestUnit.createSet(expectedCandidateUnits), TestUnit.createSet(autoChooser.getCandidateUnits(true)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, t5);
    setUnits(expectedSelectedUnits, t1, t2, t3, t4, t5);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(0, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(0, false)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t2, a2, i2, t4, a4, t5, t6);
    setUnits(expectedSelectedUnits, t1, t2, t4, t5, t6);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(1, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(1, false)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t2, a2, i2, t3, i3, t5, t6);
    setUnits(expectedSelectedUnits, t1, t2, t3, t5, t6);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(2, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(2, false)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t3, i3, t4, a4, t5, t6);
    setUnits(expectedSelectedUnits, t1, t3, t4, t5, t6);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(3, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(3, false)));
    // implicitDependents:
    // YES
    // categorizeMovement:
    // NO
    // allUnits:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t4[a4, ],t5[ , ],t6[ , ],t7[i7,I7],bb1,bb2
    // chosenUnits:
    // trn, trn, inf
    // candidateCompositeCategories:
    // [trn[inf],trn] => exact
    // [trn[arm,inf],trn[arm,inf]] => greedy
    // [trn[arm,inf],trn[inf]] => greedy
    // [trn[arm,inf],trn[arm]] => greedy
    // [trn[arm,inf],trn] => greedy
    // [trn[arm,inf],trn[inf,inf]] => greedy
    // [trn[inf],trn[arm]] => greedy
    // [trn[inf],trn[inf,inf]] => greedy
    // [trn[arm],trn[inf,inf]] => greedy
    // [trn,trn[inf,inf]] => greedy
    // candidateUnits:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t4[a4, ],t5[ , ],t6[ , ],t7[i7,I7]
    // exactSolutions:
    // t3[ ,i3], t5[ , ]
    // greedySolutions:
    // t1[a1,i1],t2[a2,i2]
    // t1[a1,i1],t3[ ,i3]
    // t1[a1,i1],t4[a4, ]
    // t1[a1,i1],t5[ , ]
    // t1[a1,i1],t7[i7,I7]
    // t3[ ,i3],t4[a4, ]
    // t3[ ,i3],t7[i7,I7]
    // t4[a4, ],t7[i7,I7]
    // t5[ , ],t7[i7,I7]
    // incompleteSolution:
    // none
    // exactSolutionCount:
    // 1
    // solutionCount:
    // 10
    setUnits(allUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, t5, t6, t7, i7, I7, bb1, bb2);
    setUnits(chosenUnits, t1, t4, i3);
    autoChooser = new UnitAutoChooser(allUnits, chosenUnits, mustMoveWith, bImplicitDependents, bCategorizeMovement);
    assertEquals(1, autoChooser.exactSolutionCount());
    assertEquals(10, autoChooser.solutionCount());
    assertTrue(autoChooser.foundCompleteSolution());
    setUnits(expectedCandidateUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, t5, t6, t7, i7, I7);
    assertEquals(TestUnit.createSet(expectedCandidateUnits), TestUnit.createSet(autoChooser.getCandidateUnits(true)));
    setUnits(expectedSelectedUnitsWithDependents, t3, i3, t5);
    setUnits(expectedSelectedUnits, t3, i3, t5);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(0, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(0, false)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t2, a2, i2);
    setUnits(expectedSelectedUnits, t1, i1, t2);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(1, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(1, false)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t4, a4);
    setUnits(expectedSelectedUnits, t1, i1, t4);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(2, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(2, false)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t7, i7, I7);
    setUnits(expectedSelectedUnits, t1, i1, t7);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(3, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(3, false)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t3, i3);
    setUnits(expectedSelectedUnits, t1, i1, t3);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(4, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(4, false)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t5);
    setUnits(expectedSelectedUnits, t1, i1, t5);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(5, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(5, false)));
    setUnits(expectedSelectedUnitsWithDependents, t7, i7, I7, t4, a4);
    setUnits(expectedSelectedUnits, t7, i7, t4);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(6, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(6, false)));
    setUnits(expectedSelectedUnitsWithDependents, t3, i3, t4, a4);
    setUnits(expectedSelectedUnits, t3, i3, t4);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(7, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(7, false)));
    setUnits(expectedSelectedUnitsWithDependents, t3, i3, t7, i7, I7);
    setUnits(expectedSelectedUnits, t3, t7, i7);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(8, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(8, false)));
    setUnits(expectedSelectedUnitsWithDependents, t5, t7, i7, I7);
    setUnits(expectedSelectedUnits, t5, t7, i7);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(9, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(9, false)));
  }

  // This mode is currently unused.
  @Test
  public void testUnitAutoChooserNoImplicitDependentsNoMovementCategorized() {
    final List<Unit> allUnits = new ArrayList<>();
    final List<Unit> chosenUnits = new ArrayList<>();
    final List<Unit> expectedCandidateUnits = new ArrayList<>();
    final List<Unit> expectedSelectedUnitsWithDependents = new ArrayList<>();
    final List<Unit> expectedSelectedUnits = new ArrayList<>();
    UnitAutoChooser autoChooser = null;
    final Map<Unit, Collection<Unit>> mustMoveWith = new HashMap<>();
    final boolean bImplicitDependents = false;
    final boolean bCategorizeMovement = false;
    // Setup units/dependencies
    final TripleAUnit bb1 = TestUnit.createUnit("bb1", battleship, british);
    final TripleAUnit t1 = TestUnit.createUnit("t1", transport, british);
    final TripleAUnit a1 = TestUnit.createUnit("a1", armour, british);
    final TripleAUnit i1 = TestUnit.createUnit("i1", infantry, british);
    loadTransport(mustMoveWith, t1, a1, i1);
    final TripleAUnit bb2 = TestUnit.createUnit("bb2", battleship, british);
    final TripleAUnit t2 = TestUnit.createUnit("t2", transport, british);
    final TripleAUnit a2 = TestUnit.createUnit("a2", armour, british);
    final TripleAUnit i2 = TestUnit.createUnit("i2", infantry, british);
    loadTransport(mustMoveWith, t2, a2, i2);
    // make this trn only have 1 movement left
    TripleAUnit.get(t2).setAlreadyMoved(1);
    final TripleAUnit t3 = TestUnit.createUnit("t3", transport, british);
    final TripleAUnit i3 = TestUnit.createUnit("i3", infantry, british);
    loadTransport(mustMoveWith, t3, i3);
    final TripleAUnit t4 = TestUnit.createUnit("t4", transport, british);
    final TripleAUnit a4 = TestUnit.createUnit("a4", armour, british);
    loadTransport(mustMoveWith, t4, a4);
    final TripleAUnit t5 = TestUnit.createUnit("t5", transport, british);
    final TripleAUnit t6 = TestUnit.createUnit("t6", transport, british);
    // BEGIN TESTS
    // implicitDependents:
    // NO
    // categorizeMovement:
    // NO
    // allUnits:
    // t1[a1,i1],t2[a2,i2],t3[i3],t4[a4],t5[],t6[],bb1
    // chosenUnits:
    // trn, trn, inf, inf, arm, arm, bb
    // candidateCompositeCategories:
    // [trn[arm,inf],trn[arm,inf]]
    // candidateUnits:
    // t1[a1,i1],t2[a2,i2],t5[ , ],t6[ , ],bb1
    // exactSolutions:
    // t1[a1,i1],t2[a2,i2],bb1
    // greedySolutions:
    // none
    // incompleteSolution:
    // none
    // exactSolutionCount:
    // 1
    // solutionCount:
    // 1
    setUnits(allUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, t5, t6, bb1);
    setUnits(chosenUnits, t1, t5, a1, a4, i2, i3, bb1);
    autoChooser = new UnitAutoChooser(allUnits, chosenUnits, mustMoveWith, bImplicitDependents, bCategorizeMovement);
    assertEquals(1, autoChooser.exactSolutionCount());
    assertEquals(1, autoChooser.solutionCount());
    assertTrue(autoChooser.foundCompleteSolution());
    setUnits(expectedCandidateUnits, t1, a1, i1, t2, a2, i2, t5, t6, bb1);
    assertEquals(TestUnit.createSet(expectedCandidateUnits), TestUnit.createSet(autoChooser.getCandidateUnits(true)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t2, a2, i2, bb1);
    setUnits(expectedSelectedUnits, t1, a1, i1, t2, a2, i2, bb1);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(0, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(0, false)));
    // implicitDependents:
    // NO
    // categorizeMovement:
    // NO
    // allUnits:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t4[a4, ],t5[ , ],t6[ , ],bb1,bb2
    // chosenUnits:
    // trn, trn, trn, inf, inf, arm, arm
    // candidateCompositeCategories:
    // [trn[inf,arm],trn[inf],trn[arm]] => exact
    // [trn[inf,arm],trn[inf,arm],trn[]] => exact
    // candidateUnits:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t4[a4, ],t5[ , ],t6[ , ]
    // exactSolutions:
    // t1[a1,i1],t3[ ,i3],t4[a4, ]
    // t1[a1,i1],t2[a2,i2],t5[ , ]
    // greedySolutions:
    // none
    // incompleteSolutions:
    // none
    // exactSolutionCount:
    // 2
    // solutionCount:
    // 2
    setUnits(allUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, t5, t6, bb1, bb2);
    setUnits(chosenUnits, t1, a2, i2, t5, i3, t6, a1);
    autoChooser = new UnitAutoChooser(allUnits, chosenUnits, mustMoveWith, bImplicitDependents, bCategorizeMovement);
    assertEquals(2, autoChooser.exactSolutionCount());
    assertEquals(2, autoChooser.solutionCount());
    assertTrue(autoChooser.foundCompleteSolution());
    setUnits(expectedCandidateUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, t5, t6);
    assertEquals(TestUnit.createSet(expectedCandidateUnits), TestUnit.createSet(autoChooser.getCandidateUnits(true)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t2, a2, i2, t5);
    setUnits(expectedSelectedUnits, t1, a1, i1, t2, a2, i2, t5);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(0, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(0, false)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t3, i3, t4, a4);
    setUnits(expectedSelectedUnits, t1, a1, i1, t3, i3, t4, a4);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(1, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(1, false)));
    // implicitDependents:
    // NO
    // categorizeMovement:
    // NO
    // allUnits:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t5[ , ],t6[ , ],t4[a4, ],bb1,bb2
    // chosenUnits:
    // trn,trn,bb,bb
    // candidateCompositeCategories:
    // [[trn,trn]] => exact
    // candidateUnits:
    // t5[ , ],t6[ , ],bb1,bb2
    // exactSolutions:
    // t5[ , ],t6[ , ],bb1,bb2
    // greedySolutions:
    // none
    // incompleSolution:
    // none
    // exactSolutionCount:
    // 1
    // solutionCount:
    // 1
    setUnits(allUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, t5, t6, bb1, bb2);
    setUnits(chosenUnits, t2, t4, bb1, bb2);
    autoChooser = new UnitAutoChooser(allUnits, chosenUnits, mustMoveWith, bImplicitDependents, bCategorizeMovement);
    assertEquals(1, autoChooser.exactSolutionCount());
    assertEquals(1, autoChooser.solutionCount());
    assertTrue(autoChooser.foundCompleteSolution());
    setUnits(expectedCandidateUnits, t5, t6, bb1, bb2);
    assertEquals(TestUnit.createSet(expectedCandidateUnits), TestUnit.createSet(autoChooser.getCandidateUnits(true)));
    setUnits(expectedSelectedUnitsWithDependents, t5, t6, bb1, bb2);
    setUnits(expectedSelectedUnits, t5, t6, bb1, bb2);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(0, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(0, false)));
    // implicitDependents:
    // NO
    // categorizeMovement:
    // NO
    // allUnits:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t4[a4, ],t5[ , ],t6[ , ],bb1,bb2
    // chosenUnits:
    // trn, trn, arm, arm
    // candidateCompositeCategories:
    // [trn[arm],?] => incomplete
    // candidateUnits:
    // t4[a4, ],t5[ , ],t6[ , ]
    // exactSolutions:
    // none
    // greedySolutions:
    // none
    // incompleteSolution:
    // t4[a4, ],t5[ , ]
    // exactSolutionCount:
    // 0
    // solutionCount:
    // 1
    setUnits(allUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, t5, t6, bb1, bb2);
    setUnits(chosenUnits, t1, a1, t2, a2);
    autoChooser = new UnitAutoChooser(allUnits, chosenUnits, mustMoveWith, bImplicitDependents, bCategorizeMovement);
    assertEquals(0, autoChooser.exactSolutionCount());
    assertEquals(1, autoChooser.solutionCount());
    assertFalse(autoChooser.foundCompleteSolution());
    setUnits(expectedCandidateUnits, t4, a4, t5, t6);
    assertEquals(TestUnit.createSet(expectedCandidateUnits), TestUnit.createSet(autoChooser.getCandidateUnits(true)));
    setUnits(expectedSelectedUnitsWithDependents, t4, a4, t5);
    setUnits(expectedSelectedUnits, t4, a4, t5);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(0, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(0, false)));
    // implicitDependents:
    // NO
    // categorizeMovement:
    // NO
    // allUnits:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t4[a4, ],bb1,bb2
    // chosenUnits:
    // trn
    // candidateCompositeCategories:
    // none
    // candidateUnits:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t4[a4, ]
    // exactSolutions:
    // none
    // greedySolutions:
    // t1[a1,i1]
    // incompleteSolution:
    // none
    // exactSolutionCount:
    // 0
    // solutionCount:
    // 1
    setUnits(allUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, bb1, bb2);
    setUnits(chosenUnits, t4);
    autoChooser = new UnitAutoChooser(allUnits, chosenUnits, mustMoveWith, bImplicitDependents, bCategorizeMovement);
    assertEquals(0, autoChooser.exactSolutionCount());
    assertEquals(1, autoChooser.solutionCount());
    assertTrue(autoChooser.foundCompleteSolution());
    setUnits(expectedCandidateUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4);
    assertEquals(TestUnit.createSet(expectedCandidateUnits), TestUnit.createSet(autoChooser.getCandidateUnits(true)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1);
    // simple solution
    setUnits(expectedSelectedUnits, t1);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(0, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(0, false)));
    // implicitDependents:
    // NO
    // categorizeMovement:
    // NO
    // allUnits:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t4[a4, ],t5[ , ],t6[ , ],bb1,bb2
    // chosenUnits:
    // trn, trn, trn, trn, trn
    // candidateCompositeCategories:
    // [[trn,trn,?]] => incomplete
    // candidateUnits:
    // t5[ , ],t6[ , ]
    // exactSolutions:
    // none
    // greedySolutions:
    // none
    // incompleteSolution:
    // t5[ , ],t6[ , ]
    // exactSolutionCount:
    // 0
    // solutionCount:
    // 1
    setUnits(allUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, t5, t6, bb1, bb2);
    setUnits(chosenUnits, t1, t2, t3, t4, t5);
    autoChooser = new UnitAutoChooser(allUnits, chosenUnits, mustMoveWith, bImplicitDependents, bCategorizeMovement);
    assertEquals(0, autoChooser.exactSolutionCount());
    assertEquals(1, autoChooser.solutionCount());
    assertFalse(autoChooser.foundCompleteSolution());
    setUnits(expectedCandidateUnits, t5, t6);
    assertEquals(TestUnit.createSet(expectedCandidateUnits), TestUnit.createSet(autoChooser.getCandidateUnits(true)));
    setUnits(expectedSelectedUnitsWithDependents, t5, t6);
    setUnits(expectedSelectedUnits, t5, t6);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(0, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(0, false)));
  }

  // The MovePanel and EditPanel use the UnitAutoChooser in this mode
  // when running the UnitChooser
  @Test
  public void testUnitAutoChooserWithImplicitDependentsWithMovementCategorized() {
    final List<Unit> allUnits = new ArrayList<>();
    final List<Unit> chosenUnits = new ArrayList<>();
    final List<Unit> expectedCandidateUnits = new ArrayList<>();
    final List<Unit> expectedSelectedUnitsWithDependents = new ArrayList<>();
    final List<Unit> expectedSelectedUnits = new ArrayList<>();
    UnitAutoChooser autoChooser = null;
    final Map<Unit, Collection<Unit>> mustMoveWith = new HashMap<>();
    final boolean bImplicitDependents = true;
    final boolean bCategorizeMovement = true;
    // Setup units/dependencies
    final TripleAUnit bb1 = TestUnit.createUnit("bb1", battleship, british);
    final TripleAUnit t1 = TestUnit.createUnit("t1", transport, british);
    final TripleAUnit a1 = TestUnit.createUnit("a1", armour, british);
    final TripleAUnit i1 = TestUnit.createUnit("i1", infantry, british);
    loadTransport(mustMoveWith, t1, a1, i1);
    final TripleAUnit bb2 = TestUnit.createUnit("bb2", battleship, british);
    final TripleAUnit t2 = TestUnit.createUnit("t2", transport, british);
    final TripleAUnit a2 = TestUnit.createUnit("a2", armour, british);
    final TripleAUnit i2 = TestUnit.createUnit("i2", infantry, british);
    loadTransport(mustMoveWith, t2, a2, i2);
    // make this trn only have 1 movement left
    TripleAUnit.get(t2).setAlreadyMoved(1);
    final TripleAUnit t3 = TestUnit.createUnit("t3", transport, british);
    final TripleAUnit i3 = TestUnit.createUnit("i3", infantry, british);
    loadTransport(mustMoveWith, t3, i3);
    final TripleAUnit t4 = TestUnit.createUnit("t4", transport, british);
    final TripleAUnit a4 = TestUnit.createUnit("a4", armour, british);
    loadTransport(mustMoveWith, t4, a4);
    final TripleAUnit t5 = TestUnit.createUnit("t5", transport, british);
    final TripleAUnit t6 = TestUnit.createUnit("t6", transport, british);
    final TripleAUnit t7 = TestUnit.createUnit("t7", transport, british);
    final TripleAUnit i7 = TestUnit.createUnit("i7", infantry, british);
    final TripleAUnit I7 = TestUnit.createUnit("I7", infantry, british);
    loadTransport(mustMoveWith, t7, i7, I7);
    // BEGIN TESTS
    // implicitDependents:
    // YES
    // categorizeMovement:
    // YES
    // allUnits:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t4[a4, ],t5[ , ],t6[ , ],bb1
    // chosenUnits:
    // trn, trn, inf, inf, arm, arm, bb
    // candidateCompositeCategories:
    // [trn[arm,inf],trn[arm,inf]] => exact
    // candidateUnits:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t4[a4, ],t5[ , ],t6[ , ],bb1
    // exactSolutions:
    // t1[a1,i1],t2[a2,i2], bb1
    // greedySolutions:
    // none
    // incompleteSolution:
    // none
    // exactSolutionCount:
    // 1
    // solutionCount:
    // 1
    setUnits(allUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, t5, t6, bb1);
    setUnits(chosenUnits, t1, t5, a1, a4, i2, i3, bb1);
    autoChooser = new UnitAutoChooser(allUnits, chosenUnits, mustMoveWith, bImplicitDependents, bCategorizeMovement);
    assertEquals(1, autoChooser.exactSolutionCount());
    assertEquals(1, autoChooser.solutionCount());
    assertTrue(autoChooser.foundCompleteSolution());
    setUnits(expectedCandidateUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, t5, t6, bb1);
    assertEquals(TestUnit.createSet(expectedCandidateUnits), TestUnit.createSet(autoChooser.getCandidateUnits(true)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t2, a2, i2, bb1);
    setUnits(expectedSelectedUnits, t1, a1, i1, t2, a2, i2, bb1);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(0, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(0, false)));
    // implicitDependents:
    // YES
    // categorizeMovement:
    // YES
    // allUnits:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t4[a4, ],t5[ , ],t6[ , ],bb1,bb2
    // chosenUnits:
    // trn, trn, trn, inf, inf, arm, arm
    // candidateCompositeCategories:
    // [trnM1[inf,arm],trn[inf],trn[arm]] => exact
    // [trnM2[inf,arm],trn[inf],trn[arm]] => exact
    // [trnM1[inf,arm],trnM2[inf,arm],trn[]] => exact
    // [trnM1[inf,arm],trnM2[inf,arm],trn[inf]] => greedy
    // [trnM1[inf,arm],trnM2[inf,arm],trn[arm]] => greedy
    // candidateUnits:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t4[a4, ],t5[ , ],t6[ , ]
    // exactSolutions:
    // t1[a1,i1],t2[a2,i2],t5[ , ]
    // t1[a1,i1],t3[ ,i3],t4[a4, ]
    // t2[a2,i2],t3[ ,i3],t4[a4, ]
    // greedySolutions:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3]
    // t1[a1,i1],t2[a2,i2],t4[a4, ]
    // incompleteSolution:
    // none
    // exactSolutionCount:
    // 3
    // solutionCount:
    // 5
    setUnits(allUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, t5, t6, bb1, bb2);
    setUnits(chosenUnits, t1, a2, i2, t5, a1, i3, t6);
    autoChooser = new UnitAutoChooser(allUnits, chosenUnits, mustMoveWith, bImplicitDependents, bCategorizeMovement);
    assertEquals(3, autoChooser.exactSolutionCount());
    assertEquals(5, autoChooser.solutionCount());
    assertTrue(autoChooser.foundCompleteSolution());
    setUnits(expectedCandidateUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, t5, t6);
    assertEquals(TestUnit.createSet(expectedCandidateUnits), TestUnit.createSet(autoChooser.getCandidateUnits(true)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t2, a2, i2, t5);
    setUnits(expectedSelectedUnits, t1, a1, i1, t2, a2, i2, t5);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(0, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(0, false)));
    setUnits(expectedSelectedUnitsWithDependents, t2, a2, i2, t3, i3, t4, a4);
    setUnits(expectedSelectedUnits, t2, a2, i2, t3, i3, t4, a4);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(1, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(1, false)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t3, i3, t4, a4);
    setUnits(expectedSelectedUnits, t1, a1, i1, t3, i3, t4, a4);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(2, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(2, false)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t2, a2, i2, t4, a4);
    setUnits(expectedSelectedUnits, t1, a1, i1, t2, a2, i2, t4);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(3, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(3, false)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t2, a2, i2, t3, i3);
    setUnits(expectedSelectedUnits, t1, a1, i1, t2, a2, i2, t3);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(4, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(4, false)));
    // implicitDependents:
    // YES
    // categorizeMovement:
    // YES
    // allUnits:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t4[a4, ],t5[ , ],t6[ , ],bb1,bb2
    // chosenUnits:
    // bb
    // candidateCompositeCategories:
    // none
    // candidateUnits:
    // bb1,bb2
    // exactSolutions:
    // bb1
    // greedySolutions:
    // none
    // incompleteSolutions:
    // none
    // exactSolutionCount:
    // 1
    // solutionCount:
    // 1
    setUnits(allUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, t5, t6, bb1, bb2);
    setUnits(chosenUnits, bb2);
    autoChooser = new UnitAutoChooser(allUnits, chosenUnits, mustMoveWith, bImplicitDependents, bCategorizeMovement);
    assertEquals(1, autoChooser.exactSolutionCount());
    assertEquals(1, autoChooser.solutionCount());
    assertTrue(autoChooser.foundCompleteSolution());
    setUnits(expectedCandidateUnits, bb1, bb2);
    assertEquals(TestUnit.createSet(expectedCandidateUnits), TestUnit.createSet(autoChooser.getCandidateUnits(true)));
    setUnits(expectedSelectedUnitsWithDependents, bb1);
    setUnits(expectedSelectedUnits, bb1);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(0, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(0, false)));
    // implicitDependents:
    // YES
    // categorizeMovement:
    // YES
    // allUnits:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t4[a4, ],t5[ , ],t6[ , ],bb1,bb2
    // chosenUnits:
    // bb,bb
    // candidateCompositeCategories:
    // none
    // candidateUnits:
    // bb1,bb2
    // exactSolutions:
    // bb1,bb2
    // greedySolutions:
    // none
    // incompleteSolution:
    // none
    // exactSolutionCount:
    // 1
    // solutionCount:
    // 1
    setUnits(allUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, t5, t6, bb1, bb2);
    setUnits(chosenUnits, bb2, bb1);
    autoChooser = new UnitAutoChooser(allUnits, chosenUnits, mustMoveWith, bImplicitDependents, bCategorizeMovement);
    assertEquals(1, autoChooser.exactSolutionCount());
    assertEquals(1, autoChooser.solutionCount());
    assertTrue(autoChooser.foundCompleteSolution());
    setUnits(expectedCandidateUnits, bb1, bb2);
    assertEquals(TestUnit.createSet(expectedCandidateUnits), TestUnit.createSet(autoChooser.getCandidateUnits(true)));
    setUnits(expectedSelectedUnitsWithDependents, bb1, bb2);
    setUnits(expectedSelectedUnits, bb1, bb2);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(0, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(0, false)));
    // implicitDependents:
    // YES
    // categorizeMovement:
    // YES
    // allUnits:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t4[a4, ],t5[ , ],t6[ , ],bb1,bb2
    // chosenUnits:
    // trn,trn,bb,bb
    // candidateCompositeCategories:
    // [trn,trn] => exact
    // [trnM1[inf,arm],trnM2[inf,arm]] => greedy
    // [trnM1[inf,arm],trn[inf]] => greedy
    // [trnM1[inf,arm],trn[arm]] => greedy
    // [trnM1[inf,arm],trn] => greedy
    // [trnM2[inf,arm],trn[inf]] => greedy
    // [trnM2[inf,arm],trn[arm]] => greedy
    // [trnM2[inf,arm],trn] => greedy
    // [trn[inf],trn[arm]] => greedy
    // [trn[inf],trn] => greedy
    // [trn[arm],trn] => greedy
    // candidateUnits:
    // t1[a1,i1,t2[a2,i2],t3[ ,i3],t4[a4, ],t5,t6,bb1,bb2
    // exactSolutions:
    // t5[ , ],t6[ , ],bb1,bb2
    // greedySolutions:
    // t1[a1,i1],t2[a2,i2],bb1,bb2
    // t1[a1,i1],t3[ ,i3],bb1,bb2
    // t1[a1,i1],t4[a4, ],bb1,bb2
    // t1[a1,i1],t5[ , ],bb1,bb2
    // t2[a2,i2],t3[ ,i3],bb1,bb2
    // t2[a2,i2],t4[a4, ],bb1,bb2
    // t2[a2,i2],t5[ , ],bb1,bb2
    // t3[ ,i3],t4[a4, ],bb1,bb2
    // t3[ ,i3],t5[ , ],bb1,bb2
    // t4[a4, ],t5[ , ],bb1,bb2
    // incompleteSolution:
    // none
    // exactSolutionCount:
    // 1
    // solutionCount:
    // 11
    setUnits(allUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, t5, t6, bb1, bb2);
    setUnits(chosenUnits, t2, t4, bb1, bb2);
    autoChooser = new UnitAutoChooser(allUnits, chosenUnits, mustMoveWith, bImplicitDependents, bCategorizeMovement);
    assertEquals(1, autoChooser.exactSolutionCount());
    assertEquals(11, autoChooser.solutionCount());
    assertTrue(autoChooser.foundCompleteSolution());
    setUnits(expectedCandidateUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, t5, t6, bb1, bb2);
    assertEquals(TestUnit.createSet(expectedCandidateUnits), TestUnit.createSet(autoChooser.getCandidateUnits(true)));
    setUnits(expectedSelectedUnitsWithDependents, t5, t6, bb1, bb2);
    setUnits(expectedSelectedUnits, t5, t6, bb1, bb2);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(0, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(0, false)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t2, a2, i2, bb1, bb2);
    setUnits(expectedSelectedUnits, t1, t2, bb1, bb2);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(1, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(1, false)));
    setUnits(expectedSelectedUnitsWithDependents, t2, a2, i2, t4, a4, bb1, bb2);
    setUnits(expectedSelectedUnits, t2, t4, bb1, bb2);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(2, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(2, false)));
    setUnits(expectedSelectedUnitsWithDependents, t2, a2, i2, t3, i3, bb1, bb2);
    setUnits(expectedSelectedUnits, t2, t3, bb1, bb2);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(3, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(3, false)));
    setUnits(expectedSelectedUnitsWithDependents, t2, a2, i2, t5, bb1, bb2);
    setUnits(expectedSelectedUnits, t2, t5, bb1, bb2);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(4, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(4, false)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t4, a4, bb1, bb2);
    setUnits(expectedSelectedUnits, t1, t4, bb1, bb2);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(5, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(5, false)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t3, i3, bb1, bb2);
    setUnits(expectedSelectedUnits, t3, t1, bb1, bb2);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(6, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(6, false)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t5, bb1, bb2);
    setUnits(expectedSelectedUnits, t1, t5, bb1, bb2);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(7, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(7, false)));
    setUnits(expectedSelectedUnitsWithDependents, t3, i3, t4, a4, bb1, bb2);
    setUnits(expectedSelectedUnits, t3, t4, bb1, bb2);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(8, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(8, false)));
    setUnits(expectedSelectedUnitsWithDependents, t4, a4, t5, bb1, bb2);
    setUnits(expectedSelectedUnits, t4, t5, bb1, bb2);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(9, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(9, false)));
    setUnits(expectedSelectedUnitsWithDependents, t3, i3, t5, bb1, bb2);
    setUnits(expectedSelectedUnits, t3, t5, bb1, bb2);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(10, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(10, false)));
    // implicitDependents:
    // YES
    // categorizeMovement:
    // YES
    // allUnits:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t4[a4, ],t5[ , ],t6[ , ],bb1,bb2
    // chosenUnits:
    // trn, trn, arm, arm
    // candidateCompositeCategories:
    // [trnM1[inf,arm],trnM2[inf,arm]] => greedy
    // [trnM1[inf,arm],trn[arm]] => greedy
    // [trnM2[inf,arm],trn[arm]] => greedy
    // candidateUnits:
    // t1[a1,i1],t2[a2,i2],t3[i3, ],t4[a4, ],t5[ , ],t6[ , ]
    // exactSolutions:
    // none
    // greedySolutions:
    // t1[a1,i1],t2[a2,i2]
    // t1[a1,i1],t4[a4, ]
    // t2[a2,i2],t4[a4, ]
    // incompleteSolution:
    // none
    // exactSolutionCount:
    // 0
    // solutionCount:
    // 3
    setUnits(allUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, t5, t6, bb1, bb2);
    setUnits(chosenUnits, t1, a1, t2, a2);
    autoChooser = new UnitAutoChooser(allUnits, chosenUnits, mustMoveWith, bImplicitDependents, bCategorizeMovement);
    assertEquals(0, autoChooser.exactSolutionCount());
    assertEquals(3, autoChooser.solutionCount());
    assertTrue(autoChooser.foundCompleteSolution());
    setUnits(expectedCandidateUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, t5, t6);
    assertEquals(TestUnit.createSet(expectedCandidateUnits), TestUnit.createSet(autoChooser.getCandidateUnits(true)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t2, a2, i2);
    setUnits(expectedSelectedUnits, t1, a1, t2, a2);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(0, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(0, false)));
    setUnits(expectedSelectedUnitsWithDependents, t2, a2, i2, t4, a4);
    setUnits(expectedSelectedUnits, t2, a2, t4, a4);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(1, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(1, false)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t4, a4);
    setUnits(expectedSelectedUnits, t1, a1, t4, a4);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(2, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(2, false)));
    // implicitDependents:
    // YES
    // categorizeMovement:
    // YES
    // allUnits:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t4[a4, ],bb1,bb2
    // chosenUnits:
    // trn
    // candidateCompositeCategories:
    // trnM1[inf,arm] => greedy
    // trnM2[inf,arm] => greedy
    // trn[inf] => greedy
    // trn[arm] => greedy
    // candidateUnits:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t4[a4, ]
    // exactSolutions:
    // none
    // greedySolutions:
    // t1[a1,i1]
    // t2[a2,i2]
    // t3[ ,i3]
    // t4[a4, ]
    // incompleteSolution:
    // none
    // exactSolutionCount:
    // 0
    // solutionCount:
    // 4
    setUnits(allUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, bb1, bb2);
    setUnits(chosenUnits, t4);
    autoChooser = new UnitAutoChooser(allUnits, chosenUnits, mustMoveWith, bImplicitDependents, bCategorizeMovement);
    assertEquals(0, autoChooser.exactSolutionCount());
    assertEquals(4, autoChooser.solutionCount());
    assertTrue(autoChooser.foundCompleteSolution());
    setUnits(expectedCandidateUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4);
    assertEquals(TestUnit.createSet(expectedCandidateUnits), TestUnit.createSet(autoChooser.getCandidateUnits(true)));
    setUnits(expectedSelectedUnitsWithDependents, t2, a2, i2);
    setUnits(expectedSelectedUnits, t2);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(0, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(0, false)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1);
    setUnits(expectedSelectedUnits, t1);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(1, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(1, false)));
    setUnits(expectedSelectedUnitsWithDependents, t4, a4);
    setUnits(expectedSelectedUnits, t4);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(2, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(2, false)));
    setUnits(expectedSelectedUnitsWithDependents, t3, i3);
    setUnits(expectedSelectedUnits, t3);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(3, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(3, false)));
    // implicitDependents:
    // YES
    // categorizeMovement:
    // YES
    // allUnits:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t4[a4, ],t5[ , ],bb1,bb2
    // chosenUnits:
    // trn
    // candidateCompositeCategories:
    // [trn] => exact
    // [trnM1[inf,arm]] => greedy
    // [trnM2[inf,arm]] => greedy
    // [trn[inf]] => greedy
    // [trn[arm]] => greedy
    // candidateUnits:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t4[a4, ],t5[ , ]
    // exactSolutions:
    // t5[ , ]
    // greedySolutions:
    // t1[a1,i1]
    // t2[a2,i2]
    // t3[ ,i3]
    // t4[a4, ]
    // incompleteSolution:
    // none
    // exactSolutionCount:
    // 1
    // solutionCount:
    // 5
    setUnits(allUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, t5, bb1, bb2);
    setUnits(chosenUnits, t4);
    autoChooser = new UnitAutoChooser(allUnits, chosenUnits, mustMoveWith, bImplicitDependents, bCategorizeMovement);
    assertEquals(1, autoChooser.exactSolutionCount());
    assertEquals(5, autoChooser.solutionCount());
    assertTrue(autoChooser.foundCompleteSolution());
    setUnits(expectedCandidateUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, t5);
    assertEquals(TestUnit.createSet(expectedCandidateUnits), TestUnit.createSet(autoChooser.getCandidateUnits(true)));
    setUnits(expectedSelectedUnitsWithDependents, t5);
    setUnits(expectedSelectedUnits, t5);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(0, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(0, false)));
    setUnits(expectedSelectedUnitsWithDependents, t2, a2, i2);
    setUnits(expectedSelectedUnits, t2);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(1, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(1, false)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1);
    setUnits(expectedSelectedUnits, t1);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(2, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(2, false)));
    setUnits(expectedSelectedUnitsWithDependents, t4, a4);
    setUnits(expectedSelectedUnits, t4);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(3, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(3, false)));
    setUnits(expectedSelectedUnitsWithDependents, t3, i3);
    setUnits(expectedSelectedUnits, t3);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(4, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(4, false)));
    // implicitDependents:
    // YES
    // categorizeMovement:
    // YES
    // allUnits:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t4[a4, ],t5[ , ],t6[ , ],bb1,bb2
    // chosenUnits:
    // trn, trn, trn, trn, trn
    // candidateCompositeCategories:
    // [trnM1[arm,inf],trnM2[arm,inf],trn[inf],trn[arm],trn] => greedy
    // [trnM1[arm,inf],trnM2[arm,inf],trn[inf],trn,trn] => greedy
    // [trnM1[arm,inf],trnM2[arm,inf],trn[arm],trn,trn] => greedy
    // [trnM1[arm,inf],trn[inf],trn[arm],trn,trn] => greedy
    // [trnM2[arm,inf],trn[inf],trn[arm],trn,trn] => greedy
    // candidateUnits:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t4[a4, ],t5[ , ],t6[ , ]
    // exactSolutions:
    // none
    // greedySolutions:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t4[a4, ],t5[ , ]
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t5[ , ],t6[ , ]
    // t1[a1,i1],t2[a2,i2],t4[a4, ],t5[ , ],t6[ , ]
    // t1[a1,i1],t3[ ,i2],t4[a4, ],t5[ , ],t6[ , ]
    // t2[a2,i2],t3[ ,i2],t4[a4, ],t5[ , ],t6[ , ]
    // incompleteSolution:
    // none
    // exactSolutionCount:
    // 0
    // solutionCount:
    // 5
    setUnits(allUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, t5, t6, bb1, bb2);
    setUnits(chosenUnits, t1, t2, t3, t4, t5);
    autoChooser = new UnitAutoChooser(allUnits, chosenUnits, mustMoveWith, bImplicitDependents, bCategorizeMovement);
    assertEquals(0, autoChooser.exactSolutionCount());
    assertEquals(5, autoChooser.solutionCount());
    assertTrue(autoChooser.foundCompleteSolution());
    setUnits(expectedCandidateUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, t5, t6);
    assertEquals(TestUnit.createSet(expectedCandidateUnits), TestUnit.createSet(autoChooser.getCandidateUnits(true)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, t5);
    setUnits(expectedSelectedUnits, t1, t2, t3, t4, t5);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(0, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(0, false)));
    setUnits(expectedSelectedUnitsWithDependents, t2, a2, i2, t4, a4, i1, t1, a1, t5, t6);
    setUnits(expectedSelectedUnits, t1, t2, t4, t5, t6);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(1, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(1, false)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t2, a2, i2, t3, i3, t5, t6);
    setUnits(expectedSelectedUnits, t1, t2, t3, t5, t6);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(2, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(2, false)));
    setUnits(expectedSelectedUnitsWithDependents, t2, i2, a2, t3, i3, t4, a4, t5, t6);
    setUnits(expectedSelectedUnits, t2, t3, t4, t5, t6);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(3, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(3, false)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t3, i3, t4, a4, t5, t6);
    setUnits(expectedSelectedUnits, t1, t3, t4, t5, t6);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(4, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(4, false)));
    // implicitDependents:
    // YES
    // categorizeMovement:
    // YES
    // allUnits:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t4[a4, ],t5[ , ],t6[ , ],t7[i7,I7],bb1,bb2
    // chosenUnits:
    // trn, trn, inf
    // candidateCompositeCategories:
    // [trn[inf],trn] => exact
    // [trnM1[arm,inf],trnM2[arm,inf]] => greedy
    // [trnM1[arm,inf],trn[inf]] => greedy
    // [trnM1[arm,inf],trn[arm]] => greedy
    // [trnM1[arm,inf],trn] => greedy
    // [trnM1[arm,inf],trn[inf,inf]] => greedy
    // [trnM2[arm,inf],trn[inf]] => greedy
    // [trnM2[arm,inf],trn[arm]] => greedy
    // [trnM2[arm,inf],trn] => greedy
    // [trnM2[arm,inf],trn[inf,inf]] => greedy
    // [trn[inf],trn[arm]] => greedy
    // [trn[inf],trn[inf,inf]] => greedy
    // [trn[arm],trn[inf,inf]] => greedy
    // [trn,trn[inf,inf]] => greedy
    // candidateUnits:
    // t1[a1,i1],t2[a2,i2],t3[ ,i3],t4[a4, ],t5[ , ],t6[ , ],t7[i7,I7]
    // exactSolutions:
    // t3[ ,i3],t5[ , ]
    // greedySolutions:
    // t1[a1,i1],t2[a2,i2]
    // t1[a1,i1],t3[ ,i3]
    // t1[a1,i1],t4[a4, ]
    // t1[a1,i1],t5[ , ]
    // t1[a1,i1],t7[i7,I7]
    // t2[a2,i2],t3[ ,i3]
    // t2[a2,i2],t4[a4, ]
    // t2[a2,i2],t5[ , ]
    // t2[a2,i2],t7[i7,I7]
    // t3[ ,i3],t4[a4, ]
    // t3[ ,i3],t7[i7,I7]
    // t4[a4, ],t7[i7,I7]
    // t5[ , ],t7[i7,I7]
    // incompleteSolution:
    // none
    // exactSolutionCount:
    // 1
    // solutionCount:
    // 14
    setUnits(allUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, t5, t6, t7, i7, I7, bb1, bb2);
    setUnits(chosenUnits, t1, t4, i3);
    autoChooser = new UnitAutoChooser(allUnits, chosenUnits, mustMoveWith, bImplicitDependents, bCategorizeMovement);
    assertEquals(1, autoChooser.exactSolutionCount());
    assertEquals(14, autoChooser.solutionCount());
    assertTrue(autoChooser.foundCompleteSolution());
    setUnits(expectedCandidateUnits, t1, a1, i1, t2, a2, i2, t3, i3, t4, a4, t5, t6, t7, i7, I7);
    assertEquals(TestUnit.createSet(expectedCandidateUnits), TestUnit.createSet(autoChooser.getCandidateUnits(true)));
    setUnits(expectedSelectedUnitsWithDependents, t3, i3, t5);
    setUnits(expectedSelectedUnits, t3, i3, t5);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(0, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(0, false)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t2, a2, i2);
    setUnits(expectedSelectedUnits, t1, i2, t2);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(1, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(1, false)));
    setUnits(expectedSelectedUnitsWithDependents, t2, a2, i2, t4, a4);
    setUnits(expectedSelectedUnits, t2, i2, t4);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(2, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(2, false)));
    setUnits(expectedSelectedUnitsWithDependents, t2, a2, i2, t7, i7, I7);
    setUnits(expectedSelectedUnits, t2, i2, t7);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(3, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(3, false)));
    setUnits(expectedSelectedUnitsWithDependents, t2, a2, i2, t3, i3);
    setUnits(expectedSelectedUnits, t2, i2, t3);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(4, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(4, false)));
    setUnits(expectedSelectedUnitsWithDependents, t2, a2, i2, t5);
    setUnits(expectedSelectedUnits, t2, i2, t5);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(5, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(5, false)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t4, a4);
    setUnits(expectedSelectedUnits, t1, i1, t4);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(6, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(6, false)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t7, i7, I7);
    setUnits(expectedSelectedUnits, t1, i1, t7);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(7, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(7, false)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t3, i3);
    setUnits(expectedSelectedUnits, t1, i1, t3);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(8, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(8, false)));
    setUnits(expectedSelectedUnitsWithDependents, t1, a1, i1, t5);
    setUnits(expectedSelectedUnits, t1, i1, t5);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(9, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(9, false)));
    setUnits(expectedSelectedUnitsWithDependents, t7, i7, t4, a4, I7);
    setUnits(expectedSelectedUnits, t7, i7, t4);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(10, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(10, false)));
    setUnits(expectedSelectedUnitsWithDependents, t3, i3, t4, a4);
    setUnits(expectedSelectedUnits, t3, i3, t4);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(11, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(11, false)));
    setUnits(expectedSelectedUnitsWithDependents, t3, i3, t7, i7, I7);
    setUnits(expectedSelectedUnits, t3, t7, i7);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(12, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(12, false)));
    setUnits(expectedSelectedUnitsWithDependents, t5, t7, i7, I7);
    setUnits(expectedSelectedUnits, t5, t7, i7);
    assertEquals(TestUnit.createSet(expectedSelectedUnitsWithDependents),
        TestUnit.createSet(autoChooser.getSolution(13, true)));
    assertEquals(TestUnit.createSet(expectedSelectedUnits), TestUnit.createSet(autoChooser.getSolution(13, false)));
  }
}
