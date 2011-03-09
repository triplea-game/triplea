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

package games.strategy.triplea.Dynamix_AI.Others;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Dynamix_AI.CommandCenter.CachedCalculationCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.GlobalCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.StatusCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.TacticalCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.ThreatInvalidationCenter;
import games.strategy.triplea.Dynamix_AI.DMatches;
import games.strategy.triplea.Dynamix_AI.DSettings;
import games.strategy.triplea.Dynamix_AI.DSorting;
import games.strategy.triplea.Dynamix_AI.DUtils;
import games.strategy.triplea.Dynamix_AI.Dynamix_AI;
import games.strategy.triplea.Dynamix_AI.Group.UnitGroup;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.oddsCalculator.ta.AggregateResults;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.Match;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;

/**
 *
 * @author Stephen
 */
public class NCM_Task
{
    private Territory m_target = null;
    private NCM_TaskType m_taskType = NCM_TaskType.Empty;
    private float m_priority = 0.0F;
    private GameData m_data = null;
    public NCM_Task(GameData data, Territory target, NCM_TaskType type, float priority)
    {
        m_data = data;
        m_taskType = type;
        m_priority = priority;
        m_target = target;
    }

    public Territory GetTarget()
    {
        return m_target;
    }

    public NCM_TaskType GetTaskType()
    {
        return m_taskType;
    }

    public float GetPriority()
    {
        return m_priority;
    }

    public void SetPriority(float priority)
    {
        m_priority = priority;
    }

    public void IncreasePriority(float increase)
    {
        m_priority = m_priority + increase;
    }

    public void DecreasePriority(float increase)
    {
        m_priority = m_priority - increase;
    }

    private List<UnitGroup> getSortedPossibleRecruits()
    {
        final HashMap<Unit, Territory> unitLocations = new HashMap<Unit, Territory>();
        final HashMap<Unit, Integer> possibles = new HashMap<Unit, Integer>();
        for (final Territory ter : m_data.getMap().getTerritories())
        {
            if(DMatches.territoryContainsMultipleAlliances(m_data).match(ter)) //If we're battling here
                continue;
            Match<Unit> unitMatch = new Match<Unit>()
            {
                @Override
                public boolean match(Unit unit)
                {
                    UnitAttachment ua = UnitAttachment.get(unit.getUnitType());
                    if (!DUtils.CanUnitReachTer(m_data, ter, unit, m_target))
                        return false;
                    if (!Matches.unitIsOwnedBy(GlobalCenter.CurrentPlayer).match(unit))
                        return false;
                    if (Matches.UnitIsFactory.match(unit) && ua.getDefense(unit.getOwner()) <= 0)
                        return false;
                    if (Matches.UnitIsAA.match(unit))
                        return false;

                    return true;
                }
            };
            List<Unit> units = Match.getMatches(DUtils.ToList(ter.getUnits().getUnits()), unitMatch);
            if (units.isEmpty())
                continue;

            for (Unit unit : units)
            {
                int suitability = DUtils.HowWellIsUnitSuitedToTask(m_data, this, ter, unit);
                if(suitability == Integer.MIN_VALUE)
                    continue;
                possibles.put(unit, suitability);
                unitLocations.put(unit, ter);
            }
        }

        List<Unit> sortedPossibles = DUtils.ToList(possibles.keySet());
        //For now, shuffle,
        Collections.shuffle(sortedPossibles);
        //Then sort by score. In this way, equal scored attack units are shuffled
        sortedPossibles = DSorting.SortListByScores_List_D(sortedPossibles, possibles.values());

        //Now put the units into UnitGroups and return the list
        List<UnitGroup> result = new ArrayList<UnitGroup>();
        for(Unit unit : sortedPossibles)
            result.add(DUtils.CreateUnitGroupForUnits(Collections.singletonList(unit), unitLocations.get(unit), m_data));
        return result;
    }

    private float m_minSurvivalChance = 0.0F;
    public void CalculateTaskRequirements()
    {
        if (m_taskType.equals(NCM_TaskType.Reinforce_Block))
            return; //Only one unit needed for block

        if (m_taskType == NCM_TaskType.Reinforce_FrontLine)
            m_minSurvivalChance = DUtils.ToFloat(DSettings.LoadSettings().TR_reinforceFrontLine_EnemyAttackSurvivalChanceRequired);
        else if (m_taskType.equals(m_taskType.Reinforce_Stabilize))
            m_minSurvivalChance = DUtils.ToFloat(DSettings.LoadSettings().TR_reinforceStabalize_EnemyAttackSurvivalChanceRequired);

        //DUtils.Log(Level.FINER, "    NCM Task requirements calculated. Min Survival Chance: {0}", m_minSurvivalChance);
    }

