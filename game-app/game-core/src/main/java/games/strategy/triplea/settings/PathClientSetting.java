package games.strategy.triplea.settings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

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

  @Override
  public Optional<String> validateValue(final Path value) {
    if (!Files.exists(value) || !Files.isWritable(value)) {
      return Optional.of(
          "Invalid path, does not exist or cannot be written (permissions): "
              + value.toAbsolutePath());
    } else {
      return Optional.empty();
    }
  }
}
