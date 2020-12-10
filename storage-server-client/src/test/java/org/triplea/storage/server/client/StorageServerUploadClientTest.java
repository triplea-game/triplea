package org.triplea.storage.server.client;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.binaryEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Fault;
import com.google.common.base.Charsets;
import java.net.URI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.lanwen.wiremock.ext.WiremockResolver;
import ru.lanwen.wiremock.ext.WiremockUriResolver;

@ExtendWith({WiremockResolver.class, WiremockUriResolver.class})
class StorageServerUploadClientTest {

  @Test
  void uploadFile(@WiremockResolver.Wiremock final WireMockServer server) {
    doServerStubbingReturningStatusCode(server, 200);

    final var uploadResponse = sendUpload(server);

    assertThat(uploadResponse.getStatusCode(), is(200));
  }

  @Test
  void uploadFileReturning403(@WiremockResolver.Wiremock final WireMockServer server) {
    doServerStubbingReturningStatusCode(server, 403);

    final var uploadResponse = sendUpload(server);

    assertThat(uploadResponse.getStatusCode(), is(403));
  }

  private static void doServerStubbingReturningStatusCode(
      final WireMockServer server, final int statusCode) {
    server.stubFor(
        WireMock.put(
                StorageServerUploadClient.UPLOAD_PATH
                    + "?"
                    + StorageServerUploadClient.FOLDER_PARAM
                    + "=folder-name&"
                    + StorageServerUploadClient.FILE_NAME_PARAM
                    + "=file-name")
            .withHeader("Authorization", equalTo("Bearer secret-key"))
            .withRequestBody(binaryEqualTo("data-payload".getBytes(Charsets.UTF_8)))
            .willReturn(WireMock.aResponse().withStatus(statusCode)));
  }

  private UploadResponse sendUpload(final WireMockServer server) {
    return StorageServerUploadClient.builder()
        .storageServerUri(URI.create(server.baseUrl()))
        .secretApiKey("secret-key")
        .timeout(Duration.of(2000, ChronoUnit.MILLIS))
        .build()
        .upload("folder-name", "file-name", "data-payload".getBytes(Charsets.UTF_8));
  }

  @Test
  void serverException(@WiremockResolver.Wiremock final WireMockServer server) {
    server.stubFor(
        WireMock.put(anyUrl())
            .willReturn(WireMock.aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE)));

    final var uploadResponse = sendUpload(server);

    assertThat(uploadResponse.getStatusCode(), is(nullValue()));
    assertThat(uploadResponse.getException(), is(notNullValue()));
  }
}
