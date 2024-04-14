package games.strategy.triplea.image;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TechTracker;
import games.strategy.triplea.ui.mapdata.MapData;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitOwner;
import games.strategy.ui.Util;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/** A factory with an image cache for creating unit images. */
@Slf4j
public class UnitImageFactory {
  public static final int DEFAULT_UNIT_ICON_SIZE = 48;
  private static final String FILE_NAME_BASE = "units/";

  /**
   * Width of all icons. You probably want getUnitImageWidth(), which takes scale factor into
   * account.
   */
  private final int unitIconWidth;

  /**
   * Height of all icons. You probably want getUnitImageHeight(), which takes scale factor into
   * account.
   */
  private final int unitIconHeight;

  private final int unitCounterOffsetWidth;
  private final int unitCounterOffsetHeight;
  // maps Point -> image
  private final Map<ImageKey, Image> images = new HashMap<>();
  // maps Point -> Icon
  private final Map<String, ImageIcon> icons = new HashMap<>();
  // Temporary colorized image files used for URLs for html views (e.g. unit stats table).
  private final Map<ImageKey, URL> colorizedTempFiles = new HashMap<>();
  private final List<File> tempFiles = new ArrayList<>();

  /** -- GETTER -- Return the unit scaling factor. */
  // Scaling factor for unit images
  @Getter private final double scaleFactor;

  private final ResourceLoader resourceLoader;
  private final MapData mapData;

  public UnitImageFactory(
      final ResourceLoader resourceLoader, final double unitScale, final MapData mapData) {
    unitIconWidth = mapData.getDefaultUnitWidth();
    unitIconHeight = mapData.getDefaultUnitHeight();
    unitCounterOffsetWidth = mapData.getDefaultUnitCounterOffsetWidth();
    unitCounterOffsetHeight = mapData.getDefaultUnitCounterOffsetHeight();
    this.scaleFactor = unitScale;
    this.resourceLoader = resourceLoader;
    this.mapData = mapData;
  }

  public void clearCache() {
    images.clear();
    icons.clear();
    deleteTempFiles();
    colorizedTempFiles.clear();
  }

  public void deleteTempFiles() {
    tempFiles.forEach(File::delete);
    tempFiles.clear();
  }

  @Value
  @Builder
  public static class ImageKey {
    GamePlayer player;
    UnitType type;
    boolean damaged;
    boolean disabled;

    public static ImageKey of(final UnitCategory unit) {
      return ImageKey.builder()
          .player(unit.getOwner())
          .type(unit.getType())
          .damaged(unit.hasDamageOrBombingUnitDamage())
          .disabled(unit.getDisabled())
          .build();
    }

    public static ImageKey of(final UnitOwner holder) {
      return ImageKey.builder().player(holder.getOwner()).type(holder.getType()).build();
    }

    public static ImageKey of(final Unit unit) {
      return ImageKey.builder()
          .player(unit.getOwner())
          .type(unit.getType())
          .damaged(Matches.unitHasTakenSomeBombingUnitDamage().test(unit))
          .disabled(Matches.unitIsDisabled().test(unit))
          .build();
    }

    public String getFullName() {
      return getBaseImageName() + player.getName();
    }

    public String getBaseImageName() {
      final GamePlayer gamePlayer = player;

      StringBuilder name = new StringBuilder(32);
      name.append(type.getName());
      if (!type.getName().endsWith("_hit") && !type.getName().endsWith("_disabled")) {
        final UnitAttachment ua = type.getUnitAttachment();
        if (type.getName().equals(Constants.UNIT_TYPE_AAGUN)) {
          if (TechTracker.hasRocket(gamePlayer) && ua.getIsRocket()) {
            name = new StringBuilder("rockets");
          }
          if (TechTracker.hasAaRadar(gamePlayer) && Matches.unitTypeIsAaForAnything().test(type)) {
            name.append("_r");
          }
        } else if (ua.getIsRocket() && Matches.unitTypeIsAaForAnything().test(type)) {
          if (TechTracker.hasRocket(gamePlayer)) {
            name.append("_rockets");
          }
          if (TechTracker.hasAaRadar(gamePlayer)) {
            name.append("_r");
          }
        } else if (ua.getIsRocket()) {
          if (TechTracker.hasRocket(gamePlayer)) {
            name.append("_rockets");
          }
        } else if (Matches.unitTypeIsAaForAnything().test(type)) {
          if (TechTracker.hasAaRadar(gamePlayer)) {
            name.append("_r");
          }
        }
        if (ua.getIsAir() && !ua.getIsStrategicBomber()) {
          if (TechTracker.hasLongRangeAir(gamePlayer)) {
            name.append("_lr");
          }
          if (TechTracker.hasJetFighter(gamePlayer)
              && (ua.getAttack(gamePlayer) > 0 || ua.getDefense(gamePlayer) > 0)) {
            name.append("_jp");
          }
        }
        if (ua.getIsAir() && ua.getIsStrategicBomber()) {
          if (TechTracker.hasLongRangeAir(gamePlayer)) {
            name.append("_lr");
          }
          if (TechTracker.hasHeavyBomber(gamePlayer)) {
            name.append("_hb");
          }
        }
        if (ua.getIsFirstStrike()
            && ua.getCanEvade()
            && (ua.getAttack(gamePlayer) > 0 || ua.getDefense(gamePlayer) > 0)
            && TechTracker.hasSuperSubs(gamePlayer)) {
          name.append("_ss");
        }
        if ((type.getName().equals(Constants.UNIT_TYPE_FACTORY) || ua.getCanProduceUnits())
            && (TechTracker.hasIndustrialTechnology(gamePlayer)
                || TechTracker.hasIncreasedFactoryProduction(gamePlayer))) {
          name.append("_it");
        }
      }
      if (disabled) {
        name.append("_disabled");
      } else if (damaged) {
        name.append("_hit");
      }
      return name.toString();
    }
  }

