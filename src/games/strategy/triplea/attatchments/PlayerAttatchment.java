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
 * PlayerAttatchment.java
 *
 * Created on August 29, 2005, 3:14 PM
 */

package games.strategy.triplea.attatchments;

import games.strategy.engine.data.DefaultAttatchment;
import games.strategy.triplea.Constants;
import java.util.*;
import games.strategy.engine.data.*;

/**
 *
 * @author  Adam Jette
 * @version 1.0
 */
public class PlayerAttatchment extends DefaultAttatchment
{
    /**
     * Convenience method.
     */
    public static PlayerAttatchment get(PlayerID p)
    {
        return (PlayerAttatchment) p.getAttatchment(Constants.PLAYER_ATTATCHMENT_NAME);
    }

    private int m_vps = 0;
    private int m_captureVps = 0; // need to store some data during a turn

    /** Creates new PlayerAttatchment */
    public PlayerAttatchment()
    {
    }

    public void setVps(String value)
    {
        m_vps = getInt(value);
    }

    public String getVps()
    {
        return "" + m_vps;
    }

    public void setCaptureVps(String value)
    {
        m_captureVps = getInt(value);
    }

    public String getCaptureVps()
    {
        return "" + m_captureVps;
    }
}
