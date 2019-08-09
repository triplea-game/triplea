package org.triplea.swing;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JComponent;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Helper class for GridBag layouts with a fixed number of columns. <br>
 * Instead of adding a GridBagLayout, you can instantiate this helper with your Swing component
 * instead, then add children to the helper. <br>
 * Child components will wrap to the next row as needed. <br>
 * Example usage: <code><pre>
 * // precondition stuff
 * JPanel panelToHaveGridBag = new JPanel();
 * int columnCount = 2;
 *
 * GridBagHelper helper = new GridBagHelper(panelToHaveGridBag, columnCount);
 * // adding 10 elements would create a 2x5 grid
 * for(int i = 0; i < 10; i ++ ) {
 *   helper.addComponents(childComponent);
 * }
 * </pre></code>
 */
public final class GridBagHelper {

  private final JComponent parent;
  private final int columns;
  private final GridBagConstraints constraints;
  private int elementCount = 0;

  public GridBagHelper(final JComponent parent, final int columns) {
    this.parent = parent;
    parent.setLayout(new GridBagLayout());
    this.columns = columns;
    constraints = new GridBagConstraints();
  }

  /**
   * Adds a component spanning multiple columns.
   *
   * @param child The component to be added.
   * @param columnSpan The number of columns to span.
   */
  void add(
      final Component child, final ColumnSpan columnSpan, final Anchor anchor, final Fill fill) {
    Preconditions.checkNotNull(child);
    Preconditions.checkNotNull(columnSpan);

    final GridBagConstraints gridBagConstraints = nextConstraint(anchor, fill);
    gridBagConstraints.gridwidth = columnSpan.value;

    parent.add(child, constraints);
    elementCount += columnSpan.value;
    gridBagConstraints.gridwidth = 1;
  }

  /**
   * Adds a child component to the parent component passed in to the GridBagHelper constructor. The
   * child component is added to the grid bag layout at the next column (left to right), wrapping to
   * the next row when needed.
   */
  public void add(final Component child) {
    addAll(child);
  }

  /** Gets the next constraint that would be applied to the next component added. */
  @VisibleForTesting
  GridBagConstraints nextConstraint() {
    return nextConstraint(Anchor.WEST, Fill.NONE);
  }

  GridBagConstraints nextConstraint(final Anchor anchor, final Fill fill) {
    final int x = elementCount % columns;
    final int y = elementCount / columns;

    constraints.gridx = x;
    constraints.gridy = y;

    constraints.anchor = anchor.getGridBagConstraintValue();
    constraints.fill = fill.getGridBagConstraintValue();

    constraints.ipadx = 3;
    constraints.ipady = 3;

    constraints.gridwidth = 1;
    constraints.gridheight = 1;

    constraints.weightx = fill.weightX;
    constraints.weighty = fill.weightY;

    return constraints;
  }

  /**
   * Adds many components in one go, a convenience API.
   *
   * @see #add(Component)
   */
  public void addAll(final Component... children) {
    Preconditions.checkArgument(children.length > 0);
    for (final Component child : children) {
      parent.add(child, nextConstraint());
      elementCount++;
    }
  }

  /**
   * Type safe 'anchor' values, these are aliases for magic values in {@code GridBagConstraints}.
   * Anchor is essentially the same as alignment.
   */
  @AllArgsConstructor
  @Getter
  public enum Anchor {
    WEST(GridBagConstraints.WEST),

    CENTER(GridBagConstraints.CENTER);

    private final int gridBagConstraintValue;
  }

  /**
   * Type safe 'Fill' values, these are aliases for magic values in {@code GridBagConstraints} Fill
   * defines how a component stretches into available space.
   */
  @AllArgsConstructor
  @Getter
  public enum Fill {
    NONE(GridBagConstraints.NONE),

    VERTICAL(GridBagConstraints.VERTICAL),

    VERTICAL_AND_HORIZONTAL(GridBagConstraints.BOTH, 1, 1);

    private final int gridBagConstraintValue;
    private final int weightX;
    private final int weightY;

    Fill(final int gridBagConstraintValue) {
      this(gridBagConstraintValue, 0, 0);
    }
  }

  /**
   * Value class wrapper, represents how many columns a given cell should span in a {@code
   * GridBagLayout}.
   */
  public static class ColumnSpan {

    final int value;

    private ColumnSpan(final int value) {
      this.value = value;
    }

    public static ColumnSpan of(final int newValue) {
      Preconditions.checkState(newValue > 0);
      return new ColumnSpan(newValue);
    }
  }
}
