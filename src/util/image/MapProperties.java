package util.image;

import games.strategy.triplea.image.UnitImageFactory;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An object to hold all the map.properties values.
 * 
 * @author veqryn
 * 
 */
public class MapProperties
{
	private Map<String, Color> COLOR_MAP = new HashMap<String, Color>();
	private double UNITS_SCALE = 0.75f;
	private int UNITS_WIDTH = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
	private int UNITS_HEIGHT = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
	private int UNITS_COUNTER_OFFSET_WIDTH = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE / 4;
	private int UNITS_COUNTER_OFFSET_HEIGHT = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
	private int MAP_WIDTH = 256;
	private int MAP_HEIGHT = 256;
	private boolean MAP_SCROLLWRAPX = true;
	private boolean MAP_SCROLLWRAPY = false;
	private boolean MAP_HASRELIEF = true;
	private boolean MAP_SHOWCAPITOLMARKERS = true;
	private boolean MAP_USETERRITORYEFFECTMARKERS = false;
	private boolean MAP_SHOWTERRITORYNAMES = true;
	private boolean MAP_SHOWRESOURCES = true;
	private boolean MAP_SHOWCONVOYNAMES = true;
	private boolean MAP_USENATION_CONVOYFLAGS = false;
	private List<String> DONT_DRAW_TERRITORY_NAMES = new ArrayList<String>();
	private boolean MAP_MAPBLENDS = false;
	private String MAP_MAPBLENDMODE = "OVERLAY"; // options are: NORMAL, OVERLAY, LINEAR_LIGHT, DIFFERENCE, MULTIPLY
	private double MAP_MAPBLENDALPHA = 0.3f;
	private boolean SCREENSHOT_TITLE_ENABLED = true;
	private int SCREENSHOT_TITLE_X = 100;
	private int SCREENSHOT_TITLE_Y = 50;
	private Color SCREENSHOT_TITLE_COLOR = new Color(0x000000);
	private int SCREENSHOT_TITLE_FONT_SIZE = 20;
	private boolean SCREENSHOT_STATS_ENABLED = true;
	private int SCREENSHOT_STATS_X = 120;
	private int SCREENSHOT_STATS_Y = 1800;
	private Color SCREENSHOT_STATS_TEXT_COLOR = new Color(0x000000);
	private Color SCREENSHOT_STATS_BORDER_COLOR = new Color(0xFFFFFF);
	
	public MapProperties()
	{
		super();
	}
	
	public Map<String, Color> getCOLOR_MAP()
	{
		return COLOR_MAP;
	}
	
	public void setCOLOR_MAP(final Map<String, Color> value)
	{
		COLOR_MAP = value;
	}
	
	public double getUNITS_SCALE()
	{
		return UNITS_SCALE;
	}
	
	public void setUNITS_SCALE(final double value)
	{
		UNITS_SCALE = value;
	}
	
	public int getUNITS_WIDTH()
	{
		return UNITS_WIDTH;
	}
	
	public void setUNITS_WIDTH(final int value)
	{
		UNITS_WIDTH = value;
	}
	
	public int getUNITS_HEIGHT()
	{
		return UNITS_HEIGHT;
	}
	
	public void setUNITS_HEIGHT(final int value)
	{
		UNITS_HEIGHT = value;
	}
	
	public int getUNITS_COUNTER_OFFSET_WIDTH()
	{
		return UNITS_COUNTER_OFFSET_WIDTH;
	}
	
	public void setUNITS_COUNTER_OFFSET_WIDTH(final int value)
	{
		UNITS_COUNTER_OFFSET_WIDTH = value;
	}
	
	public int getUNITS_COUNTER_OFFSET_HEIGHT()
	{
		return UNITS_COUNTER_OFFSET_HEIGHT;
	}
	
	public void setUNITS_COUNTER_OFFSET_HEIGHT(final int value)
	{
		UNITS_COUNTER_OFFSET_HEIGHT = value;
	}
	
	public int getMAP_WIDTH()
	{
		return MAP_WIDTH;
	}
	
	public void setMAP_WIDTH(final int value)
	{
		MAP_WIDTH = value;
	}
	
	public int getMAP_HEIGHT()
	{
		return MAP_HEIGHT;
	}
	
	public void setMAP_HEIGHT(final int value)
	{
		MAP_HEIGHT = value;
	}
	
	public boolean isMAP_SCROLLWRAPX()
	{
		return MAP_SCROLLWRAPX;
	}
	
	public void setMAP_SCROLLWRAPX(final boolean value)
	{
		MAP_SCROLLWRAPX = value;
	}
	
	public boolean isMAP_SCROLLWRAPY()
	{
		return MAP_SCROLLWRAPY;
	}
	
	public void setMAP_SCROLLWRAPY(final boolean value)
	{
		MAP_SCROLLWRAPY = value;
	}
	
	public boolean isMAP_HASRELIEF()
	{
		return MAP_HASRELIEF;
	}
	
	public void setMAP_HASRELIEF(final boolean value)
	{
		MAP_HASRELIEF = value;
	}
	
	public boolean isMAP_SHOWCAPITOLMARKERS()
	{
		return MAP_SHOWCAPITOLMARKERS;
	}
	
	public void setMAP_SHOWCAPITOLMARKERS(final boolean value)
	{
		MAP_SHOWCAPITOLMARKERS = value;
	}
	
