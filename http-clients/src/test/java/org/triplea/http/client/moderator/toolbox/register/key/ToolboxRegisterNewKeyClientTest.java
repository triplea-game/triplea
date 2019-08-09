package org.triplea.http.client.moderator.toolbox.register.key;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.http.client.HttpClientTesting;
import org.triplea.http.client.moderator.toolbox.ApiKeyPassword;
import org.triplea.http.client.moderator.toolbox.ToolboxHttpHeaders;
import ru.lanwen.wiremock.ext.WiremockResolver;
import ru.lanwen.wiremock.ext.WiremockUriResolver;

@ExtendWith({WiremockResolver.class, WiremockUriResolver.class})
class ToolboxRegisterNewKeyClientTest {

  private static final ApiKeyPassword API_KEY_PASSWORD =
      ApiKeyPassword.builder()
          .apiKey("The tuna robs with grace, trade the pacific ocean before it laughs.")
          .password("Salty, haul a lively, rough shark.")
          .build();
  private static final RegisterApiKeyResult REGISTER_API_KEY_RESULT =
      RegisterApiKeyResult.builder()
          .newApiKey("Aww, yer not burning me without an adventure!")
          .build();

  private static ToolboxRegisterNewKeyClient newClient(final WireMockServer wireMockServer) {
    final URI hostUri = URI.create(wireMockServer.url(""));
    return ToolboxRegisterNewKeyClient.newClient(hostUri);
  }

  @Test
  void registerNewKey(@WiremockResolver.Wiremock final WireMockServer server) {
    expectRequestAndReturnStatusAndBody(server, 200);

    final RegisterApiKeyResult result = newClient(server).registerNewKey(API_KEY_PASSWORD);

    assertThat(result, is(REGISTER_API_KEY_RESULT));
  }

  private void expectRequestAndReturnStatusAndBody(final WireMockServer server, final int status) {
    server.stubFor(
        WireMock.post(ToolboxRegisterNewKeyClient.REGISTER_API_KEY_PATH)
            .withHeader(ToolboxHttpHeaders.API_KEY_HEADER, equalTo(API_KEY_PASSWORD.getApiKey()))
            .withHeader(
                ToolboxHttpHeaders.API_KEY_PASSWORD_HEADER, equalTo(API_KEY_PASSWORD.getPassword()))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(status)
                    .withBody(HttpClientTesting.toJson(REGISTER_API_KEY_RESULT))));
  }

  @Test
  void registerNewKey400Case(@WiremockResolver.Wiremock final WireMockServer server) {
    expectRequestAndReturnStatusAndBody(server, 400);

    final RegisterApiKeyResult result = newClient(server).registerNewKey(API_KEY_PASSWORD);

    assertThat(result.getNewApiKey(), nullValue());
    assertThat(result.getErrorMessage(), notNullValue());
  }
}
