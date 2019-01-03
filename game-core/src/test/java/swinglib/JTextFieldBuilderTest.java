package swinglib;

import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JTextField;

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
  void actionListener() {
    // we will know we fired an action event if this value is incremented to 1
    final AtomicInteger value = new AtomicInteger(0);

    JTextFieldBuilder.builder()
        .actionListener(fieldValue -> value.incrementAndGet())
        .build()
        // and then fire the action!
        .getActionListeners()[0]
        .actionPerformed(null);
    MatcherAssert.assertThat(
        "action expected to have been called and incremented our value from 0 to 1",
        value.get(),
        Is.is(1));
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
