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

package games.strategy.triplea.Dynamix_AI.Code;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Dynamix_AI.CommandCenter.StatusCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.ThreatInvalidationCenter;
import games.strategy.triplea.Dynamix_AI.DMatches;
import games.strategy.triplea.Dynamix_AI.DSettings;
import games.strategy.triplea.Dynamix_AI.DUtils;
import games.strategy.triplea.Dynamix_AI.Dynamix_AI;
import games.strategy.triplea.Dynamix_AI.Group.MovePackage;
import games.strategy.triplea.Dynamix_AI.Group.UnitGroup;
import games.strategy.triplea.Dynamix_AI.Others.NCM_AirLandingCalculator;
import games.strategy.triplea.Dynamix_AI.Others.NCM_TargetCalculator;
import games.strategy.triplea.Dynamix_AI.Others.NCM_Task;
import games.strategy.triplea.Dynamix_AI.Others.NCM_TaskType;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.util.Match;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 *
 * @author Stephen
 */
public class DoNonCombatMove
{
    public static void doNonCombatMove(Dynamix_AI ai, GameData data, IMoveDelegate mover, PlayerID player)
    {
        MovePackage pack = new MovePackage(ai, data, mover, player, null, null, null);

        //First, we generate a list of ncm 'tasks', such as reinforcements of territories, blocking of enemies, etc.
        List<NCM_Task> tasks = GenerateTasks(pack);

        //Then we loop through them and recruit units, discarding any tasks that we can't supply with enough units to be worthwhile.
        //This phase recruits just enough units for the task to be worthwhile.
        DUtils.Log(Level.FINE, "  Calculating and performing worthwhile tasks.");
        while (calculateAndPerformWorthwhileTasks(pack, tasks))
        {
        }

        //Now we go through tasks, and retreat from the ones we couldn't defend
        DUtils.Log(Level.FINE, "  Performing target retreats on diqualified tasks.");
        for (NCM_Task task : tasks)
        {
            if (task.IsDisqualified())
                task.PerformTargetRetreat(tasks, mover);

        }

        //We now allow the worthwhile tasks to recruit additional units to make the task even more favorable.
        DUtils.Log(Level.FINE, "  Calculating and adding additional task recruits. (Wave 2)");
        for(NCM_Task task : tasks)
        {
            if(task.IsCompleted())
            {
                task.RecruitUnits2();
                if (task.IsTaskWithAdditionalRecruitsWorthwhile())
                {
                    task.PerformTask(mover);
                    task.InvalidateThreatsThisTaskResists();
                }
            }
        }

        //We now allow the worthwhile tasks to recruit as many additional units as it sees fit.
        DUtils.Log(Level.FINE, "  Calculating and adding additional task recruits. (Wave 3)");
        for(NCM_Task task : tasks)
        {
            if(task.IsCompleted())
            {
                task.RecruitUnits3();
                if (task.IsTaskWithAdditionalRecruitsWorthwhile())
                {
                    task.PerformTask(mover);
                    task.InvalidateThreatsThisTaskResists();
                }
            }
        }

        //Now that we've completed all the ncm tasks we could, we do the ordinary ncm move-to-target process.
        DUtils.Log(Level.FINE, "  Calculating and performing regular ncm move-to-target moves.");
        for(Territory ter : data.getMap().getTerritories())
        {
            if(DMatches.territoryIsOwnedByEnemy(data, player).match(ter))
                continue;

            List<Unit> terUnits = ter.getUnits().getMatches(DUtils.CompMatchAnd(Matches.unitIsOwnedBy(player), Matches.unitHasMovementLeft, Matches.UnitIsNotAA, Matches.UnitIsNotAir));
            if(terUnits.isEmpty())
                continue;

            doNonCombatMoveForTer(pack, ter, terUnits, tasks);
        }

        ThreatInvalidationCenter.get(data, player).ClearInvalidatedThreats(); //We clear invalidated threats because the enemy will come after air even if landing ter is next to resistant ter

        //Now that we've completed all the normal ncm moves, perform the special tasks
        DUtils.Log(Level.FINE, "  Attempting to land aircraft on safe territories.");
        for(Territory ter : data.getMap().getTerritories())
        {
            boolean onlyAir = Match.getMatches(ter.getUnits().getUnits(), DUtils.CompMatchAnd(Matches.UnitIsLand, Matches.UnitIsNotAA)).isEmpty();
            //We're only landing air that can't currently land, are in an abondoned ter, or are alone in a ter (meaning we or an ally left)
            if (ter.isWater() || StatusCenter.get(data, player).GetStatusOfTerritory(ter).WasAttacked || StatusCenter.get(data, player).GetStatusOfTerritory(ter).WasAbandoned || onlyAir)
            {
                List<Unit> airUnits = ter.getUnits().getMatches(DUtils.CompMatchAnd(Matches.unitIsOwnedBy(player), Matches.unitHasMovementLeft, Matches.UnitIsAir));
                if (airUnits.isEmpty())
                    continue;

                Territory landingLoc = NCM_AirLandingCalculator.CalculateLandingLocationForAirUnits(data, player, ter, airUnits, tasks);
                if (landingLoc == null)
                {
                    DUtils.Log(Level.FINER, "    Landing location not found. Ter: {0} Air Units: {1}", ter, airUnits);
                    continue;
                }
                List<UnitGroup> ugs = DUtils.CreateUnitGroupsForUnits(airUnits, ter, data);
                DUtils.Log(Level.FINER, "    Landing aircraft at {0}. From: {1}.", landingLoc, ter);
                for (UnitGroup ug : ugs)
                {
                    ug.MoveAsFarTo_NCM(landingLoc, mover);
                }
            }
        }
    }

