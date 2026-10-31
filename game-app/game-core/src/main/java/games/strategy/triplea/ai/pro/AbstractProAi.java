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
import games.strategy.triplea.ai.pro.util.ProCasualtyUtils;
import games.strategy.triplea.ai.pro.util.ProMatches;
import games.strategy.triplea.ai.pro.util.ProOddsCalculator;
import games.strategy.triplea.ai.pro.util.ProPurchaseUtils;
import games.strategy.triplea.ai.pro.util.ProTransportUtils;
import games.strategy.triplea.ai.pro.util.ProUtils;
import games.strategy.triplea.attachments.PoliticalActionAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
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
  // The combat move plan's can-hold verdict per attacked territory name.
  private Map<String, Boolean> plannedCanHold = Map.of();
  // Planned strafes: attacked territory name -> rounds to fight before retreating.
  private Map<String, Integer> storedStrafeRetreats = Map.of();

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

  public void initializeData() {
    proData.initialize(this);
  }

  public void setStoredStrafingTerritories(final List<Territory> strafingTerritories) {
    storedStrafingTerritories = strafingTerritories;
  }

  /** Attacks to retreat from once that many rounds are fought, by territory name. */
  public void setStoredStrafeRetreats(final Map<String, Integer> strafeRetreats) {
    storedStrafeRetreats = Map.copyOf(strafeRetreats);
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
        rememberCanHold(combatMoveAi.doCombatMove(moveDel));
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
      List<ProBidPlanner.Placement> bidPlan = List.of();
      if (proData.isBeta()) {
        bidPlan =
            new ProBidPlanner(this, proData, calc, combatMoveAi).plan(data, player, pusToSpend);
        initializeData();
        prepareData(data);
      }
      storedPurchaseTerritories = purchaseAi.bid(pusToSpend, purchaseDelegate, data, bidPlan);
    } else {
      // Repair factories
      purchaseAi.repair(pusToSpend, purchaseDelegate, data, player);

      // Check if any place territories exist
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories =
          ProPurchaseUtils.findPurchaseTerritories(proData, player);
      final List<Territory> possibleFactoryTerritories =
          CollectionUtils.getMatches(
              data.getMap().getTerritories(),
              ProMatches.territoryHasNoInfraFactoryAndIsOwnedLand(player)
                  .and(ProMatches.territoryIsNotConqueredUnlessPlacementCapturedAllowed(player)));
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
        if (GameStep.isNonCombatMoveStepName(stepName)) {
          proData.initializeSimulation(this, dataCopy, playerCopy);
          final Map<Territory, ProTerritory> factoryMoveMap =
              nonCombatMoveAi.simulateNonCombatMove(moveDel);
          if (storedFactoryMoveMap == null) {
            storedFactoryMoveMap =
                ProSimulateTurnUtils.transferMoveMap(proData, factoryMoveMap, data, player);
          }
        } else if (GameStep.isCombatMoveStepName(stepName)
            && !GameStep.isAirborneCombatMoveStepName(stepName)) {
          proData.initializeSimulation(this, dataCopy, playerCopy);
          final Map<Territory, ProTerritory> moveMap = combatMoveAi.doCombatMove(moveDel);
          if (storedCombatMoveMap == null) {
            // transferMoveMap drops canHold, so keep the plan's verdicts by territory name.
            rememberCanHold(moveMap);
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
        } else if (GameStep.isBattleStepName(stepName)) {
          proData.initializeSimulation(this, dataCopy, playerCopy);
          ProSimulateTurnUtils.simulateBattles(proData, dataCopy, playerCopy, bridge, calc);
        } else if (GameStep.isPlaceStepName(stepName) || GameStep.isEndTurnStepName(stepName)) {
          proData.initializeSimulation(this, dataCopy, player);
          storedPurchaseTerritories = purchaseAi.purchase(purchaseDelegate, data);
          break;
        } else if (GameStep.isPoliticsStepName(stepName)) {
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

  @Nullable
  GameData copyData(GameData data) {
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
    return steps.stream().anyMatch(s -> GameStep.isNonCombatMoveStepName(s.getName()));
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

  public Optional<Territory> retreatQuery(
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
      return Optional.empty();
    }

    // If attacker with more unit strength or strafing and isn't land battle with only air left then
    // don't retreat
    final boolean isAttacker = player.equals(battle.getAttacker());
    final Collection<Unit> attackers = battle.getAttackingUnits();
    final Collection<Unit> defenders = battle.getDefendingUnits();
    final double strengthDifference =
        ProBattleUtils.estimateStrengthDifference(battleTerritory, attackers, defenders);
    final boolean isStrafing = isAttacker && storedStrafingTerritories.contains(battleTerritory);
    final Integer strafeRounds = storedStrafeRetreats.get(battleTerritory.getName());
    if (isAttacker
        && !submerge
        && strafeRounds != null
        && battle.getBattleRound() >= strafeRounds
        && !possibleTerritories.isEmpty()
        && proData.isBeta()) {
      // Retreat to the territory holding the most of my units
      final Territory to =
          possibleTerritories.stream()
              .max(
                  Comparator.comparingInt(
                      (Territory r) ->
                          r.getUnitCollection().countMatches(Matches.unitIsOwnedBy(player))))
              .orElseThrow();
      ProLogger.info(
          player.getName()
              + " strafing: retreating from "
              + battleTerritory
              + " after round "
              + battle.getBattleRound()
              + " to "
              + to);
      return Optional.of(to);
    }
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
      return Optional.empty();
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

      // AA fire during movement has no associated pending battle; fall back to
      // simple cost-based sorting since no attacker/defender context is available.
      if (battle == null) {
        selectFromSorted.sort(ProPurchaseUtils.getCostComparator(proData));
      } else {
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
    }

    // Interleave carriers and planes
    final List<Unit> interleavedTargetList =
        new ArrayList<>(ProTransportUtils.interleaveUnitsCarriersAndPlanes(selectFromSorted, 0));
    final List<Unit> killed =
        keepLandToCapture(
            new ArrayList<>(interleavedTargetList.subList(0, defaultCasualties.getKilled().size())),
            selectFrom,
            defaultCasualties.getDamaged(),
            hit,
            friendlyUnits,
            enemyUnits,
            battleId,
            battleSite);
    killed.forEach(myCasualties::addToKilled);
    if (count != myCasualties.size()) {
      throw new IllegalStateException("AI chose wrong number of casualties");
    }
    return myCasualties;
  }

  private void rememberCanHold(final Map<Territory, ProTerritory> plan) {
    final Map<String, Boolean> canHold = new HashMap<>();
    plan.forEach((t, patd) -> canHold.put(t.getName(), patd.isCanHold()));
    plannedCanHold = canHold;
  }

  /**
   * When attacking land (not strafing) and {@code killed} would take the last land unit while air
   * survives, loses the cheapest surviving air unit instead, since only land units capture. Skips
   * the swap when the capture is worth less than the extra casualty cost, or when the attack with
   * the land unit kept wins under 50%.
   */
  private List<Unit> keepLandToCapture(
      final List<Unit> killed,
      final Collection<Unit> selectFrom,
      final Collection<Unit> damaged,
      final GamePlayer hit,
      final Collection<Unit> friendlyUnits,
      final Collection<Unit> enemyUnits,
      final UUID battleId,
      final Territory battleSite) {
    final GamePlayer player = getGamePlayer();
    if (battleSite.isWater()
        || !player.equals(hit)
        || storedStrafingTerritories.contains(battleSite)
        || storedStrafeRetreats.containsKey(battleSite.getName())) {
      return killed;
    }
    final IBattle battle =
        getGameData().getBattleDelegate().getBattleTracker().getPendingBattle(battleId);
    final Collection<Unit> attackersAlive;
    final Collection<Unit> defendersLeft;
    if (battle != null) {
      if (!player.equals(battle.getAttacker())) {
        return killed;
      }
      attackersAlive = battle.getAttackingUnits();
      defendersLeft = battle.getDefendingUnits();
    } else {
      // No pending battle (AA fire in a move, or a direct call): attacking if the land is enemy.
      if (!Matches.isTerritoryEnemy(player).test(battleSite)) {
        return killed;
      }
      attackersAlive = friendlyUnits;
      defendersLeft = enemyUnits;
    }
    final Optional<ProCasualtyUtils.KeepLandSwap> swap =
        ProCasualtyUtils.findKeepLandSwap(
            killed, attackersAlive, selectFrom, damaged, u -> ProPurchaseUtils.getCost(proData, u));
    if (swap.isEmpty()) {
      return killed;
    }
    final double captureValue = captureValue(battleSite);
    final double extraCost =
        ProPurchaseUtils.getCost(proData, swap.get().airToLose())
            - ProPurchaseUtils.getCost(proData, swap.get().landToKeep());
    if (captureValue <= extraCost) {
      return killed;
    }
    final List<Unit> withLand = new ArrayList<>(attackersAlive);
    withLand.removeAll(swap.get().apply(killed));
    final Collection<Unit> defenders =
        CollectionUtils.getMatches(defendersLeft, Matches.unitIsNotInfrastructure());
    prepareData(getGameData());
    final double keepLandWin =
        defenders.isEmpty()
            ? 100
            : calc.calculateBattleResultsNoSubmerge(
                    proData, battleSite, withLand, defenders, new HashSet<>())
                .getWinPercentage();
    ProLogger.debug(
        player.getName()
            + " keep-land check at "
            + battleSite.getName()
            + ": keep "
            + swap.get().landToKeep().getType().getName()
            + ", lose "
            + swap.get().airToLose().getType().getName()
            + ", capture value="
            + captureValue
            + ", extra cost="
            + extraCost
            + ", win% with land kept="
            + keepLandWin);
    // NaN if the calculator has no data.
    if (Double.isNaN(keepLandWin) || keepLandWin < 50) {
      return killed;
    }
    return swap.get().apply(killed);
  }

  /** What taking {@code site} is worth in PUs to this player. */
  private double captureValue(final Territory site) {
    final Optional<TerritoryAttachment> ta = TerritoryAttachment.get(site);
    if (ta.isEmpty()) {
      return 0;
    }
    final GameData data = getGameData();
    final GamePlayer owner = site.getOwner();
    boolean ownerAllyMovesFirst = false;
    for (final GamePlayer p : ProUtils.getOtherPlayersInTurnOrder(getGamePlayer())) {
      if (p.equals(owner)) {
        break;
      }
      ownerAllyMovesFirst |= data.getRelationshipTracker().isAllied(p, owner);
    }
    final int production = ta.get().getProduction();
    return ProCasualtyUtils.captureValue(
        production * Properties.getPuMultiplier(data.getProperties()),
        ProMatches.territoryHasInfraFactoryAndIsLand().test(site) ? production : 0,
        ta.get().getVictoryCity() > 0,
        ProUtils.capturedTreasury(site, data),
        ProUtils.captureStopsIncome(site, data.getMap()),
        ProUtils.getPlayerProduction(owner, data),
        plannedCanHold.getOrDefault(site.getName(), false),
        ownerAllyMovesFirst);
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
