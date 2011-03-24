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
import games.strategy.triplea.Dynamix_AI.CommandCenter.CachedInstanceCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.GlobalCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.ReconsiderSignalCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.StatusCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.StrategyCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.TacticalCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.ThreatInvalidationCenter;
import games.strategy.triplea.Dynamix_AI.DMatches;
import games.strategy.triplea.Dynamix_AI.DSettings;
import games.strategy.triplea.Dynamix_AI.DUtils;
import games.strategy.triplea.Dynamix_AI.Dynamix_AI;
import games.strategy.triplea.Dynamix_AI.Group.MovePackage;
import games.strategy.triplea.Dynamix_AI.Group.UnitGroup;
import games.strategy.triplea.Dynamix_AI.Others.NCM_AirLandingCalculator;
import games.strategy.triplea.Dynamix_AI.Others.NCM_Call;
import games.strategy.triplea.Dynamix_AI.Others.NCM_CallType;
import games.strategy.triplea.Dynamix_AI.Others.NCM_TargetCalculator;
import games.strategy.triplea.Dynamix_AI.Others.NCM_Task;
import games.strategy.triplea.Dynamix_AI.Others.NCM_TaskType;
import games.strategy.triplea.Dynamix_AI.Others.StrategyType;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.oddsCalculator.ta.AggregateResults;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.Match;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

/**
 *
 * @author Stephen
 */
