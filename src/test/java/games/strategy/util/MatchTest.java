package games.strategy.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;

public class MatchTest {
  Collection<Integer> ints = new ArrayList<>();
  Match<Integer> pos = new Match<Integer>() {
    @Override
    public boolean match(final Integer o) {
      return o > 0;
    }
  };
  Match<Integer> neg = new Match<Integer>() {
    @Override
    public boolean match(final Integer o) {
      return o < 0;
    }
  };
  Match<Integer> zero = new Match<Integer>() {
    @Override
    public boolean match(final Integer o) {
      return o == 0;
    }
  };

  @Before
  public void setUp() {
    ints.add(-1);
    ints.add(-2);
    ints.add(-3);
    ints.add(0);
    ints.add(1);
    ints.add(2);
    ints.add(3);
  }

  @Test
  public void testNever() {
    assertTrue(!Match.someMatch(ints, Match.getNeverMatch()));
    assertTrue(!Match.allMatch(ints, Match.getNeverMatch()));
    assertEquals(0, Match.getMatches(ints, Match.getNeverMatch()).size());
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
  public void testAlways() {
    assertTrue(Match.someMatch(ints, Match.getAlwaysMatch()));
    assertTrue(Match.allMatch(ints, Match.getAlwaysMatch()));
    assertEquals(7, Match.getMatches(ints, Match.getAlwaysMatch()).size());
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
  public void testOr() {
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
  public void testMap() {
    final HashMap<String, String> map = new HashMap<>();
    map.put("a", "b");
    map.put("b", "c");
    map.put("c", "d");
    assertEquals(Match.getKeysWhereValueMatch(map, Match.getAlwaysMatch()).size(), 3);
    assertEquals(Match.getKeysWhereValueMatch(map, Match.getNeverMatch()).size(), 0);
  }
}
