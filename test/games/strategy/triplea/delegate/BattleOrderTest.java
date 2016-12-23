package games.strategy.triplea.delegate;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import java.io.FileWriter;
import java.io.IOException;
import java.io.File;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import games.strategy.engine.data.export.GameDataExporter;
import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.RouteScripted;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.random.ScriptedRandomSource;
import games.strategy.test.TestUtil;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.player.ITripleAPlayer;
import games.strategy.triplea.xml.LoadGameUtil;

// Test Global 1940 rule of having strat bombing first, then amphibious assaults, then others. Also removes some auto
// resolved combats.

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
    // final InitializationDelegate initDel =
    // (InitializationDelegate) m_data.getDelegateList().getDelegate("initDelegate");
    final BattleDelegate battleDelegate = (BattleDelegate) m_data.getDelegateList().getDelegate("battle");
    final MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
    final Territory wGermany = m_data.getMap().getTerritory("Western Germany");
    final Territory scotland = m_data.getMap().getTerritory("Scotland");
    final Territory iceland = m_data.getMap().getTerritory("Iceland");
    final Territory sz123 = m_data.getMap().getTerritory("123 Sea Zone");
    final Territory sz114 = m_data.getMap().getTerritory("114 Sea Zone");
    final Territory sz115 = m_data.getMap().getTerritory("115 Sea Zone");
    final Territory balticStates = m_data.getMap().getTerritory("Baltic States");
    final Territory easternPoland = m_data.getMap().getTerritory("Eastern Poland");
    final PlayerID germans = GameDataTestUtil.germans(m_data);
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    Collection<Unit> balticFleet = new ArrayList<>();
    Collection<Unit> icelandicAssault = new ArrayList<>();
    Collection<Unit> bsAssault = new ArrayList<>();
    Collection<Unit> scotRaid = new ArrayList<>();
    bridge.setStepName("germansCombatMove");
    moveDelegate.initialize("MoveDelegate", "MoveDelegate");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();

    // Find inf in SZ123
    for (final Unit aUnit : sz123.getUnits().getUnits()) {
      if (aUnit.getType().getName().equals("infantry")) {
        icelandicAssault.add(aUnit);
      }
    }
    moveDelegate.move(icelandicAssault, new Route(sz123, iceland));

    // There is doubtless a better way of doing this, but this way works
    for (final Unit aUnit : sz114.getUnits().getUnits()) {
      balticFleet.add(aUnit);
    }
    moveDelegate.move(balticFleet, new Route(sz114, sz115));

    // Find inf & art in SZ115
    for (final Unit aUnit : sz115.getUnits().getUnits()) {
      if (aUnit.getType().getName().equals("infantry") || aUnit.getType().getName().equals("artillery")) {
        bsAssault.add(aUnit);
      }
    }
    moveDelegate.move(bsAssault, new Route(sz115, balticStates));

    /*
     * Find Strat Bombers in W Germany
     * for( final Unit aUnit : wGermany.getUnits().getUnits() ) {
     * if( aUnit.getType().getName().equals( "bomber" ) ) {
     * scotRaid.add( aUnit );
     * }
     * }
     * moveDelegate.move( scotRaid, new Route( wGermany, scotland ) );
     */

    moveDelegate.end();

    final GameDataExporter exporter1 = new games.strategy.engine.data.export.GameDataExporter(m_data, false);
    try {
      final FileWriter writer = new FileWriter("xml_b4.xml");
      writer.write(exporter1.getXML());
      writer.close();
    } catch (final IOException e1) {
      ClientLogger.logQuietly(e1);
    }

    bridge.setStepName("germansBattle");
    battleDelegate.initialize("BattleDelegateOrdered", "BattleDelegateOrdered");
    // Germans hit, Soviets miss. Prevents casualty selection popup box
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {1, 3, 6, 1, 1, 1, 1, 1, 6, 6, 6, 1, 1, 1, 6, 6, 6}));
    battleDelegate.setDelegateBridgeAndPlayer(bridge);

    battleDelegate.start();
/*    } catch (Exception e) {
      final GameDataExporter exporter = new games.strategy.engine.data.export.GameDataExporter(m_data, false);
      try {
        final FileWriter writer = new FileWriter("xml1.xml");
        writer.write(exporter.getXML());
        writer.close();
      } catch (final IOException e2) {
        ClientLogger.logQuietly(e2);
      }
    }*/

    assertEquals(germans, iceland.getOwner()); // Ensure that the amphibious assault on Iceland actually happened

    /*
     * Count bombers left in Scotland
     * int bombers = 0;
     * for( final Unit aUnit : scotland.getUnits().getUnits() ) {
     * if( aUnit.getType().getName().equals( "bomber" ) ) {
     * bombers++;
     * }
     * }
     * 
     * assertEquals( bombers, 1 ); // One shot by AAA, one bombs
     */
    assertEquals(germans, balticStates.getOwner()); // Ensure assault succeeded and occurred.
    // Ownership won't change without amphibious units

    int transports = 0;
    for (final Unit aUnit : sz123.getUnits().getUnits()) {
      if (aUnit.getType().getName().equals("transport")) {
        transports++;
      }
    }

    assertEquals(1, transports); // Only german TT should be alive
    assertEquals(2, sz123.getUnits().getUnits().size()); // Even better, only two units should be present

    // assertEquals( germans, easternPoland.getOwner() ); // Ensure last battle occurred.

  }
}
