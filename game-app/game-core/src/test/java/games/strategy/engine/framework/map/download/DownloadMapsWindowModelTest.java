package games.strategy.engine.framework.map.download;

import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.framework.map.file.system.loader.InstalledMap;
import games.strategy.engine.framework.map.file.system.loader.InstalledMapsListing;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import games.strategy.triplea.settings.ClientSetting;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.triplea.http.client.lobby.maps.listing.MapDownloadItem;
import org.triplea.map.description.file.MapDescriptionYaml;
import org.triplea.map.description.file.MapDescriptionYaml.MapGame;

final class DownloadMapsWindowModelTest extends AbstractClientSettingTestCase {
  // older than the newest version of the map, which the server dates 60 ms after 1970
  private static final Instant OLD_VERSION = Instant.EPOCH;

  @TempDir Path maps;

  @DisplayName("Uninstalling a map installed since the window opened deletes its folder")
  @Test
  void deletesMapInstalledAfterOpening() throws IOException {
    final Path pacific = givenMapFolder();
    final AtomicReference<InstalledMapsListing> onDisk =
        new AtomicReference<>(InstalledMapsListing.builder().build());
    final DownloadMapsWindowModel model =
        new DownloadMapsWindowModel(List.of(givenDownload()), onDisk::get);
    onDisk.set(givenListingWith(pacific));

    final boolean deleted = model.delete(model.getMapStore().getMapByName("Pacific").orElseThrow());

    assertThat(List.of(deleted, Files.exists(pacific)))
        .as("reported deleted and really gone, though the listing read at opening lacks it")
        .containsExactly(true, false);
  }

  @DisplayName("Cancelling the download of a new map offers it to install again")
  @Test
  void cancelledNewMapIsAvailable() {
    final DownloadMapsWindowModel model = givenModel(InstalledMapsListing.builder().build());
    final ManagedMap map = givenDownloading(model);

    final ManagedMapStatus status = model.statusWithoutDownload(map);

    assertThat(status).as("nothing of it is installed").isEqualTo(ManagedMapStatus.AVAILABLE);
  }

  @DisplayName("Cancelling the update of an out-of-date map leaves it installed and out of date")
  @Test
  void cancelledUpdateIsStillOutOfDate() throws IOException {
    final DownloadMapsWindowModel model = givenModel(givenListingWith(givenMapFolder()));
    final ManagedMap map = givenDownloading(model);

    final ManagedMapStatus status = model.statusWithoutDownload(map);

    assertThat(status)
        .as("the old version stays and offers Update again")
        .isEqualTo(ManagedMapStatus.UPDATE_AVAILABLE);
  }

  @DisplayName("A downloading map belongs to the Installed tab only if it updates an installed map")
  @Test
  void downloadingMapIsInstalledOnlyAsUpdate() throws IOException {
    final DownloadMapsWindowModel newMap = givenModel(InstalledMapsListing.builder().build());
    final DownloadMapsWindowModel update = givenModel(givenListingWith(givenMapFolder()));
    final ManagedMap newDownload = givenDownloading(newMap);
    final ManagedMap updateDownload = givenDownloading(update);

    final List<Boolean> installed =
        List.of(newMap.isInstalled(newDownload), update.isInstalled(updateDownload));

    assertThat(installed)
        .as("a new map is in Get more maps until it is done, an update in Installed")
        .containsExactly(false, true);
  }

  private Path givenMapFolder() throws IOException {
    ClientSetting.mapFolderOverride.setValue(maps);
    final Path pacific = Files.createDirectories(maps.resolve("Pacific"));
    Files.createFile(pacific.resolve("polygons.txt"));
    return pacific;
  }

  private static DownloadMapsWindowModel givenModel(final InstalledMapsListing installedMaps) {
    return new DownloadMapsWindowModel(List.of(givenDownload()), () -> installedMaps);
  }

  private static ManagedMap givenDownloading(final DownloadMapsWindowModel model) {
    final ManagedMap map = model.getMapStore().getMapByName("Pacific").orElseThrow();
    model.getMapStore().updateStatus(List.of(map), ManagedMapStatus.DOWNLOADING);
    return map;
  }

  private static MapDownloadItem givenDownload() {
    return MapDownloadItem.builder()
        .downloadUrl("url")
        .previewImageUrl("")
        .mapName("Pacific")
        .description("description")
        .lastCommitDateEpochMilli(60L)
        .downloadSizeInBytes(100L)
        .build();
  }

  private static InstalledMapsListing givenListingWith(final Path folder) {
    return InstalledMapsListing.builder()
        .installedMap(
            InstalledMap.builder()
                .lastModifiedDate(OLD_VERSION)
                .mapDescriptionYaml(
                    MapDescriptionYaml.builder()
                        .yamlFileLocation(folder.resolve("map.yml"))
                        .mapName("Pacific")
                        .mapGameList(
                            List.of(
                                MapGame.builder().xmlFileName("path.xml").gameName("game").build()))
                        .build())
                .build())
        .build();
  }
}
