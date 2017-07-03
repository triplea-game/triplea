package games.strategy.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class MatchTest {
  private static final Match<Integer> IS_ZERO_MATCH = Match.of(it -> it == 0);

  private static final Object VALUE = new Object();

  @Test
  public void testMatch() {
    assertFalse(IS_ZERO_MATCH.match(-1));
    assertTrue(IS_ZERO_MATCH.match(0));
    assertFalse(IS_ZERO_MATCH.match(1));
  }

  @Test
  public void testInverse() {
    assertFalse(Match.always().invert().match(VALUE));
    assertTrue(Match.never().invert().match(VALUE));
  }

  @Test
  public void testAlways() {
    assertTrue(Match.always().match(VALUE));
  }

  @Test
  public void testNever() {
    assertFalse(Match.never().match(VALUE));
  }

  @Test
  public void testAllOf() {
    assertTrue(Match.allOf().match(VALUE));

    assertTrue(Match.allOf(Match.always()).match(VALUE));
    assertFalse(Match.allOf(Match.never()).match(VALUE));

    assertTrue(Match.allOf(Match.always(), Match.always()).match(VALUE));
    assertFalse(Match.allOf(Match.always(), Match.never()).match(VALUE));
    assertFalse(Match.allOf(Match.never(), Match.always()).match(VALUE));
    assertFalse(Match.allOf(Match.never(), Match.never()).match(VALUE));
  }

  @Test
  public void testAnyOf() {
    assertFalse(Match.anyOf().match(VALUE));

    assertTrue(Match.anyOf(Match.always()).match(VALUE));
    assertFalse(Match.anyOf(Match.never()).match(VALUE));

    assertTrue(Match.anyOf(Match.always(), Match.always()).match(VALUE));
    assertTrue(Match.anyOf(Match.always(), Match.never()).match(VALUE));
    assertTrue(Match.anyOf(Match.never(), Match.always()).match(VALUE));
    assertFalse(Match.anyOf(Match.never(), Match.never()).match(VALUE));
  }

  @Test
  public void testGetMatches() {
    final Collection<Integer> input = Arrays.asList(-1, 0, 1);

    assertEquals("empty collection", Arrays.asList(), Match.getMatches(Arrays.asList(), Match.always()));
    assertEquals("none match", Arrays.asList(), Match.getMatches(input, Match.never()));
    assertEquals("some match", Arrays.asList(-1, 1), Match.getMatches(input, IS_ZERO_MATCH.invert()));
    assertEquals("all match", Arrays.asList(-1, 0, 1), Match.getMatches(input, Match.always()));
  }

  @Test
  public void testGetNMatches() {
    final Collection<Integer> input = Arrays.asList(-1, 0, 1);

    assertEquals("empty collection", Arrays.asList(), Match.getNMatches(Arrays.asList(), 999, Match.always()));
    assertEquals("max = 0", Arrays.asList(), Match.getNMatches(input, 0, Match.never()));
    assertEquals("none match", Arrays.asList(), Match.getNMatches(input, input.size(), Match.never()));
    assertEquals("some match; max < count",
        Arrays.asList(0),
        Match.getNMatches(Arrays.asList(-1, 0, 0, 1), 1, IS_ZERO_MATCH));
    assertEquals("some match; max = count",
        Arrays.asList(0, 0),
        Match.getNMatches(Arrays.asList(-1, 0, 0, 1), 2, IS_ZERO_MATCH));
    assertEquals("some match; max > count",
        Arrays.asList(0, 0),
        Match.getNMatches(Arrays.asList(-1, 0, 0, 1), 3, IS_ZERO_MATCH));
    assertEquals("all match; max < count",
        Arrays.asList(-1, 0),
        Match.getNMatches(input, input.size() - 1, Match.always()));
    assertEquals("all match; max = count",
        Arrays.asList(-1, 0, 1),
        Match.getNMatches(input, input.size(), Match.always()));
    assertEquals("all match; max > count",
        Arrays.asList(-1, 0, 1),
        Match.getNMatches(input, input.size() + 1, Match.always()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetNMatches_ShouldThrowExceptionWhenMaxIsNegative() {
    Match.getNMatches(Arrays.asList(-1, 0, 1), -1, Match.always());
  }

  @Test
  public void testAllMatch() {
    assertFalse("empty collection", Match.allMatch(Arrays.asList(), IS_ZERO_MATCH));
    assertFalse("none match", Match.allMatch(Arrays.asList(-1, 1), IS_ZERO_MATCH));
    assertFalse("some match", Match.allMatch(Arrays.asList(-1, 0, 1), IS_ZERO_MATCH));
    assertTrue("all match (one element)", Match.allMatch(Arrays.asList(0), IS_ZERO_MATCH));
    assertTrue("all match (multiple elements)", Match.allMatch(Arrays.asList(0, 0, 0), IS_ZERO_MATCH));
  }

  @Test
  public void testSomeMatch() {
    assertFalse("empty collection", Match.someMatch(Arrays.asList(), IS_ZERO_MATCH));
    assertFalse("none match", Match.someMatch(Arrays.asList(-1, 1), IS_ZERO_MATCH));
    assertTrue("some match (one element)", Match.someMatch(Arrays.asList(0), IS_ZERO_MATCH));
    assertTrue("some match (multiple elements)", Match.someMatch(Arrays.asList(-1, 0, 1), IS_ZERO_MATCH));
    assertTrue("all match (one element)", Match.someMatch(Arrays.asList(0), IS_ZERO_MATCH));
    assertTrue("all match (multiple elements)", Match.someMatch(Arrays.asList(0, 0, 0), IS_ZERO_MATCH));
  }

  @Test
  public void testNoneMatch() {
    assertTrue("empty collection", Match.noneMatch(Arrays.asList(), IS_ZERO_MATCH));
    assertTrue("none match (one element)", Match.noneMatch(Arrays.asList(-1), IS_ZERO_MATCH));
    assertTrue("none match (multiple elements)", Match.noneMatch(Arrays.asList(-1, 1), IS_ZERO_MATCH));
    assertFalse("some match", Match.noneMatch(Arrays.asList(-1, 0, 1), IS_ZERO_MATCH));
    assertFalse("all match", Match.noneMatch(Arrays.asList(0, 0, 0), IS_ZERO_MATCH));
  }

  @Test
  public void testCountMatches() {
    assertEquals(0, Match.countMatches(Arrays.asList(), IS_ZERO_MATCH));

    assertEquals(1, Match.countMatches(Arrays.asList(0), IS_ZERO_MATCH));
    assertEquals(1, Match.countMatches(Arrays.asList(-1, 0, 1), IS_ZERO_MATCH));

    assertEquals(2, Match.countMatches(Arrays.asList(0, 0), IS_ZERO_MATCH));
    assertEquals(2, Match.countMatches(Arrays.asList(-1, 0, 1, 0), IS_ZERO_MATCH));
  }

  @Test
  public void testGetKeysWhereValueMatch() {
    final Map<String, String> map = new HashMap<>();
    map.put("a", "b");
    map.put("b", "c");
    map.put("c", "d");

    assertEquals(3, Match.getKeysWhereValueMatch(map, Match.always()).size());
    assertEquals(0, Match.getKeysWhereValueMatch(map, Match.never()).size());
  }

  @Test
  public void testBuildAll() {
    assertTrue(Match.newCompositeBuilder().all().match(VALUE));

    assertTrue(Match.newCompositeBuilder().add(Match.always()).all().match(VALUE));
    assertFalse(Match.newCompositeBuilder().add(Match.never()).all().match(VALUE));

    assertTrue(Match.newCompositeBuilder().add(Match.always()).add(Match.always()).all().match(VALUE));
    assertFalse(Match.newCompositeBuilder().add(Match.always()).add(Match.never()).all().match(VALUE));
    assertFalse(Match.newCompositeBuilder().add(Match.never()).add(Match.always()).all().match(VALUE));
    assertFalse(Match.newCompositeBuilder().add(Match.never()).add(Match.never()).all().match(VALUE));
  }

  @Test
  public void testBuildAny() {
    assertFalse(Match.newCompositeBuilder().any().match(VALUE));

    assertTrue(Match.newCompositeBuilder().add(Match.always()).any().match(VALUE));
    assertFalse(Match.newCompositeBuilder().add(Match.never()).any().match(VALUE));

    assertTrue(Match.newCompositeBuilder().add(Match.always()).add(Match.always()).any().match(VALUE));
    assertTrue(Match.newCompositeBuilder().add(Match.always()).add(Match.never()).any().match(VALUE));
    assertTrue(Match.newCompositeBuilder().add(Match.never()).add(Match.always()).any().match(VALUE));
    assertFalse(Match.newCompositeBuilder().add(Match.never()).add(Match.never()).any().match(VALUE));
  }
}
