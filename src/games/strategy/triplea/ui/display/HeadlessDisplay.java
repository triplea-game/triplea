package games.strategy.triplea.ui.display;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.display.IDisplayBridge;
import games.strategy.net.GUID;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Die;
import games.strategy.triplea.delegate.IBattle.BattleType;

public class HeadlessDisplay implements ITripleADisplay {
  
  public HeadlessDisplay(){
  }

  @Override
  public void initialize(IDisplayBridge bridge) {}

  @Override
  public void shutDown() {
  }

  @Override
  public void reportMessageToAll(String message, String title, boolean doNotIncludeHost, boolean doNotIncludeClients,
      boolean doNotIncludeObservers) {}

  @Override
  public void reportMessageToPlayers(Collection<PlayerID> playersToSendTo, Collection<PlayerID> butNotThesePlayers,
      String message, String title) {}

  @Override
  public void showBattle(GUID battleID, Territory location, String battleTitle, Collection<Unit> attackingUnits,
      Collection<Unit> defendingUnits, Collection<Unit> killedUnits, Collection<Unit> attackingWaitingToDie,
      Collection<Unit> defendingWaitingToDie, Map<Unit, Collection<Unit>> dependentUnits, PlayerID attacker,
      PlayerID defender, boolean isAmphibious, BattleType battleType, Collection<Unit> amphibiousLandAttackers) {}

  @Override
  public void listBattleSteps(GUID battleID, List<String> steps) {}

  @Override
  public void battleEnd(GUID battleID, String message) {}

  @Override
  public void casualtyNotification(GUID battleID, String step, DiceRoll dice, PlayerID player, Collection<Unit> killed,
      Collection<Unit> damaged, Map<Unit, Collection<Unit>> dependents) {}

  @Override
  public void deadUnitNotification(GUID battleID, PlayerID player, Collection<Unit> dead,
      Map<Unit, Collection<Unit>> dependents) {}

  @Override
  public void changedUnitsNotification(GUID battleID, PlayerID player, Collection<Unit> removedUnits,
      Collection<Unit> addedUnits, Map<Unit, Collection<Unit>> dependents) {}

  @Override
  public void bombingResults(GUID battleID, List<Die> dice, int cost) {}

  @Override
  public void notifyRetreat(String shortMessage, String message, String step, PlayerID retreatingPlayer) {}

  @Override
  public void notifyRetreat(GUID battleId, Collection<Unit> retreating) {}

  @Override
  public void notifyDice(DiceRoll dice, String stepName) {}

  @Override
  public void gotoBattleStep(GUID battleId, String step) {}

}
