package games.strategy.engine.framework.startup.login;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import games.strategy.net.ClientMessenger;
import games.strategy.net.CouldNotLogInException;
import games.strategy.net.DefaultObjectStreamFactory;
import games.strategy.net.IClientMessenger;
import games.strategy.net.IConnectionLogin;
import games.strategy.net.ILoginValidator;
import games.strategy.net.IServerMessenger;
import games.strategy.net.MacFinder;
import games.strategy.net.ServerMessenger;
import games.strategy.test.TestUtil;

public final class ClientLoginIntegrationTest {
  private static final String PASSWORD = "password";
  private static final String OTHER_PASSWORD = "otherPassword";

  private IServerMessenger serverMessenger;
  private final int serverPort = TestUtil.getUniquePort();

  @Before
  public void setUp() throws Exception {
    serverMessenger = newServerMessenger();
  }

  private IServerMessenger newServerMessenger() throws Exception {
    final ServerMessenger serverMessenger = new ServerMessenger("server", serverPort);
    serverMessenger.setAcceptNewConnections(true);
    serverMessenger.setLoginValidator(newLoginValidator(serverMessenger));
    return serverMessenger;
  }

  private static ILoginValidator newLoginValidator(final IServerMessenger serverMessenger) {
    final ClientLoginValidator clientLoginValidator = new ClientLoginValidator(serverMessenger);
    clientLoginValidator.setGamePassword(PASSWORD);
    return clientLoginValidator;
  }

  @After
  public void tearDown() {
    serverMessenger.shutDown();
  }

  @Test
  public void login_ShouldSucceedUsingV1AuthenticatorWhenPasswordMatches() {
    final IConnectionLogin connectionLogin = new TestConnectionLogin() {
      @Override
      public Map<String, String> getProperties(final Map<String, String> challenge) {
        return filterV1AuthenticatorResponseProperties(super.getProperties(challenge));
      }
    };

    catchException(() -> newClientMessenger(connectionLogin).shutDown());

    assertThat(caughtException(), is(nullValue()));
  }

  private static class TestConnectionLogin extends ClientLogin {
    private final String password;

    TestConnectionLogin() {
      this(PASSWORD);
    }

    TestConnectionLogin(final String password) {
      super(null);

      this.password = password;
    }

    @Override
    public void notifyFailedLogin(final String message) {}

    @Override
    protected String promptForPassword() {
      return password;
    }

    static Map<String, String> filterV1AuthenticatorResponseProperties(final Map<String, String> response) {
      response.remove(V2Authenticator.ResponsePropertyNames.DIGEST);
      return response;
    }
  }

  private IClientMessenger newClientMessenger(final IConnectionLogin connectionLogin) throws Exception {
    return new ClientMessenger(
        "localhost",
        serverPort,
        "client",
        MacFinder.getHashedMacAddress(),
        new DefaultObjectStreamFactory(),
        connectionLogin);
  }

  @Test
  public void login_ShouldFailUsingV1AuthenticatorWhenPasswordDoesNotMatch() {
    final IConnectionLogin connectionLogin = new TestConnectionLogin(OTHER_PASSWORD) {
      @Override
      public Map<String, String> getProperties(final Map<String, String> challenge) {
        return filterV1AuthenticatorResponseProperties(super.getProperties(challenge));
      }
    };

    catchException(() -> newClientMessenger(connectionLogin).shutDown());

    assertThat(caughtException(), is(instanceOf(CouldNotLogInException.class)));
  }

  @Test
  public void login_ShouldSucceedUsingV2AuthenticatorWhenPasswordMatches() {
    final IConnectionLogin connectionLogin = new TestConnectionLogin();

    catchException(() -> newClientMessenger(connectionLogin).shutDown());

    assertThat(caughtException(), is(nullValue()));
  }

  @Test
  public void login_ShouldFailUsingV2AuthenticatorWhenPasswordDoesNotMatch() {
    final IConnectionLogin connectionLogin = new TestConnectionLogin(OTHER_PASSWORD);

    catchException(() -> newClientMessenger(connectionLogin).shutDown());

    assertThat(caughtException(), is(instanceOf(CouldNotLogInException.class)));
  }
}
