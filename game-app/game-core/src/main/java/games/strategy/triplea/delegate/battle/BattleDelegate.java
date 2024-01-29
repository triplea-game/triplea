package games.strategy.triplea.delegate.battle;

import static java.util.function.Predicate.not;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.RouteScripted;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.AutoSave;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.history.change.HistoryChangeFactory;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.player.Player;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.triplea.Properties;
import games.strategy.triplea.UnitUtils;
import games.strategy.triplea.attachments.PlayerAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.BaseTripleADelegate;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.RocketsFireHelper;
import games.strategy.triplea.delegate.TechTracker;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.triplea.delegate.UnitComparator;
import games.strategy.triplea.delegate.battle.IBattle.BattleType;
import games.strategy.triplea.delegate.battle.IBattle.WhoWon;
import games.strategy.triplea.delegate.data.BattleListing;
import games.strategy.triplea.delegate.data.BattleRecord;
import games.strategy.triplea.delegate.move.validation.AirMovementValidator;
import games.strategy.triplea.delegate.remote.IBattleDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.Getter;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Tuple;

/** Delegate to track and fight all battles. */
@AutoSave(beforeStepStart = true, afterStepEnd = true)
public class BattleDelegate extends BaseTripleADelegate implements IBattleDelegate {
  private static final String MUST_COMPLETE_BATTLE_PREFIX = "Must complete ";

  @Getter private BattleTracker battleTracker = new BattleTracker();
  private boolean needToInitialize = true;
  private boolean needToScramble = true;
  private boolean needToKamikazeSuicideAttacks = true;
  private boolean needToClearEmptyAirBattleAttacks = true;
  private boolean needToAddBombardmentSources = true;
  private boolean needToRecordBattleStatistics = true;
  private boolean needToCheckDefendingPlanesCanLand = true;
  private boolean needToCleanup = true;
  private boolean needToCreateRockets = true;
  private boolean needToFireRockets = true;
  private RocketsFireHelper rocketHelper;
  private IBattle currentBattle = null;

  @Override
  public void start() {
    super.start();
    // we may start multiple times due to loading after saving
    // only initialize once
    if (needToInitialize) {
      doInitialize(battleTracker, bridge);
      needToInitialize = false;
    }
    // do pre-combat stuff, like scrambling, after we have set up all battles, but before we have
    // bombardment, etc.
    // the order of all of this stuff matters quite a bit.
    if (needToScramble) {
      doScrambling();
      needToScramble = false;
    }
    if (needToCreateRockets) {
      rocketHelper = RocketsFireHelper.setUpRockets(bridge);
      needToCreateRockets = false;
    }
    if (needToKamikazeSuicideAttacks) {
      doKamikazeSuicideAttacks();
      needToKamikazeSuicideAttacks = false;
    }
    if (needToClearEmptyAirBattleAttacks) {
      clearEmptyAirBattleAttacks(battleTracker, bridge);
      needToClearEmptyAirBattleAttacks = false;
    }
    if (needToAddBombardmentSources) {
      addBombardmentSources();
      needToAddBombardmentSources = false;
    }
    battleTracker.fightAirRaidsAndStrategicBombing(bridge);
    if (needToFireRockets) {
      // If we are loading a save-game created during battle and after the 'needToCreateRockets'
      // phase, rocketHelper can be null here.
      if (rocketHelper == null) {
        rocketHelper = RocketsFireHelper.setUpRockets(bridge);
      }
      rocketHelper.fireRockets(bridge);
      needToFireRockets = false;
    }
    battleTracker.fightDefenselessBattles(bridge);
    battleTracker.fightBattleIfOnlyOne(bridge);
  }

  @Override
  public void end() {
    if (needToRecordBattleStatistics) {
      getBattleTracker().sendBattleRecordsToGameData(bridge);
      needToRecordBattleStatistics = false;
    }
    if (needToCleanup) {
      getBattleTracker().clearBattleRecords();
      scramblingCleanup();
      airBattleCleanup();
      needToCleanup = false;
    }
    if (needToCheckDefendingPlanesCanLand) {
      checkDefendingPlanesCanLand();
      needToCheckDefendingPlanesCanLand = false;
    }
    super.end();
    needToInitialize = true;
    needToScramble = true;
    needToCreateRockets = true;
    needToKamikazeSuicideAttacks = true;
    needToClearEmptyAirBattleAttacks = true;
    needToAddBombardmentSources = true;
    needToFireRockets = true;
    needToRecordBattleStatistics = true;
    needToCleanup = true;
    needToCheckDefendingPlanesCanLand = true;
  }

  @Override
  public Serializable saveState() {
    final BattleExtendedDelegateState state = new BattleExtendedDelegateState();
    state.superState = super.saveState();
    // add other variables to state here:
    state.battleTracker = battleTracker;
    state.needToInitialize = needToInitialize;
    state.needToScramble = needToScramble;
    state.needToCreateRockets = needToCreateRockets;
    state.needToKamikazeSuicideAttacks = needToKamikazeSuicideAttacks;
    state.needToClearEmptyAirBattleAttacks = needToClearEmptyAirBattleAttacks;
    state.needToAddBombardmentSources = needToAddBombardmentSources;
    state.needToFireRockets = needToFireRockets;
    state.needToRecordBattleStatistics = needToRecordBattleStatistics;
    state.needToCheckDefendingPlanesCanLand = needToCheckDefendingPlanesCanLand;
    state.needToCleanup = needToCleanup;
    state.currentBattle = currentBattle;
    return state;
  }

  @Override
  public void loadState(final Serializable state) {
    final BattleExtendedDelegateState s = (BattleExtendedDelegateState) state;
    super.loadState(s.superState);
    battleTracker = s.battleTracker;
    needToInitialize = s.needToInitialize;
    needToScramble = s.needToScramble;
    needToCreateRockets = s.needToCreateRockets;
    needToKamikazeSuicideAttacks = s.needToKamikazeSuicideAttacks;
    needToClearEmptyAirBattleAttacks = s.needToClearEmptyAirBattleAttacks;
    needToAddBombardmentSources = s.needToAddBombardmentSources;
    needToFireRockets = s.needToFireRockets;
    needToRecordBattleStatistics = s.needToRecordBattleStatistics;
    needToCheckDefendingPlanesCanLand = s.needToCheckDefendingPlanesCanLand;
    needToCleanup = s.needToCleanup;
    currentBattle = s.currentBattle;
  }

  @Override
  public boolean delegateCurrentlyRequiresUserInput() {
    final BattleListing battles = getBattles();
    if (battles.isEmpty()) {
      final IBattle battle = getCurrentBattle();
      return battle != null;
    }
    return true;
  }

  public static void doInitialize(final BattleTracker battleTracker, final IDelegateBridge bridge) {
    setupUnitsInSameTerritoryBattles(battleTracker, bridge);
    setupTerritoriesAbandonedToTheEnemy(battleTracker, bridge);
    // these are "blitzed" and "conquered" territories without a fight, without a pending battle
    battleTracker.clearFinishedBattles(bridge);
    resetMaxScrambleCount(bridge);
  }

  private static void clearEmptyAirBattleAttacks(
      final BattleTracker battleTracker, final IDelegateBridge bridge) {
    // these are air battle and air raids where there is no defender, probably because no air is in
    // range to defend
    battleTracker.clearEmptyAirBattleAttacks(bridge);
  }

  @Override
  public String fightBattle(
      final Territory territory, final boolean bombing, final BattleType type) {
    final IBattle battle = battleTracker.getPendingBattle(territory, type);
    if (currentBattle != null && currentBattle != battle) {
      return "Must finish "
          + getFightingWord(currentBattle)
          + " in "
          + currentBattle.getTerritory()
          + " first";
    }
    // does the battle exist
    if (battle == null) {
      return "No pending battle in" + territory.getName();
    }
    // are there battles that must occur first
    final Collection<IBattle> allMustPrecede = battleTracker.getDependentOn(battle);
    if (!allMustPrecede.isEmpty()) {
      final IBattle firstPrecede = CollectionUtils.getAny(allMustPrecede);
      final String name = firstPrecede.getTerritory().getName();
      return MUST_COMPLETE_BATTLE_PREFIX + getFightingWord(firstPrecede) + " in " + name + " first";
    }
    currentBattle = battle;
    battle.fight(bridge);
    return null;
  }

