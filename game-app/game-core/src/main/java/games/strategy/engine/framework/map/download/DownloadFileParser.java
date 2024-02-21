package games.strategy.engine.framework.map.download;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Strings;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;
import org.triplea.util.Version;
import org.triplea.yaml.YamlReader;

/**
 * Utility class to parse an available map list file config file - used to determine which maps are
 * available for download.
 */
@UtilityClass
public final class DownloadFileParser {

  enum Tags {
    url,
    mapType,
    version,
    mapName,
    description,
    mapCategory,
    img
  }

  public static List<DownloadFileDescription> parse(final InputStream is) {
    final List<?> yamlData = (List<?>) YamlReader.readList(is);

    final List<DownloadFileDescription> downloads = new ArrayList<>();
    yamlData.stream()
        .map(Map.class::cast)
        .forEach(
            yaml -> {
              final String url = (String) checkNotNull(yaml.get(Tags.url.toString()));
              final String description =
                  (String) checkNotNull(yaml.get(Tags.description.toString()));
              final String mapName = (String) checkNotNull(yaml.get(Tags.mapName.toString()));

              final Version version =
                  new Version(checkNotNull((Integer) yaml.get(Tags.version.toString())).toString());
              final DownloadFileDescription.DownloadType downloadType =
                  optEnum(
                      yaml,
                      DownloadFileDescription.DownloadType.class,
                      Tags.mapType.toString(),
                      DownloadFileDescription.DownloadType.MAP);

              final DownloadFileDescription.MapCategory mapCategory =
                  optEnum(
                      yaml,
                      DownloadFileDescription.MapCategory.class,
                      Tags.mapCategory.toString(),
                      DownloadFileDescription.MapCategory.EXPERIMENTAL);

              final String img = Strings.nullToEmpty((String) yaml.get(Tags.img.toString()));
              final DownloadFileDescription dl =
                  new DownloadFileDescription(
                      url, description, mapName, version, downloadType, mapCategory, img);
              downloads.add(dl);
            });
    return downloads;
  }

  private static <T extends Enum<T>> T optEnum(
      final Map<?, ?> jsonObject, final Class<T> type, final String name, final T defaultValue) {
    checkNotNull(jsonObject);
    checkNotNull(type);
    checkNotNull(name);
    checkNotNull(defaultValue);

    final String valueName = Strings.nullToEmpty((String) jsonObject.get(name));
    return valueName.isEmpty() ? defaultValue : Enum.valueOf(type, valueName);
  }
}
