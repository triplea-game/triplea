package games.strategy.engine.framework.map.download;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import java.io.IOException;
import java.util.Optional;
import javax.annotation.Nullable;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

final class DownloadLengthReaderTest extends AbstractClientSettingTestCase {
  private static final String URI = "some://uri";

  @ExtendWith(MockitoExtension.class)
  @Nested
  final class GetDownloadLengthFromCacheTest {
    @Mock private DownloadLengthReader.DownloadLengthSupplier downloadLengthSupplier;

    @BeforeEach
    void setUp() {
      DownloadConfiguration.downloadLengthReader().downloadLengthsByUri.clear();
    }

    @AfterEach
    void tearDown() {
      DownloadConfiguration.downloadLengthReader().downloadLengthsByUri.clear();
    }

    @Test
    void shouldUseSupplierWhenUriAbsentFromCache() {
      when(downloadLengthSupplier.get(URI)).thenReturn(Optional.of(42L));

      final Optional<Long> downloadLength = getDownloadLengthFromCache();

      assertThat(downloadLength, is(Optional.of(42L)));
      verify(downloadLengthSupplier).get(URI);
    }

    private Optional<Long> getDownloadLengthFromCache() {
      return DownloadConfiguration.downloadLengthReader()
          .getDownloadLengthFromCache(URI, downloadLengthSupplier);
    }

    @Test
    void shouldUseCacheWhenUriPresentInCache() {
      DownloadConfiguration.downloadLengthReader().downloadLengthsByUri.put(URI, 42L);

      final Optional<Long> downloadLength = getDownloadLengthFromCache();

      assertThat(downloadLength, is(Optional.of(42L)));
      verify(downloadLengthSupplier, never()).get(any());
    }

    @Test
    void shouldNotUpdateCacheWhenSupplierProvidesEmptyValue() {
      when(downloadLengthSupplier.get(URI)).thenReturn(Optional.empty());

      final Optional<Long> downloadLength = getDownloadLengthFromCache();

      assertThat(downloadLength, is(Optional.empty()));
      assertThat(
          DownloadConfiguration.downloadLengthReader().downloadLengthsByUri, is(anEmptyMap()));
    }
  }

  @ExtendWith(MockitoExtension.class)
  @Nested
  final class GetDownloadLengthFromHostTest {
    @Mock private CloseableHttpClient client;

    @Mock private Header contentLengthHeader;

    @Mock private CloseableHttpResponse response;

    @Mock private StatusLine statusLine;

    @BeforeEach
    void setUp() throws Exception {
      when(client.execute(any())).thenReturn(response);
      when(response.getStatusLine()).thenReturn(statusLine);
    }

    private void givenOkStatus() {
      when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
    }

    @Test
    void shouldReturnLengthWhenContentLengthHeaderIsPresent() throws Exception {
      givenContentLengthHeaderValueIs("42");
      givenOkStatus();

      final Optional<Long> length = getDownloadLengthFromHost();

      assertThat(length, is(Optional.of(42L)));
    }

    private void givenContentLengthHeaderValueIs(final @Nullable String value) {
      when(contentLengthHeader.getValue()).thenReturn(value);
      when(response.getFirstHeader(HttpHeaders.CONTENT_LENGTH)).thenReturn(contentLengthHeader);
    }

    private Optional<Long> getDownloadLengthFromHost() throws Exception {
      return DownloadLengthReader.getDownloadLengthFromHost(URI, client);
    }

    @Test
    void shouldThrowExceptionWhenStatusCodeIsNotOk() {
      when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_NOT_FOUND);

      final Exception e = assertThrows(IOException.class, this::getDownloadLengthFromHost);

      assertThat(e.getMessage(), containsString("status code"));
    }

    @Test
    void shouldReturnEmptyWhenContentLengthHeaderIsAbsent() throws Exception {
      givenOkStatus();
      when(response.getFirstHeader(HttpHeaders.CONTENT_LENGTH)).thenReturn(null);

      final Optional<Long> length = getDownloadLengthFromHost();

      assertThat(length, is(Optional.empty()));
    }

    @Test
    void shouldThrowExceptionWhenContentLengthHeaderValueIsAbsent() {
      givenOkStatus();
      givenContentLengthHeaderValueIs(null);

      final Exception e = assertThrows(IOException.class, this::getDownloadLengthFromHost);

      assertThat(e.getMessage(), containsString("content length header value is absent"));
    }

    @Test
    void shouldThrowExceptionWhenContentLengthHeaderValueIsNotNumber() {
      givenOkStatus();
      givenContentLengthHeaderValueIs("value");

      final Exception e = assertThrows(IOException.class, this::getDownloadLengthFromHost);

      assertThat(e.getCause(), is(instanceOf(NumberFormatException.class)));
    }
  }
}
