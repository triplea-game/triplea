package swinglib;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JComponent;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

/**
 * Helper class for gridbag layouts with a fixed number of columns.
 * To use this class, you construct it using a component that has a GridBagLayout, and you specify
 * how many columns you want. Then just add components and they'll be added to the appropriate column
 * wrapping to the next row when needed.
 * <p>
 * Example usage:
 * </p>
 * <code><pre>
 * JPanel panelToHaveGridBag = new JPanel();
 * int columnCount = 2;
 * GridBagHelper helper = new GridBagHelper(panelToHaveGridBag, 2);
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
    this.parent.setLayout(new GridBagLayout());
    this.columns = columns;
    constraints = new GridBagConstraints();
  }

  /**
   * Adds a child component to the parent component passed in to the GridBagHelper constructor.
   * The child component is added to the grid bag layout at the next column (left to right), wrapping
   * to the next row when needed.
   */
  public void add(final JComponent child) {
    addAll(child);
  }

  public void add(final Component child) {
    addAll(child);
  }

  /**
   * Adds many components in one go, a convenience api {@see add}.
   */
  public void addAll(final JComponent ... children) {
    Preconditions.checkArgument(children.length > 0);
    for (final JComponent child : children) {
      parent.add(child, nextConstraint());
      elementCount++;
    }
  }

  public void addAll(final Component ... children) {
    Preconditions.checkArgument(children.length > 0);
    for (final Component child : children) {
      parent.add(child, nextConstraint());
      elementCount++;
    }
  }

  @VisibleForTesting
  GridBagConstraints nextConstraint() {
    final int x = elementCount % columns;
    final int y = elementCount / columns;

    constraints.gridx = x;
    constraints.gridy = y;

    constraints.anchor = GridBagConstraints.WEST;

    constraints.ipadx = 3;
    constraints.ipady = 3;
    return constraints;

  }
}
