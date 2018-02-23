package games.strategy.triplea.util;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.util.IntegerMap;


public class BonusIncomeUtils {

  /**
   * Add bonus income based on the player's set percentage for all resources.
   *
   * @return string that summarizes all the changes
   */
  public static String addBonusIncome(final IntegerMap<Resource> income, final IDelegateBridge bridge,
      final PlayerID player) {
    final StringBuilder sb = new StringBuilder();
    for (final Resource resource : income.keySet()) {
      final int amount = income.getInt(resource);
      final int incomePercent = Properties.getIncomePercentage(player, bridge.getData());
      int puIncomeBonus = 0;
      if (resource.getName().equals(Constants.PUS)) {
        puIncomeBonus = Properties.getPuIncomeBonus(player, bridge.getData());
      }
      final int bonusIncome =
          (int) Math.round((((double) amount * (double) (incomePercent - 100)) / 100)) + puIncomeBonus;
      if (bonusIncome == 0) {
        continue;
      }
      final int total = player.getResources().getQuantity(resource) + bonusIncome;
      String message = "Giving " + player.getName();
      if (puIncomeBonus > 0) {
        message += " " + puIncomeBonus + " PUs &";
      }
      if (incomePercent != 100) {
        message += " " + incomePercent + "% income for " + bonusIncome + " ";
      } else {
        message = message.replace("&", "");
      }
      message += resource.getName() + "; end with " + total + " " + resource.getName();
      bridge.getHistoryWriter().startEvent(message);
      bridge.addChange(ChangeFactory.changeResourcesChange(player, resource, bonusIncome));
      sb.append(message).append("<br />");
    }
    return sb.toString();
  }

}
