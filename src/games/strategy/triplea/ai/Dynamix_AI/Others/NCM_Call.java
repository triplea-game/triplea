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
package games.strategy.triplea.ai.Dynamix_AI.Others;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.Dynamix_AI.DMatches;
import games.strategy.triplea.ai.Dynamix_AI.DSettings;
import games.strategy.triplea.ai.Dynamix_AI.DSorting;
import games.strategy.triplea.ai.Dynamix_AI.DUtils;
import games.strategy.triplea.ai.Dynamix_AI.Dynamix_AI;
import games.strategy.triplea.ai.Dynamix_AI.CommandCenter.GlobalCenter;
import games.strategy.triplea.ai.Dynamix_AI.CommandCenter.TacticalCenter;
import games.strategy.triplea.ai.Dynamix_AI.CommandCenter.ThreatInvalidationCenter;
import games.strategy.triplea.ai.Dynamix_AI.Group.UnitGroup;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.oddsCalculator.ta.AggregateResults;
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
public class NCM_Call
{
	private Territory m_target = null;
	private NCM_CallType m_callType = NCM_CallType.Empty;
	private float m_priority = 0.0F;
	private GameData m_data = null;
	
	public NCM_Call(final GameData data, final Territory target, final NCM_CallType type, final float priority)
	{
		m_data = data;
		m_callType = type;
		m_priority = priority;
		m_target = target;
	}
	
	public Territory GetTarget()
	{
		return m_target;
	}
	
