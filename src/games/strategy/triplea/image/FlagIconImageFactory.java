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

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class FlagIconImageFactory extends ImageFactory
{

    public static final int FLAG_ICON_WIDTH = 30;
    public static final int FLAG_ICON_HEIGHT = 15;
    public static final int SMALL_FLAG_ICON_WIDTH = 12;
    public static final int SMALL_FLAG_ICON_HEIGHT = 7;


    private final String PREFIX = "flags/";
    
    /** Creates new IconImageFactory */
    public FlagIconImageFactory()
    {
    }

    public Image getFlag(PlayerID id)
    {
        String key = PREFIX + id.getName() + ".gif";
        
        return getImage(key, true);
    }

  
    public Image getSmallFlag(PlayerID id)
    {
        String key = PREFIX + id.getName() + "_small.gif";
        return getImage(key, true);
    }

    public Image getLargeFlag(PlayerID id)
    {
        String key = PREFIX + id.getName() +  "_large.png";
        return getImage(key, true);
    }

    public Image getFadedFlag(PlayerID id)
    {
        String key = PREFIX + id.getName() +  "_fade.gif";
        return getImage(key, true);
    }

    
}