public class DoNonCombatMove
{
    public static void doPreCombatMove(Dynamix_AI ai, GameData data, IMoveDelegate mover, PlayerID player)
    {
        DUtils.Log(Level.FINE, "  Beginning pre-combat move section");
        Territory ourCap = TerritoryAttachment.getCapital(player, data);

        if (DUtils.GetTerTakeoverChanceAtEndOfTurn(data, player, ourCap) > .1F) //If our cap is in danger
        {
            float priority = DUtils.GetNCMTaskPriority_Stabalize(data, player, ourCap);
            NCM_Task task = new NCM_Task(data, ourCap, NCM_TaskType.Reinforce_Stabilize, priority);
            task.SetTaskRequirements(.1F); //Just get safe enough for task's to realize if they endanger cap
            task.RecruitUnits();
            if (task.IsPlannedMoveWorthwhile(Arrays.asList(task)))
            {
                DUtils.Log(Level.FINE, "    Pre-combat-move capital reinforcement task being performed.");
                task.PerformTask(mover);
            }

            TacticalCenter.NotifyStartOfRound(); //Clear frozen units, etc.
            ThreatInvalidationCenter.NotifyStartOfRound(); //Clear any units the cap-reinforcement invalidated
        }
    }
    public static void doNonCombatMove(Dynamix_AI ai, GameData data, IMoveDelegate mover, PlayerID player)
    {
        MovePackage pack = new MovePackage(ai, data, mover, player, null, null, null);

        //First, we generate a list of ncm 'tasks', such as reinforcements of territories, blocking of enemies, etc.
        List<NCM_Task> tasks = GenerateTasks(pack);

        //We loop this part, because sometimes there are attacks that at first are too risky to perform, but after some nearby tasks are performed, the results are more favorable or predictable.
        DUtils.Log(Level.FINE, "  Beginning task consideration loop section");
        for (int i = 0; i < 5; i++)
        {
            DUtils.Log(Level.FINE, "  Task consideration loop {0} started", i + 1);
            ReconsiderSignalCenter.get(data, player).ObjectsToReconsider.clear();

            List<Territory> tersAttackedBeforeLoop = new ArrayList<Territory>();
            for(NCM_Task task : tasks)
            {
                if(task.IsCompleted())
                    tersAttackedBeforeLoop.add(task.GetTarget());
            }

            while (considerAndPerformWorthwhileTasks(pack, tasks))
            {
            }

            if(ReconsiderSignalCenter.get(data, player).ObjectsToReconsider.isEmpty()) //If we performed no tasks, basically...
                break;
            else
            {
                List<Territory> tersToReconsider = DUtils.ToList(ReconsiderSignalCenter.get(data, player).ObjectsToReconsider);
                for (NCM_Task task : tasks)
                {
                    if (tersToReconsider.contains(task.GetTarget()))
                    {
                        if(task.IsCompleted())
                        {
                            /*if(!tersAttackedBeforeLoop.contains(task.GetTarget())) //If this ter was attacked this loop
                                continue;

                            for(UnitGroup ug : task.GetRecruitedUnits())
                                UnitGroup.UndoMove_NotifyAllUGs(mover, ug.GetMoveIndex()); //Undo moves, and calculate again, cause we might not need this many after all
                            task.Reset();*/
                        }
                        else
                            task.Reset(); //We reset disqualified tasks for another attempt (now that we know of completed tasks)
                    }
                }
            }
        }

        //Now we go through tasks, and retreat from the ones we couldn't defend
        DUtils.Log(Level.FINE, "  Performing target retreats on diqualified tasks.");
        for (NCM_Task task : tasks)
        {
            if (task.IsDisqualified())
            {
                task.PerformTargetRetreat(tasks, mover);
                StatusCenter.get(data, GlobalCenter.CurrentPlayer).GetStatusOfTerritory(task.GetTarget()).WasAbandoned = true;
            }
            else if(task.IsCompleted() && task.GetTaskType() == NCM_TaskType.Reinforce_Block)
            {
                //Blocks are basically the same as abandonings (as in, we don't want units ncm'ing here)
                StatusCenter.get(data, GlobalCenter.CurrentPlayer).GetStatusOfTerritory(task.GetTarget()).WasAbandoned = true;
            }
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

                //We now allow the worthwhile tasks to recruit even more additional units to make the task even more favorable.
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

        ThreatInvalidationCenter.get(data, player).SuspendThreatInvalidation(); //Suspend the threat invalidation center, as we want wave 4 to do as much as we'd ever want

        //We now allow the worthwhile tasks to recruit as many additional units as they could ever want
        DUtils.Log(Level.FINE, "  Calculating and adding additional task recruits. (Wave 4)");
        for(NCM_Task task : tasks)
        {
            if(task.IsCompleted())
            {
                task.RecruitUnits4();
                if (task.IsTaskWithAdditionalRecruitsWorthwhile())
                {
                    task.PerformTask(mover);
                    task.InvalidateThreatsThisTaskResists();
                }
            }
        }

        ThreatInvalidationCenter.get(data, player).ResumeThreatInvalidation();

        //We generate a list of ncm 'calls', such as calls for more units on a defensive front, a call for a unit to grab an empty territory, etc.
        List<NCM_Call> calls = GenerateCalls(pack);

        DUtils.Log(Level.FINE, "  Beginning call consideration section");
        while (considerAndPerformWorthwhileCalls(pack, calls))
        {
        }

        //Now that we've completed all the ncm tasks we could, we do the ordinary ncm move-to-target process.
        DUtils.Log(Level.FINE, "  Calculating and performing regular ncm move-to-target moves.");
        for(Territory ter : data.getMap().getTerritories())
        {
            if(DMatches.territoryIsOwnedByEnemy(data, player).match(ter))
                continue;

            List<Unit> terUnits = ter.getUnits().getMatches(DUtils.CompMatchAnd(Matches.unitIsOwnedBy(player), Matches.unitHasMovementLeft, Matches.UnitIsNotAA, Matches.UnitIsNotAir));
            terUnits.removeAll(TacticalCenter.get(data, player).GetFrozenUnits());
            if(terUnits.isEmpty())
                continue;

            doNonCombatMoveForTer(pack, ter, terUnits, tasks);
        }

        //We get clear invalidated threats, because the enemy will come after air even if landing ter is next to resistant ter
        ThreatInvalidationCenter.get(data, player).ClearInvalidatedThreats();

        //Now that we've completed all the normal ncm moves, perform the special tasks
        DUtils.Log(Level.FINE, "  Attempting to land aircraft on safe territories.");
        for(Territory ter : data.getMap().getTerritories())
        {
            List<Unit> airUnits = ter.getUnits().getMatches(DUtils.CompMatchAnd(Matches.unitIsOwnedBy(player), Matches.unitHasMovementLeft, Matches.UnitIsAir));
            if (airUnits.isEmpty())
                continue;

            Territory landingLoc = NCM_AirLandingCalculator.CalculateLandingLocationForAirUnits(data, player, ter, airUnits, tasks);
            if (landingLoc == null)
            {
                DUtils.Log(Level.FINER, "    Landing location not found. Ter: {0} Air Units: {1}", ter, DUtils.UnitList_ToString(airUnits));
                continue;
            }
            List<UnitGroup> ugs = DUtils.CreateUnitGroupsForUnits(airUnits, ter, data);
            DUtils.Log(Level.FINER, "    Landing aircraft at {0}. From: {1}.", landingLoc, ter);
            UnitGroup.EnableMoveBuffering();
            for (UnitGroup ug : ugs)
            {
                String error = ug.MoveAsFarTo_NCM(landingLoc, mover);
                if (error != null)
                    DUtils.Log(Level.FINEST, "        Landing failed, reason: {0}", error);
                else
                    TacticalCenter.get(data, player).FreezeUnits(airUnits); //Freeze these aircraft, otherwise they might get moved somewhere unsafe
            }
            String errors = UnitGroup.PerformBufferedMovesAndDisableMoveBufferring(mover);
            if(errors != null)
                DUtils.Log(Level.FINER, "      Some errors occurred while performing moves: {0}", errors);
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
                if(!DSettings.LoadSettings().TR_enableReinforceBlock)
                    return false;
                if(ter.isWater())
                    return false;
                List<Unit> attackers = DUtils.GetNNEnemyUnitsThatCanReach(data, ter, player, Matches.TerritoryIsLand);
                if(attackers.isEmpty())
                    return false;
                //If this ter was not conquered and it is not owned by us or our allies, this can't be a ter we reinforce
                if(!DMatches.territoryIsOwnedByXOrAlly(data, player).match(ter) && !CachedInstanceCenter.CachedBattleTracker.wasConquered(ter))
                    return false;
                if(Match.getMatches(DUtils.GetTerUnitsAtEndOfTurn(data, player, ter), Matches.unitHasDefenseThatIsMoreThanOrEqualTo(1)).size() > 0) //If this ter is already blocking enemies
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
                if(!DSettings.LoadSettings().TR_enableReinforceStabalize)
                    return false;
                if(ter.isWater())
                    return false;
                List<Unit> attackers = DUtils.GetNNEnemyUnitsThatCanReach(data, ter, player, Matches.TerritoryIsLand);
                if(attackers.isEmpty())
                    return false;
                //If this ter was not conquered and it is not owned by us or our allies, this can't be a ter we reinforce
                if(!DMatches.territoryIsOwnedByXOrAlly(data, player).match(ter) && !CachedInstanceCenter.CachedBattleTracker.wasConquered(ter))
                    return false;
                if (GlobalCenter.IsFFAGame)
                {
                    if (!capsAndNeighbors.contains(ter))
                        return false;
                }
                else
                {
                    if (!ourCaps.contains(ter))
                        return false;
                }

                return true;
            }
        };
        Match<Territory> isReinforce_FrontLine = new Match<Territory>()
        {
            @Override
            public boolean match(Territory ter)
            {
                if(!DSettings.LoadSettings().TR_enableReinforceFrontLine)
                    return false;
                if(ter.isWater())
                    return false;
                //A territory is a frontline territory, if: (Might be outdated, btw)
                //    There are units that can attack the ter
                //    The territory is owned by us or an ally, or it was attacked this round
                //    The territory has at least one non-null-enemy owned neighbor
                //    The territory is either a link between two friendly neighbors, or has at least one unique enemy neighbor
                //        (though we don't let empty, friendly neighbors cause one of our enemy neighbors to be removed from the 'unique' list)
                if (!data.getAllianceTracker().isAllied(ter.getOwner(), player) && !StatusCenter.get(data, player).GetStatusOfTerritory(ter).WasAttacked_Normal)
                    return false;
                //If this ter was not conquered and it is not owned by us or our allies, this can't be a ter we reinforce
                if(!DMatches.territoryIsOwnedByXOrAlly(data, player).match(ter) && !CachedInstanceCenter.CachedBattleTracker.wasConquered(ter))
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
                    if(neighbor.getUnits().size() <= 1) //We don't care if an almost empty friendly neighbor shares our currently unique enemy neighbors...
                        continue;

                    List<Territory> n_EnemyNeighbors = DUtils.ToList(data.getMap().getNeighbors(neighbor, DMatches.territoryIsOwnedByNNEnemy(data, player)));

                    uniqueEnemyNeighbors.removeAll(n_EnemyNeighbors);
                }

                if(uniqueEnemyNeighbors.isEmpty() && !isTerLinkBetweenFriendlies) //If this ter does not have unique enemy neighbors, and is not a link between two friendlies, we must not be a front
                    return false;

                List<Unit> attackers = DUtils.GetNNEnemyUnitsThatCanReach(data, ter, player, Matches.TerritoryIsLand);
                if(attackers.isEmpty())
                    return false; //If there are no attackers

                return true;
            }
        };
        List<Territory> tersWeCanMoveTo = DUtils.GetLandTersThatCanBeReinforcedByUnitsOwnedBy(data, player);
        DUtils.Log(Level.FINE, "  Beginning task creation loop.");
        for (Territory ter : tersWeCanMoveTo)
        {
            if(isReinforce_Block.match(ter))
            {
                float priority = DUtils.GetNCMTaskPriority_Block(data, player, ter);
                NCM_Task task = new NCM_Task(data, ter, NCM_TaskType.Reinforce_Block, priority);
                result.add(task);
                DUtils.Log(Level.FINER, "     Reinforce block task added. Ter: {0} Priority: {1}", ter.getName(), priority);
            }
            else if(isReinforce_Stabilize.match(ter))
            {
                float priority = DUtils.GetNCMTaskPriority_Stabalize(data, player, ter);
                NCM_Task task = new NCM_Task(data, ter, NCM_TaskType.Reinforce_Stabilize, priority);
                result.add(task);
                DUtils.Log(Level.FINER, "     Reinforce stabalize task added. Ter: {0} Priority: {1}", ter.getName(), priority);
            }
            else if(isReinforce_FrontLine.match(ter))
            {
                float priority = DUtils.GetNCMTaskPriority_Frontline(data, player, ter);
                NCM_Task task = new NCM_Task(data, ter, NCM_TaskType.Reinforce_FrontLine, priority);
                result.add(task);
                DUtils.Log(Level.FINER, "     Reinforce frontline task added. Ter: {0} Priority: {1}", ter.getName(), priority);
            }
        }

