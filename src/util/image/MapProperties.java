package util.image;

import games.strategy.engine.data.properties.PropertiesUI;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.triplea.ui.MapData;
import games.strategy.util.Tuple;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * An object to hold all the map.properties values.
 * 
 * @author veqryn
 * 
 */
public class MapProperties
{
	public final String MAP_PROPERTIES_FILENAME = MapData.MAP_PROPERTIES;
	public Map<String, Color> COLOR_MAP = new TreeMap<String, Color>();
	// public double UNITS_SCALE = 0.75f;
	public String UNITS_SCALE = "0.75";
	public int UNITS_WIDTH = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
	public int UNITS_HEIGHT = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
	public int UNITS_COUNTER_OFFSET_WIDTH = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE / 4;
	public int UNITS_COUNTER_OFFSET_HEIGHT = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
	public int UNITS_STACK_SIZE = 0;
	public int MAP_WIDTH = 256;
	public int MAP_HEIGHT = 256;
	public boolean MAP_SCROLLWRAPX = true;
	public boolean MAP_SCROLLWRAPY = false;
	public boolean MAP_HASRELIEF = true;
	public int MAP_CURSOR_HOTSPOT_X = 0;
	public int MAP_CURSOR_HOTSPOT_Y = 0;
	public boolean MAP_SHOWCAPITOLMARKERS = true;
	public boolean MAP_USETERRITORYEFFECTMARKERS = false;
	public boolean MAP_SHOWTERRITORYNAMES = true;
	public boolean MAP_SHOWRESOURCES = true;
	public boolean MAP_SHOWCOMMENTS = true;
	public boolean MAP_SHOWSEAZONENAMES = false;
	public boolean MAP_DRAWNAMESFROMTOPLEFT = false;
	public boolean MAP_USENATION_CONVOYFLAGS = false;
	// public List<String> DONT_DRAW_TERRITORY_NAMES = new ArrayList<String>();
	public String DONT_DRAW_TERRITORY_NAMES = "";
	public boolean MAP_MAPBLENDS = false;
	public String MAP_MAPBLENDMODE = "OVERLAY"; // options are: NORMAL, OVERLAY, LINEAR_LIGHT, DIFFERENCE, MULTIPLY
	// public double MAP_MAPBLENDALPHA = 0.3f;
	public String MAP_MAPBLENDALPHA = "0.3";
	public boolean SCREENSHOT_TITLE_ENABLED = true;
	public int SCREENSHOT_TITLE_X = 50;
	public int SCREENSHOT_TITLE_Y = 50;
	public Color SCREENSHOT_TITLE_COLOR = Color.black;// new Color(0x000000);
	public int SCREENSHOT_TITLE_FONT_SIZE = 20;
	public boolean SCREENSHOT_STATS_ENABLED = true;
	public int SCREENSHOT_STATS_X = 50;
	public int SCREENSHOT_STATS_Y = 54;
	public Color SCREENSHOT_STATS_TEXT_COLOR = Color.black;// new Color(0x000000);
	public Color SCREENSHOT_STATS_BORDER_COLOR = Color.white;// new Color(0xFFFFFF);
	
	public MapProperties()
	{
		super();
		// fill the color map
		COLOR_MAP.put("Americans", new Color(0x666600));
		COLOR_MAP.put("Australians", new Color(0xCCCC00));
		COLOR_MAP.put("British", new Color(0x916400));
		COLOR_MAP.put("Canadians", new Color(0xDBBE7F));
		COLOR_MAP.put("Chinese", new Color(0x663E66));
		COLOR_MAP.put("French", new Color(0x113A77));
		COLOR_MAP.put("Germans", new Color(0x777777));
		COLOR_MAP.put("Italians", new Color(0x0B7282));
		COLOR_MAP.put("Japanese", new Color(0xFFD400));
		COLOR_MAP.put("Puppet_States", new Color(0x1B5DA0));
		COLOR_MAP.put("Russians", new Color(0xB23B00));
		COLOR_MAP.put("Neutral", new Color(0xE2A071));
		COLOR_MAP.put("Impassible", new Color(0xD8BA7C));
	}
	
