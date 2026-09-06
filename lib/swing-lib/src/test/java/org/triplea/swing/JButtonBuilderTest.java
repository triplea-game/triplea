package org.triplea.swing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.util.concurrent.Runnables;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JButton;
import org.junit.jupiter.api.Test;

class JButtonBuilderTest {

  @Test
  void title() {
    final String value = "testing title";
    final JButton button =
        new JButtonBuilder().title(value).actionListener(Runnables.doNothing()).build();
    assertThat(button.getText()).isEqualTo(value);
  }

  @Test
  void checkActionListener() {
    // button action will be to add one to our integer, we'll fire the button action and verify we
    // get the +1
    final AtomicInteger integer = new AtomicInteger(0);
    final JButton button =
        new JButtonBuilder()
            .title("title")
            .actionListener(integer::incrementAndGet)
            .toolTip("toolTip")
            .build();

    Arrays.stream(button.getActionListeners())
        .forEach(listener -> listener.actionPerformed(new ActionEvent(new Object(), 0, "")));
    assertThat(integer.get()).isEqualTo(1);

    assertThat(button.getToolTipText()).isEqualTo("toolTip");
  }

  @Test
  void titleCannotBeEmpty() {
    assertThrows(IllegalArgumentException.class, () -> new JButtonBuilder().title(""));
  }

  @Test
  void titleIsRequired() {
    assertThrows(
        NullPointerException.class,
        () -> new JButtonBuilder().actionListener(Runnables.doNothing()).build());
  }

  @Test
  void actionListenerIsRequired() {
    assertThrows(
        NullPointerException.class, () -> new JButtonBuilder().actionListener((Runnable) null));
  }

  @Test
  void toolTipCanNotBeEmptyIfSpecified() {
    assertThrows(IllegalArgumentException.class, () -> new JButtonBuilder().toolTip(""));
  }
}
