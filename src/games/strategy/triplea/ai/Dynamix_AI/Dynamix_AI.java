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
package games.strategy.triplea.ai.Dynamix_AI;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.net.GUID;
import games.strategy.triplea.ai.AbstractAI;
import games.strategy.triplea.ai.Dynamix_AI.Code.DoCombatMove;
import games.strategy.triplea.ai.Dynamix_AI.Code.DoNonCombatMove;
import games.strategy.triplea.ai.Dynamix_AI.Code.Place;
import games.strategy.triplea.ai.Dynamix_AI.Code.Purchase;
import games.strategy.triplea.ai.Dynamix_AI.Code.SelectCasualties;
import games.strategy.triplea.ai.Dynamix_AI.Code.Tech;
import games.strategy.triplea.ai.Dynamix_AI.CommandCenter.CachedCalculationCenter;
import games.strategy.triplea.ai.Dynamix_AI.CommandCenter.CachedInstanceCenter;
import games.strategy.triplea.ai.Dynamix_AI.CommandCenter.FactoryCenter;
import games.strategy.triplea.ai.Dynamix_AI.CommandCenter.GlobalCenter;
import games.strategy.triplea.ai.Dynamix_AI.CommandCenter.KnowledgeCenter;
import games.strategy.triplea.ai.Dynamix_AI.CommandCenter.ReconsiderSignalCenter;
import games.strategy.triplea.ai.Dynamix_AI.CommandCenter.StatusCenter;
import games.strategy.triplea.ai.Dynamix_AI.CommandCenter.StrategyCenter;
import games.strategy.triplea.ai.Dynamix_AI.CommandCenter.TacticalCenter;
import games.strategy.triplea.ai.Dynamix_AI.CommandCenter.ThreatInvalidationCenter;
import games.strategy.triplea.ai.Dynamix_AI.Group.UnitGroup;
import games.strategy.triplea.ai.Dynamix_AI.Others.Battle_RetreatTerCalculator;
import games.strategy.triplea.ai.Dynamix_AI.Others.PhaseType;
import games.strategy.triplea.ai.Dynamix_AI.UI.UI;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.dataObjects.BattleListing;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.delegate.dataObjects.CasualtyList;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.delegate.remote.IBattleDelegate;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.delegate.remote.ITechDelegate;
import games.strategy.triplea.oddsCalculator.ta.AggregateResults;
import games.strategy.triplea.oddsCalculator.ta.BattleResults;
import games.strategy.triplea.oddsCalculator.ta.ConcurrentOddsCalculator;
import games.strategy.triplea.oddsCalculator.ta.IOddsCalculator;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.Match;
import games.strategy.util.Tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author Stephen (Wisconsin)
 *         2010-2011
 */
public class Dynamix_AI extends AbstractAI implements IGamePlayer, ITripleaPlayer
{
	private final static Logger s_logger = Logger.getLogger(Dynamix_AI.class.getName());
	
	private static final IOddsCalculator s_battleCalculator = new ConcurrentOddsCalculator("Dynamix_AI"); // if non-static, then only need 1 for the entire AI instance and must be shutdown when AI is gc'ed.
	
	/**
	 * Some notes on using the Dynamix logger:
	 * 
	 * First, to make the logs easily readable even when there are hundreds of lines, I want every considerable step down in the call stack to mean more log message indentation.
	 * For example, these base logs have no indentation before them, but the base logs in the DoCombatMove class will have two spaces inserted at the start, and the level below that, four spaces.
	 * In this way, when you're reading the log, you can skip over unimportant areas with speed because of the indentation.
	 * 
	 * Just keep these things in mind while adding new logging code.
	 * (P.S. For multiple reasons, it is strongly suggested that you use DUtils.Log instead of writing directly to the logger returned by this method.)
	 */
	public static Logger GetStaticLogger()
	{
		return s_logger;
	}
	