  public static boolean isBattleDependencyErrorMessage(String message) {
    return message.startsWith(MUST_COMPLETE_BATTLE_PREFIX);
  }

  private static String getFightingWord(final IBattle battle) {
    return battle.getBattleType().toDisplayText();
  }

  @Override
  public BattleListing getBattles() {
    return battleTracker.getPendingBattleSites();
  }

  /** Add bombardment units to battles. */
  @VisibleForTesting
  public void addBombardmentSources() {
    final GamePlayer attacker = bridge.getGamePlayer();
    final Player remotePlayer = bridge.getRemotePlayer();
    final Predicate<Unit> ownedAndCanBombard =
        Matches.unitCanBombard(attacker).and(Matches.unitIsOwnedBy(attacker));
    final Map<Territory, Collection<IBattle>> adjBombardment = getPossibleBombardingTerritories();
    for (final Territory t : adjBombardment.keySet()) {
      if (!battleTracker.hasPendingNonBombingBattle(t)) {
        Collection<IBattle> battles = adjBombardment.get(t);
        if (!battles.isEmpty()) {
          final Collection<Unit> bombardUnits =
              t.getUnitCollection().getMatches(ownedAndCanBombard);
          final List<Unit> listedBombardUnits = new ArrayList<>(bombardUnits);
          sortUnitsToBombard(listedBombardUnits);
          // if bombarding, ask if they want to bombard
          if (!bombardUnits.isEmpty() && !remotePlayer.selectShoreBombard(t)) {
            continue;
          }
          for (final Unit u : listedBombardUnits) {
            final IBattle battle = selectBombardingBattle(u, t, battles);
            if (battle != null) {
              if (Properties.getShoreBombardPerGroundUnitRestricted(getData().getProperties())
                  && battle.getAttackingUnits().stream().filter(Matches.unitWasAmphibious()).count()
                      <= battle.getBombardingUnits().size()) {
                battles.remove(battle);
                break;
              }
              battle.addBombardingUnit(u);
            }
          }
        }
      }
    }
  }

  /** Sort the specified units in preferred movement or unload order. */
  public static void sortUnitsToBombard(final List<Unit> units) {
    if (units.isEmpty()) {
      return;
    }
    units.sort(UnitComparator.getDecreasingBombardComparator());
  }

  /**
   * Return map of adjacent territories along attack routes in battles where fighting will occur.
   */
  private Map<Territory, Collection<IBattle>> getPossibleBombardingTerritories() {
    final Map<Territory, Collection<IBattle>> possibleBombardingTerritories = new HashMap<>();
    for (final IBattle battle : battleTracker.getPendingBattles(BattleType.NORMAL)) {
      // we only care about battles where we must fight
      // this check is really to avoid implementing getAttackingFrom() in other battle subclasses
      if (!(battle instanceof MustFightBattle)) {
        continue;
      }
      // Bombarding only allowed in amphibious battles.
      if (!battle.isAmphibious()) {
        continue;
      }
      // bombarding can only occur in territories from which at least 1 land unit attacked
      final Map<Territory, Collection<Unit>> attackingFromMap =
          ((MustFightBattle) battle).getAttackingFromMap();
      for (final Territory neighbor : ((MustFightBattle) battle).getAttackingFrom()) {
        // we do not allow bombarding from certain sea zones (like if there was a kamikaze suicide
        // attack there, etc.)
        if (battleTracker.noBombardAllowedFromHere(neighbor)) {
          continue;
        }
        final Collection<Unit> neighbourUnits = attackingFromMap.get(neighbor);
        // If all units from a territory are air - no bombard
        if (!neighbourUnits.isEmpty() && neighbourUnits.stream().allMatch(Matches.unitIsAir())) {
          continue;
        }
        final Collection<IBattle> battles =
            possibleBombardingTerritories.computeIfAbsent(neighbor, k -> new ArrayList<>());
        battles.add(battle);
      }
    }
    return possibleBombardingTerritories;
  }

  /** Select which territory to bombard. */
  private IBattle selectBombardingBattle(
      final Unit u, final Territory unitTerritory, final Collection<IBattle> battles) {
    // If only one battle to select from just return that battle
    if (battles.size() == 1) {
      return CollectionUtils.getAny(battles);
    }
    final List<Territory> territories = new ArrayList<>();
    final Map<Territory, IBattle> battleTerritories = new HashMap<>();
    for (final IBattle battle : battles) {
      // If Restricted & # of bombarding units => landing units, don't add territory to list to
      // bombard
      if (!Properties.getShoreBombardPerGroundUnitRestricted(getData().getProperties())
          || (battle.getBombardingUnits().size()
              < battle.getAttackingUnits().stream().filter(Matches.unitWasAmphibious()).count())) {
        territories.add(battle.getTerritory());
      }
      battleTerritories.put(battle.getTerritory(), battle);
    }
    final Player remotePlayer = bridge.getRemotePlayer();
    Territory bombardingTerritory = null;
    if (!territories.isEmpty()) {
      bombardingTerritory =
          remotePlayer.selectBombardingTerritory(u, unitTerritory, territories, true);
    }
    if (bombardingTerritory != null) {
      return battleTerritories.get(bombardingTerritory);
    }
    // User elected not to bombard with this unit
    return null;
  }

  private static void landParatroopers(
      final GamePlayer player, final Territory battleSite, final IDelegateBridge bridge) {
    if (TechTracker.hasParatroopers(player)) {
      final Collection<Unit> airTransports =
          CollectionUtils.getMatches(battleSite.getUnitCollection(), Matches.unitIsAirTransport());
      final Collection<Unit> paratroops =
          CollectionUtils.getMatches(
              battleSite.getUnitCollection(), Matches.unitIsAirTransportable());
      if (!airTransports.isEmpty() && !paratroops.isEmpty()) {
        final CompositeChange change = new CompositeChange();
        for (final Unit paratroop : paratroops) {
          final Unit transport = paratroop.getTransportedBy();
          if (transport == null || !airTransports.contains(transport)) {
            continue;
          }
          change.add(TransportTracker.unloadAirTransportChange(paratroop, battleSite, false));
        }
        if (!change.isEmpty()) {
          bridge
              .getHistoryWriter()
              .startEvent(player.getName() + " lands units in " + battleSite.getName());
          bridge.addChange(change);
        }
      }
    }
  }

