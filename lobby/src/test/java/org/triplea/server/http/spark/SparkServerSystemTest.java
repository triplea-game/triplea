package org.triplea.server.http.spark;

import static org.mockito.Mockito.reset;

import java.net.URI;
import java.util.function.Function;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.Mockito;
import org.triplea.http.client.lobby.login.LobbyLoginResponse;
import org.triplea.http.client.lobby.login.RegisteredUserLoginRequest;
import org.triplea.server.ServerConfiguration;
import org.triplea.server.reporting.error.CreateIssueStrategy;
import org.triplea.test.common.Integration;

import spark.Spark;


@SuppressWarnings("unchecked")
@Integration
class SparkServerSystemTest {
  static final CreateIssueStrategy errorUploadStrategy = Mockito.mock(CreateIssueStrategy.class);
  static final Function<RegisteredUserLoginRequest, LobbyLoginResponse> registeredUserLogin =
      Mockito.mock(Function.class);
  static final Function<String, LobbyLoginResponse> anonymousUserLogin = Mockito.mock(Function.class);

  private static final int SPARK_PORT = 5000;

  static final URI LOCAL_HOST = URI.create("http://localhost:" + SPARK_PORT);

  @BeforeAll
  static void startServer() {
    Spark.port(SPARK_PORT);
    SparkServer.start(ServerConfiguration.builder()
        .errorUploader(errorUploadStrategy)
        .anonymousUserLogin(anonymousUserLogin)
        .registeredUserLogin(registeredUserLogin)
        .build());
    Spark.awaitInitialization();
  }

  @AfterEach
  void resetMock() {
    reset(errorUploadStrategy);
  }

  @AfterAll
  static void stopServer() {
    Spark.stop();
    Spark.awaitStop();
  }
}
