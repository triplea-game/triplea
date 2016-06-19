package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.GameDataTestUtil.battleDelegate;
import static games.strategy.triplea.delegate.GameDataTestUtil.battleship;
import static games.strategy.triplea.delegate.GameDataTestUtil.carrier;
import static games.strategy.triplea.delegate.GameDataTestUtil.fighter;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.italians;
import static games.strategy.triplea.delegate.GameDataTestUtil.move;
import static games.strategy.triplea.delegate.GameDataTestUtil.moveDelegate;
import static games.strategy.triplea.delegate.GameDataTestUtil.removeFrom;
import static games.strategy.triplea.delegate.GameDataTestUtil.russians;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.util.DummyTripleAPlayer;
import games.strategy.triplea.xml.LoadGameUtil;

public class WW2V3_42_Test {
  private GameData m_data;

  @Before
  public void setUp() throws Exception {
    m_data = LoadGameUtil.loadTestGame("ww2v3_1942_test.xml");
  }

  private ITestDelegateBridge getDelegateBridge(final PlayerID player) {
    return GameDataTestUtil.getDelegateBridge(player, m_data);
  }

  @Test
  public void testTransportAttack() {
    final Territory sz13 = m_data.getMap().getTerritory("13 Sea Zone");
    final Territory sz12 = m_data.getMap().getTerritory("12 Sea Zone");
    final PlayerID germans = germans(m_data);
    final MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    bridge.setStepName("CombatMove");
    moveDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(m_data).start();
    final Route sz13To12 = new Route();
    sz13To12.setStart(sz13);
    sz13To12.add(sz12);
    final List<Unit> transports = sz13.getUnits().getMatches(Matches.UnitIsTransport);
    assertEquals(1, transports.size());
    final String error = moveDelegate.move(transports, sz13To12);
    assertEquals(error, null);
  }

  @Test
  public void testBombAndAttackEmptyTerritory() {
    final Territory karrelia = territory("Karelia S.S.R.", m_data);
    final Territory baltic = territory("Baltic States", m_data);
    final Territory sz5 = territory("5 Sea Zone", m_data);
    final Territory germany = territory("Germany", m_data);
    final PlayerID germans = germans(m_data);
    final MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    bridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    bridge.setRemote(new DummyTripleAPlayer() {
      @Override
      public boolean shouldBomberBomb(final Territory territory) {
        return true;
      }
    });
    // remove the russian units
    removeFrom(karrelia, karrelia.getUnits().getMatches(Matches.UnitCanBeDamaged.invert()));
    // move the bomber to attack
    move(germany.getUnits().getMatches(Matches.UnitIsStrategicBomber), new Route(germany, sz5, karrelia));
    // move an infantry to invade
    move(baltic.getUnits().getMatches(Matches.UnitIsInfantry), new Route(baltic, karrelia));
    final BattleTracker battleTracker = MoveDelegate.getBattleTracker(m_data);
    // we should have a pending land battle, and a pending bombing raid
    assertNotNull(battleTracker.getPendingBattle(karrelia, false, null));
    assertNotNull(battleTracker.getPendingBattle(karrelia, true, null));
    // the territory should not be conquered
    assertEquals(karrelia.getOwner(), russians(m_data));
  }

  @Test
  public void testLingeringSeaUnitsJoinBattle() throws Exception {
    final Territory sz5 = territory("5 Sea Zone", m_data);
    final Territory sz6 = territory("6 Sea Zone", m_data);
    final Territory sz7 = territory("7 Sea Zone", m_data);
    // add a russian battlship
    addTo(sz5, battleship(m_data).create(1, russians(m_data)));
    final ITestDelegateBridge bridge = getDelegateBridge(germans(m_data));
    bridge.setStepName("CombatMove");
    moveDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(m_data).start();
    // attack with a german sub
    move(sz7.getUnits().getUnits(), new Route(sz7, sz6, sz5));
    moveDelegate(m_data).end();
    // adding of lingering units was moved from end of combat-move phase, to start of battle phase
    battleDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    battleDelegate(m_data).start();
    // all units in sz5 should be involved in the battle
    final MustFightBattle mfb =
        (MustFightBattle) MoveDelegate.getBattleTracker(m_data).getPendingBattle(sz5, false, null);
    assertEquals(5, mfb.getAttackingUnits().size());
  }

  @Test
  public void testLingeringFightersAndALliedUnitsJoinBattle() throws Exception {
    final Territory sz5 = territory("5 Sea Zone", m_data);
    final Territory sz6 = territory("6 Sea Zone", m_data);
    final Territory sz7 = territory("7 Sea Zone", m_data);
    // add a russian battlship
    addTo(sz5, battleship(m_data).create(1, russians(m_data)));
    // add an allied carrier and a fighter
    addTo(sz5, carrier(m_data).create(1, italians(m_data)));
    addTo(sz5, fighter(m_data).create(1, germans(m_data)));
    final ITestDelegateBridge bridge = getDelegateBridge(germans(m_data));
    bridge.setStepName("CombatMove");
    moveDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(m_data).start();
    // attack with a german sub
    move(sz7.getUnits().getUnits(), new Route(sz7, sz6, sz5));
    moveDelegate(m_data).end();
    // adding of lingering units was moved from end of combat-move phase, to start of battle phase
    battleDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    battleDelegate(m_data).start();
    // all units in sz5 should be involved in the battle
    // except the italian carrier
    final MustFightBattle mfb =
        (MustFightBattle) MoveDelegate.getBattleTracker(m_data).getPendingBattle(sz5, false, null);
    assertEquals(6, mfb.getAttackingUnits().size());
  }

  @Test
  public void testLingeringSeaUnitsCanMoveAwayFromBattle() throws Exception {
    final Territory sz5 = territory("5 Sea Zone", m_data);
    final Territory sz6 = territory("6 Sea Zone", m_data);
    final Territory sz7 = territory("7 Sea Zone", m_data);
    // add a russian battlship
    addTo(sz5, battleship(m_data).create(1, russians(m_data)));
    final ITestDelegateBridge bridge = getDelegateBridge(germans(m_data));
    bridge.setStepName("CombatMove");
    moveDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(m_data).start();
    // attack with a german sub
    move(sz7.getUnits().getUnits(), new Route(sz7, sz6, sz5));
    // move the transport away
    move(sz5.getUnits().getMatches(Matches.UnitIsTransport), new Route(sz5, sz6));
    moveDelegate(m_data).end();
    // adding of lingering units was moved from end of combat-move phase, to start of battle phase
    battleDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    battleDelegate(m_data).start();
    // all units in sz5 should be involved in the battle
    final MustFightBattle mfb =
        (MustFightBattle) MoveDelegate.getBattleTracker(m_data).getPendingBattle(sz5, false, null);
    assertEquals(4, mfb.getAttackingUnits().size());
  }
}
