package tools.map.making;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import games.strategy.engine.data.properties.PropertiesUI;
import games.strategy.triplea.Constants;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.triplea.ui.mapdata.MapData;
import games.strategy.util.Tuple;

/**
 * An object to hold all the map.properties values.
 */
public class MapProperties {
  public Map<String, Color> colorMap = new TreeMap<>();
  public String unitsScale = "0.75";
  public int unitsWidth = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
  public int unitsHeight = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
  public int unitsCounterOffsetWidth = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE / 4;
  public int unitsCounterOffsetHeight = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
  public int unitsStackSize = 0;
  public int mapWidth = 256;
  public int mapHeight = 256;
  public boolean mapScrollWrapX = true;
  public boolean mapScrollWrapY = false;
  public boolean mapHasRelief = true;
  public int mapCursorHotspotX = 0;
  public int mapCursorHotspotY = 0;
  public boolean mapShowCapitolMarkers = true;
  public boolean mapUseTerritoryEffectMarkers = false;
  public boolean mapShowTerritoryNames = true;
  public boolean mapShowResources = true;
  public boolean mapShowComments = true;
  public boolean mapShowSeaZoneNames = false;
  public boolean mapDrawNamesFromTopLeft = false;
  public boolean mapUseNationConvoyFlags = false;
  public String dontDrawTerritoryNames = "";
  public boolean mapMapBlends = false;
  // options are: NORMAL, OVERLAY, LINEAR_LIGHT, DIFFERENCE, MULTIPLY
  public String mapMapBlendMode = "OVERLAY";
  public String mapMapBlendAlpha = "0.3";
  public boolean screenshotTitleEnabled = true;
  public int screenshotTitleX = 50;
  public int screenshotTitleY = 50;
  public Color screenshotTitleColor = Color.black;
  public int screenshotTitleFontSize = 20;

