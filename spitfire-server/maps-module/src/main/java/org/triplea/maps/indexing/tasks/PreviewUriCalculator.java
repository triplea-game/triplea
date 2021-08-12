package org.triplea.maps.indexing.tasks;

import java.util.function.Function;
import org.triplea.http.client.github.MapRepoListing;

public class PreviewUriCalculator implements Function<MapRepoListing, String> {
  @Override
  public String apply(final MapRepoListing mapRepoListing) {
    return mapRepoListing.getUri().toString() + "/blob/master/preview.png?raw=true";
  }
}