	@SuppressWarnings("rawtypes")
	public Tuple<PropertiesUI, List<MapPropertyWrapper>> propertyWrapperUI(final boolean editable)
	{
		return MapPropertyWrapper.createPropertiesUI(this, editable);
	}
	
	@SuppressWarnings("rawtypes")
	public void writePropertiesToObject(final List<MapPropertyWrapper> properties)
	{
		MapPropertyWrapper.writePropertiesToObject(this, properties);
	}
	
	public Map<String, Color> getCOLOR_MAP()
	{
		return COLOR_MAP;
	}
	
	public void setCOLOR_MAP(final Map<String, Color> value)
	{
		COLOR_MAP = value;
	}
	
	public String outCOLOR_MAP()
	{
		final StringBuilder buf = new StringBuilder();
		for (final Entry<String, Color> entry : COLOR_MAP.entrySet())
		{
			buf.append(MapData.PROPERTY_COLOR_PREFIX + entry.getKey() + "=" + colorToHex(entry.getValue()) + "\r\n");
		}
		return buf.toString();
	}
	
	public static String colorToHex(final Color color)
	{
		String hexString = Integer.toHexString(color.getRGB() & 0x00FFFFFF);
		while (hexString.length() < 6)
			hexString = "0" + hexString;
		return hexString;
	}
	
	/*
	public double getUNITS_SCALE()
	{
		return UNITS_SCALE;
	}
	
	public void setUNITS_SCALE(final double value)
	{
		UNITS_SCALE = value;
	}*/
	public String getUNITS_SCALE()
	{
		return UNITS_SCALE;
	}
	
	public void setUNITS_SCALE(final String value)
	{
		final double dvalue = Math.max(0.0, Math.min(2.0, Double.parseDouble(value)));
		if (Math.abs(1.25 - dvalue) < 0.01)
			UNITS_SCALE = "1.25";
		else if (Math.abs(1.0 - dvalue) < 0.01)
			UNITS_SCALE = "1.0";
		else if (Math.abs(0.875 - dvalue) < 0.01)
			UNITS_SCALE = "0.875";
		else if (Math.abs(0.8333 - dvalue) < 0.01)
			UNITS_SCALE = "0.8333";
		else if (Math.abs(0.75 - dvalue) < 0.01)
			UNITS_SCALE = "0.75";
		else if (Math.abs(0.6666 - dvalue) < 0.01)
			UNITS_SCALE = "0.6666";
		else if (Math.abs(0.5625 - dvalue) < 0.01)
			UNITS_SCALE = "0.5625";
		else if (Math.abs(0.5 - dvalue) < 0.01)
			UNITS_SCALE = "0.5";
		else
			UNITS_SCALE = "" + dvalue;
	}
	
	public String outUNITS_SCALE()
	{
		return MapData.PROPERTY_UNITS_SCALE + "=" + UNITS_SCALE + "\r\n";
	}
	
	public int getUNITS_WIDTH()
	{
		return UNITS_WIDTH;
	}
	
	public void setUNITS_WIDTH(final int value)
	{
		UNITS_WIDTH = value;
	}
	
	public String outUNITS_WIDTH()
	{
		return MapData.PROPERTY_UNITS_WIDTH + "=" + UNITS_WIDTH + "\r\n";
	}
	
	public int getUNITS_HEIGHT()
	{
		return UNITS_HEIGHT;
	}
	
	public void setUNITS_HEIGHT(final int value)
	{
		UNITS_HEIGHT = value;
	}
	
	public String outUNITS_HEIGHT()
	{
		return MapData.PROPERTY_UNITS_HEIGHT + "=" + UNITS_HEIGHT + "\r\n";
	}
	
	public int getUNITS_COUNTER_OFFSET_WIDTH()
	{
		return UNITS_COUNTER_OFFSET_WIDTH;
	}
	
	public void setUNITS_COUNTER_OFFSET_WIDTH(final int value)
	{
		UNITS_COUNTER_OFFSET_WIDTH = value;
	}
	
