package games.strategy.triplea.ai.Dynamix_AI.Others;

import java.util.List;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.Dynamix_AI.DSettings;
import games.strategy.triplea.ai.Dynamix_AI.DUtils;
import games.strategy.triplea.delegate.Matches;


public class Battle_RetreatTerCalculator {
  public static Territory CalculateBestRetreatTer(final GameData data, final PlayerID player, final List<Territory> possibles,
      final Territory battleTer) {
    final List<Territory> ourCaps = DUtils.GetAllOurCaps_ThatWeOwn(data, player);
    Territory highestScoringTer = null;
    float highestScore = Integer.MIN_VALUE;
    for (final Territory ter : possibles) {
      float score = 0;
      final float oldSurvivalChance =
          DUtils.GetSurvivalChanceOfArmy(data, player, ter, DUtils.GetTerUnitsAtEndOfTurn(data, player, ter), 500);
      final List<Unit> afterDefenders = DUtils.GetTerUnitsAtEndOfTurn(data, player, ter);
      afterDefenders.removeAll(battleTer.getUnits().getMatches(Matches.unitIsOwnedBy(player)));
      afterDefenders.addAll(battleTer.getUnits().getMatches(Matches.unitIsOwnedBy(player)));
      float newSurvivalChance = DUtils.GetSurvivalChanceOfArmy(data, player, ter, afterDefenders, 500);
      if (newSurvivalChance > .9F) {
        newSurvivalChance = .9F; // Then accept similar chances as equal
      }
      final boolean isImportant = ourCaps.contains(ter);
      final float importantTerChanceRequired =
          DUtils.ToFloat(DSettings.LoadSettings().TR_reinforceStabalize_enemyAttackSurvivalChanceRequired);
      // If this ter is important, and retreating here will make the ter safe, boost score a lot
      if (isImportant && oldSurvivalChance < importantTerChanceRequired && newSurvivalChance >= importantTerChanceRequired) {
        score += 100000;
      }
      score += newSurvivalChance * 10000;
      if (!ter.isWater()) {
        score += DUtils.GetValueOfLandTer(ter, data, player);
      }
      if (score > highestScore) {
        highestScore = score;
        highestScoringTer = ter;
      }
    }
    return highestScoringTer;
  }
}
