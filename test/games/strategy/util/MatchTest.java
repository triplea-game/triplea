package games.strategy.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;

public class MatchTest {
  Collection<Integer> m_ints = new ArrayList<>();
  Match<Integer> m_pos = new Match<Integer>() {
    @Override
    public boolean match(final Integer o) {
      return o.intValue() > 0;
    }
  };
  Match<Integer> m_neg = new Match<Integer>() {
    @Override
    public boolean match(final Integer o) {
      return o.intValue() < 0;
    }
  };
  Match<Integer> m_zero = new Match<Integer>() {
    @Override
    public boolean match(final Integer o) {
      return o.intValue() == 0;
    }
  };

  @Before
  public void setUp() {
    m_ints.add(new Integer(-1));
    m_ints.add(new Integer(-2));
    m_ints.add(new Integer(-3));
    m_ints.add(new Integer(0));
    m_ints.add(new Integer(1));
    m_ints.add(new Integer(2));
    m_ints.add(new Integer(3));
  }

  @Test
  public void testNever() {
    assertTrue(!Match.someMatch(m_ints, Match.getNeverMatch()));
    assertTrue(!Match.allMatch(m_ints, Match.getNeverMatch()));
    assertEquals(0, Match.getMatches(m_ints, Match.getNeverMatch()).size());
  }

  @Test
  public void testMatches() {
    assertTrue(m_pos.match(1));
    assertTrue(!m_pos.match(-1));
    assertTrue(m_neg.match(-1));
    assertTrue(!m_neg.match(1));
    assertTrue(m_zero.match(0));
    assertTrue(!m_zero.match(1));
  }

  @Test
  public void testAlways() {
    assertTrue(Match.someMatch(m_ints, Match.getAlwaysMatch()));
    assertTrue(Match.allMatch(m_ints, Match.getAlwaysMatch()));
    assertEquals(7, Match.getMatches(m_ints, Match.getAlwaysMatch()).size());
  }

  @Test
  public void testAnd() {
    CompositeMatch<Integer> and = new CompositeMatchAnd<>(m_pos, m_neg);
    assertTrue(!and.match(1));
    assertTrue(!Match.someMatch(m_ints, and));
    assertTrue(!Match.someMatch(m_ints, and));
    assertEquals(0, Match.getMatches(m_ints, and).size());
    and.add(m_zero);
    assertTrue(!Match.someMatch(m_ints, and));
    assertTrue(!Match.allMatch(m_ints, and));
    assertEquals(0, Match.getMatches(m_ints, and).size());
    and = new CompositeMatchAnd<>(m_pos, m_pos);
    assertTrue(and.match(1));
    assertTrue(Match.someMatch(m_ints, and));
    assertTrue(!Match.allMatch(m_ints, and));
    assertEquals(3, Match.getMatches(m_ints, and).size());
  }

  @Test
  public void testOr() {
    final CompositeMatch<Integer> or = new CompositeMatchOr<>(m_pos, m_neg);
    assertTrue(or.match(1));
    assertTrue(Match.someMatch(m_ints, or));
    assertTrue(!Match.allMatch(m_ints, or));
    assertEquals(6, Match.getMatches(m_ints, or).size());
    or.add(m_zero);
    assertTrue(Match.someMatch(m_ints, or));
    assertTrue(Match.allMatch(m_ints, or));
    assertEquals(7, Match.getMatches(m_ints, or).size());
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
