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
 * StrategyGameTest.java
 *
 * Created on October 12, 2001, 2:10 PM
 */

package games.strategy;

import junit.framework.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class StrategyGameTest extends TestCase
{

	public static Test suite()
	{
		TestSuite suite = new TestSuite();
		suite.addTestSuite(games.strategy.util.IntegerMapTest.class);
		suite.addTestSuite(games.strategy.util.MatchTest.class);
		suite.addTestSuite(games.strategy.engine.xml.ParserTest.class);
		suite.addTestSuite(games.strategy.engine.data.MapTest.class);
		suite.addTestSuite(games.strategy.engine.data.ChangeTest.class);
		suite.addTestSuite(games.strategy.engine.data.SerializationTest.class);
		suite.addTestSuite(games.strategy.engine.message.ManagerTest.class);
		suite.addTestSuite(games.strategy.net.MessengerTest.class);
		suite.addTestSuite(games.strategy.thread.ThreadPoolTest.class);
		suite.addTestSuite(games.strategy.engine.framework.GameDataManagerTest.class);

		return suite;
	}

	/** Creates new StrategyGameTest */
    public StrategyGameTest(String name)
	{
		super(name);
    }
}
