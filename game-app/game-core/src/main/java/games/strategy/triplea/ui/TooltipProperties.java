package games.strategy.triplea.ui;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitAttachment;
import org.triplea.util.LocalizeHtml;

/** Generates unit tooltips based on the content of the map's {@code tooltips.properties} file. */
public final class TooltipProperties extends PropertyFile {
  private static final String PROPERTY_FILE = "tooltips.properties";
  private static final String TOOLTIP = "tooltip";
  private static final String UNIT = "unit";

  private TooltipProperties() {
    super(PROPERTY_FILE);
  }

  public static TooltipProperties getInstance() {
    return PropertyFile.getInstance(TooltipProperties.class, TooltipProperties::new);
  }

  /** Get unit type tooltip checking for custom tooltip content. */
  public String getTooltip(final UnitType unitType, final GamePlayer gamePlayer) {

    final String customTip = getToolTip(unitType, gamePlayer, false);
    if (!customTip.isEmpty()) {
      return LocalizeHtml.localizeImgLinksInHtml(
          customTip, UiContext.getResourceLoader().getMapLocation());
    }
    final String generated =
        UnitAttachment.get(unitType)
            .toStringShortAndOnlyImportantDifferences(
                (gamePlayer == null ? GamePlayer.NULL_PLAYERID : gamePlayer));
    final String appendedTip = getToolTip(unitType, gamePlayer, true);
    if (!appendedTip.isEmpty()) {
      return generated
          + LocalizeHtml.localizeImgLinksInHtml(
              appendedTip, UiContext.getResourceLoader().getMapLocation());
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
                + (gamePlayer == null ? GamePlayer.NULL_PLAYERID.getName() : gamePlayer.getName())
                + append,
            "");
    return (tooltip == null || tooltip.isEmpty())
        ? properties.getProperty(TOOLTIP + "." + UNIT + "." + ut.getName() + append, "")
        : tooltip;
  }
}