	public String outUNITS_COUNTER_OFFSET_WIDTH()
	{
		return MapData.PROPERTY_UNITS_COUNTER_OFFSET_WIDTH + "=" + UNITS_COUNTER_OFFSET_WIDTH + "\r\n";
	}
	
	public int getUNITS_COUNTER_OFFSET_HEIGHT()
	{
		return UNITS_COUNTER_OFFSET_HEIGHT;
	}
	
	public void setUNITS_COUNTER_OFFSET_HEIGHT(final int value)
	{
		UNITS_COUNTER_OFFSET_HEIGHT = value;
	}
	
	public String outUNITS_COUNTER_OFFSET_HEIGHT()
	{
		return MapData.PROPERTY_UNITS_COUNTER_OFFSET_HEIGHT + "=" + UNITS_COUNTER_OFFSET_HEIGHT + "\r\n";
	}
	
	public int getUNITS_STACK_SIZE()
	{
		return UNITS_STACK_SIZE;
	}
	
	public void setUNITS_STACK_SIZE(final int value)
	{
		UNITS_STACK_SIZE = value;
	}
	
	public String outUNITS_STACK_SIZE()
	{
		return MapData.PROPERTY_UNITS_STACK_SIZE + "=" + UNITS_STACK_SIZE + "\r\n";
	}
	
	public int getMAP_WIDTH()
	{
		return MAP_WIDTH;
	}
	
	public void setMAP_WIDTH(final int value)
	{
		MAP_WIDTH = value;
	}
	
	public String outMAP_WIDTH()
	{
		return MapData.PROPERTY_MAP_WIDTH + "=" + MAP_WIDTH + "\r\n";
	}
	
	public int getMAP_HEIGHT()
	{
		return MAP_HEIGHT;
	}
	
	public void setMAP_HEIGHT(final int value)
	{
		MAP_HEIGHT = value;
	}
	
	public String outMAP_HEIGHT()
	{
		return MapData.PROPERTY_MAP_HEIGHT + "=" + MAP_HEIGHT + "\r\n";
	}
	
	public boolean getMAP_SCROLLWRAPX()
	{
		return MAP_SCROLLWRAPX;
	}
	
	public void setMAP_SCROLLWRAPX(final boolean value)
	{
		MAP_SCROLLWRAPX = value;
	}
	
	public String outMAP_SCROLLWRAPX()
	{
		return MapData.PROPERTY_MAP_SCROLLWRAPX + "=" + MAP_SCROLLWRAPX + "\r\n";
	}
	
	public boolean getMAP_SCROLLWRAPY()
	{
		return MAP_SCROLLWRAPY;
	}
	
	public void setMAP_SCROLLWRAPY(final boolean value)
	{
		MAP_SCROLLWRAPY = value;
	}
	
	public String outMAP_SCROLLWRAPY()
	{
		return MapData.PROPERTY_MAP_SCROLLWRAPY + "=" + MAP_SCROLLWRAPY + "\r\n";
	}
	
	public boolean getMAP_HASRELIEF()
	{
		return MAP_HASRELIEF;
	}
	
	public void setMAP_HASRELIEF(final boolean value)
	{
		MAP_HASRELIEF = value;
	}
	
	public String outMAP_HASRELIEF()
	{
		return MapData.PROPERTY_MAP_HASRELIEF + "=" + MAP_HASRELIEF + "\r\n";
	}
	
	public int getMAP_CURSOR_HOTSPOT_X()
	{
		return MAP_CURSOR_HOTSPOT_X;
	}
	
	public void setMAP_CURSOR_HOTSPOT_X(final int value)
	{
		MAP_CURSOR_HOTSPOT_X = value;
	}
	
	public String outMAP_CURSOR_HOTSPOT_X()
	{
		return MapData.PROPERTY_MAP_CURSOR_HOTSPOT_X + "=" + MAP_CURSOR_HOTSPOT_X + "\r\n";
	}
	
	public int getMAP_CURSOR_HOTSPOT_Y()
	{
		return MAP_CURSOR_HOTSPOT_Y;
	}
	
	public void setMAP_CURSOR_HOTSPOT_Y(final int value)
	{
		MAP_CURSOR_HOTSPOT_Y = value;
	}
	
