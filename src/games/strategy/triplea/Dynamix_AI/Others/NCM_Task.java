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
import games.strategy.triplea.Dynamix_AI.DMatches;
import games.strategy.triplea.Dynamix_AI.DSettings;
import games.strategy.triplea.Dynamix_AI.DSorting;
import games.strategy.triplea.Dynamix_AI.DUtils;
import games.strategy.triplea.Dynamix_AI.Dynamix_AI;
import games.strategy.triplea.Dynamix_AI.CommandCenter.CachedCalculationCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.CachedInstanceCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.GlobalCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.ReconsiderSignalCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.StatusCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.TacticalCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.ThreatInvalidationCenter;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;

/**
 * 
 * @author Stephen
 */
@SuppressWarnings("unchecked")
public class NCM_Task
{
	private Territory m_target = null;
	private NCM_TaskType m_taskType = NCM_TaskType.Empty;
	private float m_priority = 0.0F;
	private GameData m_data = null;
	
	public NCM_Task(final GameData data, final Territory target, final NCM_TaskType type, final float priority)
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
	
	public void SetPriority(final float priority)
	{
		m_priority = priority;
	}
	
	private List<UnitGroup> getSortedPossibleRecruits()
	{
		final HashMap<Unit, Territory> unitLocations = new HashMap<Unit, Territory>();
		final HashMap<Unit, Integer> possibles = new HashMap<Unit, Integer>();
		@SuppressWarnings("unused")
		final boolean addedAA = false;
		for (final Territory ter : m_data.getMap().getTerritories())
		{
			if (DMatches.territoryContainsMultipleAlliances(m_data).match(ter)) // If we're battling here
				continue;
			final HashSet<Unit> recruitsAsHashSet = DUtils.ToHashSet(GetRecruitedUnitsAsUnitList());
			final Match<Unit> unitMatch = new Match<Unit>()
			{
				@Override
				public boolean match(final Unit unit)
				{
					final UnitAttachment ua = UnitAttachment.get(unit.getUnitType());
					if (!Matches.unitIsOwnedBy(GlobalCenter.CurrentPlayer).match(unit))
						return false;
					if (Matches.UnitIsFactory.match(unit) && ua.getDefense(unit.getOwner()) <= 0)
						return false;
					if (recruitsAsHashSet.contains(unit)) // If we've already recruited this unit
						return false;
					if (!DUtils.CanUnitReachTer(m_data, ter, unit, m_target))
						return false;
					return true;
				}
			};
			final List<Unit> units = Match.getMatches(DUtils.ToList(ter.getUnits().getUnits()), unitMatch);
			if (units.isEmpty())
				continue;
			for (final Unit unit : units)
			{
				if (Matches.UnitIsAA.match(unit))
				{
					// NOTE: AA movement is currently disabled. Uncomment below to re-enable.
					// If this is an AA and we've already added an AA as a recruit or (the from ter has a factory and this is the only AA), skip AA
					// if (addedAA || (ter.getUnits().getMatches(Matches.UnitIsFactory).size() > 0 && ter.getUnits().getMatches(Matches.UnitIsAA).size() <= 1))
					continue;
					// else
					// addedAA = true;
				}
				final int suitability = DUtils.HowWellIsUnitSuitedToTask(m_data, this, ter, unit);
				if (suitability == Integer.MIN_VALUE)
					continue;
				possibles.put(unit, suitability);
				unitLocations.put(unit, ter);
			}
		}
		List<Unit> sortedPossibles = DUtils.ToList(possibles.keySet());
		// For now, shuffle,
		// Collections.shuffle(sortedPossibles);
		// Then sort by score. In this way, equal scored attack units are shuffled
		sortedPossibles = DSorting.SortListByScores_List_D(sortedPossibles, possibles.values());
		// Now put the units into UnitGroups and return the list
		final List<UnitGroup> result = new ArrayList<UnitGroup>();
		for (final Unit unit : sortedPossibles)
			result.add(DUtils.CreateUnitGroupForUnits(Collections.singletonList(unit), unitLocations.get(unit), m_data));
		return result;
	}
	
	private float m_minSurvivalChance = 0.0F;
	
	public void CalculateTaskRequirements()
	{
		if (m_taskType.equals(NCM_TaskType.Land_Reinforce_Block))
			return; // Only one unit needed for block
		if (m_taskType == NCM_TaskType.Land_Reinforce_FrontLine)
			m_minSurvivalChance = DUtils.ToFloat(DSettings.LoadSettings().TR_reinforceFrontLine_enemyAttackSurvivalChanceRequired);
		else if (m_taskType.equals(NCM_TaskType.Land_Reinforce_Stabilize))
			m_minSurvivalChance = DUtils.ToFloat(DSettings.LoadSettings().TR_reinforceStabalize_enemyAttackSurvivalChanceRequired);
		// DUtils.Log(Level.FINER, "    NCM Task requirements calculated. Min Survival Chance: {0}", m_minSurvivalChance);
	}
	
	public void SetTaskRequirements(final float minSurvivalChance)
	{
		m_minSurvivalChance = minSurvivalChance;
		// DUtils.Log(Level.FINER, "    NCM Task requirements set. Min Survival Chance: {0}", m_minSurvivalChance);
	}
	
