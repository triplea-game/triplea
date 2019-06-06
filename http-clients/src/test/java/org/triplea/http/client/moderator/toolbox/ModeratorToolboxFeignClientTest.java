package org.triplea.http.client.moderator.toolbox;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;

import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.HttpClientTesting;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.gson.Gson;

import lombok.Builder;
import ru.lanwen.wiremock.ext.WiremockResolver;
import ru.lanwen.wiremock.ext.WiremockUriResolver;

@ExtendWith({
    WiremockResolver.class,
    WiremockUriResolver.class
})
class ModeratorToolboxFeignClientTest {
  private static final String SUCCESS_JSON = "SUCCESS";
  private static final String INPUT_JSON = "word";

  private static final ModeratorEvent EVENT_1 = ModeratorEvent.builder()
      .date(Instant.now())
      .actionTarget("Malaria is a cloudy pin.")
      .moderatorAction("Jolly roger, real pin. go to puerto rico.")
      .moderatorName("All parrots loot rainy, stormy fish.")
      .build();

  private static final ModeratorEvent EVENT_2 = ModeratorEvent.builder()
      .date(Instant.now().minusSeconds(1000L))
      .actionTarget("Strength is a gutless tuna.")
      .moderatorAction("Doubloons travel with booty at the stormy madagascar!")
      .moderatorName("The son crushes with life, love the lighthouse.")
      .build();

  private static final Map<String, Object> headerMap;

  private static final String HEADER_KEY = "key";
  private static final String HEADER_VALUE = "value";

  static {
    headerMap = new HashMap<>();
    headerMap.put(HEADER_KEY, HEADER_VALUE);
  }

  @Test
  void validateApiKey(@WiremockResolver.Wiremock final WireMockServer wireMockServer) {
    wireMockServer.stubFor(
        WireMock.post(ModeratorToolboxClient.VALIDATE_API_KEY_PATH)
            .withHeader(HEADER_KEY, equalTo(HEADER_VALUE))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withBody(SUCCESS_JSON)));

    final ModeratorToolboxFeignClient client = newClient(wireMockServer);

    assertThat(
        client.validateApiKey(headerMap),
        is(ModeratorToolboxClient.SUCCESS));
  }

  private static ModeratorToolboxFeignClient newClient(final WireMockServer wireMockServer) {
    final URI hostUri = URI.create(wireMockServer.url(""));
    final int maxAttempts = 1;
    return new HttpClient<>(ModeratorToolboxFeignClient.class, hostUri, maxAttempts).get();
  }

  @Test
  void addBadWord(@WiremockResolver.Wiremock final WireMockServer wireMockServer) {
    expectPost(wireMockServer, ModeratorToolboxClient.BAD_WORD_ADD_PATH);
    final ModeratorToolboxFeignClient client = newClient(wireMockServer);

    assertThat(
        client.addBadWord(headerMap, "word"),
        is(ModeratorToolboxClient.SUCCESS));
  }

  private void expectPost(final WireMockServer wireMockServer, final String path) {
    wireMockServer.stubFor(
        WireMock.post(path)
            .withHeader(HEADER_KEY, equalTo(HEADER_VALUE))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withBody(SUCCESS_JSON)));
  }

  @Test
  void removeBadWord(@WiremockResolver.Wiremock final WireMockServer wireMockServer) {
    expectPost(wireMockServer, ModeratorToolboxClient.BAD_WORD_REMOVE_PATH);
    final ModeratorToolboxFeignClient client = newClient(wireMockServer);

    assertThat(
        client.removeBadWord(headerMap, INPUT_JSON),
        is(ModeratorToolboxClient.SUCCESS));
  }

  @Test
  void getBadWords(@WiremockResolver.Wiremock final WireMockServer wireMockServer) {
    final ModeratorToolboxFeignClient client = newClient(wireMockServer);
    stubForGetBadWods(wireMockServer);

    assertThat(
        client.getBadWords(headerMap),
        is(asList("value", "value2")));
  }

  private void stubForGetBadWods(final WireMockServer wireMockServer) {
    wireMockServer.stubFor(
        WireMock.get(ModeratorToolboxClient.BAD_WORD_GET_PATH)
            .withHeader(HEADER_KEY, equalTo(HEADER_VALUE))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withBody("[ \"value\", \"value2\" ]")));
  }

  @Test
  void lookupAuditHistory(@WiremockResolver.Wiremock final WireMockServer wireMockServer) {
    wireMockServer.stubFor(
        WireMock.get(ModeratorToolboxClient.AUDIT_HISTORY_PATH + "?rowStart=3&rowCount=10")
            .withHeader(HEADER_KEY, equalTo(HEADER_VALUE))
            .withQueryParam("rowStart", equalTo("3"))
            .withQueryParam("rowCount", equalTo("10"))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withBody(new Gson().toJson(Arrays.asList(EVENT_1, EVENT_2)))));

    final List<ModeratorEvent> results = newClient(wireMockServer)
        .lookupModeratorEvents(headerMap, 3, 10);

    assertThat(results, hasSize(2));
    assertThat(results.get(0), is(EVENT_1));
    assertThat(results.get(1), is(EVENT_2));
  }

  @Test
  void verifyErrorHandling(@WiremockResolver.Wiremock final WireMockServer wireMockServer) {
    final ModeratorToolboxFeignClient client = newClient(wireMockServer);

    asList(
        ErrorHandlingArg.builder()
            .path(ModeratorToolboxClient.VALIDATE_API_KEY_PATH)
            .requestType(HttpClientTesting.RequestType.POST)
            .serviceCall(uri -> client.validateApiKey(new HashMap<>()))
            .build(),
        ErrorHandlingArg.builder()
            .path(ModeratorToolboxClient.BAD_WORD_ADD_PATH)
            .requestType(HttpClientTesting.RequestType.POST)
            .serviceCall(uri -> client.addBadWord(new HashMap<>(), "word"))
            .build(),
        ErrorHandlingArg.builder()
            .path(ModeratorToolboxClient.BAD_WORD_GET_PATH)
            .requestType(HttpClientTesting.RequestType.GET)
            .serviceCall(uri -> client.getBadWords(new HashMap<>()))
            .build(),
        ErrorHandlingArg.builder()
            .path(ModeratorToolboxClient.BAD_WORD_REMOVE_PATH)
            .requestType(HttpClientTesting.RequestType.POST)
            .serviceCall(uri -> client.removeBadWord(new HashMap<>(), "word"))
            .build(),
        ErrorHandlingArg.builder()
            .path(ModeratorToolboxClient.AUDIT_HISTORY_PATH)
            .requestType(HttpClientTesting.RequestType.GET)
            .serviceCall(uri -> client.lookupModeratorEvents(new HashMap<>(), 0, 1))
            .build())
                .forEach(arg -> HttpClientTesting.verifyErrorHandling(
                    wireMockServer,
                    arg.path,
                    arg.requestType,
                    arg.serviceCall));
  }

  @Builder
  private static final class ErrorHandlingArg<T> {
    @Nonnull
    private final String path;
    @Nonnull
    private final HttpClientTesting.RequestType requestType;
    @Nonnull
    private final Function<URI, T> serviceCall;
  }

}
