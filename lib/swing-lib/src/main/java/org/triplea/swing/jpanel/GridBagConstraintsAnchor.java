package org.triplea.swing.jpanel;

import java.awt.GridBagConstraints;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * This field is used when the component is smaller than its display area. It determines where,
 * within the display area, to place the component.
 *
 * <p>There are three kinds of possible values: orientation relative, baseline relative and
 * absolute. Orientation relative values are interpreted relative to the container's component
 * orientation property, baseline relative values are interpreted relative to the baseline and
 * absolute values are not. The absolute values are: {@code CENTER}, {@code NORTH}, {@code
 * NORTHEAST}, {@code EAST}, {@code SOUTHEAST}, {@code SOUTH}, {@code SOUTHWEST}, {@code WEST}, and
 * {@code NORTHWEST}. The orientation relative values are: {@code PAGE_START}, {@code PAGE_END},
 * {@code LINE_START}, {@code LINE_END}, {@code FIRST_LINE_START}, {@code FIRST_LINE_END}, {@code
 * LAST_LINE_START} and {@code LAST_LINE_END}. The baseline relative values are: {@code BASELINE},
 * {@code BASELINE_LEADING}, {@code BASELINE_TRAILING}, {@code ABOVE_BASELINE}, {@code
 * ABOVE_BASELINE_LEADING}, {@code ABOVE_BASELINE_TRAILING}, {@code BELOW_BASELINE}, {@code
 * BELOW_BASELINE_LEADING}, and {@code BELOW_BASELINE_TRAILING}. The default value is {@code
 * CENTER}.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum GridBagConstraintsAnchor {
  CENTER(GridBagConstraints.CENTER),
  NORTH(GridBagConstraints.NONE),
  NORTHEAST(GridBagConstraints.NORTHEAST),
  EAST(GridBagConstraints.EAST),
  SOUTHEAST(GridBagConstraints.SOUTHEAST),
  SOUTH(GridBagConstraints.SOUTH),
  SOUTHWEST(GridBagConstraints.SOUTHWEST),
  WEST(GridBagConstraints.WEST),
  NORTHWEST(GridBagConstraints.NORTHWEST),
  PAGE_START(GridBagConstraints.PAGE_START),
  PAGE_END(GridBagConstraints.PAGE_END),
  LINE_START(GridBagConstraints.LINE_START),
  LINE_END(GridBagConstraints.LINE_END),
  FIRST_LINE_START(GridBagConstraints.FIRST_LINE_START),
  FIRST_LINE_END(GridBagConstraints.FIRST_LINE_END),
  LAST_LINE_START(GridBagConstraints.LAST_LINE_START),
  LAST_LINE_END(GridBagConstraints.LAST_LINE_END),
  BASELINE(GridBagConstraints.BASELINE),
  BASELINE_LEADING(GridBagConstraints.BASELINE_LEADING),
  BASELINE_TRAILING(GridBagConstraints.BASELINE_TRAILING),
  ABOVE_BASELINE(GridBagConstraints.ABOVE_BASELINE),
  ABOVE_BASELINE_LEADING(GridBagConstraints.ABOVE_BASELINE_LEADING),
  ABOVE_BASELINE_TRAILING(GridBagConstraints.ABOVE_BASELINE_TRAILING),
  BELOW_BASELINE(GridBagConstraints.BELOW_BASELINE),
  BELOW_BASELINE_LEADING(GridBagConstraints.BELOW_BASELINE_LEADING),
  BELOW_BASELINE_TRAILING(GridBagConstraints.BELOW_BASELINE_TRAILING);

  @Getter(AccessLevel.PACKAGE)
  private final int magicConstant;
}
