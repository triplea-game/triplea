package games.strategy.engine.framework.map.download;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import games.strategy.engine.framework.map.download.DownloadFile.DownloadState;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import games.strategy.triplea.settings.ClientSetting;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.triplea.http.client.lobby.maps.listing.MapDownloadItem;
import org.triplea.io.CloseableDownloader;

class DownloadFileTest extends AbstractClientSettingTestCase {
  private static final MapDownloadItem MAP_DOWNLOAD_ITEM =
      MapDownloadItem.builder()
          .downloadUrl("url")
          .previewImageUrl("preview-url")
          .description("description")
          .mapName("mapName")
          .lastCommitDateEpochMilli(60L)
          .downloadSizeInBytes(100L)
          .build();

  // lets the downloads that wait for it fail, so their threads end with the test
  private final CountDownLatch release = new CountDownLatch(1);

  @AfterEach
  void releaseDownloads() {
    release.countDown();
  }

  @Test
  void testBasicStartCancel() {
    final DownloadListener listener = mock(DownloadListener.class);
    final DownloadFile testObj =
        new DownloadFile(MAP_DOWNLOAD_ITEM, listener, givenReaderFailingOn(release));
    assertThat(testObj.getDownloadState()).isEqualTo(DownloadState.NOT_STARTED);

    testObj.startAsyncDownload();
    assertThat(testObj.getDownloadState()).isEqualTo(DownloadState.DOWNLOADING);

    testObj.cancelDownload();
    release.countDown();
    assertThat(testObj.getDownloadState()).isEqualTo(DownloadState.CANCELLED);
    verify(listener, after(500).never()).downloadFailed(MAP_DOWNLOAD_ITEM);
    verify(listener, never()).downloadComplete(MAP_DOWNLOAD_ITEM);
  }

  @DisplayName("A failed download ends and tells its listener, so its map can be installed again")
  @Test
  void failedDownloadEnds() {
    final DownloadListener listener = mock(DownloadListener.class);
    final DownloadFile download =
        new DownloadFile(MAP_DOWNLOAD_ITEM, listener, givenReaderFailingOn(new CountDownLatch(0)));

    download.startAsyncDownload();

    verify(listener, timeout(5000)).downloadFailed(MAP_DOWNLOAD_ITEM);
    assertThat(download.getDownloadState())
        .as("no longer running, so a new maps window does not show it downloading")
        .isEqualTo(DownloadState.FAILED);
    assertThat(download.isRunning()).isFalse();
  }

  @DisplayName("A cancelled download that breaks afterwards reports no failure")
  @Test
  void cancelledDownloadReportsNoFailure() throws InterruptedException {
    final DownloadListener listener = mock(DownloadListener.class);
    final CountDownLatch reading = new CountDownLatch(1);
    final DownloadFile download =
        new DownloadFile(
            MAP_DOWNLOAD_ITEM,
            listener,
            new ContentReader(
                uri -> {
                  reading.countDown();
                  try {
                    release.await();
                  } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                  throw new IllegalStateException("broken after the cancel");
                }));
    download.startAsyncDownload();
    // the cancel comes while the download reads, not before its thread began
    assertThat(reading.await(5, TimeUnit.SECONDS)).as("the download reads").isTrue();

    download.cancelDownload();
    release.countDown();

    // a failure now would reach a new download of the same map, eg one started again by Install
    verify(listener, after(500).never()).downloadFailed(MAP_DOWNLOAD_ITEM);
  }

  @DisplayName("A map that cannot be unpacked fails its download and tells its listener")
  @Test
  void unpackingFailureFailsDownload(@TempDir final Path maps) throws IOException {
    ClientSetting.mapFolderOverride.setValue(maps);
    final DownloadListener listener = mock(DownloadListener.class);
    final DownloadFile download = givenDownloadOf(givenZipWith("../outside.txt"), listener);

    download.startAsyncDownload();

    // the zip would write outside its folder; the map shows the failure instead of downloading
    verify(listener, timeout(5000)).downloadFailed(MAP_DOWNLOAD_ITEM);
    assertThat(download.getDownloadState()).isEqualTo(DownloadState.FAILED);
  }

  @DisplayName("A map whose unpacked files cannot be put in place is not reported installed")
  @Test
  void unpackingErrorFailsDownload(@TempDir final Path maps) throws IOException {
    ClientSetting.mapFolderOverride.setValue(maps);
    final DownloadListener listener = mock(DownloadListener.class);
    // the file "inner/inner" cannot move up into the map folder: its folder has that name
    final DownloadFile download = givenDownloadOf(givenZipWith("inner/inner"), listener);

    download.startAsyncDownload();

    verify(listener, timeout(5000)).downloadFailed(MAP_DOWNLOAD_ITEM);
    verify(listener, never()).downloadComplete(MAP_DOWNLOAD_ITEM);
    assertThat(download.getDownloadState())
        .as("the map offers Install again instead of showing a broken map as installed")
        .isEqualTo(DownloadState.FAILED);
  }

  private static DownloadFile givenDownloadOf(final byte[] zip, final DownloadListener listener) {
    return new DownloadFile(
        MAP_DOWNLOAD_ITEM,
        listener,
        new ContentReader(
            uri ->
                new CloseableDownloader() {
                  @Override
                  public InputStream getStream() {
                    return new ByteArrayInputStream(zip);
                  }

                  @Override
                  public void close() {
                    // nothing to close for bytes in memory
                  }
                }));
  }

  private static byte[] givenZipWith(final String entryName) throws IOException {
    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
      zip.putNextEntry(new ZipEntry(entryName));
      zip.write("text".getBytes(StandardCharsets.UTF_8));
      zip.closeEntry();
    }
    return bytes.toByteArray();
  }

  /** A reader that fails, eg as the server is unreachable, once the latch is counted down. */
  static ContentReader givenReaderFailingOn(final CountDownLatch latch) {
    return new ContentReader(
        uri -> {
          try {
            latch.await();
          } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          throw new IOException("server unreachable");
        });
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
}
