package swinglib;

import javax.swing.JTextField;

import com.google.common.base.Strings;

public class JTextFieldBuilder {

  private String text;
  private int columns;

  private JTextFieldBuilder() {

  }

  public JTextFieldBuilder text(final String value) {
    this.text = value;
    return this;
  }

  public JTextFieldBuilder text(final int value) {
    this.text = String.valueOf(value);
    return this;
  }

  public JTextFieldBuilder columns(final int columns) {
    this.columns = columns;
    return this;
  }

  public JTextField build() {
    final JTextField textField = new JTextField(Strings.nullToEmpty(this.text));
    if (columns > 0) {
      textField.setColumns(columns);
    }
    return textField;
  }


  public static JTextFieldBuilder builder() {
    return new JTextFieldBuilder();
  }
}
