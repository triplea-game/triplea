package games.strategy.triplea.ai.pro;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameSequence;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.framework.GameDataManager;
import games.strategy.engine.framework.GameDataUtils;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ai.AbstractAi;
import games.strategy.triplea.ai.pro.data.ProBattleResult;
import games.strategy.triplea.ai.pro.data.ProPurchaseTerritory;
import games.strategy.triplea.ai.pro.data.ProTerritory;
import games.strategy.triplea.ai.pro.logging.ProLogUi;
import games.strategy.triplea.ai.pro.logging.ProLogger;
import games.strategy.triplea.ai.pro.simulate.ProDummyDelegateBridge;
import games.strategy.triplea.ai.pro.simulate.ProSimulateTurnUtils;
import games.strategy.triplea.ai.pro.util.ProBattleUtils;
import games.strategy.triplea.ai.pro.util.ProMatches;
import games.strategy.triplea.ai.pro.util.ProOddsCalculator;
import games.strategy.triplea.ai.pro.util.ProPurchaseUtils;
import games.strategy.triplea.ai.pro.util.ProTransportUtils;
import games.strategy.triplea.attachments.PoliticalActionAttachment;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.PoliticsDelegate;
import games.strategy.triplea.delegate.battle.BattleDelegate;
import games.strategy.triplea.delegate.battle.IBattle;
import games.strategy.triplea.delegate.battle.IBattle.BattleType;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import games.strategy.triplea.delegate.data.CasualtyList;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.delegate.remote.ITechDelegate;
import games.strategy.triplea.odds.calculator.IBattleCalculator;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.Getter;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.util.Tuple;

/** Pro AI. */
public abstract class AbstractProAi extends AbstractAi {

  @Getter private final ProOddsCalculator calc;
  @Getter private final ProData proData;

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