	public Dynamix_AI(final String name, final String type)
	{
		super(name, type);
	}
	
	// These static dynamix AI instances are going to be used by the settings window to let the player change AI goals, aggresiveness, etc.
	private static final List<Dynamix_AI> s_dAIInstances = new ArrayList<Dynamix_AI>();
	
	public static void ClearAIInstancesMemory()
	{
		DUtils.Log(Level.FINE, "Clearing static Dynamix_AI instances.");
		s_dAIInstances.clear();
	}
	
	public static void AddDynamixAIIntoAIInstancesMemory(final Dynamix_AI ai)
	{
		DUtils.Log(Level.FINE, "Adding Dynamix_AI named {0} to static instances.", ai.getName());
		s_dAIInstances.add(ai);
	}
	
	public static List<Dynamix_AI> GetDynamixAIInstancesMemory()
	{
		return s_dAIInstances;
	}
	
	static IOddsCalculator getCalculator()
	{
		return s_battleCalculator;
	}
	
	public IOddsCalculator getCalc()
	{
		return s_battleCalculator;
	}
	
	/**
	 * Only call after we have left or quit a game.
	 */
	public static void gameOverClearCachedGameDataAll()
	{
		CachedInstanceCenter.clearCachedDelegatesAndData();
		UI.clearCachedInstances();
		clearStaticInstances();
	}
	
	/**
	 * Call before starting a game, and after leaving.
	 */
	public static void clearStaticInstances()
	{
		s_dAIInstances.clear();
		s_battleCalculator.setGameData(null); // is static, set to null so that we don't keep the data around after a game is exited.
		GlobalCenter.clearStaticInstances();
		FactoryCenter.ClearStaticInstances();
		KnowledgeCenter.ClearStaticInstances();
		TacticalCenter.ClearStaticInstances();
		StatusCenter.ClearStaticInstances();
		ThreatInvalidationCenter.ClearStaticInstances();
		ReconsiderSignalCenter.ClearStaticInstances();
		StrategyCenter.ClearStaticInstances();
		UnitGroup.ClearBufferedMoves();
		CachedCalculationCenter.clearCachedStaticData();
		UnitGroup.clearCachedInstances();
	}
	
	public static void Initialize(final TripleAFrame frame)
	{
		UI.Initialize(frame); // Must be done first
		DUtils.Log(Level.FINE, "Initializing Dynamix_AI...");
		clearStaticInstances();
		GlobalCenter.Initialize(CachedInstanceCenter.CachedGameData);
		CachedInstanceCenter.CachedBattleTracker = DelegateFinder.battleDelegate(CachedInstanceCenter.CachedGameData).getBattleTracker();
	}
	
	public static void ShowSettingsWindow()
	{
		DUtils.Log(Level.FINE, "Showing Dynamix_AI settings window.");
		UI.ShowSettingsWindow();
	}
	
	/**
	 * Please call this right before an action is displayed to the user.
	 */
	@Override
	public void pause()
	{
		Pause();
	}
	
	private static long s_lastActionDisplayTime = new Date().getTime();
	
	/**
	 * Please call this right before an action is displayed to the user.
	 */
	public static void Pause()
	{
		try
		{
			final long pauseTime = GetTimeTillNextScheduledActionDisplay();
			if (pauseTime == -1)
				return;
			Thread.sleep(pauseTime);
			s_lastActionDisplayTime = new Date().getTime();
		} catch (final InterruptedException ex)
		{
			DUtils.Log(Level.SEVERE, "InterruptedException occured while trying to perform AI pausing. Exception: {0}", ex);
		}
	}
	
