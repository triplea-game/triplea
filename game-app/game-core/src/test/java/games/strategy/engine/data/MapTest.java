package games.strategy.engine.data;

import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MapTest {
  private final GameData gameData = givenGameData().build();

  private Territory aa;
  private Territory ab;
  private Territory ac;
  private Territory ad;
  private Territory ba;
  private Territory bb;
  private Territory bc;
  private Territory bd;
  private Territory ca;
  private Territory cb;
  private Territory cc;
  private Territory cd;
  private Territory da;
  private Territory db;
  private Territory dc;
  private Territory dd;
  private Territory nowhere;
  private GameMap map;

  @BeforeEach
  void setUp() {
    // map, l is land, w is water
    // each territory is connected to it's direct neighbors, but not diagonals
    // llll
    // llww
    // llwl
    // llww
    aa = new Territory("aa", false, gameData);
    ab = new Territory("ab", false, gameData);
    ac = new Territory("ac", false, gameData);
    ad = new Territory("ad", false, gameData);
    ba = new Territory("ba", false, gameData);
    bb = new Territory("bb", false, gameData);
    bc = new Territory("bc", true, gameData);
    bd = new Territory("bd", true, gameData);
    ca = new Territory("ca", false, gameData);
    cb = new Territory("cb", false, gameData);
    cc = new Territory("cc", true, gameData);
    cd = new Territory("cd", false, gameData);
    da = new Territory("da", false, gameData);
    db = new Territory("db", false, gameData);
    dc = new Territory("dc", true, gameData);
    dd = new Territory("dd", true, gameData);
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
    nowhere = new Territory("nowhere", false, gameData);
    when(gameData.getMap()).thenReturn(map);
  }

  @Test
  void testNowhere() {
    assertEquals(-1, map.getDistance(aa, nowhere));
  }

  @Test
  void testCantFindByName() {
    assertNull(map.getTerritory("nowhere"));
  }

  @Test
  void testCanFindByName() {
    assertNotNull(map.getTerritory("aa"));
  }

  @Test
  void testSame() {
    assertEquals(0, map.getDistance(aa, aa));
  }

  @Test
  void testImpossibleConditionRoute() {
    assertNull(map.getRoute(aa, ca, it -> false));
  }

  @Test
  void testOne() {
    assertEquals(1, map.getDistance(aa, ab));
  }

  @Test
  void testTwo() {
    assertEquals(2, map.getDistance(aa, ac));
  }

  @Test
  void testOverWater() {
    assertEquals(3, map.getDistance(ca, cd));
  }

  @Test
  void testOverWaterCantReach() {
    assertEquals(-1, map.getLandDistance(ca, cd));
  }

  @Test
  void testLong() {
    assertEquals(6, map.getLandDistance(ad, da));
  }

  @Test
  void testNeighborLandNoSeaConnect() {
    assertEquals(-1, map.getWaterDistance(aa, ab));
  }

  @Test
  void testNeighborSeaNoLandConnect() {
    assertEquals(-1, map.getLandDistance(bc, bd));
  }

  @Test
  void testRouteSizeOne() {
    final Route rt = map.getRoute(aa, ab, it -> true);
    assertEquals(1, rt.numberOfSteps());
  }

  @Test
  void testImpossibleRoute() {
    final Route rt = map.getRoute(aa, nowhere, it -> true);
    assertNull(rt);
  }

  @Test
  void testImpossibleLandDistance() {
    final int distance = map.getLandDistance(aa, cd);
    assertEquals(-1, distance, "wrong distance");
  }

  @Test
  void testMultiplePossible() {
    final Route rt = map.getRoute(aa, dd, it -> true);
    assertEquals(aa, rt.getStart());
    assertEquals(dd, rt.getEnd());
    assertEquals(6, rt.numberOfSteps());
  }

  @Test
  void testNeighbors() {
    final Set<Territory> neighbors = map.getNeighbors(aa);
    assertEquals(2, neighbors.size());
    assertTrue(neighbors.contains(ab));
    assertTrue(neighbors.contains(ba));
  }

  @Test
  void testNeighborsWithDistance() {
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