  public AbstractProAi(
      final String name,
      final IBattleCalculator battleCalculator,
      final ProData proData,
      final String playerLabel) {
    super(name, playerLabel);
    this.proData = proData;
    calc = new ProOddsCalculator(battleCalculator);
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

  @Override
  public void stopGame() {
    super.stopGame(); // absolutely MUST call super.stopGame() first
    calc.stop();
  }

  private void initializeData() {
    proData.initialize(this);
  }

  public void setStoredStrafingTerritories(final List<Territory> strafingTerritories) {
    storedStrafingTerritories = strafingTerritories;
  }

  /**
   * Some implementations of {@link IBattleCalculator} do require setting a GameData instance before
   * actually being able to run properly. This method should take care of that.
   */
  protected abstract void prepareData(GameData data);

  @Override
  protected void move(
      final boolean nonCombat,
      final IMoveDelegate moveDel,
      final GameData data,
      final GamePlayer player) {
    final Instant start = Instant.now();
    ProLogUi.notifyStartOfRound(data.getSequence().getRound(), player.getName());
    initializeData();
    prepareData(data);
    boolean didCombatMove = false;
    boolean didNonCombatMove = false;
    if (nonCombat) {
      nonCombatMoveAi.doNonCombatMove(storedFactoryMoveMap, storedPurchaseTerritories, moveDel);
      storedFactoryMoveMap = null;
      didNonCombatMove = true;
    } else {
      if (storedCombatMoveMap == null) {
        combatMoveAi.doCombatMove(moveDel);
      } else {
        combatMoveAi.doMove(storedCombatMoveMap, moveDel, data, player);
        storedCombatMoveMap = null;
      }
      didCombatMove = true;
      // Some maps only have a single "combat" move phase. For these, do "non-combat" moves too,
      // after combat moves.
      if (!hasNonCombatMove(getGameStepsForPlayer(data, player, 0))) {
        nonCombatMoveAi.doNonCombatMove(storedFactoryMoveMap, storedPurchaseTerritories, moveDel);
        storedFactoryMoveMap = null;
        didNonCombatMove = true;
      }
    }

    Duration delta = Duration.between(start, Instant.now());
    ProLogger.info(
        String.format(
            "%s move (didCombatMove=%s  didNonCombatMove=%s) time=%s",
            player.getName(), didCombatMove, didNonCombatMove, delta.toMillis()));
  }

  @Override
  protected void purchase(
      final boolean purchaseForBid,
      final int pusToSpend,
      final IPurchaseDelegate purchaseDelegate,
      final GameData data,
      final GamePlayer player) {
    final long start = System.currentTimeMillis();
    ProLogUi.notifyStartOfRound(data.getSequence().getRound(), player.getName());
    initializeData();
    if (pusToSpend <= 0) {
      return;
    }
    if (purchaseForBid) {
      prepareData(data);
      storedPurchaseTerritories = purchaseAi.bid(pusToSpend, purchaseDelegate, data);
    } else {
      // Repair factories
      purchaseAi.repair(pusToSpend, purchaseDelegate, data, player);

      // Check if any place territories exist
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories =
          ProPurchaseUtils.findPurchaseTerritories(proData, player);
      final List<Territory> possibleFactoryTerritories =
          CollectionUtils.getMatches(
              data.getMap().getTerritories(),
              ProMatches.territoryHasNoInfraFactoryAndIsNotConqueredOwnedLand(player));
      if (purchaseTerritories.isEmpty() && possibleFactoryTerritories.isEmpty()) {
        ProLogger.info("No possible place or factory territories owned so exiting purchase logic");
        return;
      }
      ProLogger.info("Starting simulation for purchase phase");

      // Setup data copy and delegates
      final GameData dataCopy = copyData(data);
      if (dataCopy == null) {
        return;
      }
      final GamePlayer playerCopy = dataCopy.getPlayerList().getPlayerId(player.getName());
      final IMoveDelegate moveDel = dataCopy.getMoveDelegate();
      final IDelegateBridge bridge = new ProDummyDelegateBridge(this, playerCopy, dataCopy);
      moveDel.setDelegateBridgeAndPlayer(bridge);

      // Simulate the next phases until place/end of turn is reached then use simulated data for
      // purchase
      final GameSequence sequence = dataCopy.getSequence();
      final int nextStepIndex = sequence.getStepIndex() + 1;
      final List<GameStep> gameSteps = getGameStepsForPlayer(dataCopy, playerCopy, nextStepIndex);
      for (final GameStep step : gameSteps) {
        sequence.setRoundAndStep(sequence.getRound(), step.getDisplayName(), step.getPlayerId());
        final String stepName = step.getName();
        ProLogger.info("Simulating phase: " + stepName);
        if (GameStep.isNonCombatMoveStep(stepName)) {
          proData.initializeSimulation(this, dataCopy, playerCopy);
          final Map<Territory, ProTerritory> factoryMoveMap =
              nonCombatMoveAi.simulateNonCombatMove(moveDel);
          if (storedFactoryMoveMap == null) {
            storedFactoryMoveMap =
                ProSimulateTurnUtils.transferMoveMap(proData, factoryMoveMap, data, player);
          }
        } else if (GameStep.isCombatMoveStep(stepName)
            && !GameStep.isAirborneCombatMoveStep(stepName)) {
          proData.initializeSimulation(this, dataCopy, playerCopy);
          final Map<Territory, ProTerritory> moveMap = combatMoveAi.doCombatMove(moveDel);
          if (storedCombatMoveMap == null) {
            storedCombatMoveMap =
                ProSimulateTurnUtils.transferMoveMap(proData, moveMap, data, player);
          }
          // Some maps only have a combat move. For these, do both types of moves during this phase.
          if (!hasNonCombatMove(gameSteps)) {
            // Copy the data so we can simulate battles on it, in order to choose our "non combat"
            // moves based on that (estimated) board state.
            final GameData dataCopy2 = copyData(data);
            if (dataCopy2 == null) {
              return;
            }
            final GamePlayer playerCopy2 = dataCopy2.getPlayerList().getPlayerId(player.getName());
            proData.initializeSimulation(this, dataCopy2, playerCopy2);
            ProSimulateTurnUtils.simulateBattles(proData, dataCopy2, playerCopy2, bridge, calc);
            proData.initializeSimulation(this, dataCopy2, playerCopy2);
            Map<Territory, ProTerritory> factoryMoveMap =
                nonCombatMoveAi.simulateNonCombatMove(moveDel);
            if (storedFactoryMoveMap == null) {
              storedFactoryMoveMap =
                  ProSimulateTurnUtils.transferMoveMap(proData, factoryMoveMap, data, player);
            }
          }
        } else if (GameStep.isBattleStep(stepName)) {
          proData.initializeSimulation(this, dataCopy, playerCopy);
          ProSimulateTurnUtils.simulateBattles(proData, dataCopy, playerCopy, bridge, calc);
        } else if (GameStep.isPlaceStep(stepName) || GameStep.isEndTurnStep(stepName)) {
          proData.initializeSimulation(this, dataCopy, player);
          storedPurchaseTerritories = purchaseAi.purchase(purchaseDelegate, data);
          break;
        } else if (GameStep.isPoliticsStep(stepName)) {
          proData.initializeSimulation(this, dataCopy, player);
          // Can only do politics if this player still owns its capital.
          if (proData.getMyCapital() == null || proData.getMyCapital().isOwnedBy(player)) {
            final PoliticsDelegate politicsDelegate = dataCopy.getPoliticsDelegate();
            politicsDelegate.setDelegateBridgeAndPlayer(bridge);
            final List<PoliticalActionAttachment> actions = politicsAi.politicalActions();
            if (storedPoliticalActions == null) {
              storedPoliticalActions = actions;
            }
          }
        }
      }
    }
    ProLogger.info(player.getName() + " time for purchase=" + (System.currentTimeMillis() - start));
  }

  private GameData copyData(GameData data) {
    GameDataManager.Options options = GameDataManager.Options.builder().withDelegates(true).build();
    GameData dataCopy = GameDataUtils.cloneGameData(data, options).orElse(null);
    Optional.ofNullable(dataCopy).ifPresent(this::prepareData);
    return dataCopy;
  }

  private static List<GameStep> getGameStepsForPlayer(
      GameData gameData, GamePlayer gamePlayer, int startStep) {
    int stepIndex = 0;
    final List<GameStep> gameSteps = new ArrayList<>();
    for (final GameStep gameStep : gameData.getSequence()) {
      if (stepIndex >= startStep && gamePlayer.equals(gameStep.getPlayerId())) {
        gameSteps.add(gameStep);
      }
      stepIndex++;
    }
    return gameSteps;
  }

  private boolean hasNonCombatMove(Collection<GameStep> steps) {
    return steps.stream().anyMatch(s -> GameStep.isNonCombatMoveStep(s.getName()));
  }

  @Override
  protected void place(
      final boolean bid,
      final IAbstractPlaceDelegate placeDelegate,
      final GameState data,
      final GamePlayer player) {
    final long start = System.currentTimeMillis();
    ProLogUi.notifyStartOfRound(data.getSequence().getRound(), player.getName());
    initializeData();
    purchaseAi.place(storedPurchaseTerritories, placeDelegate);
    storedPurchaseTerritories = null;
    ProLogger.info(player.getName() + " time for place=" + (System.currentTimeMillis() - start));
  }

  @Override
  protected void tech(
      final ITechDelegate techDelegate, final GameData data, final GamePlayer player) {
    ProTechAi.tech(techDelegate, data, player);
  }

  @Override
  public Territory retreatQuery(
      final UUID battleId,
      final boolean submerge,
      final Territory battleTerritory,
      final Collection<Territory> possibleTerritories,
      final String message) {
    initializeData();

    // Get battle data
    final GameData data = getGameData();
    final GamePlayer player = this.getGamePlayer();
    final BattleDelegate delegate = data.getBattleDelegate();
    final IBattle battle = delegate.getBattleTracker().getPendingBattle(battleId);

    // If battle is null or amphibious then don't retreat
    if (battle == null || battleTerritory == null || battle.isAmphibious()) {
      return null;
    }

    // If attacker with more unit strength or strafing and isn't land battle with only air left then
    // don't retreat
    final boolean isAttacker = player.equals(battle.getAttacker());
    final Collection<Unit> attackers = battle.getAttackingUnits();
    final Collection<Unit> defenders = battle.getDefendingUnits();
    final double strengthDifference =
        ProBattleUtils.estimateStrengthDifference(battleTerritory, attackers, defenders);
    final boolean isStrafing = isAttacker && storedStrafingTerritories.contains(battleTerritory);
    ProLogger.info(
        player.getName()
            + " checking retreat from territory "
            + battleTerritory
            + ", attackers="
            + attackers.size()
            + ", defenders="
            + defenders.size()
            + ", submerge="
            + submerge
            + ", attacker="
            + isAttacker
            + ", isStrafing="
            + isStrafing);
    if ((isStrafing || (isAttacker && strengthDifference > 50))
        && (battleTerritory.isWater() || attackers.stream().anyMatch(Matches.unitIsLand()))) {
      return null;
    }
    prepareData(getGameData());
    return retreatAi.retreatQuery(battleId, battleTerritory, possibleTerritories);
  }

  @Override
  public boolean shouldBomberBomb(final Territory territory) {
    return combatMoveAi.isBombing();
  }

  // TODO: Consider supporting this functionality
  @Override
  public Collection<Unit> getNumberOfFightersToMoveToNewCarrier(
      final Collection<Unit> fightersThatCanBeMoved, final Territory from) {
    return new ArrayList<>();
  }

  @Override
  public CasualtyDetails selectCasualties(
      final Collection<Unit> selectFrom,
      final Map<Unit, Collection<Unit>> dependents,
      final int count,
      final String message,
      final DiceRoll dice,
      final GamePlayer hit,
      final Collection<Unit> friendlyUnits,
      final Collection<Unit> enemyUnits,
      final boolean amphibious,
      final Collection<Unit> amphibiousLandAttackers,
      final CasualtyList defaultCasualties,
      final UUID battleId,
      final Territory battleSite,
      final boolean allowMultipleHitsPerUnit) {
    initializeData();

    if (defaultCasualties.size() != count) {
      throw new IllegalStateException(
          String.format(
              "Select Casualties showing different numbers for number of hits to take (%s) vs "
                  + "total size of default casualty selections (%s) in %s (hit = %s)",
              count, defaultCasualties.size(), battleSite, hit.getName()));
    }
    if (defaultCasualties.getKilled().isEmpty()) {
      return new CasualtyDetails(defaultCasualties, false);
    }

    // Consider unit cost
    final CasualtyDetails myCasualties = new CasualtyDetails(false);
    myCasualties.addToDamaged(defaultCasualties.getDamaged());
    final List<Unit> selectFromSorted = new ArrayList<>(selectFrom);
    if (enemyUnits.isEmpty()) {
      selectFromSorted.sort(ProPurchaseUtils.getCostComparator(proData));
    } else {

      // Get battle data
      final GameData data = getGameData();
      final GamePlayer player = this.getGamePlayer();
      final BattleDelegate delegate = data.getBattleDelegate();
      final IBattle battle = delegate.getBattleTracker().getPendingBattle(battleId);

      // If defender and could lose battle then don't consider unit cost as just trying to survive
      boolean needToCheck = true;
      final boolean isAttacker = player.equals(battle.getAttacker());
      if (!isAttacker) {
        final Collection<Unit> attackers = battle.getAttackingUnits();
        final Collection<Unit> defenders = new ArrayList<>(battle.getDefendingUnits());
        defenders.removeAll(defaultCasualties.getKilled());
        final double strengthDifference =
            ProBattleUtils.estimateStrengthDifference(battleSite, attackers, defenders);
        int minStrengthDifference = 60;
        if (!Properties.getLowLuck(data.getProperties())) {
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
          final double unitCost1 = ProPurchaseUtils.getCost(proData, unit1);
          final double unitCost2 = ProPurchaseUtils.getCost(proData, unit2);
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
  public Map<Territory, Collection<Unit>> scrambleUnitsQuery(
      final Territory scrambleTo,
      final Map<Territory, Tuple<Collection<Unit>, Collection<Unit>>> possibleScramblers) {
    initializeData();

    // Get battle data
    final GameData data = getGameData();
    final GamePlayer player = this.getGamePlayer();
    final BattleDelegate delegate = data.getBattleDelegate();
    final IBattle battle =
        delegate.getBattleTracker().getPendingBattle(scrambleTo, BattleType.NORMAL);

    // If battle is null then don't scramble
    if (battle == null) {
      return null;
    }
    final Collection<Unit> attackers = battle.getAttackingUnits();
    final Collection<Unit> defenders = battle.getDefendingUnits();
    ProLogger.info(
        player.getName()
            + " checking scramble to "
            + scrambleTo
            + ", attackers="
            + attackers.size()
            + ", defenders="
            + defenders.size()
            + ", possibleScramblers="
            + possibleScramblers);
    prepareData(getGameData());
    return scrambleAi.scrambleUnitsQuery(scrambleTo, possibleScramblers);
  }

  @Override
  public boolean selectAttackSubs(final Territory unitTerritory) {
    initializeData();

    // Get battle data
    final GameData data = getGameData();
    final GamePlayer player = this.getGamePlayer();
    final BattleDelegate delegate = data.getBattleDelegate();
    final IBattle battle =
        delegate.getBattleTracker().getPendingBattle(unitTerritory, BattleType.NORMAL);

    // If battle is null then don't attack
    if (battle == null) {
      return false;
    }
    final Collection<Unit> attackers = battle.getAttackingUnits();
    final Collection<Unit> defenders = battle.getDefendingUnits();
    ProLogger.info(
        player.getName()
            + " checking sub attack in "
            + unitTerritory
            + ", attackers="
            + attackers
            + ", defenders="
            + defenders);
    prepareData(getGameData());

    // Calculate battle results
    final ProBattleResult result =
        calc.calculateBattleResults(proData, unitTerritory, attackers, defenders, new HashSet<>());
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
