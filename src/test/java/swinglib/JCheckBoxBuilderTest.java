package swinglib;

import javax.swing.JCheckBox;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.Test;

public class JCheckBoxBuilderTest {
  @Test
  public void build() {
    final JCheckBox box = JCheckBoxBuilder.builder()
        .build();

    MatcherAssert.assertThat(box.isEnabled(), Is.is(true));
    MatcherAssert.assertThat(box.isSelected(), Is.is(true));
  }

  @Test
  public void selected() {
    MatcherAssert.assertThat(
        JCheckBoxBuilder.builder()
            .selected(false)
            .build()
            .isSelected(),
        Is.is(false));

    MatcherAssert.assertThat(
        JCheckBoxBuilder.builder()
            .selected(true)
            .build()
            .isSelected(),
        Is.is(true));
  }
}
