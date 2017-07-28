package swinglib;

import javax.swing.JTextArea;

public final class JTextAreaBuilder {


  private JTextAreaBuilder() {

  }

  public static JTextAreaBuilder builder() {
    return new JTextAreaBuilder();
  }

  public JTextArea build() {
    final JTextArea textArea = new JTextArea();


    /*
     * final JTextArea description = new JTextArea(setting.description, 2, 40);
     * description.setEditable(false);
     * description.setWrapStyleWord(true);
     *
     * description.setMaximumSize(new Dimension(120, 50));
     * description.setEditable(false);
     * description.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
     */
    textArea.setWrapStyleWord(true);
    textArea.setLineWrap(true);
    return textArea;
  }



  public JTextAreaBuilder readOnly() {
    return this;
  }

  public JTextAreaBuilder borderWidth(final int width) {

    return this;
  }

  public JTextAreaBuilder maximumSize(final int width, final int height) {

    return this;
  }

  public JTextAreaBuilder rows(final int value) {

    return this;
  }

  public JTextAreaBuilder columns(final int value) {
    return this;
  }
}
