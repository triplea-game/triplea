package org.triplea.http.client.moderator.toolbox.api.key;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.triplea.http.client.HttpClientTesting.API_KEY_PASSWORD;
import static org.triplea.http.client.HttpClientTesting.serve200ForToolboxPostWithBody;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.http.client.HttpClientTesting;
import org.triplea.http.client.moderator.toolbox.NewApiKey;
import org.triplea.http.client.moderator.toolbox.ToolboxHttpHeaders;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import ru.lanwen.wiremock.ext.WiremockResolver;
import ru.lanwen.wiremock.ext.WiremockUriResolver;

@ExtendWith({
    WiremockResolver.class,
    WiremockUriResolver.class
})
class ToolboxApiKeyClientTest {
  private static final ApiKeyData API_KEY_DATA = ApiKeyData.builder()
      .publicId("All cannibals view coal-black, rough ships.")
      .lastUsed(Instant.now())
      .lastUsedIp("Jolly, ye big whale- set sails for malaria!")
      .build();

  private static final String KEY_ID = "Sail me bilge rat, ye swashbuckling mast!";

  private static final NewApiKey NEW_KEY = new NewApiKey("Yuck! Pieces o' madness are forever rainy.");

  private static ToolboxApiKeyClient newClient(final WireMockServer wireMockServer) {
    final URI hostUri = URI.create(wireMockServer.url(""));
    return ToolboxApiKeyClient.newClient(hostUri, API_KEY_PASSWORD);
  }

  @Nested
  final class ValidateApiKey {
    @Test
    void success(@WiremockResolver.Wiremock final WireMockServer server) {
      validateWillReturn(server, 200);

      final Optional<String> result = newClient(server).validateApiKey();

      assertThat(result, isEmpty());
    }

    private void validateWillReturn(final WireMockServer server, final int status) {
      server.stubFor(
          WireMock.post(ToolboxApiKeyClient.VALIDATE_API_KEY_PATH)
              .withHeader(ToolboxHttpHeaders.API_KEY_HEADER, equalTo(API_KEY_PASSWORD.getApiKey()))
              .withHeader(ToolboxHttpHeaders.API_KEY_PASSWORD_HEADER, equalTo(API_KEY_PASSWORD.getPassword()))
              .willReturn(
                  WireMock.aResponse().withStatus(status)));
    }

    @Test
    void failure(@WiremockResolver.Wiremock final WireMockServer server) {
      validateWillReturn(server, 401);

      final Optional<String> result = newClient(server).validateApiKey();

      assertThat(result, isPresent());
    }
  }


  @Test
  void getApiKeys(@WiremockResolver.Wiremock final WireMockServer server) {
    server.stubFor(
        WireMock.get(ToolboxApiKeyClient.GET_API_KEYS)
            .withHeader(ToolboxHttpHeaders.API_KEY_HEADER, equalTo(API_KEY_PASSWORD.getApiKey()))
            .withHeader(ToolboxHttpHeaders.API_KEY_PASSWORD_HEADER, equalTo(API_KEY_PASSWORD.getPassword()))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withBody(HttpClientTesting.toJson(Collections.singletonList(API_KEY_DATA)))));

    final List<ApiKeyData> result = newClient(server).getApiKeys();

    assertThat(result, hasSize(1));
    assertThat(result.get(0), is(API_KEY_DATA));
  }

  @Test
  void deleteApiKey(@WiremockResolver.Wiremock final WireMockServer server) {
    serve200ForToolboxPostWithBody(server, ToolboxApiKeyClient.DELETE_API_KEY, KEY_ID);

    newClient(server).deleteApiKey(KEY_ID);
  }

  @Test
  void generateSingleUseKey(@WiremockResolver.Wiremock final WireMockServer server) {
    server.stubFor(
        WireMock.post(ToolboxApiKeyClient.GENERATE_SINGLE_USE_KEY_PATH)
            .withHeader(ToolboxHttpHeaders.API_KEY_HEADER, equalTo(API_KEY_PASSWORD.getApiKey()))
            .withHeader(ToolboxHttpHeaders.API_KEY_PASSWORD_HEADER, equalTo(API_KEY_PASSWORD.getPassword()))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withBody(HttpClientTesting.toJson(NEW_KEY))));

    final NewApiKey result = newClient(server).generateSingleUseKey();

    assertThat(result, is(NEW_KEY));
  }
}
