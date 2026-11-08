package games.strategy.engine.framework.map.download;

import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.framework.map.download.MapDetails.Fact;
import games.strategy.engine.framework.map.download.MapPreviews.Size;
import games.strategy.engine.framework.map.download.MapRow.Actions;
import games.strategy.engine.framework.map.file.system.loader.InstalledMap;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.HierarchyEvent;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.swing.JEditorPane;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.maps.listing.MapDownloadItem;
import org.triplea.http.client.lobby.maps.listing.MapTag;
import org.triplea.map.description.file.MapDescriptionYaml;
import org.triplea.map.description.file.MapDescriptionYaml.MapGame;

final class MapDetailsTest {
  private static final Instant OLDER = Instant.parse("2024-03-03T10:00:00Z");
  private static final Instant NEWER = Instant.parse("2025-06-09T10:00:00Z");

  @DisplayName("The details of an outdated map compare the installed and the newest version")
  @Test
  void outdatedMapComparesVersions() {
    final ManagedMap map = new ManagedMap(givenDownload(), givenInstalledMap());

    assertThat(MapDetails.facts(map))
        .as("status first, then both versions and the download size")
        .containsExactly(
            new Fact("Status", "Update available"),
            new Fact("Installed version", "3 Mar 2024"),
            new Fact("Newest version", "9 Jun 2025"),
            new Fact("Size", "2 MB"));
  }

  @DisplayName("The details of a map to get have no installed version")
  @Test
  void mapToGetHasNoInstalledVersion() {
    final ManagedMap map = new ManagedMap(givenDownload(), givenInstalledMap());
    map.setMapStatus(ManagedMapStatus.AVAILABLE);

    assertThat(MapDetails.facts(map))
        .as("a removed map keeps the installed map it was read with, which is gone now")
        .containsExactly(
            new Fact("Status", "Not installed"),
            new Fact("Newest version", "9 Jun 2025"),
            new Fact("Size", "2 MB"));
  }

  @DisplayName("The details end with the tags, the values of one tag on one line")
  @Test
  void tagsAreFacts() {
    final ManagedMap map =
        new ManagedMap(
            MapDownloadItem.builder()
                .downloadUrl("")
                .previewImageUrl("")
                .mapName("World War II Global")
                .description("description")
                .lastCommitDateEpochMilli(NEWER.toEpochMilli())
                .downloadSizeInBytes(2L * 1024 * 1024)
                .mapTags(
                    List.of(
                        MapTag.builder().name("Category").value("Pacific").displayOrder(3).build(),
                        MapTag.builder().name("Era").value("Modern").displayOrder(2).build(),
                        MapTag.builder().name("Category").value("WW2").displayOrder(1).build()))
                .build());

    assertThat(MapDetails.facts(map))
        .as("in display order, so 'WW2' before 'Pacific'")
        .endsWith(new Fact("Category", "WW2, Pacific"), new Fact("Era", "Modern"));
  }

  @DisplayName("Showing the same map again leaves its description in place, scrolled as it was")
  @Test
  void sameMapAgainKeepsDescription() {
    final MapDetails details = givenDetails();
    final ManagedMap global = new ManagedMap(givenDownload());
    details.show(global);
    final List<HierarchyEvent> moves = new ArrayList<>();
    descriptionIn(details).addHierarchyListener(moves::add);

    // eg after another map's download changed the list
    details.show(global);

    assertThat(moves).as("the description is not built anew, so it keeps its scrolling").isEmpty();
  }

  private static MapDetails givenDetails() {
    return new MapDetails(
        new MapPreviews(Size.DETAILS), new Actions(maps -> {}, map -> {}, map -> {}));
  }

  /** The description, which sits in the scrolling part of the details. */
  private static JEditorPane descriptionIn(final Component component) {
    return findDescription(component).orElseThrow();
  }

  private static Optional<JEditorPane> findDescription(final Component component) {
    if (component instanceof JEditorPane description) {
      return Optional.of(description);
    }
    return component instanceof Container container
        ? Arrays.stream(container.getComponents())
            .map(MapDetailsTest::findDescription)
            .flatMap(Optional::stream)
            .findFirst()
        : Optional.empty();
  }

  private static MapDownloadItem givenDownload() {
    return MapDownloadItem.builder()
        .downloadUrl("")
        .previewImageUrl("")
        .mapName("World War II Global")
        .description("description")
        .lastCommitDateEpochMilli(NEWER.toEpochMilli())
        .downloadSizeInBytes(2L * 1024 * 1024)
        .build();
  }

  private static InstalledMap givenInstalledMap() {
    return InstalledMap.builder()
        .lastModifiedDate(OLDER)
        .mapDescriptionYaml(
            MapDescriptionYaml.builder()
                .yamlFileLocation(Path.of("/local/file"))
                .mapName("World War II Global")
                .mapGameList(
                    List.of(MapGame.builder().xmlFileName("path.xml").gameName("game").build()))
                .build())
        .build();
  }
}
