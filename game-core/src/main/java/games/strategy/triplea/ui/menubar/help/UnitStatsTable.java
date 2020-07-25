package games.strategy.triplea.ui.menubar.help;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.triplea.ui.TooltipProperties;
import games.strategy.triplea.ui.UiContext;
import games.strategy.triplea.util.TuvUtils;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class UnitStatsTable {

  public static String getUnitStatsTable(final GameData gameData, final UiContext uiContext) {
    // html formatted string
    final StringBuilder hints = new StringBuilder();
    hints.append("<html>");
    hints.append("<head><style>th, tr{color:black}</style></head>");
    try {
      gameData.acquireReadLock();
      final Map<GamePlayer, Map<UnitType, ResourceCollection>> costs =
          TuvUtils.getResourceCostsForTuv(gameData, true);
      final Map<GamePlayer, List<UnitType>> playerUnitTypes =
          UnitType.getAllPlayerUnitsWithImages(gameData, uiContext, true);
      final String color3 = "FEECE2";
      final String color2 = "BDBDBD";
      final String color1 = "ABABAB";
      int i = 0;
      for (final Map.Entry<GamePlayer, List<UnitType>> entry : playerUnitTypes.entrySet()) {
        final GamePlayer player = entry.getKey();
        hints.append("<p><table border=\"1\" bgcolor=\"" + color1 + "\">");
        hints
            .append(
                "<tr><th style=\"font-size:120%;000000\" bgcolor=\"" + color3 + "\" colspan=\"4\">")
            .append(player == null ? "NULL" : player.getName())
            .append(" Units</th></tr>");
        hints
            .append("<tr")
            .append(((i & 1) == 0) ? " bgcolor=\"" + color1 + "\"" : " bgcolor=\"" + color2 + "\"")
            .append("><td>Unit</td><td>Name</td><td>Cost</td><td>Tool Tip</td></tr>");
        for (final UnitType ut : entry.getValue()) {
          if (uiContext.getMapData().shouldDrawUnit(ut.getName())) {
            i++;
            hints
                .append("<tr")
                .append(
                    ((i & 1) == 0) ? " bgcolor=\"" + color1 + "\"" : " bgcolor=\"" + color2 + "\"")
                .append(">")
                .append("<td>")
                .append(getUnitImageUrl(ut, player, uiContext))
                .append("</td>")
                .append("<td>")
                .append(ut.getName())
                .append("</td>")
                .append("<td>")
                .append(costs.get(player).get(ut).toStringForHtml())
                .append("</td>")
                .append("<td>")
                .append(TooltipProperties.getInstance().getTooltip(ut, player))
                .append("</td></tr>");
          }
        }
        i++;
        hints
            .append("<tr")
            .append(((i & 1) == 0) ? " bgcolor=\"" + color1 + "\"" : " bgcolor=\"" + color2 + "\"")
            .append(">")
            .append(
                "<td>Unit</td><td>Name</td><td>Cost</td><td>Tool Tip</td></tr></table></p><br />");
      }
    } finally {
      gameData.releaseReadLock();
    }
    hints.append("</html>");
    return hints.toString();
  }

  private static String getUnitImageUrl(
      final UnitType unitType, final GamePlayer player, final UiContext uiContext) {
    final UnitImageFactory unitImageFactory = uiContext.getUnitImageFactory();
    if (player == null || unitImageFactory == null) {
      return "no image";
    }
    final Optional<URL> imageUrl = unitImageFactory.getBaseImageUrl(unitType.getName(), player);
    final String imageLocation = imageUrl.map(Object::toString).orElse("");

    return "<img src=\"" + imageLocation + "\" border=\"0\"/>";
  }
}
