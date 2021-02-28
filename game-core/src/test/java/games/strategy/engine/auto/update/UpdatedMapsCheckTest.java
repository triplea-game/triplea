package games.strategy.engine.auto.update;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.mockito.Mockito.verify;

import games.strategy.engine.framework.map.download.DownloadFileDescription;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("unused")
@ExtendWith(MockitoExtension.class)
final class UpdatedMapsCheckTest {
  private static final Instant NOW = Instant.now();

  @Mock private Runnable lastCheckSetter;

  @ParameterizedTest
  @MethodSource
  @DisplayName(
      "If last update check is epoch start or beyond last check threshold, then "
          + "we expect a map update check to be needed.")
  void mapUpdateCheckNeeded(final long lastCheckEpochMilli) {
    final boolean result =
        UpdatedMapsCheck.isMapUpdateCheckRequired(lastCheckEpochMilli, lastCheckSetter);

    assertThat(result, is(true));

    verify(lastCheckSetter).run();
  }

  static List<Long> mapUpdateCheckNeeded() {
    return List.of(
        0L, NOW.minus(UpdatedMapsCheck.THRESHOLD_DAYS + 1, ChronoUnit.DAYS).toEpochMilli());
  }

  @ParameterizedTest
  @MethodSource
  @DisplayName(
      "If last update check is in future or is before the last update check threshold,"
          + "then we do not need a map update check")
  void updateCheckNotNeeded(final long lastCheckTime) {
    final boolean result =
        UpdatedMapsCheck.isMapUpdateCheckRequired(lastCheckTime, lastCheckSetter);

    assertThat(result, is(false));

    verify(lastCheckSetter).run();
  }

  static List<Long> updateCheckNotNeeded() {
    // no need to check when:
    return List.of(
        // last check time is now
        NOW.toEpochMilli(),
        // last check is in future
        NOW.plus(1, ChronoUnit.DAYS).toEpochMilli(),
        // last check is one day short of the threshold
        NOW.minus(UpdatedMapsCheck.THRESHOLD_DAYS - 1, ChronoUnit.DAYS).toEpochMilli(),
        // last check is within a minute of the threshold but not beyond
        NOW.minus(UpdatedMapsCheck.THRESHOLD_DAYS, ChronoUnit.DAYS).plusSeconds(60).toEpochMilli());
  }

  @Test
  void localMapIsCurrent() {
    final var availableMaps = List.of(buildDownloadDescription("map name", 2));
    final Function<String, Integer> mapVersionLookupFunction = anyMapName -> 2;

    final Collection<String> result =
        UpdatedMapsCheck.computeOutOfDateMaps(availableMaps, mapVersionLookupFunction);

    assertThat(result, is(empty()));
  }

  @SuppressWarnings("SameParameterValue")
  private static DownloadFileDescription buildDownloadDescription(
      final String mapName, final int version) {
    return DownloadFileDescription.builder().mapName(mapName).version(version).build();
  }

  @Test
  void localMapIsAhead() {
    final var availableMaps = List.of(buildDownloadDescription("map name", 2));
    final Function<String, Integer> mapVersionLookupFunction = anyMapName -> 3;

    final Collection<String> result =
        UpdatedMapsCheck.computeOutOfDateMaps(availableMaps, mapVersionLookupFunction);

    assertThat(result, is(empty()));
  }

  @Test
  void localMapIsOutOfDate() {
    final var availableMaps =
        List.of(
            buildDownloadDescription("isCurrent", 2),
            buildDownloadDescription("new name version", 2));

    // if version function receives 'isCurrent', we'll return the current version of '2',
    // otherwise we'll return '1' which is less than the available current version.
    final Function<String, Integer> mapVersionLookupFunction =
        mapName -> mapName.equals("isCurrent") ? 2 : 1;

    final Collection<String> result =
        UpdatedMapsCheck.computeOutOfDateMaps(availableMaps, mapVersionLookupFunction);

    assertThat(
        "we stubbed an old version for the 'new map version', we expect it to"
            + "be on the return list of out of date maps",
        result,
        hasItem("new name version"));
  }
}
