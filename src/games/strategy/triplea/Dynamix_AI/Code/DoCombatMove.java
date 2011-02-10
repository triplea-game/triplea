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
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Dynamix_AI.CommandCenter.StatusCenter;
import games.strategy.triplea.Dynamix_AI.DUtils;
import games.strategy.triplea.Dynamix_AI.Dynamix_AI;
import games.strategy.triplea.Dynamix_AI.Group.MovePackage;
import games.strategy.triplea.Dynamix_AI.Others.CM_Task;
import games.strategy.triplea.Dynamix_AI.Others.CM_TaskType;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.oddsCalculator.ta.AggregateResults;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.Match;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 *
 * @author Stephen
 */
public class DoCombatMove
{
    public static void doCombatMove(Dynamix_AI ai, GameData data, IMoveDelegate mover, PlayerID player)
    {
        MovePackage pack = new MovePackage(ai, data, mover, player, null, null, null);

        List<CM_Task> tasks = GenerateTasks(pack);

        DUtils.Log(Level.FINE, "  Beginning first combat move loop.");
        while (doCombatMoveLoop(pack, tasks))
        {
        }

        DUtils.Log(Level.FINE, "  Beginning second combat move loop.");
        for(CM_Task task : tasks)
        {
            if(task.IsCompleted())
            {
                if (task.IsTaskWithAdditionalRecruitsWorthwhile())
                {
                    task.PerformTask(mover);
                    task.InvalidateThreatsThisTaskResists();
                }
            }
        }

        DUtils.Log(Level.FINE, "  Beginning third combat move loop.");
        for(CM_Task task : tasks)
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

        DUtils.Log(Level.FINE, "  Beginning fourth combat move loop.");
        for(CM_Task task : tasks)
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
    }