    private static List<NCM_Task> GenerateTasks(final MovePackage pack)
    {
        List<NCM_Task> result = new ArrayList<NCM_Task>();

        final GameData data = pack.Data;
        final PlayerID player = pack.Player;
        final List<Territory> ourCaps = TerritoryAttachment.getAllCapitals(player, data);
        Match<Territory> isReinforce_Block = new Match<Territory>()
        {
            @Override
            public boolean match(Territory ter)
            {
                if (!data.getAllianceTracker().isAllied(ter.getOwner(), player))
                    return false;
                if(ter.getUnits().getMatches(Matches.unitHasDefenseThatIsMoreThanOrEqualTo(1)).size() > 0) //If this ter is already blocking enemies
                    return false;
                if (data.getMap().getNeighbors(ter, DMatches.territoryIsOwnedByNNEnemy(data, player)).isEmpty()) //If it's not next to enemy
                    return false;
                List<Unit> blitzers = DUtils.GetNNEnemyLUnitsThatCanReach(data, ter, player, Matches.TerritoryIsLand);
                if(blitzers.isEmpty()) //If no enemy can blitz it
                    return false;
                if (data.getMap().getNeighbors(ter, DMatches.terIsFriendlyEmptyAndWithoutEnemyNeighbors(data, player)).isEmpty()) //We only block when there is territory to protect from blitzing
                    return false;

                return true;
            }
        };
        final List<Territory> capsAndNeighbors = new ArrayList<Territory>();
        for(Territory cap : ourCaps)
            capsAndNeighbors.addAll(DUtils.GetTerritoriesWithinXDistanceOfY(data, cap, 1));
        Match<Territory> isReinforce_Stabilize = new Match<Territory>()
        {
            @Override
            public boolean match(Territory ter)
            {
                if (!data.getAllianceTracker().isAllied(ter.getOwner(), player))
                    return false;
                if(!capsAndNeighbors.contains(ter))
                    return false;

                return true;
            }
        };
        Match<Territory> isReinforce_FrontLine = new Match<Territory>()
        {
            @Override
            public boolean match(Territory ter)
            {
                if (!data.getAllianceTracker().isAllied(ter.getOwner(), player))
                    return false;
                if(data.getMap().getNeighbors(ter, DMatches.territoryIsOwnedByNNEnemy(data, player)).isEmpty()) //If this is not the front line
                    return false;
                
                List<Territory> friendlyNeighbors = DUtils.ToList(data.getMap().getNeighbors(ter, DMatches.territoryIsOwnedByXOrAlly(data, player)));
                List<Territory> uniqueEnemyNeighbors = DUtils.ToList(data.getMap().getNeighbors(ter, DMatches.territoryIsOwnedByNNEnemy(data, player)));

                boolean isTerLinkBetweenFriendlies = false;
                for(Territory friendlyNeighbor : friendlyNeighbors)
                {
                    for (Territory friendlyNeighbor2 : friendlyNeighbors)
                    {
                        if(friendlyNeighbor2.equals(friendlyNeighbor))
                            continue;

                        if(!data.getMap().isValidRoute(new Route(friendlyNeighbor, friendlyNeighbor2))) //If these two friendly neighbors are connected only by this ter
                            isTerLinkBetweenFriendlies = true; //Then we must be part of the front, if we have enemy neighbors(which we have)
                    }
                }

                for (Territory neighbor : friendlyNeighbors)
                {
                    List<Territory> n_EnemyNeighbors = DUtils.ToList(data.getMap().getNeighbors(neighbor, DMatches.territoryIsOwnedByNNEnemy(data, player)));

                    uniqueEnemyNeighbors.removeAll(n_EnemyNeighbors);
                }

                if(uniqueEnemyNeighbors.isEmpty() && !isTerLinkBetweenFriendlies) //If this ter does not have unique enemy neighbors, and is not a link between two friendlies, we must not be a front
                    return false;

                return true;
            }
        };
        List<Territory> tersWeCanMoveTo = DUtils.ToList(data.getMap().getTerritories());
        for (Territory ter : tersWeCanMoveTo)
        {
            if(isReinforce_Block.match(ter))
            {
                float priority = DUtils.GetReinforceBlockPriority(data, player, ter);
                NCM_Task task = new NCM_Task(data, ter, NCM_TaskType.Reinforce_Block, priority);
                result.add(task);
                DUtils.Log(Level.FINER, "     Reinforce block task added. Ter: {0} Priority: {1}", ter.getName(), priority);
            }
            else if(isReinforce_Stabilize.match(ter))
            {
                float priority = DUtils.GetReinforceStabalizePriority(data, player, ter);
                NCM_Task task = new NCM_Task(data, ter, NCM_TaskType.Reinforce_Stabilize, priority);
                result.add(task);
                DUtils.Log(Level.FINER, "     Reinforce stabalize task added. Ter: {0} Priority: {1}", ter.getName(), priority);
            }
            else if(isReinforce_FrontLine.match(ter))
            {
                float priority = DUtils.GetReinforceFrontlinePriority(data, player, ter);
                NCM_Task task = new NCM_Task(data, ter, NCM_TaskType.Reinforce_FrontLine, priority);
                result.add(task);
                DUtils.Log(Level.FINER, "     Reinforce frontline task added. Ter: {0} Priority: {1}", ter.getName(), priority);
            }
        }

        return result;
    }

