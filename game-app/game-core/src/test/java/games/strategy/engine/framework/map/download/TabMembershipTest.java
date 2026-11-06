package games.strategy.engine.framework.map.download;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.maps.listing.MapDownloadItem;

final class TabMembershipTest {
  @DisplayName("Maps stay in the tab they were in when the window opened, whatever happens to them")
  @Test
  void tabsFixedAtOpen() {
    final ManagedMap pacific = givenMap("Pacific");
    final ManagedMap europe = givenMap("Europe");
    europe.setMapStatus(ManagedMapStatus.INSTALLED);
    final List<ManagedMap> maps = List.of(pacific, europe);
    final TabMembership tabs =
        new TabMembership(maps, map -> map.getMapStatus() == ManagedMapStatus.INSTALLED);

    pacific.setMapStatus(ManagedMapStatus.INSTALLED);
    europe.setMapStatus(ManagedMapStatus.AVAILABLE);

    assertThat(List.of(tabs.installed(maps), tabs.toGet(maps)))
        .as("the newly installed map stays in Get more maps, the uninstalled one in Installed")
        .containsExactly(List.of(europe), List.of(pacific));
  }

  private static ManagedMap givenMap(final String name) {
    return new ManagedMap(
        MapDownloadItem.builder()
            .downloadUrl("")
            .previewImageUrl("")
            .mapName(name)
            .description("description")
            .lastCommitDateEpochMilli(60L)
            .downloadSizeInBytes(1024L)
            .build());
  }
}
