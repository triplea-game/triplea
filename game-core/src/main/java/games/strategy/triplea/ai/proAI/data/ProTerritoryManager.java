package games.strategy.triplea.ai.proAI.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.ai.proAI.ProData;
import games.strategy.triplea.ai.proAI.logging.ProLogger;
import games.strategy.triplea.ai.proAI.util.ProBattleUtils;
import games.strategy.triplea.ai.proAI.util.ProMatches;
import games.strategy.triplea.ai.proAI.util.ProOddsCalculator;
import games.strategy.triplea.ai.proAI.util.ProTransportUtils;
import games.strategy.triplea.ai.proAI.util.ProUtils;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveValidator;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.util.CollectionUtils;
import games.strategy.util.PredicateBuilder;

/**
 * Manages info about territories.
 */
public class ProTerritoryManager {

  private final ProOddsCalculator calc;
  private final PlayerID player;

  private ProMyMoveOptions attackOptions;
  private ProMyMoveOptions potentialAttackOptions;
  private ProMyMoveOptions defendOptions;
  private ProOtherMoveOptions alliedAttackOptions;
  private ProOtherMoveOptions enemyDefendOptions;
  private ProOtherMoveOptions enemyAttackOptions;

  public ProTerritoryManager(final ProOddsCalculator calc) {
    this.calc = calc;
    player = ProData.getPlayer();
    attackOptions = new ProMyMoveOptions();
    potentialAttackOptions = new ProMyMoveOptions();
    defendOptions = new ProMyMoveOptions();
    alliedAttackOptions = new ProOtherMoveOptions();
    enemyDefendOptions = new ProOtherMoveOptions();
    enemyAttackOptions = new ProOtherMoveOptions();
  }

  public ProTerritoryManager(final ProOddsCalculator calc, final ProTerritoryManager territoryManager) {
    this(calc);
    attackOptions = new ProMyMoveOptions(territoryManager.attackOptions);
    potentialAttackOptions = new ProMyMoveOptions(territoryManager.potentialAttackOptions);
    defendOptions = new ProMyMoveOptions(territoryManager.defendOptions);
    alliedAttackOptions = territoryManager.getAlliedAttackOptions();
    enemyDefendOptions = territoryManager.getEnemyDefendOptions();
    enemyAttackOptions = territoryManager.getEnemyAttackOptions();
  }

  public void populateAttackOptions() {
    findAttackOptions(player, ProData.myUnitTerritories, attackOptions.getTerritoryMap(),
        attackOptions.getUnitMoveMap(), attackOptions.getTransportMoveMap(), attackOptions.getBombardMap(),
        attackOptions.getTransportList(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), false, false);
    findBombingOptions();
    alliedAttackOptions = findAlliedAttackOptions(player);
  }

  public void populatePotentialAttackOptions() {
    findPotentialAttackOptions(player, ProData.myUnitTerritories, potentialAttackOptions.getTerritoryMap(),
        potentialAttackOptions.getUnitMoveMap(), potentialAttackOptions.getTransportMoveMap(),
        potentialAttackOptions.getBombardMap(), potentialAttackOptions.getTransportList());
  }

  public void populateDefenseOptions(final List<Territory> clearedTerritories) {
    findDefendOptions(player, ProData.myUnitTerritories, defendOptions.getTerritoryMap(),
        defendOptions.getUnitMoveMap(), defendOptions.getTransportMoveMap(), defendOptions.getTransportList(),
        clearedTerritories, false);
  }

  public void populateEnemyAttackOptions(final List<Territory> clearedTerritories,
      final List<Territory> territoriesToCheck) {
    enemyAttackOptions = findEnemyAttackOptions(player, clearedTerritories, territoriesToCheck);
  }

  public void populateEnemyDefenseOptions() {
    findScrambleOptions(player, attackOptions.getTerritoryMap());
    enemyDefendOptions = findEnemyDefendOptions(player);
  }

  public List<ProTerritory> removeTerritoriesThatCantBeConquered() {
    return removeTerritoriesThatCantBeConquered(player, attackOptions.getTerritoryMap(), attackOptions.getUnitMoveMap(),
        attackOptions.getTransportMoveMap(), alliedAttackOptions, enemyDefendOptions, false);
  }

