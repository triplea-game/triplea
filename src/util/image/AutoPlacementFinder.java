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

package util.image;

import java.util.*;
import java.awt.*;
import java.util.List;
import games.strategy.triplea.ui.*;
import java.awt.geom.*;
import games.strategy.util.*;
import java.io.*;

public class AutoPlacementFinder
{

    private static final int PLACEWIDTH = 46;
    private static final int PLACEHEIGHT = 46;

    
    
    static void calculate()
    {
        Map m_placements = new HashMap();

        TerritoryData.setFourthEdition(true);
        
        Iterator terrIter = TerritoryData.getInstance().getTerritories().iterator();
        while (terrIter.hasNext())
        {
            String name = (String)terrIter.next();

            Collection points;
            if(TerritoryData.getInstance().hasContainedTerritory(name))
            {
                Set containedPolygons = new HashSet();
                Iterator containedIter = TerritoryData.getInstance().getContainedTerritory(name).iterator();
                while (containedIter.hasNext()) {
                    String containedName = (String)containedIter.next();
                    containedPolygons.addAll(TerritoryData.getInstance().getPolygons(containedName));
                }

                points = getPlacementsStartingAtTopLeft(TerritoryData.getInstance().getPolygons(name),
                                                        TerritoryData.getInstance().getBoundingRect(name),
                                                        TerritoryData.getInstance().getCenter(name),
                                                        containedPolygons);
                m_placements.put(name, points);

            }
            else
            {
                points = getPlacementsStartingAtMiddle(TerritoryData.getInstance().getPolygons(name),
                    TerritoryData.getInstance().getBoundingRect(name),
                        TerritoryData.getInstance().getCenter(name));
                    m_placements.put(name, points);

            }


            System.out.println(name + ":" + points.size());


        }

        try
        {
            PointFileReaderWriter.writeOneToMany(new FileOutputStream("place.txt"), m_placements);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    static List getPlacementsStartingAtMiddle(Collection countryPolygons, Rectangle bounding, Point center)
    {
        List placementRects = new ArrayList();
        List placementPoints = new ArrayList();

        Rectangle2D place = new Rectangle2D.Double(center.x, center.y, PLACEHEIGHT, PLACEWIDTH);
        int x = center.x - (PLACEHEIGHT / 2);
        int y = center.y - (PLACEWIDTH / 2);
        int step = 1;

        for(int i = 0; i < 2 * Math.max(bounding.width , bounding.height ); i++)
        {
            for(int j = 0; j < Math.abs(step); j++)
            {
                if (step > 0)
                    x++;
                else
                    x--;
                isPlacement(countryPolygons, Collections.EMPTY_SET, placementRects, placementPoints, place, x, y);
            }
            for(int j = 0; j < Math.abs(step); j++)
            {
                if (step > 0)
                    y++;
                else
                    y--;
                isPlacement(countryPolygons, Collections.EMPTY_SET, placementRects, placementPoints, place, x, y);
            }

            step = -step;
            if (step > 0)
                step++;
            else
                step--;

//            System.out.println("x:" + x + " y:" + y);
        }


        if(placementPoints.isEmpty())
        {
            int defaultx =center.x - (PLACEHEIGHT / 2);
            int defaulty = center.y - (PLACEWIDTH / 2);
            placementPoints.add(new Point(defaultx, defaulty));
        }

        return placementPoints;
    }



    static List getPlacementsStartingAtTopLeft(Collection countryPolygons,  Rectangle bounding, Point center,  Collection containedCountryPolygons)
    {
        List placementRects = new ArrayList();
        List placementPoints = new ArrayList();

        Rectangle2D place = new Rectangle2D.Double(center.x, center.y, PLACEHEIGHT, PLACEWIDTH);
        for(int x = bounding.x; x  < bounding.width + bounding.x; x++)
        {
            for (int y = bounding.y; y < bounding.height + bounding.y; y++)
            {
                isPlacement(countryPolygons, containedCountryPolygons, placementRects, placementPoints, place, x, y);
            }
        }

        if(placementPoints.isEmpty())
        {
            int defaultx =center.x - (PLACEHEIGHT / 2);
            int defaulty = center.y - (PLACEWIDTH / 2);
            placementPoints.add(new Point(defaultx, defaulty));
        }

        return placementPoints;
    }

    /**
     *
     * @param countryPolygons Collection
     * @param containedCountryPolygons Collection - polygons of countries contained with ourselves
     * @param placementRects List
     * @param placementPoints List
     * @param place Rectangle2D
     * @param x int
     * @param y int
     */
    private static void isPlacement(Collection countryPolygons, Collection containedCountryPolygons,  List placementRects, List placementPoints, Rectangle2D place, int x, int y)
    {
        place.setFrame(x,y,PLACEWIDTH, PLACEHEIGHT);
        if(containedIn(place, countryPolygons) &&
           ! intersectsOneOf(place, placementRects) &&
           //make sure it isnt in or intersects the contained country
           ( !containedIn(place, containedCountryPolygons) && ! intersectsOneOf(place, containedCountryPolygons)
           )
         )
        {
            placementPoints.add(new Point( (int) place.getX(), (int) place.getY()));
            Rectangle2D newRect = new Rectangle2D.Double();
            newRect.setFrame(place);
            placementRects.add(newRect);
        }
    }

    public static boolean containedIn(Rectangle2D r, Collection shapes)
    {
        Iterator iter = shapes.iterator();
        while (iter.hasNext())
        {
            Shape item = (Shape)iter.next();
            if(item.contains(r))
                return true;
        }
        return false;
    }

    public static boolean intersectsOneOf(Rectangle2D r, Collection shapes)
    {
        if(shapes.isEmpty())
            return false;

        Iterator iter = shapes.iterator();
        while (iter.hasNext())
        {
            Shape item = (Shape) iter.next();
            if(item.intersects(r))
                return true;
        }
        return false;

    }


    public static void main(String[] args)
    {
        calculate();
    }


}
