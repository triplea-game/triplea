package games.strategy.triplea.settings.scrolling;

import games.strategy.triplea.settings.HasDefaults;
import games.strategy.triplea.settings.SystemPreferenceKey;
import games.strategy.triplea.settings.SystemPreferences;

public class ScrollSettings implements HasDefaults {

  static final int DEFAULT_MAP_EDGE_SCROLL_SPEED = 30;
  static final int DEFAULT_MAP_EDGE_SCROLL_ZONE_SIZE = 30;

  static final int DEFAULT_ARROW_KEY_SCROLL_SPEED = 70;
  static final int DEFAULT_FASTER_ARROW_KEY_SCROLL_MULTIPLIER = 3;
  static final int DEFAULT_WHEEL_SCROLL_AMOUNT = 60;

  @Override
  public void setToDefault() {
    setMapEdgeScrollSpeed(String.valueOf(DEFAULT_MAP_EDGE_SCROLL_SPEED));
    setMapEdgeScrollZoneSize(String.valueOf(DEFAULT_MAP_EDGE_SCROLL_ZONE_SIZE));

    setArrowKeyScrollSpeed(String.valueOf(DEFAULT_ARROW_KEY_SCROLL_SPEED));
    setFasterArrowKeyScrollMultiplier(String.valueOf(DEFAULT_FASTER_ARROW_KEY_SCROLL_MULTIPLIER));
    setWheelScrollAmount(String.valueOf(DEFAULT_WHEEL_SCROLL_AMOUNT));
  }


  private int getProp(final SystemPreferenceKey key, final int defaultValue) {
    return SystemPreferences.get(key, defaultValue);
  }

  public int getMapEdgeScrollZoneSize() {
    return getProp(SystemPreferenceKey.MAP_EDGE_SCROLL_ZONE_SIZE, DEFAULT_MAP_EDGE_SCROLL_ZONE_SIZE);
  }

  public void setMapEdgeScrollZoneSize(final String value) {
    SystemPreferences.put(SystemPreferenceKey.MAP_EDGE_SCROLL_ZONE_SIZE, value);
  }

  public int getArrowKeyScrollSpeed() {
    return getProp(SystemPreferenceKey.ARROW_KEY_SCROLL_SPEED, DEFAULT_ARROW_KEY_SCROLL_SPEED);
  }

  public void setArrowKeyScrollSpeed(final String value) {
    SystemPreferences.put(SystemPreferenceKey.ARROW_KEY_SCROLL_SPEED, value);
  }

  public int getFasterArrowKeyScrollMultiplier() {
    return getProp(SystemPreferenceKey.FASTER_ARROW_KEY_SCROLL_MULTIPLIER, DEFAULT_FASTER_ARROW_KEY_SCROLL_MULTIPLIER);
  }

  public void setFasterArrowKeyScrollMultiplier(final String value) {
    SystemPreferences.put(SystemPreferenceKey.FASTER_ARROW_KEY_SCROLL_MULTIPLIER, value);
  }

  public int getMapEdgeScrollSpeed() {
    return getProp(SystemPreferenceKey.MAP_EDGE_SCROLL_SPEED, DEFAULT_MAP_EDGE_SCROLL_SPEED);
  }

  public void setMapEdgeScrollSpeed(final String value) {
    SystemPreferences.put(SystemPreferenceKey.MAP_EDGE_SCROLL_SPEED, value);
  }

  public int getWheelScrollAmount() {
    return getProp(SystemPreferenceKey.WHEEL_SCROLL_AMOUNT, DEFAULT_WHEEL_SCROLL_AMOUNT);
  }

  public void setWheelScrollAmount(final String value) {
    SystemPreferences.put(SystemPreferenceKey.WHEEL_SCROLL_AMOUNT, value);
  }

}
