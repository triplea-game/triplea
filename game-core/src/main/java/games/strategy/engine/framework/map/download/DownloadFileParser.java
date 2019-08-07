package games.strategy.engine.framework.map.download;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.snakeyaml.engine.v1.api.Load;
import org.snakeyaml.engine.v1.api.LoadSettingsBuilder;
import org.snakeyaml.engine.v1.exceptions.YamlEngineException;
import org.triplea.util.Version;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * Utility class to parse an available map list file config file - used to determine which maps are available for
 * download.
 */
final class DownloadFileParser {

  private DownloadFileParser() {}

  enum Tags {
    url, mapType, version, mapName, description, mapCategory, img
  }

  public static List<DownloadFileDescription> parse(final InputStream is) throws IOException {
    try {
      return parseImpl(is);
    } catch (final YamlEngineException e) {
      throw new IOException(e);
    }
  }

  private static List<DownloadFileDescription> parseImpl(final InputStream is) throws IOException {
    final Load load = new Load(new LoadSettingsBuilder().build());
    final List<?> yamlData = (List<?>) load.loadFromInputStream(is);

    final List<DownloadFileDescription> downloads = new ArrayList<>();
    yamlData.stream().map(Map.class::cast).forEach(yaml -> {
      final String url = (String) Preconditions.checkNotNull(yaml.get(Tags.url.toString()));
      final String description = (String) Preconditions.checkNotNull(yaml.get(Tags.description.toString()));
      final String mapName = (String) Preconditions.checkNotNull(yaml.get(Tags.mapName.toString()));

      final Version version = new Version(Preconditions.checkNotNull((Integer) yaml.get(Tags.version.toString())), 0);
      final DownloadFileDescription.DownloadType downloadType = optEnum(
          yaml,
          DownloadFileDescription.DownloadType.class,
          Tags.mapType.toString(),
          DownloadFileDescription.DownloadType.MAP);

      final DownloadFileDescription.MapCategory mapCategory = optEnum(
          yaml,
          DownloadFileDescription.MapCategory.class,
          Tags.mapCategory.toString(),
          DownloadFileDescription.MapCategory.EXPERIMENTAL);

      final String img = Strings.nullToEmpty((String) yaml.get(Tags.img.toString()));
      final DownloadFileDescription dl =
          new DownloadFileDescription(url, description, mapName, version, downloadType, mapCategory, img);
      downloads.add(dl);
    });
    return downloads;
  }

  private static <T extends Enum<T>> T optEnum(
      final Map<?, ?> jsonObject,
      final Class<T> type,
      final String name,
      final T defaultValue) {
    checkNotNull(jsonObject);
    checkNotNull(type);
    checkNotNull(name);
    checkNotNull(defaultValue);

    final String valueName = Strings.nullToEmpty((String) jsonObject.get(name));
    return valueName.isEmpty() ? defaultValue : Enum.valueOf(type, valueName);
  }
}
