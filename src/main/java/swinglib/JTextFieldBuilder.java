package swinglib;

import javax.swing.JTextField;

public final class JTextFieldBuilder {

  private JTextFieldBuilder() {

  }

  public static JTextFieldBuilder builder() {
    return new JTextFieldBuilder();
  }

  private JTextField build() {
    return new JTextField();
  }
}