  /**
   * Set up the battles where the battle occurs because units are in the same territory. This
   * happens when subs emerge (after being submerged), and when naval units are placed in enemy
   * occupied sea zones, and also when political relationships change and potentially leave units in
   * now-hostile territories.
   */
  private static void setupUnitsInSameTerritoryBattles(
      final BattleTracker battleTracker, final IDelegateBridge bridge) {
    final GamePlayer player = bridge.getGamePlayer();
    final GameData data = bridge.getData();
    final boolean ignoreTransports = Properties.getIgnoreTransportInMovement(data.getProperties());
    final Predicate<Unit> seaTransports =
        Matches.unitIsSeaTransportButNotCombatSeaTransport().and(Matches.unitIsSea());
    final Predicate<Unit> seaTransportsOrSubs = seaTransports.or(Matches.unitCanEvade());
    // we want to match all sea zones with our units and enemy units
    final Predicate<Territory> anyTerritoryWithOwnAndEnemy =
        Matches.territoryHasUnitsOwnedBy(player).and(Matches.territoryHasEnemyUnits(player));
    final Predicate<Territory> enemyTerritoryAndOwnUnits =
        Matches.isTerritoryEnemyAndNotUnownedWater(player)
            .and(Matches.territoryHasUnitsOwnedBy(player));
    final Predicate<Territory> enemyUnitsOrEnemyTerritory =
        anyTerritoryWithOwnAndEnemy.or(enemyTerritoryAndOwnUnits);
    final List<Territory> battleTerritories =
        CollectionUtils.getMatches(data.getMap().getTerritories(), enemyUnitsOrEnemyTerritory);
    for (final Territory territory : battleTerritories) {
      final List<Unit> attackingUnits =
          territory.getUnitCollection().getMatches(Matches.unitIsOwnedBy(player));
      // now make sure to add any units that must move with these attacking units, so that they get
      // included as
      // dependencies
      final Map<Unit, Collection<Unit>> transportMap =
          TransportTracker.transporting(territory.getUnitCollection());
      final Set<Unit> dependants = new HashSet<>();
      for (final Entry<Unit, Collection<Unit>> entry : transportMap.entrySet()) {
        // only consider those transports that we are attacking with. allied and enemy transports
        // are not added.
        if (attackingUnits.contains(entry.getKey())) {
          dependants.addAll(entry.getValue());
        }
      }
      // no duplicates
      dependants.removeAll(attackingUnits);
      // add the dependants to the attacking list
      attackingUnits.addAll(dependants);
      final List<Unit> enemyUnits =
          territory.getUnitCollection().getMatches(Matches.enemyUnit(player));
      final IBattle bombingBattle = battleTracker.getPendingBombingBattle(territory);
      if (bombingBattle != null) {
        // we need to remove any units which are participating in bombing raids
        attackingUnits.removeAll(bombingBattle.getAttackingUnits());
      }
      if (attackingUnits.stream().allMatch(Matches.unitIsInfrastructure())) {
        continue;
      }
      IBattle battle = battleTracker.getPendingBattle(territory, BattleType.NORMAL);
      if (battle == null) {
        // we must land any paratroopers here, but only if there is not going to be a battle (cus
        // battles land them
        // separately, after aa fires)
        if (enemyUnits.stream().allMatch(Matches.unitIsInfrastructure())) {
          landParatroopers(player, territory, bridge);
        }
        bridge
            .getHistoryWriter()
            .startEvent(player.getName() + " creates battle in territory " + territory.getName());
        battleTracker.addBattle(
            new RouteScripted(territory), attackingUnits, player, bridge, null, null);
        battle = battleTracker.getPendingBattle(territory, BattleType.NORMAL);
      }
      if (battle == null) {
        continue;
      }
      if (bombingBattle != null) {
        battleTracker.addDependency(battle, bombingBattle);
      }
      if (battle.isEmpty()) {
        battle.addAttackChange(new RouteScripted(territory), attackingUnits, null);
      }
      if (!battle.getAttackingUnits().containsAll(attackingUnits)) {
        List<Unit> attackingUnitsNeedToBeAdded = new ArrayList<>(attackingUnits);
        attackingUnitsNeedToBeAdded.removeAll(battle.getAttackingUnits());
        attackingUnitsNeedToBeAdded.removeAll(battle.getDependentUnits(battle.getAttackingUnits()));
        if (territory.isWater()) {
          attackingUnitsNeedToBeAdded =
              CollectionUtils.getMatches(
                  attackingUnitsNeedToBeAdded, Matches.unitIsLand().negate());
        } else {
          attackingUnitsNeedToBeAdded =
              CollectionUtils.getMatches(attackingUnitsNeedToBeAdded, Matches.unitIsSea().negate());
        }
        if (!attackingUnitsNeedToBeAdded.isEmpty()) {
          battle.addAttackChange(new RouteScripted(territory), attackingUnitsNeedToBeAdded, null);
        }
      }
      // Reach stalemate if all attacking and defending units are transports
      if ((ignoreTransports
              && !attackingUnits.isEmpty()
              && attackingUnits.stream().allMatch(seaTransports)
              && !enemyUnits.isEmpty()
              && enemyUnits.stream().allMatch(seaTransports))
          || (!attackingUnits.isEmpty()
              && attackingUnits.stream().allMatch(Matches.unitHasAttackValueOfAtLeast(1).negate())
              && !enemyUnits.isEmpty()
              && enemyUnits.stream().allMatch(Matches.unitHasDefendValueOfAtLeast(1).negate()))) {
        final BattleResults results = new BattleResults(battle, WhoWon.DRAW, data);
        battleTracker
            .getBattleRecords()
            .addResultToBattle(
                player,
                battle.getBattleId(),
                null,
                0,
                0,
                BattleRecord.BattleResultDescription.STALEMATE,
                results);
        battle.cancelBattle(bridge);
        battleTracker.removeBattle(battle, data);
        continue;
      }
      // possibility to ignore battle altogether
      if (!attackingUnits.isEmpty()) {
        final Player remotePlayer = bridge.getRemotePlayer();
        final boolean isWater = territory.isWater();
        if ((isWater && Properties.getSeaBattlesMayBeIgnored(data.getProperties()))
            || (!isWater && Properties.getLandBattlesMayBeIgnored(data.getProperties()))) {
          if (!remotePlayer.selectAttackUnits(territory)) {
            final BattleResults results = new BattleResults(battle, WhoWon.NOT_FINISHED, data);
            battleTracker
                .getBattleRecords()
                .addResultToBattle(
                    player,
                    battle.getBattleId(),
                    null,
                    0,
                    0,
                    BattleRecord.BattleResultDescription.NO_BATTLE,
                    results);
            battle.cancelBattle(bridge);
            battleTracker.removeBattle(battle, data);
          }
          continue;
        }
        // TODO check if incoming units can attack before asking
        // if only enemy transports... attack them?
        if (ignoreTransports
            && !enemyUnits.isEmpty()
            && enemyUnits.stream().allMatch(seaTransports)) {
          if (!remotePlayer.selectAttackTransports(territory)) {
            final BattleResults results = new BattleResults(battle, WhoWon.NOT_FINISHED, data);
            battleTracker
                .getBattleRecords()
                .addResultToBattle(
                    player,
                    battle.getBattleId(),
                    null,
                    0,
                    0,
                    BattleRecord.BattleResultDescription.NO_BATTLE,
                    results);
            battle.cancelBattle(bridge);
            battleTracker.removeBattle(battle, data);
          }
          continue;
        }
        // if only enemy subs... attack them?
        if (!enemyUnits.isEmpty()
            && enemyUnits.stream().allMatch(Matches.unitCanBeMovedThroughByEnemies())) {
          if (!remotePlayer.selectAttackSubs(territory)) {
            final BattleResults results = new BattleResults(battle, WhoWon.NOT_FINISHED, data);
            battleTracker
                .getBattleRecords()
                .addResultToBattle(
                    player,
                    battle.getBattleId(),
                    null,
                    0,
                    0,
                    BattleRecord.BattleResultDescription.NO_BATTLE,
                    results);
            battle.cancelBattle(bridge);
            battleTracker.removeBattle(battle, data);
          }
          continue;
        }
        // if only enemy transports and subs... attack them?
        if (ignoreTransports
            && !enemyUnits.isEmpty()
            && enemyUnits.stream().allMatch(seaTransportsOrSubs)
            && !remotePlayer.selectAttackUnits(territory)) {
          final BattleResults results = new BattleResults(battle, WhoWon.NOT_FINISHED, data);
          battleTracker
              .getBattleRecords()
              .addResultToBattle(
                  player,
                  battle.getBattleId(),
                  null,
                  0,
                  0,
                  BattleRecord.BattleResultDescription.NO_BATTLE,
                  results);
          battle.cancelBattle(bridge);
          battleTracker.removeBattle(battle, data);
        }
      }
    }
  }

