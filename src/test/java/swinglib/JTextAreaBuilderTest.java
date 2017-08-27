package swinglib;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import javax.swing.JTextArea;

import org.junit.Test;

public class JTextAreaBuilderTest {

  @Test
  public void defaultValues() {
    final JTextArea area = JTextAreaBuilder.builder()
        .build();

    assertThat(area.getWrapStyleWord(), is(true));
    assertThat(area.isEditable(), is(true));
  }

  @Test
  public void text() {
    final JTextArea area = JTextAreaBuilder.builder()
        .text("value")
        .build();

    assertThat(area.getText(), is("value"));
  }

  @Test
  public void rows() {
    final JTextArea area = JTextAreaBuilder.builder()
        .rows(5)
        .build();

    assertThat(area.getRows(), is(5));
  }

  @Test(expected = IllegalArgumentException.class)
  public void rowsNonZero() {
    JTextAreaBuilder.builder().rows(0);
  }

  @Test
  public void columns() {
    final JTextArea area = JTextAreaBuilder.builder()
        .columns(20)
        .build();

    assertThat(area.getColumns(), is(20));
  }

  @Test(expected = IllegalArgumentException.class)
  public void columnsNonZero() {
    JTextAreaBuilder.builder().columns(0);
  }

  @Test
  public void readOnly() {
    final JTextArea area = JTextAreaBuilder.builder()
        .readOnly()
        .build();
    assertThat(area.isEditable(), is(false));
  }
}
