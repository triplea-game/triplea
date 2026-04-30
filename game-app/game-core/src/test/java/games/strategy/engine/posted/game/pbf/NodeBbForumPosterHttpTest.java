package games.strategy.engine.posted.game.pbf;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.WireMockServer;
import games.strategy.engine.posted.game.pbf.NodeBbForumPoster.ForumPostingParameters;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.triplea.http.HttpClientHeaders;

/**
 * End-to-end test for {@link NodeBbForumPoster} that runs a real Apache HttpClient against a
 * WireMock-stubbed NodeBB-style endpoint and verifies that {@code Triplea-Version} and {@code
 * User-Agent} headers are attached to outbound requests.
 */
class NodeBbForumPosterHttpTest extends AbstractClientSettingTestCase {

  private static final String TEST_VERSION = "9.9.9-test";
  private static final int TOPIC_ID = 12345;
  private static final String TOKEN = "test-token";

  private WireMockServer server;
  private String forumUrl;

  @BeforeEach
  void startServer() {
    server = new WireMockServer(0);
    server.start();
    forumUrl = "http://localhost:" + server.port();
    HttpClientHeaders.setVersion(TEST_VERSION);
  }

  @AfterEach
  void stopServer() {
    server.stop();
    HttpClientHeaders.setProvider(
        () ->
            Map.of(
                HttpClientHeaders.VERSION_HEADER, "Unknown",
                HttpClientHeaders.USER_AGENT_HEADER, "triplea/Unknown"));
  }

  @Test
  @DisplayName("postTurnSummary attaches Triplea-Version and User-Agent to topic-post request")
  void topicPostHasStandardHeaders() throws ExecutionException, InterruptedException {
    server.stubFor(
        post(urlEqualTo("/api/v2/topics/" + TOPIC_ID)).willReturn(aResponse().withStatus(200)));

    final NodeBbForumPoster poster =
        new NodeBbForumPoster(
            ForumPostingParameters.builder()
                .topicId(TOPIC_ID)
                .token(TOKEN.toCharArray())
                .forumUrl(forumUrl)
                .build());

    final String result = poster.postTurnSummary("body", "title", null).get();

    assertThat(result, is("Successfully posted!"));
    server.verify(
        postRequestedFor(urlEqualTo("/api/v2/topics/" + TOPIC_ID))
            .withHeader(HttpClientHeaders.VERSION_HEADER, equalTo(TEST_VERSION))
            .withHeader(HttpClientHeaders.USER_AGENT_HEADER, equalTo("triplea/" + TEST_VERSION)));
  }

  @Test
  @DisplayName("savegame upload request also carries Triplea-Version and User-Agent")
  void uploadHasStandardHeaders() throws Exception {
    server.stubFor(
        post(urlPathEqualTo("/api/v2/util/upload"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{\"response\":{\"images\":[{\"url\":\"http://example.com/save.tsvg\"}]}}")));
    server.stubFor(
        post(urlEqualTo("/api/v2/topics/" + TOPIC_ID)).willReturn(aResponse().withStatus(200)));

    final Path tmp = Files.createTempFile("test-save", ".tsvg");
    Files.writeString(tmp, "fake-savegame");
    tmp.toFile().deleteOnExit();

    final NodeBbForumPoster poster =
        new NodeBbForumPoster(
            ForumPostingParameters.builder()
                .topicId(TOPIC_ID)
                .token(TOKEN.toCharArray())
                .forumUrl(forumUrl)
                .build());

    poster
        .postTurnSummary(
            "body",
            "title",
            NodeBbForumPoster.SaveGameParameter.builder()
                .path(tmp)
                .displayName("save.tsvg")
                .build())
        .get();

    server.verify(
        postRequestedFor(urlPathEqualTo("/api/v2/util/upload"))
            .withHeader(HttpClientHeaders.VERSION_HEADER, equalTo(TEST_VERSION))
            .withHeader(HttpClientHeaders.USER_AGENT_HEADER, matching("triplea/" + TEST_VERSION)));
  }
}
