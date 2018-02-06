package games.strategy.triplea.ai.proAI;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.framework.GameDataUtils;
import games.strategy.net.GUID;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ai.AbstractAi;
import games.strategy.triplea.ai.proAI.data.ProBattleResult;
import games.strategy.triplea.ai.proAI.data.ProPurchaseTerritory;
import games.strategy.triplea.ai.proAI.data.ProTerritory;
import games.strategy.triplea.ai.proAI.logging.ProLogUi;
import games.strategy.triplea.ai.proAI.logging.ProLogger;
import games.strategy.triplea.ai.proAI.simulate.ProDummyDelegateBridge;
import games.strategy.triplea.ai.proAI.simulate.ProSimulateTurnUtils;
import games.strategy.triplea.ai.proAI.util.ProBattleUtils;
import games.strategy.triplea.ai.proAI.util.ProMatches;
import games.strategy.triplea.ai.proAI.util.ProOddsCalculator;
import games.strategy.triplea.ai.proAI.util.ProPurchaseUtils;
import games.strategy.triplea.ai.proAI.util.ProTransportUtils;
import games.strategy.triplea.attachments.PoliticalActionAttachment;
import games.strategy.triplea.delegate.BattleCalculator;
import games.strategy.triplea.delegate.BattleDelegate;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.IBattle;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.PoliticsDelegate;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.delegate.dataObjects.CasualtyList;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.delegate.remote.ITechDelegate;
import games.strategy.triplea.oddsCalculator.ta.IOddsCalculator;
import games.strategy.triplea.oddsCalculator.ta.OddsCalculator;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.util.CollectionUtils;
import games.strategy.util.Tuple;

/**
 * Pro AI.
 */
public class ProAi extends AbstractAi {

  private static final IOddsCalculator concurrentCalc = new OddsCalculator(null);
  protected ProOddsCalculator calc;

  // Phases
  private final ProCombatMoveAi combatMoveAi;
  private final ProNonCombatMoveAi nonCombatMoveAi;
  private final ProPurchaseAi purchaseAi;
  private final ProRetreatAi retreatAi;
  private final ProScrambleAi scrambleAi;
  private final ProPoliticsAi politicsAi;

  // Data shared across phases
  private Map<Territory, ProTerritory> storedCombatMoveMap;
  private Map<Territory, ProTerritory> storedFactoryMoveMap;
  private Map<Territory, ProPurchaseTerritory> storedPurchaseTerritories;
  private List<PoliticalActionAttachment> storedPoliticalActions;
  private List<Territory> storedStrafingTerritories;

  public ProAi(final String name, final String type) {
    super(name, type);
    initializeCalc();
    combatMoveAi = new ProCombatMoveAi(this);
    nonCombatMoveAi = new ProNonCombatMoveAi(this);
    purchaseAi = new ProPurchaseAi(this);
    retreatAi = new ProRetreatAi(this);
    scrambleAi = new ProScrambleAi(this);
    politicsAi = new ProPoliticsAi(this);
    storedCombatMoveMap = null;
    storedFactoryMoveMap = null;
    storedPurchaseTerritories = null;
    storedPoliticalActions = null;
    storedStrafingTerritories = new ArrayList<>();
  }

  protected void initializeCalc() {
    calc = new ProOddsCalculator(concurrentCalc);
  }

  public ProOddsCalculator getCalc() {
    return calc;
  }

  public static void initialize(final TripleAFrame frame) {
    ProLogUi.initialize(frame);
    ProLogger.info("Initialized Hard AI");
  }

  public static void showSettingsWindow() {
    ProLogger.info("Showing Hard AI settings window");
    ProLogUi.showSettingsWindow();
  }

  public static void gameOverClearCache() {
    // Are static, clear so that we don't keep the data around after a game is exited
    concurrentCalc.setGameData(null);
    ProLogUi.clearCachedInstances();
  }

  @Override
  public void stopGame() {
    super.stopGame(); // absolutely MUST call super.stopGame() first
    calc.cancelCalcs();
  }

  private void initializeData() {
    ProData.initialize(this);
  }

  public void setStoredStrafingTerritories(final List<Territory> strafingTerritories) {
    storedStrafingTerritories = strafingTerritories;
  }

  @Override
  protected void move(final boolean nonCombat, final IMoveDelegate moveDel, final GameData data,
      final PlayerID player) {
    final long start = System.currentTimeMillis();
    BattleCalculator.clearOolCache();
    ProLogUi.notifyStartOfRound(data.getSequence().getRound(), player.getName());
    initializeData();
    calc.setData(data);
    if (nonCombat) {
      nonCombatMoveAi.doNonCombatMove(storedFactoryMoveMap, storedPurchaseTerritories, moveDel);
      storedFactoryMoveMap = null;
    } else {
      if (storedCombatMoveMap == null) {
        combatMoveAi.doCombatMove(moveDel);
      } else {
        combatMoveAi.doMove(storedCombatMoveMap, moveDel, data, player);
        storedCombatMoveMap = null;
      }
    }
    ProLogger
        .info(player.getName() + " time for nonCombat=" + nonCombat + " time=" + (System.currentTimeMillis() - start));
  }

