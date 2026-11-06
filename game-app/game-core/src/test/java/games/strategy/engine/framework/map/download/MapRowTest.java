package games.strategy.engine.framework.map.download;

import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.framework.map.download.MapPreviews.Caches;
import games.strategy.engine.framework.map.download.MapRow.Actions;
import games.strategy.engine.framework.map.download.MapRow.Listener;
import games.strategy.engine.framework.map.file.system.loader.InstalledMap;
import java.awt.Component;
import java.awt.Container;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import javax.swing.JButton;
import javax.swing.JLabel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.maps.listing.MapDownloadItem;
import org.triplea.http.client.lobby.maps.listing.MapTag;
import org.triplea.map.description.file.MapDescriptionYaml;
import org.triplea.map.description.file.MapDescriptionYaml.MapGame;

final class MapRowTest {
  private static final Actions NO_ACTIONS = new Actions(maps -> {}, map -> {}, map -> {});
  private static final Instant OLDER = Instant.parse("2024-03-03T10:00:00Z");
  private static final Instant NEWER = Instant.parse("2025-06-09T10:00:00Z");

  @DisplayName("An installed map with a newer version says when both versions are from")
  @Test
  void outdatedMapComparesVersions() {
    final ManagedMap map = new ManagedMap(givenDownload(NEWER), givenInstalledMap(OLDER));

    assertThat(MapRow.statusText(map))
        .as("the player sees that and why the map is old")
        .isEqualTo("Update available · yours from 3 Mar 2024 · newest from 9 Jun 2025");
  }

  @DisplayName("An installed map without a newer version is up to date")
  @Test
  void currentMapIsUpToDate() {
    final ManagedMap map = new ManagedMap(givenDownload(OLDER), givenInstalledMap(NEWER));

    assertThat(MapRow.statusText(map)).isEqualTo("Installed");
  }

  @DisplayName("A map to get shows its download size")
  @Test
  void availableMapShowsSize() {
    final ManagedMap map = new ManagedMap(givenDownload(NEWER));

    assertThat(MapRow.statusText(map)).as("2 MiB download").isEqualTo("2 MB");
  }

  @DisplayName("A removed map shows its size, not the version that was installed")
  @Test
  void removedMapShowsSize() {
    final ManagedMap map = new ManagedMap(givenDownload(NEWER), givenInstalledMap(OLDER));

    map.setMapStatus(ManagedMapStatus.AVAILABLE);

    assertThat(MapRow.statusText(map))
        .as("the map list keeps the installed map it read at the start")
        .isEqualTo("2 MB");
  }

  @DisplayName("A download shows how much of the map has arrived")
  @Test
  void progressInPercent() {
    assertThat(MapRow.progressText(givenDownload(NEWER), 1024L * 1024))
        .as("1 MiB of 2 MiB")
        .isEqualTo("Downloading · 50%");
  }

  @DisplayName("The line about a map is the first sentence of its description, as plain text")
  @Test
  void shortDescriptionIsFirstSentence() {
    final String html =
        "<html><style>p { color: red; }</style><h2>Global&nbsp;War</h2>"
            + "<p>Fight <b>World War II</b> on a global map. Six players.</p></html>";

    assertThat(MapRow.shortDescription(html))
        .as("tags, styles and entities gone, cut after the first sentence")
        .isEqualTo("Global War Fight World War II on a global map.");
  }

  @DisplayName("A long first sentence is cut at a word, with an ellipsis")
  @Test
  void longDescriptionIsCut() {
    final String html = "word ".repeat(40);

    final String about = MapRow.shortDescription(html);

    assertThat(about)
        .as("about 100 characters, so it fits one line of the row")
        .hasSizeLessThanOrEqualTo(101)
        .endsWith("word…");
  }

  @DisplayName("The creator is the name after the first 'by', up to a bracket or an ampersand")
  @Test
  void creatorAfterBy() {
    final String html =
        "<p>1888A Steam &amp; Steel by Ebbe (graphics, sound, testing) &amp; TheDog</p>";

    assertThat(MapRow.creatorOf(html))
        .as("the first name only; helpers in brackets and after '&' are left out")
        .contains("Ebbe");
  }

  @DisplayName("An 'Author:' line names the creator, even before a 'by'")
  @Test
  void creatorFromAuthorLine() {
    assertThat(MapRow.creatorOf("Inspired by Classic. <br>Author: Bung<br>Version 2"))
        .as("'Author:' wins over the first 'by'; the name ends with its line")
        .contains("Bung");
  }

  @DisplayName("A description without 'by' or an author names no creator")
  @Test
  void noCreator() {
    assertThat(MapRow.creatorOf("<b>Fight</b> World War II, disabled by default."))
        .as("'by default' is no name, as names are capitalized")
        .isEmpty();
  }

