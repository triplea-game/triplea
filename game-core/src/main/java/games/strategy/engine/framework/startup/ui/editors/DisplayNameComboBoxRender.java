package games.strategy.engine.framework.startup.ui.editors;

import java.awt.Component;
import java.util.Optional;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

/** Render to render IBeans in a combo box. */
class DisplayNameComboBoxRender extends DefaultListCellRenderer {
  private static final long serialVersionUID = -93452907043224502L;

  @Override
  public Component getListCellRendererComponent(
      final JList<?> list,
      final Object value,
      final int index,
      final boolean isSelected,
      final boolean cellHasFocus) {
    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    setText(
        Optional.ofNullable(value)
            .map(IBean.class::cast)
            .map(IBean::getDisplayName)
            .orElse("disabled"));
    return this;
  }
}
