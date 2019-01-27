package games.strategy.engine.framework.map.download;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.TempDirectory;
import org.junitpioneer.jupiter.TempDirectory.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import games.strategy.triplea.settings.AbstractClientSettingTestCase;

final class ContentReaderTest extends AbstractClientSettingTestCase {
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
      new ContentReader(() -> client).downloadToFile(URI, file);
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
}