	public static long GetTimeTillNextScheduledActionDisplay()
	{
		// If we're not in a phase that has pausing enabled
		if (!DUtils.ToList(DUtils.ToArray(PhaseType.Purchase, PhaseType.Combat_Move, PhaseType.Non_Combat_Move, PhaseType.Place)).contains(GlobalCenter.CurrentPhaseType))
			return -1;
		final long timeSince = new Date().getTime() - s_lastActionDisplayTime;
		long wantedActionLength = 0;
		switch (GlobalCenter.CurrentPhaseType)
		{
			case Purchase:
				wantedActionLength = DSettings.LoadSettings().PurchaseWait_AL;
				break;
			case Combat_Move:
				wantedActionLength = DSettings.LoadSettings().CombatMoveWait_AL;
				break;
			case Non_Combat_Move:
				wantedActionLength = DSettings.LoadSettings().NonCombatMoveWait_AL;
				break;
			case Place:
				wantedActionLength = DSettings.LoadSettings().PlacementWait_AL;
				break;
		}
		int timeTill = (int) (wantedActionLength - timeSince);
		timeTill = Math.max(timeTill, 0);
		return timeTill;
	}
	
	private void NotifyGameRound(final GameData data)
	{
		if (GlobalCenter.GameRound != data.getSequence().getRound())
		{
			GlobalCenter.GameRound = data.getSequence().getRound();
			UI.NotifyStartOfRound(GlobalCenter.GameRound);
			DUtils.Log(Level.FINE, "-----Start of game round notification sent out. Round {0}-----", GlobalCenter.GameRound);
			TacticalCenter.NotifyStartOfRound();
			FactoryCenter.NotifyStartOfRound();
			StatusCenter.NotifyStartOfRound();
			ThreatInvalidationCenter.NotifyStartOfRound();
			ReconsiderSignalCenter.NotifyStartOfRound();
			StrategyCenter.NotifyStartOfRound();
			GlobalCenter.CurrentPlayer = PlayerID.NULL_PLAYERID; // Reset current player to re-enable trigger in NotifyPlayer method
		}
	}
	
	private void NotifyPlayer(final PlayerID player)
	{
		if (GlobalCenter.FirstDynamixPlayer == null)
			GlobalCenter.FirstDynamixPlayer = player;
		if (GlobalCenter.CurrentPlayer != player)
		{
			GlobalCenter.CurrentPlayer = player;
			DUtils.Log(Level.FINE, "-----Start of player's turn notification sent out. Player: {0}-----", player.getName());
			s_battleCalculator.setGameData(getGameData());
			; // Refresh DOddsCalculator game data each time the player changes, to keep it up to date
			GlobalCenter.CurrentPhaseType = PhaseType.Unknown; // Reset current phase type to re-enable trigger in NotifyPhaseType method
		}
	}
	
	private void NotifyPhaseType(final PhaseType phaseType)
	{
		if (GlobalCenter.FirstDynamixPhase == null)
			GlobalCenter.FirstDynamixPhase = phaseType;
		if (GlobalCenter.CurrentPhaseType != phaseType)
		{
			GlobalCenter.CurrentPhaseType = phaseType;
			DUtils.Log(Level.FINE, "-----Start of phase notification sent out. Phase: {0}-----", phaseType);
		}
	}
	
	@Override
	protected void place(final boolean bid, final IAbstractPlaceDelegate placeDelegate, final GameData data, final PlayerID player)
	{
		NotifyGameRound(data);
		NotifyPlayer(player);
		NotifyPhaseType(PhaseType.Place);
		Place.place(this, bid, placeDelegate, data, player);
		if (bid) // We don't want info from the bid phase spilling over into first real round
			FactoryCenter.NotifyStartOfRound(); // So clear data
	}
	
	int m_moveLastType = -1;
	
