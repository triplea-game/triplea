package games.strategy.triplea.settings;

final class StringClientSetting extends ClientSetting<String> {
  StringClientSetting(final String name) {
    super(String.class, name);
  }

  StringClientSetting(final String name, final String defaultValue) {
    super(String.class, name, defaultValue);
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
