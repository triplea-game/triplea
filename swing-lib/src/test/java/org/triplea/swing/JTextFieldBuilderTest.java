package org.triplea.swing;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JTextField;
import org.awaitility.Awaitility;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.Test;

class JTextFieldBuilderTest {

  @Test
  void defaultValues() {
    final JTextField field = JTextFieldBuilder.builder().build();

    MatcherAssert.assertThat(field.isEnabled(), Is.is(true));

    MatcherAssert.assertThat(field.getText(), Is.is(""));
  }

  @Test
  void text() {
    final String testValue = "test value";
    MatcherAssert.assertThat(
        JTextFieldBuilder.builder().text(testValue).build().getText(), Is.is(testValue));
  }

  @Test
  void textWithIntegerValue() {
    MatcherAssert.assertThat(JTextFieldBuilder.builder().text(2).build().getText(), Is.is("2"));
  }

  @Test
  void columns() {
    MatcherAssert.assertThat(JTextFieldBuilder.builder().columns(3).build().getColumns(), Is.is(3));
  }

  @Test
  void textListener() {
    // we will know we fired an action event if this value is incremented to 1
    final AtomicInteger value = new AtomicInteger(0);

    JTextFieldBuilder.builder()
        .textListener(fieldValue -> value.incrementAndGet())
        .build()
        .setText("text");

    // Callback is buffered, we need to wait long enough for the event to be scheduled and fired.
    // Eventually callback action is expected to have been called and incremented our value from 0
    // to 1.
    Awaitility.await()
        .atMost(Duration.ofMillis(DocumentListenerBuilder.CALLBACK_DELAY_MS * 2))
        .until(() -> value.get() == 1);
  }

  @Test
  void enabled() {
    MatcherAssert.assertThat(JTextFieldBuilder.builder().build().isEnabled(), Is.is(true));
  }

  @Test
  void readyOnly() {
    MatcherAssert.assertThat(
        JTextFieldBuilder.builder().readOnly().build().isEditable(), Is.is(false));
  }

  @Test
  void disabled() {
    MatcherAssert.assertThat(
        JTextFieldBuilder.builder().disabled().build().isEnabled(), Is.is(false));
  }
}
