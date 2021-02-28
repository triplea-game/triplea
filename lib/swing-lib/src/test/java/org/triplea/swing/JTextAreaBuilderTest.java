package org.triplea.swing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.swing.JTextArea;
import org.junit.jupiter.api.Test;

class JTextAreaBuilderTest {

  @Test
  void defaultValues() {
    final JTextArea area = JTextAreaBuilder.builder().build();

    assertThat(area.getWrapStyleWord(), is(true));
    assertThat(area.isEditable(), is(true));
  }

  @Test
  void text() {
    final JTextArea area = JTextAreaBuilder.builder().text("value").build();

    assertThat(area.getText(), is("value"));
  }

  @Test
  void rows() {
    final JTextArea area = JTextAreaBuilder.builder().rows(5).build();

    assertThat(area.getRows(), is(5));
  }

  @Test
  void rowsNonZero() {
    assertThrows(IllegalArgumentException.class, () -> JTextAreaBuilder.builder().rows(0));
  }

  @Test
  void columns() {
    final JTextArea area = JTextAreaBuilder.builder().columns(20).build();

    assertThat(area.getColumns(), is(20));
  }

  @Test
  void columnsNonZero() {
    assertThrows(IllegalArgumentException.class, () -> JTextAreaBuilder.builder().columns(0));
  }

  @Test
  void readOnly() {
    final JTextArea area = JTextAreaBuilder.builder().readOnly().build();
    assertThat(area.isEditable(), is(false));
  }
}
