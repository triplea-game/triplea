package games.strategy.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

public class IntegerMapTest {
  private final Object v1 = new Object();
  private final Object v2 = new Object();
  private final Object v3 = new Object();

  @Test
  public void shouldBeConstructableFromJavaMap() {
    final IntegerMap<Object> expected = new IntegerMap<>();
    expected.add(v1, 1);
    expected.add(v2, 2);
    expected.add(v3, 3);

    final IntegerMap<Object> actual = new IntegerMap<>(ImmutableMap.of(v1, 1, v2, 2, v3, 3));

    assertThat(actual, is(expected));
  }

  @Test
  public void shouldBeEquatableAndHashable() {
    EqualsVerifier.forClass(IntegerMap.class)
        .suppress(Warning.NULL_FIELDS)
        .verify();

    // We need to explicitly test this case because EqualsVerifier's internal prefab values for HashMap use the
    // same value for all key/value pairs
    assertThat(
        "should not be equal when keys are equal but values are not equal",
        new IntegerMap<>(ImmutableMap.of(v1, 1)),
        is(not(new IntegerMap<>(ImmutableMap.of(v1, 2)))));
  }

  @Test
  public void testAdd() {
    final IntegerMap<Object> map = new IntegerMap<>();
    map.add(v1, 5);
    assertEquals(5, map.getInt(v1));
    map.add(v1, 10);
    assertEquals(15, map.getInt(v1));
    map.add(v1, -20);
    assertEquals(-5, map.getInt(v1));
    map.add(v1, 5);
    assertEquals(0, map.getInt(v1));
  }

  @Test
  public void testPositive() {
    IntegerMap<Object> map = new IntegerMap<>();
    map.add(v1, 5);
    map.add(v2, 3);
    map.add(v3, 0);
    assertTrue(map.isPositive());
    map = new IntegerMap<>();
    map.add(v1, 5);
    map.add(v2, -3);
    map.add(v3, 1);
    assertTrue(!map.isPositive());
  }

  @Test
  public void testAddMap() {
    final IntegerMap<Object> map1 = new IntegerMap<>();
    map1.add(v1, 5);
    map1.add(v2, 3);
    final IntegerMap<Object> map2 = new IntegerMap<>();
    map2.add(v1, 5);
    map2.add(v2, -3);
    map2.add(v3, 1);
    map1.add(map2);
    assertEquals(10, map1.getInt(v1));
    assertEquals(0, map1.getInt(v2));
    assertEquals(1, map1.getInt(v3));
  }

  @Test
  public void testGreaterThanOrEqualTo() {
    final IntegerMap<Object> map1 = new IntegerMap<>();
    map1.add(v1, 5);
    map1.add(v2, 3);
    final IntegerMap<Object> map2 = new IntegerMap<>();
    map2.add(v1, 5);
    map2.add(v2, 3);
    map2.add(v3, 1);
    assertFalse(map1.greaterThanOrEqualTo(map2));
    assertTrue(map2.greaterThanOrEqualTo(map2));
    map1.add(v3, 3);
    assertTrue(map1.greaterThanOrEqualTo(map2));
  }

  @Test
  public void testGetInt() {
    final IntegerMap<Object> map = new IntegerMap<>();
    map.add(v1, 0);
    map.add(v2, 5);
    assertEquals(0, map.getInt(v1));
    assertEquals(5, map.getInt(v2));
    assertEquals(0, map.getInt(v3));
    map.add(v3, -1);
    assertEquals(-1, map.getInt(v3));
  }

  @Test
  public void testMultiplyAllValuesBy() {
    final IntegerMap<Object> map = new IntegerMap<>();
    map.add(v1, 5);
    map.add(v2, -3);
    map.add(v3, 1);
    map.multiplyAllValuesBy(314);
    assertEquals(1570, map.getInt(v1));
    assertEquals(-942, map.getInt(v2));
    assertEquals(314, map.getInt(v3));
  }

  @Test
  public void testAllValuesEquals() {
    final IntegerMap<Object> map = new IntegerMap<>();
    assertTrue(map.allValuesEqual(0));
    assertTrue(map.allValuesEqual(-100));
    assertTrue(map.allValuesEqual(100));
    map.put(v1, 0);
    assertTrue(map.allValuesEqual(0));
    assertFalse(map.allValuesEqual(-100));
    assertFalse(map.allValuesEqual(100));
    map.put(v2, 1);
    assertFalse(map.allValuesEqual(0));
    assertFalse(map.allValuesEqual(1));
    map.add(v1, 1);
    assertTrue(map.allValuesEqual(1));
    map.removeKey(v2);
    assertTrue(map.allValuesEqual(1));
  }

  @Test
  public void testLowestKey() {
    final IntegerMap<Object> map = new IntegerMap<>();
    assertNull(map.lowestKey());
    map.put(v1, 0);
    assertEquals(v1, map.lowestKey());
    map.put(v2, 10);
    assertEquals(v1, map.lowestKey());
    map.put(v3, -10);
    assertEquals(v3, map.lowestKey());
  }

  @Test
  public void testTotalValues() {
    final IntegerMap<Object> map = new IntegerMap<>();
    assertEquals(0, map.totalValues());
    map.put(v1, 0);
    assertEquals(0, map.totalValues());
    map.put(v2, 1);
    assertEquals(1, map.totalValues());
    map.put(v3, -2);
    assertEquals(-1, map.totalValues());
  }

  @Test
  public void testSubstractMap() {
    final IntegerMap<Object> map1 = new IntegerMap<>();
    map1.add(v1, 5);
    map1.add(v2, 3);
    final IntegerMap<Object> map2 = new IntegerMap<>();
    map2.add(v1, 5);
    map2.add(v2, -3);
    map2.add(v3, 1);
    map1.subtract(map2);
    assertEquals(0, map1.getInt(v1));
    assertEquals(6, map1.getInt(v2));
    assertEquals(-1, map1.getInt(v3));
  }

  @Test
  public void testAddMultiple() {
    final IntegerMap<Object> map1 = new IntegerMap<>();
    map1.add(v1, 5);
    map1.add(v2, 3);
    final IntegerMap<Object> map2 = new IntegerMap<>();
    map2.add(v1, 5);
    map2.add(v2, -3);
    map2.add(v3, 1);
    map1.addMultiple(map2, 314);
    assertEquals(1575, map1.getInt(v1));
    assertEquals(-939, map1.getInt(v2));
    assertEquals(314, map1.getInt(v3));
  }
}
