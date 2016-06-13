package games.strategy.triplea.settings.scrolling;

import games.strategy.triplea.settings.HasDefaults;
import games.strategy.triplea.settings.PreferenceKey;
import games.strategy.triplea.settings.SystemPreferences;

public class ScrollSettings implements HasDefaults {

  private int mapEdgeFasterScrollZoneSize;
  private int mapEdgeScrollZoneSize;

  private int mapEdgeFasterScrollMultiplier;

  private int arrowKeyScrollSpeed;
  private int fasterArrowKeyScrollMultiplier;


  private int mapEdgeScrollSpeed;
  private int wheelScrollAmount;

  public ScrollSettings() {
    mapEdgeFasterScrollZoneSize = getProp(PreferenceKey.MAP_EDGE_FASTER_SCROLL_ZONE_SIZE, 10);
    mapEdgeScrollZoneSize = getProp(PreferenceKey.MAP_EDGE_SCROLL_ZONE_SIZE, 30);

    mapEdgeFasterScrollMultiplier = getProp(PreferenceKey.MAP_EDGE_FASTER_SCROLL_MULTIPLER, 2);

    arrowKeyScrollSpeed = getProp(PreferenceKey.ARROW_KEY_SCROLL_SPEED, 70);
    fasterArrowKeyScrollMultiplier = getProp(PreferenceKey.FASTER_ARROW_KEY_SCROLL_MULTIPLIER, 3);

    mapEdgeScrollSpeed = getProp(PreferenceKey.MAP_EDGE_SCROLL_SPEED, 30);
    wheelScrollAmount = getProp(PreferenceKey.WHEEL_SCROLL_AMOUNT, 60);
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
    return mapEdgeFasterScrollZoneSize;
  }

  public void setMapEdgeFasterScrollMultiplier(String value) {
    this.mapEdgeFasterScrollMultiplier = Integer.parseInt(value);
    SystemPreferences.put(PreferenceKey.MAP_EDGE_FASTER_SCROLL_MULTIPLER, value);
  }

  public int getMapEdgeFasterScrollMultiplier() {
    return mapEdgeFasterScrollMultiplier;
  }

  public void setMapEdgeFasterScrollZoneSize(String value) {
    this.mapEdgeFasterScrollZoneSize = Integer.parseInt(value);
    SystemPreferences.put(PreferenceKey.MAP_EDGE_FASTER_SCROLL_ZONE_SIZE, value);
  }


  public int getMapEdgeScrollZoneSize() {
    return mapEdgeScrollZoneSize;
  }

  public void setMapEdgeScrollZoneSize(String value) {
    this.mapEdgeScrollZoneSize = Integer.parseInt(value);
    SystemPreferences.put(PreferenceKey.MAP_EDGE_SCROLL_ZONE_SIZE, value);
  }

  public int getArrowKeyScrollSpeed() {
    return arrowKeyScrollSpeed;
  }

  public void setArrowKeyScrollSpeed(String value) {
    arrowKeyScrollSpeed = Integer.parseInt(value);
    SystemPreferences.put(PreferenceKey.ARROW_KEY_SCROLL_SPEED, value);
  }


  public int getFasterArrowKeyScrollMultiplier() {
    return fasterArrowKeyScrollMultiplier;
  }

  public void setFasterArrowKeyScrollMultiplier(String value) {
    this.fasterArrowKeyScrollMultiplier = Integer.valueOf(value);
    SystemPreferences.put(PreferenceKey.FASTER_ARROW_KEY_SCROLL_MULTIPLIER, value);
  }

  public int getMapEdgeScrollSpeed() {
    return mapEdgeScrollSpeed;
  }

  public void setMapEdgeScrollSpeed(String value) {
    this.mapEdgeScrollSpeed = Integer.valueOf(value);
    SystemPreferences.put(PreferenceKey.MAP_EDGE_SCROLL_SPEED, value);
  }

  public int getWheelScrollAmount() {
    return wheelScrollAmount;
  }

  public void setWheelScrollAmount(String value) {
    this.wheelScrollAmount = Integer.valueOf(value);
    SystemPreferences.put(PreferenceKey.WHEEL_SCROLL_AMOUNT, value);
  }

}
