package swinglib;

import static org.hamcrest.MatcherAssert.assertThat;

import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JButton;

import org.hamcrest.core.Is;

import org.junit.Test;

public class JButtonBuilderTest {

  @Test
  public void testBuild() {
    // button action will be to add one to our integer, we'll fire the button action and verify we get the +1
    final AtomicInteger integer = new AtomicInteger(0);
    final JButton button = JButtonBuilder.builder()
        .title("title")
        .actionListener(integer::incrementAndGet)
        .toolTip("toolTip")
        .build();

    assertThat(button.getText(), Is.is("title"));

    Arrays.stream(button.getActionListeners())
        .forEach(listener -> listener.actionPerformed(new ActionEvent(new Object(), 0, "")));
    assertThat(integer.get(), Is.is(1));

    assertThat(button.getToolTipText(), Is.is("toolTip"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void titleIsRequired() {
    JButtonBuilder.builder()
        .title("")
        .actionListener(() -> {
        })
        .build();
  }

  @Test(expected = NullPointerException.class)
  public void actionIsRequired() {
    JButtonBuilder.builder()
        .title("title")
        .actionListener(null)
        .build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void toolTipCannotBeEmpty() {
    JButtonBuilder.builder()
        .title("title")
        .toolTip("")
        .actionListener(() -> {
        })
        .build();
  }
}
