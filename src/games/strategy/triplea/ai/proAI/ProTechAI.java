package games.strategy.triplea.ai.proAI;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.Constants;
import games.strategy.triplea.ai.strongAI.SUtils;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.delegate.remote.ITechDelegate;

import java.util.List;

/**
 * Pro tech AI.
 */
public class ProTechAI {

  public void tech(final ITechDelegate techDelegate, final GameData data, final PlayerID player) {
    if (!games.strategy.triplea.Properties.getWW2V3TechModel(data)) {
      return;
    }
    final Territory myCapitol = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
    final float eStrength = SUtils.getStrengthOfPotentialAttackers(myCapitol, data, player, false, true, null);
    float myStrength = SUtils.strength(myCapitol.getUnits().getUnits(), false, false, false);
    final List<Territory> areaStrength = SUtils.getNeighboringLandTerritories(data, player, myCapitol);
    for (final Territory areaTerr : areaStrength) {
      myStrength += SUtils.strength(areaTerr.getUnits().getUnits(), false, false, false) * 0.75F;
    }
    final boolean capDanger = myStrength < (eStrength * 1.25F + 3.0F);
    final Resource pus = data.getResourceList().getResource(Constants.PUS);
    final int PUs = player.getResources().getQuantity(pus);
    final Resource techtokens = data.getResourceList().getResource(Constants.TECH_TOKENS);
    final int TechTokens = player.getResources().getQuantity(techtokens);
    int TokensToBuy = 0;
    if (!capDanger && TechTokens < 3 && PUs > Math.random() * 160) {
      TokensToBuy = 1;
    }
    if (TechTokens > 0 || TokensToBuy > 0) {
      final List<TechnologyFrontier> cats = TechAdvance.getPlayerTechCategories(data, player);
      // retaining 65% chance of choosing land advances using basic ww2v3 model.
      if (data.getTechnologyFrontier().isEmpty()) {
        if (Math.random() > 0.35) {
          techDelegate.rollTech(TechTokens + TokensToBuy, cats.get(1), TokensToBuy, null);
        } else {
          techDelegate.rollTech(TechTokens + TokensToBuy, cats.get(0), TokensToBuy, null);
        }
      } else {
        final int rand = (int) (Math.random() * cats.size());
        techDelegate.rollTech(TechTokens + TokensToBuy, cats.get(rand), TokensToBuy, null);
      }
    }
  }

}