  /**
   * Set up the battles where we have abandoned a contested territory during combat move to the
   * enemy. The enemy then takes over the territory in question.
   */
  private static void setupTerritoriesAbandonedToTheEnemy(
      final BattleTracker battleTracker, final IDelegateBridge bridge) {
    final GameState data = bridge.getData();
    if (!Properties.getAbandonedTerritoriesMayBeTakenOverImmediately(data.getProperties())) {
      return;
    }
    final GamePlayer player = bridge.getGamePlayer();
    final List<Territory> battleTerritories =
        CollectionUtils.getMatches(
            data.getMap().getTerritories(),
            Matches.territoryIsNotUnownedWater()
                .and(Matches.territoryHasEnemyUnitsThatCanCaptureItAndIsOwnedByTheirEnemy(player)));
    // all territories that contain enemy units, where the territory is owned by an enemy of these
    // units
    for (final Territory territory : battleTerritories) {
      final List<Unit> abandonedToUnits =
          territory.getUnitCollection().getMatches(Matches.enemyUnit(player));
      final GamePlayer abandonedToPlayer = UnitUtils.findPlayerWithMostUnits(abandonedToUnits);

      // now make sure to add any units that must move with these units, so that they get included
      // as dependencies
      final Map<Unit, Collection<Unit>> transportMap =
          TransportTracker.transporting(territory.getUnitCollection());

      abandonedToUnits.addAll(
          transportMap.entrySet().stream()
              .filter(e -> abandonedToUnits.contains(e.getKey()))
              .map(Entry::getValue)
              .flatMap(Collection::stream)
              .filter(not(abandonedToUnits::contains))
              .collect(Collectors.toSet()));

      // either we have abandoned the territory (so there are no more units that are enemy units of
      // our enemy units)
      // or we are possibly bombing the territory (so we may have units there still)
      final Set<Unit> enemyUnitsOfAbandonedToUnits =
          abandonedToUnits.stream()
              .map(Unit::getOwner)
              .map(
                  p ->
                      Matches.unitIsEnemyOf(p)
                          .and(Matches.unitIsNotAir())
                          .and(Matches.unitIsNotInfrastructure()))
              .map(territory.getUnitCollection()::getMatches)
              .flatMap(Collection::stream)
              .collect(Collectors.toSet());
      // only look at bombing battles, because otherwise the normal attack will determine the
      // ownership of the territory
      final IBattle bombingBattle = battleTracker.getPendingBombingBattle(territory);
      if (bombingBattle != null) {
        enemyUnitsOfAbandonedToUnits.removeAll(bombingBattle.getAttackingUnits());
      }
      if (!enemyUnitsOfAbandonedToUnits.isEmpty()) {
        continue;
      }
      final IBattle nonFightingBattle =
          battleTracker.getPendingBattle(territory, BattleType.NORMAL);
      if (nonFightingBattle != null) {
        throw new IllegalStateException(
            "Should not be possible to have a normal battle in: "
                + territory.getName()
                + " and have abandoned or only bombing there too.");
      }
      final var historyWriter = bridge.getHistoryWriter();
      historyWriter.startEvent(
          String.format(
              "%s has abandoned %s to %s",
              player.getName(), territory.getName(), abandonedToPlayer.getName()),
          abandonedToUnits);
      battleTracker.takeOver(territory, abandonedToPlayer, bridge, null, abandonedToUnits);
      // TODO: if there are multiple defending unit owners, allow picking which one takes over the
      // territory
    }
  }

  private void doScrambling() {
    final GameData data = getData();
    if (!Properties.getScrambleRulesInEffect(data.getProperties())) {
      return;
    }
    final BattleListing pendingBattleSites = battleTracker.getPendingBattleSites();
    final Set<Territory> territoriesWithBattles =
        pendingBattleSites.getNormalBattlesIncludingAirBattles();
    if (Properties.getCanScrambleIntoAirBattles(data.getProperties())) {
      territoriesWithBattles.addAll(
          pendingBattleSites.getStrategicBombingRaidsIncludingAirBattles());
    }

    // now scramble them
    final var scrambleLogic =
        new ScrambleLogic(getData(), player, territoriesWithBattles, battleTracker);
    for (final var territoryToScramblersEntry :
        scrambleLogic.getUnitsThatCanScrambleByDestination().entrySet()) {
      final Territory to = territoryToScramblersEntry.getKey();
      final Map<Territory, Tuple<Collection<Unit>, Collection<Unit>>> scramblers =
          territoryToScramblersEntry.getValue();

      // Remove any units that were already scrambled to other territories.
      scramblers
          .entrySet()
          .removeIf(
              e -> {
                final Collection<Unit> unitsToScramble = e.getValue().getSecond();
                unitsToScramble.retainAll(e.getKey().getUnitCollection());
                return unitsToScramble.isEmpty();
              });

      boolean scrambledHere = false;
      GamePlayer defender = data.getPlayerList().getNullPlayer();
      if (!scramblers.isEmpty()) {
        // Determine defender.
        if (battleTracker.hasPendingNonBombingBattle(to)) {
          defender = AbstractBattle.findDefender(to, player, data);
        }
        // find possible scrambling defending in the from territories
        if (defender.isNull()) {
          defender =
              scramblers.keySet().stream()
                  .map(from -> AbstractBattle.findDefender(from, player, data))
                  .filter(player -> !player.isNull())
                  .findFirst()
                  .orElse(data.getPlayerList().getNullPlayer());
        }
        if (defender.isNull()) {
          continue;
        }

        final Map<Territory, Collection<Unit>> toScramble =
            getRemotePlayer(defender).scrambleUnitsQuery(to, scramblers);
        if (toScramble == null) {
          continue;
        }
        if (!scramblers.keySet().containsAll(toScramble.keySet())) {
          throw new IllegalStateException("Trying to scramble from illegal territory");
        }
        // verify max allowed
        for (final Territory t : scramblers.keySet()) {
          Collection<Unit> units = toScramble.get(t);
          if (units == null) {
            continue;
          }
          if (units.size() > ScrambleLogic.getMaxScrambleCount(scramblers.get(t).getFirst())) {
            throw new IllegalStateException(
                "Trying to scramble "
                    + units.size()
                    + " out of "
                    + t.getName()
                    + ", but max allowed is "
                    + scramblers.get(t).getFirst());
          }
        }

        // Validate players have enough fuel to move there and back
        final Map<GamePlayer, ResourceCollection> playerFuelCost = new HashMap<>();
        for (final Entry<Territory, Collection<Unit>> entry : toScramble.entrySet()) {
          final Map<GamePlayer, ResourceCollection> map =
              Route.getScrambleFuelCostCharge(entry.getValue(), entry.getKey(), to, data);
          for (final var playerAndCostEntry : map.entrySet()) {
            if (playerFuelCost.containsKey(playerAndCostEntry.getKey())) {
              playerFuelCost.get(playerAndCostEntry.getKey()).add(playerAndCostEntry.getValue());
            } else {
              playerFuelCost.put(playerAndCostEntry.getKey(), playerAndCostEntry.getValue());
            }
          }
        }
        for (final Entry<GamePlayer, ResourceCollection> playerAndCost :
            playerFuelCost.entrySet()) {
          if (!playerAndCost
              .getKey()
              .getResources()
              .has(playerAndCost.getValue().getResourcesCopy())) {
            throw new IllegalStateException(
                "Not enough fuel to scramble, player: "
                    + playerAndCost.getKey()
                    + ", needs: "
                    + playerAndCost.getValue());
          }
        }

        final CompositeChange change = new CompositeChange();
        for (final Territory t : toScramble.keySet()) {
          final Collection<Unit> scrambling = toScramble.get(t);
          if (scrambling == null || scrambling.isEmpty()) {
            continue;
          }
          int numberScrambled = scrambling.size();
          final Collection<Unit> airbases =
              t.getUnitCollection().getMatches(scrambleLogic.getAirbaseThatCanScramblePredicate());
          final int maxCanScramble = ScrambleLogic.getMaxScrambleCount(airbases);
          if (maxCanScramble != Integer.MAX_VALUE) {
            // TODO: maybe sort from biggest to smallest first?
            for (final Unit airbase : airbases) {
              final int allowedScramble = airbase.getMaxScrambleCount();
              if (allowedScramble > 0) {
                final int newAllowed;
                if (allowedScramble >= numberScrambled) {
                  newAllowed = allowedScramble - numberScrambled;
                  numberScrambled = 0;
                } else {
                  newAllowed = 0;
                  numberScrambled -= allowedScramble;
                }
                change.add(
                    ChangeFactory.unitPropertyChange(airbase, newAllowed, Unit.MAX_SCRAMBLE_COUNT));
              }
              if (numberScrambled <= 0) {
                break;
              }
            }
          }
          for (final Unit u : scrambling) {
            change.add(ChangeFactory.unitPropertyChange(u, t, Unit.ORIGINATED_FROM));
            change.add(ChangeFactory.unitPropertyChange(u, true, Unit.WAS_SCRAMBLED));
            change.add(Route.getFuelChanges(Set.of(u), new Route(t, to), u.getOwner(), data));
          }
          // should we mark combat, or call setupUnitsInSameTerritoryBattles again?
          change.add(ChangeFactory.moveUnits(t, to, scrambling));
          final var historyWriter = bridge.getHistoryWriter();
          historyWriter.startEvent(
              String.format(
                  "%s scrambles %d units out of %s to defend against the attack in %s",
                  defender.getName(), scrambling.size(), t.getName(), to.getName()),
              scrambling);
          scrambledHere = true;
        }
        if (!change.isEmpty()) {
          bridge.addChange(change);
        }
      }
      if (!scrambledHere) {
        continue;
      }
      // make sure the units join the battle, or create a new battle.
      final IBattle bombing = battleTracker.getPendingBombingBattle(to);
      IBattle battle = battleTracker.getPendingBattle(to, BattleType.NORMAL);
      if (battle == null) {
        final List<Unit> attackingUnits =
            to.getUnitCollection().getMatches(Matches.unitIsOwnedBy(player));
        if (bombing != null) {
          attackingUnits.removeAll(bombing.getAttackingUnits());
        }
        // no need to create a "bombing" battle or air battle, because those are set up
        // automatically whenever the map allows scrambling into an air battle / air raid
        if (attackingUnits.isEmpty()) {
          continue;
        }
        final var historyWriter = bridge.getHistoryWriter();
        historyWriter.startEvent(
            defender.getName() + " scrambles to create a battle in territory " + to.getName());
        // TODO: the attacking sea units do not remember where they came from, so they cannot
        // retreat anywhere. Need to fix.
        battleTracker.addBattle(new RouteScripted(to), attackingUnits, player, bridge, null, null);
        battle = battleTracker.getPendingBattle(to, BattleType.NORMAL);
        if (battle instanceof MustFightBattle) {
          final MustFightBattle mfb = (MustFightBattle) battle;
          if (attackingUnits.stream().anyMatch(Matches.unitIsSeaTransport())) {
            // need to reload the transports since unload only happens after amphibious sea battles
            // are finished
            final CompositeChange reloadTransportChange = new CompositeChange();
            TransportTracker.reloadTransports(attackingUnits, reloadTransportChange);
            if (!reloadTransportChange.isEmpty()) {
              bridge.addChange(reloadTransportChange);
            }
          }
          if (attackingUnits.stream().anyMatch(Matches.unitIsAir().negate())) {
            // TODO: for now, we will hack and say that the attackers came from Everywhere, and hope
            // the user will choose the correct place to retreat to! (TODO: Fix this)
            final Map<Territory, Collection<Unit>> attackingFromMap = new HashMap<>();
            final Predicate<Territory> predicate =
                to.isWater() ? Matches.territoryIsWater() : Matches.territoryIsLand();
            final Collection<Territory> neighbors = data.getMap().getNeighbors(to, predicate);
            // neighbors.removeAll(territoriesWithBattles);
            // neighbors.removeAll(Matches.getMatches(neighbors,
            // Matches.territoryHasEnemyUnits(player, data)));
            for (final Territory t : neighbors) {
              attackingFromMap.put(t, attackingUnits);
            }
            mfb.setAttackingFromMap(attackingFromMap);
          }
        }
      } else if (battle instanceof MustFightBattle) {
        ((MustFightBattle) battle).resetDefendingUnits(player);
      }
      // now make sure any amphibious battles that are dependent on this 'new' sea battle have their
      // dependencies set.
      if (to.isWater()) {
        for (final Territory t : data.getMap().getNeighbors(to, Matches.territoryIsLand())) {
          final IBattle adjacentBattle = battleTracker.getPendingBattle(t, BattleType.NORMAL);
          if (adjacentBattle != null) {
            if (Matches.battleIsAmphibiousWithUnitsAttackingFrom(to).test(adjacentBattle)) {
              battleTracker.addDependency(adjacentBattle, battle);
            }
            if (adjacentBattle instanceof MustFightBattle) {
              // and we want to reset the defenders if the scrambling air has left that battle
              ((MustFightBattle) adjacentBattle).resetDefendingUnits(player);
            }
          }
        }
      }
    }
  }

