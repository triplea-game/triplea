package games.strategy.triplea.oddsCalculator.ta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.IntStream;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitHitsChange;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.UnitTypeList;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.framework.GameDataUtils;
import games.strategy.engine.framework.IGameModifiedChannel;
import games.strategy.engine.gamePlayer.IRemotePlayer;
import games.strategy.engine.history.DelegateHistoryWriter;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.engine.random.PlainRandomSource;
import games.strategy.net.GUID;
import games.strategy.sound.HeadlessSoundChannel;
import games.strategy.sound.ISound;
import games.strategy.triplea.ai.AbstractAi;
import games.strategy.triplea.ai.AiUtils;
import games.strategy.triplea.delegate.BattleTracker;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.GameDelegateBridge;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MustFightBattle;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.delegate.dataObjects.CasualtyList;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.delegate.remote.ITechDelegate;
import games.strategy.triplea.ui.display.HeadlessDisplay;
import games.strategy.triplea.ui.display.ITripleADisplay;
import games.strategy.util.CollectionUtils;
import games.strategy.util.Tuple;

public class OddsCalculator implements IOddsCalculator, Callable<AggregateResults> {
  public static final String OOL_ALL = "*";

  public static final String OOL_SEPARATOR = ";";
  public static final String OOL_SEPARATOR_REGEX = ";";
  public static final String OOL_AMOUNT_DESCRIPTOR = "^";
  public static final String OOL_AMOUNT_DESCRIPTOR_REGEX = "\\^";

  private GameData gameData = null;
  private PlayerID attacker = null;
  private PlayerID defender = null;
  private Territory location = null;
  private Collection<Unit> attackingUnits = new ArrayList<>();
  private Collection<Unit> defendingUnits = new ArrayList<>();
  private Collection<Unit> bombardingUnits = new ArrayList<>();
  private Collection<TerritoryEffect> territoryEffects = new ArrayList<>();
  private boolean keepOneAttackingLandUnit = false;
  private boolean amphibious = false;
  private int retreatAfterRound = -1;
  private int retreatAfterXUnitsLeft = -1;
  private boolean retreatWhenOnlyAirLeft = false;
  private String attackerOrderOfLosses = null;
  private String defenderOrderOfLosses = null;
  private int runCount = 0;
  private volatile boolean cancelled = false;
  private volatile boolean isDataSet = false;
  private volatile boolean isCalcSet = false;
  private volatile boolean isRunning = false;

  public OddsCalculator(final GameData data) {
    this(data, false);
  }

  OddsCalculator(final GameData data, final boolean dataHasAlreadyBeenCloned) {
    gameData = data == null ? null : (dataHasAlreadyBeenCloned ? data : GameDataUtils.cloneGameData(data, false));
    if (data != null) {
      isDataSet = true;
    }
  }

  @Override
  public void setGameData(final GameData data) {
    if (isRunning) {
      return;
    }
    isDataSet = false;
    isCalcSet = false;
    gameData = (data == null ? null : GameDataUtils.cloneGameData(data, false));
    // reset old data
    attacker = null;
    defender = null;
    location = null;
    attackingUnits = new ArrayList<>();
    defendingUnits = new ArrayList<>();
    bombardingUnits = new ArrayList<>();
    territoryEffects = new ArrayList<>();
    runCount = 0;
    if (data != null) {
      isDataSet = true;
    }
  }

