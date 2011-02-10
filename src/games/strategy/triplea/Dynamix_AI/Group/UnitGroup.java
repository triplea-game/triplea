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

package games.strategy.triplea.Dynamix_AI.Group;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Dynamix_AI.CommandCenter.GlobalCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.StatusCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.TacticalCenter;
import games.strategy.triplea.Dynamix_AI.DMatches;
import games.strategy.triplea.Dynamix_AI.DUtils;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.Match;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

/**
 *
 * @author Stephen
 */
public class UnitGroup
{
    private Collection<Unit> m_units = new ArrayList<Unit>();
    private Territory m_startTer = null;
    private Territory m_movedTo = null;
    private float m_attack = 0F;
    private float m_defense = 0F;
    private Match<Territory> m_cmRouteMatch = null;
    private HashMap<Match<Territory>, Integer> m_ncmCRouteMatches = null;
    private GameData m_data = null;
    private List<Territory> m_neighbors = new ArrayList<Territory>();
    private int m_moveIndex = 0;

    public UnitGroup(Unit unit, Territory startTer, GameData data)
    {
        TacticalCenter.get(data, GlobalCenter.CurrentPlayer).AllDelegateUnitGroups.add(this);
        m_units = Collections.singleton(unit);
        m_startTer = startTer;
        m_attack = DUtils.GetAttackScoreOfUnits(m_units);
        m_defense = DUtils.GetDefenseScoreOfUnits(m_units);
        m_data = data;
        GenerateRouteMatches();
        GenerateNeighbors();
    }

    public UnitGroup(Collection<Unit> units, Territory startTer, GameData data)
    {        
        TacticalCenter.get(data, GlobalCenter.CurrentPlayer).AllDelegateUnitGroups.add(this);
        m_units = units;
        m_startTer = startTer;
        m_attack = DUtils.GetAttackScoreOfUnits(m_units);
        m_defense = DUtils.GetDefenseScoreOfUnits(m_units);
        m_data = data;
        GenerateRouteMatches();
        GenerateNeighbors();
    }

    @Override
    public int hashCode()
    {
        String hashString = m_units.hashCode() + "" + m_startTer.getName();
        return hashString.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final UnitGroup other = (UnitGroup) obj;
        if (this.m_units != other.m_units && (this.m_units == null || !this.m_units.equals(other.m_units)))
        {
            return false;
        }
        if (this.m_startTer != other.m_startTer && (this.m_startTer == null || !this.m_startTer.equals(other.m_startTer)))
        {
            return false;
        }
        return true;
    }

    private void GenerateNeighbors()
    {
        m_neighbors = new ArrayList<Territory>(m_data.getMap().getNeighbors(m_startTer));
    }

    private void GenerateRouteMatches()
    {
        PlayerID player = null;
        boolean land = false;
        boolean air = false;
        boolean sea = false;
        for (Unit unit : m_units)
        {
            player = unit.getOwner();
            UnitAttachment ua = UnitAttachment.get(unit.getUnitType());
            if(ua.isAir())
                air = true;
            else if(ua.isSea())
                sea = true;
            else
                land = true;
        }

        if(player == null || player.isNull())
            player = GlobalCenter.CurrentPlayer;

        if(air)
        {
            m_cmRouteMatch = Matches.TerritoryIsNotImpassable;
        }
        else if(sea)
        {
            m_cmRouteMatch = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasNoEnemyUnits(player, m_data));
        }
        else
        {
             m_cmRouteMatch = new CompositeMatchAnd<Territory>(Matches.TerritoryIsLand, Matches.territoryHasNoEnemyUnits(player, m_data));
        }