	public boolean isMAP_USETERRITORYEFFECTMARKERS()
	{
		return MAP_USETERRITORYEFFECTMARKERS;
	}
	
	public void setMAP_USETERRITORYEFFECTMARKERS(final boolean value)
	{
		MAP_USETERRITORYEFFECTMARKERS = value;
	}
	
	public boolean isMAP_SHOWTERRITORYNAMES()
	{
		return MAP_SHOWTERRITORYNAMES;
	}
	
	public void setMAP_SHOWTERRITORYNAMES(final boolean value)
	{
		MAP_SHOWTERRITORYNAMES = value;
	}
	
	public boolean isMAP_SHOWRESOURCES()
	{
		return MAP_SHOWRESOURCES;
	}
	
	public void setMAP_SHOWRESOURCES(final boolean value)
	{
		MAP_SHOWRESOURCES = value;
	}
	
	public boolean isMAP_SHOWCONVOYNAMES()
	{
		return MAP_SHOWCONVOYNAMES;
	}
	
	public void setMAP_SHOWCONVOYNAMES(final boolean value)
	{
		MAP_SHOWCONVOYNAMES = value;
	}
	
	public boolean isMAP_USENATION_CONVOYFLAGS()
	{
		return MAP_USENATION_CONVOYFLAGS;
	}
	
	public void setMAP_USENATION_CONVOYFLAGS(final boolean value)
	{
		MAP_USENATION_CONVOYFLAGS = value;
	}
	
	public List<String> getDONT_DRAW_TERRITORY_NAMES()
	{
		return DONT_DRAW_TERRITORY_NAMES;
	}
	
	public void setDONT_DRAW_TERRITORY_NAMES(final List<String> value)
	{
		DONT_DRAW_TERRITORY_NAMES = value;
	}
	
	public boolean isMAP_MAPBLENDS()
	{
		return MAP_MAPBLENDS;
	}
	
	public void setMAP_MAPBLENDS(final boolean value)
	{
		MAP_MAPBLENDS = value;
	}
	
	public String getMAP_MAPBLENDMODE()
	{
		return MAP_MAPBLENDMODE;
	}
	
	public void setMAP_MAPBLENDMODE(final String value)
	{
		MAP_MAPBLENDMODE = value;
	}
	
	public double getMAP_MAPBLENDALPHA()
	{
		return MAP_MAPBLENDALPHA;
	}
	
	public void setMAP_MAPBLENDALPHA(final double value)
	{
		MAP_MAPBLENDALPHA = value;
	}
	
	public boolean isSCREENSHOT_TITLE_ENABLED()
	{
		return SCREENSHOT_TITLE_ENABLED;
	}
	
	public void setSCREENSHOT_TITLE_ENABLED(final boolean value)
	{
		SCREENSHOT_TITLE_ENABLED = value;
	}
	
	public int getSCREENSHOT_TITLE_X()
	{
		return SCREENSHOT_TITLE_X;
	}
	
	public void setSCREENSHOT_TITLE_X(final int value)
	{
		SCREENSHOT_TITLE_X = value;
	}
	
	public int getSCREENSHOT_TITLE_Y()
	{
		return SCREENSHOT_TITLE_Y;
	}
	
	public void setSCREENSHOT_TITLE_Y(final int value)
	{
		SCREENSHOT_TITLE_Y = value;
	}
	
	public Color getSCREENSHOT_TITLE_COLOR()
	{
		return SCREENSHOT_TITLE_COLOR;
	}
	
	public void setSCREENSHOT_TITLE_COLOR(final Color value)
	{
		SCREENSHOT_TITLE_COLOR = value;
	}
	
	public int getSCREENSHOT_TITLE_FONT_SIZE()
	{
		return SCREENSHOT_TITLE_FONT_SIZE;
	}
	
	public void setSCREENSHOT_TITLE_FONT_SIZE(final int value)
	{
		SCREENSHOT_TITLE_FONT_SIZE = value;
	}
	
	public boolean isSCREENSHOT_STATS_ENABLED()
	{
		return SCREENSHOT_STATS_ENABLED;
	}
	
	public void setSCREENSHOT_STATS_ENABLED(final boolean value)
	{
		SCREENSHOT_STATS_ENABLED = value;
	}
	
	public int getSCREENSHOT_STATS_X()
	{
		return SCREENSHOT_STATS_X;
	}
	
	public void setSCREENSHOT_STATS_X(final int value)
	{
		SCREENSHOT_STATS_X = value;
	}
	
	public int getSCREENSHOT_STATS_Y()
	{
		return SCREENSHOT_STATS_Y;
	}
	
	public void setSCREENSHOT_STATS_Y(final int value)
	{
		SCREENSHOT_STATS_Y = value;
	}
	
	public Color getSCREENSHOT_STATS_TEXT_COLOR()
	{
		return SCREENSHOT_STATS_TEXT_COLOR;
	}
	
	public void setSCREENSHOT_STATS_TEXT_COLOR(final Color value)
	{
		SCREENSHOT_STATS_TEXT_COLOR = value;
	}
	
	public Color getSCREENSHOT_STATS_BORDER_COLOR()
	{
		return SCREENSHOT_STATS_BORDER_COLOR;
	}
	
	public void setSCREENSHOT_STATS_BORDER_COLOR(final Color value)
	{
		SCREENSHOT_STATS_BORDER_COLOR = value;
	}
}
