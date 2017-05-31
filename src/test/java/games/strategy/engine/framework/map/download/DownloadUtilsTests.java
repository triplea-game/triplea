package games.strategy.engine.framework.map.download;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static com.googlecode.catchexception.apis.CatchExceptionHamcrestMatchers.hasMessageThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
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
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import games.strategy.engine.framework.map.download.DownloadUtils.DownloadLengthSupplier;

@RunWith(Enclosed.class)
public final class DownloadUtilsTests {
  private static final String URI = "some://uri";

  @RunWith(MockitoJUnitRunner.class)
  public static final class DownloadToFile {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private CloseableHttpClient client;

    @Mock
    private HttpEntity entity;

    private File file;

    private FileOutputStream os;

    @Mock
    private CloseableHttpResponse response;

    @Mock
    private StatusLine statusLine;

    /**
     * Sets up the test fixture.
     */
    @Before
    public void setUp() throws Exception {
      file = temporaryFolder.newFile();
      os = new FileOutputStream(file);

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
      DownloadUtils.downloadToFile(URI, os, client);
    }

    private byte[] fileContent() throws Exception {
      return Files.readAllBytes(file.toPath());
    }

    @Test
    public void shouldThrowExceptionWhenStatusCodeIsNotOk() {
      when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_NOT_FOUND);

      catchException(() -> downloadToFile());

      assertThat(caughtException(), allOf(
          is(instanceOf(IOException.class)),
          hasMessageThat(containsString("status code"))));
    }

    @Test
    public void shouldThrowExceptionWhenEntityIsAbsent() {
      when(response.getEntity()).thenReturn(null);

      catchException(() -> downloadToFile());

      assertThat(caughtException(), allOf(
          is(instanceOf(IOException.class)),
          hasMessageThat(containsString("entity is missing"))));
    }
  }

  @RunWith(MockitoJUnitRunner.class)
  public static final class GetDownloadLengthFromCache {
    @Mock
    private DownloadLengthSupplier downloadLengthSupplier;

    @Before
    public void setUp() {
      DownloadUtils.downloadLengthsByUri.clear();
    }

    @After
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

  @RunWith(MockitoJUnitRunner.class)
  public static final class GetDownloadLengthFromHost {
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
    @Before
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

      catchException(() -> getDownloadLengthFromHost());

      assertThat(caughtException(), allOf(
          is(instanceOf(IOException.class)),
          hasMessageThat(containsString("status code"))));
    }

    @Test
    public void shouldReturnEmptyWhenContentLengthHeaderIsAbsent() throws Exception {
      when(response.getFirstHeader(HttpHeaders.CONTENT_LENGTH)).thenReturn(null);

      final Optional<Long> length = getDownloadLengthFromHost();

      assertThat(length, is(Optional.empty()));
    }

    @Test
    public void shouldThrowExceptionWhenContentLengthHeaderValueIsAbsent() throws Exception {
      when(contentLengthHeader.getValue()).thenReturn(null);

      catchException(() -> getDownloadLengthFromHost());

      assertThat(caughtException(), allOf(
          is(instanceOf(IOException.class)),
          hasMessageThat(containsString("content length header value is absent"))));
    }

    @Test
    public void shouldThrowExceptionWhenContentLengthHeaderValueIsNotNumber() throws Exception {
      when(contentLengthHeader.getValue()).thenReturn("value");

      catchException(() -> getDownloadLengthFromHost());

      assertThat(caughtException(), is(instanceOf(IOException.class)));
      assertThat(caughtException().getCause(), is(instanceOf(NumberFormatException.class)));
    }
  }
}
