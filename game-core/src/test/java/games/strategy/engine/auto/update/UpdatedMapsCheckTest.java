package games.strategy.engine.auto.update;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.util.Version;

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
  void shouldSkipCheckWhenNoAvailableDownloadsFound() {
    final Map<String, Version> availableMapVersions = Map.of();

    final Collection<String> result =
        UpdatedMapsCheck.computeOutOfDateMaps(
            Map.of("installed_map.properties", new Version(1, 0, 0)), availableMapVersions);

    assertThat(result, is(empty()));
  }

  @ParameterizedTest
  @MethodSource
  void localMapsNotOutOfDate(final Map<String, Version> localMaps) {
    final Map<String, Version> availableMaps = Map.of("map name", new Version(2, 0, 0));

    final Collection<String> result =
        UpdatedMapsCheck.computeOutOfDateMaps(localMaps, availableMaps);

    assertThat(result, is(empty()));
  }

  static List<Map<String, Version>> localMapsNotOutOfDate() {
    return List.of(
        // no local maps
        Map.of(),
        // local map is current
        Map.of("map_name.properties", new Version(2, 0, 0)),
        // local map is more recent
        Map.of("map_name.properties", new Version(3, 0, 0)));
  }

  @Test
  void localMapsOutOfDate() {
    final Map<String, Version> localOutOfDateMap =
        Map.of("map_name.zip.properties", new Version(1, 0, 0));
    final Map<String, Version> availableMaps = Map.of("Map Name", new Version(2, 0, 0));

    final Collection<String> result =
        UpdatedMapsCheck.computeOutOfDateMaps(localOutOfDateMap, availableMaps);

    assertThat("Version value of available map is greater than local map", result, hasSize(1));
    assertThat(result.iterator().next(), is("Map Name"));
  }
}
