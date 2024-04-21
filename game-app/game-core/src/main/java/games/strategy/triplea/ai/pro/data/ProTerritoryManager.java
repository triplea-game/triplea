package games.strategy.triplea.ai.pro.data;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.util.BreadthFirstSearch;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ai.pro.ProData;
import games.strategy.triplea.ai.pro.logging.ProLogger;
import games.strategy.triplea.ai.pro.util.ProBattleUtils;
import games.strategy.triplea.ai.pro.util.ProMatches;
import games.strategy.triplea.ai.pro.util.ProOddsCalculator;
import games.strategy.triplea.ai.pro.util.ProTransportUtils;
import games.strategy.triplea.ai.pro.util.ProUtils;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.delegate.battle.ScrambleLogic;
import games.strategy.triplea.delegate.move.validation.MoveValidator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import lombok.Getter;
import org.apache.commons.lang3.mutable.MutableObject;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.util.Tuple;

/** Manages info about territories. */
public class ProTerritoryManager {

  private final ProOddsCalculator calc;
  private final ProData proData;
  private final GamePlayer player;

  @Getter private ProMyMoveOptions attackOptions;
  private ProMyMoveOptions potentialAttackOptions;
  @Getter private ProMyMoveOptions defendOptions;
  @Getter private ProOtherMoveOptions alliedAttackOptions;
  @Getter private ProOtherMoveOptions enemyDefendOptions;
  @Getter private ProOtherMoveOptions enemyAttackOptions;

  public ProTerritoryManager(final ProOddsCalculator calc, final ProData proData) {
    this.calc = calc;
    this.proData = proData;
    player = proData.getPlayer();
    attackOptions = new ProMyMoveOptions();
    potentialAttackOptions = new ProMyMoveOptions();
    defendOptions = new ProMyMoveOptions();
    alliedAttackOptions = new ProOtherMoveOptions();
    enemyDefendOptions = new ProOtherMoveOptions();
    enemyAttackOptions = new ProOtherMoveOptions();
  }

  public ProTerritoryManager(
      final ProOddsCalculator calc,
      final ProData proData,
      final ProTerritoryManager territoryManager) {
    this(calc, proData);
    attackOptions = new ProMyMoveOptions(territoryManager.attackOptions, proData);
    potentialAttackOptions = new ProMyMoveOptions(territoryManager.potentialAttackOptions, proData);
    defendOptions = new ProMyMoveOptions(territoryManager.defendOptions, proData);
    alliedAttackOptions = territoryManager.getAlliedAttackOptions();
    enemyDefendOptions = territoryManager.getEnemyDefendOptions();
    enemyAttackOptions = territoryManager.getEnemyAttackOptions();
  }

  /** Sets 'alliedAttackOptions' field to possible available attack options. */
  public void populateAttackOptions() {
    findAttackOptions(
        proData,
        player,
        proData.getMyUnitTerritories(),
        attackOptions.getTerritoryMap(),
        attackOptions.getUnitMoveMap(),
        attackOptions.getTransportMoveMap(),
        attackOptions.getBombardMap(),
        attackOptions.getTransportList(),
        new ArrayList<>(),
        new ArrayList<>(),
        new ArrayList<>(),
        false,
        false);
    findBombingOptions();
    alliedAttackOptions = findAlliedAttackOptions(player);
  }

  public void populatePotentialAttackOptions() {
    findPotentialAttackOptions(
        proData,
        player,
        proData.getMyUnitTerritories(),
        potentialAttackOptions.getTerritoryMap(),
        potentialAttackOptions.getUnitMoveMap(),
        potentialAttackOptions.getTransportMoveMap(),
        potentialAttackOptions.getBombardMap(),
        potentialAttackOptions.getTransportList());
  }

  public void populateDefenseOptions(final List<Territory> clearedTerritories) {
    findDefendOptions(
        proData,
        player,
        proData.getMyUnitTerritories(),
        defendOptions.getTerritoryMap(),
        defendOptions.getUnitMoveMap(),
        defendOptions.getTransportMoveMap(),
        defendOptions.getTransportList(),
        clearedTerritories,
        false);
  }

  public void populateEnemyAttackOptions(
      final Collection<Territory> clearedTerritories,
      final Collection<Territory> territoriesToCheck) {
    enemyAttackOptions =
        findEnemyAttackOptions(proData, player, clearedTerritories, territoriesToCheck);
  }

  public void populateEnemyDefenseOptions() {
    findScrambleOptions(proData, player, attackOptions.getTerritoryMap());
    enemyDefendOptions = findEnemyDefendOptions(proData, player);
  }

  public List<ProTerritory> removeTerritoriesThatCantBeConquered() {
    return removeTerritoriesThatCantBeConquered(
        player,
        attackOptions.getTerritoryMap(),
        attackOptions.getUnitMoveMap(),
        attackOptions.getTransportMoveMap(),
        alliedAttackOptions,
        enemyDefendOptions,
        false);
  }

