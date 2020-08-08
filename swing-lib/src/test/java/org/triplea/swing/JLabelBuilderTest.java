package org.triplea.swing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import javax.swing.JComponent;
import javax.swing.JLabel;
import org.junit.jupiter.api.Test;

class JLabelBuilderTest {

  @Test
  void text() {
    final String value = "some text";
    final JLabel label = JLabelBuilder.builder().text(value).build();
    assertThat(label.getText(), is(value));
  }

  @Test
  void leftAlign() {
    final JLabel label = JLabelBuilder.builder().text("value").leftAlign().build();
    assertThat(label.getAlignmentX(), is(JComponent.LEFT_ALIGNMENT));
  }

  @Test
  void iconTextGap() {
    final int value = 42;
    final JLabel label = JLabelBuilder.builder().text("value").iconTextGap(value).build();
    assertThat(label.getIconTextGap(), is(value));
  }

  @Test
  void maxSize() {
    final int maxWidth = 300;
    final int maxHeight = 500;
    final JLabel label =
        JLabelBuilder.builder().text("testing").maximumSize(maxWidth, maxHeight).build();

    assertThat(label.getMaximumSize().width, is(maxWidth));
    assertThat(label.getMaximumSize().height, is(maxHeight));
  }
}
