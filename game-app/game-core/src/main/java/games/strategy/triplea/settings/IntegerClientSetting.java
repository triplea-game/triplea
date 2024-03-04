package games.strategy.triplea.settings;

import javax.annotation.Nullable;

final class IntegerClientSetting extends ClientSetting<Integer> {
  IntegerClientSetting(final String name) {
    super(Integer.class, name);
  }

  IntegerClientSetting(final String name, final int defaultValue) {
    super(Integer.class, name, defaultValue);
  }

  @Override
  protected String encodeValue(final Integer value) {
    return value.toString();
  }

  @Override
  protected @Nullable Integer decodeValue(final String encodedValue) throws ValueEncodingException {
    try {
      if (encodedValue.isEmpty()) {
        return null;
      }
      return Integer.valueOf(encodedValue);
    } catch (final NumberFormatException e) {
      throw new ValueEncodingException(e);
    }
  }
}