	public NCM_CallType GetCallType()
	{
		return m_callType;
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
		boolean addedAA = false;
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
					if (Matches.UnitIsInfrastructure.match(unit) && ua.getDefense(unit.getOwner()) <= 0)
						return false;
					if (Matches.UnitCanProduceUnits.match(unit) && ua.getDefense(unit.getOwner()) <= 0)
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
				if (Matches.UnitIsAAforAnything.match(unit))
				{
					// If this is an AA and we've already added an AA as a recruit or (the from ter has a factory and this is the only AA), skip AA
					if (addedAA || (ter.getUnits().getMatches(Matches.UnitCanProduceUnitsAndCanBeDamaged).size() > 0 && ter.getUnits().getMatches(Matches.UnitIsAAforAnything).size() <= 1))
						continue;
					else
						addedAA = true;
				}
				final int suitability = DUtils.HowWellIsUnitSuitedToCall(m_data, this, ter, unit);
				if (suitability == Integer.MIN_VALUE)
					continue;
				possibles.put(unit, suitability);
				unitLocations.put(unit, ter);
			}
		}
		List<Unit> sortedPossibles = DUtils.ToList(possibles.keySet());
		// For now, shuffle,
		Collections.shuffle(sortedPossibles);
		// Then sort by score. In this way, equal scored attack units are shuffled
		sortedPossibles = DSorting.SortListByScores_List_D(sortedPossibles, possibles.values());
		// Now put the units into UnitGroups and return the list
		final List<UnitGroup> result = new ArrayList<UnitGroup>();
		for (final Unit unit : sortedPossibles)
			result.add(DUtils.CreateUnitGroupForUnits(Collections.singletonList(unit), unitLocations.get(unit), m_data));
		return result;
	}
	
	private float m_minSurvivalChance = 0.0F;
	
	public void CalculateCallRequirements()
	{
		if (m_callType.equals(NCM_CallType.Land_ForLandGrab))
			return; // Only one unit needed for land grab call
		if (m_callType == NCM_CallType.Land_ForDefensiveFront)
			m_minSurvivalChance = .75F;
		else if (m_callType == NCM_CallType.Land_ForCapitalDefense)
			m_minSurvivalChance = 1.0F;
		// DUtils.Log(Level.FINER, "    NCM Call requirements calculated. Min Survival Chance: {0}", m_minSurvivalChance);
	}
	
	public void SetCallRequirements(final float minSurvivalChance)
	{
		m_minSurvivalChance = minSurvivalChance;
		// DUtils.Log(Level.FINER, "    NCM Call requirements set. Min Survival Chance: {0}", m_minSurvivalChance);
	}
	
	private float getMeetingOfMinSurvivalChanceScore(final AggregateResults simulatedAttack, final float minSurvivalChance)
	{
		if (m_callType.equals(NCM_CallType.Land_ForLandGrab))
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
		if (m_callType.equals(NCM_CallType.Land_ForLandGrab))
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
		if (m_callType.equals(NCM_CallType.Land_ForLandGrab) && m_recruitedUnits.size() > 0)
			return; // Only one unit needed for land grab call
		final float minSurvivalChance = .90F;
		final int maxBattleVolleys = 5;
		recruitEnoughUnitsToMeetXYZ(minSurvivalChance, maxBattleVolleys);
	}
	
	public void RecruitUnits3()
	{
		if (m_callType.equals(NCM_CallType.Land_ForLandGrab) && m_recruitedUnits.size() > 0)
			return; // Only one unit needed for land grab call
		final float minSurvivalChance = 1.0F;
		final int maxBattleVolleys = 3; // (One seems to cause problems)
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
	
	public boolean IsPlannedMoveWorthwhile(final List<NCM_Call> allCalls)
	{
		DUtils.Log(Level.FINEST, "      Determining if ncm call is worthwhile. Target: {0} Recruits Size: {1}", m_target, m_recruitedUnits.size());
		if (m_recruitedUnits.isEmpty())
			return false; // Calls aren't 'worthwhile' unless there are units to move
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
					DUtils.Log(Level.FINEST, "      Performing call would endanger capital, so canceling.");
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
		if (m_callType.equals(NCM_CallType.Land_ForDefensiveFront))
		{
			final float howCloseToMeetingMinSurvivalChance = getMeetingOfMinSurvivalChanceScore(simulatedAttack, m_minSurvivalChance);
			DUtils.Log(Level.FINEST, "        How close to meeting min survival chance: {0} Needed: {1}", howCloseToMeetingMinSurvivalChance, .98F);
			if (howCloseToMeetingMinSurvivalChance < .98F)
				return false;
			return true; // We've met all requirements
		}
		else if (m_callType.equals(NCM_CallType.Land_ForCapitalDefense))
		{
			final float howCloseToMeetingMinSurvivalChance = getMeetingOfMinSurvivalChanceScore(simulatedAttack, m_minSurvivalChance);
			DUtils.Log(Level.FINEST, "        How close to meeting min survival chance: {0} Needed: {1}", howCloseToMeetingMinSurvivalChance, .98F);
			if (howCloseToMeetingMinSurvivalChance < .98F)
				DUtils.Log(Level.FINEST, "        Since this is a call for capital defense, we'll perform this call even if the requirements aren't met...");// return false;
			return true; // We've met all requirements
		}
		else
		{
			boolean canUnitsGetBack = false;
			if (UnitAttachment.get(m_recruitedUnits.get(0).GetFirstUnit().getUnitType()).getMovement(player) > 1)
				canUnitsGetBack = true;
			if (canUnitsGetBack) // If the user said "Only grab land with blitz attacks", there wouldn't be any non-blitz units here (not counted as possibles earlier)
				return true;
			final int unitCost = DUtils.GetTUVOfUnits(GetRecruitedUnitsAsUnitList(), GlobalCenter.GetPUResource());
			final int production = TerritoryAttachment.getProduction(m_target);
			final List<Unit> landAttackers = DUtils.GetNNEnemyLUnitsThatCanReach(m_data, m_target, GlobalCenter.CurrentPlayer, Matches.TerritoryIsLandOrWater);
			if (unitCost - 1 <= production || landAttackers.isEmpty())
				return true;
			return false;
		}
	}
	
	public boolean IsCallWithAdditionalRecruitsWorthwhile()
	{
		DUtils.Log(Level.FINEST, "      Determining if ncm call with additional recruits is worthwhile. Target: {0} Recruits Size: {1}", m_target, m_recruitedUnits.size());
		if (m_recruitedUnits.isEmpty())
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
					DUtils.Log(Level.FINEST, "      Performing call with additional recruits would endanger capital, so canceling.");
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
	
	public void Reset()
	{
		DUtils.Log(Level.FINER, "        Resetting call. Target: {0} Call Type: {1} Priority: {2} Recruit Size: {3}", m_target, m_callType, m_priority, m_recruitedUnits.size());
		m_completed = false;
		m_disqualified = false;
		m_recruitedUnits = new ArrayList<UnitGroup>();
	}
	
	private boolean m_completed = false;
	
	public boolean IsCompleted()
	{
		return m_completed;
	}
	
	public void PerformCall(final IMoveDelegate mover)
	{
		if (m_recruitedUnits.isEmpty())
		{
			DUtils.Log(Level.FINER, "      Call is called to perform, but there are no recruits! Target: {0} Call Type: {1} Priority: {2}", m_target, m_callType, m_priority);
			m_completed = true;
			return; // We don't want to pause for an 'empty' call
		}
		UnitGroup.EnableMoveBuffering();
		boolean anythingMoved = false;
		for (final UnitGroup ug : m_recruitedUnits)
		{
			if (ug.GetMovedTo() != null)
				continue; // If this recruit has already moved
			final String error = ug.MoveAsFarTo_NCM(m_target, mover, true); // Don't move into dangerous ters
			if (error != null)
				DUtils.Log(Level.FINER, "        NCM call perfoming move failed, reason: {0}", error);
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
		m_completed = true;
	}
}
