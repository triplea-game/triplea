package games.strategy.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
  public void toMap_ShouldReturnEquivalentJavaMap() {
    final IntegerMap<Object> map = new IntegerMap<>();
    map.add(v1, 1);
    map.add(v2, 2);
    map.add(v3, 3);

    assertThat(map.toMap(), is(ImmutableMap.of(v1, 1, v2, 2, v3, 3)));
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
        new IntegerMap<>(v1, 1),
        is(not(new IntegerMap<>(v1, 2))));
  }

  @Test
  public void testAdd() {
    final IntegerMap<Object> map = new IntegerMap<>();
    map.add(v1, 5);
    assertEquals(map.getInt(v1), 5);
    map.add(v1, 10);
    assertEquals(map.getInt(v1), 15);
    map.add(v1, -20);
    assertEquals(map.getInt(v1), -5);
    map.add(v1, 5);
    assertEquals(map.getInt(v1), 0);
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
  public void testGreaterThan() {
    final IntegerMap<Object> map1 = new IntegerMap<>();
    map1.add(v1, 5);
    map1.add(v2, 3);
    final IntegerMap<Object> map2 = new IntegerMap<>();
    map2.add(v1, 5);
    map2.add(v2, 3);
    map2.add(v3, 1);
    assertTrue(!map1.greaterThanOrEqualTo(map2));
    assertTrue(map2.greaterThanOrEqualTo(map2));
    map1.add(v3, 3);
    assertTrue(map1.greaterThanOrEqualTo(map2));
  }
}
