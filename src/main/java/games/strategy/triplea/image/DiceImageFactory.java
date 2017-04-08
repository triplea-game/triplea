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
  public int DIE_WIDTH = 32;
  public int DIE_HEIGHT = 32;
  private final int m_diceSides;
  private final ResourceLoader m_resourceLoader;
  private static final Color s_ignored = new Color(100, 100, 100, 200);
  // maps Integer -> Image
  private final Map<Integer, Image> m_images = new HashMap<>();
  private final Map<Integer, Image> m_imagesHit = new HashMap<>();
  private final Map<Integer, Image> m_imagesIgnored = new HashMap<>();

  public DiceImageFactory(final ResourceLoader loader, final int diceSides) {
    final int PIP_SIZE = 6;
    m_diceSides = Math.max(6, diceSides);
    m_resourceLoader = loader;
    generateDice(PIP_SIZE, Color.black, m_images);
    generateDice(PIP_SIZE, Color.red, m_imagesHit);
    generateDice(PIP_SIZE, s_ignored, m_imagesIgnored);
  }

  private void generateDice(final int PIP_SIZE, final Color color, final Map<Integer, Image> images) {
    final ImageFactory iFactory = new ImageFactory();
    iFactory.setResourceLoader(m_resourceLoader);
    for (int i = 1; i <= m_diceSides; i++) {
      Image img = null;
      if (m_resourceLoader != null) {
        if (color == Color.black) {
          img = iFactory.getImage("dice/" + i + ".png", false);
        } else if (color == Color.red) {
          img = iFactory.getImage("dice/" + i + "_hit.png", false);
        } else if (color == s_ignored) {
          img = iFactory.getImage("dice/" + i + "_ignored.png", false);
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
        if (i == 1 || i == 3 || i == 5) {
          graphics.fillOval(DIE_WIDTH / 2 - (PIP_SIZE / 2), DIE_HEIGHT / 2 - (PIP_SIZE / 2), PIP_SIZE, PIP_SIZE);
        }
        // dots in top left and bottom right
        if (i == 3 || i == 5 || i == 4) {
          graphics.fillOval(DIE_WIDTH / 4 - (PIP_SIZE / 2), DIE_HEIGHT / 4 - (PIP_SIZE / 2), PIP_SIZE, PIP_SIZE);
          graphics.fillOval(3 * DIE_WIDTH / 4 - (PIP_SIZE / 2), 3 * DIE_HEIGHT / 4 - (PIP_SIZE / 2), PIP_SIZE,
              PIP_SIZE);
        }
        // dots in bottom left and top right
        if (i == 5 || i == 4) {
          graphics.fillOval(3 * DIE_WIDTH / 4 - (PIP_SIZE / 2), DIE_HEIGHT / 4 - (PIP_SIZE / 2), PIP_SIZE, PIP_SIZE);
          graphics.fillOval(DIE_WIDTH / 4 - (PIP_SIZE / 2), 3 * DIE_HEIGHT / 4 - (PIP_SIZE / 2), PIP_SIZE, PIP_SIZE);
        }
        // center two for 2
        if (i == 2 || i == 6) {
          graphics.fillOval(DIE_WIDTH / 3 - (PIP_SIZE / 2), DIE_HEIGHT / 2 - (PIP_SIZE / 2), PIP_SIZE, PIP_SIZE);
          graphics.fillOval(2 * DIE_WIDTH / 3 - (PIP_SIZE / 2), DIE_HEIGHT / 2 - (PIP_SIZE / 2), PIP_SIZE, PIP_SIZE);
        }
        if (i == 6) {
          graphics.fillOval(DIE_WIDTH / 3 - (PIP_SIZE / 2), DIE_HEIGHT / 4 - (PIP_SIZE / 2), PIP_SIZE, PIP_SIZE);
          graphics.fillOval(2 * DIE_WIDTH / 3 - (PIP_SIZE / 2), DIE_HEIGHT / 4 - (PIP_SIZE / 2), PIP_SIZE, PIP_SIZE);
          graphics.fillOval(DIE_WIDTH / 3 - (PIP_SIZE / 2), 3 * DIE_HEIGHT / 4 - (PIP_SIZE / 2), PIP_SIZE, PIP_SIZE);
          graphics.fillOval(2 * DIE_WIDTH / 3 - (PIP_SIZE / 2), 3 * DIE_HEIGHT / 4 - (PIP_SIZE / 2), PIP_SIZE,
              PIP_SIZE);
        }
        if (i > 6) {
          graphics.setFont(new Font("Arial", Font.BOLD, 16));
          ((Graphics2D) graphics).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
          final String number = Integer.toString(i);
          final int widthOffset = graphics.getFontMetrics().charsWidth(number.toCharArray(), 0, number.length());
          final int heightOffset = graphics.getFontMetrics().getHeight();
          graphics.drawString(number, (DIE_WIDTH - widthOffset) / 2, (DIE_HEIGHT + heightOffset) / 2 - 2);
        }
        images.put(i, canvas);
        graphics.dispose();
      }
    }
  }

  public Image getDieImage(final int i, final Die.DieType type) {
    if (i <= 0) {
      throw new IllegalArgumentException("die must be greater than 0, not:" + i);
    }
    if (i > m_diceSides) {
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
          graphics.setColor(s_ignored);
          break;
        default:
          throw new IllegalStateException("??");
      }
      graphics.drawRoundRect(1, 1, DIE_WIDTH - 3, DIE_HEIGHT - 3, 5, 5);
      ((Graphics2D) graphics).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      final String number = Integer.toString(i);
      final int widthOffset = graphics.getFontMetrics().charsWidth(number.toCharArray(), 0, number.length());
      final int heightOffset = graphics.getFontMetrics().getHeight();
      graphics.drawString(number, (DIE_WIDTH - widthOffset) / 2, (DIE_HEIGHT + heightOffset) / 2 - 2);
      return canvas;
    }
    switch (type) {
      case HIT:
        return m_imagesHit.get(i);
      case MISS:
        return m_images.get(i);
      case IGNORED:
        return m_imagesIgnored.get(i);
      default:
        throw new IllegalStateException("??");
    }
  }

  public Icon getDieIcon(final int i, final Die.DieType type) {
    return new ImageIcon(getDieImage(i, type));
  }

}