  private void scramblingCleanup() {
    // return scrambled units to their original territories, or let them move 1 or x to a new
    // territory.
    final GameData data = getData();
    if (!Properties.getScrambleRulesInEffect(data.getProperties())) {
      return;
    }
    final boolean mustReturnToBase = Properties.getScrambledUnitsReturnToBase(data.getProperties());
    for (final Territory t : data.getMap().getTerritories()) {
      int carrierCostOfCurrentTerr = 0;
      final Collection<Unit> wasScrambled =
          t.getUnitCollection().getMatches(Matches.unitWasScrambled());
      for (final Unit u : wasScrambled) {
        final GamePlayer owner = u.getOwner();
        final CompositeChange change = new CompositeChange();
        Territory landingTerr = null;
        final String historyText;
        if (!mustReturnToBase
            || !Matches.isTerritoryAllied(u.getOwner()).test(u.getOriginatedFrom())) {
          final Collection<Territory> possible =
              whereCanAirLand(u, t, owner, data, battleTracker, carrierCostOfCurrentTerr);
          if (possible.size() > 1) {
            String text =
                String.format(
                    "Select territory for air units to land. (Current territory is %s): %s",
                    t.getName(), MyFormatter.unitsToText(List.of(u)));
            landingTerr = getRemotePlayer(owner).selectTerritoryForAirToLand(possible, t, text);
          } else if (possible.size() == 1) {
            landingTerr = CollectionUtils.getAny(possible);
          }
          if (landingTerr == null || landingTerr.equals(t)) {
            carrierCostOfCurrentTerr += AirMovementValidator.carrierCost(List.of(u));
            historyText = "Scrambled unit stays in territory " + t.getName();
          } else {
            historyText =
                "Moving scrambled unit from " + t.getName() + " to " + landingTerr.getName();
          }
        } else {
          landingTerr = u.getOriginatedFrom();
          historyText =
              String.format(
                  "Moving scrambled unit from %s  back to originating territory: %s",
                  t.getName(), landingTerr.getName());
        }
        if (landingTerr != null && !landingTerr.equals(t)) {
          change.add(ChangeFactory.moveUnits(t, landingTerr, List.of(u)));
          change.add(Route.getFuelChanges(Set.of(u), new Route(t, landingTerr), owner, data));
        }
        change.add(ChangeFactory.unitPropertyChange(u, null, Unit.ORIGINATED_FROM));
        change.add(ChangeFactory.unitPropertyChange(u, false, Unit.WAS_SCRAMBLED));
        if (!change.isEmpty()) {
          bridge.getHistoryWriter().startEvent(historyText, u);
          bridge.addChange(change);
        }
      }
    }
  }

  private static void resetMaxScrambleCount(final IDelegateBridge bridge) {
    // reset the tripleaUnit property for all airbases that were used
    final GameState data = bridge.getData();
    if (!Properties.getScrambleRulesInEffect(data.getProperties())) {
      return;
    }
    final CompositeChange change = new CompositeChange();
    for (final Territory t : data.getMap().getTerritories()) {
      final Collection<Unit> airbases = t.getUnitCollection().getMatches(Matches.unitIsAirBase());
      for (final Unit airbase : airbases) {
        final UnitAttachment ua = airbase.getUnitAttachment();
        final int currentMax = airbase.getMaxScrambleCount();
        final int allowedMax = ua.getMaxScrambleCount();
        if (currentMax != allowedMax) {
          change.add(
              ChangeFactory.unitPropertyChange(airbase, allowedMax, Unit.MAX_SCRAMBLE_COUNT));
        }
      }
    }
    if (!change.isEmpty()) {
      bridge.getHistoryWriter().startEvent("Preparing Airbases for Possible Scrambling");
      bridge.addChange(change);
    }
  }

