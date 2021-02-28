package games.strategy.triplea.ui.display;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.display.IDisplay;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Die;
import games.strategy.triplea.delegate.battle.IBattle.BattleType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of {@link IDisplay} appropriate for a headless environment. All methods are stubs
 * that do nothing.
 */
public class HeadlessDisplay implements IDisplay {

  @Override
  public void shutDown() {}

  @Override
  public void reportMessageToAll(
      final String message,
      final String title,
      final boolean doNotIncludeHost,
      final boolean doNotIncludeClients,
      final boolean doNotIncludeObservers) {}

  @Override
  public void reportMessageToPlayers(
      final Collection<GamePlayer> playersToSendTo,
      final Collection<GamePlayer> butNotThesePlayers,
      final String message,
      final String title) {}

  @Override
  public void showBattle(
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
      final Collection<Unit> amphibiousLandAttackers) {}

  @Override
  public void listBattleSteps(final UUID battleId, final List<String> steps) {}

  @Override
  public void battleEnd(final UUID battleId, final String message) {}

  @Override
  public void casualtyNotification(
      final UUID battleId,
      final String step,
      final DiceRoll dice,
      final GamePlayer player,
      final Collection<Unit> killed,
      final Collection<Unit> damaged,
      final Map<Unit, Collection<Unit>> dependents) {}

  @Override
  public void deadUnitNotification(
      final UUID battleId,
      final GamePlayer player,
      final Collection<Unit> dead,
      final Map<Unit, Collection<Unit>> dependents) {}

  @Override
  public void changedUnitsNotification(
      final UUID battleId,
      final GamePlayer player,
      final Collection<Unit> removedUnits,
      final Collection<Unit> addedUnits,
      final Map<Unit, Collection<Unit>> dependents) {}

  @Override
  public void bombingResults(final UUID battleId, final List<Die> dice, final int cost) {}

  @Override
  public void notifyRetreat(
      final String shortMessage,
      final String message,
      final String step,
      final GamePlayer retreatingPlayer) {}

  @Override
  public void notifyRetreat(final UUID battleId, final Collection<Unit> retreating) {}

  @Override
  public void notifyDice(final DiceRoll dice, final String stepName) {}

  @Override
  public void gotoBattleStep(final UUID battleId, final String step) {}
}