    public void SetTaskRequirements(float minSurvivalChance)
    {
        m_minSurvivalChance = minSurvivalChance;

        //DUtils.Log(Level.FINER, "    NCM Task requirements set. Min Survival Chance: {0}", m_minSurvivalChance);
    }

    private float getMeetingOfMinSurvivalChanceScore(AggregateResults simulatedAttack, float minSurvivalChance)
    {
        if(m_taskType.equals(NCM_TaskType.Reinforce_Block))
        {
            if(m_recruitedUnits.size() > 0)
                return 1.0F; //Has reached, but not exceeded
            else
                return 0.0F;
        }

        if(StatusCenter.get(m_data, GlobalCenter.CurrentPlayer).GetStatusOfTerritory(m_target).WasAttacked_Normal)
            return 1.0F; //If this ter was attacked, we don't care if we can't survive here (hmmm... not sure if we should keep this)

        return DUtils.Divide_SL((float)simulatedAttack.getDefenderWinPercent(), minSurvivalChance); //We're this close to meeting our min survival chance
    }

    private float getMeetingOfMaxBattleVolleysScore(AggregateResults simulatedAttack, int maxBattleVolleys)
    {
        if(m_taskType.equals(NCM_TaskType.Reinforce_Block))
        {
            if(m_recruitedUnits.size() > 0)
                return 1.0F; //Has reached, but not exceeded
            else
                return 0.0F;
        }
        if(simulatedAttack.getAttackerWinPercent() > .5F) //If the enemy actually has the better chance of winning this battle
            return 0.0F; //Then count low battle volley score as something bad

        return DUtils.Divide_SL(maxBattleVolleys, (float)simulatedAttack.getAverageBattleRoundsFought()); //We're this close to getting the average battle volley count below max amount
    }

    private List<UnitGroup> m_recruitedUnits = new ArrayList<UnitGroup>();
    public void RecruitUnits()
    {
        recruitEnoughUnitsToMeetXYZ(m_minSurvivalChance, 100);
    }

    public void RecruitUnits2()
    {
        float minSurvivalChance = .90F;
        int maxBattleVolleys = 3; //We want to destroy attackers in three volleys

        recruitEnoughUnitsToMeetXYZ(minSurvivalChance, maxBattleVolleys);
    }

    public void RecruitUnits3()
    {
        float minSurvivalChance = 1.0F;
        int maxBattleVolleys = 1; //We want to destroy attackers in one volley

        if(m_taskType.equals(NCM_TaskType.Reinforce_Block))
            return; //Only one unit needed for land grab

        recruitEnoughUnitsToMeetXYZ(minSurvivalChance, maxBattleVolleys);
    }

