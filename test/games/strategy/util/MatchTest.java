package games.strategy.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import junit.framework.TestCase;

@SuppressWarnings("unchecked")
public class MatchTest extends TestCase {
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

  @Override
  public void setUp() {
    m_ints.add(new Integer(-1));
    m_ints.add(new Integer(-2));
    m_ints.add(new Integer(-3));
    m_ints.add(new Integer(0));
    m_ints.add(new Integer(1));
    m_ints.add(new Integer(2));
    m_ints.add(new Integer(3));
  }

  /** Creates new IntegerMapTest */
  public MatchTest(final String name) {
    super(name);
  }

  public void testNever() {
    assertTrue(!Match.someMatch(m_ints, Match.NEVER_MATCH));
    assertTrue(!Match.allMatch(m_ints, Match.NEVER_MATCH));
    assertEquals(0, Match.getMatches(m_ints, Match.NEVER_MATCH).size());
  }

  public void testMatches() {
    assertTrue(m_pos.match(1));
    assertTrue(!m_pos.match(-1));
    assertTrue(m_neg.match(-1));
    assertTrue(!m_neg.match(1));
    assertTrue(m_zero.match(0));
    assertTrue(!m_zero.match(1));
  }

  public void testAlways() {
    assertTrue(Match.someMatch(m_ints, Match.ALWAYS_MATCH));
    assertTrue(Match.allMatch(m_ints, Match.ALWAYS_MATCH));
    assertEquals(7, Match.getMatches(m_ints, Match.ALWAYS_MATCH).size());
  }

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

  public void testMap() {
    final HashMap<String, String> map = new HashMap<>();
    map.put("a", "b");
    map.put("b", "c");
    map.put("c", "d");
    assertEquals(Match.getKeysWhereValueMatch(map, Match.ALWAYS_MATCH).size(), 3);
    assertEquals(Match.getKeysWhereValueMatch(map, Match.NEVER_MATCH).size(), 0);
  }
}
