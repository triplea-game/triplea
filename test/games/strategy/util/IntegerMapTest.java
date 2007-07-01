/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * IntegerMapTest.java
 *
 * Created on November 7, 2001, 1:46 PM
 */

package games.strategy.util;

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
		IntegerMap<Object> map = new IntegerMap<Object>();
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
		IntegerMap<Object> map = new IntegerMap<Object>();
		map.add(v1, 5);	
		map.add(v2, 3);	
		map.add(v3, 0);	
		assertTrue(map.isPositive());
		
		map = new IntegerMap<Object>();
		map.add(v1, 5);	
		map.add(v2, -3);	
		map.add(v3, 1);	
		assertTrue(!map.isPositive());
	}
	
	public void testAddMap()
	{
		IntegerMap<Object> map1 = new IntegerMap<Object>();
		map1.add(v1, 5);	
		map1.add(v2, 3);	
		
		IntegerMap<Object> map2 = new IntegerMap<Object>();
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
		IntegerMap<Object> map1 = new IntegerMap<Object>();
		map1.add(v1, 5);	
		map1.add(v2, 3);	
		
		IntegerMap<Object> map2 = new IntegerMap<Object>();
		map2.add(v1, 5);	
		map2.add(v2, 3);	
		map2.add(v3, 1);	
		
		assertTrue(!map1.greaterThanOrEqualTo(map2));
		assertTrue(map2.greaterThanOrEqualTo(map2));
		
		map1.add(v3, 3);
		assertTrue(map1.greaterThanOrEqualTo(map2));
	}
}
