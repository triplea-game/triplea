package games.strategy.triplea.ui.display;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import games.strategy.triplea.ui.MainGameFrame;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.display.IDisplayBridge;
import games.strategy.net.GUID;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Die;
import games.strategy.triplea.delegate.IBattle.BattleType;

public class DummyTripleADisplay implements ITripleADisplay {
  private final MainGameFrame m_ui;

  /**
   * A display which does absolutely nothing
   */
  public DummyTripleADisplay() {
    m_ui = null;
  }

  /**
   * A display which does absolutely nothing, except for stopping the game on shutdown.
   *
   * @param ui
   *        MainGameFrame which we will call .stopGame() on if this DummyTripleaDisplay has .shutDown() called.
   */
  public DummyTripleADisplay(final MainGameFrame ui) {
    m_ui = ui;
  }

  @Override
  public void initialize(final IDisplayBridge bridge) {}

  @Override
  public void shutDown() {
    // make sure to shut down the ui if there is one
    if (m_ui != null) {
      m_ui.stopGame();
    }
  }

  @Override
  public void reportMessageToAll(final String message, final String title, final boolean doNotIncludeHost,
      final boolean doNotIncludeClients, final boolean doNotIncludeObservers) {}

  @Override
  public void reportMessageToPlayers(final Collection<PlayerID> playersToSendTo,
      final Collection<PlayerID> butNotThesePlayers, final String message, final String title) {}

  @Override
  public void showBattle(final GUID battleID, final Territory location, final String battleTitle,
      final Collection<Unit> attackingUnits, final Collection<Unit> defendingUnits, final Collection<Unit> killedUnits,
      final Collection<Unit> attackingWaitingToDie, final Collection<Unit> defendingWaitingToDie,
      final Map<Unit, Collection<Unit>> dependentUnits, final PlayerID attacker, final PlayerID defender,
      final boolean isAmphibious, final BattleType battleType, final Collection<Unit> amphibiousLandAttackers) {}

  @Override
  public void listBattleSteps(final GUID battleID, final List<String> steps) {}

  @Override
  public void battleEnd(final GUID battleID, final String message) {}

  @Override
  public void casualtyNotification(final GUID battleID, final String step, final DiceRoll dice, final PlayerID player,
      final Collection<Unit> killed, final Collection<Unit> damaged, final Map<Unit, Collection<Unit>> dependents) {}

  @Override
  public void deadUnitNotification(final GUID battleID, final PlayerID player, final Collection<Unit> dead,
      final Map<Unit, Collection<Unit>> dependents) {}

  @Override
  public void changedUnitsNotification(final GUID battleID, final PlayerID player, final Collection<Unit> removedUnits,
      final Collection<Unit> addedUnits, final Map<Unit, Collection<Unit>> dependents) {}

  @Override
  public void bombingResults(final GUID battleID, final List<Die> dice, final int cost) {}

  @Override
  public void notifyRetreat(final String shortMessage, final String message, final String step,
      final PlayerID retreatingPlayer) {}

  @Override
  public void notifyRetreat(final GUID battleId, final Collection<Unit> retreating) {}

  @Override
  public void notifyDice(final DiceRoll dice, final String stepName) {}

  @Override
  public void gotoBattleStep(final GUID battleId, final String step) {}
}
