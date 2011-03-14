/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package games.strategy.triplea.Dynamix_AI.CommandCenter;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Dynamix_AI.DUtils;
import games.strategy.triplea.Dynamix_AI.Group.UnitGroup;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;

/**
 *
 * @author Stephen
 */
public class ReconsiderSignalCenter
{
    private static HashMap<PlayerID, ReconsiderSignalCenter> s_RSCInstances = new HashMap<PlayerID, ReconsiderSignalCenter>();
    public static ReconsiderSignalCenter get(GameData data, PlayerID player)
    {
        if(!s_RSCInstances.containsKey(player))
            s_RSCInstances.put(player, create(data, player));
        return s_RSCInstances.get(player);
    }
    private static ReconsiderSignalCenter create(GameData data, PlayerID player)
    {
        return new ReconsiderSignalCenter(data, player);
    }
    public static void ClearStaticInstances()
    {
        s_RSCInstances.clear();
    }
    public static void NotifyStartOfRound()
    {
        s_RSCInstances.clear();
    }
    private GameData m_data = null;
    private PlayerID m_player = null;
    public ReconsiderSignalCenter(GameData data, PlayerID player)
    {
        m_data = data;
        m_player = player;
    }

    public HashSet<Object> ObjectsToReconsider = new HashSet<Object>();
}
