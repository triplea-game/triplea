package org.triplea.maps.indexing.tasks;

import com.google.common.annotations.VisibleForTesting;
import java.time.Instant;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.triplea.http.client.github.MapRepoListing;
import org.triplea.maps.indexing.MapIndexDao;

/**
 * Given a repo and the last commit date of that repo, this predicate checks if we should skip
 * indexing of that repo. We should skip indexing if the last commit date of the repo is more recent
 * then what we have stored in database.
 */
@Slf4j
@AllArgsConstructor(onConstructor_ = @VisibleForTesting)
public class SkipMapIndexingCheck implements BiPredicate<MapRepoListing, Instant> {
  @Nonnull private final Function<MapRepoListing, Optional<Instant>> databaseLastCommitDateLookup;

  public SkipMapIndexingCheck(final MapIndexDao mapIndexDao) {
    databaseLastCommitDateLookup =
        mapRepoListing -> mapIndexDao.getLastCommitDate(mapRepoListing.getUri().toString());
  }

  @Override
  public boolean test(final MapRepoListing mapRepoListing, final Instant lastCommitDateOnRepo) {
    final Instant lastCommitDateInDatabase =
        databaseLastCommitDateLookup.apply(mapRepoListing).orElse(null);

    if (lastCommitDateInDatabase != null
        && !lastCommitDateInDatabase.isBefore(lastCommitDateOnRepo)) {
      log.info(
          "Skipping, map indexing is up to date for: {}, "
              + "last commit date on repo: {}, "
              + "last commit date in database: {}",
          mapRepoListing.getUri(),
          lastCommitDateInDatabase,
          lastCommitDateInDatabase);
      return true;
    } else {
      return false;
    }
  }
}
