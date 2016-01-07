package games.performance;


import java.awt.event.KeyEvent;
import javax.swing.JCheckBoxMenuItem;

public class EnablePerformanceLoggingCheckBox extends JCheckBoxMenuItem {
  private static final long serialVersionUID = -8622339162927764406L;

  public EnablePerformanceLoggingCheckBox() {
    super("Enable Performance Logging");
    setSelected(PerformanceLogger.isEnabled());
    setMnemonic(KeyEvent.VK_P);
    addActionListener( e -> {
      handleCheckAction(super.isSelected());
    } );
  }

  private static void handleCheckAction(final boolean checked) {
    PerformanceLogger.setEnabled(checked);
  }
}
