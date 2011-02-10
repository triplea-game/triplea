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

package games.strategy.triplea.Dynamix_AI;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Dynamix_AI.CommandCenter.StatusCenter;
import games.strategy.triplea.Dynamix_AI.Group.UnitGroup;
import games.strategy.triplea.Dynamix_AI.Others.TerritoryStatus;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.Match;
import java.util.List;

/**
 * Some notes on the matches in this class:
 *
 * First, to keep the matches organized, I would like all the matches to be put into their section, which should be created if not yet existing.
 * All unit matches in one section, all territory matches in another, etc.
 *
 * Also, make sure there are markers to show the start and end of each section, as well as ten lines of blank space between each section.
 * @author Stephen
 */
public class DMatches
{
    ///////////////////////////////////////////////Unit Group Matches///////////////////////////////////////////////
    public static Match<UnitGroup> UnitGroupCanReach_Some(final Territory target)
    {
        return new Match<UnitGroup>()
        {
            public boolean match(UnitGroup ug)
            {
                Route ncmRoute = ug.GetNCMRoute(target);
                if(ncmRoute == null)
                    return false;
                return Match.someMatch(ug.GetUnits(), Matches.UnitHasEnoughMovement(ncmRoute.getLength()));
            }
        };
    }
    public static Match<UnitGroup> UnitGroupCanReach_All(final Territory target)
    {
        return new Match<UnitGroup>()
        {
            public boolean match(UnitGroup ug)
            {
                Route ncmRoute = ug.GetNCMRoute(target);
                if(ncmRoute == null)
                    return false;
                return Match.allMatch(ug.GetUnits(), Matches.UnitHasEnoughMovement(ncmRoute.getLength()));
            }
        };
    }
    public static Match<UnitGroup> UnitGroupHasEnoughMovement_Some(final int minMovement)
    {
        return new Match<UnitGroup>()
        {
            public boolean match(UnitGroup ug)
            {
                return Match.someMatch(ug.GetUnits(), Matches.UnitHasEnoughMovement(minMovement));
            }
        };
    }
    public static Match<UnitGroup> UnitGroupHasEnoughMovement_All(final int minMovement)
    {
        return new Match<UnitGroup>()
        {
            public boolean match(UnitGroup ug)
            {
                return Match.allMatch(ug.GetUnits(), Matches.UnitHasEnoughMovement(minMovement));
            }
        };
    }
    public static Match<UnitGroup> UnitGroupUnitsMatchX_All(final Match<Unit> match)
    {
        return new Match<UnitGroup>()
        {
            public boolean match(UnitGroup ug)
            {
                return Match.allMatch(ug.GetUnits(), match);
            }
        };
    }
        public static final Match<UnitGroup> UnitGroupIsSeaOrAir = new Match<UnitGroup>()
    {
        public boolean match(UnitGroup unitGroup)
        {
            UnitAttachment ua = UnitAttachment.get(unitGroup.GetFirstUnit().getType());
            return ua.isSea() || ua.isAir();
        }
    };
    public static final Match<UnitGroup> UnitGroupIsLand = new Match<UnitGroup>()
    {
        public boolean match(UnitGroup unitGroup)
        {
            UnitAttachment ua = UnitAttachment.get(unitGroup.GetFirstUnit().getType());
            return !ua.isSea();
        }
    };
    ///////////////////////////////////////////////End Unit Group Matches///////////////////////////////////////////////
    
    
    
    
    
    
    
    
    
    
    ///////////////////////////////////////////////Unit Matches///////////////////////////////////////////////
     public static Match<Unit> unitIs(final Unit u1)
    {
    	return new Match<Unit>()
    	{
    		public boolean match(Unit u2)
    		{
    	            return u1 == u2;
    		}
    	};
    }
    public static Match<Unit> unitIsNotInList(final List<Unit> list)
    {
        return new Match<Unit>()
        {
            @Override
            public boolean match(Unit ter)
            {
                return !list.contains(ter);
            }
        };
    }
    public static Match<Unit> unitIsInList(final List<Unit> list)
    {
        return new Match<Unit>()
        {
            @Override
            public boolean match(Unit ter)
            {
                return list.contains(ter);
            }
        };
    }
    public static Match<Unit> unitIsNNEnemyOf(final GameData data, final PlayerID player)
    {
        return new Match<Unit>()
        {
            @Override
            public boolean match(Unit u)
            {
                if(u.getOwner().getName().toLowerCase().equals("neutral"))
                    return false;
                return !data.getAllianceTracker().isAllied(u.getOwner(), player);
            }
        };
    }
    ///////////////////////////////////////////////End Unit Matches///////////////////////////////////////////////










