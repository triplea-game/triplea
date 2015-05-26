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
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.framework.GameDataUtils;
import games.strategy.net.GUID;
import games.strategy.triplea.Constants;
import games.strategy.triplea.ai.AbstractAI;
import games.strategy.triplea.ai.Dynamix_AI.DUtils;
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
import games.strategy.triplea.ai.strongAI.SUtils;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.BattleCalculator;
import games.strategy.triplea.delegate.BattleDelegate;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.IBattle;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.delegate.dataObjects.CasualtyList;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.delegate.remote.ITechDelegate;
import games.strategy.triplea.oddsCalculator.ta.ConcurrentOddsCalculator;
import games.strategy.triplea.oddsCalculator.ta.IOddsCalculator;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.util.Match;
import games.strategy.util.Tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
public class ProAI extends AbstractAI
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
	private final ProScrambleAI scrambleAI;
	
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
		territoryValueUtils = new ProTerritoryValueUtils(this, utils, battleUtils);
		simulateTurnUtils = new ProSimulateTurnUtils(this, utils, battleUtils, moveUtils);
		combatMoveAI = new ProCombatMoveAI(utils, battleUtils, transportUtils, attackOptionsUtils, moveUtils, territoryValueUtils, purchaseUtils);
		nonCombatMoveAI = new ProNonCombatMoveAI(utils, battleUtils, transportUtils, attackOptionsUtils, moveUtils, territoryValueUtils, purchaseUtils);
		purchaseAI = new ProPurchaseAI(this, utils, battleUtils, transportUtils, attackOptionsUtils, moveUtils, territoryValueUtils, purchaseUtils);
		retreatAI = new ProRetreatAI(this, battleUtils);
		scrambleAI = new ProScrambleAI(this, battleUtils, attackOptionsUtils);
		data = null;
		storedCombatMoveMap = null;
		storedPurchaseTerritories = null;
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
		final long start = System.currentTimeMillis();
		BattleCalculator.clearOOLCache();
		LogUI.notifyStartOfRound(data.getSequence().getRound(), player.getName());
		s_battleCalculator.setGameData(data);
		if (nonCombat)
		{
			nonCombatMoveAI.doNonCombatMove(storedFactoryMoveMap, storedPurchaseTerritories, moveDel, data, player, false);
			storedFactoryMoveMap = null;
		}
		else
		{
			if (storedCombatMoveMap == null)
			{
				combatMoveAI.doCombatMove(moveDel, data, player, false);
			}
			else
			{
				combatMoveAI.doMove(storedCombatMoveMap, moveDel, data, player, false);
				storedCombatMoveMap = null;
			}
		}
		LogUtils.log(Level.FINE, player.getName() + " time for nonCombat=" + nonCombat + " time=" + (System.currentTimeMillis() - start));
	}
	
	@Override
	protected void purchase(final boolean purchaseForBid, int PUsToSpend, final IPurchaseDelegate purchaseDelegate, final GameData data, final PlayerID player)
	{
		final long start = System.currentTimeMillis();
		BattleCalculator.clearOOLCache();
		LogUI.notifyStartOfRound(data.getSequence().getRound(), player.getName());
		if (PUsToSpend <= 0)
			return;
		if (purchaseForBid)
		{
			purchaseAI.bid(PUsToSpend, purchaseDelegate, data, player);
		}
		else
		{
			// Repair factories
			PUsToSpend = purchaseAI.repair(PUsToSpend, purchaseDelegate, data, player);
			
			// Check if any place territories exist
			final Map<Territory, ProPurchaseTerritory> purchaseTerritories = purchaseUtils.findPurchaseTerritories(player);
			if (purchaseTerritories.isEmpty())
			{
				LogUtils.log(Level.FINE, "No possible place territories owned so exiting purchase logic");
				return;
			}
			
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
			final int nextStepIndex = dataCopy.getSequence().getStepIndex() + 1;
			final Map<Unit, Territory> unitTerritoryMap = utils.createUnitTerritoryMap(playerCopy);
			for (int i = nextStepIndex; i < gameSteps.size(); i++)
			{
				final GameStep step = gameSteps.get(i);
				if (!playerCopy.equals(step.getPlayerID()))
				{
					continue;
				}
				dataCopy.getSequence().setRoundAndStep(dataCopy.getSequence().getRound(), step.getDisplayName(), step.getPlayerID());
				final String stepName = step.getName();
				LogUtils.log(Level.FINE, "Simulating phase: " + stepName);
				if (stepName.endsWith("NonCombatMove"))
				{
					final Map<Territory, ProAttackTerritoryData> factoryMoveMap = nonCombatMoveAI.doNonCombatMove(null, null, moveDel, dataCopy, playerCopy, true);
					if (storedFactoryMoveMap == null)
						storedFactoryMoveMap = simulateTurnUtils.transferMoveMap(factoryMoveMap, unitTerritoryMap, dataCopy, data, player);
				}
				else if (stepName.endsWith("CombatMove") && !stepName.endsWith("AirborneCombatMove"))
				{
					final Map<Territory, ProAttackTerritoryData> moveMap = combatMoveAI.doCombatMove(moveDel, dataCopy, playerCopy, true);
					if (storedCombatMoveMap == null)
						storedCombatMoveMap = simulateTurnUtils.transferMoveMap(moveMap, unitTerritoryMap, dataCopy, data, player);
				}
				else if (stepName.endsWith("Battle"))
				{
					simulateTurnUtils.simulateBattles(dataCopy, playerCopy, bridge);
				}
				else if (stepName.endsWith("Place") || stepName.endsWith("EndTurn"))
				{
					storedPurchaseTerritories = purchaseAI.purchase(PUsToSpend, purchaseDelegate, dataCopy, data, player);
					this.data = null;
					break;
				}
			}
		}
		LogUtils.log(Level.FINE, player.getName() + " time for purchase=" + (System.currentTimeMillis() - start));
	}
	
	@Override
	protected void place(final boolean bid, final IAbstractPlaceDelegate placeDelegate, final GameData data, final PlayerID player)
	{
		final long start = System.currentTimeMillis();
		BattleCalculator.clearOOLCache();
		LogUI.notifyStartOfRound(data.getSequence().getRound(), player.getName());
		if (bid)
		{
			purchaseAI.bidPlace(storedPurchaseTerritories, placeDelegate, data, player);
		}
		else
		{
			purchaseAI.place(storedPurchaseTerritories, placeDelegate, data, player);
			storedPurchaseTerritories = null;
		}
		LogUtils.log(Level.FINE, player.getName() + " time for place=" + (System.currentTimeMillis() - start));
	}
	
	@Override
	protected void tech(final ITechDelegate techDelegate, final GameData data, final PlayerID player)
	{
		if (!games.strategy.triplea.Properties.getWW2V3TechModel(data))
			return;
		long last, now;
		last = System.currentTimeMillis();
		s_logger.fine("Doing Tech ");
		final Territory myCapitol = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
		final float eStrength = SUtils.getStrengthOfPotentialAttackers(myCapitol, data, player, false, true, null);
		float myStrength = SUtils.strength(myCapitol.getUnits().getUnits(), false, false, false);
		final List<Territory> areaStrength = SUtils.getNeighboringLandTerritories(data, player, myCapitol);
		for (final Territory areaTerr : areaStrength)
			myStrength += SUtils.strength(areaTerr.getUnits().getUnits(), false, false, false) * 0.75F;
		final boolean capDanger = myStrength < (eStrength * 1.25F + 3.0F);
		
		final Resource pus = data.getResourceList().getResource(Constants.PUS);
		final int PUs = player.getResources().getQuantity(pus);
		final Resource techtokens = data.getResourceList().getResource(Constants.TECH_TOKENS);
		final int TechTokens = player.getResources().getQuantity(techtokens);
		int TokensToBuy = 0;
		if (!capDanger && TechTokens < 3 && PUs > Math.random() * 160)
			TokensToBuy = 1;
		if (TechTokens > 0 || TokensToBuy > 0)
		{
			final List<TechnologyFrontier> cats = TechAdvance.getPlayerTechCategories(data, player);
			// retaining 65% chance of choosing land advances using basic ww2v3 model.
			if (data.getTechnologyFrontier().isEmpty())
			{
				if (Math.random() > 0.35)
					techDelegate.rollTech(TechTokens + TokensToBuy, cats.get(1), TokensToBuy, null);
				else
					techDelegate.rollTech(TechTokens + TokensToBuy, cats.get(0), TokensToBuy, null);
			}
			else
			{
				final int rand = (int) (Math.random() * cats.size());
				techDelegate.rollTech(TechTokens + TokensToBuy, cats.get(rand), TokensToBuy, null);
			}
		}
		now = System.currentTimeMillis();
		s_logger.finest("Time Taken " + (now - last));
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
		
		// If I'm attacker, have more unit strength, and isn't land battle with only air left then don't retreat
		final boolean isAttacker = player.equals(battle.getAttacker());
		final List<Unit> attackers = (List<Unit>) battle.getAttackingUnits();
		final List<Unit> defenders = (List<Unit>) battle.getDefendingUnits();
		final double strengthDifference = battleUtils.estimateStrengthDifference(battleTerritory, attackers, defenders);
		LogUtils.log(Level.FINE, player.getName() + " checking retreat from territory " + battleTerritory + ", attackers=" + attackers.size() + ", defenders=" + defenders.size() + ", submerge="
					+ submerge + ", attacker=" + isAttacker);
		if (isAttacker && strengthDifference > 50 && (battleTerritory.isWater() || Match.someMatch(attackers, Matches.UnitIsLand)))
			return null;
		
		s_battleCalculator.setGameData(getGameData());
		return retreatAI.retreatQuery(battleID, submerge, battleTerritory, possibleTerritories, message);
	}
	
	@Override
	public boolean shouldBomberBomb(final Territory territory)
	{
		return false;
	}
	
	@Override
	public Collection<Unit> getNumberOfFightersToMoveToNewCarrier(final Collection<Unit> fightersThatCanBeMoved, final Territory from)
	{
		final List<Unit> rVal = new ArrayList<Unit>();
		// for (final Unit fighter : fightersThatCanBeMoved)
		// rVal.add(fighter);
		return rVal;
	}
	
	@Override
	public CasualtyDetails selectCasualties(final Collection<Unit> selectFrom, final Map<Unit, Collection<Unit>> dependents, final int count, final String message, final DiceRoll dice,
				final PlayerID hit, final Collection<Unit> friendlyUnits, final PlayerID enemyPlayer, final Collection<Unit> enemyUnits, final boolean amphibious,
				final Collection<Unit> amphibiousLandAttackers, final CasualtyList defaultCasualties, final GUID battleID, final Territory battlesite, final boolean allowMultipleHitsPerUnit)
	{
		if (defaultCasualties.size() != count)
			throw new IllegalStateException("Select Casualties showing different numbers for number of hits to take vs total size of default casualty selections");
		if (defaultCasualties.getKilled().size() <= 0)
			return new CasualtyDetails(defaultCasualties, false);
		
		// Consider unit cost
		final CasualtyDetails myCasualties = new CasualtyDetails(false);
		myCasualties.addToDamaged(defaultCasualties.getDamaged());
		final List<Unit> selectFromSorted = new ArrayList<Unit>(selectFrom);
		if (enemyUnits.isEmpty())
		{
			Collections.sort(selectFromSorted, ProPurchaseUtils.getCostComparator());
		}
		else
		{
			// Get battle data
			final GameData data = getGameData();
			final PlayerID player = getPlayerID();
			final BattleDelegate delegate = DelegateFinder.battleDelegate(data);
			final IBattle battle = delegate.getBattleTracker().getPendingBattle(battleID);
			
			// If defender and less strength then don't consider unit cost as just trying to survive
			boolean needToCheck = true;
			final boolean isAttacker = player.equals(battle.getAttacker());
			if (!isAttacker)
			{
				final List<Unit> attackers = (List<Unit>) battle.getAttackingUnits();
				final List<Unit> defenders = (List<Unit>) battle.getDefendingUnits();
				final double strengthDifference = battleUtils.estimateStrengthDifference(battlesite, attackers, defenders);
				if (strengthDifference > 50)
					needToCheck = false;
			}
			
			// Use bubble sort to save expensive units
			while (needToCheck)
			{
				needToCheck = false;
				for (int i = 0; i < selectFromSorted.size() - 1; i++)
				{
					final Unit unit1 = selectFromSorted.get(i);
					final Unit unit2 = selectFromSorted.get(i + 1);
					final double unitCost1 = ProPurchaseUtils.getCost(unit1.getType(), unit1.getOwner(), unit1.getData());
					final double unitCost2 = ProPurchaseUtils.getCost(unit2.getType(), unit2.getOwner(), unit2.getData());
					if (unitCost1 > 1.5 * unitCost2)
					{
						selectFromSorted.set(i, unit2);
						selectFromSorted.set(i + 1, unit1);
						needToCheck = true;
					}
				}
			}
		}
		
		// Interleave carriers and planes
		final List<Unit> interleavedTargetList = new ArrayList<Unit>(DUtils.InterleaveUnits_CarriersAndPlanes(selectFromSorted, 0));
		for (int i = 0; i < defaultCasualties.getKilled().size(); ++i)
			myCasualties.addToKilled(interleavedTargetList.get(i));
		
		if (count != myCasualties.size())
			throw new IllegalStateException("AI chose wrong number of casualties");
		
		return myCasualties;
	}
	
	/**
	 * Ask the player which units, if any, they want to scramble to defend against the attacker.
	 * 
	 * @param scrambleTo
	 *            - the territory we are scrambling to defend in, where the units will end up if scrambled
	 * @param possibleScramblers
	 *            - possible units which we could scramble, with where they are from and how many allowed from that location
	 * @return a list of units to scramble mapped to where they are coming from
	 */
	@Override
	public HashMap<Territory, Collection<Unit>> scrambleUnitsQuery(final Territory scrambleTo, final Map<Territory, Tuple<Collection<Unit>, Collection<Unit>>> possibleScramblers)
	{
		// Get battle data
		final GameData data = getGameData();
		final PlayerID player = getPlayerID();
		final BattleDelegate delegate = DelegateFinder.battleDelegate(data);
		final IBattle battle = delegate.getBattleTracker().getPendingBattle(scrambleTo, false, BattleType.NORMAL);
		
		// If battle is null then don't scramble
		if (battle == null)
			return null;
		
		final List<Unit> attackers = (List<Unit>) battle.getAttackingUnits();
		final List<Unit> defenders = (List<Unit>) battle.getDefendingUnits();
		LogUtils.log(Level.FINE, player.getName() + " checking scramble to " + scrambleTo + ", attackers=" + attackers.size() + ", defenders=" + defenders.size() + ", possibleScramblers="
					+ possibleScramblers);
		
		s_battleCalculator.setGameData(getGameData());
		
		return scrambleAI.scrambleUnitsQuery(scrambleTo, possibleScramblers);
	}
}