  private List<ProTerritory> removeTerritoriesThatCantBeConquered(
      final GamePlayer player,
      final Map<Territory, ProTerritory> attackMap,
      final Map<Unit, Set<Territory>> unitAttackMap,
      final Map<Unit, Set<Territory>> transportAttackMap,
      final ProOtherMoveOptions alliedAttackOptions,
      final ProOtherMoveOptions enemyDefendOptions,
      final boolean isIgnoringRelationships) {

    ProLogger.info("Removing territories that can't be conquered");
    final GameState data = proData.getData();
    final GameMap gameMap = data.getMap();

    // Determine if territory can be successfully attacked with max possible attackers
    final List<Territory> territoriesToRemove = new ArrayList<>();
    for (final Territory t : attackMap.keySet()) {
      final ProTerritory patd = attackMap.get(t);

      // Check if I can win without amphib units
      final List<Unit> defenders;
      if (isIgnoringRelationships) {
        defenders = new ArrayList<>(t.getUnitCollection());
        // Don't include any of the attacking units as defenders.
        defenders.removeAll(patd.getMaxUnits());
      } else {
        defenders = patd.getMaxEnemyDefenders(player);
      }
      patd.setMaxBattleResult(
          calc.estimateAttackBattleResults(proData, t, patd.getMaxUnits(), defenders, Set.of()));

      // Add in amphib units if I can't win without them
      if (patd.getMaxBattleResult().getWinPercentage() < proData.getWinPercentage()
          && !patd.getMaxAmphibUnits().isEmpty()) {
        final Set<Unit> combinedUnits = new HashSet<>(patd.getMaxUnits());
        combinedUnits.addAll(patd.getMaxAmphibUnits());
        patd.setMaxBattleResult(
            calc.estimateAttackBattleResults(
                proData, t, combinedUnits, defenders, patd.getMaxBombardUnits()));
        patd.setNeedAmphibUnits(true);
      }

      // Check strafing and using allied attack if enemy capital/factory
      boolean isEnemyCapitalOrFactory = false;
      final TerritoryAttachment ta = TerritoryAttachment.get(t);
      if (!ProUtils.isNeutralLand(t)
          && ((ta != null && ta.isCapital())
              || ProMatches.territoryHasInfraFactoryAndIsLand().test(t))) {
        isEnemyCapitalOrFactory = true;
      }
      if (patd.getMaxBattleResult().getWinPercentage() < proData.getMinWinPercentage()
          && isEnemyCapitalOrFactory
          && alliedAttackOptions.getMax(t) != null) {

        // Check for allied attackers
        final ProTerritory alliedAttack = alliedAttackOptions.getMax(t);
        final Set<Unit> alliedUnits = new HashSet<>(alliedAttack.getMaxUnits());
        alliedUnits.addAll(alliedAttack.getMaxAmphibUnits());
        if (!alliedUnits.isEmpty()) {

          // Make sure allies' capital isn't next to territory
          final GamePlayer alliedPlayer = CollectionUtils.getAny(alliedUnits).getOwner();
          final Territory capital =
              TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(alliedPlayer, gameMap);
          if (capital != null && !gameMap.getNeighbors(capital).contains(t)) {

            // Get max enemy defenders
            final Set<Unit> additionalEnemyDefenders = new HashSet<>();
            final List<GamePlayer> players = ProUtils.getOtherPlayersInTurnOrder(player);
            for (final ProTerritory enemyDefendOption : enemyDefendOptions.getAll(t)) {
              final Set<Unit> enemyUnits = new HashSet<>(enemyDefendOption.getMaxUnits());
              enemyUnits.addAll(enemyDefendOption.getMaxAmphibUnits());
              if (!enemyUnits.isEmpty()) {
                final GamePlayer enemyPlayer = CollectionUtils.getAny(enemyUnits).getOwner();
                if (ProUtils.isPlayersTurnFirst(players, enemyPlayer, alliedPlayer)) {
                  additionalEnemyDefenders.addAll(enemyUnits);
                }
              }
            }

            // Check allied result without strafe
            final Set<Unit> enemyDefendersBeforeStrafe = new HashSet<>(defenders);
            enemyDefendersBeforeStrafe.addAll(additionalEnemyDefenders);
            final ProBattleResult result =
                calc.estimateAttackBattleResults(
                    proData,
                    t,
                    new ArrayList<>(alliedUnits),
                    new ArrayList<>(enemyDefendersBeforeStrafe),
                    alliedAttack.getMaxBombardUnits());
            if (result.getWinPercentage() < proData.getWinPercentage()) {
              patd.setStrafing(true);

              // Try to strafe to allow allies to conquer territory
              final Set<Unit> combinedUnits = new HashSet<>(patd.getMaxUnits());
              combinedUnits.addAll(patd.getMaxAmphibUnits());
              final ProBattleResult strafeResult =
                  calc.callBattleCalcWithRetreatAir(
                      proData, t, combinedUnits, defenders, patd.getMaxBombardUnits());

              // Check allied result with strafe
              final Set<Unit> enemyDefendersAfterStrafe =
                  new HashSet<>(strafeResult.getAverageDefendersRemaining());
              enemyDefendersAfterStrafe.addAll(additionalEnemyDefenders);
              patd.setMaxBattleResult(
                  calc.estimateAttackBattleResults(
                      proData,
                      t,
                      new ArrayList<>(alliedUnits),
                      new ArrayList<>(enemyDefendersAfterStrafe),
                      alliedAttack.getMaxBombardUnits()));

              ProLogger.debug(
                  "Checking strafing territory: "
                      + t
                      + ", alliedPlayer="
                      + CollectionUtils.getAny(alliedUnits).getOwner().getName()
                      + ", maxWin%="
                      + patd.getMaxBattleResult().getWinPercentage()
                      + ", maxAttackers="
                      + alliedUnits.size()
                      + ", maxDefenders="
                      + enemyDefendersAfterStrafe.size());
            }
          }
        }
      }

      if (patd.getMaxBattleResult().getWinPercentage() < proData.getMinWinPercentage()
          || (patd.isStrafing()
              && (patd.getMaxBattleResult().getWinPercentage() < proData.getWinPercentage()
                  || !patd.getMaxBattleResult().isHasLandUnitRemaining()))) {
        territoriesToRemove.add(t);
      }
    }

    // Remove territories that can't be successfully attacked
    Collections.sort(territoriesToRemove);
    final List<ProTerritory> result = new ArrayList<>(attackMap.values());
    for (final Territory t : territoriesToRemove) {
      final ProTerritory proTerritoryToRemove = attackMap.get(t);
      final Set<Unit> combinedUnits = new HashSet<>(proTerritoryToRemove.getMaxUnits());
      combinedUnits.addAll(proTerritoryToRemove.getMaxAmphibUnits());
      ProLogger.debug(
          "Removing territory that we can't successfully attack: "
              + t
              + ", maxWin%="
              + proTerritoryToRemove.getMaxBattleResult().getWinPercentage()
              + ", maxAttackers="
              + combinedUnits.size());
      result.remove(proTerritoryToRemove);
      for (final Set<Territory> territories : unitAttackMap.values()) {
        territories.remove(t);
      }
      for (final Set<Territory> territories : transportAttackMap.values()) {
        territories.remove(t);
      }
    }
    return result;
  }

  public List<ProTerritory> removePotentialTerritoriesThatCantBeConquered() {
    return removeTerritoriesThatCantBeConquered(
        player,
        potentialAttackOptions.getTerritoryMap(),
        potentialAttackOptions.getUnitMoveMap(),
        potentialAttackOptions.getTransportMoveMap(),
        alliedAttackOptions,
        enemyDefendOptions,
        true);
  }

  public List<Territory> getDefendTerritories() {
    return new ArrayList<>(defendOptions.getTerritoryMap().keySet());
  }

  public List<Territory> getStrafingTerritories() {
    final List<Territory> strafingTerritories = new ArrayList<>();
    for (final Territory t : attackOptions.getTerritoryMap().keySet()) {
      if (attackOptions.getTerritoryMap().get(t).isStrafing()) {
        strafingTerritories.add(t);
      }
    }
    return strafingTerritories;
  }

