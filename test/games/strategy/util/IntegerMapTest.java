package games.strategy.util;

import junit.framework.TestCase;

public class IntegerMapTest extends TestCase { 
  private final Object v1 = new Object();
  private final Object v2 = new Object();
  private final Object v3 = new Object();

  /** Creates new IntegerMapTest */
  public IntegerMapTest(final String name) {
    super(name);
  }

  public void testAdd() {
    final IntegerMap<Object> map = new IntegerMap<Object>();
    map.add(v1, 5);
    assertEquals(map.getInt(v1), 5);
    map.add(v1, 10);
    assertEquals(map.getInt(v1), 15);
    map.add(v1, -20);
    assertEquals(map.getInt(v1), -5);
    map.add(v1, new Integer(5));
    assertEquals(map.getInt(v1), 0);
  }

  public void testPositive() {
    IntegerMap<Object> map = new IntegerMap<Object>();
    map.add(v1, 5);
    map.add(v2, 3);
    map.add(v3, 0);
    assertTrue(map.isPositive());
    map = new IntegerMap<Object>();
    map.add(v1, 5);
    map.add(v2, -3);
    map.add(v3, 1);
    assertTrue(!map.isPositive());
  }

  public void testAddMap() {
    final IntegerMap<Object> map1 = new IntegerMap<Object>();
    map1.add(v1, 5);
    map1.add(v2, 3);
    final IntegerMap<Object> map2 = new IntegerMap<Object>();
    map2.add(v1, 5);
    map2.add(v2, -3);
    map2.add(v3, 1);
    map1.add(map2);
    assertEquals(10, map1.getInt(v1));
    assertEquals(0, map1.getInt(v2));
    assertEquals(1, map1.getInt(v3));
  }

  public void testGreaterThan() {
    final IntegerMap<Object> map1 = new IntegerMap<Object>();
    map1.add(v1, 5);
    map1.add(v2, 3);
    final IntegerMap<Object> map2 = new IntegerMap<Object>();
    map2.add(v1, 5);
    map2.add(v2, 3);
    map2.add(v3, 1);
    assertTrue(!map1.greaterThanOrEqualTo(map2));
    assertTrue(map2.greaterThanOrEqualTo(map2));
    map1.add(v3, 3);
    assertTrue(map1.greaterThanOrEqualTo(map2));
  }
}
