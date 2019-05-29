package org.triplea.server.http;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

import java.net.URI;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.error.report.ErrorUploadClient;
import org.triplea.http.client.error.report.ErrorUploadRequest;
import org.triplea.test.common.Integration;

import io.dropwizard.testing.DropwizardTestSupport;

/**
 * Integration test launches a server and verifies we can
 * access each endpoint. As this is an integrated test, we do
 * not verify anything other than we receive a valid-looking
 * response. Logic validation is left for unit tests.
 */
@Integration
class HttpServerIntegrationTest {

  private static final DropwizardTestSupport<AppConfig> SUPPORT =
      new DropwizardTestSupport<>(ServerApplication.class, "configuration-prerelease.yml");

  private static ErrorUploadClient client;


  @BeforeAll
  static void beforeClass() {
    SUPPORT.before();
    client = ErrorUploadClient.newClient(URI.create("http://localhost:8080"));
  }

  @AfterAll
  static void afterClass() {
    SUPPORT.after();
  }

  @Test
  void canSubmitErrorReport() {
    client.canSubmitErrorReport();
  }

  @Test
  void uploadErrorReport() {
    assertThat(
        client.uploadErrorReport(ErrorUploadRequest.builder()
            .body("body")
            .title("title")
            .build()),
        notNullValue());
  }
}
