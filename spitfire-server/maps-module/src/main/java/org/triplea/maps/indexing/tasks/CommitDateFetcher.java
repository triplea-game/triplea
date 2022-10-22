package org.triplea.maps.indexing.tasks;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.triplea.http.client.github.GithubApiClient;
import org.triplea.http.client.github.MapRepoListing;

/**
 * Given a map repo listing, does an API call to github to return the last commit date for that
 * repo. Returns an empty if there are any errors fetching last commit date.
 */
@Builder
@Slf4j
public class CommitDateFetcher implements Function<MapRepoListing, Optional<Instant>> {

  @Nonnull private final GithubApiClient githubApiClient;

  @Override
  public Optional<Instant> apply(final MapRepoListing mapRepoListing) {
    try {
      return Optional.of(
          githubApiClient.fetchBranchInfo(mapRepoListing.getName(), "master").getLastCommitDate());
    } catch (final Exception e) {
      log.error(
          "Could not index map: {}, unable to fetch last commit date. Either the last commit"
              + "date was missing from the webservice call payload from github, or most likely"
              + "the webservice call to github failed.",
          mapRepoListing.getUri(),
          e);
      return Optional.empty();
    }
  }
}
