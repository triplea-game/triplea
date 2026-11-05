package games.strategy.engine.framework.map.download;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import games.strategy.engine.framework.map.download.DownloadFile.DownloadState;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.triplea.http.client.lobby.maps.listing.MapDownloadItem;
import org.triplea.io.CloseableDownloader;

class DownloadFileTest {
  private static final MapDownloadItem TEST_MAP =
      MapDownloadItem.builder()
          .downloadUrl("url")
          .previewImageUrl("preview-url")
          .description("description")
          .mapName("mapName")
          .lastCommitDateEpochMilli(60L)
          .downloadSizeInBytes(100L)
          .build();

  @TempDir private Path tempDir;

  @Test
  void testBasicStartCancel() throws InterruptedException {
    final CountDownLatch downloadStarted = new CountDownLatch(1);
    final CountDownLatch releaseDownload = new CountDownLatch(1);
    final ContentReader blockingContentReader =
        new ContentReader(
            uri -> {
              downloadStarted.countDown();
              try {
                releaseDownload.await();
              } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
              }
              throw new IOException("download interrupted");
            });
    final DownloadFile testObj =
        new DownloadFile(
            TEST_MAP,
            mock(DownloadListener.class),
            blockingContentReader,
            path -> Optional.empty());
    assertThat(testObj.getDownloadState()).isEqualTo(DownloadState.NOT_STARTED);

    testObj.startAsyncDownload();
    assertThat(testObj.getDownloadState()).isEqualTo(DownloadState.DOWNLOADING);
    assertThat(downloadStarted.await(5, TimeUnit.SECONDS)).isTrue();

    testObj.cancelDownload();
    assertThat(testObj.getDownloadState()).isEqualTo(DownloadState.CANCELLED);

    releaseDownload.countDown();
  }

  @Test
  void normalizeMapName() {
    assertThat(DownloadFile.normalizeMapName("valid-name")).isEqualTo("valid-name");
    assertThat(DownloadFile.normalizeMapName("also_valid")).isEqualTo("also_valid");
    assertThat(DownloadFile.normalizeMapName("a&b"))
        .as("Ampersand is a valid map name but scary in a file system, should be stripped")
        .isEqualTo("ab");
  }

  @ParameterizedTest
  @ValueSource(strings = {"*", ".", "\"", "/", "\\", "[", "]", ":", ";", "|", ","})
  void invalidCharactersAreStripped(final String invalidCharacter) {
    assertThat(DownloadFile.normalizeMapName(invalidCharacter)).isEqualTo("");
  }

  @Test
  void downloadFailureIsReportedAndFreesTheDownload() {
    final DownloadListener listener = mock(DownloadListener.class);
    final ContentReader failingContentReader =
        new ContentReader(
            uri -> {
              throw new IOException("server unreachable");
            });
    final DownloadFile downloadFile =
        new DownloadFile(TEST_MAP, listener, failingContentReader, path -> Optional.empty());

    downloadFile.startAsyncDownload();

    verify(listener, timeout(5000)).downloadFailed(TEST_MAP);
    verify(listener, never()).downloadComplete(any());
    assertThat(downloadFile.getDownloadState()).isEqualTo(DownloadState.FAILED);
    assertThat(downloadFile.isDone()).isTrue();
  }

  @Test
  void unzipFailureIsReportedAndFreesTheDownload() {
    final DownloadListener listener = mock(DownloadListener.class);
    final DownloadFile downloadFile =
        new DownloadFile(
            TEST_MAP, listener, contentReaderReturningEmptyFile(), path -> Optional.empty());

    downloadFile.startAsyncDownload();

    verify(listener, timeout(5000)).downloadFailed(TEST_MAP);
    verify(listener, never()).downloadComplete(any());
    assertThat(downloadFile.getDownloadState()).isEqualTo(DownloadState.FAILED);
    assertThat(downloadFile.isDone()).isTrue();
  }

  @Test
  void successfulDownloadIsReportedAsComplete() throws IOException {
    Files.writeString(
        tempDir.resolve("map.yml"),
        """
        map_name: example map
        games:
          - game_name: Great Game
            file_name: game-file.xml
        """);
    final DownloadListener listener = mock(DownloadListener.class);
    final DownloadFile downloadFile =
        new DownloadFile(
            TEST_MAP, listener, contentReaderReturningEmptyFile(), path -> Optional.of(tempDir));

    downloadFile.startAsyncDownload();

    verify(listener, timeout(5000)).downloadComplete(TEST_MAP);
    verify(listener, never()).downloadFailed(any());
    assertThat(downloadFile.getDownloadState()).isEqualTo(DownloadState.DONE);
    assertThat(downloadFile.isDone()).isTrue();
  }

  private static ContentReader contentReaderReturningEmptyFile() {
    return new ContentReader(
        uri ->
            new CloseableDownloader() {
              @Override
              public InputStream getStream() {
                return new ByteArrayInputStream(new byte[0]);
              }

              @Override
              public void close() {}
            });
  }
}