    private void recruitEnoughUnitsToMeetXYZ(float minSurvivalChance, int maxBattleVolleys)
    {
        if(m_taskType.equals(NCM_TaskType.Reinforce_Block) && m_recruitedUnits.size() > 0)
            return; //We only need one unit

        List<UnitGroup> sortedPossibles = getSortedPossibleRecruits();
        if(sortedPossibles.isEmpty())
            return;

        for (UnitGroup ug : sortedPossibles)
        {
            if(m_recruitedUnits.contains(ug)) //If already recruited
                continue;

            List<Unit> attackers = DUtils.GetSPNNEnemyUnitsThatCanReach(m_data, m_target, GlobalCenter.CurrentPlayer, Matches.TerritoryIsLand);
            List<Unit> defenders = GetRecruitedUnitsAsUnitList();

            AggregateResults simulatedAttack = DUtils.GetBattleResults(attackers, defenders, m_target, m_data, 1, true);

            float howCloseToMeetingMinSurvivalChance = getMeetingOfMinSurvivalChanceScore(simulatedAttack, minSurvivalChance);
            if (howCloseToMeetingMinSurvivalChance < DUtils.ToFloat(DSettings.LoadSettings().AA_percentOfMeetingOfEnemyAttackSurvivalConstantNeededToPerformNCMTask) + .02F)
            {
                m_recruitedUnits.add(ug);
                continue;
            }

            float howCloseToMeetingBattleVolleyMax = getMeetingOfMaxBattleVolleysScore(simulatedAttack, maxBattleVolleys);
            if (howCloseToMeetingBattleVolleyMax < .98F)
            {
                m_recruitedUnits.add(ug);
                continue;
            }

            break; //We've met all requirements
        }

        m_recruitedUnits = m_recruitedUnits.subList(0, Math.max(0, m_recruitedUnits.size() - 7)); //Backtrack 7 units

        //Now do it carefully
        for (UnitGroup ug : sortedPossibles)
        {
            if(m_recruitedUnits.contains(ug)) //If already recruited
                continue;

            List<Unit> attackers = DUtils.GetSPNNEnemyUnitsThatCanReach(m_data, m_target, GlobalCenter.CurrentPlayer, Matches.TerritoryIsLand);
            List<Unit> defenders = GetRecruitedUnitsAsUnitList();

            AggregateResults simulatedAttack = DUtils.GetBattleResults(attackers, defenders, m_target, m_data, DSettings.LoadSettings().CA_CMNCM_determinesIfTasksRequirementsAreMetEnoughForRecruitingStop, true);

            float howCloseToMeetingMinSurvivalChance = getMeetingOfMinSurvivalChanceScore(simulatedAttack, minSurvivalChance);
            if (howCloseToMeetingMinSurvivalChance < DUtils.ToFloat(DSettings.LoadSettings().AA_percentOfMeetingOfEnemyAttackSurvivalConstantNeededToPerformNCMTask) + .02F)
            {
                m_recruitedUnits.add(ug);
                continue;
            }

            float howCloseToMeetingBattleVolleyMax = getMeetingOfMaxBattleVolleysScore(simulatedAttack, maxBattleVolleys);
            if (howCloseToMeetingBattleVolleyMax < .98F)
            {
                m_recruitedUnits.add(ug);
                continue;
            }

            break; //We've met all requirements
        }
    }

    public List<UnitGroup> GetRecruitedUnits()
    {
        return m_recruitedUnits;
    }

