package org.triplea.java.collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableMap;
import java.util.Iterator;
import java.util.Map;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.Test;

class IntegerMapTest {
  private final Object k1 = new Object();
  private final Object k2 = new Object();
  private final Object k3 = new Object();

  @Test
  void shouldBeConstructableFromJavaMap() {
    final IntegerMap<Object> expected = new IntegerMap<>();
    expected.add(k1, 1);
    expected.add(k2, 2);
    expected.add(k3, 3);

    final IntegerMap<Object> actual = new IntegerMap<>(ImmutableMap.of(k1, 1, k2, 2, k3, 3));

    assertThat(actual, is(expected));
  }

  @Test
  void shouldBeEquatableAndHashable() {
    EqualsVerifier.forClass(IntegerMap.class).suppress(Warning.NULL_FIELDS).verify();

    // We need to explicitly test this case because EqualsVerifier's internal prefab values for
    // LinkedHashMap use the
    // same value for all key/value pairs
    assertThat(
        "should not be equal when keys are equal but values are not equal",
        new IntegerMap<>(ImmutableMap.of(k1, 1)),
        is(not(new IntegerMap<>(ImmutableMap.of(k1, 2)))));
  }

  @Test
  void testAdd() {
    final IntegerMap<Object> map = new IntegerMap<>();
    map.add(k1, 5);
    assertEquals(5, map.getInt(k1));
    map.add(k1, 10);
    assertEquals(15, map.getInt(k1));
    map.add(k1, -20);
    assertEquals(-5, map.getInt(k1));
    map.add(k1, 5);
    assertEquals(0, map.getInt(k1));
  }

  @Test
  void testPositive() {
    IntegerMap<Object> map = new IntegerMap<>();
    map.add(k1, 5);
    map.add(k2, 3);
    map.add(k3, 0);
    assertTrue(map.isPositive());
    map = new IntegerMap<>();
    map.add(k1, 5);
    map.add(k2, -3);
    map.add(k3, 1);
    assertFalse(map.isPositive());
  }

  @Test
  void testAddMap() {
    final IntegerMap<Object> map1 = new IntegerMap<>();
    map1.add(k1, 5);
    map1.add(k2, 3);
    final IntegerMap<Object> map2 = new IntegerMap<>();
    map2.add(k1, 5);
    map2.add(k2, -3);
    map2.add(k3, 1);
    map1.add(map2);
    assertEquals(10, map1.getInt(k1));
    assertEquals(0, map1.getInt(k2));
    assertEquals(1, map1.getInt(k3));
  }

  @Test
  void testGreaterThanOrEqualTo() {
    final IntegerMap<Object> map1 = new IntegerMap<>();
    map1.add(k1, 5);
    map1.add(k2, 3);
    final IntegerMap<Object> map2 = new IntegerMap<>();
    map2.add(k1, 5);
    map2.add(k2, 3);
    map2.add(k3, 1);
    assertFalse(map1.greaterThanOrEqualTo(map2));
    assertTrue(map2.greaterThanOrEqualTo(map2));
    map1.add(k3, 3);
    assertTrue(map1.greaterThanOrEqualTo(map2));
  }

  @Test
  void testGetInt() {
    final IntegerMap<Object> map = new IntegerMap<>();
    map.add(k1, 0);
    map.add(k2, 5);
    assertEquals(0, map.getInt(k1));
    assertEquals(5, map.getInt(k2));
    assertEquals(0, map.getInt(k3));
    map.add(k3, -1);
    assertEquals(-1, map.getInt(k3));
  }

  @Test
  void testMultiplyAllValuesBy() {
    final IntegerMap<Object> map = new IntegerMap<>();
    map.add(k1, 5);
    map.add(k2, -3);
    map.add(k3, 1);
    map.multiplyAllValuesBy(314);
    assertEquals(1570, map.getInt(k1));
    assertEquals(-942, map.getInt(k2));
    assertEquals(314, map.getInt(k3));
  }

  @Test
  void testAllValuesEquals() {
    final IntegerMap<Object> map = new IntegerMap<>();
    assertTrue(map.allValuesEqual(0));
    assertTrue(map.allValuesEqual(-100));
    assertTrue(map.allValuesEqual(100));
    map.put(k1, 0);
    assertTrue(map.allValuesEqual(0));
    assertFalse(map.allValuesEqual(-100));
    assertFalse(map.allValuesEqual(100));
    map.put(k2, 1);
    assertFalse(map.allValuesEqual(0));
    assertFalse(map.allValuesEqual(1));
    map.add(k1, 1);
    assertTrue(map.allValuesEqual(1));
    map.removeKey(k2);
    assertTrue(map.allValuesEqual(1));
  }

  @Test
  void testLowestKey() {
    final IntegerMap<Object> map = new IntegerMap<>();
    assertNull(map.lowestKey());
    map.put(k1, 0);
    assertEquals(k1, map.lowestKey());
    map.put(k2, 10);
    assertEquals(k1, map.lowestKey());
    map.put(k3, -10);
    assertEquals(k3, map.lowestKey());
  }

  @Test
  void testTotalValues() {
    final IntegerMap<Object> map = new IntegerMap<>();
    assertEquals(0, map.totalValues());
    map.put(k1, 0);
    assertEquals(0, map.totalValues());
    map.put(k2, 1);
    assertEquals(1, map.totalValues());
    map.put(k3, -2);
    assertEquals(-1, map.totalValues());
  }

  @Test
  void testSubstractMap() {
    final IntegerMap<Object> map1 = new IntegerMap<>();
    map1.add(k1, 5);
    map1.add(k2, 3);
    final IntegerMap<Object> map2 = new IntegerMap<>();
    map2.add(k1, 5);
    map2.add(k2, -3);
    map2.add(k3, 1);
    map1.subtract(map2);
    assertEquals(0, map1.getInt(k1));
    assertEquals(6, map1.getInt(k2));
    assertEquals(-1, map1.getInt(k3));
  }

  @Test
  void testAddMultiple() {
    final IntegerMap<Object> map1 = new IntegerMap<>();
    map1.add(k1, 5);
    map1.add(k2, 3);
    final IntegerMap<Object> map2 = new IntegerMap<>();
    map2.add(k1, 5);
    map2.add(k2, -3);
    map2.add(k3, 1);
    map1.addMultiple(map2, 314);
    assertEquals(1575, map1.getInt(k1));
    assertEquals(-939, map1.getInt(k2));
    assertEquals(314, map1.getInt(k3));
  }

  @Test
  void testInsertionOrderIsKept() {
    final IntegerMap<Object> test = new IntegerMap<>();
    test.add(k1, 2);
    test.add(k2, 1);
    test.add(k3, 0);
    final Iterator<Map.Entry<Object, Integer>> iterator = test.entrySet().iterator();
    final Map.Entry<Object, Integer> first = iterator.next();
    assertEquals(k1, first.getKey());
    assertEquals(2, first.getValue().intValue());
    final Map.Entry<Object, Integer> second = iterator.next();
    assertEquals(k2, second.getKey());
    assertEquals(1, second.getValue().intValue());
    final Map.Entry<Object, Integer> third = iterator.next();
    assertEquals(k3, third.getKey());
    assertEquals(0, third.getValue().intValue());
  }
}
