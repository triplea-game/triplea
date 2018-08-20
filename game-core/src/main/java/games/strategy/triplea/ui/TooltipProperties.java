package games.strategy.triplea.ui;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.ResourceLoader;
import games.strategy.util.UrlStreams;
import lombok.extern.java.Log;

@Log
public class TooltipProperties {
  // Filename
  private static final String PROPERTY_FILE = "tooltips.properties";
  // Properties
  private static final String TOOLTIP = "tooltip";
  private static final String UNIT = "unit";
  private static TooltipProperties ttp = null;
  private static Instant timestamp = Instant.EPOCH;
  private final Properties properties = new Properties();

  protected TooltipProperties() {
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

  public String getAppendedToolTip(final UnitType ut, final PlayerID playerId) {
    return getToolTip(ut, playerId, true);
  }

  public String getToolTip(final UnitType ut, final PlayerID playerId) {
    return getToolTip(ut, playerId, false);
  }

  private String getToolTip(final UnitType ut, final PlayerID playerId, final boolean isAppending) {
    final String append = isAppending ? ".append" : "";
    final String tooltip = properties.getProperty(TOOLTIP + "." + UNIT + "." + ut.getName() + "."
        + (playerId == null ? PlayerID.NULL_PLAYERID.getName() : playerId.getName()) + append, "");
    return (tooltip == null || tooltip.isEmpty())
        ? properties.getProperty(TOOLTIP + "." + UNIT + "." + ut.getName() + append, "")
        : tooltip;
  }

}
