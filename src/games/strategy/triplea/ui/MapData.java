/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package games.strategy.triplea.ui;

import games.strategy.engine.data.*;
import games.strategy.triplea.Constants;
import games.strategy.util.PointFileReaderWriter;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;

/**
 * contains data about the territories useful for drawing
 */

public class MapData
{
    private final String DEFAULT_UNIT_SCALE_PROPERTY = "units.scale";
    private final String HAS_RELIEF_IMAGES = "map.hasRelief";
    private final String SHOW_CAPITOL_MARKERS = "map.showCapitolMarkers";
    
    private static final String CENTERS_FILE = "centers.txt";
    private static final String POLYGON_FILE = "polygons.txt";
    private static final String PLACEMENT_FILE = "place.txt";
    private static final String MAP_PROPERTIES = "map.properties";
    private static final String CAPITAL_MARKERS = "capitols.txt";
    private static final String VC_MARKERS = "vc.txt";
    private static final String IMPASSIBLE = "Impassible";


    //default colour if none is defined.
    private final List<Color> m_defaultColours = new ArrayList<Color>(Arrays.asList(new Color[]
    { Color.RED, Color.MAGENTA, Color.YELLOW, Color.ORANGE, Color.CYAN, Color.GREEN, Color.PINK, Color.GRAY }));

    //maps PlayerName as String to Color
    private Map<String, Color> m_playerColors = new HashMap<String, Color>();

    
    //maps String -> List of points
    private Map<String, List<Point>> m_place;

    //maps String -> Collection of Polygons
    private Map<String, List<Polygon>> m_polys;

    //maps String -> Point
    private Map<String,Point> m_centers;

    //maps String -> Point    
    private Map<String,Point> m_vcPlace;
    
    //maps String -> Point
    private Map<String,Point> m_capitolPlace;

    //maps String -> List of String
    private Map<String, List<String>> m_contains;

    private Properties m_mapProperties;

    
    public boolean scrollWrapX()
    {
        return Boolean.valueOf(m_mapProperties.getProperty("map.scrollWrapX", "true")).booleanValue();
    }
    
    /**
     * Constructor TerritoryData(java.lang.String)
     * 
     * Sets the map directory for this instance of TerritoryData
     * 
     * @param java.lang.String
     *            mapNameDir the given map directory
     *  
     */
    public MapData(String mapNameDir)
    {
        try
        {
            String prefix = Constants.MAP_DIR + mapNameDir + "/";
	
            m_place = PointFileReaderWriter.readOneToMany(this.getClass().getResourceAsStream(prefix + PLACEMENT_FILE));
            m_polys = PointFileReaderWriter.readOneToManyPolygons(this.getClass().getResourceAsStream(prefix + POLYGON_FILE));
            m_centers = PointFileReaderWriter.readOneToOne(this.getClass().getResourceAsStream(prefix + CENTERS_FILE));
            m_vcPlace = PointFileReaderWriter.readOneToOne(this.getClass().getResourceAsStream(prefix + VC_MARKERS));
            m_capitolPlace = PointFileReaderWriter.readOneToOne(this.getClass().getResourceAsStream(prefix + CAPITAL_MARKERS));
            m_mapProperties = new Properties();
            
            try
            {
                URL url = this.getClass().getResource(prefix + MAP_PROPERTIES);
                if(url == null)
                    throw new IllegalStateException("No map.properties file defined");
                m_mapProperties.load(url.openStream());
            }
            catch(Exception e)
            {
                System.out.println("Error reading map.properties:" + e);
            }

            initializeContains();
        } catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }

    public double getDefaultUnitScale()
    {
        
        if(m_mapProperties.getProperty(DEFAULT_UNIT_SCALE_PROPERTY) == null)
            return 1.0;
        
        try
        {
            return Double.parseDouble(m_mapProperties.getProperty(DEFAULT_UNIT_SCALE_PROPERTY));
        } catch (NumberFormatException e)
        {
            e.printStackTrace();
            return 1.0;
        }
    }

