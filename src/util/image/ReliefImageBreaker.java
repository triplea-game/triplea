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

import games.strategy.util.PointFileReaderWriter;
import java.io.*;
import java.util.Map;
import games.strategy.ui.Util;
import java.awt.*;
import java.awt.image.*;
import java.util.*;
import javax.swing.*;
import javax.imageio.*;
import java.awt.Graphics2D;
import games.strategy.ui.*;
import games.strategy.triplea.ui.*;

/**
 * Utility for breaking an image into seperate smaller images.
 */

public class ReliefImageBreaker
{
    private static JFrame observer = new JFrame();

    private static Image loadImage(String name)
    {
        File f = new File(name);
        if(!f.exists())
            throw new IllegalArgumentException("file not found");
        Image img = Toolkit.getDefaultToolkit().createImage(name);
        MediaTracker tracker = new MediaTracker(new Panel());
        tracker.addImage(img, 1);
        try
        {
            tracker.waitForAll();
            return img;
        }
        catch (InterruptedException ie)
        {
            ie.printStackTrace();
            return loadImage(name);
        }
    }

    //for the relief map
//    public static final String LARGE_MAP_FILENAME = "../additonalImageData/relief.png";
//    public static final String SMALL_MAPS_LOCATION = "newImages";
//    public static final boolean ONLY_DO_SEA_ZONES= false;

    //for the sea map
    public static final String LARGE_MAP_FILENAME = "/home/sgb/dev/triplea/data/games/strategy/triplea/image/images/maps/largeMap.gif";
    public static final String SMALL_MAPS_LOCATION = "newImages";
    public static final boolean ONLY_DO_SEA_ZONES= true;



    public static void main(String[] args) throws Exception
    {
        new ReliefImageBreaker().createMaps();
    }

    public void createMaps() throws IOException
    {

        Image map = loadImage(LARGE_MAP_FILENAME);

        Iterator unitIter = TerritoryData.getInstance().getTerritories().iterator();

        while (unitIter.hasNext())
        {

            String territoryName = (String) unitIter.next();
            boolean seaZone = territoryName.endsWith("Sea Zone");
            if(!seaZone && ONLY_DO_SEA_ZONES)
                continue;
            if(seaZone && !ONLY_DO_SEA_ZONES)
                continue;


            processImage(territoryName, map);
        }
    }


    private void processImage(String territory, Image map) throws IOException
    {
        Rectangle bounds = TerritoryData.getInstance().getBoundingRect(territory);
        int width = bounds.width;
        int height = bounds.height;

        BufferedImage alphaChannelImage = Util.createImage(bounds.width, bounds.height, true);
        Iterator iter = TerritoryData.getInstance().getPolygons(territory).iterator();
        while (iter.hasNext())
        {
            Polygon item = (Polygon)iter.next();
            item = new Polygon(item.xpoints, item.ypoints, item.npoints);
            item.translate(-bounds.x, -bounds.y);
            alphaChannelImage.getGraphics().fillPolygon(item);
        }

        GraphicsConfiguration   m_localGraphicSystem = GraphicsEnvironment.getLocalGraphicsEnvironment()
       .getDefaultScreenDevice()
       .getDefaultConfiguration();

        BufferedImage relief = m_localGraphicSystem.createCompatibleImage(width,
            height,
            ONLY_DO_SEA_ZONES ? Transparency.BITMASK :  Transparency.TRANSLUCENT);
        relief.getGraphics().drawImage(map, 0,0, width, height, bounds.x, bounds.y, bounds.x + width, bounds.y + height, observer);

        blankOutline(alphaChannelImage, relief);

        String outFileName = SMALL_MAPS_LOCATION + "/" + territory;
        if (!ONLY_DO_SEA_ZONES)
            outFileName += "_relief.png";
        else
            outFileName += ".png";

          outFileName = outFileName.replace(' ', '_');

          ImageIO.write(relief, "png" , new File(outFileName));
    }

  
    //set the alpha channel to the same as that of the base image
    private void blankOutline(Image alphaChannelImage, BufferedImage relief)
    {
        Graphics2D gc = (Graphics2D) relief.getGraphics();
        // setup our composite
        Composite prevComposite = gc.getComposite();
        gc.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_IN));

        // draw the image, and check for the possibility it doesn't complete now
        ImageIoCompletionWatcher watcher = new ImageIoCompletionWatcher();
        boolean drawComplete = gc.drawImage(alphaChannelImage, 0, 0, watcher);

        // use the watcher to for the draw to finish
        if (!drawComplete)
        {
          watcher.waitForCompletion();
        }

        // cleanup
        gc.setComposite(prevComposite);
    }


}
