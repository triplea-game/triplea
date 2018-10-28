package games.strategy.triplea.settings;

final class EnumClientSetting<E extends Enum<E>> extends ClientSetting<E> {
  EnumClientSetting(final Class<E> type, final String name, final E defaultValue) {
    super(type, name, defaultValue);
  }

  @Override
  protected String formatValue(final E value) {
    return value.toString();
  }

  @Override
  protected E parseValue(final String encodedValue) {
    return Enum.valueOf(getType(), encodedValue);
  }
}
