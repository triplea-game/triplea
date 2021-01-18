package games.strategy.engine.framework.map.download;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Strings;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;

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

  public static List<DownloadFileDescription> parse(final InputStream is) throws IOException {
    try {
      return parseImpl(is);
    } catch (final YamlEngineException e) {
      throw new IOException(e);
    }
  }

  private static List<DownloadFileDescription> parseImpl(final InputStream is) {
    final Load load = new Load(LoadSettings.builder().build());
    final List<?> yamlData = (List<?>) load.loadFromInputStream(is);

    final List<DownloadFileDescription> downloads = new ArrayList<>();
    yamlData.stream()
        .map(Map.class::cast)
        .forEach(
            yaml -> {
              final String url = (String) checkNotNull(yaml.get(Tags.url.toString()));
              final String description =
                  (String) checkNotNull(yaml.get(Tags.description.toString()));
              final String mapName = (String) checkNotNull(yaml.get(Tags.mapName.toString()));

              final Integer version = (Integer) yaml.get(Tags.version.toString());

              final DownloadFileDescription.MapCategory mapCategory =
                  optEnum(
                      yaml,
                      DownloadFileDescription.MapCategory.class,
                      Tags.mapCategory.toString(),
                      DownloadFileDescription.MapCategory.EXPERIMENTAL);

              final String img = Strings.nullToEmpty((String) yaml.get(Tags.img.toString()));
              final DownloadFileDescription dl =
                  new DownloadFileDescription(url, description, mapName, version, mapCategory, img);
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
