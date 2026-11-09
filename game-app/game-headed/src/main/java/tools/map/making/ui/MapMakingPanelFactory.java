package tools.map.making.ui;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.triplea.swing.JButtonBuilder;

public class MapMakingPanelFactory {
  private static final int SPACING_HEIGHT = 30;

  public record ButtonSpec(String labelText, Runnable runnable) {}

  public static JPanel get(String title, ButtonSpec... buttonSpecs) {
    final JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
    panel.add(Box.createVerticalStrut(SPACING_HEIGHT));
    panel.add(new JLabel(title));
    panel.add(Box.createVerticalStrut(SPACING_HEIGHT));
    for (ButtonSpec buttonSpec : buttonSpecs) {
      panel.add(
          new JButtonBuilder(buttonSpec.labelText).actionListener(buttonSpec.runnable).build());
      panel.add(Box.createVerticalStrut(SPACING_HEIGHT));
    }
    return panel;
  }
}
