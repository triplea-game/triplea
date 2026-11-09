package games.strategy.engine.framework.startup.ui.panels.main.game.selector;

import games.strategy.engine.framework.map.file.system.loader.InstalledMapsListing;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.ThreadRunner;

/**
 * Thumbnail of the selected map's preview image, shown left of the game title. Hidden when the map
 * has no preview image.
 */
@Slf4j
final class MapThumbnail {
  /** Keyed by height and normalized map name; empty records a map without a preview image. */
  private static final Map<String, Optional<Icon>> cache = new ConcurrentHashMap<>();

  private final JLabel label = new JLabel();
  private final int height;

  /** Cache key of the map last asked for; a load that finishes for another map is dropped. */
  @Nullable private String shownKey;

  MapThumbnail(final int height) {
    this.height = height;
    label.setVerticalAlignment(SwingConstants.TOP);
    label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));
    label.setVisible(false);
  }

  JComponent getComponent() {
    return label;
  }

  /** Shows the preview image of the named map. Call on the EDT. */
  void showMap(final String mapName) {
    final String key = height + ":" + InstalledMapsListing.normalizeName(mapName);
    if (key.equals(shownKey)) {
      return;
    }
    shownKey = key;
    final Optional<Icon> cached = cache.get(key);
    if (cached != null) {
      setIcon(cached);
      return;
    }
    label.setVisible(false);
    ThreadRunner.runInNewThread(
        () -> {
          final Optional<Icon> icon = load(mapName);
          cache.put(key, icon);
          SwingUtilities.invokeLater(
              () -> {
                if (key.equals(shownKey)) {
                  setIcon(icon);
                }
              });
        });
  }

  /** Hides the thumbnail. Call on the EDT. */
  void clear() {
    shownKey = null;
    setIcon(Optional.empty());
  }

  private void setIcon(final Optional<Icon> icon) {
    label.setIcon(icon.orElse(null));
    label.setVisible(icon.isPresent());
  }

  private Optional<Icon> load(final String mapName) {
    return InstalledMapsListing.parseMapFiles()
        .findPreviewImageForMapName(mapName)
        .flatMap(MapThumbnail::readImage)
        .map(this::scale);
  }

  private static Optional<BufferedImage> readImage(final Path file) {
    try {
      // null when no ImageIO reader understands the file
      return Optional.ofNullable(ImageIO.read(file.toFile()));
    } catch (final IOException e) {
      log.warn("Failed to read map preview image: {}", file, e);
      return Optional.empty();
    }
  }

  /** Scales to the thumbnail height, narrowing very wide images to at most three heights wide. */
  private Icon scale(final BufferedImage image) {
    final double factor =
        Math.min((double) height / image.getHeight(), 3.0 * height / image.getWidth());
    final int width = Math.max(1, (int) Math.round(image.getWidth() * factor));
    final int scaledHeight = Math.max(1, (int) Math.round(image.getHeight() * factor));
    return new ImageIcon(image.getScaledInstance(width, scaledHeight, Image.SCALE_SMOOTH));
  }
}
