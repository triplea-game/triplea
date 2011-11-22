package games.strategy.net;

import games.strategy.engine.EngineVersion;
import games.strategy.engine.framework.startup.login.ClientLogin;
import games.strategy.engine.framework.startup.login.ClientLoginValidator;
import games.strategy.test.TestUtil;
import games.strategy.util.MD5Crypt;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class MessengerLoginTest extends TestCase
{
	private int SERVER_PORT = -1;
	
	@Override
	public void setUp()
	{
		SERVER_PORT = TestUtil.getUniquePort();
	}
	
	public void testSimple() throws Exception
	{
		final ILoginValidator validator = new ILoginValidator()
		{
			public String verifyConnection(final Map<String, String> propertiesSentToClient, final Map<String, String> propertiesReadFromClient, final String clientName, final String mac,
						final SocketAddress remoteAddress)
			{
				return null;
			}
			
			public Map<String, String> getChallengeProperties(final String userName, final SocketAddress remoteAddress)
			{
				return new HashMap<String, String>();
			}
		};
		final IConnectionLogin login = new IConnectionLogin()
		{
			public void notifyFailedLogin(final String message)
			{
				fail();
			}
			
			public Map<String, String> getProperties(final Map<String, String> challengProperties)
			{
				return new HashMap<String, String>();
			}
		};
		final ServerMessenger server = new ServerMessenger("test", SERVER_PORT);
		try
		{
			server.setLoginValidator(validator);
			server.setAcceptNewConnections(true);
			final String mac = MacFinder.GetHashedMacAddress();
			final ClientMessenger client = new ClientMessenger("localhost", SERVER_PORT, "fee", mac, new DefaultObjectStreamFactory(), login);
			client.shutDown();
		} finally
		{
			server.shutDown();
		}
	}
	
	public void testRefused() throws Exception
	{
		final ILoginValidator validator = new ILoginValidator()
		{
			public String verifyConnection(final Map<String, String> propertiesSentToClient, final Map<String, String> propertiesReadFromClient, final String clientName, final String mac,
						final SocketAddress remoteAddress)
			{
				return "error";
			}
			
			public Map<String, String> getChallengeProperties(final String userName, final SocketAddress remoteAddress)
			{
				return new HashMap<String, String>();
			}
		};
		final IConnectionLogin login = new IConnectionLogin()
		{
			public void notifyFailedLogin(final String message)
			{
			}
			
			public Map<String, String> getProperties(final Map<String, String> challengProperties)
			{
				return new HashMap<String, String>();
			}
		};
		final ServerMessenger server = new ServerMessenger("test", SERVER_PORT);
		try
		{
			server.setLoginValidator(validator);
			server.setAcceptNewConnections(true);
			try
			{
				final String mac = MacFinder.GetHashedMacAddress();
				new ClientMessenger("localhost", SERVER_PORT, "fee", mac, new DefaultObjectStreamFactory(), login);
				fail("we should not have logged in");
			} catch (final CouldNotLogInException expected)
			{
				// we expect this exception
			}
		} finally
		{
			server.shutDown();
		}
	}
	
	public void testGetMagic()
	{
		final String salt = "falafel";
		final String password = "king";
		final String encrypted = MD5Crypt.crypt(password, salt, MD5Crypt.MAGIC);
		assertEquals(salt, MD5Crypt.getSalt(MD5Crypt.MAGIC, encrypted));
	}
	
	public void testPassword() throws Exception
	{
		final ClientLoginValidator validator = new ClientLoginValidator();
		validator.setGamePassword("foo");
		final IConnectionLogin login = new IConnectionLogin()
		{
			public void notifyFailedLogin(final String message)
			{
				fail();
			}
			
			public Map<String, String> getProperties(final Map<String, String> challengProperties)
			{
				final String salt = challengProperties.get(ClientLoginValidator.SALT_PROPERTY);
				final HashMap<String, String> rVal = new HashMap<String, String>();
				rVal.put(ClientLogin.PASSWORD_PROPERTY, MD5Crypt.crypt("foo", salt));
				rVal.put(ClientLogin.ENGINE_VERSION_PROPERTY, EngineVersion.VERSION.toString());
				return rVal;
			}
		};
		final ServerMessenger server = new ServerMessenger("test", SERVER_PORT);
		try
		{
			server.setLoginValidator(validator);
			server.setAcceptNewConnections(true);
			final String mac = MacFinder.GetHashedMacAddress();
			final ClientMessenger client = new ClientMessenger("localhost", SERVER_PORT, "fee", mac, new DefaultObjectStreamFactory(), login);
			client.shutDown();
		} finally
		{
			server.shutDown();
		}
	}
}
