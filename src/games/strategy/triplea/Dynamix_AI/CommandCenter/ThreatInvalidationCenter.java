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
import games.strategy.triplea.Dynamix_AI.DSettings;
import games.strategy.triplea.Dynamix_AI.DUtils;
import games.strategy.triplea.Dynamix_AI.Others.ThreatInvalidationType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

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

    private boolean ThreatInvalidationSuspended = false;
    public void SuspendThreatInvalidation()
    {
        ThreatInvalidationSuspended = true;
    }
    public void ResumeThreatInvalidation()
    {
        ThreatInvalidationSuspended = false;
    }

    private HashMap<Territory, List<Unit>> InvalidatedEnemyUnits = new HashMap<Territory, List<Unit>>();
    public void InvalidateThreats(List<Unit> threats, Territory hotspot)
    {
        if(DSettings.LoadSettings().AA_threatInvalidationType.equals(ThreatInvalidationType.None))
            return;

        if (DSettings.LoadSettings().AA_percentageOfResistedThreatThatTasksInvalidate != 100)
        {
            DUtils.Log(Level.FINER, "            Threats we would invalidate if we invalidated all: {0}", DUtils.UnitList_ToString(threats));
            threats = DUtils.GetXPercentOfTheItemsInList(threats, DUtils.ToFloat(DSettings.LoadSettings().AA_percentageOfResistedThreatThatTasksInvalidate));
        }

        List<Territory> tersWereInvalidatingThreatsFor = new ArrayList<Territory>();
        if(DSettings.LoadSettings().AA_threatInvalidationType.equals(ThreatInvalidationType.Global))
            tersWereInvalidatingThreatsFor = DUtils.ToList(m_data.getMap().getTerritories());
        else //We're invalidating threats around the territory, the extent of the ring of ters we invalidate the threats for is user-set
            tersWereInvalidatingThreatsFor = DUtils.GetTerritoriesWithinXDistanceOfY(m_data, hotspot, DSettings.LoadSettings().AA_threatInvalidationAroundHotspotRadius);
        for (Territory ter : tersWereInvalidatingThreatsFor)
        {
            DUtils.AddObjectsToListValueForKeyInMap(InvalidatedEnemyUnits, ter, threats);
        }
        DUtils.Log(Level.FINER, "          Invalidating threats. Units: {0} New Total Size: {1} Hotspot: {2} Ters: {3}", DUtils.UnitList_ToString(threats), InvalidatedEnemyUnits.get(hotspot).size(), hotspot.getName(), tersWereInvalidatingThreatsFor);
    }
    public boolean IsUnitInvalidated(Unit unit, Territory ter)
    {
        if(ThreatInvalidationSuspended)
            return false;
        if(!InvalidatedEnemyUnits.containsKey(ter))
            return false;
        return InvalidatedEnemyUnits.get(ter).contains(unit);
    }
    public void ClearInvalidatedThreats()
    {
        DUtils.Log(Level.FINE, "    Clearing invalidated threats.");
        InvalidatedEnemyUnits.clear();
    }
}
