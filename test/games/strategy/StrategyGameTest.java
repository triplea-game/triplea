package games.strategy;


import games.strategy.engine.chat.ChatIgnoreListTest;
import games.strategy.engine.chat.ChatMessagePanel;
import games.strategy.engine.chat.ChatPanelTest;
import games.strategy.engine.chat.ChatTest;
import games.strategy.engine.chat.StatusTest;
import games.strategy.engine.lobby.server.ModeratorControllerTest;
import games.strategy.engine.lobby.server.login.LobbyLoginValidatorTest;
import games.strategy.engine.lobby.server.userDB.BadWordControllerTest;
import games.strategy.engine.lobby.server.userDB.BannedIpControllerTest;
import games.strategy.engine.lobby.server.userDB.DBUserControllerTest;
import games.strategy.engine.message.ChannelMessengerTest;
import games.strategy.engine.message.EndPointTest;
import games.strategy.engine.message.RemoteInterfaceHelperTest;
import games.strategy.engine.message.RemoteMessengerTest;
import games.strategy.engine.random.CryptoRandomSourceTest;
import games.strategy.util.VersionTest;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class StrategyGameTest extends TestCase
{

	public static Test suite()
	{
		TestSuite suite = new TestSuite(StrategyGameTest.class.getSimpleName());
		suite.addTestSuite(games.strategy.util.IntegerMapTest.class);
		suite.addTestSuite(games.strategy.util.MatchTest.class);
		suite.addTestSuite(games.strategy.util.PropertyUtilTest.class);
        
		suite.addTestSuite(games.strategy.engine.xml.ParserTest.class);
		suite.addTestSuite(games.strategy.engine.data.AllianceTrackerTest.class);
		suite.addTestSuite(games.strategy.engine.data.MapTest.class);
		suite.addTestSuite(games.strategy.engine.data.ChangeTest.class);
		suite.addTestSuite(games.strategy.engine.data.SerializationTest.class);
		suite.addTestSuite(games.strategy.net.MessengerTest.class);
        suite.addTestSuite(games.strategy.net.MessengerLoginTest.class);
        
        
        
		suite.addTestSuite(games.strategy.thread.ThreadPoolTest.class);
		suite.addTestSuite(games.strategy.thread.LockUtilTest.class);
		suite.addTestSuite(games.strategy.engine.framework.GameDataManagerTest.class);
		suite.addTestSuite(ChannelMessengerTest.class);
		suite.addTestSuite(RemoteMessengerTest.class);
		suite.addTestSuite(CryptoRandomSourceTest.class);
		suite.addTestSuite(VersionTest.class);
		suite.addTestSuite(EndPointTest.class);
        suite.addTestSuite(ChatPanelTest.class);
        suite.addTestSuite(ChatIgnoreListTest.class);
        suite.addTestSuite(ChatTest.class);
        suite.addTestSuite(StatusTest.class);
        
        
        suite.addTestSuite(DBUserControllerTest.class);
        suite.addTestSuite(BannedIpControllerTest.class);
        suite.addTestSuite(BadWordControllerTest.class);
        suite.addTestSuite(LobbyLoginValidatorTest.class);
        suite.addTestSuite(ModeratorControllerTest.class);
        
        suite.addTestSuite(RemoteInterfaceHelperTest.class);
        
        

		return suite;
	}

	/** Creates new StrategyGameTest */
    public StrategyGameTest(String name)
	{
		super(name);
    }
}
