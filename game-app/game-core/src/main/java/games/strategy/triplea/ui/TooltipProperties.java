package games.strategy.triplea.ui;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.UnitType;
import java.util.Properties;
import org.jetbrains.annotations.NonNls;
import org.triplea.util.LocalizeHtml;

/** Generates unit tooltips based on the content of the map's {@code tooltips.properties} file. */
public final class TooltipProperties {
  @NonNls private static final String PROPERTY_FILE = "tooltips.properties";
  private static final String TOOLTIP = "tooltip";
  private static final String UNIT = "unit";
  private final UiContext uiContext;
  private final Properties properties;

  public TooltipProperties(final UiContext uiContext) {
    properties = uiContext.getResourceLoader().loadPropertyFile(PROPERTY_FILE);
    this.uiContext = uiContext;
  }

  /** Get unit type tooltip checking for custom tooltip content. */
  public String getTooltip(final UnitType unitType, final GamePlayer gamePlayer) {
    final String customTip = getToolTip(unitType, gamePlayer, false);
    if (!customTip.isEmpty()) {
      return LocalizeHtml.localizeImgLinksInHtml(customTip, uiContext.getMapLocation());
    }
    final String generated =
        unitType
            .getUnitAttachment()
            .toStringShortAndOnlyImportantDifferences(
                (gamePlayer == null
                    ? unitType.getData().getPlayerList().getNullPlayer()
                    : gamePlayer));
    final String appendedTip = getToolTip(unitType, gamePlayer, true);
    if (!appendedTip.isEmpty()) {
      return generated
          + LocalizeHtml.localizeImgLinksInHtml(appendedTip, uiContext.getMapLocation());
    }
    return generated;
  }

  private String getToolTip(
      final UnitType ut, final GamePlayer gamePlayer, final boolean isAppending) {
    final String append = isAppending ? ".append" : "";
    final String tooltip =
        properties.getProperty(
            TOOLTIP
                + "."
                + UNIT
                + "."
                + ut.getName()
                + "."
                + (gamePlayer == null
                    ? ut.getData().getPlayerList().getNullPlayer().getName()
                    : gamePlayer.getName())
                + append,
            "");
    return (tooltip == null || tooltip.isEmpty())
        ? properties.getProperty(TOOLTIP + "." + UNIT + "." + ut.getName() + append, "")
        : tooltip;
  }
}
