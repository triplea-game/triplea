package games.strategy.engine.display;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.message.IChannelSubscriber;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Die;
import games.strategy.triplea.delegate.battle.IBattle.BattleType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A Display is a view of the game. Displays listen on the display channel for game events. A
 * Display may interact with many {@link games.strategy.engine.player.Player}s.
 */
public interface IDisplay extends IChannelSubscriber {
  /**
   * Sends a message to all TripleAFrame that have joined the game, possibly including observers.
   */
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
  void listBattleSteps(UUID battleId, List<String> steps);

  /** The given battle has ended. */
  void battleEnd(UUID battleId, String message);

  /** Notify that the casualties occurred. */
  void casualtyNotification(
      UUID battleId,
      String step,
      DiceRoll dice,
      GamePlayer player,
      Collection<Unit> killed,
      Collection<Unit> damaged,
      Map<Unit, Collection<Unit>> dependents);

  /** Notify that the casualties occurred, and only the casualty. */
  void deadUnitNotification(
      UUID battleId,
      GamePlayer player,
      Collection<Unit> dead,
      Map<Unit, Collection<Unit>> dependents);

  void changedUnitsNotification(
      UUID battleId,
      GamePlayer player,
      Collection<Unit> removedUnits,
      Collection<Unit> addedUnits,
      Map<Unit, Collection<Unit>> dependents);

  /** Notification of the results of a bombing raid. */
  void bombingResults(UUID battleId, List<Die> dice, int cost);

  /** Notify that the given player has retreated some or all of his units. */
  void notifyRetreat(String shortMessage, String message, String step, GamePlayer retreatingPlayer);

  void notifyRetreat(UUID battleId, Collection<Unit> retreating);

  /** Show dice for the given battle and step. */
  void notifyDice(DiceRoll dice, String stepName);

  void gotoBattleStep(UUID battleId, String step);

  void shutDown();
}
