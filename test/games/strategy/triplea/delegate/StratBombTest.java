package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.RouteScripted;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.random.IRandomSource;
import games.strategy.engine.random.ScriptedRandomSource;
import games.strategy.test.TestUtil;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.player.ITripleAPlayer;
import games.strategy.triplea.xml.LoadGameUtil;

public class StratBombTest {
  private GameData m_data;

  @Before
  public void setUp() throws Exception {
    m_data = LoadGameUtil.loadTestGame("ww2global40_2nd_edition_test.xml");
  }

  private ITestDelegateBridge getDelegateBridge(final PlayerID player) {
    return GameDataTestUtil.getDelegateBridge(player, m_data);
  }


  @Test
  public void testBombingRaid2targets() {
    final Territory germany = m_data.getMap().getTerritory("Germany");
    final Territory uk = m_data.getMap().getTerritory("United Kingdom");
    final PlayerID germans = GameDataTestUtil.germans(m_data);
    final PlayerID british = GameDataTestUtil.british(m_data);
    // add a unit
    final Unit stratBomber = GameDataTestUtil.bomber(m_data).create(british);
    final Unit tacBomber1 = GameDataTestUtil.tacBomber(m_data).create(british);
    final Unit tacBomber2 = GameDataTestUtil.tacBomber(m_data).create(british);
    m_data.performChange(ChangeFactory.addUnits(uk, Collections.singleton(stratBomber)));
    m_data.performChange(ChangeFactory.addUnits(uk, Collections.singleton(tacBomber1)));
    m_data.performChange(ChangeFactory.addUnits(uk, Collections.singleton(tacBomber2)));
    final BattleTracker tracker = new BattleTracker();
    List<Unit> attackers = Collections.emptyList();
    attackers.add(stratBomber);
    attackers.add(tacBomber1);
    attackers.add(tacBomber2);
    HashMap<Unit, HashSet<Unit>> targets = null;
   

//    final Collection<Unit> enemyTargets = Match.getMatches(uk.getUnits().getMatches(new CompositeMatchAnd<>(Matches.enemyUnit(germans, m_data),              Matches.UnitIsAtMaxDamageOrNotCanBeDamaged(uk).invert(), Matches.unitIsBeingTransported().invert())), Matches.UnitIsLegalBombingTargetBy(stratBomber));

    for( final Unit target : germany.getUnits().getUnits() ) {
    	System.out.println(target.getType().getName());
    	switch( target.getType().getName() ) {
				case "airfield"      : targets.put(target, new HashSet<>(Collections.singleton(tacBomber1)));
					break;
				case "harbour"       : targets.put(target, new HashSet<>(Collections.singleton(tacBomber2)));
					break;
				case "factory_major" : targets.put(target, new HashSet<>(Collections.singleton(stratBomber)));
					break;
		  }
		}
    final ITestDelegateBridge bridge = getDelegateBridge(british);
    tracker.addBattle(new RouteScripted(germany), attackers, true, british, bridge, null, null, targets, true);
                
    final StrategicBombingRaidBattle battle = new StrategicBombingRaidBattle(germany, m_data, british, tracker);
    battle.addAttackChange(m_data.getMap().getRoute(uk, germany), uk.getUnits().getMatches(Matches.UnitIsStrategicBomber), null);
    addTo(germany, uk.getUnits().getMatches(Matches.UnitIsStrategicBomber));
    tracker.getBattleRecords(m_data).addBattle(british, battle.getBattleID(), germany, battle.getBattleType(), m_data);
    // aa guns rolls 1,3, first one hits, remaining bomber rolls 1 dice at 2
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {3, 3, 2}));
    // if we try to move aa, then the game will ask us if we want to move
    // fail if we are called
    final InvocationHandler handler = new InvocationHandler() {
      @Override
      public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        return null;
      }
    };
    final ITripleAPlayer player = (ITripleAPlayer) Proxy
        .newProxyInstance(Thread.currentThread().getContextClassLoader(),
            TestUtil.getClassArrayFrom(ITripleAPlayer.class), handler);
    bridge.setRemote(player);
    final int PUsBeforeRaid = germans.getResources().getQuantity(m_data.getResourceList().getResource(Constants.PUS));
    battle.fight(bridge);
    final int PUsAfterRaid = germans.getResources().getQuantity(m_data.getResourceList().getResource(Constants.PUS));
    // targets dice is 4, so damage is 1 + 4 = 5
    // bomber 2 hits at 2, so damage is 3, for a total of 8
    // Changed to match StrategicBombingRaidBattle changes
    assertEquals(PUsBeforeRaid - 8, PUsAfterRaid);
  }
}
