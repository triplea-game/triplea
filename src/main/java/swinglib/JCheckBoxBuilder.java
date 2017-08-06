package swinglib;

import javax.swing.JCheckBox;

public class JCheckBoxBuilder {

  private JCheckBoxBuilder() {

  }

  public JCheckBox build() {
    return new JCheckBox();
  }

  public static JCheckBoxBuilder builder() {
    return new JCheckBoxBuilder();
  }

}
