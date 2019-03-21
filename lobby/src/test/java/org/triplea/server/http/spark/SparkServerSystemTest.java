package org.triplea.server.http.spark;

import static org.mockito.Mockito.reset;

import java.net.URI;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.Mockito;
import org.triplea.server.ServerConfiguration;
import org.triplea.server.reporting.error.CreateIssueStrategy;
import org.triplea.test.common.Integration;

import spark.Spark;


@Integration
class SparkServerSystemTest {

  static final CreateIssueStrategy errorUploadStrategy = Mockito.mock(CreateIssueStrategy.class);

  private static final int SPARK_PORT = 5000;

  static final URI LOCAL_HOST = URI.create("http://localhost:" + SPARK_PORT);

  @BeforeAll
  static void startServer() {
    Spark.port(SPARK_PORT);
    SparkServer.start(ServerConfiguration.builder()
        .errorUploader(errorUploadStrategy)
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
  }
}
