package games.strategy.triplea.ui.menubar.help;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.triplea.image.UnitImageFactory.ImageKey;
import games.strategy.triplea.ui.UiContext;
import games.strategy.triplea.util.TuvUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class UnitStatsTable {

  public static String getUnitStatsTable(final GameData gameData, final UiContext uiContext) {
    // html formatted string
    final StringBuilder hints = new StringBuilder();
    hints.append("<html>");
    hints.append("<head><style>th, tr{color:black}</style></head>");
    final Map<GamePlayer, Map<UnitType, ResourceCollection>> costs =
        TuvUtils.getResourceCostsForTuv(gameData, true);
    final Map<GamePlayer, List<UnitType>> playerUnitTypes =
        getAllPlayerUnitsWithImages(gameData, uiContext);
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
              .append(uiContext.getTooltipProperties().getTooltip(ut, player))
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
    hints.append("</html>");
    return hints.toString();
  }

  /** Will return a key of NULL for any units which we do not have art for. */
  private static Map<GamePlayer, List<UnitType>> getAllPlayerUnitsWithImages(
      final GameState data, final UiContext uiContext) {
    final Map<GamePlayer, List<UnitType>> unitTypes = new LinkedHashMap<>();
    for (final GamePlayer p : data.getPlayerList().getPlayers()) {
      unitTypes.put(p, getPlayerUnitsWithImages(p, data, uiContext));
    }
    final Set<UnitType> unitsSoFar = new HashSet<>();
    for (final List<UnitType> l : unitTypes.values()) {
      unitsSoFar.addAll(l);
    }
    final Set<UnitType> all = data.getUnitTypeList().getAllUnitTypes();
    all.removeAll(unitsSoFar);
    unitTypes.put(
        data.getPlayerList().getNullPlayer(),
        getPlayerUnitsWithImages(data.getPlayerList().getNullPlayer(), data, uiContext));
    unitsSoFar.addAll(unitTypes.get(data.getPlayerList().getNullPlayer()));
    all.removeAll(unitsSoFar);
    if (!all.isEmpty()) {
      unitTypes.put(null, new ArrayList<>(all));
    }
    return unitTypes;
  }

  private static List<UnitType> getPlayerUnitsWithImages(
      final GamePlayer player, final GameState data, final UiContext uiContext) {
    final Set<UnitType> unitTypes = new HashSet<>();
    // add first based on current production ability
    if (player.getProductionFrontier() != null) {
      for (final ProductionRule productionRule : player.getProductionFrontier()) {
        for (final Map.Entry<NamedAttachable, Integer> entry :
            productionRule.getResults().entrySet()) {
          if (UnitType.class.isAssignableFrom(entry.getKey().getClass())) {
            final UnitType ut = (UnitType) entry.getKey();
            unitTypes.add(ut);
          }
        }
      }
    }
    // this next part is purely to allow people to "add" neutral (null player) units to
    // territories.
    // This is because the null player does not have a production frontier, and we also do not
    // know what units we have art for, so only use the units on a map.
    for (final Territory t : data.getMap()) {
      for (final Unit u : t.getUnitCollection()) {
        if (u.isOwnedBy(player)) {
          final UnitType ut = u.getType();
          unitTypes.add(ut);
        }
      }
    }
    // now check if we have the art for anything that is left
    for (final UnitType ut : data.getUnitTypeList().getAllUnitTypes()) {
      if (!unitTypes.contains(ut)) {
        try {
          final UnitImageFactory imageFactory = uiContext.getUnitImageFactory();
          if (imageFactory.hasImage(ImageKey.builder().player(player).type(ut).build())) {
            unitTypes.add(ut);
          }
        } catch (final Exception e) {
          log.error("Exception while drawing unit type: " + ut + ", ", e);
        }
      }
    }
    return unitTypes.stream()
        .sorted(Comparator.comparing(UnitType::getName))
        .collect(Collectors.toList());
  }

  private static String getUnitImageUrl(
      final UnitType unitType, final GamePlayer player, final UiContext uiContext) {
    final UnitImageFactory unitImageFactory = uiContext.getUnitImageFactory();
    if (player == null || unitImageFactory == null) {
      return "no image";
    }
    final String imageLocation =
        unitImageFactory
            .getPossiblyTransformedImageUrl(
                ImageKey.builder().type(unitType).player(player).build())
            .map(Object::toString)
            .orElse("");
    return "<img src=\"" + imageLocation + "\" border=\"0\"/>";
  }
}
