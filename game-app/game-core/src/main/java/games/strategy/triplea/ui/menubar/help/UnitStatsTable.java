package games.strategy.triplea.ui.menubar.help;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.triplea.image.UnitImageFactory.ImageKey;
import games.strategy.triplea.ui.UiContext;
import games.strategy.triplea.util.TuvUtils;
import java.text.MessageFormat;
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
import org.jetbrains.annotations.NonNls;

@UtilityClass
@Slf4j
public class UnitStatsTable {

  public static String getUnitStatsTable(final GameData gameData, final UiContext uiContext) {
    // html formatted string
    final @NonNls StringBuilder hints = new StringBuilder();
    hints.append("<html><head><style>th, tr{color:black}</style></head>");
    final Map<GamePlayer, Map<UnitType, ResourceCollection>> costs =
        TuvUtils.getResourceCostsForTuv(gameData, true);
    final Map<GamePlayer, List<UnitType>> playerUnitTypes =
        getAllPlayerUnitsWithImages(gameData, uiContext);
    final @NonNls String strColorVeryLightOrange = "FEECE2";
    final @NonNls String strColorLightGrey = "BDBDBD";
    final @NonNls String strColorGrey = "ABABAB";
    for (final Map.Entry<GamePlayer, List<UnitType>> entry : playerUnitTypes.entrySet()) {
      int lineIndex = 0;
      final GamePlayer player = entry.getKey();
      hints.append("<p><table border=\"1\" bgcolor=\"" + strColorGrey + "\">");
      hints
          .append(
              "<tr><th style=\"font-size:120%;000000\" bgcolor=\""
                  + strColorVeryLightOrange
                  + "\" colspan=\"4\">")
          .append(player == null ? "NULL" : player.getName())
          .append(" Units</th></tr>");
      String lineHeaderAndFooter = "<td>Unit</td><td>Name</td><td>Cost</td><td>Tool Tip</td>";
      addUnitStatsTableLineColored(
          hints, lineHeaderAndFooter, lineIndex, strColorGrey, strColorLightGrey);
      for (final UnitType ut : entry.getValue()) {
        if (uiContext.getMapData().shouldDrawUnit(ut.getName())) {
          lineIndex++;
          addUnitStatsTableLineColored(
              hints,
              MessageFormat.format(
                  "<td>{0}</td><td>{1}</td><td>{2}</td><td>{3}</td>",
                  getUnitImageUrl(ut, player, uiContext),
                  ut.getName(),
                  costs.get(player).get(ut).toStringForHtml(),
                  uiContext.getTooltipProperties().getTooltip(ut, player)),
              lineIndex,
              strColorGrey,
              strColorLightGrey);
        }
      }
      lineIndex++;
      addUnitStatsTableLineColored(
          hints, lineHeaderAndFooter, lineIndex, strColorGrey, strColorLightGrey);
      hints.append("</table></p><br />");
    }
    hints.append("</html>");
    return hints.toString();
  }

  private static void addUnitStatsTableLineColored(
      @NonNls StringBuilder hints,
      @NonNls String newLine,
      int lineIndex,
      @NonNls String strColorGrey,
      @NonNls String strColorLightGrey) {
    hints
        .append("<tr bgcolor=\"")
        .append(isOddLineIndex(lineIndex) ? strColorGrey : strColorLightGrey)
        .append("\">")
        .append(newLine)
        .append("</tr>");
  }

  private static boolean isOddLineIndex(int lineIndex) {
    return (lineIndex & 1) == 0;
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
    fillPlayerUnitTypesFromProductionAbility(player, unitTypes);
    // this next part is purely to allow people to "add" neutral (null player) units to
    // territories.
    // This is because the null player does not have a production frontier, and we also do not
    // know what units we have art for, so only use the units on a map.
    for (final Territory t : data.getMap()) {
      t.getUnitCollection().stream()
          .filter(Matches.unitIsOwnedBy(player))
          .forEach(
              unit -> {
                final UnitType ut = unit.getType();
                unitTypes.add(ut);
              });
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

  private static void fillPlayerUnitTypesFromProductionAbility(
      GamePlayer player, Set<UnitType> unitTypes) {
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
  }

  private static @NonNls String getUnitImageUrl(
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
