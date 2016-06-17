package games.strategy.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import games.strategy.engine.ClientContext;
import games.strategy.engine.framework.startup.login.ClientLogin;
import games.strategy.engine.framework.startup.login.ClientLoginValidator;
import games.strategy.engine.message.DummyMessenger;
import games.strategy.test.TestUtil;
import games.strategy.util.MD5Crypt;

public class MessengerLoginTest {
  private int SERVER_PORT = -1;

  @Before
  public void setUp() {
    SERVER_PORT = TestUtil.getUniquePort();
  }

  @Test
  public void testSimple() throws Exception {
    final ILoginValidator validator = new ILoginValidator() {
      @Override
      public String verifyConnection(final Map<String, String> propertiesSentToClient,
          final Map<String, String> propertiesReadFromClient, final String clientName, final String mac,
          final SocketAddress remoteAddress) {
        return null;
      }

      @Override
      public Map<String, String> getChallengeProperties(final String userName, final SocketAddress remoteAddress) {
        return new HashMap<>();
      }
    };
    final IConnectionLogin login = new IConnectionLogin() {
      @Override
      public void notifyFailedLogin(final String message) {
        fail();
      }

      @Override
      public Map<String, String> getProperties(final Map<String, String> challengProperties) {
        return new HashMap<>();
      }
    };
    final ServerMessenger server = new ServerMessenger("test", SERVER_PORT);
    try {
      server.setLoginValidator(validator);
      server.setAcceptNewConnections(true);
      final String mac = MacFinder.getHashedMacAddress();
      final ClientMessenger client =
          new ClientMessenger("localhost", SERVER_PORT, "fee", mac, new DefaultObjectStreamFactory(), login);
      client.shutDown();
    } finally {
      server.shutDown();
    }
  }

  @Test
  public void testRefused() throws Exception {
    final ILoginValidator validator = new ILoginValidator() {
      @Override
      public String verifyConnection(final Map<String, String> propertiesSentToClient,
          final Map<String, String> propertiesReadFromClient, final String clientName, final String mac,
          final SocketAddress remoteAddress) {
        return "error";
      }

      @Override
      public Map<String, String> getChallengeProperties(final String userName, final SocketAddress remoteAddress) {
        return new HashMap<>();
      }
    };
    final IConnectionLogin login = new IConnectionLogin() {
      @Override
      public void notifyFailedLogin(final String message) {}

      @Override
      public Map<String, String> getProperties(final Map<String, String> challengProperties) {
        return new HashMap<>();
      }
    };
    final ServerMessenger server = new ServerMessenger("test", SERVER_PORT);
    try {
      server.setLoginValidator(validator);
      server.setAcceptNewConnections(true);
      try {
        final String mac = MacFinder.getHashedMacAddress();
        new ClientMessenger("localhost", SERVER_PORT, "fee", mac, new DefaultObjectStreamFactory(), login);
        fail("we should not have logged in");
      } catch (final CouldNotLogInException expected) {
        // we expect this exception
      }
    } finally {
      server.shutDown();
    }
  }

  @Test
  public void testGetMagic() {
    final String salt = "falafel";
    final String password = "king";
    final String encrypted = MD5Crypt.crypt(password, salt, MD5Crypt.MAGIC);
    assertEquals(salt, MD5Crypt.getSalt(MD5Crypt.MAGIC, encrypted));
  }

  @Test
  public void testPassword() throws Exception {
    final ClientLoginValidator validator = new ClientLoginValidator(new DummyMessenger());
    validator.setGamePassword("foo");
    final IConnectionLogin login = new IConnectionLogin() {
      @Override
      public void notifyFailedLogin(final String message) {
        fail();
      }

      @Override
      public Map<String, String> getProperties(final Map<String, String> challengProperties) {
        final String salt = challengProperties.get(ClientLoginValidator.SALT_PROPERTY);
        final HashMap<String, String> rVal = new HashMap<>();
        rVal.put(ClientLogin.PASSWORD_PROPERTY, MD5Crypt.crypt("foo", salt));
        rVal.put(ClientLogin.ENGINE_VERSION_PROPERTY, ClientContext.engineVersion().toString());
        return rVal;
      }
    };
    final ServerMessenger server = new ServerMessenger("test", SERVER_PORT);
    try {
      server.setLoginValidator(validator);
      server.setAcceptNewConnections(true);
      final String mac = MacFinder.getHashedMacAddress();
      final ClientMessenger client =
          new ClientMessenger("localhost", SERVER_PORT, "fee", mac, new DefaultObjectStreamFactory(), login);
      client.shutDown();
    } finally {
      server.shutDown();
    }
  }
}
