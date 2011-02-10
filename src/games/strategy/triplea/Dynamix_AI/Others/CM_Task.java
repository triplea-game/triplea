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
import games.strategy.triplea.Dynamix_AI.CommandCenter.CachedInstanceCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.GlobalCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.StatusCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.TacticalCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.ThreatInvalidationCenter;
import games.strategy.triplea.Dynamix_AI.DMatches;
import games.strategy.triplea.Dynamix_AI.DUtils;
import games.strategy.triplea.Dynamix_AI.Dynamix_AI;
import games.strategy.triplea.Dynamix_AI.Group.UnitGroup;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.oddsCalculator.ta.AggregateResults;
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
public class CM_Task
{
    private Territory m_target = null;
    private CM_TaskType m_taskType = CM_TaskType.Empty;
    private float m_priority = 0.0F;
    private GameData m_data = null;
    public CM_Task(GameData data, Territory target, CM_TaskType type, float priority)
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

    public CM_TaskType GetTaskType()
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
        final HashMap<UnitGroup, Integer> possibles = new HashMap<UnitGroup, Integer>();
        for (final Territory ter : m_data.getMap().getTerritories())
        {
            if(DMatches.territoryContainsMultipleAlliances(m_data).match(ter)) //If we're battling here
                continue;
            Match<Unit> unitMatch = new Match<Unit>()
            {
                @Override
                public boolean match(Unit unit)
                {
                    if (!DUtils.CanUnitReachTer(m_data, ter, unit, m_target))
                        return false;
                    if (!Matches.unitIsOwnedBy(GlobalCenter.CurrentPlayer).match(unit))
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
                possibles.put(DUtils.CreateUnitGroupForUnits(Collections.singleton(unit), ter, m_data), suitability);
            }
        }

        List<UnitGroup> sortedPossibles = new ArrayList<UnitGroup>(possibles.keySet());        
        Collections.sort(sortedPossibles, new Comparator<UnitGroup>()
        {
            public int compare(UnitGroup ug1, UnitGroup ug2)
            {
                return (int) (possibles.get(ug2) - possibles.get(ug1));
            }
        });
        sortedPossibles = DUtils.PairUpUnitGroupsInListForOptimalAttack(m_data, GlobalCenter.CurrentPlayer, sortedPossibles, m_target, 25); //We want this list paired up, in case suitability is the same
        Collections.sort(sortedPossibles, new Comparator<UnitGroup>()
        {
            public int compare(UnitGroup ug1, UnitGroup ug2)
            {
                return (int) (possibles.get(ug2) - possibles.get(ug1));
            }
        });
        return sortedPossibles;
    }

    private float m_minChance = 0.0F;
    private float m_maxVulnerability = 0.0F;
    private int m_maxBattleVolleys = 0;
    public void CalculateTaskRequirements()
    {
        m_maxVulnerability = .5F; //For now, we just want to make the enemy pay if they attack us
        m_maxBattleVolleys = 100; //We usually don't care

        if (m_taskType == CM_TaskType.Attack_Offensive)
        {
            m_minChance = .7F;
            if(DUtils.GetAliveEnemyCaps(m_data, GlobalCenter.CurrentPlayer).contains(m_target))
                m_minChance = .5F;
        }
        else if (m_taskType.equals(m_taskType.Attack_Stabilize))
        {
            m_minChance = .55F;
            m_maxVulnerability = 1.0F;
        }
        else
            return; //Only one unit needed for land grab

        if (m_target.getOwner().isNull())
        {
            if (m_taskType == CM_TaskType.Attack_Offensive)
            {
                m_minChance = .95F;
                m_maxVulnerability = .10F; //Only take neutrals if threat after attack is very low
                m_maxBattleVolleys = 1; //And we can take it in one hit
            }
            else if (m_taskType.equals(m_taskType.Attack_Stabilize))
                m_minChance = .75F;
            else
                return; //Only one unit needed for land grab
        }

        TacticalCenter.get(m_data, GlobalCenter.CurrentPlayer).BattleRetreatChanceAssignments.put(m_target, m_minChance);
        DUtils.Log(Level.FINER, "    CM Task requirements calculated. Min Chance: {0} Max Vulnerability: {1} Max Battle Volleys: {2}", m_minChance, m_maxVulnerability, m_maxBattleVolleys);
    }

