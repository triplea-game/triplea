package games.strategy.triplea.settings.scrolling;

import games.strategy.triplea.settings.HasDefaults;
import games.strategy.triplea.settings.PreferenceKey;
import games.strategy.triplea.settings.SystemPreferences;

public class ScrollSettings implements HasDefaults {

  private static final int DEFAULT_MAP_EDGE_SCROLL_SPEED = 30;
  private static final int DEFAULT_MAP_EDGE_SCROLL_ZONE_SIZE = 30;
  private static final int DEFAULT_MAP_EDGE_FASTER_SCROLL_MULTIPLIER = 2;
  private static final int DEFAULT_MAP_EDGE_FASTER_SCROLL_ZONE_SIZE = 10;

  private static final int DEFAULT_ARROW_KEY_SCROLL_SPEED = 70;
  private static final int DEFAULT_FASTER_ARROW_KEY_SCROLL_MULTIPLIER = 3;
  private static final int DEFAULT_WHEEL_SCROLL_AMOUNT = 60;

  @Override
  public void setToDefault() {
    setMapEdgeScrollSpeed(String.valueOf(DEFAULT_MAP_EDGE_SCROLL_SPEED));
    setMapEdgeScrollZoneSize(String.valueOf(DEFAULT_MAP_EDGE_SCROLL_ZONE_SIZE));
    setMapEdgeFasterScrollMultiplier(String.valueOf(DEFAULT_MAP_EDGE_FASTER_SCROLL_MULTIPLIER));
    setMapEdgeFasterScrollZoneSize(String.valueOf(DEFAULT_MAP_EDGE_FASTER_SCROLL_ZONE_SIZE));

    setArrowKeyScrollSpeed(String.valueOf(DEFAULT_ARROW_KEY_SCROLL_SPEED));
    setFasterArrowKeyScrollMultiplier(String.valueOf(DEFAULT_FASTER_ARROW_KEY_SCROLL_MULTIPLIER));
    setWheelScrollAmount(String.valueOf(DEFAULT_WHEEL_SCROLL_AMOUNT));
  }


  private int getProp(PreferenceKey key, int defaultValue) {
    return SystemPreferences.get(key, defaultValue);
  }

  public int getMapEdgeFasterScrollZoneSize() {
    return getProp(PreferenceKey.MAP_EDGE_FASTER_SCROLL_ZONE_SIZE, DEFAULT_MAP_EDGE_FASTER_SCROLL_ZONE_SIZE);
  }

  public void setMapEdgeFasterScrollMultiplier(String value) {
    SystemPreferences.put(PreferenceKey.MAP_EDGE_FASTER_SCROLL_MULTIPLER, value);
  }

  public int getMapEdgeFasterScrollMultiplier() {
    return getProp(PreferenceKey.MAP_EDGE_FASTER_SCROLL_MULTIPLER, DEFAULT_MAP_EDGE_FASTER_SCROLL_MULTIPLIER);
  }

  public void setMapEdgeFasterScrollZoneSize(String value) {
    SystemPreferences.put(PreferenceKey.MAP_EDGE_FASTER_SCROLL_ZONE_SIZE, value);
  }


  public int getMapEdgeScrollZoneSize() {
    return getProp(PreferenceKey.MAP_EDGE_SCROLL_ZONE_SIZE, DEFAULT_MAP_EDGE_SCROLL_ZONE_SIZE);
  }

  public void setMapEdgeScrollZoneSize(String value) {
    SystemPreferences.put(PreferenceKey.MAP_EDGE_SCROLL_ZONE_SIZE, value);
  }

  public int getArrowKeyScrollSpeed() {
    return getProp(PreferenceKey.ARROW_KEY_SCROLL_SPEED, DEFAULT_ARROW_KEY_SCROLL_SPEED);
  }

  public void setArrowKeyScrollSpeed(String value) {
    SystemPreferences.put(PreferenceKey.ARROW_KEY_SCROLL_SPEED, value);
  }

  public int getFasterArrowKeyScrollMultiplier() {
    return getProp(PreferenceKey.FASTER_ARROW_KEY_SCROLL_MULTIPLIER, DEFAULT_FASTER_ARROW_KEY_SCROLL_MULTIPLIER);
  }

  public void setFasterArrowKeyScrollMultiplier(String value) {
    SystemPreferences.put(PreferenceKey.FASTER_ARROW_KEY_SCROLL_MULTIPLIER, value);
  }

  public int getMapEdgeScrollSpeed() {
    return getProp(PreferenceKey.MAP_EDGE_SCROLL_SPEED, DEFAULT_MAP_EDGE_SCROLL_SPEED);
  }

  public void setMapEdgeScrollSpeed(String value) {
    SystemPreferences.put(PreferenceKey.MAP_EDGE_SCROLL_SPEED, value);
  }

  public int getWheelScrollAmount() {
    return getProp(PreferenceKey.WHEEL_SCROLL_AMOUNT, DEFAULT_WHEEL_SCROLL_AMOUNT);
  }

  public void setWheelScrollAmount(String value) {
    SystemPreferences.put(PreferenceKey.WHEEL_SCROLL_AMOUNT, value);
  }

}
