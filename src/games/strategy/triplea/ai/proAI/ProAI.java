package games.strategy.triplea.ai.proAI;

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
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.framework.GameDataUtils;
import games.strategy.net.GUID;
import games.strategy.triplea.ai.proAI.logging.LogUI;
import games.strategy.triplea.ai.proAI.simulate.ProDummyDelegateBridge;
import games.strategy.triplea.ai.proAI.simulate.ProSimulateTurnUtils;
import games.strategy.triplea.ai.proAI.util.LogUtils;
import games.strategy.triplea.ai.proAI.util.ProAttackOptionsUtils;
import games.strategy.triplea.ai.proAI.util.ProBattleUtils;
import games.strategy.triplea.ai.proAI.util.ProMoveUtils;
import games.strategy.triplea.ai.proAI.util.ProPurchaseUtils;
import games.strategy.triplea.ai.proAI.util.ProTerritoryValueUtils;
import games.strategy.triplea.ai.proAI.util.ProTransportUtils;
import games.strategy.triplea.ai.proAI.util.ProUtils;
import games.strategy.triplea.ai.strongAI.StrongAI;
import games.strategy.triplea.delegate.BattleDelegate;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.triplea.delegate.IBattle;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.oddsCalculator.ta.ConcurrentOddsCalculator;
import games.strategy.triplea.oddsCalculator.ta.IOddsCalculator;
import games.strategy.triplea.ui.TripleAFrame;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Pro AI.
 * 
 * @author Ron Murhammer
 * @since 2014
 */
public class ProAI extends StrongAI
{
	private final static Logger s_logger = Logger.getLogger(ProAI.class.getName());
	
	private final static IOddsCalculator s_battleCalculator = new ConcurrentOddsCalculator("ProAI"); // if non-static, then only need 1 for the entire AI instance and must be shutdown when AI is gc'ed.
	
	// Utilities
	private final ProUtils utils;
	private final ProBattleUtils battleUtils;
	private final ProTransportUtils transportUtils;
	private final ProAttackOptionsUtils attackOptionsUtils;
	private final ProMoveUtils moveUtils;
	private final ProTerritoryValueUtils territoryValueUtils;
	private final ProSimulateTurnUtils simulateTurnUtils;
	private final ProPurchaseUtils purchaseUtils;
	
	// Phases
	private final ProCombatMoveAI combatMoveAI;
	private final ProNonCombatMoveAI nonCombatMoveAI;
	private final ProPurchaseAI purchaseAI;
	private final ProRetreatAI retreatAI;
	
	// Data
	private GameData data;
	private Map<Territory, ProAttackTerritoryData> storedCombatMoveMap;
	private Map<Territory, ProAttackTerritoryData> storedFactoryMoveMap;
	private Map<Territory, ProPurchaseTerritory> storedPurchaseTerritories;
	
	public ProAI(final String name, final String type)
	{
		super(name, type);
		utils = new ProUtils(this);
		battleUtils = new ProBattleUtils(this, utils);
		transportUtils = new ProTransportUtils(this, utils);
		purchaseUtils = new ProPurchaseUtils(this);
		attackOptionsUtils = new ProAttackOptionsUtils(this, utils, battleUtils, transportUtils, purchaseUtils);
		moveUtils = new ProMoveUtils(this, utils);
		territoryValueUtils = new ProTerritoryValueUtils(this, utils);
		simulateTurnUtils = new ProSimulateTurnUtils(this, utils, battleUtils, moveUtils);
		combatMoveAI = new ProCombatMoveAI(utils, battleUtils, transportUtils, attackOptionsUtils, moveUtils, territoryValueUtils, purchaseUtils);
		nonCombatMoveAI = new ProNonCombatMoveAI(utils, battleUtils, transportUtils, attackOptionsUtils, moveUtils, territoryValueUtils, purchaseUtils);
		purchaseAI = new ProPurchaseAI(this, utils, battleUtils, transportUtils, attackOptionsUtils, moveUtils, territoryValueUtils, purchaseUtils);
		retreatAI = new ProRetreatAI(this, battleUtils);
		data = null;
		storedCombatMoveMap = null;
		storedPurchaseTerritories = new HashMap<Territory, ProPurchaseTerritory>();
	}
	