	private float getMeetingOfMinSurvivalChanceScore(final AggregateResults simulatedAttack, final float minSurvivalChance)
	{
		if (m_taskType.equals(NCM_TaskType.Land_Reinforce_Block))
		{
			if (m_recruitedUnits.size() > 0)
				return 1.0F; // Has reached, but not exceeded
			else
				return 0.0F;
		}
		return DUtils.Divide_SL((float) simulatedAttack.getDefenderWinPercent(), minSurvivalChance); // We're this close to meeting our min survival chance
	}
	
	private float getMeetingOfMaxBattleVolleysScore(final AggregateResults simulatedAttack, final int maxBattleVolleys)
	{
		if (m_taskType.equals(NCM_TaskType.Land_Reinforce_Block))
		{
			if (m_recruitedUnits.size() > 0)
				return 1.0F; // Has reached, but not exceeded
			else
				return 0.0F;
		}
		return DUtils.Divide_SL(maxBattleVolleys, (float) simulatedAttack.getAverageBattleRoundsFought()); // We're this close to getting the average battle volley count below max amount
	}
	
	private List<UnitGroup> m_recruitedUnits = new ArrayList<UnitGroup>();
	
	public void RecruitUnits()
	{
		recruitEnoughUnitsToMeetXYZ(m_minSurvivalChance, 100);
	}
	
	public void RecruitUnits2()
	{
		if (m_taskType.equals(NCM_TaskType.Land_Reinforce_Block) && m_recruitedUnits.size() > 0)
			return; // We only need one unit
		final float minSurvivalChance = .8F;
		final int maxBattleVolleys = 5;
		recruitEnoughUnitsToMeetXYZ(minSurvivalChance, maxBattleVolleys);
	}
	
	public void RecruitUnits3()
	{
		if (m_taskType.equals(NCM_TaskType.Land_Reinforce_Block) && m_recruitedUnits.size() > 0)
			return; // We only need one unit
		final float minSurvivalChance = .9F;
		final int maxBattleVolleys = 3;
		recruitEnoughUnitsToMeetXYZ(minSurvivalChance, maxBattleVolleys);
	}
	
	public void RecruitUnits4()
	{
		if (m_taskType.equals(NCM_TaskType.Land_Reinforce_Block) && m_recruitedUnits.size() > 0)
			return; // We only need one unit
		final float minSurvivalChance = 1.0F;
		final int maxBattleVolleys = 1;
		recruitEnoughUnitsToMeetXYZ(minSurvivalChance, maxBattleVolleys);
	}
	
	private void recruitEnoughUnitsToMeetXYZ(final float minSurvivalChance, final int maxBattleVolleys)
	{
		final List<UnitGroup> sortedPossibles = getSortedPossibleRecruits();
		if (sortedPossibles.isEmpty())
			return;
		for (final UnitGroup ug : sortedPossibles)
		{
			if (m_recruitedUnits.contains(ug)) // If already recruited
				continue;
			final List<Unit> attackers = DUtils.GetSPNNEnemyUnitsThatCanReach(m_data, m_target, GlobalCenter.CurrentPlayer, Matches.TerritoryIsLandOrWater);
			final List<Unit> defenders = GetRecruitedUnitsAsUnitList();
			defenders.addAll(DUtils.GetUnitsGoingToBePlacedAtX(m_data, GlobalCenter.CurrentPlayer, m_target));
			if (!DSettings.LoadSettings().AA_ignoreAlliedUnitsAsDefenses)
			{
				defenders.removeAll(m_target.getUnits().getUnits());
				defenders.addAll(m_target.getUnits().getUnits());
			}
			final AggregateResults simulatedAttack = DUtils.GetBattleResults(attackers, defenders, m_target, m_data, 1, true);
			final float howCloseToMeetingMinSurvivalChance = getMeetingOfMinSurvivalChanceScore(simulatedAttack, minSurvivalChance);
			if (howCloseToMeetingMinSurvivalChance < 1.0F)
			{
				m_recruitedUnits.add(ug);
				continue;
			}
			final float howCloseToMeetingBattleVolleyMax = getMeetingOfMaxBattleVolleysScore(simulatedAttack, maxBattleVolleys);
			if (howCloseToMeetingBattleVolleyMax < 1.0F)
			{
				m_recruitedUnits.add(ug);
				continue;
			}
			break; // We've met all requirements
		}
		m_recruitedUnits = DUtils.TrimRecruits_NonMovedOnes(m_recruitedUnits, 7); // Backtrack 7 units
		// Now do it carefully
		for (final UnitGroup ug : sortedPossibles)
		{
			if (m_recruitedUnits.contains(ug)) // If already recruited
				continue;
			final List<Unit> attackers = DUtils.GetSPNNEnemyUnitsThatCanReach(m_data, m_target, GlobalCenter.CurrentPlayer, Matches.TerritoryIsLandOrWater);
			final List<Unit> defenders = GetRecruitedUnitsAsUnitList();
			defenders.addAll(DUtils.GetUnitsGoingToBePlacedAtX(m_data, GlobalCenter.CurrentPlayer, m_target));
			if (!DSettings.LoadSettings().AA_ignoreAlliedUnitsAsDefenses)
			{
				defenders.removeAll(m_target.getUnits().getUnits());
				defenders.addAll(m_target.getUnits().getUnits());
			}
			final AggregateResults simulatedAttack = DUtils.GetBattleResults(attackers, defenders, m_target, m_data,
						DSettings.LoadSettings().CA_CMNCM_determinesIfTasksRequirementsAreMetEnoughForRecruitingStop, true);
			final float howCloseToMeetingMinSurvivalChance = getMeetingOfMinSurvivalChanceScore(simulatedAttack, minSurvivalChance);
			if (howCloseToMeetingMinSurvivalChance < 1.0F)
			{
				m_recruitedUnits.add(ug);
				continue;
			}
			final float howCloseToMeetingBattleVolleyMax = getMeetingOfMaxBattleVolleysScore(simulatedAttack, maxBattleVolleys);
			if (howCloseToMeetingBattleVolleyMax < 1.0F)
			{
				m_recruitedUnits.add(ug);
				continue;
			}
			break; // We've met all requirements
		}
	}
	