    ///////////////////////////////////////////////Territory Matches///////////////////////////////////////////////
    public static Match<Territory> terIsFriendlyEmptyAndWithoutEnemyNeighbors(final GameData data, final PlayerID player)
    {
        return new Match<Territory>()
        {
            @Override
            public boolean match(Territory ter)
            {
                if (!DMatches.territoryIsOwnedByXOrAlly(data, player).match(ter))
                    return false;
                if (Matches.territoryHasUnitsThatMatch(Matches.unitHasDefenseThatIsMoreThanOrEqualTo(1)).match(ter))
                    return false;
                if (data.getMap().getNeighbors(ter, DMatches.territoryIsOwnedByNNEnemy(data, player)).size() > 0) //If it's next to enemy
                    return false;

                return true;
            }
        };
    }
    public static Match<Territory> TerritoryHasVulnerabilityEqualToOrMoreThan(final GameData data, final PlayerID player, final float minVulnerability)
    {
    	return new Match<Territory>()
    	{
            public boolean match(Territory ter)
            {
                if (DUtils.GetVulnerabilityOfArmy(data, player, ter, DUtils.ToList(ter.getUnits().getUnits())) >= minVulnerability)
                    return true;
                else
                    return false;
            }
        };
    }
    public static Match<Territory> territoryIsInList(final List<Territory> list)
    {
        return new Match<Territory>()
        {
            @Override
            public boolean match(Territory ter)
            {
                return list.contains(ter);
            }
        };
    }
    public static Match<Territory> territoryIsNotInList(final List<Territory> list)
    {
        return new Match<Territory>()
        {
            @Override
            public boolean match(Territory ter)
            {
                return !list.contains(ter);
            }
        };
    }
    public static Match<Territory> territoryIsWithinXLMovesOfATerInList(final List<Territory> list, final int maxJumpDist, final GameData data)
    {
        return new Match<Territory>()
        {
            @Override
            public boolean match(Territory territory)
            {
                for (Territory ter : list)
                {
                    if (DUtils.CanWeGetFromXToY_ByLand(data.getMap(), ter, territory) && DUtils.GetJumpsFromXToY_Land(data.getMap(), ter, territory) <= maxJumpDist)
                    {
                        return true;
                    }
                }
                return false;
            }
        };
    }
    public static Match<Territory> territoryIsOwnedByEnemy(final GameData data, final PlayerID player)
    {
        return new Match<Territory>()
        {
            public boolean match(Territory t)
            {
                if(!data.getAllianceTracker().isAllied(player, t.getOwner()))
                    return true;
                return false;
            }
        };
    }
    public static Match<Territory> territoryIsOwnedByNNEnemy(final GameData data, final PlayerID player)
    {
        return new Match<Territory>()
        {
            public boolean match(Territory t)
            {
                if(!t.getOwner().getName().equals("Neutral") && !data.getAllianceTracker().isAllied(player, t.getOwner()))
                    return true;
                return false;
            }
        };
    }
    public static Match<Territory> territoryIsOwnedBy(final PlayerID player)
    {
        return new Match<Territory>()
        {
            public boolean match(Territory t)
            {
                return t.getOwner().getName().equals(player.getName());
            }
        };
    }
    public static Match<Territory> territoryIsLandAndOwnedBy(final PlayerID player)
    {
        return new Match<Territory>()
        {
            public boolean match(Territory ter)
            {
                return !ter.isWater() && player.equals(ter.getOwner());
            }
        };
    }
    public static Match<Territory> territoryIsOwnedByXOrAlly(final GameData data, final PlayerID player)
    {
        return new Match<Territory>()
        {
            public boolean match(Territory t)
            {
                return data.getAllianceTracker().isAllied(player, t.getOwner());
            }
        };
    }
    public static Match<Territory> territoryMatchesDMatch(final GameData data, final PlayerID player, final Match<TerritoryStatus> DMatch)
    {
        return new Match<Territory>()
        {
            public boolean match(Territory ter)
            {
                return DMatch.match(StatusCenter.get(data, player).GetStatusOfTerritory(ter));
            }
        };
    }
    public static Match<Territory> territoryHasNNEnemyLandUnits(final PlayerID player, final GameData data)
    {
        return new Match<Territory>()
        {
            public boolean match(Territory t)
            {
                return t.getUnits().someMatch(DUtils.CompMatchAnd(DMatches.unitIsNNEnemyOf(data, player), Matches.UnitIsLand));
            }
        };

    }
    public static Match<Territory> territoryContainsMultipleAlliances(final GameData data)
    {
        return new Match<Territory>()
        {
            public boolean match(Territory t)
            {
                PlayerID lastPlayer = null;
                for(Unit unit : t.getUnits())
                {
                    if(lastPlayer == null)
                        lastPlayer = unit.getOwner();
                    if(!data.getAllianceTracker().isAllied(lastPlayer, unit.getOwner()))
                        return true;
                }
                return false;
            }
        };
    }
    ///////////////////////////////////////////////End Territory Matches///////////////////////////////////////////////










    ///////////////////////////////////////////////Territory Status Matches///////////////////////////////////////////////
    public static final Match<TerritoryStatus> TS_IsEndangered = new Match<TerritoryStatus>()
    {
        public boolean match(TerritoryStatus ts)
        {
            if(ts.IsEndangered)
                return true;
            else
                return false;
        }
    };
    public static final Match<TerritoryStatus> TS_WasAttacked = new Match<TerritoryStatus>()
    {
        public boolean match(TerritoryStatus ts)
        {
            if(ts.WasAttacked)
                return true;
            else
                return false;
        }
    };
    public static final Match<TerritoryStatus> TS_WasBlitzed = new Match<TerritoryStatus>()
    {
        public boolean match(TerritoryStatus ts)
        {
            if(ts.WasBlitzed)
                return true;
            else
                return false;
        }
    };
    public static final Match<TerritoryStatus> TS_WasAbandoned = new Match<TerritoryStatus>()
    {
        public boolean match(TerritoryStatus ts)
        {
            if(ts.WasAbandoned)
                return true;
            else
                return false;
        }
    };
    ///////////////////////////////////////////////End Territory Status Matches///////////////////////////////////////////////
}
