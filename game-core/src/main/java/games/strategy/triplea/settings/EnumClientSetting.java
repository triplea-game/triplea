package games.strategy.triplea.settings;

final class EnumClientSetting<E extends Enum<E>> extends ClientSetting<E> {
  EnumClientSetting(final Class<E> type, final String name, final E defaultValue) {
    super(type, name, defaultValue);
  }

  @Override
  protected String encodeValue(final E value) {
    return value.toString();
  }

  @Override
  protected E decodeValue(final String encodedValue) throws ValueEncodingException {
    try {
      return Enum.valueOf(getType(), encodedValue);
    } catch (final IllegalArgumentException e) {
      throw new ValueEncodingException(e);
    }
  }
}
