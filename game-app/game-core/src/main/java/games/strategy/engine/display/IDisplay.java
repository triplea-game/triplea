package games.strategy.engine.display;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.PlayerList;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitsList;
import games.strategy.engine.message.IChannelSubscriber;
import games.strategy.engine.message.RemoteActionCode;
import games.strategy.engine.message.wire.EntityRef;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Die;
import games.strategy.triplea.delegate.battle.IBattle.BattleType;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;
import org.triplea.java.RemoveOnNextMajorRelease;

/**
 * A Display is a view of the game. Displays listen on the display channel for game events. A
 * Display may interact with many {@link games.strategy.engine.player.Player}s.
 */
public interface IDisplay extends IChannelSubscriber {
  /**
   * Sends a message to all TripleAFrame that have joined the game, possibly including observers.
   */
  @RemoteActionCode(10)
  void reportMessageToAll(
      String message,
      String title,
      boolean doNotIncludeHost,
      boolean doNotIncludeClients,
      boolean doNotIncludeObservers);

  @Builder
  @AllArgsConstructor
  class BroadcastMessageMessage implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 4749612689122510834L;

    public static final MessageType<BroadcastMessageMessage> TYPE =
        MessageType.of(BroadcastMessageMessage.class);

    @Nonnull private final String message;
    @Nonnull private final String title;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }

    public void invokeCallback(IDisplay iDisplay) {
      iDisplay.reportMessageToAll(message, title, true, false, true);
    }
  }

  /**
   * Sends a message to all TripleAFrame's that are playing AND are controlling one or more of the
   * players listed but NOT any of the players listed as butNotThesePlayers. (No message to any
   * observers or players not in the list.)
   */
  @RemoteActionCode(11)
  void reportMessageToPlayers(
      Collection<GamePlayer> playersToSendTo,
      Collection<GamePlayer> butNotThesePlayers,
      String message,
      String title);

  /**
   * Display info about the battle. This is the first message to be displayed in a battle.
   *
   * @param battleId - a unique id for the battle
   * @param location - where the battle occurs
   * @param battleTitle - the title of the battle
   * @param attackingUnits - attacking units
   * @param defendingUnits - defending units
   * @param killedUnits - killed units
   * @param dependentUnits - unit dependencies, maps Unit->Collection of units
   * @param attacker - PlayerId of attacker
   * @param defender - PlayerId of defender
   */
  @RemoveOnNextMajorRelease(
      "Remove isAmphibious, amphibiousLandAttackers, dependentUnits, and battleTitle")
  @RemoteActionCode(12)
  void showBattle(
      UUID battleId,
      Territory location,
      String battleTitle,
      Collection<Unit> attackingUnits,
      Collection<Unit> defendingUnits,
      Collection<Unit> killedUnits,
      Collection<Unit> attackingWaitingToDie,
      Collection<Unit> defendingWaitingToDie,
      Map<Unit, Collection<Unit>> dependentUnits,
      GamePlayer attacker,
      GamePlayer defender,
      boolean isAmphibious,
      BattleType battleType,
      Collection<Unit> amphibiousLandAttackers);

  /**
   * Displays the steps for the specified battle.
   *
   * @param battleId - the battle we are listing steps for.
   * @param steps - a collection of strings denoting all steps in the battle
   */
  @RemoteActionCode(6)
  void listBattleSteps(UUID battleId, List<String> steps);

  /** The given battle has ended. */
  @RemoteActionCode(0)
  void battleEnd(UUID battleId, String message);

  /** Notify that the casualties occurred. */
  @RemoteActionCode(2)
  void casualtyNotification(
      UUID battleId,
      String step,
      DiceRoll dice,
      GamePlayer player,
      Collection<Unit> killed,
      Collection<Unit> damaged,
      Map<Unit, Collection<Unit>> dependents);

  /** Notify that the casualties occurred, and only the casualty. */
  @RemoteActionCode(4)
  void deadUnitNotification(
      UUID battleId,
      GamePlayer player,
      Collection<Unit> dead,
      Map<Unit, Collection<Unit>> dependents);

  @RemoteActionCode(3)
  void changedUnitsNotification(
      UUID battleId,
      GamePlayer player,
      Collection<Unit> removedUnits,
      Collection<Unit> addedUnits,
      Map<Unit, Collection<Unit>> dependents);

  /** Notification of the results of a bombing raid. */
  @RemoteActionCode(1)
  void bombingResults(UUID battleId, List<Die> dice, int cost);

  class BombingResultsMessage implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 6321445317606043212L;

    public static final MessageType<IDisplay.BombingResultsMessage> TYPE =
        MessageType.of(IDisplay.BombingResultsMessage.class);

    private final String battleId;
    private final List<DieRollData> diceData;
    private final Integer cost;

    public BombingResultsMessage(final UUID battleId, final List<Die> dice, final int cost) {
      this.battleId = battleId.toString();
      this.diceData = dice.stream().map(DieRollData::new).collect(Collectors.toList());
      this.cost = cost;
    }

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }

    public void accept(final IDisplay display) {
      display.bombingResults(UUID.fromString(battleId), DieRollData.toDieList(diceData), cost);
    }
  }

  /** Notify that the given player has retreated some or all of his units. */
  @RemoteActionCode(9)
  void notifyRetreat(String shortMessage, String message, String step, GamePlayer retreatingPlayer);

  @Builder
  class NotifyRetreatMessage implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5238964272146913900L;

    public static final MessageType<NotifyRetreatMessage> TYPE =
        MessageType.of(NotifyRetreatMessage.class);

    @Nonnull private final String shortMessage;
    @Nonnull private final String message;
    @Nonnull private final String step;
    @Nonnull private final String retreatingPlayerName;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }

    public void accept(final IDisplay display, final PlayerList playerlist) {
      display.notifyRetreat(
          shortMessage, message, step, playerlist.getPlayerId(retreatingPlayerName));
    }
  }

  @RemoteActionCode(8)
  void notifyRetreat(UUID battleId, Collection<Unit> retreating);

  class NotifyUnitsRetreatingMessage implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 8891803210930242938L;

    public static final MessageType<NotifyUnitsRetreatingMessage> TYPE =
        MessageType.of(NotifyUnitsRetreatingMessage.class);

    @Nonnull private final String battleId;
    @Nonnull private final Collection<String> retreatingUnitIds;

    public NotifyUnitsRetreatingMessage(
        final UUID battleId, final Collection<Unit> retreatingUnits) {
      this.battleId = battleId.toString();
      this.retreatingUnitIds =
          retreatingUnits.stream()
              .map(Unit::getId)
              .map(UUID::toString)
              .collect(Collectors.toList());
    }

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }

    public void accept(final IDisplay display, final UnitsList unitsList) {
      display.notifyRetreat(
          UUID.fromString(battleId),
          retreatingUnitIds.stream()
              .map(UUID::fromString)
              .map(unitsList::get)
              .collect(Collectors.toList()));
    }
  }

  /** Show dice for the given battle and step. */
  @RemoteActionCode(7)
  void notifyDice(DiceRoll dice, String stepName);

  @Builder
  @AllArgsConstructor
  class NotifyDiceMessage implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 7519027418522003773L;

    public static final MessageType<NotifyDiceMessage> TYPE =
        MessageType.of(NotifyDiceMessage.class);

    private final String stepName;
    private final List<DieRollData> diceRollData;
    private final int diceRollHits;
    private final double diceRollExpectedHits;
    private final String playerName;

    public NotifyDiceMessage(
        final DiceRoll diceRoll, final String stepName, final String playerName) {
      this.stepName = stepName;
      diceRollExpectedHits = diceRoll.getExpectedHits();
      diceRollHits = diceRoll.getHits();
      diceRollData =
          diceRoll.getRolls().stream().map(DieRollData::new).collect(Collectors.toList());
      this.playerName = playerName;
    }

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }

    public void accept(final IDisplay display) {
      final List<Die> rolls = DieRollData.toDieList(diceRollData);
      DiceRoll diceRoll = new DiceRoll(rolls, diceRollHits, diceRollExpectedHits, playerName);
      display.notifyDice(diceRoll, stepName);
    }
  }

  @RemoteActionCode(5)
  void gotoBattleStep(UUID battleId, String step);

  @AllArgsConstructor
  class GoToBattleStepMessage implements WebSocketMessage, Consumer<IDisplay>, Serializable {
    @Serial private static final long serialVersionUID = 2015225022060370679L;

    public static final MessageType<GoToBattleStepMessage> TYPE =
        MessageType.of(GoToBattleStepMessage.class);

    private final String battleStepUuid;
    private final String battleStepName;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }

    @Override
    public void accept(final IDisplay display) {
      display.gotoBattleStep(UUID.fromString(battleStepUuid), battleStepName);
    }
  }

  @RemoteActionCode(13)
  void shutDown();

  class DisplayShutdownMessage implements WebSocketMessage, Consumer<IDisplay> {
    public static final MessageType<DisplayShutdownMessage> TYPE =
        MessageType.of(DisplayShutdownMessage.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }

    @Override
    public void accept(final IDisplay display) {
      display.shutDown();
    }
  }

  @Value
  class DieRollData implements Serializable {
    @Serial private static final long serialVersionUID = 8494082086391123010L;

    String type;
    int rolledAt;
    int value;

    public DieRollData(final Die die) {
      this.type = die.getType().toString();
      this.rolledAt = die.getRolledAt();
      this.value = die.getValue();
    }

    static List<Die> toDieList(final List<DieRollData> diceRollData) {
      return diceRollData.stream()
          .map(
              dieRollData ->
                  Die.builder()
                      .rolledAt(dieRollData.rolledAt)
                      .value(dieRollData.value)
                      .type(Die.DieType.valueOf(dieRollData.type))
                      .build())
          .collect(Collectors.toList());
    }
  }

  /** Typed-dispatch payload for {@link #reportMessageToPlayers}. */
  class ReportMessageToPlayersMessage implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 1263542879302145566L;

    public static final MessageType<ReportMessageToPlayersMessage> TYPE =
        MessageType.of(ReportMessageToPlayersMessage.class);

    private final List<EntityRef> playersToSendTo;
    @Nullable private final List<EntityRef> butNotThesePlayers;
    private final String message;
    private final String title;

    public ReportMessageToPlayersMessage(
        final Collection<GamePlayer> playersToSendTo,
        @Nullable final Collection<GamePlayer> butNotThesePlayers,
        final String message,
        final String title) {
      this.playersToSendTo = DisplayEntities.toPlayerRefs(playersToSendTo);
      this.butNotThesePlayers = DisplayEntities.toPlayerRefs(butNotThesePlayers);
      this.message = message;
      this.title = title;
    }

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }

    public void accept(final IDisplay display, final GameData gameData) {
      display.reportMessageToPlayers(
          DisplayEntities.resolvePlayers(playersToSendTo, gameData),
          DisplayEntities.resolvePlayers(butNotThesePlayers, gameData),
          message,
          title);
    }
  }

  /** Typed-dispatch payload for {@link #showBattle}. */
  class ShowBattleMessage implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 3384618987225198151L;

    public static final MessageType<ShowBattleMessage> TYPE =
        MessageType.of(ShowBattleMessage.class);

    private final String battleId;
    private final EntityRef location;
    private final String battleTitle;
    private final List<EntityRef> attackingUnits;
    private final List<EntityRef> defendingUnits;
    private final List<EntityRef> killedUnits;
    private final List<EntityRef> attackingWaitingToDie;
    private final List<EntityRef> defendingWaitingToDie;
    private final List<DisplayEntities.DependentUnits> dependentUnits;
    private final EntityRef attacker;
    private final EntityRef defender;
    private final boolean isAmphibious;
    private final String battleType;
    private final List<EntityRef> amphibiousLandAttackers;

    public ShowBattleMessage(
        final UUID battleId,
        final Territory location,
        final String battleTitle,
        final Collection<Unit> attackingUnits,
        final Collection<Unit> defendingUnits,
        final Collection<Unit> killedUnits,
        final Collection<Unit> attackingWaitingToDie,
        final Collection<Unit> defendingWaitingToDie,
        final Map<Unit, Collection<Unit>> dependentUnits,
        final GamePlayer attacker,
        final GamePlayer defender,
        final boolean isAmphibious,
        final BattleType battleType,
        final Collection<Unit> amphibiousLandAttackers) {
      this.battleId = battleId.toString();
      this.location = EntityRef.of(location);
      this.battleTitle = battleTitle;
      this.attackingUnits = DisplayEntities.toUnitRefs(attackingUnits);
      this.defendingUnits = DisplayEntities.toUnitRefs(defendingUnits);
      this.killedUnits = DisplayEntities.toUnitRefs(killedUnits);
      this.attackingWaitingToDie = DisplayEntities.toUnitRefs(attackingWaitingToDie);
      this.defendingWaitingToDie = DisplayEntities.toUnitRefs(defendingWaitingToDie);
      this.dependentUnits = DisplayEntities.toDependents(dependentUnits);
      this.attacker = EntityRef.of(attacker);
      this.defender = EntityRef.of(defender);
      this.isAmphibious = isAmphibious;
      this.battleType = battleType.name();
      this.amphibiousLandAttackers = DisplayEntities.toUnitRefs(amphibiousLandAttackers);
    }

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }

    public void accept(final IDisplay display, final GameData gameData) {
      display.showBattle(
          UUID.fromString(battleId),
          location.resolveTerritory(gameData),
          battleTitle,
          DisplayEntities.resolveUnits(attackingUnits, gameData),
          DisplayEntities.resolveUnits(defendingUnits, gameData),
          DisplayEntities.resolveUnits(killedUnits, gameData),
          DisplayEntities.resolveUnits(attackingWaitingToDie, gameData),
          DisplayEntities.resolveUnits(defendingWaitingToDie, gameData),
          DisplayEntities.resolveDependents(dependentUnits, gameData),
          attacker.resolvePlayer(gameData),
          defender.resolvePlayer(gameData),
          isAmphibious,
          BattleType.valueOf(battleType),
          DisplayEntities.resolveUnits(amphibiousLandAttackers, gameData));
    }
  }

  /** Typed-dispatch payload for {@link #listBattleSteps}. */
  class ListBattleStepsMessage implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 6774966848320300525L;

    public static final MessageType<ListBattleStepsMessage> TYPE =
        MessageType.of(ListBattleStepsMessage.class);

    private final String battleId;
    private final List<String> steps;

    public ListBattleStepsMessage(final UUID battleId, final List<String> steps) {
      this.battleId = battleId.toString();
      this.steps = List.copyOf(steps);
    }

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }

    public void accept(final IDisplay display) {
      display.listBattleSteps(UUID.fromString(battleId), steps);
    }
  }

  /** Typed-dispatch payload for {@link #battleEnd}. */
  class BattleEndMessage implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5121705706254773584L;

    public static final MessageType<BattleEndMessage> TYPE = MessageType.of(BattleEndMessage.class);

    private final String battleId;
    private final String message;

    public BattleEndMessage(final UUID battleId, final String message) {
      this.battleId = battleId.toString();
      this.message = message;
    }

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }

    public void accept(final IDisplay display) {
      display.battleEnd(UUID.fromString(battleId), message);
    }
  }

  /** Typed-dispatch payload for {@link #casualtyNotification}. */
  class CasualtyNotificationMessage implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 8748094836451237845L;

    public static final MessageType<CasualtyNotificationMessage> TYPE =
        MessageType.of(CasualtyNotificationMessage.class);

    private final String battleId;
    private final String step;
    private final List<DieRollData> diceRollData;
    private final int diceRollHits;
    private final double diceRollExpectedHits;
    private final String dicePlayerName;
    private final EntityRef player;
    private final List<EntityRef> killed;
    private final List<EntityRef> damaged;
    private final List<DisplayEntities.DependentUnits> dependents;

    public CasualtyNotificationMessage(
        final UUID battleId,
        final String step,
        final DiceRoll dice,
        final GamePlayer player,
        final Collection<Unit> killed,
        final Collection<Unit> damaged,
        final Map<Unit, Collection<Unit>> dependents) {
      this.battleId = battleId.toString();
      this.step = step;
      this.diceRollData =
          dice.getRolls().stream().map(DieRollData::new).collect(Collectors.toList());
      this.diceRollHits = dice.getHits();
      this.diceRollExpectedHits = dice.getExpectedHits();
      this.dicePlayerName = dice.getPlayerName();
      this.player = EntityRef.of(player);
      this.killed = DisplayEntities.toUnitRefs(killed);
      this.damaged = DisplayEntities.toUnitRefs(damaged);
      this.dependents = DisplayEntities.toDependents(dependents);
    }

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }

    public void accept(final IDisplay display, final GameData gameData) {
      final DiceRoll dice =
          new DiceRoll(
              DieRollData.toDieList(diceRollData),
              diceRollHits,
              diceRollExpectedHits,
              dicePlayerName);
      display.casualtyNotification(
          UUID.fromString(battleId),
          step,
          dice,
          player.resolvePlayer(gameData),
          DisplayEntities.resolveUnits(killed, gameData),
          DisplayEntities.resolveUnits(damaged, gameData),
          DisplayEntities.resolveDependents(dependents, gameData));
    }
  }

  /** Typed-dispatch payload for {@link #deadUnitNotification}. */
  class DeadUnitNotificationMessage implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 2266879842103155624L;

    public static final MessageType<DeadUnitNotificationMessage> TYPE =
        MessageType.of(DeadUnitNotificationMessage.class);

    private final String battleId;
    private final EntityRef player;
    private final List<EntityRef> dead;
    private final List<DisplayEntities.DependentUnits> dependents;

    public DeadUnitNotificationMessage(
        final UUID battleId,
        final GamePlayer player,
        final Collection<Unit> dead,
        final Map<Unit, Collection<Unit>> dependents) {
      this.battleId = battleId.toString();
      this.player = EntityRef.of(player);
      this.dead = DisplayEntities.toUnitRefs(dead);
      this.dependents = DisplayEntities.toDependents(dependents);
    }

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }

    public void accept(final IDisplay display, final GameData gameData) {
      display.deadUnitNotification(
          UUID.fromString(battleId),
          player.resolvePlayer(gameData),
          DisplayEntities.resolveUnits(dead, gameData),
          DisplayEntities.resolveDependents(dependents, gameData));
    }
  }

  /** Typed-dispatch payload for {@link #changedUnitsNotification}. */
  class ChangedUnitsNotificationMessage implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 7392033472549920184L;

    public static final MessageType<ChangedUnitsNotificationMessage> TYPE =
        MessageType.of(ChangedUnitsNotificationMessage.class);

    private final String battleId;
    private final EntityRef player;
    private final List<EntityRef> removedUnits;
    @Nullable private final List<EntityRef> addedUnits;
    @Nullable private final List<DisplayEntities.DependentUnits> dependents;

    public ChangedUnitsNotificationMessage(
        final UUID battleId,
        final GamePlayer player,
        final Collection<Unit> removedUnits,
        @Nullable final Collection<Unit> addedUnits,
        @Nullable final Map<Unit, Collection<Unit>> dependents) {
      this.battleId = battleId.toString();
      this.player = EntityRef.of(player);
      this.removedUnits = DisplayEntities.toUnitRefs(removedUnits);
      this.addedUnits = DisplayEntities.toUnitRefs(addedUnits);
      this.dependents = DisplayEntities.toDependents(dependents);
    }

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }

    public void accept(final IDisplay display, final GameData gameData) {
      display.changedUnitsNotification(
          UUID.fromString(battleId),
          player.resolvePlayer(gameData),
          DisplayEntities.resolveUnits(removedUnits, gameData),
          DisplayEntities.resolveUnits(addedUnits, gameData),
          DisplayEntities.resolveDependents(dependents, gameData));
    }
  }

  /**
   * Encodes the unit and player graphs carried by the display notifications as wire references,
   * re-resolving them against the receiver's own {@link GameData}. The referenced units already
   * exist on the receiving display (placed by the change stream and named by an earlier {@link
   * #showBattle}), so resolution is by id with no create step.
   */
  final class DisplayEntities {
    private DisplayEntities() {}

    @Nullable
    static List<EntityRef> toUnitRefs(@Nullable final Collection<Unit> units) {
      return units == null ? null : units.stream().map(EntityRef::of).collect(Collectors.toList());
    }

    @Nullable
    static List<Unit> resolveUnits(@Nullable final List<EntityRef> refs, final GameData gameData) {
      return refs == null
          ? null
          : refs.stream().map(ref -> ref.resolveUnit(gameData)).collect(Collectors.toList());
    }

    @Nullable
    static List<EntityRef> toPlayerRefs(@Nullable final Collection<GamePlayer> players) {
      return players == null
          ? null
          : players.stream().map(EntityRef::of).collect(Collectors.toList());
    }

    @Nullable
    static List<GamePlayer> resolvePlayers(
        @Nullable final List<EntityRef> refs, final GameData gameData) {
      return refs == null
          ? null
          : refs.stream().map(ref -> ref.resolvePlayer(gameData)).collect(Collectors.toList());
    }

    @Nullable
    static List<DependentUnits> toDependents(
        @Nullable final Map<Unit, Collection<Unit>> dependents) {
      return dependents == null
          ? null
          : dependents.entrySet().stream()
              .map(
                  entry ->
                      new DependentUnits(
                          EntityRef.of(entry.getKey()), toUnitRefs(entry.getValue())))
              .collect(Collectors.toList());
    }

    @Nullable
    static Map<Unit, Collection<Unit>> resolveDependents(
        @Nullable final List<DependentUnits> dependents, final GameData gameData) {
      if (dependents == null) {
        return null;
      }
      final Map<Unit, Collection<Unit>> resolved = new HashMap<>();
      for (final DependentUnits entry : dependents) {
        resolved.put(
            entry.getUnit().resolveUnit(gameData), resolveUnits(entry.getDependents(), gameData));
      }
      return resolved;
    }

    /** A single {@code Unit -> its dependent units} entry from a dependency map. */
    @Value
    static class DependentUnits implements Serializable {
      @Serial private static final long serialVersionUID = 6902537129875302144L;

      EntityRef unit;
      @Nullable List<EntityRef> dependents;
    }
  }
}
