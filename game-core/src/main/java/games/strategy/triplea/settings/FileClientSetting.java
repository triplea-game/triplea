package games.strategy.triplea.settings;

import java.io.File;

final class FileClientSetting extends ClientSetting<File> {
  FileClientSetting(final String name) {
    super(File.class, name);
  }

  FileClientSetting(final String name, final File defaultValue) {
    super(File.class, name, defaultValue);
  }

  @Override
  protected String formatValue(final File value) {
    return value.getPath();
  }

  @Override
  protected File parseValue(final String encodedValue) {
    return new File(encodedValue);
  }
}