    public List<Unit> GetRecruitedUnitsAsUnitList()
    {
        List<Unit> result = new ArrayList<Unit>();
        for(UnitGroup ug : m_recruitedUnits)
        {
            result.addAll(ug.GetUnits());
        }
        return result;
    }
    public boolean IsPlannedMoveWorthwhile(List<NCM_Task> allTasks)
    {
        DUtils.Log(Level.FINEST, "      Determining if ncm task is worthwhile. Target: {0} Recruits Size: {1}", m_target, m_recruitedUnits.size());

        //if(m_recruitedUnits.isEmpty()) //Remove check, as a reinforce task can sometimes have requirements met without any units recruited (no threats to, for example, a cap neighbor)
        //    return false;

        PlayerID player = GlobalCenter.CurrentPlayer;

        List<Territory> ourCaps = TerritoryAttachment.getAllCapitals(player, m_data);

        List<Territory> capsAndNeighbors = new ArrayList<Territory>();
        for (Territory cap : ourCaps)
            capsAndNeighbors.addAll(DUtils.GetTerritoriesWithinXDistanceOfY(m_data, cap, 1));
        HashSet<Unit> capsAndNeighborsUnits = DUtils.ToHashSet(DUtils.GetUnitsInTerritories(capsAndNeighbors));
        boolean areRecruitsFromCapsOrNeighbors = false;
        for (Unit recruit : GetRecruitedUnitsAsUnitList())
        {
            if (capsAndNeighborsUnits.contains(recruit))
            {
                areRecruitsFromCapsOrNeighbors = true;
                break;
            }
        }
        if (areRecruitsFromCapsOrNeighbors)
        {
            Territory ourClosestCap = DUtils.GetOurClosestCap(m_data, player, m_target);
            List<Unit> recruits = DUtils.CombineCollections(GetRecruitedUnitsAsUnitList(), DUtils.GetUnitsGoingToBePlacedAtX(m_data, player, m_target));
            List<Float> capTakeoverChances = DUtils.GetTerTakeoverChanceBeforeAndAfterMove(m_data, player, ourClosestCap, m_target, recruits, DSettings.LoadSettings().CA_CMNCM_determinesIfTaskEndangersCap);
            if (capTakeoverChances.get(1) > .1F) //If takeover chance is 10% or more after move
            {
                //And takeover chance before and after move is at least 1% different or there average attackers left before and after move is at least 1 different
                if (capTakeoverChances.get(1) - capTakeoverChances.get(0) > .01F || capTakeoverChances.get(3) - capTakeoverChances.get(2) > 1)
                {
                    DUtils.Log(Level.FINEST, "      Perfoming task would endanger capital, so canceling.");
                    return false;
                }
            }
        }

        List<Unit> attackers = DUtils.GetSPNNEnemyUnitsThatCanReach(m_data, m_target, GlobalCenter.CurrentPlayer, Matches.TerritoryIsLand);
        List<Unit> defenders = GetRecruitedUnitsAsUnitList();

        AggregateResults simulatedAttack = DUtils.GetBattleResults(attackers, defenders, m_target, m_data, DSettings.LoadSettings().CA_CMNCM_determinesResponseResultsToSeeIfTaskWorthwhile, true);

        if (m_taskType.equals(m_taskType.Reinforce_Block))
        {
            if(m_recruitedUnits.isEmpty())
                return false;

            Territory startTer = m_recruitedUnits.get(0).GetStartTerritory(); //Reinforce_Block's are done with only one unit
            Route route = m_data.getMap().getRoute(startTer, m_target, Matches.TerritoryIsLand);

            int unitCost = DUtils.GetTUVOfUnits(GetRecruitedUnitsAsUnitList(), GlobalCenter.CurrentPlayer, GlobalCenter.GetPUResource());
            TerritoryAttachment ta = TerritoryAttachment.get(m_target);

            if(ta.getProduction() < unitCost - 1 && attackers.size() > 0)
                return false;

            return true;
        }
        else if (m_taskType.equals(NCM_TaskType.Reinforce_FrontLine))
        {
            float howCloseToMeetingMinSurvivalChance = getMeetingOfMinSurvivalChanceScore(simulatedAttack, m_minSurvivalChance);
            float percentOfRequirementNeeded_SurvivalChance = DUtils.ToFloat(DSettings.LoadSettings().AA_percentOfMeetingOfEnemyAttackSurvivalConstantNeededToPerformNCMTask);
            DUtils.Log(Level.FINEST, "        How close to meeting min survival chance: {0} Needed: {1}", howCloseToMeetingMinSurvivalChance, percentOfRequirementNeeded_SurvivalChance);

            if (howCloseToMeetingMinSurvivalChance < percentOfRequirementNeeded_SurvivalChance)
                return false;

            return true; //We've met all requirements
        }
        else
        {
            float howCloseToMeetingMinSurvivalChance = getMeetingOfMinSurvivalChanceScore(simulatedAttack, m_minSurvivalChance);
            float percentOfRequirementNeeded_SurvivalChance = DUtils.ToFloat(DSettings.LoadSettings().AA_percentOfMeetingOfEnemyAttackSurvivalConstantNeededToPerformNCMTask);
            DUtils.Log(Level.FINEST, "        How close to meeting min survival chance: {0} Needed: {1}", howCloseToMeetingMinSurvivalChance, percentOfRequirementNeeded_SurvivalChance);

            if (howCloseToMeetingMinSurvivalChance < percentOfRequirementNeeded_SurvivalChance)
                return false;

            return true; //We've met all requirements
        }
    }

