package games.strategy.triplea.settings;

import java.nio.file.Path;
import java.nio.file.Paths;

final class PathClientSetting extends ClientSetting<Path> {
  PathClientSetting(final String name) {
    super(Path.class, name);
  }

  PathClientSetting(final String name, final Path defaultValue) {
    super(Path.class, name, defaultValue);
  }

  @Override
  protected String encodeValue(final Path value) {
    return value.toString();
  }

  @Override
  protected Path decodeValue(final String encodedValue) {
    return Paths.get(encodedValue);
  }
}
