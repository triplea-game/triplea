package games.strategy.engine.framework.map.download;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.framework.system.HttpProxy;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;

/**
 * Map preview pictures, loaded in the background and kept for the maps window, scaled to the size
 * it shows them in: small in the rows of the lists, large in the details of the selected map. Only
 * the most recent ones are kept, so browsing hundreds of maps does not fill the memory.
 */
@Slf4j
final class MapPreviews {
  /** The sizes the maps window shows previews in; the picture fills it, cut where needed. */
  @Getter
  @AllArgsConstructor
  enum Size {
    /** In a row of a map list; all rows of a long list keep theirs. */
    ROW(144, 81, 400),
    /** On a tile of the tiles view; fewer are kept, as they are larger. */
    TILE(256, 144, 200),
    /**
     * In the details of the selected map, which size it to their width; only the maps looked at
     * recently keep theirs.
     */
    DETAILS(960, 540, 12);

    private final int width;
    private final int height;
    private final int kept;
  }

  /** The previews of a maps window, one cache for each size it shows them in. */
  record Caches(MapPreviews rows, MapPreviews tiles, MapPreviews details) {
    static Caches newCaches() {
      return new Caches(
          new MapPreviews(Size.ROW), new MapPreviews(Size.TILE), new MapPreviews(Size.DETAILS));
    }
  }

  private static final int TIMEOUT_MILLIS = 10_000;

  /** Downloads three previews at a time, so that a slow map host blocks no other work. */
  private static final ExecutorService LOADER =
      Executors.newFixedThreadPool(
          3,
          runnable -> {
            final Thread thread = new Thread(runnable, "Map preview loader");
            thread.setDaemon(true);
            return thread;
          });

  @Getter private final Size size;
  // the screen pixels per pixel of the layout, eg 1.5 at 150 % scaling; previews are made sharp
  @Getter private final double deviceScale = screenScale();
  // all state is read and written on the Swing thread only
  private final Map<String, ImageIcon> previews;
  // the callers waiting for a preview that is loading, by URL
  private final Map<String, List<Consumer<Optional<ImageIcon>>>> waiting = new HashMap<>();
  // previews that could not be loaded, by URL; they are not tried again
  private final Set<String> failed = new HashSet<>();

  MapPreviews(final Size size) {
    this.size = size;
    final int kept = size.getKept();
    previews =
        new LinkedHashMap<>(16, 0.75f, true) {
          private static final long serialVersionUID = 1L;

          @Override
          protected boolean removeEldestEntry(final Map.Entry<String, ImageIcon> eldest) {
            return size() > kept;
          }
        };
  }

  /** Returns the preview if it is loaded already. */
  Optional<ImageIcon> get(final String url) {
    return Optional.ofNullable(previews.get(url));
  }

  /**
   * Loads the preview in the background and passes it, or empty if it cannot be loaded, to the
   * callback on the Swing thread. A preview that failed once is not loaded again, one that is not
   * on the web never. Call this on the Swing thread.
   */
  void load(final @Nullable String url, final Consumer<Optional<ImageIcon>> callback) {
    if (url == null || !isWebAddress(url)) {
      // a map without a preview, or one the server names eg as a file:, jar: or UNC path, which
      // would read the player's own files: nothing to fetch
      callback.accept(Optional.empty());
      return;
    }
    final Optional<ImageIcon> cached = get(url);
    if (cached.isPresent() || failed.contains(url)) {
      callback.accept(cached);
      return;
    }
    final List<Consumer<Optional<ImageIcon>>> callbacks = waiting.get(url);
    if (callbacks != null) {
      // loading already
      callbacks.add(callback);
      return;
    }
    waiting.put(url, new ArrayList<>(List.of(callback)));
    LOADER.execute(
        () -> {
          final Optional<ImageIcon> preview = loadScaled(url, size, deviceScale);
          SwingUtilities.invokeLater(
              () -> {
                preview.ifPresentOrElse(icon -> previews.put(url, icon), () -> failed.add(url));
                waiting.remove(url).forEach(waitingCallback -> waitingCallback.accept(preview));
              });
        });
  }

  private static boolean isWebAddress(final String url) {
    final String lowerCase = url.toLowerCase(Locale.ROOT);
    return lowerCase.startsWith("https://") || lowerCase.startsWith("http://");
  }

  /** The scale of the main screen, eg 2 at 200 % scaling; 1 without a screen. */
  private static double screenScale() {
    return GraphicsEnvironment.isHeadless()
        ? 1
        : Math.max(
            1,
            GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration()
                .getDefaultTransform()
                .getScaleX());
  }

  private static Optional<ImageIcon> loadScaled(
      final @NonNls String url, final Size size, final double deviceScale) {
    try {
      return read(url).map(image -> new ImageIcon(scale(image, size, deviceScale)));
    } catch (final IOException | RuntimeException e) {
      // info: a missing or broken preview is no reason to bother the player with a dialog;
      // whatever goes wrong, the waiting rows must still hear back
      log.info("Could not load the map preview {}", url, e);
      return Optional.empty();
    }
  }

  private static Optional<BufferedImage> read(final String url) throws IOException {
    final URL previewUrl = URI.create(url).toURL();
    // through the proxy of the settings, like map downloads
    final URLConnection connection = previewUrl.openConnection(HttpProxy.getProxy());
    connection.setConnectTimeout(TIMEOUT_MILLIS);
    connection.setReadTimeout(TIMEOUT_MILLIS);
    try (InputStream stream = connection.getInputStream()) {
      return Optional.ofNullable(ImageIO.read(stream));
    }
  }

  /**
   * Returns the part of the picture a box of the size shows, in the box's shape, as large as fits
   * and centered, so the full download is not kept. It has as many pixels as the screen shows in
   * the box, eg twice the size at 200 % scaling, but never more than the picture has: a small
   * picture is not blown up.
   */
  @VisibleForTesting
  static BufferedImage scale(final BufferedImage image, final Size size, final double deviceScale) {
    final double shape = (double) size.getWidth() / size.getHeight();
    final int cropWidth =
        Math.max(1, Math.min(image.getWidth(), (int) Math.round(image.getHeight() * shape)));
    final int cropHeight =
        Math.max(1, Math.min(image.getHeight(), (int) Math.round(cropWidth / shape)));
    final BufferedImage crop =
        image.getSubimage(
            (image.getWidth() - cropWidth) / 2,
            (image.getHeight() - cropHeight) / 2,
            cropWidth,
            cropHeight);
    final int width = Math.min(cropWidth, (int) Math.round(size.getWidth() * deviceScale));
    final int height = Math.max(1, Math.min(cropHeight, (int) Math.round(width / shape)));
    return resample(crop, width, height);
  }

  /**
   * Shrinks the picture to the size in steps of half, then once with bicubic interpolation, which
   * keeps fine lines like borders and names that a single step loses.
   */
  private static BufferedImage resample(
      final BufferedImage image, final int width, final int height) {
    BufferedImage current = image;
    while (current.getWidth() / 2 >= width && current.getHeight() / 2 >= height) {
      current =
          draw(
              current,
              current.getWidth() / 2,
              current.getHeight() / 2,
              RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }
    return draw(current, width, height, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
  }

  private static BufferedImage draw(
      final BufferedImage image, final int width, final int height, final Object interpolation) {
    final BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D graphics = scaled.createGraphics();
    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation);
    graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    graphics.drawImage(image, 0, 0, width, height, null);
    graphics.dispose();
    return scaled;
  }
}