  /** Set the unitScaling factor. */
  public UnitImageFactory withScaleFactor(final double scaleFactor) {
    return this.scaleFactor == scaleFactor
        ? this
        : new UnitImageFactory(resourceLoader, scaleFactor, mapData);
  }

  /** Return the width of scaled units. */
  public int getUnitImageWidth() {
    return (int) (scaleFactor * unitIconWidth);
  }

  /** Return the height of scaled units. */
  public int getUnitImageHeight() {
    return (int) Math.round(scaleFactor * unitIconHeight);
  }

  public int getUnitCounterOffsetWidth() {
    return (int) (scaleFactor * unitCounterOffsetWidth);
  }

  public int getUnitCounterOffsetHeight() {
    return (int) (scaleFactor * unitCounterOffsetHeight);
  }

  public boolean hasImage(final ImageKey imageKey) {
    return images.containsKey(imageKey) || getBaseImageUrl(imageKey).isPresent();
  }

  /**
   * Return the appropriate unit image. If an image cannot be found, a placeholder 'no-image' image
   * is returned.
   */
  public Image getImage(final ImageKey imageKey) {
    return Optional.ofNullable(images.get(imageKey))
        .or(
            () ->
                getTransformedImage(imageKey)
                    .map(
                        baseImage -> {
                          // We want to scale units according to the given scale factor.
                          // We use smooth scaling since the images are cached to allow to take our
                          // time in doing the scaling.
                          // Image observer is null, since the image should have been guaranteed to
                          // be loaded.
                          final int baseWidth = baseImage.getWidth(null);
                          final int baseHeight = baseImage.getHeight(null);
                          final int width = Math.max(1, (int) (baseWidth * scaleFactor));
                          final int height = Math.max(1, (int) (baseHeight * scaleFactor));
                          final Image scaledImage =
                              baseImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                          // Ensure the scaling is completed.
                          Util.ensureImageLoaded(scaledImage);
                          images.put(imageKey, scaledImage);
                          return scaledImage;
                        }))
        .orElseGet(
            () -> {
              BufferedImage image =
                  resourceLoader.getImageOrThrow(FILE_NAME_BASE + "missing_unit_image.png");
              Color playerColor = mapData.getPlayerColor(imageKey.getPlayer().getName());
              ImageTransformer.colorize(playerColor, image);

              Graphics graphics = image.getGraphics();
              Font font = graphics.getFont();
              graphics.setFont(font.deriveFont(8.0f));
              graphics.setColor(Color.LIGHT_GRAY);
              graphics.drawString(imageKey.getBaseImageName(), 5, 28);
              images.put(imageKey, image);
              return image;
            });
  }

  public Optional<URL> getBaseImageUrl(final ImageKey imageKey) {
    final String baseImageName = imageKey.getBaseImageName();
    final GamePlayer gamePlayer = imageKey.getPlayer();
    // URL uses '/' not '\'
    final String fileName = FILE_NAME_BASE + gamePlayer.getName() + "/" + baseImageName + ".png";
    final String fileName2 = FILE_NAME_BASE + baseImageName + ".png";
    final URL url = resourceLoader.getResource(fileName, fileName2);
    return Optional.ofNullable(url);
  }

