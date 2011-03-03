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
import games.strategy.triplea.Constants;
import games.strategy.triplea.Dynamix_AI.CommandCenter.CachedInstanceCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.GlobalCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.StatusCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.TacticalCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.ThreatInvalidationCenter;
import games.strategy.triplea.Dynamix_AI.DMatches;
import games.strategy.triplea.Dynamix_AI.DSettings;
import games.strategy.triplea.Dynamix_AI.DUtils;
import games.strategy.triplea.Dynamix_AI.Dynamix_AI;
import games.strategy.triplea.Dynamix_AI.Group.UnitGroup;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
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
                    UnitAttachment ua = UnitAttachment.get(unit.getUnitType());
                    if (!DUtils.CanUnitReachTer(m_data, ter, unit, m_target))
                        return false;
                    if (!Matches.unitIsOwnedBy(GlobalCenter.CurrentPlayer).match(unit))
                        return false;
                    if (Matches.UnitIsFactory.match(unit) && ua.getAttack(unit.getOwner()) == 0)
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
        sortedPossibles = DUtils.PairUpUnitGroupsInListForOptimalAttack(m_data, GlobalCenter.CurrentPlayer, sortedPossibles, m_target, DSettings.LoadSettings().CA_CMNCM_sortsPossibleTaskRecruitsForOptimalAttackDefense); //We want this list paired up, in case suitability is the same
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
    public void CalculateTaskRequirements()
    {
        if(m_taskType.equals(CM_TaskType.LandGrab))
            return;  //Only one unit needed for land grab

        if (m_taskType == CM_TaskType.Attack_Offensive)
        {
            if (m_target.getOwner().isNull())
            {
                m_minChance = DUtils.ToFloat(DSettings.LoadSettings().TR_attackOffensive_Neutrals_TakeoverChanceRequired);
                m_maxVulnerability = DUtils.ToFloat(DSettings.LoadSettings().TR_attackOffensive_Neutrals_CounterAttackSurvivalChanceRequired);
            }
            else if (DUtils.GetAliveEnemyCaps(m_data, GlobalCenter.CurrentPlayer).contains(m_target))
            {
                m_minChance = DUtils.ToFloat(DSettings.LoadSettings().TR_attackOffensive_Capitals_TakeoverChanceRequired);
                m_maxVulnerability = DUtils.ToFloat(DSettings.LoadSettings().TR_attackOffensive_Capitals_CounterAttackSurvivalChanceRequired);
            }
            else
            {
                m_minChance = DUtils.ToFloat(DSettings.LoadSettings().TR_attackOffensive_TakeoverChanceRequired);
                m_maxVulnerability = DUtils.ToFloat(DSettings.LoadSettings().TR_attackOffensive_CounterAttackSurvivalChanceRequired);
            }
        }
        else if (m_taskType.equals(m_taskType.Attack_Stabilize))
        {
            if (m_target.getOwner().isNull())
            {
                m_minChance = DUtils.ToFloat(DSettings.LoadSettings().TR_attackOffensive_Neutrals_TakeoverChanceRequired);
                m_maxVulnerability = DUtils.ToFloat(DSettings.LoadSettings().TR_attackOffensive_Neutrals_CounterAttackSurvivalChanceRequired);
            }
            else if (DUtils.GetAliveEnemyCaps(m_data, GlobalCenter.CurrentPlayer).contains(m_target))
            {
                m_minChance = DUtils.ToFloat(DSettings.LoadSettings().TR_attackOffensive_Capitals_TakeoverChanceRequired);
                m_maxVulnerability = DUtils.ToFloat(DSettings.LoadSettings().TR_attackOffensive_Capitals_CounterAttackSurvivalChanceRequired);
            }
            else
            {
                m_minChance = DUtils.ToFloat(DSettings.LoadSettings().TR_attackOffensive_TakeoverChanceRequired);
                m_maxVulnerability = DUtils.ToFloat(DSettings.LoadSettings().TR_attackStabalize_CounterAttackSurvivalChanceRequired);
            }
        }

        TacticalCenter.get(m_data, GlobalCenter.CurrentPlayer).BattleRetreatChanceAssignments.put(m_target, m_minChance);
        DUtils.Log(Level.FINER, "    CM Task requirements calculated. Min Chance: {0} Max Vulnerability: {1}", m_minChance, m_maxVulnerability);
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
        recruitEnoughUnitsToMeetXYZ(m_minChance, m_maxVulnerability, 100);
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
        if(m_taskType.equals(CM_TaskType.LandGrab) && m_recruitedUnits.size() > 0)
            return; //We only need one unit

        List<UnitGroup> sortedPossibles = getSortedPossibleRecruits();
        if(sortedPossibles.isEmpty())
            return;

        for (UnitGroup ug : sortedPossibles)
        {
            if(m_recruitedUnits.contains(ug)) //If already recruited
                continue;

            AggregateResults simulatedAttack = DUtils.GetBattleResults(GetRecruitedUnitsAsUnitList(), DUtils.ToList(m_target.getUnits().getMatches(Matches.unitIsEnemyOf(m_data, GlobalCenter.CurrentPlayer))), m_target, m_data, 5, false);
            List<Unit> responseAttackers = DUtils.DetermineResponseAttackers(m_data, GlobalCenter.CurrentPlayer, m_target, simulatedAttack);
            List<Unit> responseDefenders = Match.getMatches(simulatedAttack.GetAverageAttackingUnitsRemaining(), Matches.UnitIsNotAir); //Air can't defend ter because they need to land
            AggregateResults simulatedResponse = DUtils.GetBattleResults(responseAttackers, responseDefenders, m_target, m_data, 1, false);

            float howCloseToMeetingTakeoverChanceMin = getMeetingOfTakeoverChanceScore(simulatedAttack, minChance);
            if(howCloseToMeetingTakeoverChanceMin < DUtils.ToFloat(DSettings.LoadSettings().AA_percentOfMeetingOfAttackTakeoverConstantNeededToPerformCMTask) + .02)
            {
                m_recruitedUnits.add(ug);
                continue;
            }

            float howCloseToMeetingVulnerabilityMax = getMeetingOfVulnerabilityMaxScore(simulatedResponse, maxVulnerability);
            if (howCloseToMeetingVulnerabilityMax < DUtils.ToFloat(DSettings.LoadSettings().AA_percentOfMeetingOfCounterAttackSurvivalConstantNeededToPerformCMTask) + .02)
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

        for (UnitGroup ug : sortedPossibles)
        {
            if(m_recruitedUnits.contains(ug)) //If already recruited
                continue;

            AggregateResults simulatedAttack = DUtils.GetBattleResults(GetRecruitedUnitsAsUnitList(), DUtils.ToList(m_target.getUnits().getMatches(Matches.unitIsEnemyOf(m_data, GlobalCenter.CurrentPlayer))), m_target, m_data, 250, false);
            List<Unit> responseAttackers = DUtils.DetermineResponseAttackers(m_data, GlobalCenter.CurrentPlayer, m_target, simulatedAttack);
            List<Unit> responseDefenders = Match.getMatches(simulatedAttack.GetAverageAttackingUnitsRemaining(), Matches.UnitIsNotAir); //Air can't defend ter because they need to land
            AggregateResults simulatedResponse = DUtils.GetBattleResults(responseAttackers, responseDefenders, m_target, m_data, DSettings.LoadSettings().CA_CMNCM_determinesIfTasksRequirementsAreMetEnoughForRecruitingStop, false);

            float howCloseToMeetingTakeoverChanceMin = getMeetingOfTakeoverChanceScore(simulatedAttack, minChance);
            if(howCloseToMeetingTakeoverChanceMin < DUtils.ToFloat(DSettings.LoadSettings().AA_percentOfMeetingOfAttackTakeoverConstantNeededToPerformCMTask) + .02)
            {
                m_recruitedUnits.add(ug);
                continue;
            }

            float howCloseToMeetingVulnerabilityMax = getMeetingOfVulnerabilityMaxScore(simulatedResponse, maxVulnerability);
            if (howCloseToMeetingVulnerabilityMax < DUtils.ToFloat(DSettings.LoadSettings().AA_percentOfMeetingOfCounterAttackSurvivalConstantNeededToPerformCMTask) + .02)
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

    public boolean IsPlannedAttackWorthwhile(List<CM_Task> allTasks)
    {
        DUtils.Log(Level.FINEST, "    Determining if cm task is worthwhile. Target: {0} Recruits Size: {1}", m_target, m_recruitedUnits.size());

        if (m_target.getOwner().isNull())
        {
            //Atm, AI over-attacks neutrals on FFA maps, so for now, ignore attacks on non-empty neutrals 95% of the time on FFA maps
            if(m_target.getUnits().getMatches(Matches.unitHasDefenseThatIsMoreThanOrEqualTo(1)).size() > 0 && Math.random() < .95F)
                return false;
            //Atm, don't attack neutrals if the neutral attack charge is more than the territory production, 75% of the time
            if(TerritoryAttachment.get(m_target) == null || Integer.parseInt(m_data.getProperties().get(Constants.NEUTRAL_CHARGE_PROPERTY, 0).toString()) > TerritoryAttachment.get(m_target).getProduction() && Math.random() < .75F)
                return false;
            //Never attack if neutral charge is over three times the ter value
            if(TerritoryAttachment.get(m_target) == null || Integer.parseInt(m_data.getProperties().get(Constants.NEUTRAL_CHARGE_PROPERTY, 0).toString()) > TerritoryAttachment.get(m_target).getProduction() * 3)
                return false;
        }

        if (false) //Atm, causes unwanted task cancelling. Was: m_taskType == CM_TaskType.Attack_Offensive)
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
                            if (DMatches.UnitGroupCanReach_Some(task.GetTarget()).match(ug)) //If this more important task is reachable by this recruit
                                return false; //Cancel this task so we can build up units to perform the more important task
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
            List<Float> capTakeoverChances = DUtils.GetTerTakeoverChanceBeforeAndAfterMove(m_data, GlobalCenter.CurrentPlayer, ourCap, m_target, GetRecruitedUnitsAsUnitList(), DSettings.LoadSettings().CA_CMNCM_determinesIfTaskEndangersCap);
            if (capTakeoverChances.get(1) > .1F) //If takeover chance is 10% or more after move
            {
                if (capTakeoverChances.get(1) - capTakeoverChances.get(0) > .01F) //and takeover chance before and after move is at least 1% different
                    return false;
            }
        }

        AggregateResults simulatedAttack = DUtils.GetBattleResults(GetRecruitedUnitsAsUnitList(), DUtils.ToList(m_target.getUnits().getMatches(Matches.unitIsEnemyOf(m_data, GlobalCenter.CurrentPlayer))), m_target, m_data, DSettings.LoadSettings().CA_CM_determinesAttackResultsToSeeIfTaskWorthwhile, false);
        List<Unit> responseAttackers = DUtils.DetermineResponseAttackers(m_data, GlobalCenter.CurrentPlayer, m_target, simulatedAttack);
        List<Unit> responseDefenders = Match.getMatches(simulatedAttack.GetAverageAttackingUnitsRemaining(), Matches.UnitIsNotAir); //Air can't defend ter because they need to land
        AggregateResults simulatedResponse = DUtils.GetBattleResults(responseAttackers, responseDefenders, m_target, m_data, DSettings.LoadSettings().CA_CMNCM_determinesResponseResultsToSeeIfTaskWorthwhile, false);

        if (m_taskType == CM_TaskType.Attack_Offensive)
        {
            //Hey, just a note to any developers reading this code:
            //    If you think it'll help, you can change these 'getMeetingOf...' methods so they return a value higher than 1.0F or lower than 0.0F.
            //    For example, you might want the 'meetTUVWants..' method to return even higher than 1.0 if the enemy, for example, loses more than twice as much TUV as us. (In attack and response)
            //    That might make an attack go through even if the other things aren't met, which may be good sometimes, like in the example situation where we'd make the enemy lose a lot more TUV.
            //    If you do make these sort of changes, though, please do it carefully, sloppy changes could mess up the code.
            float howCloseToMeetingTakeoverChanceMin = getMeetingOfTakeoverChanceScore(simulatedAttack, m_minChance);
            float percentOfRequirementNeeded_TakeoverChance = DUtils.ToFloat(DSettings.LoadSettings().AA_percentOfMeetingOfAttackTakeoverConstantNeededToPerformCMTask);
            DUtils.Log(Level.FINEST, "      How close to meeting takeover chance min: {0} Needed: {1}", howCloseToMeetingTakeoverChanceMin, percentOfRequirementNeeded_TakeoverChance);
            if(howCloseToMeetingTakeoverChanceMin < percentOfRequirementNeeded_TakeoverChance)
                return false;

            float howCloseToMeetingVulnerabilityMax = getMeetingOfVulnerabilityMaxScore(simulatedResponse, m_maxVulnerability);
            float percentOfRequirementNeeded_Vulnerability = DUtils.ToFloat(DSettings.LoadSettings().AA_percentOfMeetingOfCounterAttackSurvivalConstantNeededToPerformCMTask);
            DUtils.Log(Level.FINEST, "      How close to meeting vulnerability max: {0} Needed: {1}", howCloseToMeetingVulnerabilityMax, percentOfRequirementNeeded_Vulnerability);
            if (howCloseToMeetingVulnerabilityMax < percentOfRequirementNeeded_Vulnerability)
                return false;

            return true; //We've met all requirements
        }
        else if (m_taskType.equals(m_taskType.Attack_Stabilize))
        {
            float howCloseToMeetingTakeoverChanceMin = getMeetingOfTakeoverChanceScore(simulatedAttack, m_minChance);
            float percentOfRequirementNeeded_TakeoverChance = DUtils.ToFloat(DSettings.LoadSettings().AA_percentOfMeetingOfAttackTakeoverConstantNeededToPerformCMTask);
            DUtils.Log(Level.FINEST, "      How close to meeting takeover chance min: {0} Needed: {1}", howCloseToMeetingTakeoverChanceMin, percentOfRequirementNeeded_TakeoverChance);
            if(howCloseToMeetingTakeoverChanceMin < percentOfRequirementNeeded_TakeoverChance)
                return false;

            float howCloseToMeetingVulnerabilityMax = getMeetingOfVulnerabilityMaxScore(simulatedResponse, m_maxVulnerability);
            float percentOfRequirementNeeded_Vulnerability = DUtils.ToFloat(DSettings.LoadSettings().AA_percentOfMeetingOfCounterAttackSurvivalConstantNeededToPerformCMTask);
            DUtils.Log(Level.FINEST, "      How close to meeting vulnerability max: {0} Needed: {1}", howCloseToMeetingVulnerabilityMax, percentOfRequirementNeeded_Vulnerability);
            if (howCloseToMeetingVulnerabilityMax < percentOfRequirementNeeded_Vulnerability)
                return false;

            return true; //We've met all requirements
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
            if(canUnitsGetBack)
                return true;

            int unitCost = DUtils.GetTUVOfUnits(GetRecruitedUnitsAsUnitList(), GlobalCenter.CurrentPlayer, GlobalCenter.GetPUResource());
            TerritoryAttachment ta = TerritoryAttachment.get(m_target);

            List<Unit> landAttackers = DUtils.GetNNEnemyLUnitsThatCanReach(m_data, m_target, GlobalCenter.CurrentPlayer, Matches.TerritoryIsLand);

            if (unitCost - 1 < ta.getProduction() || landAttackers.isEmpty())
                return true;

            return false;
        }
    }

    public boolean IsTaskWithAdditionalRecruitsWorthwhile()
    {
        if (m_recruitedUnits.isEmpty()) //Can happen if all recruits are waiting for reinforcements to complete a better, nearby task
            return false;

        PlayerID player = GlobalCenter.CurrentPlayer;

        List<Territory> ourCaps = TerritoryAttachment.getAllCapitals(player, m_data);

        List<Territory> capsAndNeighbors = new ArrayList<Territory>();
        for(Territory cap : ourCaps)
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
            List<Float> capTakeoverChances = DUtils.GetTerTakeoverChanceBeforeAndAfterMove(m_data, GlobalCenter.CurrentPlayer, ourClosestCap, m_target, GetRecruitedUnitsAsUnitList(), DSettings.LoadSettings().CA_CMNCM_determinesIfTaskEndangersCap);
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
     * Now, if one task can resist the plane stack, we assume the other movements are partially 'safe' from this enemy stack)
     */
    public void InvalidateThreatsThisTaskResists()
    {
        PlayerID player = GlobalCenter.CurrentPlayer;

        if (m_taskType == CM_TaskType.Attack_Offensive)
        {
            ThreatInvalidationCenter.get(m_data, player).SuspendThreatInvalidation(); //We want to invalidate threats only if this task REALLY DOES resist ALL ENEMIES, including INVALIDATED ones.
            AggregateResults simulatedAttack = DUtils.GetBattleResults(GetRecruitedUnitsAsUnitList(), DUtils.ToList(m_target.getUnits().getMatches(Matches.unitIsEnemyOf(m_data, GlobalCenter.CurrentPlayer))), m_target, m_data, 250, false);
            List<Unit> threats = DUtils.DetermineResponseAttackers(m_data, GlobalCenter.CurrentPlayer, m_target, simulatedAttack);
            if(threats.isEmpty()) //No threats to invalidate
                return;
            List<Unit> responseDefenders = Match.getMatches(simulatedAttack.GetAverageAttackingUnitsRemaining(), Matches.UnitIsNotAir); //Air can't defend ter because they need to land
            AggregateResults simulatedResponse = DUtils.GetBattleResults(threats, responseDefenders, m_target, m_data, DSettings.LoadSettings().CA_CMNCM_determinesVulnerabilityAfterTaskToSeeIfToInvalidateAttackers, false);

            if (simulatedResponse.getDefenderWinPercent() > .4F)
            {
                ThreatInvalidationCenter.get(m_data, player).InvalidateThreats(threats, m_target);
                DUtils.Log(Level.FINER, "      Attack_Offensive task succeeded with enough defense, so invalidating threats resisted by this task. Target: {0} Units Invalidated: {1}", m_target, threats);
            }
            ThreatInvalidationCenter.get(m_data, player).ResumeThreatInvalidation();
        }
        else if (m_taskType == CM_TaskType.Attack_Stabilize)
        {
            ThreatInvalidationCenter.get(m_data, player).SuspendThreatInvalidation(); //We want to invalidate threats only if this task REALLY DOES resist ALL ENEMIES, including INVALIDATED ones.
            AggregateResults simulatedAttack = DUtils.GetBattleResults(GetRecruitedUnitsAsUnitList(), DUtils.ToList(m_target.getUnits().getMatches(Matches.unitIsEnemyOf(m_data, GlobalCenter.CurrentPlayer))), m_target, m_data, 250, false);
            List<Unit> threats = DUtils.DetermineResponseAttackers(m_data, GlobalCenter.CurrentPlayer, m_target, simulatedAttack);
            if(threats.isEmpty()) //No threats to invalidate
                return;
            List<Unit> responseDefenders = Match.getMatches(simulatedAttack.GetAverageAttackingUnitsRemaining(), Matches.UnitIsNotAir); //Air can't defend ter because they need to land
            AggregateResults simulatedResponse = DUtils.GetBattleResults(threats, responseDefenders, m_target, m_data, DSettings.LoadSettings().CA_CMNCM_determinesVulnerabilityAfterTaskToSeeIfToInvalidateAttackers, false);

            if (simulatedResponse.getDefenderWinPercent() > .4F)
            {
                ThreatInvalidationCenter.get(m_data, player).InvalidateThreats(threats, m_target);
                DUtils.Log(Level.FINER, "      Attack_Stabalize task succeeded with enough defense, so invalidating threats resisted by this task. Target: {0} Units Invalidated: {1}", m_target, threats);
            }
            ThreatInvalidationCenter.get(m_data, player).ResumeThreatInvalidation();
        }
    }

    public void Reset()
    {
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
        if (m_recruitedUnits.isEmpty())
            DUtils.Log(Level.FINEST, "      Task is called to perform, but there are no recruits! Target: {0} Task Type: {1} Priority: {2}", m_target, m_taskType, m_priority);
        if(!m_completed) //Only pause if this is the initial attack group
            Dynamix_AI.Pause();
        UnitGroup.EnableMoveBuffering();
        for(UnitGroup ug : m_recruitedUnits)
        {
            if(ug.GetMovedTo() != null)
                continue; //If this recruit has already moved
            String error = ug.MoveAsFarTo_CM(m_target, mover);
            if (error != null)
                DUtils.Log(Level.FINEST, "        CM task perfoming move failed, reason: {0}", error);
        }
        UnitGroup.PerformBufferedMovesAndDisableMoveBufferring(mover);
        m_completed = true;
    }
}
