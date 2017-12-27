package tools.map.making;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import games.strategy.engine.data.properties.PropertiesUI;
import games.strategy.triplea.Constants;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.util.Tuple;

/**
 * An object to hold all the map.properties values.
 */
class MapProperties {
  private Map<String, Color> colorMap = new TreeMap<>();
  private String unitsScale = "0.75";
  private int unitsWidth = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
  private int unitsHeight = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
  private int unitsCounterOffsetWidth = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE / 4;
  private int unitsCounterOffsetHeight = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
  private int unitsStackSize = 0;
  private int mapWidth = 256;
  private int mapHeight = 256;
  private boolean mapScrollWrapX = true;
  private boolean mapScrollWrapY = false;
  private boolean mapHasRelief = true;
  private int mapCursorHotspotX = 0;
  private int mapCursorHotspotY = 0;
  private boolean mapShowCapitolMarkers = true;
  private boolean mapUseTerritoryEffectMarkers = false;
  private boolean mapShowTerritoryNames = true;
  private boolean mapShowResources = true;
  private boolean mapShowComments = true;
  private boolean mapShowSeaZoneNames = false;
  private boolean mapDrawNamesFromTopLeft = false;
  private boolean mapUseNationConvoyFlags = false;
  private String dontDrawTerritoryNames = "";
  private boolean mapMapBlends = false;
  // options are: NORMAL, OVERLAY, LINEAR_LIGHT, DIFFERENCE, MULTIPLY
  private String mapMapBlendMode = "OVERLAY";
  private String mapMapBlendAlpha = "0.3";
  private boolean screenshotTitleEnabled = true;
  private int screenshotTitleX = 50;
  private int screenshotTitleY = 50;
  private Color screenshotTitleColor = Color.black;
  private int screenshotTitleFontSize = 20;

  MapProperties() {
    super();
    // fill the color map
    colorMap.put(Constants.PLAYER_NAME_AMERICANS, new Color(0x666600));
    colorMap.put(Constants.PLAYER_NAME_AUSTRALIANS, new Color(0xCCCC00));
    colorMap.put(Constants.PLAYER_NAME_BRITISH, new Color(0x916400));
    colorMap.put(Constants.PLAYER_NAME_CANADIANS, new Color(0xDBBE7F));
    colorMap.put(Constants.PLAYER_NAME_CHINESE, new Color(0x663E66));
    colorMap.put(Constants.PLAYER_NAME_FRENCH, new Color(0x113A77));
    colorMap.put(Constants.PLAYER_NAME_GERMANS, new Color(0x777777));
    colorMap.put(Constants.PLAYER_NAME_ITALIANS, new Color(0x0B7282));
    colorMap.put(Constants.PLAYER_NAME_JAPANESE, new Color(0xFFD400));
    colorMap.put(Constants.PLAYER_NAME_PUPPET_STATES, new Color(0x1B5DA0));
    colorMap.put(Constants.PLAYER_NAME_RUSSIANS, new Color(0xB23B00));
    colorMap.put(Constants.PLAYER_NAME_NEUTRAL, new Color(0xE2A071));
    colorMap.put(Constants.PLAYER_NAME_IMPASSABLE, new Color(0xD8BA7C));
  }

  Tuple<PropertiesUI, List<MapPropertyWrapper<?>>> propertyWrapperUi(final boolean editable) {
    return MapPropertyWrapper.createPropertiesUi(this, editable);
  }

  void writePropertiesToObject(final List<MapPropertyWrapper<?>> properties) {
    MapPropertyWrapper.writePropertiesToObject(this, properties);
  }

  public Map<String, Color> getColorMap() {
    return colorMap;
  }

  public void setColorMap(final Map<String, Color> value) {
    colorMap = value;
  }

  public String getUnitsScale() {
    return unitsScale;
  }

  public void setUnitsScale(final String value) {
    final double dvalue = Math.max(0.0, Math.min(2.0, Double.parseDouble(value)));
    if (Math.abs(1.25 - dvalue) < 0.01) {
      unitsScale = "1.25";
    } else if (Math.abs(1.0 - dvalue) < 0.01) {
      unitsScale = "1.0";
    } else if (Math.abs(0.875 - dvalue) < 0.01) {
      unitsScale = "0.875";
    } else if (Math.abs(0.8333 - dvalue) < 0.01) {
      unitsScale = "0.8333";
    } else if (Math.abs(0.75 - dvalue) < 0.01) {
      unitsScale = "0.75";
    } else if (Math.abs(0.6666 - dvalue) < 0.01) {
      unitsScale = "0.6666";
    } else if (Math.abs(0.5625 - dvalue) < 0.01) {
      unitsScale = "0.5625";
    } else if (Math.abs(0.5 - dvalue) < 0.01) {
      unitsScale = "0.5";
    } else {
      unitsScale = "" + dvalue;
    }
  }

  public int getUnitsWidth() {
    return unitsWidth;
  }

  public void setUnitsWidth(final int value) {
    unitsWidth = value;
  }

  public int getUnitsHeight() {
    return unitsHeight;
  }

  public void setUnitsHeight(final int value) {
    unitsHeight = value;
  }

