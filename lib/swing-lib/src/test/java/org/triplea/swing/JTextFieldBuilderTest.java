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

    // Callback is buffered and fires after CALLBACK_DELAY_MS. The ceiling is a generous upper
    // bound, not a latency assertion: await returns as soon as the callback fires, so a large
    // timeout keeps the test robust under CPU contention (e.g. parallel test forks) without
    // slowing the passing case.
    Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> value.get() == 1);
  }

  @Test
  void textListenerWithMaxLength() {
    final AtomicReference<String> value = new AtomicReference<>();

    JTextFieldBuilder.builder().maxLength(20).textListener(value::set).build().setText("test");

    // Generous ceiling, not a latency assertion; see textListener() above.
    Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> "test".equals(value.get()));
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
