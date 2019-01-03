package swinglib;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;

import java.awt.Dimension;
import java.awt.Point;

import javax.swing.JFrame;
import javax.swing.JLabel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.test.common.swing.DisabledInHeadlessGraphicsEnvironment;
import org.triplea.test.common.swing.SwingComponentWrapper;

@ExtendWith(DisabledInHeadlessGraphicsEnvironment.class)
class JFrameBuilderTest {
  private static final String TITLE = "A falsis, finis secundus quadra.";
  private static final int WIDTH = 100;
  private static final int HEIGHT = 1000;

  @Test
  void title() {
    final JFrame frame = JFrameBuilder.builder().title(TITLE).build();

    assertThat(frame.getTitle(), is(TITLE));
  }

  @Test
  void minSizeIsSetByDefault() {
    final Dimension minSize = JFrameBuilder.builder().build().getMinimumSize();

    assertThat(minSize.width > 0, is(true));
    assertThat(minSize.height > 0, is(true));
  }

  @Test
  void minSize() {
    final Dimension minSize =
        JFrameBuilder.builder().minSize(WIDTH, HEIGHT).build().getMinimumSize();

    assertThat(minSize.width, is(WIDTH));
    assertThat(minSize.height, is(HEIGHT));
  }

  @Test
  void alwaysOnTop() {
    assertThat(JFrameBuilder.builder().alwaysOnTop().build().isAlwaysOnTop(), is(true));

    assertThat(JFrameBuilder.builder().build().isAlwaysOnTop(), is(false));
  }

  @Test
  void locationRelativeTo() {
    final JLabel label = new JLabel();
    label.setLocation(new Point(10, 10));

    final Point defaultLocation =
        JFrameBuilder.builder().locateRelativeTo(label).build().getLocation();

    final Point relativeLocation = JFrameBuilder.builder().build().getLocation();

    assertThat(
        "location should change when setting position relative to another component",
        defaultLocation,
        not(equalTo(relativeLocation)));
  }

  @Test
  void addComponents() {
    final String name = "Consiliums ridetis!";
    final JLabel component = new JLabel();
    component.setName(name);

    final JFrame frame = JFrameBuilder.builder().add(component).build();

    SwingComponentWrapper.of(frame).assertHasComponentByName(name);
  }
}
