package games.strategy.triplea.delegate;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.RouteScripted;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.random.ScriptedRandomSource;
import games.strategy.test.TestUtil;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.player.ITripleAPlayer;
import games.strategy.triplea.xml.LoadGameUtil;

// Test Global 1940 rule of having strat bombing first, then amphibious assaults, then others. Also removes some auto resolved combats.

public class BattleOrderTest {
  private GameData m_data;

  @Before
  public void setUp() throws Exception {
    m_data = LoadGameUtil.loadTestGame(LoadGameUtil.TestMapXml.GLOBAL1940);
  }

  private ITestDelegateBridge getDelegateBridge(final PlayerID player) {
    return GameDataTestUtil.getDelegateBridge(player, m_data);
  }


  @Test
  public void BattleTest() {
	final BattleDelegate delegate = (BattleDelegate) m_data.getDelegateList().getDelegate("battle");
	delegate.initialize("BattleDelegateOrdered", "BattleDelegateOrdered");
	final Territory wgermany = m_data.getMap().getTerritory("Western Germany");
    final Territory uk = m_data.getMap().getTerritory("United Kingdom");
    final Territory iceland = m_data.getMap().getTerritory("Iceland");
    final Territory sz122 = m_data.getMap().getTerritory("122 Sea Zone");
    final PlayerID british = GameDataTestUtil.british(m_data);
    final PlayerID germans = GameDataTestUtil.germans(m_data);
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    bridge.setStepName("germansBattle");
    delegate.setDelegateBridgeAndPlayer(bridge);
    delegate.start();
    assertEquals( iceland.getOwner() , germans );
  }
}
