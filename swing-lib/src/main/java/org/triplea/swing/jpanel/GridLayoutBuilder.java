package org.triplea.swing.jpanel;

import java.awt.Component;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class GridLayoutBuilder {

  private final JPanelBuilder panelBuilder;
  private final int rows;
  private final int columns;

  private final List<Component> components = new ArrayList<>();

  public JPanel build() {
    final JPanel panel = panelBuilder.build();
    panel.setLayout(new GridLayout(rows, columns));
    components.forEach(panel::add);
    return panel;
  }

  public GridLayoutBuilder add(final Component component) {
    components.add(component);
    return this;
  }
}