	public List<UnitGroup> GetRecruitedUnits()
	{
		return m_recruitedUnits;
	}
	
	public List<Unit> GetRecruitedUnitsAsUnitList()
	{
		final List<Unit> result = new ArrayList<Unit>();
		for (final UnitGroup ug : m_recruitedUnits)
		{
			result.addAll(ug.GetUnits());
		}
		return result;
	}
	
	public boolean IsPlannedMoveWorthwhile(final List<NCM_Task> allTasks)
	{
		DUtils.Log(Level.FINEST, "      Determining if ncm task is worthwhile. Target: {0} Recruits Size: {1}", m_target, m_recruitedUnits.size());
		// if(m_recruitedUnits.isEmpty()) //Remove check, as a reinforce task can sometimes have requirements met without any units recruited (no threats to, for example, a cap neighbor)
		// return false;
		final PlayerID player = GlobalCenter.CurrentPlayer;
		final List<Territory> ourCaps = DUtils.GetAllOurCaps(m_data, player);
		final List<Territory> capsAndNeighbors = new ArrayList<Territory>();
		for (final Territory cap : ourCaps)
			capsAndNeighbors.addAll(DUtils.GetTerritoriesWithinXDistanceOfY(m_data, cap, 1));
		final HashSet<Unit> capsAndNeighborsUnits = DUtils.ToHashSet(DUtils.GetUnitsInTerritories(capsAndNeighbors));
		boolean areRecruitsFromCapsOrNeighbors = false;
		for (final Unit recruit : GetRecruitedUnitsAsUnitList())
		{
			if (capsAndNeighborsUnits.contains(recruit))
			{
				areRecruitsFromCapsOrNeighbors = true;
				break;
			}
		}
		if (areRecruitsFromCapsOrNeighbors && !ourCaps.contains(m_target))
		{
			final Territory ourClosestCap = DUtils.GetOurClosestCap(m_data, player, m_target);
			ThreatInvalidationCenter.get(m_data, player).SuspendThreatInvalidation();
			final List<Unit> recruits = DUtils.CombineCollections(GetRecruitedUnitsAsUnitList(), DUtils.GetUnitsGoingToBePlacedAtX(m_data, player, m_target));
			final List<Float> capTakeoverChances = DUtils.GetTerTakeoverChanceBeforeAndAfterMove(m_data, player, ourClosestCap, m_target, recruits,
						DSettings.LoadSettings().CA_CMNCM_determinesIfTaskEndangersCap);
			ThreatInvalidationCenter.get(m_data, player).ResumeThreatInvalidation();
			if (capTakeoverChances.get(1) > .1F) // If takeover chance is 10% or more after move
			{
				// And takeover chance before and after move is at least 1% different or there average attackers left before and after move is at least 1 different
				if (capTakeoverChances.get(1) - capTakeoverChances.get(0) > .01F || capTakeoverChances.get(3) - capTakeoverChances.get(2) > 1)
				{
					DUtils.Log(Level.FINEST, "      Performing task would endanger capital, so canceling.");
					return false;
				}
			}
		}
		final List<Unit> attackers = DUtils.GetSPNNEnemyUnitsThatCanReach(m_data, m_target, GlobalCenter.CurrentPlayer, Matches.TerritoryIsLandOrWater);
		final List<Unit> defenders = GetRecruitedUnitsAsUnitList();
		defenders.addAll(DUtils.GetUnitsGoingToBePlacedAtX(m_data, GlobalCenter.CurrentPlayer, m_target));
		if (!DSettings.LoadSettings().AA_ignoreAlliedUnitsAsDefenses)
		{
			defenders.removeAll(m_target.getUnits().getUnits());
			defenders.addAll(m_target.getUnits().getUnits());
		}
		final AggregateResults simulatedAttack = DUtils
					.GetBattleResults(attackers, defenders, m_target, m_data, DSettings.LoadSettings().CA_CMNCM_determinesResponseResultsToSeeIfTaskWorthwhile, true);
		DUtils.Log(Level.FINEST, "        Enemy attack simulated. Attackers Size: {0} Defenders Size: {1} Takeover Chance: {2}", attackers.size(), defenders.size(),
					simulatedAttack.getAttackerWinPercent());
		if (m_taskType.equals(NCM_TaskType.Land_Reinforce_Block))
		{
			if (m_recruitedUnits.isEmpty())
				return false;
			final int unitCost = DUtils.GetTUVOfUnits(GetRecruitedUnitsAsUnitList(), GlobalCenter.GetPUResource());
			final TerritoryAttachment ta = TerritoryAttachment.get(m_target);
			if (ta.getProduction() < unitCost - 1 && attackers.size() > 0)
				return false;
			return true;
		}
		else if (m_taskType.equals(NCM_TaskType.Land_Reinforce_FrontLine))
		{
			float howCloseToMeetingMinSurvivalChance = getMeetingOfMinSurvivalChanceScore(simulatedAttack, m_minSurvivalChance);
			if (StatusCenter.get(m_data, player).GetStatusOfTerritory(m_target).WasAttacked_Stabalize || StatusCenter.get(m_data, player).GetStatusOfTerritory(m_target).WasAttacked_Offensive)
				howCloseToMeetingMinSurvivalChance = 9.98789F; // If we stabalize/offensive attacked here, consider this ter safe to defend
			DUtils.Log(Level.FINEST, "        How close to meeting min survival chance: {0} Needed: {1}", howCloseToMeetingMinSurvivalChance, .98F);
			if (howCloseToMeetingMinSurvivalChance < .98F)
			{
				final int tradeScoreIfAttacked = -DUtils.GetTaskTradeScore(m_data, m_target, attackers, defenders, simulatedAttack, new ArrayList<Unit>(), new ArrayList<Unit>(), null);
				DUtils.Log(Level.FINEST, "        Trade score if attacked: {0} Required for bypass: {1}", tradeScoreIfAttacked,
							DSettings.LoadSettings().TR_reinforceFrontline_enemyAttackTradeScoreRequiredToBypassRequirements);
				if (tradeScoreIfAttacked >= DSettings.LoadSettings().TR_reinforceFrontline_enemyAttackTradeScoreRequiredToBypassRequirements)
					return true; // Attacking this ter would actually hurt the enemy, so we know we're safe
				return false;
			}
			return true; // We've met all requirements
		}
		else if (m_taskType.equals(NCM_TaskType.Land_Reinforce_Stabilize))
		{
			final float howCloseToMeetingMinSurvivalChance = getMeetingOfMinSurvivalChanceScore(simulatedAttack, m_minSurvivalChance);
			DUtils.Log(Level.FINEST, "        How close to meeting min survival chance: {0} Needed: {1}", howCloseToMeetingMinSurvivalChance, .98F);
			if (howCloseToMeetingMinSurvivalChance < .98F)
				return false;
			// Note: Our airplanes should see the weakened capital and land there...
			if (ourCaps.contains(m_target)) // If this is our cap, we want to reinforce it, even if the requirements aren't met (this shouldn't happen)
			{
				DUtils.Log(Level.FINEST, "        This is our cap, we want to reinforce it, even though the requirements aren't met (this shouldn't happen)");
				return true;
			}
			return true; // We've met all requirements
		}
		else
		{
			return false; // Shouldn't happen
		}
	}
	