    private static List<CM_Task> GenerateTasks(final MovePackage pack)
    {
        List<CM_Task> result = new ArrayList<CM_Task>();

        final GameData data = pack.Data;
        final PlayerID player = pack.Player;
        final Territory ourCap = TerritoryAttachment.getCapital(player, data);
        Match<Territory> isLandGrab = new Match<Territory>()
        {
            @Override
            public boolean match(Territory ter)
            {
                if (data.getAllianceTracker().isAllied(ter.getOwner(), player))
                    return false;
                if (ter.isWater())
                    return false;
                if (TerritoryAttachment.get(ter) == null)
                    return false;
                if (TerritoryAttachment.get(ter).getProduction() < 1)
                    return false;
                if (ter.getUnits().getMatches(new CompositeMatchAnd<Unit>(Matches.unitHasDefenseThatIsMoreThanOrEqualTo(1), Matches.unitIsEnemyOf(data, player), Matches.UnitIsNotAA)).size() > 0)
                    return false;

                return true;
            }
        };
        final List<Territory> capAndNeighbors = DUtils.GetTerritoriesWithinXDistanceOfY(data, ourCap, 1);
        Match<Territory> isAttack_Stabilize = new Match<Territory>()
        {
            @Override
            public boolean match(Territory ter)
            {
                if (ter.getOwner() != null && data.getAllianceTracker().isAllied(ter.getOwner(), player))
                    return false;
                if (!capAndNeighbors.contains(ter)) //If this ter is neither cap or cap neighbor, it's not a stabalization task (yet)
                    return false;

                return true;
            }
        };
        Match<Territory> isAttack_Offensive = new Match<Territory>()
        {
            @Override
            public boolean match(Territory ter)
            {
                if (ter.getOwner() != null && data.getAllianceTracker().isAllied(ter.getOwner(), player))
                    return false;

                //I decided we can have duplicates. (ie. offensive attack as well as land grab)
                //if (ter.getUnits().getMatches(new CompositeMatchAnd<Unit>(Matches.unitHasDefenseThatIsMoreThanOrEqualTo(1), Matches.unitIsEnemyOf(data, player))).isEmpty())
                //    return false;

                return true;
            }
        };
        List<Territory> tersWeCanAttack = DUtils.GetEnemyTersThatCanBeAttackedByUnitsOwnedBy(data, player);
        DUtils.Log(Level.FINEST, "  Beginning task creation loop. tersWeCanAttack: {0}", tersWeCanAttack);
        for (Territory ter : tersWeCanAttack)
        {
            //Hey, just a note to any developers reading this code:
            //    If you think it'll help, you can add in special 'combat move' tasks, that in reality just lock down units from attacking elsewhere.
            //    For example, you might want a cm task to 'lock-down' units to the cap and other important areas so the territory stays defendable.
            //    If you do make these sort of changes, though, please do it carefully, sloppy changes could mess up the code.
            if(isLandGrab.match(ter))
            {
                float priority = DUtils.GetLandGrabOnTerPriority(data, player, ter);
                CM_Task task = new CM_Task(data, ter, CM_TaskType.LandGrab, priority);
                result.add(task);
                DUtils.Log(Level.FINER, "    Land grab task added. Ter: {0} Priority: {1}", ter.getName(), priority);
            }
            else if(isAttack_Stabilize.match(ter))
            {
                if(ter.isWater())
                {
                    if(true) //For now, ignore water
                        continue;
                    List<Unit> possibleAttackers = DUtils.GetUnitsOwnedByPlayerThatCanReach(data, ter, player, Matches.TerritoryIsLandOrWater);
                    possibleAttackers = Match.getMatches(possibleAttackers, new CompositeMatchOr<Unit>(Matches.UnitIsSea, Matches.UnitIsAir));
                    AggregateResults results = DUtils.GetBattleResults(possibleAttackers, DUtils.ToList(ter.getUnits().getUnits()), ter, data, 250, true);
                    if(results.getAttackerWinPercent() < .25F)
                        continue;
                }
                else
                {
                    List<Unit> possibleAttackers = DUtils.GetUnitsOwnedByPlayerThatCanReach(data, ter, player, Matches.TerritoryIsLand);
                    possibleAttackers = Match.getMatches(possibleAttackers, new CompositeMatchOr<Unit>(Matches.UnitIsLand, Matches.UnitIsAir));
                    AggregateResults results = DUtils.GetBattleResults(possibleAttackers, DUtils.ToList(ter.getUnits().getUnits()), ter, data, 250, true);
                    if(results.getAttackerWinPercent() < .25F)
                        continue;
                }                
                float priority = DUtils.GetStabilizationAttackOnTerPriority(data, player, ter);
                CM_Task task = new CM_Task(data, ter, CM_TaskType.Attack_Stabilize, priority);
                result.add(task);
                DUtils.Log(Level.FINER, "    Attack_Stabilize task added. Ter: {0} Priority: {1}", ter.getName(), priority);
            }
            else if(isAttack_Offensive.match(ter))
            {
                if(ter.isWater())
                {
                    if(true) //For now, ignore water
                        continue;
                    List<Unit> possibleAttackers = DUtils.GetUnitsOwnedByPlayerThatCanReach(data, ter, player, Matches.TerritoryIsLandOrWater);
                    possibleAttackers = Match.getMatches(possibleAttackers, new CompositeMatchOr<Unit>(Matches.UnitIsSea, Matches.UnitIsAir));
                    AggregateResults results = DUtils.GetBattleResults(possibleAttackers, DUtils.ToList(ter.getUnits().getUnits()), ter, data, 250, true);
                    if(results.getAttackerWinPercent() < .20F)
                        continue;
                }
                else
                {
                    List<Unit> possibleAttackers = DUtils.GetUnitsOwnedByPlayerThatCanReach(data, ter, player, Matches.TerritoryIsLand);
                    possibleAttackers = Match.getMatches(possibleAttackers, new CompositeMatchOr<Unit>(Matches.UnitIsLand, Matches.UnitIsAir));
                    AggregateResults results = DUtils.GetBattleResults(possibleAttackers, DUtils.ToList(ter.getUnits().getUnits()), ter, data, 250, true);
                    if(results.getAttackerWinPercent() < .20F)
                        continue;
                }
                float priority = DUtils.GetOffensiveAttackOnTerPriority(data, player, ter);
                CM_Task task = new CM_Task(data, ter, CM_TaskType.Attack_Offensive, priority);
                result.add(task);
                DUtils.Log(Level.FINER, "    Attack_Offensive task added. Ter: {0} Priority: {1}", ter.getName(), priority);
            }
        }

        return result;
    }

    private static boolean doCombatMoveLoop(MovePackage pack, List<CM_Task> tasks)
    {
        GameData data = pack.Data;
        PlayerID player = pack.Player;
        IMoveDelegate mover = pack.Mover;

        CM_Task highestPriorityTask = null;
        float highestTaskPriority = Integer.MIN_VALUE;
        for (CM_Task task : tasks)
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
            if(highestPriorityTask.IsPlannedAttackWorthwhile(tasks))
            {
                DUtils.Log(Level.FINER, "    Task worthwhile, performing planned task.");
                highestPriorityTask.PerformTask(mover);
                if(highestPriorityTask.GetTaskType() == CM_TaskType.LandGrab)
                    StatusCenter.get(data, player).GetStatusOfTerritory(highestPriorityTask.GetTarget()).WasBlitzed = true;
                else
                    StatusCenter.get(data, player).GetStatusOfTerritory(highestPriorityTask.GetTarget()).WasAttacked = true;
                highestPriorityTask.InvalidateThreatsThisTaskResists();
            }
            else
            {
                DUtils.Log(Level.FINER, "    CM Task not worthwhile, disqualifying.");
                highestPriorityTask.Disqualify();
            }
        }

        if(highestPriorityTask != null)
            return true;
        else
            return false;
    }
}