	public static void Initialize(final TripleAFrame frame)
	{
		LogUI.initialize(frame); // Must be done first
		LogUtils.log(Level.FINE, "Initialized Hard AI");
	}
	
	public static void ShowSettingsWindow()
	{
		LogUtils.log(Level.FINE, "Showing Hard AI settings window");
		LogUI.showSettingsWindow();
	}
	
	public static Logger getLogger()
	{
		return s_logger;
	}
	
	public static void gameOverClearCache()
	{
		s_battleCalculator.setGameData(null); // is static, set to null so that we don't keep the data around after a game is exited.
		LogUI.clearCachedInstances();
	}
	
	public IOddsCalculator getCalc()
	{
		return s_battleCalculator;
	}
	
	@Override
	public final GameData getGameData()
	{
		if (data != null)
			return data;
		else
			return super.getGameData();
	}
	
	@Override
	public void stopGame()
	{
		super.stopGame(); // absolutely MUST call super.stopGame() first
		s_battleCalculator.cancel(); // cancel any current calcing
	}
	
	@Override
	protected void move(final boolean nonCombat, final IMoveDelegate moveDel, final GameData data, final PlayerID player)
	{
		s_battleCalculator.setGameData(data);
		if (nonCombat)
		{
			nonCombatMoveAI.doNonCombatMove(storedFactoryMoveMap, storedPurchaseTerritories, moveDel, data, player);
			storedFactoryMoveMap = null;
		}
		else
		{
			LogUI.notifyStartOfRound(data.getSequence().getRound(), player.getName());
			if (storedCombatMoveMap == null)
			{
				combatMoveAI.doCombatMove(moveDel, data, player);
			}
			else
			{
				combatMoveAI.doMove(storedCombatMoveMap, moveDel, data, player);
				storedCombatMoveMap = null;
			}
		}
	}
	