	public boolean IsTaskWithAdditionalRecruitsWorthwhile()
	{
		DUtils.Log(Level.FINEST, "      Determining if ncm task with additional recruits is worthwhile. Target: {0} Recruits Size: {1}", m_target, m_recruitedUnits.size());
		if (m_recruitedUnits.isEmpty()) // Can happen if all recruits are waiting for reinforcements to complete a better, nearby task
			return false;
		final PlayerID player = GlobalCenter.CurrentPlayer;
		final List<Territory> ourCaps = DUtils.GetAllOurCaps(m_data, player);
		final List<Territory> capsAndNeighbors = new ArrayList<Territory>();
		for (final Territory cap : ourCaps)
			capsAndNeighbors.addAll(DUtils.GetTerritoriesWithinXDistanceOfY(m_data, cap, 1));
		final HashSet<Unit> capsAndNeighborsUnits = DUtils.ToHashSet(DUtils.GetUnitsInTerritories(capsAndNeighbors));
		boolean areRecruitsFromCapsOrNeighbors = false;
		for (final Unit recruit : GetRecruitedUnitsAsUnitList())
		{
			if (capsAndNeighborsUnits.contains(recruit))
			{
				areRecruitsFromCapsOrNeighbors = true;
				break;
			}
		}
		if (areRecruitsFromCapsOrNeighbors && !ourCaps.contains(m_target))
		{
			final Territory ourClosestCap = DUtils.GetOurClosestCap(m_data, player, m_target);
			ThreatInvalidationCenter.get(m_data, player).SuspendThreatInvalidation();
			final List<Float> capTakeoverChances = DUtils.GetTerTakeoverChanceBeforeAndAfterMove(m_data, player, ourClosestCap, m_target, GetRecruitedUnitsAsUnitList(),
						DSettings.LoadSettings().CA_CMNCM_determinesIfTaskEndangersCap);
			ThreatInvalidationCenter.get(m_data, player).ResumeThreatInvalidation();
			if (capTakeoverChances.get(1) > .1F) // If takeover chance is 10% or more after move
			{
				// And takeover chance before and after move is at least 1% different or there average attackers left before and after move is at least 1 different
				if (capTakeoverChances.get(1) - capTakeoverChances.get(0) > .01F || capTakeoverChances.get(3) - capTakeoverChances.get(2) > 1)
				{
					DUtils.Log(Level.FINEST, "      Performing task with additional recruits would endanger capital, so canceling.");
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
	
	public void PerformTargetRetreat(final List<NCM_Task> allTasks, final IMoveDelegate mover)
	{
		DUtils.Log(Level.FINER, "      Attemping to perform target retreat for task. Target: {0} Recruits: {1}", m_target, DUtils.UnitGroupList_ToString(m_recruitedUnits));
		final PlayerID player = GlobalCenter.CurrentPlayer;
		final List<UnitGroup> retreatUnits = new ArrayList<UnitGroup>();
		// If we're retreating from this ter, retreat all non air units on this ter
		retreatUnits.addAll(DUtils.CreateUnitGroupsForUnits(
					Match.getMatches(m_target.getUnits().getUnits(), DUtils.CompMatchAnd(Matches.unitIsOwnedBy(player), DMatches.UnitIsMoveableType, Matches.UnitIsNotAir)), m_target, m_data));
		if (retreatUnits.isEmpty())
		{
			DUtils.Log(Level.FINER, "        No units to retreat for task. Target: {0}", m_target);
			return; // We have nothing to do, because there are no retreat units
		}
		// Have the frontline move to a safe frontline territory, if existing, otherwise, move to safest neighbor.
		if (m_taskType.equals(NCM_TaskType.Land_Reinforce_FrontLine))
		{
			Territory bestRetreatTer = null;
			float bestRetreatTerScore = Integer.MIN_VALUE;
			for (final NCM_Task task : (List<NCM_Task>) DUtils.ShuffleList(allTasks)) // Shuffle, so our retreat isn't predicatable
			{
				if (task.IsCompleted() && (task.GetTaskType().equals(NCM_TaskType.Land_Reinforce_FrontLine) || task.GetTaskType().equals(NCM_TaskType.Land_Reinforce_Stabilize)))
				{
					final Route ncmRoute = m_data.getMap().getLandRoute(m_target, task.GetTarget());
					if (ncmRoute == null)
						continue;
					if (Match.allMatch(retreatUnits, DMatches.UnitGroupHasEnoughMovement_All(ncmRoute.getLength()))) // If this is a valid, reachable frontline territory
					{
						List<Unit> possibleAttackers = DUtils.GetSPNNEnemyUnitsThatCanReach(m_data, task.GetTarget(), GlobalCenter.CurrentPlayer, Matches.TerritoryIsLandOrWater);
						possibleAttackers = Match.getMatches(possibleAttackers, new CompositeMatchOr<Unit>(Matches.UnitIsLand, Matches.UnitIsAir));
						final List<Unit> defenders = DUtils.GetTerUnitsAtEndOfTurn(m_data, player, task.GetTarget());
						defenders.retainAll(TacticalCenter.get(m_data, player).GetFrozenUnits()); // Only count units that have been frozen here
						defenders.removeAll(DUtils.ToUnitList(retreatUnits)); // (Don't double add)
						defenders.addAll(DUtils.ToUnitList(retreatUnits)); // And the units we're retreating
						final AggregateResults results = DUtils.GetBattleResults(possibleAttackers, defenders, task.GetTarget(), m_data, 500, true);
						float score = 0;
						score -= results.getAttackerWinPercent();
						score -= (DUtils.GetDefenseScoreOfUnits(results.GetAverageAttackingUnitsRemaining()) * .01F); // Have leftover invader strength only decide if takeover chances match
						score += DUtils.GetValueOfLandTer(task.GetTarget(), m_data, player);
						if (score > bestRetreatTerScore)
						{
							bestRetreatTer = task.GetTarget();
							bestRetreatTerScore = score;
						}
					}
				}
			}
			if (bestRetreatTer == null) // If we couldn't find any completed, reachable frontline ters to retreat to
			{
				for (final Territory ter : (List<Territory>) DUtils.ShuffleList(DUtils.GetTerritoriesWithinXDistanceOfY(m_data, m_target, GlobalCenter.FastestUnitMovement))) // Shuffle, so our retreat isn't predicatable
				{
					if (ter.isWater())
						continue;
					if (!DMatches.territoryIsOwnedByXOrAlly(m_data, GlobalCenter.CurrentPlayer).match(ter))
						continue;
					final Route ncmRoute = m_data.getMap().getLandRoute(m_target, ter);
					if (ncmRoute == null)
						continue;
					if (Match.allMatch(retreatUnits, DMatches.UnitGroupHasEnoughMovement_All(ncmRoute.getLength()))) // If this is a valid, reachable reinforce ter
					{
						List<Unit> possibleAttackers = DUtils.GetSPNNEnemyUnitsThatCanReach_CountXAsPassthroughs(m_data, ter, GlobalCenter.CurrentPlayer, Matches.TerritoryIsLandOrWater,
									Collections.singletonList(m_target));
						possibleAttackers = Match.getMatches(possibleAttackers, new CompositeMatchOr<Unit>(Matches.UnitIsLand, Matches.UnitIsAir));
						final List<Unit> defenders = DUtils.GetTerUnitsAtEndOfTurn(m_data, player, ter);
						defenders.retainAll(TacticalCenter.get(m_data, player).GetFrozenUnits()); // Only count units that have been frozen here
						defenders.removeAll(DUtils.ToUnitList(retreatUnits)); // (Don't double add)
						defenders.addAll(DUtils.ToUnitList(retreatUnits)); // And the units we're retreating
						final AggregateResults results = DUtils.GetBattleResults(possibleAttackers, defenders, ter, m_data, 500, true);
						float score = 0;
						score -= results.getAttackerWinPercent() * 1000;
						score -= results.GetAverageAttackingUnitsRemaining().size();
						score -= DUtils.GetDefenseScoreOfUnits(results.GetAverageAttackingUnitsRemaining()); // Have leftover invader strength only decide if takeover chances match
						score += DUtils.GetValueOfLandTer(ter, m_data, player);
						if (score > bestRetreatTerScore)
						{
							bestRetreatTer = ter;
							bestRetreatTerScore = score;
						}
					}
				}
			}
			if (bestRetreatTer != null)
			{
				final List<Territory> ourCaps = DUtils.GetAllOurCaps(m_data, player);
				final List<Territory> capsNeighbors = new ArrayList<Territory>();
				for (final Territory cap : ourCaps)
					capsNeighbors.addAll(DUtils.GetTerritoriesWithinXDistanceOfY(m_data, cap, 1));
				capsNeighbors.removeAll(ourCaps); // We only want the neighbors
				if (capsNeighbors.contains(m_target))
				{
					final Territory ourClosestCap = DUtils.GetOurClosestCap(m_data, player, m_target);
					ThreatInvalidationCenter.get(m_data, player).SuspendThreatInvalidation();
					final List<Float> capTakeoverChances = DUtils.GetTerTakeoverChanceBeforeAndAfterMove(m_data, player, ourClosestCap, m_target, GetRecruitedUnitsAsUnitList(),
								DSettings.LoadSettings().CA_CMNCM_determinesIfTaskEndangersCap);
					ThreatInvalidationCenter.get(m_data, player).ResumeThreatInvalidation();
					if (capTakeoverChances.get(1) > .1F) // If takeover chance is 10% or more after move
					{
						// And takeover chance before and after move is at least 1% different or there average attackers left before and after move is at least 1 different
						if (capTakeoverChances.get(1) - capTakeoverChances.get(0) > .01F || capTakeoverChances.get(3) - capTakeoverChances.get(2) > 1)
						{
							DUtils.Log(Level.FINEST, "      Retreating units would endanger capital, so leaving one behind.");
							UnitGroup cheapestUG = null;
							int cheapestCost = Integer.MAX_VALUE;
							for (final UnitGroup ug : retreatUnits)
							{
								final int cost = DUtils.GetTUVOfUnit(ug.GetFirstUnit(), GlobalCenter.GetPUResource());
								if (cost < cheapestCost)
								{
									cheapestUG = ug;
									cheapestCost = cost;
								}
							}
							retreatUnits.remove(cheapestUG);
						}
					}
				}
				DUtils.Log(Level.FINER, "      Attempting to perform target retreat. Target: {0} Retreat To: {1} Retreat Units: {2}", m_target, bestRetreatTer,
							DUtils.UnitGroupList_ToString(retreatUnits));
				Dynamix_AI.Pause();
				UnitGroup.EnableMoveBuffering();
				for (final UnitGroup ug : retreatUnits)
				{
					final String error = ug.MoveAsFarTo_NCM(bestRetreatTer, mover);
					if (error == null)
						TacticalCenter.get(m_data, GlobalCenter.CurrentPlayer).FreezeUnits(ug.GetUnitsAsList());
					else
						DUtils.Log(Level.FINER, "        NCM move failed, reason: {0}", error);
				}
				final String errors = UnitGroup.PerformBufferedMovesAndDisableMoveBufferring(mover);
				if (errors != null)
					DUtils.Log(Level.FINER, "      Some errors occurred while performing moves: {0}", errors);
			}
			else
				DUtils.Log(Level.FINER, "      No retreat to ter found for for task. Target: {0} Recruits: {1} Retreat Units: {2}", m_target, m_recruitedUnits,
							DUtils.UnitGroupList_ToString(retreatUnits));
		}
		else if (m_taskType.equals(NCM_TaskType.Land_Reinforce_Stabilize))
		{
			Territory bestRetreatTer = null;
			float bestRetreatTerScore = Integer.MIN_VALUE;
			final List<Territory> ourCaps = DUtils.GetAllOurCaps(m_data, player);
			final List<Territory> capsAndNeighbors = new ArrayList<Territory>();
			for (final Territory cap : ourCaps)
				capsAndNeighbors.addAll(DUtils.GetTerritoriesWithinXDistanceOfY(m_data, cap, 1));
			if (capsAndNeighbors.contains(m_target))
				bestRetreatTer = DUtils.GetOurClosestCap(m_data, player, m_target); // We are endangered and next to cap, so retreat there (not sure if we should do this)
			if (bestRetreatTer == null)
			{
				for (final Territory ter : (List<Territory>) DUtils.ShuffleList(DUtils.GetTerritoriesWithinXDistanceOfY(m_data, m_target, GlobalCenter.FastestUnitMovement))) // Shuffle, so our retreat isn't predicatable
				{
					if (ter.isWater())
						continue;
					if (!DMatches.territoryIsOwnedByXOrAlly(m_data, GlobalCenter.CurrentPlayer).match(ter))
						continue;
					final Route ncmRoute = CachedCalculationCenter.GetPassableLandRoute(m_data, m_target, ter);
					if (ncmRoute == null)
						continue;
					if (Match.allMatch(retreatUnits, DMatches.UnitGroupHasEnoughMovement_All(ncmRoute.getLength()))) // If this is a valid, reachable reinforce ter
					{
						List<Unit> possibleAttackers = DUtils.GetSPNNEnemyUnitsThatCanReach_CountXAsPassthroughs(m_data, ter, GlobalCenter.CurrentPlayer, Matches.TerritoryIsLandOrWater,
									Collections.singletonList(m_target));
						possibleAttackers = Match.getMatches(possibleAttackers, new CompositeMatchOr<Unit>(Matches.UnitIsLand, Matches.UnitIsAir));
						// Note that since this ter was not a reinforce_task ter(well, at least not a successful one), it is most likely within our territory
						final List<Unit> defenders = DUtils.GetTerUnitsAtEndOfTurn(m_data, player, ter);
						defenders.retainAll(TacticalCenter.get(m_data, player).GetFrozenUnits()); // Only count units that have been frozen here
						defenders.removeAll(DUtils.ToUnitList(retreatUnits)); // (Don't double add)
						defenders.addAll(DUtils.ToUnitList(retreatUnits)); // And the units we're retreating
						final AggregateResults results = DUtils.GetBattleResults(possibleAttackers, defenders, ter, m_data, 500, true);
						float score = 0;
						score -= results.getAttackerWinPercent() * 1000;
						score -= results.GetAverageAttackingUnitsRemaining().size();
						score -= DUtils.GetDefenseScoreOfUnits(results.GetAverageAttackingUnitsRemaining()); // Have leftover invader strength only decide if takeover chances match
						score += DUtils.GetValueOfLandTer(ter, m_data, player);
						if (score > bestRetreatTerScore)
						{
							bestRetreatTer = ter;
							bestRetreatTerScore = score;
						}
					}
				}
			}
			if (bestRetreatTer != null)
			{
				final List<Territory> capsNeighbors = new ArrayList<Territory>();
				for (final Territory cap : ourCaps)
					capsNeighbors.addAll(DUtils.GetTerritoriesWithinXDistanceOfY(m_data, cap, 1));
				capsNeighbors.removeAll(ourCaps); // We only want the neighbors
				if (capsNeighbors.contains(m_target))
				{
					final Territory ourClosestCap = DUtils.GetOurClosestCap(m_data, player, m_target);
					ThreatInvalidationCenter.get(m_data, player).SuspendThreatInvalidation();
					final List<Float> capTakeoverChances = DUtils.GetTerTakeoverChanceBeforeAndAfterMove(m_data, player, ourClosestCap, m_target, GetRecruitedUnitsAsUnitList(),
								DSettings.LoadSettings().CA_CMNCM_determinesIfTaskEndangersCap);
					ThreatInvalidationCenter.get(m_data, player).ResumeThreatInvalidation();
					if (capTakeoverChances.get(1) > .1F) // If takeover chance is 10% or more after move
					{
						// And takeover chance before and after move is at least 1% different or there average attackers left before and after move is at least 1 different
						if (capTakeoverChances.get(1) - capTakeoverChances.get(0) > .01F || capTakeoverChances.get(3) - capTakeoverChances.get(2) > 1)
						{
							DUtils.Log(Level.FINEST, "      Retreating units would endanger capital, so leaving one behind.");
							UnitGroup cheapestUG = null;
							int cheapestCost = Integer.MAX_VALUE;
							for (final UnitGroup ug : retreatUnits)
							{
								final int cost = DUtils.GetTUVOfUnit(ug.GetFirstUnit(), GlobalCenter.GetPUResource());
								if (cost < cheapestCost)
								{
									cheapestUG = ug;
									cheapestCost = cost;
								}
							}
							retreatUnits.remove(cheapestUG);
						}
					}
				}
				DUtils.Log(Level.FINER, "      Attempting to perform target retreat. Target: {0} Retreat To: {1} Retreat Units: {2}", m_target, bestRetreatTer,
							DUtils.UnitGroupList_ToString(retreatUnits));
				Dynamix_AI.Pause();
				UnitGroup.EnableMoveBuffering();
				for (final UnitGroup ug : retreatUnits)
				{
					final String error = ug.MoveAsFarTo_NCM(bestRetreatTer, mover);
					if (error == null)
						TacticalCenter.get(m_data, GlobalCenter.CurrentPlayer).FreezeUnits(ug.GetUnitsAsList());
					else
						DUtils.Log(Level.FINEST, "        NCM move failed, reason: {0}", error);
				}
				final String errors = UnitGroup.PerformBufferedMovesAndDisableMoveBufferring(mover);
				if (errors != null)
					DUtils.Log(Level.FINER, "      Some errors occurred while performing moves: {0}", errors);
			}
			else
				DUtils.Log(Level.FINER, "      No retreat to ter found for for task. Target: {0} Recruits: {1} Retreat Units: {2}", m_target, m_recruitedUnits,
							DUtils.UnitGroupList_ToString(retreatUnits));
		}
	}
	
	/**
	 * If this is an ncm task that is strong enough to resist all it's threats, we invalidate them because they can't attack more than one place.
	 * (This method was added to fix the problem where one far-away airplane 'stack' can discourage ALL our attacks in the area, which is very bad.
	 * Now, if one task can resist the plane stack, we assume the other movements are partially 'safe' from this enemy stack)
	 */
	public void InvalidateThreatsThisTaskResists()
	{
		final PlayerID player = GlobalCenter.CurrentPlayer;
		final List<Territory> ourCaps = DUtils.GetAllOurCaps(m_data, player);
		if (ourCaps.contains(m_target))
			return; // If this is one of our caps, don't invalidate it's threats (we can't really threaten enemy attacks with it, cause we need it safe)
		if (m_target.getUnits().getMatches(Matches.UnitIsFactory).size() > 0) // If this is a factory ter, don't invalidate it's threats (we don't want to attack neighbors that get taken over from fact ters
			return;
		if (m_taskType == NCM_TaskType.Land_Reinforce_FrontLine)
		{
			final List<Unit> threats = DUtils.GetSPNNEnemyUnitsThatCanReach(m_data, m_target, GlobalCenter.CurrentPlayer, Matches.TerritoryIsLandOrWater);
			if (threats.isEmpty()) // No threats to invalidate
				return;
			final List<Unit> defenders = GetRecruitedUnitsAsUnitList();
			final AggregateResults simulatedAttack = DUtils.GetBattleResults(threats, defenders, m_target, m_data,
						DSettings.LoadSettings().CA_CMNCM_determinesSurvivalChanceAfterTaskToSeeIfToInvalidateAttackers, true);
			if (simulatedAttack.getDefenderWinPercent() > .4F)
			{
				ThreatInvalidationCenter.get(m_data, player).InvalidateThreats(threats, m_target);
				// DUtils.Log(Level.FINER, "      Land_Reinforce_Frontline task succeeded with enough defense, so invalidating threats resisted by this task. Target: {0} Units Invalidated: {1}", m_target, threats);
			}
		}
		else if (m_taskType == NCM_TaskType.Land_Reinforce_Stabilize)
		{
			final List<Unit> threats = DUtils.GetSPNNEnemyUnitsThatCanReach(m_data, m_target, GlobalCenter.CurrentPlayer, Matches.TerritoryIsLandOrWater);
			if (threats.isEmpty()) // No threats to invalidate
				return;
			final List<Unit> defenders = GetRecruitedUnitsAsUnitList();
			final AggregateResults simulatedAttack = DUtils.GetBattleResults(threats, defenders, m_target, m_data,
						DSettings.LoadSettings().CA_CMNCM_determinesSurvivalChanceAfterTaskToSeeIfToInvalidateAttackers, true);
			if (simulatedAttack.getDefenderWinPercent() > .4F)
			{
				ThreatInvalidationCenter.get(m_data, player).InvalidateThreats(threats, m_target);
				// DUtils.Log(Level.FINER, "      Land_Reinforce_Stabalize task succeeded with enough defense, so invalidating threats resisted by this task. Target: {0} Units Invalidated: {1}", m_target, threats);
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
	
	public void PerformTask(final IMoveDelegate mover)
	{
		if (m_recruitedUnits.isEmpty())
		{
			DUtils.Log(Level.FINER, "      Task is called to perform, but there are no recruits! Target: {0} Task Type: {1} Priority: {2}", m_target, m_taskType, m_priority);
			m_completed = true;
			return; // We don't want to pause for an 'empty' task
		}
		UnitGroup.EnableMoveBuffering();
		boolean anythingMoved = false;
		for (final UnitGroup ug : m_recruitedUnits)
		{
			if (ug.GetMovedTo() != null)
				continue; // If this recruit has already moved
			final String error = ug.MoveAsFarTo_NCM(m_target, mover);
			if (error != null)
				DUtils.Log(Level.FINER, "        NCM task perfoming move failed, reason: {0}", error);
			else
			{
				TacticalCenter.get(m_data, GlobalCenter.CurrentPlayer).FreezeUnits(ug.GetUnitsAsList());
				anythingMoved = true;
			}
		}
		if (!anythingMoved)
		{
			m_disqualified = true;
			return;
		}
		if (!m_completed) // Only pause if this is the initial attack group
			Dynamix_AI.Pause();
		final String errors = UnitGroup.PerformBufferedMovesAndDisableMoveBufferring(mover);
		if (errors != null)
		{
			DUtils.Log(Level.FINER, "      Some errors occurred while performing moves: {0}", errors);
			m_disqualified = true;
			return;
		}
		ReconsiderSignalCenter.get(m_data, GlobalCenter.CurrentPlayer).ObjectsToReconsider.addAll(CachedInstanceCenter.CachedGameData.getMap().getNeighbors(m_target));
		m_completed = true;
		StatusCenter.get(m_data, GlobalCenter.CurrentPlayer).GetStatusOfTerritory(m_target).NotifyTaskPerform(this);
	}
}
