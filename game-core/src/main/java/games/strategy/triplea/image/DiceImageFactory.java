package games.strategy.triplea.image;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.delegate.Die;
import games.strategy.ui.Util;

/**
 * Utility for creating dice images.
 */
public class DiceImageFactory {
  public static final int DIE_WIDTH = 32;
  public static final int DIE_HEIGHT = 32;
  private final int diceSides;
  private final ResourceLoader resourceLoader;
  private static final Color IGNORED = new Color(100, 100, 100, 200);
  // maps Integer -> Image
  private final Map<Integer, Image> images = new HashMap<>();
  private final Map<Integer, Image> imagesHit = new HashMap<>();
  private final Map<Integer, Image> imagesIgnored = new HashMap<>();

  public DiceImageFactory(final ResourceLoader loader, final int diceSides) {
    this.diceSides = Math.max(6, diceSides);
    resourceLoader = loader;
    final int pipSize = 6;
    generateDice(pipSize, Color.black, images);
    generateDice(pipSize, Color.red, imagesHit);
    generateDice(pipSize, IGNORED, imagesIgnored);
  }

  private void generateDice(final int pipSize, final Color color, final Map<Integer, Image> images) {
    final ImageFactory imageFactory = new ImageFactory();
    imageFactory.setResourceLoader(resourceLoader);
    for (int i = 0; i <= diceSides; i++) {
      Image img = null;
      if (resourceLoader != null) {
        if (Color.BLACK.equals(color)) {
          img = imageFactory.getImage("dice/" + i + ".png", false);
        } else if (Color.RED.equals(color)) {
          img = imageFactory.getImage("dice/" + i + "_hit.png", false);
        } else if (IGNORED.equals(color)) {
          img = imageFactory.getImage("dice/" + i + "_ignored.png", false);
        }
      }
      if (img != null) {
        images.put(i, img);
      } else {
        final Image canvas = Util.createImage(DIE_WIDTH, DIE_HEIGHT, true);
        final Graphics graphics = canvas.getGraphics();
        graphics.setColor(color);
        graphics.drawRoundRect(1, 1, DIE_WIDTH - 3, DIE_HEIGHT - 3, 5, 5);
        ((Graphics2D) graphics).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // center dot
        if ((i == 1) || (i == 3) || (i == 5)) {
          graphics.fillOval((DIE_WIDTH / 2) - (pipSize / 2), (DIE_HEIGHT / 2) - (pipSize / 2), pipSize, pipSize);
        }
        // dots in top left and bottom right
        if ((i == 3) || (i == 5) || (i == 4)) {
          graphics.fillOval((DIE_WIDTH / 4) - (pipSize / 2), (DIE_HEIGHT / 4) - (pipSize / 2), pipSize, pipSize);
          graphics.fillOval(((3 * DIE_WIDTH) / 4) - (pipSize / 2), ((3 * DIE_HEIGHT) / 4) - (pipSize / 2), pipSize,
              pipSize);
        }
        // dots in bottom left and top right
        if ((i == 5) || (i == 4)) {
          graphics.fillOval(((3 * DIE_WIDTH) / 4) - (pipSize / 2), (DIE_HEIGHT / 4) - (pipSize / 2), pipSize, pipSize);
          graphics.fillOval((DIE_WIDTH / 4) - (pipSize / 2), ((3 * DIE_HEIGHT) / 4) - (pipSize / 2), pipSize, pipSize);
        }
        // center two for 2
        if ((i == 2) || (i == 6)) {
          graphics.fillOval((DIE_WIDTH / 3) - (pipSize / 2), (DIE_HEIGHT / 2) - (pipSize / 2), pipSize, pipSize);
          graphics.fillOval(((2 * DIE_WIDTH) / 3) - (pipSize / 2), (DIE_HEIGHT / 2) - (pipSize / 2), pipSize, pipSize);
        }
        if (i == 6) {
          graphics.fillOval((DIE_WIDTH / 3) - (pipSize / 2), (DIE_HEIGHT / 4) - (pipSize / 2), pipSize, pipSize);
          graphics.fillOval(((2 * DIE_WIDTH) / 3) - (pipSize / 2), (DIE_HEIGHT / 4) - (pipSize / 2), pipSize, pipSize);
          graphics.fillOval((DIE_WIDTH / 3) - (pipSize / 2), ((3 * DIE_HEIGHT) / 4) - (pipSize / 2), pipSize, pipSize);
          graphics.fillOval(((2 * DIE_WIDTH) / 3) - (pipSize / 2), ((3 * DIE_HEIGHT) / 4) - (pipSize / 2), pipSize,
              pipSize);
        }
        if (i > 6) {
          graphics.setFont(new Font("Arial", Font.BOLD, 16));
          ((Graphics2D) graphics).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
          final String number = Integer.toString(i);
          final int widthOffset = graphics.getFontMetrics().charsWidth(number.toCharArray(), 0, number.length());
          final int heightOffset = graphics.getFontMetrics().getHeight();
          graphics.drawString(number, (DIE_WIDTH - widthOffset) / 2, ((DIE_HEIGHT + heightOffset) / 2) - 2);
        }
        images.put(i, canvas);
        graphics.dispose();
      }
    }
  }

  private Image getDieImage(final int i, final Die.DieType type) {
    if (i < 0) {
      throw new IllegalArgumentException("Die can't be less than 0, value: " + i);
    }
    if (i > diceSides) {
      final Image canvas = Util.createImage(DIE_WIDTH, DIE_HEIGHT, true);
      final Graphics graphics = canvas.getGraphics();
      graphics.setFont(new Font("Arial", Font.BOLD, 16));
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
          throw new IllegalStateException("??");
      }
      graphics.drawRoundRect(1, 1, DIE_WIDTH - 3, DIE_HEIGHT - 3, 5, 5);
      ((Graphics2D) graphics).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      final String number = Integer.toString(i);
      final int widthOffset = graphics.getFontMetrics().charsWidth(number.toCharArray(), 0, number.length());
      final int heightOffset = graphics.getFontMetrics().getHeight();
      graphics.drawString(number, (DIE_WIDTH - widthOffset) / 2, ((DIE_HEIGHT + heightOffset) / 2) - 2);
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
        throw new IllegalStateException("??");
    }
  }

  public Icon getDieIcon(final int i, final Die.DieType type) {
    return new ImageIcon(getDieImage(i, type));
  }

}