        if(air)
        {
            m_ncmCRouteMatches = new HashMap<Match<Territory>, Integer>();
            m_ncmCRouteMatches.put(DUtils.CompMatchAnd(Matches.TerritoryIsNotImpassable, DUtils.CompMatchOr(DMatches.territoryIsOwnedByXOrAlly(m_data, player), Matches.territoryHasUnitsThatMatch(Matches.UnitIsAA).invert())), 1); //Is passible and without enemy AA's
            m_ncmCRouteMatches.put(Matches.TerritoryIsNotImpassable, 2); //Is any passable ter
        }
        else if(sea)
        {
            m_ncmCRouteMatches = new HashMap<Match<Territory>, Integer>();
            m_ncmCRouteMatches.put(new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasUnitsOwnedBy(player)), 10); //We love sea with our units, cause we can control the movement
            m_ncmCRouteMatches.put(new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasUnitsThatMatch(Matches.unitIsEnemyOf(m_data, player).invert())), 15); //Allied is ok, but they could move
            m_ncmCRouteMatches.put(new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasNoEnemyUnits(player, m_data)), 20); //Enemy free is fine
            m_ncmCRouteMatches.put(Matches.TerritoryIsWater, 25); //We don't like having to go through enemies
        }
        else
        {
            m_ncmCRouteMatches = new HashMap<Match<Territory>, Integer>();
            m_ncmCRouteMatches.put(new CompositeMatchAnd<Territory>(Matches.TerritoryIsLand, DMatches.territoryIsOwnedBy(player)), 10);
            m_ncmCRouteMatches.put(new CompositeMatchAnd<Territory>(Matches.TerritoryIsLand, DMatches.territoryIsOwnedByXOrAlly(m_data, player)), 12);
            m_ncmCRouteMatches.put(new CompositeMatchAnd<Territory>(Matches.TerritoryIsLand, Matches.TerritoryIsNotImpassable, Matches.TerritoryIsNotNeutral), 15);
            m_ncmCRouteMatches.put(new CompositeMatchAnd<Territory>(Matches.TerritoryIsLand), 1000); //We really don't want to go through impassibles, actually never, cause we can't... 8|
        }
    }

    public Collection<Unit> GetUnits()
    {
        return m_units;
    }

    public Territory GetStartTerritory()
    {
        return m_startTer;
    }

    public float GetAttack()
    {
        return m_attack;
    }

    public float GetDefense()
    {
        return m_defense;
    }

    public Unit GetFirstUnit()
    {
        return ((Unit) m_units.toArray()[0]);
    }

    public Route GetCMRoute(Territory ter)
    {
        Route route = m_data.getMap().getRoute_IgnoreEnd(m_startTer, ter, m_cmRouteMatch);
        if (route == null || route.getTerritories().size() < 2 || route.getStart().getName().equals(route.getEnd().getName()))
            return null;
        int slowest = DUtils.GetSlowestMovementUnitInList(new ArrayList<Unit>(m_units));
        if(slowest < 1)
            return null;
        if(UnitAttachment.get(GetFirstUnit().getUnitType()).isAir())
        {
            route = DUtils.TrimRoute_ToLength(route, slowest, GetFirstUnit().getOwner(), m_data);
        }
        else if(UnitAttachment.get(GetFirstUnit().getUnitType()).isSea())
        {
            route = DUtils.TrimRoute_AtFirstTerWithEnemyUnits(route, slowest, GetFirstUnit().getOwner(), m_data);
        }
        else
        {
            route = DUtils.TrimRoute_AtFirstTerWithEnemyUnits(route, slowest, GetFirstUnit().getOwner(), m_data);
        }

        if (route == null || route.getTerritories().size() < 2 || route.getStart().getName().equals(route.getEnd().getName()))
            return null;

        if (StatusCenter.get(m_data, GlobalCenter.CurrentPlayer).GetStatusOfTerritory(route.getEnd()).WasAbandoned)
            route = DUtils.TrimRoute_BeforeFirstTerMatching(route, slowest, GlobalCenter.CurrentPlayer, m_data, DMatches.territoryMatchesDMatch(m_data, GlobalCenter.CurrentPlayer, DMatches.TS_WasAbandoned));

        if (route == null || route.getTerritories().size() < 2 || route.getStart().getName().equals(route.getEnd().getName()))
            return null;

        return route;
    }

    public Route GetNCMRoute(Territory ter)
    {
        Route route = m_data.getMap().getCompositeRoute(m_startTer, ter, m_ncmCRouteMatches);
        if (route == null || route.getTerritories() == null || route.getTerritories().size() < 2 || route.getStart().getName().equals(route.getEnd().getName()))
            return null;

        int slowest = DUtils.GetSlowestMovementUnitInList(new ArrayList<Unit>(m_units));
        if(slowest < 1)
            return null;
        if(UnitAttachment.get(GetFirstUnit().getUnitType()).isAir())
        {
            route = DUtils.TrimRoute_ToLength(route, slowest, GetFirstUnit().getOwner(), m_data);
        }
        else if(UnitAttachment.get(GetFirstUnit().getUnitType()).isSea())
        {
            route = DUtils.TrimRoute_BeforeFirstTerWithEnemyUnits(route, slowest, GetFirstUnit().getOwner(), m_data);
        }
        else
        {
            route = DUtils.TrimRoute_AtLastFriendlyTer(route, slowest, GetFirstUnit().getOwner(), m_data);
        }

        if (route == null || route.getTerritories().size() < 2 || route.getStart().getName().equals(route.getEnd().getName()))
            return null;

        if (StatusCenter.get(m_data, GlobalCenter.CurrentPlayer).GetStatusOfTerritory(route.getEnd()).WasAbandoned)
            route = DUtils.TrimRoute_BeforeFirstTerMatching(route, slowest, GlobalCenter.CurrentPlayer, m_data, DMatches.territoryMatchesDMatch(m_data, GlobalCenter.CurrentPlayer, DMatches.TS_WasAbandoned));

        if (route == null || route.getTerritories().size() < 2 || route.getStart().getName().equals(route.getEnd().getName()))
            return null;

        return route;
    }

    /**
     * Returns false if move failed. Possible causes of move failure:
     * unit group already moved,
     * target is unit group start ter,
     * route is null,
     * trimmed route end is an abandoned territory.
     */
    public boolean MoveAsFarTo_CM(Territory ter, IMoveDelegate mover)
    {
        Route route = null;
        //try
        //{
            if(m_movedTo != null || GetStartTerritory().getName().equals(ter.getName()))
                return false;

            route = GetCMRoute(ter);

            if(route == null)
                return false;

            if(StatusCenter.get(m_data, GlobalCenter.CurrentPlayer).GetStatusOfTerritory(route.getEnd()).WasAbandoned)
                return false;

            List<Unit> unitsToMove = new ArrayList<Unit>(m_units);
            unitsToMove.removeAll(TacticalCenter.get(m_data, GlobalCenter.CurrentPlayer).FrozenUnits);
            mover.move(unitsToMove, route);

            m_moveIndex = movesCount;
            movesCount++;
            m_movedTo = route.getEnd();

            if (route.getEnd().getUnits().containsAll(unitsToMove))
                DUtils.Log(Level.FINER, "      Performed cm move, as far to: {0} Units: {1} Route: {2}", ter, unitsToMove, route);
            else
            {
                DUtils.Log(Level.FINER, "        CM move failed! Target: {0} Units: {1} Route: {2}", ter, unitsToMove, route);
                return false;
            }
            if (unitsToMove.size() != m_units.size())
                DUtils.Log(Level.FINER, "      Some units in group must be frozen, because m_units and unitsToMove don't match. m_units: {0} unitsToMove: {1}", m_units, unitsToMove);

        /*}
        catch (NullPointerException ex)
        {
            System.out.append("Units Size:" + m_units.size());
            if (route != null)
            {
                System.out.append("StartTer:" + route.getStart().getName());
                System.out.append("EndTer:" + route.getEnd().getName());
            }
            System.out.append(ex.toString());
        }*/
        return true;
    }

    /**
     * Returns false if move failed. Possible causes of move failure:
     * unit group already moved,
     * target is unit group start ter,
     * route is null,
     * trimmed route end is an abandoned territory.
     */
    public boolean MoveAsFarTo_NCM(Territory ter, IMoveDelegate mover)
    {
        Route route = null;
        //try
        //{
            if (m_movedTo != null || GetStartTerritory().getName().equals(ter.getName()))
                return false;

            route = GetNCMRoute(ter);

            if(route == null)
                return false;
            
            List<Unit> unitsToMove = new ArrayList<Unit>(m_units);
            unitsToMove.removeAll(TacticalCenter.get(m_data, GlobalCenter.CurrentPlayer).FrozenUnits);
            mover.move(unitsToMove, route);

            m_moveIndex = movesCount;
            movesCount++;
            m_movedTo = route.getEnd();

            if (route.getEnd().getUnits().containsAll(unitsToMove))
                DUtils.Log(Level.FINER, "      Performed ncm move, as far to: {0} Units: {1} Route: {2}", ter, unitsToMove, route);
            else
            {
                DUtils.Log(Level.FINER, "        NCM move failed! Target: {0} Units: {1} Route: {2}", ter, unitsToMove, route);
                return false;
            }
            if (unitsToMove.size() != m_units.size())
                DUtils.Log(Level.FINER, "      Some units in group must be frozen, because m_units and unitsToMove don't match. m_units: {0} unitsToMove: {1}", m_units, unitsToMove);
        //}
        /*catch (NullPointerException ex)
        {
            System.out.append("Units Size:" + m_units.size());
            if (route != null)
            {
                System.out.append("StartTer:" + route.getStart().getName());
                System.out.append("EndTer:" + route.getEnd().getName());
            }
            System.out.append(ex.toString());
        }*/
        return true;
    }

    /**
     * Returns false if move failed. Possible causes of move failure:
     * units have no movement,
     * route is null, invalid, or a loop,
     * trimmed route is null, invalid, or a loop,
     * trimmed route end is an abandoned territory.
     */
    public boolean MoveAsFarAlongRoute_NCM(IMoveDelegate mover, Route fullRoute)
    {
        Route route = fullRoute;
        //try
        //{
            if (m_movedTo != null)
                return false;

            int slowest = DUtils.GetSlowestMovementUnitInList(new ArrayList<Unit>(m_units));
            if (route == null || route.getTerritories().size() < 2 || route.getStart().getName().equals(route.getEnd().getName()))
                return false;
            if(slowest < 1)
                return false;
            if(UnitAttachment.get(GetFirstUnit().getUnitType()).isAir())
            {
                route = DUtils.TrimRoute_ToLength(route, slowest, GetFirstUnit().getOwner(), m_data);
            }
            else if(UnitAttachment.get(GetFirstUnit().getUnitType()).isSea())
            {
                route = DUtils.TrimRoute_BeforeFirstTerWithEnemyUnits(route, slowest, GetFirstUnit().getOwner(), m_data);
            }
            else
            {
                route = DUtils.TrimRoute_AtLastFriendlyTer(route, slowest, GetFirstUnit().getOwner(), m_data);
            }
            
            if (route == null || route.getTerritories().size() < 2 || route.getStart().getName().equals(route.getEnd().getName()))
                return false;
            
            if(StatusCenter.get(m_data, GlobalCenter.CurrentPlayer).GetStatusOfTerritory(route.getEnd()).WasAbandoned)
                route = DUtils.TrimRoute_BeforeFirstTerMatching(route, slowest, GlobalCenter.CurrentPlayer, m_data, DMatches.territoryMatchesDMatch(m_data, GlobalCenter.CurrentPlayer, DMatches.TS_WasAbandoned));

            if (route == null || route.getTerritories().size() < 2 || route.getStart().getName().equals(route.getEnd().getName()))
                return false;

            List<Unit> unitsToMove = new ArrayList<Unit>(m_units);
            unitsToMove.removeAll(TacticalCenter.get(m_data, GlobalCenter.CurrentPlayer).FrozenUnits);
            mover.move(unitsToMove, route);

            m_moveIndex = movesCount;
            movesCount++;
            m_movedTo = route.getEnd();

            if (route.getEnd().getUnits().containsAll(unitsToMove))
                DUtils.Log(Level.FINER, "      Performed ncm move, as far to: {0} Units: {1} Route: {2}", fullRoute.getEnd(), unitsToMove, route);
            else
            {
                DUtils.Log(Level.FINER, "        NCM move failed! Target: {0} Units: {1} Route: {2}", fullRoute.getEnd(), unitsToMove, route);
                return false;
            }
            if (unitsToMove.size() != m_units.size())
                DUtils.Log(Level.FINER, "      Some units in group must be frozen, because m_units and unitsToMove don't match. m_units: {0} unitsToMove: {1}", m_units, unitsToMove);
        //}
        /*catch (NullPointerException ex)
        {
            System.out.append("Units Size:" + m_units.size());
            if (route != null)
            {
                System.out.append("StartTer:" + route.getStart().getName());
                System.out.append("EndTer:" + route.getEnd().getName());
            }
            System.out.append(ex.toString());
        }*/
        return true;
    }

    public Territory GetMovedTo()
    {
        return m_movedTo;
    }

    public void UndoMove(IMoveDelegate mover)
    {
        if(m_movedTo == null)
            return;

        mover.undoMove(m_moveIndex);
        m_movedTo = null;
        movesCount--;
    }

    public int GetMoveIndex()
    {
        return m_moveIndex;
    }

    public static int movesCount = 0;
    public void NotifyMoveUndo(int removedMoveIndex)
    {
        if(m_moveIndex > removedMoveIndex)
            m_moveIndex--;
    }

    public List<Territory> GetNeighbors()
    {
        return m_neighbors;
    }

    public Match<Territory> GetRouteMatch()
    {
        return m_cmRouteMatch;
    }

    /**
     * (UndoAllMovesEndingInTer_ReturnStartTersOfUnitGroupsUndone)
     * @param data - Game data
     * @param player - Current player
     * @param ter - All unit groups that have moved here will get undone by this method
     * @param mover - The move delegate this is used to undo the moves
     * @return the list of ters that the undone unit groups originated from
     */
    public static List<Territory> UndoAllMovesEndingInTer_RStartTersOfUGsUndone(GameData data, PlayerID player, Territory ter, IMoveDelegate mover)
    {
        DUtils.Log(Level.FINER, "    Undoing all moves ending at {0}", ter.getName());
        List<Territory> result = new ArrayList<Territory>();
        for (UnitGroup ug : TacticalCenter.get(data, player).AllDelegateUnitGroups)
        {
            if (ug.GetMovedTo() != null && ug.GetMovedTo().equals(ter))
            {
                ug.UndoMove(mover);
                result.add(ug.GetStartTerritory());
                for (UnitGroup ug2 : TacticalCenter.get(data, player).AllDelegateUnitGroups)
                {
                    ug2.NotifyMoveUndo(ug.GetMoveIndex());
                }
            }
        }
        return result;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("|UG|Units: ");
        for(Unit unit : m_units)
        {
            builder.append(unit.toString());
        }
        return builder.toString();
    }
}