  public Optional<URL> getPossiblyTransformedImageUrl(final ImageKey imageKey) {
    final Optional<URL> url = getBaseImageUrl(imageKey);
    if (url.isEmpty() || !shouldTransformImage(imageKey)) {
      return url;
    }
    return Optional.of(
        colorizedTempFiles.computeIfAbsent(
            imageKey,
            key -> {
              // The cast is safe because we use BufferedImage when transforming images.
              BufferedImage bufferedImage = (BufferedImage) loadImageAndTransform(url.get(), key);
              try {
                // Create a temp file that can be used in URLs. Note: JEditorPane doesn't support
                // base64-encoded data: URLs, so we need to actually have a file on disk. We use
                // a cache so that we don't create the same files multiple times.
                File file = Files.createTempFile(key.getFullName(), ".png").toFile();
                // Delete the file on exit.
                file.deleteOnExit();
                tempFiles.add(file);
                ImageIO.write(bufferedImage, "PNG", file);
                return file.toURI().toURL();
              } catch (IOException e) {
                log.error("Failed to create temp file: ", e);
              }
              // Return the non-colorized URL on error.
              return url.get();
            }));
  }

  private Optional<Image> getTransformedImage(final ImageKey imageKey) {
    return getBaseImageUrl(imageKey)
        .map(imageLocation -> loadImageAndTransform(imageLocation, imageKey));
  }

  private Image loadImageAndTransform(URL imageLocation, ImageKey imageKey) {
    Image image = Toolkit.getDefaultToolkit().getImage(imageLocation);
    Util.ensureImageLoaded(image);
    return transformImageIfNeeded(image, imageKey);
  }

  private Image transformImageIfNeeded(Image image, ImageKey imageKey) {
    if (!shouldTransformImage(imageKey)) {
      return image;
    }
    final String playerName = imageKey.getPlayer().getName();
    // Create an image copy so we don't modify the one returned by the toolkit which may be cached.
    image = createImageCopy(image);
    Optional<Color> unitColor = mapData.getUnitColor(playerName);
    if (unitColor.isPresent()) {
      final int brightness = mapData.getUnitBrightness(playerName);
      ImageTransformer.colorize(unitColor.get(), brightness, image);
    }
    if (mapData.shouldFlipUnit(playerName)) {
      ImageTransformer.flipHorizontally(image);
    }
    return image;
  }

  private boolean shouldTransformImage(ImageKey imageKey) {
    if (mapData.ignoreTransformingUnit(imageKey.getType().getName())) {
      return false;
    }
    final String playerName = imageKey.getPlayer().getName();
    return mapData.getUnitColor(playerName).isPresent() || mapData.shouldFlipUnit(playerName);
  }

  private static BufferedImage createImageCopy(Image image) {
    final var copy =
        new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
    Graphics g = copy.createGraphics();
    g.drawImage(image, 0, 0, null);
    g.dispose();
    return copy;
  }

  /**
   * Returns the highlight image for the specified unit.
   *
   * @return The highlight image or empty if no base image is available for the specified unit.
   */
  public Image getHighlightImage(final ImageKey imageKey) {
    Image image = getImage(imageKey);
    final BufferedImage highlightedImage =
        Util.newImage(image.getWidth(null), image.getHeight(null), true);
    // copy the real image
    final Graphics2D g = highlightedImage.createGraphics();
    g.drawImage(image, 0, 0, null);
    // we want a highlight only over the area that is not clear
    g.setComposite(AlphaComposite.SrcIn);
    g.setColor(new Color(240, 240, 240, 127));
    g.fillRect(0, 0, image.getWidth(null), image.getHeight(null));
    g.dispose();
    return highlightedImage;
  }

  /** Return an icon image for a unit. */
//  public ImageIcon getIcon(final ImageKey imageKey) {
//    final String fullName = imageKey.getFullName();
//    return icons.computeIfAbsent(fullName, key -> new ImageIcon(getImage(imageKey)));
//  }

  public ImageIcon getIcon(final ImageKey imageKey) {
    final String fullName = imageKey.getFullName();
    if (icons.containsKey(fullName)) {
      return icons.get(fullName);
    }
    final Optional<Image> image = getTransformedImage(imageKey);
    if (image.isEmpty()) {
      return new ImageIcon(getImage(imageKey));
    }

    final ImageIcon icon = new ImageIcon(image.get());
    icons.put(fullName, icon);
    return icon;
  }

  public Dimension getImageDimensions(final ImageKey imageKey) {
    final Image image = getImage(imageKey);
    final int width = (int) (image.getWidth(null) * scaleFactor);
    final int height = (int) (image.getHeight(null) * scaleFactor);
    return new Dimension(width, height);
  }
}