  public List<Territory> getCantHoldTerritories() {
    final List<Territory> territoriesThatCantBeHeld = new ArrayList<>();
    for (final Territory t : defendOptions.getTerritoryMap().keySet()) {
      if (!defendOptions.getTerritoryMap().get(t).isCanHold()) {
        territoriesThatCantBeHeld.add(t);
      }
    }
    return territoriesThatCantBeHeld;
  }

  public boolean haveUsedAllAttackTransports() {
    final Set<Unit> movedTransports = new HashSet<>();
    for (final ProTerritory patd : attackOptions.getTerritoryMap().values()) {
      movedTransports.addAll(patd.getAmphibAttackMap().keySet());
      movedTransports.addAll(
          CollectionUtils.getMatches(patd.getUnits(), Matches.unitIsSeaTransport()));
    }
    return movedTransports.size() >= attackOptions.getTransportList().size();
  }

  private void findScrambleOptions(
      final ProData proData, final GamePlayer player, final Map<Territory, ProTerritory> moveMap) {
    final GameState data = proData.getData();

    if (!Properties.getScrambleRulesInEffect(data.getProperties())) {
      return;
    }

    final var scrambleLogic = new ScrambleLogic(data, player, moveMap.keySet());
    for (final var territoryToScramblersEntry :
        scrambleLogic.getUnitsThatCanScrambleByDestination().entrySet()) {
      final Territory to = territoryToScramblersEntry.getKey();
      for (final Tuple<Collection<Unit>, Collection<Unit>> airbasesAndScramblers :
          territoryToScramblersEntry.getValue().values()) {
        final Collection<Unit> airbases = airbasesAndScramblers.getFirst();
        final Collection<Unit> scramblers = airbasesAndScramblers.getSecond();
        final int maxCanScramble = ScrambleLogic.getMaxScrambleCount(airbases);

        final List<Unit> addTo = moveMap.get(to).getMaxScrambleUnits();
        if (scramblers.size() <= maxCanScramble) {
          addTo.addAll(scramblers);
        } else {
          scramblers.stream()
              .sorted(
                  Comparator.<Unit>comparingDouble(
                          unit ->
                              ProBattleUtils.estimateStrength(to, List.of(unit), List.of(), false))
                      .reversed())
              .limit(maxCanScramble)
              .forEachOrdered(addTo::add);
        }
      }
    }
  }

  private static void findAttackOptions(
      final ProData proData,
      final GamePlayer player,
      final List<Territory> myUnitTerritories,
      final Map<Territory, ProTerritory> moveMap,
      final Map<Unit, Set<Territory>> unitMoveMap,
      final Map<Unit, Set<Territory>> transportMoveMap,
      final Map<Unit, Set<Territory>> bombardMap,
      final List<ProTransport> transportMapList,
      final List<Territory> enemyTerritories,
      final List<Territory> alliedTerritories,
      final Collection<Territory> territoriesToCheck,
      final boolean isCheckingEnemyAttacks,
      final boolean isIgnoringRelationships) {
    final Map<Territory, Set<Territory>> landRoutesMap = new HashMap<>();
    final List<Territory> territoriesThatCantBeHeld = new ArrayList<>(enemyTerritories);
    territoriesThatCantBeHeld.addAll(territoriesToCheck);
    findNavalMoveOptions(
        proData,
        player,
        myUnitTerritories,
        moveMap,
        unitMoveMap,
        transportMoveMap,
        ProMatches.territoryIsEnemyOrHasEnemyUnitsOrCantBeHeld(player, territoriesThatCantBeHeld),
        enemyTerritories,
        true,
        isCheckingEnemyAttacks);
    findLandMoveOptions(
        proData,
        player,
        myUnitTerritories,
        moveMap,
        unitMoveMap,
        landRoutesMap,
        ProMatches.territoryIsEnemyOrCantBeHeld(player, territoriesThatCantBeHeld),
        enemyTerritories,
        alliedTerritories,
        true,
        isCheckingEnemyAttacks,
        isIgnoringRelationships);
    findAirMoveOptions(
        proData,
        player,
        myUnitTerritories,
        moveMap,
        unitMoveMap,
        ProMatches.territoryHasEnemyUnitsOrCantBeHeld(player, territoriesThatCantBeHeld),
        enemyTerritories,
        alliedTerritories,
        true,
        isCheckingEnemyAttacks,
        isIgnoringRelationships);
    findAmphibMoveOptions(
        proData,
        player,
        myUnitTerritories,
        moveMap,
        transportMapList,
        landRoutesMap,
        ProMatches.territoryIsEnemyOrCantBeHeld(player, territoriesThatCantBeHeld),
        true,
        isCheckingEnemyAttacks,
        isIgnoringRelationships);
    findBombardOptions(
        proData,
        player,
        myUnitTerritories,
        moveMap,
        bombardMap,
        transportMapList,
        isCheckingEnemyAttacks);
  }

  private void findBombingOptions() {
    for (final Unit unit : attackOptions.getUnitMoveMap().keySet()) {
      if (Matches.unitIsStrategicBomber().test(unit)) {
        attackOptions
            .getBomberMoveMap()
            .put(unit, new HashSet<>(attackOptions.getUnitMoveMap().get(unit)));
      }
    }
  }

  private ProOtherMoveOptions findAlliedAttackOptions(final GamePlayer player) {
    final GameState data = proData.getData();

    // Get enemy players in order of turn
    final List<GamePlayer> alliedPlayers = ProUtils.getAlliedPlayersInTurnOrder(player);
    final List<Map<Territory, ProTerritory>> alliedAttackMaps = new ArrayList<>();

    // Loop through each enemy to determine the maximum number of enemy units that can attack each
    // territory
    for (final GamePlayer alliedPlayer : alliedPlayers) {
      final List<Territory> alliedUnitTerritories =
          CollectionUtils.getMatches(
              data.getMap().getTerritories(), Matches.territoryHasUnitsOwnedBy(alliedPlayer));
      final Map<Territory, ProTerritory> attackMap = new HashMap<>();
      final Map<Unit, Set<Territory>> unitAttackMap = new HashMap<>();
      final Map<Unit, Set<Territory>> transportAttackMap = new HashMap<>();
      final Map<Unit, Set<Territory>> bombardMap = new HashMap<>();
      final List<ProTransport> transportMapList = new ArrayList<>();
      alliedAttackMaps.add(attackMap);
      findAttackOptions(
          proData,
          alliedPlayer,
          alliedUnitTerritories,
          attackMap,
          unitAttackMap,
          transportAttackMap,
          bombardMap,
          transportMapList,
          new ArrayList<>(),
          new ArrayList<>(),
          new ArrayList<>(),
          false,
          false);
    }
    return new ProOtherMoveOptions(alliedAttackMaps, player, true);
  }

