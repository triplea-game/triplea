package games.strategy.performance;


import javafx.scene.control.CheckMenuItem;

public class EnablePerformanceLoggingCheckBox extends CheckMenuItem {
  public EnablePerformanceLoggingCheckBox() {
    super("Enable Performance _Logging");
    setSelected(PerfTimer.isEnabled());
    setMnemonicParsing(true);
    setOnAction(e -> handleCheckAction(super.isSelected()));
  }

  private static void handleCheckAction(final boolean checked) {
    PerfTimer.setEnabled(checked);
  }
}
