package org.triplea.http;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;

import java.util.Map;
import org.apache.http.Header;
import org.apache.http.client.methods.HttpGet;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class HttpClientHeadersTest {

  @AfterEach
  void resetToDefault() {
    HttpClientHeaders.resetForTesting();
  }

  @Nested
  @DisplayName("apply()")
  class Apply {
    @Test
    @DisplayName("default provider attaches Unknown headers when no provider is set")
    void defaultUnknownHeaders() {
      final HttpGet request = new HttpGet("https://example.com/");

      HttpClientHeaders.apply(request);

      assertThat(
          request.getAllHeaders(),
          headersMatching(
              Map.of(
                  HttpClientHeaders.VERSION_HEADER, "Unknown",
                  HttpClientHeaders.USER_AGENT_HEADER, "triplea/Unknown")));
    }

    @Test
    @DisplayName("attaches all headers returned by the registered provider")
    void appliesProviderHeaders() {
      HttpClientHeaders.setProvider(
          () ->
              Map.of(
                  HttpClientHeaders.VERSION_HEADER,
                  "2.7.0",
                  HttpClientHeaders.USER_AGENT_HEADER,
                  "triplea/2.7.0",
                  "X-Extra",
                  "extra-value"));
      final HttpGet request = new HttpGet("https://example.com/");

      HttpClientHeaders.apply(request);

      assertThat(
          request.getAllHeaders(),
          headersMatching(
              Map.of(
                  HttpClientHeaders.VERSION_HEADER,
                  "2.7.0",
                  HttpClientHeaders.USER_AGENT_HEADER,
                  "triplea/2.7.0",
                  "X-Extra",
                  "extra-value")));
    }
  }

  @Nested
  @DisplayName("setProvider()")
  class SetProvider {
    @Test
    @DisplayName("most recent provider wins")
    void lastProviderWins() {
      HttpClientHeaders.setProvider(() -> Map.of(HttpClientHeaders.VERSION_HEADER, "1.0"));
      HttpClientHeaders.setProvider(() -> Map.of(HttpClientHeaders.VERSION_HEADER, "2.0"));
      final HttpGet request = new HttpGet("https://example.com/");

      HttpClientHeaders.apply(request);

      assertThat(
          request.getAllHeaders(),
          headersMatching(Map.of(HttpClientHeaders.VERSION_HEADER, "2.0")));
    }
  }

  @Nested
  @DisplayName("setVersion()")
  class SetVersion {
    @Test
    @DisplayName("emits Triplea-Version and triplea/<version> User-Agent")
    void emitsBothHeaders() {
      HttpClientHeaders.setVersion("2.8.1");
      final HttpGet request = new HttpGet("https://example.com/");

      HttpClientHeaders.apply(request);

      assertThat(
          request.getAllHeaders(),
          headersMatching(
              Map.of(
                  HttpClientHeaders.VERSION_HEADER, "2.8.1",
                  HttpClientHeaders.USER_AGENT_HEADER, "triplea/2.8.1")));
    }
  }

  @SuppressWarnings("unchecked")
  private static Matcher<Header[]> headersMatching(final Map<String, String> expected) {
    final Matcher<Header>[] expectations =
        expected.entrySet().stream()
            .map(e -> new HeaderMatcher(e.getKey(), e.getValue()))
            .toArray(Matcher[]::new);
    return arrayContainingInAnyOrder(expectations);
  }

  private static final class HeaderMatcher extends BaseMatcher<Header> {
    private final String name;
    private final String value;

    HeaderMatcher(final String name, final String value) {
      this.name = name;
      this.value = value;
    }

    @Override
    public boolean matches(final Object item) {
      if (!(item instanceof Header)) {
        return false;
      }
      final Header h = (Header) item;
      return name.equals(h.getName()) && value.equals(h.getValue());
    }

    @Override
    public void describeTo(final Description description) {
      description.appendText(name + ": " + value);
    }
  }
}