    public boolean getHasRelief()
    {
        return Boolean.valueOf(m_mapProperties.getProperty(HAS_RELIEF_IMAGES, "true")).booleanValue();
    }

    public boolean drawCapitolMarkers()
    {
        return Boolean.valueOf(m_mapProperties.getProperty(SHOW_CAPITOL_MARKERS, "true")).booleanValue();
    }
    
    
    private void initializeContains()
    {

        m_contains = new HashMap<String, List<String>>();

        Iterator<String> seaIter = getTerritories().iterator();
        while (seaIter.hasNext())
        {
            List<String> contained = new ArrayList<String>();
            String seaTerritory = seaIter.next();
            if (!seaTerritory.endsWith("Sea Zone"))
                continue;

            Iterator<String> landIter = getTerritories().iterator();
            while (landIter.hasNext())
            {
                String landTerritory = landIter.next();
                if (landTerritory.endsWith("Sea Zone"))
                    continue;

                Polygon landPoly = (Polygon) getPolygons(landTerritory).iterator().next();
                Polygon seaPoly = (Polygon) getPolygons(seaTerritory).iterator().next();
                if (seaPoly.contains(landPoly.getBounds()))
                {
                    contained.add(landTerritory);
                }
            }

            if (!contained.isEmpty())
                m_contains.put(seaTerritory, contained);
        }
    }

    
    public Color getPlayerColor(String playerName)
    {
        //already loaded, just return
        if(m_playerColors.containsKey(playerName))
            return m_playerColors.get(playerName);
        
        //look in map.properties
        String propertiesKey = "color." + playerName;
        if(m_mapProperties.getProperty(propertiesKey) != null)
        {
            String colorString = m_mapProperties.getProperty(propertiesKey);
            if(colorString.length() != 6)
            {
                throw new IllegalStateException("Player colors must be a 6 digit hex number, eg FF0011, not:" + colorString);
            }
            try
            {
                Integer colorInt = Integer.decode("0x" +colorString);
                Color color = new Color(colorInt.intValue());
                m_playerColors.put(playerName, color);
                return color;
            }
            catch(NumberFormatException nfe)
            {
                throw new IllegalStateException("Player colors must be a 6 digit hex number, eg FF0011");
            }
        }
        
        //dont crash, use one of our default colors
        //its ugly, but usable
        System.out.println("No color defined for " + playerName + ".  Edit map.properties in the map folder to set it");
        Color color = m_defaultColours.remove(0);
        m_playerColors.put(playerName, color);
        return color;

            
    }
    
    /**
     * returns the color for impassible territories
     */
    public Color impassibleColor()
    {
        // just use getPlayerColor, since it parses the properties 
        return getPlayerColor(IMPASSIBLE);
    }

    /**
     * 
     * @return a Set of territory names as Strings. generally this shouldnt be
     *         used, instead you should use aGameData.getMap().getTerritories()
     */
    public Set<String> getTerritories()
    {
        return m_polys.keySet();
    }

    /**
     * Does this territory have any territories contained within it
     */
    public boolean hasContainedTerritory(String territoryName)
    {
        return m_contains.containsKey(territoryName);
    }

    /**
     * returns the name of the territory contained in the given territory. This
     * applies to islands within sea zones.
     * 
     * @return possiblly null
     */
    public List getContainedTerritory(String territoryName)
    {
        return m_contains.get(territoryName);
    }

    public void verify(GameData data)
    {
        verifyKeys(data, m_centers, "centers");
        verifyKeys(data, m_polys, "polygons");
        verifyKeys(data, m_place, "place");
    }

