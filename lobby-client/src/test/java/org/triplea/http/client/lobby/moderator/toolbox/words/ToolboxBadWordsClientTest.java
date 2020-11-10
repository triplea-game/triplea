package org.triplea.http.client.lobby.moderator.toolbox.words;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.triplea.http.client.HttpClientTesting.EXPECTED_API_KEY;
import static org.triplea.http.client.HttpClientTesting.serve200ForToolboxPostWithBody;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.WireMockTest;
import org.triplea.test.common.JsonUtil;
import ru.lanwen.wiremock.ext.WiremockResolver;

class ToolboxBadWordsClientTest extends WireMockTest {
  private static final String BAD_WORD = "Damn yer bilge rat, feed the corsair.";
  private static final List<String> badWords = List.of("one", "two", "three");

  private static ToolboxBadWordsClient newClient(final WireMockServer wireMockServer) {
    return newClient(wireMockServer, ToolboxBadWordsClient::newClient);
  }

  @Test
  void removeBadWord(@WiremockResolver.Wiremock final WireMockServer server) {
    serve200ForToolboxPostWithBody(server, ToolboxBadWordsClient.BAD_WORD_REMOVE_PATH, BAD_WORD);

    newClient(server).removeBadWord(BAD_WORD);
  }

  @Test
  void addBadWord(@WiremockResolver.Wiremock final WireMockServer server) {
    serve200ForToolboxPostWithBody(server, ToolboxBadWordsClient.BAD_WORD_ADD_PATH, BAD_WORD);

    newClient(server).addBadWord(BAD_WORD);
  }

  @Test
  void getBadWords(@WiremockResolver.Wiremock final WireMockServer server) {
    server.stubFor(
        WireMock.get(ToolboxBadWordsClient.BAD_WORD_GET_PATH)
            .withHeader(AuthenticationHeaders.API_KEY_HEADER, equalTo(EXPECTED_API_KEY))
            .willReturn(WireMock.aResponse().withStatus(200).withBody(JsonUtil.toJson(badWords))));

    final List<String> result = newClient(server).getBadWords();

    assertThat(result, is(badWords));
  }
}
