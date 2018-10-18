package games.strategy.triplea.settings;

final class IntegerClientSetting extends ClientSetting<Integer> {
  IntegerClientSetting(final String name) {
    super(Integer.class, name);
  }

  IntegerClientSetting(final String name, final int defaultValue) {
    super(Integer.class, name, defaultValue);
  }

  @Override
  protected String formatValue(final Integer value) {
    return value.toString();
  }

  @Override
  protected Integer parseValue(final String encodedValue) {
    return Integer.valueOf(encodedValue);
  }
}
