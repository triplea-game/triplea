package games.strategy.engine.framework.map.download;

import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.framework.map.file.system.loader.InstalledMapsListing;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import java.util.List;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.maps.listing.MapDownloadItem;

class ManagedMapStoreTest extends AbstractClientSettingTestCase {
  private static final MapDownloadItem TEST_MAP =
      MapDownloadItem.builder()
          .downloadUrl("url")
          .previewImageUrl("preview-url")
          .description("description")
          .mapName("mapName")
          .lastCommitDateEpochMilli(60L)
          .downloadSizeInBytes(100L)
          .build();

  @Test
  void downloadFailedOffersMapForInstallAgain() throws Exception {
    final ManagedMapStore store = new ManagedMapStore();
    store.initialize(List.of(TEST_MAP), new InstalledMapsListing(List.of()));
    final ManagedMap managedMap = store.getMapByName(TEST_MAP.getMapName()).orElseThrow();
    store.updateStatus(List.of(managedMap), ManagedMapStatus.DOWNLOADING);

    store.downloadFailed(TEST_MAP);
    SwingUtilities.invokeAndWait(() -> {});

    assertThat(managedMap.getMapStatus()).isEqualTo(ManagedMapStatus.AVAILABLE);
    assertThat(store.getByStatus(ManagedMapStatus.AVAILABLE)).containsExactly(managedMap);
  }

  @Test
  void disposedStoreNoLongerListensToDownloadCoordinator() {
    final ManagedMapStore store = new ManagedMapStore();
    assertThat(DownloadCoordinator.instance.hasDownloadListener(store)).isTrue();

    store.dispose();

    assertThat(DownloadCoordinator.instance.hasDownloadListener(store)).isFalse();
  }
}
