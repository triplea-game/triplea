package games.strategy.ui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;

import com.formdev.flatlaf.FlatDarkLaf;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class ScrollableTextFieldTest {
  @BeforeAll
  static void installFlatLaf() throws Exception {
    SwingUtilities.invokeAndWait(FlatDarkLaf::setup);
  }

  @Test
  void disabledStepperArrowsFollowThemeForegroundNotGifBlack() throws Exception {
    final int[] distances = new int[2];
    SwingUtilities.invokeAndWait(
        () -> {
          final ScrollableTextField field = new ScrollableTextField(0, 0);
          final List<JButton> buttons = findButtons(field);
          assertThat(buttons, hasSize(4));
          final JButton up = buttons.get(0);
          assertThat(up.getPreferredSize().width, lessThan(72));
          final Color average = paintIconAverage(up);
          final Color expected =
              Optional.ofNullable(UIManager.getColor("Button.disabledText"))
                  .orElse(up.getForeground());
          distances[0] = colorDistance(average, expected);
          distances[1] = colorDistance(average, Color.BLACK);
        });

    assertThat(distances[0], lessThan(distances[1]));
  }

  @Test
  void maxOfOneKeepsIncrementButtonsPainted() throws Exception {
    final int[] painted = new int[1];
    SwingUtilities.invokeAndWait(
        () -> {
          final ScrollableTextField field = new ScrollableTextField(0, 1);
          final JButton up = findButtons(field).get(0);
          painted[0] = nonBackgroundPixelCount(up);
        });

    assertThat(painted[0], greaterThan(0));
  }

  private static List<JButton> findButtons(final Container root) {
    final List<JButton> buttons = new ArrayList<>();
    for (final Component child : root.getComponents()) {
      if (child instanceof JButton button) {
        buttons.add(button);
      } else if (child instanceof Container container) {
        buttons.addAll(findButtons(container));
      }
    }
    return buttons;
  }

  private static Color paintIconAverage(final JButton button) {
    final BufferedImage image = paintIcon(button);
    return averageNonBackground(image, button.getBackground());
  }

  private static int nonBackgroundPixelCount(final JButton button) {
    return nonBackgroundPixelCount(paintIcon(button), button.getBackground());
  }

  private static BufferedImage paintIcon(final JButton button) {
    final Icon icon = button.getIcon();
    assertThat(icon, notNullValue());
    final BufferedImage image =
        new BufferedImage(
            Math.max(1, icon.getIconWidth()),
            Math.max(1, icon.getIconHeight()),
            BufferedImage.TYPE_INT_RGB);
    final Graphics2D graphics = image.createGraphics();
    graphics.setColor(button.getBackground());
    graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
    icon.paintIcon(button, graphics, 0, 0);
    graphics.dispose();
    return image;
  }

  private static Color averageNonBackground(final BufferedImage image, final Color background) {
    long red = 0;
    long green = 0;
    long blue = 0;
    int count = 0;
    for (int x = 0; x < image.getWidth(); x++) {
      for (int y = 0; y < image.getHeight(); y++) {
        final Color pixel = new Color(image.getRGB(x, y));
        if (colorDistance(pixel, background) < 25) {
          continue;
        }
        red += pixel.getRed();
        green += pixel.getGreen();
        blue += pixel.getBlue();
        count++;
      }
    }
    if (count == 0) {
      return Color.BLACK;
    }
    return new Color((int) (red / count), (int) (green / count), (int) (blue / count));
  }

  private static int nonBackgroundPixelCount(final BufferedImage image, final Color background) {
    int count = 0;
    for (int x = 0; x < image.getWidth(); x++) {
      for (int y = 0; y < image.getHeight(); y++) {
        if (colorDistance(new Color(image.getRGB(x, y)), background) >= 25) {
          count++;
        }
      }
    }
    return count;
  }

  private static int colorDistance(final Color left, final Color right) {
    final int dRed = left.getRed() - right.getRed();
    final int dGreen = left.getGreen() - right.getGreen();
    final int dBlue = left.getBlue() - right.getBlue();
    return dRed * dRed + dGreen * dGreen + dBlue * dBlue;
  }
}
