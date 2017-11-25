package games.strategy.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

public class MatchTest {
  private static final Predicate<Integer> IS_ZERO_MATCH = Match.of(it -> it == 0);

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
}
