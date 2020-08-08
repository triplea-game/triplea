package org.triplea.swing.jpanel;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JPanel;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.triplea.java.Postconditions;

/**
 * Convenience builder for Gridbag layout where row/column numbers are auto-incremented. Example:
 * <code><pre>
 *   final int columnCount = 2;
 *   final JPanel panel = new JPanelBuilder()
 *       .gridLayout(columnCount)
 *       .add(new JLabel("placed in: row 0, column 0"))
 *       .add(new JLabel("placed in: row 0, column 1"))
 *       .add(new JLabel("placed in: row 1, column 0"))
 *       .add(new JLabel("placed in: row 1, column 1"))
 *       .build();
 * </pre></code>
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class SimpleGridBagLayoutBuilder {
  private final JPanelBuilder panelBuilder;
  private final int columnCount;

  private final List<GridBagComponent> components = new ArrayList<>();
  private int element = 0;

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

  public SimpleGridBagLayoutBuilder add(final JComponent component) {
    final CoordinateCalculator coordinateCalculator = new CoordinateCalculator(columnCount);
    components.add(
        new GridBagComponent(
            component,
            new GridBagConstraintsBuilder(
                    coordinateCalculator.calculateColumn(element),
                    coordinateCalculator.calculateRow(element))
                .build()));
    element++;
    return this;
  }

  public SimpleGridBagLayoutBuilder add(
      final JComponent component,
      final GridBagConstraintsAnchor anchor,
      final GridBagConstraintsFill fill) {
    final CoordinateCalculator coordinateCalculator = new CoordinateCalculator(columnCount);
    components.add(
        new GridBagComponent(
            component,
            new GridBagConstraintsBuilder(
                    coordinateCalculator.calculateColumn(element),
                    coordinateCalculator.calculateRow(element))
                .anchor(anchor)
                .fill(fill)
                .build()));
    element++;
    return this;
  }

  @VisibleForTesting
  static class CoordinateCalculator {
    private final int columnCount;

    CoordinateCalculator(final int columnCount) {
      Preconditions.checkArgument(columnCount > 0);
      this.columnCount = columnCount;
    }

    int calculateRow(final int elementCount) {
      return elementCount / columnCount;
    }

    int calculateColumn(final int elementCount) {
      final int column = elementCount % columnCount;
      Postconditions.assertState(
          column < columnCount, "Column out of bounds: " + column + ", max: " + columnCount);
      return column;
    }
  }
}
