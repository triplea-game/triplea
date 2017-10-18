package games.strategy.engine.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import games.strategy.triplea.delegate.Matches;

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

  @BeforeEach
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
    assertEquals(-1, map.getDistance(aa, nowhere));
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
    assertEquals(0, map.getDistance(aa, aa));
  }

  @Test
  public void testImpossibleConditionRoute() {
    assertNull(map.getRoute(aa, ba, Matches.never()));
  }

  @Test
  public void testOne() {
    assertEquals(1, map.getDistance(aa, ab));
  }

  @Test
  public void testTwo() {
    assertEquals(2, map.getDistance(aa, ac));
  }

  @Test
  public void testOverWater() {
    assertEquals(3, map.getDistance(ca, cd));
  }

  @Test
  public void testOverWaterCantReach() {
    assertEquals(-1, map.getLandDistance(ca, cd));
  }

  @Test
  public void testLong() {
    assertEquals(6, map.getLandDistance(ad, da));
  }

  @Test
  public void testLongRoute() {
    final Route route = map.getLandRoute(ad, da);
    assertEquals(6, route.numberOfSteps());
  }

  @Test
  public void testNeighborLandNoSeaConnect() {
    assertEquals(-1, map.getWaterDistance(aa, ab));
  }

  @Test
  public void testNeighborSeaNoLandConnect() {
    assertEquals(-1, map.getLandDistance(bc, bd));
  }

  @Test
  public void testRouteToSelf() {
    final Route rt = map.getRoute(aa, aa);
    assertEquals(0, rt.numberOfSteps());
  }

  @Test
  public void testRouteSizeOne() {
    final Route rt = map.getRoute(aa, ab);
    assertEquals(1, rt.numberOfSteps());
  }

  @Test
  public void testImpossibleRoute() {
    final Route rt = map.getRoute(aa, nowhere);
    assertNull(rt);
  }

  @Test
  public void testImpossibleLandRoute() {
    final Route rt = map.getLandRoute(aa, cd);
    assertNull(rt);
  }

  @Test
  public void testImpossibleLandDistance() {
    final int distance = map.getLandDistance(aa, cd);
    assertEquals(-1, distance, "wrong distance");
  }

  @Test
  public void testWaterRout() {
    final Route rt = map.getWaterRoute(bd, dd);
    assertEquals(bc, rt.getTerritoryAtStep(0), "bc:" + rt);
    assertEquals(cc, rt.getTerritoryAtStep(1), "cc");
    assertEquals(dc, rt.getTerritoryAtStep(2), "dc");
    assertEquals(dd, rt.getTerritoryAtStep(3), "dd");
  }

  @Test
  public void testMultiplePossible() {
    final Route rt = map.getRoute(aa, dd);
    assertEquals(aa, rt.getStart());
    assertEquals(dd, rt.getEnd());
    assertEquals(6, rt.numberOfSteps());
  }

  @Test
  public void testNeighbors() {
    final Set<Territory> neighbors = map.getNeighbors(aa);
    assertEquals(2, neighbors.size());
    assertTrue(neighbors.contains(ab));
    assertTrue(neighbors.contains(ba));
  }

  @Test
  public void testNeighborsWithDistance() {
    Set<Territory> neighbors = map.getNeighbors(aa, 0);
    assertEquals(0, neighbors.size());
    neighbors = map.getNeighbors(aa, 1);
    assertEquals(2, neighbors.size());
    assertTrue(neighbors.contains(ab));
    assertTrue(neighbors.contains(ba));
    neighbors = map.getNeighbors(aa, 2);
    assertEquals(5, neighbors.size());
    assertTrue(neighbors.contains(ab));
    assertTrue(neighbors.contains(ac));
    assertTrue(neighbors.contains(ba));
    assertTrue(neighbors.contains(bb));
    assertTrue(neighbors.contains(ca));
  }
}
