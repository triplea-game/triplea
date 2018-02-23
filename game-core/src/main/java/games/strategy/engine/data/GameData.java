package games.strategy.engine.data;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.swing.SwingUtilities;

import com.google.common.base.MoreObjects;

import games.strategy.engine.data.events.GameDataChangeListener;
import games.strategy.engine.data.events.GameMapListener;
import games.strategy.engine.data.events.TerritoryListener;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.framework.GameDataManager;
import games.strategy.engine.framework.IGameLoader;
import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.history.History;
import games.strategy.io.IoUtils;
import games.strategy.thread.LockUtil;
import games.strategy.triplea.ResourceLoader;
import games.strategy.util.Tuple;
import games.strategy.util.Version;

/**
 * Central place to find all the information for a running game.
 *
 * <p>
 * Using this object you can find the territories, connections, production rules,
 * unit types...
 * </p>
 *
 * <p>
 * Threading. The game data, and all parts of the game data (such as Territories, Players, Units...) are protected by a
 * read/write lock. If
 * you are reading the game data, you should read while you have the read lock as below.
 * </p>
 *
 * <p>
 * <code>
 * data.acquireReadLock();
 * try
 * {
 *   //read data here
 * }
 * finally
 * {
 *   data.releaseReadLock();
 * }
 * </code>
 * The exception is delegates within a start(), end() or any method called from an IGamePlayer through the delegates
 * remote interface. The
 * delegate will have a read lock for the duration of those methods.
 * </p>
 *
 * <p>
 * Non engine code must NOT acquire the games writeLock(). All changes to game Data must be made through a
 * DelegateBridge or through a
 * History object.
 * </p>
 */
public class GameData implements Serializable {
  private static final long serialVersionUID = -2612710634080125728L;
  public static final String GAME_UUID = "GAME_UUID";
  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  private transient LockUtil lockUtil = LockUtil.INSTANCE;
  private transient volatile boolean forceInSwingEventThread = false;
  private String gameName;
  private Version gameVersion;
  private int diceSides;
  private transient List<TerritoryListener> territoryListeners = new CopyOnWriteArrayList<>();
  private transient List<GameDataChangeListener> dataChangeListeners = new CopyOnWriteArrayList<>();
  private transient List<GameMapListener> gameMapListeners = new CopyOnWriteArrayList<>();
  private final AllianceTracker alliances = new AllianceTracker();
  // Tracks current relationships between players, this is empty if relationships aren't used
  private final RelationshipTracker relationships = new RelationshipTracker(this);
  private final DelegateList delegateList;
  private final GameMap map = new GameMap(this);
  private final PlayerList playerList = new PlayerList(this);
  private final ProductionFrontierList productionFrontierList = new ProductionFrontierList(this);
  private final ProductionRuleList productionRuleList = new ProductionRuleList(this);
  private final RepairFrontierList repairFrontierList = new RepairFrontierList(this);
  private final RepairRuleList repairRuleList = new RepairRuleList(this);
  private final ResourceList resourceList = new ResourceList(this);
  private final GameSequence sequence = new GameSequence(this);
  private final UnitTypeList unitTypeList = new UnitTypeList(this);
  // Tracks all relationshipTypes that are in the current game, default there will be the SelfRelation and the
  // NullRelation any other relations are map designer created.
  private final RelationshipTypeList relationshipTypeList = new RelationshipTypeList(this);
  private final GameProperties properties = new GameProperties(this);
  private final UnitsList unitsList = new UnitsList();
  private final TechnologyFrontier technologyFrontier = new TechnologyFrontier("allTechsForGame", this);
  private transient ResourceLoader resourceLoader;
  private IGameLoader loader;
  private final History gameHistory = new History(this);
  private transient volatile boolean testLockIsHeld = false;
  private final List<Tuple<IAttachment, ArrayList<Tuple<String, String>>>> attachmentOrderAndValues =
      new ArrayList<>();
  // TODO: change to Map/HashMap upon next incompatible release
  private final Hashtable<String, TerritoryEffect> territoryEffectList = new Hashtable<>();
  private final BattleRecordsList battleRecordsList = new BattleRecordsList(this);