    private float getMeetingOfTakeoverChanceScore(AggregateResults simulatedAttack, float minChance)
    {
        if(m_taskType.equals(CM_TaskType.LandGrab))
        {
            if(m_recruitedUnits.size() > 0)
                return 1.0F; //Has reached, but not exceeded
            else
                return 0.0F;
        }

        return DUtils.Divide_SL((float)simulatedAttack.getAttackerWinPercent(), minChance); //We're this close to meeting the min takeover chance
    }

    private float getMeetingOfVulnerabilityMaxScore(AggregateResults simulatedResponse, float maxVulnerability)
    {
        if(m_taskType.equals(CM_TaskType.LandGrab))
        {
            if(m_recruitedUnits.size() > 0)
                return 1.0F; //Has reached, but not exceeded
            else
                return 0.0F;
        }

        return DUtils.Divide_SL(maxVulnerability, (float)simulatedResponse.getAttackerWinPercent()); //We're this close to getting our vulnerability below max amount
    }

    private float getMeetingOfMaxBattleVolleysScore(AggregateResults simulatedAttack, int maxBattleVolleys)
    {
        if(m_taskType.equals(CM_TaskType.LandGrab))
        {
            if(m_recruitedUnits.size() > 0)
                return 1.0F; //Has reached, but not exceeded
            else
                return 0.0F;
        }
        if(simulatedAttack.getAttackerWinPercent() < .5F) //If the enemy actually has the better chance of winning this battle
            return 0.0F; //Then count low battle volley score as something bad

        return DUtils.Divide_SL(maxBattleVolleys, (float)simulatedAttack.getAverageBattleRoundsFought()); //We're this close to getting the average battle volley count below max amount
    }

    private List<UnitGroup> m_recruitedUnits = new ArrayList<UnitGroup>();
    public void RecruitUnits()
    {
        recruitEnoughUnitsToMeetXYZ(m_minChance, m_maxVulnerability, m_maxBattleVolleys);
        DUtils.Log(Level.FINEST, "    Wave 1 recruits calculated. Target: {0} Recruits: {1}", m_target, m_recruitedUnits);
    }

    public void RecruitUnits2()
    {        
        float minChance = 0.0F;
        float maxVulnerability = .55F;
        int maxBattleVolleys = 5; //We want to take it in five volleys
        if (m_taskType == CM_TaskType.Attack_Offensive)
            minChance = .85F;
        else if (m_taskType.equals(m_taskType.Attack_Stabilize))
            minChance = .90F;
        else
            return; //Only one unit needed for land grab

        recruitEnoughUnitsToMeetXYZ(minChance, maxVulnerability, maxBattleVolleys);
        DUtils.Log(Level.FINEST, "    Wave 2 recruits calculated. Target: {0} Recruits: {1}", m_target, m_recruitedUnits);
    }

    public void RecruitUnits3()
    {
        float minChance = .97F;
        float maxVulnerability = .4F;
        int maxBattleVolleys = 3; //We want to take it in three volleys

        if(m_taskType.equals(CM_TaskType.LandGrab))
            return; //Only one unit needed for land grab

        recruitEnoughUnitsToMeetXYZ(minChance, maxVulnerability, maxBattleVolleys);
        DUtils.Log(Level.FINEST, "    Wave 3 recruits calculated. Target: {0} Recruits: {1}", m_target, m_recruitedUnits);
    }

    public void RecruitUnits4()
    {
        recruitEnoughUnitsToMeetXYZ(1.0F, .0F, 1);
        DUtils.Log(Level.FINEST, "    Wave 4 recruits calculated. Target: {0} Recruits: {1}", m_target, m_recruitedUnits);
    }

