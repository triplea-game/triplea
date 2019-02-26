package games.strategy.triplea.settings;

final class BooleanClientSetting extends ClientSetting<Boolean> {
  BooleanClientSetting(final String name, final boolean defaultValue) {
    super(Boolean.class, name, defaultValue);
  }

  @Override
  protected String encodeValue(final Boolean value) {
    return value.toString();
  }

  @Override
  protected Boolean decodeValue(final String encodedValue) {
    return Boolean.valueOf(encodedValue);
  }
}