	public String outMAP_CURSOR_HOTSPOT_Y()
	{
		return MapData.PROPERTY_MAP_CURSOR_HOTSPOT_Y + "=" + MAP_CURSOR_HOTSPOT_Y + "\r\n";
	}
	
	public boolean getMAP_SHOWCAPITOLMARKERS()
	{
		return MAP_SHOWCAPITOLMARKERS;
	}
	
	public void setMAP_SHOWCAPITOLMARKERS(final boolean value)
	{
		MAP_SHOWCAPITOLMARKERS = value;
	}
	
	public String outMAP_SHOWCAPITOLMARKERS()
	{
		return MapData.PROPERTY_MAP_SHOWCAPITOLMARKERS + "=" + MAP_SHOWCAPITOLMARKERS + "\r\n";
	}
	
	public boolean getMAP_USETERRITORYEFFECTMARKERS()
	{
		return MAP_USETERRITORYEFFECTMARKERS;
	}
	
	public void setMAP_USETERRITORYEFFECTMARKERS(final boolean value)
	{
		MAP_USETERRITORYEFFECTMARKERS = value;
	}
	
	public String outMAP_USETERRITORYEFFECTMARKERS()
	{
		return MapData.PROPERTY_MAP_USETERRITORYEFFECTMARKERS + "=" + MAP_USETERRITORYEFFECTMARKERS + "\r\n";
	}
	
	public boolean getMAP_SHOWTERRITORYNAMES()
	{
		return MAP_SHOWTERRITORYNAMES;
	}
	
	public void setMAP_SHOWTERRITORYNAMES(final boolean value)
	{
		MAP_SHOWTERRITORYNAMES = value;
	}
	
	public String outMAP_SHOWTERRITORYNAMES()
	{
		return MapData.PROPERTY_MAP_SHOWTERRITORYNAMES + "=" + MAP_SHOWTERRITORYNAMES + "\r\n";
	}
	
	public boolean getMAP_SHOWRESOURCES()
	{
		return MAP_SHOWRESOURCES;
	}
	
	public void setMAP_SHOWRESOURCES(final boolean value)
	{
		MAP_SHOWRESOURCES = value;
	}
	
	public String outMAP_SHOWRESOURCES()
	{
		return MapData.PROPERTY_MAP_SHOWRESOURCES + "=" + MAP_SHOWRESOURCES + "\r\n";
	}
	
	public boolean getMAP_SHOWCOMMENTS()
	{
		return MAP_SHOWCOMMENTS;
	}
	
	public void setMAP_SHOWCOMMENTS(final boolean value)
	{
		MAP_SHOWCOMMENTS = value;
	}
	
	public String outMAP_SHOWCOMMENTS()
	{
		return MapData.PROPERTY_MAP_SHOWCOMMENTS + "=" + MAP_SHOWCOMMENTS + "\r\n";
	}
	
	public boolean getMAP_SHOWSEAZONENAMES()
	{
		return MAP_SHOWSEAZONENAMES;
	}
	
	public void setMAP_SHOWSEAZONENAMES(final boolean value)
	{
		MAP_SHOWSEAZONENAMES = value;
	}
	
	public String outMAP_SHOWSEAZONENAMES()
	{
		return MapData.PROPERTY_MAP_SHOWSEAZONENAMES + "=" + MAP_SHOWSEAZONENAMES + "\r\n";
	}
	
	public boolean getMAP_DRAWNAMESFROMTOPLEFT()
	{
		return MAP_DRAWNAMESFROMTOPLEFT;
	}
	
	public void setMAP_DRAWNAMESFROMTOPLEFT(final boolean value)
	{
		MAP_DRAWNAMESFROMTOPLEFT = value;
	}
	
	public String outMAP_DRAWNAMESFROMTOPLEFT()
	{
		return MapData.PROPERTY_MAP_DRAWNAMESFROMTOPLEFT + "=" + MAP_DRAWNAMESFROMTOPLEFT + "\r\n";
	}
	
	public boolean getMAP_USENATION_CONVOYFLAGS()
	{
		return MAP_USENATION_CONVOYFLAGS;
	}
	
