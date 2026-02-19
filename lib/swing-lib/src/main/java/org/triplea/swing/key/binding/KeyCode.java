package org.triplea.swing.key.binding;

import java.awt.event.KeyEvent;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Enum to represent Swing KeyEvent objects. Rather than storing the keys as hex numbers, this class
 * stores them as enums which can then be mapped to the magic number defined in {@code KeyEvent}
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public enum KeyCode {
  SPACE(KeyEvent.VK_SPACE),
  ENTER(KeyEvent.VK_ENTER),
  ESCAPE(KeyEvent.VK_ESCAPE),
  PERIOD(KeyEvent.VK_PERIOD),
  COMMA(KeyEvent.VK_COMMA),
  EQUALS(KeyEvent.VK_EQUALS),
  MINUS(KeyEvent.VK_MINUS),
  UP(KeyEvent.VK_UP),
  DOWN(KeyEvent.VK_DOWN),
  A(KeyEvent.VK_A),
  B(KeyEvent.VK_B),
  C(KeyEvent.VK_C),
  D(KeyEvent.VK_D),
  E(KeyEvent.VK_E),
  F(KeyEvent.VK_F),
  G(KeyEvent.VK_G),
  H(KeyEvent.VK_H),
  I(KeyEvent.VK_I),
  J(KeyEvent.VK_J),
  K(KeyEvent.VK_K),
  L(KeyEvent.VK_L),
  M(KeyEvent.VK_M),
  N(KeyEvent.VK_N),
  O(KeyEvent.VK_O),
  P(KeyEvent.VK_P),
  Q(KeyEvent.VK_Q),
  R(KeyEvent.VK_R),
  S(KeyEvent.VK_S),
  T(KeyEvent.VK_T),
  U(KeyEvent.VK_U),
  V(KeyEvent.VK_V),
  W(KeyEvent.VK_W),
  X(KeyEvent.VK_X),
  Y(KeyEvent.VK_Y),
  Z(KeyEvent.VK_Z),
  NR_0(KeyEvent.VK_0),
  NR_1(KeyEvent.VK_1),
  NR_2(KeyEvent.VK_2),
  NR_3(KeyEvent.VK_3),
  NR_4(KeyEvent.VK_4),
  NR_5(KeyEvent.VK_5),
  NR_6(KeyEvent.VK_6),
  NR_7(KeyEvent.VK_7),
  NR_8(KeyEvent.VK_8),
  NR_9(KeyEvent.VK_9),
  ;

  private final int inputEventCode;
}