  /**
   * Calculates odds using the stored game data.
   */
  @Override
  public void setCalculateData(final PlayerID attacker, final PlayerID defender, final Territory location,
      final Collection<Unit> attacking, final Collection<Unit> defending, final Collection<Unit> bombarding,
      final Collection<TerritoryEffect> territoryEffects, final int runCount) throws IllegalStateException {
    if (isRunning) {
      return;
    }
    isCalcSet = false;
    if (!isDataSet) {
      throw new IllegalStateException("Called set calculation before setting game data!");
    }
    this.attacker =
        gameData.getPlayerList().getPlayerId(attacker == null ? PlayerID.NULL_PLAYERID.getName() : attacker.getName());
    this.defender =
        gameData.getPlayerList().getPlayerId(defender == null ? PlayerID.NULL_PLAYERID.getName() : defender.getName());
    this.location = gameData.getMap().getTerritory(location.getName());
    attackingUnits = GameDataUtils.translateIntoOtherGameData(attacking, gameData);
    defendingUnits = GameDataUtils.translateIntoOtherGameData(defending, gameData);
    bombardingUnits = GameDataUtils.translateIntoOtherGameData(bombarding, gameData);
    this.territoryEffects = GameDataUtils.translateIntoOtherGameData(territoryEffects, gameData);
    gameData.performChange(ChangeFactory.removeUnits(this.location, this.location.getUnits().getUnits()));
    gameData.performChange(ChangeFactory.addUnits(this.location, attackingUnits));
    gameData.performChange(ChangeFactory.addUnits(this.location, defendingUnits));
    this.runCount = runCount;
    isCalcSet = true;
  }

  @Override
  public AggregateResults calculate() {
    if (!getIsReady()) {
      throw new IllegalStateException("Called calculate before setting calculate data!");
    }
    return calculate(runCount);
  }

  private AggregateResults calculate(final int count) {
    isRunning = true;
    final long start = System.currentTimeMillis();
    // CasualtySortingCaching can cause issues if there is more than 1 one battle being calced at the same time (like if
    // the AI and a human
    // are both using the calc)
    // TODO: first, see how much it actually speeds stuff up by, and if it does make a difference then convert it to a
    // per-thread, per-calc
    // caching
    final List<Unit> attackerOrderOfLosses =
        OddsCalculator.getUnitListByOrderOfLoss(this.attackerOrderOfLosses, attackingUnits, gameData);
    final List<Unit> defenderOrderOfLosses =
        OddsCalculator.getUnitListByOrderOfLoss(this.defenderOrderOfLosses, defendingUnits, gameData);
    final AggregateResults aggregateResults = IntStream.range(0, count).parallel()
        .filter(i -> !cancelled)
        .mapToObj(i -> {
          final CompositeChange allChanges = new CompositeChange();
          final BattleTracker battleTracker = new BattleTracker();
          try {
            final DummyDelegateBridge bridge1 =
                new DummyDelegateBridge(attacker, gameData, allChanges, attackerOrderOfLosses, defenderOrderOfLosses,
                    keepOneAttackingLandUnit, retreatAfterRound, retreatAfterXUnitsLeft, retreatWhenOnlyAirLeft);
            final GameDelegateBridge bridge = new GameDelegateBridge(bridge1);
            final MustFightBattle battle = new MustFightBattle(location, attacker, gameData, battleTracker);
            battle.setHeadless(true);
            battle.isAmphibious();
            battle.setUnits(defendingUnits, attackingUnits, bombardingUnits,
                (amphibious ? attackingUnits : new ArrayList<>()), defender, territoryEffects);
            bridge1.setBattle(battle);
            battle.fight(bridge);
            return new BattleResults(battle, gameData);
          } finally {
            // restore the game to its original state
            gameData.performChange(allChanges.invert());
            battleTracker.clear();
            battleTracker.clearBattleRecords();
          }
        }).collect(Collector.of(() -> new AggregateResults(count), (a, b) -> a.addResult(b), (a1, a2) -> {
          a1.addResults(a2.getResults());
          return a1;
        }));
    aggregateResults.setTime(System.currentTimeMillis() - start);
    isRunning = false;
    cancelled = false;
    return aggregateResults;
  }

  @Override
  public AggregateResults call() {
    return calculate();
  }

  private boolean getIsReady() {
    return isDataSet && isCalcSet;
  }

  @Override
  public void setKeepOneAttackingLandUnit(final boolean bool) {
    keepOneAttackingLandUnit = bool;
  }

  @Override
  public void setAmphibious(final boolean bool) {
    amphibious = bool;
  }

