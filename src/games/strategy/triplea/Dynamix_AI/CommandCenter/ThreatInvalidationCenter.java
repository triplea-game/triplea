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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 *
 * @author Stephen
 */
public class ThreatInvalidationCenter
{
    private static HashMap<PlayerID, ThreatInvalidationCenter> s_TICInstances = new HashMap<PlayerID, ThreatInvalidationCenter>();
    public static ThreatInvalidationCenter get(GameData data, PlayerID player)
    {
        if(!s_TICInstances.containsKey(player))
            s_TICInstances.put(player, create(data, player));
        return s_TICInstances.get(player);
    }
    private static ThreatInvalidationCenter create(GameData data, PlayerID player)
    {
        return new ThreatInvalidationCenter(data, player);
    }
    public static void ClearStaticInstances()
    {
        s_TICInstances.clear();
    }
    public static void NotifyStartOfRound()
    {
        s_TICInstances.clear();
    }
    private GameData m_data = null;
    private PlayerID m_player = null;
    public ThreatInvalidationCenter(GameData data, PlayerID player)
    {
        m_data = data;
        m_player = player;
    }

    private HashSet<Unit> InvalidatedEnemyUnits = new HashSet<Unit>();
    public void InvalidateThreats(List<Unit> threats)
    {
        InvalidatedEnemyUnits.addAll(threats);
    }
    public boolean IsUnitInvalidated(Unit unit)
    {
        return InvalidatedEnemyUnits.contains(unit);
    }
    public void ClearInvalidatedThreats()
    {
        InvalidatedEnemyUnits.clear();
    }
}
