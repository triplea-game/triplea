/*
 * MapTest.java
 *
 * Created on November 22, 2001, 1:46 PM
 */

package games.strategy.util;

import java.util.*;
import junit.framework.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class MatchTest extends TestCase
{
	
	Collection m_ints = new ArrayList();
	
	Match m_pos = new Match()
	{
		public boolean match(Object o)
		{
			return ((Integer) o).intValue() > 0;
		}
	};

	Match m_neg = new Match()
	{
		public boolean match(Object o)
		{
			return ((Integer) o).intValue() < 0;
		}
	};

	Match m_zero = new Match()
	{
		public boolean match(Object o)
		{
			return ((Integer) o).intValue() == 0;
		}
	};
	
	public static Test suite()
	{
		TestSuite suite = new TestSuite();
		suite.addTestSuite(MatchTest.class);
		return suite;
	}
	
	public void setUp()
	{
		m_ints.add(new Integer(-1));
		m_ints.add(new Integer(-2));
		m_ints.add(new Integer(-3));
		m_ints.add(new Integer(0));
		m_ints.add(new Integer(1));
		m_ints.add(new Integer(2));
		m_ints.add(new Integer(3));
	}
	
	/** Creates new IntegerMapTest */
    public MatchTest(String name) 
	{
		super(name);
    }

	public void testNever()
	{
		assertTrue( !Match.someMatch(m_ints, Match.NEVER_MATCH));
		assertTrue( !Match.allMatch(m_ints, Match.NEVER_MATCH));
		assertEquals(0, Match.getMatches(m_ints, Match.NEVER_MATCH).size()); 
	}

	public void testMatches()
	{
		assertTrue( m_pos.match(new Integer(1)));
		assertTrue(! m_pos.match(new Integer(-1)));
		
		assertTrue( m_neg.match(new Integer(-1)));
		assertTrue(! m_neg.match(new Integer(1)));
		
		assertTrue( m_zero.match(new Integer(0)));
		assertTrue(! m_zero.match(new Integer(1)));
		
	}
	

	public void testAlways()
	{
		assertTrue( Match.someMatch(m_ints, Match.ALWAYS_MATCH));
		assertTrue( Match.allMatch(m_ints, Match.ALWAYS_MATCH));
		assertEquals(7, Match.getMatches(m_ints, Match.ALWAYS_MATCH).size()); 
	}

	public void testAnd()
	{
		CompositeMatch and = new CompositeMatchAnd(m_pos, m_neg);
		assertTrue(!and.match(new Integer(1)));
		
		assertTrue(!Match.someMatch(m_ints, and));
		assertTrue(! Match.someMatch(m_ints, and));
		assertEquals(0, Match.getMatches(m_ints, and).size());
		
		and.add(m_zero);
		assertTrue(! Match.someMatch(m_ints, and));
		assertTrue(! Match.allMatch(m_ints, and));
		assertEquals(0, Match.getMatches(m_ints, and).size());
		
		and = new CompositeMatchAnd(m_pos, m_pos);
		assertTrue(and.match(new Integer(1)));
		
		assertTrue(Match.someMatch(m_ints, and));
		assertTrue(! Match.allMatch(m_ints, and));
		assertEquals(3, Match.getMatches(m_ints, and).size());


	}
	
	public void testOr()
	{
		CompositeMatch or = new CompositeMatchOr(m_pos, m_neg);
		assertTrue(or.match(new Integer(1)));
		
		assertTrue(Match.someMatch(m_ints, or));
		assertTrue(! Match.allMatch(m_ints, or));
		assertEquals(6, Match.getMatches(m_ints, or).size());
		
		or.add(m_zero);
		assertTrue(Match.someMatch(m_ints, or));
		assertTrue(Match.allMatch(m_ints, or));
		assertEquals(7, Match.getMatches(m_ints, or).size());

	}
}
