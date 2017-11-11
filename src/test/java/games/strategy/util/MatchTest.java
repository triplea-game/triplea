package games.strategy.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

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
    assertTrue(Match.allMatch(Arrays.asList(), IS_ZERO_MATCH), "empty collection");
    assertFalse(Match.allMatch(Arrays.asList(-1, 1), IS_ZERO_MATCH), "none match");
    assertFalse(Match.allMatch(Arrays.asList(-1, 0, 1), IS_ZERO_MATCH), "some match");
    assertTrue(Match.allMatch(Arrays.asList(0), IS_ZERO_MATCH), "all match (one element)");
    assertTrue(Match.allMatch(Arrays.asList(0, 0, 0), IS_ZERO_MATCH), "all match (multiple elements)");
  }

  @Test
  public void testAnyMatch() {
    assertFalse(Match.anyMatch(Arrays.asList(), IS_ZERO_MATCH), "empty collection");
    assertFalse(Match.anyMatch(Arrays.asList(-1, 1), IS_ZERO_MATCH), "none match");
    assertTrue(Match.anyMatch(Arrays.asList(0), IS_ZERO_MATCH), "some match (one element)");
    assertTrue(Match.anyMatch(Arrays.asList(-1, 0, 1), IS_ZERO_MATCH), "some match (multiple elements)");
    assertTrue(Match.anyMatch(Arrays.asList(0), IS_ZERO_MATCH), "all match (one element)");
    assertTrue(Match.anyMatch(Arrays.asList(0, 0, 0), IS_ZERO_MATCH), "all match (multiple elements)");
  }

  @Test
  public void testNoneMatch() {
    assertTrue(Stream.<Integer>empty().noneMatch(IS_ZERO_MATCH), "empty collection");
    assertTrue(Arrays.asList(-1).stream().noneMatch(IS_ZERO_MATCH), "none match (one element)");
    assertTrue(Arrays.asList(-1, 1).stream().noneMatch(IS_ZERO_MATCH), "none match (multiple elements)");
    assertFalse(Arrays.asList(-1, 0, 1).stream().noneMatch(IS_ZERO_MATCH), "some match");
    assertFalse(Arrays.asList(0, 0, 0).stream().noneMatch(IS_ZERO_MATCH), "all match");
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