    private void verifyKeys(GameData data, Map<String,?> aMap, String dataTypeForErrorMessage) throws IllegalStateException
    {
        StringBuilder errors = new StringBuilder();
        Iterator<String> iter = aMap.keySet().iterator();
        while (iter.hasNext())
        {
            String name = iter.next();
            Territory terr = data.getMap().getTerritory(name);
            if (terr == null)
                errors.append("Territory in file could not be found in game data for "
		               + dataTypeForErrorMessage + ". Territory name <" + name + ">\n");
        }

        Iterator territories = data.getMap().getTerritories().iterator();

        Set<String> keySet = aMap.keySet();
        while (territories.hasNext())
        {
            Territory terr = (Territory) territories.next();
            if (!keySet.contains(terr.getName()))
            {
                errors.append("No data of type " + dataTypeForErrorMessage + " for territory:" + terr.getName() + "\n");
            }
        }

        if (errors.length() > 0)
            throw new IllegalStateException(errors.toString());

    }

    public List<Point> getPlacementPoints(Territory terr)
    {
        return m_place.get(terr.getName());
    }

    public List<Polygon> getPolygons(String terr)
    {
        return m_polys.get(terr);
    }

    public List<Polygon> getPolygons(Territory terr)
    {
        return getPolygons(terr.getName());
    }

    public Point getCenter(String terr)
    {
        return new Point((Point) m_centers.get(terr));
    }

    public Point getCenter(Territory terr)
    {
        return getCenter(terr.getName());
    }
    
    public Point getCapitolMarkerLocation(Territory terr)
    {
        if(m_capitolPlace.containsKey(terr.getName()))
            return (Point) m_capitolPlace.get(terr.getName());
        return getCenter(terr);
    }

    public Point getVCPlacementPoint(Territory terr)
    {        
        if(m_vcPlace.containsKey(terr.getName()))
            return (Point) m_vcPlace.get(terr.getName());
        return getCenter(terr);
    }
    
    /**
     * Get the territory at the x,y co-ordinates could be null.
     */
    public String getTerritoryAt(int x, int y)
    {
        String seaName = null;

        //try to find a land territory.
        //sea zones often surround a land territory
        Iterator<String> keyIter = m_polys.keySet().iterator();
        while (keyIter.hasNext())
        {
            String name = keyIter.next();
            Collection polygons = m_polys.get(name);
            Iterator polyIter = polygons.iterator();
            while (polyIter.hasNext())
            {
                Polygon poly = (Polygon) polyIter.next();
                if (poly.contains(x, y))
                {
                    if (name.endsWith("Sea Zone"))
                    {
                        seaName = name;
                    } else
                        return name;
                }

            }
        }
        return seaName;

    }
    
    public Dimension getMapDimensions()
    {
        int width = Integer.parseInt(m_mapProperties.getProperty("map.width"));
        int height = Integer.parseInt(m_mapProperties.getProperty("map.height"));
        
        return new Dimension(width, height);
    }

    public Rectangle getBoundingRect(Territory terr)
    {
        String name = terr.getName();
        return getBoundingRect(name);
    }

    public Rectangle getBoundingRect(String name)
    {
        List polys = getPolygons(name);
        if (polys == null)
            throw new IllegalStateException("No polygons found for:" + name);

        Rectangle bounds = null;

        for (int i = 0; i < polys.size(); i++)
        {
            Polygon item = (Polygon) polys.get(i);
            if (bounds == null)
                bounds = item.getBounds();
            else
                bounds.add(item.getBounds());
        }

        return bounds;
    }

    /**
     * Get the territories that intersect or are bound by this shapes bounding
     * rect.
     * 
     * @return List of territory names as Strings
     */
    public List<String> territoriesThatOverlap(Rectangle2D bounds)
    {
        List<String> rVal = null;

        Iterator<String> terrIter = getTerritories().iterator();
        while (terrIter.hasNext())
        {
            String terr = terrIter.next();

            List polygons = getPolygons(terr);
            for (int i = 0; i < polygons.size(); i++)
            {
                Polygon item = (Polygon) polygons.get(i);
                if (item.intersects(bounds) || item.contains(bounds) || bounds.contains(item.getBounds2D()))
                {
                    if (rVal == null)
                        rVal = new ArrayList<String>(4);

                    rVal.add(terr);
                    //only add it once
                    break;
                }
            }
        }
        if (rVal == null)
            return Collections.emptyList();

        return rVal;

    }

}
