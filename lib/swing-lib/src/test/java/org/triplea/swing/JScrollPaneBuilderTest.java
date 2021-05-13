package org.triplea.swing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.border.Border;
import org.junit.jupiter.api.Test;

final class JScrollPaneBuilderTest {
  private final JScrollPaneBuilder builder = new JScrollPaneBuilder(new JLabel());

  @Test
  void buildShouldSetBorderWhenProvided() {
    final Border border = BorderFactory.createEmptyBorder();

    final JScrollPane scrollPane = builder.border(border).build();

    assertThat(scrollPane.getBorder(), is(sameInstance(border)));
  }

  @Test
  void maxSize() {
    final JScrollPane scrollPane = builder.maxSize(100, 200).build();

    assertThat(scrollPane.getMaximumSize().width, is(100));
    assertThat(scrollPane.getMaximumSize().height, is(200));
  }

  @Test
  void preferredSize() {
    final JScrollPane scrollPane = builder.preferredSize(300, 500).build();

    assertThat(scrollPane.getPreferredSize().width, is(300));
    assertThat(scrollPane.getPreferredSize().height, is(500));
  }
}
