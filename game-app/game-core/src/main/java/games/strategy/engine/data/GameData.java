package games.strategy.engine.data;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import games.strategy.engine.data.events.GameDataChangeListener;
import games.strategy.engine.data.events.TerritoryListener;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.framework.GameDataManager;
import games.strategy.engine.framework.IGameLoader;
import games.strategy.engine.history.History;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleA;
import games.strategy.triplea.delegate.AbstractMoveDelegate;
import games.strategy.triplea.delegate.PoliticsDelegate;
import games.strategy.triplea.delegate.PurchaseDelegate;
import games.strategy.triplea.delegate.TechTracker;
import games.strategy.triplea.delegate.TechnologyDelegate;
import games.strategy.triplea.delegate.battle.BattleDelegate;
import games.strategy.ui.Util;
import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import lombok.Getter;
import org.triplea.io.FileUtils;
import org.triplea.io.IoUtils;
import org.triplea.java.ObjectUtils;
import org.triplea.java.RemoveOnNextMajorRelease;
import org.triplea.map.description.file.MapDescriptionYaml;
import org.triplea.map.game.notes.GameNotes;
import org.triplea.util.Tuple;
import org.triplea.util.Version;

/**
 * Central place to find all the information for a running game.
 *
 * <p>Using this object you can find the territories, connections, production rules, unit types...
 *
 * <p>Threading. The game data, and all parts of the game data (such as Territories, Players,
 * Units...) are protected by a read/write lock. If you are reading the game data, you should read
 * while you have the read lock as below.
 *
 * <pre>{@code
 * try (GameData.Unlocker ignored = gameData.acquireReadLock()) {
 *   gameData.getStuff();
 * }
 * }</pre>
 *
 * <p>The exception is delegates within a start(), end() or any method called from an IRemotePlayer
 * through the delegates remote interface. The delegate will have a read lock for the duration of
 * those methods.
 *
 * <p>Non engine code must NOT acquire the games writeLock(). All changes to game Data must be made
 * through a DelegateBridge or through a History object.
 */
public class GameData implements Serializable, GameState {
  private static final long serialVersionUID = -2612710634080125728L;

  /** When we load a game from a save file, this property will be the name of that file. */
  private static final String SAVE_GAME_FILE_NAME_PROPERTY = "save.game.file.name";

  private transient ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  private transient volatile boolean forceInSwingEventThread = false;
  private String gameName;
  @RemoveOnNextMajorRelease @Deprecated private Version gameVersion;
  @Getter private int diceSides;
  private transient List<TerritoryListener> territoryListeners = new CopyOnWriteArrayList<>();

  private transient List<GameDataChangeListener> dataChangeListeners = new CopyOnWriteArrayList<>();
  private transient Map<String, IDelegate> delegates = new HashMap<>();
  private final AllianceTracker alliances = new AllianceTracker();
  // Tracks current relationships between players, this is empty if relationships aren't used
  private final RelationshipTracker relationships = new RelationshipTracker(this);
  private final GameMap map = new GameMap(this);
  private final PlayerList playerList = new PlayerList(this);
  private final ProductionFrontierList productionFrontierList = new ProductionFrontierList(this);
  private final ProductionRuleList productionRuleList = new ProductionRuleList(this);
  private final RepairFrontierList repairFrontierList = new RepairFrontierList(this);
  private final RepairRules repairRules = new RepairRules(this);
  private final ResourceList resourceList = new ResourceList(this);
  private final GameSequence sequence = new GameSequence(this);
  private final UnitTypeList unitTypeList = new UnitTypeList(this);
  // Tracks all relationshipTypes that are in the current game, default there will be the
  // SelfRelation and the
  // NullRelation any other relations are map designer created.
  private final RelationshipTypeList relationshipTypeList = new RelationshipTypeList(this);
  private final GameProperties properties = new GameProperties(this);
  private final UnitsList unitsList = new UnitsList();
  private final TechnologyFrontier technologyFrontier =
      new TechnologyFrontier("allTechsForGame", this);
  @Getter private transient TechTracker techTracker = new TechTracker(this);
  private final IGameLoader loader = new TripleA();
  private History gameHistory = new History(this);

  @Getter
  private List<Tuple<IAttachment, List<Tuple<String, String>>>> attachmentOrderAndValues =
      new ArrayList<>();