	@Override
	protected void purchase(final boolean purchaseForBid, final int PUsToSpend, final IPurchaseDelegate purchaseDelegate, final GameData data, final PlayerID player)
	{
		if (PUsToSpend <= 0)
			return;
		if (purchaseForBid)
		{
			super.purchase(true, PUsToSpend, purchaseDelegate, data, player);
			// purchaseAI.bid(PUsToSpend, purchaseDelegate, data, player);
		}
		else
		{
			LogUtils.log(Level.FINE, "Starting simulation for purchase phase");
			
			// Setup data copy and delegates
			GameData dataCopy;
			try
			{
				data.acquireReadLock();
				dataCopy = GameDataUtils.cloneGameData(data, true);
			} catch (final Throwable t)
			{
				LogUtils.log(Level.WARNING, "Error trying to clone game data for simulating phases", t);
				return;
			} finally
			{
				data.releaseReadLock();
			}
			this.data = dataCopy;
			s_battleCalculator.setGameData(dataCopy);
			final PlayerID playerCopy = dataCopy.getPlayerList().getPlayerID(player.getName());
			final IMoveDelegate moveDel = DelegateFinder.moveDelegate(dataCopy);
			final IDelegateBridge bridge = new ProDummyDelegateBridge(this, playerCopy, dataCopy);
			moveDel.setDelegateBridgeAndPlayer(bridge);
			
			// Determine turn sequence
			final List<GameStep> gameSteps = new ArrayList<GameStep>();
			for (final Iterator<GameStep> it = dataCopy.getSequence().iterator(); it.hasNext();)
			{
				final GameStep gameStep = it.next();
				gameSteps.add(gameStep);
			}
			
			// Simulate the next phases until place/end of turn is reached then use simulated data for purchase
			final String nationName = dataCopy.getSequence().getStep().getName().replace("Purchase", "");
			final int nextStepIndex = dataCopy.getSequence().getStepIndex() + 1;
			final Map<Unit, Territory> unitTerritoryMap = utils.createUnitTerritoryMap(playerCopy);
			for (int i = nextStepIndex; i < gameSteps.size(); i++)
			{
				final GameStep step = gameSteps.get(i);
				dataCopy.getSequence().setRoundAndStep(dataCopy.getSequence().getRound(), step.getDisplayName(), playerCopy);
				final String stepName = step.getName();
				LogUtils.log(Level.FINE, "Simulating phase: " + stepName);
				if (stepName.startsWith(nationName) && stepName.endsWith("NonCombatMove"))
				{
					final Map<Territory, ProAttackTerritoryData> factoryMoveMap = nonCombatMoveAI.doNonCombatMove(null, null, moveDel, dataCopy, playerCopy);
					if (storedFactoryMoveMap == null)
						storedFactoryMoveMap = simulateTurnUtils.transferMoveMap(factoryMoveMap, unitTerritoryMap, dataCopy, data, player);
				}
				else if (stepName.startsWith(nationName) && stepName.endsWith("CombatMove"))
				{
					final Map<Territory, ProAttackTerritoryData> moveMap = combatMoveAI.doCombatMove(moveDel, dataCopy, playerCopy);
					if (storedCombatMoveMap == null)
						storedCombatMoveMap = simulateTurnUtils.transferMoveMap(moveMap, unitTerritoryMap, dataCopy, data, player);
				}
				else if (stepName.startsWith(nationName) && stepName.endsWith("Battle"))
				{
					simulateTurnUtils.simulateBattles(dataCopy, playerCopy, bridge);
				}
				else if (stepName.startsWith(nationName) && (stepName.endsWith("Place") || stepName.endsWith("EndTurn")))
				{
					storedPurchaseTerritories = purchaseAI.purchase(PUsToSpend, purchaseDelegate, dataCopy, player);
					this.data = null;
					break;
				}
			}
		}
	}
	
	@Override
	public void place(final boolean bid, final IAbstractPlaceDelegate placeDelegate, final GameData data, final PlayerID player)
	{
		if (bid)
		{
			super.place(bid, placeDelegate, data, player);
		}
		else
		{
			purchaseAI.place(storedPurchaseTerritories, placeDelegate, data, player);
		}
	}
	
	@Override
	public Territory retreatQuery(final GUID battleID, final boolean submerge, final Territory battleTerritory, final Collection<Territory> possibleTerritories, final String message)
	{
		// Get battle data
		final GameData data = getGameData();
		final PlayerID player = getPlayerID();
		final BattleDelegate delegate = DelegateFinder.battleDelegate(data);
		final IBattle battle = delegate.getBattleTracker().getPendingBattle(battleID);
		
		// If battle is null or amphibious then don't retreat
		if (battle == null || battleTerritory == null || battle.isAmphibious())
			return null;
		
		// If I'm attacker and have more unit strength then don't retreat
		final boolean isAttacker = player.equals(battle.getAttacker());
		final List<Unit> attackers = (List<Unit>) battle.getAttackingUnits();
		final List<Unit> defenders = (List<Unit>) battle.getDefendingUnits();
		final double strengthDifference = battleUtils.estimateStrengthDifference(battleTerritory, attackers, defenders);
		LogUtils.log(Level.FINE, player.getName() + " checking retreat from territory " + battleTerritory + ", attackers=" + attackers.size() + ", defenders=" + defenders.size() + ", submerge="
					+ submerge + ", attacker=" + isAttacker);
		if (isAttacker && strengthDifference > 50)
			return null;
		
		s_battleCalculator.setGameData(getGameData());
		return retreatAI.retreatQuery(battleID, submerge, battleTerritory, possibleTerritories, message);
	}
	
	@Override
	public boolean shouldBomberBomb(final Territory territory)
	{
		return false;
	}
	
}
