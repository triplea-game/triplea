package games.strategy.triplea.delegate.battle.data;

import java.util.List;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.AirBattle;

/**
 * Helper class for battle related data nd rules which we would otherwise get from AbstractBattle + GameData.
 * Meant to be a stepping stone for reducing dependency on gameData and allow simplifications
 * when using methods related to gameData.
 */
public class BattleRules {


  private final boolean isBombingRun;
  private final Territory battleSite;
  private final GameData gameData;
  private final PlayerID attacker;

  public BattleRules(
      final GameData gameData,
      final boolean isBombingRun,
      final Territory battleSite,
      final PlayerID attacker) {
    this.gameData = gameData;
    this.isBombingRun = isBombingRun;
    this.battleSite = battleSite;
    this.attacker = attacker;
  }


  public List<Unit> defendingUnits() {
    return isBombingRun
        ? battleSite.getUnits().getMatches(
            AirBattle.defendingBombingRaidInterceptors(attacker, gameData))
        : battleSite.getUnits().getMatches(
            AirBattle.defendingGroundSeaBattleInterceptors(attacker, gameData));
  }
}