  private final Map<String, TerritoryEffect> territoryEffectList = new HashMap<>();
  private final BattleRecordsList battleRecordsList = new BattleRecordsList(this);
  private transient GameDataEventListeners gameDataEventListeners = new GameDataEventListeners();

  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    // The process of deserializing makes use of this lock,
    // we'll get an NPE if we don't set this field here already.
    readWriteLock = new ReentrantReadWriteLock();
    in.defaultReadObject();
    gameDataEventListeners = new GameDataEventListeners();
    techTracker = new TechTracker(this);
  }

  /**
   * Converts the current GameData object to a byte array, useful for serialization or for copying
   * the game data.
   */
  public byte[] toBytes() {
    try {
      return IoUtils.writeToMemory(os -> GameDataManager.saveGame(os, this));
    } catch (final IOException e) {
      throw new RuntimeException("Failed to write game data to bytes", e);
    }
  }

  /**
   * Return the GameMap. The game map allows you to list the territories in the game, and to see
   * which territory is connected to which.
   *
   * @return the map for this game.
   */
  @Override
  public GameMap getMap() {
    return map;
  }

  /** Returns a collection of all units in the game. */
  @Override
  public UnitsList getUnits() {
    return unitsList;
  }

  /** Returns list of Players in the game. */
  @Override
  public PlayerList getPlayerList() {
    return playerList;
  }

  /** Returns list of resources available in the game. */
  @Override
  public ResourceList getResourceList() {
    return resourceList;
  }

  /** Returns list of production Frontiers for this game. */
  @Override
  public ProductionFrontierList getProductionFrontierList() {
    return productionFrontierList;
  }

  /** Returns list of Production Rules for the game. */
  @Override
  public ProductionRuleList getProductionRuleList() {
    return productionRuleList;
  }

  /** Returns the Technology Frontier for this game. */
  @Override
  public TechnologyFrontier getTechnologyFrontier() {
    return technologyFrontier;
  }

  /** Returns the list of production Frontiers for this game. */
  @Override
  public RepairFrontierList getRepairFrontierList() {
    return repairFrontierList;
  }

  /** Returns the list of Production Rules for the game. */
  @Override
  public RepairRules getRepairRules() {
    return repairRules;
  }

  /** Returns the Alliance Tracker for the game. */
  @Override
  public AllianceTracker getAllianceTracker() {
    return alliances;
  }

  /**
   * Returns whether we should throw an error if changes to this game data are made outside of the
   * swing event thread.
   */
  public boolean areChangesOnlyInSwingEventThread() {
    return forceInSwingEventThread;
  }

  /**
   * If set to true, then we will throw an error when the game data is changed outside the swing
   * event thread.
   */
  public void forceChangesOnlyInSwingEventThread() {
    forceInSwingEventThread = true;
  }

  @Override
  public GameSequence getSequence() {
    return sequence;
  }

  /**
   * UnitTypeList is a collection of all of the unit types in the game
   *
   * <p>It is a read-only data structure and so doesn't require a lock to read it. It is only
   * modified during the initial parsing of the game data.
   */
  @Override
  public UnitTypeList getUnitTypeList() {
    return unitTypeList;
  }

  public Collection<IDelegate> getDelegates() {
    return delegates.values();
  }

  public void addDelegate(final IDelegate delegate) {
    delegates.put(delegate.getName(), delegate);
  }

  public IDelegate getDelegate(final String name) {
    return delegates.get(name);
  }

  @Override
  public UnitHolder getUnitHolder(final String name, final String type) {
    switch (type) {
      case UnitHolder.PLAYER:
        return playerList.getPlayerId(name);
      case UnitHolder.TERRITORY:
        return map.getTerritory(name);
      default:
        throw new IllegalStateException("Invalid type:" + type);
    }
  }

  @Override
  public GameProperties getProperties() {
    return properties;
  }

  public void addTerritoryListener(final TerritoryListener listener) {
    territoryListeners.add(listener);
  }

  public void removeTerritoryListener(final TerritoryListener listener) {
    territoryListeners.remove(listener);
  }

  /**
   * Adds a data change listener that will be invoked whenever GameData is updated.
   *
   * @deprecated Use {@link #addGameDataEventListener(GameDataEvent, Runnable)} instead.
   */
  @Deprecated
  public void addDataChangeListener(final GameDataChangeListener listener) {
    dataChangeListeners.add(listener);
  }

  public void removeDataChangeListener(final GameDataChangeListener listener) {
    dataChangeListeners.remove(listener);
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

  public void fireGameDataEvent(final GameDataEvent event) {
    gameDataEventListeners.accept(event);
  }

  /**
   * Registers a game data event listener that is invoked when a given event occurs. There is no
   * ordering guarantee of when listeners will be called beyond that they will be called when the
   * given event occurs.
   *
   * @param event The event to listen for.
   * @param listener Action that will be executed when the event occurs.
   */
  public void addGameDataEventListener(final GameDataEvent event, final Runnable listener) {
    gameDataEventListeners.addListener(event, listener);
  }

  public IGameLoader getGameLoader() {
    return loader;
  }

  @VisibleForTesting
  public void setGameName(final String gameName) {
    this.gameName = gameName;
  }

  @Override
  public String getGameName() {
    return gameName;
  }

  @Override
  public String getMapName() {
    return String.valueOf(properties.get(Constants.MAP_NAME));
  }

  public void setMapName(final String mapName) {
    properties.set(Constants.MAP_NAME, mapName);
  }

  public void setDiceSides(final int diceSides) {
    if (diceSides > 0 && diceSides <= 200) {
      this.diceSides = diceSides;
    } else {
      this.diceSides = 6;
    }
  }

  public History getHistory() {
    // don't ensure the lock is held when getting the history
    // history operations often acquire the write lock and we can't acquire the write lock if we
    // have the read lock
    return gameHistory;
  }

  public void setHistory(final History history) {
    gameHistory = history;
  }

  public void resetHistory() {
    gameHistory = new History(this);
    GameStep step = getSequence().getStep();
    // Put the history in a round and step, so that child nodes can be added without errors.
    final boolean oldForceInSwingEventThread = forceInSwingEventThread;
    forceInSwingEventThread = false;
    gameHistory
        .getHistoryWriter()
        .startNextStep(
            step.getName(), step.getDelegateName(), step.getPlayerId(), step.getDisplayName());
    forceInSwingEventThread = oldForceInSwingEventThread;
  }

  /** Not to be called by mere mortals. */
  public void postDeSerialize() {
    territoryListeners = new CopyOnWriteArrayList<>();
    dataChangeListeners = new CopyOnWriteArrayList<>();
    delegates = new HashMap<>();
    fixUpNullPlayers();
  }

  @RemoveOnNextMajorRelease
  private void fixUpNullPlayers() {
    // Old save games may have territories and units owned by a different null player object that
    // has a null data. Update them to the new null player, so that code can rely on getData()
    // not being null.
    GamePlayer nullPlayer = playerList.getNullPlayer();
    for (Territory t : getMap().getTerritories()) {
      // Note: We use referenceEquals() because .equals() or .isOwnedBy() is not sufficient.
      if (t.getOwner().isNull() && !ObjectUtils.referenceEquals(t.getOwner(), nullPlayer)) {
        t.setOwner(nullPlayer);
      }
    }
    for (Unit u : getUnits()) {
      // Note: We use referenceEquals() because .equals() or .isOwnedBy() is not sufficient.
      if (u.getOwner().isNull() && !ObjectUtils.referenceEquals(u.getOwner(), nullPlayer)) {
        u.setOwner(nullPlayer);
      }
    }
  }

  @RemoveOnNextMajorRelease
  public void fixUpNullPlayersInDelegates() {
    BattleDelegate battleDelegate = (BattleDelegate) getDelegate("battle");
    if (battleDelegate != null) {
      battleDelegate.getBattleTracker().fixUpNullPlayers(playerList.getNullPlayer());
    }
  }

  public interface Unlocker extends Closeable {
    @Override
    void close();
  }

  /**
   * No changes to the game data should be made unless this lock is held. calls to acquire lock will
   * block if the lock is held, and will be held until the release method is called.
   *
   * <p>Example use:
   *
   * <pre>{@code
   * try (GameData.Unlocker ignored = gameData.acquireReadLock()) {
   *   gameData.getStuff();
   * }
   * }</pre>
   */
  public Unlocker acquireReadLock() {
    return acquireLock(readWriteLock.readLock());
  }

  /**
   * No changes to the game data should be made unless this lock is held. calls to acquire lock will
   * block if the lock is held, and will be held until the release method is called.
   *
   * <p>Example use:
   *
   * <pre>{@code
   * try (GameData.Unlocker ignored = gameData.acquireWriteLock()) {
   *   gameData.doStuff();
   * }
   * }</pre>
   */
  public Unlocker acquireWriteLock() {
    return acquireLock(readWriteLock.writeLock());
  }

  private static Unlocker acquireLock(Lock lock) {
    lock.lock();
    return lock::unlock;
  }

  public void addToAttachmentOrderAndValues(
      final Tuple<IAttachment, List<Tuple<String, String>>> attachmentAndValues) {
    attachmentOrderAndValues.add(attachmentAndValues);
  }

  public void setAttachmentOrderAndValues(
      List<Tuple<IAttachment, List<Tuple<String, String>>>> values) {
    attachmentOrderAndValues = values;
  }

  /**
   * Returns all relationshipTypes that are valid in this game, default there is the NullRelation
   * (relation with the Null player / Neutral) and the SelfRelation (Relation with yourself) all
   * other relations are map designer defined.
   */
  @Override
  public RelationshipTypeList getRelationshipTypeList() {
    return relationshipTypeList;
  }

  /** Returns a tracker which tracks all current relationships that exist between all players. */
  @Override
  public RelationshipTracker getRelationshipTracker() {
    return relationships;
  }

  @Override
  public Map<String, TerritoryEffect> getTerritoryEffectList() {
    return territoryEffectList;
  }

  @Override
  public BattleRecordsList getBattleRecordsList() {
    return battleRecordsList;
  }

  @Override
  public PoliticsDelegate getPoliticsDelegate() {
    return (PoliticsDelegate) findDelegate("politics");
  }

  @Override
  public PurchaseDelegate getPurchaseDelegate() {
    return (PurchaseDelegate) findDelegate("purchase");
  }

  private IDelegate findDelegate(final String delegateName) {
    final IDelegate delegate = this.getDelegate(delegateName);
    if (delegate == null) {
      throw new IllegalStateException(delegateName + " delegate not found");
    }
    return delegate;
  }

  @Override
  public BattleDelegate getBattleDelegate() {
    return (BattleDelegate) findDelegate("battle");
  }

  @Override
  public AbstractMoveDelegate getMoveDelegate() {
    return (AbstractMoveDelegate) findDelegate("move");
  }

  @Override
  public TechnologyDelegate getTechDelegate() {
    return (TechnologyDelegate) findDelegate("tech");
  }

  /**
   * Call this before starting the game and before the game data has been sent to the clients in
   * order to remove player delegates for players who have been disabled.
   */
  public void preGameDisablePlayers(final Predicate<GamePlayer> shouldDisablePlayer) {
    final Set<GamePlayer> playersWhoShouldBeRemoved = new HashSet<>();
    playerList.getPlayers().stream()
        .filter(p -> (p.getCanBeDisabled() && shouldDisablePlayer.test(p)))
        .forEach(
            p -> {
              p.setIsDisabled(true);
              playersWhoShouldBeRemoved.add(p);
            });
    if (!playersWhoShouldBeRemoved.isEmpty()) {
      removePlayerStepsFromSequence(playersWhoShouldBeRemoved);
    }
  }

  private void removePlayerStepsFromSequence(final Set<GamePlayer> playersWhoShouldBeRemoved) {
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

  /** Executes a change and notifies listeners. */
  public void performChange(final Change change) {
    if (areChangesOnlyInSwingEventThread()) {
      Util.ensureOnEventDispatchThread();
    }
    try (Unlocker ignored = acquireWriteLock()) {
      change.perform(this);
    }
    dataChangeListeners.forEach(listener -> listener.gameDataChanged(change));
    GameDataEvent.lookupEvent(change).ifPresent(this::fireGameDataEvent);
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
   * Returns the current game round (with locking). TODO: the locking here is probably not
   * necessary! If the current round is updated immediately after we return from this method, then
   * the lock will have been to no effect anyways!
   */
  public int getCurrentRound() {
    try (GameData.Unlocker ignored = acquireReadLock()) {
      return getSequence().getRound();
    }
  }

  public Optional<String> getSaveGameFileName() {
    return Optional.ofNullable(getProperties().get(SAVE_GAME_FILE_NAME_PROPERTY, null));
  }

  public void setSaveGameFileName(final String saveGameFileName) {
    getProperties().set(SAVE_GAME_FILE_NAME_PROPERTY, saveGameFileName);
  }

  /**
   * Reads the game notes from the game-notes file and returns that text with an auto-generated
   * header. Returns empty if the 'map.yml' or game notes file cannot be found.
   */
  public String loadGameNotes(final Path mapLocation) {
    // From the game-xml file name, we can find the game-notes file.
    return getGameXmlPath(mapLocation).map(GameNotes::loadGameNotes).orElse("");
  }

  public Optional<Path> getGameXmlPath(final Path mapLocation) {
    // Given a game name, the map.yml file can tell us the path to the game xml file.
    return findMapDescriptionYaml(mapLocation)
        .flatMap(yaml -> yaml.getGameXmlPathByGameName(getGameName()));
  }

  private Optional<MapDescriptionYaml> findMapDescriptionYaml(final Path mapLocation) {
    return FileUtils.findFileInParentFolders(mapLocation, MapDescriptionYaml.MAP_YAML_FILE_NAME)
        .flatMap(MapDescriptionYaml::fromFile);
  }
}