    public boolean IsTaskWithAdditionalRecruitsWorthwhile()
    {
        DUtils.Log(Level.FINEST, "      Determining if ncm task with additional recruits is worthwhile. Target: {0} Recruits Size: {1}", m_target, m_recruitedUnits.size());

        if (m_recruitedUnits.isEmpty()) //Can happen if all recruits are waiting for reinforcements to complete a better, nearby task
            return false;

        PlayerID player = GlobalCenter.CurrentPlayer;

        List<Territory> ourCaps = TerritoryAttachment.getAllCapitals(player, m_data);

        List<Territory> capsAndNeighbors = new ArrayList<Territory>();
        for (Territory cap : ourCaps)
            capsAndNeighbors.addAll(DUtils.GetTerritoriesWithinXDistanceOfY(m_data, cap, 1));
        HashSet<Unit> capsAndNeighborsUnits = DUtils.ToHashSet(DUtils.GetUnitsInTerritories(capsAndNeighbors));
        boolean areRecruitsFromCapsOrNeighbors = false;
        for (Unit recruit : GetRecruitedUnitsAsUnitList())
        {
            if (capsAndNeighborsUnits.contains(recruit))
            {
                areRecruitsFromCapsOrNeighbors = true;
                break;
            }
        }
        if (areRecruitsFromCapsOrNeighbors)
        {
            Territory ourClosestCap = DUtils.GetOurClosestCap(m_data, player, m_target);
            List<Float> capTakeoverChances = DUtils.GetTerTakeoverChanceBeforeAndAfterMove(m_data, player, ourClosestCap, m_target, GetRecruitedUnitsAsUnitList(), DSettings.LoadSettings().CA_CMNCM_determinesIfTaskEndangersCap);
            if (capTakeoverChances.get(1) > .1F) //If takeover chance is 10% or more after move
            {
                //And takeover chance before and after move is at least 1% different or there average attackers left before and after move is at least 1 different
                if (capTakeoverChances.get(1) - capTakeoverChances.get(0) > .01F || capTakeoverChances.get(3) - capTakeoverChances.get(2) > 1)
                {
                    DUtils.Log(Level.FINEST, "      Perfoming task with additional recruits would endanger capital, so canceling.");
                    return false;
                }
            }
        }

        return true;
    }

    private boolean m_disqualified = false;
    public boolean IsDisqualified()
    {
        return m_disqualified;
    }

    public void Disqualify()
    {
        m_disqualified = true;
    }

