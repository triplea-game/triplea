package games.strategy.engine.framework.map.download;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.function.Consumer;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentDownloaderTest {
  private static final URI FAKE_URI = URI.create("http://fake.com");

  @Mock private InputStream inputStream;

  @Mock private HttpEntity httpEntity;

  @Mock private StatusLine statusLine;

  @Mock private CloseableHttpClient httpClient;

  @Mock private CloseableHttpResponse closeableHttpResponse;

  @Mock private Consumer<HttpGet> proxySettings;

  @Test
  @DisplayName("If http return status is not a 200, should throw an IOException")
  void throwIfNotOkayStatus() throws Exception {
    givenStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);

    assertThrows(
        IOException.class, () -> new ContentDownloader(httpClient, FAKE_URI, proxySettings));
  }

  void givenStatus(final int status) throws Exception {
    when(httpClient.execute(Mockito.any())).thenReturn(closeableHttpResponse);
    when(closeableHttpResponse.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(status);
  }

  @Test
  @DisplayName("If http response does not contain an entity, error, should throw an IOException")
  void throwIfResponseEntityIsMissing() throws Exception {
    givenResponseEntity(null);

    assertThrows(
        IOException.class, () -> new ContentDownloader(httpClient, FAKE_URI, proxySettings));
  }

  void givenResponseEntity(final HttpEntity httpEntity) throws Exception {
    givenStatus(HttpStatus.SC_OK);
    when(closeableHttpResponse.getEntity()).thenReturn(httpEntity);
  }

  @Test
  @DisplayName("Happy case, verify we can get an input stream from the http response entity")
  void returnsInputStreamFromEntityContent() throws Exception {
    givenEntityContent();

    final InputStream inputStream =
        new ContentDownloader(httpClient, FAKE_URI, proxySettings).getStream();

    assertThat(inputStream, sameInstance(inputStream));
  }

  void givenEntityContent() throws Exception {
    when(httpEntity.getContent()).thenReturn(inputStream);
    givenResponseEntity(httpEntity);
  }

  @Test
  @DisplayName("Verify proxy settings are applied")
  void proxySettingsAreApplied() throws Exception {
    givenEntityContent();

    new ContentDownloader(httpClient, FAKE_URI, proxySettings);

    verify(proxySettings).accept(any());
  }

  @SuppressWarnings({"EmptyTryBlock", "try"})
  @Test
  @DisplayName("Verify all resources are closed")
  void resourcesAreClosed() throws Exception {
    givenEntityContent();
    try (CloseableDownloader ignored = new ContentDownloader(httpClient, FAKE_URI, proxySettings)) {
      // no-op
    }

    verify(inputStream).close();
    verify(closeableHttpResponse).close();
    verify(httpClient).close();
  }
}