  private static ProOtherMoveOptions findEnemyAttackOptions(
      final ProData proData,
      final GamePlayer player,
      final Collection<Territory> clearedTerritories,
      final Collection<Territory> territoriesToCheck) {
    final GameState data = proData.getData();

    // Get enemy players in order of turn
    final List<GamePlayer> enemyPlayers = ProUtils.getEnemyPlayersInTurnOrder(player);
    final List<Map<Territory, ProTerritory>> enemyAttackMaps = new ArrayList<>();
    final Set<Territory> alliedTerritories = new HashSet<>();
    final List<Territory> enemyTerritories = new ArrayList<>(clearedTerritories);

    // Loop through each enemy to determine the maximum number of enemy units that can attack each
    // territory
    for (final GamePlayer enemyPlayer : enemyPlayers) {
      final List<Territory> enemyUnitTerritories =
          CollectionUtils.getMatches(
              data.getMap().getTerritories(), Matches.territoryHasUnitsOwnedBy(enemyPlayer));
      enemyUnitTerritories.removeAll(clearedTerritories);
      final Map<Territory, ProTerritory> attackMap = new HashMap<>();
      final Map<Unit, Set<Territory>> unitAttackMap = new HashMap<>();
      final Map<Unit, Set<Territory>> transportAttackMap = new HashMap<>();
      final Map<Unit, Set<Territory>> bombardMap = new HashMap<>();
      final List<ProTransport> transportMapList = new ArrayList<>();
      enemyAttackMaps.add(attackMap);
      findAttackOptions(
          proData,
          enemyPlayer,
          enemyUnitTerritories,
          attackMap,
          unitAttackMap,
          transportAttackMap,
          bombardMap,
          transportMapList,
          enemyTerritories,
          new ArrayList<>(alliedTerritories),
          territoriesToCheck,
          true,
          true);
      alliedTerritories.addAll(
          CollectionUtils.getMatches(attackMap.keySet(), Matches.territoryIsLand()));
      enemyTerritories.removeAll(alliedTerritories);
    }
    return new ProOtherMoveOptions(enemyAttackMaps, player, true);
  }

  private static void findPotentialAttackOptions(
      final ProData proData,
      final GamePlayer player,
      final List<Territory> myUnitTerritories,
      final Map<Territory, ProTerritory> moveMap,
      final Map<Unit, Set<Territory>> unitMoveMap,
      final Map<Unit, Set<Territory>> transportMoveMap,
      final Map<Unit, Set<Territory>> bombardMap,
      final List<ProTransport> transportMapList) {
    final Map<Territory, Set<Territory>> landRoutesMap = new HashMap<>();
    final List<GamePlayer> otherPlayers = ProUtils.getPotentialEnemyPlayers(player);
    findNavalMoveOptions(
        proData,
        player,
        myUnitTerritories,
        moveMap,
        unitMoveMap,
        transportMoveMap,
        ProMatches.territoryIsPotentialEnemyOrHasPotentialEnemyUnits(player, otherPlayers),
        new ArrayList<>(),
        true,
        false);
    findLandMoveOptions(
        proData,
        player,
        myUnitTerritories,
        moveMap,
        unitMoveMap,
        landRoutesMap,
        ProMatches.territoryIsPotentialEnemy(player, otherPlayers),
        new ArrayList<>(),
        new ArrayList<>(),
        true,
        false,
        true);
    findAirMoveOptions(
        proData,
        player,
        myUnitTerritories,
        moveMap,
        unitMoveMap,
        ProMatches.territoryHasPotentialEnemyUnits(player, otherPlayers),
        new ArrayList<>(),
        new ArrayList<>(),
        true,
        false,
        true);
    findAmphibMoveOptions(
        proData,
        player,
        myUnitTerritories,
        moveMap,
        transportMapList,
        landRoutesMap,
        ProMatches.territoryIsPotentialEnemy(player, otherPlayers),
        true,
        false,
        true);
    findBombardOptions(
        proData, player, myUnitTerritories, moveMap, bombardMap, transportMapList, false);
  }

  private static void findDefendOptions(
      final ProData proData,
      final GamePlayer player,
      final List<Territory> myUnitTerritories,
      final Map<Territory, ProTerritory> moveMap,
      final Map<Unit, Set<Territory>> unitMoveMap,
      final Map<Unit, Set<Territory>> transportMoveMap,
      final List<ProTransport> transportMapList,
      final List<Territory> clearedTerritories,
      final boolean isCheckingEnemyAttacks) {
    final Map<Territory, Set<Territory>> landRoutesMap = new HashMap<>();
    findNavalMoveOptions(
        proData,
        player,
        myUnitTerritories,
        moveMap,
        unitMoveMap,
        transportMoveMap,
        ProMatches.territoryHasNoEnemyUnitsOrCleared(player, clearedTerritories),
        clearedTerritories,
        false,
        isCheckingEnemyAttacks);
    findLandMoveOptions(
        proData,
        player,
        myUnitTerritories,
        moveMap,
        unitMoveMap,
        landRoutesMap,
        Matches.isTerritoryAllied(player),
        new ArrayList<>(),
        clearedTerritories,
        false,
        isCheckingEnemyAttacks,
        false);
    findAirMoveOptions(
        proData,
        player,
        myUnitTerritories,
        moveMap,
        unitMoveMap,
        ProMatches.territoryCanLandAirUnits(player, false, new ArrayList<>(), new ArrayList<>()),
        new ArrayList<>(),
        new ArrayList<>(),
        false,
        isCheckingEnemyAttacks,
        false);
    findAmphibMoveOptions(
        proData,
        player,
        myUnitTerritories,
        moveMap,
        transportMapList,
        landRoutesMap,
        Matches.isTerritoryAllied(player),
        false,
        isCheckingEnemyAttacks,
        false);
  }

