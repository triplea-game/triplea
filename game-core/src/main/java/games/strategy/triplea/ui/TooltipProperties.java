package games.strategy.triplea.ui;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;

import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.util.LocalizeHtml;
import games.strategy.util.UrlStreams;
import lombok.extern.java.Log;

/** Generates unit tooltips based on the content of the map's {@code tooltips.properties} file. */
@Log
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
    final ResourceLoader loader = AbstractUiContext.getResourceLoader();
    final URL url = loader.getResource(PROPERTY_FILE);
    if (url != null) {
      final Optional<InputStream> inputStream = UrlStreams.openStream(url);
      if (inputStream.isPresent()) {
        try {
          properties.load(inputStream.get());
        } catch (final IOException e) {
          log.log(Level.SEVERE, "Error reading: " + PROPERTY_FILE, e);
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
  public String getTooltip(final UnitType unitType, final PlayerId playerId) {
    final String customTip = getToolTip(unitType, playerId, false);
    if (!customTip.isEmpty()) {
      return LocalizeHtml.localizeImgLinksInHtml(customTip);
    }
    final String generated =
        UnitAttachment.get(unitType)
            .toStringShortAndOnlyImportantDifferences(
                (playerId == null ? PlayerId.NULL_PLAYERID : playerId));
    final String appendedTip = getToolTip(unitType, playerId, true);
    if (!appendedTip.isEmpty()) {
      return generated + LocalizeHtml.localizeImgLinksInHtml(appendedTip);
    }
    return generated;
  }

  private String getToolTip(final UnitType ut, final PlayerId playerId, final boolean isAppending) {
    final String append = isAppending ? ".append" : "";
    final String tooltip =
        properties.getProperty(
            TOOLTIP
                + "."
                + UNIT
                + "."
                + ut.getName()
                + "."
                + (playerId == null ? PlayerId.NULL_PLAYERID.getName() : playerId.getName())
                + append,
            "");
    return (tooltip == null || tooltip.isEmpty())
        ? properties.getProperty(TOOLTIP + "." + UNIT + "." + ut.getName() + append, "")
        : tooltip;
  }
}
