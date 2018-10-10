package games.strategy.triplea.settings;

final class BooleanClientSetting extends ClientSetting<Boolean> {
  BooleanClientSetting(final String name, final boolean defaultValue) {
    super(name, defaultValue);
  }

  @Override
  protected String formatValue(final Boolean value) {
    return value.toString();
  }

  @Override
  protected Boolean parseValue(final String encodedValue) {
    return Boolean.valueOf(encodedValue);
  }
}