  private static ProOtherMoveOptions findEnemyDefendOptions(
      final ProData proData, final GamePlayer player) {
    final GameState data = proData.getData();

    // Get enemy players in order of turn
    final List<GamePlayer> enemyPlayers = ProUtils.getEnemyPlayersInTurnOrder(player);
    final List<Map<Territory, ProTerritory>> enemyMoveMaps = new ArrayList<>();
    final List<Territory> clearedTerritories =
        CollectionUtils.getMatches(
            data.getMap().getTerritories(), Matches.isTerritoryAllied(player));

    // Loop through each enemy to determine the maximum number of enemy units that can defend each
    // territory
    for (final GamePlayer enemyPlayer : enemyPlayers) {
      final List<Territory> enemyUnitTerritories =
          CollectionUtils.getMatches(
              data.getMap().getTerritories(), Matches.territoryHasUnitsOwnedBy(enemyPlayer));
      final Map<Territory, ProTerritory> moveMap = new HashMap<>();
      final Map<Unit, Set<Territory>> unitMoveMap = new HashMap<>();
      final Map<Unit, Set<Territory>> transportMoveMap = new HashMap<>();
      final List<ProTransport> transportMapList = new ArrayList<>();
      enemyMoveMaps.add(moveMap);
      findDefendOptions(
          proData,
          enemyPlayer,
          enemyUnitTerritories,
          moveMap,
          unitMoveMap,
          transportMoveMap,
          transportMapList,
          clearedTerritories,
          true);
    }

    return new ProOtherMoveOptions(enemyMoveMaps, player, false);
  }

  private static void findNavalMoveOptions(
      final ProData proData,
      final GamePlayer player,
      final List<Territory> myUnitTerritories,
      final Map<Territory, ProTerritory> moveMap,
      final Map<Unit, Set<Territory>> unitMoveMap,
      final Map<Unit, Set<Territory>> transportMoveMap,
      final Predicate<Territory> moveToTerritoryMatch,
      final List<Territory> clearedTerritories,
      final boolean isCombatMove,
      final boolean isCheckingEnemyAttacks) {
    final GameState data = proData.getData();
    final GameMap gameMap = data.getMap();
    final Predicate<Territory> canMove =
        isCheckingEnemyAttacks
            ? ProMatches.territoryCanMoveSeaUnits(player, isCombatMove)
            : ProMatches.territoryCanMoveSeaUnitsThroughOrClearedAndNotInList(
                player, isCombatMove, clearedTerritories, List.of());
    for (final Territory myUnitTerritory : myUnitTerritories) {
      // Find my naval units that have movement left
      final List<Unit> mySeaUnits =
          myUnitTerritory.getMatches(ProMatches.unitCanBeMovedAndIsOwnedSea(player, isCombatMove));

      // Check each sea unit individually since they can have different ranges
      for (final Unit mySeaUnit : mySeaUnits) {
        // If my combat move and carrier has dependent allied fighters then skip it
        if (isCombatMove && !isCheckingEnemyAttacks) {
          final Collection<Unit> carrierMustMoveWith =
              MoveValidator.carrierMustMoveWith(myUnitTerritory, player).get(mySeaUnit);
          if (carrierMustMoveWith != null && !carrierMustMoveWith.isEmpty()) {
            continue;
          }
        }

        // Find range
        final BigDecimal range =
            getUnitRange(mySeaUnit, myUnitTerritory, player, isCheckingEnemyAttacks);

        // Find list of potential territories to move to
        final Set<Territory> possibleMoveTerritories =
            gameMap.getNeighborsByMovementCost(
                myUnitTerritory, range, ProMatches.territoryCanMoveSeaUnits(player, isCombatMove));
        possibleMoveTerritories.add(myUnitTerritory);
        final Collection<Territory> potentialTerritories =
            CollectionUtils.getMatches(possibleMoveTerritories, moveToTerritoryMatch);
        if (!isCombatMove && !potentialTerritories.contains(myUnitTerritory)) {
          potentialTerritories.add(myUnitTerritory);
        }

        for (final Territory potentialTerritory : potentialTerritories) {
          // Find route over water
          final Route myRoute =
              gameMap.getRouteForUnit(
                  myUnitTerritory, potentialTerritory, canMove, mySeaUnit, player);
          if (myRoute == null) {
            continue;
          }
          final BigDecimal myRouteLength = myRoute.getMovementCost(mySeaUnit);
          if (myRouteLength.compareTo(range) > 0) {
            continue;
          }

          // Populate territories with sea unit
          proData.getProTerritory(moveMap, potentialTerritory).addMaxUnit(mySeaUnit);

          // Populate appropriate unit move options map
          if (Matches.unitIsSeaTransport().test(mySeaUnit)) {
            transportMoveMap
                .computeIfAbsent(mySeaUnit, k -> new HashSet<>())
                .add(potentialTerritory);
          } else {
            unitMoveMap.computeIfAbsent(mySeaUnit, k -> new HashSet<>()).add(potentialTerritory);
          }
        }
      }
    }
  }

  private static void findLandMoveOptions(
      final ProData proData,
      final GamePlayer player,
      final List<Territory> myUnitTerritories,
      final Map<Territory, ProTerritory> moveMap,
      final Map<Unit, Set<Territory>> unitMoveMap,
      final Map<Territory, Set<Territory>> landRoutesMap,
      final Predicate<Territory> moveToTerritoryMatch,
      final List<Territory> enemyTerritories,
      final List<Territory> clearedTerritories,
      final boolean isCombatMove,
      final boolean isCheckingEnemyAttacks,
      final boolean isIgnoringRelationships) {
    final GameData data = proData.getData();
    final GameMap gameMap = data.getMap();

    for (final Territory myUnitTerritory : myUnitTerritories) {

      // Find my land units that have movement left
      final List<Unit> myLandUnits =
          myUnitTerritory.getMatches(ProMatches.unitCanBeMovedAndIsOwnedLand(player, isCombatMove));

      // Check each land unit individually since they can have different ranges
      for (final Unit u : myLandUnits) {
        final Territory startTerritory = proData.getUnitTerritory(u);
        // Should this use getUnitRange()?
        final BigDecimal range = u.getMovementLeft();
        final Set<Territory> possibleMoveTerritories =
            gameMap.getNeighborsByMovementCost(
                myUnitTerritory,
                range,
                isIgnoringRelationships
                    ? ProMatches.territoryCanPotentiallyMoveSpecificLandUnit(player, u)
                    : ProMatches.territoryCanMoveSpecificLandUnit(player, isCombatMove, u));
        possibleMoveTerritories.add(myUnitTerritory);
        final Collection<Territory> potentialTerritories =
            CollectionUtils.getMatches(possibleMoveTerritories, moveToTerritoryMatch);
        if (!isCombatMove && !potentialTerritories.contains(myUnitTerritory)) {
          potentialTerritories.add(myUnitTerritory);
        }
        final Predicate<Territory> canMove =
            isCheckingEnemyAttacks
                ? ProMatches.territoryCanMoveLandUnitsThroughIgnoreEnemyUnits(
                    player, u, startTerritory, isCombatMove, enemyTerritories, clearedTerritories)
                : ProMatches.territoryCanMoveLandUnitsThrough(
                    player, u, startTerritory, isCombatMove, enemyTerritories);
        for (final Territory t : potentialTerritories) {
          // Find route over land checking whether unit can blitz
          if (!isLandMoveOption(isCombatMove, player, u, myUnitTerritory, t, range, canMove)) {
            continue;
          }
          //  System.err.println(u + " -> " + t);

          // Add to route map
          landRoutesMap.computeIfAbsent(t, k -> new HashSet<>()).add(myUnitTerritory);

          // Populate territories with land units
          final ProTerritory potentialTerritoryMove = proData.getProTerritory(moveMap, t);
          final List<Unit> unitsToAdd =
              ProTransportUtils.findBestUnitsToLandTransport(
                  u, startTerritory, potentialTerritoryMove.getMaxUnits());
          potentialTerritoryMove.addMaxUnits(unitsToAdd);

          // Populate unit move options map
          unitMoveMap.computeIfAbsent(u, k -> new HashSet<>()).add(t);
        }
      }
    }
  }

