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
public class MapProperties {
  public Map<String, Color> COLOR_MAP = new TreeMap<>();
  public String UNITS_SCALE = "0.75";
  public int UNITS_WIDTH = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
  public int UNITS_HEIGHT = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
  public int UNITS_COUNTER_OFFSET_WIDTH = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE / 4;
  public int UNITS_COUNTER_OFFSET_HEIGHT = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
  public int MAP_WIDTH = 256;
  public int MAP_HEIGHT = 256;

  public MapProperties() {
    super();
    // fill the color map
    COLOR_MAP.put(Constants.PLAYER_NAME_AMERICANS, new Color(0x666600));
    COLOR_MAP.put(Constants.PLAYER_NAME_AUSTRALIANS, new Color(0xCCCC00));
    COLOR_MAP.put(Constants.PLAYER_NAME_BRITISH, new Color(0x916400));
    COLOR_MAP.put(Constants.PLAYER_NAME_CANADIANS, new Color(0xDBBE7F));
    COLOR_MAP.put(Constants.PLAYER_NAME_CHINESE, new Color(0x663E66));
    COLOR_MAP.put(Constants.PLAYER_NAME_FRENCH, new Color(0x113A77));
    COLOR_MAP.put(Constants.PLAYER_NAME_GERMANS, new Color(0x777777));
    COLOR_MAP.put(Constants.PLAYER_NAME_ITALIANS, new Color(0x0B7282));
    COLOR_MAP.put(Constants.PLAYER_NAME_JAPANESE, new Color(0xFFD400));
    COLOR_MAP.put(Constants.PLAYER_NAME_PUPPET_STATES, new Color(0x1B5DA0));
    COLOR_MAP.put(Constants.PLAYER_NAME_RUSSIANS, new Color(0xB23B00));
    COLOR_MAP.put(Constants.PLAYER_NAME_NEUTRAL, new Color(0xE2A071));
    COLOR_MAP.put(Constants.PLAYER_NAME_IMPASSABLE, new Color(0xD8BA7C));
  }

  public Tuple<PropertiesUI, List<MapPropertyWrapper<?>>> propertyWrapperUI(final boolean editable) {
    return MapPropertyWrapper.createPropertiesUI(this, editable);
  }

  public void writePropertiesToObject(final List<MapPropertyWrapper<?>> properties) {
    MapPropertyWrapper.writePropertiesToObject(this, properties);
  }

  public Map<String, Color> getCOLOR_MAP() {
    return COLOR_MAP;
  }

  public String getUNITS_SCALE() {
    return UNITS_SCALE;
  }

  public void setUNITS_SCALE(final String value) {
    final double dvalue = Math.max(0.0, Math.min(2.0, Double.parseDouble(value)));
    if (Math.abs(1.25 - dvalue) < 0.01) {
      UNITS_SCALE = "1.25";
    } else if (Math.abs(1.0 - dvalue) < 0.01) {
      UNITS_SCALE = "1.0";
    } else if (Math.abs(0.875 - dvalue) < 0.01) {
      UNITS_SCALE = "0.875";
    } else if (Math.abs(0.8333 - dvalue) < 0.01) {
      UNITS_SCALE = "0.8333";
    } else if (Math.abs(0.75 - dvalue) < 0.01) {
      UNITS_SCALE = "0.75";
    } else if (Math.abs(0.6666 - dvalue) < 0.01) {
      UNITS_SCALE = "0.6666";
    } else if (Math.abs(0.5625 - dvalue) < 0.01) {
      UNITS_SCALE = "0.5625";
    } else if (Math.abs(0.5 - dvalue) < 0.01) {
      UNITS_SCALE = "0.5";
    } else {
      UNITS_SCALE = "" + dvalue;
    }
  }


  public void setUNITS_WIDTH(final int value) {
    UNITS_WIDTH = value;
  }

  public void setUNITS_HEIGHT(final int value) {
    UNITS_HEIGHT = value;
  }

  public void setUNITS_COUNTER_OFFSET_WIDTH(final int value) {
    UNITS_COUNTER_OFFSET_WIDTH = value;
  }

  public void setUNITS_COUNTER_OFFSET_HEIGHT(final int value) {
    UNITS_COUNTER_OFFSET_HEIGHT = value;
  }

  public int getMAP_WIDTH() {
    return MAP_WIDTH;
  }

  public void setMAP_WIDTH(final int value) {
    MAP_WIDTH = value;
  }

  public int getMAP_HEIGHT() {
    return MAP_HEIGHT;
  }

  public void setMAP_HEIGHT(final int value) {
    MAP_HEIGHT = value;
  }

}
