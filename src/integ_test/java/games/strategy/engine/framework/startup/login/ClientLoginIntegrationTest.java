package games.strategy.engine.framework.startup.login;

import static games.strategy.test.Assertions.assertNotThrows;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import games.strategy.net.ClientMessenger;
import games.strategy.net.CouldNotLogInException;
import games.strategy.net.DefaultObjectStreamFactory;
import games.strategy.net.IClientMessenger;
import games.strategy.net.IConnectionLogin;
import games.strategy.net.ILoginValidator;
import games.strategy.net.IServerMessenger;
import games.strategy.net.MacFinder;
import games.strategy.net.ServerMessenger;

public final class ClientLoginIntegrationTest {
  private static final String PASSWORD = "password";
  private static final String OTHER_PASSWORD = "otherPassword";

  private IServerMessenger serverMessenger;
  private int serverPort;

  @BeforeEach
  public void setUp() throws Exception {
    serverMessenger = newServerMessenger();
    serverPort = serverMessenger.getLocalNode().getSocketAddress().getPort();
  }

  private static IServerMessenger newServerMessenger() throws Exception {
    final ServerMessenger serverMessenger = new ServerMessenger("server", 0);
    serverMessenger.setAcceptNewConnections(true);
    serverMessenger.setLoginValidator(newLoginValidator(serverMessenger));
    return serverMessenger;
  }

  private static ILoginValidator newLoginValidator(final IServerMessenger serverMessenger) {
    final ClientLoginValidator clientLoginValidator = new ClientLoginValidator(serverMessenger);
    clientLoginValidator.setGamePassword(PASSWORD);
    return clientLoginValidator;
  }

  @AfterEach
  public void tearDown() {
    serverMessenger.shutDown();
  }

  @Test
  public void login_ShouldSucceedUsingMd5CryptAuthenticatorWhenPasswordMatches() {
    final IConnectionLogin connectionLogin = new TestConnectionLogin() {
      @Override
      public Map<String, String> getProperties(final Map<String, String> challenge) {
        return filterMd5CryptAuthenticatorResponseProperties(super.getProperties(challenge));
      }
    };

    assertNotThrows(() -> newClientMessenger(connectionLogin).shutDown());
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
    protected String promptForPassword() {
      return password;
    }

    static Map<String, String> filterMd5CryptAuthenticatorResponseProperties(final Map<String, String> response) {
      response.remove(HmacSha512Authenticator.ResponsePropertyNames.DIGEST);
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
  public void login_ShouldFailUsingMd5CryptAuthenticatorWhenPasswordDoesNotMatch() {
    final IConnectionLogin connectionLogin = new TestConnectionLogin(OTHER_PASSWORD) {
      @Override
      public Map<String, String> getProperties(final Map<String, String> challenge) {
        return filterMd5CryptAuthenticatorResponseProperties(super.getProperties(challenge));
      }
    };

    assertThrows(CouldNotLogInException.class, () -> newClientMessenger(connectionLogin).shutDown());
  }

  @Test
  public void login_ShouldSucceedUsingHmacSha512AuthenticatorWhenPasswordMatches() {
    final IConnectionLogin connectionLogin = new TestConnectionLogin();

    assertNotThrows(() -> newClientMessenger(connectionLogin).shutDown());
  }

  @Test
  public void login_ShouldFailUsingHmacSha512AuthenticatorWhenPasswordDoesNotMatch() {
    final IConnectionLogin connectionLogin = new TestConnectionLogin(OTHER_PASSWORD);

    assertThrows(CouldNotLogInException.class, () -> newClientMessenger(connectionLogin).shutDown());
  }
}
