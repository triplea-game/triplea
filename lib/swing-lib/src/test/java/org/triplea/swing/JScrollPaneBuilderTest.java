package org.triplea.swing;

import static org.assertj.core.api.Assertions.assertThat;

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

    assertThat(scrollPane.getBorder()).isSameAs(border);
  }

  @Test
  void maxSize() {
    final JScrollPane scrollPane = builder.maxSize(100, 200).build();

    assertThat(scrollPane.getMaximumSize().width).isEqualTo(100);
    assertThat(scrollPane.getMaximumSize().height).isEqualTo(200);
  }

  @Test
  void preferredSize() {
    final JScrollPane scrollPane = builder.preferredSize(300, 500).build();

    assertThat(scrollPane.getPreferredSize().width).isEqualTo(300);
    assertThat(scrollPane.getPreferredSize().height).isEqualTo(500);
  }
}
