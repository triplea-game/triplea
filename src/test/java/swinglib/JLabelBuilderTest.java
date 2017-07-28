package swinglib;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.Is;

import org.junit.Test;

public class JLabelBuilderTest {

  @Test
  public void text() {
    final String value = "some text";
    final JLabel label = JLabelBuilder.builder()
        .text(value)
        .build();
    MatcherAssert.assertThat(label.getText(), Is.is(value));
  }

  @Test
  public void leftAlign() {
    final JLabel label = JLabelBuilder.builder()
        .text("value")
        .leftAlign()
        .build();
    MatcherAssert.assertThat(label.getAlignmentX(), Is.is(JComponent.LEFT_ALIGNMENT));
  }

  @Test(expected = NullPointerException.class)
  public void textIsRequired() {
    JLabelBuilder.builder().build();
  }

  @Test
  public void maxSize() {
    final int maxWidth = 300;
    final int maxHeight = 500;
    final JLabel label = JLabelBuilder.builder()
        .text("testing")
        .maximumSize(maxWidth, maxHeight)
        .build();

    MatcherAssert.assertThat(label.getMaximumSize().width, Is.is(maxWidth));
    MatcherAssert.assertThat(label.getMaximumSize().height, Is.is(maxHeight));
  }
}