  private List<ProTerritory> removeTerritoriesThatCantBeConquered(final PlayerID player,
      final Map<Territory, ProTerritory> attackMap, final Map<Unit, Set<Territory>> unitAttackMap,
      final Map<Unit, Set<Territory>> transportAttackMap, final ProOtherMoveOptions alliedAttackOptions,
      final ProOtherMoveOptions enemyDefendOptions, final boolean isIgnoringRelationships) {

    ProLogger.info("Removing territories that can't be conquered");
    final GameData data = ProData.getData();

    // Determine if territory can be successfully attacked with max possible attackers
    final List<Territory> territoriesToRemove = new ArrayList<>();
    for (final Territory t : attackMap.keySet()) {
      final ProTerritory patd = attackMap.get(t);

      // Check if I can win without amphib units and ignore AA since max units might have lots of planes
      List<Unit> defenders = CollectionUtils.getMatches(patd.getMaxEnemyDefenders(player, data),
          ProMatches.unitIsEnemyAndNotAa(player, data));
      if (isIgnoringRelationships) {
        defenders = new ArrayList<>(t.getUnits());
      }
      patd.setMaxBattleResult(calc.estimateAttackBattleResults(t, patd.getMaxUnits(), defenders, new HashSet<>()));

      // Add in amphib units if I can't win without them
      if (patd.getMaxBattleResult().getWinPercentage() < ProData.winPercentage && !patd.getMaxAmphibUnits().isEmpty()) {
        final Set<Unit> combinedUnits = new HashSet<>(patd.getMaxUnits());
        combinedUnits.addAll(patd.getMaxAmphibUnits());
        patd.setMaxBattleResult(calc.estimateAttackBattleResults(t, new ArrayList<>(combinedUnits), defenders,
            patd.getMaxBombardUnits()));
        patd.setNeedAmphibUnits(true);
      }

      // Check strafing and using allied attack if enemy capital/factory
      boolean isEnemyCapitalOrFactory = false;
      final TerritoryAttachment ta = TerritoryAttachment.get(t);
      if (!t.getOwner().isNull()
          && ((ta != null && ta.isCapital()) || ProMatches.territoryHasInfraFactoryAndIsLand().test(t))) {
        isEnemyCapitalOrFactory = true;
      }
      if (patd.getMaxBattleResult().getWinPercentage() < ProData.minWinPercentage && isEnemyCapitalOrFactory
          && alliedAttackOptions.getMax(t) != null) {

        // Check for allied attackers
        final ProTerritory alliedAttack = alliedAttackOptions.getMax(t);
        final Set<Unit> alliedUnits = new HashSet<>(alliedAttack.getMaxUnits());
        alliedUnits.addAll(alliedAttack.getMaxAmphibUnits());
        if (!alliedUnits.isEmpty()) {

          // Make sure allies' capital isn't next to territory
          final PlayerID alliedPlayer = alliedUnits.iterator().next().getOwner();
          final Territory capital = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(alliedPlayer, data);
          if (capital != null && !data.getMap().getNeighbors(capital).contains(t)) {

            // Get max enemy defenders
            final Set<Unit> additionalEnemyDefenders = new HashSet<>();
            final List<PlayerID> players = ProUtils.getOtherPlayersInTurnOrder(player);
            for (final ProTerritory enemyDefendOption : enemyDefendOptions.getAll(t)) {
              final Set<Unit> enemyUnits = new HashSet<>(enemyDefendOption.getMaxUnits());
              enemyUnits.addAll(enemyDefendOption.getMaxAmphibUnits());
              if (!enemyUnits.isEmpty()) {
                final PlayerID enemyPlayer = enemyUnits.iterator().next().getOwner();
                if (ProUtils.isPlayersTurnFirst(players, enemyPlayer, alliedPlayer)) {
                  additionalEnemyDefenders.addAll(enemyUnits);
                }
              }
            }

            // Check allied result without strafe
            final Set<Unit> enemyDefendersBeforeStrafe = new HashSet<>(defenders);
            enemyDefendersBeforeStrafe.addAll(additionalEnemyDefenders);
            final ProBattleResult result =
                calc.estimateAttackBattleResults(t, new ArrayList<>(alliedUnits),
                    new ArrayList<>(enemyDefendersBeforeStrafe), alliedAttack.getMaxBombardUnits());
            if (result.getWinPercentage() < ProData.winPercentage) {
              patd.setStrafing(true);

              // Try to strafe to allow allies to conquer territory
              final Set<Unit> combinedUnits = new HashSet<>(patd.getMaxUnits());
              combinedUnits.addAll(patd.getMaxAmphibUnits());
              final ProBattleResult strafeResult = calc.callBattleCalculator(t, new ArrayList<>(combinedUnits),
                  defenders, patd.getMaxBombardUnits(), true);

              // Check allied result with strafe
              final Set<Unit> enemyDefendersAfterStrafe = new HashSet<>(strafeResult.getAverageDefendersRemaining());
              enemyDefendersAfterStrafe.addAll(additionalEnemyDefenders);
              patd.setMaxBattleResult(calc.estimateAttackBattleResults(t, new ArrayList<>(alliedUnits),
                  new ArrayList<>(enemyDefendersAfterStrafe), alliedAttack.getMaxBombardUnits()));

              ProLogger.debug("Checking strafing territory: " + t + ", alliedPlayer="
                  + alliedUnits.iterator().next().getOwner().getName() + ", maxWin%="
                  + patd.getMaxBattleResult().getWinPercentage() + ", maxAttackers=" + alliedUnits.size()
                  + ", maxDefenders=" + enemyDefendersAfterStrafe.size());
            }
          }
        }
      }

      if (patd.getMaxBattleResult().getWinPercentage() < ProData.minWinPercentage
          || (patd.isStrafing() && (patd.getMaxBattleResult().getWinPercentage() < ProData.winPercentage
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
      ProLogger.debug("Removing territory that we can't successfully attack: " + t + ", maxWin%="
          + proTerritoryToRemove.getMaxBattleResult().getWinPercentage() + ", maxAttackers=" + combinedUnits.size());
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
    return removeTerritoriesThatCantBeConquered(player, potentialAttackOptions.getTerritoryMap(),
        potentialAttackOptions.getUnitMoveMap(), potentialAttackOptions.getTransportMoveMap(), alliedAttackOptions,
        enemyDefendOptions, true);
  }

  public ProMyMoveOptions getAttackOptions() {
    return attackOptions;
  }

  public ProMyMoveOptions getDefendOptions() {
    return defendOptions;
  }

  public ProOtherMoveOptions getAlliedAttackOptions() {
    return alliedAttackOptions;
  }

  public ProOtherMoveOptions getEnemyDefendOptions() {
    return enemyDefendOptions;
  }

  public ProOtherMoveOptions getEnemyAttackOptions() {
    return enemyAttackOptions;
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
      movedTransports.addAll(CollectionUtils.getMatches(patd.getUnits(), Matches.unitIsTransport()));
    }
    return movedTransports.size() >= attackOptions.getTransportList().size();
  }

  private static void findScrambleOptions(final PlayerID player, final Map<Territory, ProTerritory> moveMap) {
    final GameData data = ProData.getData();

    if (!Properties.getScrambleRulesInEffect(data)) {
      return;
    }

    // Find scramble properties
    final boolean fromIslandOnly = Properties.getScrambleFromIslandOnly(data);
    final boolean toSeaOnly = Properties.getScrambleToSeaOnly(data);
    final int maxScrambleDistance = StreamSupport.stream(data.getUnitTypeList().spliterator(), false)
        .map(UnitAttachment::get)
        .filter(UnitAttachment::getCanScramble)
        .mapToInt(UnitAttachment::getMaxScrambleDistance)
        .max()
        .orElse(0);
    final Predicate<Unit> airbasesCanScramble = Matches.unitIsEnemyOf(data, player)
        .and(Matches.unitIsAirBase())
        .and(Matches.unitIsNotDisabled())
        .and(Matches.unitIsBeingTransported().negate());
    final Predicate<Territory> canScramble = PredicateBuilder
        .of(Matches.territoryIsWater().or(Matches.isTerritoryEnemy(player, data)))
        .and(Matches.territoryHasUnitsThatMatch(Matches.unitCanScramble()
            .and(Matches.unitIsEnemyOf(data, player))
            .and(Matches.unitIsNotDisabled())))
        .and(Matches.territoryHasUnitsThatMatch(airbasesCanScramble))
        .andIf(fromIslandOnly, Matches.territoryIsIsland())
        .build();

    // Find potential territories to scramble from
    final HashMap<Territory, HashSet<Territory>> scrambleTerrs = new HashMap<>();
    for (final Territory t : moveMap.keySet()) {
      if (t.isWater() || !toSeaOnly) {
        final HashSet<Territory> canScrambleFrom = new HashSet<>(
            CollectionUtils.getMatches(data.getMap().getNeighbors(t, maxScrambleDistance), canScramble));
        if (!canScrambleFrom.isEmpty()) {
          scrambleTerrs.put(t, canScrambleFrom);
        }
      }
    }
    if (scrambleTerrs.isEmpty()) {
      return;
    }

    // Find potential max units that can be scrambled to each territory
    for (final Territory to : scrambleTerrs.keySet()) {
      for (final Territory from : scrambleTerrs.get(to)) {

        // Find potential scramble units from territory
        final Collection<Unit> airbases = from.getUnits().getMatches(airbasesCanScramble);
        final int maxCanScramble = getMaxScrambleCount(airbases);
        final Route toBattleRoute = data.getMap().getRoute_IgnoreEnd(from, to, Matches.territoryIsNotImpassable());
        List<Unit> canScrambleAir = from.getUnits().getMatches(Matches.unitIsEnemyOf(data, player)
            .and(Matches.unitCanScramble())
            .and(Matches.unitIsNotDisabled())
            .and(Matches.unitWasScrambled().negate())
            .and(Matches.unitCanScrambleOnRouteDistance(toBattleRoute)));

        // Add max scramble units
        if (maxCanScramble > 0 && !canScrambleAir.isEmpty()) {
          if (maxCanScramble < canScrambleAir.size()) {
            Collections.sort(canScrambleAir, (o1, o2) -> {
              final double strength1 =
                  ProBattleUtils.estimateStrength(to, Collections.singletonList(o1), new ArrayList<>(), false);
              final double strength2 =
                  ProBattleUtils.estimateStrength(to, Collections.singletonList(o2), new ArrayList<>(), false);
              return Double.compare(strength2, strength1);
            });
            canScrambleAir = canScrambleAir.subList(0, maxCanScramble);
          }
          moveMap.get(to).getMaxScrambleUnits().addAll(canScrambleAir);
        }
      }
    }
  }

  private static int getMaxScrambleCount(final Collection<Unit> airbases) {
    if (airbases.isEmpty() || !airbases.stream().allMatch(Matches.unitIsAirBase().and(Matches.unitIsNotDisabled()))) {
      throw new IllegalStateException("All units must be viable airbases");
    }

    // find how many is the max this territory can scramble
    int maxScrambled = 0;
    for (final Unit base : airbases) {
      final UnitAttachment ua = UnitAttachment.get(base.getType());
      final int baseMax = ua.getMaxScrambleCount();
      if (baseMax == -1) {
        return Integer.MAX_VALUE;
      }
      maxScrambled += baseMax;
    }
    return maxScrambled;
  }

  private static void findAttackOptions(final PlayerID player, final List<Territory> myUnitTerritories,
      final Map<Territory, ProTerritory> moveMap, final Map<Unit, Set<Territory>> unitMoveMap,
      final Map<Unit, Set<Territory>> transportMoveMap, final Map<Unit, Set<Territory>> bombardMap,
      final List<ProTransport> transportMapList, final List<Territory> enemyTerritories,
      final List<Territory> alliedTerritories, final List<Territory> territoriesToCheck,
      final boolean isCheckingEnemyAttacks, final boolean isIgnoringRelationships) {
    final GameData data = ProData.getData();

    final Map<Territory, Set<Territory>> landRoutesMap = new HashMap<>();
    final List<Territory> territoriesThatCantBeHeld = new ArrayList<>(enemyTerritories);
    territoriesThatCantBeHeld.addAll(territoriesToCheck);
    findNavalMoveOptions(player, myUnitTerritories, moveMap, unitMoveMap, transportMoveMap,
        ProMatches.territoryIsEnemyOrHasEnemyUnitsOrCantBeHeld(player, data, territoriesThatCantBeHeld),
        enemyTerritories, true, isCheckingEnemyAttacks);
    findLandMoveOptions(player, myUnitTerritories, moveMap, unitMoveMap, landRoutesMap,
        ProMatches.territoryIsEnemyOrCantBeHeld(player, data, territoriesThatCantBeHeld), enemyTerritories,
        alliedTerritories, true, isCheckingEnemyAttacks, isIgnoringRelationships);
    findAirMoveOptions(player, myUnitTerritories, moveMap, unitMoveMap,
        ProMatches.territoryHasEnemyUnitsOrCantBeHeld(player, data, territoriesThatCantBeHeld), enemyTerritories,
        alliedTerritories, true, isCheckingEnemyAttacks, isIgnoringRelationships);
    findAmphibMoveOptions(player, myUnitTerritories, moveMap, transportMapList, landRoutesMap,
        ProMatches.territoryIsEnemyOrCantBeHeld(player, data, territoriesThatCantBeHeld), true,
        isCheckingEnemyAttacks, isIgnoringRelationships);
    findBombardOptions(player, myUnitTerritories, moveMap, bombardMap, transportMapList, isCheckingEnemyAttacks);
  }

  private void findBombingOptions() {
    for (final Unit unit : attackOptions.getUnitMoveMap().keySet()) {
      if (Matches.unitIsStrategicBomber().test(unit)) {
        attackOptions.getBomberMoveMap().put(unit, new HashSet<>(attackOptions.getUnitMoveMap().get(unit)));
      }
    }
  }

  private static ProOtherMoveOptions findAlliedAttackOptions(final PlayerID player) {
    final GameData data = ProData.getData();

    // Get enemy players in order of turn
    final List<PlayerID> alliedPlayers = ProUtils.getAlliedPlayersInTurnOrder(player);
    final List<Map<Territory, ProTerritory>> alliedAttackMaps = new ArrayList<>();

    // Loop through each enemy to determine the maximum number of enemy units that can attack each territory
    for (final PlayerID alliedPlayer : alliedPlayers) {
      final List<Territory> alliedUnitTerritories =
          CollectionUtils.getMatches(data.getMap().getTerritories(), Matches.territoryHasUnitsOwnedBy(alliedPlayer));
      final Map<Territory, ProTerritory> attackMap = new HashMap<>();
      final Map<Unit, Set<Territory>> unitAttackMap = new HashMap<>();
      final Map<Unit, Set<Territory>> transportAttackMap = new HashMap<>();
      final Map<Unit, Set<Territory>> bombardMap = new HashMap<>();
      final List<ProTransport> transportMapList = new ArrayList<>();
      alliedAttackMaps.add(attackMap);
      findAttackOptions(alliedPlayer, alliedUnitTerritories, attackMap, unitAttackMap, transportAttackMap, bombardMap,
          transportMapList, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), false, false);
    }
    return new ProOtherMoveOptions(alliedAttackMaps, player, true);
  }

  private static ProOtherMoveOptions findEnemyAttackOptions(final PlayerID player,
      final List<Territory> clearedTerritories, final List<Territory> territoriesToCheck) {
    final GameData data = ProData.getData();

    // Get enemy players in order of turn
    final List<PlayerID> enemyPlayers = ProUtils.getEnemyPlayersInTurnOrder(player);
    final List<Map<Territory, ProTerritory>> enemyAttackMaps = new ArrayList<>();
    final Set<Territory> alliedTerritories = new HashSet<>();
    final List<Territory> enemyTerritories = new ArrayList<>(clearedTerritories);

    // Loop through each enemy to determine the maximum number of enemy units that can attack each territory
    for (final PlayerID enemyPlayer : enemyPlayers) {
      final List<Territory> enemyUnitTerritories =
          CollectionUtils.getMatches(data.getMap().getTerritories(), Matches.territoryHasUnitsOwnedBy(enemyPlayer));
      enemyUnitTerritories.removeAll(clearedTerritories);
      final Map<Territory, ProTerritory> attackMap = new HashMap<>();
      final Map<Unit, Set<Territory>> unitAttackMap = new HashMap<>();
      final Map<Unit, Set<Territory>> transportAttackMap = new HashMap<>();
      final Map<Unit, Set<Territory>> bombardMap = new HashMap<>();
      final List<ProTransport> transportMapList = new ArrayList<>();
      enemyAttackMaps.add(attackMap);
      findAttackOptions(enemyPlayer, enemyUnitTerritories, attackMap, unitAttackMap, transportAttackMap, bombardMap,
          transportMapList, enemyTerritories, new ArrayList<>(alliedTerritories), territoriesToCheck, true, true);
      alliedTerritories.addAll(CollectionUtils.getMatches(attackMap.keySet(), Matches.territoryIsLand()));
      enemyTerritories.removeAll(alliedTerritories);
    }
    return new ProOtherMoveOptions(enemyAttackMaps, player, true);
  }

  private static void findPotentialAttackOptions(final PlayerID player, final List<Territory> myUnitTerritories,
      final Map<Territory, ProTerritory> moveMap, final Map<Unit, Set<Territory>> unitMoveMap,
      final Map<Unit, Set<Territory>> transportMoveMap, final Map<Unit, Set<Territory>> bombardMap,
      final List<ProTransport> transportMapList) {
    final GameData data = ProData.getData();

    final Map<Territory, Set<Territory>> landRoutesMap = new HashMap<>();
    final List<PlayerID> otherPlayers = ProUtils.getPotentialEnemyPlayers(player);
    findNavalMoveOptions(player, myUnitTerritories, moveMap, unitMoveMap, transportMoveMap,
        ProMatches.territoryIsPotentialEnemyOrHasPotentialEnemyUnits(player, data, otherPlayers), new ArrayList<>(),
        true, false);
    findLandMoveOptions(player, myUnitTerritories, moveMap, unitMoveMap, landRoutesMap,
        ProMatches.territoryIsPotentialEnemy(player, data, otherPlayers), new ArrayList<>(), new ArrayList<>(), true,
        false, true);
    findAirMoveOptions(player, myUnitTerritories, moveMap, unitMoveMap,
        ProMatches.territoryHasPotentialEnemyUnits(player, data, otherPlayers), new ArrayList<>(), new ArrayList<>(),
        true, false, true);
    findAmphibMoveOptions(player, myUnitTerritories, moveMap, transportMapList, landRoutesMap,
        ProMatches.territoryIsPotentialEnemy(player, data, otherPlayers), true, false, true);
    findBombardOptions(player, myUnitTerritories, moveMap, bombardMap, transportMapList, false);
  }

  private static void findDefendOptions(final PlayerID player, final List<Territory> myUnitTerritories,
      final Map<Territory, ProTerritory> moveMap, final Map<Unit, Set<Territory>> unitMoveMap,
      final Map<Unit, Set<Territory>> transportMoveMap, final List<ProTransport> transportMapList,
      final List<Territory> clearedTerritories, final boolean isCheckingEnemyAttacks) {
    final GameData data = ProData.getData();

    final Map<Territory, Set<Territory>> landRoutesMap = new HashMap<>();
    findNavalMoveOptions(player, myUnitTerritories, moveMap, unitMoveMap, transportMoveMap,
        ProMatches.territoryHasNoEnemyUnitsOrCleared(player, data, clearedTerritories), clearedTerritories, false,
        isCheckingEnemyAttacks);
    findLandMoveOptions(player, myUnitTerritories, moveMap, unitMoveMap, landRoutesMap,
        Matches.isTerritoryAllied(player, data), new ArrayList<>(), clearedTerritories, false, isCheckingEnemyAttacks,
        false);
    findAirMoveOptions(player, myUnitTerritories, moveMap, unitMoveMap,
        ProMatches.territoryIsNotConqueredAlliedLand(player, data), new ArrayList<>(), new ArrayList<>(), false,
        isCheckingEnemyAttacks, false);
    findAmphibMoveOptions(player, myUnitTerritories, moveMap, transportMapList, landRoutesMap,
        Matches.isTerritoryAllied(player, data), false, isCheckingEnemyAttacks, false);
  }

  private static ProOtherMoveOptions findEnemyDefendOptions(final PlayerID player) {
    final GameData data = ProData.getData();

    // Get enemy players in order of turn
    final List<PlayerID> enemyPlayers = ProUtils.getEnemyPlayersInTurnOrder(player);
    final List<Map<Territory, ProTerritory>> enemyMoveMaps = new ArrayList<>();
    final List<Territory> clearedTerritories =
        CollectionUtils.getMatches(data.getMap().getTerritories(), Matches.isTerritoryAllied(player, data));

    // Loop through each enemy to determine the maximum number of enemy units that can defend each territory
    for (final PlayerID enemyPlayer : enemyPlayers) {
      final List<Territory> enemyUnitTerritories =
          CollectionUtils.getMatches(data.getMap().getTerritories(), Matches.territoryHasUnitsOwnedBy(enemyPlayer));
      final Map<Territory, ProTerritory> moveMap = new HashMap<>();
      final Map<Unit, Set<Territory>> unitMoveMap = new HashMap<>();
      final Map<Unit, Set<Territory>> transportMoveMap = new HashMap<>();
      final List<ProTransport> transportMapList = new ArrayList<>();
      enemyMoveMaps.add(moveMap);
      findDefendOptions(enemyPlayer, enemyUnitTerritories, moveMap, unitMoveMap, transportMoveMap, transportMapList,
          clearedTerritories, true);
    }

    return new ProOtherMoveOptions(enemyMoveMaps, player, false);
  }

  private static void findNavalMoveOptions(final PlayerID player, final List<Territory> myUnitTerritories,
      final Map<Territory, ProTerritory> moveMap, final Map<Unit, Set<Territory>> unitMoveMap,
      final Map<Unit, Set<Territory>> transportMoveMap, final Predicate<Territory> moveToTerritoryMatch,
      final List<Territory> clearedTerritories, final boolean isCombatMove, final boolean isCheckingEnemyAttacks) {
    final GameData data = ProData.getData();

    for (final Territory myUnitTerritory : myUnitTerritories) {

      // Find my naval units that have movement left
      final List<Unit> mySeaUnits =
          myUnitTerritory.getUnits().getMatches(ProMatches.unitCanBeMovedAndIsOwnedSea(player, isCombatMove));

      // Check each sea unit individually since they can have different ranges
      for (final Unit mySeaUnit : mySeaUnits) {

        // If my combat move and carrier has dependent allied fighters then skip it
        if (isCombatMove && !isCheckingEnemyAttacks) {
          final Map<Unit, Collection<Unit>> carrierMustMoveWith =
              MoveValidator.carrierMustMoveWith(myUnitTerritory.getUnits().getUnits(), myUnitTerritory, data, player);
          if (carrierMustMoveWith.containsKey(mySeaUnit) && !carrierMustMoveWith.get(mySeaUnit).isEmpty()) {
            continue;
          }
        }

        // Find range
        int range = TripleAUnit.get(mySeaUnit).getMovementLeft();
        if (isCheckingEnemyAttacks) {
          range = UnitAttachment.get(mySeaUnit.getType()).getMovement(player);
          if (Matches.unitCanBeGivenBonusMovementByFacilitiesInItsTerritory(myUnitTerritory, player, data)
              .test(mySeaUnit)) {
            range++; // assumes bonus of +1 for now
          }
        }

        // Find list of potential territories to move to
        final Set<Territory> possibleMoveTerritories = data.getMap().getNeighbors(myUnitTerritory, range,
            ProMatches.territoryCanMoveSeaUnits(player, data, isCombatMove));
        possibleMoveTerritories.add(myUnitTerritory);
        final Set<Territory> potentialTerritories =
            new HashSet<>(CollectionUtils.getMatches(possibleMoveTerritories, moveToTerritoryMatch));
        if (!isCombatMove) {
          potentialTerritories.add(myUnitTerritory);
        }
        for (final Territory potentialTerritory : potentialTerritories) {

          // Find route over water
          boolean hasNoRoute = true;
          final List<Territory> eliminatedTerritories = new ArrayList<>();
          while (true) { // Need a loop to consider different route combinations to avoid canals
            Route myRoute = data.getMap().getRoute_IgnoreEnd(myUnitTerritory, potentialTerritory,
                ProMatches.territoryCanMoveSeaUnitsThroughOrClearedAndNotInList(player, data, isCombatMove,
                    clearedTerritories, eliminatedTerritories));
            if (isCheckingEnemyAttacks) {
              myRoute = data.getMap().getRoute_IgnoreEnd(myUnitTerritory, potentialTerritory,
                  ProMatches.territoryCanMoveSeaUnitsAndNotInList(player, data, isCombatMove, eliminatedTerritories));
            }
            if (myRoute == null) {
              break;
            }
            if (MoveValidator.validateCanal(myRoute, Collections.singletonList(mySeaUnit), player, data) != null) {
              if (!myRoute.getMiddleSteps().isEmpty()) {
                eliminatedTerritories.addAll(myRoute.getMiddleSteps()); // Add failed canal territories to list
                continue;
              }
              break;
            }
            final int myRouteLength = myRoute.numberOfSteps();
            if (myRouteLength > range) {
              break;
            }
            hasNoRoute = false;
            break;
          }
          if (hasNoRoute) {
            continue;
          }

          // Populate territories with sea unit
          if (moveMap.containsKey(potentialTerritory)) {
            moveMap.get(potentialTerritory).addMaxUnit(mySeaUnit);
          } else {
            final ProTerritory moveTerritoryData = new ProTerritory(potentialTerritory);
            moveTerritoryData.addMaxUnit(mySeaUnit);
            moveMap.put(potentialTerritory, moveTerritoryData);
          }

          // Populate appropriate unit move options map
          if (Matches.unitIsTransport().test(mySeaUnit)) {
            if (transportMoveMap.containsKey(mySeaUnit)) {
              transportMoveMap.get(mySeaUnit).add(potentialTerritory);
            } else {
              final Set<Territory> unitMoveTerritories = new HashSet<>();
              unitMoveTerritories.add(potentialTerritory);
              transportMoveMap.put(mySeaUnit, unitMoveTerritories);
            }
          } else {
            if (unitMoveMap.containsKey(mySeaUnit)) {
              unitMoveMap.get(mySeaUnit).add(potentialTerritory);
            } else {
              final Set<Territory> unitMoveTerritories = new HashSet<>();
              unitMoveTerritories.add(potentialTerritory);
              unitMoveMap.put(mySeaUnit, unitMoveTerritories);
            }
          }
        }
      }
    }
  }

  private static void findLandMoveOptions(final PlayerID player, final List<Territory> myUnitTerritories,
      final Map<Territory, ProTerritory> moveMap, final Map<Unit, Set<Territory>> unitMoveMap,
      final Map<Territory, Set<Territory>> landRoutesMap, final Predicate<Territory> moveToTerritoryMatch,
      final List<Territory> enemyTerritories, final List<Territory> clearedTerritories, final boolean isCombatMove,
      final boolean isCheckingEnemyAttacks, final boolean isIgnoringRelationships) {
    final GameData data = ProData.getData();

    for (final Territory myUnitTerritory : myUnitTerritories) {

      // Find my land units that have movement left
      final List<Unit> myLandUnits =
          myUnitTerritory.getUnits().getMatches(ProMatches.unitCanBeMovedAndIsOwnedLand(player, isCombatMove));

      // Check each land unit individually since they can have different ranges
      for (final Unit myLandUnit : myLandUnits) {
        final Territory startTerritory = ProData.unitTerritoryMap.get(myLandUnit);
        final int range = TripleAUnit.get(myLandUnit).getMovementLeft();
        Set<Territory> possibleMoveTerritories = data.getMap().getNeighbors(myUnitTerritory, range,
            ProMatches.territoryCanMoveSpecificLandUnit(player, data, isCombatMove, myLandUnit));
        if (isIgnoringRelationships) {
          possibleMoveTerritories = data.getMap().getNeighbors(myUnitTerritory, range,
              ProMatches.territoryCanPotentiallyMoveSpecificLandUnit(player, data, myLandUnit));
        }
        possibleMoveTerritories.add(myUnitTerritory);
        final Set<Territory> potentialTerritories =
            new HashSet<>(CollectionUtils.getMatches(possibleMoveTerritories, moveToTerritoryMatch));
        if (!isCombatMove) {
          potentialTerritories.add(myUnitTerritory);
        }
        for (final Territory potentialTerritory : potentialTerritories) {

          // Find route over land checking whether unit can blitz
          Route myRoute = data.getMap().getRoute_IgnoreEnd(myUnitTerritory, potentialTerritory,
              ProMatches.territoryCanMoveLandUnitsThrough(player, data, myLandUnit, startTerritory, isCombatMove,
                  enemyTerritories));
          if (isCheckingEnemyAttacks) {
            myRoute = data.getMap().getRoute_IgnoreEnd(myUnitTerritory, potentialTerritory,
                ProMatches.territoryCanMoveLandUnitsThroughIgnoreEnemyUnits(player, data, myLandUnit, startTerritory,
                    isCombatMove, enemyTerritories, clearedTerritories));
          }
          if (myRoute == null) {
            continue;
          }
          if (myRoute.hasMoreThenOneStep()
              && myRoute.getMiddleSteps().stream().anyMatch(Matches.isTerritoryEnemy(player, data))
              && Matches.unitIsOfTypes(TerritoryEffectHelper.getUnitTypesThatLostBlitz(myRoute.getAllTerritories()))
                  .test(myLandUnit)) {
            continue; // If blitzing then make sure none of the territories cause blitz ability to be lost
          }
          final int myRouteLength = myRoute.numberOfSteps();
          if (myRouteLength > range) {
            continue;
          }

          // Add to route map
          if (landRoutesMap.containsKey(potentialTerritory)) {
            landRoutesMap.get(potentialTerritory).add(myUnitTerritory);
          } else {
            final Set<Territory> territories = new HashSet<>();
            territories.add(myUnitTerritory);
            landRoutesMap.put(potentialTerritory, territories);
          }

          // Populate territories with land units
          if (moveMap.containsKey(potentialTerritory)) {
            moveMap.get(potentialTerritory).addMaxUnit(myLandUnit);
          } else {
            final ProTerritory moveTerritoryData = new ProTerritory(potentialTerritory);
            moveTerritoryData.addMaxUnit(myLandUnit);
            moveMap.put(potentialTerritory, moveTerritoryData);
          }

          // Populate unit move options map
          if (unitMoveMap.containsKey(myLandUnit)) {
            unitMoveMap.get(myLandUnit).add(potentialTerritory);
          } else {
            final Set<Territory> unitMoveTerritories = new HashSet<>();
            unitMoveTerritories.add(potentialTerritory);
            unitMoveMap.put(myLandUnit, unitMoveTerritories);
          }
        }
      }
    }
  }

  private static void findAirMoveOptions(final PlayerID player, final List<Territory> myUnitTerritories,
      final Map<Territory, ProTerritory> moveMap, final Map<Unit, Set<Territory>> unitMoveMap,
      final Predicate<Territory> moveToTerritoryMatch, final List<Territory> enemyTerritories,
      final List<Territory> alliedTerritories, final boolean isCombatMove, final boolean isCheckingEnemyAttacks,
      final boolean isIgnoringRelationships) {
    final GameData data = ProData.getData();

    // TODO: add carriers to landing possibilities for non-enemy attacks
    // Find possible carrier landing territories
    final Set<Territory> possibleCarrierTerritories = new HashSet<>();
    if (isCheckingEnemyAttacks || !isCombatMove) {
      final Map<Unit, Set<Territory>> unitMoveMap2 = new HashMap<>();
      findNavalMoveOptions(player, myUnitTerritories, new HashMap<>(), unitMoveMap2, new HashMap<>(),
          Matches.territoryIsWater(), enemyTerritories, false, true);
      for (final Unit u : unitMoveMap2.keySet()) {
        if (Matches.unitIsCarrier().test(u)) {
          possibleCarrierTerritories.addAll(unitMoveMap2.get(u));
        }
      }
      for (final Territory t : data.getMap().getTerritories()) {
        if (t.getUnits().anyMatch(Matches.unitIsAlliedCarrier(player, data))) {
          possibleCarrierTerritories.add(t);
        }
      }
    }

    for (final Territory myUnitTerritory : myUnitTerritories) {

      // Find my air units that have movement left
      final List<Unit> myAirUnits =
          myUnitTerritory.getUnits().getMatches(ProMatches.unitCanBeMovedAndIsOwnedAir(player, isCombatMove));

      // Check each air unit individually since they can have different ranges
      for (final Unit myAirUnit : myAirUnits) {

        // Find range
        int range = TripleAUnit.get(myAirUnit).getMovementLeft();
        if (isCheckingEnemyAttacks) {
          range = UnitAttachment.get(myAirUnit.getType()).getMovement(player);
          if (Matches.unitCanBeGivenBonusMovementByFacilitiesInItsTerritory(myUnitTerritory, player, data)
              .test(myAirUnit)) {
            range++; // assumes bonus of +1 for now
          }
        }

        // Find potential territories to move to
        Set<Territory> possibleMoveTerritories = data.getMap().getNeighbors(myUnitTerritory, range,
            ProMatches.territoryCanMoveAirUnits(player, data, isCombatMove));
        if (isIgnoringRelationships) {
          possibleMoveTerritories = data.getMap().getNeighbors(myUnitTerritory, range,
              ProMatches.territoryCanPotentiallyMoveAirUnits(player, data));
        }
        possibleMoveTerritories.add(myUnitTerritory);
        final Set<Territory> potentialTerritories =
            new HashSet<>(CollectionUtils.getMatches(possibleMoveTerritories, moveToTerritoryMatch));
        if (!isCombatMove && Matches.unitCanLandOnCarrier().test(myAirUnit)) {
          potentialTerritories.addAll(CollectionUtils.getMatches(possibleMoveTerritories,
              Matches.territoryIsInList(possibleCarrierTerritories)));
        }


        for (final Territory potentialTerritory : potentialTerritories) {

          // Find route ignoring impassable and territories with AA
          Predicate<Territory> canFlyOverMatch = ProMatches.territoryCanMoveAirUnitsAndNoAa(player, data, isCombatMove);
          if (isCheckingEnemyAttacks) {
            canFlyOverMatch = ProMatches.territoryCanMoveAirUnits(player, data, isCombatMove);
          }
          final Route myRoute = data.getMap().getRoute_IgnoreEnd(myUnitTerritory, potentialTerritory, canFlyOverMatch);
          if (myRoute == null) {
            continue;
          }
          final int myRouteLength = myRoute.numberOfSteps();
          final int remainingMoves = range - myRouteLength;
          if (remainingMoves < 0) {
            continue;
          }

          // Check if unit can land
          if (isCombatMove && (remainingMoves < myRouteLength || myUnitTerritory.isWater())) {
            final Set<Territory> possibleLandingTerritories =
                data.getMap().getNeighbors(potentialTerritory, remainingMoves, canFlyOverMatch);
            final List<Territory> landingTerritories = CollectionUtils.getMatches(possibleLandingTerritories,
                ProMatches.territoryCanLandAirUnits(player, data, isCombatMove, enemyTerritories, alliedTerritories));
            List<Territory> carrierTerritories = new ArrayList<>();
            if (Matches.unitCanLandOnCarrier().test(myAirUnit)) {
              carrierTerritories = CollectionUtils.getMatches(possibleLandingTerritories,
                  Matches.territoryIsInList(possibleCarrierTerritories));
            }
            if (landingTerritories.isEmpty() && carrierTerritories.isEmpty()) {
              continue;
            }
          }

          // Populate enemy territories with air unit
          if (moveMap.containsKey(potentialTerritory)) {
            moveMap.get(potentialTerritory).addMaxUnit(myAirUnit);
          } else {
            final ProTerritory moveTerritoryData = new ProTerritory(potentialTerritory);
            moveTerritoryData.addMaxUnit(myAirUnit);
            moveMap.put(potentialTerritory, moveTerritoryData);
          }

          // Populate unit attack options map
          if (unitMoveMap.containsKey(myAirUnit)) {
            unitMoveMap.get(myAirUnit).add(potentialTerritory);
          } else {
            final Set<Territory> unitMoveTerritories = new HashSet<>();
            unitMoveTerritories.add(potentialTerritory);
            unitMoveMap.put(myAirUnit, unitMoveTerritories);
          }
        }
      }
    }
  }

  private static void findAmphibMoveOptions(final PlayerID player, final List<Territory> myUnitTerritories,
      final Map<Territory, ProTerritory> moveMap, final List<ProTransport> transportMapList,
      final Map<Territory, Set<Territory>> landRoutesMap, final Predicate<Territory> moveAmphibToTerritoryMatch,
      final boolean isCombatMove, final boolean isCheckingEnemyAttacks, final boolean isIgnoringRelationships) {
    final GameData data = ProData.getData();

    for (final Territory myUnitTerritory : myUnitTerritories) {

      // Find my transports and amphibious units that have movement left
      final List<Unit> myTransportUnits =
          myUnitTerritory.getUnits().getMatches(ProMatches.unitCanBeMovedAndIsOwnedTransport(player, isCombatMove));
      Predicate<Territory> unloadAmphibTerritoryMatch = ProMatches.territoryCanMoveLandUnits(player, data, isCombatMove)
          .and(moveAmphibToTerritoryMatch);
      if (isIgnoringRelationships) {
        unloadAmphibTerritoryMatch = ProMatches.territoryCanPotentiallyMoveLandUnits(player, data)
            .and(moveAmphibToTerritoryMatch);
      }

      // Check each transport unit individually since they can have different ranges
      for (final Unit myTransportUnit : myTransportUnits) {

        // Get remaining moves
        int movesLeft = TripleAUnit.get(myTransportUnit).getMovementLeft();
        if (isCheckingEnemyAttacks) {
          movesLeft = UnitAttachment.get(myTransportUnit.getType()).getMovement(player);
          if (Matches.unitCanBeGivenBonusMovementByFacilitiesInItsTerritory(myUnitTerritory, player, data)
              .test(myTransportUnit)) {
            movesLeft++; // assumes bonus of +1 for now
          }
        }

        // Find units to load and territories to unload
        final ProTransport proTransportData = new ProTransport(myTransportUnit);
        transportMapList.add(proTransportData);
        final Set<Territory> currentTerritories = new HashSet<>();
        currentTerritories.add(myUnitTerritory);
        while (movesLeft >= 0) {
          final Set<Territory> nextTerritories = new HashSet<>();
          for (final Territory currentTerritory : currentTerritories) {

            // Find neighbors I can move to
            final Set<Territory> possibleNeighborTerritories = data.getMap().getNeighbors(currentTerritory,
                ProMatches.territoryCanMoveSeaUnitsThrough(player, data, isCombatMove));
            for (final Territory possibleNeighborTerritory : possibleNeighborTerritories) {
              if (MoveValidator.validateCanal(new Route(currentTerritory, possibleNeighborTerritory),
                  Collections.singletonList(myTransportUnit), player, data) == null) {
                nextTerritories.add(possibleNeighborTerritory);
              }
            }

            // Get loaded units or get units that can be loaded into current territory if no enemies present
            final List<Unit> units = new ArrayList<>();
            final Set<Territory> myUnitsToLoadTerritories = new HashSet<>();
            if (TransportTracker.isTransporting(myTransportUnit)) {
              units.addAll(TransportTracker.transporting(myTransportUnit));
            } else if (Matches.territoryHasEnemySeaUnits(player, data).negate().test(currentTerritory)) {
              final Set<Territory> possibleLoadTerritories = data.getMap().getNeighbors(currentTerritory);
              for (final Territory possibleLoadTerritory : possibleLoadTerritories) {
                List<Unit> possibleUnits = possibleLoadTerritory.getUnits().getMatches(
                    ProMatches.unitIsOwnedTransportableUnitAndCanBeLoaded(player, myTransportUnit, isCombatMove));
                if (isCheckingEnemyAttacks) {
                  possibleUnits = possibleLoadTerritory.getUnits()
                      .getMatches(ProMatches.unitIsOwnedCombatTransportableUnit(player));
                }
                for (final Unit possibleUnit : possibleUnits) {
                  if (UnitAttachment.get(possibleUnit.getType()).getTransportCost() <= UnitAttachment
                      .get(myTransportUnit.getType()).getTransportCapacity()) {
                    units.add(possibleUnit);
                    myUnitsToLoadTerritories.add(possibleLoadTerritory);
                  }
                }
              }
            }

            // If there are any units to be transported
            if (!units.isEmpty()) {

              // Find all water territories I can move to
              final Set<Territory> seaMoveTerritories = new HashSet<>();
              seaMoveTerritories.add(currentTerritory);
              if (movesLeft > 0) {
                Set<Territory> neighborTerritories = data.getMap().getNeighbors(currentTerritory, movesLeft,
                    ProMatches.territoryCanMoveSeaUnitsThrough(player, data, isCombatMove));
                if (isCheckingEnemyAttacks) {
                  neighborTerritories = data.getMap().getNeighbors(currentTerritory, movesLeft,
                      ProMatches.territoryCanMoveSeaUnits(player, data, isCombatMove));
                }
                for (final Territory neighborTerritory : neighborTerritories) {
                  final Route myRoute = data.getMap().getRoute_IgnoreEnd(currentTerritory, neighborTerritory,
                      ProMatches.territoryCanMoveSeaUnitsThrough(player, data, isCombatMove));
                  if (myRoute == null) {
                    continue;
                  }
                  if (MoveValidator.validateCanal(myRoute, Collections.singletonList(myTransportUnit), player,
                      data) != null) {
                    continue;
                  }
                  seaMoveTerritories.add(neighborTerritory);
                }
              }

              // Find possible unload territories
              final Set<Territory> amphibTerritories = new HashSet<>();
              for (final Territory seaMoveTerritory : seaMoveTerritories) {
                amphibTerritories.addAll(data.getMap().getNeighbors(seaMoveTerritory, unloadAmphibTerritoryMatch));
              }

              // Add to transport map
              proTransportData.addTerritories(amphibTerritories, myUnitsToLoadTerritories);
              proTransportData.addSeaTerritories(seaMoveTerritories, myUnitsToLoadTerritories);
            }
          }
          currentTerritories.clear();
          currentTerritories.addAll(nextTerritories);
          movesLeft--;
        }
      }
    }

    // Remove any territories from transport map that I can move to on land and transports with no amphib options
    for (final ProTransport proTransportData : transportMapList) {
      final Map<Territory, Set<Territory>> transportMap = proTransportData.getTransportMap();
      final List<Territory> transportTerritoriesToRemove = new ArrayList<>();
      for (final Territory t : transportMap.keySet()) {
        final Set<Territory> transportMoveTerritories = transportMap.get(t);
        final Set<Territory> landMoveTerritories = landRoutesMap.get(t);
        if (landMoveTerritories != null) {
          transportMoveTerritories.removeAll(landMoveTerritories);
          if (transportMoveTerritories.isEmpty()) {
            transportTerritoriesToRemove.add(t);
          }
        }
      }
      for (final Territory t : transportTerritoriesToRemove) {
        transportMap.remove(t);
      }
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
        List<Unit> amphibUnits = ProTransportUtils.getUnitsToTransportFromTerritories(player, transport,
            territoriesCanLoadFrom, alreadyAddedToMaxAmphibUnits);
        if (isCheckingEnemyAttacks) {
          amphibUnits = ProTransportUtils.getUnitsToTransportFromTerritories(player, transport, territoriesCanLoadFrom,
              alreadyAddedToMaxAmphibUnits, ProMatches.unitIsOwnedCombatTransportableUnit(player));
        }

        // Add amphib units to attack map
        if (moveMap.containsKey(moveTerritory)) {
          moveMap.get(moveTerritory).addMaxAmphibUnits(amphibUnits);
        } else {
          final ProTerritory moveTerritoryData = new ProTerritory(moveTerritory);
          moveTerritoryData.addMaxAmphibUnits(amphibUnits);
          moveMap.put(moveTerritory, moveTerritoryData);
        }
      }
    }
  }

  private static void findBombardOptions(final PlayerID player, final List<Territory> myUnitTerritories,
      final Map<Territory, ProTerritory> moveMap, final Map<Unit, Set<Territory>> bombardMap,
      final List<ProTransport> transportMapList, final boolean isCheckingEnemyAttacks) {
    final GameData data = ProData.getData();

    // Find all transport unload from and to territories
    final Set<Territory> unloadFromTerritories = new HashSet<>();
    final Set<Territory> unloadToTerritories = new HashSet<>();
    for (final ProTransport amphibData : transportMapList) {
      unloadFromTerritories.addAll(amphibData.getSeaTransportMap().keySet());
      unloadToTerritories.addAll(amphibData.getTransportMap().keySet());
    }

    // Loop through territories with my units
    for (final Territory myUnitTerritory : myUnitTerritories) {

      // Find my bombard units that have movement left
      final List<Unit> mySeaUnits =
          myUnitTerritory.getUnits().getMatches(ProMatches.unitCanBeMovedAndIsOwnedBombard(player));

      // Check each sea unit individually since they can have different ranges
      for (final Unit mySeaUnit : mySeaUnits) {

        // Find range
        int range = TripleAUnit.get(mySeaUnit).getMovementLeft();
        if (isCheckingEnemyAttacks) {
          range = UnitAttachment.get(mySeaUnit.getType()).getMovement(player);
          if (Matches.unitCanBeGivenBonusMovementByFacilitiesInItsTerritory(myUnitTerritory, player, data)
              .test(mySeaUnit)) {
            range++; // assumes bonus of +1 for now
          }
        }

        // Find list of potential territories to move to
        final Set<Territory> potentialTerritories =
            data.getMap().getNeighbors(myUnitTerritory, range, ProMatches.territoryCanMoveSeaUnits(player, data, true));
        potentialTerritories.add(myUnitTerritory);
        potentialTerritories.retainAll(unloadFromTerritories);
        for (final Territory bombardFromTerritory : potentialTerritories) {

          // Find route over water with no enemy units blocking
          Route myRoute = data.getMap().getRoute(myUnitTerritory, bombardFromTerritory,
              ProMatches.territoryCanMoveSeaUnitsThrough(player, data, true));
          if (isCheckingEnemyAttacks) {
            myRoute = data.getMap().getRoute(myUnitTerritory, bombardFromTerritory,
                ProMatches.territoryCanMoveSeaUnits(player, data, true));
          }
          if (myRoute == null) {
            continue;
          }
          if (MoveValidator.validateCanal(myRoute, Collections.singletonList(mySeaUnit), player, data) != null) {
            continue;
          }
          final int myRouteLength = myRoute.numberOfSteps();
          if (myRouteLength > range) {
            continue;
          }

          // Find potential unload to territories
          final Set<Territory> bombardToTerritories = new HashSet<>(data.getMap().getNeighbors(bombardFromTerritory));
          bombardToTerritories.retainAll(unloadToTerritories);

          // Populate attack territories with bombard unit
          for (final Territory bombardToTerritory : bombardToTerritories) {
            if (moveMap.containsKey(bombardToTerritory)) { // Should always contain it
              moveMap.get(bombardToTerritory).addMaxBombardUnit(mySeaUnit);
              moveMap.get(bombardToTerritory).addBombardOptionsMap(mySeaUnit, bombardFromTerritory);
            }
          }

          // Populate bombard options map
          if (bombardMap.containsKey(mySeaUnit)) {
            bombardMap.get(mySeaUnit).addAll(bombardToTerritories);
          } else {
            bombardMap.put(mySeaUnit, bombardToTerritories);
          }
        }
      }
    }
  }
}
