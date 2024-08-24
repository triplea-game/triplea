package games.strategy.engine.framework.startup.login;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import games.strategy.net.ClientMessenger;
import games.strategy.net.CouldNotLogInException;
import games.strategy.net.DefaultObjectStreamFactory;
import games.strategy.net.IClientMessenger;
import games.strategy.net.IConnectionLogin;
import games.strategy.net.ILoginValidator;
import games.strategy.net.IServerMessenger;
import games.strategy.net.TestServerMessenger;
import org.jetbrains.annotations.NonNls;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.triplea.domain.data.SystemId;
import org.triplea.util.Version;

final class ClientLoginIntegrationTest {
  @NonNls private static final String PASSWORD = "password";
  @NonNls private static final String OTHER_PASSWORD = "otherPassword";

  private IServerMessenger serverMessenger;
  private int serverPort;

  @BeforeEach
  void setUp() throws Exception {
    serverMessenger = newServerMessenger();
    serverPort = serverMessenger.getLocalNode().getSocketAddress().getPort();
  }

  private static IServerMessenger newServerMessenger() throws Exception {
    final IServerMessenger serverMessenger = new TestServerMessenger();
    serverMessenger.setAcceptNewConnections(true);
    serverMessenger.setLoginValidator(newLoginValidator(serverMessenger));
    return serverMessenger;
  }

  private static ILoginValidator newLoginValidator(final IServerMessenger serverMessenger) {
    return ClientLoginValidator.builder()
        .serverMessenger(serverMessenger)
        .password(PASSWORD)
        .build();
  }

  @AfterEach
  void tearDown() {
    serverMessenger.shutDown();
  }

  private static class TestConnectionLogin extends ClientLogin {
    private final String password;

    TestConnectionLogin() {
      this(PASSWORD);
    }

    TestConnectionLogin(final String password) {
      super(null, new Version("2.0.0"));

      this.password = password;
    }

    @Override
    protected String promptForPassword() {
      return password;
    }
  }

  private IClientMessenger newClientMessenger(final IConnectionLogin connectionLogin)
      throws Exception {
    return new ClientMessenger(
        "localhost",
        serverPort,
        "client",
        SystemId.of("system-id"),
        new DefaultObjectStreamFactory(),
        connectionLogin);
  }

  @Nested
  final class LoginTest {
    @Test
    void shouldSucceedWhenPasswordMatches() {
      final IConnectionLogin connectionLogin = new TestConnectionLogin();

      assertDoesNotThrow(() -> newClientMessenger(connectionLogin).shutDown());
    }

    @Test
    void shouldFailWhenPasswordDoesNotMatch() {
      final IConnectionLogin connectionLogin = new TestConnectionLogin(OTHER_PASSWORD);

      assertThrows(
          CouldNotLogInException.class, () -> newClientMessenger(connectionLogin).shutDown());
    }
  }
}
