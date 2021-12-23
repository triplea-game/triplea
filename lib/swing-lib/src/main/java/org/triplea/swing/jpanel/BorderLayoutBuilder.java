package org.triplea.swing.jpanel;

import com.google.common.base.Preconditions;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class BorderLayoutBuilder {
  private JPanelBuilder panelBuilder;

  private final List<BorderLayoutComponent> components = new ArrayList<>();

  @AllArgsConstructor
  private static class BorderLayoutComponent {
    private final Component component;
    private final BorderLayoutPosition position;

    private void addToPanel(final JPanel panel) {
      position.place(component, panel);
    }
  }

  /** Swing border layout locations. */
  @AllArgsConstructor
  private enum BorderLayoutPosition {
    CENTER(BorderLayout.CENTER),
    SOUTH(BorderLayout.SOUTH),
    NORTH(BorderLayout.NORTH),
    WEST(BorderLayout.WEST),
    EAST(BorderLayout.EAST);

    private final String swingPlacement;

    private void place(final Component component, final JPanel panel) {
      panel.add(component, swingPlacement);
    }
  }

  public JPanel build() {
    final JPanel panel = panelBuilder.build();
    panel.setLayout(new BorderLayout());
    components.forEach(c -> c.addToPanel(panel));
    return panel;
  }

  /** Adds a given component to the southern portion of a border layout. */
  public BorderLayoutBuilder addSouth(final Component component) {
    return add(component, BorderLayoutPosition.SOUTH);
  }

  /** Adds a given component to the 'north' portion of the border layout. */
  public BorderLayoutBuilder addNorth(final Component component) {
    return add(component, BorderLayoutPosition.NORTH);
  }

  /** Adds a given component to the 'east' portion of the border layout. */
  public BorderLayoutBuilder addEast(final Component component) {
    return add(component, BorderLayoutPosition.EAST);
  }

  /** Adds a given component to the 'west' portion of the border layout. */
  public BorderLayoutBuilder addWest(final Component component) {
    return add(component, BorderLayoutPosition.WEST);
  }

  /** Adds a given component to the 'center' portion of the border layout. */
  public BorderLayoutBuilder addCenter(final Component component) {
    return add(component, BorderLayoutPosition.CENTER);
  }

  private BorderLayoutBuilder add(final Component component, final BorderLayoutPosition position) {
    Preconditions.checkNotNull(component);
    components.add(new BorderLayoutComponent(component, position));
    return this;
  }
}
