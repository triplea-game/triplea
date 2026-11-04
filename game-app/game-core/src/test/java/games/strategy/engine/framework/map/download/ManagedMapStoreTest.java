package games.strategy.engine.framework.map.download;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import games.strategy.engine.framework.map.file.system.loader.InstalledMapsListing;
import java.util.List;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.maps.listing.MapDownloadItem;

final class ManagedMapStoreTest {
  @DisplayName("A failed download marks its map failed, which offers to install it again")
  @Test
  void failedDownloadMarksMapFailed() throws Exception {
    final MapDownloadItem download = givenDownload("Pacific");
    final ManagedMapStore store = givenStoreWith(download);
    store.updateStatus(
        List.of(store.getMapByMapDownloadItem(download)), ManagedMapStatus.DOWNLOADING);

    store.downloadFailed(download);
    // the status changes on the Swing thread
    SwingUtilities.invokeAndWait(() -> {});

    assertThat(store.getMapByMapDownloadItem(download).getMapStatus())
        .as("no longer downloading, so the row shows 'Download failed' and Install")
        .isEqualTo(ManagedMapStatus.ERROR);
  }

  @DisplayName("A download of a map the store does not hold is none of its business")
  @Test
  void downloadOfMapNotHeldIsIgnored() throws Exception {
    final MapDownloadItem download = givenDownload("Pacific");
    final ManagedMapStore store = givenStoreWith(download);
    final MapDownloadItem other = givenDownload("Europe");

    assertThatCode(() -> store.downloadComplete(other))
        .as("eg started in an earlier window while this store's listing could not be read")
        .doesNotThrowAnyException();
    SwingUtilities.invokeAndWait(() -> {});
    assertThat(store.getMapByMapDownloadItem(download).getMapStatus())
        .as("the store's own map is untouched")
        .isEqualTo(ManagedMapStatus.AVAILABLE);
  }

  private static ManagedMapStore givenStoreWith(final MapDownloadItem download) {
    final ManagedMapStore store = new ManagedMapStore();
    store.initialize(List.of(download), InstalledMapsListing.builder().build());
    return store;
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