        return result;
    }

    private static boolean considerAndPerformWorthwhileTasks(MovePackage pack, List<NCM_Task> tasks)
    {
        GameData data = pack.Data;
        PlayerID player = pack.Player;
        IMoveDelegate mover = pack.Mover;

        //We could also just sort the tasks by priority, then go through the list
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
                DUtils.Log(Level.FINER, "      Task worthwhile, performing planned task.");
                highestPriorityTask.PerformTask(mover);
                highestPriorityTask.InvalidateThreatsThisTaskResists();
            }
            else
            {
                highestPriorityTask.Disqualify();                
            }
        }

        if(highestPriorityTask != null)
            return true;
        else
            return false;
    }

    private static List<NCM_Call> GenerateCalls(final MovePackage pack)
    {
        List<NCM_Call> result = new ArrayList<NCM_Call>();

        final GameData data = pack.Data;
        final PlayerID player = pack.Player;
        Match<Territory> isLandGrabCall = new Match<Territory>()
        {
            @Override
            public boolean match(Territory ter)
            {
                if(!DSettings.LoadSettings().CR_enableCallForLandGrab)
                    return false;
                if (ter.isWater())
                    return false;
                if (TerritoryAttachment.get(ter) == null || TerritoryAttachment.get(ter).isImpassible())
                    return false;
                if (data.getAllianceTracker().isAllied(ter.getOwner(), player))
                    return false;
                if (TerritoryAttachment.get(ter).getProduction() < 1)
                    return false;
                if (ter.getUnits().getMatches(new CompositeMatchAnd<Unit>(Matches.unitHasDefenseThatIsMoreThanOrEqualTo(1), Matches.unitIsEnemyOf(data, player), Matches.UnitIsNotAA)).size() > 0)
                    return false;
                if(Match.getMatches(DUtils.GetUnitsOwnedByPlayerThatCanReach(data, ter, player, Matches.TerritoryIsLand), Matches.UnitIsLand).size() > 0) //If we have no units that can already grab it
                    return false;
                if(data.getMap().getNeighbors(ter, DMatches.territoryIsOwnedByXOrAlly(data, player)).isEmpty()) //If this ter is within enemy territory
                    return false;

                return true;
            }
        };
        Match<Territory> isDefensiveFrontCall = new Match<Territory>()
        {
            @Override
            public boolean match(Territory ter)
            {
                if(!DSettings.LoadSettings().CR_enableCallForDefensiveFront)
                    return false;
                if (ter.isWater())
                    return false;
                if (TerritoryAttachment.get(ter) == null || TerritoryAttachment.get(ter).isImpassible())
                    return false;
                if (!data.getAllianceTracker().isAllied(ter.getOwner(), player))
                    return false;

                List<Unit> attackers = DUtils.GetNNEnemyUnitsThatCanReach(data, ter, player, Matches.TerritoryIsLand);
                if(attackers.isEmpty())
                    return false;

                List<Unit> spAttackers = DUtils.GetSPUnitsInList(attackers);
                if(spAttackers.isEmpty())
                    return false;

                PlayerID mainAttacker = spAttackers.get(0).getOwner();
                StrategyType strategy = StrategyCenter.get(data, player).GetCalculatedStrategyAssignments().get(mainAttacker);
                if(strategy != StrategyType.Enemy_Defensive)
                    return false;

                List<Unit> defenders = DUtils.GetUnitsOwnedByPlayerThatCanReach(data, ter, player, Matches.TerritoryIsLand);
                defenders.removeAll(DUtils.GetTerUnitsAtEndOfTurn(data, player, ter));
                defenders.addAll(DUtils.GetTerUnitsAtEndOfTurn(data, player, ter));

                AggregateResults results = DUtils.GetBattleResults(attackers, defenders, ter, data, 50, true);
                if(results.getAttackerWinPercent() < .25F) //If the attacker has low chance of taking ter, we don't need to send units over here
                    return false;

                return true;
            }
        };
        Match<Territory> isCapitalDefenseCall = new Match<Territory>()
        {
            @Override
            public boolean match(Territory ter)
            {
                //TODO
                if(true)
                    return false;

                return true;
            }
        };
        List<Territory> tersWeCanCallTo = DUtils.ToList(data.getMap().getTerritories());
        DUtils.Log(Level.FINE, "  Beginning call creation loop.");
        for (Territory ter : tersWeCanCallTo)
        {
            if(isLandGrabCall.match(ter))
            {
                float priority = DUtils.GetNCMCallPriority_ForLandGrab(data, player, ter);
                NCM_Call task = new NCM_Call(data, ter, NCM_CallType.Call_ForLandGrab, priority);
                result.add(task);
                DUtils.Log(Level.FINER, "     For land grab call added. Ter: {0} Priority: {1}", ter.getName(), priority);
            }
            else if(isDefensiveFrontCall.match(ter))
            {
                float priority = DUtils.GetNCMCallPriority_ForDefensiveFront(data, player, ter);
                NCM_Call task = new NCM_Call(data, ter, NCM_CallType.Call_ForDefensiveFront, priority);
                result.add(task);
                DUtils.Log(Level.FINER, "     For defensive front call added. Ter: {0} Priority: {1}", ter.getName(), priority);
            }
            else if(isCapitalDefenseCall.match(ter))
            {
                float priority = DUtils.GetNCMCallPriority_ForDefensiveFront(data, player, ter);
                NCM_Call task = new NCM_Call(data, ter, NCM_CallType.Call_ForCapitalDefense, priority);
                result.add(task);
                DUtils.Log(Level.FINER, "     For capital defense call added. Ter: {0} Priority: {1}", ter.getName(), priority);
            }
        }

        return result;
    }

    private static boolean considerAndPerformWorthwhileCalls(MovePackage pack, List<NCM_Call> calls)
    {
        GameData data = pack.Data;
        PlayerID player = pack.Player;
        IMoveDelegate mover = pack.Mover;

        //We could also just sort the calls by priority, then go through the list
        NCM_Call highestPriorityCall = null;
        float highestCallPriority = Integer.MIN_VALUE;
        for (NCM_Call call : calls)
        {
            if(call.IsDisqualified())
                continue;
            if(call.IsCompleted())
                continue;

            float priority = call.GetPriority();
            if (priority > highestCallPriority)
            {
                highestPriorityCall = call;
                highestCallPriority = priority;
            }
        }
        if (highestPriorityCall != null) //If we have a good move left
        {
            highestPriorityCall.CalculateCallRequirements();
            highestPriorityCall.RecruitUnits();
            if(highestPriorityCall.IsPlannedMoveWorthwhile(calls))
            {
                DUtils.Log(Level.FINER, "      Call worthwhile, performing planned call.");
                highestPriorityCall.PerformCall(mover);
            }
            else
            {
                highestPriorityCall.Disqualify();
            }
        }

        if(highestPriorityCall != null)
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
            Route ncmRoute = ug.GetNCMRoute(target, true);
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
            List<Float> fromDangerBeforeAndAfter = DUtils.GetTerTakeoverChanceBeforeAndAfterMoves(data, player, ter, movedToTers, terUnits, DSettings.LoadSettings().CA_NCM_determinesSurvivalChanceOfFromTerAfterMoveToSeeIfToCancelMove);
            if (fromDangerBeforeAndAfter.get(1) > .5F && fromDangerBeforeAndAfter.get(0) < .5F) //If move-from-ter will be endangered after move, but wasn't before it
            {
                DUtils.Log(Level.FINER, "    Regular ncm move-to-target from {0} to {1} canceled because of endangering of more valuable from ter. Units: {2}", ter, target, DUtils.UnitGroupList_ToString(ugs));
                return; //Then skip this move. TODO: Get code to just leave enough for ter to be safe
            }
        }
        
        DUtils.Log(Level.FINER, "    Performing regular ncm move-to-target move from {0} to {1}. Units: {2}", ter, target, DUtils.UnitGroupList_ToString(ugs));
        UnitGroup.EnableMoveBuffering();
        for (UnitGroup ug : ugs)
        {
            String error = ug.MoveAsFarTo_NCM(target, mover, true);
            if (error != null)
                DUtils.Log(Level.FINER, "      NCM move-to-target move or preparation failed, reason: {0}", error);
            else
                Dynamix_AI.Pause();
        }
        String errors = UnitGroup.PerformBufferedMovesAndDisableMoveBufferring(mover);
        if(errors != null)
            DUtils.Log(Level.FINER, "      Some errors occurred while performing moves: {0}", errors);
    }
}
