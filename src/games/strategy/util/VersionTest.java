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

import java.util.*;
import junit.framework.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class VersionTest extends TestCase
{
	
	public static Test suite()
	{
		TestSuite suite = new TestSuite();
		suite.addTestSuite(VersionTest.class);
		return suite;
	}
	
	/** Creates new IntegerMapTest */
    public VersionTest(String name) 
	{
		super(name);
    }

	public void testCompare()
	{
		Version v1 = new Version(0,0,0);
		Version v2 = new Version(1,0,0);

		assertTrue(!v1.equals(v2));
		assertTrue(!v2.equals(v1));

	}
	public void testCompare2()
	{
		Version v1 = new Version(0,0,0);
		Version v2 = new Version(1,1,0);

		assertTrue(!v1.equals(v2));
		assertTrue(!v2.equals(v1));

	}
	public void testCompare3()
	{
		Version v1 = new Version(0,0,0);
		Version v2 = new Version(0,1,0);

		assertTrue(!v1.equals(v2));
		assertTrue(!v2.equals(v1));

	}
	public void testCompare4()
	{
		Version v1 = new Version(0,0,0);
		Version v2 = new Version(0,0,1);

		assertTrue(!v1.equals(v2));
		assertTrue(!v2.equals(v1));

	}

	public void testCompare5()
	{
	    //micro differences should have no difference
		Version v1 = new Version(0,0,0,0);
		Version v2 = new Version(0,0,0,1);

		assertTrue(v1.equals(v2));
		assertTrue(v2.equals(v1));

	}
	
	public void testRead1()
	{
		assertTrue("1.2.3".equals(new Version("1.2.3").toString()));
	}
	
	public void testRead2()
	{
		assertTrue("1.2".equals(new Version("1.2").toString()));
	}	
	
	public void testRead3()
	{
		assertTrue("1.2".equals(new Version("1.2.0").toString()));
	}	
	
}
