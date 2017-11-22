package games.strategy.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import games.strategy.triplea.delegate.Matches;

public class MatchTest {
  private static final Predicate<Integer> IS_ZERO_MATCH = Match.of(it -> it == 0);

  private static final Object VALUE = new Object();

  @Test
  public void testMatch() {
    assertFalse(IS_ZERO_MATCH.test(-1));
    assertTrue(IS_ZERO_MATCH.test(0));
    assertFalse(IS_ZERO_MATCH.test(1));
  }

  @Test
  public void testTest() {
    assertFalse(IS_ZERO_MATCH.test(-1));
    assertTrue(IS_ZERO_MATCH.test(0));
    assertFalse(IS_ZERO_MATCH.test(1));
  }

  @Test
  public void testInverse() {
    assertFalse(Matches.always().negate().test(VALUE));
    assertTrue(Matches.never().negate().test(VALUE));
  }

  @Test
  public void testAllOf() {
    assertTrue(Match.allOf().test(VALUE));

    assertTrue(Match.allOf(Matches.always()).test(VALUE));
    assertFalse(Match.allOf(Matches.never()).test(VALUE));

    assertTrue(Match.allOf(Matches.always(), Matches.always()).test(VALUE));
    assertFalse(Match.allOf(Matches.always(), Matches.never()).test(VALUE));
    assertFalse(Match.allOf(Matches.never(), Matches.always()).test(VALUE));
    assertFalse(Match.allOf(Matches.never(), Matches.never()).test(VALUE));
  }
}
