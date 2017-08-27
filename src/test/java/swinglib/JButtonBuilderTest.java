package swinglib;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JButton;

import org.junit.Test;

public class JButtonBuilderTest {

  @Test
  public void title() {
    final String value = "testing title";
    final JButton button = JButtonBuilder.builder()
        .title(value)
        .actionListener(() -> {
        })
        .build();
    assertThat(button.getText(), is(value));
  }

  @Test
  public void checkActionListener() {
    // button action will be to add one to our integer, we'll fire the button action and verify we get the +1
    final AtomicInteger integer = new AtomicInteger(0);
    final JButton button = JButtonBuilder.builder()
        .title("title")
        .actionListener(integer::incrementAndGet)
        .toolTip("toolTip")
        .build();

    Arrays.stream(button.getActionListeners())
        .forEach(listener -> listener.actionPerformed(new ActionEvent(new Object(), 0, "")));
    assertThat(integer.get(), is(1));

    assertThat(button.getToolTipText(), is("toolTip"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void titleCannotBeEmpty() {
    JButtonBuilder.builder().title("");
  }

  @Test(expected = NullPointerException.class)
  public void titleIsRequired() {
    JButtonBuilder.builder()
        .actionListener(() -> {
        })
        .build();
  }

  @Test(expected = NullPointerException.class)
  public void actionListenerIsRequired() {
    JButtonBuilder.builder().actionListener(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void toolTipCanNotBeEmptyIfSpecified() {
    JButtonBuilder.builder().toolTip("");
  }
}
