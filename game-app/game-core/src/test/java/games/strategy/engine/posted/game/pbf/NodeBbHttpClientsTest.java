package games.strategy.engine.posted.game.pbf;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.github.tomakehurst.wiremock.WireMockServer;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;

class NodeBbHttpClientsTest extends AbstractClientSettingTestCase {

  @Test
  void newPreAuthClient_attachesUserAgentNoAuthorization() throws Exception {
    final WireMockServer server = new WireMockServer(0);
    try {
      server.start();
      server.stubFor(get(urlEqualTo("/")).willReturn(aResponse().withStatus(200)));

      try (CloseableHttpClient client = NodeBbHttpClients.newPreAuthClient()) {
        client.execute(new HttpGet("http://localhost:" + server.port() + "/")).close();
      }

      server.verify(
          getRequestedFor(urlEqualTo("/"))
              .withHeader("User-Agent", matching("triplea/.*"))
              .withHeader("Authorization", absent()));
    } finally {
      server.stop();
    }
  }

  @Test
  void newPostAuthClient_attachesUserAgentAndAuthorization() throws Exception {
    final WireMockServer server = new WireMockServer(0);
    try {
      server.start();
      server.stubFor(get(urlEqualTo("/")).willReturn(aResponse().withStatus(200)));

      try (CloseableHttpClient client = NodeBbHttpClients.newPostAuthClient("test-token")) {
        client.execute(new HttpGet("http://localhost:" + server.port() + "/")).close();
      }

      server.verify(
          getRequestedFor(urlEqualTo("/"))
              .withHeader("User-Agent", matching("triplea/.*"))
              .withHeader("Authorization", equalTo("Bearer test-token")));
    } finally {
      server.stop();
    }
  }
}