    private void recruitEnoughUnitsToMeetXYZ(float minChance, float maxVulnerability, int maxBattleVolleys)
    {
        List<UnitGroup> sortedPossibles = getSortedPossibleRecruits();

        if(m_taskType.equals(CM_TaskType.LandGrab) && m_recruitedUnits.size() > 0)
            return; //We only need one unit

        for (UnitGroup ug : sortedPossibles)
        {
            if(m_recruitedUnits.contains(ug)) //If already recruited
                continue;

            AggregateResults simulatedAttack = DUtils.GetBattleResults(GetRecruitedUnitsAsUnitList(), DUtils.ToList(m_target.getUnits().getMatches(Matches.unitIsEnemyOf(m_data, GlobalCenter.CurrentPlayer))), m_target, m_data, 5, false);
            List<Unit> responseAttackers = DUtils.DetermineResponseAttackers(m_data, GlobalCenter.CurrentPlayer, m_target, simulatedAttack);
            List<Unit> responseDefenders = Match.getMatches(simulatedAttack.GetAverageAttackingUnitsRemaining(), Matches.UnitIsNotAir); //Air can't defend ter because they need to land
            AggregateResults simulatedResponse = DUtils.GetBattleResults(responseAttackers, responseDefenders, m_target, m_data, 5, false);

            float howCloseToMeetingTakeoverChanceMin = getMeetingOfTakeoverChanceScore(simulatedAttack, minChance);
            float howCloseToMeetingVulnerabilityMax = getMeetingOfVulnerabilityMaxScore(simulatedResponse, maxVulnerability);
            float howCloseToMeetingMaxBattleVolleys = getMeetingOfMaxBattleVolleysScore(simulatedAttack, maxBattleVolleys);

            float totalScore = howCloseToMeetingTakeoverChanceMin + howCloseToMeetingVulnerabilityMax + howCloseToMeetingMaxBattleVolleys;
            float howCloseToTaskBeingWorthwhile = totalScore / 3; //Average closeness

            if(howCloseToTaskBeingWorthwhile < .98F) //If we haven't met our '98% of requirements' goal
                m_recruitedUnits.add(ug);
            else
                 break;
        }

        for (UnitGroup ug : sortedPossibles)
        {
            if(m_recruitedUnits.contains(ug)) //If already recruited
                continue;

            AggregateResults simulatedAttack = DUtils.GetBattleResults(GetRecruitedUnitsAsUnitList(), DUtils.ToList(m_target.getUnits().getMatches(Matches.unitIsEnemyOf(m_data, GlobalCenter.CurrentPlayer))), m_target, m_data, 250, false);
            List<Unit> responseAttackers = DUtils.DetermineResponseAttackers(m_data, GlobalCenter.CurrentPlayer, m_target, simulatedAttack);
            List<Unit> responseDefenders = Match.getMatches(simulatedAttack.GetAverageAttackingUnitsRemaining(), Matches.UnitIsNotAir); //Air can't defend ter because they need to land
            AggregateResults simulatedResponse = DUtils.GetBattleResults(responseAttackers, responseDefenders, m_target, m_data, 250, false);

            float howCloseToMeetingTakeoverChanceMin = getMeetingOfTakeoverChanceScore(simulatedAttack, minChance);
            float howCloseToMeetingVulnerabilityMax = getMeetingOfVulnerabilityMaxScore(simulatedResponse, maxVulnerability);
            float howCloseToMeetingMaxBattleVolleys = getMeetingOfMaxBattleVolleysScore(simulatedAttack, maxBattleVolleys);

            float totalScore = howCloseToMeetingTakeoverChanceMin + howCloseToMeetingVulnerabilityMax + howCloseToMeetingMaxBattleVolleys;
            float howCloseToTaskBeingWorthwhile = totalScore / 3; //Average closeness

            if(howCloseToTaskBeingWorthwhile < .98F) //If we haven't met our '98% of requirements' goal
                m_recruitedUnits.add(ug);
            else
                 break;
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

    public boolean IsPlannedAttackWorthwhile(List<CM_Task> allTasks)
    {
        DUtils.Log(Level.FINEST, "    Determining if cm task is worthwhile. Target: {0} Recruits: {1}", m_target, m_recruitedUnits);

        if (m_target.getOwner().isNull() && Math.random() < .95F) //Atm, AI over-attacks neutrals, so for now, ignore attacks on neutrals 95% of the time
            return false;

        if (false) //Atm, causes unwanted behavior and weird manuevers. Was: m_taskType == CM_TaskType.Attack_Offensive)
        {
            for (UnitGroup ug : new ArrayList<UnitGroup>(m_recruitedUnits))
            {
                for (CM_Task task : allTasks)
                {
                    if (task.equals(this))
                        continue;

                    if (task.GetPriority() >= this.GetPriority() * 2) //If task is twice as important is this one
                    {
                        if (task.IsDisqualified()) //We're looking for higher priority tasks(higher priority ones should have already been processed) that we're currently unable to perform and complete
                        {
                            if (DMatches.UnitGroupCanReach_Some(task.GetTarget()).match(ug)) //If this better task is reachable by this recruit
                            {
                                m_recruitedUnits.remove(ug); //Then remove this unit from the recruit list as we want to let this unit wait for reinforcements to complete the better task
                            }
                        }
                    }
                }
            }
        }

        if(m_recruitedUnits.isEmpty()) //Can happen if all recruits are waiting for reinforcements to complete a better, nearby task
            return false;
        Territory ourCap = TerritoryAttachment.getCapital(GlobalCenter.CurrentPlayer, m_data);

        List<Territory> capAndNeighbors = DUtils.GetTerritoriesWithinXDistanceOfY(m_data, ourCap, 1);
        HashSet<Unit> capAndNeighborsUnits = DUtils.ToHashSet(DUtils.GetUnitsInTerritories(capAndNeighbors));
        boolean areRecruitsFromCapOrNeighbor = false;
        for (Unit recruit : GetRecruitedUnitsAsUnitList())
        {
            if (capAndNeighborsUnits.contains(recruit))
            {
                areRecruitsFromCapOrNeighbor = true;
                break;
            }
        }

        if (areRecruitsFromCapOrNeighbor)
        {
            List<Float> capTakeoverChances = DUtils.GetTerTakeoverChanceBeforeAndAfterMove(m_data, GlobalCenter.CurrentPlayer, ourCap, m_target, GetRecruitedUnitsAsUnitList(), 250);
            if (capTakeoverChances.get(1) > .01F) //If takeover chance is 1% or more after move
            {
                if (capTakeoverChances.get(1) - capTakeoverChances.get(0) > .005) //and takeover chance before and after move is at least half a percent different
                    return false;
            }
        }

        AggregateResults simulatedAttack = DUtils.GetBattleResults(GetRecruitedUnitsAsUnitList(), DUtils.ToList(m_target.getUnits().getMatches(Matches.unitIsEnemyOf(m_data, GlobalCenter.CurrentPlayer))), m_target, m_data, 500, false);
        List<Unit> responseAttackers = DUtils.DetermineResponseAttackers(m_data, GlobalCenter.CurrentPlayer, m_target, simulatedAttack);
        List<Unit> responseDefenders = Match.getMatches(simulatedAttack.GetAverageAttackingUnitsRemaining(), Matches.UnitIsNotAir); //Air can't defend ter because they need to land
        AggregateResults simulatedResponse = DUtils.GetBattleResults(responseAttackers, responseDefenders, m_target, m_data, 500, false);

        if (m_taskType == CM_TaskType.Attack_Offensive)
        {
            //Hey, just a note to any developers reading this code:
            //    If you think it'll help, you can change these 'getMeetingOf...' methods so they return a value higher than 1.0F or lower than 0.0F.
            //    For example, you might want the 'meetTUVWants..' method to return even higher than 1.0 if the enemy, for example, loses more than twice as much TUV as us. (In attack and response)
            //    That might make an attack go through even if the other things aren't met, which may be good sometimes, like in the example situation where we'd make the enemy lose a lot more TUV.
            //    If you do make these sort of changes, though, please do it carefully, sloppy changes could mess up the code.
            float howCloseToMeetingTakeoverChanceMin = getMeetingOfTakeoverChanceScore(simulatedAttack, m_minChance);
            float howCloseToMeetingVulnerabilityMax = getMeetingOfVulnerabilityMaxScore(simulatedResponse, m_maxVulnerability);
            float howCloseToMeetingMaxBattleVolleys = getMeetingOfMaxBattleVolleysScore(simulatedAttack, m_maxBattleVolleys);

            float totalScore = howCloseToMeetingTakeoverChanceMin + howCloseToMeetingVulnerabilityMax + howCloseToMeetingMaxBattleVolleys;
            float howCloseToTaskBeingWorthwhile = totalScore / 3; //Average closeness

            DUtils.Log(Level.FINEST, "        Determining if cm task is worthwhile. HowCloseToTask, BeingWorthWhile: {0} MeetingTakeoverMin: {1} MeetingVulnerabilityMax: {2}, MeetingMaxBattleVolleys: {3}", howCloseToTaskBeingWorthwhile, howCloseToMeetingTakeoverChanceMin, howCloseToMeetingVulnerabilityMax, howCloseToMeetingMaxBattleVolleys);

            if(howCloseToTaskBeingWorthwhile < .96F) //If we haven't met our '96% of requirements' goal
                return false;
        }
        else if (m_taskType.equals(m_taskType.Attack_Stabilize))
        {
            float howCloseToMeetingTakeoverChanceMin = getMeetingOfTakeoverChanceScore(simulatedAttack, m_minChance);
            float howCloseToMeetingVulnerabilityMax = getMeetingOfVulnerabilityMaxScore(simulatedResponse, m_maxVulnerability);
            float howCloseToMeetingMaxBattleVolleys = getMeetingOfMaxBattleVolleysScore(simulatedAttack, m_maxBattleVolleys);            

            float totalScore = howCloseToMeetingTakeoverChanceMin + howCloseToMeetingVulnerabilityMax + howCloseToMeetingMaxBattleVolleys;
            float howCloseToTaskBeingWorthwhile = totalScore / 3; //Average closeness

            DUtils.Log(Level.FINEST, "        Determining if cm task is worthwhile. HowCloseToTask, BeingWorthWhile: {0} MeetingTakeoverMin: {1} MeetingVulnerabilityMax: {2}, MeetingMaxBattleVolleys: {3}", howCloseToTaskBeingWorthwhile, howCloseToMeetingTakeoverChanceMin, howCloseToMeetingVulnerabilityMax, howCloseToMeetingMaxBattleVolleys);

            if(howCloseToTaskBeingWorthwhile < .96F) //If we haven't met our '96% of requirements' goal
                return false;
        }
        else
        {
            if(CachedInstanceCenter.CachedBattleTracker.wasConquered(m_target)) //If the blitz target was already taken
            {
                StatusCenter.get(m_data, GlobalCenter.CurrentPlayer).GetStatusOfTerritory(m_target).WasBlitzed = true;
                m_completed = true;
                m_recruitedUnits = new ArrayList<UnitGroup>(); //No need to send unit
                return false; //Already taken care of by another task
            }
            Territory startTer = m_recruitedUnits.get(0).GetStartTerritory(); //Land grabs are done with only one unit
            Route route = m_data.getMap().getRoute(startTer, m_target, Matches.TerritoryIsLand);

            boolean canUnitsGetBack = false;
            for(Unit unit : GetRecruitedUnitsAsUnitList())
            {
                TripleAUnit ta = TripleAUnit.get(unit);
                if(ta.getMovementLeft() >= route.getLength() * 2)
                    canUnitsGetBack = true;
            }

            int unitCost = DUtils.GetTUVOfUnits(GetRecruitedUnitsAsUnitList(), GlobalCenter.CurrentPlayer, GlobalCenter.GetPUResource());
            TerritoryAttachment ta = TerritoryAttachment.get(m_target);

            List<Unit> landAttackers = DUtils.GetNNEnemyLUnitsThatCanReach(m_data, m_target, GlobalCenter.CurrentPlayer, Matches.TerritoryIsLand);

            if(ta.getProduction() < unitCost - 1 && !canUnitsGetBack && landAttackers.size() > 0)
                return false;
        }

        return true;
    }

    public boolean IsTaskWithAdditionalRecruitsWorthwhile()
    {
        if (m_recruitedUnits.isEmpty()) //Can happen if all recruits are waiting for reinforcements to complete a better, nearby task
            return false;
        Territory ourCap = TerritoryAttachment.getCapital(GlobalCenter.CurrentPlayer, m_data);

        List<Territory> capAndNeighbors = DUtils.GetTerritoriesWithinXDistanceOfY(m_data, ourCap, 1);
        HashSet<Unit> capAndNeighborsUnits = DUtils.ToHashSet(DUtils.GetUnitsInTerritories(capAndNeighbors));
        boolean areRecruitsFromCapOrNeighbor = false;
        for (Unit recruit : GetRecruitedUnitsAsUnitList())
        {
            if (capAndNeighborsUnits.contains(recruit))
            {
                areRecruitsFromCapOrNeighbor = true;
                break;
            }
        }

        if (areRecruitsFromCapOrNeighbor)
        {
            List<Float> capTakeoverChances = DUtils.GetTerTakeoverChanceBeforeAndAfterMove(m_data, GlobalCenter.CurrentPlayer, ourCap, m_target, GetRecruitedUnitsAsUnitList(), 250);
            if (capTakeoverChances.get(1) > .01F) //If takeover chance is 1% or more after move
            {
                if (capTakeoverChances.get(1) - capTakeoverChances.get(0) > .005) //and takeover chance before and after move is at least half a percent different
                    return false;
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

    /**
     * If this is a cm task that is strong enough to resist all it's threats, we invalidate them because they can't attack more than one place.
     * (This method was added to fix the problem where one far-away airplane 'stack' can discourage ALL our attacks in the area, which is very bad.
     * Now, if one task can resist the plane stack, we assume the other movements are 'safe' from this enemy stack)
     */
    public void InvalidateThreatsThisTaskResists()
    {
        PlayerID player = GlobalCenter.CurrentPlayer;

        if (m_taskType == CM_TaskType.Attack_Offensive)
        {
            AggregateResults simulatedAttack = DUtils.GetBattleResults(GetRecruitedUnitsAsUnitList(), DUtils.ToList(m_target.getUnits().getMatches(Matches.unitIsEnemyOf(m_data, GlobalCenter.CurrentPlayer))), m_target, m_data, 5, false);
            List<Unit> threats = DUtils.DetermineResponseAttackers(m_data, GlobalCenter.CurrentPlayer, m_target, simulatedAttack);
            if(threats.isEmpty()) //No threats to invalidate
                return;
            List<Unit> responseDefenders = Match.getMatches(simulatedAttack.GetAverageAttackingUnitsRemaining(), Matches.UnitIsNotAir); //Air can't defend ter because they need to land
            AggregateResults simulatedResponse = DUtils.GetBattleResults(threats, responseDefenders, m_target, m_data, 250, false);

            if (simulatedResponse.getDefenderWinPercent() > .4F)
            {
                ThreatInvalidationCenter.get(m_data, player).InvalidateThreats(threats);
                DUtils.Log(Level.FINER, "      Attack_Offensive task succeeded with enough defense, so invalidating threats resisted by this task. Target: {0} Units Invalidated: {1}", m_target, threats);
            }
        }
        else if (m_taskType == CM_TaskType.Attack_Stabilize)
        {
            AggregateResults simulatedAttack = DUtils.GetBattleResults(GetRecruitedUnitsAsUnitList(), DUtils.ToList(m_target.getUnits().getMatches(Matches.unitIsEnemyOf(m_data, GlobalCenter.CurrentPlayer))), m_target, m_data, 5, false);
            List<Unit> threats = DUtils.DetermineResponseAttackers(m_data, GlobalCenter.CurrentPlayer, m_target, simulatedAttack);
            if(threats.isEmpty()) //No threats to invalidate
                return;
            List<Unit> responseDefenders = Match.getMatches(simulatedAttack.GetAverageAttackingUnitsRemaining(), Matches.UnitIsNotAir); //Air can't defend ter because they need to land
            AggregateResults simulatedResponse = DUtils.GetBattleResults(threats, responseDefenders, m_target, m_data, 250, false);

            if (simulatedResponse.getDefenderWinPercent() > .4F)
            {
                ThreatInvalidationCenter.get(m_data, player).InvalidateThreats(threats);
                DUtils.Log(Level.FINER, "      Attack_Stabalize task succeeded with enough defense, so invalidating threats resisted by this task. Target: {0} Units Invalidated: {1}", m_target, threats);
            }
        }
    }

    private boolean m_completed = false;
    public boolean IsCompleted()
    {
        return m_completed;
    }

    public void PerformTask(IMoveDelegate mover)
    {
        if (m_recruitedUnits.isEmpty())
        {
            DUtils.Log(Level.FINEST, "      Task is called to perform, but there are no recruits! Target: {0} Task Type: {1} Priority: {2}", m_target, m_taskType, m_priority);
            return;
        }
        Dynamix_AI.Pause();
        for(UnitGroup ug : m_recruitedUnits)
        {
            ug.MoveAsFarTo_CM(m_target, mover);
        }
        m_completed = true;              
    }
}