  private void airBattleCleanup() {
    final GameState data = getData();
    if (!Properties.getRaidsMayBePreceededByAirBattles(data.getProperties())) {
      return;
    }
    final CompositeChange change = new CompositeChange();
    for (final Territory t : data.getMap().getTerritories()) {
      for (final Unit u : t.getUnitCollection().getMatches(Matches.unitWasInAirBattle())) {
        change.add(ChangeFactory.unitPropertyChange(u, false, Unit.WAS_IN_AIR_BATTLE));
      }
    }
    if (!change.isEmpty()) {
      bridge.getHistoryWriter().startEvent("Cleaning up after air battles");
      bridge.addChange(change);
    }
  }

  private void checkDefendingPlanesCanLand() {
    final GameData data = getData();
    final Map<Territory, Collection<Unit>> defendingAirThatCanNotLand =
        battleTracker.getDefendingAirThatCanNotLand();
    final boolean isWW2v2orIsSurvivingAirMoveToLand =
        Properties.getWW2V2(data.getProperties())
            || Properties.getSurvivingAirMoveToLand(data.getProperties());
    final Predicate<Unit> alliedDefendingAir =
        Matches.unitIsAir().and(Matches.unitWasScrambled().negate());
    for (final Entry<Territory, Collection<Unit>> entry : defendingAirThatCanNotLand.entrySet()) {
      final Territory battleSite = entry.getKey();
      final Collection<Unit> defendingAir = entry.getValue();
      if (defendingAir == null || defendingAir.isEmpty()) {
        continue;
      }
      defendingAir.retainAll(battleSite.getUnitCollection());
      if (defendingAir.isEmpty()) {
        continue;
      }
      final GamePlayer defender = AbstractBattle.findDefender(battleSite, player, data);
      // Get all land territories where we can land
      final Set<Territory> neighbors = data.getMap().getNeighbors(battleSite);
      final Predicate<Territory> alliedLandTerritories =
          Matches.airCanLandOnThisAlliedNonConqueredLandTerritory(defender);
      // Get those that are neighbors
      final Collection<Territory> canLandHere =
          CollectionUtils.getMatches(neighbors, alliedLandTerritories);
      // Get all sea territories where there are allies
      final Predicate<Territory> neighboringSeaZonesWithAlliedUnits =
          Matches.territoryIsWater().and(Matches.territoryHasAlliedUnits(defender));
      // Get those that are neighbors
      final Collection<Territory> areSeaNeighbors =
          CollectionUtils.getMatches(neighbors, neighboringSeaZonesWithAlliedUnits);
      // Set up match criteria for allied carriers
      final Predicate<Unit> alliedCarrier =
          Matches.unitIsCarrier().and(Matches.alliedUnit(defender));
      // Set up match criteria for allied planes
      final Predicate<Unit> alliedPlane = Matches.unitIsAir().and(Matches.alliedUnit(defender));
      // See if neighboring carriers have any capacity available
      for (final Territory currentTerritory : areSeaNeighbors) {
        // get the capacity of the carriers and cost of fighters
        final Collection<Unit> alliedCarriers =
            currentTerritory.getUnitCollection().getMatches(alliedCarrier);
        final Collection<Unit> alliedPlanes =
            currentTerritory.getUnitCollection().getMatches(alliedPlane);
        final int alliedCarrierCapacity =
            AirMovementValidator.carrierCapacity(alliedCarriers, currentTerritory);
        final int alliedPlaneCost = AirMovementValidator.carrierCost(alliedPlanes);
        // if there is free capacity, add the territory to landing possibilities
        if (alliedCarrierCapacity - alliedPlaneCost >= 1) {
          canLandHere.add(currentTerritory);
        }
      }
      if (isWW2v2orIsSurvivingAirMoveToLand) {
        Territory territory;
        while (canLandHere.size() > 1 && !defendingAir.isEmpty()) {
          territory =
              getRemotePlayer(defender)
                  .selectTerritoryForAirToLand(
                      canLandHere,
                      battleSite,
                      "Select territory for air units to land. (Current territory is "
                          + battleSite.getName()
                          + "): "
                          + MyFormatter.unitsToText(defendingAir));
          // added for test script
          if (territory == null) {
            territory = CollectionUtils.getAny(canLandHere);
          }
          if (territory.isWater()) {
            landPlanesOnCarriers(
                bridge,
                alliedDefendingAir,
                defendingAir,
                alliedCarrier,
                alliedPlane,
                territory,
                battleSite);
          } else {
            moveAirAndLand(bridge, defendingAir, defendingAir, territory, battleSite);
            continue;
          }
          // remove the territory from those available
          canLandHere.remove(territory);
        }
        // Land in the last remaining territory
        if (!canLandHere.isEmpty() && !defendingAir.isEmpty()) {
          territory = CollectionUtils.getAny(canLandHere);
          if (territory.isWater()) {
            landPlanesOnCarriers(
                bridge,
                alliedDefendingAir,
                defendingAir,
                alliedCarrier,
                alliedPlane,
                territory,
                battleSite);
          } else {
            moveAirAndLand(bridge, defendingAir, defendingAir, territory, battleSite);
            continue;
          }
        }
      } else if (!canLandHere.isEmpty()) {
        // now defending air has what can't stay, is there a place we can go?
        // check for an island in this sea zone
        for (final Territory currentTerritory : canLandHere) {
          // only one neighbor, it's an island.
          if (data.getMap().getNeighbors(currentTerritory).size() == 1) {
            moveAirAndLand(bridge, defendingAir, defendingAir, currentTerritory, battleSite);
          }
        }
      }
      if (!defendingAir.isEmpty()) {
        // nowhere to go, they must die
        final var historyWriter = bridge.getHistoryWriter();
        historyWriter.addChildToEvent(
            MyFormatter.unitsToText(defendingAir) + " could not land and were killed",
            new ArrayList<>(defendingAir));
        final Change change = ChangeFactory.removeUnits(battleSite, defendingAir);
        bridge.addChange(change);
      }
    }
  }

  private static void landPlanesOnCarriers(
      final IDelegateBridge bridge,
      final Predicate<Unit> alliedDefendingAir,
      final Collection<Unit> defendingAir,
      final Predicate<Unit> alliedCarrier,
      final Predicate<Unit> alliedPlane,
      final Territory newTerritory,
      final Territory battleSite) {
    // Get the capacity of the carriers in the selected zone
    final Collection<Unit> alliedCarriersSelected =
        newTerritory.getUnitCollection().getMatches(alliedCarrier);
    final Collection<Unit> alliedPlanesSelected =
        newTerritory.getUnitCollection().getMatches(alliedPlane);
    final int alliedCarrierCapacitySelected =
        AirMovementValidator.carrierCapacity(alliedCarriersSelected, newTerritory);
    final int alliedPlaneCostSelected = AirMovementValidator.carrierCost(alliedPlanesSelected);
    // Find the available capacity of the carriers in that territory
    final int territoryCapacity = alliedCarrierCapacitySelected - alliedPlaneCostSelected;
    if (territoryCapacity > 0) {
      // move that number of planes from the battlezone
      // TODO: this seems to assume that the air units all have 1 carrier cost!! fixme
      final Collection<Unit> movingAir =
          CollectionUtils.getNMatches(defendingAir, territoryCapacity, alliedDefendingAir);
      moveAirAndLand(bridge, movingAir, defendingAir, newTerritory, battleSite);
    }
  }

  private static void moveAirAndLand(
      final IDelegateBridge bridge,
      final Collection<Unit> defendingAirBeingMoved,
      final Collection<Unit> defendingAirTotal,
      final Territory newTerritory,
      final Territory battleSite) {
    bridge
        .getHistoryWriter()
        .addChildToEvent(
            MyFormatter.unitsToText(defendingAirBeingMoved)
                + " forced to land in "
                + newTerritory.getName(),
            new ArrayList<>(defendingAirBeingMoved));
    final Change change = ChangeFactory.moveUnits(battleSite, newTerritory, defendingAirBeingMoved);
    bridge.addChange(change);
    // remove those that landed in case it was a carrier
    defendingAirTotal.removeAll(defendingAirBeingMoved);
  }

