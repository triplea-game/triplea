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
		
		return suite;
	}

	/** Creates new StrategyGameTest */
    public StrategyGameTest(String name) 
	{
		super(name);
    }
}