	public void setMAP_USENATION_CONVOYFLAGS(final boolean value)
	{
		MAP_USENATION_CONVOYFLAGS = value;
	}
	
	public String outMAP_USENATION_CONVOYFLAGS()
	{
		return MapData.PROPERTY_MAP_USENATION_CONVOYFLAGS + "=" + MAP_USENATION_CONVOYFLAGS + "\r\n";
	}
	
	/*
	public List<String> getDONT_DRAW_TERRITORY_NAMES()
	{
		return DONT_DRAW_TERRITORY_NAMES;
	}
	
	public void setDONT_DRAW_TERRITORY_NAMES(final List<String> value)
	{
		DONT_DRAW_TERRITORY_NAMES = value;
	}*/
	
	public String getDONT_DRAW_TERRITORY_NAMES()
	{
		return DONT_DRAW_TERRITORY_NAMES;
	}
	
	public void setDONT_DRAW_TERRITORY_NAMES(final String value)
	{
		DONT_DRAW_TERRITORY_NAMES = value;
	}
	
	public String outDONT_DRAW_TERRITORY_NAMES()
	{
		return MapData.PROPERTY_DONT_DRAW_TERRITORY_NAMES + "=" + DONT_DRAW_TERRITORY_NAMES + "\r\n";
	}
	
	public boolean getMAP_MAPBLENDS()
	{
		return MAP_MAPBLENDS;
	}
	
	public void setMAP_MAPBLENDS(final boolean value)
	{
		MAP_MAPBLENDS = value;
	}
	
	public String outMAP_MAPBLENDS()
	{
		return MapData.PROPERTY_MAP_MAPBLENDS + "=" + MAP_MAPBLENDS + "\r\n";
	}
	
	public String getMAP_MAPBLENDMODE()
	{
		return MAP_MAPBLENDMODE;
	}
	
	public void setMAP_MAPBLENDMODE(final String value)
	{
		MAP_MAPBLENDMODE = value;
	}
	
	public String outMAP_MAPBLENDMODE()
	{
		return MapData.PROPERTY_MAP_MAPBLENDMODE + "=" + MAP_MAPBLENDMODE + "\r\n";
	}
	
	/*
	public double getMAP_MAPBLENDALPHA()
	{
		return MAP_MAPBLENDALPHA;
	}
	
	public void setMAP_MAPBLENDALPHA(final double value)
	{
		MAP_MAPBLENDALPHA = value;
	}*/
	
	public String getMAP_MAPBLENDALPHA()
	{
		return MAP_MAPBLENDALPHA;
	}
	
	public void setMAP_MAPBLENDALPHA(final String value)
	{
		Double.parseDouble(value);
		MAP_MAPBLENDALPHA = value;
	}
	
	public String outMAP_MAPBLENDALPHA()
	{
		return MapData.PROPERTY_MAP_MAPBLENDALPHA + "=" + MAP_MAPBLENDALPHA + "\r\n";
	}
	
	public boolean getSCREENSHOT_TITLE_ENABLED()
	{
		return SCREENSHOT_TITLE_ENABLED;
	}
	
	public void setSCREENSHOT_TITLE_ENABLED(final boolean value)
	{
		SCREENSHOT_TITLE_ENABLED = value;
	}
	
	public String outSCREENSHOT_TITLE_ENABLED()
	{
		return MapData.PROPERTY_SCREENSHOT_TITLE_ENABLED + "=" + SCREENSHOT_TITLE_ENABLED + "\r\n";
	}
	
	public int getSCREENSHOT_TITLE_X()
	{
		return SCREENSHOT_TITLE_X;
	}
	
	public void setSCREENSHOT_TITLE_X(final int value)
	{
		SCREENSHOT_TITLE_X = value;
	}
	
	public String outSCREENSHOT_TITLE_X()
	{
		return MapData.PROPERTY_SCREENSHOT_TITLE_X + "=" + SCREENSHOT_TITLE_X + "\r\n";
	}
	
	public int getSCREENSHOT_TITLE_Y()
	{
		return SCREENSHOT_TITLE_Y;
	}
	