  public MapProperties() {
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

  public Tuple<PropertiesUI, List<MapPropertyWrapper<?>>> propertyWrapperUI(final boolean editable) {
    return MapPropertyWrapper.createPropertiesUI(this, editable);
  }

  public void writePropertiesToObject(final List<MapPropertyWrapper<?>> properties) {
    MapPropertyWrapper.writePropertiesToObject(this, properties);
  }

  public Map<String, Color> getColorMap() {
    return colorMap;
  }

  public void setColorMap(final Map<String, Color> value) {
    colorMap = value;
  }

  public String outColorMap() {
    final StringBuilder buf = new StringBuilder();
    for (final Entry<String, Color> entry : colorMap.entrySet()) {
      buf.append(MapData.PROPERTY_COLOR_PREFIX).append(entry.getKey()).append("=").append(colorToHex(entry.getValue()))
          .append("\r\n");
    }
    return buf.toString();
  }

  private static String colorToHex(final Color color) {
    String hexString = Integer.toHexString(color.getRGB() & 0x00FFFFFF);
    while (hexString.length() < 6) {
      hexString = "0" + hexString;
    }
    return hexString;
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

  public String outUnitsScale() {
    return MapData.PROPERTY_UNITS_SCALE + "=" + unitsScale + "\r\n";
  }

  public int getUnitsWidth() {
    return unitsWidth;
  }

  public void setUnitsWidth(final int value) {
    unitsWidth = value;
  }

  public String outUnitsWidth() {
    return MapData.PROPERTY_UNITS_WIDTH + "=" + unitsWidth + "\r\n";
  }

  public int getUnitsHeight() {
    return unitsHeight;
  }

  public void setUnitsHeight(final int value) {
    unitsHeight = value;
  }

  public String outUnitsHeight() {
    return MapData.PROPERTY_UNITS_HEIGHT + "=" + unitsHeight + "\r\n";
  }

  public int getUnitsCounterOffsetWidth() {
    return unitsCounterOffsetWidth;
  }

  public void setUnitsCounterOffsetWidth(final int value) {
    unitsCounterOffsetWidth = value;
  }

  public String outUnitsCounterOffsetWidth() {
    return MapData.PROPERTY_UNITS_COUNTER_OFFSET_WIDTH + "=" + unitsCounterOffsetWidth + "\r\n";
  }

  public int getUnitsCounterOffsetHeight() {
    return unitsCounterOffsetHeight;
  }

  public void setUnitsCounterOffsetHeight(final int value) {
    unitsCounterOffsetHeight = value;
  }

  public String outUnitsCounterOffsetHeight() {
    return MapData.PROPERTY_UNITS_COUNTER_OFFSET_HEIGHT + "=" + unitsCounterOffsetHeight + "\r\n";
  }

  public int getUnitsStackSize() {
    return unitsStackSize;
  }

  public void setUnitsStackSize(final int value) {
    unitsStackSize = value;
  }

  public String outUnitsStackSize() {
    return MapData.PROPERTY_UNITS_STACK_SIZE + "=" + unitsStackSize + "\r\n";
  }

  public int getMapWidth() {
    return mapWidth;
  }

  public void setMapWidth(final int value) {
    mapWidth = value;
  }

  public String outMapWidth() {
    return MapData.PROPERTY_MAP_WIDTH + "=" + mapWidth + "\r\n";
  }

  public int getMapHeight() {
    return mapHeight;
  }

  public void setMapHeight(final int value) {
    mapHeight = value;
  }

  public String outMapHeight() {
    return MapData.PROPERTY_MAP_HEIGHT + "=" + mapHeight + "\r\n";
  }

  public boolean getMapScrollWrapX() {
    return mapScrollWrapX;
  }

  public void setMapScrollWrapX(final boolean value) {
    mapScrollWrapX = value;
  }

  public String outMapScrollWrapX() {
    return MapData.PROPERTY_MAP_SCROLLWRAPX + "=" + mapScrollWrapX + "\r\n";
  }

  public boolean getMapScrollWrapY() {
    return mapScrollWrapY;
  }

  public void setMapScrollWrapY(final boolean value) {
    mapScrollWrapY = value;
  }

  public String outMapScrollWrapY() {
    return MapData.PROPERTY_MAP_SCROLLWRAPY + "=" + mapScrollWrapY + "\r\n";
  }

  public boolean getMapHasRelief() {
    return mapHasRelief;
  }

  public void setMapHasRelief(final boolean value) {
    mapHasRelief = value;
  }

  public String outMapHasRelief() {
    return MapData.PROPERTY_MAP_HASRELIEF + "=" + mapHasRelief + "\r\n";
  }

  public int getMapCursorHotspotX() {
    return mapCursorHotspotX;
  }

  public void setMapCursorHotspotX(final int value) {
    mapCursorHotspotX = value;
  }

  public String outMapCursorHotspotX() {
    return MapData.PROPERTY_MAP_CURSOR_HOTSPOT_X + "=" + mapCursorHotspotX + "\r\n";
  }

  public int getMapCursorHotspotY() {
    return mapCursorHotspotY;
  }

  public void setMapCursorHotspotY(final int value) {
    mapCursorHotspotY = value;
  }

  public String outMapCursorHotspotY() {
    return MapData.PROPERTY_MAP_CURSOR_HOTSPOT_Y + "=" + mapCursorHotspotY + "\r\n";
  }

  public boolean getMapShowCapitolMarkers() {
    return mapShowCapitolMarkers;
  }

  public void setMapShowCapitolMarkers(final boolean value) {
    mapShowCapitolMarkers = value;
  }

  public String outMapShowCapitolMarkers() {
    return MapData.PROPERTY_MAP_SHOWCAPITOLMARKERS + "=" + mapShowCapitolMarkers + "\r\n";
  }

  public boolean getMapUseTerritoryEffectMarkers() {
    return mapUseTerritoryEffectMarkers;
  }

  public void setMapUseTerritoryEffectMarkers(final boolean value) {
    mapUseTerritoryEffectMarkers = value;
  }

  public String outMapUseTerritoryEffectMarkers() {
    return MapData.PROPERTY_MAP_USETERRITORYEFFECTMARKERS + "=" + mapUseTerritoryEffectMarkers + "\r\n";
  }

  public boolean getMapShowTerritoryNames() {
    return mapShowTerritoryNames;
  }

  public void setMapShowTerritoryNames(final boolean value) {
    mapShowTerritoryNames = value;
  }

  public String outMapShowTerritoryNames() {
    return MapData.PROPERTY_MAP_SHOWTERRITORYNAMES + "=" + mapShowTerritoryNames + "\r\n";
  }

  public boolean getMapShowResources() {
    return mapShowResources;
  }

  public void setMapShowResources(final boolean value) {
    mapShowResources = value;
  }

  public String outMapShowResources() {
    return MapData.PROPERTY_MAP_SHOWRESOURCES + "=" + mapShowResources + "\r\n";
  }

  public boolean getMapShowComments() {
    return mapShowComments;
  }

  public void setMapShowComments(final boolean value) {
    mapShowComments = value;
  }

  public String outMapShowComments() {
    return MapData.PROPERTY_MAP_SHOWCOMMENTS + "=" + mapShowComments + "\r\n";
  }

  public boolean getMapShowSeaZoneNames() {
    return mapShowSeaZoneNames;
  }

  public void setMapShowSeaZoneNames(final boolean value) {
    mapShowSeaZoneNames = value;
  }

  public String outMapShowSeaZoneNames() {
    return MapData.PROPERTY_MAP_SHOWSEAZONENAMES + "=" + mapShowSeaZoneNames + "\r\n";
  }

  public boolean getMapDrawNamesFromTopLeft() {
    return mapDrawNamesFromTopLeft;
  }

  public void setMapDrawNamesFromTopLeft(final boolean value) {
    mapDrawNamesFromTopLeft = value;
  }

  public String outMapDrawNamesFromTopLeft() {
    return MapData.PROPERTY_MAP_DRAWNAMESFROMTOPLEFT + "=" + mapDrawNamesFromTopLeft + "\r\n";
  }

  public boolean getMapUseNationConvoyFlags() {
    return mapUseNationConvoyFlags;
  }

  public void setMapUseNationConvoyFlags(final boolean value) {
    mapUseNationConvoyFlags = value;
  }

  public String outMapUseNationConvoyFlags() {
    return MapData.PROPERTY_MAP_USENATION_CONVOYFLAGS + "=" + mapUseNationConvoyFlags + "\r\n";
  }

  public String getDontDrawTerritoryNames() {
    return dontDrawTerritoryNames;
  }

  public void setDontDrawTerritoryNames(final String value) {
    dontDrawTerritoryNames = value;
  }

  public String outDontDrawTerritoryNames() {
    return MapData.PROPERTY_DONT_DRAW_TERRITORY_NAMES + "=" + dontDrawTerritoryNames + "\r\n";
  }

  public boolean getMapMapBlends() {
    return mapMapBlends;
  }

  public void setMapMapBlends(final boolean value) {
    mapMapBlends = value;
  }

  public String outMapMapBlends() {
    return MapData.PROPERTY_MAP_MAPBLENDS + "=" + mapMapBlends + "\r\n";
  }

  public String getMapMapBlendMode() {
    return mapMapBlendMode;
  }

  public void setMapMapBlendMode(final String value) {
    mapMapBlendMode = value;
  }

  public String outMapMapBlendMode() {
    return MapData.PROPERTY_MAP_MAPBLENDMODE + "=" + mapMapBlendMode + "\r\n";
  }

  public String getMapMapBlendAlpha() {
    return mapMapBlendAlpha;
  }

  public void setMapMapBlendAlpha(final String value) {
    Double.parseDouble(value);
    mapMapBlendAlpha = value;
  }

  public String outMapMapBlendAlpha() {
    return MapData.PROPERTY_MAP_MAPBLENDALPHA + "=" + mapMapBlendAlpha + "\r\n";
  }

  public boolean getScreenshotTitleEnabled() {
    return screenshotTitleEnabled;
  }

  public void setScreenshotTitleEnabled(final boolean value) {
    screenshotTitleEnabled = value;
  }

  public String outScreenshotTitleEnabled() {
    return MapData.PROPERTY_SCREENSHOT_TITLE_ENABLED + "=" + screenshotTitleEnabled + "\r\n";
  }

  public int getScreenshotTitleX() {
    return screenshotTitleX;
  }

  public void setScreenshotTitleX(final int value) {
    screenshotTitleX = value;
  }

  public String outScreenshotTitleX() {
    return MapData.PROPERTY_SCREENSHOT_TITLE_X + "=" + screenshotTitleX + "\r\n";
  }

  public int getScreenshotTitleY() {
    return screenshotTitleY;
  }

  public void setScreenshotTitleY(final int value) {
    screenshotTitleY = value;
  }

  public String outScreenshotTitleY() {
    return MapData.PROPERTY_SCREENSHOT_TITLE_Y + "=" + screenshotTitleY + "\r\n";
  }

  public Color getScreenshotTitleColor() {
    return screenshotTitleColor;
  }

  public void setScreenshotTitleColor(final Color value) {
    screenshotTitleColor = value;
  }

  public String outScreenshotTitleColor() {
    return MapData.PROPERTY_SCREENSHOT_TITLE_COLOR + "=" + colorToHex(screenshotTitleColor) + "\r\n";
  }

  public int getScreenshotTitleFontSize() {
    return screenshotTitleFontSize;
  }

  public void setScreenshotTitleFontSize(final int value) {
    screenshotTitleFontSize = value;
  }

  public String outScreenshotTitleFontSize() {
    return MapData.PROPERTY_SCREENSHOT_TITLE_FONT_SIZE + "=" + screenshotTitleFontSize + "\r\n";
  }
}