    public void PerformTargetRetreat(List<NCM_Task> allTasks, IMoveDelegate mover)
    {
        DUtils.Log(Level.FINER, "      Attemping to perform target retreat for task. Target: {0} Recruits: {1}", m_target, DUtils.UnitGroupList_ToString(m_recruitedUnits));

        PlayerID player = GlobalCenter.CurrentPlayer;
        
        List<UnitGroup> retreatUnits = new ArrayList<UnitGroup>();

        //If we're retreating from this ter, retreat all non air units on this ter
        retreatUnits.add(DUtils.CreateUnitGroupForUnits(Match.getMatches(m_target.getUnits().getUnits(), DUtils.CompMatchAnd(Matches.unitIsOwnedBy(player), DMatches.UnitIsNonAAMoveableType, Matches.UnitIsNotAir)), m_target, m_data));

        if(retreatUnits.get(0).GetUnits().isEmpty())
        {
            DUtils.Log(Level.FINER, "        No units to retreat for task. Target: {0}", m_target);
            return; //We have nothing to do, because there are no retreat units
        }

        //Have the frontline move to a safe frontline territory, if existing, otherwise, move to safest neighbor.
        if (m_taskType.equals(NCM_TaskType.Reinforce_FrontLine))
        {
            Territory bestRetreatTer = null;
            float bestRetreatTerScore = Integer.MIN_VALUE;
            for(NCM_Task task : (List<NCM_Task>)DUtils.ShuffleList(allTasks)) //Shuffle, so our retreat isn't predicatable
            {
                if(task.IsCompleted() && (task.GetTaskType().equals(NCM_TaskType.Reinforce_FrontLine) || task.GetTaskType().equals(NCM_TaskType.Reinforce_Stabilize)))
                {
                    Route ncmRoute = m_data.getMap().getLandRoute(m_target, task.GetTarget());
                    if (ncmRoute == null)
                        continue;
                    if (Match.allMatch(retreatUnits, DMatches.UnitGroupHasEnoughMovement_All(ncmRoute.getLength()))) //If this is a valid, reachable frontline territory
                    {
                        List<Unit> possibleAttackers = DUtils.GetSPNNEnemyUnitsThatCanReach(m_data, task.GetTarget(), GlobalCenter.CurrentPlayer, Matches.TerritoryIsLand);
                        possibleAttackers = Match.getMatches(possibleAttackers, new CompositeMatchOr<Unit>(Matches.UnitIsLand, Matches.UnitIsAir));
                        List<Unit> defenders = DUtils.GetTerUnitsAtEndOfTurn(m_data, player, task.GetTarget());
                        defenders.retainAll(TacticalCenter.get(m_data, player).GetFrozenUnits()); //Only count units that have been frozen here
                        defenders.removeAll(DUtils.ToUnitList(retreatUnits)); //(Don't double add)
                        defenders.addAll(DUtils.ToUnitList(retreatUnits)); //And the units we're retreating
                        AggregateResults results = DUtils.GetBattleResults(possibleAttackers, defenders, task.GetTarget(), m_data, 500, true);

                        float score = 0;
                        score -= results.getAttackerWinPercent();
                        score -= (DUtils.GetDefenseScoreOfUnits(results.GetAverageAttackingUnitsRemaining()) * .01F); //Have leftover invader strength only decide if takeover chances match

                        if (score > bestRetreatTerScore)
                        {
                            bestRetreatTer = task.GetTarget();
                            bestRetreatTerScore = score;
                        }
                    }
                }
            }

            if(bestRetreatTer == null) //If we couldn't find any completed, reachable frontline ters to retreat to
            {
                for(Territory ter : (List<Territory>)DUtils.ShuffleList(DUtils.GetTerritoriesWithinXDistanceOfY(m_data, m_target, GlobalCenter.FastestUnitMovement))) //Shuffle, so our retreat isn't predicatable
                {
                    if(ter.isWater())
                        continue;
                    if(!DMatches.territoryIsOwnedByXOrAlly(m_data, GlobalCenter.CurrentPlayer).match(ter))
                        continue;

                    Route ncmRoute = m_data.getMap().getLandRoute(m_target, ter);
                    if (ncmRoute == null)
                        continue;
                    if (Match.allMatch(retreatUnits, DMatches.UnitGroupHasEnoughMovement_All(ncmRoute.getLength()))) //If this is a valid, reachable reinforce ter
                    {
                        List<Unit> possibleAttackers = DUtils.GetSPNNEnemyUnitsThatCanReach_CountXAsPassthrough(m_data, ter, GlobalCenter.CurrentPlayer, Matches.TerritoryIsLand, m_target);
                        possibleAttackers = Match.getMatches(possibleAttackers, new CompositeMatchOr<Unit>(Matches.UnitIsLand, Matches.UnitIsAir));
                        List<Unit> defenders = DUtils.GetTerUnitsAtEndOfTurn(m_data, player, ter);
                        defenders.retainAll(TacticalCenter.get(m_data, player).GetFrozenUnits()); //Only count units that have been frozen here
                        defenders.removeAll(DUtils.ToUnitList(retreatUnits)); //(Don't double add)
                        defenders.addAll(DUtils.ToUnitList(retreatUnits)); //And the units we're retreating
                        AggregateResults results = DUtils.GetBattleResults(possibleAttackers, defenders, ter, m_data, 500, true);

                        float score = 0;
                        score -= results.getAttackerWinPercent() * 1000;
                        score -= results.GetAverageAttackingUnitsRemaining().size();
                        score -= DUtils.GetDefenseScoreOfUnits(results.GetAverageAttackingUnitsRemaining()); //Have leftover invader strength only decide if takeover chances match

                        if (score > bestRetreatTerScore)
                        {
                            bestRetreatTer = ter;
                            bestRetreatTerScore = score;
                        }
                    }
                }
            }

            if(bestRetreatTer != null)
            {                
                DUtils.Log(Level.FINER, "      Attempting to perform target retreat. Target: {0} Retreat To: {1} Retreat Units: {2}", m_target, bestRetreatTer, DUtils.UnitGroupList_ToString(retreatUnits));
                Dynamix_AI.Pause();
                UnitGroup.EnableMoveBuffering();
                for (UnitGroup ug : retreatUnits)
                {
                    String error = ug.MoveAsFarTo_NCM(bestRetreatTer, mover);
                    if(error == null)
                        TacticalCenter.get(m_data, GlobalCenter.CurrentPlayer).FreezeUnits(ug.GetUnitsAsList());                        
                    else
                        DUtils.Log(Level.FINER, "        NCM move failed, reason: {0}", error);
                }
                UnitGroup.PerformBufferedMovesAndDisableMoveBufferring(mover);
            }
            else
                DUtils.Log(Level.FINER, "      No retreat to ter found for for task. Target: {0} Recruits: {1} Retreat Units: {2}", m_target, m_recruitedUnits, DUtils.UnitGroupList_ToString(retreatUnits));
        }
        else if(m_taskType.equals(NCM_TaskType.Reinforce_Stabilize))
        {
            Territory bestRetreatTer = null;
            float bestRetreatTerScore = Integer.MIN_VALUE;

            List<Territory> ourCaps = TerritoryAttachment.getAllCapitals(player, m_data);
            List<Territory> capsAndNeighbors = new ArrayList<Territory>();
            for (Territory cap : ourCaps)
                capsAndNeighbors.addAll(DUtils.GetTerritoriesWithinXDistanceOfY(m_data, cap, 1));
            if(capsAndNeighbors.contains(m_target))
                bestRetreatTer = DUtils.GetOurClosestCap(m_data, player, m_target); //We are endangered and next to cap, so retreat there (not sure if we should do this)

            if(bestRetreatTer == null)
            {
                for(Territory ter : (List<Territory>)DUtils.ShuffleList(DUtils.GetTerritoriesWithinXDistanceOfY(m_data, m_target, GlobalCenter.FastestUnitMovement))) //Shuffle, so our retreat isn't predicatable
                {
                    if(ter.isWater())
                        continue;
                    if(!DMatches.territoryIsOwnedByXOrAlly(m_data, GlobalCenter.CurrentPlayer).match(ter))
                        continue;

                    Route ncmRoute = CachedCalculationCenter.GetLandRoute(m_data, m_target, ter);
                    if (ncmRoute == null)
                        continue;
                    if (Match.allMatch(retreatUnits, DMatches.UnitGroupHasEnoughMovement_All(ncmRoute.getLength()))) //If this is a valid, reachable reinforce ter
                    {
                        List<Unit> possibleAttackers = DUtils.GetSPNNEnemyUnitsThatCanReach_CountXAsPassthrough(m_data, ter, GlobalCenter.CurrentPlayer, Matches.TerritoryIsLand, m_target);
                        possibleAttackers = Match.getMatches(possibleAttackers, new CompositeMatchOr<Unit>(Matches.UnitIsLand, Matches.UnitIsAir));
                        //Note that since this ter was not a reinforce_task ter(well, at least not a successful one), it is most likely within our territory
                        List<Unit> defenders = DUtils.GetTerUnitsAtEndOfTurn(m_data, player, ter);
                        defenders.retainAll(TacticalCenter.get(m_data, player).GetFrozenUnits()); //Only count units that have been frozen here
                        defenders.removeAll(DUtils.ToUnitList(retreatUnits)); //(Don't double add)
                        defenders.addAll(DUtils.ToUnitList(retreatUnits)); //And the units we're retreating
                        AggregateResults results = DUtils.GetBattleResults(possibleAttackers, defenders, ter, m_data, 500, true);

                        float score = 0;
                        score -= results.getAttackerWinPercent() * 1000;
                        score -= results.GetAverageAttackingUnitsRemaining().size();
                        score -= DUtils.GetDefenseScoreOfUnits(results.GetAverageAttackingUnitsRemaining()); //Have leftover invader strength only decide if takeover chances match

                        if (score > bestRetreatTerScore)
                        {
                            bestRetreatTer = ter;
                            bestRetreatTerScore = score;
                        }
                    }
                }
            }

            if(bestRetreatTer != null)
            {                
                DUtils.Log(Level.FINER, "      Attempting to perform target retreat. Target: {0} Retreat To: {1} Retreat Units: {2}", m_target, bestRetreatTer, DUtils.UnitGroupList_ToString(retreatUnits));
                Dynamix_AI.Pause();
                UnitGroup.EnableMoveBuffering();
                for (UnitGroup ug : retreatUnits)
                {
                    String error = ug.MoveAsFarTo_NCM(bestRetreatTer, mover);
                    if(error == null)
                        TacticalCenter.get(m_data, GlobalCenter.CurrentPlayer).FreezeUnits(ug.GetUnitsAsList());
                    else
                        DUtils.Log(Level.FINEST, "        NCM move failed, reason: {0}", error);
                }
                UnitGroup.PerformBufferedMovesAndDisableMoveBufferring(mover);
            }
            else
                DUtils.Log(Level.FINER, "      No retreat to ter found for for task. Target: {0} Recruits: {1} Retreat Units: {2}", m_target, m_recruitedUnits, DUtils.UnitGroupList_ToString(retreatUnits));
        }
    }

