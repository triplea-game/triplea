package games.strategy.triplea.delegate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.RouteScripted;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.AutoSave;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.triplea.MapSupport;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attachments.PlayerAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.triplea.delegate.IBattle.WhoWon;
import games.strategy.triplea.delegate.dataObjects.BattleListing;
import games.strategy.triplea.delegate.dataObjects.BattleRecord;
import games.strategy.triplea.delegate.remote.IBattleDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.oddsCalculator.ta.BattleResults;
import games.strategy.triplea.player.ITripleAPlayer;
import games.strategy.util.CollectionUtils;
import games.strategy.util.IntegerMap;
import games.strategy.util.PredicateBuilder;
import games.strategy.util.Tuple;
import games.strategy.util.Util;

@MapSupport
@AutoSave(beforeStepStart = true, afterStepEnd = true)
public class BattleDelegate extends BaseTripleADelegate implements IBattleDelegate {
  private BattleTracker battleTracker = new BattleTracker();
  private boolean needToInitialize = true;
  private boolean needToScramble = true;
  private boolean needToKamikazeSuicideAttacks = true;
  private boolean needToClearEmptyAirBattleAttacks = true;
  private boolean needToAddBombardmentSources = true;
  private boolean needToRecordBattleStatistics = true;
  private boolean needToCheckDefendingPlanesCanLand = true;
  private boolean needToCleanup = true;
  private IBattle currentBattle = null;

  @Override
  public void setDelegateBridgeAndPlayer(final IDelegateBridge delegateBridge) {
    super.setDelegateBridgeAndPlayer(new GameDelegateBridge(delegateBridge));
  }