  private static boolean isLandMoveOption(
      final boolean isCombatMove,
      final GamePlayer player,
      final Unit u,
      final Territory from,
      final Territory to,
      final BigDecimal range,
      final Predicate<Territory> canMove) {
    Route r = player.getData().getMap().getRouteForUnit(from, to, canMove, u, player);
    if (r == null) {
      return false;
    }
    if (r.hasMoreThanOneStep()
        && r.getMiddleSteps().stream().anyMatch(Matches.isTerritoryEnemy(player))
        && Matches.unitIsOfTypes(
                TerritoryEffectHelper.getUnitTypesThatLostBlitz(r.getAllTerritories()))
            .test(u)) {
      // If blitzing then make sure none of the territories cause blitz ability to be lost
      return false;
    }
    if (r.getMovementCost(u).compareTo(range) > 0) {
      return false;
    }

    // Skip units that can't participate in combat during combat moves except for land transports.
    if (isCombatMove && !Matches.unitIsLandTransport().test(u)) {
      Collection<Unit> enemyUnits =
          CollectionUtils.getMatches(to.getUnits(), Matches.unitIsEnemyOf(player));
      return Matches.unitCanParticipateInCombat(true, player, to, 1, enemyUnits).test(u);
    }
    return true;
  }

  private static void findAirMoveOptions(
      final ProData proData,
      final GamePlayer player,
      final List<Territory> myUnitTerritories,
      final Map<Territory, ProTerritory> moveMap,
      final Map<Unit, Set<Territory>> unitMoveMap,
      final Predicate<Territory> moveToTerritoryMatch,
      final List<Territory> enemyTerritories,
      final List<Territory> alliedTerritories,
      final boolean isCombatMove,
      final boolean isCheckingEnemyAttacks,
      final boolean isIgnoringRelationships) {
    final GameData data = proData.getData();
    final GameMap gameMap = data.getMap();

    // TODO: add carriers to landing possibilities for non-enemy attacks
    // Find possible carrier landing territories
    final Set<Territory> possibleCarrierTerritories = new HashSet<>();
    if (isCheckingEnemyAttacks || !isCombatMove) {
      final Map<Unit, Set<Territory>> unitMoveMap2 = new HashMap<>();
      findNavalMoveOptions(
          proData,
          player,
          myUnitTerritories,
          new HashMap<>(),
          unitMoveMap2,
          new HashMap<>(),
          Matches.territoryIsWater(),
          enemyTerritories,
          false,
          true);
      for (final Unit u : unitMoveMap2.keySet()) {
        if (Matches.unitIsCarrier().test(u)) {
          possibleCarrierTerritories.addAll(unitMoveMap2.get(u));
        }
      }
      for (final Territory t : gameMap.getTerritories()) {
        if (t.anyUnitsMatch(Matches.unitIsAlliedCarrier(player))) {
          possibleCarrierTerritories.add(t);
        }
      }
    }

    final Predicate<Territory> canMove =
        isIgnoringRelationships
            ? ProMatches.territoryCanPotentiallyMoveAirUnits(player)
            : ProMatches.territoryCanMoveAirUnits(data, player, isCombatMove);
    // Find route ignoring impassable and territories with AA
    final Predicate<Territory> canFlyOverMatch =
        isCheckingEnemyAttacks
            ? ProMatches.territoryCanMoveAirUnits(data, player, isCombatMove)
            : ProMatches.territoryCanMoveAirUnitsAndNoAa(data, player, isCombatMove);

    final Predicate<Unit> unitMatch = ProMatches.unitCanBeMovedAndIsOwnedAir(player, isCombatMove);
    for (final Territory myUnitTerritory : myUnitTerritories) {
      // Find my air units that have movement left
      final List<Unit> myAirUnits = myUnitTerritory.getMatches(unitMatch);

      // Check each air unit individually since they can have different ranges
      for (final Unit myAirUnit : myAirUnits) {
        // Find range
        final BigDecimal range =
            getUnitRange(myAirUnit, myUnitTerritory, player, isCheckingEnemyAttacks);

        // Find potential territories to move to
        final Set<Territory> possibleMoveTerritories =
            gameMap.getNeighborsByMovementCost(myUnitTerritory, range, canMove);
        possibleMoveTerritories.add(myUnitTerritory);
        final Set<Territory> potentialTerritories =
            new HashSet<>(
                CollectionUtils.getMatches(possibleMoveTerritories, moveToTerritoryMatch));
        if (!isCombatMove && Matches.unitCanLandOnCarrier().test(myAirUnit)) {
          potentialTerritories.addAll(
              CollectionUtils.getMatches(
                  possibleMoveTerritories, possibleCarrierTerritories::contains));
        }

        for (final Territory potentialTerritory : potentialTerritories) {
          final Route myRoute =
              gameMap.getRouteForUnit(
                  myUnitTerritory, potentialTerritory, canFlyOverMatch, myAirUnit, player);
          if (myRoute == null) {
            continue;
          }
          final BigDecimal myRouteLength = myRoute.getMovementCost(myAirUnit);
          final BigDecimal remainingMoves = range.subtract(myRouteLength);
          if (remainingMoves.compareTo(BigDecimal.ZERO) < 0) {
            continue;
          }

          // Check if unit can land
          if (isCombatMove
              && (remainingMoves.compareTo(myRouteLength) < 0 || myUnitTerritory.isWater())) {
            final Set<Territory> possibleLandingTerritories =
                gameMap.getNeighborsByMovementCost(
                    potentialTerritory, remainingMoves, canFlyOverMatch);
            final List<Territory> landingTerritories =
                CollectionUtils.getMatches(
                    possibleLandingTerritories,
                    ProMatches.territoryCanLandAirUnits(
                        player, isCombatMove, enemyTerritories, alliedTerritories));
            List<Territory> carrierTerritories = List.of();
            if (Matches.unitCanLandOnCarrier().test(myAirUnit)) {
              carrierTerritories =
                  CollectionUtils.getMatches(
                      possibleLandingTerritories, possibleCarrierTerritories::contains);
            }
            if (landingTerritories.isEmpty() && carrierTerritories.isEmpty()) {
              continue;
            }
          }

          // Populate enemy territories with air unit
          proData.getProTerritory(moveMap, potentialTerritory).addMaxUnit(myAirUnit);

          // Populate unit attack options map
          unitMoveMap.computeIfAbsent(myAirUnit, k -> new HashSet<>()).add(potentialTerritory);
        }
      }
    }
  }

