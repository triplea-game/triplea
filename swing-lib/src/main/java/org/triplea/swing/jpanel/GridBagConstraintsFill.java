package org.triplea.swing.jpanel;

import java.awt.GridBagConstraints;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * This field is used when the component's display area is larger than the component's requested
 * size. It determines whether to resize the component, and if so, how.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum GridBagConstraintsFill {
  /** Do not resize the component. */
  NONE(GridBagConstraints.NONE),
  /**
   * Make the component wide enough to fill its display area horizontally, but do not change its
   * height.
   */
  HORIZONTAL(GridBagConstraints.HORIZONTAL),

  /**
   * Make the component tall enough to fill its display area vertically, but do not change its
   * width.
   */
  VERTICAL(GridBagConstraints.VERTICAL),

  /** Make the component fill its display area * entirely. */
  BOTH(GridBagConstraints.BOTH);

  @Getter(AccessLevel.PACKAGE)
  private final int magicConstant;
}