  @Override
  public void start() {
    super.start();
    // we may start multiple times due to loading after saving
    // only initialize once
    if (needToInitialize) {
      doInitialize(battleTracker, bridge);
      needToInitialize = false;
    }
    // do pre-combat stuff, like scrambling, after we have setup all battles, but before we have bombardment, etc.
    // the order of all of this stuff matters quite a bit.
    if (needToScramble) {
      doScrambling();
      needToScramble = false;
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
    needToKamikazeSuicideAttacks = true;
    needToClearEmptyAirBattleAttacks = true;
    needToAddBombardmentSources = true;
    needToRecordBattleStatistics = true;
    needToCleanup = true;
    needToCheckDefendingPlanesCanLand = true;
  }

  @Override
  public Serializable saveState() {
    final BattleExtendedDelegateState state = new BattleExtendedDelegateState();
    state.superState = super.saveState();
    // add other variables to state here:
    state.m_battleTracker = battleTracker;
    state.m_needToInitialize = needToInitialize;
    state.m_needToScramble = needToScramble;
    state.m_needToKamikazeSuicideAttacks = needToKamikazeSuicideAttacks;
    state.m_needToClearEmptyAirBattleAttacks = needToClearEmptyAirBattleAttacks;
    state.m_needToAddBombardmentSources = needToAddBombardmentSources;
    state.m_needToRecordBattleStatistics = needToRecordBattleStatistics;
    state.m_needToCheckDefendingPlanesCanLand = needToCheckDefendingPlanesCanLand;
    state.m_needToCleanup = needToCleanup;
    state.m_currentBattle = currentBattle;
    return state;
  }

  @Override
  public void loadState(final Serializable state) {
    final BattleExtendedDelegateState s = (BattleExtendedDelegateState) state;
    super.loadState(s.superState);
    battleTracker = s.m_battleTracker;
    needToInitialize = s.m_needToInitialize;
    needToScramble = s.m_needToScramble;
    needToKamikazeSuicideAttacks = s.m_needToKamikazeSuicideAttacks;
    needToClearEmptyAirBattleAttacks = s.m_needToClearEmptyAirBattleAttacks;
    needToAddBombardmentSources = s.m_needToAddBombardmentSources;
    needToRecordBattleStatistics = s.m_needToRecordBattleStatistics;
    needToCheckDefendingPlanesCanLand = s.m_needToCheckDefendingPlanesCanLand;
    needToCleanup = s.m_needToCleanup;
    currentBattle = s.m_currentBattle;
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

  static void doInitialize(final BattleTracker battleTracker, final IDelegateBridge bridge) {
    setupUnitsInSameTerritoryBattles(battleTracker, bridge);
    setupTerritoriesAbandonedToTheEnemy(battleTracker, bridge);
    // these are "blitzed" and "conquered" territories without a fight, without a pending
    // battle
    battleTracker.clearFinishedBattles(bridge);
    resetMaxScrambleCount(bridge);
  }

  private static void clearEmptyAirBattleAttacks(final BattleTracker battleTracker, final IDelegateBridge bridge) {
    // these are air battle and air raids where there is no defender, probably because no
    // air is in range to defend
    battleTracker.clearEmptyAirBattleAttacks(bridge);
  }

  @Override
  public String fightCurrentBattle() {
    if (currentBattle == null) {
      return null;
    }
    // fight the battle
    currentBattle.fight(bridge);
    currentBattle = null;
    // and were done
    return null;
  }

  @Override
  public String fightBattle(final Territory territory, final boolean bombing, final BattleType type) {
    final IBattle battle = battleTracker.getPendingBattle(territory, bombing, type);
    if ((currentBattle != null) && (currentBattle != battle)) {
      return "Must finish " + getFightingWord(currentBattle) + " in " + currentBattle.getTerritory() + " first";
    }
    // does the battle exist
    if (battle == null) {
      return "No pending battle in" + territory.getName();
    }
    // are there battles that must occur first
    final Collection<IBattle> allMustPrecede = battleTracker.getDependentOn(battle);
    if (!allMustPrecede.isEmpty()) {
      final IBattle firstPrecede = allMustPrecede.iterator().next();
      final String name = firstPrecede.getTerritory().getName();
      return "Must complete " + getFightingWord(firstPrecede) + " in " + name + " first";
    }
    currentBattle = battle;
    // fight the battle
    battle.fight(bridge);
    currentBattle = null;
    // and were done
    return null;
  }

  private static String getFightingWord(final IBattle battle) {
    return battle.getBattleType().toString();
  }

  @Override
  public BattleListing getBattles() {
    return battleTracker.getPendingBattleSites();
  }

  private static boolean isShoreBombardPerGroundUnitRestricted(final GameData data) {
    return Properties.getShoreBombardPerGroundUnitRestricted(data);
  }

  public BattleTracker getBattleTracker() {
    return battleTracker;
  }

  public IDelegateBridge getBattleBridge() {
    return getBridge();
  }

  /**
   * Add bombardment units to battles. Made public for test purposes only.
   */
  void addBombardmentSources() {
    final PlayerID attacker = bridge.getPlayerId();
    final ITripleAPlayer remotePlayer = getRemotePlayer();
    final Predicate<Unit> ownedAndCanBombard = Matches.unitCanBombard(attacker).and(Matches.unitIsOwnedBy(attacker));
    final Map<Territory, Collection<IBattle>> adjBombardment = getPossibleBombardingTerritories();
    final boolean shoreBombardPerGroundUnitRestricted = isShoreBombardPerGroundUnitRestricted(getData());
    for (final Territory t : adjBombardment.keySet()) {
      if (!battleTracker.hasPendingBattle(t, false)) {
        Collection<IBattle> battles = adjBombardment.get(t);
        battles = CollectionUtils.getMatches(battles, Matches.battleIsAmphibious());
        if (!battles.isEmpty()) {
          final Collection<Unit> bombardUnits = t.getUnits().getMatches(ownedAndCanBombard);
          final List<Unit> listedBombardUnits = new ArrayList<>();
          listedBombardUnits.addAll(bombardUnits);
          sortUnitsToBombard(listedBombardUnits, attacker);
          if (!bombardUnits.isEmpty()) {
            // ask if they want to bombard
            if (!remotePlayer.selectShoreBombard(t)) {
              continue;
            }
          }
          for (final Unit u : listedBombardUnits) {
            final IBattle battle = selectBombardingBattle(u, t, battles);
            if (battle != null) {
              if (shoreBombardPerGroundUnitRestricted) {
                if (battle.getAmphibiousLandAttackers().size() <= battle.getBombardingUnits().size()) {
                  battles.remove(battle);
                  break;
                }
              }
              battle.addBombardingUnit(u);
            }
          }
        }
      }
    }
  }

  /**
   * Sort the specified units in preferred movement or unload order.
   */
  private static void sortUnitsToBombard(final List<Unit> units, final PlayerID player) {
    if (units.isEmpty()) {
      return;
    }
    Collections.sort(units, UnitComparator.getDecreasingAttackComparator(player));
  }

  /**
   * Return map of adjacent territories along attack routes in battles where fighting will occur.
   */
  private Map<Territory, Collection<IBattle>> getPossibleBombardingTerritories() {
    final Map<Territory, Collection<IBattle>> possibleBombardingTerritories = new HashMap<>();
    for (final Territory t : battleTracker.getPendingBattleSites(false)) {
      final IBattle battle = battleTracker.getPendingBattle(t, false, BattleType.NORMAL);
      // we only care about battles where we must fight
      // this check is really to avoid implementing getAttackingFrom() in other battle subclasses
      if (!(battle instanceof MustFightBattle)) {
        continue;
      }
      // bombarding can only occur in territories from which at least 1 land unit attacked
      final Map<Territory, Collection<Unit>> attackingFromMap = ((MustFightBattle) battle).getAttackingFromMap();
      for (final Territory neighbor : ((MustFightBattle) battle).getAttackingFrom()) {
        // we do not allow bombarding from certain sea zones (like if there was a kamikaze suicide attack there, etc)
        if (battleTracker.noBombardAllowedFromHere(neighbor)) {
          continue;
        }
        final Collection<Unit> neighbourUnits = attackingFromMap.get(neighbor);
        // If all units from a territory are air- no bombard
        if (!neighbourUnits.isEmpty() && neighbourUnits.stream().allMatch(Matches.unitIsAir())) {
          continue;
        }
        Collection<IBattle> battles = possibleBombardingTerritories.get(neighbor);
        if (battles == null) {
          battles = new ArrayList<>();
          possibleBombardingTerritories.put(neighbor, battles);
        }
        battles.add(battle);
      }
    }
    return possibleBombardingTerritories;
  }

  /**
   * Select which territory to bombard.
   */
  private IBattle selectBombardingBattle(final Unit u, final Territory unitTerritory,
      final Collection<IBattle> battles) {
    final boolean bombardRestricted = isShoreBombardPerGroundUnitRestricted(getData());
    // If only one battle to select from just return that battle
    if ((battles.size() == 1)) {
      return battles.iterator().next();
    }
    final List<Territory> territories = new ArrayList<>();
    final Map<Territory, IBattle> battleTerritories = new HashMap<>();
    for (final IBattle battle : battles) {
      // If Restricted & # of bombarding units => landing units, don't add territory to list to bombard
      if (bombardRestricted) {
        if (battle.getBombardingUnits().size() < battle.getAmphibiousLandAttackers().size()) {
          territories.add(battle.getTerritory());
        }
      } else {
        territories.add(battle.getTerritory());
      }
      battleTerritories.put(battle.getTerritory(), battle);
    }
    final ITripleAPlayer remotePlayer = getRemotePlayer();
    Territory bombardingTerritory = null;
    if (!territories.isEmpty()) {
      bombardingTerritory = remotePlayer.selectBombardingTerritory(u, unitTerritory, territories, true);
    }
    if (bombardingTerritory != null) {
      return battleTerritories.get(bombardingTerritory);
    }
    // User elected not to bombard with this unit
    return null;
  }

  private static void landParatroopers(final PlayerID player, final Territory battleSite,
      final IDelegateBridge bridge) {
    if (TechTracker.hasParatroopers(player)) {
      final Collection<Unit> airTransports =
          CollectionUtils.getMatches(battleSite.getUnits(), Matches.unitIsAirTransport());
      final Collection<Unit> paratroops =
          CollectionUtils.getMatches(battleSite.getUnits(), Matches.unitIsAirTransportable());
      if (!airTransports.isEmpty() && !paratroops.isEmpty()) {
        final CompositeChange change = new CompositeChange();
        for (final Unit u : paratroops) {
          final TripleAUnit taUnit = (TripleAUnit) u;
          final Unit transport = taUnit.getTransportedBy();
          if ((transport == null) || !airTransports.contains(transport)) {
            continue;
          }
          change.add(TransportTracker.unloadAirTransportChange(taUnit, battleSite, false));
        }
        if (!change.isEmpty()) {
          bridge.getHistoryWriter().startEvent(player.getName() + " lands units in " + battleSite.getName());
          bridge.addChange(change);
        }
      }
    }
  }

  /**
   * Setup the battles where the battle occurs because units are in the
   * same territory. This happens when subs emerge (after being submerged), and
   * when naval units are placed in enemy occupied sea zones, and also
   * when political relationships change and potentially leave units in now-hostile territories.
   */
  private static void setupUnitsInSameTerritoryBattles(final BattleTracker battleTracker,
      final IDelegateBridge bridge) {
    final PlayerID player = bridge.getPlayerId();
    final GameData data = bridge.getData();
    final boolean ignoreTransports = isIgnoreTransportInMovement(data);
    final boolean ignoreSubs = isIgnoreSubInMovement(data);
    final Predicate<Unit> seaTransports = Matches.unitIsTransportButNotCombatTransport().and(Matches.unitIsSea());
    final Predicate<Unit> seaTranportsOrSubs = seaTransports.or(Matches.unitIsSub());
    // we want to match all sea zones with our units and enemy units
    final Predicate<Territory> anyTerritoryWithOwnAndEnemy = Matches.territoryHasUnitsOwnedBy(player)
        .and(Matches.territoryHasEnemyUnits(player, data));
    final Predicate<Territory> enemyTerritoryAndOwnUnits = Matches.isTerritoryEnemyAndNotUnownedWater(player, data)
        .and(Matches.territoryHasUnitsOwnedBy(player));
    final Predicate<Territory> enemyUnitsOrEnemyTerritory = anyTerritoryWithOwnAndEnemy.or(enemyTerritoryAndOwnUnits);
    final List<Territory> battleTerritories =
        CollectionUtils.getMatches(data.getMap().getTerritories(), enemyUnitsOrEnemyTerritory);
    for (final Territory territory : battleTerritories) {
      final List<Unit> attackingUnits = territory.getUnits().getMatches(Matches.unitIsOwnedBy(player));
      // now make sure to add any units that must move with these attacking units, so that they get included as
      // dependencies
      final Map<Unit, Collection<Unit>> transportMap = TransportTracker.transporting(territory.getUnits());
      final HashSet<Unit> dependants = new HashSet<>();
      for (final Entry<Unit, Collection<Unit>> entry : transportMap.entrySet()) {
        // only consider those transports that we are attacking with. allied and enemy transports are not added.
        if (attackingUnits.contains(entry.getKey())) {
          dependants.addAll(entry.getValue());
        }
      }
      // no duplicates
      dependants.removeAll(attackingUnits);
      // add the dependants to the attacking list
      attackingUnits.addAll(dependants);
      final List<Unit> enemyUnits = territory.getUnits().getMatches(Matches.enemyUnit(player, data));
      final IBattle bombingBattle = battleTracker.getPendingBattle(territory, true, null);
      if (bombingBattle != null) {
        // we need to remove any units which are participating in bombing raids
        attackingUnits.removeAll(bombingBattle.getAttackingUnits());
      }
      if (attackingUnits.stream().allMatch(Matches.unitIsInfrastructure())) {
        continue;
      }
      IBattle battle = battleTracker.getPendingBattle(territory, false, BattleType.NORMAL);
      if (battle == null) {
        // we must land any paratroopers here, but only if there is not going to be a battle (cus battles land them
        // separately, after aa
        // fires)
        if (enemyUnits.stream().allMatch(Matches.unitIsInfrastructure())) {
          landParatroopers(player, territory, bridge);
        }
        bridge.getHistoryWriter().startEvent(player.getName() + " creates battle in territory " + territory.getName());
        battleTracker.addBattle(new RouteScripted(territory), attackingUnits, player, bridge, null, null);
        battle = battleTracker.getPendingBattle(territory, false, BattleType.NORMAL);
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
              CollectionUtils.getMatches(attackingUnitsNeedToBeAdded, Matches.unitIsLand().negate());
        } else {
          attackingUnitsNeedToBeAdded =
              CollectionUtils.getMatches(attackingUnitsNeedToBeAdded, Matches.unitIsSea().negate());
        }
        if (!attackingUnitsNeedToBeAdded.isEmpty()) {
          battle.addAttackChange(new RouteScripted(territory), attackingUnitsNeedToBeAdded, null);
        }
      }
      // Reach stalemate if all attacking and defending units are transports
      if ((ignoreTransports && !attackingUnits.isEmpty() && attackingUnits.stream().allMatch(seaTransports)
          && !enemyUnits.isEmpty() && enemyUnits.stream().allMatch(seaTransports))
          || (!attackingUnits.isEmpty()
              && attackingUnits.stream().allMatch(Matches.unitHasAttackValueOfAtLeast(1).negate())
              && !enemyUnits.isEmpty()
              && enemyUnits.stream().allMatch(Matches.unitHasDefendValueOfAtLeast(1).negate()))) {
        final BattleResults results = new BattleResults(battle, WhoWon.DRAW, data);
        battleTracker.getBattleRecords().addResultToBattle(player, battle.getBattleId(), null, 0, 0,
            BattleRecord.BattleResultDescription.STALEMATE, results);
        battle.cancelBattle(bridge);
        battleTracker.removeBattle(battle);
        continue;
      }
      // possibility to ignore battle altogether
      if (!attackingUnits.isEmpty()) {
        final ITripleAPlayer remotePlayer = getRemotePlayer(bridge);
        if (territory.isWater() && Properties.getSeaBattlesMayBeIgnored(data)) {
          if (!remotePlayer.selectAttackUnits(territory)) {
            final BattleResults results = new BattleResults(battle, WhoWon.NOTFINISHED, data);
            battleTracker.getBattleRecords().addResultToBattle(player, battle.getBattleId(), null, 0, 0,
                BattleRecord.BattleResultDescription.NO_BATTLE, results);
            battle.cancelBattle(bridge);
            battleTracker.removeBattle(battle);
          }
          continue;
        }
        // Check for ignored units
        if (ignoreTransports || ignoreSubs) {
          // TODO check if incoming units can attack before asking
          // if only enemy transports... attack them?
          if (ignoreTransports && !enemyUnits.isEmpty() && enemyUnits.stream().allMatch(seaTransports)) {
            if (!remotePlayer.selectAttackTransports(territory)) {
              final BattleResults results = new BattleResults(battle, WhoWon.NOTFINISHED, data);
              battleTracker.getBattleRecords().addResultToBattle(player, battle.getBattleId(), null, 0, 0,
                  BattleRecord.BattleResultDescription.NO_BATTLE, results);
              battle.cancelBattle(bridge);
              battleTracker.removeBattle(battle);
            }
            continue;
          }
          // if only enemy subs... attack them?
          if (ignoreSubs && !enemyUnits.isEmpty() && enemyUnits.stream().allMatch(Matches.unitIsSub())) {
            if (!remotePlayer.selectAttackSubs(territory)) {
              final BattleResults results = new BattleResults(battle, WhoWon.NOTFINISHED, data);
              battleTracker.getBattleRecords().addResultToBattle(player, battle.getBattleId(), null, 0, 0,
                  BattleRecord.BattleResultDescription.NO_BATTLE, results);
              battle.cancelBattle(bridge);
              battleTracker.removeBattle(battle);
            }
            continue;
          }
          // if only enemy transports and subs... attack them?
          if (ignoreSubs && ignoreTransports && !enemyUnits.isEmpty()
              && enemyUnits.stream().allMatch(seaTranportsOrSubs)) {
            if (!remotePlayer.selectAttackUnits(territory)) {
              final BattleResults results = new BattleResults(battle, WhoWon.NOTFINISHED, data);
              battleTracker.getBattleRecords().addResultToBattle(player, battle.getBattleId(), null, 0, 0,
                  BattleRecord.BattleResultDescription.NO_BATTLE, results);
              battle.cancelBattle(bridge);
              battleTracker.removeBattle(battle);
            }
          }
        }
      }
    }
  }

  /**
   * Setup the battles where we have abandoned a contested territory during combat move to the enemy.
   * The enemy then takes over the territory in question.
   */
  private static void setupTerritoriesAbandonedToTheEnemy(final BattleTracker battleTracker,
      final IDelegateBridge bridge) {
    final GameData data = bridge.getData();
    if (!Properties.getAbandonedTerritoriesMayBeTakenOverImmediately(data)) {
      return;
    }
    final PlayerID player = bridge.getPlayerId();
    final List<Territory> battleTerritories = CollectionUtils.getMatches(data.getMap().getTerritories(),
        Matches.territoryIsNotUnownedWater()
            .and(Matches.territoryHasEnemyUnitsThatCanCaptureItAndIsOwnedByTheirEnemy(player, data)));
    // all territories that contain enemy units, where the territory is owned by an enemy of these units
    for (final Territory territory : battleTerritories) {
      final List<Unit> abandonedToUnits = territory.getUnits().getMatches(Matches.enemyUnit(player, data));
      final PlayerID abandonedToPlayer = AbstractBattle.findPlayerWithMostUnits(abandonedToUnits);

      // now make sure to add any units that must move with these units, so that they get included as dependencies
      final Map<Unit, Collection<Unit>> transportMap = TransportTracker.transporting(territory.getUnits());

      abandonedToUnits.addAll(transportMap.entrySet().stream()
          .filter(e -> abandonedToUnits.contains(e.getKey()))
          .map(Entry::getValue)
          .flatMap(Collection::stream)
          .filter(Util.not(abandonedToUnits::contains))
          .collect(Collectors.toSet()));

      // either we have abandoned the territory (so there are no more units that are enemy units of our enemy units)
      // or we are possibly bombing the territory (so we may have units there still)
      final Set<Unit> enemyUnitsOfAbandonedToUnits = abandonedToUnits.stream()
          .map(Unit::getOwner)
          .map(p -> Matches.unitIsEnemyOf(data, p)
              .and(Matches.unitIsNotAir())
              .and(Matches.unitIsNotInfrastructure()))
          .map(territory.getUnits()::getMatches)
          .flatMap(Collection::stream)
          .collect(Collectors.toSet());
      // only look at bombing battles, because otherwise the normal attack will determine the ownership of the territory
      final IBattle bombingBattle = battleTracker.getPendingBattle(territory, true, null);
      if (bombingBattle != null) {
        enemyUnitsOfAbandonedToUnits.removeAll(bombingBattle.getAttackingUnits());
      }
      if (!enemyUnitsOfAbandonedToUnits.isEmpty()) {
        continue;
      }
      final IBattle nonFightingBattle = battleTracker.getPendingBattle(territory, false, BattleType.NORMAL);
      if (nonFightingBattle != null) {
        throw new IllegalStateException("Should not be possible to have a normal battle in: " + territory.getName()
            + " and have abandoned or only bombing there too.");
      }
      bridge.getHistoryWriter().startEvent(
          player.getName() + " has abandoned " + territory.getName() + " to " + abandonedToPlayer.getName(),
          abandonedToUnits);
      battleTracker.takeOver(territory, abandonedToPlayer, bridge, null, abandonedToUnits);
      // TODO: if there are multiple defending unit owners, allow picking which one takes over the territory
    }
  }

  private void doScrambling() {
    // first, figure out all the territories where scrambling units could scramble to
    // then ask the defending player if they wish to scramble units there, and actually move the units there
    final GameData data = getData();
    if (!Properties.getScrambleRulesInEffect(data)) {
      return;
    }
    final boolean fromIslandOnly = Properties.getScrambleFromIslandOnly(data);
    final boolean toSeaOnly = Properties.getScrambleToSeaOnly(data);
    final boolean toAnyAmphibious = Properties.getScrambleToAnyAmphibiousAssault(data);
    final boolean toSbr = Properties.getCanScrambleIntoAirBattles(data);
    int maxScrambleDistance = 0;
    for (final UnitType unitType : data.getUnitTypeList()) {
      final UnitAttachment ua = UnitAttachment.get(unitType);
      if (ua.getCanScramble() && (maxScrambleDistance < ua.getMaxScrambleDistance())) {
        maxScrambleDistance = ua.getMaxScrambleDistance();
      }
    }
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

    final Set<Territory> territoriesWithBattles =
        battleTracker.getPendingBattleSites().getNormalBattlesIncludingAirBattles();
    if (toSbr) {
      territoriesWithBattles
          .addAll(battleTracker.getPendingBattleSites().getStrategicBombingRaidsIncludingAirBattles());
    }
    final Set<Territory> territoriesWithBattlesWater = new HashSet<>();
    territoriesWithBattlesWater.addAll(CollectionUtils.getMatches(territoriesWithBattles, Matches.territoryIsWater()));
    final Set<Territory> territoriesWithBattlesLand = new HashSet<>();
    territoriesWithBattlesLand.addAll(CollectionUtils.getMatches(territoriesWithBattles, Matches.territoryIsLand()));
    final Map<Territory, Set<Territory>> scrambleTerrs = new HashMap<>();
    for (final Territory battleTerr : territoriesWithBattlesWater) {
      final Set<Territory> canScrambleFrom = new HashSet<>(
          CollectionUtils.getMatches(data.getMap().getNeighbors(battleTerr, maxScrambleDistance), canScramble));
      if (!canScrambleFrom.isEmpty()) {
        scrambleTerrs.put(battleTerr, canScrambleFrom);
      }
    }
    for (final Territory battleTerr : territoriesWithBattlesLand) {
      if (!toSeaOnly) {
        final Set<Territory> canScrambleFrom = new HashSet<>(
            CollectionUtils.getMatches(data.getMap().getNeighbors(battleTerr, maxScrambleDistance), canScramble));
        if (!canScrambleFrom.isEmpty()) {
          scrambleTerrs.put(battleTerr, canScrambleFrom);
        }
      }
      final IBattle battle = battleTracker.getPendingBattle(battleTerr, false, BattleType.NORMAL);
      // do not forget we may already have the territory in the list, so we need to add to the collection, not overwrite
      // it.
      if ((battle != null) && battle.isAmphibious() && (battle instanceof DependentBattle)) {
        final Collection<Territory> amphibFromTerrs = ((DependentBattle) battle).getAmphibiousAttackTerritories();
        amphibFromTerrs.removeAll(territoriesWithBattlesWater);
        for (final Territory amphibFrom : amphibFromTerrs) {
          final Set<Territory> canScrambleFrom = scrambleTerrs.getOrDefault(amphibFrom, new HashSet<>());
          if (toAnyAmphibious) {
            canScrambleFrom.addAll(
                CollectionUtils.getMatches(data.getMap().getNeighbors(amphibFrom, maxScrambleDistance), canScramble));
          } else if (canScramble.test(battleTerr)) {
            canScrambleFrom.add(battleTerr);
          }
          if (!canScrambleFrom.isEmpty()) {
            scrambleTerrs.put(amphibFrom, canScrambleFrom);
          }
        }
      }
    }
    // now scrambleTerrs is a list of places we can scramble from
    if (scrambleTerrs.isEmpty()) {
      return;
    }

    final Map<Tuple<Territory, PlayerID>, Collection<Map<Territory, Tuple<Collection<Unit>, Collection<Unit>>>>> scramblersByTerritoryPlayer =
        new HashMap<>();
    for (final Territory to : scrambleTerrs.keySet()) {
      // find who we should ask
      PlayerID defender = null;
      if (battleTracker.hasPendingBattle(to, false)) {
        defender = AbstractBattle.findDefender(to, player, data);
      }
      final Map<Territory, Tuple<Collection<Unit>, Collection<Unit>>> scramblers = new HashMap<>();
      for (final Territory from : scrambleTerrs.get(to)) {
        if (defender == null) {
          defender = AbstractBattle.findDefender(from, player, data);
        }
        // find how many is the max this territory can scramble
        final Collection<Unit> airbases = from.getUnits().getMatches(airbasesCanScramble);
        final int maxCanScramble = getMaxScrambleCount(airbases);
        final Route toBattleRoute = data.getMap().getRoute_IgnoreEnd(from, to, Matches.territoryIsNotImpassable());
        final Collection<Unit> canScrambleAir = from.getUnits().getMatches(Matches.unitIsEnemyOf(data, player)
            .and(Matches.unitCanScramble())
            .and(Matches.unitIsNotDisabled())
            .and(Matches.unitWasScrambled().negate())
            .and(Matches.unitCanScrambleOnRouteDistance(toBattleRoute)));
        if ((maxCanScramble > 0) && !canScrambleAir.isEmpty()) {
          scramblers.put(from, Tuple.of(airbases, canScrambleAir));
        }
      }
      if ((defender == null) || scramblers.isEmpty()) {
        continue;
      }
      final Tuple<Territory, PlayerID> terrPlayer = Tuple.of(to, defender);
      final Collection<Map<Territory, Tuple<Collection<Unit>, Collection<Unit>>>> tempScrambleList =
          scramblersByTerritoryPlayer.getOrDefault(terrPlayer, new ArrayList<>());
      tempScrambleList.add(scramblers);
      scramblersByTerritoryPlayer.put(terrPlayer, tempScrambleList);
    }
    // now scramble them
    for (final Tuple<Territory, PlayerID> terrPlayer : scramblersByTerritoryPlayer.keySet()) {
      final Territory to = terrPlayer.getFirst();
      final PlayerID defender = terrPlayer.getSecond();
      if ((defender == null) || defender.isNull()) {
        continue;
      }
      boolean scrambledHere = false;
      for (final Map<Territory, Tuple<Collection<Unit>, Collection<Unit>>> scramblers : scramblersByTerritoryPlayer
          .get(terrPlayer)) {
        // verify that we didn't already scramble any of these units
        final Iterator<Territory> territoryIter = scramblers.keySet().iterator();
        while (territoryIter.hasNext()) {
          final Territory t = territoryIter.next();
          scramblers.get(t).getSecond().retainAll(t.getUnits());
          if (scramblers.get(t).getSecond().isEmpty()) {
            territoryIter.remove();
          }
        }
        if (scramblers.isEmpty()) {
          continue;
        }
        final Map<Territory, Collection<Unit>> toScramble =
            getRemotePlayer(defender).scrambleUnitsQuery(to, scramblers);
        if (toScramble == null) {
          continue;
        }
        // verify max allowed
        if (!scramblers.keySet().containsAll(toScramble.keySet())) {
          throw new IllegalStateException("Trying to scramble from illegal territory");
        }
        for (final Territory t : scramblers.keySet()) {
          if (toScramble.get(t) == null) {
            continue;
          }
          if (toScramble.get(t).size() > getMaxScrambleCount(scramblers.get(t).getFirst())) {
            throw new IllegalStateException("Trying to scramble " + toScramble.get(t).size() + " out of " + t.getName()
                + ", but max allowed is " + scramblers.get(t).getFirst());
          }
        }
        final CompositeChange change = new CompositeChange();
        for (final Territory t : toScramble.keySet()) {
          final Collection<Unit> scrambling = toScramble.get(t);
          if ((scrambling == null) || scrambling.isEmpty()) {
            continue;
          }
          int numberScrambled = scrambling.size();
          final Collection<Unit> airbases = t.getUnits().getMatches(airbasesCanScramble);
          final int maxCanScramble = getMaxScrambleCount(airbases);
          if (maxCanScramble != Integer.MAX_VALUE) {
            // TODO: maybe sort from biggest to smallest first?
            for (final Unit airbase : airbases) {
              final int allowedScramble = ((TripleAUnit) airbase).getMaxScrambleCount();
              if (allowedScramble > 0) {
                final int newAllowed;
                if (allowedScramble >= numberScrambled) {
                  newAllowed = allowedScramble - numberScrambled;
                  numberScrambled = 0;
                } else {
                  newAllowed = 0;
                  numberScrambled -= allowedScramble;
                }
                change.add(ChangeFactory.unitPropertyChange(airbase, newAllowed, TripleAUnit.MAX_SCRAMBLE_COUNT));
              }
              if (numberScrambled <= 0) {
                break;
              }
            }
          }
          for (final Unit u : scrambling) {
            change.add(ChangeFactory.unitPropertyChange(u, t, TripleAUnit.ORIGINATED_FROM));
            change.add(ChangeFactory.unitPropertyChange(u, true, TripleAUnit.WAS_SCRAMBLED));
          }
          // should we mark combat, or call setupUnitsInSameTerritoryBattles again?
          change.add(ChangeFactory.moveUnits(t, to, scrambling));
          bridge.getHistoryWriter().startEvent(defender.getName() + " scrambles " + scrambling.size()
              + " units out of " + t.getName() + " to defend against the attack in " + to.getName(), scrambling);
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
      final IBattle bombing = battleTracker.getPendingBattle(to, true, null);
      IBattle battle = battleTracker.getPendingBattle(to, false, BattleType.NORMAL);
      if (battle == null) {
        final List<Unit> attackingUnits = to.getUnits().getMatches(Matches.unitIsOwnedBy(player));
        if (bombing != null) {
          attackingUnits.removeAll(bombing.getAttackingUnits());
        }
        // no need to create a "bombing" battle or air battle, because those are set up automatically whenever the map
        // allows scrambling
        // into an air battle / air raid
        if (attackingUnits.isEmpty()) {
          continue;
        }
        bridge.getHistoryWriter()
            .startEvent(defender.getName() + " scrambles to create a battle in territory " + to.getName());
        // TODO: the attacking sea units do not remember where they came from, so they cannot retreat anywhere. Need to
        // fix.
        battleTracker.addBattle(new RouteScripted(to), attackingUnits, player, bridge, null, null);
        battle = battleTracker.getPendingBattle(to, false, BattleType.NORMAL);
        if (battle instanceof MustFightBattle) {
          // this is an ugly mess of hacks, but will have to stay here till all transport related code is gutted and
          // refactored.
          final MustFightBattle mfb = (MustFightBattle) battle;
          final Collection<Territory> neighborsLand = data.getMap().getNeighbors(to, Matches.territoryIsLand());
          if (attackingUnits.stream().anyMatch(Matches.unitIsTransport())) {
            // first, we have to reset the "transportedBy" setting for all the land units that were offloaded
            final CompositeChange change1 = new CompositeChange();
            mfb.reLoadTransports(attackingUnits, change1);
            if (!change1.isEmpty()) {
              bridge.addChange(change1);
            }
            // after that is applied, we have to make a map of all dependencies
            final Map<Unit, Collection<Unit>> dependenciesForMfb =
                TransportTracker.transporting(attackingUnits, attackingUnits);
            for (final Unit transport : CollectionUtils.getMatches(attackingUnits, Matches.unitIsTransport())) {
              // however, the map we add to the newly created battle, cannot hold any units that are NOT in this
              // territory.
              // BUT it must still hold all transports
              if (!dependenciesForMfb.containsKey(transport)) {
                dependenciesForMfb.put(transport, new ArrayList<>());
              }
            }
            final Map<Territory, Map<Unit, Collection<Unit>>> dependencies = new HashMap<>();
            dependencies.put(to, dependenciesForMfb);
            for (final Territory t : neighborsLand) {
              // All other maps, must hold only the transported units that in their territory
              final Collection<Unit> allNeighborUnits =
                  new ArrayList<>(CollectionUtils.getMatches(attackingUnits, Matches.unitIsTransport()));
              allNeighborUnits.addAll(t.getUnits().getMatches(Matches.unitIsLandAndOwnedBy(player)));
              final Map<Unit, Collection<Unit>> dependenciesForNeighbors =
                  TransportTracker.transporting(CollectionUtils.getMatches(allNeighborUnits, Matches.unitIsTransport()),
                      CollectionUtils.getMatches(allNeighborUnits, Matches.unitIsTransport().negate()));
              dependencies.put(t, dependenciesForNeighbors);
            }
            mfb.addDependentUnits(dependencies.get(to));
            for (final Territory territoryNeighborToNewBattle : neighborsLand) {
              final IBattle battleInTerritoryNeighborToNewBattle =
                  battleTracker.getPendingBattle(territoryNeighborToNewBattle, false, BattleType.NORMAL);
              if ((battleInTerritoryNeighborToNewBattle != null)
                  && (battleInTerritoryNeighborToNewBattle instanceof MustFightBattle)) {
                final MustFightBattle mfbattleInTerritoryNeighborToNewBattle =
                    (MustFightBattle) battleInTerritoryNeighborToNewBattle;
                mfbattleInTerritoryNeighborToNewBattle
                    .addDependentUnits(dependencies.get(territoryNeighborToNewBattle));
              } else if ((battleInTerritoryNeighborToNewBattle != null)
                  && (battleInTerritoryNeighborToNewBattle instanceof NonFightingBattle)) {
                final NonFightingBattle nfbattleInTerritoryNeighborToNewBattle =
                    (NonFightingBattle) battleInTerritoryNeighborToNewBattle;
                nfbattleInTerritoryNeighborToNewBattle
                    .addDependentUnits(dependencies.get(territoryNeighborToNewBattle));
              }
            }
          }
          if (attackingUnits.stream().anyMatch(Matches.unitIsAir().negate())) {
            // TODO: for now, we will hack and say that the attackers came from Everywhere, and hope the user will
            // choose the correct place
            // to retreat to! (TODO: Fix this)
            final Map<Territory, Collection<Unit>> attackingFromMap = new HashMap<>();
            final Collection<Territory> neighbors = data.getMap().getNeighbors(to,
                (Matches.territoryIsLand().test(to) ? Matches.territoryIsLand() : Matches.territoryIsWater()));
            // neighbors.removeAll(territoriesWithBattles);
            // neighbors.removeAll(Matches.getMatches(neighbors, Matches.territoryHasEnemyUnits(player, data)));
            for (final Territory t : neighbors) {
              attackingFromMap.put(t, attackingUnits);
            }
            mfb.setAttackingFromAndMap(attackingFromMap);
          }
        }
      } else if (battle instanceof MustFightBattle) {
        ((MustFightBattle) battle).resetDefendingUnits(player, data);
      }
      // now make sure any amphibious battles that are dependent on this 'new' sea battle have their dependencies set.
      if (to.isWater()) {
        for (final Territory t : data.getMap().getNeighbors(to, Matches.territoryIsLand())) {
          final IBattle battleAmphib = battleTracker.getPendingBattle(t, false, BattleType.NORMAL);
          if (battleAmphib != null) {
            if (!battleTracker.getDependentOn(battle).contains(battleAmphib)) {
              battleTracker.addDependency(battleAmphib, battle);
            }
            if (battleAmphib instanceof MustFightBattle) {
              // and we want to reset the defenders if the scrambling air has left that battle
              ((MustFightBattle) battleAmphib).resetDefendingUnits(player, data);
            }
          }
        }
      }
    }
  }

  public static int getMaxScrambleCount(final Collection<Unit> airbases) {
    if (airbases.isEmpty()
        || !airbases.stream().allMatch(Matches.unitIsAirBase().and(Matches.unitIsNotDisabled()))) {
      throw new IllegalStateException("All units must be viable airbases");
    }
    // find how many is the max this territory can scramble
    int maxScrambled = 0;
    for (final Unit base : airbases) {
      final int baseMax = ((TripleAUnit) base).getMaxScrambleCount();
      if (baseMax == -1) {
        return Integer.MAX_VALUE;
      }
      maxScrambled += baseMax;
    }
    return maxScrambled;
  }

  private void scramblingCleanup() {
    // return scrambled units to their original territories, or let them move 1 or x to a new territory.
    final GameData data = getData();
    if (!Properties.getScrambleRulesInEffect(data)) {
      return;
    }
    final boolean mustReturnToBase = Properties.getScrambledUnitsReturnToBase(data);
    for (final Territory t : data.getMap().getTerritories()) {
      int carrierCostOfCurrentTerr = 0;
      final Collection<Unit> wasScrambled = t.getUnits().getMatches(Matches.unitWasScrambled());
      for (final Unit u : wasScrambled) {
        final CompositeChange change = new CompositeChange();
        final Territory originatedFrom = TripleAUnit.get(u).getOriginatedFrom();
        Territory landingTerr = null;
        final String historyText;
        if (!mustReturnToBase || !Matches.isTerritoryAllied(u.getOwner(), data).test(originatedFrom)) {
          final Collection<Territory> possible = whereCanAirLand(Collections.singletonList(u), t, u.getOwner(), data,
              battleTracker, carrierCostOfCurrentTerr, 1, !mustReturnToBase);
          if (possible.size() > 1) {
            landingTerr = getRemotePlayer(u.getOwner()).selectTerritoryForAirToLand(possible, t,
                "Select territory for air units to land. (Current territory is " + t.getName() + "): "
                    + MyFormatter.unitsToText(Collections.singletonList(u)));
          } else if (possible.size() == 1) {
            landingTerr = possible.iterator().next();
          }
          if ((landingTerr == null) || landingTerr.equals(t)) {
            carrierCostOfCurrentTerr += AirMovementValidator.carrierCost(Collections.singletonList(u));
            historyText = "Scrambled unit stays in territory " + t.getName();
          } else {
            historyText = "Moving scrambled unit from " + t.getName() + " to " + landingTerr.getName();
          }
        } else {
          landingTerr = originatedFrom;
          historyText =
              "Moving scrambled unit from " + t.getName() + " back to originating territory: " + landingTerr.getName();
        }
        // if null, we leave it to die
        if (landingTerr != null) {
          change.add(ChangeFactory.moveUnits(t, landingTerr, Collections.singletonList(u)));
        }
        change.add(ChangeFactory.unitPropertyChange(u, null, TripleAUnit.ORIGINATED_FROM));
        change.add(ChangeFactory.unitPropertyChange(u, false, TripleAUnit.WAS_SCRAMBLED));
        if (!change.isEmpty()) {
          bridge.getHistoryWriter().startEvent(historyText, u);
          bridge.addChange(change);
        }
      }
    }
  }

  private static void resetMaxScrambleCount(final IDelegateBridge bridge) {
    // reset the tripleaUnit property for all airbases that were used
    final GameData data = bridge.getData();
    if (!Properties.getScrambleRulesInEffect(data)) {
      return;
    }
    final CompositeChange change = new CompositeChange();
    for (final Territory t : data.getMap().getTerritories()) {
      final Collection<Unit> airbases = t.getUnits().getMatches(Matches.unitIsAirBase());
      for (final Unit u : airbases) {
        final UnitAttachment ua = UnitAttachment.get(u.getType());
        final int currentMax = ((TripleAUnit) u).getMaxScrambleCount();
        final int allowedMax = ua.getMaxScrambleCount();
        if (currentMax != allowedMax) {
          change.add(ChangeFactory.unitPropertyChange(u, allowedMax, TripleAUnit.MAX_SCRAMBLE_COUNT));
        }
      }
    }
    if (!change.isEmpty()) {
      bridge.getHistoryWriter().startEvent("Preparing Airbases for Possible Scrambling");
      bridge.addChange(change);
    }
  }

  private void airBattleCleanup() {
    final GameData data = getData();
    if (!Properties.getRaidsMayBePreceededByAirBattles(data)) {
      return;
    }
    final CompositeChange change = new CompositeChange();
    for (final Territory t : data.getMap().getTerritories()) {
      for (final Unit u : t.getUnits().getMatches(Matches.unitWasInAirBattle())) {
        change.add(ChangeFactory.unitPropertyChange(u, false, TripleAUnit.WAS_IN_AIR_BATTLE));
      }
    }
    if (!change.isEmpty()) {
      bridge.getHistoryWriter().startEvent("Cleaning up after air battles");
      bridge.addChange(change);
    }
  }

  private void checkDefendingPlanesCanLand() {
    final GameData data = getData();
    final Map<Territory, Collection<Unit>> defendingAirThatCanNotLand = battleTracker.getDefendingAirThatCanNotLand();
    final boolean isWW2v2orIsSurvivingAirMoveToLand = Properties.getWW2V2(data)
        || Properties.getSurvivingAirMoveToLand(data);
    final Predicate<Unit> alliedDefendingAir = Matches.unitIsAir().and(Matches.unitWasScrambled().negate());
    for (final Entry<Territory, Collection<Unit>> entry : defendingAirThatCanNotLand.entrySet()) {
      final Territory battleSite = entry.getKey();
      final Collection<Unit> defendingAir = entry.getValue();
      if ((defendingAir == null) || defendingAir.isEmpty()) {
        continue;
      }
      defendingAir.retainAll(battleSite.getUnits());
      if (defendingAir.isEmpty()) {
        continue;
      }
      final PlayerID defender = AbstractBattle.findDefender(battleSite, player, data);
      // Get all land territories where we can land
      final Set<Territory> neighbors = data.getMap().getNeighbors(battleSite);
      final Predicate<Territory> alliedLandTerritories =
          Matches.airCanLandOnThisAlliedNonConqueredLandTerritory(defender, data);
      // Get those that are neighbors
      final Collection<Territory> canLandHere = CollectionUtils.getMatches(neighbors, alliedLandTerritories);
      // Get all sea territories where there are allies
      final Predicate<Territory> neighboringSeaZonesWithAlliedUnits = Matches.territoryIsWater()
          .and(Matches.territoryHasAlliedUnits(defender, data));
      // Get those that are neighbors
      final Collection<Territory> areSeaNeighbors =
          CollectionUtils.getMatches(neighbors, neighboringSeaZonesWithAlliedUnits);
      // Set up match criteria for allied carriers
      final Predicate<Unit> alliedCarrier = Matches.unitIsCarrier().and(Matches.alliedUnit(defender, data));
      // Set up match criteria for allied planes
      final Predicate<Unit> alliedPlane = Matches.unitIsAir().and(Matches.alliedUnit(defender, data));
      // See if neighboring carriers have any capacity available
      for (final Territory currentTerritory : areSeaNeighbors) {
        // get the capacity of the carriers and cost of fighters
        final Collection<Unit> alliedCarriers = currentTerritory.getUnits().getMatches(alliedCarrier);
        final Collection<Unit> alliedPlanes = currentTerritory.getUnits().getMatches(alliedPlane);
        final int alliedCarrierCapacity = AirMovementValidator.carrierCapacity(alliedCarriers, currentTerritory);
        final int alliedPlaneCost = AirMovementValidator.carrierCost(alliedPlanes);
        // if there is free capacity, add the territory to landing possibilities
        if ((alliedCarrierCapacity - alliedPlaneCost) >= 1) {
          canLandHere.add(currentTerritory);
        }
      }
      if (isWW2v2orIsSurvivingAirMoveToLand) {
        Territory territory;
        while ((canLandHere.size() > 1) && (defendingAir.size() > 0)) {
          territory = getRemotePlayer(defender).selectTerritoryForAirToLand(canLandHere, battleSite,
              "Select territory for air units to land. (Current territory is " + battleSite.getName() + "): "
                  + MyFormatter.unitsToText(defendingAir));
          // added for test script
          if (territory == null) {
            territory = canLandHere.iterator().next();
          }
          if (territory.isWater()) {
            landPlanesOnCarriers(bridge, alliedDefendingAir, defendingAir, alliedCarrier, alliedPlane,
                territory, battleSite);
          } else {
            moveAirAndLand(bridge, defendingAir, defendingAir, territory, battleSite);
            continue;
          }
          // remove the territory from those available
          canLandHere.remove(territory);
        }
        // Land in the last remaining territory
        if ((canLandHere.size() > 0) && (defendingAir.size() > 0)) {
          territory = canLandHere.iterator().next();
          if (territory.isWater()) {
            landPlanesOnCarriers(bridge, alliedDefendingAir, defendingAir, alliedCarrier, alliedPlane,
                territory, battleSite);
          } else {
            moveAirAndLand(bridge, defendingAir, defendingAir, territory, battleSite);
            continue;
          }
        }
      } else if (canLandHere.size() > 0) {
        // now defending air has what cant stay, is there a place we can go?
        // check for an island in this sea zone
        for (final Territory currentTerritory : canLandHere) {
          // only one neighbor, its an island.
          if (data.getMap().getNeighbors(currentTerritory).size() == 1) {
            moveAirAndLand(bridge, defendingAir, defendingAir, currentTerritory, battleSite);
          }
        }
      }
      if (defendingAir.size() > 0) {
        // no where to go, they must die
        bridge.getHistoryWriter().addChildToEvent(
            MyFormatter.unitsToText(defendingAir) + " could not land and were killed",
            new ArrayList<>(defendingAir));
        final Change change = ChangeFactory.removeUnits(battleSite, defendingAir);
        bridge.addChange(change);
      }
    }
  }

  private static void landPlanesOnCarriers(final IDelegateBridge bridge, final Predicate<Unit> alliedDefendingAir,
      final Collection<Unit> defendingAir, final Predicate<Unit> alliedCarrier,
      final Predicate<Unit> alliedPlane, final Territory newTerritory, final Territory battleSite) {
    // Get the capacity of the carriers in the selected zone
    final Collection<Unit> alliedCarriersSelected = newTerritory.getUnits().getMatches(alliedCarrier);
    final Collection<Unit> alliedPlanesSelected = newTerritory.getUnits().getMatches(alliedPlane);
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

  private static void moveAirAndLand(final IDelegateBridge bridge, final Collection<Unit> defendingAirBeingMoved,
      final Collection<Unit> defendingAirTotal, final Territory newTerritory, final Territory battleSite) {
    bridge.getHistoryWriter().addChildToEvent(
        MyFormatter.unitsToText(defendingAirBeingMoved) + " forced to land in " + newTerritory.getName(),
        new ArrayList<>(defendingAirBeingMoved));
    final Change change = ChangeFactory.moveUnits(battleSite, newTerritory, defendingAirBeingMoved);
    bridge.addChange(change);
    // remove those that landed in case it was a carrier
    defendingAirTotal.removeAll(defendingAirBeingMoved);
  }

  /**
   * KamikazeSuicideAttacks are attacks that are made during an Opponent's turn, using Resources that you own that have
   * been designated.
   * The resources are designated in PlayerAttachment, and hold information like the attack power of the resource.
   * KamikazeSuicideAttacks are done in any territory that is a kamikazeZone, and the attacks are done by the original
   * owner of that
   * territory.
   * The user has the option not to do any attacks, and they make target any number of units with any number of resource
   * tokens.
   * The units are then attacked individually by each resource token (meaning that casualties do not get selected
   * because the attacks are
   * targeted).
   * The enemies of current player should decide all their attacks before the attacks are rolled.
   */
  private void doKamikazeSuicideAttacks() {
    final GameData data = getData();
    if (!Properties.getUseKamikazeSuicideAttacks(data)) {
      return;
    }
    // the current player is not the one who is doing these attacks, it is the all the enemies of this player who will
    // do attacks
    final Collection<PlayerID> enemies =
        CollectionUtils.getMatches(data.getPlayerList().getPlayers(), Matches.isAtWar(player, data));
    if (enemies.isEmpty()) {
      return;
    }
    final Predicate<Unit> canBeAttackedDefault = Matches.unitIsOwnedBy(player)
        .and(Matches.unitIsSea())
        .and(Matches.unitIsNotTransportButCouldBeCombatTransport())
        .and(Matches.unitIsNotSub());
    final boolean onlyWhereThereAreBattlesOrAmphibious =
        Properties.getKamikazeSuicideAttacksOnlyWhereBattlesAre(data);
    final Collection<Territory> pendingBattles = battleTracker.getPendingBattleSites(false);
    // create a list of all kamikaze zones, listed by enemy
    final Map<PlayerID, Collection<Territory>> kamikazeZonesByEnemy = new HashMap<>();
    for (final Territory t : data.getMap().getTerritories()) {
      final TerritoryAttachment ta = TerritoryAttachment.get(t);
      if ((ta == null) || !ta.getKamikazeZone()) {
        continue;
      }
      final PlayerID owner = !Properties.getKamikazeSuicideAttacksDoneByCurrentTerritoryOwner(data)
          ? ta.getOriginalOwner()
          : t.getOwner();
      if (owner == null) {
        continue;
      }
      if (enemies.contains(owner)) {
        if (t.getUnits().getUnits().stream().noneMatch(Matches.unitIsOwnedBy(player))) {
          continue;
        }
        if (onlyWhereThereAreBattlesOrAmphibious) {
          // if no battle or amphibious from here, ignore it
          if (!pendingBattles.contains(t)) {
            if (!Matches.territoryIsWater().test(t)) {
              continue;
            }
            boolean amphib = false;
            final Collection<Territory> landNeighbors = data.getMap().getNeighbors(t, Matches.territoryIsLand());
            for (final Territory neighbor : landNeighbors) {
              final IBattle battle = battleTracker.getPendingBattle(neighbor, false, BattleType.NORMAL);
              if (battle == null) {
                final Map<Territory, Collection<Unit>> whereFrom =
                    battleTracker.getFinishedBattlesUnitAttackFromMap().get(neighbor);
                if ((whereFrom != null) && whereFrom.containsKey(t)) {
                  amphib = true;
                  break;
                }
                continue;
              }
              if (battle.isAmphibious() && (((battle instanceof MustFightBattle)
                  && ((MustFightBattle) battle).getAmphibiousAttackTerritories().contains(t))
                  || ((battle instanceof NonFightingBattle)
                  && ((NonFightingBattle) battle).getAmphibiousAttackTerritories().contains(t)))) {
                amphib = true;
                break;
              }
            }
            if (!amphib) {
              continue;
            }
          }
        }
        final Collection<Territory> currentTerrs = kamikazeZonesByEnemy.getOrDefault(owner, new ArrayList<>());
        currentTerrs.add(t);
        kamikazeZonesByEnemy.put(owner, currentTerrs);
      }
    }
    if (kamikazeZonesByEnemy.isEmpty()) {
      return;
    }
    for (final Entry<PlayerID, Collection<Territory>> entry : kamikazeZonesByEnemy.entrySet()) {
      final PlayerID currentEnemy = entry.getKey();
      final PlayerAttachment pa = PlayerAttachment.get(currentEnemy);
      if (pa == null) {
        continue;
      }
      Predicate<Unit> canBeAttacked = canBeAttackedDefault;
      final Set<UnitType> suicideAttackTargets = pa.getSuicideAttackTargets();
      if (suicideAttackTargets != null) {
        canBeAttacked = Matches.unitIsOwnedBy(player).and(Matches.unitIsOfTypes(suicideAttackTargets));
      }
      // See if the player has any attack tokens
      final IntegerMap<Resource> resourcesAndAttackValues = pa.getSuicideAttackResources();
      if (resourcesAndAttackValues.size() <= 0) {
        continue;
      }
      final IntegerMap<Resource> playerResourceCollection = currentEnemy.getResources().getResourcesCopy();
      final IntegerMap<Resource> attackTokens = new IntegerMap<>();
      for (final Resource possible : resourcesAndAttackValues.keySet()) {
        final int amount = playerResourceCollection.getInt(possible);
        if (amount > 0) {
          attackTokens.put(possible, amount);
        }
      }
      if (attackTokens.size() <= 0) {
        continue;
      }
      // now let the enemy decide if they will do attacks
      final Collection<Territory> kamikazeZones = entry.getValue();
      final HashMap<Territory, Collection<Unit>> possibleUnitsToAttack = new HashMap<>();
      for (final Territory t : kamikazeZones) {
        final List<Unit> validTargets = t.getUnits().getMatches(canBeAttacked);
        if (!validTargets.isEmpty()) {
          possibleUnitsToAttack.put(t, validTargets);
        }
      }
      final Map<Territory, HashMap<Unit, IntegerMap<Resource>>> attacks =
          getRemotePlayer(currentEnemy).selectKamikazeSuicideAttacks(possibleUnitsToAttack);
      if ((attacks == null) || attacks.isEmpty()) {
        continue;
      }
      // now validate that we have the resources and those units are valid targets
      for (final Entry<Territory, HashMap<Unit, IntegerMap<Resource>>> territoryEntry : attacks.entrySet()) {
        final Territory t = territoryEntry.getKey();
        final Collection<Unit> possibleUnits = possibleUnitsToAttack.get(t);
        if ((possibleUnits == null) || !possibleUnits.containsAll(territoryEntry.getValue().keySet())) {
          throw new IllegalStateException("Player has chosen illegal units during Kamikaze Suicide Attacks");
        }
        for (final IntegerMap<Resource> resourceMap : territoryEntry.getValue().values()) {
          attackTokens.subtract(resourceMap);
        }
      }
      if (!attackTokens.isPositive()) {
        throw new IllegalStateException("Player has chosen illegal resource during Kamikaze Suicide Attacks");
      }
      for (final Entry<Territory, HashMap<Unit, IntegerMap<Resource>>> territoryEntry : attacks.entrySet()) {
        final Territory location = territoryEntry.getKey();
        for (final Entry<Unit, IntegerMap<Resource>> unitEntry : territoryEntry.getValue().entrySet()) {
          final Unit unitUnderFire = unitEntry.getKey();
          final IntegerMap<Resource> numberOfAttacks = unitEntry.getValue();
          if ((numberOfAttacks != null) && (numberOfAttacks.size() > 0) && (numberOfAttacks.totalValues() > 0)) {
            fireKamikazeSuicideAttacks(unitUnderFire, numberOfAttacks, resourcesAndAttackValues, currentEnemy,
                location);
          }
        }
      }
    }
  }

  /**
   * This rolls the dice and validates them to see if units died or not.
   * It will use LowLuck or normal dice.
   * If any units die, we remove them from the game, and if units take damage but live, we also do that here.
   */
  private void fireKamikazeSuicideAttacks(final Unit unitUnderFire, final IntegerMap<Resource> numberOfAttacks,
      final IntegerMap<Resource> resourcesAndAttackValues, final PlayerID firingEnemy, final Territory location) {
    // TODO: find a way to autosave after each dice roll.
    final GameData data = getData();
    final int diceSides = data.getDiceSides();
    final CompositeChange change = new CompositeChange();
    int hits = 0;
    int[] rolls = null;
    if (Properties.getLowLuck(data)) {
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
          rolls = bridge.getRandom(diceSides, 1, firingEnemy, DiceType.COMBAT,
              "Rolling for remainder in Kamikaze Suicide Attack on unit: " + unitUnderFire.getType().getName());
          if (remainder > rolls[0]) {
            hits++;
          }
        }
      }
    } else {
      // avoid multiple calls of getRandom, so just do it once at the beginning
      final int numTokens = numberOfAttacks.totalValues();
      rolls = bridge.getRandom(diceSides, numTokens, firingEnemy, DiceType.COMBAT,
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
        "Kamikaze Suicide Attack attacks " + MyFormatter.unitsToText(Collections.singleton(unitUnderFire));
    final String dice = " scoring " + hits + " hits.  Rolls: " + MyFormatter.asDice(rolls);
    bridge.getHistoryWriter().startEvent(title + dice, unitUnderFire);
    if (hits > 0) {
      final UnitAttachment ua = UnitAttachment.get(unitUnderFire.getType());
      final int currentHits = unitUnderFire.getHits();
      if (ua.getHitPoints() <= (currentHits + hits)) {
        // TODO: kill dependents
        change.add(ChangeFactory.removeUnits(location, Collections.singleton(unitUnderFire)));
      } else {
        final IntegerMap<Unit> hitMap = new IntegerMap<>();
        hitMap.put(unitUnderFire, hits);
        change.add(createDamageChange(hitMap, bridge));
      }
    }
    if (!change.isEmpty()) {
      bridge.addChange(change);
    }
    // kamikaze suicide attacks, even if unsuccessful, deny the ability to bombard from this sea zone
    battleTracker.addNoBombardAllowedFromHere(location);
    // TODO: display this as actual dice for both players
    final Collection<PlayerID> playersInvolved = new ArrayList<>();
    playersInvolved.add(player);
    playersInvolved.add(firingEnemy);
    this.getDisplay().reportMessageToPlayers(playersInvolved, null, title + dice, title);
  }

  static void markDamaged(final Collection<Unit> damaged, final IDelegateBridge bridge) {
    if (damaged.size() == 0) {
      return;
    }
    final IntegerMap<Unit> damagedMap = new IntegerMap<>();
    for (final Unit u : damaged) {
      damagedMap.add(u, 1);
    }
    bridge.addChange(createDamageChange(damagedMap, bridge));
  }

  private static Change createDamageChange(final IntegerMap<Unit> damagedMap, final IDelegateBridge bridge) {
    final Set<Unit> units = new HashSet<>(damagedMap.keySet());
    for (final Unit u : units) {
      damagedMap.add(u, u.getHits());
    }
    final Change damagedChange = ChangeFactory.unitsHit(damagedMap);
    bridge.getHistoryWriter().addChildToEvent("Units damaged: " + MyFormatter.unitsToText(units), units);
    return damagedChange;
  }

  private static Collection<Territory> whereCanAirLand(final Collection<Unit> strandedAir, final Territory currentTerr,
      final PlayerID alliedPlayer, final GameData data, final BattleTracker battleTracker,
      final int carrierCostForCurrentTerr, final int allowedMovement,
      final boolean useMaxScrambleDistance) {
    int maxDistance = allowedMovement;
    if ((maxDistance > 1) || useMaxScrambleDistance) {
      UnitType ut = null;
      for (final Unit u : strandedAir) {
        if (ut == null) {
          ut = u.getType();
        } else if (!ut.equals(u.getType())) {
          throw new IllegalStateException(
              "whereCanAirLand can only accept 1 UnitType if byMovementCost or scrambled is true");
        }
      }
      if (useMaxScrambleDistance) {
        maxDistance = UnitAttachment.get(ut).getMaxScrambleDistance();
      }
    }
    if ((maxDistance < 1) || (strandedAir == null) || strandedAir.isEmpty()) {
      return Collections.singletonList(currentTerr);
    }
    final boolean areNeutralsPassableByAir = (Properties.getNeutralFlyoverAllowed(data)
        && !Properties.getNeutralsImpassable(data));
    final HashSet<Territory> canNotLand = new HashSet<>();
    canNotLand.addAll(battleTracker.getPendingBattleSites(false));
    canNotLand.addAll(
        CollectionUtils.getMatches(data.getMap().getTerritories(), Matches.territoryHasEnemyUnits(alliedPlayer, data)));
    final Collection<Territory> possibleTerrs =
        new ArrayList<>(data.getMap().getNeighbors(currentTerr, maxDistance));
    if (maxDistance > 1) {
      final Iterator<Territory> possibleIter = possibleTerrs.iterator();
      while (possibleIter.hasNext()) {
        final Route route = data.getMap().getRoute(currentTerr, possibleIter.next(),
            Matches.airCanFlyOver(alliedPlayer, data, areNeutralsPassableByAir));
        if ((route == null) || (route.getMovementCost(strandedAir.iterator().next()) > maxDistance)) {
          possibleIter.remove();
        }
      }
    }
    possibleTerrs.add(currentTerr);
    final HashSet<Territory> availableLand = new HashSet<>();
    availableLand.addAll(CollectionUtils.getMatches(possibleTerrs,
        Matches.isTerritoryAllied(alliedPlayer, data).and(Matches.territoryIsLand())));
    availableLand.removeAll(canNotLand);
    final HashSet<Territory> whereCanLand = new HashSet<>();
    whereCanLand.addAll(availableLand);
    // now for carrier-air-landing validation
    if (!strandedAir.isEmpty() && strandedAir.stream().allMatch(Matches.unitCanLandOnCarrier())) {
      final HashSet<Territory> availableWater = new HashSet<>();
      availableWater.addAll(CollectionUtils.getMatches(possibleTerrs,
          Matches.territoryHasUnitsThatMatch(Matches.unitIsAlliedCarrier(alliedPlayer, data))
              .and(Matches.territoryIsWater())));
      availableWater.removeAll(battleTracker.getPendingBattleSites(false));
      // a rather simple calculation, either we can take all the air, or we can't, nothing in the middle
      final int carrierCost = AirMovementValidator.carrierCost(strandedAir);
      final Iterator<Territory> waterIter = availableWater.iterator();
      while (waterIter.hasNext()) {
        final Territory t = waterIter.next();
        int carrierCapacity = AirMovementValidator
            .carrierCapacity(t.getUnits().getMatches(Matches.unitIsAlliedCarrier(alliedPlayer, data)), t);
        if (!t.equals(currentTerr)) {
          carrierCapacity -= AirMovementValidator.carrierCost(t.getUnits().getMatches(
              Matches.unitCanLandOnCarrier().and(Matches.alliedUnit(alliedPlayer, data))));
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

  private static boolean isIgnoreTransportInMovement(final GameData data) {
    return Properties.getIgnoreTransportInMovement(data);
  }

  private static boolean isIgnoreSubInMovement(final GameData data) {
    return Properties.getIgnoreSubInMovement(data);
  }

  @Override
  public Class<? extends IRemote> getRemoteType() {
    return IBattleDelegate.class;
  }

  @Override
  public Territory getCurrentBattleTerritory() {
    final IBattle b = currentBattle;
    return (b != null) ? b.getTerritory() : null;
  }

  @Override
  public IBattle getCurrentBattle() {
    return currentBattle;
  }
}