	@Override
	protected void move(final boolean nonCombat, final IMoveDelegate moveDel, final GameData data, final PlayerID player)
	{
		UnitGroup.movesCount = 0; // Dynamix is able to undo it's moves, via UnitGroup, but we need to reset move count each phase for it to work
		if (!nonCombat)
		{
			NotifyGameRound(data);
			NotifyPlayer(player);
			NotifyPhaseType(PhaseType.Combat_Move);
			ThreatInvalidationCenter.get(data, player).ClearInvalidatedThreats();
			DoCombatMove.doCombatMove(this, data, moveDel, player);
			TacticalCenter.get(data, player).AllDelegateUnitGroups.clear();
			TacticalCenter.get(data, player).ClearFrozenUnits();
			TacticalCenter.get(data, player).ClearStartOfTurnUnitLocations();
			pause();
		}
		else
		{
			NotifyGameRound(data);
			NotifyPlayer(player);
			NotifyPhaseType(PhaseType.Non_Combat_Move);
			ThreatInvalidationCenter.get(data, player).ClearInvalidatedThreats();
			DoNonCombatMove.doNonCombatMove(this, data, moveDel, player);
			TacticalCenter.get(data, player).AllDelegateUnitGroups.clear();
			TacticalCenter.get(data, player).ClearFrozenUnits();
			TacticalCenter.get(data, player).ClearStartOfTurnUnitLocations();
			pause();
		}
		if (m_moveLastType == -1)
			m_moveLastType = 1;
		else if (m_moveLastType == 0)
			m_moveLastType = 1;
		else
			// Put finalize code here that should run after combat and non-combat move have both completed
			m_moveLastType = 0;
	}
	
	@Override
	protected void tech(final ITechDelegate techDelegate, final GameData data, final PlayerID player)
	{
		NotifyGameRound(data);
		NotifyPlayer(player);
		NotifyPhaseType(PhaseType.Tech);
		Tech.tech(this, techDelegate, data, player);
	}
	
	@Override
	protected void purchase(final boolean purchaseForBid, final int PUsToSpend, final IPurchaseDelegate purchaser, final GameData data, final PlayerID player)
	{
		NotifyGameRound(data);
		NotifyPlayer(player);
		NotifyPhaseType(PhaseType.Purchase);
		Purchase.purchase(this, purchaseForBid, PUsToSpend, purchaser, data, player);
	}
	
	@Override
	protected void battle(final IBattleDelegate battleDelegate, final GameData data, final PlayerID player)
	{
		NotifyGameRound(data);
		NotifyPlayer(player);
		NotifyPhaseType(PhaseType.Battle);
		// Generally all AI's will follow the same logic: loop until all battles are fought
		// Rather than trying to analyze battles to figure out which must be fought before others,
		// as in the case of a naval battle preceding an amphibious attack,
		// keep trying to fight every battle until all battles are resolved
		while (true)
		{
			final BattleListing listing = battleDelegate.getBattles();
			if (listing.getBattles().isEmpty())
				break;
			for (final Entry<BattleType, Collection<Territory>> entry : listing.getBattles().entrySet())
			{
				for (final Territory current : entry.getValue())
				{
					setBattleInfo(current);
					@SuppressWarnings("unused")
					final String error = battleDelegate.fightBattle(current, entry.getKey().isBombingRun(), entry.getKey());
					// if (error != null)
					// s_logger.fine(error);
				}
			}
			setBattleInfo(null);
		}
	}
	
	private void setBattleInfo(final Territory bTerr)
	{
		m_battleTer = bTerr;
	}
	
	private Territory getBattleTerritory()
	{
		return m_battleTer;
	}
	
	Territory m_battleTer = null;
	
	/*public Collection<Unit> scrambleQuery(final GUID battleID, final Collection<Territory> possibleTerritories, final String message, final PlayerID player)
	{
		return null;
	}*/
	
	@Override
	public HashMap<Territory, Collection<Unit>> scrambleUnitsQuery(final Territory scrambleTo, final Map<Territory, Tuple<Collection<Unit>, Collection<Unit>>> possibleScramblers)
	{
		return null;
	}
	
