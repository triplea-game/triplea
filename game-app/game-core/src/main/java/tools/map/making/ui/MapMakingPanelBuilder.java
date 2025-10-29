package tools.map.making.ui;

import java.awt.Component;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.triplea.swing.JButtonBuilder;

public class MapMakingPanelBuilder {
  protected static final int SPACING_HEIGHT = 30;

  final JPanel panel = new JPanel();

  public record ButtonSpec(String labelText, Runnable runnable) {}

  public MapMakingPanelBuilder(String title) {
    panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
    addVerticalStructDefault();
    panel.add(new JLabel(title));
    addVerticalStructDefault();
  }

  public JPanel build() {
    return panel;
  }

  public MapMakingPanelBuilder add(Component component) {
    panel.add(component);
    return this;
  }

  public MapMakingPanelBuilder addButtons(List<ButtonSpec> buttonSpecs) {
    buttonSpecs.forEach(
        buttonSpec -> {
          panel.add(
              new JButtonBuilder(buttonSpec.labelText).actionListener(buttonSpec.runnable).build());
          addVerticalStructDefault();
        });
    return this;
  }

  public MapMakingPanelBuilder addVerticalStructDefault() {
    panel.add(Box.createVerticalStrut(SPACING_HEIGHT));
    return this;
  }
}
