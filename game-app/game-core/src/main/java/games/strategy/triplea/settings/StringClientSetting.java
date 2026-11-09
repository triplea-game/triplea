package games.strategy.triplea.settings;

import java.util.function.Supplier;

final class StringClientSetting extends ClientSetting<String> {
  StringClientSetting(final String name) {
    super(String.class, name);
  }

  StringClientSetting(final String name, final String defaultValue) {
    super(String.class, name, defaultValue);
  }

  StringClientSetting(final String name, final Supplier<String> defaultValue) {
    super(String.class, name, defaultValue);
  }

  @Override
  protected String encodeValue(final String value) {
    return value;
  }

  @Override
  protected String decodeValue(final String encodedValue) {
    return encodedValue;
  }
}
