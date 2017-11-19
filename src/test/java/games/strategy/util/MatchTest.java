package games.strategy.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
  public void testTest() {
    assertFalse(IS_ZERO_MATCH.test(-1));
    assertTrue(IS_ZERO_MATCH.test(0));
    assertFalse(IS_ZERO_MATCH.test(1));
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
}