  @Override
  protected void purchase(final boolean purchaseForBid, final int pusToSpend, final IPurchaseDelegate purchaseDelegate,
      final GameData data, final PlayerID player) {
    final long start = System.currentTimeMillis();
    BattleCalculator.clearOolCache();
    ProLogUi.notifyStartOfRound(data.getSequence().getRound(), player.getName());
    initializeData();
    if (pusToSpend <= 0) {
      return;
    }
    if (purchaseForBid) {
      calc.setData(data);
      storedPurchaseTerritories = purchaseAi.bid(pusToSpend, purchaseDelegate, data);
    } else {

      // Repair factories
      purchaseAi.repair(pusToSpend, purchaseDelegate, data, player);

      // Check if any place territories exist
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories = ProPurchaseUtils.findPurchaseTerritories(player);
      final List<Territory> possibleFactoryTerritories = CollectionUtils.getMatches(data.getMap().getTerritories(),
          ProMatches.territoryHasNoInfraFactoryAndIsNotConqueredOwnedLand(player, data));
      if (purchaseTerritories.isEmpty() && possibleFactoryTerritories.isEmpty()) {
        ProLogger.info("No possible place or factory territories owned so exiting purchase logic");
        return;
      }
      ProLogger.info("Starting simulation for purchase phase");

      // Setup data copy and delegates
      GameData dataCopy;
      try {
        data.acquireReadLock();
        dataCopy = GameDataUtils.cloneGameData(data, true);
      } catch (final Throwable t) {
        ProLogger.log(Level.WARNING, "Error trying to clone game data for simulating phases", t);
        return;
      } finally {
        data.releaseReadLock();
      }
      calc.setData(dataCopy);
      final PlayerID playerCopy = dataCopy.getPlayerList().getPlayerId(player.getName());
      final IMoveDelegate moveDel = DelegateFinder.moveDelegate(dataCopy);
      final IDelegateBridge bridge = new ProDummyDelegateBridge(this, playerCopy, dataCopy);
      moveDel.setDelegateBridgeAndPlayer(bridge);

      // Determine turn sequence
      final List<GameStep> gameSteps = new ArrayList<>();
      for (final GameStep gameStep : dataCopy.getSequence()) {
        gameSteps.add(gameStep);
      }

      // Simulate the next phases until place/end of turn is reached then use simulated data for purchase
      final int nextStepIndex = dataCopy.getSequence().getStepIndex() + 1;
      for (int i = nextStepIndex; i < gameSteps.size(); i++) {
        final GameStep step = gameSteps.get(i);
        if (!playerCopy.equals(step.getPlayerId())) {
          continue;
        }
        dataCopy.getSequence().setRoundAndStep(dataCopy.getSequence().getRound(), step.getDisplayName(),
            step.getPlayerId());
        final String stepName = step.getName();
        ProLogger.info("Simulating phase: " + stepName);
        if (stepName.endsWith("NonCombatMove")) {
          ProData.initializeSimulation(this, dataCopy, playerCopy);
          final Map<Territory, ProTerritory> factoryMoveMap = nonCombatMoveAi.simulateNonCombatMove(moveDel);
          if (storedFactoryMoveMap == null) {
            storedFactoryMoveMap = ProSimulateTurnUtils.transferMoveMap(factoryMoveMap, data, player);
          }
        } else if (stepName.endsWith("CombatMove") && !stepName.endsWith("AirborneCombatMove")) {
          ProData.initializeSimulation(this, dataCopy, playerCopy);
          final Map<Territory, ProTerritory> moveMap = combatMoveAi.doCombatMove(moveDel);
          if (storedCombatMoveMap == null) {
            storedCombatMoveMap = ProSimulateTurnUtils.transferMoveMap(moveMap, data, player);
          }
        } else if (stepName.endsWith("Battle")) {
          ProData.initializeSimulation(this, dataCopy, playerCopy);
          ProSimulateTurnUtils.simulateBattles(dataCopy, playerCopy, bridge, calc);
        } else if (stepName.endsWith("Place") || stepName.endsWith("EndTurn")) {
          ProData.initializeSimulation(this, dataCopy, player);
          storedPurchaseTerritories = purchaseAi.purchase(purchaseDelegate, data);
          break;
        } else if (stepName.endsWith("Politics")) {
          ProData.initializeSimulation(this, dataCopy, player);
          final PoliticsDelegate politicsDelegate = DelegateFinder.politicsDelegate(dataCopy);
          politicsDelegate.setDelegateBridgeAndPlayer(bridge);
          final List<PoliticalActionAttachment> actions = politicsAi.politicalActions();
          if (storedPoliticalActions == null) {
            storedPoliticalActions = actions;
          }
        }
      }
    }
    ProLogger.info(player.getName() + " time for purchase=" + (System.currentTimeMillis() - start));
  }