	@Override
	public Territory retreatQuery(final GUID battleID, final boolean submerge, final Territory battleTerritory, final Collection<Territory> possibleTerritories, final String message)
	{
		DUtils.Log(Level.FINE, "Retreat query starting. Battle Ter: {0} Possible retreat locations: {1}", getBattleTerritory(), possibleTerritories);
		final GameData data = getPlayerBridge().getGameData();
		final PlayerID player = GlobalCenter.CurrentPlayer;
		final Territory battleTer = getBattleTerritory();
		// BattleTer will be null if we're defending and TripleA calls this method to ask if we want to retreat(submerge) our subs when being attacked
		// PossibleTerritories will be empty if subs move in our sub ter, and our sub is 'attacking'
		if (battleTer == null || possibleTerritories.isEmpty())
			return null; // Don't submerge
		final List<Unit> attackers = battleTer.getUnits().getMatches(Matches.unitIsOwnedBy(getPlayerID()));
		final List<Unit> defenders = battleTer.getUnits().getMatches(Matches.unitIsEnemyOf(data, getPlayerID()));
		final AggregateResults simulatedAttack = DUtils.GetBattleResults(attackers, defenders, battleTer, data, DSettings.LoadSettings().CA_Retreat_determinesIfAIShouldRetreat, true);
		float chanceNeededToContinue = .6F;
		if (TacticalCenter.get(data, getPlayerID()).BattleRetreatChanceAssignments.containsKey(battleTer))
		{
			DUtils.Log(Level.FINER, "Found specific battle retreat chance assignment for territory '{0}'. Retreat Chance: {1}", battleTer,
						TacticalCenter.get(data, player).BattleRetreatChanceAssignments.get(battleTer));
			chanceNeededToContinue = TacticalCenter.get(data, getPlayerID()).BattleRetreatChanceAssignments.get(battleTer);
			if (simulatedAttack.getAttackerWinPercent() < chanceNeededToContinue)
			{
				// Calculate best retreat ter and retreat to it
				final Territory retreatTer = Battle_RetreatTerCalculator.CalculateBestRetreatTer(data, player, new ArrayList<Territory>(possibleTerritories), battleTer);
				return retreatTer;
			}
		}
		else
		// Must be attack_trade type
		{
			final List<Unit> responseAttackers = DUtils.DetermineResponseAttackers(data, GlobalCenter.CurrentPlayer, battleTer, simulatedAttack);
			final List<Unit> responseDefenders = Match.getMatches(simulatedAttack.GetAverageAttackingUnitsRemaining(), Matches.UnitIsNotAir); // Air can't defend ter because they need to land
			final AggregateResults responseResults = DUtils.GetBattleResults(responseAttackers, responseDefenders, battleTer, data, DSettings.LoadSettings().CA_Retreat_determinesIfAIShouldRetreat,
						true);
			final int tradeScore = DUtils.GetTaskTradeScore(data, battleTer, attackers, defenders, simulatedAttack, responseAttackers, responseDefenders, responseResults);
			DUtils.Log(Level.FINER, "Attack_Trade battle assumed. Score: {0} Required: {1}", tradeScore, DSettings.LoadSettings().TR_attackTrade_totalTradeScoreRequired);
			if (tradeScore < DSettings.LoadSettings().TR_attackTrade_totalTradeScoreRequired)
			{
				// Calculate best retreat ter and retreat to it
				final Territory retreatTer = Battle_RetreatTerCalculator.CalculateBestRetreatTer(data, player, new ArrayList<Territory>(possibleTerritories), battleTer);
				return retreatTer;
			}
			final int leftoverLandUnitsWanted = 2; // TODO: Figure out the number determined in CM_Task
			int timesWeReachLeftoverLUnitsGoal = 0;
			for (final BattleResults result : simulatedAttack.getResults())
			{
				if (Match.getMatches(result.getRemainingAttackingUnits(), Matches.UnitIsLand).size() >= leftoverLandUnitsWanted)
					timesWeReachLeftoverLUnitsGoal++;
			}
			final float certaintyOfReachingLUnitsCount = (float) timesWeReachLeftoverLUnitsGoal / (float) simulatedAttack.getResults().size();
			if (certaintyOfReachingLUnitsCount < DUtils.ToFloat(DSettings.LoadSettings().TR_attackTrade_certaintyOfReachingDesiredNumberOfLeftoverLandUnitsRequired))
			{
				// Calculate best retreat ter and retreat to it
				final Territory retreatTer = Battle_RetreatTerCalculator.CalculateBestRetreatTer(data, player, new ArrayList<Territory>(possibleTerritories), battleTer);
				return retreatTer;
			}
		}
		return null;
	}
	
