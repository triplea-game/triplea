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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import javax.annotation.Nullable;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
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
import org.junitpioneer.jupiter.TempDirectory;
import org.junitpioneer.jupiter.TempDirectory.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import games.strategy.triplea.settings.AbstractClientSettingTestCase;

final class DownloadUtilsTest extends AbstractClientSettingTestCase {
  private static final String URI = "some://uri";

  @ExtendWith(MockitoExtension.class)
  @ExtendWith(TempDirectory.class)
  @Nested
  final class DownloadToFileTest {
    @Mock
    private CloseableHttpClient client;

    @Mock
    private HttpEntity entity;

    private File file;

    @Mock
    private CloseableHttpResponse response;

    @Mock
    private StatusLine statusLine;

    @BeforeEach
    void setUp(@TempDir final Path tempDirPath) throws Exception {
      file = Files.createTempFile(tempDirPath, null, null).toFile();

      when(client.execute(any())).thenReturn(response);
      when(response.getStatusLine()).thenReturn(statusLine);
    }

    @Test
    void shouldCopyEntityToFileWhenHappyPath() throws Exception {
      when(response.getEntity()).thenReturn(entity);
      when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
      final byte[] bytes = givenEntityContentIs(new byte[] {42, 43, 44, 45});

      downloadToFile();

      assertThat(fileContent(), is(bytes));
    }

    private byte[] givenEntityContentIs(final byte[] bytes) throws Exception {
      when(entity.getContent()).thenReturn(new ByteArrayInputStream(bytes));
      return bytes;
    }

    private void downloadToFile() throws Exception {
      try (FileOutputStream os = new FileOutputStream(file)) {
        ContentReader.downloadToFile(URI, os, client);
      }
    }

    private byte[] fileContent() throws Exception {
      return Files.readAllBytes(file.toPath());
    }

    @Test
    void shouldThrowExceptionWhenStatusCodeIsNotOk() {
      when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_NOT_FOUND);

      final Exception e = assertThrows(IOException.class, this::downloadToFile);

      assertThat(e.getMessage(), containsString("status code"));
    }

    @Test
    void shouldThrowExceptionWhenEntityIsAbsent() {
      when(response.getEntity()).thenReturn(null);
      when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);

      final Exception e = assertThrows(IOException.class, this::downloadToFile);

      assertThat(e.getMessage(), containsString("entity is missing"));
    }
  }

  @ExtendWith(MockitoExtension.class)
  @Nested
  final class GetDownloadLengthFromCacheTest {
    @Mock
    private DownloadLengthReader.DownloadLengthSupplier downloadLengthSupplier;

    @BeforeEach
    @SuppressWarnings("deprecation")
    void setUp() {
      DownloadUtils.downloadLengthReader.downloadLengthsByUri.clear();
    }

    @AfterEach
    @SuppressWarnings("deprecation")
    void tearDown() {
      DownloadUtils.downloadLengthReader.downloadLengthsByUri.clear();
    }

    @Test
    void shouldUseSupplierWhenUriAbsentFromCache() {
      when(downloadLengthSupplier.get(URI)).thenReturn(Optional.of(42L));

      final Optional<Long> downloadLength = getDownloadLengthFromCache();

      assertThat(downloadLength, is(Optional.of(42L)));
      verify(downloadLengthSupplier).get(URI);
    }

    @SuppressWarnings("deprecation")
    private Optional<Long> getDownloadLengthFromCache() {
      return DownloadUtils.downloadLengthReader.getDownloadLengthFromCache(URI, downloadLengthSupplier);
    }

    @Test
    @SuppressWarnings("deprecation")
    void shouldUseCacheWhenUriPresentInCache() {
      DownloadUtils.downloadLengthReader.downloadLengthsByUri.put(URI, 42L);

      final Optional<Long> downloadLength = getDownloadLengthFromCache();

      assertThat(downloadLength, is(Optional.of(42L)));
      verify(downloadLengthSupplier, never()).get(any());
    }

    @Test
    @SuppressWarnings("deprecation")
    void shouldNotUpdateCacheWhenSupplierProvidesEmptyValue() {
      when(downloadLengthSupplier.get(URI)).thenReturn(Optional.empty());

      final Optional<Long> downloadLength = getDownloadLengthFromCache();

      assertThat(downloadLength, is(Optional.empty()));
      assertThat(DownloadUtils.downloadLengthReader.downloadLengthsByUri, is(anEmptyMap()));
    }
  }

  @ExtendWith(MockitoExtension.class)
  @Nested
  final class GetDownloadLengthFromHostTest {
    @Mock
    private CloseableHttpClient client;

    @Mock
    private Header contentLengthHeader;

    @Mock
    private CloseableHttpResponse response;

    @Mock
    private StatusLine statusLine;

    @BeforeEach
    void setUp() throws Exception {
      when(client.execute(any())).thenReturn(response);
      when(response.getStatusLine()).thenReturn(statusLine);
      when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
    }

    @Test
    void shouldReturnLengthWhenContentLengthHeaderIsPresent() throws Exception {
      givenContentLengthHeaderValueIs("42");

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
      when(response.getFirstHeader(HttpHeaders.CONTENT_LENGTH)).thenReturn(null);

      final Optional<Long> length = getDownloadLengthFromHost();

      assertThat(length, is(Optional.empty()));
    }

    @Test
    void shouldThrowExceptionWhenContentLengthHeaderValueIsAbsent() {
      givenContentLengthHeaderValueIs(null);

      final Exception e = assertThrows(IOException.class, this::getDownloadLengthFromHost);

      assertThat(e.getMessage(), containsString("content length header value is absent"));
    }

    @Test
    void shouldThrowExceptionWhenContentLengthHeaderValueIsNotNumber() {
      givenContentLengthHeaderValueIs("value");

      final Exception e = assertThrows(IOException.class, this::getDownloadLengthFromHost);

      assertThat(e.getCause(), is(instanceOf(NumberFormatException.class)));
    }
  }
}