    /**
     * If this is an ncm task that is strong enough to resist all it's threats, we invalidate them because they can't attack more than one place.
     * (This method was added to fix the problem where one far-away airplane 'stack' can discourage ALL our attacks in the area, which is very bad.
     * Now, if one task can resist the plane stack, we assume the other movements are partially 'safe' from this enemy stack)
     */
    public void InvalidateThreatsThisTaskResists()
    {
        PlayerID player = GlobalCenter.CurrentPlayer;

        if(m_taskType == NCM_TaskType.Reinforce_FrontLine)
        {
            List<Unit> threats = DUtils.GetSPNNEnemyUnitsThatCanReach(m_data, m_target, GlobalCenter.CurrentPlayer, Matches.TerritoryIsLand);
            if(threats.isEmpty()) //No threats to invalidate
                return;
            List<Unit> defenders = GetRecruitedUnitsAsUnitList();

            AggregateResults simulatedAttack = DUtils.GetBattleResults(threats, defenders, m_target, m_data, DSettings.LoadSettings().CA_CMNCM_determinesSurvivalChanceAfterTaskToSeeIfToInvalidateAttackers, true);

            if (simulatedAttack.getDefenderWinPercent() > .4F)
            {
                ThreatInvalidationCenter.get(m_data, player).InvalidateThreats(threats, m_target);
                //DUtils.Log(Level.FINER, "      Reinforce_Frontline task succeeded with enough defense, so invalidating threats resisted by this task. Target: {0} Units Invalidated: {1}", m_target, threats);
            }
        }
        else if (m_taskType == NCM_TaskType.Reinforce_Stabilize)
        {
            List<Unit> threats = DUtils.GetSPNNEnemyUnitsThatCanReach(m_data, m_target, GlobalCenter.CurrentPlayer, Matches.TerritoryIsLand);
            if(threats.isEmpty()) //No threats to invalidate
                return;
            List<Unit> defenders = GetRecruitedUnitsAsUnitList();

            AggregateResults simulatedAttack = DUtils.GetBattleResults(threats, defenders, m_target, m_data, DSettings.LoadSettings().CA_CMNCM_determinesSurvivalChanceAfterTaskToSeeIfToInvalidateAttackers, true);

            if (simulatedAttack.getDefenderWinPercent() > .4F)
            {
                ThreatInvalidationCenter.get(m_data, player).InvalidateThreats(threats, m_target);
                //DUtils.Log(Level.FINER, "      Reinforce_Stabalize task succeeded with enough defense, so invalidating threats resisted by this task. Target: {0} Units Invalidated: {1}", m_target, threats);
            }
        }
    }

