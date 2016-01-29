package games.performance;


import java.awt.event.KeyEvent;
import javax.swing.JCheckBoxMenuItem;

public class EnablePerformanceLoggingCheckBox extends JCheckBoxMenuItem {
  private static final long serialVersionUID = -8622339162927764406L;

  public EnablePerformanceLoggingCheckBox() {
    super("Enable Performance Logging");
    setSelected(Perf.isEnabled());
    setMnemonic(KeyEvent.VK_L);
    addActionListener( e -> {
      handleCheckAction(super.isSelected());
    } );
  }

  private static void handleCheckAction(final boolean checked) {
    Perf.setEnabled(checked);
  }
}
