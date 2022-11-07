package games.strategy.triplea.settings;

import javax.annotation.Nullable;

final class LongClientSetting extends ClientSetting<Long> {
  LongClientSetting(final String name) {
    super(Long.class, name);
  }

  LongClientSetting(final String name, final long defaultValue) {
    super(Long.class, name, defaultValue);
  }

  @Override
  protected String encodeValue(final Long value) {
    return value.toString();
  }

  @Override
  @Nullable
  protected Long decodeValue(final String encodedValue) throws ValueEncodingException {
    try {
      if (encodedValue.isEmpty()) {
        return null;
      }
      return Long.valueOf(encodedValue);
    } catch (final NumberFormatException e) {
      throw new ValueEncodingException(e);
    }
  }
}