	public void setSCREENSHOT_TITLE_Y(final int value)
	{
		SCREENSHOT_TITLE_Y = value;
	}
	
	public String outSCREENSHOT_TITLE_Y()
	{
		return MapData.PROPERTY_SCREENSHOT_TITLE_Y + "=" + SCREENSHOT_TITLE_Y + "\r\n";
	}
	
	public Color getSCREENSHOT_TITLE_COLOR()
	{
		return SCREENSHOT_TITLE_COLOR;
	}
	
	public void setSCREENSHOT_TITLE_COLOR(final Color value)
	{
		SCREENSHOT_TITLE_COLOR = value;
	}
	
	public String outSCREENSHOT_TITLE_COLOR()
	{
		return MapData.PROPERTY_SCREENSHOT_TITLE_COLOR + "=" + colorToHex(SCREENSHOT_TITLE_COLOR) + "\r\n";
	}
	
	public int getSCREENSHOT_TITLE_FONT_SIZE()
	{
		return SCREENSHOT_TITLE_FONT_SIZE;
	}
	
	public void setSCREENSHOT_TITLE_FONT_SIZE(final int value)
	{
		SCREENSHOT_TITLE_FONT_SIZE = value;
	}
	
	public String outSCREENSHOT_TITLE_FONT_SIZE()
	{
		return MapData.PROPERTY_SCREENSHOT_TITLE_FONT_SIZE + "=" + SCREENSHOT_TITLE_FONT_SIZE + "\r\n";
	}
	
	public boolean getSCREENSHOT_STATS_ENABLED()
	{
		return SCREENSHOT_STATS_ENABLED;
	}
	
	public void setSCREENSHOT_STATS_ENABLED(final boolean value)
	{
		SCREENSHOT_STATS_ENABLED = value;
	}
	
	public String outSCREENSHOT_STATS_ENABLED()
	{
		return MapData.PROPERTY_SCREENSHOT_STATS_ENABLED + "=" + SCREENSHOT_STATS_ENABLED + "\r\n";
	}
	
	public int getSCREENSHOT_STATS_X()
	{
		return SCREENSHOT_STATS_X;
	}
	
	public void setSCREENSHOT_STATS_X(final int value)
	{
		SCREENSHOT_STATS_X = value;
	}
	
	public String outSCREENSHOT_STATS_X()
	{
		return MapData.PROPERTY_SCREENSHOT_STATS_X + "=" + SCREENSHOT_STATS_X + "\r\n";
	}
	
	public int getSCREENSHOT_STATS_Y()
	{
		return SCREENSHOT_STATS_Y;
	}
	
	public void setSCREENSHOT_STATS_Y(final int value)
	{
		SCREENSHOT_STATS_Y = value;
	}
	
	public String outSCREENSHOT_STATS_Y()
	{
		return MapData.PROPERTY_SCREENSHOT_STATS_Y + "=" + SCREENSHOT_STATS_Y + "\r\n";
	}
	
	public Color getSCREENSHOT_STATS_TEXT_COLOR()
	{
		return SCREENSHOT_STATS_TEXT_COLOR;
	}
	
	public void setSCREENSHOT_STATS_TEXT_COLOR(final Color value)
	{
		SCREENSHOT_STATS_TEXT_COLOR = value;
	}
	
	public String outSCREENSHOT_STATS_TEXT_COLOR()
	{
		return MapData.PROPERTY_SCREENSHOT_STATS_TEXT_COLOR + "=" + colorToHex(SCREENSHOT_STATS_TEXT_COLOR) + "\r\n";
	}
	
	public Color getSCREENSHOT_STATS_BORDER_COLOR()
	{
		return SCREENSHOT_STATS_BORDER_COLOR;
	}
	
	public void setSCREENSHOT_STATS_BORDER_COLOR(final Color value)
	{
		SCREENSHOT_STATS_BORDER_COLOR = value;
	}
	
	public String outSCREENSHOT_STATS_BORDER_COLOR()
	{
		return MapData.PROPERTY_SCREENSHOT_STATS_BORDER_COLOR + "=" + colorToHex(SCREENSHOT_STATS_BORDER_COLOR) + "\r\n";
	}
}
