package games.strategy.triplea.settings;

import org.triplea.swing.SettingPersistence;

/**
 * Specialized client setting class to return a boolean property value rather than an optional.
 * Boolean client settings are false by default and always have either a false or a true value
 * (never null).
 */
public final class BooleanClientSetting extends ClientSetting<Boolean>
    implements SettingPersistence {

  BooleanClientSetting(final String name) {
    this(name, false);
  }

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

  @Override
  public void saveSetting(final boolean value) {
    setValueAndFlush(value);
  }

  @Override
  public boolean getSetting() {
    return getValue().or(this::getDefaultValue).orElse(false);
  }
}