  /**
   * KamikazeSuicideAttacks are attacks that are made during an Opponent's turn, using Resources
   * that you own that have been designated. The resources are designated in PlayerAttachment, and
   * hold information like the attack power of the resource. KamikazeSuicideAttacks are done in any
   * territory that is a kamikazeZone, and the attacks are done by the original owner of that
   * territory. The user has the option not to do any attacks, and they make target any number of
   * units with any number of resource tokens. The units are then attacked individually by each
   * resource token (meaning that casualties do not get selected because the attacks are targeted).
   * The enemies of current player should decide all their attacks before the attacks are rolled.
   */
  private void doKamikazeSuicideAttacks() {
    final GameState data = getData();
    if (!Properties.getUseKamikazeSuicideAttacks(data.getProperties())) {
      return;
    }
    // the current player is not the one who is doing these attacks, it is the all the enemies of
    // this player who will
    // do attacks
    final Collection<GamePlayer> enemies =
        CollectionUtils.getMatches(data.getPlayerList().getPlayers(), Matches.isAtWar(player));
    if (enemies.isEmpty()) {
      return;
    }
    final Predicate<Unit> canBeAttackedDefault =
        Matches.unitIsOwnedBy(player)
            .and(Matches.unitIsSea())
            .and(Matches.unitIsNotSeaTransportButCouldBeCombatSeaTransport())
            .and(Matches.unitCanEvade().negate());
    final boolean onlyWhereThereAreBattlesOrAmphibious =
        Properties.getKamikazeSuicideAttacksOnlyWhereBattlesAre(data.getProperties());
    final Collection<Territory> pendingBattles = battleTracker.getPendingBattleSites(false);
    // create a list of all kamikaze zones, listed by enemy
    final Map<GamePlayer, Collection<Territory>> kamikazeZonesByEnemy = new HashMap<>();
    for (final Territory t : data.getMap().getTerritories()) {
      final TerritoryAttachment ta = TerritoryAttachment.get(t);
      if (ta == null || !ta.getKamikazeZone()) {
        continue;
      }
      final GamePlayer owner =
          !Properties.getKamikazeSuicideAttacksDoneByCurrentTerritoryOwner(data.getProperties())
              ? ta.getOriginalOwner()
              : t.getOwner();
      if (owner == null) {
        continue;
      }
      if (enemies.contains(owner)) {
        if (t.getUnits().stream().noneMatch(Matches.unitIsOwnedBy(player))) {
          continue;
        }
        // if no battle or amphibious from here, ignore it
        if (onlyWhereThereAreBattlesOrAmphibious && !pendingBattles.contains(t)) {
          if (!Matches.territoryIsWater().test(t)) {
            continue;
          }
          boolean amphib = false;
          final Collection<Territory> landNeighbors =
              data.getMap().getNeighbors(t, Matches.territoryIsLand());
          for (final Territory neighbor : landNeighbors) {
            final IBattle battle = battleTracker.getPendingBattle(neighbor, BattleType.NORMAL);
            if (battle == null) {
              final Map<Territory, Collection<Unit>> whereFrom =
                  battleTracker.getFinishedBattlesUnitAttackFromMap().get(neighbor);
              if (whereFrom != null && whereFrom.containsKey(t)) {
                amphib = true;
                break;
              }
              continue;
            }
            if (battle.isAmphibious()
                && ((battle instanceof MustFightBattle
                        && ((MustFightBattle) battle).getAmphibiousAttackTerritories().contains(t))
                    || (battle instanceof NonFightingBattle
                        && ((NonFightingBattle) battle)
                            .getAmphibiousAttackTerritories()
                            .contains(t)))) {
              amphib = true;
              break;
            }
          }
          if (!amphib) {
            continue;
          }
        }
        kamikazeZonesByEnemy.computeIfAbsent(owner, key -> new ArrayList<>()).add(t);
      }
    }
    if (kamikazeZonesByEnemy.isEmpty()) {
      return;
    }
    for (final Entry<GamePlayer, Collection<Territory>> entry : kamikazeZonesByEnemy.entrySet()) {
      final GamePlayer currentEnemy = entry.getKey();
      final PlayerAttachment pa = PlayerAttachment.get(currentEnemy);
      if (pa == null) {
        continue;
      }
      Predicate<Unit> canBeAttacked = canBeAttackedDefault;
      final Set<UnitType> suicideAttackTargets = pa.getSuicideAttackTargets();
      if (!suicideAttackTargets.isEmpty()) {
        canBeAttacked =
            Matches.unitIsOwnedBy(player).and(Matches.unitIsOfTypes(suicideAttackTargets));
      }
      // See if the player has any attack tokens
      final IntegerMap<Resource> resourcesAndAttackValues = pa.getSuicideAttackResources();
      if (resourcesAndAttackValues.isEmpty()) {
        continue;
      }
      final IntegerMap<Resource> playerResourceCollection =
          currentEnemy.getResources().getResourcesCopy();
      final IntegerMap<Resource> attackTokens = new IntegerMap<>();
      for (final Resource possible : resourcesAndAttackValues.keySet()) {
        final int amount = playerResourceCollection.getInt(possible);
        if (amount > 0) {
          attackTokens.put(possible, amount);
        }
      }
      if (attackTokens.isEmpty()) {
        continue;
      }
      // now let the enemy decide if they will do attacks
      final Collection<Territory> kamikazeZones = entry.getValue();
      final Map<Territory, Collection<Unit>> possibleUnitsToAttack = new HashMap<>();
      for (final Territory t : kamikazeZones) {
        final List<Unit> validTargets = t.getUnitCollection().getMatches(canBeAttacked);
        if (!validTargets.isEmpty()) {
          possibleUnitsToAttack.put(t, validTargets);
        }
      }
      final Map<Territory, Map<Unit, IntegerMap<Resource>>> attacks =
          getRemotePlayer(currentEnemy).selectKamikazeSuicideAttacks(possibleUnitsToAttack);
      if (attacks == null || attacks.isEmpty()) {
        continue;
      }
      // now validate that we have the resources and those units are valid targets
      for (final Entry<Territory, Map<Unit, IntegerMap<Resource>>> territoryEntry :
          attacks.entrySet()) {
        final Territory t = territoryEntry.getKey();
        final Collection<Unit> possibleUnits = possibleUnitsToAttack.get(t);
        if (possibleUnits == null
            || !possibleUnits.containsAll(territoryEntry.getValue().keySet())) {
          throw new IllegalStateException(
              "Player has chosen illegal units during Kamikaze Suicide Attacks");
        }
        for (final IntegerMap<Resource> resourceMap : territoryEntry.getValue().values()) {
          attackTokens.subtract(resourceMap);
        }
      }
      if (!attackTokens.isPositive()) {
        throw new IllegalStateException(
            "Player has chosen illegal resource during Kamikaze Suicide Attacks");
      }
      for (final Entry<Territory, Map<Unit, IntegerMap<Resource>>> territoryEntry :
          attacks.entrySet()) {
        final Territory location = territoryEntry.getKey();
        for (final Entry<Unit, IntegerMap<Resource>> unitEntry :
            territoryEntry.getValue().entrySet()) {
          final Unit unitUnderFire = unitEntry.getKey();
          final IntegerMap<Resource> numberOfAttacks = unitEntry.getValue();
          if (numberOfAttacks != null
              && !numberOfAttacks.isEmpty()
              && numberOfAttacks.totalValues() > 0) {
            fireKamikazeSuicideAttacks(
                unitUnderFire, numberOfAttacks, resourcesAndAttackValues, currentEnemy, location);
          }
        }
      }
    }
  }