  @Override
  public void setRetreatAfterRound(final int value) {
    retreatAfterRound = value;
  }

  @Override
  public void setRetreatAfterXUnitsLeft(final int value) {
    retreatAfterXUnitsLeft = value;
  }

  @Override
  public void setRetreatWhenOnlyAirLeft(final boolean value) {
    retreatWhenOnlyAirLeft = value;
  }

  @Override
  public void setAttackerOrderOfLosses(final String attackerOrderOfLosses) {
    this.attackerOrderOfLosses = attackerOrderOfLosses;
  }

  @Override
  public void setDefenderOrderOfLosses(final String defenderOrderOfLosses) {
    this.defenderOrderOfLosses = defenderOrderOfLosses;
  }

  @Override
  public void cancel() {
    cancelled = true;
  }

  @Override
  public void shutdown() {
    cancel();
  }

  static boolean isValidOrderOfLoss(final String orderOfLoss, final GameData data) {
    if (orderOfLoss == null || orderOfLoss.trim().length() == 0) {
      return true;
    }
    try {
      final String[] sections;
      if (orderOfLoss.contains(OOL_SEPARATOR)) {
        sections = orderOfLoss.trim().split(OOL_SEPARATOR_REGEX);
      } else {
        sections = new String[1];
        sections[0] = orderOfLoss.trim();
      }
      final UnitTypeList unitTypes;
      try {
        data.acquireReadLock();
        unitTypes = data.getUnitTypeList();
      } finally {
        data.releaseReadLock();
      }
      for (final String section : sections) {
        if (section.length() == 0) {
          continue;
        }
        final String[] amountThenType = section.split(OOL_AMOUNT_DESCRIPTOR_REGEX);
        if (amountThenType.length != 2) {
          return false;
        }
        if (!amountThenType[0].equals(OOL_ALL)) {
          final int amount = Integer.parseInt(amountThenType[0]);
          if (amount <= 0) {
            return false;
          }
        }
        final UnitType type = unitTypes.getUnitType(amountThenType[1]);
        if (type == null) {
          return false;
        }
      }
    } catch (final Exception e) {
      return false;
    }
    return true;
  }

  private static List<Unit> getUnitListByOrderOfLoss(final String ool, final Collection<Unit> units,
      final GameData data) {
    if (ool == null || ool.trim().length() == 0) {
      return null;
    }
    final List<Tuple<Integer, UnitType>> map = new ArrayList<>();
    final String[] sections;
    if (ool.contains(OOL_SEPARATOR)) {
      sections = ool.trim().split(OOL_SEPARATOR_REGEX);
    } else {
      sections = new String[1];
      sections[0] = ool.trim();
    }
    for (final String section : sections) {
      if (section.length() == 0) {
        continue;
      }
      final String[] amountThenType = section.split(OOL_AMOUNT_DESCRIPTOR_REGEX);
      final int amount = amountThenType[0].equals(OOL_ALL) ? Integer.MAX_VALUE : Integer.parseInt(amountThenType[0]);
      final UnitType type = data.getUnitTypeList().getUnitType(amountThenType[1]);
      map.add(Tuple.of(amount, type));
    }
    Collections.reverse(map);
    final Set<Unit> unitsLeft = new HashSet<>(units);
    final List<Unit> order = new ArrayList<>();
    for (final Tuple<Integer, UnitType> section : map) {
      final List<Unit> unitsOfType =
          CollectionUtils.getNMatches(unitsLeft, section.getFirst(), Matches.unitIsOfType(section.getSecond()));
      order.addAll(unitsOfType);
      unitsLeft.removeAll(unitsOfType);
    }
    Collections.reverse(order);
    return order;
  }

  private static class DummyDelegateBridge implements IDelegateBridge {
    private final PlainRandomSource randomSource = new PlainRandomSource();
    private final ITripleADisplay display = new HeadlessDisplay();
    private final ISound soundChannel = new HeadlessSoundChannel();
    private final DummyPlayer attackingPlayer;
    private final DummyPlayer defendingPlayer;
    private final PlayerID attacker;
    private final DelegateHistoryWriter writer = new DelegateHistoryWriter(new DummyGameModifiedChannel());
    private final CompositeChange allChanges;
    private final GameData gameData;
    private MustFightBattle battle = null;

