package org.triplea.swing.jpanel;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class BoxLayoutBuilder {
  private final JPanelBuilder panelBuilder;
  private final BoxLayoutOrientation boxLayoutOrientation;

  private final List<Component> components = new ArrayList<>();

  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  enum BoxLayoutOrientation {
    HORIZONTAL(BoxLayout.X_AXIS),
    VERTICAL(BoxLayout.Y_AXIS);

    private final int swingLayoutCode;
  }

  public JPanel build() {
    final JPanel panel = panelBuilder.build();
    panel.setLayout(new BoxLayout(panel, boxLayoutOrientation.swingLayoutCode));
    components.forEach(panel::add);
    return panel;
  }

  public BoxLayoutBuilder add(final Component component) {
    components.add(component);
    return this;
  }

  /**
   * use this when you want an empty space component that will take up extra space. For example,
   * with a gridbag layout with 2 columns, if you have 2 components, the second will be stretched by
   * default to fill all available space to the right. This right hand component would then resize
   * with the window. If on the other hand a 3 column grid bag were used and the last element were a
   * horizontal glue, then the 2nd component would then have a fixed size.
   */
  public BoxLayoutBuilder addHorizontalGlue() {
    components.add(Box.createHorizontalGlue());
    return this;
  }

  public BoxLayoutBuilder addVerticalStrut(final int strutSize) {
    components.add(Box.createVerticalStrut(strutSize));
    return this;
  }

  public BoxLayoutBuilder addHorizontalStrut(final int strutSize) {
    components.add(Box.createHorizontalStrut(strutSize));
    return this;
  }

  /**
   * Adds {@code component} to the panel and ensures it will be left-justified in the final layout.
   * Primarily for use with vertical box layouts.
   */
  public BoxLayoutBuilder addLeftJustified(final Component component) {
    final Box box = Box.createHorizontalBox();
    box.add(component);
    box.add(Box.createHorizontalGlue());
    components.add(box);
    return this;
  }
}
