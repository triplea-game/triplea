package tools.map.making;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import games.strategy.engine.data.properties.PropertiesUi;
import games.strategy.triplea.Constants;
import games.strategy.util.Tuple;

/**
 * An object to hold all the map.properties values.
 */
class MapProperties {
  private Map<String, Color> colorMap = new TreeMap<>();
  private double unitsScale = 0.75;
  private int mapWidth = 256;
  private int mapHeight = 256;

  MapProperties() {
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

  Tuple<PropertiesUi, List<MapPropertyWrapper<?>>> propertyWrapperUi(final boolean editable) {
    return MapPropertyWrapper.newPropertiesUi(this, editable);
  }

  void writePropertiesToObject(final List<MapPropertyWrapper<?>> properties) {
    MapPropertyWrapper.writePropertiesToObject(this, properties);
  }

  Map<String, Color> getColorMap() {
    return colorMap;
  }

  double getUnitsScale() {
    return unitsScale;
  }

  /**
   * Sets the value of the {@code units.scale} map property.
   *
   * <p>
   * The implementation accounts for small rounding errors when {@code value} is one of the standard units scale
   * values: 0.5, 0.5625, 0.6666, 0.75, 0.8333, 0.875, 1.0, 1.25.
   * </p>
   */
  void setUnitsScale(final double value) {
    final double dvalue = Math.max(0.0, Math.min(2.0, value));
    if (Math.abs(1.25 - dvalue) < 0.01) {
      unitsScale = 1.25;
    } else if (Math.abs(1.0 - dvalue) < 0.01) {
      unitsScale = 1.0;
    } else if (Math.abs(0.875 - dvalue) < 0.01) {
      unitsScale = 0.875;
    } else if (Math.abs(0.8333 - dvalue) < 0.01) {
      unitsScale = 0.8333;
    } else if (Math.abs(0.75 - dvalue) < 0.01) {
      unitsScale = 0.75;
    } else if (Math.abs(0.6666 - dvalue) < 0.01) {
      unitsScale = 0.6666;
    } else if (Math.abs(0.5625 - dvalue) < 0.01) {
      unitsScale = 0.5625;
    } else if (Math.abs(0.5 - dvalue) < 0.01) {
      unitsScale = 0.5;
    } else {
      unitsScale = dvalue;
    }
  }

  int getMapWidth() {
    return mapWidth;
  }

  void setMapWidth(final int value) {
    mapWidth = value;
  }

  int getMapHeight() {
    return mapHeight;
  }

  void setMapHeight(final int value) {
    mapHeight = value;
  }

}
