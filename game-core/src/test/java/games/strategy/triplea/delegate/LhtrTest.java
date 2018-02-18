package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.random.ScriptedRandomSource;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.player.ITripleAPlayer;
import games.strategy.triplea.xml.TestMapGameData;

public class LhtrTest {
  private GameData gameData;

  @BeforeEach
  public void setUp() throws Exception {
    gameData = TestMapGameData.LHTR.getGameData();
  }

  private ITestDelegateBridge getDelegateBridge(final PlayerID player) {
    return GameDataTestUtil.getDelegateBridge(player, gameData);
  }

  @Test
  public void testFightersCanLandOnNewPlacedCarrier() {
    final MoveDelegate delegate = (MoveDelegate) gameData.getDelegateList().getDelegate("move");
    delegate.initialize("MoveDelegate", "MoveDelegate");
    final PlayerID germans = GameDataTestUtil.germans(gameData);
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    bridge.setStepName("germanNonCombatMove");
    delegate.setDelegateBridgeAndPlayer(bridge);
    delegate.start();
    final Territory baltic = gameData.getMap().getTerritory("5 Sea Zone");
    final Territory easternEurope = gameData.getMap().getTerritory("Eastern Europe");
    final UnitType carrirType = GameDataTestUtil.carrier(gameData);
    // move a fighter to the baltic
    final Route route = new Route();
    route.setStart(easternEurope);
    route.add(baltic);
    final UnitType fighterType = GameDataTestUtil.fighter(gameData);
    delegate.move(easternEurope.getUnits().getMatches(Matches.unitIsOfType(fighterType)), route);
    // add a carrier to be produced in germany
    final TripleAUnit carrier = new TripleAUnit(carrirType, germans, gameData);
    gameData.performChange(ChangeFactory.addUnits(germans, Collections.singleton(carrier)));
    // end the move phase
    delegate.end();
    // make sure the fighter is still there
    // in lhtr fighters can hover, and carriers placed beneath them
    assertTrue(baltic.getUnits().anyMatch(Matches.unitIsOfType(fighterType)));
  }

  @Test
  public void testFightersDestroyedWhenNoPendingCarriers() {
    final MoveDelegate delegate = (MoveDelegate) gameData.getDelegateList().getDelegate("move");
    delegate.initialize("MoveDelegate", "MoveDelegate");
    final PlayerID germans = GameDataTestUtil.germans(gameData);
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    bridge.setStepName("germanNonCombatMove");
    delegate.setDelegateBridgeAndPlayer(bridge);
    delegate.start();
    final Territory baltic = gameData.getMap().getTerritory("5 Sea Zone");
    final Territory easternEurope = gameData.getMap().getTerritory("Eastern Europe");
    // move a fighter to the baltic
    final Route route = new Route();
    route.setStart(easternEurope);
    route.add(baltic);
    final UnitType fighterType = GameDataTestUtil.fighter(gameData);
    delegate.move(easternEurope.getUnits().getMatches(Matches.unitIsOfType(fighterType)), route);
    // end the move phase
    delegate.end();
    // there is no pending carrier to be placed
    // the fighter cannot hover
    assertFalse(baltic.getUnits().anyMatch(Matches.unitIsOfType(fighterType)));
  }

  @Test
  public void testAaGunsDontFireNonCombat() {
    final MoveDelegate delegate = (MoveDelegate) gameData.getDelegateList().getDelegate("move");
    delegate.initialize("MoveDelegate", "MoveDelegate");
    final PlayerID germans = GameDataTestUtil.germans(gameData);
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    bridge.setStepName("germanNonCombatMove");
    delegate.setDelegateBridgeAndPlayer(bridge);
    delegate.start();
    // if we try to move aa, then the game will ask us if we want to move
    // fail if we are called
    final ITripleAPlayer player = (ITripleAPlayer) Proxy
        .newProxyInstance(Thread.currentThread().getContextClassLoader(),
            new Class<?>[] {ITripleAPlayer.class}, (p, m, a) -> fail("method called:" + m));
    bridge.setRemote(player);
    // move 1 fighter over the aa gun in caucus
    final Route route = new Route();
    route.setStart(gameData.getMap().getTerritory("Ukraine S.S.R."));
    route.add(gameData.getMap().getTerritory("Caucasus"));
    route.add(gameData.getMap().getTerritory("West Russia"));
    final List<Unit> fighter = route.getStart().getUnits().getMatches(Matches.unitIsAir());
    delegate.move(fighter, route);
  }

  @Test
  public void testSubDefenseBonus() {
    final UnitType sub = GameDataTestUtil.submarine(gameData);
    final UnitAttachment attachment = UnitAttachment.get(sub);
    final PlayerID japanese = GameDataTestUtil.japanese(gameData);
    // before the advance, subs defend and attack at 2
    assertEquals(2, attachment.getDefense(japanese));
    assertEquals(2, attachment.getAttack(japanese));
    final ITestDelegateBridge bridge = getDelegateBridge(japanese);
    TechTracker.addAdvance(japanese, bridge,
        TechAdvance.findAdvance(TechAdvance.TECH_PROPERTY_SUPER_SUBS, gameData, japanese));
    // after tech advance, this is now 3
    assertEquals(3, attachment.getDefense(japanese));
    assertEquals(3, attachment.getAttack(japanese));
    // make sure this only changes for the player with the tech
    final PlayerID americans = GameDataTestUtil.americans(gameData);
    assertEquals(2, attachment.getDefense(americans));
    assertEquals(2, attachment.getAttack(americans));
  }

