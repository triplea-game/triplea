package org.triplea.swing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.border.Border;
import org.junit.jupiter.api.Test;

final class JScrollPaneBuilderTest {
  private final JScrollPaneBuilder builder = JScrollPaneBuilder.builder();

  @Test
  void buildShouldThrowExceptionWhenViewUnspecified() {
    final Exception e = assertThrows(IllegalStateException.class, builder::build);
    assertThat(e.getMessage(), containsString("view"));
  }

  @Test
  void buildShouldSetBorderWhenProvided() {
    final Border border = BorderFactory.createEmptyBorder();

    final JScrollPane scrollPane = builder.view(new JLabel()).border(border).build();

    assertThat(scrollPane.getBorder(), is(sameInstance(border)));
  }

  @Test
  void buildShouldSetView() {
    final Component view = new JLabel();

    final JScrollPane scrollPane = builder.view(view).build();

    assertThat(scrollPane.getViewport().getView(), is(sameInstance(view)));
  }
}
