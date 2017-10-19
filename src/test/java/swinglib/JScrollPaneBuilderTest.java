package swinglib;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static com.googlecode.catchexception.apis.CatchExceptionHamcrestMatchers.hasMessageThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JScrollPane;

import org.junit.jupiter.api.Test;

public final class JScrollPaneBuilderTest {
  private final JScrollPaneBuilder builder = JScrollPaneBuilder.builder();

  @Test
  public void view_ShouldThrowExceptionWhenViewIsNull() {
    catchException(() -> builder.view(null));

    assertThat(caughtException(), allOf(
        is(instanceOf(NullPointerException.class)),
        hasMessageThat(containsString("view"))));
  }

  @Test
  public void build_ShouldThrowExceptionWhenViewUnspecified() {
    catchException(() -> builder.build());

    assertThat(caughtException(), allOf(
        is(instanceOf(IllegalStateException.class)),
        hasMessageThat(containsString("view"))));
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
