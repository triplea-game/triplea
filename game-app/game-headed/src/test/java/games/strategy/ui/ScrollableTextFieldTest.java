package games.strategy.ui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.formdev.flatlaf.FlatDarkLaf;
import java.awt.Component;
import java.awt.Container;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
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
}
