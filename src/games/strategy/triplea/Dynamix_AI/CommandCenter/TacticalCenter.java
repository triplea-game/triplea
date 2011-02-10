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

/**
 *
 * @author Stephen
 */
public class TacticalCenter
{
    private static HashMap<PlayerID, TacticalCenter> s_TCInstances = new HashMap<PlayerID, TacticalCenter>();
    public static TacticalCenter get(GameData data, PlayerID player)
    {
        if(!s_TCInstances.containsKey(player))
            s_TCInstances.put(player, create(data, player));
        return s_TCInstances.get(player);
    }
    private static TacticalCenter create(GameData data, PlayerID player)
    {
        return new TacticalCenter(data, player);
    }
    public static void ClearStaticInstances()
    {
        s_TCInstances.clear();
    }
    public static void NotifyEndOfTurn()
    {
        s_TCInstances.clear();
    }
    private GameData m_data = null;
    private PlayerID m_player = null;
    public TacticalCenter(GameData data, PlayerID player)
    {
        m_data = data;
        m_player = player;
    }

    private List<PlayerID> EnemyPlayersSortedByPriority = new ArrayList<PlayerID>();
    public List<UnitGroup> AllDelegateUnitGroups = new ArrayList<UnitGroup>();
    public HashSet<Unit> FrozenUnits = new HashSet<Unit>();
    public List<PlayerID> GetEnemyListSortedByPriority()
    {
        if(EnemyPlayersSortedByPriority == null || EnemyPlayersSortedByPriority.isEmpty())
            EnemyPlayersSortedByPriority = DUtils.GenerateEnemyListSortedByPriority(m_data, m_player);

        return EnemyPlayersSortedByPriority;
    }
    public void ClearEnemyListSortedByPriority()
    {
        EnemyPlayersSortedByPriority.clear();
    }
    public HashMap<Territory, Float> BattleRetreatChanceAssignments = new HashMap<Territory, Float>();
}
