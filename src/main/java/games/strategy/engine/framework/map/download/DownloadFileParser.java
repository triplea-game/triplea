package games.strategy.engine.framework.map.download;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

import org.json.JSONArray;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import games.strategy.util.Version;

/**
 * Utility class to parse an available map list file config file - used to determine which maps are available for
 * download.
 */
final class DownloadFileParser {

  private DownloadFileParser() {}

  enum Tags {
    url, mapType, version, mapName, description, mapCategory, img
  }

  enum ValueType {
    MAP, MAP_TOOL, MAP_SKIN, MAP_MOD
  }

  public static List<DownloadFileDescription> parse(final InputStream is) {
    final JSONArray yamlData = new JSONArray(new Yaml().loadAs(is, List.class));

    final List<DownloadFileDescription> downloads = new ArrayList<>();
    StreamSupport.stream(yamlData.spliterator(), false).map(JSONObject.class::cast).forEach(yaml -> {
      final String url = yaml.getString(Tags.url.toString());
      final String description = yaml.getString(Tags.description.toString());
      final String mapName = yaml.getString(Tags.mapName.toString());

      final Version version = new Version(yaml.getInt(Tags.version.toString()), 0);
      final DownloadFileDescription.DownloadType downloadType = yaml.optEnum(
          DownloadFileDescription.DownloadType.class,
          Tags.mapType.toString(),
          DownloadFileDescription.DownloadType.MAP);

      final DownloadFileDescription.MapCategory mapCategory = yaml.optEnum(
          DownloadFileDescription.MapCategory.class,
          Tags.mapCategory.toString(),
          DownloadFileDescription.MapCategory.EXPERIMENTAL);

      final String img = yaml.optString(Tags.img.toString());
      final DownloadFileDescription dl =
          new DownloadFileDescription(url, description, mapName, version, downloadType, mapCategory, img);
      downloads.add(dl);
    });
    return downloads;
  }
}
