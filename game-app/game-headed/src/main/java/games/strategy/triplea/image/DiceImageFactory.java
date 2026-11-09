package games.strategy.triplea.image;

import com.google.common.collect.ImmutableMap;
import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.delegate.Die;
import games.strategy.ui.Util;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/** Utility for creating dice images. */
public class DiceImageFactory {
  public static final int DIE_WIDTH = 32;
  public static final int DIE_HEIGHT = 32;
  private static final Color IGNORED = new Color(100, 100, 100, 200);

  private final int diceSides;
  private final ResourceLoader resourceLoader;
  private final Map<Integer, Image> images;
  private final Map<Integer, Image> imagesHit;
  private final Map<Integer, Image> imagesIgnored;

  public DiceImageFactory(final ResourceLoader loader, final int diceSides) {
    this.diceSides = Math.max(6, diceSides);
    resourceLoader = loader;
    images = generateDice(Color.BLACK);
    imagesHit = generateDice(Color.RED);
    imagesIgnored = generateDice(IGNORED);
  }

  private Map<Integer, Image> generateDice(final Color color) {
    final ImageFactory imageFactory = new ImageFactory();
    imageFactory.setResourceLoader(resourceLoader);
    return IntStream.rangeClosed(0, diceSides)
        .parallel()
        .boxed()
        .collect(
            Collectors.toMap(
                Function.identity(),
                side -> getImageOrFallback(resourceLoader, imageFactory, color, side)));
  }

  private static Image getImageOrFallback(
      @Nullable final ResourceLoader loader,
      final ImageFactory factory,
      final Color color,
      final int dieSide) {
    if (loader != null) {
      final Image image = factory.getImage(getDiceResourceName(color, dieSide)).orElse(null);
      if (image != null) {
        return image;
      }
    }
    return drawFallbackDie(dieSide, color);
  }

  private static String getDiceResourceName(final Color color, final int dieSide) {
    final String suffix =
        ImmutableMap.<Color, String>builder()
            .put(Color.BLACK, "")
            .put(Color.RED, "_hit")
            .put(IGNORED, "_ignored")
            .build()
            .get(color);
    return "dice/" + dieSide + Objects.requireNonNull(suffix) + ".png";
  }

  private static Image drawFallbackDie(final int dieSide, final Color color) {
    final int pipSize = 6;
    final Image canvas = Util.newImage(DIE_WIDTH, DIE_HEIGHT, true);
    final Graphics2D graphics = (Graphics2D) canvas.getGraphics();
    graphics.setColor(color);
    graphics.drawRoundRect(1, 1, DIE_WIDTH - 3, DIE_HEIGHT - 3, 5, 5);
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    // center dot
    if (dieSide == 1 || dieSide == 3 || dieSide == 5) {
      graphics.fillOval(
          DIE_WIDTH / 2 - (pipSize / 2), DIE_HEIGHT / 2 - (pipSize / 2), pipSize, pipSize);
    }
    // dots in top left and bottom right
    if (dieSide == 3 || dieSide == 5 || dieSide == 4) {
      graphics.fillOval(
          DIE_WIDTH / 4 - (pipSize / 2), DIE_HEIGHT / 4 - (pipSize / 2), pipSize, pipSize);
      graphics.fillOval(
          3 * DIE_WIDTH / 4 - (pipSize / 2), 3 * DIE_HEIGHT / 4 - (pipSize / 2), pipSize, pipSize);
    }
    // dots in bottom left and top right
    if (dieSide == 5 || dieSide == 4) {
      graphics.fillOval(
          3 * DIE_WIDTH / 4 - (pipSize / 2), DIE_HEIGHT / 4 - (pipSize / 2), pipSize, pipSize);
      graphics.fillOval(
          DIE_WIDTH / 4 - (pipSize / 2), 3 * DIE_HEIGHT / 4 - (pipSize / 2), pipSize, pipSize);
    }
    // center two for 2
    if (dieSide == 2 || dieSide == 6) {
      graphics.fillOval(
          DIE_WIDTH / 3 - (pipSize / 2), DIE_HEIGHT / 2 - (pipSize / 2), pipSize, pipSize);
      graphics.fillOval(
          2 * DIE_WIDTH / 3 - (pipSize / 2), DIE_HEIGHT / 2 - (pipSize / 2), pipSize, pipSize);
    }
    if (dieSide == 6) {
      graphics.fillOval(
          DIE_WIDTH / 3 - (pipSize / 2), DIE_HEIGHT / 4 - (pipSize / 2), pipSize, pipSize);
      graphics.fillOval(
          2 * DIE_WIDTH / 3 - (pipSize / 2), DIE_HEIGHT / 4 - (pipSize / 2), pipSize, pipSize);
      graphics.fillOval(
          DIE_WIDTH / 3 - (pipSize / 2), 3 * DIE_HEIGHT / 4 - (pipSize / 2), pipSize, pipSize);
      graphics.fillOval(
          2 * DIE_WIDTH / 3 - (pipSize / 2), 3 * DIE_HEIGHT / 4 - (pipSize / 2), pipSize, pipSize);
    }
    if (dieSide > 6) {
      graphics.setFont(new Font(MapImage.FONT_FAMILY_DEFAULT, Font.BOLD, 16));
      graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      final String number = Integer.toString(dieSide);
      final int widthOffset =
          graphics.getFontMetrics().charsWidth(number.toCharArray(), 0, number.length());
      final int heightOffset = graphics.getFontMetrics().getHeight();
      graphics.drawString(
          number, (DIE_WIDTH - widthOffset) / 2, (DIE_HEIGHT + heightOffset) / 2 - 2);
    }
    graphics.dispose();
    return canvas;
  }

  private Image getDieImage(final int i, final Die.DieType type) {
    if (i < 0) {
      throw new IllegalArgumentException("Die can't be less than 0, value: " + i);
    }
    if (i > diceSides) {
      final Image canvas = Util.newImage(DIE_WIDTH, DIE_HEIGHT, true);
      final Graphics graphics = canvas.getGraphics();
      graphics.setFont(new Font(MapImage.FONT_FAMILY_DEFAULT, Font.BOLD, 16));
      switch (type) {
        case HIT:
          graphics.setColor(Color.RED);
          break;
        case MISS:
          graphics.setColor(Color.BLACK);
          break;
        case IGNORED:
          graphics.setColor(IGNORED);
          break;
        default:
          throw new AssertionError("There's no die type '" + type + "'");
      }
      graphics.drawRoundRect(1, 1, DIE_WIDTH - 3, DIE_HEIGHT - 3, 5, 5);
      ((Graphics2D) graphics)
          .setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      final String number = Integer.toString(i);
      final int widthOffset =
          graphics.getFontMetrics().charsWidth(number.toCharArray(), 0, number.length());
      final int heightOffset = graphics.getFontMetrics().getHeight();
      graphics.drawString(
          number, (DIE_WIDTH - widthOffset) / 2, (DIE_HEIGHT + heightOffset) / 2 - 2);
      return canvas;
    }
    switch (type) {
      case HIT:
        return imagesHit.get(i);
      case MISS:
        return images.get(i);
      case IGNORED:
        return imagesIgnored.get(i);
      default:
        throw new AssertionError("There's no die type '" + type + "'");
    }
  }

  public Icon getDieIcon(final int i, final Die.DieType type) {
    return new ImageIcon(getDieImage(i, type));
  }
}