  /**
   * This rolls the dice and validates them to see if units died or not. It will use LowLuck or
   * normal dice. If any units die, we remove them from the game, and if units take damage but live,
   * we also do that here.
   */
  private void fireKamikazeSuicideAttacks(
      final Unit unitUnderFire,
      final IntegerMap<Resource> numberOfAttacks,
      final IntegerMap<Resource> resourcesAndAttackValues,
      final GamePlayer firingEnemy,
      final Territory location) {
    // TODO: find a way to autosave after each dice roll.
    final GameData data = getData();
    final int diceSides = data.getDiceSides();
    final CompositeChange change = new CompositeChange();
    int hits = 0;
    int[] rolls = null;
    if (Properties.getLowLuck(data.getProperties())) {
      int power = 0;
      for (final Entry<Resource, Integer> entry : numberOfAttacks.entrySet()) {
        final Resource r = entry.getKey();
        final int num = entry.getValue();
        change.add(ChangeFactory.changeResourcesChange(firingEnemy, r, -num));
        power += num * resourcesAndAttackValues.getInt(r);
      }
      if (power > 0) {
        hits = power / diceSides;
        final int remainder = power % diceSides;
        if (remainder > 0) {
          rolls =
              bridge.getRandom(
                  diceSides,
                  1,
                  firingEnemy,
                  DiceType.COMBAT,
                  "Rolling for remainder in Kamikaze Suicide Attack on unit: "
                      + unitUnderFire.getType().getName());
          if (remainder > rolls[0]) {
            hits++;
          }
        }
      }
    } else {
      // avoid multiple calls of getRandom, so just do it once at the beginning
      final int numTokens = numberOfAttacks.totalValues();
      rolls =
          bridge.getRandom(
              diceSides,
              numTokens,
              firingEnemy,
              DiceType.COMBAT,
              "Rolling for Kamikaze Suicide Attack on unit: " + unitUnderFire.getType().getName());
      final int[] powerOfTokens = new int[numTokens];
      int j = 0;
      for (final Entry<Resource, Integer> entry : numberOfAttacks.entrySet()) {
        final Resource r = entry.getKey();
        int num = entry.getValue();
        change.add(ChangeFactory.changeResourcesChange(firingEnemy, r, -num));
        final int power = resourcesAndAttackValues.getInt(r);
        while (num > 0) {
          powerOfTokens[j] = power;
          j++;
          num--;
        }
      }
      for (int i = 0; i < rolls.length; i++) {
        if (powerOfTokens[i] > rolls[i]) {
          hits++;
        }
      }
    }
    final String title =
        "Kamikaze Suicide Attack attacks " + MyFormatter.unitsToText(Set.of(unitUnderFire));
    final String dice = " scoring " + hits + " hits.  Rolls: " + MyFormatter.asDice(rolls);
    bridge.getHistoryWriter().startEvent(title + dice, unitUnderFire);
    if (hits > 0) {
      final UnitAttachment ua = unitUnderFire.getUnitAttachment();
      final int currentHits = unitUnderFire.getHits();
      if (ua.getHitPoints() <= currentHits + hits) {
        HistoryChangeFactory.removeUnitsFromTerritory(location, List.of(unitUnderFire))
            .perform(bridge);
      } else {
        HistoryChangeFactory.damageUnits(location, IntegerMap.of(Map.of(unitUnderFire, hits)))
            .perform(bridge);
      }
    }
    if (!change.isEmpty()) {
      bridge.addChange(change);
    }
    // kamikaze suicide attacks, even if unsuccessful, deny the ability to bombard from this sea
    // zone
    battleTracker.addNoBombardAllowedFromHere(location);
    // TODO: display this as actual dice for both players
    final Collection<GamePlayer> playersInvolved = new ArrayList<>();
    playersInvolved.add(player);
    playersInvolved.add(firingEnemy);
    bridge
        .getDisplayChannelBroadcaster()
        .reportMessageToPlayers(playersInvolved, null, title + dice, title);
  }

  public static void markDamaged(
      final Collection<Unit> damaged, final IDelegateBridge bridge, final Territory territory) {
    if (damaged.isEmpty()) {
      return;
    }
    final IntegerMap<Unit> damagedMap = new IntegerMap<>();
    for (final Unit u : damaged) {
      damagedMap.add(u, 1);
    }
    HistoryChangeFactory.damageUnits(territory, damagedMap).perform(bridge);
  }

  private static Collection<Territory> whereCanAirLand(
      final Unit strandedAir,
      final Territory currentTerr,
      final GamePlayer alliedPlayer,
      final GameState data,
      final BattleTracker battleTracker,
      final int carrierCostForCurrentTerr) {
    Preconditions.checkNotNull(strandedAir);

    final int maxDistance = strandedAir.getUnitAttachment().getMaxScrambleDistance();
    if (maxDistance <= 0) {
      return List.of(currentTerr);
    }
    final boolean areNeutralsPassableByAir =
        (Properties.getNeutralFlyoverAllowed(data.getProperties())
            && !Properties.getNeutralsImpassable(data.getProperties()));
    final Set<Territory> canNotLand = new HashSet<>();
    canNotLand.addAll(battleTracker.getPendingBattleSites(false));
    canNotLand.addAll(
        CollectionUtils.getMatches(
            data.getMap().getTerritories(), Matches.territoryHasEnemyUnits(alliedPlayer)));
    final Collection<Territory> possibleTerrs =
        data.getMap()
            .getNeighborsByMovementCost(
                currentTerr,
                new BigDecimal(maxDistance),
                Matches.airCanFlyOver(alliedPlayer, areNeutralsPassableByAir));
    final Iterator<Territory> possibleIter = possibleTerrs.iterator();
    while (possibleIter.hasNext()) {
      final Route route =
          data.getMap()
              .getRouteForUnit(
                  currentTerr,
                  possibleIter.next(),
                  Matches.airCanFlyOver(alliedPlayer, areNeutralsPassableByAir),
                  strandedAir,
                  alliedPlayer);
      if ((route == null)
          || (route.getMovementCost(strandedAir).compareTo(new BigDecimal(maxDistance)) > 0)) {
        possibleIter.remove();
      }
    }
    possibleTerrs.add(currentTerr);
    final Set<Territory> availableLand =
        new HashSet<>(
            CollectionUtils.getMatches(
                possibleTerrs,
                Matches.isTerritoryAllied(alliedPlayer).and(Matches.territoryIsLand())));
    availableLand.removeAll(canNotLand);
    final Set<Territory> whereCanLand = new HashSet<>(availableLand);
    // now for carrier-air-landing validation
    if (Matches.unitCanLandOnCarrier().test(strandedAir)) {
      final Set<Territory> availableWater =
          new HashSet<>(
              CollectionUtils.getMatches(
                  possibleTerrs,
                  Matches.territoryHasUnitsThatMatch(Matches.unitIsAlliedCarrier(alliedPlayer))
                      .and(Matches.territoryIsWater())));
      availableWater.removeAll(battleTracker.getPendingBattleSites(false));
      // simple calculation, either we can take all the air, or we can't, nothing in the middle
      final int carrierCost = AirMovementValidator.carrierCost(strandedAir);
      final Iterator<Territory> waterIter = availableWater.iterator();
      while (waterIter.hasNext()) {
        final Territory t = waterIter.next();
        int carrierCapacity =
            AirMovementValidator.carrierCapacity(
                t.getUnitCollection().getMatches(Matches.unitIsAlliedCarrier(alliedPlayer)), t);
        if (!t.equals(currentTerr)) {
          carrierCapacity -=
              AirMovementValidator.carrierCost(
                  t.getUnitCollection()
                      .getMatches(
                          Matches.unitCanLandOnCarrier().and(Matches.alliedUnit(alliedPlayer))));
        } else {
          carrierCapacity -= carrierCostForCurrentTerr;
        }
        if (carrierCapacity < carrierCost) {
          waterIter.remove();
        }
      }
      whereCanLand.addAll(availableWater);
    }
    return whereCanLand;
  }

  @Override
  public Class<? extends IRemote> getRemoteType() {
    return IBattleDelegate.class;
  }

  @Override
  public IBattle getCurrentBattle() {
    return currentBattle;
  }

  public void clearCurrentBattle(final IBattle battle) {
    if (battle.equals(currentBattle)) {
      currentBattle = null;
    }
  }
}