  /** Creates new GameData. */
  public GameData() {
    super();
    delegateList = new DelegateList(this);
    properties.set(GAME_UUID, UUID.randomUUID().toString());
  }

  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    lockUtil = LockUtil.INSTANCE;
  }

  /**
   * Converts the current GameData object to a byte array, useful for serialization or for
   * copying the game data.
   */
  public byte[] toBytes() {
    try {
      return IoUtils.writeToMemory(os -> GameDataManager.saveGame(os, this));
    } catch (final IOException e) {
      throw new RuntimeException("Failed to write game data to bytes", e);
    }
  }

  /**
   * Return the GameMap. The game map allows you to list the territories in the game, and
   * to see which territory is connected to which.
   *
   * @return the map for this game.
   */
  public GameMap getMap() {
    return map;
  }

  /**
   * Print an exception report if we are testing the lock is held, and
   * do not currently hold the read or write lock.
   */
  private void ensureLockHeld() {
    if (!testLockIsHeld) {
      return;
    }
    if (readWriteLockMissing()) {
      return;
    }
    if (!lockUtil.isLockHeld(readWriteLock.readLock()) && !lockUtil.isLockHeld(readWriteLock.writeLock())) {
      new Exception("Lock not held").printStackTrace(System.out);
    }
  }

  /**
   * @return a collection of all units in the game.
   */
  public UnitsList getUnits() {
    // ensureLockHeld();
    return unitsList;
  }

  /**
   * @return list of Players in the game.
   */
  public PlayerList getPlayerList() {
    return playerList;
  }

  /**
   * @return list of resources available in the game.
   */
  public ResourceList getResourceList() {
    ensureLockHeld();
    return resourceList;
  }

  /**
   * @return list of production Frontiers for this game.
   */
  public ProductionFrontierList getProductionFrontierList() {
    ensureLockHeld();
    return productionFrontierList;
  }

  /**
   * @return list of Production Rules for the game.
   */
  public ProductionRuleList getProductionRuleList() {
    ensureLockHeld();
    return productionRuleList;
  }

  /**
   * @return The Technology Frontier for this game.
   */
  public TechnologyFrontier getTechnologyFrontier() {
    return technologyFrontier;
  }

  /**
   * @return The list of production Frontiers for this game.
   */
  public RepairFrontierList getRepairFrontierList() {
    ensureLockHeld();
    return repairFrontierList;
  }

  /**
   * @return The list of Production Rules for the game.
   */
  public RepairRuleList getRepairRuleList() {
    ensureLockHeld();
    return repairRuleList;
  }

  /**
   * @return The Alliance Tracker for the game.
   */
  public AllianceTracker getAllianceTracker() {
    ensureLockHeld();
    return alliances;
  }

  /**
   * @return whether we should throw an error if changes to this game data are made outside of the swing
   *         event thread.
   */
  public boolean areChangesOnlyInSwingEventThread() {
    return forceInSwingEventThread;
  }

  /**
   * If set to true, then we will throw an error when the game data is changed outside
   * the swing event thread.
   */
  public void forceChangesOnlyInSwingEventThread() {
    forceInSwingEventThread = true;
  }

  public GameSequence getSequence() {
    ensureLockHeld();
    return sequence;
  }

  public UnitTypeList getUnitTypeList() {
    ensureLockHeld();
    return unitTypeList;
  }

  public DelegateList getDelegateList() {
    ensureLockHeld();
    return delegateList;
  }

  public UnitHolder getUnitHolder(final String name, final String type) {
    ensureLockHeld();
    if (type.equals(UnitHolder.PLAYER)) {
      return playerList.getPlayerId(name);
    } else if (type.equals(UnitHolder.TERRITORY)) {
      return map.getTerritory(name);
    } else {
      throw new IllegalStateException("Invalid type:" + type);
    }
  }

  public GameProperties getProperties() {
    return properties;
  }

  public void addTerritoryListener(final TerritoryListener listener) {
    territoryListeners.add(listener);
  }

  public void removeTerritoryListener(final TerritoryListener listener) {
    territoryListeners.remove(listener);
  }

  public void addDataChangeListener(final GameDataChangeListener listener) {
    dataChangeListeners.add(listener);
  }

  public void removeDataChangeListener(final GameDataChangeListener listener) {
    dataChangeListeners.remove(listener);
  }

  public void addGameMapListener(final GameMapListener listener) {
    gameMapListeners.add(listener);
  }

  public void removeGameMapListener(final GameMapListener listener) {
    gameMapListeners.remove(listener);
  }

  void notifyTerritoryUnitsChanged(final Territory t) {
    territoryListeners.forEach(territoryListener -> territoryListener.unitsChanged(t));
  }

  void notifyTerritoryAttachmentChanged(final Territory t) {
    territoryListeners.forEach(territoryListener -> territoryListener.attachmentChanged(t));
  }

  void notifyTerritoryOwnerChanged(final Territory t) {
    territoryListeners.forEach(territoryListener -> territoryListener.ownerChanged(t));
  }

  void notifyGameDataChanged(final Change change) {
    dataChangeListeners.forEach(dataChangelistener -> dataChangelistener.gameDataChanged(change));
  }

  void notifyMapDataChanged() {
    gameMapListeners.forEach(GameMapListener::gameMapDataChanged);
  }

  public IGameLoader getGameLoader() {
    return loader;
  }

  void setGameLoader(final IGameLoader loader) {
    this.loader = loader;
  }

  void setGameVersion(final Version gameVersion) {
    this.gameVersion = gameVersion;
  }

  public Version getGameVersion() {
    return gameVersion;
  }

  void setGameName(final String gameName) {
    this.gameName = gameName;
  }

  public String getGameName() {
    return gameName;
  }

  void setDiceSides(final int diceSides) {
    if ((diceSides > 0) && (diceSides <= 200)) {
      this.diceSides = diceSides;
    } else {
      this.diceSides = 6;
    }
  }

  public int getDiceSides() {
    return diceSides;
  }

  public History getHistory() {
    // don't ensure the lock is held when getting the history
    // history operations often acquire the write lock
    // and we cant acquire the write lock if we have the read lock
    return gameHistory;
  }

  /**
   * Not to be called by mere mortals.
   */
  public void postDeSerialize() {
    territoryListeners = new ArrayList<>();
    dataChangeListeners = new ArrayList<>();
    gameMapListeners = new ArrayList<>();
  }

  /**
   * No changes to the game data should be made unless this lock is held.
   * calls to acquire lock will block if the lock is held, and will be held
   * until the release method is called
   */
  public void acquireReadLock() {
    if (readWriteLockMissing()) {
      return;
    }
    lockUtil.acquireLock(readWriteLock.readLock());
  }

  public void releaseReadLock() {
    if (readWriteLockMissing()) {
      return;
    }
    lockUtil.releaseLock(readWriteLock.readLock());
  }

  /**
   * No changes to the game data should be made unless this lock is held.
   * calls to acquire lock will block if the lock is held, and will be held
   * until the release method is called
   */
  public void acquireWriteLock() {
    if (readWriteLockMissing()) {
      return;
    }
    lockUtil.acquireLock(readWriteLock.writeLock());
  }

  public void releaseWriteLock() {
    if (readWriteLockMissing()) {
      return;
    }
    lockUtil.releaseLock(readWriteLock.writeLock());
  }

  /**
   * @return boolean, whether readWriteLock is missing
   *         This can happen in very odd circumstances while deserializing.
   */
  private boolean readWriteLockMissing() {
    return readWriteLock == null;
  }

  public void clearAllListeners() {
    dataChangeListeners.clear();
    territoryListeners.clear();
    gameMapListeners.clear();
    if (resourceLoader != null) {
      resourceLoader.close();
      resourceLoader = null;
    }
  }

  /**
   * On reads of the game data components, make sure that the
   * read or write lock is held.
   */
  public void testLocksOnRead() {
    testLockIsHeld = true;
  }

  public void addToAttachmentOrderAndValues(
      final Tuple<IAttachment, ArrayList<Tuple<String, String>>> attachmentAndValues) {
    attachmentOrderAndValues.add(attachmentAndValues);
  }

  public List<Tuple<IAttachment, ArrayList<Tuple<String, String>>>> getAttachmentOrderAndValues() {
    return attachmentOrderAndValues;
  }

  /**
   * @return all relationshipTypes that are valid in this game, default there is the NullRelation (relation with the
   *         Nullplayer / Neutral)
   *         and the SelfRelation (Relation with yourself) all other relations are mapdesigner defined.
   */
  public RelationshipTypeList getRelationshipTypeList() {
    ensureLockHeld();
    return relationshipTypeList;
  }

  /**
   * @return a tracker which tracks all current relationships that exist between all players.
   */
  public RelationshipTracker getRelationshipTracker() {
    ensureLockHeld();
    return relationships;
  }

  public Map<String, TerritoryEffect> getTerritoryEffectList() {
    return territoryEffectList;
  }

  public BattleRecordsList getBattleRecordsList() {
    return battleRecordsList;
  }

  /**
   * Call this before starting the game and before the game data has been sent to the clients in order to make any
   * final modifications to the game data.
   * For example, this method will remove player delegates for players who have been disabled.
   */
  public void doPreGameStartDataModifications(final PlayerListing playerListing) {
    final Set<PlayerID> playersWhoShouldBeRemoved = new HashSet<>();
    final Map<String, Boolean> playersEnabledListing = playerListing.getPlayersEnabledListing();
    playerList.getPlayers().stream()
        .filter(p -> (p.getCanBeDisabled() && !playersEnabledListing.get(p.getName())))
        .forEach(p -> {
          p.setIsDisabled(true);
          playersWhoShouldBeRemoved.add(p);
        });
    if (!playersWhoShouldBeRemoved.isEmpty()) {
      removePlayerStepsFromSequence(playersWhoShouldBeRemoved);
    }
  }

  private void removePlayerStepsFromSequence(final Set<PlayerID> playersWhoShouldBeRemoved) {
    final int currentIndex = sequence.getStepIndex();
    int index = 0;
    int toSubtract = 0;
    final Iterator<GameStep> stepIter = sequence.iterator();
    while (stepIter.hasNext()) {
      final GameStep step = stepIter.next();
      if (playersWhoShouldBeRemoved.contains(step.getPlayerId())) {
        stepIter.remove();
        if (index < currentIndex) {
          toSubtract++;
        }
      }
      index++;
    }
    sequence.setStepIndex(Math.max(0, Math.min(sequence.size() - 1, currentIndex - toSubtract)));
  }

  public void performChange(final Change change) {
    if (areChangesOnlyInSwingEventThread() && !SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("Wrong thread");
    }
    try {
      acquireWriteLock();
      change.perform(this);
    } finally {
      releaseWriteLock();
    }
    notifyGameDataChanged(change);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("diceSides", diceSides)
        .add("gameName", gameName)
        .add("gameVersion", gameVersion)
        .add("loader", loader)
        .add("playerList", playerList)
        .toString();
  }

  /**
   * Returns the current game round (with locking).
   * TODO: the locking here is probably not necessary! If the current round is updated immediately
   * after we return from this method, then the lock will have been to no effect anyways!
   */
  public int getCurrentRound() {
    try {
      acquireReadLock();
      return getSequence().getRound();
    } finally {
      releaseReadLock();
    }
  }
}