  private static void findAmphibMoveOptions(
      final ProData proData,
      final GamePlayer player,
      final List<Territory> myUnitTerritories,
      final Map<Territory, ProTerritory> moveMap,
      final List<ProTransport> transportMapList,
      final Map<Territory, Set<Territory>> landRoutesMap,
      final Predicate<Territory> moveAmphibToTerritoryMatch,
      final boolean isCombatMove,
      final boolean isCheckingEnemyAttacks,
      final boolean isIgnoringRelationships) {
    final GameData data = proData.getData();
    final GameMap map = data.getMap();
    final Predicate<Unit> isTransport =
        ProMatches.unitCanBeMovedAndIsOwnedTransport(player, isCombatMove);
    final Predicate<Territory> canMoveSeaThrough =
        ProMatches.territoryCanMoveSeaUnitsThrough(player, isCombatMove);
    final Predicate<Territory> canMoveSea =
        ProMatches.territoryCanMoveSeaUnits(player, isCombatMove);
    Predicate<Territory> unloadAmphibTerritoryMatch;
    if (isIgnoringRelationships) {
      unloadAmphibTerritoryMatch = ProMatches.territoryCanPotentiallyMoveLandUnits(player);
    } else {
      unloadAmphibTerritoryMatch = ProMatches.territoryCanMoveLandUnits(player, isCombatMove);
    }
    unloadAmphibTerritoryMatch = unloadAmphibTerritoryMatch.and(moveAmphibToTerritoryMatch);

    for (final Territory transportTerritory : myUnitTerritories) {
      // Find my transports and amphibious units that have movement left
      final List<Unit> transports = transportTerritory.getMatches(isTransport);

      // Check each transport unit individually since they can have different ranges
      for (final Unit transport : transports) {
        // Find units to load and territories to unload
        final ProTransport proTransportData = new ProTransport(transport);
        transportMapList.add(proTransportData);
        final Set<Territory> currentTerritories = new HashSet<>();
        currentTerritories.add(transportTerritory);
        final Predicate<Unit> canBeTransported =
            isCheckingEnemyAttacks
                ? ProMatches.unitIsOwnedCombatTransportableUnit(player)
                : ProMatches.unitIsOwnedTransportableUnitAndCanBeLoaded(
                    player, transport, isCombatMove);
        // Get remaining moves
        int movesLeft =
            getUnitRange(transport, transportTerritory, player, isCheckingEnemyAttacks).intValue();
        MoveValidator moveValidator = new MoveValidator(data, !isCombatMove);
        while (movesLeft >= 0) {
          final Set<Territory> nextTerritories = new HashSet<>();
          for (final Territory from : currentTerritories) {
            // Find neighbors I can move to
            for (final Territory neighbor : map.getNeighbors(from, canMoveSeaThrough)) {
              final Route route = new Route(from, neighbor);
              if (moveValidator.validateCanal(route, List.of(transport), player) == null) {
                nextTerritories.add(neighbor);
              }
            }

            // Get loaded units or get units that can be loaded into current territory if no enemies
            // present
            boolean haveUnitsToTransport = false;
            final Set<Territory> loadFromTerritories = new HashSet<>();
            if (!transport.getTransporting(transportTerritory).isEmpty()) {
              haveUnitsToTransport = true;
            } else if (Matches.territoryHasEnemySeaUnits(player).negate().test(from)) {
              int capacity = transport.getUnitAttachment().getTransportCapacity();
              Predicate<Unit> canFitOnTransport =
                  canBeTransported.and(u -> u.getUnitAttachment().getTransportCost() <= capacity);
              for (Territory loadTerritory : map.getNeighbors(from)) {
                if (loadTerritory.anyUnitsMatch(canFitOnTransport)) {
                  loadFromTerritories.add(loadTerritory);
                  haveUnitsToTransport = true;
                }
              }
            }

            // If there are any units to be transported
            if (!haveUnitsToTransport) {
              continue;
            }
            // Find all water territories I can move to
            final Set<Territory> seaMoveTerritories = new HashSet<>();
            seaMoveTerritories.add(from);
            if (movesLeft > 0) {
              final Predicate<Territory> canMove =
                  isCheckingEnemyAttacks ? canMoveSea : canMoveSeaThrough;
              for (final Territory to : map.getNeighbors(from, movesLeft, canMove)) {
                if (map.getRouteForUnit(from, to, canMoveSeaThrough, transport, player) != null) {
                  seaMoveTerritories.add(to);
                }
              }
            }
            // Find possible unload territories
            final Set<Territory> unloadTerritories = new HashSet<>();
            for (final Territory to : seaMoveTerritories) {
              unloadTerritories.addAll(map.getNeighbors(to, unloadAmphibTerritoryMatch));
            }

            // Add to transport map
            proTransportData.addTerritories(unloadTerritories, loadFromTerritories);
            proTransportData.addSeaTerritories(seaMoveTerritories, loadFromTerritories);
          }
          currentTerritories.clear();
          currentTerritories.addAll(nextTerritories);
          movesLeft--;
        }
      }
    }

    // Remove any territories from transport map that I can move to on land and transports with no
    // amphib options
    for (final ProTransport proTransportData : transportMapList) {
      final Map<Territory, Set<Territory>> transportMap = proTransportData.getTransportMap();
      for (final Territory t : transportMap.keySet()) {
        final Set<Territory> landMoveTerritories = landRoutesMap.get(t);
        if (landMoveTerritories != null) {
          transportMap.get(t).removeAll(landMoveTerritories);
        }
      }
      transportMap.values().removeIf(Collection::isEmpty);
    }

    // Add transport units to attack map
    for (final ProTransport proTransportData : transportMapList) {
      final Map<Territory, Set<Territory>> transportMap = proTransportData.getTransportMap();
      final Unit transport = proTransportData.getTransport();
      for (final Territory moveTerritory : transportMap.keySet()) {

        // Get units to transport
        final Set<Territory> territoriesCanLoadFrom = transportMap.get(moveTerritory);
        List<Unit> alreadyAddedToMaxAmphibUnits = new ArrayList<>();
        if (moveMap.containsKey(moveTerritory)) {
          alreadyAddedToMaxAmphibUnits = moveMap.get(moveTerritory).getMaxAmphibUnits();
        }
        final List<Unit> amphibUnits =
            isCheckingEnemyAttacks
                ? ProTransportUtils.getUnitsToTransportFromTerritories(
                    player,
                    transport,
                    territoriesCanLoadFrom,
                    alreadyAddedToMaxAmphibUnits,
                    ProMatches.unitIsOwnedCombatTransportableUnit(player))
                : ProTransportUtils.getUnitsToTransportFromTerritories(
                    player, transport, territoriesCanLoadFrom, alreadyAddedToMaxAmphibUnits);

        // Add amphib units to attack map
        proData.getProTerritory(moveMap, moveTerritory).addMaxAmphibUnits(amphibUnits);
      }
    }
  }