  @DisplayName("The placeholder the server sends for maps without one counts as no description")
  @Test
  void placeholderIsMissingDescription() {
    final String placeholder = "No description available for: https://github.com/triplea-maps/x.";

    assertThat(List.of(MapRow.isMissingDescription(placeholder), MapRow.isMissingDescription(" ")))
        .as("neither the placeholder nor a blank description shows a line")
        .containsExactly(true, true);
    assertThat(MapRow.creatorOf(placeholder)).as("its 'for:' names no one").isEmpty();
    assertThat(MapRow.isMissingDescription("<p>Six players.</p>")).isFalse();
  }

  @DisplayName("Every map offers Install or Uninstall; an outdated one also Update")
  @Test
  void buttonsPerStatus() {
    final ManagedMap toGet = new ManagedMap(givenDownload(NEWER));
    final ManagedMap installed = new ManagedMap(givenDownload(OLDER), givenInstalledMap(NEWER));
    final ManagedMap outdated = new ManagedMap(givenDownload(NEWER), givenInstalledMap(OLDER));
    final ManagedMap downloading = new ManagedMap(givenDownload(NEWER));
    downloading.setMapStatus(ManagedMapStatus.DOWNLOADING);

    assertThat(
            Stream.of(toGet, installed, outdated, downloading)
                .map(
                    map ->
                        MapRow.newButtons(map, NO_ACTIONS).stream().map(JButton::getText).toList())
                .toList())
        .as("the same buttons in the rows and the details")
        .containsExactly(
            List.of("Install"),
            List.of("Uninstall"),
            List.of("Uninstall", "Update"),
            List.of("Cancel"));
  }

  @DisplayName("A failed update keeps Uninstall, the map is still installed")
  @Test
  void failedUpdateCanStillBeUninstalled() {
    final ManagedMap failedUpdate = new ManagedMap(givenDownload(NEWER), givenInstalledMap(OLDER));
    failedUpdate.setMapStatus(ManagedMapStatus.ERROR);
    final ManagedMap failedInstall = new ManagedMap(givenDownload(NEWER));
    failedInstall.setMapStatus(ManagedMapStatus.ERROR);

    assertThat(
            Stream.of(failedUpdate, failedInstall)
                .map(
                    map ->
                        MapRow.newButtons(map, NO_ACTIONS).stream().map(JButton::getText).toList())
                .toList())
        .as("both can be tried again; only the installed one has something to uninstall")
        .containsExactly(List.of("Uninstall", "Install"), List.of("Install"));
  }

  @DisplayName("The views with pictures show each value of a tag as a chip, as written; LIST none")
  @Test
  void tagChipsInViewsWithPictures() {
    final String html = "<html><b>WW2";
    final ManagedMap map =
        new ManagedMap(
            MapDownloadItem.builder()
                .downloadUrl("")
                .previewImageUrl("")
                .mapName("World War II Global")
                .description("description")
                .lastCommitDateEpochMilli(NEWER.toEpochMilli())
                .downloadSizeInBytes(1024L)
                .mapTags(
                    List.of(
                        MapTag.builder().name("Genre").value("Fantasy").displayOrder(1).build(),
                        MapTag.builder().name("Genre").value(html).displayOrder(2).build()))
                .build());

    assertThat(labelsWithText(givenRow(map, MapBrowserView.LIST), "Fantasy"))
        .as("the compact list has no room for tags")
        .isEmpty();
    assertThat(Stream.of(MapBrowserView.LIST_WITH_PICTURES, MapBrowserView.TILES))
        .allSatisfy(
            view ->
                assertThat(Stream.of("Fantasy", html))
                    .as("two genres, two chips")
                    .allSatisfy(
                        value ->
                            assertThat(labelsWithText(givenRow(map, view), value))
                                .singleElement()
                                .satisfies(
                                    chip ->
                                        assertThat(chip.getClientProperty("html.disable"))
                                            .as("the server's text is shown as written")
                                            .isEqualTo(Boolean.TRUE))));
  }

  private static MapRow givenRow(final ManagedMap map, final MapBrowserView view) {
    return new MapRow(
        map, view, Caches.newCaches(), NO_ACTIONS, new Listener(row -> {}, row -> {}, row -> {}));
  }

  private static List<JLabel> labelsWithText(final Component component, final String text) {
    return labels(component).stream().filter(label -> text.equals(label.getText())).toList();
  }

  private static List<JLabel> labels(final Component component) {
    final Stream<JLabel> own =
        component instanceof JLabel label ? Stream.of(label) : Stream.empty();
    final Stream<JLabel> children =
        component instanceof Container container
            ? Arrays.stream(container.getComponents()).flatMap(c -> labels(c).stream())
            : Stream.empty();
    return Stream.concat(own, children).toList();
  }

  private static MapDownloadItem givenDownload(final Instant lastCommit) {
    return MapDownloadItem.builder()
        .downloadUrl("")
        .previewImageUrl("")
        .mapName("World War II Global")
        .description("description")
        .lastCommitDateEpochMilli(lastCommit.toEpochMilli())
        .downloadSizeInBytes(2L * 1024 * 1024)
        .build();
  }

  private static InstalledMap givenInstalledMap(final Instant lastModified) {
    return InstalledMap.builder()
        .lastModifiedDate(lastModified)
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