  public int getUnitsCounterOffsetWidth() {
    return unitsCounterOffsetWidth;
  }

  public void setUnitsCounterOffsetWidth(final int value) {
    unitsCounterOffsetWidth = value;
  }

  public int getUnitsCounterOffsetHeight() {
    return unitsCounterOffsetHeight;
  }

  public void setUnitsCounterOffsetHeight(final int value) {
    unitsCounterOffsetHeight = value;
  }

  public int getUnitsStackSize() {
    return unitsStackSize;
  }

  public void setUnitsStackSize(final int value) {
    unitsStackSize = value;
  }

  public int getMapWidth() {
    return mapWidth;
  }

  public void setMapWidth(final int value) {
    mapWidth = value;
  }

  public int getMapHeight() {
    return mapHeight;
  }

  public void setMapHeight(final int value) {
    mapHeight = value;
  }

  public boolean getMapScrollWrapX() {
    return mapScrollWrapX;
  }

  public void setMapScrollWrapX(final boolean value) {
    mapScrollWrapX = value;
  }

  public boolean getMapScrollWrapY() {
    return mapScrollWrapY;
  }

  public void setMapScrollWrapY(final boolean value) {
    mapScrollWrapY = value;
  }

  public boolean getMapHasRelief() {
    return mapHasRelief;
  }

  public void setMapHasRelief(final boolean value) {
    mapHasRelief = value;
  }

  public int getMapCursorHotspotX() {
    return mapCursorHotspotX;
  }

  public void setMapCursorHotspotX(final int value) {
    mapCursorHotspotX = value;
  }

  public int getMapCursorHotspotY() {
    return mapCursorHotspotY;
  }

  public void setMapCursorHotspotY(final int value) {
    mapCursorHotspotY = value;
  }

  public boolean getMapShowCapitolMarkers() {
    return mapShowCapitolMarkers;
  }

  public void setMapShowCapitolMarkers(final boolean value) {
    mapShowCapitolMarkers = value;
  }

  public boolean getMapUseTerritoryEffectMarkers() {
    return mapUseTerritoryEffectMarkers;
  }

  public void setMapUseTerritoryEffectMarkers(final boolean value) {
    mapUseTerritoryEffectMarkers = value;
  }

  public boolean getMapShowTerritoryNames() {
    return mapShowTerritoryNames;
  }

  public void setMapShowTerritoryNames(final boolean value) {
    mapShowTerritoryNames = value;
  }

  public boolean getMapShowResources() {
    return mapShowResources;
  }

  public void setMapShowResources(final boolean value) {
    mapShowResources = value;
  }

  public boolean getMapShowComments() {
    return mapShowComments;
  }

  public void setMapShowComments(final boolean value) {
    mapShowComments = value;
  }

  public boolean getMapShowSeaZoneNames() {
    return mapShowSeaZoneNames;
  }

  public void setMapShowSeaZoneNames(final boolean value) {
    mapShowSeaZoneNames = value;
  }

  public boolean getMapDrawNamesFromTopLeft() {
    return mapDrawNamesFromTopLeft;
  }

  public void setMapDrawNamesFromTopLeft(final boolean value) {
    mapDrawNamesFromTopLeft = value;
  }

  public boolean getMapUseNationConvoyFlags() {
    return mapUseNationConvoyFlags;
  }

  public void setMapUseNationConvoyFlags(final boolean value) {
    mapUseNationConvoyFlags = value;
  }

  public String getDontDrawTerritoryNames() {
    return dontDrawTerritoryNames;
  }

  public void setDontDrawTerritoryNames(final String value) {
    dontDrawTerritoryNames = value;
  }

  public boolean getMapMapBlends() {
    return mapMapBlends;
  }

  public void setMapMapBlends(final boolean value) {
    mapMapBlends = value;
  }

  public String getMapMapBlendMode() {
    return mapMapBlendMode;
  }

  public void setMapMapBlendMode(final String value) {
    mapMapBlendMode = value;
  }

  public String getMapMapBlendAlpha() {
    return mapMapBlendAlpha;
  }

  public void setMapMapBlendAlpha(final String value) {
    Double.parseDouble(value);
    mapMapBlendAlpha = value;
  }

  public boolean getScreenshotTitleEnabled() {
    return screenshotTitleEnabled;
  }

  public void setScreenshotTitleEnabled(final boolean value) {
    screenshotTitleEnabled = value;
  }

  public int getScreenshotTitleX() {
    return screenshotTitleX;
  }

  public void setScreenshotTitleX(final int value) {
    screenshotTitleX = value;
  }

  public int getScreenshotTitleY() {
    return screenshotTitleY;
  }

  public void setScreenshotTitleY(final int value) {
    screenshotTitleY = value;
  }

  public Color getScreenshotTitleColor() {
    return screenshotTitleColor;
  }

  public void setScreenshotTitleColor(final Color value) {
    screenshotTitleColor = value;
  }

  public int getScreenshotTitleFontSize() {
    return screenshotTitleFontSize;
  }

  public void setScreenshotTitleFontSize(final int value) {
    screenshotTitleFontSize = value;
  }
}
