package org.triplea.swing.jpanel;

import com.google.common.base.Preconditions;
import java.awt.GridBagConstraints;
import java.awt.Insets;

/**
 * Builder for a {@code GridBagConstraint} to be used with {@code GridBagLayout}.
 *
 * <p>Example usage:. <code><pre>
 *     final JPanel panel = new JPanel();
 *     panel.setLayout(new GridBagLayout());
 *     panel.add(
 *       new JLabel("Label"),
 *       new GridBagConstraintBuilder(0, 0)  // gridX, gridY
 *         .gridWidth(1) // default value
 *         .gridHeight(1) // default value
 *         .weightX(0.0) // default value
 *         .weightY(0.0) // default value
 *         .anchor(GridBagConstraintAnchor.CENTER) // default value
 *         .fill(GridBagConstraintFill.NONE) // default value
 *         .insets(0, 0, 0, 0)  // default value:  top = 0 , left = 0, bottom = 0, right = 0
 *         .padX(0) // default value
 *         .padY(0) // default value
 *         .build());
 * </pre></code>
 */
public class GridBagConstraintsBuilder {
  private final int gridX;
  private final int gridY;

  private int gridWidth = 1;
  private int gridHeight = 1;

  private double weightX;
  private double weightY;

  private GridBagConstraintsAnchor anchor = GridBagConstraintsAnchor.WEST;
  private GridBagConstraintsFill fill = GridBagConstraintsFill.NONE;

  private Insets insets = new Insets(0, 0, 0, 0);

  private int padX = 0;
  private int padY = 0;

  public GridBagConstraintsBuilder(final int gridX, final int gridY) {
    Preconditions.checkArgument(gridX >= 0);
    Preconditions.checkArgument(gridY >= 0);
    this.gridX = gridX;
    this.gridY = gridY;
  }

  /** Constructs a Swing {@code GridBagConstraints} using current builder values. */
  public GridBagConstraints build() {
    return new GridBagConstraints(
        gridX,
        gridY,
        gridWidth,
        gridHeight,
        weightX,
        weightY,
        anchor.getMagicConstant(),
        fill.getMagicConstant(),
        insets,
        padX,
        padY);
  }

  /** Default value is 1 */
  public GridBagConstraintsBuilder gridWidth(final int gridWidth) {
    this.gridWidth = gridWidth;
    return this;
  }

  /** Default value is 1 */
  public GridBagConstraintsBuilder gridHeight(final int gridHeight) {
    this.gridHeight = gridHeight;
    return this;
  }

  /** Default value is 0 */
  public GridBagConstraintsBuilder weightX(final double weightX) {
    this.weightX = weightX;
    return this;
  }

  /** Default value is 0 */
  public GridBagConstraintsBuilder weightY(final double weightY) {
    this.weightY = weightY;
    return this;
  }

  /**
   * Sets grid bag constraint anchor value, this dictates how components are placed in the grid
   * cell.
   *
   * <p>Default value is: GridBagConstraintsAnchor.WEST
   */
  public GridBagConstraintsBuilder anchor(final GridBagConstraintsAnchor anchor) {
    this.anchor = anchor;
    return this;
  }

  /**
   * Sets grid bag constraint fill value, this dictates how components are adjusted when placed in
   * grid cell, whether they are expanded to fill space.
   *
   * <p>Default value is: GridBagConstraintsFill.NONE
   */
  public GridBagConstraintsBuilder fill(final GridBagConstraintsFill fill) {
    this.fill = fill;
    return this;
  }

  public GridBagConstraintsBuilder insets(
      final int top, final int left, final int bottom, final int right) {
    insets = new Insets(top, left, bottom, right);
    return this;
  }

  public GridBagConstraintsBuilder insets(final Insets insets) {
    this.insets = insets;
    return this;
  }

  /** Default value is 0 */
  public GridBagConstraintsBuilder padX(final int padX) {
    this.padX = padX;
    return this;
  }

  /** Default value is 0 */
  public GridBagConstraintsBuilder padY(final int padY) {
    this.padY = padY;
    return this;
  }
}
