package games.strategy.engine.framework.map.download;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import games.strategy.util.Version;

/**
 * Utility class to parse an available map list file config file - used to determine which maps are available for
 * download
 */
public final class DownloadFileParser {

  private DownloadFileParser() {}

  public static enum Tags {
    url, mapType, version, mapName, description
  }

  public static enum ValueType {
    MAP, MAP_TOOL, MAP_SKIN, MAP_MOD
  }

  public static List<DownloadFileDescription> parse(final InputStream is) {
    final List<Map<String, Object>> yamlData = (List<Map<String, Object>>) (new Yaml()).load(is);

    final List<DownloadFileDescription> rVal = new ArrayList<DownloadFileDescription>();
    for (final Map<String, Object> yaml : yamlData) {
      final String url = (String) yaml.get(Tags.url.toString());
      final String description = (String) yaml.get(Tags.description.toString());
      final String mapName = (String) yaml.get(Tags.mapName.toString());

      Version version = null;
      final Object versionObj = yaml.get(Tags.version.toString());
      if (versionObj != null) {
        final String versionString = String.valueOf(versionObj);
        version = new Version(versionString);
      }

      DownloadFileDescription.DownloadType downloadType = DownloadFileDescription.DownloadType.MAP;

      final String mapTypeString = (String) yaml.get(Tags.mapType.toString());
      if (mapTypeString != null) {
        downloadType = DownloadFileDescription.DownloadType.valueOf(mapTypeString);
      }

      final DownloadFileDescription dl = new DownloadFileDescription(url, description, mapName, version, downloadType);
      rVal.add(dl);
    }
    return rVal;
  }
}
