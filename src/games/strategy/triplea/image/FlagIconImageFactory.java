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

/*
 * FlagIconImageFactory.java
 *
 * Created on November 26, 2001, 8:27 PM
 */

package games.strategy.triplea.image;

import games.strategy.engine.data.PlayerID;

import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import javax.imageio.ImageIO;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class FlagIconImageFactory
{

    private static FlagIconImageFactory s_instance = new FlagIconImageFactory();

    public static FlagIconImageFactory instance()
    {
        return s_instance;
    }

    public static final int FLAG_ICON_WIDTH = 30;

    public static final int FLAG_ICON_HEIGHT = 15;

    public static final int SMALL_FLAG_ICON_WIDTH = 12;

    public static final int SMALL_FLAG_ICON_HEIGHT = 7;

    //maps name -> image
    private final Map m_images = new HashMap();

    /** Creates new IconImageFactory */
    private FlagIconImageFactory()
    {
    };

    public Image getFlag(PlayerID id)
    {
        if (!m_images.containsKey(id.getName()))
        {
            URL url = this.getClass().getResource("images/flags/" + id.getName() + ".gif");
            if (url == null)
                throw new IllegalStateException("No flag for player:" + id.getName());

            Image image;
            try
            {
                image = ImageIO.read(url);
            } catch (IOException e)
            {
                e.printStackTrace();
                throw new IllegalStateException(e.getMessage());
            }
            m_images.put(id.getName(), image);
        }

        return (Image) m_images.get(id.getName());
    }

    public Image getSmallFlag(PlayerID id)
    {
        String key = id.getName() + "SMALL";
        if (!m_images.containsKey(key))
        {
            URL url = this.getClass().getResource("images/flags/" + id.getName() + "_small.gif");
            if (url == null)
                throw new IllegalStateException("No small flag for player:" + id.getName());

            Image image;
            try
            {
                image = ImageIO.read(url);
            } catch (IOException e)
            {
                e.printStackTrace();
                throw new IllegalStateException(e.getMessage());
            }
            m_images.put(key, image);
        }

        return (Image) m_images.get(key);
    }

}