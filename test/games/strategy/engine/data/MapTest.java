package games.strategy.engine.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import games.strategy.util.Match;

public class MapTest {
  Territory aa;
  Territory ab;
  Territory ac;
  Territory ad;
  Territory ba;
  Territory bb;
  Territory bc;
  Territory bd;
  Territory ca;
  Territory cb;
  Territory cc;
  Territory cd;
  Territory da;
  Territory db;
  Territory dc;
  Territory dd;
  Territory nowhere;
  GameMap map;

  @Before
  public void setUp() {
    // map, l is land, w is water
    // each territory is connected to
    // it's direct neighbors, but not diagonals
    // llll
    // llww
    // llwl
    // llww
    aa = new Territory("aa", false, null);
    ab = new Territory("ab", false, null);
    ac = new Territory("ac", false, null);
    ad = new Territory("ad", false, null);
    ba = new Territory("ba", false, null);
    bb = new Territory("bb", false, null);
    bc = new Territory("bc", true, null);
    bd = new Territory("bd", true, null);
    ca = new Territory("ca", false, null);
    cb = new Territory("cb", false, null);
    cc = new Territory("cc", true, null);
    cd = new Territory("cd", false, null);
    da = new Territory("da", false, null);
    db = new Territory("db", false, null);
    dc = new Territory("dc", true, null);
    dd = new Territory("dd", true, null);
    map = new GameMap(null);
    map.addTerritory(aa);
    map.addTerritory(ab);
    map.addTerritory(ac);
    map.addTerritory(ad);
    map.addTerritory(ba);
    map.addTerritory(bb);
    map.addTerritory(bc);
    map.addTerritory(bd);
    map.addTerritory(ca);
    map.addTerritory(cb);
    map.addTerritory(cc);
    map.addTerritory(cd);
    map.addTerritory(da);
    map.addTerritory(db);
    map.addTerritory(dc);
    map.addTerritory(dd);
    map.addConnection(aa, ab);
    map.addConnection(ab, ac);
    map.addConnection(ac, ad);
    map.addConnection(ba, bb);
    map.addConnection(bb, bc);
    map.addConnection(bc, bd);
    map.addConnection(ca, cb);
    map.addConnection(cb, cc);
    map.addConnection(cc, cd);
    map.addConnection(da, db);
    map.addConnection(db, dc);
    map.addConnection(dc, dd);
    map.addConnection(aa, ba);
    map.addConnection(ba, ca);
    map.addConnection(ca, da);
    map.addConnection(ab, bb);
    map.addConnection(bb, cb);
    map.addConnection(cb, db);
    map.addConnection(ac, bc);
    map.addConnection(bc, cc);
    map.addConnection(cc, dc);
    map.addConnection(ad, bd);
    map.addConnection(bd, cd);
    map.addConnection(cd, dd);
    nowhere = new Territory("nowhere", false, null);
  }

  @Test
  public void testNowhere() {
    assertTrue(-1 == map.getDistance(aa, nowhere));
  }

  @Test
  public void testCantFindByName() {
    assertNull(map.getTerritory("nowhere"));
  }

  @Test
  public void testCanFindByName() {
    assertNotNull(map.getTerritory("aa"));
  }

  @Test
  public void testSame() {
    assertTrue(0 == map.getDistance(aa, aa));
  }

  @Test
  public void testImpossibleConditionRoute() {
    final Match<Territory> test = new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        return false;
      }
    };
    assertNull(map.getRoute(aa, ba, test));
  }

  @Test
  public void testOne() {
    final int distance = map.getDistance(aa, ab);
    assertTrue("" + distance, 1 == distance);
  }

  @Test
  public void testTwo() {
    final int distance = map.getDistance(aa, ac);
    assertTrue("" + distance, 2 == distance);
  }

  @Test
  public void testOverWater() {
    assertTrue(map.getDistance(ca, cd) == 3);
  }

  @Test
  public void testOverWaterCantReach() {
    assertTrue(map.getLandDistance(ca, cd) == -1);
  }

  @Test
  public void testLong() {
    assertTrue(map.getLandDistance(ad, da) == 6);
  }

  @Test
  public void testLongRoute() {
    final Route route = map.getLandRoute(ad, da);
    assertEquals(route.numberOfSteps(), 6);
  }

  @Test
  public void testNeighborLandNoSeaConnect() {
    assertTrue(-1 == map.getWaterDistance(aa, ab));
  }

  public void testNeighborSeaNoLandConnect() {
    assertTrue(-1 == map.getLandDistance(bc, bd));
  }

  @Test
  public void testRouteToSelf() {
    final Route rt = map.getRoute(aa, aa);
    assertTrue(rt.numberOfSteps() == 0);
  }

  @Test
  public void testRouteSizeOne() {
    final Route rt = map.getRoute(aa, ab);
    assertTrue(rt.numberOfSteps() == 1);
  }

  @Test
  public void testImpossibleRoute() {
    final Route rt = map.getRoute(aa, nowhere);
    assertNull(rt);
  }

  @Test
  public void testImpossibleLandRoute() {
    final Route rt = map.getLandRoute(aa, cd);
    assertTrue(rt == null);
  }

  @Test
  public void testImpossibleLandDistance() {
    final int distance = map.getLandDistance(aa, cd);
    assertTrue("wrongDistance exp -1, got:" + distance, distance == -1);
  }

  @Test
  public void testWaterRout() {
    final Route rt = map.getWaterRoute(bd, dd);
    assertTrue("bc:" + rt, rt.getTerritoryAtStep(0).equals(bc));
    assertTrue("cc", rt.getTerritoryAtStep(1).equals(cc));
    assertTrue("dc", rt.getTerritoryAtStep(2).equals(dc));
    assertTrue("dd", rt.getTerritoryAtStep(3).equals(dd));
  }

  @Test
  public void testMultiplePossible() {
    final Route rt = map.getRoute(aa, dd);
    assertEquals(rt.getStart(), aa);
    assertEquals(rt.getEnd(), dd);
    assertEquals(rt.numberOfSteps(), 6);
  }

  @Test
  public void testNeighbors() {
    final Set<Territory> neighbors = map.getNeighbors(aa);
    assertTrue(neighbors.size() == 2);
    assertTrue(neighbors.contains(ab));
    assertTrue(neighbors.contains(ba));
  }

  @Test
  public void testNeighborsWithDistance() {
    Set<Territory> neighbors = map.getNeighbors(aa, 0);
    assertTrue(neighbors.size() == 0);
    neighbors = map.getNeighbors(aa, 1);
    assertTrue(neighbors.size() == 2);
    assertTrue(neighbors.contains(ab));
    assertTrue(neighbors.contains(ba));
    neighbors = map.getNeighbors(aa, 2);
    assertTrue(neighbors.size() == 5);
    assertTrue(neighbors.contains(ab));
    assertTrue(neighbors.contains(ac));
    assertTrue(neighbors.contains(ba));
    assertTrue(neighbors.contains(bb));
    assertTrue(neighbors.contains(ca));
  }
}