	@Override
	public boolean confirmMoveInFaceOfAA(final Collection<Territory> aaFiringTerritories)
	{
		// Hmmm... Atm, true and false are both bad. With true, the AI may destroy aircraft unnecesarily, with false, the AI may attack a ter thinking it has air support, which never comes.
		return true;
	}
	
	@Override
	public Territory selectTerritoryForAirToLand(final Collection<Territory> candidates, final Territory currentTerritory, final String unitMessage)
	{
		return candidates.iterator().next();
	}
	
	@Override
	public Collection<Unit> getNumberOfFightersToMoveToNewCarrier(final Collection<Unit> fightersThatCanBeMoved, final Territory from)
	{
		final List<Unit> result = new ArrayList<Unit>();
		for (final Unit fighter : fightersThatCanBeMoved)
		{
			result.add(fighter);
		}
		return result;
	}
	
	@Override
	public boolean shouldBomberBomb(final Territory territory)
	{
		final List<Unit> nonBomberAttackingUnits = Match.getMatches(territory.getUnits().getUnits(),
					new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(getPlayerID()), Matches.UnitIsNotStrategicBomber));
		if (nonBomberAttackingUnits.isEmpty())
			return true;
		else
			return false;
	}
	
	@Override
	public Unit whatShouldBomberBomb(final Territory territory, final Collection<Unit> potentialTargets, final Collection<Unit> bombers)
	{
		if (potentialTargets == null || potentialTargets.isEmpty())
			return null;
		final Collection<Unit> factories = Match.getMatches(potentialTargets, Matches.UnitCanProduceUnitsAndCanBeDamaged);
		if (factories.isEmpty())
			return potentialTargets.iterator().next();
		return factories.iterator().next();
	}
	
	@Override
	public int[] selectFixedDice(final int numRolls, final int hitAt, final boolean hitOnlyIfEquals, final String message, final int diceSides)
	{
		final int[] dice = new int[numRolls];
		for (int i = 0; i < numRolls; i++)
		{
			dice[i] = (int) Math.ceil(Math.random() * diceSides);
		}
		return dice;
	}
	
	@Override
	public CasualtyDetails selectCasualties(final Collection<Unit> selectFrom, final Map<Unit, Collection<Unit>> dependents, final int count, final String message, final DiceRoll dice,
				final PlayerID hit, final CasualtyList defaultCasualties, final GUID battleID, final Territory battlesite, final boolean allowMultipleHitsPerUnit)
	{
		DUtils.Log(Level.FINE, "Select casualties method called. Message: {0}", message);
		return SelectCasualties.selectCasualties(this, getGameData(), selectFrom, dependents, count, message, dice, hit, defaultCasualties, battleID, allowMultipleHitsPerUnit);
	}
	
	@Override
	public void reportError(final String error)
	{
		DUtils.Log(Level.FINE, "Error message reported: {0}", error);
		if (error.equals("Wrong number of casualties selected") || error.equals("Cannot remove enough units of those types"))
		{
			SelectCasualties.NotifyCasualtySelectionError(error);
		}
	}
	
	@Override
	public Collection<Unit> selectUnitsQuery(final Territory current, final Collection<Unit> possible, final String message)
	{
		return null;
	}
}
