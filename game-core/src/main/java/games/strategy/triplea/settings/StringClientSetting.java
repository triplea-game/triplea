package games.strategy.triplea.settings;

import java.io.File;

final class StringClientSetting extends ClientSetting<String> {
  StringClientSetting(final String name) {
    super(String.class, name);
  }

  StringClientSetting(final String name, final String defaultValue) {
    super(String.class, name, defaultValue);
  }

  StringClientSetting(final String name, final File defaultValue) {
    super(String.class, name, defaultValue.getAbsolutePath());
  }

  @Override
  protected String formatValue(final String value) {
    return value;
  }

  @Override
  protected String parseValue(final String encodedValue) {
    return encodedValue;
  }
}