  @Test
  public void testLhtrBombingRaid() {
    final Territory germany = gameData.getMap().getTerritory("Germany");
    final Territory uk = gameData.getMap().getTerritory("United Kingdom");
    final PlayerID germans = GameDataTestUtil.germans(gameData);
    final PlayerID british = GameDataTestUtil.british(gameData);
    final BattleTracker tracker = new BattleTracker();
    final StrategicBombingRaidBattle battle = new StrategicBombingRaidBattle(germany, gameData, british, tracker);
    battle.addAttackChange(gameData.getMap().getRoute(uk, germany),
        uk.getUnits().getMatches(Matches.unitIsStrategicBomber()), null);
    addTo(germany, uk.getUnits().getMatches(Matches.unitIsStrategicBomber()));
    tracker.getBattleRecords().addBattle(british, battle.getBattleId(), germany, battle.getBattleType());
    final ITestDelegateBridge bridge = getDelegateBridge(british);
    TechTracker.addAdvance(british, bridge,
        TechAdvance.findAdvance(TechAdvance.TECH_PROPERTY_HEAVY_BOMBER, gameData, british));
    // aa guns rolls 3, misses, bomber rolls 2 dice at 3 and 4
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {2, 2, 3}));
    // if we try to move aa, then the game will ask us if we want to move
    // fail if we are called
    final ITripleAPlayer player = (ITripleAPlayer) Proxy
        .newProxyInstance(Thread.currentThread().getContextClassLoader(),
            new Class<?>[] {ITripleAPlayer.class}, (p, m, a) -> null);
    bridge.setRemote(player);
    final int pusBeforeRaid = germans.getResources().getQuantity(gameData.getResourceList().getResource(Constants.PUS));
    battle.fight(bridge);
    final int pusAfterRaid = germans.getResources().getQuantity(gameData.getResourceList().getResource(Constants.PUS));
    // targets dice is 4, so damage is 1 + 4 = 5
    // Changed to match StrategicBombingRaidBattle changes
    assertEquals(pusBeforeRaid - 5, pusAfterRaid);
  }

  @Test
  public void testLhtrBombingRaid2Bombers() {
    final Territory germany = gameData.getMap().getTerritory("Germany");
    final Territory uk = gameData.getMap().getTerritory("United Kingdom");
    final PlayerID germans = GameDataTestUtil.germans(gameData);
    final PlayerID british = GameDataTestUtil.british(gameData);
    // add a unit
    final Unit bomber = GameDataTestUtil.bomber(gameData).create(british);
    final Change change = ChangeFactory.addUnits(uk, Collections.singleton(bomber));
    gameData.performChange(change);
    final BattleTracker tracker = new BattleTracker();
    final StrategicBombingRaidBattle battle = new StrategicBombingRaidBattle(germany, gameData, british, tracker);
    battle.addAttackChange(gameData.getMap().getRoute(uk, germany),
        uk.getUnits().getMatches(Matches.unitIsStrategicBomber()), null);
    addTo(germany, uk.getUnits().getMatches(Matches.unitIsStrategicBomber()));
    tracker.getBattleRecords().addBattle(british, battle.getBattleId(), germany, battle.getBattleType());
    final ITestDelegateBridge bridge = getDelegateBridge(british);
    TechTracker.addAdvance(british, bridge,
        TechAdvance.findAdvance(TechAdvance.TECH_PROPERTY_HEAVY_BOMBER, gameData, british));
    // aa guns rolls 3,3 both miss, bomber 1 rolls 2 dice at 3,4 and bomber 2 rolls dice at 1,2
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {3, 3, 2, 3, 0, 1}));
    // if we try to move aa, then the game will ask us if we want to move
    // fail if we are called
    final ITripleAPlayer player = (ITripleAPlayer) Proxy
        .newProxyInstance(Thread.currentThread().getContextClassLoader(),
            new Class<?>[] {ITripleAPlayer.class}, (p, m, a) -> null);
    bridge.setRemote(player);
    final int pusBeforeRaid = germans.getResources().getQuantity(gameData.getResourceList().getResource(Constants.PUS));
    battle.fight(bridge);
    final int pusAfterRaid = germans.getResources().getQuantity(gameData.getResourceList().getResource(Constants.PUS));
    // targets dice is 4, so damage is 1 + 4 = 5
    // bomber 2 hits at 2, so damage is 3, for a total of 8
    // Changed to match StrategicBombingRaidBattle changes
    assertEquals(pusBeforeRaid - 8, pusAfterRaid);
  }
}