    public void Reset()
    {
        DUtils.Log(Level.FINER, "        Resetting task. Target: {0} Task Type: {1} Priority: {2} Recruit Size: {3}", m_target, m_taskType, m_priority, m_recruitedUnits.size());
        m_completed = false;
        m_disqualified = false;
        m_recruitedUnits = new ArrayList<UnitGroup>();
    }

    private boolean m_completed = false;
    public boolean IsCompleted()
    {
        return m_completed;
    }

    public void PerformTask(IMoveDelegate mover)
    {
        if(m_recruitedUnits.isEmpty())
        {
            DUtils.Log(Level.FINER, "      Task is called to perform, but there are no recruits! Target: {0} Task Type: {1} Priority: {2}", m_target, m_taskType, m_priority);
            m_completed = true;
            return; //We don't want to pause for an 'empty' task
        }
        if(!m_completed) //Only pause if this is the initial attack group
            Dynamix_AI.Pause();
        UnitGroup.EnableMoveBuffering();
        for(UnitGroup ug : m_recruitedUnits)
        {
            if (ug.GetMovedTo() != null)
                continue; //If this recruit has already moved
            String error = ug.MoveAsFarTo_NCM(m_target, mover);
            if (error != null)
                DUtils.Log(Level.FINER, "        NCM task perfoming move failed, reason: {0}", error);
            else
                TacticalCenter.get(m_data, GlobalCenter.CurrentPlayer).FreezeUnits(ug.GetUnitsAsList());
        }
        UnitGroup.PerformBufferedMovesAndDisableMoveBufferring(mover);
        m_completed = true;
    }
}
