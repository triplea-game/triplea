package org.triplea.swing.key.binding;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class KeyCombinationTest {

  @ParameterizedTest
  @MethodSource
  @DisplayName(
      "Verify all key code with button down mask combinations can be "
          + "translated to an awt KeyStroke object")
  void toKeyStroke(final ButtonDownMask buttonDownMask, final KeyCode keyCode) {
    assertDoesNotThrow(() -> KeyCombination.of(keyCode, buttonDownMask).toKeyStroke());
  }

  /** Returns all tuple combination of all keycodes with button down mask. */
  @SuppressWarnings("unused")
  private static List<Arguments> toKeyStroke() {
    final List<Arguments> arguments = new ArrayList<>();
    for (final ButtonDownMask buttonDownMask : ButtonDownMask.values()) {
      for (final KeyCode keyCode : KeyCode.values()) {
        arguments.add(Arguments.of(buttonDownMask, keyCode));
      }
    }
    return arguments;
  }
}
