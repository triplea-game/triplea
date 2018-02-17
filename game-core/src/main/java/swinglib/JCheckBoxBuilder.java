package swinglib;

import javax.swing.JCheckBox;

/**
 * Relatively simple builder for a swing JCheckBox.
 * By default check boxes are 'selected'.
 */
public class JCheckBoxBuilder {

  private boolean isSelected = true;

  private JCheckBoxBuilder() {

  }

  /**
   * Builds the swing component.
   */
  public JCheckBox build() {
    final JCheckBox checkBox = new JCheckBox();
    checkBox.setSelected(isSelected);
    return checkBox;
  }

  public JCheckBoxBuilder selected(final boolean isSelected) {
    this.isSelected = isSelected;
    return this;
  }

  public static JCheckBoxBuilder builder() {
    return new JCheckBoxBuilder();
  }

}
