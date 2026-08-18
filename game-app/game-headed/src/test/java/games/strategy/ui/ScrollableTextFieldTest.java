package games.strategy.ui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.formdev.flatlaf.FlatDarkLaf;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class ScrollableTextFieldTest {
  @BeforeAll
  static void installFlatLaf() throws Exception {
    SwingUtilities.invokeAndWait(FlatDarkLaf::setup);
  }

  @Test
  void stepperButtonsKeepOriginalGifIcons() throws Exception {
    SwingUtilities.invokeAndWait(
        () -> {
          final ScrollableTextField field = new ScrollableTextField(0, 1);
          final List<JButton> buttons = findButtons(field);
          assertThat(buttons, hasSize(4));
          for (final JButton button : buttons) {
            assertThat(button.getIcon(), instanceOf(ImageIcon.class));
            assertThat(button.getIcon().getIconWidth(), is(19));
            assertThat(button.getIcon().getIconHeight(), is(7));
            assertThat(button.getClientProperty("JButton.buttonType"), nullValue());
            assertThat(button.getDisabledIcon(), notNullValue());
          }
        });
  }

  @Test
  void disabledStepperArrowsStayVisibleOnFlatDarkLaf() throws Exception {
    final int[] painted = new int[1];
    SwingUtilities.invokeAndWait(
        () -> {
          final ScrollableTextField field = new ScrollableTextField(0, 0);
          final JButton up = findButtons(field).get(0);
          assertThat(up.isEnabled(), is(false));
          painted[0] = nonBackgroundPixelCount(paintDisabledIcon(up), up.getBackground());
        });

    assertThat(painted[0], greaterThan(0));
  }

  @Test
  void maxOfOneKeepsIncrementButtonsPainted() throws Exception {
    final int[] painted = new int[1];
    SwingUtilities.invokeAndWait(
        () -> {
          final ScrollableTextField field = new ScrollableTextField(0, 1);
          field.setValue(1);
          final JButton up = findButtons(field).get(0);
          assertThat(up.isEnabled(), is(false));
          painted[0] = nonBackgroundPixelCount(paintDisabledIcon(up), up.getBackground());
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

  private static BufferedImage paintDisabledIcon(final JButton button) {
    final Icon icon = button.getDisabledIcon();
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
