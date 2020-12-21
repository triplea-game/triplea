package games.strategy.triplea.ui;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameDataInjections;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.framework.IGameModifiedChannel;
import games.strategy.engine.framework.startup.ui.PlayerTypes;
import games.strategy.engine.history.DelegateHistoryWriter;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.engine.player.Player;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.triplea.ai.AbstractBuiltInAi;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import games.strategy.triplea.delegate.data.CasualtyList;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.delegate.remote.ITechDelegate;
import games.strategy.triplea.ui.display.HeadlessDisplay;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;
import org.triplea.sound.HeadlessSoundChannel;
import org.triplea.sound.ISound;
import org.triplea.util.Tuple;

/** Class used to avoid making actual data changes when checking objectives. */
public class ObjectiveDummyDelegateBridge implements IDelegateBridge {
  private final IDisplay display = new HeadlessDisplay();
  private final ISound soundChannel = new HeadlessSoundChannel();
  private final DelegateHistoryWriter writer =
      new DelegateHistoryWriter(new DummyGameModifiedChannel());
  private final GameData gameData;
  private final ObjectivePanelDummyPlayer dummyAi =
      new ObjectivePanelDummyPlayer("objective panel dummy");

  public ObjectiveDummyDelegateBridge(final GameData data) {
    gameData = data;
  }

  @Override
  public GameData getData() {
    return gameData;
  }

  @Override
  public void sendMessage(final WebSocketMessage webSocketMessage) {}

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
  public Player getRemotePlayer(final GamePlayer gamePlayer) {
    return dummyAi;
  }

  @Override
  public Player getRemotePlayer() {
    return dummyAi;
  }

  @Override
  public int[] getRandom(
      final int max,
      final int count,
      final GamePlayer player,
      final DiceType diceType,
      final String annotation) {
    if (count <= 0) {
      throw new IllegalStateException("count must be > o, annotation:" + annotation);
    }
    final int[] numbers = new int[count];
    for (int i = 0; i < count; i++) {
      numbers[i] = getRandom(max, player, diceType, annotation);
    }
    return numbers;
  }

  @Override
  public int getRandom(
      final int max, final GamePlayer player, final DiceType diceType, final String annotation) {
    return 0;
  }

  @Override
  public GamePlayer getGamePlayer() {
    return GamePlayer.NULL_PLAYERID;
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
  public void addChange(final Change change) {}

  @Override
  public void stopGameSequence() {}

  static class DummyGameModifiedChannel implements IGameModifiedChannel {
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
    public void stepChanged(
        final String stepName,
        final String delegateName,
        final GamePlayer player,
        final int round,
        final String displayName,
        final boolean loadedFromSavedGame) {}
  }

  static class ObjectivePanelDummyPlayer extends AbstractBuiltInAi {
    ObjectivePanelDummyPlayer(final String name) {
      super(name);
    }

    @Override
    protected void move(
        final boolean nonCombat,
        final IMoveDelegate moveDel,
        final GameData data,
        final GamePlayer player) {
      throw new UnsupportedOperationException();
    }

    @Override
    protected void place(
        final boolean placeForBid,
        final IAbstractPlaceDelegate placeDelegate,
        final GameDataInjections data,
        final GamePlayer player) {
      throw new UnsupportedOperationException();
    }

    @Override
    protected void purchase(
        final boolean purchaseForBid,
        final int pusToSpend,
        final IPurchaseDelegate purchaseDelegate,
        final GameData data,
        final GamePlayer player) {
      throw new UnsupportedOperationException();
    }

    @Override
    protected void tech(
        final ITechDelegate techDelegate, final GameData data, final GamePlayer player) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean confirmMoveInFaceOfAa(final Collection<Territory> aaFiringTerritories) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Unit> getNumberOfFightersToMoveToNewCarrier(
        final Collection<Unit> fightersThatCanBeMoved, final Territory from) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Territory retreatQuery(
        final UUID battleId,
        final boolean submerge,
        final Territory battleSite,
        final Collection<Territory> possibleTerritories,
        final String message) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Map<Territory, Collection<Unit>> scrambleUnitsQuery(
        final Territory scrambleTo,
        final Map<Territory, Tuple<Collection<Unit>, Collection<Unit>>> possibleScramblers) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Unit> selectUnitsQuery(
        final Territory current, final Collection<Unit> possible, final String message) {
      throw new UnsupportedOperationException();
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
      throw new UnsupportedOperationException();
    }

    @Override
    public Territory selectTerritoryForAirToLand(
        final Collection<Territory> candidates,
        final Territory currentTerritory,
        final String unitMessage) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean shouldBomberBomb(final Territory territory) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Unit whatShouldBomberBomb(
        final Territory territory,
        final Collection<Unit> potentialTargets,
        final Collection<Unit> bombers) {
      throw new UnsupportedOperationException();
    }

    @Override
    public PlayerTypes.Type getPlayerType() {
      return PlayerTypes.BATTLE_CALC_DUMMY;
    }
  }
}
