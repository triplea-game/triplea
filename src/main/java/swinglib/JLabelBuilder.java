package swinglib;

import java.awt.Dimension;

import javax.swing.JLabel;

public class JLabelBuilder {

  private String text;
  private Alignment alignment;
  private Dimension maxSize;

  private JLabelBuilder() {}

  public static JLabelBuilder builder() {
    return new JLabelBuilder();
  }

  public JLabel build() {
    return new JLabel();
  }

  public JLabelBuilder leftAlign() {
    alignment = Alignment.LEFT;
    return this;
  }

  public JLabelBuilder text(final String text) {
    this.text = text;
    return this;
  }

  public JLabelBuilder maximumSize(final int width, final int height) {
    maxSize = new Dimension(width, height);
    return this;
  }

  private enum Alignment {
    LEFT
  }
}
