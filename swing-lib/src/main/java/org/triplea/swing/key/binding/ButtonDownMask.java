package org.triplea.swing.key.binding;

import java.awt.event.InputEvent;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Mapping between button down mask names and the input event magic int representing that button
 * down. Button down essentially means if you're holding a button down
 */
@AllArgsConstructor
@Getter(AccessLevel.PACKAGE)
public enum ButtonDownMask {
  NONE(0),
  BUTTON1(InputEvent.BUTTON1_DOWN_MASK),
  BUTTON2(InputEvent.BUTTON2_DOWN_MASK),
  BUTTON3(InputEvent.BUTTON3_DOWN_MASK),
  SHIFT(InputEvent.SHIFT_DOWN_MASK),
  CTRL(InputEvent.CTRL_DOWN_MASK),
  META(InputEvent.META_DOWN_MASK),
  ALT(InputEvent.ALT_DOWN_MASK),
  ALT_GRAPH(InputEvent.ALT_GRAPH_DOWN_MASK),
  ;

  private final int inputEventCode;
}
