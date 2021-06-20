package org.triplea.maps.indexing;

import java.time.Instant;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.triplea.http.client.github.MapRepoListing;

/**
 * Given a map repo name and URI, reads pertinent indexing information. Indexing will be skipped if
 * database is up to date and the repo has not been updated since.
 *
 * <ul>
 *   <li>mapName: read from map.yml found in the repository
 *   <li>lastCommitDate: github API is queried for the repo's master branch last commit date.
 *   <li>description: read from description.html file
 * </ul>
 */
@Builder
class MapIndexingTask implements Function<MapRepoListing, Optional<MapIndexingResult>> {
  @Nonnull private final Function<MapRepoListing, Optional<Instant>> lastCommitDateFetcher;
  @Nonnull private final BiPredicate<MapRepoListing, Instant> skipMapIndexingCheck;
  @Nonnull private final Function<MapRepoListing, Optional<String>> mapNameReader;
  @Nonnull private final Function<MapRepoListing, String> mapDescriptionReader;

  @Override
  public Optional<MapIndexingResult> apply(final MapRepoListing mapRepoListing) {
    final Instant lastCommitDateOnRepo = lastCommitDateFetcher.apply(mapRepoListing).orElse(null);
    if (lastCommitDateOnRepo == null) {
      return Optional.empty();
    }

    if (skipMapIndexingCheck.test(mapRepoListing, lastCommitDateOnRepo)) {
      return Optional.empty();
    }

    final String mapName = mapNameReader.apply(mapRepoListing).orElse(null);
    if (mapName == null) {
      return Optional.empty();
    }

    final String description = mapDescriptionReader.apply(mapRepoListing);

    return Optional.of(
        MapIndexingResult.builder()
            .mapName(mapName)
            .mapRepoUri(mapRepoListing.getUri().toString())
            .lastCommitDate(lastCommitDateOnRepo)
            .description(description)
            .build());
  }
}
