package games.strategy.engine.framework.map.download;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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

    final List<DownloadFileDescription> rVal = new ArrayList<>();
    for (int i = 0; i < yamlData.length(); i++) {
      final JSONObject yaml = yamlData.getJSONObject(i);
      final String url = yaml.getString(Tags.url.toString());
      final String description = yaml.getString(Tags.description.toString());
      final String mapName = yaml.getString(Tags.mapName.toString());

      final Object versionObject = yaml.opt(Tags.version.toString());
      final Version version = versionObject != null ? new Version(versionObject.toString()) : null;
      DownloadFileDescription.DownloadType downloadType = DownloadFileDescription.DownloadType.MAP;

      final String mapTypeString = yaml.optString(Tags.mapType.toString(), null);
      if (mapTypeString != null) {
        downloadType = DownloadFileDescription.DownloadType.valueOf(mapTypeString);
      }

      DownloadFileDescription.MapCategory mapCategory = DownloadFileDescription.MapCategory.EXPERIMENTAL;
      final String mapCategoryString = yaml.optString(Tags.mapCategory.toString(), null);
      if (mapCategoryString != null) {
        mapCategory = DownloadFileDescription.MapCategory.valueOf(mapCategoryString);
      }

      final String img = yaml.optString(Tags.img.toString(), "");
      final DownloadFileDescription dl =
          new DownloadFileDescription(url, description, mapName, version, downloadType, mapCategory, img);
      rVal.add(dl);
    }
    return rVal;
  }
}
