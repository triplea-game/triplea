/*
 * IntegerMapTest.java
 *
 * Created on November 7, 2001, 1:46 PM
 */

package games.strategy.util;

import java.util.*;
import junit.framework.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class IntegerMapTest extends TestCase
{
	
	private Object v1 = new Object();
	private Object v2 = new Object();
	private Object v3 = new Object();
	
	public static Test suite()
	{
		TestSuite suite = new TestSuite();
		suite.addTestSuite(IntegerMapTest.class);
		return suite;
	}
	
	/** Creates new IntegerMapTest */
    public IntegerMapTest(String name) 
	{
		super(name);
    }

	public void testAdd()
	{
		IntegerMap map = new IntegerMap();
		map.add(v1, 5);	
		assertEquals(map.getInt(v1), 5);
		map.add(v1,10);
		assertEquals(map.getInt(v1), 15);
		map.add(v1,-20);
		assertEquals(map.getInt(v1), -5);
		map.add(v1, new Integer(5));
		assertEquals(map.getInt(v1), 0);
		
	}
	
	public void testPositive()
	{
		IntegerMap map = new IntegerMap();
		map.add(v1, 5);	
		map.add(v2, 3);	
		map.add(v3, 0);	
		assertTrue(map.isPositive());
		
		map = new IntegerMap();
		map.add(v1, 5);	
		map.add(v2, -3);	
		map.add(v3, 1);	
		assertTrue(!map.isPositive());
	}
	
	public void testAddMap()
	{
		IntegerMap map1 = new IntegerMap();
		map1.add(v1, 5);	
		map1.add(v2, 3);	
		
		IntegerMap map2 = new IntegerMap();
		map2.add(v1, 5);	
		map2.add(v2, -3);	
		map2.add(v3, 1);	
		
		map1.add(map2);
		
		assertEquals(10, map1.getInt(v1));
		assertEquals(0, map1.getInt(v2));
		assertEquals(1, map1.getInt(v3));
	}
	
	public void testGreaterThan()
	{
		IntegerMap map1 = new IntegerMap();
		map1.add(v1, 5);	
		map1.add(v2, 3);	
		
		IntegerMap map2 = new IntegerMap();
		map2.add(v1, 5);	
		map2.add(v2, 3);	
		map2.add(v3, 1);	
		
		assertTrue(!map1.greaterThanOrEqualTo(map2));
		assertTrue(map2.greaterThanOrEqualTo(map2));
		
		map1.add(v3, 3);
		assertTrue(map1.greaterThanOrEqualTo(map2));
	}
}
