package org.triplea.swing.jpanel;

import com.google.common.base.Preconditions;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class FlowLayoutBuilder {
  private final JPanelBuilder panelBuilder;

  private final List<Component> components = new ArrayList<>();
  private Direction direction = Direction.CENTER;

  private int hgap = 5;
  private int vgap = 5;

  public JPanel build() {
    final JPanel panel = panelBuilder.build();
    panel.setLayout(new FlowLayout(direction.swingCode, hgap, vgap));
    components.forEach(panel::add);
    return panel;
  }

  @AllArgsConstructor
  public enum Direction {
    CENTER(FlowLayout.CENTER),
    RIGHT(FlowLayout.RIGHT);

    private final int swingCode;
  }

  public FlowLayoutBuilder flowDirection(final Direction direction) {
    this.direction = direction;
    return this;
  }

  public FlowLayoutBuilder add(final Component component) {
    Preconditions.checkNotNull(component);
    components.add(component);
    return this;
  }

  public FlowLayoutBuilder hgap(final int hgap) {
    Preconditions.checkArgument(hgap >= 0);
    this.hgap = hgap;
    return this;
  }

  public FlowLayoutBuilder vgap(final int vgap) {
    Preconditions.checkArgument(vgap >= 0);
    this.vgap = vgap;
    return this;
  }

  public FlowLayoutBuilder addLabel(final String text) {
    return add(new JLabel(text));
  }
}
