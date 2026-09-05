package org.triplea.swing;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JTextField;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

class JTextFieldBuilderTest {

  @Test
  void defaultValues() {
    final JTextField field = JTextFieldBuilder.builder().build();

    assertThat(field.isEnabled()).isTrue();

    assertThat(field.getText()).isEqualTo("");
  }

  @Test
  void text() {
    final String testValue = "test value";
    assertThat(JTextFieldBuilder.builder().text(testValue).build().getText()).isEqualTo(testValue);
  }

  @Test
  void textWithIntegerValue() {
    assertThat(JTextFieldBuilder.builder().text(2).build().getText()).isEqualTo("2");
  }

  @Test
  void columns() {
    assertThat(JTextFieldBuilder.builder().columns(3).build().getColumns()).isEqualTo(3);
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
  void textListenerWithMaxLength() {
    final AtomicReference<String> value = new AtomicReference<>();

    JTextFieldBuilder.builder().maxLength(20).textListener(value::set).build().setText("test");

    Awaitility.await()
        .atMost(Duration.ofMillis(DocumentListenerBuilder.CALLBACK_DELAY_MS * 2))
        .until(() -> "test".equals(value.get()));
  }

  @Test
  void enabled() {
    assertThat(JTextFieldBuilder.builder().build().isEnabled()).isTrue();
  }

  @Test
  void readyOnly() {
    assertThat(JTextFieldBuilder.builder().readOnly().build().isEditable()).isFalse();
  }

  @Test
  void disabled() {
    assertThat(JTextFieldBuilder.builder().disabled().build().isEnabled()).isFalse();
  }
}
