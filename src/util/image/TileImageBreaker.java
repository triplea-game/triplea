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

import games.strategy.triplea.ui.screen.TileManager;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * Utility for breaking an image into seperate smaller images.
 * 
 * User must make a new directory called "newImages" and then run the utility
 * first.
 * 
 * To create sea zones only, he must choose "Y" at the prompt. To create
 * territories, he must choose "N" at the prompt.
 * 
 * sea zone images directory must be renamed to "seazone
 */

public class TileImageBreaker
{
    private static final String SMALL_MAPS_LOCATION = new FileSave("Where to save Tile Images?", null).getPathString();

    private static JFrame observer = new JFrame();

    private boolean m_baseMap;

    /**
     * main(java.lang.String[] args)
     * 
     * Main program begins here. Creates a new instance of ReliefImageBreaker
     * and calls createMaps() method to start the computations.
     * 
     * @param java.lang.String[]
     *            args the command line parameters
     * 
     * @exception java.lang.Exception
     *                throws
     */
    public static void main(String[] args) throws Exception
    {
        new TileImageBreaker().createMaps();
    }

    /**
     * createMaps()
     * 
     * One of the main methods that is used to create the actual maps. Calls on
     * various methods to get user input and create the maps.
     * 
     * @exception java.io.IOException
     *                throws
     *  
     */
    public void createMaps() throws IOException
    {

        Image map = loadImage(); //ask user to input image location

        if (map == null)
        {
            System.out.println("You need to select a map image for this to work");
            System.out.println("Shutting down");
            System.exit(0);
            return;
        }

        //m_baseMap = doBaseMap(); //ask user wether it is sea zone only or
                                     // not
//        String mapDir = getMapDirectory(); //ask user where the map is
//
//        if (mapDir == null || mapDir.equals(""))
//        {
//            System.out.println("You need to specify a map name for this to work");
//            System.out.println("Shutting down");
//            System.exit(0);
//        }
//
//        try
//        {
//            MapData.setMapDir(mapDir); //makes TripleA read all the text data
//                                       // files for the map.
//        } catch (NullPointerException npe)
//        {
//            System.out.println("Bad data given or missing text files, shutting down");
//            System.exit(0);
//        }

        for (int x = 0; (x) * TileManager.TILE_SIZE < map.getWidth(null); x++)
        {
            for (int y = 0; (y) * TileManager.TILE_SIZE < map.getHeight(null); y++)
            {
                Rectangle bounds = new Rectangle(x * TileManager.TILE_SIZE, y * TileManager.TILE_SIZE, TileManager.TILE_SIZE, TileManager.TILE_SIZE);

                GraphicsConfiguration m_localGraphicSystem = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                        .getDefaultConfiguration();

                BufferedImage relief = m_localGraphicSystem.createCompatibleImage(TileManager.TILE_SIZE, TileManager.TILE_SIZE,
                        m_baseMap ? Transparency.BITMASK : Transparency.TRANSLUCENT);
                relief.getGraphics().drawImage(map, 0, 0, TileManager.TILE_SIZE, TileManager.TILE_SIZE, bounds.x, bounds.y,
                        bounds.x + TileManager.TILE_SIZE, bounds.y + TileManager.TILE_SIZE, observer);

                
                //boolean match = false;
//                Iterator territories = MapData.getInstance().getTerritories().iterator();
//                while(territories.hasNext())
//                {
//                    String territoryName = territories.next().toString();
//                    Rectangle territoryBounds = MapData.getInstance().getBoundingRect(territoryName); 
//                    boolean seaZone = (territoryName.toUpperCase().indexOf("SEA")  != -1) ||
//                    (territoryName.toUpperCase().indexOf("SZ") != -1 );
//                    if( (seaZone == m_baseMap) &&
//                            (territoryBounds.contains(bounds) || bounds.contains(territoryBounds) || bounds.intersects(territoryBounds))
//                            )
//                    {
//                      
//                        //match = true;
//                        break;
//                    }
//                }
                
                String outFileName = SMALL_MAPS_LOCATION + File.separator + x  + "_"  + y + ".png";
                
//                if(!match)
//                {
//                    System.out.println("Skipping" + outFileName);
//                    continue;
//                }
	                
                
                
                ImageIO.write(relief, "png", new File(outFileName));
                System.out.println("wrote " + outFileName);
            }
        }
        

        System.out.println("All Finished!");
        System.exit(0);
    }

    /**
     * java.lang.boolean doSeaZone()
     * 
     * Asks user wether to do base map or not
     * 
     * @return java.lang.boolean TRUE to do base map
     */
//    private static boolean doBaseMap()
//    {
//        String ans = "";
//
//        while (true)
//        {
//            ans = JOptionPane.showInputDialog(null, "Do Base map (ie not relief map)? Enter [Y/N]");
//
//            if (ans == null)
//            {
//                System.out.println("Cannot leave this blank!");
//                System.out.println("Retry");
//            } else
//            {
//
//                if (ans.equalsIgnoreCase("Y"))
//                {
//                    return true;
//                } else if (ans.equalsIgnoreCase("N"))
//                {
//                    return false;
//                } else
//                {
//                    System.out.println("You must enter Y or N");
//                }
//            }
//
//        }//while
//    }

    /**
     * java.lang.String getMapDirectory()
     * 
     * Asks the user to input a valid map name that will be used to form the map
     * directory in the core of TripleA in the class TerritoryData.
     * 
     * we need the exact map name as indicated in the XML game file ie."revised"
     * "classic" "pact_of_steel" of course, without the quotes.
     * 
     * @return java.lang.String mapDir the map name
     */
//    private static String getMapDirectory()
//    {
//        String mapDir = JOptionPane.showInputDialog(null, "Enter the name of the map (ie. revised)");
//
//        if (mapDir != null)
//        {
//            return mapDir;
//        } else
//        {
//            return null;
//        }
//    }

    /**
     * java.awt.Image loadImage()
     * 
     * Asks the user to select an image and then it loads it up into an Image
     * object and returns it to the calling class.
     * 
     * @return java.awt.Image img the loaded image
     */
    private static Image loadImage()
    {
        System.out.println("Select the map");
        String mapName = new FileOpen("Select The Map").getPathString();

        if (mapName != null)
        {
            Image img = Toolkit.getDefaultToolkit().createImage(mapName);
            MediaTracker tracker = new MediaTracker(new Panel());
            tracker.addImage(img, 1);

            try
            {
                tracker.waitForAll();
                return img;
            } catch (InterruptedException ie)
            {
                ie.printStackTrace();
                return loadImage();
            }
        } else
        {
            return null;
        }
    }


  

}//end class ReliefImageBreaker
