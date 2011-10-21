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
import games.strategy.triplea.Dynamix_AI.DMatches;
import games.strategy.triplea.Dynamix_AI.DSettings;
import games.strategy.triplea.Dynamix_AI.DUtils;
import games.strategy.triplea.Dynamix_AI.Dynamix_AI;
import games.strategy.triplea.Dynamix_AI.CommandCenter.CachedCalculationCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.CachedInstanceCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.GlobalCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.ReconsiderSignalCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.ThreatInvalidationCenter;
import games.strategy.triplea.Dynamix_AI.Group.MovePackage;
import games.strategy.triplea.Dynamix_AI.Group.UnitGroup;
import games.strategy.triplea.Dynamix_AI.Others.CM_Task;
import games.strategy.triplea.Dynamix_AI.Others.CM_TaskType;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.oddsCalculator.ta.AggregateResults;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.Match;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.SwingUtilities;

/**
 * 
 * @author Stephen
 */
@SuppressWarnings("unchecked")
public class DoCombatMove
{
	public static void doCombatMove(Dynamix_AI ai, GameData data, IMoveDelegate mover, PlayerID player)
	{
		if (DSettings.LoadSettings().AIC_disableAllUnitMovements)
		{
			final String message = ai.getName() + " is skipping it's cm phase, as instructed.";
			DUtils.Log(Level.FINE, message);
			Runnable runner = new Runnable()
			{
				@Override
				public void run()
				{
					CachedInstanceCenter.CachedDelegateBridge.getHistoryWriter().startEvent(message);
				}
			};
			try
			{
				SwingUtilities.invokeAndWait(runner);
			} catch (InterruptedException ex)
			{
			} catch (InvocationTargetException ex)
			{
			}
			Dynamix_AI.Pause();
			return;
		}
		
		MovePackage pack = new MovePackage(ai, data, mover, player, null, null, null);
		
		List<CM_Task> tasks = GenerateTasks(pack);
		
		final List<Territory> ourCaps = TerritoryAttachment.getAllCapitals(player, data);
		final List<Territory> capsAndNeighbors = new ArrayList<Territory>();
		for (Territory cap : ourCaps)
			capsAndNeighbors.addAll(DUtils.GetTerritoriesWithinXDistanceOfY(data, cap, 1));
		List<CM_Task> capsAndNeighborsAttackTasks = new ArrayList<CM_Task>();
		for (CM_Task task : tasks)
		{
			if (capsAndNeighbors.contains(task.GetTarget()))
				capsAndNeighborsAttackTasks.add(task);
		}
		
		DUtils.Log(Level.FINE, "  Beginning capital-protecting attacks on cap-neighboring enemies section");
		// First, we check if there are any attacks on cap-neighbors we want to do
		while (considerAndPerformWorthwhileTasks(pack, capsAndNeighborsAttackTasks))
		{
		}
		
		// Then we run the NCM doPreCombatMove method which attempts to move units to the cap to keep it safe
		DoNonCombatMove.doPreCombatMove(ai, data, mover, player);
		
		// We loop this part, because sometimes there are attacks that at first are too risky to perform, but after some nearby tasks are performed, the results are more favorable or predictable.
		DUtils.Log(Level.FINE, "  Beginning task consideration loop section");
		for (int i = 0; i < 5; i++)
		{
			DUtils.Log(Level.FINE, "  Task consideration loop {0} started", i + 1);
			ReconsiderSignalCenter.get(data, player).ObjectsToReconsider.clear();
			
			List<Territory> tersAttackedBeforeLoop = new ArrayList<Territory>();
			for (CM_Task task : tasks)
			{
				if (task.IsCompleted())
					tersAttackedBeforeLoop.add(task.GetTarget());
			}
			
			while (considerAndPerformWorthwhileTasks(pack, tasks))
			{
			}
			
			if (ReconsiderSignalCenter.get(data, player).ObjectsToReconsider.isEmpty()) // If we performed no tasks, basically...
				break;
			else
			{
				List<Territory> tersToReconsider = DUtils.ToList(ReconsiderSignalCenter.get(data, player).ObjectsToReconsider);
				for (CM_Task task : tasks)
				{
					if (tersToReconsider.contains(task.GetTarget()))
					{
						if (task.IsCompleted())
						{
							/*if(!tersAttackedBeforeLoop.contains(task.GetTarget())) //If this ter was attacked this loop
							    continue;

							for(UnitGroup ug : task.GetRecruitedUnits())
							    UnitGroup.UndoMove_NotifyAllUGs(mover, ug.GetMoveIndex()); //Undo moves, and calculate again, cause we might not need this many after all
							task.Reset();*/
						}
						else
							task.Reset(); // We reset disqualified tasks for another attempt (now that we know of completed tasks)
					}
				}
			}
		}
		
		DUtils.Log(Level.FINE, "  Calculating and adding additional task recruits. (Wave 2)");
		for (CM_Task task : tasks)
		{
			if (task.IsCompleted())
			{
				task.RecruitUnits2();
				if (task.IsTaskWithAdditionalRecruitsWorthwhile())
				{
					task.PerformTask(mover);
					task.InvalidateThreatsThisTaskResists();
				}
			}
		}
		
		DUtils.Log(Level.FINE, "  Calculating and adding additional task recruits. (Wave 3)");
		for (CM_Task task : tasks)
		{
			if (task.IsCompleted())
			{
				task.RecruitUnits3();
				if (task.IsTaskWithAdditionalRecruitsWorthwhile())
				{
					task.PerformTask(mover);
					task.InvalidateThreatsThisTaskResists();
				}
			}
		}
		
		ThreatInvalidationCenter.get(data, player).SuspendThreatInvalidation(); // Suspend the threat invalidation center, as we want wave 4 to do as much as we'd ever want
		
		DUtils.Log(Level.FINE, "  Calculating and adding additional task recruits. (Wave 4)");
		for (CM_Task task : tasks)
		{
			if (task.IsCompleted())
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
		
		DUtils.Log(Level.FINE, "  Beginning of temporary ship movement block.");
		if (true) // Temporary ship movement block
		{
			UnitGroup.PerformBufferedMovesAndDisableMoveBufferring(mover); // Make sure move buffering is off, so moves are done right away
			
			for (Territory ter : data.getMap().getTerritories())
			{
				if (!ter.isWater())
					continue;
				
				List<Unit> ourUnitGroup = ter.getUnits().getMatches(Matches.unitIsOwnedBy(player));
				if (ourUnitGroup.isEmpty())
					continue;
				
				int unfilledTransports = 0;
				int filledTransports = 0;
				for (Unit unit : ourUnitGroup)
				{
					if (Matches.UnitIsTransport.match(unit))
					{
						if (Matches.unitIsTransporting().match(unit))
							filledTransports++;
						else
							unfilledTransports++;
					}
				}
				
				// If we have transports to fill, AND as long as we have fewer than 10 filled transports
				if (unfilledTransports > 0 && filledTransports < 10) // Todo: Remove hard-coded 10
				{
					Territory loadingTer = null;
					Territory loadingPort = null;
					int highestLoadingTerScore = Integer.MIN_VALUE;
					for (Territory ter2 : data.getMap().getTerritories())
					{
						if (ter2.isWater())
							continue;
						if (ter2.getUnits().getMatches(DUtils.CompMatchAnd(Matches.unitIsLandAndOwnedBy(player), Matches.UnitHasEnoughMovement(1), Matches.UnitCanBeTransported)).isEmpty())
							continue;
						List<Territory> areaTroubleTers = DUtils.GetTerritoriesWithinXDistanceOfYMatchingZAndHavingRouteMatchingA(data, ter2, (int) (3 * GlobalCenter.MapTerCountScale),
									DUtils.CompMatchAnd(Matches.TerritoryIsLand, Matches.territoryHasEnemyLandUnits(player, data)), DMatches.TerritoryIsLandAndPassable);
						if (areaTroubleTers.size() > 0)
							continue; // Only load units from areas in peace
							
						Territory openPort = null;
						for (Territory port : data.getMap().getNeighbors(ter2, Matches.TerritoryIsWater))
						{
							if (data.getMap().getRoute(ter2, port, DUtils.CompMatchAnd(Matches.TerritoryIsWater)) == null)
								continue;
							
							openPort = port;
							break;
						}
						
						if (openPort == null)
							continue;
						
						int score = 0;
						score -= CachedCalculationCenter.GetSeaRoute(data, ter, openPort).getLength();
						
						if (score > highestLoadingTerScore)
						{
							highestLoadingTerScore = score;
							loadingTer = ter2;
							loadingPort = openPort;
						}
					}
					
					if (loadingTer == null)
						continue;
					
					// Move our ships to the loading port
					UnitGroup ships = DUtils.CreateUnitGroupForUnits(ourUnitGroup, ter, data);
					String error = ships.MoveAsFarTo_CM(loadingPort, mover);
					if (error != null)
					{
						DUtils.Log(Level.FINER, "    There was an error moving ships[{0}] to loading port({1}->{2}): {3}", ships, ter, loadingPort, error);
						continue;
					}
					
					List<Unit> toLoadOntoShips = loadingTer.getUnits().getMatches(
								DUtils.CompMatchAnd(Matches.unitIsLandAndOwnedBy(player), Matches.UnitHasEnoughMovement(1), Matches.UnitCanBeTransported));
					
					// Move land units onto ships
					for (Unit unit : toLoadOntoShips)
					{
						UnitGroup ug = DUtils.CreateUnitGroupForUnit(unit, loadingTer, data);
						String error2 = ug.MoveAsFarTo_NCM(loadingPort, mover);
						if (error2 != null)
						{
							DUtils.Log(Level.FINER, "    There was an error moving units[{0}] onto ship({1}->{2}): {3}", ug, loadingTer, loadingPort, error2);
							continue;
						}
						ourUnitGroup.add(unit);
					}
				}
				else if (filledTransports == 0) // We are a bunch of battleships
				{
					Territory closestTerWithOtherShips = DUtils.GetClosestTerMatchingX(data, ter,
								DUtils.CompMatchAnd(Matches.TerritoryIsWater, Matches.territoryIs(ter).invert(), Matches.territoryHasUnitsThatMatch(Matches.unitIsOwnedBy(player))));
					
					for (Unit unit : ourUnitGroup)
					{
						UnitGroup ug = DUtils.CreateUnitGroupForUnit(unit, ter, data);
						ug.MoveAsFarTo_CM(closestTerWithOtherShips, mover);
					}
				}
				
				// All ships should now either have units on them or are on their way to get some
				
				// Check to see if we still have unfilled transports
				unfilledTransports = 0;
				filledTransports = 0;
				for (Unit unit : ourUnitGroup)
				{
					if (Matches.UnitIsTransport.match(unit))
					{
						if (Matches.unitIsTransporting().match(unit))
							filledTransports++;
						else
							unfilledTransports++;
					}
				}
				
				// Calculate unloading territory
				Territory unloadingTer = null;
				Territory unloadingPort = null;
				int highestUnloadingTerScore = Integer.MIN_VALUE;
				for (Territory ter2 : data.getMap().getTerritories())
				{
					if (ter2.isWater())
						continue;
					if (!Matches.TerritoryIsPassableAndNotRestricted(player).match(ter2))
						continue; // We have to be able to land units here
						
					Territory openPort = null;
					for (Territory port : data.getMap().getNeighbors(ter2, Matches.TerritoryIsWater))
					{
						if (data.getMap().getRoute(ourUnitGroup.get(0).getTerritoryUnitIsIn(), port, DUtils.CompMatchAnd(Matches.TerritoryIsWater)) == null)
							continue;
						
						openPort = port;
						break;
					}
					
					if (openPort == null)
						continue;
					
					int score = 0;
					List<Territory> areaTroubleTers = DUtils.GetTerritoriesWithinXDistanceOfYMatchingZAndHavingRouteMatchingA(data, ter2, (int) (3 * GlobalCenter.MapTerCountScale),
								DUtils.CompMatchAnd(Matches.TerritoryIsLand, Matches.territoryHasEnemyLandUnits(player, data)), DMatches.TerritoryIsLandAndPassable);
					if (areaTroubleTers.isEmpty())
						score -= 1000000000; // We really really want to land somewhere close to someplace we can actually help
					List<Territory> continentTroubleTers = DUtils.GetTerritoriesWithinXDistanceOfYMatchingZAndHavingRouteMatchingA(data, ter2, Integer.MAX_VALUE,
								DUtils.CompMatchAnd(Matches.TerritoryIsLand, Matches.territoryHasEnemyLandUnits(player, data)), DMatches.TerritoryIsLandAndPassable);
					if (continentTroubleTers.isEmpty())
						score -= 10000000; // We really want to land on a continent where we can actually help
					score -= CachedCalculationCenter.GetSeaRoute(data, ourUnitGroup.get(0).getTerritoryUnitIsIn(), openPort).getLength() * 100000; // And having the unload ter closeby is good too
					score -= ter2.getUnits().getMatches(Matches.unitIsEnemyOf(data, player)).size() * 100; // We prefer to not be greeted by a huge army
					score += TerritoryAttachment.get(ter2).getProduction(); // And we like ters we get money from... :)
					
					if (score > highestUnloadingTerScore)
					{
						highestUnloadingTerScore = score;
						unloadingTer = ter2;
						unloadingPort = openPort;
					}
				}
				
				if (unloadingTer == null)
					continue;
				
				if (unfilledTransports > 0 && unloadingPort != ourUnitGroup.get(0).getTerritoryUnitIsIn())
				{
					if (filledTransports < 10) // As long as we have fewer than 10 filled transports //Todo: Remove hard-coded 10
						continue; // Don't go on to unload port until all transports are filled(with units on them)
				}
				
				// Move all units to unloading port
				UnitGroup ships = DUtils.CreateUnitGroupForUnits(ourUnitGroup, ourUnitGroup.get(0).getTerritoryUnitIsIn(), data);
				String error = ships.MoveAsFarTo_CM(unloadingPort, mover);
				if (error != null)
				{
					DUtils.Log(Level.FINER, "    There was an error moving ships[{0}] to unloading port({1}->{2}): {3}", ships, ourUnitGroup.get(0).getTerritoryUnitIsIn(), unloadingPort, error);
					continue;
				}
				
				// Move all land units onto unloading ter
				for (Unit unit : (List<Unit>) Match.getMatches(ourUnitGroup, DUtils.CompMatchAnd(Matches.UnitIsLand)))
				{
					UnitGroup ug = DUtils.CreateUnitGroupForUnit(unit, unloadingPort, data);
					String error2 = ug.MoveAsFarTo_CM(unloadingTer, mover);
					if (error2 != null)
					{
						DUtils.Log(Level.FINER, "    There was an error moving units[{0}] to unloading ter({1}->{2}): {3}", ug, unloadingPort, unloadingTer, error2);
						continue;
					}
				}
			}
		}
	}
	
	private static List<CM_Task> GenerateTasks(final MovePackage pack)
	{
		List<CM_Task> result = new ArrayList<CM_Task>();
		
		final GameData data = pack.Data;
		final PlayerID player = pack.Player;
		final List<Territory> ourCaps = TerritoryAttachment.getAllCapitals(player, data);
		Match<Territory> isLandGrab = new Match<Territory>()
		{
			@Override
			public boolean match(Territory ter)
			{
				if (!DSettings.LoadSettings().TR_enableAttackLandGrab)
					return false;
				if (ter.isWater())
					return false;
				if (TerritoryAttachment.get(ter) == null || TerritoryAttachment.get(ter).isImpassible())
					return false;
				if (data.getRelationshipTracker().isAllied(ter.getOwner(), player))
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
		final List<Territory> capsAndNeighbors = new ArrayList<Territory>();
		for (Territory cap : ourCaps)
			capsAndNeighbors.addAll(DUtils.GetTerritoriesWithinXDistanceOfY(data, cap, 1));
		Match<Territory> isAttack_Stabilize = new Match<Territory>()
		{
			@Override
			public boolean match(Territory ter)
			{
				if (!DSettings.LoadSettings().TR_enableAttackStabalize)
					return false;
				if (ter.isWater())
					return false;
				if (TerritoryAttachment.get(ter) == null || TerritoryAttachment.get(ter).isImpassible())
					return false;
				if (ter.getOwner() != null && data.getRelationshipTracker().isAllied(ter.getOwner(), player))
					return false;
				if (GlobalCenter.IsFFAGame)
				{
					if (!capsAndNeighbors.contains(ter)) // If this ter is neither cap or cap neighbor, it's not a stabalization task (will add stablize tasks for other things later on)
						return false;
				}
				else
				{
					if (!ourCaps.contains(ter)) // If this ter is not our cap, it's not a stabalization task (On non-ffa maps)
						return false;
				}
				
				return true;
			}
		};
		Match<Territory> isAttack_Offensive = new Match<Territory>()
		{
			@Override
			public boolean match(Territory ter)
			{
				if (!DSettings.LoadSettings().TR_enableAttackOffensive)
					return false;
				if (ter.isWater())
					return false;
				if (TerritoryAttachment.get(ter) == null || TerritoryAttachment.get(ter).isImpassible())
					return false;
				if (ter.getOwner() != null && data.getRelationshipTracker().isAllied(ter.getOwner(), player))
					return false;
				
				return true;
			}
		};
		Match<Territory> isAttack_Trade = new Match<Territory>()
		{
			@Override
			public boolean match(Territory ter)
			{
				if (!DSettings.LoadSettings().TR_enableAttackTrade)
					return false;
				if (ter.isWater())
					return false;
				if (TerritoryAttachment.get(ter) == null || TerritoryAttachment.get(ter).isImpassible())
					return false;
				if (ter.getOwner() != null && data.getRelationshipTracker().isAllied(ter.getOwner(), player))
					return false;
				if (ter.getUnits().getMatches(new CompositeMatchAnd<Unit>(Matches.unitHasDefenseThatIsMoreThanOrEqualTo(1), Matches.unitIsEnemyOf(data, player), Matches.UnitIsNotAA)).isEmpty())
					return false; // This is a land-grab
					
				return true;
			}
		};
		List<Territory> tersWeCanAttack = DUtils.GetEnemyTersThatCanBeAttackedByUnitsOwnedBy(data, player);
		DUtils.Log(Level.FINE, "  Beginning task creation loop. tersWeCanAttack: {0}", tersWeCanAttack);
		for (Territory ter : tersWeCanAttack)
		{
			// Hey, just a note to any developers reading this code:
			// If you think it'll help, you can add in special 'combat move' tasks, that in reality just lock down units from attacking elsewhere.
			// For example, you might want a cm task to 'lock-down' units to the cap and other important areas so the territory stays defendable.
			// If you do make these sort of changes, though, please do it carefully, sloppy changes could complicate the code.
			
			// Attack_Trade tasks can exist along with other tasks on the same attack ter (if normal attack isn't worthwhile, check if trade attack is worthwhile)
			if (isAttack_Trade.match(ter))
			{
				List<Unit> possibleAttackers = DUtils.GetUnitsOwnedByPlayerThatCanReach(data, ter, player, Matches.TerritoryIsLandOrWater);
				possibleAttackers = Match.getMatches(possibleAttackers, new CompositeMatchOr<Unit>(Matches.UnitIsLand, Matches.UnitIsAir));
				AggregateResults results = DUtils.GetBattleResults(possibleAttackers, DUtils.ToList(ter.getUnits().getUnits()), ter, data,
							DSettings.LoadSettings().CA_CM_determinesIfTaskCreationsWorthwhileBasedOnTakeoverChance, true);
				if (results.getAttackerWinPercent() > .5F)
				{
					float priority = DUtils.GetCMTaskPriority_Trade(data, player, ter);
					CM_Task task = new CM_Task(data, ter, CM_TaskType.Land_Attack_Trade, priority);
					result.add(task);
					DUtils.Log(Level.FINER, "    Attack_Trade task added. Ter: {0} Priority: {1}", ter.getName(), priority);
				}
			}
			
			if (isLandGrab.match(ter))
			{
				float priority = DUtils.GetCMTaskPriority_LandGrab(data, player, ter);
				CM_Task task = new CM_Task(data, ter, CM_TaskType.Land_LandGrab, priority);
				result.add(task);
				DUtils.Log(Level.FINER, "    Land grab task added. Ter: {0} Priority: {1}", ter.getName(), priority);
			}
			else if (isAttack_Stabilize.match(ter))
			{
				List<Unit> possibleAttackers = DUtils.GetUnitsOwnedByPlayerThatCanReach(data, ter, player, Matches.TerritoryIsLandOrWater);
				possibleAttackers = Match.getMatches(possibleAttackers, new CompositeMatchOr<Unit>(Matches.UnitIsLand, Matches.UnitIsAir));
				AggregateResults results = DUtils.GetBattleResults(possibleAttackers, DUtils.ToList(ter.getUnits().getUnits()), ter, data,
							DSettings.LoadSettings().CA_CM_determinesIfTaskCreationsWorthwhileBasedOnTakeoverChance, true);
				if (results.getAttackerWinPercent() < .25F)
					continue;
				
				float priority = DUtils.GetCMTaskPriority_Stabalization(data, player, ter);
				CM_Task task = new CM_Task(data, ter, CM_TaskType.Land_Attack_Stabilize, priority);
				result.add(task);
				DUtils.Log(Level.FINER, "    Attack_Stabilize task added. Ter: {0} Priority: {1}", ter.getName(), priority);
			}
			else if (isAttack_Offensive.match(ter))
			{
				List<Unit> possibleAttackers = DUtils.GetUnitsOwnedByPlayerThatCanReach(data, ter, player, Matches.TerritoryIsLandOrWater);
				possibleAttackers = Match.getMatches(possibleAttackers, new CompositeMatchOr<Unit>(Matches.UnitIsLand, Matches.UnitIsAir));
				AggregateResults results = DUtils.GetBattleResults(possibleAttackers, DUtils.ToList(ter.getUnits().getUnits()), ter, data,
							DSettings.LoadSettings().CA_CM_determinesIfTaskCreationsWorthwhileBasedOnTakeoverChance, true);
				if (results.getAttackerWinPercent() < .20F)
					continue;
				
				float priority = DUtils.GetCMTaskPriority_Offensive(data, player, ter);
				CM_Task task = new CM_Task(data, ter, CM_TaskType.Land_Attack_Offensive, priority);
				result.add(task);
				DUtils.Log(Level.FINER, "    Attack_Offensive task added. Ter: {0} Priority: {1}", ter.getName(), priority);
			}
		}
		
		return result;
	}
	
	private static boolean considerAndPerformWorthwhileTasks(MovePackage pack, List<CM_Task> tasks)
	{
		@SuppressWarnings("unused")
		GameData data = pack.Data;
		@SuppressWarnings("unused")
		PlayerID player = pack.Player;
		IMoveDelegate mover = pack.Mover;
		
		// We could also just sort the tasks by priority, then go through the list
		CM_Task highestPriorityTask = null;
		float highestTaskPriority = Integer.MIN_VALUE;
		for (CM_Task task : tasks)
		{
			if (task.IsDisqualified())
				continue;
			if (task.IsCompleted())
				continue;
			
			float priority = task.GetPriority();
			if (priority > highestTaskPriority)
			{
				highestPriorityTask = task;
				highestTaskPriority = priority;
			}
		}
		if (highestPriorityTask != null) // If we have a good move left
		{
			highestPriorityTask.CalculateTaskRequirements();
			highestPriorityTask.RecruitUnits();
			if (highestPriorityTask.IsPlannedAttackWorthwhile(tasks))
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
		
		if (highestPriorityTask != null)
			return true;
		else
			return false;
	}
}
