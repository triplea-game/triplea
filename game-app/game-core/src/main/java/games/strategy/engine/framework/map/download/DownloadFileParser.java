package games.strategy.engine.framework.map.download;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Strings;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;
import org.triplea.http.client.maps.listing.MapDownloadListing;
import org.triplea.yaml.YamlReader;
import org.triplea.yaml.YamlReader.InvalidYamlFormatException;

/**
 * Utility class to parse an available map list file config file - used to determine which maps are
 * available for download.
 */
@UtilityClass
final class DownloadFileParser {

  enum Tags {
    url,
    version,
    mapName,
    description,
    mapCategory,
    img
  }

  public static List<MapDownloadListing> parse(final InputStream is) throws IOException {
    try {
      return parseImpl(is);
    } catch (final InvalidYamlFormatException e) {
      throw new IOException(e);
    }
  }

  private static List<MapDownloadListing> parseImpl(final InputStream is) {
    final List<Map<String, Object>> yamlData = YamlReader.readList(is);

    final List<MapDownloadListing> downloads = new ArrayList<>();
    yamlData.stream()
        .map(Map.class::cast)
        .forEach(
            yaml -> {
              final String url = (String) checkNotNull(yaml.get(Tags.url.toString()));
              final String description =
                  (String) checkNotNull(yaml.get(Tags.description.toString()));
              final String mapName = (String) checkNotNull(yaml.get(Tags.mapName.toString()));

              final Integer version = (Integer) yaml.get(Tags.version.toString());

              final String mapCategory =
                  (String) yaml.getOrDefault(Tags.mapCategory.toString(), "EXPERIMENTAL");

              final String img = Strings.nullToEmpty((String) yaml.get(Tags.img.toString()));
              downloads.add(
                  MapDownloadListing.builder()
                      .downloadUrl(url)
                      .description(description)
                      .mapName(mapName)
                      .version(version)
                      .mapCategory(mapCategory)
                      .previewImageUrl(img)
                      .build());
            });
    return downloads;
  }
}
