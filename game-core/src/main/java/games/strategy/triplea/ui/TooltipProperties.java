package games.strategy.triplea.ui;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.attachments.UnitAttachment;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.util.Optional;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.UrlStreams;
import org.triplea.util.LocalizeHtml;

/** Generates unit tooltips based on the content of the map's {@code tooltips.properties} file. */
@Slf4j
public final class TooltipProperties {
  // Filename
  private static final String PROPERTY_FILE = "tooltips.properties";
  // Properties
  private static final String TOOLTIP = "tooltip";
  private static final String UNIT = "unit";
  private static TooltipProperties ttp = null;
  private static Instant timestamp = Instant.EPOCH;
  private final Properties properties = new Properties();

  private TooltipProperties() {
    final ResourceLoader loader = UiContext.getResourceLoader();
    final URL url = loader.getResource(PROPERTY_FILE);
    if (url != null) {
      final Optional<InputStream> inputStream = UrlStreams.openStream(url);
      if (inputStream.isPresent()) {
        try {
          properties.load(inputStream.get());
        } catch (final IOException e) {
          log.error("Error reading: " + PROPERTY_FILE, e);
        }
      }
    }
  }

  public static TooltipProperties getInstance() {
    // cache properties for 5 seconds
    if (ttp == null || timestamp.plusSeconds(5).isBefore(Instant.now())) {
      ttp = new TooltipProperties();
      timestamp = Instant.now();
    }
    return ttp;
  }

  /** Get unit type tooltip checking for custom tooltip content. */
  public String getTooltip(final UnitType unitType, final GamePlayer gamePlayer) {
    final String customTip = getToolTip(unitType, gamePlayer, false);
    if (!customTip.isEmpty()) {
      return LocalizeHtml.localizeImgLinksInHtml(customTip);
    }
    final String generated =
        UnitAttachment.get(unitType)
            .toStringShortAndOnlyImportantDifferences(
                (gamePlayer == null ? GamePlayer.NULL_PLAYERID : gamePlayer));
    final String appendedTip = getToolTip(unitType, gamePlayer, true);
    if (!appendedTip.isEmpty()) {
      return generated + LocalizeHtml.localizeImgLinksInHtml(appendedTip);
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
