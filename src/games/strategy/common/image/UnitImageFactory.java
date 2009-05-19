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

package games.strategy.common.image;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.framework.GameRunner;
import games.strategy.ui.Util;


/**
 * Utility class to get image for a Unit.<p>
 * 
 * This class is a simplified version of Sean Bridges's games.strategy.triplea.image.UnitImageFactory.
 *
 * @author Lane Schwartz
 */
public class UnitImageFactory
{
    private static final File BASE_FOLDER = new File(GameRunner.getRootFolder(), "images/units/");
    
    // Image cache
    private final Map<String, Image> m_images = new HashMap<String, Image>();


    /** 
     * Creates new IconImageFactory 
     */
    public UnitImageFactory()
    {  
    }


    /**
     * Return the appropriate unit image.
     */
    public Image getImage(UnitType type, PlayerID player, GameData data)
    {
        String baseName = getBaseImageName(type, player, data);
        String fullName = baseName + player.getName();

        if(m_images.containsKey(fullName))
        {
            return m_images.get(fullName);
        }

        Image baseImage = getBaseImage(baseName, player);

        m_images.put(fullName, baseImage);
        return baseImage;

    }

    private BufferedImage getBaseImage(String baseImageName, PlayerID id)
    {
        String fileName = id.getName() + File.separator + baseImageName  + ".png";

        BufferedImage image = null;
        try
        {   
            image = ImageIO.read( new File(BASE_FOLDER, fileName) );

            Util.ensureImageLoaded(image);
        }
        catch (InterruptedException ex)
        {
            ex.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return image;

    }


    private String getBaseImageName(UnitType type, PlayerID id, GameData data)
    {
        StringBuilder name = new StringBuilder(32);
        name.append(type.getName());

        return name.toString();
    }


}
