package games.strategy.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import org.junit.Test;

public class MatchTest {
  private static final Object VALUE = new Object();

  private final Collection<Integer> ints = Arrays.asList(-1, -2, -3, 0, 1, 2, 3);
  private final Match<Integer> pos = Match.of(it -> it > 0);
  private final Match<Integer> neg = Match.of(it -> it < 0);
  private final Match<Integer> zero = Match.of(it -> it == 0);

  @Test
  public void testNever() {
    assertFalse(Match.someMatch(ints, Match.never()));
    assertFalse(Match.allMatch(ints, Match.never()));
    assertEquals(0, Match.getMatches(ints, Match.never()).size());
  }

  @Test
  public void testMatches() {
    assertTrue(pos.match(1));
    assertTrue(!pos.match(-1));
    assertTrue(neg.match(-1));
    assertTrue(!neg.match(1));
    assertTrue(zero.match(0));
    assertTrue(!zero.match(1));
  }

  @Test
  public void testInverse() {
    assertFalse(Match.always().invert().match(new Object()));
    assertTrue(Match.never().invert().match(new Object()));
  }

  @Test
  public void testAlways() {
    assertTrue(Match.someMatch(ints, Match.always()));
    assertTrue(Match.allMatch(ints, Match.always()));
    assertEquals(7, Match.getMatches(ints, Match.always()).size());
  }

  @Test
  public void testAnd() {
    CompositeMatch<Integer> and = new CompositeMatchAnd<>(pos, neg);
    assertTrue(!and.match(1));
    assertTrue(!Match.someMatch(ints, and));
    assertTrue(!Match.someMatch(ints, and));
    assertEquals(0, Match.getMatches(ints, and).size());
    and.add(zero);
    assertTrue(!Match.someMatch(ints, and));
    assertTrue(!Match.allMatch(ints, and));
    assertEquals(0, Match.getMatches(ints, and).size());
    and = new CompositeMatchAnd<>(pos, pos);
    assertTrue(and.match(1));
    assertTrue(Match.someMatch(ints, and));
    assertTrue(!Match.allMatch(ints, and));
    assertEquals(3, Match.getMatches(ints, and).size());
  }

  @Test
  public void testAny() {
    assertFalse(Match.any().match(VALUE));
    assertTrue(Match.any(Match.always()).match(VALUE));
    assertFalse(Match.any(Match.never()).match(VALUE));
    assertTrue(Match.any(Match.always(), Match.always()).match(VALUE));
    assertTrue(Match.any(Match.always(), Match.never()).match(VALUE));
    assertTrue(Match.any(Match.never(), Match.always()).match(VALUE));
    assertFalse(Match.any(Match.never(), Match.never()).match(VALUE));
  }

  @Test
  public void testOr() {
    assertFalse(new CompositeMatchOr<Integer>().match(1));
    final CompositeMatch<Integer> or = new CompositeMatchOr<>(pos, neg);
    assertTrue(or.match(1));
    assertTrue(Match.someMatch(ints, or));
    assertTrue(!Match.allMatch(ints, or));
    assertEquals(6, Match.getMatches(ints, or).size());
    or.add(zero);
    assertTrue(Match.someMatch(ints, or));
    assertTrue(Match.allMatch(ints, or));
    assertEquals(7, Match.getMatches(ints, or).size());
  }

  @Test
  public void testAddInverse() {
    final CompositeMatch<Object> or = new CompositeMatchOr<>();
    or.addInverse(Match.always());
    assertFalse(or.match(new Object()));
  }

  @Test
  public void testMap() {
    final HashMap<String, String> map = new HashMap<>();
    map.put("a", "b");
    map.put("b", "c");
    map.put("c", "d");
    assertEquals(3, Match.getKeysWhereValueMatch(map, Match.always()).size());
    assertEquals(0, Match.getKeysWhereValueMatch(map, Match.never()).size());
  }
}
