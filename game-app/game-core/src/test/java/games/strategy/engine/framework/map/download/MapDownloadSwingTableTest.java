package games.strategy.engine.framework.map.download;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.maps.listing.MapDownloadItem;

class MapDownloadSwingTableTest {

  @Test
  void getSwingComponentWithEmptyContent() {
    final var table = new MapDownloadSwingTable(List.of()).getSwingComponent();

    assertThat(table.getRowCount()).isEqualTo(0);
  }

  @Test
  void getSwingComponentWithContentSelectsFirstRow() {
    final var mapDownloadItem =
        MapDownloadItem.builder()
            .downloadUrl("url")
            .previewImageUrl("preview-url")
            .description("description")
            .mapName("mapName")
            .lastCommitDateEpochMilli(60L)
            .downloadSizeInBytes(100L)
            .build();

    final var table =
        new MapDownloadSwingTable(List.of(new ManagedMap(mapDownloadItem))).getSwingComponent();

    assertThat(table.getSelectedRow()).isEqualTo(0);
  }
}
