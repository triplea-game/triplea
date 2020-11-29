package games.strategy.engine.display;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.PlayerList;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitsList;
import games.strategy.engine.message.IChannelSubscriber;
import games.strategy.engine.message.RemoteActionCode;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Die;
import games.strategy.triplea.delegate.battle.IBattle.BattleType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
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

  class BombingResultsMessage implements WebSocketMessage {
    public static final MessageType<IDisplay.BombingResultsMessage> TYPE =
        MessageType.of(IDisplay.BombingResultsMessage.class);

    private final String battleId;
    private final List<DieRollData> diceData;
    private final Integer cost;

    public BombingResultsMessage(final UUID battleId, final List<Die> dice, final int cost) {
      this.battleId = battleId.toString();
      this.diceData = dice.stream().map(DieRollData::new).collect(Collectors.toList());
      ;
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
  class NotifyRetreatMessage implements WebSocketMessage {
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

  class NotifyUnitsRetreatingMessage implements WebSocketMessage {
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
  class NotifyDiceMessage implements WebSocketMessage {
    public static final MessageType<NotifyDiceMessage> TYPE =
        MessageType.of(NotifyDiceMessage.class);

    private final String stepName;
    private final List<DieRollData> diceRollData;
    private final int diceRollHits;
    private final double diceRollExpectedHits;

    public NotifyDiceMessage(final DiceRoll diceRoll, final String stepName) {
      this.stepName = stepName;
      diceRollExpectedHits = diceRoll.getExpectedHits();
      diceRollHits = diceRoll.getHits();
      diceRollData =
          diceRoll.getRolls().stream().map(DieRollData::new).collect(Collectors.toList());
    }

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }

    public void accept(final IDisplay display) {
      display.notifyDice(convertPayloadToDiceRoll(), stepName);
    }

    private DiceRoll convertPayloadToDiceRoll() {
      final List<Die> rolls = DieRollData.toDieList(diceRollData);
      return new DiceRoll(rolls, diceRollHits, diceRollExpectedHits);
    }
  }

  @RemoteActionCode(5)
  void gotoBattleStep(UUID battleId, String step);

  @AllArgsConstructor
  class GoToBattleStepMessage implements WebSocketMessage, Consumer<IDisplay> {
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
  class DieRollData {
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
}
