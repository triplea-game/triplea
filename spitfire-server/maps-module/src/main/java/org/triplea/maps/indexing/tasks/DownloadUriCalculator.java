package org.triplea.maps.indexing.tasks;

import java.util.function.Function;
import org.triplea.http.client.github.MapRepoListing;

/** Given a map repo URI, determines the download URI for that map. */
public class DownloadUriCalculator implements Function<MapRepoListing, String> {

  @Override
  public String apply(final MapRepoListing mapRepoListing) {
    return mapRepoListing.getUri().toString() + "/archive/refs/heads/master.zip";
  }
}
