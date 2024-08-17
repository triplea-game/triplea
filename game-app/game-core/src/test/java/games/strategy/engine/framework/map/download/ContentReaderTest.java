package games.strategy.engine.framework.map.download;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jetbrains.annotations.NonNls;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.io.CloseableDownloader;

@SuppressWarnings("InnerClassMayBeStatic")
final class ContentReaderTest extends AbstractClientSettingTestCase {
  @NonNls private static final String URI = "some://uri";

  @ExtendWith(MockitoExtension.class)
  @Nested
  final class DownloadToFileTest {
    @Mock private CloseableDownloader closeableDownloader;

    private Path file;

    @BeforeEach
    void setUp(@TempDir final Path tempDirPath) throws Exception {
      file = Files.createTempFile(tempDirPath, null, null);
    }

    @Test
    void shouldCopyEntityToFileWhenHappyPath() throws Exception {
      final byte[] bytes = givenEntityContentIs(new byte[] {42, 43, 44, 45});

      downloadToFile();

      assertThat(fileContent(), is(bytes));
    }

    private byte[] givenEntityContentIs(final byte[] bytes) {
      when(closeableDownloader.getStream()).thenReturn(new ByteArrayInputStream(bytes));
      return bytes;
    }

    private void downloadToFile() throws Exception {
      new ContentReader(uri -> closeableDownloader).downloadToFile(URI, file);
    }

    private byte[] fileContent() throws Exception {
      return Files.readAllBytes(file);
    }
  }
}