    public DummyDelegateBridge(final PlayerID attacker, final GameData data, final CompositeChange allChanges,
        final List<Unit> attackerOrderOfLosses, final List<Unit> defenderOrderOfLosses,
        final boolean attackerKeepOneLandUnit, final int retreatAfterRound, final int retreatAfterXUnitsLeft,
        final boolean retreatWhenOnlyAirLeft) {
      attackingPlayer = new DummyPlayer(this, true, "battle calc dummy", "None (AI)", attackerOrderOfLosses,
          attackerKeepOneLandUnit, retreatAfterRound, retreatAfterXUnitsLeft, retreatWhenOnlyAirLeft);
      defendingPlayer = new DummyPlayer(this, false, "battle calc dummy", "None (AI)", defenderOrderOfLosses, false,
          retreatAfterRound, -1, false);
      gameData = data;
      this.attacker = attacker;
      this.allChanges = allChanges;
    }

    @Override
    public GameData getData() {
      return gameData;
    }

    @Override
    public void leaveDelegateExecution() {}

    @Override
    public Properties getStepProperties() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getStepName() {
      throw new UnsupportedOperationException();
    }

    @Override
    public IRemotePlayer getRemotePlayer(final PlayerID id) {
      return id.equals(attacker) ? attackingPlayer : defendingPlayer;
    }

    @Override
    public IRemotePlayer getRemotePlayer() {
      // the current player is attacker
      return attackingPlayer;
    }

    @Override
    public int[] getRandom(final int max, final int count, final PlayerID player, final DiceType diceType,
        final String annotation) {
      return randomSource.getRandom(max, count, annotation);
    }

    @Override
    public int getRandom(final int max, final PlayerID player, final DiceType diceType, final String annotation) {
      return randomSource.getRandom(max, annotation);
    }

    @Override
    public PlayerID getPlayerId() {
      return attacker;
    }

    @Override
    public IDelegateHistoryWriter getHistoryWriter() {
      return writer;
    }

    @Override
    public IDisplay getDisplayChannelBroadcaster() {
      return display;
    }

    @Override
    public ISound getSoundChannelBroadcaster() {
      return soundChannel;
    }

    @Override
    public void enterDelegateExecution() {}

    @Override
    public void addChange(final Change change) {
      if (!(change instanceof UnitHitsChange)) {
        return;
      }
      allChanges.add(change);
      gameData.performChange(change);
    }

    @Override
    public void stopGameSequence() {}

    public MustFightBattle getBattle() {
      return battle;
    }

    public void setBattle(final MustFightBattle battle) {
      this.battle = battle;
    }
  }

  private static class DummyGameModifiedChannel implements IGameModifiedChannel {
    @Override
    public void addChildToEvent(final String text, final Object renderingData) {}

    @Override
    public void gameDataChanged(final Change change) {}

    @Override
    public void shutDown() {}

    @Override
    public void startHistoryEvent(final String event) {}


    @Override
    public void startHistoryEvent(final String event, final Object renderingData) {}

    @Override
    public void stepChanged(final String stepName, final String delegateName, final PlayerID player, final int round,
        final String displayName, final boolean loadedFromSavedGame) {}
  }

  private static class DummyPlayer extends AbstractAi {
    private final boolean keepAtLeastOneLand;
    // negative = do not retreat
    private final int retreatAfterRound;
    // negative = do not retreat
    private final int retreatAfterXUnitsLeft;
    private final boolean retreatWhenOnlyAirLeft;
    private final DummyDelegateBridge bridge;
    private final boolean isAttacker;
    private final List<Unit> orderOfLosses;

