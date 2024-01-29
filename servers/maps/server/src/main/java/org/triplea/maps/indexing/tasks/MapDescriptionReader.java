package org.triplea.maps.indexing.tasks;

import java.net.URI;
import java.util.Optional;
import java.util.function.Function;
import org.triplea.http.client.github.MapRepoListing;
import org.triplea.io.ContentDownloader;

/**
 * A function where if given a map repo listing will find the 'description.html' file in that repo
 * and returns its contents. If the contents are too long or the file is missing then will return a
 * 'description-missing' error message with details on how to fix it.
 */
public class MapDescriptionReader implements Function<MapRepoListing, String> {
  private static final int DESCRIPTION_COLUMN_DATABASE_MAX_LENGTH = 3000;

  @Override
  public String apply(final MapRepoListing mapRepoListing) {
    final String description = downloadDescription(mapRepoListing).orElse(null);
    if (description == null) {
      return String.format(
          "No description available for: %s. "
              + "Contact the map author and request they add a 'description.html' file",
          mapRepoListing.getUri());
    } else if (description.length() > DESCRIPTION_COLUMN_DATABASE_MAX_LENGTH) {
      return String.format(
          "The description for this map is too long at %s characters. Max length is %s. "
              + "Contact the map author for: %s"
              + ", and request they reduce the length of the file 'description.html'",
          description.length(), DESCRIPTION_COLUMN_DATABASE_MAX_LENGTH, mapRepoListing.getUri());
    } else {
      return description;
    }
  }

  private Optional<String> downloadDescription(final MapRepoListing mapRepoListing) {
    final String descriptionUri =
        mapRepoListing.getUri().toString() + "/blob/master/description.html?raw=true";
    return ContentDownloader.downloadAsString(URI.create(descriptionUri));
  }
}
