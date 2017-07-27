package swinglib;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JComponent;
import javax.swing.SpringLayout;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

/**
 * Helper class for gridbag layouts with a fixed number of columns.
 * Example usage:
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
   * Adds components to the parent component used when constructing the {@code GridBagHelper}.
   * Components are added as rows in a grid bag layout and wrap to the next column when appropriate.
   * You can call this method multiple times and keep appending components.
   */
  public void addComponents(final JComponent ... children) {
    Preconditions.checkArgument(children.length > 0);
    for (final JComponent child : children) {
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
