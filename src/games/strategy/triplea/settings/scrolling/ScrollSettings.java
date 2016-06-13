package games.strategy.triplea.settings.scrolling;

import games.strategy.triplea.settings.HasDefaults;
import games.strategy.triplea.settings.PreferenceKey;
import games.strategy.triplea.settings.SystemPreferences;

public class ScrollSettings implements HasDefaults {

  public ScrollSettings() {

  }

  @Override
  public void setToDefault() {
    setMapEdgeFasterScrollZoneSize("10");
    setMapEdgeScrollZoneSize("30");
    setMapEdgeFasterScrollMultiplier("2");
    setArrowKeyScrollSpeed("70");
    setFasterArrowKeyScrollMultiplier("3");
    setWheelScrollAmount("60");
  }


  private int getProp(PreferenceKey key, int defaultValue) {
    return SystemPreferences.get(key, defaultValue);
  }

  public int getMapEdgeFasterScrollZoneSize() {
    return getProp(PreferenceKey.MAP_EDGE_FASTER_SCROLL_ZONE_SIZE, 10);
  }

  public void setMapEdgeFasterScrollMultiplier(String value) {
    SystemPreferences.put(PreferenceKey.MAP_EDGE_FASTER_SCROLL_MULTIPLER, value);
  }

  public int getMapEdgeFasterScrollMultiplier() {
    return getProp(PreferenceKey.MAP_EDGE_FASTER_SCROLL_MULTIPLER, 2);
  }

  public void setMapEdgeFasterScrollZoneSize(String value) {
    SystemPreferences.put(PreferenceKey.MAP_EDGE_FASTER_SCROLL_ZONE_SIZE, value);
  }


  public int getMapEdgeScrollZoneSize() {
    return getProp(PreferenceKey.MAP_EDGE_SCROLL_ZONE_SIZE, 30);
  }

  public void setMapEdgeScrollZoneSize(String value) {
    SystemPreferences.put(PreferenceKey.MAP_EDGE_SCROLL_ZONE_SIZE, value);
  }

  public int getArrowKeyScrollSpeed() {
    return getProp(PreferenceKey.ARROW_KEY_SCROLL_SPEED, 70);
  }

  public void setArrowKeyScrollSpeed(String value) {
    SystemPreferences.put(PreferenceKey.ARROW_KEY_SCROLL_SPEED, value);
  }

  public int getFasterArrowKeyScrollMultiplier() {
    return getProp(PreferenceKey.FASTER_ARROW_KEY_SCROLL_MULTIPLIER, 3);
  }

  public void setFasterArrowKeyScrollMultiplier(String value) {
    SystemPreferences.put(PreferenceKey.FASTER_ARROW_KEY_SCROLL_MULTIPLIER, value);
  }

  public int getMapEdgeScrollSpeed() {
    return getProp(PreferenceKey.MAP_EDGE_SCROLL_SPEED, 30);
  }

  public void setMapEdgeScrollSpeed(String value) {
    SystemPreferences.put(PreferenceKey.MAP_EDGE_SCROLL_SPEED, value);
  }

  public int getWheelScrollAmount() {
    return getProp(PreferenceKey.WHEEL_SCROLL_AMOUNT, 60);
  }

  public void setWheelScrollAmount(String value) {
    SystemPreferences.put(PreferenceKey.WHEEL_SCROLL_AMOUNT, value);
  }

}