    public DummyPlayer(final DummyDelegateBridge dummyDelegateBridge, final boolean attacker, final String name,
        final String type, final List<Unit> orderOfLosses, final boolean keepAtLeastOneLand,
        final int retreatAfterRound,
        final int retreatAfterXUnitsLeft, final boolean retreatWhenOnlyAirLeft) {
      super(name, type);
      this.keepAtLeastOneLand = keepAtLeastOneLand;
      this.retreatAfterRound = retreatAfterRound;
      this.retreatAfterXUnitsLeft = retreatAfterXUnitsLeft;
      this.retreatWhenOnlyAirLeft = retreatWhenOnlyAirLeft;
      bridge = dummyDelegateBridge;
      isAttacker = attacker;
      this.orderOfLosses = orderOfLosses;
    }

    private MustFightBattle getBattle() {
      return bridge.getBattle();
    }

    private List<Unit> getOurUnits() {
      final MustFightBattle battle = getBattle();
      if (battle == null) {
        return null;
      }
      return new ArrayList<>((isAttacker ? battle.getAttackingUnits() : battle.getDefendingUnits()));
    }

    private List<Unit> getEnemyUnits() {
      final MustFightBattle battle = getBattle();
      if (battle == null) {
        return null;
      }
      return new ArrayList<>((isAttacker ? battle.getDefendingUnits() : battle.getAttackingUnits()));
    }

    @Override
    protected void move(final boolean nonCombat, final IMoveDelegate moveDel, final GameData data,
        final PlayerID player) {}

    @Override
    protected void place(final boolean placeForBid, final IAbstractPlaceDelegate placeDelegate, final GameData data,
        final PlayerID player) {}

    @Override
    protected void purchase(final boolean purcahseForBid, final int pusToSpend,
        final IPurchaseDelegate purchaseDelegate,
        final GameData data, final PlayerID player) {}

    @Override
    protected void tech(final ITechDelegate techDelegate, final GameData data, final PlayerID player) {}

    @Override
    public boolean confirmMoveInFaceOfAa(final Collection<Territory> aaFiringTerritories) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Unit> getNumberOfFightersToMoveToNewCarrier(final Collection<Unit> fightersThatCanBeMoved,
        final Territory from) {
      throw new UnsupportedOperationException();
    }

    /**
     * The battle calc doesn't actually care if you have available territories to retreat to or not.
     * It will always let you retreat to the 'current' territory (the battle territory), even if that is illegal.
     * This is because the battle calc does not know where the attackers are actually coming from.
     */
    @Override
    public Territory retreatQuery(final GUID battleId, final boolean submerge, final Territory battleSite,
        final Collection<Territory> possibleTerritories, final String message) {
      // null = do not retreat
      if (possibleTerritories.isEmpty()) {
        return null;
      }
      if (submerge) {
        // submerge if all air vs subs
        final Predicate<Unit> seaSub = Matches.unitIsSea().and(Matches.unitIsSub());
        final Predicate<Unit> planeNotDestroyer = Matches.unitIsAir().and(Matches.unitIsDestroyer().negate());
        final List<Unit> ourUnits = getOurUnits();
        final List<Unit> enemyUnits = getEnemyUnits();
        if (ourUnits == null || enemyUnits == null) {
          return null;
        }
        if (!enemyUnits.isEmpty() && enemyUnits.stream().allMatch(planeNotDestroyer) && !ourUnits.isEmpty()
            && ourUnits.stream().allMatch(seaSub)) {
          return possibleTerritories.iterator().next();
        }
        return null;
      }

      final MustFightBattle battle = getBattle();
      if (battle == null) {
        return null;
      }
      if (retreatAfterRound > -1 && battle.getBattleRound() >= retreatAfterRound) {
        return possibleTerritories.iterator().next();
      }
      if (!retreatWhenOnlyAirLeft && retreatAfterXUnitsLeft <= -1) {
        return null;
      }
      final Collection<Unit> unitsLeft = isAttacker ? battle.getAttackingUnits() : battle.getDefendingUnits();
      final Collection<Unit> airLeft = CollectionUtils.getMatches(unitsLeft, Matches.unitIsAir());
      if (retreatWhenOnlyAirLeft) {
        // lets say we have a bunch of 3 attack air unit, and a 4 attack non-air unit,
        // and we want to retreat when we have all air units left + that 4 attack non-air (cus it gets taken
        // casualty
        // last)
        // then we add the number of air, to the retreat after X left number (which we would set to '1')
        int retreatNum = airLeft.size();
        if (retreatAfterXUnitsLeft > 0) {
          retreatNum += retreatAfterXUnitsLeft;
        }
        if (retreatNum >= unitsLeft.size()) {
          return possibleTerritories.iterator().next();
        }
      }
      if (retreatAfterXUnitsLeft > -1 && retreatAfterXUnitsLeft >= unitsLeft.size()) {
        return possibleTerritories.iterator().next();
      }
      return null;
    }