    private static boolean calculateAndPerformWorthwhileTasks(MovePackage pack, List<NCM_Task> tasks)
    {
        GameData data = pack.Data;
        PlayerID player = pack.Player;
        IMoveDelegate mover = pack.Mover;

        NCM_Task highestPriorityTask = null;
        float highestTaskPriority = Integer.MIN_VALUE;
        for (NCM_Task task : tasks)
        {
            if(task.IsDisqualified())
                continue;
            if(task.IsCompleted())
                continue;

            float priority = task.GetPriority();
            if (priority > highestTaskPriority)
            {
                highestPriorityTask = task;
                highestTaskPriority = priority;
            }
        }
        if (highestPriorityTask != null) //If we have a good move left
        {
            highestPriorityTask.CalculateTaskRequirements();
            highestPriorityTask.RecruitUnits();
            if(highestPriorityTask.IsPlannedMoveWorthwhile(tasks))
            {
                DUtils.Log(Level.FINER, "    Task worthwhile, performing planned task.");
                highestPriorityTask.PerformTask(mover);
                highestPriorityTask.InvalidateThreatsThisTaskResists();
            }
            else
            {
                DUtils.Log(Level.FINER, "    NCM Task not worthwhile, disqualifying.");
                highestPriorityTask.Disqualify();                
            }
        }

        if(highestPriorityTask != null)
            return true;
        else
            return false;
    }

    /**
     * This method calculates and moves a group of units to its ncm target, if the move is acceptable.
     */
    private static void doNonCombatMoveForTer(MovePackage pack, Territory ter, List<Unit> terUnits, List<NCM_Task> tasks)
    {
        GameData data = pack.Data;
        IMoveDelegate mover = pack.Mover;
        PlayerID player = pack.Player;

        Territory target = NCM_TargetCalculator.CalculateNCMTargetForTerritory(data, player, ter, terUnits, tasks);
        if (target == null)
        {
            DUtils.Log(Level.FINER, "    NCM target not found for ter: {0}", ter);
            return;
        }
        float valueOfFrom = DUtils.GetValueOfLandTer(ter, data, player);          
        float valueOfHighestTo = Integer.MIN_VALUE;
        List<Territory> movedToTers = new ArrayList<Territory>();
        List<UnitGroup> ugs = DUtils.CreateSpeedSplitUnitGroupsForUnits(terUnits, ter, data);
        for (UnitGroup ug : ugs)
        {
            Route ncmRoute = ug.GetNCMRoute(target);
            if(ncmRoute == null)
                continue;
            Territory to = ncmRoute.getEnd();
            movedToTers.add(to);
            float toValue = DUtils.GetValueOfLandTer(to, data, player);
            if(toValue > valueOfHighestTo)
            {
                valueOfHighestTo = toValue;
            }
        }

        if (valueOfFrom >= valueOfHighestTo * 2) //If start from ter is more than twice as valuable as move-to
        {
            List<Float> fromDangerBeforeAndAfter = DUtils.GetTerTakeoverChanceBeforeAndAfterMoves(data, player, ter, movedToTers, terUnits, DSettings.LoadSettings().CA_NCM_determinesVulnerabilityOfFromTerAfterMoveToSeeIfToCancelMove);
            if (fromDangerBeforeAndAfter.get(1) > .5F && fromDangerBeforeAndAfter.get(0) < .5F) //If move-from-ter will be endangered after move, but wasn't before it
            {
                DUtils.Log(Level.FINER, "    Regular ncm move-to-target from {0} to {1} canceled because of endangering of more valuable from ter.", ter, target);
                return; //Then skip this move. TODO: Get code to just leave enough for ter to be safe
            }
        }
        
        DUtils.Log(Level.FINER, "    Performing regular ncm move-to-target move from {0} to {1}.", ter, target);
        Dynamix_AI.Pause();
        for (UnitGroup ug : ugs)
        {
            ug.MoveAsFarTo_NCM(target, mover);
        }
    }
}