  @Override
  protected void place(final boolean bid, final IAbstractPlaceDelegate placeDelegate, final GameData data,
      final PlayerID player) {
    final long start = System.currentTimeMillis();
    BattleCalculator.clearOolCache();
    ProLogUi.notifyStartOfRound(data.getSequence().getRound(), player.getName());
    initializeData();
    purchaseAi.place(storedPurchaseTerritories, placeDelegate);
    storedPurchaseTerritories = null;
    ProLogger.info(player.getName() + " time for place=" + (System.currentTimeMillis() - start));
  }

  @Override
  protected void tech(final ITechDelegate techDelegate, final GameData data, final PlayerID player) {
    ProTechAi.tech(techDelegate, data, player);
  }

  @Override
  public Territory retreatQuery(final GUID battleId, final boolean submerge, final Territory battleTerritory,
      final Collection<Territory> possibleTerritories, final String message) {
    initializeData();

    // Get battle data
    final GameData data = getGameData();
    final PlayerID player = getPlayerId();
    final BattleDelegate delegate = DelegateFinder.battleDelegate(data);
    final IBattle battle = delegate.getBattleTracker().getPendingBattle(battleId);

    // If battle is null or amphibious then don't retreat
    if (battle == null || battleTerritory == null || battle.isAmphibious()) {
      return null;
    }

    // If attacker with more unit strength or strafing and isn't land battle with only air left then don't retreat
    final boolean isAttacker = player.equals(battle.getAttacker());
    final List<Unit> attackers = (List<Unit>) battle.getAttackingUnits();
    final List<Unit> defenders = (List<Unit>) battle.getDefendingUnits();
    final double strengthDifference = ProBattleUtils.estimateStrengthDifference(battleTerritory, attackers, defenders);
    final boolean isStrafing = isAttacker && storedStrafingTerritories.contains(battleTerritory);
    ProLogger.info(player.getName() + " checking retreat from territory " + battleTerritory + ", attackers="
        + attackers.size() + ", defenders=" + defenders.size() + ", submerge=" + submerge + ", attacker=" + isAttacker
        + ", isStrafing=" + isStrafing);
    if ((isStrafing || (isAttacker && strengthDifference > 50))
        && (battleTerritory.isWater() || attackers.stream().anyMatch(Matches.unitIsLand()))) {
      return null;
    }
    calc.setData(getGameData());
    return retreatAi.retreatQuery(battleId, battleTerritory, possibleTerritories);
  }

  @Override
  public boolean shouldBomberBomb(final Territory territory) {
    return combatMoveAi.isBombing();
  }

  // TODO: Consider supporting this functionality
  @Override
  public Collection<Unit> getNumberOfFightersToMoveToNewCarrier(final Collection<Unit> fightersThatCanBeMoved,
      final Territory from) {
    return new ArrayList<>();
  }

