package swinglib;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JScrollPane;

import org.junit.jupiter.api.Test;

public final class JScrollPaneBuilderTest {
  private final JScrollPaneBuilder builder = JScrollPaneBuilder.builder();

  @Test
  public void view_ShouldThrowExceptionWhenViewIsNull() {
    assertThat(assertThrows(NullPointerException.class, () -> builder.view(null)).getMessage(), containsString("view"));
  }

  @Test
  public void build_ShouldThrowExceptionWhenViewUnspecified() {
    assertThat(assertThrows(IllegalStateException.class, () -> builder.build()).getMessage(), containsString("view"));
  }

  @Test
  public void build_ShouldSetView() {
    final Component view = new JLabel();

    final JScrollPane scrollPane = builder
        .view(view)
        .build();

    assertThat(scrollPane.getViewport().getView(), is(sameInstance(view)));
  }
}
