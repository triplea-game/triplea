package games.strategy.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import games.strategy.triplea.delegate.Matches;

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
    assertFalse(Matches.always().invert().match(VALUE));
    assertTrue(Matches.never().invert().match(VALUE));
  }

  @Test
  public void testAllOf() {
    assertTrue(Match.allOf().match(VALUE));

    assertTrue(Match.allOf(Matches.always()).match(VALUE));
    assertFalse(Match.allOf(Matches.never()).match(VALUE));

    assertTrue(Match.allOf(Matches.always(), Matches.always()).match(VALUE));
    assertFalse(Match.allOf(Matches.always(), Matches.never()).match(VALUE));
    assertFalse(Match.allOf(Matches.never(), Matches.always()).match(VALUE));
    assertFalse(Match.allOf(Matches.never(), Matches.never()).match(VALUE));
  }

  @Test
  public void testAnyOf() {
    assertFalse(Match.anyOf().match(VALUE));

    assertTrue(Match.anyOf(Matches.always()).match(VALUE));
    assertFalse(Match.anyOf(Matches.never()).match(VALUE));

    assertTrue(Match.anyOf(Matches.always(), Matches.always()).match(VALUE));
    assertTrue(Match.anyOf(Matches.always(), Matches.never()).match(VALUE));
    assertTrue(Match.anyOf(Matches.never(), Matches.always()).match(VALUE));
    assertFalse(Match.anyOf(Matches.never(), Matches.never()).match(VALUE));
  }

  @Test
  public void testAllMatch() {
    assertTrue("empty collection", Match.allMatch(Arrays.asList(), IS_ZERO_MATCH));
    assertFalse("none match", Match.allMatch(Arrays.asList(-1, 1), IS_ZERO_MATCH));
    assertFalse("some match", Match.allMatch(Arrays.asList(-1, 0, 1), IS_ZERO_MATCH));
    assertTrue("all match (one element)", Match.allMatch(Arrays.asList(0), IS_ZERO_MATCH));
    assertTrue("all match (multiple elements)", Match.allMatch(Arrays.asList(0, 0, 0), IS_ZERO_MATCH));
  }

  @Test
  public void testAnyMatch() {
    assertFalse("empty collection", Match.anyMatch(Arrays.asList(), IS_ZERO_MATCH));
    assertFalse("none match", Match.anyMatch(Arrays.asList(-1, 1), IS_ZERO_MATCH));
    assertTrue("some match (one element)", Match.anyMatch(Arrays.asList(0), IS_ZERO_MATCH));
    assertTrue("some match (multiple elements)", Match.anyMatch(Arrays.asList(-1, 0, 1), IS_ZERO_MATCH));
    assertTrue("all match (one element)", Match.anyMatch(Arrays.asList(0), IS_ZERO_MATCH));
    assertTrue("all match (multiple elements)", Match.anyMatch(Arrays.asList(0, 0, 0), IS_ZERO_MATCH));
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
  public void testBuildAll() {
    assertTrue(Match.newCompositeBuilder().all().match(VALUE));

    assertTrue(Match.newCompositeBuilder().add(Matches.always()).all().match(VALUE));
    assertFalse(Match.newCompositeBuilder().add(Matches.never()).all().match(VALUE));

    assertTrue(Match.newCompositeBuilder().add(Matches.always()).add(Matches.always()).all().match(VALUE));
    assertFalse(Match.newCompositeBuilder().add(Matches.always()).add(Matches.never()).all().match(VALUE));
    assertFalse(Match.newCompositeBuilder().add(Matches.never()).add(Matches.always()).all().match(VALUE));
    assertFalse(Match.newCompositeBuilder().add(Matches.never()).add(Matches.never()).all().match(VALUE));
  }

  @Test
  public void testBuildAny() {
    assertFalse(Match.newCompositeBuilder().any().match(VALUE));

    assertTrue(Match.newCompositeBuilder().add(Matches.always()).any().match(VALUE));
    assertFalse(Match.newCompositeBuilder().add(Matches.never()).any().match(VALUE));

    assertTrue(Match.newCompositeBuilder().add(Matches.always()).add(Matches.always()).any().match(VALUE));
    assertTrue(Match.newCompositeBuilder().add(Matches.always()).add(Matches.never()).any().match(VALUE));
    assertTrue(Match.newCompositeBuilder().add(Matches.never()).add(Matches.always()).any().match(VALUE));
    assertFalse(Match.newCompositeBuilder().add(Matches.never()).add(Matches.never()).any().match(VALUE));
  }
}
