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
import java.util.Optional;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.experimental.extensions.MockitoExtension;
import org.junit.experimental.extensions.TemporaryFolder;
import org.junit.experimental.extensions.TemporaryFolderExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import games.strategy.engine.framework.map.download.DownloadUtils.DownloadLengthSupplier;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;

public final class DownloadUtilsTest extends AbstractClientSettingTestCase {
  private static final String URI = "some://uri";

  @ExtendWith(MockitoExtension.class)
  @ExtendWith(TemporaryFolderExtension.class)
  @Nested
  public final class DownloadToFileTest {

    private TemporaryFolder temporaryFolder;

    @Mock
    private CloseableHttpClient client;

    @Mock
    private HttpEntity entity;

    private File file;

    @Mock
    private CloseableHttpResponse response;

    @Mock
    private StatusLine statusLine;

    /**
     * Sets up the test fixture.
     */
    @BeforeEach
    public void setUp() throws Exception {
      file = temporaryFolder.newFile(getClass().getName());

      when(client.execute(any())).thenReturn(response);
      when(response.getEntity()).thenReturn(entity);
      when(response.getStatusLine()).thenReturn(statusLine);
      when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
    }

    @Test
    public void shouldCopyEntityToFileWhenHappyPath() throws Exception {
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
        DownloadUtils.downloadToFile(URI, os, client);
      }
    }

    private byte[] fileContent() throws Exception {
      return Files.readAllBytes(file.toPath());
    }

    @Test
    public void shouldThrowExceptionWhenStatusCodeIsNotOk() {
      when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_NOT_FOUND);

      final Exception e = assertThrows(IOException.class, () -> downloadToFile());

      assertThat(e.getMessage(), containsString("status code"));
    }

    @Test
    public void shouldThrowExceptionWhenEntityIsAbsent() {
      when(response.getEntity()).thenReturn(null);

      final Exception e = assertThrows(IOException.class, () -> downloadToFile());

      assertThat(e.getMessage(), containsString("entity is missing"));
    }
  }

  @ExtendWith(MockitoExtension.class)
  @Nested
  public final class GetDownloadLengthFromCacheTest {
    @Mock
    private DownloadLengthSupplier downloadLengthSupplier;

    @BeforeEach
    public void setUp() {
      DownloadUtils.downloadLengthsByUri.clear();
    }

    @AfterEach
    public void tearDown() {
      DownloadUtils.downloadLengthsByUri.clear();
    }

    @Test
    public void shouldUseSupplierWhenUriAbsentFromCache() {
      when(downloadLengthSupplier.get(URI)).thenReturn(Optional.of(42L));

      final Optional<Long> downloadLength = getDownloadLengthFromCache();

      assertThat(downloadLength, is(Optional.of(42L)));
      verify(downloadLengthSupplier).get(URI);
    }

    private Optional<Long> getDownloadLengthFromCache() {
      return DownloadUtils.getDownloadLengthFromCache(URI, downloadLengthSupplier);
    }

    @Test
    public void shouldUseCacheWhenUriPresentInCache() {
      DownloadUtils.downloadLengthsByUri.put(URI, 42L);

      final Optional<Long> downloadLength = getDownloadLengthFromCache();

      assertThat(downloadLength, is(Optional.of(42L)));
      verify(downloadLengthSupplier, never()).get(any());
    }

    @Test
    public void shouldNotUpdateCacheWhenSupplierProvidesEmptyValue() {
      when(downloadLengthSupplier.get(URI)).thenReturn(Optional.empty());

      final Optional<Long> downloadLength = getDownloadLengthFromCache();

      assertThat(downloadLength, is(Optional.empty()));
      assertThat(DownloadUtils.downloadLengthsByUri, is(anEmptyMap()));
    }
  }

  @ExtendWith(MockitoExtension.class)
  @Nested
  public final class GetDownloadLengthFromHostTest {
    @Mock
    private CloseableHttpClient client;

    @Mock
    private Header contentLengthHeader;

    @Mock
    private CloseableHttpResponse response;

    @Mock
    private StatusLine statusLine;

    /**
     * Sets up the test fixture.
     */
    @BeforeEach
    public void setUp() throws Exception {
      when(client.execute(any())).thenReturn(response);
      when(response.getFirstHeader(HttpHeaders.CONTENT_LENGTH)).thenReturn(contentLengthHeader);
      when(response.getStatusLine()).thenReturn(statusLine);
      when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
    }

    @Test
    public void shouldReturnLengthWhenContentLengthHeaderIsPresent() throws Exception {
      when(contentLengthHeader.getValue()).thenReturn("42");

      final Optional<Long> length = getDownloadLengthFromHost();

      assertThat(length, is(Optional.of(42L)));
    }

    private Optional<Long> getDownloadLengthFromHost() throws Exception {
      return DownloadUtils.getDownloadLengthFromHost(URI, client);
    }

    @Test
    public void shouldThrowExceptionWhenStatusCodeIsNotOk() {
      when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_NOT_FOUND);

      final Exception e = assertThrows(IOException.class, () -> getDownloadLengthFromHost());

      assertThat(e.getMessage(), containsString("status code"));
    }

    @Test
    public void shouldReturnEmptyWhenContentLengthHeaderIsAbsent() throws Exception {
      when(response.getFirstHeader(HttpHeaders.CONTENT_LENGTH)).thenReturn(null);

      final Optional<Long> length = getDownloadLengthFromHost();

      assertThat(length, is(Optional.empty()));
    }

    @Test
    public void shouldThrowExceptionWhenContentLengthHeaderValueIsAbsent() {
      when(contentLengthHeader.getValue()).thenReturn(null);

      final Exception e = assertThrows(IOException.class, () -> getDownloadLengthFromHost());

      assertThat(e.getMessage(), containsString("content length header value is absent"));
    }

    @Test
    public void shouldThrowExceptionWhenContentLengthHeaderValueIsNotNumber() {
      when(contentLengthHeader.getValue()).thenReturn("value");

      final Exception e = assertThrows(IOException.class, () -> getDownloadLengthFromHost());

      assertThat(e.getCause(), is(instanceOf(NumberFormatException.class)));
    }
  }
}
