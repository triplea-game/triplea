package games.strategy.engine.framework.map.download;

import static games.strategy.engine.framework.map.download.DownloadFileTest.givenReaderFailingOn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import games.strategy.engine.framework.map.download.DownloadFile.DownloadState;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.maps.listing.MapDownloadItem;

final class DownloadCoordinatorTest {
  // lets the downloads that wait for it fail, so their threads end with the test
  private final CountDownLatch release = new CountDownLatch(1);

  @AfterEach
  void releaseDownloads() {
    release.countDown();
  }

  @DisplayName("Cancelling a download ends only that one; the others go on")
  @Test
  void cancelEndsOneDownload() {
    final DownloadCoordinator coordinator = new DownloadCoordinator(givenReaderFailingOn(release));
    final MapDownloadItem pacific = givenDownload("Pacific");
    coordinator.accept(pacific);
    coordinator.accept(givenDownload("Europe"));

    coordinator.cancel(pacific);

    assertThat(statesByMapName(coordinator))
        .as("the cancelled map stopped, the other map still downloads")
        .containsExactlyInAnyOrderEntriesOf(
            Map.of("Pacific", DownloadState.CANCELLED, "Europe", DownloadState.DOWNLOADING));
  }

  @DisplayName("A cancelled download can be started again")
  @Test
  void cancelledDownloadCanStartAgain() {
    final DownloadCoordinator coordinator = new DownloadCoordinator(givenReaderFailingOn(release));
    final MapDownloadItem pacific = givenDownload("Pacific");
    coordinator.accept(pacific);
    coordinator.cancel(pacific);
    coordinator.getDownloads();

    coordinator.accept(pacific);

    assertThat(statesByMapName(coordinator))
        .as("Install after Cancel downloads the map anew")
        .containsExactlyEntriesOf(Map.of("Pacific", DownloadState.DOWNLOADING));
  }

  @DisplayName("A failed download leaves the running downloads")
  @Test
  void failedDownloadEnds() {
    final DownloadCoordinator coordinator =
        new DownloadCoordinator(givenReaderFailingOn(new CountDownLatch(0)));
    coordinator.accept(givenDownload("Pacific"));

    await()
        .atMost(Duration.ofSeconds(5))
        .alias("the download thread fails at once")
        .until(() -> coordinator.getDownloads().stream().noneMatch(DownloadFile::isRunning));
  }

  @DisplayName("A listener that fails does not keep the others from the news")
  @Test
  void failingListenerKeepsNoOneFromTheNews() {
    final DownloadCoordinator coordinator =
        new DownloadCoordinator(givenReaderFailingOn(new CountDownLatch(0)));
    final DownloadListener failing = mock(DownloadListener.class);
    doThrow(new IllegalStateException("not my map")).when(failing).downloadFailed(any());
    final DownloadListener next = mock(DownloadListener.class);
    coordinator.addDownloadListener(failing);
    coordinator.addDownloadListener(next);

    coordinator.accept(givenDownload("Pacific"));

    // eg the open maps window after the store of an earlier one: it still shows the failure
    verify(next, timeout(5000))
        .downloadFailed(argThat(download -> "Pacific".equals(download.getMapName())));
  }

  @DisplayName("A map is downloaded once, also when a listing read again describes it otherwise")
  @Test
  void mapIsDownloadedOnce() {
    final DownloadCoordinator coordinator = new DownloadCoordinator(givenReaderFailingOn(release));
    coordinator.accept(givenDownload("Pacific"));
    final MapDownloadItem relisted =
        MapDownloadItem.builder()
            .downloadUrl("url")
            .previewImageUrl("")
            .mapName("Pacific")
            .description("described anew")
            .lastCommitDateEpochMilli(61L)
            .downloadSizeInBytes(100L)
            .build();

    coordinator.accept(relisted);
    coordinator.cancel(relisted);

    assertThat(statesByMapName(coordinator))
        .as("one download into the map's one folder; cancelled by either listing's item")
        .containsExactlyEntriesOf(Map.of("Pacific", DownloadState.CANCELLED));
  }

  private static Map<String, DownloadState> statesByMapName(final DownloadCoordinator coordinator) {
    return coordinator.getDownloads().stream()
        .collect(
            Collectors.toMap(
                download -> download.getDownload().getMapName(), DownloadFile::getDownloadState));
  }

  private static MapDownloadItem givenDownload(final String mapName) {
    return MapDownloadItem.builder()
        .downloadUrl("url")
        .previewImageUrl("")
        .mapName(mapName)
        .description("description")
        .lastCommitDateEpochMilli(60L)
        .downloadSizeInBytes(100L)
        .build();
  }
}
