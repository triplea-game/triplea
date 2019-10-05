package org.triplea.swing.jpanel;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JPanel;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class GridBagLayoutBuilder {
  private JPanelBuilder panelBuilder;

  private final List<GridBagComponent> components = new ArrayList<>();

  @AllArgsConstructor
  private static class GridBagComponent {
    private final JComponent component;
    private final GridBagConstraints constraints;
  }

  /**
   * Constructs a Swing JPanel using current builder values. Values that must be set: (requires no
   * values to be set)
   */
  public JPanel build() {
    final JPanel panel = panelBuilder.build();
    panel.setLayout(new GridBagLayout());
    components.forEach(c -> panel.add(c.component, c.constraints));
    return panel;
  }

  public GridBagLayoutBuilder add(
      final JComponent component, final GridBagConstraints constraints) {
    components.add(new GridBagComponent(component, constraints));
    return this;
  }
}
