package games.strategy.ui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.util.ArrayList;
import java.util.List;
import javax.swing.GrayFilter;
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
  void disabledStepperIconsAreBakedInsteadOfLafFiltered() throws Exception {
    SwingUtilities.invokeAndWait(
        () -> {
          final ScrollableTextField zeroMax = new ScrollableTextField(0, 0);
          assertThat(findButtons(zeroMax).stream().noneMatch(JButton::isEnabled), is(true));
          assertDisabledIconsAreBaked(zeroMax);

          final ScrollableTextField maxOfOne = new ScrollableTextField(0, 1);
          maxOfOne.setValue(1);
          final List<JButton> maxOfOneButtons = findButtons(maxOfOne);
          assertThat(maxOfOneButtons.get(0).isEnabled(), is(false));
          assertThat(maxOfOneButtons.get(2).isEnabled(), is(false));
          assertDisabledIconsAreBaked(maxOfOne);
        });
  }

  @Test
  void disabledGrayFilterPercentFollowsFlatLafTheme() throws Exception {
    final int[] dark = new int[1];
    final int[] light = new int[1];
    final int[] expectedDark = new int[1];
    final int[] expectedLight = new int[1];
    SwingUtilities.invokeAndWait(
        () -> {
          FlatDarkLaf.setup();
          dark[0] = firstOpaqueRgb(disabledImage(new ScrollableTextField(0, 0)));
          expectedDark[0] = firstOpaqueRgb(grayed(enabledImage(new ScrollableTextField(0, 0)), 34));
          FlatLightLaf.setup();
          light[0] = firstOpaqueRgb(disabledImage(new ScrollableTextField(0, 0)));
          expectedLight[0] =
              firstOpaqueRgb(grayed(enabledImage(new ScrollableTextField(0, 0)), 58));
          FlatDarkLaf.setup();
        });
    assertThat(dark[0], is(expectedDark[0]));
    assertThat(light[0], is(expectedLight[0]));
  }

  private static void assertDisabledIconsAreBaked(final ScrollableTextField field) {
    final List<JButton> buttons = findButtons(field);
    assertThat(buttons, hasSize(4));
    for (final JButton button : buttons) {
      final Icon disabled = button.getDisabledIcon();
      assertThat(disabled, instanceOf(ImageIcon.class));
      final Image image = ((ImageIcon) disabled).getImage();
      assertThat(image, instanceOf(BufferedImage.class));
      assertThat(image.getSource(), not(instanceOf(FilteredImageSource.class)));
    }
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

  private static BufferedImage enabledImage(final ScrollableTextField field) {
    return copy(((ImageIcon) findButtons(field).get(0).getIcon()).getImage());
  }

  private static BufferedImage disabledImage(final ScrollableTextField field) {
    return copy(((ImageIcon) findButtons(field).get(0).getDisabledIcon()).getImage());
  }

  private static BufferedImage grayed(final BufferedImage src, final int percent) {
    final GrayFilter filter = new GrayFilter(true, percent);
    final BufferedImage dst =
        new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
    for (int y = 0; y < src.getHeight(); y++) {
      for (int x = 0; x < src.getWidth(); x++) {
        dst.setRGB(x, y, filter.filterRGB(x, y, src.getRGB(x, y)));
      }
    }
    return dst;
  }

  private static BufferedImage copy(final Image image) {
    final int width = Math.max(1, image.getWidth(null));
    final int height = Math.max(1, image.getHeight(null));
    final BufferedImage copy = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D graphics = copy.createGraphics();
    graphics.drawImage(image, 0, 0, null);
    graphics.dispose();
    return copy;
  }

  private static int firstOpaqueRgb(final BufferedImage image) {
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        final int argb = image.getRGB(x, y);
        if (((argb >>> 24) & 0xff) != 0) {
          return argb & 0x00ffffff;
        }
      }
    }
    throw new AssertionError("disabled icon had no opaque pixels");
  }
}
