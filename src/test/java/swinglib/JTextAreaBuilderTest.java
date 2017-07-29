package swinglib;

import static org.hamcrest.MatcherAssert.assertThat;

import javax.swing.JTextArea;

import org.hamcrest.core.Is;

import org.junit.Test;

public class JTextAreaBuilderTest {

  @Test
  public void defaultValues() {
    final JTextArea area = JTextAreaBuilder.builder()
        .build();

    assertThat(area.getWrapStyleWord(), Is.is(true));
    assertThat(area.isEditable(), Is.is(true));
  }


  @Test
  public void text() {
    final JTextArea area = JTextAreaBuilder.builder()
        .text("value")
        .build();

    assertThat(area.getText(), Is.is("value"));
  }

  @Test
  public void rows() {
    final JTextArea area = JTextAreaBuilder.builder()
        .rows(5)
        .build();

    assertThat(area.getRows(), Is.is(5));
  }

  @Test(expected = IllegalArgumentException.class)
  public void rowsNonZero() {
    JTextAreaBuilder.builder()
        .rows(0)
        .build();
  }


  @Test
  public void columns() {
    final JTextArea area = JTextAreaBuilder.builder()
        .columns(20)
        .build();

    assertThat(area.getColumns(), Is.is(20));
  }

  @Test(expected = IllegalArgumentException.class)
  public void columnsNonZero() {
    JTextAreaBuilder.builder()
        .columns(0)
        .build();
  }

  @Test
  public void maximumSize() {
    final int maxWidth = 1000;
    final int maxHeight = 33;

    final JTextArea area = JTextAreaBuilder.builder()
        .maximumSize(maxWidth, maxHeight)
        .build();
    assertThat(area.getMaximumSize().width, Is.is(maxWidth));
    assertThat(area.getMaximumSize().height, Is.is(maxHeight));
  }

  @Test
  public void readOnly() {
    final JTextArea area = JTextAreaBuilder.builder()
        .readOnly()
        .build();
    assertThat(area.isEditable(), Is.is(false));
  }


}