  private static void findBombardOptions(
      final ProData proData,
      final GamePlayer player,
      final List<Territory> myUnitTerritories,
      final Map<Territory, ProTerritory> moveMap,
      final Map<Unit, Set<Territory>> bombardMap,
      final List<ProTransport> transportMapList,
      final boolean isCheckingEnemyAttacks) {
    final GameState data = proData.getData();
    final GameMap gameMap = data.getMap();

    // Find all transport unload from and to territories
    final Set<Territory> unloadFromTerritories = new HashSet<>();
    final Set<Territory> unloadToTerritories = new HashSet<>();
    for (final ProTransport amphibData : transportMapList) {
      unloadFromTerritories.addAll(amphibData.getSeaTransportMap().keySet());
      unloadToTerritories.addAll(amphibData.getTransportMap().keySet());
    }

    final Predicate<Territory> canMove =
        isCheckingEnemyAttacks
            ? ProMatches.territoryCanMoveSeaUnits(player, true)
            : ProMatches.territoryCanMoveSeaUnitsThrough(player, true);

    // Loop through territories with my units
    for (final Territory myUnitTerritory : myUnitTerritories) {
      // Find my bombard units that have movement left
      final List<Unit> mySeaUnits =
          myUnitTerritory.getMatches(ProMatches.unitCanBeMovedAndIsOwnedBombard(player));

      // Check each sea unit individually since they can have different ranges
      for (final Unit mySeaUnit : mySeaUnits) {
        // Find range
        final BigDecimal range =
            getUnitRange(mySeaUnit, myUnitTerritory, player, isCheckingEnemyAttacks);

        // Find list of potential territories to move to
        final Set<Territory> potentialTerritories =
            gameMap.getNeighborsByMovementCost(
                myUnitTerritory, range, ProMatches.territoryCanMoveSeaUnits(player, true));
        potentialTerritories.add(myUnitTerritory);
        potentialTerritories.retainAll(unloadFromTerritories);
        for (final Territory bombardFromTerritory : potentialTerritories) {
          // Find route over water with no enemy units blocking
          final Route myRoute =
              gameMap.getRouteForUnit(
                  myUnitTerritory, bombardFromTerritory, canMove, mySeaUnit, player);
          if (myRoute == null) {
            continue;
          }
          final BigDecimal myRouteLength = myRoute.getMovementCost(mySeaUnit);
          if (myRouteLength.compareTo(range) > 0) {
            continue;
          }

          // Find potential unload to territories
          final Set<Territory> bombardToTerritories =
              new HashSet<>(gameMap.getNeighbors(bombardFromTerritory));
          bombardToTerritories.retainAll(unloadToTerritories);

          // Populate attack territories with bombard unit
          for (final Territory bombardToTerritory : bombardToTerritories) {
            if (moveMap.containsKey(bombardToTerritory)) { // Should always contain it
              moveMap.get(bombardToTerritory).addMaxBombardUnit(mySeaUnit);
              moveMap.get(bombardToTerritory).addBombardOptionsMap(mySeaUnit, bombardFromTerritory);
            }
          }

          // Populate bombard options map
          bombardMap.computeIfAbsent(mySeaUnit, k -> new HashSet<>()).addAll(bombardToTerritories);
        }
      }
    }
  }

  private static BigDecimal getUnitRange(
      final Unit unit,
      final Territory unitTerritory,
      final GamePlayer player,
      final boolean isCheckingEnemyAttacks) {
    if (isCheckingEnemyAttacks) {
      final BigDecimal range = new BigDecimal(unit.getUnitAttachment().getMovement(player));
      if (Matches.unitCanBeGivenBonusMovementByFacilitiesInItsTerritory(unitTerritory, player)
          .test(unit)) {
        return range.add(BigDecimal.ONE); // assumes bonus of +1 for now
      }
      return range;
    }
    return unit.getMovementLeft();
  }

  public Optional<Territory> findClosestTerritory(
      Collection<Territory> fromTerritories,
      Predicate<Territory> canMove,
      Predicate<Territory> isDestination) {
    BreadthFirstSearch bfs = new BreadthFirstSearch(fromTerritories, canMove.or(isDestination));
    MutableObject<Territory> destination = new MutableObject<>();
    bfs.traverse(
        (territory, distance) -> {
          if (isDestination.test(territory)) {
            destination.setValue(territory);
            return false;
          }
          return true;
        });
    return Optional.ofNullable(destination.getValue());
  }
}