    // Added new collection autoKilled to handle killing units prior to casualty selection
    @Override
    public CasualtyDetails selectCasualties(final Collection<Unit> selectFrom,
        final Map<Unit, Collection<Unit>> dependents, final int count, final String message, final DiceRoll dice,
        final PlayerID hit, final Collection<Unit> friendlyUnits, final PlayerID enemyPlayer,
        final Collection<Unit> enemyUnits, final boolean amphibious, final Collection<Unit> amphibiousLandAttackers,
        final CasualtyList defaultCasualties, final GUID battleId, final Territory battlesite,
        final boolean allowMultipleHitsPerUnit) {
      final List<Unit> damagedUnits = new ArrayList<>(defaultCasualties.getDamaged());
      final List<Unit> killedUnits = new ArrayList<>(defaultCasualties.getKilled());
      if (keepAtLeastOneLand) {
        final List<Unit> notKilled = new ArrayList<>(selectFrom);
        notKilled.removeAll(killedUnits);
        // no land units left, but we have a non land unit to kill and land unit was killed
        if (!notKilled.stream().anyMatch(Matches.unitIsLand()) && notKilled.stream().anyMatch(Matches.unitIsNotLand())
            && killedUnits.stream().anyMatch(Matches.unitIsLand())) {
          final List<Unit> notKilledAndNotLand = CollectionUtils.getMatches(notKilled, Matches.unitIsNotLand());
          // sort according to cost
          Collections.sort(notKilledAndNotLand, AiUtils.getCostComparator());
          // remove the last killed unit, this should be the strongest
          killedUnits.remove(killedUnits.size() - 1);
          // add the cheapest unit
          killedUnits.add(notKilledAndNotLand.get(0));
        }
      }
      if (orderOfLosses != null && !orderOfLosses.isEmpty() && !killedUnits.isEmpty()) {
        final List<Unit> orderOfLosses = new ArrayList<>(this.orderOfLosses);
        orderOfLosses.retainAll(selectFrom);
        if (!orderOfLosses.isEmpty()) {
          int killedSize = killedUnits.size();
          killedUnits.clear();
          while (killedSize > 0 && !orderOfLosses.isEmpty()) {
            killedUnits.add(orderOfLosses.get(0));
            orderOfLosses.remove(0);
            killedSize--;
          }
          if (killedSize > 0) {
            final List<Unit> defaultKilled = new ArrayList<>(defaultCasualties.getKilled());
            defaultKilled.removeAll(killedUnits);
            while (killedSize > 0) {
              killedUnits.add(defaultKilled.get(0));
              defaultKilled.remove(0);
              killedSize--;
            }
          }
        }
      }
      final CasualtyDetails casualtyDetails = new CasualtyDetails(killedUnits, damagedUnits, false);
      return casualtyDetails;
    }

    @Override
    public Territory selectTerritoryForAirToLand(final Collection<Territory> candidates,
        final Territory currentTerritory,
        final String unitMessage) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean shouldBomberBomb(final Territory territory) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Unit whatShouldBomberBomb(final Territory territory, final Collection<Unit> potentialTargets,
        final Collection<Unit> bombers) {
      throw new UnsupportedOperationException();
    }
  }
}