  @Override
  public CasualtyDetails selectCasualties(final Collection<Unit> selectFrom,
      final Map<Unit, Collection<Unit>> dependents, final int count, final String message, final DiceRoll dice,
      final PlayerID hit, final Collection<Unit> friendlyUnits, final PlayerID enemyPlayer,
      final Collection<Unit> enemyUnits, final boolean amphibious, final Collection<Unit> amphibiousLandAttackers,
      final CasualtyList defaultCasualties, final GUID battleId, final Territory battlesite,
      final boolean allowMultipleHitsPerUnit) {
    initializeData();

    if (defaultCasualties.size() != count) {
      throw new IllegalStateException("Select Casualties showing different numbers for number of hits to take vs total "
          + "size of default casualty selections");
    }
    if (defaultCasualties.getKilled().size() <= 0) {
      return new CasualtyDetails(defaultCasualties, false);
    }

    // Consider unit cost
    final CasualtyDetails myCasualties = new CasualtyDetails(false);
    myCasualties.addToDamaged(defaultCasualties.getDamaged());
    final List<Unit> selectFromSorted = new ArrayList<>(selectFrom);
    if (enemyUnits.isEmpty()) {
      Collections.sort(selectFromSorted, ProPurchaseUtils.getCostComparator());
    } else {

      // Get battle data
      final GameData data = getGameData();
      final PlayerID player = getPlayerId();
      final BattleDelegate delegate = DelegateFinder.battleDelegate(data);
      final IBattle battle = delegate.getBattleTracker().getPendingBattle(battleId);

      // If defender and could lose battle then don't consider unit cost as just trying to survive
      boolean needToCheck = true;
      final boolean isAttacker = player.equals(battle.getAttacker());
      if (!isAttacker) {
        final List<Unit> attackers = (List<Unit>) battle.getAttackingUnits();
        final List<Unit> defenders = (List<Unit>) battle.getDefendingUnits();
        defenders.removeAll(defaultCasualties.getKilled());
        final double strengthDifference = ProBattleUtils.estimateStrengthDifference(battlesite, attackers, defenders);
        int minStrengthDifference = 60;
        if (!Properties.getLowLuck(data)) {
          minStrengthDifference = 55;
        }
        if (strengthDifference > minStrengthDifference) {
          needToCheck = false;
        }
      }

      // Use bubble sort to save expensive units
      while (needToCheck) {
        needToCheck = false;
        for (int i = 0; i < selectFromSorted.size() - 1; i++) {
          final Unit unit1 = selectFromSorted.get(i);
          final Unit unit2 = selectFromSorted.get(i + 1);
          final double unitCost1 = ProPurchaseUtils.getCost(unit1);
          final double unitCost2 = ProPurchaseUtils.getCost(unit2);
          if (unitCost1 > 1.5 * unitCost2) {
            selectFromSorted.set(i, unit2);
            selectFromSorted.set(i + 1, unit1);
            needToCheck = true;
          }
        }
      }
    }

    // Interleave carriers and planes
    final List<Unit> interleavedTargetList =
        new ArrayList<>(ProTransportUtils.interleaveUnitsCarriersAndPlanes(selectFromSorted, 0));
    for (int i = 0; i < defaultCasualties.getKilled().size(); ++i) {
      myCasualties.addToKilled(interleavedTargetList.get(i));
    }
    if (count != myCasualties.size()) {
      throw new IllegalStateException("AI chose wrong number of casualties");
    }
    return myCasualties;
  }

  @Override
  public HashMap<Territory, Collection<Unit>> scrambleUnitsQuery(final Territory scrambleTo,
      final Map<Territory, Tuple<Collection<Unit>, Collection<Unit>>> possibleScramblers) {
    initializeData();

    // Get battle data
    final GameData data = getGameData();
    final PlayerID player = getPlayerId();
    final BattleDelegate delegate = DelegateFinder.battleDelegate(data);
    final IBattle battle = delegate.getBattleTracker().getPendingBattle(scrambleTo, false, BattleType.NORMAL);

    // If battle is null then don't scramble
    if (battle == null) {
      return null;
    }
    final List<Unit> attackers = (List<Unit>) battle.getAttackingUnits();
    final List<Unit> defenders = (List<Unit>) battle.getDefendingUnits();
    ProLogger.info(player.getName() + " checking scramble to " + scrambleTo + ", attackers=" + attackers.size()
        + ", defenders=" + defenders.size() + ", possibleScramblers=" + possibleScramblers);
    calc.setData(getGameData());
    return scrambleAi.scrambleUnitsQuery(scrambleTo, possibleScramblers);
  }

  @Override
  public boolean selectAttackSubs(final Territory unitTerritory) {
    initializeData();

    // Get battle data
    final GameData data = getGameData();
    final PlayerID player = getPlayerId();
    final BattleDelegate delegate = DelegateFinder.battleDelegate(data);
    final IBattle battle = delegate.getBattleTracker().getPendingBattle(unitTerritory, false, BattleType.NORMAL);

    // If battle is null then don't attack
    if (battle == null) {
      return false;
    }
    final List<Unit> attackers = (List<Unit>) battle.getAttackingUnits();
    final List<Unit> defenders = (List<Unit>) battle.getDefendingUnits();
    ProLogger.info(player.getName() + " checking sub attack in " + unitTerritory + ", attackers=" + attackers
        + ", defenders=" + defenders);
    calc.setData(getGameData());

    // Calculate battle results
    final ProBattleResult result = calc.calculateBattleResults(unitTerritory, attackers, defenders, new HashSet<>());
    ProLogger.debug(player.getName() + " sub attack TUVSwing=" + result.getTuvSwing());
    return result.getTuvSwing() > 0;
  }

  @Override
  public void politicalActions() {
    initializeData();

    if (storedPoliticalActions == null) {
      politicsAi.politicalActions();
    } else {
      politicsAi.doActions(storedPoliticalActions);
      storedPoliticalActions = null;
    }
  }
}
