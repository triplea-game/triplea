package games.strategy.engine.framework.map.download;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.yaml.snakeyaml.Yaml;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

import games.strategy.util.OpenJsonUtils;
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

  public static List<DownloadFileDescription> parse(final InputStream is) {
    final JSONArray yamlData = new JSONArray(new Yaml().loadAs(is, List.class));

    final List<DownloadFileDescription> downloads = new ArrayList<>();
    OpenJsonUtils.stream(yamlData).map(JSONObject.class::cast).forEach(yaml -> {
      final String url = yaml.getString(Tags.url.toString());
      final String description = yaml.getString(Tags.description.toString());
      final String mapName = yaml.getString(Tags.mapName.toString());

      final Version version = new Version(yaml.getInt(Tags.version.toString()), 0);
      final DownloadFileDescription.DownloadType downloadType = OpenJsonUtils.optEnum(
          yaml,
          DownloadFileDescription.DownloadType.class,
          Tags.mapType.toString(),
          DownloadFileDescription.DownloadType.MAP);

      final DownloadFileDescription.MapCategory mapCategory = OpenJsonUtils.optEnum(
          yaml,
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
